package actr.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Prediction of BOLD response that arises from buffer activity.
 * 
 * @author Dario Salvucci
 */
public class Bold {
	private Model model;
	private Map<Symbol, Activity> activities;

	boolean brainImaging = false;
	double boldScale = .75;
	double boldExponent = 6;
	double boldIncrement = .200; // 1.5;
	double boldSettle = 40;

	private final Symbol buffers[] = { Symbol.retrieval, Symbol.imaginal,
			Symbol.visual, Symbol.aural, Symbol.manual, Symbol.vocal };
	private final double boldMax = maximum();

	private class Span {
		double startTime;
		boolean active;

		Span(double st, boolean a) {
			startTime = st;
			active = a;
		}
	}

	private class Activity {
		Vector<Span> spans = new Vector<Span>();

		Activity() {
			spans.add(new Span(0.0, false));
		}

		void add(double time, boolean active) {
			Span last = spans.lastElement();
			if (last.active != active) {
				if (last.startTime != time)
					spans.add(new Span(time, active));
				else
					last.active = active;
			}
		}

		@Override
		public String toString() {
			String s = "[";
			for (int i = 0; i < spans.size(); i++) {
				Span span = spans.elementAt(i);
				s += " (" + span.startTime + ": " + span.active + ")";
			}
			return s + " ]";
		}
	}

	Bold(Model model) {
		this.model = model;

		activities = new HashMap<Symbol, Activity>();
		for (int i = 0; i < buffers.length; i++)
			activities.put(buffers[i], new Activity());
	}

	void start() {
		update();
	}

	void update() {
		// System.out.print (model.getTime() + " [ ");
		// Iterator<Symbol> it = activities.keySet().iterator();
		// while (it.hasNext())
		// {
		// Symbol buffer = it.next();
		// Activity activity = activities.get(buffer);
		// double cb = cumulative (activity);
		// System.out.print (buffer + ":" + String.format ("%2.3f ", cb) + " ");
		// }
		// System.out.println ("]");

		model.updateVisuals();

		if (model.getEvents().hasMoreEvents()) {
			model.addEvent(new actr.model.Event(
					model.getTime() + boldIncrement, "bold", "") {
				@Override
				public void action() {
					update();
				}
			});
		}
	}

	public boolean isImaging() {
		return brainImaging;
	}

	void recordActivity(Symbol buffer) {
		Symbol stateBuffer = (buffer.isState()) ? buffer : Symbol.get("?"
				+ buffer.getString());
		Chunk c = model.getBuffers().get(stateBuffer);
		boolean active = (c.get(Symbol.buffer) != Symbol.empty || (c
				.get(Symbol.state) != Symbol.free && c.get(Symbol.state) != Symbol.error));

		buffer = (buffer.isState()) ? Symbol.get(buffer.getString()
				.substring(1)) : buffer;
		Activity activity = activities.get(buffer);
		if (activity != null)
			activity.add(model.getTime(), active);
	}

	private double point(double t) {
		if (t <= 0)
			return 0;
		double tScaled = t / boldScale;
		return Math.pow(tScaled, boldExponent) * Math.exp(-tScaled);
	}

	private double span(double start, double end, double current) {
		if (end < current - boldSettle)
			return 0;
		double t = Math.max(start, current - boldSettle);
		double sum = 0;
		double dt = .100;
		for (; t <= end; t += dt)
			sum += point(current - t) * dt;
		return sum;
	}

	private double cumulative(Activity activity) {
		double current = model.getTime();
		double sum = 0;
		Vector<Span> spans = activity.spans;
		for (int i = 0; i < spans.size(); i++) {
			Span span = spans.elementAt(i);
			if (span.active) {
				double start = span.startTime;
				double end = (i + 1 < spans.size()) ? spans.elementAt(i + 1).startTime
						: current;
				if (end >= current - boldSettle)
					sum += span(start, end, current);
			}
		}
		return sum / boldMax;
	}

	private double maximum() {
		double factorial = 1;
		for (int i = 2; i <= boldExponent; i++)
			factorial *= i;
		return factorial * boldScale;
	}

	public double getValue(Symbol buffer) {
		Activity activity = activities.get(buffer);
		if (activity != null)
			return cumulative(activity);
		else
			return 0;
	}
}
