package actr.model;

import java.util.*;

/**
 * Chunks of declarative knowledge represented as pairings of slots and slot
 * values.
 * <p>
 * Unlike the classic ACT-R architecture, this implementation does <i>not</i>
 * use chunk types to specify the type of each chunk via a special <tt>isa</tt>
 * slot. Instead, the model can assign a slot value to any slot name (including
 * using <tt>isa</tt> in the traditional way, thus ensuring compatibility with
 * older models).
 * 
 * @author Dario Salvucci
 */
public class Chunk {
	public final Model model;
	private /*final*/ Symbol name;
	private boolean isRequest;
	private boolean retrieved;
	private double retrievalTime;
	private double lastUsedAsGoal = 0;
	private final Map<Symbol, Symbol> slots = new HashMap<>();
	private final List<SlotCondition> requestConditions = new ArrayList<>();
	private double creationTime;
	private int useCount;
	private final List<Double> uses = new ArrayList<>();
	private int fan;
	private double baseLevel, activation;
	@Deprecated private int threadID;

	/**
	 * Creates a new chunk.
	 * 
	 * @param name
	 * @param model
	 */
	public Chunk(Symbol name, Model model) {
		this.name = name;
		this.model = model;
		creationTime = model.getTime();
		isRequest = false;
		useCount = 0;
		fan = 1;
		baseLevel = 0;
		activation = 0;
		threadID = model.getCurrentThreadID();
		retrieved = false;
		retrievalTime = 0;
	}

	Chunk copy() {
		Chunk c2 = new Chunk(Symbol.getUnique(name.getString()), model);
		for (Symbol slot : slots.keySet()) {
			c2.set(slot, get(slot));
		}
		// c2.creationTime = creationTime;
		// c2.request = request;
		// c2.useCount = useCount;
		// c2.uses = (Vector<Double>) uses.clone();
		// c2.fan = fan;
		// c2.baseLevel = baseLevel;
		// c2.activation = activation;
		return c2;
	}

	/**
	 * Gets the name of the chunk.
	 * 
	 * @return the chunk name
	 */
	public Symbol name() {
		return name;
	}

	/**
	 * Gets the last computed total activation of the chunk.
	 * 
	 * @return the chunk activation
	 */
	public double activation() {
		return activation;
	}

	/**
	 * Gets the last computed base-level activation of the chunk.
	 * 
	 * @return the base-level activation
	 */
	public double baseLevel() {
		return baseLevel;
	}

	boolean isRequest() {
		return isRequest;
	}

	boolean isRetrieved() {
		return retrieved;
	}

	double getRetrievalTime() {
		return retrievalTime;
	}

	double getLastUsedAsGoal() {
		return lastUsedAsGoal;
	}

	@Deprecated void setName(Symbol name) {
		this.name = name;
	}

	void setRequest(boolean b) {
		isRequest = b;
	}

	void setRetrieved(boolean b) {
		retrieved = b;
	}

	void setRetrievalTime(double time) {
		retrievalTime = time;
	}

	void setLastUsedAsGoal(double time) {
		lastUsedAsGoal = time;
	}

	/**
	 * Gets the value of the given slot, or <tt>Symbol.nil</tt> if the slot is
	 * undefined.
	 * 
	 * @param slot
	 *            the slot name
	 * @return the slot value
	 */
	public Symbol get(Symbol slot) {
		Symbol sym = slots.get(slot);
		return sym == null ? Symbol.nil : sym;
	}

	/**
	 * Gets the number of slots in the chunk.
	 * 
	 * @return the slot count
	 */
	public int slotCount() {
		return slots.size();
	}

	/**
	 * Gets an iterator to iterate through all slot names.
	 * 
	 * @return an iterator for slot names
	 */
	public Iterator<Symbol> getSlotNames() {
		return slots.keySet().iterator();
	}

	/**
	 * Gets an iterator to iterate through all slot values.
	 * 
	 * @return an iterator for slot values
	 */
	public Iterator<Symbol> getSlotValues() {
		return slots.values().iterator();
	}

	/**
	 * Sets the chunk slot to the given value.
	 * 
	 * @param slot
	 * @param value
	 */
	public void set(Symbol slot, Symbol value) {
		boolean adjustFan = (model.declarative.get(name) != null);

		Symbol oldValue = get(slot);
		if (adjustFan && oldValue != Symbol.nil) {
			Chunk oldValueChunk = model.declarative.get(oldValue);
			if (oldValueChunk != null)
				oldValueChunk.decreaseFan();
		}

		if (value == Symbol.nil && !slot.getString().startsWith(":"))
			slots.remove(slot);
		else {
			slots.put(slot, value);
			if (adjustFan && slot != Symbol.isa && value != Symbol.nil) {
				Chunk valueChunk = model.declarative.get(value);
				if (valueChunk == null) {
					valueChunk = new Chunk(value, model);
					valueChunk = model.declarative.add(valueChunk);
				}
				valueChunk.increaseFan();
			}
		}
	}

	void setCreationTime(double time) {
		creationTime = time;
		useCount = 1;
		uses.clear();
		uses.add(time);
	}

	void setBaseLevel(double baseLevel) {
		if (!model.declarative.baseLevelLearning)
			this.baseLevel = baseLevel;
		else if (model.declarative.optimizedLearning)
			useCount = (int) Math.round(baseLevel);
		else {
			int n = (int) Math.round(baseLevel);
			for (int i = 0; i < n; i++) {
				double frac = 1.0 * i / n;
				uses.add((1.0 - frac) * creationTime + frac * model.getTime());
			}
		}
	}

	/**
	 * Checks if another chunk is equal to this one by checking the equality of
	 * all slots and values.
	 * 
	 * @param c2
	 *            the chunk for comparison
	 * @return <tt>true</tt> if the chunks are equal
	 */
	@Override public boolean equals(Object x) {
		if (this == x)
			return true;
		Chunk c2 = (Chunk)x;
		if (slotCount() == 0 && c2.slotCount() == 0)
			return (name == c2.name());
		if (slotCount() != c2.slotCount())
			return false;
		return slots.equals(c2.slots);
	}

	double computeBaseLevel() {
		if (!model.declarative.baseLevelLearning)
			return baseLevel;
		double time = model.getTime();
		if (time <= creationTime)
			time = creationTime + .001;
		if (model.declarative.optimizedLearning) {
			baseLevel = Math.log(useCount / (1 - model.declarative.baseLevelDecayRate))
					- model.declarative.baseLevelDecayRate * Math.log(time - creationTime);
		} else {
			double sum = 0;
			int n = uses.size();
			for (double use : uses) {
				sum += Math.pow(time - use, -model.declarative.baseLevelDecayRate);
			}
			baseLevel = Math.log(sum);
		}
		return baseLevel;
	}

	int appearsInSlotsOf(Chunk c) {
		int count = 0;
		for (Symbol slot : c.slots.keySet())
			if (slot != Symbol.get("isa") && c.get(slot) == name)
				count++;
		return count;
	}

	void setFan(int f) {
		fan = f;
	}

	void increaseFan() {
		fan++;
	}

	void increaseFan(int df) {
		fan += df;
	}

	void decreaseFan() {
		fan--;
	}

	double computeSji(Chunk cj, Chunk ci) {
		return cj.appearsInSlotsOf(ci) == 0 && cj.name() != ci.name() ? 0 : model.declarative.maximumAssociativeStrength - Math.log(cj.fan);
	}

	double computeSpreadingActivation(Chunk goal, double totalW) {
		double sum = 0;
		int numGoalSlots = 0;
		Iterator<Symbol> it = goal.getSlotNames();
		while (it.hasNext()) {
			Symbol slot = it.next();
			if (slot == Symbol.isa || slot.getString().startsWith(":"))
				continue;
			Symbol value = goal.get(slot);
			// if (value==Symbol.nil || value.isNumber() || value.isString())
			// continue;
			if (value == Symbol.nil)
				continue;
			Chunk cj = model.declarative.get(value);
			if (cj == null)
				continue;
			numGoalSlots++;
			if (model.declarative.activationTrace && computeSji(cj, this) != 0)
				model.output("***    spreading activation " + goal.name() + ": " + cj.name() + " -> "
						+ this.name() + " [" + String.format("%.3f", computeSji(cj, this)) + "]");
			sum += computeSji(cj, this);
		}
		double wji = (numGoalSlots == 0) ? 0 : totalW / numGoalSlots;
		return wji * sum;
	}

	double computePartialMatch(Chunk request) {
		double sum = 0;
		for (Symbol slot : request.slots.keySet()) {
			if (slot == Symbol.isa)
				continue;
			Symbol value = request.get(slot);
			sum += model.declarative.getSimilarity(value, get(slot));
		}
		return model.declarative.mismatchPenalty * sum;
	}

	double computeActivation(Chunk request) {
		activation = computeBaseLevel();
		if (model.declarative.spreadingActivation) {
			if (model.declarative.goalActivation > 0) {
				Chunk goal = model.buffers.get(Symbol.goal);
				if (goal != null)
					activation += computeSpreadingActivation(goal, model.declarative.goalActivation);
			}
			if (model.declarative.imaginalActivation > 0) {
				Chunk imaginal = model.buffers.get(Symbol.imaginal);
				if (imaginal != null)
					activation += computeSpreadingActivation(imaginal, model.declarative.imaginalActivation);
			}
		}
		if (model.declarative.partialMatching)
			activation += computePartialMatch(request);
		if (model.declarative.activationNoiseS != 0)
			activation += Utilities.getNoise(model.declarative.activationNoiseS);
		return activation;
	}

	int getUseCount() {
		return model.declarative.optimizedLearning ? useCount : uses.size();
	}

	/**
	 * Gets the creation time of the chunk, in seconds from simulation onset.
	 * 
	 * @return the creation time
	 */
	public double getCreationTime() {
		return creationTime;
	}

	int getThreadID() {
		return threadID;
	}

	void setThreadID(int id) {
		threadID = id;
	}

	void assignNewThreadID() {
		threadID = model.getNewThreadID();
	}

	void addUse() {
		if (model.declarative.optimizedLearning)
			useCount++;
		else
			uses.add(model.getTime());
	}

	void addRequestCondition(SlotCondition condition) {
		requestConditions.add(condition);
	}

	Iterator<SlotCondition> getRequestConditions() {
		return requestConditions.iterator();
	}

	/**
	 * Gets a string representation of the chunk showing its slot names and
	 * values.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "(" + name;
		Symbol isa = get(Symbol.isa);
		if (isa != null && isa != Symbol.nil)
			s += " isa " + isa;
		for (Map.Entry<Symbol, Symbol> entry : slots.entrySet()) {
			Symbol slot = entry.getKey();
			if (slot == Symbol.isa)
				continue;
			Symbol value = entry.getValue();
			s += " " + slot + " " + value;
		}
		return s + ")"; // + " [bl="+getBaseLevel()+"] [fan=" + fan + "]";
	}
}
