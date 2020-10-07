package actr.model;

import java.util.List;
import java.util.Objects;

/**
 * Condition tested for a single slot within a buffer condition.
 * 
 * @author Dario Salvucci
 */
public class SlotCondition {
	private final Model model;
	private Symbol slot;
	private Symbol value;
	private final String operator;

	SlotCondition(String operator, Symbol slot, Symbol value, Model model) {
		this.operator = operator;
		this.slot = slot;
		this.value = value;
		this.model = model;
	}

	SlotCondition copy() {
		return new SlotCondition(operator, slot, value, model);
	}

	@Override public boolean equals(Object x) {
		if (this == x) return true;
		SlotCondition sc2 = (SlotCondition) x;
		return (Objects.equals(operator, sc2.operator)) && slot == sc2.slot && value == sc2.value;
	}

	public Symbol getSlot() {
		return slot;
	}

	public Symbol getValue() {
		return value;
	}

	public String getOperator() {
		return operator;
	}

	boolean test(Symbol buffer, Chunk bufferChunk, Instantiation inst) {
		boolean negated = (operator != null && operator.equals("-"));

		Symbol realSlot = (slot.isVariable()) ? inst.get(slot) : slot;
		if (realSlot == null) {
			// slot variable isn't defined yet, so save it in the instantiation
			// to check later
			inst.addDelayedSlotCondition(buffer, this, bufferChunk);
			return true; // negated; // false;
		}

		Symbol bufferValue = bufferChunk.get(realSlot);
		if (bufferValue == null)
			return negated; // false;
		Symbol testValue = value;
		if (testValue.isVariable()) {
			if (bufferValue == Symbol.nil)
				return negated; // false;
			testValue = inst.get(testValue);
			if (testValue == null) {
				inst.set(value, bufferValue);
				return true;
			}
		}

		if (operator == null)
			return (testValue == bufferValue) || model.declarative.isa(bufferValue, testValue);
		else if (operator.equals("-"))
			return (testValue != bufferValue) && !model.declarative.isa(bufferValue, testValue);
		else {
			double bufferNumber = Double.parseDouble(bufferValue.getString());
			double testNumber = Double.parseDouble(testValue.getString());
			switch (operator) {
				case "<":
					return (bufferNumber < testNumber);
				case ">":
					return (bufferNumber > testNumber);
				case "<=":
					return (bufferNumber <= testNumber);
				case ">=":
					return (bufferNumber >= testNumber);
			}
		}
		return false;
	}

	void specialize(Symbol variable, Symbol instvalue) {
		if (slot == variable)
			slot = instvalue;
		if (value == variable)
			value = instvalue;
	}

	/**
	 * Gets a string representation of the slot condition with an instantiation
	 * if provided.
	 * 
	 * @return the string
	 */
	public String toString(Instantiation inst, List<Symbol> used) {
		String s = "      ";
		if (operator != null)
			s += operator + " ";
		s += slot + " " + value;
		if (inst != null && value.isVariable() && (used == null || !used.contains(value))) {
			Symbol v = inst.get(value);
			if (v != null) {
				s = String.format("%-35s", s);
				s += "[" + value + " <- " + v + "]";
			}
			if (used != null)
				used.add(value);
		}
		return s;
	}

	/**
	 * Gets a string representation of the slot condition, with standard
	 * indentation.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return toString(null, null);
	}
}
