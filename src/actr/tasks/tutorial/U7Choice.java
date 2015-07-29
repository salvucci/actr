package actr.tasks.tutorial;

import java.util.Random;
import java.util.Vector;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;
import actr.task.Utilities;

/**
 * Tutorial Unit 7: Schumacher et al. (2001) Experiment 3
 * 
 * @author Dario Salvucci
 */
public class U7Choice extends Task {
	// Task Code

	final double trialSpacing = 5.0;
	final int trialsPerSession = 12;
	final int totalSessions = 4;

	TaskLabel label;
	int sessionNumber = 1;
	int trialNumber = 0;
	double startTime;

	Trial currentTrial;
	Vector<Trial> trials = new Vector<Trial>();

	class Trial {
		int session;
		double responseTime;
	}

	public U7Choice() {
		super();
		label = new TaskLabel("O--", 100, 100, 50, 30);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		addPeriodicUpdate(trialSpacing);
	}

	Random random = new Random();

	String randomVisualStimulus() {
		int i = random.nextInt(3);
		if (i == 0)
			return "O--";
		else if (i == 1)
			return "-O-";
		else
			return "--O";
	}

	@Override
	public void update(double time) {
		if (currentTrial != null)
			trials.add(currentTrial);

		trialNumber++;
		if (trialNumber > trialsPerSession) {
			sessionNumber++;
			if (sessionNumber > totalSessions) {
				getModel().stop();
				return;
			}
			trialNumber = 1;
		}

		label.setText(randomVisualStimulus());
		label.setVisible(true);
		processDisplay();

		startTime = time;

		currentTrial = new Trial();
		currentTrial.session = sessionNumber;
	}

	@Override
	public void typeKey(char c) {
		currentTrial.responseTime = getModel().getTime() - startTime;
		label.setVisible(false);
		processDisplay();
	}

	// Analysis Code

	double humanRT[] = { .62, .56, .49, .46 };

	@Override
	public int analysisIterations() {
		// during initial testing, return 1 for only a single long simulation
		// for final runs, return 10 to collect data from 10 simulated
		// experiment participants
		return 1;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double modelRT[] = new double[humanRT.length];
		double modelCount[] = new double[humanRT.length];

		for (int i = 0; i < tasks.length; i++) {
			U7Choice task = (U7Choice) (tasks[i]);
			for (int j = 0; j < task.trials.size(); j++) {
				Trial trial = task.trials.elementAt(j);
				modelRT[trial.session - 1] += trial.responseTime;
				modelCount[trial.session - 1] += 1;
			}
		}

		for (int i = 0; i < modelRT.length; i++)
			modelRT[i] /= modelCount[i];

		Result result = new Result("DualChoice RT", modelRT, humanRT);

		if (output) {
			getModel().output("=====\n");

			getModel().output("Visual-Manual Choice");
			getModel().output("Human:\t" + Utilities.toString(humanRT));
			getModel().output("Model:\t" + Utilities.toString(modelRT));

			getModel().output(String.format("\nR = %.2f", Statistics.correlation(modelRT, humanRT)));
			getModel().output(String.format("\nRMSE = %.2f", Statistics.rmse(modelRT, humanRT)));
		}

		return result;
	}
}
