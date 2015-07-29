package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Task;

/**
 * Tutorial Unit 1: Addition Task
 * 
 * @author Dario Salvucci
 */
public class U1Addition extends Task {
	@Override
	public Result analyze(Task[] tasks, boolean output) {
		boolean ok = (getModel().getProcedural().getLastProductionFired().getName().getString()
				.contains("terminate-addition"));
		return new Result("U1Addition", ok);
	}
}
