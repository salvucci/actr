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
 * Model of PVT test and Fatigue mechanism : PVT on 88 hours of sleep deprivation 
 * 
 * Paper: Sleep Deprivation and Sustained Attention Performance:
 * 			Integrating Mathematical and Cognitive Modeling
 * 
 * Glenn Gunzlemann, Joshua B.Gross, Kevin A. Gluck, David F. Dinges 
 * 
 * @author Ehsan Khosroshahi
 */

public class PVT88hours extends Task {
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

	public PVT88hours() {
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

	//	@Override
	//	public int analysisIterations() {
	// 		return 100;
	//	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

	
		PVT88hours t = (PVT88hours) tasks[0]; // getting the numbers of sessions and blocks
		int numberOfSessions = t.sessions.size();
		int numberOfBlocks = t.sessions.get(0).blocks.size();

		Values[] AveProportionLapses = new Values[numberOfSessions];
		Values[] AveProportionFalseAlerts = new Values[numberOfSessions];
		Values[] AveMedianAlertResponses = new Values[numberOfSessions];
		Values[] AveLSNRapx = new Values[numberOfSessions];
		Values[] AveProportionSleepAtacks = new Values[numberOfSessions];
		Values[][] AveProportionAlertResponcesDis = new Values[numberOfSessions][35];

		// allocating memory to the vectors
		for (int i = 0; i < numberOfSessions; i++) {
			AveProportionLapses[i] = new Values();
			AveProportionFalseAlerts[i] = new Values();
			AveMedianAlertResponses[i] = new Values();
			AveProportionSleepAtacks[i] = new Values();
			AveLSNRapx[i] = new Values();
			for (int j = 0; j < 35; j++) {
				AveProportionAlertResponcesDis[i][j] = new Values();
			}	
		}

		// data for the blocks
		Values [][] blocksAveProportionLapses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksAveProportionFalseAlerts = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksAveMedianAlertResponses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksAveLSNRapx = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksAveProportionSleepAtacks = new Values[numberOfSessions][numberOfBlocks];
		Values [][][] blocksAveProportionAlertResponcesDis = new Values[numberOfSessions][numberOfBlocks][35];

		for (int i = 0; i < numberOfSessions; i++) {
			for (int j = 0; j < numberOfBlocks; j++) {
				blocksAveProportionLapses[i][j] = new Values();
				blocksAveProportionFalseAlerts[i][j] = new Values();
				blocksAveMedianAlertResponses[i][j] = new Values();
				blocksAveLSNRapx[i][j] = new Values();
				blocksAveProportionSleepAtacks[i][j] = new Values();
				for (int k = 0; k < 35; k++) {
					blocksAveProportionAlertResponcesDis[i][j][k] = new Values();
				}
			}
		}

		
		for (Task taskCast : tasks) {
			PVT88hours task = (PVT88hours) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				AveProportionLapses[i].add(session.getProportionOfLapses());
				AveProportionFalseAlerts[i].add(session.getProportionOfFalseAlert());
				AveMedianAlertResponses[i].add(session.getMedianAlertReactionTimes());
				AveLSNRapx[i].add(session.getLSNR_apx());
				AveProportionSleepAtacks[i].add(session.getProportionOfSleepAttacks());
				double [] proportionAlertDis = session.getProportionAlertResponseDistribution(); 
				for (int j = 0; j < 35; j++) {
					AveProportionAlertResponcesDis[i][j].add(proportionAlertDis[j]);
				}

				
				for (int j = 0; j < session.blocks.size(); j++) {
					Block b = session.blocks.get(j);
					blocksAveProportionLapses[i][j].add(b.getNumberOfLapses());
					blocksAveProportionFalseAlerts[i][j].add(b.getNumberOfFalseAlerts());
					blocksAveMedianAlertResponses[i][j].add(b.getMedianAlertReactionTimes());
					blocksAveLSNRapx[i][j].add(b.getLSNR_apx());
					blocksAveProportionSleepAtacks[i][j].add(b.getProportionOfSleepAttacks());
					double blocksAP[] = b.getProportionAlertResponseDistribution();
					for (int k = 0; k < 35; k++) {
						blocksAveProportionAlertResponcesDis[i][j][k].add(blocksAP[k]);
					}
				}

			}
		}

		////////////////////////////////////////////////////////////

		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");

		getModel().output("******* The Reslut Based on the Sessions **********\n");

		getModel().outputInLine("Time Awake     " + "\t");
		PVT88hours task = (PVT88hours) tasks[0];
		for (int i = 0; i < numberOfSessions; i++) {
			SessionPVT session = task.sessions.get(i);
			getModel().outputInLine(session.timeAwake + "\t");
		}
		getModel().output("\n");

		getModel().outputInLine("Lapses %     " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(AveProportionLapses[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("FalseStarts %" + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(AveProportionFalseAlerts[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("Median AlertRT" + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(AveMedianAlertResponses[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("LSNR_apx       " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(AveLSNRapx[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("Sleep Attacks  " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(AveProportionSleepAtacks[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("\n\n");
		
		
		///////////////////////////////////////////////////////////////////////////////////////////////////		
		getModel().output("******* Proportion of Responses **********\n");
		getModel().output("Day\tFS\t"
				+ " ---------------------------    Alert Responses    --------------------------- "
				+ " Alert Responses "
				+ " ---------------------------    Alert Responses    ----------------------------\t"
				+ "L\tSA");
		// -------------------   BASE LINE  ---------------------------
		Values BaseFA = new Values();
		Values[] BaseAR = new Values[35];
		Values BaseL = new Values();
		Values BaseSA = new Values();
		for (int j = 0; j < 35; j++) 
			BaseAR[j] = new Values();

		for (int s = 0; s < 8; s++) {
			for (int i = 0; i < 35; i++)
				BaseAR[i].add(AveProportionAlertResponcesDis[s][i].mean());
			BaseFA.add(AveProportionFalseAlerts[s].mean());
			BaseL.add(AveProportionLapses[s].mean());
			BaseSA.add(AveProportionSleepAtacks[s].mean());
		}

		double[] AlertResponsesProportionBase = new double[35];
		for (int i = 0; i < 35; i++)
			AlertResponsesProportionBase[i] = BaseAR[i].mean();
		getModel().output("Base" + "\t"+
				df2.format( BaseFA.mean()) + "\t"
				+ Utilities.toString(AlertResponsesProportionBase) + " "
				+ df2.format(BaseL.mean()) + "\t"
				+ df2.format(BaseSA.mean()));
		// ----------------------- END OF BASE LINE --------------------------
		
		// ----------------------------   DAY 1  -----------------------------
		Values Day1FA = new Values();
		Values[] Day1AR = new Values[35];
		Values Day1L = new Values();
		Values Day1SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day1AR[j] = new Values();

		for (int s = 8; s < 20; s++) {
			for (int i = 0; i < 35; i++)
				Day1AR[i].add(AveProportionAlertResponcesDis[s][i].mean());
			Day1FA.add(AveProportionFalseAlerts[s].mean());
			Day1L.add(AveProportionLapses[s].mean());
			Day1SA.add(AveProportionSleepAtacks[s].mean());
		}

		double[] AlertResponsesProportionDay1 = new double[35];
		for (int i = 0; i < 35; i++)
			AlertResponsesProportionDay1[i] = Day1AR[i].mean();
		getModel().output("Day 1" + "\t"+
				df2.format( BaseFA.mean()) + "\t"
				+ Utilities.toString(AlertResponsesProportionDay1) + " "
				+ df2.format(BaseL.mean()) + "\t"
				+ df2.format(BaseSA.mean()));
		// ----------------------- END OF DAY 1 --------------------------
		
		// ------------------------    DAY 2   ---------------------------
		Values Day2FA = new Values();
		Values[] Day2AR = new Values[35];
		Values Day2L = new Values();
		Values Day2SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day2AR[j] = new Values();

		for (int s = 20; s < 32; s++) {
			for (int i = 0; i < 35; i++)
				Day2AR[i].add(AveProportionAlertResponcesDis[s][i].mean());
			Day2FA.add(AveProportionFalseAlerts[s].mean());
			Day2L.add(AveProportionLapses[s].mean());
			Day2SA.add(AveProportionSleepAtacks[s].mean());
		}

		double[] AlertResponsesProportionDay2 = new double[35];
		for (int i = 0; i < 35; i++)
			AlertResponsesProportionDay2[i] = Day2AR[i].mean();
		getModel().output("Day 2" + "\t"+
				df2.format( Day2FA.mean()) + "\t"
				+ Utilities.toString(AlertResponsesProportionDay2) + " "
				+ df2.format(Day2L.mean()) + "\t"
				+ df2.format(Day2SA.mean()));
		// ----------------------- END OF DAY 2 --------------------------
		
		// -----------------------    DAY 3   ---------------------------
		Values Day3FA = new Values();
		Values[] Day3AR = new Values[35];
		Values Day3L = new Values();
		Values Day3SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day3AR[j] = new Values();

		for (int s = 32; s < 44; s++) {
			for (int i = 0; i < 35; i++)
				Day3AR[i].add(AveProportionAlertResponcesDis[s][i].mean());
			Day3FA.add(AveProportionFalseAlerts[s].mean());
			Day3L.add(AveProportionLapses[s].mean());
			Day3SA.add(AveProportionSleepAtacks[s].mean());
		}

		double[] AlertResponsesProportionDay3 = new double[35];
		for (int i = 0; i < 35; i++)
			AlertResponsesProportionDay3[i] = Day3AR[i].mean();
		getModel().output("Day 3" + "\t"+
				df2.format( Day3FA.mean()) + "\t"
				+ Utilities.toString(AlertResponsesProportionDay3) + " "
				+ df2.format(Day3L.mean()) + "\t"
				+ df2.format(Day3SA.mean()));
		// ----------------------- END OF DAY 3 --------------------------

		///////////////////////////////////////////////////////////////////////////////////////////////////		
		// Writing the output to csv files in the specified directory (outputDIR)
		Result result = new Result();
		String DIR = getModel().getFatigue().getOutputDIR();

		if (DIR == null)
			return result;

		try {
			///////////////////////////////////////////////////////////////////////////////////////////////////					
			// Writing Numbers to the file based on days

			//File dataFile = new File("./test/fatigue/pvt_88hour/DistributionForDays.csv");
			File dataFile = new File(DIR + "/DistributionForDays.csv");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintStream data = new PrintStream(dataFile);

			data.println("Day,FA,150,160,170,180,190,200,210,220,230,240,250,260,270,280,290,300,"
					+ "310,320,330,340,350,360,370,380,390,400,410,420,430,440,450,460,470,480,490,Lapses,SA");

			// Base line
			data.print("BaseLine,"+ BaseFA.mean()+ ",");
			for (int i = 0; i < AlertResponsesProportionBase.length; i++) 
				data.print(AlertResponsesProportionBase[i] + ",");
			data.print(BaseL.mean() + ",");
			data.print(BaseSA.mean());
			data.print("\n");
			data.flush();

			// Day1 line
			data.print("Day1," + Day1FA.mean()+ ",");
			for (int i = 0; i < AlertResponsesProportionDay1.length; i++) 
				data.print(AlertResponsesProportionDay1[i] + ",");
			data.print(Day1L.mean() + ",");
			data.print(Day1SA.mean());
			data.print("\n");
			data.flush();

			// Day2 line
			data.print("Day2," + Day2FA.mean()+ ",");
			for (int i = 0; i < AlertResponsesProportionDay2.length; i++) 
				data.print(AlertResponsesProportionDay2[i] + ",");
			data.print(Day2L.mean() + ",");
			data.print(Day2SA.mean());
			data.print("\n");
			data.flush();

			// Day3 line
			data.print("Day3," + Day3FA.mean()+ ",");
			for (int i = 0; i < AlertResponsesProportionDay3.length; i++) 
				data.print(AlertResponsesProportionDay3[i] + ",");
			data.print(Day3L.mean() + ",");
			data.print(Day3SA.mean());
			data.print("\n");
			data.flush();

			data.close();
			
			///////////////////////////////////////////////////////////////////////////////////////////////////					
			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File(DIR+ "/SessionsData.csv");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);

			dataSession.print("Time Awake" + ",");
			task = (PVT88hours) tasks[0];
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				dataSession.print(session.timeAwake + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("Lapses %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionLapses[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("FalseStarts %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionFalseAlerts[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("MedianRT" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveMedianAlertResponses[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("LSNR_apx" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveLSNRapx[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("SleepAttacks %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionSleepAtacks[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();


			// writing the Standard Deviations
			dataSession.print("\n");

			dataSession.print("Lapses (P) SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionLapses[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("FalseStarts % SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionFalseAlerts[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("MedianRT SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveMedianAlertResponses[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("LSNR_apx SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveLSNRapx[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("SleepAttacks % SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(AveProportionSleepAtacks[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("\n\n");
			dataSession.close();


		} catch (IOException e) {
			getModel().output(" ");
			getModel().output("!!!!!");
			getModel().outputError("The directory for output data does not exist: " + DIR);
		}


		return result;
	}

}
