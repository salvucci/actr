package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;

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
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	// the following two variables are for handling sleep attacks
	private int sleepAttackIndex = 0;

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
	int sessionNumber = 0;  // starts from 0
	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

//	private PrintStream data;
	
	class Session {
		double startTime =0;
		int falseStarts = 0;
		int alertRosponses = 0;
		int alertResponseSpread [] = new int[35]; // Alert responses (150-500ms, 10ms intervals )
		int lapses = 0;
		int sleepAttacks = 0;
		int stimulusIndex = 0;
		double totalSessionTime =0 ;
		int responses = 0 ; //number of responses, this can be diff from the stimulusIndex because of false resonces
		double responseTotalTime = 0;
	}

	public PVT88hours() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		lastTime = 0;

		currentSession = new Session();
		stimulusVisibility = false;

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
		getModel().getFatigue().startFatigueSession();

		addUpdate(1.0);

//		try {
//			File dataFile = new File("./test/fatigue/pvt_driver/data.txt");
//			if (!dataFile.exists())
//				dataFile.createNewFile();
//			data = new PrintStream(dataFile);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;

		if (currentSession.totalSessionTime <= 600.0) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			lastTime = getModel().getTime();
			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),
					Symbol.get("stimulus"));

			// calling percentage reset after any new task presentation (audio or visual)
			getModel().getFatigue().fatigueResetPercentages();

			// Handling the sleep attacks -- adding an event in 30 s to see if the current stimulus is still on
			currentSession.stimulusIndex ++;
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex==currentSession.stimulusIndex && stimulusVisibility == true ){
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						currentSession.sleepAttacks++;
						currentSession.responses++; // when sleep attack happens we add to the number of responses
//						System.out.println("Sleep attack at time ==>" + (getModel().getTime() - currentSession.startTime)
//								+ "model time :" + getModel().getTime());
//						System.out.println(currentSession.stimulusIndex + " " + sleepAttackIndex);
						addUpdate(1.0);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));
					}
					repaint();

				}
			});
		}

		// Starting a new Session
		else {
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length ){
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						sessions.add(currentSession);
						currentSession = new Session();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
						getModel().getFatigue().startFatigueSession();
//						System.out.println(getModel().getFatigue().computeBioMathValueForHour());
						addUpdate(1.0);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));
					}
				});
				
			}else{
				sessions.add(currentSession);
				getModel().stop();
			}


		}
	}

	@Override
	public void typeKey(char c) {

		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime ;

			if (response != null) // && response.equals("spc"))
			{
				currentSession.responses++;
				currentSession.responseTotalTime += responseTime;
			}

			label.setVisible(false);
			processDisplay();

			Random random = new Random();
			interStimulusInterval = random.nextDouble() * 8 + 1; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;

			if (responseTime < .150){
				currentSession.falseStarts++;
			}
			else if (responseTime > .150 && responseTime <= .500){
				currentSession.alertResponseSpread[(int) ((responseTime - .150) * 100)]++; // making the array for response time
				currentSession.alertRosponses++;
			}
			else if (responseTime > .500 && responseTime < 30.0){
				currentSession.lapses++;
			}
			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));

		} else {
			currentSession.responses++;
			currentSession.falseStarts++;
		}

	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		int numberOfSessions = timesOfPVT.length;
		Values[] totallLapsesValues = new Values[numberOfSessions];
		Values[] totallFalseAlerts = new Values[numberOfSessions]; 
		Values[] totallSleepAtacks = new Values[numberOfSessions];
		Values[] totallAlertResponces = new Values[numberOfSessions];
		Values[][] totallAlertResponcesSpread = new Values[numberOfSessions][35];
		Values[] totallResponsesNumber  =new Values[numberOfSessions];
		
		Values[] totallProportionLapses = new Values[numberOfSessions];
		Values[] totallProportionFalseAlerts = new Values[numberOfSessions]; 
		Values[] totallProportionSleepAtacks = new Values[numberOfSessions];
		Values[] totallProportionAlertRresponces = new Values[numberOfSessions];
		Values[][] totallProportionAlertResponcesSpread = new Values[numberOfSessions][35];

		// allocating memory to the vectors
		for (int i = 0; i < numberOfSessions; i++) {
			totallLapsesValues[i] = new Values();
			totallFalseAlerts[i] = new Values();
			totallSleepAtacks[i] = new Values();
			totallAlertResponces[i] = new Values();
			totallResponsesNumber[i] = new Values();
			totallProportionLapses[i] = new Values();
			totallProportionFalseAlerts[i] = new Values();
			totallProportionSleepAtacks[i] = new Values();
			totallProportionAlertRresponces[i] = new Values();
			for (int j = 0; j < 35; j++) {
				totallAlertResponcesSpread[i][j] = new Values();
				totallProportionAlertResponcesSpread[i][j] = new Values();
			}	
		}
		
		for (Task taskCast : tasks) {
			PVT88hours task = (PVT88hours) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				totallFalseAlerts[i].add(task.sessions.elementAt(i).falseStarts);
				totallLapsesValues[i].add(task.sessions.get(i).lapses);
				totallSleepAtacks[i].add(task.sessions.get(i).sleepAttacks);
				totallAlertResponces[i].add(task.sessions.get(i).alertRosponses);
				totallResponsesNumber[i].add(task.sessions.get(i).responses);
				for (int j = 0; j < 35; j++) {
					totallAlertResponcesSpread[i][j].add((double) task.sessions.get(i).alertResponseSpread[j]);
				}
				
				totallProportionFalseAlerts[i].add((double)task.sessions.get(i).falseStarts/task.sessions.get(i).responses);
				totallProportionLapses[i].add((double)task.sessions.get(i).lapses/task.sessions.get(i).responses);
				totallProportionSleepAtacks[i].add((double)task.sessions.get(i).sleepAttacks/task.sessions.get(i).responses);
				totallProportionAlertRresponces[i].add((double)task.sessions.get(i).alertRosponses/task.sessions.get(i).responses);
				for (int j = 0; j < 35; j++) {
					totallProportionAlertResponcesSpread[i][j].add(
							(double) task.sessions.get(i).alertResponseSpread[j]/task.sessions.get(i).responses);
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
				BaseAR[i].add(totallProportionAlertResponcesSpread[s][i].mean());
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
		
		// TO DO : Writing Numbers to the file 

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
						Day1AR[i].add(totallProportionAlertResponcesSpread[s][i].mean());
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
				
				// TO DO : Writing Numbers to the file 

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
						Day2AR[i].add(totallProportionAlertResponcesSpread[s][i].mean());
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
				
				// TO DO : Writing Numbers to the file 

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
						Day3AR[i].add(totallProportionAlertResponcesSpread[s][i].mean());
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
				
				// TO DO : Writing Numbers to the file 

				// ----------------------- END OF DAY 3 --------------------------

				
				
		
		
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
