package actr.model;

/**
 * Action performed for a single slot within a buffer action.
 * 
 * @author Dario Salvucci
 */
class SlotAction {
	private Model model;
	private Symbol slot;
	private Symbol value;
	private String operator;

	SlotAction(String operator, Symbol slot, Symbol value, Model model) {
		this.operator = operator;
		this.slot = slot;
		this.value = value;
		this.model = model;
	}

	SlotAction(Symbol slot, Symbol value, Model model) {
		this(null, slot, value, model);
	}

	SlotAction copy() {
		return new SlotAction(operator, slot, value, model);
	}

	public boolean equals(SlotAction sa2) {
		return (slot == sa2.slot && value == sa2.value);
	}

	public String getOperator() {
		return operator;
	}

	public Symbol getSlot() {
		return slot;
	}

	public Symbol getValue() {
		return value;
	}

	void fire(Instantiation inst, Chunk bufferChunk) {
		Symbol realSlot = (slot.isVariable()) ? inst.get(slot) : slot;
		if (realSlot == null)
			return;
		Symbol realValue = (value.isVariable()) ? inst.get(value) : value;
		if (operator == null)
			bufferChunk.set(realSlot, realValue);
		else
			bufferChunk.addRequestCondition(new SlotCondition(operator,
					realSlot, realValue, model));
	}

	void specialize(Symbol variable, Symbol instvalue) {
		if (slot == variable)
			slot = instvalue;
		if (value == variable)
			value = instvalue;
	}

	/**
	 * Gets a string representation of the slot action, with standard
	 * indentation.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return (operator != null ? operator : "") + slot + " " + value;
	}
}
