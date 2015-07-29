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
 * Dual Choice PRP: Schumacher et al. (2001) Experiment 1
 * 
 * @author Dario Salvucci
 */
public class DualChoicePTS extends Task {
	// Task Code
	final double trialSpacing = 10.0;
	final int totalTrials = 24;

	TaskLabel label;
	int numTrials = -1;
	double auralVocalStartTime, visualManualStartTime;
	Trial currentTrial;
	Vector<Trial> trials = new Vector<Trial>();

	TrialType currentTrialType = TrialType.AURAL_VOCAL;

	class Trial {
		double visualManualResponseTime;
		double auralVocalResponseTime;
		TrialType type;
	}

	enum TrialType {
		AURAL_VOCAL, VISUAL_MANUAL, DUAL
	}

	public DualChoicePTS() {
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
		numTrials++;

		if (numTrials > totalTrials)
			getModel().stop();
		else {
			if (currentTrialType.equals(TrialType.AURAL_VOCAL)) {
				addAural("tone", "tone", randomAuralStimulus());

				auralVocalStartTime = time;

				currentTrial = new Trial();
				currentTrial.type = TrialType.AURAL_VOCAL;

				currentTrialType = TrialType.VISUAL_MANUAL;
			} else if (currentTrialType.equals(TrialType.VISUAL_MANUAL)) {
				addEvent(new Event(time, "task", "update") {
					@Override
					public void action() {
						label.setText(randomVisualStimulus());
						label.setVisible(true);
						processDisplay();
					}
				});

				visualManualStartTime = time;

				currentTrial = new Trial();
				currentTrial.type = TrialType.VISUAL_MANUAL;

				currentTrialType = TrialType.DUAL;
			} else if (currentTrialType.equals(TrialType.DUAL)) {

				addAural("tone", "tone", randomAuralStimulus());

				addEvent(new Event(time, "task", "update") {
					@Override
					public void action() {
						label.setText(randomVisualStimulus());
						label.setVisible(true);
						processDisplay();
					}
				});

				auralVocalStartTime = time;
				visualManualStartTime = time;

				currentTrial = new Trial();
				currentTrial.type = TrialType.DUAL;

				currentTrialType = TrialType.AURAL_VOCAL;
			}
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

	double humanRT[][] = { { .446, .456 }, { .281, .283 } };

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double modelRT[][] = new double[2][2];
		double modelCount[] = new double[3];

		for (int i = 0; i < tasks.length; i++) {
			DualChoicePTS task = (DualChoicePTS) (tasks[i]);
			for (int j = 0; j < task.trials.size(); j++) {
				Trial trial = task.trials.elementAt(j);
				if (trial.type.equals(TrialType.AURAL_VOCAL)) {
					modelRT[0][0] += trial.auralVocalResponseTime;
					modelCount[0] += 1;
				} else if (trial.type.equals(TrialType.VISUAL_MANUAL)) {
					modelRT[1][0] += trial.visualManualResponseTime;
					modelCount[1] += 1;
				} else if (trial.type.equals(TrialType.DUAL)) {
					modelRT[0][1] += trial.auralVocalResponseTime;
					modelRT[1][1] += trial.visualManualResponseTime;
					modelCount[2] += 1;
				}
			}
		}

		// AV
		modelRT[0][0] = modelRT[0][0] / modelCount[0];

		// VM
		modelRT[1][0] = modelRT[1][0] / modelCount[1];

		// Dual
		modelRT[0][1] = modelRT[0][1] / modelCount[2];
		modelRT[1][1] = modelRT[1][1] / modelCount[2];

		Result result = new Result("DualChoice RT", modelRT, humanRT);

		if (output) {
			getModel().output("=====\n");

			getModel().output("Task #1: Aural-Vocal, Single- vs. Dual-Task");
			getModel().output("Human:\t" + Utilities.toString(humanRT[0]));
			getModel().output("Model:\t" + Utilities.toString(modelRT[0]));

			getModel().output("\nTask #2: Visual-Manual, Single- vs. Dual-Task");
			getModel().output("Human:\t" + Utilities.toString(humanRT[1]));
			getModel().output("Model:\t" + Utilities.toString(modelRT[1]));

			getModel().output(String.format("\nR = %.2f", Statistics.correlation(modelRT, humanRT)));
			getModel().output(String.format("\nRMSE = %.2f", Statistics.rmse(modelRT, humanRT)));
		}

		return result;
	}
}
