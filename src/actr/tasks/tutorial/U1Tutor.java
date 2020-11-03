package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Task;

/**
 * Tutorial Unit 1: Tutor Task
 * 
 * @author Dario Salvucci
 */
public class U1Tutor extends Task {
	@Override
	public Result analyze(Task[] tasks, boolean output) {
		boolean ok = (getModel().procedural.getLastProductionFired().name.getString()
				.contains("add-tens-done"));
		return new Result("U1Tutor", ok);
	}
}
