package actr.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Maintains the state of the ACT-R buffers.
 * 
 * @author Dario Salvucci
 */
public class Buffers {
	private final Model model;
	private final Map<Symbol, Chunk> buffers;
	private final Map<Symbol, Double> touchTimes;
	private Vector<Chunk> goals;
	private double lastGoalSetTime = -1;

	boolean bufferChunkDecay = false;
	double bufferChunkLife = 10.0;

	Buffers(Model model) {
		this.model = model;
		buffers = new HashMap<>();
		touchTimes = new HashMap<>();
		goals = new Vector<>();
	}

	/**
	 * Checks whether the given symbol is a legal buffer symbol by checking for
	 * the presence of a state buffer for that symbol.
	 * 
	 * @param symbol
	 *            the symbol
	 * @return <tt>true</tt> if the symbol is a legal buffer symbol
	 */
	public boolean isLegalBuffer(Symbol symbol) {
		String stripped = symbol.getString().replace("=", "");
		return (get(Symbol.get("?" + stripped)) != null);
	}

	/**
	 * Checks whether the specified buffer is a state buffer with a <tt>'?'</tt>
	 * prefix.
	 * 
	 * @param buffer
	 *            the buffer
	 * @return <tt>true</tt> if the buffer is state buffer
	 */
	public static boolean isState(Symbol buffer) {
		return buffer.getString().charAt(0) == '?';
	}

	/**
	 * Gets the chunk in the specified buffer.
	 * 
	 * @param buffer
	 *            the buffer name (without a prefix for normal buffers but with
	 *            the '?' prefix for state buffers)
	 * @return the buffer chunk, or <tt>null</tt> if the buffer is empty
	 */
	public Chunk get(Symbol buffer) {
		return buffers.get(buffer);
	}

	/**
	 * Gets the slot value of a buffer chunk.
	 * 
	 * @param buffer
	 *            the buffer name (without a prefix for normal buffers but with
	 *            the '?' prefix for state buffers)
	 * @param slot
	 *            the slot to be accessed
	 * @return the value of the slot, or <tt>null</tt> if the buffer or slot is
	 *         empty
	 */
	public Symbol getSlot(Symbol buffer, Symbol slot) {
		Chunk c = buffers.get(buffer);
		return c == null ? null : c.get(slot);
	}

	/**
	 * Gets a buffer chunk by the name of that chunk.
	 * 
	 * @param name
	 *            the chunk name
	 * @return the buffer chunk, or <tt>null</tt> if the chunk name does not
	 *         exist
	 */
	public Chunk getBufferChunk(Symbol name) {
		for (Chunk chunk : buffers.values()) {
			if (chunk.name() == name)
				return chunk;
		}
		return null;
	}

	void set(Symbol buffer, Chunk c) {
		if (buffer == Symbol.goal) {
			if (lastGoalSetTime < model.getTime()) {
				Chunk oldGoal = get(Symbol.goal);
				if (oldGoal != null)
					c.setThreadID(oldGoal.getThreadID());
				else
					c.assignNewThreadID();
				goals.remove(oldGoal);
			} else
				c.assignNewThreadID();
			goals.add(c);
			lastGoalSetTime = model.getTime();
		}
		buffers.put(buffer, c);

		if (!isState(buffer)) {
			touch(buffer);
		}

		if (!isState(buffer) && !c.isRequest()) {
			Chunk state = get(Symbol.get("?" + buffer));
			if (state != null)
				state.set(Symbol.buffer, Symbol.full);
		}

		if (model.bold.brainImaging)
			model.bold.recordActivity(buffer);
	}

	void setSlot(Symbol buffer, Symbol slot, Symbol value) {
		Chunk c = buffers.get(buffer);
		if (c != null)
			c.set(slot, value);
		if (!isState(buffer))
			touch(buffer);

		if (model.bold.brainImaging)
			model.bold.recordActivity(buffer);
	}

	void clear(Symbol buffer) {
		if (buffer == Symbol.goal)
			goals.remove(buffers.get(buffer));
		else if (buffer == Symbol.temporal)
			model.removeEvents("temporal");
		else if (buffer == Symbol.retrieval)
			model.removeEvents("declarative");
		else if (buffer == Symbol.visual)
			model.removeEvents("vision");
		else if (buffer == Symbol.aural)
			model.removeEvents("audio");
		else if (buffer == Symbol.manual)
			model.removeEvents("motor");
		else if (buffer == Symbol.vocal)
			model.removeEvents("speech");

		buffers.remove(buffer);
		touchTimes.remove(buffer);

		if (!isState(buffer)) {
			Chunk state = get(Symbol.get("?" + buffer));
			if (state != null) {
				state.set(Symbol.buffer, Symbol.empty);
				state.set(Symbol.state, Symbol.free);
			}
		}

		if (model.bold.brainImaging)
			model.bold.recordActivity(buffer);
	}

	void touch(Symbol buffer) {
		touchTimes.put(buffer, model.getTime());
	}

	void removeDecayedChunks() {
		if (!bufferChunkDecay)
			return;
		Iterator<Symbol> it = buffers.keySet().iterator();
		while (it.hasNext()) {
			Symbol buffer = it.next();
			if (buffer == Symbol.retrieval) {
				Double ttd = touchTimes.get(buffer);
				double tt = (ttd == null) ? 0 : ttd;
				if (tt + bufferChunkLife < model.getTime()) {
					if (model.verboseTrace)
						model.output("buffers", buffer + " cleared (buffer decay)");
					clear(buffer);
					it = buffers.keySet().iterator(); // buffers hashset has
														// changed!
				}
			}
		}
	}

	void sortGoals() {
		Vector<Chunk> v = new Vector<>();
		while (goals.size() > 0) {
			double minTime = 1e10;
			Chunk min = null;
			for (int i = 0; i < goals.size(); i++)
				if (goals.elementAt(i).getLastUsedAsGoal() < minTime) {
					min = goals.elementAt(i);
					minTime = min.getLastUsedAsGoal();
				}
			v.add(min);
			goals.remove(min);
		}
		goals = v;
	}

	int numGoals() {
		return goals.size();
	}

	void tryGoal(int index) {
		buffers.put(Symbol.goal, goals.elementAt(index));
	}

	void replaceSlotValues(Chunk c1, Chunk c2) {
		for (Symbol buffer : buffers.keySet()) {
			Chunk chunk = get(buffer);
			Iterator<Symbol> slots = chunk.getSlotNames();
			while (slots.hasNext()) {
				Symbol slot = slots.next();
				Symbol value = chunk.get(slot);
				if (value == c1.name())
					chunk.set(slot, c2.name());
			}
		}
	}

	/**
	 * Gets a string representation of the buffers showing existing buffers and
	 * their content.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "Buffers:\n";
		s += String.format("%-16s", Symbol.goal) + " : " + get(Symbol.goal) + "\n";
		Iterator<Symbol> it = buffers.keySet().iterator();
		while (it.hasNext()) {
			Symbol buffer = it.next();
			if (!buffer.isState() && buffer != Symbol.goal)
				s += String.format("%-16s", buffer) + " : " + get(buffer) + "\n";
		}

		s += "\nStates:\n";
		it = buffers.keySet().iterator();
		while (it.hasNext()) {
			Symbol buffer = it.next();
			if (buffer.isState() && buffer != Symbol.goalState)
				s += String.format("%-16s", buffer) + " : " + get(buffer) + "\n";
		}

		s += "\nGoals:\n";
		for (int i = 0; i < goals.size(); i++)
			s += goals.elementAt(i) + "\n";

		return s;
	}
}
