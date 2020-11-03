package actr.tasks.tutorial;

import actr.task.Result;
import actr.task.Task;
import actr.task.TaskLabel;

/**
 * Tutorial Unit 3: Sperling Task
 * 
 * @author Dario Salvucci
 */
public class U3Sperling extends Task {
	String response = "";

	public U3Sperling() {
		super();
		String[] letters = { "V", "N", "T", "Z", "C", "R", "Y", "K", "W", "J", "G", "F" };
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 4; j++) {
				String letter = letters[i * 4 + j];
				TaskLabel label = new TaskLabel(letter, 75 + j * 50, 101 + i * 50, 40, 20);
				add(label);
			}
	}

	@Override
	public void start() {
		response = "";
		processDisplay();
		getModel().audio.addAural("sound", "sound", "1000");
	}

	@Override
	public void typeKey(char c) {
		response += c;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		boolean ok = (response.length() == 5
				&& getModel().procedural.getLastProductionFired().name.getString().contains("stop-report"));
		return new Result("U3Sperling", ok);
	}
}
