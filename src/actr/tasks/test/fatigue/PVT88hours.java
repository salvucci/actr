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
	private double PVTduration = 600.0;
	private double [] timesOfPVT = {	
			//			20  ,22  ,
			//			32  ,34  ,36  ,38  ,40  ,42 , 44  ,46  ,

			// 88 TSD :  44 sessions in total
			56  ,58  ,60  ,62  ,64  ,66 , 68  ,70  , //base line
			72  ,74  ,76  ,78  ,80  ,82  ,84  ,86  ,88  ,90  ,92 , 94  , //day 1
			96  ,98  ,100 ,102 ,104 ,106 ,108, 110 ,112 ,114 ,116 ,118 , //day 2
			120 ,122 ,124, 126 ,128 ,130 ,132 ,134 ,136 ,138 ,140, 142 , //day 3
			//End of 88 TSD
	};
	
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

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
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
						currentSession.sleepAttacks++;
						currentBlock.sleepAttacks++;
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
			currentSession.bioMathValue = getModel().getFatigue().getBioMathModelValueforHour(timesOfPVT[sessionNumber]);
			currentSession.timeAwake = getModel().getFatigue().getTimeAwake(timesOfPVT[sessionNumber]);
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new SessionPVT();
						currentBlock = currentSession.new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						currentBlock.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
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
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentBlock.blockReactionTimes.add(responseTime);
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentBlock.blockReactionTimes.add(1);
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
		
		DecimalFormat df = new DecimalFormat("#.00");		
		PVT88hours t = (PVT88hours) tasks[0]; // getting the numbers of sessions and blocks
		int numberOfSessions = t.sessions.size();

		Values[] totallProportionLapses = new Values[numberOfSessions];
		Values[] totallProportionFalseAlerts = new Values[numberOfSessions]; 
		Values[] totallProportionSleepAtacks = new Values[numberOfSessions];
		Values[] totallProportionAlertRresponces = new Values[numberOfSessions];
		Values[][] totallProportionAlertResponcesDis = new Values[numberOfSessions][35];
		Values[] totallMeanAlertRresponces = new Values[numberOfSessions];
		
		// allocating memory to the vectors
		for (int i = 0; i < numberOfSessions; i++) {
			totallProportionLapses[i] = new Values();
			totallProportionFalseAlerts[i] = new Values();
			totallProportionSleepAtacks[i] = new Values();
			totallProportionAlertRresponces[i] = new Values();
			totallMeanAlertRresponces[i] = new Values();
			for (int j = 0; j < 35; j++) {
				totallProportionAlertResponcesDis[i][j] = new Values();
			}	
		}

		for (Task taskCast : tasks) {
			PVT88hours task = (PVT88hours) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				totallProportionFalseAlerts[i].add(session.getProportionOfFalseAlert());
				totallProportionLapses[i].add(session.getProportionOfLapses());
				totallProportionSleepAtacks[i].add(session.getProportionOfSleepAttacks());
				totallProportionAlertRresponces[i].add(session.getProportionOfAlertResponses());
				totallMeanAlertRresponces[i].add(session.getMeanAlertReactionTimes());
				double [] proportionAlertDis = session.getProportionAlertResponseDistribution(); 
				for (int j = 0; j < 35; j++) {
					totallProportionAlertResponcesDis[i][j].add(proportionAlertDis[j]);
				}
			}
		}

		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");

		getModel().output("******* Proportion of Responses **********\n");
		getModel()
		.output("Day\tFS\t"
				+ " ---------------------------    Alert Responses    --------------------------- "
				+ " Alert Responses "
				+ " ---------------------------    Alert Responses    ----------------------------\t"
				+ "L\tSA");
		///////////////////////////////////////////////////////////////////////////////////////////////////			
		// -------------------   BASE LINE  ---------------------------
		Values BaseFA = new Values();
		Values[] BaseAR = new Values[35];
		Values BaseL = new Values();
		Values BaseSA = new Values();
		for (int j = 0; j < 35; j++) 
			BaseAR[j] = new Values();

		for (int s = 0; s < 8; s++) {
			for (int i = 0; i < 35; i++)
				BaseAR[i].add(totallProportionAlertResponcesDis[s][i].mean());
			BaseFA.add(totallProportionFalseAlerts[s].mean());
			BaseL.add(totallProportionLapses[s].mean());
			BaseSA.add(totallProportionSleepAtacks[s].mean());
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
		///////////////////////////////////////////////////////////////////////////////////////////////////			
		// ----------------------------   DAY 1  -----------------------------
		Values Day1FA = new Values();
		Values[] Day1AR = new Values[35];
		Values Day1L = new Values();
		Values Day1SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day1AR[j] = new Values();

		for (int s = 8; s < 20; s++) {
			for (int i = 0; i < 35; i++)
				Day1AR[i].add(totallProportionAlertResponcesDis[s][i].mean());
			Day1FA.add(totallProportionFalseAlerts[s].mean());
			Day1L.add(totallProportionLapses[s].mean());
			Day1SA.add(totallProportionSleepAtacks[s].mean());
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
		///////////////////////////////////////////////////////////////////////////////////////////////////				
		// ------------------------    DAY 2   ---------------------------
		Values Day2FA = new Values();
		Values[] Day2AR = new Values[35];
		Values Day2L = new Values();
		Values Day2SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day2AR[j] = new Values();

		for (int s = 20; s < 32; s++) {
			for (int i = 0; i < 35; i++)
				Day2AR[i].add(totallProportionAlertResponcesDis[s][i].mean());
			Day2FA.add(totallProportionFalseAlerts[s].mean());
			Day2L.add(totallProportionLapses[s].mean());
			Day2SA.add(totallProportionSleepAtacks[s].mean());
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
		///////////////////////////////////////////////////////////////////////////////////////////////////					
		// -----------------------    DAY 3   ---------------------------
		Values Day3FA = new Values();
		Values[] Day3AR = new Values[35];
		Values Day3L = new Values();
		Values Day3SA = new Values();
		for (int j = 0; j < 35; j++) 
			Day3AR[j] = new Values();

		for (int s = 32; s < 34; s++) {
			for (int i = 0; i < 35; i++)
				Day3AR[i].add(totallProportionAlertResponcesDis[s][i].mean());
			Day3FA.add(totallProportionFalseAlerts[s].mean());
			Day3L.add(totallProportionLapses[s].mean());
			Day3SA.add(totallProportionSleepAtacks[s].mean());
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
		
		
		try {
			// Writing Numbers to the file based on days
			File dataFile = new File("./test/fatigue/pvt_88hour/data.txt");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintStream data = new PrintStream(dataFile);
			
			// Base line
			data.print(BaseFA.mean()+ " ");
			for (int i = 0; i < AlertResponsesProportionBase.length; i++) 
				data.print(AlertResponsesProportionBase[i] + " ");
			data.print(BaseL.mean() + " ");
			data.print(BaseSA.mean());
			data.print("\n");
			data.flush();

			// Day1 line
			data.print(Day1FA.mean()+ " ");
			for (int i = 0; i < AlertResponsesProportionDay1.length; i++) 
				data.print(AlertResponsesProportionDay1[i] + " ");
			data.print(Day1L.mean() + " ");
			data.print(Day1SA.mean());
			data.print("\n");
			data.flush();
			
			// Day2 line
			data.print(Day2FA.mean()+ " ");
			for (int i = 0; i < AlertResponsesProportionDay2.length; i++) 
				data.print(AlertResponsesProportionDay2[i] + " ");
			data.print(Day2L.mean() + " ");
			data.print(Day2SA.mean());
			data.print("\n");
			data.flush();
			
			// Day3 line
			data.print(Day3FA.mean()+ " ");
			for (int i = 0; i < AlertResponsesProportionDay3.length; i++) 
				data.print(AlertResponsesProportionDay3[i] + " ");
			data.print(Day3L.mean() + " ");
			data.print(Day3SA.mean());
			data.print("\n");
			data.flush();
			
			data.close();
			
			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File("./test/fatigue/pvt_88hour/dataSessions.csv");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);
			
			dataSession.print("AwakeTime" + ",");
			PVT88hours task = (PVT88hours) tasks[0];
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				dataSession.print(session.timeAwake + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("FalseStarts" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totallProportionFalseAlerts[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("MeanAlertResponces" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totallMeanAlertRresponces[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("Lapses" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totallProportionLapses[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("SleepAttacks" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(totallProportionSleepAtacks[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}



		//		getModel().output("******* Average Proportion of Responses **********\n");
		//		getModel().output("#\tFS\t" + "AR\t " + "L\t"+ "SA");
		//		
		////		double[] AlertResponsesProportion = new double[35];
		//		for (int s = 0; s < numberOfSessions; s++) {
		////			for (int i = 0; i < 35; i++)
		////				AlertResponsesProportion[i] = totallProportionAlertResponcesSpread[s][i].mean();
		//
		//			getModel().output(s + "\t"+
		//					df2.format( totallProportionFalseAlerts[s].mean()) + "\t"
		//					//+ Utilities.toString(AlertResponsesProportion) + " "
		//					+ df2.format( totallProportionAlertRresponces[s].mean()) + "\t"
		//					+ df2.format(totallProportionLapses[s].mean()) + "\t"
		//					+ df2.format(totallProportionSleepAtacks[s].mean()));	
		//		}
		//		
		//		getModel().output("\nAverage Number of lapses in the time points \n" );
		//		// TO DO 
		//		
		//		getModel().output("\n*******************************************\n");

		Result result = new Result();
		return result;
	}

}
