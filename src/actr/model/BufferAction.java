package actr.model;

import actr.task.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Actions performed on a buffer as specified in the (right-hand) action side of
 * an ACT-R production rule.
 * 
 * @author Dario Salvucci
 */
class BufferAction {
	public final Model model;
	private char prefix;
	public final Symbol buffer;
	private final List<SlotAction> slotActions = new ArrayList();
	private Symbol directAction;
	private Symbol bind;
	private final List<String> specials = new ArrayList();

	BufferAction(char prefix, Symbol buffer, Model model) {
		this.prefix = prefix;
		this.buffer = buffer;
		this.model = model;
		directAction = null;
		bind = null;
	}

	BufferAction copy() {
		BufferAction ba = new BufferAction(prefix, buffer, model);
		for (int i = 0; i < slotActions.size(); i++)
			ba.slotActions.add(slotActions.get(i).copy());
		ba.directAction = directAction;
		ba.bind = bind;
		ba.specials.addAll(specials);
		return ba;
	}

	/**
	 * Checks recursively whether two buffer actions are the same.
	 * 
	 * @param ba2
	 *            the second buffer action
	 * @return <tt>true</tt> if the two buffer actions are the same, or
	 *         <tt>false</tt> otherwise
	 */
	@Override public boolean equals(Object x) {
		if (this == x)
			return true;
		BufferAction ba2 = (BufferAction) x;
		if (prefix != ba2.prefix)
			return false;
		if (buffer != ba2.buffer)
			return false;
		if (slotActions.size() != ba2.slotActions.size())
			return false;
		if (directAction != ba2.directAction)
			return false;
		if (bind != ba2.bind)
			return false;
		if (specials.size() != ba2.specials.size())
			return false;
		for (int i = 0; i < slotActions.size(); i++)
			if (!slotActions.get(i).equals(ba2.slotActions.get(i)))
				return false;
		for (int i = 0; i < specials.size(); i++)
			if (!specials.get(i).equals(ba2.specials.get(i)))
				return false;
		return true;
	}

	/**
	 * Gets the buffer action's prefix (one of <tt>= + - !</tt>).
	 * 
	 * @return the prefix character
	 */
	public char getPrefix() {
		return prefix;
	}

	/**
	 * Gets the number of slot actions in this buffer action.
	 * 
	 * @return the number of slot actions
	 */
	public int slotCount() {
		return slotActions.size();
	}

	/**
	 * Gets the slot action for a given index.
	 * 
	 * @param i
	 *            the slot action index
	 * @return the slot action at the given index
	 */
	public SlotAction getSlotAction(int i) {
		return slotActions.get(i);
	}

	/**
	 * Gets an iterator for all slot actions.
	 * 
	 * @return an iterator that iterates over all slot actions
	 */
	public Iterator<SlotAction> getSlotActions() {
		return slotActions.iterator();
	}

	/**
	 * Gets the slot action for a given slot.
	 * 
	 * @param slot
	 *            the slot
	 * @return the slot action for that slot, or <tt>null</tt> if the slot is
	 *         not present
	 */
	public SlotAction getSlotAction(Symbol slot) {
		for (SlotAction slotAction : slotActions)
			if (slotAction.getSlot() == slot)
				return slotAction;
		return null;
	}

	/**
	 * Checks whether the buffer action specifies a given slot.
	 * 
	 * @param slot
	 *            the slot
	 * @returns <tt>true</tt> if the buffer action specifies the slot in any
	 *          slot action, and <tt>false</tt> otherwise
	 */
	public boolean hasSlotAction(Symbol slot) {
		return getSlotAction(slot) != null;
	}

	/**
	 * Checks whether the buffer action specifies a given slot value.
	 * 
	 * @param value
	 *            the slot value
	 * @returns <tt>true</tt> if the buffer action specifies the slot value in
	 *          any slot action, and <tt>false</tt> otherwise
	 */
	public boolean hasSlotValue(Symbol value) {
		for (SlotAction slotAction : slotActions)
			if (slotAction.getValue() == value)
				return true;
		return false;
	}

	/**
	 * Checks whether the buffer action represents the direct setting of a
	 * buffer (as in <tt>=buffer> =chunk</tt>).
	 * 
	 * @return <tt>true</tt> if the buffer action is a direct setting, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isDirect() {
		return (directAction != null);
	}

	/**
	 * Checks whether the buffer action is a special action denoted by the
	 * <tt>!</tt> prefix.
	 * 
	 * @return <tt>true</tt> if the buffer action is special, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean isSpecial() {
		return (prefix == '!');
	}

	void addAction(SlotAction sa) {
		slotActions.add(sa);
	}

	void setPrefix(char c) {
		prefix = c;
	}

	void setDirect(Symbol s) {
		directAction = s;
	}

	void setBind(Symbol s) {
		bind = s;
	}

	void addSpecial(String s) {
		specials.add(s);
	}

	private void storeInMemory(Symbol buffer, Instantiation inst, boolean forceVisual) {
		Chunk bufferChunk = model.buffers.get(buffer);
		if (bufferChunk != null && (forceVisual || ((buffer != Symbol.visloc && buffer != Symbol.aurloc
				&& buffer != Symbol.visual && buffer != Symbol.aural)))) {
			if (model.verboseTrace)
				model.output("declarative", "store chunk [" + bufferChunk.name() + "] " + bufferChunk);
			Chunk newChunk = model.declarative.add(bufferChunk);
			if (newChunk != bufferChunk && model.verboseTrace)
				model.output("declarative", "merged into [" + newChunk.name() + "]");
			if (newChunk != bufferChunk)
				inst.replaceValue(bufferChunk.name(), newChunk.name());
		}
	}

	void fire(Instantiation inst) {
		if (directAction != null) {
			Symbol directSymbol = inst.get(directAction);
			if (directSymbol == null) {
				model.outputWarning(directAction + " not a valid symbol");
				return;
			}
			Chunk direct = model.declarative.get(directSymbol);
			if (direct == null)
				direct = model.buffers.getBufferChunk(directSymbol);
			if (direct == null) {
				model.outputWarning(directAction + " -> " + directSymbol + " not a valid chunk");
				return;
			}
			direct = direct.copy();
			direct.setRequest(true);
			model.buffers.set(buffer, direct);
		} else if (prefix == '=') {
			Chunk bufferChunk = model.buffers.get(buffer); // inst.get
																// (Symbol.get("="+buffer));
			if (bufferChunk == null) {
				model.outputWarning(buffer + " empty, not referenced in LHS?");
				return;
			}
			for (SlotAction slotAction : slotActions) slotAction.fire(inst, bufferChunk);
		} else if (prefix == '+') {
			if (model.declarative.addChunkOnNewRequest)
				storeInMemory(buffer, inst, false);
			Chunk requestChunk = new Chunk(Symbol.getUnique("chunk"), model);
			for (SlotAction slotAction : slotActions) slotAction.fire(inst, requestChunk);
			Symbol chunkType = requestChunk.get(Symbol.get("isa"));
			if (chunkType != Symbol.nil)
				requestChunk.setName(Symbol.getUnique(chunkType.getString()));
			requestChunk.setRequest(true);
			model.buffers.set(buffer, requestChunk);
		} else if (prefix == '-') {
			storeInMemory(buffer, inst, true);
			model.buffers.clear(buffer);
		} else if (prefix == '!') {
			List<String> tokens = new ArrayList<>();
			String s = "";
			int ss = specials.size();
			for (int i = 0; i < ss; i++) {
				String special = specials.get(i);
				if (Symbol.get(special).isVariable()) {
					Symbol value = inst.get(Symbol.get(special));
					special = (value == null) ? "<unbound variable>" : value.getString();
				}
				tokens.add(special);
				s += special + ((i == ss - 1) ? "" : " ");
			}
			if (buffer == Symbol.get("output")) {
				if (s.length() >= 3)
					s = s.substring(2, s.length() - 2);
				if (model.verboseTrace)
					model.output(s);
			} else if (buffer == Symbol.get("bind")) {
				try {
					inst.set(bind, Symbol.get(Utilities.evalCompute(tokens.iterator())));
				} catch (Exception e) {
					inst.set(bind, Symbol.get(Task.bind(tokens.iterator())));
				}
			} else {
				try {
					Utilities.evalCompute(tokens.iterator());
				} catch (Exception e) {
					model.getTask().eval(tokens.iterator());
				}
			}
		}
	}

	void specialize(Symbol variable, Symbol value) {
		for (SlotAction slotAction : slotActions) slotAction.specialize(variable, value);
	}

	void expandDirectAction(Instantiation inst) {
		if (directAction == null)
			return;
		Symbol name = inst.get(directAction);
		Chunk chunk = model.declarative.get(name);
		if (chunk == null)
			chunk = model.buffers.getBufferChunk(name);
		// when used for production compilation, expansion doesn't always have
		// the chunk in the buffer!
		// (particularly for the first production in compilation, which fired
		// previously)
		if (chunk == null)
			return;
		Iterator<Symbol> it = chunk.getSlotNames();
		while (it.hasNext()) {
			Symbol slot = it.next();
			Symbol value = chunk.get(slot);
			slotActions.add(new SlotAction(slot, value, model));
		}
		directAction = null;
	}

	/**
	 * Gets a string representation of the buffer action, with standard
	 * indentation.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "";
		if (directAction == null) {
			s += "   " + prefix + buffer + ">\n";
			for (SlotAction slotAction : slotActions) s += slotAction + "\n";
		} else
			s += "   " + prefix + buffer + "> " + directAction + "\n";
		return s;
	}
}
