package actr.model;

import java.util.Iterator;
import java.util.Vector;

/**
 * Conditions tested for a buffer as specified in the (left-hand) condition side
 * of an ACT-R production rule.
 * 
 * @author Dario Salvucci
 */
class BufferCondition {
	private Model model;
	private char prefix;
	private Symbol buffer;
	private Vector<SlotCondition> slotConditions;
	private Vector<String> specials;

	BufferCondition(char prefix, Symbol buffer, Model model) {
		this.prefix = prefix;
		this.buffer = buffer;
		this.model = model;
		slotConditions = new Vector<SlotCondition>();
		specials = new Vector<String>();
	}

	BufferCondition copy() {
		BufferCondition bc = new BufferCondition(prefix, buffer, model);
		for (int i = 0; i < slotConditions.size(); i++)
			bc.slotConditions.add(slotConditions.elementAt(i).copy());
		for (int i = 0; i < specials.size(); i++)
			bc.specials.add(specials.elementAt(i));
		return bc;
	}

	/**
	 * Checks recursively whether two buffer conditions are the same.
	 * 
	 * @param ba2
	 *            the second buffer action
	 * @return <tt>true</tt> if the two buffer conditions are the same, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean equals(BufferCondition bc2) {
		if (prefix != bc2.prefix)
			return false;
		if (buffer != bc2.buffer)
			return false;
		if (slotConditions.size() != bc2.slotConditions.size())
			return false;
		for (int i = 0; i < slotConditions.size(); i++)
			if (!slotConditions.elementAt(i).equals(bc2.slotConditions.elementAt(i)))
				return false;
		if (specials.size() != bc2.specials.size())
			return false;
		for (int i = 0; i < specials.size(); i++)
			if (!specials.elementAt(i).equals(bc2.specials.elementAt(i)))
				return false;
		return true;
	}

	/**
	 * Gets the buffer condition's prefix (one of <tt>= !</tt>).
	 * 
	 * @return the prefix character
	 */
	public char getPrefix() {
		return prefix;
	}

	/**
	 * Gets the buffer name associated with this buffer condition.
	 * 
	 * @return the buffer name
	 */
	public Symbol getBuffer() {
		return buffer;
	}

	/**
	 * Gets the number of slot conditions in this buffer action.
	 * 
	 * @return the number of slot conditions
	 */
	public int slotCount() {
		return slotConditions.size();
	}

	/**
	 * Gets the slot condition for a given index.
	 * 
	 * @param i
	 *            the slot condition index
	 * @return the slot condition at the given index
	 */
	public SlotCondition getSlotCondition(int i) {
		return slotConditions.elementAt(i);
	}

	/**
	 * Gets an iterator for all slot conditions.
	 * 
	 * @return an iterator that iterates over all slot conditions
	 */
	public Iterator<SlotCondition> getSlotConditions() {
		return slotConditions.iterator();
	}

	/**
	 * Gets the slot condition for a given slot.
	 * 
	 * @param slot
	 *            the slot
	 * @return the slot condition for that slot, or <tt>null</tt> if the slot is
	 *         not present
	 */
	public SlotCondition getSlotCondition(Symbol slot) {
		for (int i = 0; i < slotConditions.size(); i++)
			if (slotConditions.elementAt(i).getSlot() == slot)
				return slotConditions.elementAt(i);
		return null;
	}

	/**
	 * Checks whether the buffer condition specifies a given slot.
	 * 
	 * @param slot
	 *            the slot
	 * @returns <tt>true</tt> if the buffer condition specifies the slot in any
	 *          slot condition, and <tt>false</tt> otherwise
	 */
	public boolean hasSlotCondition(Symbol slot) {
		return getSlotCondition(slot) != null;
	}

	/**
	 * Checks whether the buffer condition specifies a given slot value.
	 * 
	 * @param value
	 *            the slot value
	 * @returns <tt>true</tt> if the buffer condition specifies the slot value
	 *          in any slot condition, and <tt>false</tt> otherwise
	 */
	public boolean hasSlotValue(Symbol value) {
		for (int i = 0; i < slotConditions.size(); i++)
			if (slotConditions.elementAt(i).getValue() == value)
				return true;
		return false;
	}

	/**
	 * Checks whether the buffer condition is a special condition denoted by the
	 * <tt>!</tt> prefix.
	 * 
	 * @return <tt>true</tt> if the buffer condition is special, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isSpecial() {
		return (prefix == '!');
	}

	void addCondition(SlotCondition sc) {
		slotConditions.add(sc);
	}

	void addSpecial(String s) {
		specials.add(s);
	}

	boolean testBufferChunk(Chunk bufferChunk, Instantiation inst) {
		for (int i = 0; i < slotConditions.size(); i++) {
			SlotCondition slotCondition = slotConditions.elementAt(i);
			if (model.getProcedural().whyNotTrace) {
				int savedSize = inst.size();
				boolean result = slotCondition.test(buffer, bufferChunk, inst);
				if (model.getProcedural().whyNotTrace) {
					if (result && inst.size() > savedSize)
						model.output(slotCondition.toString(inst, null));
					else
						model.output(slotCondition.toString());
				}
				if (!result)
					return false;
			} else {
				if (!slotCondition.test(buffer, bufferChunk, inst))
					return false;
			}
		}
		if (buffer == Symbol.goal) {
			Chunk goal = model.getBuffers().get(Symbol.goal);
			if (goal != null)
				inst.setThreadID(goal.getThreadID());
		}
		return true;
	}

	boolean test(Instantiation inst) {
		if (prefix == '!') {
			if (model.getProcedural().whyNotTrace)
				model.output(toStringFirstLine(null));
			Vector<String> tokens = new Vector<String>();
			for (int i = 0; i < specials.size(); i++) {
				String special = specials.elementAt(i);
				if (Symbol.get(special).isVariable())
					special = inst.get(Symbol.get(special)).getString();
				tokens.add(special);
			}
			try {
				return Utilities.evalComputeCondition(tokens.iterator());
			} catch (Exception e) {
				return model.getTask().evalCondition(tokens.iterator());
			}
		} else {
			Chunk bufferChunk = model.getBuffers().get(buffer);
			if (bufferChunk != null && prefix == '=')
				inst.set(Symbol.get("=" + buffer), bufferChunk.getName());
			if (model.getProcedural().whyNotTrace)
				model.output(toStringFirstLine(inst));
			if (bufferChunk == null)
				return false;
			return testBufferChunk(bufferChunk, inst);
		}
	}

	void specialize(Symbol variable, Symbol value) {
		for (int i = 0; i < slotConditions.size(); i++)
			slotConditions.elementAt(i).specialize(variable, value);
	}

	/**
	 * Gets a string representation of the first line of the buffer condition,
	 * with an instantiation if provided.
	 * 
	 * @return the string
	 */
	String toStringFirstLine(Instantiation inst) {
		String s = "";
		if (prefix == '!') {
			s = "   !" + buffer + "! (";
			for (int i = 0; i < specials.size(); i++)
				s += " " + specials.elementAt(i);
			s += " )";
		} else {
			s = "   " + ((prefix == '?') ? "" : prefix) + buffer + ">";
			if (inst != null && prefix == '=') {
				Symbol v = inst.get(Symbol.get("=" + buffer));
				if (v != null) {
					s = String.format("%-35s", s);
					s += "[" + "=" + buffer + " <- " + v + "]";
				}
			}
		}
		return s;
	}

	/**
	 * Gets a string representation of the buffer condition with an
	 * instantiation.
	 * 
	 * @return the string
	 */
	public String toString(Instantiation inst, Vector<Symbol> used) {
		String s = "";
		s += toStringFirstLine(inst) + "\n";
		for (int i = 0; i < slotConditions.size(); i++)
			s += slotConditions.elementAt(i).toString(inst, used) + "\n";
		return s;
	}

	/**
	 * Gets a string representation of the buffer condition, with standard
	 * indentation.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return toString(null, null);
	}
}
