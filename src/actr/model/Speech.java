package actr.model;

/**
 * The speech module representing ACT-R's vocal speech generation.
 * 
 * @author Dario Salvucci
 */
public class Speech extends Module {
	private Model model;
	private String lastText;

	double syllableRate = .150;
	int charsPerSyllable = 3;
	double subvocalizeDetectDelay = .300;

	final double prepFirstText = .150;
	final double prepDiffText = .100;
	final double prepSameText = .000;
	final double initiationTime = .050;
	final double clearTime = .050;

	Speech(Model model) {
		this.model = model;
		lastText = null;
	}

	double prepareMovement(double time, String text) {
		time += (lastText == null) ? prepFirstText
				: (lastText.equals(text) ? prepSameText : prepDiffText);
		model.getBuffers().setSlot(Symbol.vocalState, Symbol.preparation,
				Symbol.busy);
		model.getBuffers().setSlot(Symbol.vocalState, Symbol.processor,
				Symbol.busy);
		model.getBuffers()
				.setSlot(Symbol.vocalState, Symbol.state, Symbol.busy);
		model.addEvent(new Event(time, "speech", "preparation-complete") {
			@Override
			public void action() {
				model.getBuffers().setSlot(Symbol.vocalState,
						Symbol.preparation, Symbol.free);
			}
		});
		lastText = text;
		return time;
	}

	double initiateMovement(double time) {
		time += initiationTime;
		model.addEvent(new Event(time, "speech", "initiation-complete") {
			@Override
			public void action() {
				model.getBuffers().setSlot(Symbol.vocalState, Symbol.processor,
						Symbol.free);
				model.getBuffers().setSlot(Symbol.vocalState, Symbol.execution,
						Symbol.busy);
			}
		});
		return time;
	}

	void finishMovement(double time) {
		model.addEvent(new Event(time, "speech", "finish-movement") {
			@Override
			public void action() {
				model.getBuffers().setSlot(Symbol.vocalState, Symbol.execution,
						Symbol.free);
				model.getBuffers().setSlot(Symbol.vocalState, Symbol.state,
						Symbol.free);
			}
		});
	}

	double getArticulationTime(String text) {
		return syllableRate * (1.0 * text.length() / charsPerSyllable);
	}

	void sendVocalToAudio(String text) {
		// model.getAudio().addAural (Symbol.getUnique("vocal").getString(),
		// "word", text);
	}

	@Override
	void update() {
		Chunk request = model.getBuffers().get(Symbol.vocal);
		if (request == null || !request.isRequest())
			return;
		request.setRequest(false);
		model.getBuffers().clear(Symbol.vocal);
		double eventTime = model.getTime();

		if (request.get(Symbol.isa) == Symbol.get("clear")) {
			if (model.verboseTrace)
				model.output("speech", "clear");
			model.getBuffers().setSlot(Symbol.vocalState, Symbol.preparation,
					Symbol.busy);
			model.getBuffers().setSlot(Symbol.vocalState, Symbol.state,
					Symbol.busy);
			model.addEvent(new Event(eventTime + clearTime, "speech",
					"change state last none prep free") {
				@Override
				public void action() {
					lastText = null;
					model.getBuffers().setSlot(Symbol.vocalState,
							Symbol.preparation, Symbol.free);
					model.getBuffers().setSlot(Symbol.vocalState, Symbol.state,
							Symbol.free);
				}
			});
			return;
		}

		else if (request.get(Symbol.isa) == Symbol.get("speak")) {
			final String text = request.get(Symbol.get("string")).getString()
					.replace("\"", "");
			if (model.verboseTrace)
				model.output("speech", "speak \"" + text + "\"");
			eventTime = prepareMovement(eventTime, text);
			eventTime = initiateMovement(eventTime);
			model.addEvent(new Event(eventTime, "speech", "output-speech \""
					+ text + "\"") {
				@Override
				public void action() {
					model.getTask().speak(text);
					sendVocalToAudio(text);
				}
			});
			eventTime += getArticulationTime(text);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("subvocalize")) {
			final String text = request.get(Symbol.get("string")).getString()
					.replace("\"", "");
			if (model.verboseTrace)
				model.output("speech", "subvocalize \"" + text + "\"");
			eventTime = prepareMovement(eventTime, text);
			eventTime = initiateMovement(eventTime);
			model.addEvent(new Event(eventTime, "", "output-subvocalize \""
					+ text + "\"") {
				@Override
				public void action() {
					sendVocalToAudio(text);
				}
			});
			eventTime += getArticulationTime(text);
			finishMovement(eventTime);
		}
	}
}
