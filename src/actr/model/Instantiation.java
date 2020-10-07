package actr.model;

import java.util.*;

/**
 * An instantiation of a production rule specified as a mapping from variables
 * to values.
 * 
 * @author Dario Salvucci
 */
public class Instantiation {
	private final Production p;
	private final double time;
	private final double u;
	private final Map<Symbol, Symbol> mapping;
	private final List<DelayedSlotCondition> delayedSlotConditions;
	private int threadID = 0;

	static class DelayedSlotCondition {
		Symbol buffer;
		SlotCondition slotCondition;
		Chunk bufferChunk;
	}

	Instantiation(Production p, double time, double u) {
		this.p = p;
		this.time = time;
		this.u = u;
		mapping = new HashMap<>();
		delayedSlotConditions = new ArrayList<>();
	}

	Instantiation copy() {
		Instantiation newi = new Instantiation(p, time, u);
		for (Symbol variable : mapping.keySet()) {
			Symbol chunk = get(variable);
			newi.set(variable, chunk);
		}
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

	void replaceValue(Symbol value1, Symbol value2) {
		for (Symbol variable : mapping.keySet()) {
			Symbol chunk = get(variable);
			if (chunk == value1)
				set(variable, value2);
		}
	}

	/**
	 * Gets the production associated with this instantiation.
	 * 
	 * @return the production
	 */
	public Production getProduction() {
		return p;
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

	/**
	 * Gets the time at which this instantiation fires.
	 * 
	 * @return the time in seconds after the start of simulation
	 */
	public double getTime() {
		return time;
	}

	void addDelayedSlotCondition(Symbol buffer, SlotCondition sc, Chunk bufferChunk) {
		DelayedSlotCondition dsc = new DelayedSlotCondition();
		dsc.buffer = buffer;
		dsc.slotCondition = sc;
		dsc.bufferChunk = bufferChunk;
		delayedSlotConditions.add(dsc);
	}

	Iterable<DelayedSlotCondition> getDelayedSlotConditions() {
		return delayedSlotConditions;
	}

	/**
	 * Gets a string representation of the instantiation as the mapping of
	 * variables to values.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "<inst " + p.getName() + " ";
		for (Symbol v : mapping.keySet()) {
			Symbol c = get(v);
			s += " " + v + "->" + c;
		}
		return s + ">";
	}
}
