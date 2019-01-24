package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.test.fatigue.SessionPVT.Block;

/**
 * Model of PVT test and Fatigue mechanism
 * 
 * Paper: Efficient driver drowsiness detection at moderate levels of drowsiness
 * 
 * Pia M. Forsmana, Bryan J. Vilaa,b, Robert A. Short c, Christopher G. Mott d, Hans P.A. Van Dongena,
 * 
 * @author Ehsan Khosroshahi
 */



public class PVTdriver_NightPRE extends Task {
	private double PVTduration = 0;
	private ArrayList<Double> timesOfPVT;

	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	private int sleepAttackIndex = 0; // the variable for handling sleep attacks
	Random random;

	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private SessionPVT currentSession;
	private Vector<SessionPVT> sessions = new Vector<SessionPVT>();

	public PVTdriver_NightPRE() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		random = new Random();
		lastTime = 0;
		currentSession = new SessionPVT();
		currentBlock = currentSession.new Block();
		stimulusVisibility = false;
		currentSession.startTime = 0;
		currentBlock.startTime = 0;

		PVTduration = getModel().getFatigue().getTaskDuration();
		timesOfPVT = getModel().getFatigue().getTaskSchdule();

		getModel().getFatigue().setFatigueHour(timesOfPVT.get(sessionNumber));
		getModel().getFatigue().startFatigueSession();


		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);
	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;
		currentBlock.totalBlockTime = getModel().getTime() - currentBlock.startTime;

		if (currentSession.totalSessionTime <= PVTduration) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			if (getModel().isVerbose())
				getModel().output("!!!!! Stimulus !!!!!");

			lastTime = getModel().getTime(); // when the stimulus has happened

			// Handling the sleep attacks -- adding an event in 30 s to see if
			// the current stimulus is still on
			currentSession.stimulusIndex++;
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility == true) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						currentSession.reactionTimes.add(30000);
						currentSession.timeOfReactionsFromStart.add(currentSession.totalSessionTime);
						currentBlock.reactionTimes.add(30000);
						currentBlock.timeOfReactionsFromStart.add(currentBlock.totalBlockTime);
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at session time  ==> " + (getModel().getTime() - currentSession.startTime)
								+ " model time :" + getModel().getTime());
						getModel().output("Stimulus index in the session ==> " + currentSession.stimulusIndex );

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage(); // reseting the system
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
					repaint();
				}
			});

			// Handling the 5-min blocks
			// adding a new block
			if (currentBlock.totalBlockTime >= 300 ) {
				currentSession.blocks.add(currentBlock);
				currentBlock = currentSession.new Block();
				currentBlock.startTime = currentSession.startTime + currentSession.blockIndex * 300.0;
				currentSession.blockIndex++;
			}
		}

		// Starting a new Session
		else {
			currentSession.blocks.add(currentBlock);
			currentSession.bioMathValue = getModel().getFatigue().getBioMathModelValueforHour(timesOfPVT.get(sessionNumber));
			currentSession.timeAwake = getModel().getFatigue().getTimeAwake(timesOfPVT.get(sessionNumber));
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.size()) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new SessionPVT();
						currentBlock = currentSession.new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						currentBlock.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT.get(sessionNumber));
						getModel().getFatigue().startFatigueSession();

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
				});
			} else {
				getModel().stop();
			}
		}
	}

	@Override
	public void eval(Iterator<String> it) {
		it.next(); // (
		String cmd = it.next();
		if (cmd.equals("fatigue-reset-percentage")) {
			fatigueResetPercentage();
		}
	}

	// calling percentage reset after any new task presentation (audio or visual)
	void fatigueResetPercentage() {
		getModel().getFatigue().fatigueResetPercentages();
		if (getModel().isVerbose())
			getModel().output("!!!! Fatigue Percentage Reset !!!!");
	}

	@Override
	public void typeKey(char c) {
		double currentSessionTime = getModel().getTime() - currentSession.startTime;
		double currentBlockTime = getModel().getTime() - currentBlock.startTime;
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond

			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentSession.timeOfReactionsFromStart.add(currentSessionTime);
				currentBlock.reactionTimes.add(responseTime);
				currentBlock.timeOfReactionsFromStart.add(currentBlockTime);
			}

			label.setVisible(false);
			processDisplay();

			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentSession.timeOfReactionsFromStart.add(currentSessionTime);
			currentBlock.reactionTimes.add(1);
			currentBlock.timeOfReactionsFromStart.add(currentBlockTime);
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}
	}

		@Override
		public int analysisIterations() {
	 		return 13;
		}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		DecimalFormat df = new DecimalFormat("#.00");
		PVTdriver_NightPRE t = (PVTdriver_NightPRE) tasks[0]; // getting the numbers of sessions and blocks
		int numberOfSessions = t.sessions.size();
		

		Values[] totalProportionLapses = new Values[numberOfSessions];
		Values[] totalProportionFalseAlerts = new Values[numberOfSessions]; 
		Values[] totalProportionSleepAtacks = new Values[numberOfSessions];
		Values[] totalProportionAlertRresponces = new Values[numberOfSessions];
		Values[][] totalProportionAlertResponcesDis = new Values[numberOfSessions][35];
		Values[] totalMeanAlertRresponces = new Values[numberOfSessions];
		Values[] totalMedianAlertRresponces = new Values[numberOfSessions];
		Values[] totalLSNRapx = new Values[numberOfSessions];
		
		// allocating memory to the vectors
		for (int i = 0; i < numberOfSessions; i++) {
			totalProportionLapses[i] = new Values();
			totalProportionFalseAlerts[i] = new Values();
			totalProportionSleepAtacks[i] = new Values();
			totalProportionAlertRresponces[i] = new Values();
			totalMeanAlertRresponces[i] = new Values();
			totalMedianAlertRresponces[i] = new Values();
			totalLSNRapx[i] = new Values();
			for (int j = 0; j < 35; j++) {
				totalProportionAlertResponcesDis[i][j] = new Values();
			}	
		}

		
		for (Task taskCast : tasks) {
			PVTdriver_NightPRE task = (PVTdriver_NightPRE) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				totalProportionFalseAlerts[i].add(session.getProportionOfFalseStarts());
				totalProportionLapses[i].add(session.getProportionOfLapses());
				totalProportionSleepAtacks[i].add(session.getProportionOfSleepAttacks());
				totalProportionAlertRresponces[i].add(session.getProportionOfAlertResponses());
				totalMeanAlertRresponces[i].add(session.getMeanAlertReactionTimes());
				totalMedianAlertRresponces[i].add(session.getMedianAlertReactionTimes());
				totalLSNRapx[i].add(session.getLSNR_apx());
				double [] proportionAlertDis = session.getProportionAlertResponseDistribution(); 
				for (int j = 0; j < 35; j++) {
					totalProportionAlertResponcesDis[i][j].add(proportionAlertDis[j]);
				}
			}
		}
	
		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");
		
//		getModel().output("******* Proportion of Responses **********\n");
//		getModel()
//		.output("#\tFS  "
//				+ " ---------------------------    Alert Responses    --------------------------- "
//				+ " Alert Responses "
//				+ " ---------------------------    Alert Responses    ---------------------------- "
//				+ "L    SA");
		
		getModel().output("******* Average Proportion of Responses **********\n");
		getModel().output("#\tFS\t" + "AR\t " + "L\t"+ "SA");
		
//		double[] AlertResponsesProportion = new double[35];
		for (int s = 0; s < numberOfSessions; s++) {
//			for (int i = 0; i < 35; i++)
//				AlertResponsesProportion[i] = totallProportionAlertResponcesSpread[s][i].mean();

			getModel().output(s + "\t"+
					df2.format( totalProportionFalseAlerts[s].mean()) + "\t"
					//+ Utilities.toString(AlertResponsesProportion) + " "
					+ df2.format( totalProportionAlertRresponces[s].mean()) + "\t"
					+ df2.format(totalProportionLapses[s].mean()) + "\t"
					+ df2.format(totalProportionSleepAtacks[s].mean()));	
		}
		
		getModel().output("\nAverage proportion of lapses in the time points \n" );
		getModel().output("Day\t21:00\t00:00\t03:00\t06:00 " );
		for (int i = 0; i < 5; i++) {	
			getModel().output((i+1)+"\t"+df3.format(totalProportionLapses[i*4].mean())+"\t"+df3.format(totalProportionLapses[i*4+1].mean())+"\t"
					+df3.format(totalProportionLapses[i*4+2].mean())+"\t"+df3.format(totalProportionLapses[i*4+3].mean()));
		}	
		getModel().output("\n*******************************************\n");

		
		
		try {
			
			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File("./test/fatigue/pvt_driver/DataSessions.csv");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);
			
			dataSession.print("Time Awake" + ",");
			PVTdriver_NightPRE task = (PVTdriver_NightPRE) tasks[0];
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				dataSession.print(session.timeAwake + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("Lapses (P)" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalProportionLapses[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("FalseStarts (P)" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalProportionFalseAlerts[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("Median Alert RT" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalMedianAlertRresponces[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("LSNR_apx" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalLSNRapx[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("\n");
			
			dataSession.print("Lapses (P) SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalProportionLapses[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("FalseStarts (P) SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalProportionFalseAlerts[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("Median RT SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalMedianAlertRresponces[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("LSNR_apx SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totalLSNRapx[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("\n");
			
			dataSession.close();
			

			// saving the raw times of lapses
			File rawLapsesFile = new File("./test/fatigue/pvt_driver/RawLapsesNightPRE.csv");
			if (!rawLapsesFile.exists())
				rawLapsesFile.createNewFile();
			PrintStream rawLapses = new PrintStream(rawLapsesFile);

			rawLapses.println("session#,TimeInSession,RT");
			
			for (Task taskCast : tasks) {
				PVTdriver_NightPRE taskLapses = (PVTdriver_NightPRE) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					SessionPVT session = taskLapses.sessions.get(i);
					for (int j = 0; j < session.reactionTimes.size(); j++) {
						if (session.reactionTimes.get(j) > 500 && session.reactionTimes.get(j) < 30000){
							rawLapses.println(i + "," + session.timeOfReactionsFromStart.get(j) + "," + session.reactionTimes.get(j));
							rawLapses.flush();
						}		
					}
				}
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
		Result result = new Result();
		return result;
	}

}
