package actr.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import actr.task.Task;

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
			while (t.hasMoreTokens() || !t.getToken().isEmpty()) {
				if (!t.getToken().equals("("))
					model.recordError(t);
				t.advance();

				switch (t.getToken()) {
					case "set-task":
						t.advance();
						String taskName = t.getToken();
						taskName = taskName.substring(1, taskName.length() - 1);
						Task task = Task.createTaskInstance(taskName);
						if (taskOverride == null) {
							if (task != null) {
								model.setTask(task);
								task.setModel(model);
							} else
								model.recordError("task '" + taskName + "' is not defined", t);
						}
						t.advance();
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "set-parameter":
						t.advance();
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							String parameter = t.getToken();
							t.advance();
							if (t.getToken().equals(")")) {
								model.recordError(t);
								break;
							}
							String value = t.getToken();
							t.advance();
							if (!parameter.startsWith("*") || !parameter.endsWith("*"))
								model.recordWarning("parameter should start and end with '*'", t);
							t.addVariable(parameter, value);
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "p":
					case "p*": {
						Production p = parseProduction(model);
						model.procedural.add(p);
						break;
					}
					case "add-dm":
						t.advance();
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							Chunk c = parseChunk(model);
							Symbol cname = c.name();
							c = model.declarative.add(c, true);
							if (cname != c.name())
								model.recordWarning("chunk '" + cname + "' has been merged into '" + c.name() + "'", t);
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "chunk-type":
						t.advance();
						ChunkType chunkType = parseChunkType(model);
						if (chunkType != null)
							model.declarative.add(chunkType);
						break;
					case "add-math-facts":
						t.advance();
						String plusFact = t.getToken();
						t.advance();
						String timesFact = t.getToken();
						t.advance();
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
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "goal-focus":
						t.advance();
						String goalName = t.getToken();
						t.advance();
						Chunk c = model.declarative.get(Symbol.get(goalName));
						if (c != null)
							model.buffers.set(Symbol.goal, c);
						else
							model.recordWarning("chunk '" + goalName + "' has not been defined", t);
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "spp": {
						t.advance();
						Symbol pname = lastProduction;
						if (!t.getToken().startsWith(":") && !t.getToken().startsWith(")")) {
							pname = Symbol.get(t.getToken());
							t.advance();
						}
						Production p = model.procedural.get(pname);
						if (p == null) {
							model.recordError("production '" + pname + "' has not been defined", t);
							while (t.hasMoreTokens() && !t.getToken().equals(")"))
								t.advance();
							t.advance();
							break label;
						}
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							String parameter = t.getToken();
							t.advance();
							if (t.getToken().equals(")"))
								model.recordError("missing parameter value", t);
							String value = t.getToken();
							t.advance();
							p.setParameter(parameter, value, t);
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					}
					case "sgp":
						t.advance();
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							String parameter = t.getToken();
							t.advance();
							String value = t.getToken();
							t.advance();
							model.setParameter(parameter, value, t);
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "set-base-levels":
						t.advance();
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							if (!t.getToken().equals("("))
								model.recordError(t);
							t.advance();
							Chunk cc = model.declarative.get(Symbol.get(t.getToken()));
							t.advance();
							double baseLevel = Double.parseDouble(t.getToken());
							t.advance();
							if (cc != null)
								cc.setBaseLevel(baseLevel);
							else
								model.recordWarning("chunk '" + cc + "' has not been defined", t);
							if (!t.getToken().equals(")"))
								model.recordError(t);
							t.advance();
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "set-all-base-levels":
						t.advance();
						double baseLevel = Double.parseDouble(t.getToken());
						model.declarative.setAllBaseLevels(baseLevel);
						t.advance();
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "set-similarities":
						t.advance();
						while (t.hasMoreTokens() && !t.getToken().equals(")")) {
							if (!t.getToken().equals("("))
								model.recordError(t);
							t.advance();
							Symbol s1 = Symbol.get(t.getToken());
							t.advance();
							Symbol s2 = Symbol.get(t.getToken());
							t.advance();
							model.declarative.setSimilarity(s1, s2, Double.parseDouble(t.getToken()));
							t.advance();
							if (!t.getToken().equals(")"))
								model.recordError(t);
							t.advance();
						}
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "start-hand-at-mouse":
						t.advance();
						model.motor.moveHandToMouse();
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					case "set-visual-frequency":
						t.advance();
						String id = t.getToken();
						t.advance();
						double frequency = Double.parseDouble(t.getToken());
						model.vision.setVisualFrequency(id, frequency);
						t.advance();
						if (!t.getToken().equals(")"))
							model.recordError(t);
						t.advance();
						break;
					default:
						model.recordError(t);
						break;
				}
			}
		} catch (Exception e) {
		}
	}

	Production parseProduction(Model model) throws Exception {
		// if (!t.getToken().equals("(")) { model.error(t); throw new
		// Exception(); };
		// t.advance();
		if (!t.getToken().equals("p") && !t.getToken().equals("p*"))
			model.recordError(t);
		t.advance();
		Symbol name = Symbol.get(t.getToken());
		t.advance();

		if (model.procedural.get(name) != null) {
			String oldname = name.getString();
			name = Symbol.getUnique(oldname);
			model.recordWarning("production '" + oldname + "' exists; renaming second production as '" + name + "'", t);
		}

		Production p = new Production(name, model);
		Set<String> variables = new HashSet<>();

		while (t.hasMoreTokens() && !t.getToken().equals("==>")) {
			BufferCondition bc = parseBufferCondition(model, variables);
			p.addBufferCondition(bc);
		}

		if (!t.getToken().equals("==>"))
			model.recordError(t);
		t.advance();

		while (t.hasMoreTokens() && !t.getToken().equals(")")) {
			BufferAction ba = parseBufferAction(model, variables);
			p.addBufferAction(ba);
		}

		if (!t.getToken().equals(")"))
			model.recordError(t);
		t.advance();

		lastProduction = name;
		return p;
	}

	BufferCondition parseBufferCondition(Model model, Set<String> variables) throws Exception {
		char prefix = t.getToken().charAt(0);
		String bufferName = t.getToken().substring(1, t.getToken().length() - 1);
		if (prefix != '!' && !contains(bufferName, defaultBuffers))
			model.recordWarning("buffer '" + bufferName + "' is not a default buffer", t);
		if (prefix == '=')
			variables.add("=" + bufferName);
		Symbol buffer = Symbol.get((prefix == '?' ? "?" : "") + bufferName);
		t.advance();

		BufferCondition bc = new BufferCondition(prefix, buffer, model);

		if (prefix == '!') {
			if (!t.getToken().equals("("))
				model.recordError(t);
			bc.addSpecial(t.getToken());
			t.advance();
			int nparens = 0;
			while (t.hasMoreTokens() && nparens > 0 || !t.getToken().equals(")")) {
				if (t.getToken().equals("("))
					nparens++;
				else if (t.getToken().equals(")"))
					nparens--;
				bc.addSpecial(t.getToken());
				t.advance();
			}
			if (!t.getToken().equals(")"))
				model.recordError(t);
			bc.addSpecial(t.getToken());
			t.advance();
		} else {
			while (t.hasMoreTokens() && !t.getToken().startsWith("?") && !t.getToken().startsWith("!")
					&& !(t.getToken().startsWith("=") && t.getToken().endsWith(">"))) {
				SlotCondition slotCondition = parseSlotCondition(model, variables);
				bc.addCondition(slotCondition);
			}
		}
		return bc;
	}

	SlotCondition parseSlotCondition(Model model, Set<String> variables) {
		String operator = null;
		if (t.getToken().equals("-") || t.getToken().equals("<") || t.getToken().equals(">")
				|| t.getToken().equals("<=") || t.getToken().equals(">=")) {
			operator = t.getToken();
			t.advance();
		}
		Symbol slot = Symbol.get(t.getToken());
		if (slot.isVariable())
			variables.add(slot.getString());
		t.advance();
		Symbol value = Symbol.get(t.getToken());
		if (value.isVariable())
			variables.add(value.getString());
		t.advance();
		return new SlotCondition(operator, slot, value, model);
	}

	BufferAction parseBufferAction(Model model, Set<String> variables) throws Exception {
		char prefix = t.getToken().charAt(0);
		String bufferName = t.getToken().substring(1, t.getToken().length() - 1);
		if (prefix != '!' && !contains(bufferName, defaultBuffers))
			model.recordWarning("buffer '" + bufferName + "' is not a default buffer", t);
		Symbol buffer = Symbol.get((prefix == '?' ? "?" : "") + bufferName);
		t.advance();

		BufferAction ba = new BufferAction(prefix, buffer, model);
		if (prefix == '!') {
			if (!t.getToken().equals("(")) {
				ba.setBind(Symbol.get(t.getToken()));
				variables.add(t.getToken());
				t.advance();
			}
			if (!t.getToken().equals("("))
				model.recordError(t);
			ba.addSpecial(t.getToken());
			t.advance();
			int nparens = 0;
			while (t.hasMoreTokens() && (nparens > 0 || !t.getToken().equals(")"))) {
				if (t.getToken().equals("("))
					nparens++;
				else if (t.getToken().equals(")"))
					nparens--;
				ba.addSpecial(t.getToken());
				t.advance();
			}
			if (!t.getToken().equals(")"))
				model.recordError(t);
			ba.addSpecial(t.getToken());
			t.advance();
			return ba;
		} else if (prefix == '-') {
			return ba;
		} else if (t.getToken().startsWith("=") && !t.getToken().endsWith(">")) // direct
																				// setting
																				// of
																				// buffer?
		{
			String direct = t.getToken();
			t.advance();
			String next = t.getToken();
			if (next.equals(")") || next.startsWith("-") || next.startsWith("+") || next.startsWith("!")
					|| (next.startsWith("=") && next.endsWith(">"))) {
				ba.setDirect(Symbol.get(direct));
				if (!variables.contains(direct))
					model.recordWarning("variable '" + direct + "' is not set", t);
				return ba;
			} else
				t.pushBack(direct);
		}

		while (t.hasMoreTokens() && !t.getToken().equals(")")
				&& !(t.getToken().startsWith("=") && t.getToken().endsWith(">"))
				&& !(t.getToken().startsWith("-") && t.getToken().length() > 1) && !t.getToken().startsWith("+")
				&& !t.getToken().startsWith("!")) {
			SlotAction slotAction = parseSlotAction(model, variables);
			ba.addAction(slotAction);
		}
		return ba;
	}

	SlotAction parseSlotAction(Model model, Set<String> variables) {
		String operator = null;
		if (t.getToken().equals("-") || t.getToken().equals("<") || t.getToken().equals(">")
				|| t.getToken().equals("<=") || t.getToken().equals(">=")) {
			operator = t.getToken();
			t.advance();
		}
		Symbol slot = Symbol.get(t.getToken());
		if (slot.isVariable() && !variables.contains(slot.getString()))
			model.recordWarning("variable '" + slot.getString() + "' is not set", t);
		t.advance();
		Symbol value = Symbol.get(t.getToken());
		if (value.isVariable() && !variables.contains(value.getString()))
			model.recordWarning("variable '" + value.getString() + "' is not set", t);
		t.advance();
		return new SlotAction(operator, slot, value, model);
	}

	Chunk parseChunk(Model model) throws Exception {
		if (!t.getToken().equals("("))
			model.recordError(t);
		t.advance();
		Symbol name = Symbol.get(t.getToken());
		t.advance();

		Chunk c = model.declarative.get(name);
		if (c == null)
			c = new Chunk(name, model);
		else
			model.recordWarning("redefining chunk '" + name + "'", t);

		while (t.hasMoreTokens() && !t.getToken().equals(")")) {
			Symbol slot = Symbol.get(t.getToken());
			t.advance();
			if (t.getToken().equals(")"))
				model.recordError(t);
			Symbol value = Symbol.get(t.getToken());
			t.advance();
			c.set(slot, value);
		}
		if (!t.getToken().equals(")"))
			model.recordError(t);
		t.advance();

		return c;
	}

	static Chunk parseNewChunk(Model model, Symbol name, String slots) {
		Parser p = new Parser(slots);
		Chunk c = new Chunk(name, model);
		while (p.t.hasMoreTokens()) {
			Symbol slot = Symbol.get(p.t.getToken());
			p.t.advance();
			Symbol value = Symbol.get(p.t.getToken());
			p.t.advance();
			c.set(slot, value);
		}
		return c;
	}

	ChunkType parseChunkType(Model model) throws Exception {
		ChunkType chunkType;
		if (t.getToken().equals("(")) {
			t.advance();
			Symbol name = Symbol.get(t.getToken());
			t.advance();
			chunkType = new ChunkType(name);
			if (!t.getToken().equals("("))
				model.recordError(t);
			t.advance();
			if (!t.getToken().equals(":include"))
				model.recordError(t);
			t.advance();
			while (t.hasMoreTokens() && !t.getToken().equals(")")) {
				Symbol parentName = Symbol.get(t.getToken());
				ChunkType parent = model.declarative.getChunkType(parentName);
				if (parent != null)
					chunkType.addParent(parent);
				else
					model.recordError("invalid parent chunk type", t);
				t.advance();
			}
			if (!t.getToken().equals(")"))
				model.recordError(t);
			t.advance();
			if (!t.getToken().equals(")"))
				model.recordError(t);
			t.advance();
		} else {
			Symbol name = Symbol.get(t.getToken());
			t.advance();
			chunkType = new ChunkType(name);
		}
		int parens = 0;
		while (t.hasMoreTokens() && (!t.getToken().equals(")") || parens > 0)) {
			if (t.getToken().equals("("))
				parens++;
			else if (t.getToken().equals(")"))
				parens--;
			t.advance();
		}
		if (!t.getToken().equals(")"))
			model.recordError(t);
		t.advance();
		return chunkType;
	}
}
