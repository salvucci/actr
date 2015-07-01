package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;

/**
 * Tutorial Unit 5: Fan Task
 * 
 * @author Dario Salvucci
 */
public class U5Fan extends Task {
	// --- Task Code ---//

	TaskLabel personLabel, locationLabel;
	boolean lastCorrect = false;
	double lastTime = 0;
	int tupleIndex = 0;
	String response = null;
	double responseTime = 0;
	String[][] tuples = { { "lawyer", "store", "t" },
			{ "captain", "cave", "t" }, { "hippie", "church", "t" },
			{ "debutante", "bank", "t" }, { "earl", "castle", "t" },
			{ "hippie", "bank", "t" }, { "fireman", "park", "t" },
			{ "captain", "park", "t" }, { "hippie", "park", "t" },
			{ "fireman", "store", "nil" }, { "captain", "store", "nil" },
			{ "giant", "store", "nil" }, { "fireman", "bank", "nil" },
			{ "captain", "bank", "nil" }, { "giant", "bank", "nil" },
			{ "lawyer", "park", "nil" }, { "earl", "park", "nil" },
			{ "giant", "park", "nil" } };
	boolean correct[] = new boolean[tuples.length];
	double rts[] = new double[tuples.length];

	public U5Fan() {
		super();
		setLayout(null);

		String words[] = { "The", "person", "is", "in", "the", "location" };
		int x = 15; // 25;
		for (int i = 0; i < words.length; i++) {
			TaskLabel label = new TaskLabel(words[i], x, 150, 75, 20);
			add(label);
			if (words[i].equals("person"))
				personLabel = label;
			else if (words[i].equals("location"))
				locationLabel = label;
			x += 75;
		}
	}

	static boolean lastRunUsedPerson = false;

	@Override
	public void start() {
		if (lastRunUsedPerson)
			getModel().runCommand("(spp retrieve-from-location :u 10)");
		else
			getModel().runCommand("(spp retrieve-from-person :u 10)");
		lastRunUsedPerson = !lastRunUsedPerson;

		lastTime = -100;
		tupleIndex = -1;
		response = null;
		responseTime = 0;
		addPeriodicUpdate(30.0);
	}

	@Override
	public void update(double time) {
		if (tupleIndex >= 0) {
			if (response == null)
				correct[tupleIndex] = false;
			else {
				correct[tupleIndex] = (tuples[tupleIndex][2].equals("t")) ? response
						.equals("k") : response.equals("d");
				rts[tupleIndex] = responseTime;
			}
			response = null;
			responseTime = 0;
		}
		tupleIndex++;
		if (tupleIndex >= tuples.length)
			getModel().stop();
		else {
			personLabel.setText(tuples[tupleIndex][0]);
			locationLabel.setText(tuples[tupleIndex][1]);
			processDisplay();
			lastTime = time;
		}
	}

	@Override
	public void typeKey(char c) {
		response = c + "";
		responseTime = getModel().getTime() - lastTime;
	}

	// --- Analysis Code ---//

	final double humanTimes[] = { 1.11, 1.17, 1.22, 1.17, 1.20, 1.22, 1.15,
			1.23, 1.36, 1.20, 1.22, 1.26, 1.25, 1.36, 1.29, 1.26, 1.47, 1.47 };

	@Override
	public int analysisIterations() {
		return 20;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double[] modelTimes = new double[tuples.length];
		double counts[] = new double[tuples.length];
		for (int n = 0; n < tasks.length; n++) {
			U5Fan task = (U5Fan) tasks[n];
			for (int i = 0; i < task.rts.length; i++)
				if (task.correct[i]) {
					modelTimes[i] += task.rts[i];
					counts[i] += 1;
				}
		}
		for (int i = 0; i < tuples.length; i++)
			modelTimes[i] = (counts[i] == 0) ? 0 : (modelTimes[i] / counts[i]);

		if (output) {
			double r = Statistics.correlation(modelTimes, humanTimes);
			getModel().output("\n=====\n");
			for (int i = 0; i < tuples.length; i += 3)
				getModel().output(
						String.format("%.3f\t%.3f\t%.3f", modelTimes[i],
								modelTimes[i + 1], modelTimes[i + 2]));
			getModel().output("R = " + r);
		}

		return new Result("U5Fan", modelTimes, humanTimes);
	}
}
