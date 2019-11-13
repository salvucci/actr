package actr.task;

import java.util.Vector;

public class Result {
	private Vector<Measure> measures = new Vector<>();

	public static final boolean includeRMSE = false;

	private static class Measure {
		final String name;
		final double r;
		final double error;
		double rmse;
		final int points;
		String resultString;

		Measure(String name, double r, double error, int points) {
			this.name = name;
			this.r = r;
			this.error = error;
			this.points = points;
		}

		public Measure(String name, double[] model, double[] human) {
			this.name = name;
			this.r = Statistics.correlation(model, human);
			this.error = Statistics.error(model, human);
			this.rmse = Statistics.rmse(model, human);
			this.points = model.length;
		}

		@Override
		public String toString() {
			if (resultString != null)
				return String.format("%-20s", name) + resultString;

			String nameString = String.format("%-20s", name);
			String rString = String.format("%.2f", r);
			if (r > .99)
				rString = ">.99";
			String errorString = String.format("%.2f", error);
			String pointsString = String.format("%d", points);
			String extra = (includeRMSE) ? ("   (RMSE = " + String.format("%.2f", rmse) + ")") : "";
			return nameString + rString + "    " + errorString + "    " + pointsString + extra;
		}
	}

	private static class OkMeasure extends Measure {
		OkMeasure(String name) {
			super(name, 0, 0, 0);
			resultString = "-ok-";
		}
	}

	private static class NotOkMeasure extends Measure {
		NotOkMeasure(String name) {
			super(name, 0, 0, 0);
			resultString = "XXXX";
		}
	}

	public Result() {
		measures = new Vector<>();
	}

	public Result(String name, boolean ok) {
		measures.add(ok ? new OkMeasure(name) : new NotOkMeasure(name));
	}

	public Result(String name, double r, double error, int points) {
		measures.add(new Measure(name, r, error, points));
	}

	public Result(String name, double[] model, double[] human) {
		measures.add(new Measure(name, model, human));
	}

	public Result(String name, double[][] model, double[][] human) {
		measures.add(new Measure(name, Statistics.flatten(model), Statistics.flatten(human)));
	}

	public void add(String name, double r, double error, int points) {
		measures.add(new Measure(name, r, error, points));
	}

	public void add(String name, double[] model, double[] human) {
		measures.add(new Measure(name, model, human));
	}

	public void add(String name, double[][] model, double[][] human) {
		measures.add(new Measure(name, Statistics.flatten(model), Statistics.flatten(human)));
	}

	static public String headerString() {
		return "Task                R       Err     Pts\n-----------------------------------------";
	}

	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < measures.size(); i++)
			s += measures.elementAt(i) + (i < measures.size() - 1 ? "\n" : "");
		return s;
	}
}
