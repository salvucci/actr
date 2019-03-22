package actr.model;

import java.util.Iterator;
import java.util.Vector;

/**
 * Condition-action production rules that serve as the primary representation of
 * cognitive skill.
 * 
 * @author Dario Salvucci
 */
public class Production {
	private Symbol name;
	private Model model;
	private Vector<BufferCondition> conditions;
	private Vector<BufferAction> actions;
	private double u;
	private double p = 1; // the probability that the goal will achieve with the current this produciton 
	private boolean hasReward = false;
	private double reward = 0;
	private boolean breakPoint = false;
	private int timesFired = 0;
	private boolean constantUtility = false;

	Production(Symbol name, Model model) {
		this.name = name;
		this.model = model;
		conditions = new Vector<BufferCondition>();
		actions = new Vector<BufferAction>();
		u = model.getProcedural().initialUtility;
	}

	Production copy() {
		Production p = new Production(Symbol.getUnique(name.getString()), model);
		for (int i = 0; i < conditions.size(); i++)
			p.conditions.add(conditions.elementAt(i).copy());
		for (int i = 0; i < actions.size(); i++)
			p.actions.add(actions.elementAt(i).copy());
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
	public boolean equals(Production p2) {
		if (conditions.size() != p2.conditions.size())
			return false;
		for (int i = 0; i < conditions.size(); i++)
			if (!conditions.elementAt(i).equals(p2.conditions.elementAt(i)))
				return false;
		if (actions.size() != p2.actions.size())
			return false;
		for (int i = 0; i < actions.size(); i++)
			if (!actions.elementAt(i).equals(p2.actions.elementAt(i)))
				return false;
		return true;
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
	public double getUtility() { return u; }
	public double getProbability() { return p; }
	
	void setName (Symbol name) { this.name = name; }
	void setUtility (double x) { u = x; }
	void setProbability (double x) { p = x; } 
	
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

	void setParameter (String parameter, String value, Tokenizer t) throws Exception
	{
		if (parameter.equals(":u")) {
			u = Double.valueOf(value);
			constantUtility = true;
		}
		else if (parameter.equals(":p")) p = Double.valueOf(value);
		else if (parameter.equals(":reward"))
		{
			hasReward = true;
			reward = Double.valueOf(value);
		}
		else if (parameter.equals(":break")) breakPoint = !value.equals("nil");
		else model.recordWarning ("unknown production parameter "+parameter, t);
	}


	Iterator<BufferCondition> getConditions() {
		return conditions.iterator();
	}

	BufferCondition getBufferCondition(Symbol buffer) {
		for (int i = 0; i < conditions.size(); i++)
			if (conditions.elementAt(i).getBuffer() == buffer)
				return conditions.elementAt(i);
		return null;
	}
	boolean hasBufferCondition(Symbol buffer) {
		return getBufferCondition(buffer) != null;
	}

	SlotCondition getSlotCondition(Symbol buffer, Symbol slot) {
		BufferCondition bc = getBufferCondition(buffer);
		if (bc != null)
			return bc.getSlotCondition(slot);
		else
			return null;
	}

	Iterator<BufferAction> getActions() {
		return actions.iterator();
	}

	BufferAction getBufferAction(Symbol buffer) {
		for (int i = 0; i < actions.size(); i++)
			if (actions.elementAt(i).getBuffer() == buffer)
				return actions.elementAt(i);
		return null;
	}

	boolean hasBufferAction(Symbol buffer) {
		return getBufferAction(buffer) != null;
	}

	SlotAction getSlotAction(Symbol buffer, Symbol slot) {
		BufferAction ba = getBufferAction(buffer);
		if (ba != null)
			return ba.getSlotAction(slot);
		else
			return null;
	}

	BufferAction getBufferAction(char prefix, Symbol buffer) {
		for (int i = 0; i < actions.size(); i++) {
			BufferAction ba = actions.elementAt(i);
			if (ba.getBuffer() == buffer && ba.getPrefix() == prefix)
				return ba;
		}
		return null;
	}

	boolean hasBufferAction(char prefix, Symbol buffer) {
		return getBufferAction(prefix, buffer) != null;
	}

	boolean hasSpecials() {
		for (int i = 0; i < conditions.size(); i++) {
			BufferCondition bc = conditions.elementAt(i);
			if (bc.isSpecial())
				return true;
		}
		for (int i = 0; i < actions.size(); i++) {
			BufferAction ba = actions.elementAt(i);
			if (ba.isDirect() || ba.isSpecial())
				return true;
		}
		return false;
	}

	boolean hasConditionSlotValue(Symbol value) {
		for (int i = 0; i < conditions.size(); i++)
			if (conditions.get(i).hasSlotValue(value))
				return true;
		return false;
	}

	boolean hasActionSlotValue(Symbol value) {
		for (int i = 0; i < actions.size(); i++)
			if (actions.get(i).hasSlotValue(value))
				return true;
		return false;
	}

	boolean hasSlotValue(Symbol value) {
		return hasConditionSlotValue(value) || hasActionSlotValue(value);
	}

	Vector<Symbol> getConditionVariables() {
		Vector<Symbol> variables = new Vector<Symbol>();
		Iterator<BufferCondition> it = getConditions();
		while (it.hasNext()) {
			BufferCondition bc = it.next();
			if (bc.getPrefix() == '=')
				variables.add(Symbol.get("=" + bc.getBuffer()));
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
		Vector<Symbol> variables = new Vector<Symbol>();
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

	Vector<Symbol> getVariables() {
		Vector<Symbol> cvs = getConditionVariables();
		Vector<Symbol> avs = getActionVariables();
		for (int i = 0; i < avs.size(); i++)
			if (!cvs.contains(avs.elementAt(i)))
				cvs.add(avs.elementAt(i));
		return cvs;
	}

	boolean queriesOtherThanBusy(Symbol buffer) {
		BufferCondition bc = getBufferCondition(buffer);
		if (bc == null)
			return false;
		if (bc.slotCount() == 0)
			return false;
		SlotCondition state = bc.getSlotCondition(Symbol.state);
		if (state == null)
			return true;
		return (state.getValue() != Symbol.busy);
	}

	boolean queriesForError(Symbol buffer) {
		BufferCondition bc = getBufferCondition(buffer);
		if (bc == null)
			return false;
		SlotCondition state = bc.getSlotCondition(Symbol.state);
		if (state == null)
			return false;
		return (state.getValue() == Symbol.error);
	}

	Instantiation instantiate(Buffers buffers) {
		boolean savedWhyNotTrace = model.getProcedural().whyNotTrace;
		SlotCondition prodGoalSC = getSlotCondition(Symbol.goal, Symbol.isa);
		Symbol prodGoalType = (prodGoalSC != null) ? prodGoalSC.getValue() : null;
		Symbol bufGoalType = buffers.getSlot(Symbol.goal, Symbol.isa);
		if (prodGoalType != bufGoalType && !(prodGoalType == null || prodGoalType.isVariable()))
			model.getProcedural().whyNotTrace = false;

		if (model.getProcedural().whyNotTrace)
			model.output(name.getString());
		
		/* 
		 * putting the fatigue (alertness) inside the unstU (instantiation utility). Subtract the cognitive cycle
		 */
		double instU;
		double noise = Utilities.getNoise(model.getProcedural().utilityNoiseS);
		if (model.getFatigue().isFatigueEnabled() && !constantUtility){
			//Fatigue old version: just the affect of time on task 
//			instU  = u * model.getFatigue().getFatigueFP() +  Utilities.getNoise(model.getProcedural().utilityNoiseS); // original  
			
			// Fatigue new model with the additive factor
			double BioMath = model.getFatigue().computeBioMathValue();
			double FPMC0 = model.getFatigue().getFatigueFPMC0();
			double FPMC = model.getFatigue().getFatigueFPMC();
			double FPBMC = model.getFatigue().getFatigueFPBMC();			
			instU  = model.getFatigue().getFatigueFPPercent() * ( 
					Math.pow(1 + model.getFatigue().mpTime(), -(FPMC + FPMC0*BioMath) ) + u  -  (FPBMC * BioMath)  +  noise) ;
//			model.output(name.toString() + " U:" + (Math.pow(1 + model.getFatigue().mpTime(), -(FPMC + FPMC0*BioMath) ) + u  -  (FPBMC * BioMath)) 
//					+ " noise:" + noise + " dec" + model.getFatigue().getFatigueFPPercent());
		}
		else if (model.getFatigue().isFatigueEnabled() && constantUtility){
			instU = model.getFatigue().getFatigueFPPercent() *(u + noise);
//			model.output(name.toString() + " U:" + u + " noise:" + noise + " dec" + model.getFatigue().getFatigueFPPercent());
		}
		else{
			instU = u + noise;
//			model.output(name.toString() + "U:" + u + " noise:" + noise + " dec" + model.getFatigue().getFatigueFPPercent());
		}
		//System.out.println("u  ::: " + instU + "----" + name);

		Instantiation inst = new Instantiation (this, model.getTime(), instU);

		boolean fatigueMismatch = false;
		
		for (int i = 0; i < conditions.size(); i++) {
			BufferCondition bc = conditions.elementAt(i);
			if (!bc.test(inst)) {
				if (model.getFatigue().isFatigueEnabled() && model.getFatigue().isFatiguePartialMatching()) {
					Chunk bufferChunk = model.getBuffers().get(bc.getBuffer());
					if (bufferChunk == null)
						return null;
					else
						fatigueMismatch = true;
				} else {
					if (model.getProcedural().whyNotTrace)
						model.output("   X instantiation failed\n");
					model.getProcedural().whyNotTrace = savedWhyNotTrace;
					return null;
				}
			}
		}

		Iterator<Instantiation.DelayedSlotCondition> it = inst.getDelayedSlotConditions();
		while (it.hasNext()) {
			Instantiation.DelayedSlotCondition dsc = it.next();
			if (model.getProcedural().whyNotTrace)
				model.output("   [delayed] " + dsc.buffer + ">");
			if (!dsc.slotCondition.test(dsc.buffer, dsc.bufferChunk, inst)) {
				if (model.getFatigue().isFatigueEnabled() && model.getFatigue().isFatiguePartialMatching()) {
					fatigueMismatch = true;
				} else {
					if (model.getProcedural().whyNotTrace)
						model.output("   X instantiation failed\n");
					model.getProcedural().whyNotTrace = savedWhyNotTrace;
					return null;
				}
			}
		}

		if (fatigueMismatch) {
			// if the production has not matched but fatigue partial matching is on,
			// set instantiation utility to 0 (see Walsh, Gunzelmann, & Van Dongen, 2017)
			inst.setUtility(model.getFatigue().getFatigueFPPercent() * (0 + noise));
		}

		if (model.getProcedural().whyNotTrace)
			model.output("   * instantiation succeeded: " + inst + "\n");
		model.getProcedural().whyNotTrace = savedWhyNotTrace;
		return inst;
	}

	void fire(Instantiation inst) {
		Chunk goal = model.getBuffers().get(Symbol.goal);
		if (goal != null)
			goal.setLastUsedAsGoal(model.getTime());

		for (int i = 0; i < conditions.size(); i++) {
			BufferCondition bc = conditions.elementAt(i);
			if (bc.getPrefix() == '=' && bc.getBuffer() != Symbol.goal && bc.getBuffer() != Symbol.temporal
			// && bc.getBuffer()!=Symbol.imaginal
			) {
				Symbol buffer = bc.getBuffer();
				boolean found = false;
				for (int j = 0; j < actions.size(); j++)
					if (actions.elementAt(j).getBuffer() == buffer)
						found = true;
				if (!found)
					model.getBuffers().clear(bc.getBuffer());
				else
					model.getBuffers().touch(bc.getBuffer());
			}
		}
		for (int i = 0; i < actions.size(); i++) {
			BufferAction ba = actions.elementAt(i);
			ba.fire(inst);
		}

		timesFired++;
	}

	void specialize(Symbol variable, Symbol value) {
		if (value == null) {
			model.outputError("cannot specialize " + variable + " to null value");
			return;
		}
		for (int i = 0; i < conditions.size(); i++)
			conditions.elementAt(i).specialize(variable, value);
		for (int i = 0; i < actions.size(); i++)
			actions.elementAt(i).specialize(variable, value);
	}

	void expandDirectActions(Instantiation inst) {
		for (int i = 0; i < actions.size(); i++)
			actions.elementAt(i).expandDirectAction(inst);
	}

	/**
	 * Gets a string representation of the production with an instantiation.
	 * 
	 * @return the string
	 */
	public String toString(Instantiation inst) {
		Vector<Symbol> used = new Vector<Symbol>();
		String s = "(p " + name + "\n";
		for (int i = 0; i < conditions.size(); i++)
			s += conditions.elementAt(i).toString(inst, used);
		s += "==>\n";
		for (int i = 0; i < actions.size(); i++)
			s += actions.elementAt(i);
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
