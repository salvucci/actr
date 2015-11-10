package actr.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Procedural memory that holds skill knowledge represented as production rules.
 * 
 * @author Dario Salvucci
 */
public class Procedural extends Module {
	private Model model;
	private Map<Symbol, Production> productions;
	private Vector<Instantiation> rewardFirings;
	private Instantiation lastFiredInst;
	private Map<Integer, Instantiation> lastFiredOnThread;

	double actionTime = .050;
	boolean variableProductionFiringTime = false;

	boolean utilityUseThreshold = false;
	double utilityThreshold = 0;
	boolean utilityLearning = false;
	double utilityNoiseS = 0;
	double utilityLearningAlpha = 0.2;
	double initialUtility = 0;
	boolean productionLearning = false;
	double productionCompilationThresholdTime = 2.0;
	double productionCompilationNewUtility = 0;
	boolean productionCompilationAddUtilities = false;
	boolean productionCompilationThreaded = true;
	double fatigueUtilityThreshold = 0;
	double fatigueUtility = 0;

	boolean conflictSetTrace = false;
	boolean whyNotTrace = false;
	boolean productionCompilationTrace = false;
	boolean threadedCognitionTrace = false;

	Procedural(Model model) {
		this.model = model;
		productions = new HashMap<Symbol, Production>();
		rewardFirings = new Vector<Instantiation>();
		lastFiredInst = null;
		lastFiredOnThread = new HashMap<Integer, Instantiation>();
	}

	void add(Production p) {
		productions.put(p.getName(), p);
	}

	/**
	 * Gets the production with the given name.
	 * 
	 * @param name
	 *            the name symbol
	 * @return the production, or <tt>null</tt> if it does not exist
	 */
	public Production get(Symbol name) {
		return productions.get(name);
	}

	/**
	 * Gets the production iterator for all productions.
	 * 
	 * @return the iterator
	 */
	public Iterator<Production> getProductions() {
		return productions.values().iterator();
	}

	/**
	 * Gets the size of procedural memory as the number of productions
	 * 
	 * @return the number of productions
	 */
	public int size() {
		return productions.size();
	}

	Production exists(Production p) {
		Iterator<Production> it = productions.values().iterator();
		while (it.hasNext()) {
			Production ip = it.next();
			if (ip.equals(p))
				return ip;
		}
		return null;
	}

	/**
	 * Gets the last production that fired in the simulation.
	 * 
	 * @return the last production to fire
	 */
	public Production getLastProductionFired() {
		return lastFiredInst.getProduction();
	}

	public void setUtilityThreshold(double ut) {
		utilityThreshold = ut;
	}

	public double getFatigueUtility() {

		return fatigueUtility;
	}

	public double getFatigueUtilityThreshold() {
		return fatigueUtilityThreshold;
	}

	void findInstantiations(final Buffers buffers) {
		// if (model.verboseTrace) model.output ("procedural",
		// "conflict-resolution");
		buffers.removeDecayedChunks();

		HashSet<Instantiation> set = new HashSet<Instantiation>();
		buffers.sortGoals();

		if (buffers.numGoals() == 0) {
			Iterator<Production> it = productions.values().iterator();
			while (it.hasNext()) {
				Production p = it.next();
				Instantiation inst = p.instantiate(buffers);
				if (inst != null)
					set.add(inst);
			}
		} else {
			for (int i = 0; set.isEmpty() && i < buffers.numGoals(); i++) {
				buffers.tryGoal(i);
				if (threadedCognitionTrace)
					model.output("*** (tct) trying goal " + buffers.get(Symbol.goal));
				Iterator<Production> it = productions.values().iterator();
				while (it.hasNext()) {
					Production p = it.next();
					Instantiation inst = p.instantiate(buffers);
					if (inst != null)
						set.add(inst);
				}
			}
		}

		if (threadedCognitionTrace)
			model.output("*** (tct) found " + set.size() + " match" + (set.size() == 1 ? "" : "es"));

		if (!set.isEmpty()) {
			if (conflictSetTrace)
				model.output("Conflict Set:");
			Iterator<Instantiation> itInst = set.iterator();
			Instantiation highestU = itInst.next();
			if (conflictSetTrace)
				model.output("* (" + String.format("%.3f", highestU.getUtility()) + ") " + highestU);
			while (itInst.hasNext()) {
				Instantiation inst = itInst.next();
				if (conflictSetTrace)
					model.output("* (" + String.format("%.3f", inst.getUtility()) + ") " + inst);
				if (inst.getUtility() > highestU.getUtility())
					highestU = inst;
			}

			final Instantiation finalInst = highestU;

			// System.out.println(model.getTime() + " " +
			// highestU.getProduction().getName() + " u: " +
			// highestU.getUtility() + "----" );

			// Scaling the instantiation production which has the highest
			// utility
			// with the fp parameter which is a representative of fatigue in the
			// model
			// highestU.setUtility(highestU.getUtility()
			// * model.getFatigue().fatigue_fp);

			// System.out.println(model.getTime() + " " +
			// highestU.getProduction().getName() + " u: " +
			// (highestU.getUtility()* model.getFatigue().compute_fp() +
			// Utilities.getNoise(model.getProcedural().utilityNoiseS))
			// + "----" );
			// System.out.println(model.getTime() + " " +
			// highestU.getProduction().getName() + " ut: " +
			// (model.getFatigue().compute_ft()*model.getProcedural().utilityThreshold)
			// + "----" );

			// for being able to return the utility and utility threshold after
			// applying fatigue mechanism
			fatigueUtility = initialUtility * model.getFatigue().computeFP();
			fatigueUtilityThreshold = model.getFatigue().computeFT() * model.getProcedural().utilityThreshold;

			double realActionTime = actionTime;
			if (model.randomizeTime && variableProductionFiringTime)
				realActionTime = model.randomizeTime(realActionTime);

			if (model.getFatigue().fatigueEnabled && highestU
					.getUtility() < (model.getFatigue().computeFT() * model.getProcedural().utilityThreshold)) {
				if (conflictSetTrace)
					model.output(String.format("[utility below current threshold of %.3f]",
							model.getProcedural().utilityThreshold));

				model.addEvent(new Event(model.getTime() + (realActionTime - .001), "procedural",
						"[no rule fired, utility below threshold]") {
					public void action() {
						findInstantiations(buffers);
					}
				});
			} else {
				if (conflictSetTrace)
					model.output("-> (" + String.format("%.3f", finalInst.getUtility()) + ") " + finalInst);

				if (finalInst.getProduction().isBreakPoint()) {
					model.addEvent(new Event(model.getTime() + (realActionTime - .001), "procedural",
							"about to fire " + finalInst.getProduction().getName().getString().toUpperCase()) {
						public void action() {
							model.output("------", "break");
							model.stop();
						}
					});
				}

				String extra = "";
				if (buffers.numGoals() > 1) {
					Chunk goal = buffers.get(Symbol.goal);
					extra = " [" + ((goal != null) ? goal.getName().getString() : "nil") + "]";
				}

				// model.addEvent(new Event(model.getTime() + .050 ,
				model.addEvent(new Event(model.getTime() + (realActionTime - .001), "procedural",
						"** " + finalInst.getProduction().getName().getString().toUpperCase() + " **" + extra) {
					public void action() {
						fire(finalInst, buffers);
						findInstantiations(buffers);
					}
				});
			}
		}
	}

	// void fatigueUpdate()
	// {
	// fatigue_fp = Math.max(0.000001, fatigue_fp - fatigue_fp_dec );
	// }

	void fire(Instantiation inst, Buffers buffers) {
		inst.getProduction().fire(inst);
		model.update();

		if (productionLearning) {
			Instantiation lastFired = (!productionCompilationThreaded) ? lastFiredInst
					: lastFiredOnThread.get(new Integer(inst.getThreadID()));

			if (lastFired != null && inst.getTime()
					- lastFired.getTime() > model.getProcedural().productionCompilationThresholdTime) {
				if (productionCompilationTrace)
					model.output("*** (pct) no compilation: too much time between firings");
			} else if (lastFired != null) {
				Production newp = new Compilation(lastFired, inst, model).compile();

				if (newp != null) {
					Production oldp = exists(newp);
					if (oldp != null) {
						double alpha = utilityLearningAlpha;

						if (productionCompilationAddUtilities) {
							double sum = lastFired.getProduction().getUtility() + inst.getProduction().getUtility();
							oldp.setUtility(oldp.getUtility() + alpha * (sum - oldp.getUtility()));
						} else {
							oldp.setUtility(oldp.getUtility()
									+ alpha * (lastFired.getProduction().getUtility() - oldp.getUtility()));
						}

						if (productionCompilationTrace)
							model.output("*** (pct) strengthening " + oldp.getName() + " [u="
									+ String.format("%.3f", oldp.getUtility()) + "]");
					} else {
						model.getProcedural().add(newp);
						if (productionCompilationTrace) {
							model.output("\n*** (pct)\n");
							model.output("" + lastFired.getProduction().toString(lastFired));
							model.output("" + inst.getProduction().toString(inst));
							model.output("" + newp);
							// model.output
							// ("*** (pct) new production:\n"+newp);
						}
					}
				} else {
					if (productionCompilationTrace)
						model.output("*** (pct) no compilation: productions cannot be combined");
				}
			} else {
				if (productionCompilationTrace)
					model.output("*** (pct) no compilation: no previous production");
			}
		}

		lastFiredInst = inst;
		lastFiredOnThread.put(new Integer(inst.getThreadID()), inst);

		if (utilityLearning) {
			rewardFirings.add(inst);
			if (rewardFirings.size() > 100)
				rewardFirings.removeElementAt(0);
		}
		if (utilityLearning && inst.getProduction().hasReward()) {
			adjustUtilities(inst.getProduction().getReward());
			rewardFirings.clear();
		}
	}

	void adjustUtilities(double reward) {
		double alpha = utilityLearningAlpha;
		for (int i = 0; i < rewardFirings.size(); i++) {
			Instantiation inst = rewardFirings.elementAt(i);
			Production p = inst.getProduction();
			double pReward = reward - (model.getTime() - inst.getTime());
			p.setUtility(p.getUtility() + alpha * (pReward - p.getUtility()));
			// model.output ("*** "+ p.getName() + " : " + p.getUtility());
		}
	}

	/**
	 * Gets a string representation of the procedural store including all
	 * productions.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "";
		Iterator<Production> it = productions.values().iterator();
		while (it.hasNext())
			s += it.next() + "\n";
		return s;
	}
}
