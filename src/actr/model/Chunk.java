package actr.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
	private Model model;
	private Symbol name;
	private boolean isRequest;
	private boolean retrieved;
	private double retrievalTime;
	private double lastUsedAsGoal = 0;
	private Map<Symbol, Symbol> slots;
	private List<SlotCondition> requestConditions;
	private double creationTime;
	private int useCount;
	private Vector<Double> uses;
	private int fan;
	private double baseLevel, activation;
	private int threadID;

	/**
	 * Creates a new chunk.
	 * 
	 * @param name
	 * @param model
	 */
	public Chunk(Symbol name, Model model) {
		this.name = name;
		this.model = model;
		slots = new HashMap<Symbol, Symbol>();
		requestConditions = new Vector<SlotCondition>();
		creationTime = model.getTime();
		isRequest = false;
		useCount = 0;
		uses = new Vector<Double>();
		fan = 1;
		baseLevel = 0;
		activation = 0;
		threadID = model.getCurrentThreadID();
		retrieved = false;
		retrievalTime = 0;
	}

	Chunk copy() {
		Chunk c2 = new Chunk(Symbol.getUnique(name.getString()), model);
		Iterator<Symbol> it = slots.keySet().iterator();
		while (it.hasNext()) {
			Symbol slot = it.next();
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
	public Symbol getName() {
		return name;
	}

	/**
	 * Gets the last computed total activation of the chunk.
	 * 
	 * @return the chunk activation
	 */
	public double getActivation() {
		return activation;
	}

	/**
	 * Gets the last computed base-level activation of the chunk.
	 * 
	 * @return the base-level activation
	 */
	public double getBaseLevel() {
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

	void setName(Symbol name) {
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
		if (sym == null)
			return Symbol.nil;
		else
			return sym;
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
		boolean adjustFan = (model.getDeclarative().get(name) != null);

		Symbol oldValue = get(slot);
		if (adjustFan && oldValue != Symbol.nil) {
			Chunk oldValueChunk = model.getDeclarative().get(oldValue);
			if (oldValueChunk != null)
				oldValueChunk.decreaseFan();
		}

		if (value == Symbol.nil && !slot.getString().startsWith(":"))
			slots.remove(slot);
		else {
			slots.put(slot, value);
			if (adjustFan && slot != Symbol.isa && value != Symbol.nil) {
				Chunk valueChunk = model.getDeclarative().get(value);
				if (valueChunk == null) {
					valueChunk = new Chunk(value, model);
					valueChunk = model.getDeclarative().add(valueChunk);
				}
				valueChunk.increaseFan();
			}
		}
	}

	void setCreationTime(double time) {
		creationTime = time;
		useCount = 1;
		uses.clear();
		uses.add(new Double(time));
	}

	void setBaseLevel(double baseLevel) {
		if (!model.getDeclarative().baseLevelLearning)
			this.baseLevel = baseLevel;
		else if (model.getDeclarative().optimizedLearning)
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
	public boolean equals(Chunk c2) {
		if (slotCount() == 0 && c2.slotCount() == 0)
			return (name == c2.getName());
		if (slotCount() != c2.slotCount())
			return false;
		Iterator<Symbol> it = getSlotNames();
		while (it.hasNext()) {
			Symbol slot = it.next();
			Symbol value = get(slot);
			Symbol value2 = c2.get(slot);
			if (value != value2)
				return false;
		}
		return true;
	}

	double computeBaseLevel() {
		if (!model.getDeclarative().baseLevelLearning)
			return baseLevel;
		double time = model.getTime();
		if (time <= creationTime)
			time = creationTime + .001;
		if (model.getDeclarative().optimizedLearning) {
			baseLevel = Math.log(useCount
					/ (1 - model.getDeclarative().baseLevelDecayRate))
					- model.getDeclarative().baseLevelDecayRate
					* Math.log(time - creationTime);
		} else {
			double sum = 0;
			for (int i = 0; i < uses.size(); i++) {
				double use = uses.elementAt(i).doubleValue();
				sum += Math.pow(time - use,
						-model.getDeclarative().baseLevelDecayRate);
			}
			baseLevel = Math.log(sum);
		}
		return baseLevel;
	}

	int appearsInSlotsOf(Chunk c2) {
		int count = 0;
		Iterator<Symbol> it = c2.slots.keySet().iterator();
		while (it.hasNext()) {
			Symbol slot = it.next();
			if (slot != Symbol.get("isa")) {
				Symbol value = c2.get(slot);
				if (value == name)
					count++;
			}
		}
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
		if (cj.appearsInSlotsOf(ci) == 0 && cj.getName() != ci.getName())
			return 0;
		else
			return model.getDeclarative().maximumAssociativeStrength
					- Math.log(cj.fan);
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
			Chunk cj = model.getDeclarative().get(value);
			if (cj == null)
				continue;
			numGoalSlots++;
			if (model.getDeclarative().activationTrace
					&& computeSji(cj, this) != 0)
				model.output("***    spreading activation " + goal.getName()
						+ ": " + cj.getName() + " -> " + this.getName() + " ["
						+ String.format("%.3f", computeSji(cj, this)) + "]");
			sum += computeSji(cj, this);
		}
		double wji = (numGoalSlots == 0) ? 0 : totalW / numGoalSlots;
		return wji * sum;
	}

	double computePartialMatch(Chunk request) {
		double sum = 0;
		Iterator<Symbol> it = request.slots.keySet().iterator();
		while (it.hasNext()) {
			Symbol slot = it.next();
			if (slot == Symbol.isa)
				continue;
			Symbol value = request.get(slot);
			sum += model.getDeclarative().getSimilarity(value, get(slot));
		}
		return model.getDeclarative().mismatchPenalty * sum;
	}

	double computeActivation(Chunk request) {
		activation = computeBaseLevel();
		if (model.getDeclarative().spreadingActivation) {
			if (model.getDeclarative().goalActivation > 0) {
				Chunk goal = model.getBuffers().get(Symbol.goal);
				if (goal != null)
					activation += computeSpreadingActivation(goal,
							model.getDeclarative().goalActivation);
			}
			if (model.getDeclarative().imaginalActivation > 0) {
				Chunk imaginal = model.getBuffers().get(Symbol.imaginal);
				if (imaginal != null)
					activation += computeSpreadingActivation(imaginal,
							model.getDeclarative().imaginalActivation);
			}
		}
		if (model.getDeclarative().partialMatching)
			activation += computePartialMatch(request);
		if (model.getDeclarative().activationNoiseS != 0)
			activation += Utilities
					.getNoise(model.getDeclarative().activationNoiseS);
		return activation;
	}

	int getUseCount() {
		if (model.getDeclarative().optimizedLearning)
			return useCount;
		else
			return uses.size();
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
		if (model.getDeclarative().optimizedLearning)
			useCount++;
		else
			uses.add(new Double(model.getTime()));
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
		Iterator<Symbol> it = slots.keySet().iterator();
		while (it.hasNext()) {
			Symbol slot = it.next();
			if (slot == Symbol.isa)
				continue;
			Symbol value = slots.get(slot);
			s += " " + slot + " " + value;
		}
		return s + ")"; // + " [bl="+getBaseLevel()+"] [fan=" + fan + "]";
	}
}
