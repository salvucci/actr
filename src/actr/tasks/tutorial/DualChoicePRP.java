package actr.tasks.tutorial;

import java.util.Random;
import java.util.Vector;

import actr.model.Event;
import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;
import actr.task.Utilities;

/**
 * Dual Choice PRP: Schumacher et al. (2001) Experiment 2
 * 
 * @author Dario Salvucci
 */
public class DualChoicePRP extends Task {
	// Task Code

	final double soas[] = { .050, .150, .250, .500, 1.000 };
	final double trialSpacing = 10.0;
	final int totalTrials = 24;

	TaskLabel label;
	int numTrials = -1;
	int currentSOAIndex = -1;
	double auralVocalStartTime, visualManualStartTime;
	Trial currentTrial;
	Vector<Trial> trials = new Vector<Trial>();

	class Trial {
		int soaIndex;
		double visualManualResponseTime;
		double auralVocalResponseTime;
	}

	public DualChoicePRP() {
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

	String randomAuralStimulus() {
		int i = random.nextInt(3);
		if (i == 0)
			return "low";
		else if (i == 1)
			return "mid";
		else
			return "high";
	}

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

		currentSOAIndex = (currentSOAIndex + 1) % soas.length;
		numTrials++;

		if (numTrials > totalTrials)
			getModel().stop();
		else {
			double soa = soas[currentSOAIndex];

			addAural("tone", "tone", randomAuralStimulus());

			addEvent(new Event(time + soa, "task", "update") {
				@Override
				public void action() {
					label.setText(randomVisualStimulus());
					label.setVisible(true);
					processDisplay();
				}
			});

			auralVocalStartTime = time;
			visualManualStartTime = time + soa;

			currentTrial = new Trial();
			currentTrial.soaIndex = currentSOAIndex;
		}
	}

	@Override
	public void typeKey(char c) {
		currentTrial.visualManualResponseTime = getModel().getTime() - visualManualStartTime;
		label.setVisible(false);
	}

	@Override
	public void speak(String s) {
		currentTrial.auralVocalResponseTime = getModel().getTime() - auralVocalStartTime;
	}

	// Analysis Code

	double humanRT[][] = { { .410, .415, .420, .415, .415 }, { .475, .395, .350, .300, .295 } };

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double modelRT[][] = new double[2][soas.length];
		double modelCount[] = new double[soas.length];

		for (int i = 0; i < tasks.length; i++) {
			DualChoicePRP task = (DualChoicePRP) (tasks[i]);
			for (int j = 0; j < task.trials.size(); j++) {
				Trial trial = task.trials.elementAt(j);
				modelRT[0][trial.soaIndex] += trial.auralVocalResponseTime;
				modelRT[1][trial.soaIndex] += trial.visualManualResponseTime;
				modelCount[trial.soaIndex] += 1;
			}
		}

		for (int i = 0; i < 2; i++)
			for (int j = 0; j < soas.length; j++)
				modelRT[i][j] /= modelCount[j];

		Result result = new Result("DualChoice RT", modelRT, humanRT);

		if (output) {
			getModel().output("=====\n");

			getModel().output("Task #1: Aural-Vocal");
			getModel().output("Human:\t" + Utilities.toString(humanRT[0]));
			getModel().output("Model:\t" + Utilities.toString(modelRT[0]));

			getModel().output("\nTask #2: Visual-Manual");
			getModel().output("Human:\t" + Utilities.toString(humanRT[1]));
			getModel().output("Model:\t" + Utilities.toString(modelRT[1]));

			getModel().output(String.format("\nR = %.2f", Statistics.correlation(modelRT, humanRT)));
			getModel().output(String.format("\nRMSE = %.2f", Statistics.rmse(modelRT, humanRT)));
		}

		return result;
	}
}
