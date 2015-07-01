package actr.tasks.tutorial;

import java.util.Vector;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;
import actr.task.Utilities;

/**
 * Tutorial Unit 4: Alpha-Arithmetic Task
 * 
 * @author Dario Salvucci
 */
public class U4AlphaArithmetic extends Task {
	// Task Code

	private final String trialTuples[][] = { { "A", "2", "C", "K" },
			{ "D", "2", "F", "K" }, { "B", "3", "E", "K" },
			{ "E", "3", "H", "K" }, { "C", "4", "G", "K" },
			{ "F", "4", "J", "K" }, { "A", "2", "D", "D" },
			{ "D", "2", "G", "D" }, { "B", "3", "F", "D" },
			{ "E", "3", "I", "D" }, { "C", "4", "H", "D" },
			{ "F", "4", "K", "D" }, { "A", "2", "C", "K" },
			{ "D", "2", "F", "K" }, { "B", "3", "E", "K" },
			{ "E", "3", "H", "K" }, { "C", "4", "G", "K" },
			{ "F", "4", "J", "K" }, { "A", "2", "D", "D" },
			{ "D", "2", "G", "D" }, { "B", "3", "F", "D" },
			{ "E", "3", "I", "D" }, { "C", "4", "H", "D" },
			{ "F", "4", "K", "D" } };

	private TaskLabel addend1Label, addend2Label, sumLabel;

	private final double trialSpacing = 5.0;

	private int totalBlocks = 3;
	private int totalTrialsPerBlock = 8;

	private int currentBlock = 1;
	private int currentTrialInBlock = 0;
	private int currentTuple = 0;
	private double currentTrialStartTime = 0;

	private Trial currentTrial;

	Vector<Trial> trials = new Vector<Trial>();

	class Trial {
		int block;
		String addend1, addend2, sum, answer;
		double responseTime;
	}

	public U4AlphaArithmetic() {
		super();

		add(addend1Label = new TaskLabel("A", 100, 150, 25, 30));
		add(new TaskLabel("+", 125, 150, 25, 30));
		add(addend2Label = new TaskLabel("1", 150, 150, 25, 30));
		add(new TaskLabel("=", 175, 150, 25, 30));
		add(sumLabel = new TaskLabel("B", 200, 150, 25, 30));

		Utilities.shuffle(trialTuples);

		currentTrial = null;
	}

	@Override
	public void start() {
		addPeriodicUpdate(trialSpacing);
	}

	@Override
	public void update(double time) {
		if (currentTrial != null)
			trials.add(currentTrial);

		currentTrialInBlock++;
		if (currentTrialInBlock > totalTrialsPerBlock) {
			currentBlock++;
			if (currentBlock > totalBlocks) {
				getModel().stop();
				return;
			}
			currentTrialInBlock = 1;
		}

		currentTrial = new Trial();
		String[] tuple = trialTuples[currentTuple];
		currentTuple = (currentTuple + 1) % trialTuples.length;

		currentTrial.block = currentBlock;
		currentTrial.addend1 = tuple[0];
		currentTrial.addend2 = tuple[1];
		currentTrial.sum = tuple[2];
		currentTrial.answer = tuple[3];

		addend1Label.setText(currentTrial.addend1);
		addend2Label.setText(currentTrial.addend2);
		sumLabel.setText(currentTrial.sum);
		processDisplay();

		currentTrialStartTime = getModel().getTime();
	}

	@Override
	public void typeKey(char c) {
		currentTrial.responseTime = getModel().getTime()
				- currentTrialStartTime;
	}

	// Analysis Code

	private double humanRT[][] = { { 1.84, 2.46, 2.82 }, { 1.21, 1.45, 1.42 },
			{ 1.14, 1.21, 1.17 } };

	@Override
	public int analysisIterations() {
		return 100;
	}

	private void printTable(double results[][]) {
		getModel().output("\tTwo\tThree\tFour");
		for (int b = 0; b < 3; b++) {
			String s = "Block " + (b + 1);
			for (int j = 0; j < 3; j++)
				s += "\t" + String.format("%.3f", results[b][j]);
			getModel().output(s);
		}
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double modelRT[][] = new double[3][3];
		double modelCount[][] = new double[3][3];

		for (int i = 0; i < tasks.length; i++) {
			U4AlphaArithmetic task = (U4AlphaArithmetic) (tasks[i]);
			for (int j = 0; j < task.trials.size(); j++) {
				Trial trial = task.trials.elementAt(j);

				int blockIndex = trial.block - 1;
				int sizeIndex = (trial.addend2.equals("2") ? 0 : (trial.addend2
						.equals("3") ? 1 : 2));

				modelRT[blockIndex][sizeIndex] += trial.responseTime;
				modelCount[blockIndex][sizeIndex] += 1;
			}
		}

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				modelRT[i][j] /= modelCount[i][j];

		Result result = new Result("AlphaArithmetic RT", modelRT, humanRT);

		if (output) {
			getModel().output("=====");

			getModel().output("\nHuman");
			printTable(humanRT);

			getModel().output("\nModel");
			printTable(modelRT);

			getModel().output(
					String.format("\nR = %.2f",
							Statistics.correlation(modelRT, humanRT)));
			getModel().output(
					String.format("\nRMSE = %.2f",
							Statistics.rmse(modelRT, humanRT)));
		}

		return result;
	}
}
