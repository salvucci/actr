package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;

/**
 * Tutorial Unit 5: Siegler Task
 * 
 * @author Dario Salvucci
 */
public class U5Siegler extends Task {
	// --- Task Code ---//

	int tupleIndex = 0;
	double lastTime = 0;
	int[][] tuples = { { 1, 1 }, { 1, 2 }, { 1, 3 }, { 2, 2 }, { 2, 3 }, { 3, 3 } };
	String response;
	String responses[] = new String[tuples.length];

	@Override
	public void start() {
		tupleIndex = -1;
		response = null;
		lastTime = -100;
		addPeriodicUpdate(30.0);
	}

	@Override
	public void update(double time) {
		if (tupleIndex >= 0) {
			responses[tupleIndex] = response;
			response = "";
		}
		tupleIndex++;
		if (tupleIndex >= tuples.length)
			getModel().stop();
		else {
			addAural(0.00, "arg1", "sound", "" + tuples[tupleIndex][0]);
			addAural(0.75, "arg2", "sound", "" + tuples[tupleIndex][1]);
		}
		lastTime = time;
	}

	static final String numbers[] = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "other" };

	int getIndex(String s) {
		for (int i = 0; i < numbers.length; i++)
			if (numbers[i].equals(s))
				return i;
		return numbers.length - 1;
	}

	@Override
	public void speak(String s) {
		response = s;
	}

	// --- Analysis Code ---//

	static final double humanCounts[][] = { { 0, .05, .86, 0, .02, 0, .02, 0, 0, .06 },
			{ 0, .04, .07, .75, .04, 0, .02, 0, 0, .09 }, { 0, .02, 0, .10, .75, .05, .01, .03, 0, .06 },
			{ .02, 0, .04, .05, .80, .04, 0, .05, 0, 0 }, { 0, 0, .07, .09, .25, .45, .08, .01, .01, .06 },
			{ .04, 0, 0, .05, .21, .09, .48, 0, .02, .11 } };

	@Override
	public int analysisIterations() {
		return 100;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double modelCounts[][];
		modelCounts = new double[tuples.length][numbers.length];
		int totals[] = new int[tuples.length];
		for (int n = 0; n < tasks.length; n++) {
			U5Siegler task = (U5Siegler) tasks[n];
			for (int i = 0; i < task.responses.length; i++) {
				int index = getIndex(task.responses[i]);
				modelCounts[i][index] += 1;
				totals[i]++;
			}
		}
		for (int i = 0; i < tuples.length; i++)
			for (int j = 0; j < numbers.length; j++)
				if (totals[i] > 0)
					modelCounts[i][j] /= totals[i];
				else
					modelCounts[i][j] = 0;

		if (output) {
			double r = Statistics.correlation(modelCounts, humanCounts);
			getModel().output("\n=====\n");
			for (int i = 0; i < tuples.length; i++) {
				String s = "";
				for (int j = 0; j < numbers.length; j++)
					s += String.format("%.2f\t", modelCounts[i][j]);
				getModel().output(s);
			}
			getModel().output("R = " + String.format("%.2f", r));
		}

		return new Result("U5Siegler", modelCounts, humanCounts);
	}
}
