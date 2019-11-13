package actr.tasks.tutorial;

import java.util.Vector;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;
import actr.task.Utilities;

/**
 * Tutorial Unit 4: Paired Associates Task
 * 
 * @author Dario Salvucci
 */
public class U7Paired extends Task {
	// --- Task Code ---//

	final TaskLabel label;
	double lastTime = 0;
	int pairIndex = 0, pairItem = 0;
	final String[][] pairs = { { "bank", "0" }, { "card", "1" }, { "dart", "2" }, { "face", "3" }, { "game", "4" },
			{ "hand", "5" }, { "jack", "6" }, { "king", "7" }, { "lamb", "8" }, { "mask", "9" }, { "neck", "0" },
			{ "pipe", "1" }, { "quip", "2" }, { "rope", "3" }, { "sock", "4" }, { "tent", "5" }, { "vent", "6" },
			{ "wall", "7" }, { "xray", "8" }, { "zinc", "9" } };
	int iteration = 0;
	static final int runIterations = 8;
	String response = null;
	double responseTime = 0;
	Trial currentTrial;
	final Vector<Trial> trials = new Vector<>();

	static class Trial {
		int responses = 0;
		int responsesCorrect = 0;
		double responseTotalTime = 0;
	}

	public U7Paired() {
		super();
		label = new TaskLabel("-", 150, 150, 40, 20);
		add(label);
	}

	@Override
	public void start() {
		iteration = 0;
		lastTime = -10;
		pairIndex = 0;
		pairItem = 0;
		response = null;
		responseTime = 0;
		currentTrial = new Trial();
		Utilities.shuffle(pairs);
		addPeriodicUpdate(5.0);
	}

	@Override
	public void update(double time) {
		String lastCorrect = label.getText();
		String item = pairs[pairIndex][pairItem];
		label.setText(item);
		processDisplay();
		pairItem++;
		if (pairItem == 1) {
			currentTrial.responses++;
			if (response != null && response.equals(lastCorrect)) {
				currentTrial.responsesCorrect++;
				currentTrial.responseTotalTime += responseTime;
			}
			response = null;
		}
		if (pairItem >= 2) {
			pairIndex++;
			pairItem = 0;
		}
		if (pairIndex >= pairs.length) {
			trials.add(currentTrial);
			iteration++;
			if (iteration >= runIterations)
				getModel().stop();
			currentTrial = new Trial();
			pairIndex = 0;
			pairItem = 0;
		}
		lastTime = time;
	}

	@Override
	public void typeKey(char c) {
		response = String.valueOf(c);
		responseTime = getModel().getTime() - lastTime;
	}

	// --- Analysis Code ---//

	final double[] humanTimes = { 0.0, 2.158, 1.967, 1.762, 1.680, 1.552, 1.467, 1.402 };
	final double[] humanCorrect = { 0.000, .526, .667, .798, .887, .924, .958, .954 };

	@Override
	public int analysisIterations() {
		return 10;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double[] modelTimes = new double[runIterations];
		double[] modelCorrect = new double[runIterations];
		for (int i = 0; i < runIterations; i++) {
			double responses = 0, responsesCorrect = 0, responseTime = 0;
			for (Task value : tasks) {
				U7Paired task = (U7Paired) value;
				responses += task.trials.elementAt(i).responses;
				responsesCorrect += task.trials.elementAt(i).responsesCorrect;
				responseTime += task.trials.elementAt(i).responseTotalTime;
			}
			modelTimes[i] = (responsesCorrect == 0) ? 0 : (responseTime / responsesCorrect);
			modelCorrect[i] = (responses == 0) ? 0 : (1.0 * responsesCorrect / responses);
		}

		if (output) {
			getModel().output("\n=====\n");
			getModel().output(
					"Gazes [R = " + String.format("%.2f", Statistics.correlation(modelTimes, humanTimes)) + "]:");
			getModel().output("Human:\t" + Utilities.toString(humanTimes));
			getModel().output("Model:\t" + Utilities.toString(modelTimes));
			getModel().output("\nGaze Durations [R = "
					+ String.format("%.2f", Statistics.correlation(modelCorrect, humanCorrect)) + "]:");
			getModel().output("Human:\t" + Utilities.toString(humanCorrect));
			getModel().output("Model:\t" + Utilities.toString(modelCorrect));
		}

		Result result = new Result();
		result.add("U7Paired RT", modelTimes, humanTimes);
		result.add("U7Paired Corr", modelCorrect, humanCorrect);
		return result;
	}

}
