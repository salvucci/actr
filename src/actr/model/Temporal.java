package actr.model;

/**
 * The temporal module representing ACT-R's perception of time.
 * 
 * @author Dario Salvucci
 */
public class Temporal extends Module {
	private final Model model;
	private int ticks = 0;
	private double tick = 0;

	final double timeNoise = .015;
	final double timeMultiplier = 1.1;
	final double timeMasterStartIncrement = .011;

	Temporal(Model model) {
		this.model = model;
	}

	@Override
	void update() {
		Chunk request = model.buffers.get(Symbol.temporal);
		if (request == null || !request.isRequest())
			return;
		request.setRequest(false);
		model.buffers.clear(Symbol.temporal);

		if (request.get(Symbol.isa) == Symbol.get("time")) {
			tick = timeMasterStartIncrement;
			ticks = 0;

			Chunk timeChunk = new Chunk(Symbol.getUnique("time"), model);
			timeChunk.set(Symbol.isa, Symbol.time);
			timeChunk.set(Symbol.ticks, Symbol.get(0));
			model.buffers.set(Symbol.temporal, timeChunk);

			model.buffers.setSlot(Symbol.temporalState, Symbol.buffer, Symbol.full);
			model.buffers.setSlot(Symbol.temporalState, Symbol.state, Symbol.busy);

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

				model.buffers.setSlot(Symbol.temporal, Symbol.ticks, Symbol.get(ticks));

				queueTickIncrement();
			}
		});
	}
}
