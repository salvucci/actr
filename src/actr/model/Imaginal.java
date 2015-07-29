package actr.model;

/**
 * The imaginal module that holds a temporary problem state for the current
 * task.
 * 
 * @author Dario Salvucci
 */
public class Imaginal extends Module {
	private Model model;

	double imaginalDelay = .200;

	Imaginal(Model model) {
		this.model = model;
	}

	@Override
	void update() {
		final Chunk chunk = model.getBuffers().get(Symbol.imaginal);
		if (chunk != null && chunk.isRequest()) {
			chunk.setRequest(false);
			model.getBuffers().clear(Symbol.imaginal);
			if (model.getBuffers().getSlot(Symbol.imaginalState, Symbol.state) == Symbol.busy) {
				model.outputWarning("imaginal busy, request ignored");
				return;
			}
			if (model.verboseTrace)
				model.output("imaginal", "set-buffer init");
			model.getBuffers().setSlot(Symbol.imaginalState, Symbol.state, Symbol.busy);
			model.getBuffers().setSlot(Symbol.imaginalState, Symbol.buffer, Symbol.requested);
			model.addEvent(
					new Event(model.getTime() + imaginalDelay, "imaginal", "set-buffer [" + chunk.getName() + "]") {
						@Override
						public void action() {
							model.getBuffers().set(Symbol.imaginal, chunk);
							model.getBuffers().setSlot(Symbol.imaginalState, Symbol.state, Symbol.free);
							model.getBuffers().setSlot(Symbol.imaginalState, Symbol.buffer, Symbol.full);
						}
					});
		}
	}
}
