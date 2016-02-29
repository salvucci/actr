package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;

import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;



/**
 * Model of PVT test and Fatigue mechanism
 * 
 * Paper: Efficient driver drowsiness detection at moderate levels of drowsiness
 * 
 * Pia M. Forsmana, Bryan J. Vilaa,b, Robert A. Short c, Christopher G. Mott d, Hans P.A. Van Dongena,
 * 
 * @author Ehsan Khosroshahi
 */

public class PVTdriver extends Task {
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
			//	
			45.0, 48.0, 51.0, 54.0, //day1 
			69.0, 72.0, 75.0, 78.0, //day2
			93.0, 96.0, 99.0, 102.0,//day3
			117.0,120.0,123.0,126.0,//day4
			141.0,144.0,147.0,150.0 //day5
	};
	int sessionNumber = 0;  // starts from 0

	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

	private PrintStream day1;
	private PrintStream day2;
	private PrintStream day3;
	private PrintStream day4;
	private PrintStream b5Stream;
	private PrintStream b6Stream;
	private PrintStream b7Stream;
	private PrintStream uutStream;



	class Session {
		double startTime =0;
		int falseStarts = 0;
		int alertResponse[] = new int[35]; // Alert responses (150-500ms, 10ms
		// intervals )

		double totalSessionTime =0 ;
		int lapses = 0;
		int sleepAttacks = 0;
		int stimulusIndex = 0;
		int responses = 0 ; //number of responses, this can be diff from the stimulusIndex because of false resonces
		double responseTotalTime = 0;
	}

	public PVTdriver() {
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

		try {
			File day1file = new File("./test/fatigue/pvt_driver/day1.txt");
			if (!day1file.exists())
				day1file.createNewFile();
			day1 = new PrintStream(day1file);

		} catch (IOException e) {
			e.printStackTrace();
		}

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
						System.out.println("Sleep attack at time ==>" + currentSession.totalSessionTime);
						addUpdate(1.0);
						
						// setting up the state to wait
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));
					}
					repaint();

				}
			});
		}

		// Starting a new Session
		else {
			//			System.out.println("session # : " + sessionNumber);
			//			System.out.println(currentSession.totalSessionTime);
			//			System.out.println("responses ==> " + currentSession.responses);
			sessionNumber++;
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length ){
				sessions.add(currentSession);
				currentSession = new Session();
				stimulusVisibility = false;
				sleepAttackIndex = 0;
				currentSession.startTime = getModel().getTime();
				getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
				getModel().getFatigue().startFatigueSession();
				addUpdate(1.0);

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

			if (responseTime < .150)
				currentSession.falseStarts++;
			else if (responseTime > .150 && responseTime <= .500)
				currentSession.alertResponse[(int) ((responseTime - .150) * 100)]++; // making the array for response time
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
		Values[][] totallAlertResponces = new Values[35][numberOfSessions];
		Values[] totallResponsesNumber  =new Values[numberOfSessions];
		
		Values[] totallProportionLapsesValues = new Values[numberOfSessions];
		Values[] totallProportionFalseAlerts = new Values[numberOfSessions]; 
		Values[] totallProportionSleepAtacks = new Values[numberOfSessions];
		Values[][] totallProportionAlertResponces = new Values[35][numberOfSessions];
		
		
		for (Task taskCast : tasks) {
			PVTdriver task = (PVTdriver) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				totallFalseAlerts[i].add(task.sessions.get(i).falseStarts);
				totallLapsesValues[i].add(task.sessions.get(i).lapses);
				totallSleepAtacks[i].add(task.sessions.get(i).sleepAttacks);
				totallResponsesNumber[i].add(task.sessions.get(i).responses);
				for (int j = 0; j < 35; j++) {
					totallAlertResponces[i][j].add((double) task.sessions.get(i).alertResponse[j]);
				}
				
				totallProportionFalseAlerts[i].add((double)task.sessions.get(i).falseStarts/task.sessions.get(i).responses);
				totallProportionLapsesValues[i].add((double)task.sessions.get(i).lapses/task.sessions.get(i).responses);
				totallProportionSleepAtacks[i].add((double)task.sessions.get(i).sleepAttacks/task.sessions.get(i).responses);
				for (int j = 0; j < 35; j++) {
					totallProportionAlertResponces[i][j].add((double) task.sessions.get(i).alertResponse[j]/task.sessions.get(i).responses);
				}
			}
		}
		
		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");
		
		getModel().output("******* Proportion of Responses **********\n");
		getModel()
		.output("#\tFS  "
				+ " ---------------------------    Alert Responses    --------------------------- "
				+ " Alert Responses "
				+ " ---------------------------    Alert Responses    ---------------------------- "
				+ "L    SA");
		
		double[] AlertResponsesProportion = new double[35];
		for (int s = 0; s < numberOfSessions; s++) {
			for (int i = 0; i < 35; i++)
				AlertResponsesProportion[i] = totallProportionAlertResponces[s][i].mean();

			getModel().output(s + "\t"+
					df2.format( totallProportionFalseAlerts[s].mean()) + " "
					+ Utilities.toString(AlertResponsesProportion) + " "
					+ df2.format(totallProportionLapsesValues[s].mean()) + " "
					+ df2.format(totallProportionSleepAtacks[s].mean()));	
		}
		
		getModel().output("\nAverage Number of lapses in sessions from 1 to 20 " );
		
		double[] averageLapsesInSessions = new double[numberOfSessions];
		for (int i = 0; i < numberOfSessions; i++) {
			averageLapsesInSessions[i] = totallLapsesValues[i].mean();
		}
		getModel().output( Utilities.toString(averageLapsesInSessions) );
		
		
		getModel().output("\n*******************************************\n");

		Result result = new Result();
		return result;
	}

}
