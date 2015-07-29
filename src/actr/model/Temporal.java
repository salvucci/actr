package actr.model;

/**
 * The temporal module representing ACT-R's perception of time.
 * 
 * @author Dario Salvucci
 */
public class Temporal extends Module {
	private Model model;
	private int ticks = 0;
	private double tick = 0;

	double timeNoise = .015;
	double timeMultiplier = 1.1;
	double timeMasterStartIncrement = .011;

	Temporal(Model model) {
		this.model = model;
	}

	@Override
	void update() {
		Chunk request = model.getBuffers().get(Symbol.temporal);
		if (request == null || !request.isRequest())
			return;
		request.setRequest(false);
		model.getBuffers().clear(Symbol.temporal);

		if (request.get(Symbol.isa) == Symbol.get("time")) {
			tick = timeMasterStartIncrement;
			ticks = 0;

			Chunk timeChunk = new Chunk(Symbol.getUnique("time"), model);
			timeChunk.set(Symbol.isa, Symbol.time);
			timeChunk.set(Symbol.ticks, Symbol.get(0));
			model.getBuffers().set(Symbol.temporal, timeChunk);

			model.getBuffers().setSlot(Symbol.temporalState, Symbol.buffer, Symbol.full);
			model.getBuffers().setSlot(Symbol.temporalState, Symbol.state, Symbol.busy);

			model.removeEvents("temporal");
			queueTickIncrement();
		}
	}

	void queueTickIncrement() {
		model.addEvent(new Event(model.getTime() + tick, "temporal", "increment ticks [" + (ticks + 1) + "]") {
			@Override
			public void action() {
				ticks++;
				tick *= timeMultiplier;
				tick += Utilities.getNoise(timeNoise * tick);

				model.getBuffers().setSlot(Symbol.temporal, Symbol.ticks, Symbol.get(ticks));

				queueTickIncrement();
			}
		});
	}
}
