package actr.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import actr.task.Task;

import static java.lang.Double.parseDouble;

/**
 * The parser that translates an ACT-R model file into its code representation.
 * 
 * @author Dario Salvucci
 */
class Parser {
	private Tokenizer t;
	private Symbol lastProduction = null;

	public static final String[] defaultBuffers = { "goal", "retrieval", "visual-location", "visual", "aural-location",
			"aural", "manual", "vocal", "imaginal", "temporal" };

	Parser(String s) {
		t = new Tokenizer(s);
	}

	Parser(File file) {
		try {
			t = new Tokenizer(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static boolean contains(String s, String[] a) {
		for (String value : a)
			if (value.equals(s))
				return true;
		return false;
	}

	void parse(Model model) {
		parse(model, null);
	}

	void parse(Model model, String taskOverride) {
		try {
			model.clearErrors();

			if (taskOverride != null) {
				Task task = Task.createTaskInstance(taskOverride);
				if (task != null) {
					model.setTask(task);
					task.setModel(model);
				}
			}

			label:
			while (t.hasMoreTokens() || !t.token().equals("")) {
				if (!t.token().equals("("))
					model.recordError(t);
				t.next();

				switch (t.token()) {
					case "set-task" -> {
						t.next();
						String taskName = t.token();
						taskName = taskName.substring(1, taskName.length() - 1);
						Task task = Task.createTaskInstance(taskName);
						if (taskOverride == null) {
							if (task != null) {
								model.setTask(task);
								task.setModel(model);
							} else
								model.recordError("task '" + taskName + "' is not defined", t);
						}
						t.next();
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-output-dir" -> {
						t.next();
						String output = t.token();
						output = output.substring(1, output.length() - 1);
						if (!new File(output).exists()) // checking to see if the output directory exists
							model.recordError(t);
						model.fatigue.setOutputDIR(output);
						t.next();
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
// For the sleep schedule in Fatigue model
					case "set-sleep-schedule" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							parseSleepSchedule(model);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
// For the PVT schedule in Fatigue model
					case "set-task-schedule" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							parseTaskSchedule(model);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
// For the PVT duration in Fatigue model
					case "set-task-duration" -> {
						t.next();
						if (t.hasMoreTokens() && !t.token().equals(")")) {
							if (!Utilities.isNumericPos(t.token()))
								model.recordError(t);
							double TaskDuration = parseDouble(t.token());
							model.fatigue.setTaskDuration(TaskDuration);
							t.next();
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-parameter" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							String parameter = t.token();
							t.next();
							if (t.token().equals(")")) {
								model.recordError(t);
								break;
							}
							String value = t.token();
							t.next();
							if (!parameter.startsWith("*") || !parameter.endsWith("*"))
								model.recordWarning("parameter should start and end with '*'", t);
							t.addVariable(parameter, value);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "p", "p*" -> {
						Production p = parseProduction(model);
						model.procedural.add(p);
					}
					case "add-dm" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							Chunk c = parseChunk(model);
							Symbol cname = c.name();
							c = model.declarative.add(c, true);
							if (cname != c.name())
								model.recordWarning("chunk '" + cname + "' has been merged into '" + c.name() + "'", t);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "chunk-type" -> {
						t.next();
						ChunkType chunkType = parseChunkType(model);
						if (chunkType != null)
							model.declarative.add(chunkType);
					}
					case "add-math-facts" -> {
						t.next();
						String plusFact = t.token();
						t.next();
						String timesFact = t.token();
						t.next();
						for (int i = 1; i <= 12; i++)
							for (int j = 1; j <= 12; j++) {
								String name = "plus-" + i + "-" + j;
								String slots = "isa " + plusFact + " arg1 \"" + i + "\" arg2 \"" + j + "\" sum \"" + (i + j)
										+ "\"";
								Chunk c = parseNewChunk(model, Symbol.getUnique(name), slots);
								model.declarative.add(c, true);
								name = "times-" + i + "-" + j;
								slots = "isa " + timesFact + " arg1 \"" + i + "\" arg2 \"" + j + "\" product \"" + (i * j)
										+ "\"";
								c = parseNewChunk(model, Symbol.getUnique(name), slots);
								model.declarative.add(c, true);
							}
						for (int i = 0; i <= 99; i++) {
							String s = "\"" + i + "\"";
							model.vision.setVisualFrequency(s, (i < 10) ? .1 : .01);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "goal-focus" -> {
						t.next();
						String goalName = t.token();
						t.next();
						Chunk c = model.declarative.get(Symbol.get(goalName));
						if (c != null)
							model.buffers.set(Symbol.goal, c);
						else
							model.recordWarning("chunk '" + goalName + "' has not been defined", t);
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "spp" -> {
						t.next();
						Symbol pname = lastProduction;
						if (!t.token().startsWith(":") && !t.token().startsWith(")")) {
							pname = Symbol.get(t.token());
							t.next();
						}
						Production p = model.procedural.get(pname);
						if (p == null) {
							model.recordError("production '" + pname + "' has not been defined", t);
							while (t.hasMoreTokens() && !t.token().equals(")"))
								t.next();
							t.next();
							break label;
						}
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							String parameter = t.token();
							t.next();
							if (t.token().equals(")"))
								model.recordError("missing parameter value", t);
							String value = t.token();
							t.next();
							p.setParameter(parameter, value, t);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "sgp" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							String parameter = t.token();
							t.next();
							String value = t.token();
							t.next();
							model.setParameter(parameter, value, t);
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-base-levels" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							if (!t.token().equals("("))
								model.recordError(t);
							t.next();
							Chunk c = model.declarative.get(Symbol.get(t.token()));
							t.next();
							double baseLevel = parseDouble(t.token());
							t.next();
							if (c != null)
								c.setBaseLevel(baseLevel);
							else
								model.recordWarning("chunk '" + c + "' has not been defined", t);
							if (!t.token().equals(")"))
								model.recordError(t);
							t.next();
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-all-base-levels" -> {
						t.next();
						double baseLevel = parseDouble(t.token());
						model.declarative.setAllBaseLevels(baseLevel);
						t.next();
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-similarities" -> {
						t.next();
						while (t.hasMoreTokens() && !t.token().equals(")")) {
							if (!t.token().equals("("))
								model.recordError(t);
							t.next();
							Symbol s1 = Symbol.get(t.token());
							t.next();
							Symbol s2 = Symbol.get(t.token());
							t.next();
							model.declarative.setSimilarity(s1, s2, parseDouble(t.token()));
							t.next();
							if (!t.token().equals(")"))
								model.recordError(t);
							t.next();
						}
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "start-hand-at-mouse" -> {
						t.next();
						model.motor.moveHandToMouse();
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					case "set-visual-frequency" -> {
						t.next();
						String id = t.token();
						t.next();
						double frequency = parseDouble(t.token());
						model.vision.setVisualFrequency(id, frequency);
						t.next();
						if (!t.token().equals(")"))
							model.recordError(t);
						t.next();
					}
					default -> model.recordError(t);
				}
			}
		} catch (Exception e) {
		}
	}

	Production parseProduction(Model model) throws Exception {
		if (!t.token().equals("p") && !t.token().equals("p*"))
			model.recordError(t);
		t.next();
		Symbol name = Symbol.get(t.token());
		t.next();

		if (model.procedural.get(name) != null) {
			String oldname = name.getString();
			name = Symbol.getUnique(oldname);
			model.recordWarning("production '" + oldname + "' exists; renaming second production as '" + name + "'", t);
		}

		Production p = new Production(name, model);
		Set<String> variables = new HashSet<>();

		while (t.hasMoreTokens() && !t.token().equals("==>")) {
			BufferCondition bc = parseBufferCondition(model, variables);
			p.addBufferCondition(bc);
		}

		if (!t.token().equals("==>"))
			model.recordError(t);
		t.next();

		while (t.hasMoreTokens() && !t.token().equals(")")) {
			BufferAction ba = parseBufferAction(model, variables);
			p.addBufferAction(ba);
		}

		if (!t.token().equals(")"))
			model.recordError(t);
		t.next();

		lastProduction = name;
		return p;
	}

	BufferCondition parseBufferCondition(Model model, Set<String> variables) throws Exception {
		char prefix = t.token().charAt(0);
		String bufferName = t.token().substring(1, t.token().length() - 1);
		if (prefix != '!' && !contains(bufferName, defaultBuffers))
			model.recordWarning("buffer '" + bufferName + "' is not a default buffer", t);
		if (prefix == '=')
			variables.add("=" + bufferName);
		Symbol buffer = Symbol.get((prefix == '?' ? "?" : "") + bufferName);
		t.next();

		BufferCondition bc = new BufferCondition(prefix, buffer, model);

		if (prefix == '!') {
			if (!t.token().equals("("))
				model.recordError(t);
			bc.addSpecial(t.token());
			t.next();
			int nparens = 0;
			while (t.hasMoreTokens() && nparens > 0 || !t.token().equals(")")) {
				if (t.token().equals("("))
					nparens++;
				else if (t.token().equals(")"))
					nparens--;
				bc.addSpecial(t.token());
				t.next();
			}
			if (!t.token().equals(")"))
				model.recordError(t);
			bc.addSpecial(t.token());
			t.next();
		} else {
			while (t.hasMoreTokens() && !t.token().startsWith("?") && !t.token().startsWith("!")
					&& !(t.token().startsWith("=") && t.token().endsWith(">"))) {
				SlotCondition slotCondition = parseSlotCondition(model, variables);
				bc.addCondition(slotCondition);
			}
		}
		return bc;
	}

	SlotCondition parseSlotCondition(Model model, Set<String> variables) {
		String operator = null;
		if (t.token().equals("-") || t.token().equals("<") || t.token().equals(">")
				|| t.token().equals("<=") || t.token().equals(">=")) {
			operator = t.token();
			t.next();
		}
		Symbol slot = Symbol.get(t.token());
		if (slot.isVariable())
			variables.add(slot.getString());
		t.next();
		Symbol value = Symbol.get(t.token());
		if (value.isVariable())
			variables.add(value.getString());
		t.next();
		return new SlotCondition(operator, slot, value, model);
	}

	BufferAction parseBufferAction(Model model, Set<String> variables) throws Exception {
		char prefix = t.token().charAt(0);
		String bufferName = t.token().substring(1, t.token().length() - 1);
		if (prefix != '!' && !contains(bufferName, defaultBuffers))
			model.recordWarning("buffer '" + bufferName + "' is not a default buffer", t);
		Symbol buffer = Symbol.get((prefix == '?' ? "?" : "") + bufferName);
		t.next();

		BufferAction ba = new BufferAction(prefix, buffer, model);
		if (prefix == '!') {
			if (!t.token().equals("(")) {
				ba.setBind(Symbol.get(t.token()));
				variables.add(t.token());
				t.next();
			}
			if (!t.token().equals("("))
				model.recordError(t);
			ba.addSpecial(t.token());
			t.next();
			int nparens = 0;
			while (t.hasMoreTokens() && (nparens > 0 || !t.token().equals(")"))) {
				if (t.token().equals("("))
					nparens++;
				else if (t.token().equals(")"))
					nparens--;
				ba.addSpecial(t.token());
				t.next();
			}
			if (!t.token().equals(")"))
				model.recordError(t);
			ba.addSpecial(t.token());
			t.next();
			return ba;
		} else if (prefix == '-') {
			return ba;
		} else if (t.token().startsWith("=") && !t.token().endsWith(">")) // direct
																				// setting
																				// of
																				// buffer?
		{
			String direct = t.token();
			t.next();
			String next = t.token();
			if (next.equals(")") || next.startsWith("-") || next.startsWith("+") || next.startsWith("!")
					|| (next.startsWith("=") && next.endsWith(">"))) {
				ba.setDirect(Symbol.get(direct));
				if (!variables.contains(direct))
					model.recordWarning("variable '" + direct + "' is not set", t);
				return ba;
			} else
				t.pushBack(direct);
		}

		while (t.hasMoreTokens() && !t.token().equals(")")
				&& !(t.token().startsWith("=") && t.token().endsWith(">"))
				&& !(t.token().startsWith("-") && t.token().length() > 1) && !t.token().startsWith("+")
				&& !t.token().startsWith("!")) {
			SlotAction slotAction = parseSlotAction(model, variables);
			ba.addAction(slotAction);
		}
		return ba;
	}

	SlotAction parseSlotAction(Model model, Set<String> variables) {
		String operator = null;
		if (t.token().equals("-") || t.token().equals("<") || t.token().equals(">")
				|| t.token().equals("<=") || t.token().equals(">=")) {
			operator = t.token();
			t.next();
		}
		Symbol slot = Symbol.get(t.token());
		if (slot.isVariable() && !variables.contains(slot.getString()))
			model.recordWarning("variable '" + slot.getString() + "' is not set", t);
		t.next();
		Symbol value = Symbol.get(t.token());
		if (value.isVariable() && !variables.contains(value.getString()))
			model.recordWarning("variable '" + value.getString() + "' is not set", t);
		t.next();
		return new SlotAction(operator, slot, value, model);
	}

	Chunk parseChunk(Model model) throws Exception {
		if (!t.token().equals("("))
			model.recordError(t);
		t.next();
		Symbol name = Symbol.get(t.token());
		t.next();

		Chunk c = model.declarative.get(name);
		if (c == null)
			c = new Chunk(name, model);
		else
			model.recordWarning("redefining chunk '" + name + "'", t);

		while (t.hasMoreTokens() && !t.token().equals(")")) {
			Symbol slot = Symbol.get(t.token());
			t.next();
			if (t.token().equals(")"))
				model.recordError(t);
			Symbol value = Symbol.get(t.token());
			t.next();
			c.set(slot, value);
		}
		if (!t.token().equals(")"))
			model.recordError(t);
		t.next();

		return c;
	}

	void parseSleepSchedule(Model model) throws Exception {
		if (!t.token().equals("("))
			model.recordError(t);
		t.next();
		if (!Utilities.isNumericPos(t.token()))
			model.recordError(t);
		double first = parseDouble(t.token());
		model.fatigue.addWake(first);
		t.next();
		if (t.token().equals(")") || !Utilities.isNumericPos(t.token()))
			model.recordError(t);
		double second = parseDouble(t.token());
		if (second < first)
			model.recordError(t);
		model.fatigue.addSleep(second);
		t.next();
		if (!t.token().equals(")"))
			model.recordError(t);
		t.next();

	}

	void parseTaskSchedule(Model model) throws Exception {
		if (!Utilities.isNumericPos(t.token()))
			model.recordError(t);
		double PVTtime = parseDouble(t.token());
		model.fatigue.addTaskSchedule(PVTtime);
		t.next();
	}

	static Chunk parseNewChunk(Model model, Symbol name, String slots) {
		Parser p = new Parser(slots);
		Chunk c = new Chunk(name, model);
		while (p.t.hasMoreTokens()) {
			Symbol slot = Symbol.get(p.t.token());
			p.t.next();
			Symbol value = Symbol.get(p.t.token());
			p.t.next();
			c.set(slot, value);
		}
		return c;
	}

	ChunkType parseChunkType(Model model) throws Exception {
		ChunkType chunkType;
		if (t.token().equals("(")) {
			t.next();
			Symbol name = Symbol.get(t.token());
			t.next();
			chunkType = new ChunkType(name);
			if (!t.token().equals("("))
				model.recordError(t);
			t.next();
			if (!t.token().equals(":include"))
				model.recordError(t);
			t.next();
			while (t.hasMoreTokens() && !t.token().equals(")")) {
				Symbol parentName = Symbol.get(t.token());
				ChunkType parent = model.declarative.getChunkType(parentName);
				if (parent != null)
					chunkType.addParent(parent);
				else
					model.recordError("invalid parent chunk type", t);
				t.next();
			}
			if (!t.token().equals(")"))
				model.recordError(t);
			t.next();
			if (!t.token().equals(")"))
				model.recordError(t);
			t.next();
		} else {
			Symbol name = Symbol.get(t.token());
			t.next();
			chunkType = new ChunkType(name);
		}
		int parens = 0;
		while (t.hasMoreTokens() && (!t.token().equals(")") || parens > 0)) {
			if (t.token().equals("("))
				parens++;
			else if (t.token().equals(")"))
				parens--;
			t.next();
		}
		if (!t.token().equals(")"))
			model.recordError(t);
		t.next();
		return chunkType;
	}
}
