package actr.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The audio module representing ACT-R's auditory perception.
 * 
 * @author Dario Salvucci
 */
public class Audio extends Module {
	private final Model model;
	private final Map<Symbol, AuralObject> audicon;
	private final Map<Symbol, AuralObject> aurallocs;

	double toneDetectDelay = .050;
	double toneRecodeDelay = .285;
	double digitDetectDelay = .300;
	double digitRecodeDelay = .500;

	Audio(Model model) {
		this.model = model;
		audicon = new HashMap<>();
		aurallocs = new HashMap<>();
	}

	private static class AuralObject {
		final Symbol id;
		final Symbol type;
		final Symbol content;
		Chunk auralloc;

		AuralObject(Symbol id, Symbol type, Symbol content) {
			this.id = id;
			this.type = type;
			this.content = content;
			auralloc = null;
		}

		@Override
		public String toString() {
			return "[" + id + " " + type + " " + content + "]";
		}
	}

	void clearAural() {
		audicon.clear();
	}

	/**
	 * Adds an aural sound for access by the aural module. The identifier should
	 * be unique for different sounds; if the same identifier is used, the new
	 * sound will cancel the previously scheduled one.
	 * 
	 * @param id
	 *            the unique name of the aural object
	 * @param type
	 *            the type of the aural object (i.e., the "kind" slot value)
	 * @param content
	 *            the content of the aural object (i.e., the "content" slot
	 *            value)
	 */
	public void addAural(final String id, String type, String content) {
		final AuralObject ao = new AuralObject(Symbol.get(id), Symbol.get(type), Symbol.get(content));
		final Chunk auralloc = createAuralLocChunk(ao);
		if (auralloc != null) {
			double detectDelay = (auralloc.get(Symbol.kind) == Symbol.tone) ? toneDetectDelay : digitDetectDelay;
			model.addEvent(
					new Event(model.getTime() + detectDelay, "audio", "audio-event [" + auralloc.name() + "]") {
						@Override
						public void action() {
							audicon.put(Symbol.get(id), ao);
							if (model.bufferStuffing && model.buffers.get(Symbol.aurloc) == null) {
								if (model.verboseTrace)
									model.output("audio", "unrequested [" + auralloc.name() + "]");
								model.buffers.set(Symbol.aurloc, auralloc);
								model.buffers.setSlot(Symbol.aurlocState, Symbol.state, Symbol.free);
								model.buffers.setSlot(Symbol.aurlocState, Symbol.buffer, Symbol.unrequested);
							}
						}
					});
		}
		model.noteTaskUpdated();
	}

	private Chunk createAuralLocChunk(AuralObject ao) {
		if (ao == null)
			return null;
		Chunk auralloc = new Chunk(Symbol.getUnique("audio-event"), model);
		auralloc.set(Symbol.isa, Symbol.get("audio-event"));
		auralloc.set(Symbol.kind, ao.type);
		auralloc.set(Symbol.location, Symbol.get("external"));
		ao.auralloc = auralloc;
		aurallocs.put(auralloc.name(), ao);
		return auralloc;
	}

	private Chunk findAuralLocation(Chunk request) {
		Symbol kind = request.get(Symbol.kind);

		AuralObject found = null;
		Iterator<AuralObject> it = audicon.values().iterator();
		while (found == null && it.hasNext()) {
			AuralObject ao = it.next();
			if (kind != Symbol.nil && kind != ao.type)
				continue;
			found = ao;
		}
		return createAuralLocChunk(found);
	}

	@Override
	public void update() {
		Chunk request = model.buffers.get(Symbol.aurloc);
		if (request != null && request.isRequest()) {
			request.setRequest(false);
			model.buffers.clear(Symbol.aurloc);
			final Chunk auralloc = findAuralLocation(request);
			if (auralloc != null) {
				if (model.verboseTrace)
					model.output("audio", "find-sound [" + auralloc.name() + "]");
				model.buffers.set(Symbol.aurloc, auralloc);
				model.buffers.setSlot(Symbol.aurlocState, Symbol.state, Symbol.free);
				model.buffers.setSlot(Symbol.aurlocState, Symbol.buffer, Symbol.full);
			} else {
				if (model.verboseTrace)
					model.output("audio", "find-sound-failure");
				model.buffers.setSlot(Symbol.aurlocState, Symbol.state, Symbol.error);
				model.buffers.setSlot(Symbol.aurlocState, Symbol.buffer, Symbol.empty);
			}
		}

		request = model.buffers.get(Symbol.aural);
		if (request != null && request.isRequest()) // &&
													// request.get(Symbol.isa)==Symbol.get("sound"))
		{
			request.setRequest(false);
			model.buffers.clear(Symbol.aural);
			if (model.verboseTrace)
				model.output("audio", "attend-sound");
			Symbol aurallocName = request.get(Symbol.event);
			if (aurallocName == null) {
				model.outputWarning("bad aural location");
				return;
			}
			AuralObject ao = aurallocs.get(aurallocName);
			Chunk auralloc = ao.auralloc;
			if (auralloc == null) {
				model.outputWarning("bad aural location");
				return;
			}
			Symbol kind = auralloc.get(Symbol.kind);
			final Chunk aural = new Chunk(Symbol.getUnique(kind.getString()), model);
			aural.set(Symbol.isa, kind);
			aural.set(Symbol.event, aurallocName);
			aural.set(Symbol.content, ao.content);
			double recodeDelay = (auralloc.get(Symbol.kind) == Symbol.tone) ? toneRecodeDelay : digitRecodeDelay;
			model.buffers.setSlot(Symbol.auralState, Symbol.state, Symbol.busy);
			model.buffers.setSlot(Symbol.auralState, Symbol.buffer, Symbol.requested);
			model.addEvent(new Event(model.getTime() + recodeDelay, "audio",
					"audio-encoding-complete [" + aural.name() + "]") {
				@Override
				public void action() {
					model.buffers.set(Symbol.aural, aural);
					model.buffers.setSlot(Symbol.auralState, Symbol.state, Symbol.free);
					model.buffers.setSlot(Symbol.auralState, Symbol.buffer, Symbol.full);
				}
			});
			// XXX doesn't handle failure
		}
	}
}
