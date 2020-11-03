package actr.tasks.tutorial;

import java.util.Vector;

import actr.task.Result;
import actr.task.Statistics;
import actr.task.Task;
import actr.task.TaskLabel;
import actr.task.Utilities;

/**
 * Tutorial Unit 3: Subitizing Task
 * 
 * @author Dario Salvucci
 */
public class U3Subitize extends Task {
	// --- Task Code ---//

	final int count;
	double rt;

	static final int[] counts = { 3, 6, 9, 2, 5, 8, 1, 4, 7, 10 };
	static int currentCount = -1;

	public U3Subitize() {
		super();

		currentCount++;
		if (currentCount >= counts.length)
			currentCount = 0;
		count = counts[currentCount];
	}

	static class Point {
		final int x;
		final int y;

		Point() {
			x = Utilities.random.nextInt(240) + 20;
			y = Utilities.random.nextInt(240) + 20;
		}

		boolean tooClose(Point p) {
			return Math.abs(x - p.x) < 40 && Math.abs(y - p.y) < 40;
		}

		boolean tooClose(Vector<Point> points) {
			for (int i = 0; i < points.size(); i++)
				if (tooClose(points.elementAt(i)))
					return true;
			return false;
		}
	}

	@Override
	public void start() {
		Vector<Point> points = new Vector<>();
		for (int i = 0; i < count; i++) {
			Point p = new Point();
			while (p.tooClose(points))
				p = new Point();
			points.add(p);
			TaskLabel label = new TaskLabel("x", p.x, p.y, 20, 10);
			add(label);
		}
		processDisplay();
	}

	@Override
	public void typeKey(char c) {
		rt = getModel().getTime();
	}

	@Override
	public void speak(String s) {
		rt = getModel().getTime();
	}

	// --- Analysis Code ---//

	final double[] humanRT = { .60, .65, .70, .86, 1.12, 1.50, 1.79, 2.13, 2.15, 2.58 };

	@Override
	public int analysisIterations() {
		return 10;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		try {
			double[] modelRT = new double[counts.length];
			int[] totals = new int[counts.length];
			for (Task value : tasks) {
				U3Subitize task = (U3Subitize) value;
				modelRT[task.count - 1] += task.rt;
				totals[task.count - 1]++;
			}
			for (int i = 0; i < modelRT.length; i++)
				if (totals[i] > 0)
					modelRT[i] /= totals[i];

			if (output) {
				double r = Statistics.correlation(modelRT, humanRT);
				double rmse = Statistics.rmse(modelRT, humanRT);
				getModel().output("\n=====\n");
				getModel().output("Human:\t" + Utilities.toString(humanRT));
				getModel().output("Model:\t" + Utilities.toString(modelRT));
				getModel().output("\nR = " + String.format("%.2f", r));
				getModel().output("RMSE = " + String.format("%.2f", rmse));
			}

			return new Result("U3Subitize", modelRT, humanRT);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
