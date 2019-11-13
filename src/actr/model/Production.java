package actr.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Condition-action production rules that serve as the primary representation of
 * cognitive skill.
 * 
 * @author Dario Salvucci
 */
public class Production {
	private Symbol name;
	private final Model model;
	private final List<BufferCondition> conditions = new ArrayList();
	private final List<BufferAction> actions = new ArrayList();
	private double u;
	private boolean hasReward = false;
	private double reward = 0;
	private boolean breakPoint = false;
	private int timesFired = 0;

	Production(Symbol name, Model model) {
		this.name = name;
		this.model = model;
		u = model.procedural.initialUtility;
	}

	Production copy() {
		Production p = new Production(Symbol.getUnique(name.getString()), model);
		for (int i = 0; i < conditions.size(); i++)
			p.conditions.add(conditions.get(i).copy());
		for (int i = 0; i < actions.size(); i++)
			p.actions.add(actions.get(i).copy());
		return p;
	}

	/**
	 * Checks recursively whether two productions are the same.
	 * 
	 * @param p2
	 *            the second production
	 * @return <tt>true</tt> if the two productions are the same, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean equals(Object x) {
		if (this == x) return true;
		Production p2 = (Production) x;
		if (conditions.size() != p2.conditions.size())
			return false;
		if (actions.size() != p2.actions.size())
			return false;
		return conditions.equals(p2.conditions) && actions.equals(p2.actions);
	}

	void addBufferCondition(BufferCondition bc) {
		conditions.add(bc);
	}

	void addBufferAction(BufferAction ac) {
		actions.add(ac);
	}

	/**
	 * Gets the name of the production.
	 * 
	 * @return the name symbol
	 */
	public Symbol getName() {
		return name;
	}

	/**
	 * Gets the utility of the production.
	 * 
	 * @return the utility value
	 */
	public double getUtility() {
		return u;
	}

	void setName(Symbol name) {
		this.name = name;
	}

	void setUtility(double x) {
		u = x;
	}

	/**
	 * Checks whether this production has an associated reward.
	 * 
	 * @return <tt>true</tt> if the production has a reward, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean hasReward() {
		return hasReward;
	}

	/**
	 * Gets the reward associated with this production.
	 * 
	 * @return the reward value
	 */
	public double getReward() {
		return reward;
	}

	void setReward(double reward) {
		this.reward = reward;
		hasReward = true;
	}

	void clearReward() {
		reward = 0;
		hasReward = false;
	}

	boolean isBreakPoint() {
		return breakPoint;
	}

	/**
	 * Gets the number of times the production has fired.
	 * 
	 * @return the number of times fired
	 */
	public int getTimesFired() {
		return timesFired;
	}

	void setParameter(String parameter, String value, Tokenizer t) {
		switch (parameter) {
			case ":u":
				u = Double.parseDouble(value);
				break;
			case ":reward":
				hasReward = true;
				reward = Double.parseDouble(value);
				break;
			case ":break":
				breakPoint = !value.equals("nil");
				break;
			default:
				model.recordWarning("unknown production parameter " + parameter, t);
				break;
		}
	}

	Iterator<BufferCondition> getConditions() {
		return conditions.iterator();
	}

	BufferCondition getBufferCondition(Symbol buffer) {
		for (BufferCondition condition : conditions)
			if (condition.buffer == buffer)
				return condition;
		return null;
	}

	boolean hasBufferCondition(Symbol buffer) {
		return getBufferCondition(buffer) != null;
	}

	SlotCondition getSlotCondition(Symbol buffer, Symbol slot) {
		BufferCondition bc = getBufferCondition(buffer);
		return bc != null ? bc.getSlotCondition(slot) : null;
	}

	Iterator<BufferAction> getActions() {
		return actions.iterator();
	}

	BufferAction getBufferAction(Symbol buffer) {
		for (BufferAction action : actions)
			if (action.buffer == buffer)
				return action;
		return null;
	}

	boolean hasBufferAction(Symbol buffer) {
		return getBufferAction(buffer) != null;
	}

	SlotAction getSlotAction(Symbol buffer, Symbol slot) {
		BufferAction ba = getBufferAction(buffer);
		return ba != null ? ba.getSlotAction(slot) : null;
	}

	BufferAction getBufferAction(char prefix, Symbol buffer) {
		for (BufferAction ba : actions) {
			if (ba.buffer == buffer && ba.getPrefix() == prefix)
				return ba;
		}
		return null;
	}

	boolean hasBufferAction(char prefix, Symbol buffer) {
		return getBufferAction(prefix, buffer) != null;
	}

	boolean hasSpecials() {
		for (BufferCondition bc : conditions) {
			if (bc.isSpecial())
				return true;
		}
		for (BufferAction ba : actions) {
			if (ba.isDirect() || ba.isSpecial())
				return true;
		}
		return false;
	}

	boolean hasConditionSlotValue(Symbol value) {
		for (BufferCondition condition : conditions)
			if (condition.hasSlotValue(value))
				return true;
		return false;
	}

	boolean hasActionSlotValue(Symbol value) {
		for (BufferAction action : actions)
			if (action.hasSlotValue(value))
				return true;
		return false;
	}

	boolean hasSlotValue(Symbol value) {
		return hasConditionSlotValue(value) || hasActionSlotValue(value);
	}

	List<Symbol> getConditionVariables() {
		List<Symbol> variables = new Vector<>();
		Iterator<BufferCondition> it = getConditions();
		while (it.hasNext()) {
			BufferCondition bc = it.next();
			if (bc.getPrefix() == '=')
				variables.add(Symbol.get("=" + bc.buffer));
			Iterator<SlotCondition> itBC = bc.getSlotConditions();
			while (itBC.hasNext()) {
				SlotCondition sc = itBC.next();
				if (sc.getSlot().isVariable())
					variables.add(sc.getSlot());
				if (sc.getValue().isVariable())
					variables.add(sc.getValue());
			}
		}
		return variables;
	}

	Vector<Symbol> getActionVariables() {
		Vector<Symbol> variables = new Vector<>();
		Iterator<BufferAction> it = getActions();
		while (it.hasNext()) {
			BufferAction ba = it.next();
			Iterator<SlotAction> itBA = ba.getSlotActions();
			while (itBA.hasNext()) {
				SlotAction sa = itBA.next();
				if (sa.getSlot().isVariable())
					variables.add(sa.getSlot());
				if (sa.getValue().isVariable())
					variables.add(sa.getValue());
			}
		}
		return variables;
	}

	List<Symbol> getVariables() {
		List<Symbol> cvs = getConditionVariables();
		List<Symbol> avs = getActionVariables();
		for (Symbol ai : avs) {
			if (!cvs.contains(ai))
				cvs.add(ai);
		}
		return cvs;
	}

	boolean queriesOtherThanBusy(Symbol buffer) {
		BufferCondition bc = getBufferCondition(buffer);
		if (bc == null)
			return false;
		if (bc.slotCount() == 0)
			return false;
		SlotCondition state = bc.getSlotCondition(Symbol.state);
		return state == null || state.getValue() != Symbol.busy;
	}

	boolean queriesForError(Symbol buffer) {
		BufferCondition bc = getBufferCondition(buffer);
		if (bc == null)
			return false;
		SlotCondition state = bc.getSlotCondition(Symbol.state);
		return state != null && state.getValue() == Symbol.error;
	}

	Instantiation instantiate(Buffers buffers) {
		boolean savedWhyNotTrace = model.procedural.whyNotTrace;
		SlotCondition prodGoalSC = getSlotCondition(Symbol.goal, Symbol.isa);
		Symbol prodGoalType = (prodGoalSC != null) ? prodGoalSC.getValue() : null;
		Symbol bufGoalType = buffers.getSlot(Symbol.goal, Symbol.isa);
		if (prodGoalType != bufGoalType && !(prodGoalType == null || prodGoalType.isVariable()))
			model.procedural.whyNotTrace = false;

		if (model.procedural.whyNotTrace)
			model.output(name.getString());
		double instU = u + Utilities.getNoise(model.procedural.utilityNoiseS);
		Instantiation inst = new Instantiation(this, model.getTime(), instU);

		for (BufferCondition bc : conditions) {
			if (!bc.test(inst)) {
				if (model.procedural.whyNotTrace)
					model.output("   X instantiation failed\n");
				model.procedural.whyNotTrace = savedWhyNotTrace;
				return null;
			}
		}

		for (Instantiation.DelayedSlotCondition dsc : inst.getDelayedSlotConditions()) {
			if (model.procedural.whyNotTrace)
				model.output("   [delayed] " + dsc.buffer + ">");
			if (!dsc.slotCondition.test(dsc.buffer, dsc.bufferChunk, inst)) {
				if (model.procedural.whyNotTrace)
					model.output("   X instantiation failed\n");
				model.procedural.whyNotTrace = savedWhyNotTrace;
				return null;
			}
		}

		if (model.procedural.whyNotTrace)
			model.output("   * instantiation succeeded: " + inst + "\n");
		model.procedural.whyNotTrace = savedWhyNotTrace;
		return inst;
	}

	void fire(Instantiation inst) {
		Chunk goal = model.buffers.get(Symbol.goal);
		if (goal != null)
			goal.setLastUsedAsGoal(model.getTime());

		for (BufferCondition bc : conditions) {
			// && bc.getBuffer()!=Symbol.imaginal
			if (bc.getPrefix() == '=') {
				Symbol bb = bc.buffer;
				if (bb != Symbol.goal && bb != Symbol.temporal) {
					boolean found = false;
					for (BufferAction action : actions)
						if (action.buffer == bb) {
							found = true;
							break;
						}
					if (!found)
						model.buffers.clear(bb);
					else
						model.buffers.touch(bb);
				}
			}
		}
		for (BufferAction ba : actions) {
			ba.fire(inst);
		}

		timesFired++;
	}

	void specialize(Symbol variable, Symbol value) {
		if (value == null) {
			model.outputError("cannot specialize " + variable + " to null value");
			return;
		}
		for (BufferCondition condition : conditions) condition.specialize(variable, value);
		for (BufferAction action : actions) action.specialize(variable, value);
	}

	void expandDirectActions(Instantiation inst) {
		for (BufferAction action : actions) action.expandDirectAction(inst);
	}

	/**
	 * Gets a string representation of the production with an instantiation.
	 * 
	 * @return the string
	 */
	public String toString(Instantiation inst) {
		List<Symbol> used = new ArrayList<>();
		String s = "(p " + name + "\n";
		for (BufferCondition condition : conditions) s += condition.toString(inst, used);
		s += "==>\n";
		for (BufferAction action : actions) s += action;
		s += String.format(") [u: %.3f]\n", getUtility());
		return s;
	}

	/**
	 * Gets a string representation of the production, with standard
	 * indentation.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return toString(null);
	}
}
