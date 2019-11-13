package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Task;

/**
 * Tutorial Unit 1: Count Task
 * 
 * @author Dario Salvucci
 */
public class U1Count extends Task {
	@Override
	public Result analyze(Task[] tasks, boolean output) {
		boolean ok = (getModel().procedural.getLastProductionFired().getName().getString().contains("stop"));
		return new Result("U1Count", ok);
	}
}
