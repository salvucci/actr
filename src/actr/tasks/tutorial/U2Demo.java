package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Task;
import actr.task.TaskLabel;

/**
 * Tutorial Unit 2: Demo Task
 * 
 * @author Dario Salvucci
 */
public class U2Demo extends actr.task.Task {
	final TaskLabel label;

	public U2Demo() {
		super();
		label = new TaskLabel("A", 100, 100, 40, 20);
		add(label);
	}

	@Override
	public void start() {
		label.setText("a");
		processDisplay();
	}

	@Override
	public void typeKey(char c) {
		label.setText("-");
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		boolean ok = (getModel().procedural.getLastProductionFired().name.getString().contains("respond"));
		return new Result("U2Demo", ok);
	}
}
