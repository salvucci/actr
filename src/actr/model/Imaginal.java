package actr.model;

/**
 * The imaginal module that holds a temporary problem state for the current
 * task.
 * 
 * @author Dario Salvucci
 */
public class Imaginal extends Module {
	private final Model model;

	double imaginalDelay = .200;

	Imaginal(Model model) {
		this.model = model;
	}

	@Override
	public void update() {
		final Chunk chunk = model.buffers.get(Symbol.imaginal);
		if (chunk != null && chunk.isRequest()) {
			chunk.setRequest(false);
			model.buffers.clear(Symbol.imaginal);
			if (model.buffers.getSlot(Symbol.imaginalState, Symbol.state) == Symbol.busy) {
				model.outputWarning("imaginal busy, request ignored");
				return;
			}
			if (model.verboseTrace)
				model.output("imaginal", "set-buffer init");
			model.buffers.setSlot(Symbol.imaginalState, Symbol.state, Symbol.busy);
			model.buffers.setSlot(Symbol.imaginalState, Symbol.buffer, Symbol.requested);
			model.addEvent(
					new Event(model.getTime() + imaginalDelay, "imaginal", "set-buffer [" + chunk.name() + "]") {
						@Override
						public void action() {
							model.buffers.set(Symbol.imaginal, chunk);
							model.buffers.setSlot(Symbol.imaginalState, Symbol.state, Symbol.free);
							model.buffers.setSlot(Symbol.imaginalState, Symbol.buffer, Symbol.full);
						}
					});
		}
	}
}
