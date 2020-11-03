package actr.model;

import java.util.Iterator;
import java.util.List;

/**
 * Production compilation process that transforms two rules into a single rule.
 * This version represents the "simple" version that uses only a generic
 * compilation process for all buffers except the retrieval buffer, which still
 * uses a special compilation process.
 * 
 * @author Dario Salvucci
 */
class Compilation {
	private final Instantiation inst1;
	private final Instantiation inst2;
	private final Model model;
	private final Production p1;
	private final Production p2;
	private final Production newp;

	Compilation(Instantiation inst1, Instantiation inst2, Model model) {
		this.inst1 = inst1.copy();
		this.inst2 = inst2.copy();
		this.model = model;
		p1 = inst1.production.copy();
		p2 = inst2.production.copy();
		p1.expandDirectActions(inst1);
		p2.expandDirectActions(inst2);
		String name = inst1.production.name + "++" + inst2.production.name;
		newp = new Production(Symbol.getUnique(name), model);
	}

	void addCondition(BufferCondition bc) {
		if (bc != null)
			newp.addBufferCondition(bc);
	}

	void addAction(BufferAction ba) {
		if (ba != null)
			newp.addBufferAction(ba);
	}

	static BufferCondition blendConditions(BufferCondition bc1, BufferCondition bc2) {
		if (bc1 == null)
			return bc2;
		if (bc2 == null)
			return bc1;
		int c = bc2.slotCount();
		for (int i = 0; i < c; i++) {
			SlotCondition sc = bc2.getSlotCondition(i);
			SlotCondition scnew = bc1.getSlotCondition(sc.getSlot());
			if (scnew == null)
				bc1.addCondition(sc);
		}
		return bc1;
	}

	BufferCondition blendConditionsMinusActions(BufferCondition bc1, BufferCondition bc2, BufferAction ba1) {
		if (bc1 == null && bc2 == null)
			return null;
		if (bc2 == null)
			return bc1;
		if (ba1 == null)
			return blendConditions(bc2, bc2);
		if (bc1 == null)
			bc1 = new BufferCondition('=', bc2.buffer, model);
		int c = bc2.slotCount();
		for (int i = 0; i < c; i++) {
			SlotCondition sc = bc2.getSlotCondition(i);
			Symbol scs = sc.getSlot();
			if (bc1.getSlotCondition(scs) == null && !ba1.hasSlotAction(scs))
				bc1.addCondition(sc);
		}
		return bc1;
	}

	static BufferAction blendActions(BufferAction ba1, BufferAction ba2) {
		if (ba1 == null)
			return ba2;
		if (ba2 == null)
			return ba1;
		int scc = ba1.slotCount();
		for (int i = 0; i < scc; i++) {
			SlotAction sc = ba1.getSlotAction(i);
			if (!ba2.hasSlotAction(sc.getSlot()))
				ba2.addAction(sc);
		}
		return ba2;
	}

	BufferCondition createStandardStateCondition(Symbol stateBuffer) {
		BufferCondition bc = new BufferCondition('?', stateBuffer, model);
		bc.addCondition(new SlotCondition(null, Symbol.buffer, Symbol.empty, model));
		bc.addCondition(new SlotCondition(null, Symbol.state, Symbol.free, model));
		return bc;
	}

	boolean checkSpecials() {
		return (!p1.hasSpecials() && !p2.hasSpecials());
	}

	boolean compileGoalStyle(Symbol buffer) {
		Symbol stateBuffer = Symbol.get("?" + buffer);

		// if ((p1.hasBufferAction('+',buffer) ||
		// p1.hasBufferAction('-',buffer))
		// && p2.hasBufferAction('+',buffer))
		// return false;

		if (!p1.hasBufferAction('+', buffer)) {
			addCondition(blendConditionsMinusActions(p1.getBufferCondition(buffer), p2.getBufferCondition(buffer),
					p1.getBufferAction('=', buffer)));
			addCondition(blendConditions(p1.getBufferCondition(stateBuffer), p2.getBufferCondition(stateBuffer)));

			addAction(blendActions(p1.getBufferAction('=', buffer), p2.getBufferAction('=', buffer)));
			addAction(blendActions(p1.getBufferAction('-', buffer), p2.getBufferAction('-', buffer)));
			addAction(p2.getBufferAction('+', buffer));
		} else // if (p1.hasBufferAction('+',buffer) &&
				// !p2.hasBufferAction('+',buffer))
		{
			addCondition(p1.getBufferCondition(buffer));
			addCondition(p1.getBufferCondition(stateBuffer));

			BufferCondition c2 = p2.getBufferCondition(buffer);
			if (c2 != null) {
				Iterator<SlotCondition> it = c2.getSlotConditions();
				while (it.hasNext()) {
					SlotCondition sc = it.next();
					if (sc.getValue().isVariable())
						p2.specialize(sc.getValue(), inst2.get(sc.getValue()));
				}
			}

			addAction(p1.getBufferAction('=', buffer));
			addAction(blendActions(p1.getBufferAction('-', buffer), p2.getBufferAction('-', buffer)));

			BufferAction ba = blendActions(p1.getBufferAction('+', buffer), p2.getBufferAction('=', buffer));
			if (ba != null)
				ba.setPrefix('+');
			addAction(ba);
		}
		return true;
	}

	boolean compilePercMotorStyle(Symbol buffer) {
		Symbol stateBuffer = Symbol.get("?" + buffer);

		if ((p1.hasBufferAction('+', buffer) || p1.hasBufferAction('-', buffer)) && (p2.hasBufferAction('+', buffer)
				|| p2.hasBufferCondition(buffer) || p2.queriesOtherThanBusy(stateBuffer)))
			return false;

		if (p1.hasBufferAction('+', buffer))
			addCondition(blendConditionsMinusActions(p1.getBufferCondition(buffer), p2.getBufferCondition(buffer),
					p1.getBufferAction('+', buffer)));
		else
			addCondition(blendConditionsMinusActions(p1.getBufferCondition(buffer), p2.getBufferCondition(buffer),
					p1.getBufferAction('=', buffer)));
		addCondition(blendConditions(p1.getBufferCondition(stateBuffer), p2.getBufferCondition(stateBuffer)));

		addAction(blendActions(p1.getBufferAction('=', buffer), p2.getBufferAction('=', buffer)));
		addAction(blendActions(p1.getBufferAction('-', buffer), p2.getBufferAction('-', buffer)));
		addAction(blendActions(p1.getBufferAction('+', buffer), p2.getBufferAction('+', buffer)));

		return true;
	}

	boolean compileRetrievalStyle(Symbol buffer) {
		Symbol stateBuffer = Symbol.get("?" + buffer);

		if (p1.hasBufferAction(buffer) && p2.queriesForError(stateBuffer))
			return false;

		if (p1.hasBufferAction(buffer) && p2.hasBufferAction(buffer) && !p2.hasBufferCondition(buffer))
			return false;

		// NEW: specialize ALL variables when compiling a retrieval
		Iterator<Symbol> it = inst1.getVariables();
		while (it.hasNext()) {
			Symbol var = it.next();
			Symbol val = inst1.get(var);
			if (val != null)
				p1.specialize(var, val);
		}
		it = inst2.getVariables();
		while (it.hasNext()) {
			Symbol var = it.next();
			Symbol val = inst2.get(var);
			if (val != null)
				p2.specialize(var, val);
		}
		// NEW done

		if (p1.hasBufferAction(buffer) && p2.hasBufferCondition(buffer)) {
			// note: specializing p1 & p2 automatically specializes newp (linked
			// pointers)

			BufferAction a1 = p1.getBufferAction(buffer);
			for (int i = 0; a1 != null && i < a1.slotCount(); i++) {
				SlotAction sa = a1.getSlotAction(i);
				if (sa.getSlot().isVariable()) {
					Symbol oldval = sa.getSlot();
					Symbol newval = inst1.get(oldval);
					p1.specialize(oldval, newval);
					p2.specialize(oldval, newval);
					oldval = sa.getValue();
					newval = inst1.get(oldval);
					p1.specialize(oldval, newval);
					p2.specialize(oldval, newval);
				} else if (sa.getValue().isVariable()) {
					Symbol oldval = sa.getValue();
					Symbol newval = inst1.get(oldval);
					p1.specialize(oldval, newval);
					p2.specialize(oldval, newval);
				}
			}

			p2.specialize(Symbol.get("=" + buffer), inst2.get(Symbol.get("=" + buffer)));
			BufferCondition c2 = p2.getBufferCondition(buffer);
			Iterator<SlotCondition> itSC = c2.getSlotConditions();
			while (itSC.hasNext()) {
				SlotCondition sc = itSC.next();
				if (sc.getSlot().isVariable()) {
					Symbol oldval = sc.getSlot();
					Symbol newval = inst2.get(oldval);
					p2.specialize(oldval, newval);
					p1.specialize(oldval, newval);
					oldval = sc.getValue();
					newval = inst2.get(oldval);
					p2.specialize(oldval, newval);
					p1.specialize(oldval, newval);
				} else if (sc.getValue().isVariable()) {
					Symbol oldval = sc.getValue();
					Symbol newval = inst2.get(oldval);
					p2.specialize(oldval, newval);
					p1.specialize(oldval, newval);
				}
			}

			addCondition(p1.getBufferCondition(buffer));

			if (p1.queriesForError(stateBuffer)) {
				addCondition(p1.getBufferCondition(stateBuffer));
				addAction(new BufferAction('-', Symbol.retrieval, model));
			}

			addAction(p2.getBufferAction(buffer));

			if (newp.hasBufferAction('+', buffer)) {
				addCondition(p1.getBufferCondition(stateBuffer));
				if (!newp.hasBufferCondition(stateBuffer))
					addCondition(createStandardStateCondition(stateBuffer));
			}
		} else {
			if (p1.hasBufferAction(buffer)) {
				addCondition(p1.getBufferCondition(buffer));
				if (!newp.hasBufferCondition(buffer) // XXX bug? this fix added
														// 5/20/11
				)
					addCondition(p1.getBufferCondition(stateBuffer));
			} else {
				addCondition(blendConditions(p1.getBufferCondition(buffer), p2.getBufferCondition(buffer)));
				if (!newp.hasBufferCondition(buffer) // XXX bug? this fix added
														// 5/20/11
				)
					addCondition(
							blendConditions(p1.getBufferCondition(stateBuffer), p2.getBufferCondition(stateBuffer)));
			}

			addAction(blendActions(p1.getBufferAction('=', buffer), p2.getBufferAction('=', buffer)));
			addAction(blendActions(p1.getBufferAction('-', buffer), p2.getBufferAction('-', buffer)));
			addAction(blendActions(p1.getBufferAction('+', buffer), p2.getBufferAction('+', buffer)));
		}
		return true;
	}

	static void replaceValue(Production p, Instantiation inst, Symbol oldval, Symbol newval) {
		p.specialize(oldval, newval);
		inst.replaceVariable(oldval, newval);
	}

	void uniquifyVariables() {
		List<Symbol> p1vars = p1.getVariables();
		List<Symbol> p2vars = p2.getVariables();
		for (Symbol var : p1vars) {
			if (p2vars.contains(var)) {
				if (model.buffers.isLegalBuffer(var))
					p2.specialize(var, inst2.get(var));
				else
					replaceValue(p2, inst2, var, Symbol.get(var.getString() + "2"));
			}
		}
	}

	void synchronizeVariables() {
		Iterator<BufferCondition> itBC = p2.getConditions();
		while (itBC.hasNext()) {
			BufferCondition c2 = itBC.next();
			Symbol buffer = c2.buffer;
			Iterator<SlotCondition> itSC = c2.getSlotConditions();
			while (itSC.hasNext()) {
				SlotCondition sc2 = itSC.next();
				Symbol slot2 = sc2.getSlot();
				if (slot2.isVariable())
					continue;
				Symbol value2 = sc2.getValue();

				SlotAction sa1 = p1.getSlotAction(buffer, slot2);
				if (sa1 != null) {
					Symbol value1 = sa1.getValue();
					if (!value2.isVariable())
						replaceValue(p1, inst1, value1, value2);
					else
						replaceValue(p2, inst2, value2, value1);
				}
			}
		}
	}

	Production compile() {
		if ((inst2.time - inst1.time) < model.procedural.productionCompilationThresholdTime) {
			uniquifyVariables();
			synchronizeVariables();
			if (checkSpecials()

			&& compileGoalStyle(Symbol.goal) && compileGoalStyle(Symbol.imaginal)

			&& compilePercMotorStyle(Symbol.visloc) && compilePercMotorStyle(Symbol.visual)
					&& compilePercMotorStyle(Symbol.aurloc) && compilePercMotorStyle(Symbol.aural)
					&& compilePercMotorStyle(Symbol.manual) && compilePercMotorStyle(Symbol.vocal)

			&& compileRetrievalStyle(Symbol.retrieval)) {
				newp.setUtility(model.procedural.productionCompilationNewUtility);
				return newp;
			}
		}
		return null;
	}
}
