package actr.tasks.tutorial;

import java.util.Iterator;

import actr.model.Symbol;
import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskButton;
import actr.task.TaskLabel;
import actr.task.TaskLine;

/**
 * Tutorial Unit 6: Building Sticks Task
 * 
 * @author Dario Salvucci
 */
public class U6BST extends Task {
	final U6BST panel;
	final TaskButton buttonA;
	final TaskButton buttonB;
	final TaskButton buttonC;
	final TaskButton buttonReset;
	TaskLine lineA, lineB, lineC, lineTarget, line;
	final TaskLabel doneLabel;
	String choice = null;
	boolean done = false;
	final int[][] stimuli = { { 15, 250, 55, 125 }, { 10, 155, 22, 101 }, { 14, 200, 37, 112 }, { 22, 200, 32, 114 },
			{ 10, 243, 37, 159 }, { 22, 175, 40, 73 }, { 15, 250, 49, 137 }, { 10, 179, 32, 105 }, { 20, 213, 42, 104 },
			{ 14, 237, 51, 116 }, { 12, 149, 30, 72 }, { 14, 237, 51, 121 }, { 22, 200, 32, 114 }, { 14, 200, 37, 112 },
			{ 15, 250, 55, 125 } };
	int stimIndex = 0;
	final String[] responses = new String[stimuli.length];
	final double[] utilities = new double[4];

	public U6BST() {
		super();
		panel = this;
		setLayout(null);

		buttonA = new TaskButton("A", 10, 25, 20, 20) {
			@Override
			public void doClick() {
				if (choice == null)
					choice = "under";
				changeLine((line.getWidth() > lineTarget.getWidth()) ? -lineA.getWidth() : lineA.getWidth());
			}
		};
		add(buttonA);

		buttonB = new TaskButton("B", 10, 50, 20, 20) {
			@Override
			public void doClick() {
				if (choice == null)
					choice = "over";
				changeLine((line.getWidth() > lineTarget.getWidth()) ? -lineB.getWidth() : lineB.getWidth());
			}
		};
		add(buttonB);

		buttonC = new TaskButton("C", 10, 75, 20, 20) {
			@Override
			public void doClick() {
				if (choice == null)
					choice = "under";
				changeLine((line.getWidth() > lineTarget.getWidth()) ? -lineC.getWidth() : lineC.getWidth());
			}
		};
		add(buttonC);

		buttonReset = new TaskButton("Reset", 10, 125, 40, 20) {
			@Override
			public void doClick() {
				changeLine(-line.getWidth());
				panel.repaint();
			}
		};
		add(buttonReset);

		lineA = new TaskLine(50, 35, 10, 1);
		add(lineA);
		lineB = new TaskLine(50, 60, 40, 1);
		add(lineB);
		lineC = new TaskLine(50, 85, 20, 1);
		add(lineC);
		lineTarget = new TaskLine(50, 110, 0, 1);
		add(lineTarget);
		line = new TaskLine(50, 135, 0, 1);
		add(line);

		doneLabel = new TaskLabel("done", 180, 200, 35, 20);
		doneLabel.setVisible(false);
		add(doneLabel);
	}

	@Override
	public void start() {
		stimIndex = -1;
		addUpdate(0);
	}

	void changeLine(int delta) {
		line.changeWidth(delta);
		if (line.getWidth() == lineTarget.getWidth()) {
			done = true;
			doneLabel.setVisible(true);
			addUpdate(5.0);
		}
		processDisplay();
		panel.repaint();
	}

	@Override
	public void update(double time) {
		if (stimIndex >= 0)
			responses[stimIndex] = choice;
		stimIndex++;
		if (stimIndex < stimuli.length) {
			lineA.setWidth(stimuli[stimIndex][0]);
			lineB.setWidth(stimuli[stimIndex][1]);
			lineC.setWidth(stimuli[stimIndex][2]);
			lineTarget.setWidth(stimuli[stimIndex][3]);
			line.setWidth(0);
			choice = null;
			done = false;
			doneLabel.setVisible(false);
			panel.repaint();
			processDisplay();
		} else
			getModel().stop();

		String prefix = (getModel().procedural.get(Symbol.get("decide-over")).getUtility() == 0) ? "u6bst*" : "";
		utilities[0] = getModel().procedural.get(Symbol.get(prefix + "decide-over")).getUtility();
		utilities[1] = getModel().procedural.get(Symbol.get(prefix + "decide-under")).getUtility();
		utilities[2] = getModel().procedural.get(Symbol.get(prefix + "force-over")).getUtility();
		utilities[3] = getModel().procedural.get(Symbol.get(prefix + "force-under")).getUtility();
	}

	@Override
	public void eval(Iterator<String> it) {
		String s = "***** ( ";
		s += getModel().procedural.get(Symbol.get("decide-over")).getUtility() + " ";
		s += getModel().procedural.get(Symbol.get("decide-under")).getUtility() + " ";
		s += getModel().procedural.get(Symbol.get("force-over")).getUtility() + " ";
		s += getModel().procedural.get(Symbol.get("force-under")).getUtility() + " ";
		s += ")";
		getModel().output(s);
	}

	// --- Analysis Code ---//

	final double[] humanCounts = { 20.0, 67.0, 20.0, 47.0, 87.0, 20.0, 80.0, 93.0, 83.0, 13.0, 29.0, 27.0, 80.0, 73.0, 53.0 };

	@Override
	public int analysisIterations() {
		return 20;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		double[] modelCounts = new double[stimuli.length];
		for (Task value : tasks) {
			U6BST task = (U6BST) value;
			for (int i = 0; i < stimuli.length; i++)
				if (task.responses[i] != null)
					modelCounts[i] += (task.responses[i].equals("over")) ? 1 : 0;
			for (int i = 0; i < 4; i++)
				utilities[i] += task.utilities[i];
		}
		for (int i = 0; i < stimuli.length; i++)
			modelCounts[i] = 100.0 * (modelCounts[i] / tasks.length);
		for (int i = 0; i < 4; i++)
			utilities[i] /= tasks.length;

		if (output) {
			double r = Statistics.correlation(modelCounts, humanCounts);
			getModel().output("\n=====\n");
			String s1 = "", s2 = "";
			for (int i = 0; i < stimuli.length; i++) {
				s1 += (i + 1) + "\t";
				s2 += String.format("%.1f\t", modelCounts[i]);
			}
			getModel().output(s1 + "\n" + s2 + "\n");
			getModel().output("R = " + String.format("%.2f", r));
			String s = "decide-over";
			getModel().output(String.format("%-15s : %.3f", s, utilities[0]));
			s = "decide-under";
			getModel().output(String.format("%-15s : %.3f", s, utilities[1]));
			s = "force-over";
			getModel().output(String.format("%-15s : %.3f", s, utilities[2]));
			s = "force-under";
			getModel().output(String.format("%-15s : %.3f", s, utilities[3]));
		}

		return new Result("U6BST", modelCounts, humanCounts);
	}
}
