package actr.model;

import java.util.*;


/**
 * An instantiation of a production rule specified as a mapping from variables
 * to values.
 * 
 * @author Dario Salvucci
 */
public class Instantiation {
	public final Production production;
	public final double time;
	private double u;
	private final Map<Symbol, Symbol> mapping;
	public final List<DelayedSlotCondition> delayedSlotConditions;
	private int threadID = 0;

	static class DelayedSlotCondition {
		Symbol buffer;
		SlotCondition slotCondition;
		Chunk bufferChunk;
	}

	Instantiation(Production production, double time, double u) {
		this.production = production;
		this.time = time;
		this.u = u;
		mapping = new HashMap<>(0);
		delayedSlotConditions = new ArrayList<>();
	}

	Instantiation copy() {
		Instantiation newi = new Instantiation(production, time, u);
		newi.mapping.putAll(mapping);
		return newi;
	}

	void set(Symbol variable, Symbol chunk) {
		mapping.put(variable, chunk);
	}

	/**
	 * Gets the size of the instantiation (as the number of variables).
	 * 
	 * @return the instantiation size
	 */
	public int size() {
		return mapping.size();
	}

	/**
	 * Gets the value of the given variable.
	 * 
	 * @param variable
	 *            the variable
	 * @return the value as a symbol (which can be retrieved from declarative
	 *         memory for the full chunk if desired)
	 */
	public Symbol get(Symbol variable) {
		return mapping.get(variable);
	}

	void setThreadID(int threadID) {
		this.threadID = threadID;
	}

	int getThreadID() {
		return threadID;
	}

	/**
	 * Gets an iterator for all the variables in the instantiation.
	 * 
	 * @return an iterator for all variables
	 */
	public Iterator<Symbol> getVariables() {
		return mapping.keySet().iterator();
	}

	void replaceVariable(Symbol var, Symbol var2) {
		Symbol previousValue = mapping.remove(var);
		if (previousValue != null)
			set(var2, previousValue);
	}

	void replaceValue(Symbol from, Symbol to) {
		for (Map.Entry<Symbol, Symbol> variable : mapping.entrySet()) {
			if (variable.getValue() == from)
				variable.setValue(to);
		}
	}

	/**
	 * Gets the utility value of this instantiation.
	 * 
	 * @return the utility value
	 */
	public double getUtility() {
		return u;
	}
	
	void setUtility(double val) {
		u = val;
	}

	void addDelayedSlotCondition(Symbol buffer, SlotCondition sc, Chunk bufferChunk) {
		DelayedSlotCondition dsc = new DelayedSlotCondition();
		dsc.buffer = buffer;
		dsc.slotCondition = sc;
		dsc.bufferChunk = bufferChunk;
		delayedSlotConditions.add(dsc);
	}

	/**
	 * Gets a string representation of the instantiation as the mapping of
	 * variables to values.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "<inst " + production.name + " ";
		for (Map.Entry<Symbol,Symbol> v : mapping.entrySet()) {
			s += " " + v.getKey() + "->" + v.getValue();
		}
		return s + ">";
	}
}
