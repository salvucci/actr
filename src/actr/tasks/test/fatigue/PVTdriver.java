package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
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
		int responses = 0;
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

		addUpdate(1.0);

		try {
			File day1file = new File("./test/fatigue/pvt_driver/day1.txt");
			if (!day1file.exists())
				day1file.createNewFile();
			day1 = new PrintStream(day1file);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// this was for when you want to reset the numbers at each session
		// getModel().getFatigue().resetFatigueModule();
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
		}

		// Starting a new Session
		else {

			System.out.println(currentSession.totalSessionTime);
			System.out.println("responses ==> " + currentSession.responses);
			

			getModel().stop();

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
				currentSession.alertResponse[(int) ((responseTime - .150) * 100)]++; // making
																						// the
																						// array
																						// for
																						// response
																						// time
			else if (responseTime > .500 && responseTime < 30.0)
				currentSession.lapses++;
			else if (responseTime >= 30.0)
				currentSession.sleepAttacks++;
			
			System.out.println("Sleep attack at time ==>" + getModel().getTime());

			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));

		} else {
			currentSession.responses++;
			currentSession.falseStarts++;

		}

	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		
		PVTdriver task = (PVTdriver) tasks[0];

		getModel().output("******* Proportion of Responses **********\n");
		getModel()
				.output("FS  "
						+ " ---------------------------    Alert Responses    --------------------------- "
						+ " Alert Responses "
						+ " ---------------------------    Alert Responses    ---------------------------- "
						+ "L    SA");
		double[] AlertResponsesProportion = new double[35];
		for (int i = 0; i < 35; i++)
			AlertResponsesProportion[i] = (double) task.currentSession.alertResponse[i] / task.currentSession.responses;
		
		getModel().output(
				String.format("%.2f", (double) task.currentSession.falseStarts / task.currentSession.responses) + " "
						+ Utilities.toString(AlertResponsesProportion) + " "
						+ String.format("%.2f", (double) task.currentSession.lapses / task.currentSession.responses) + " "
						+ String.format("%.2f", (double) task.currentSession.sleepAttacks / task.currentSession.responses));

		getModel().output("\nNumber of lapses in the session: "+ task.currentSession.lapses);
		
		
		getModel().output("\n*******************************************\n");

	
		Result result = new Result();
		return result;
	}

}
