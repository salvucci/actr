package actr.model;

import java.util.*;

/**
 * The vision module representing ACT-R's visual perception.
 * 
 * @author Dario Salvucci
 */
public class Vision extends Module {
	private final Model model;
	private final Map<Symbol, VisualObject> visicon;
	private final Map<Symbol, VisualObject> vislocs;
	private final List<VisualObject> finsts;
	private VisualObject lastEncodedVisObj = null;
	private double lastVisLocRequestX, lastVisLocRequestY;
	private int eyeX, eyeY;

	double visualAttentionLatency = .085;
	double visualMovementTolerance = Utilities.angle2pixels(0.5);
	int visualNumFinsts = 4;
	double visualFinstSpan = 3.0;
	double visualOnsetSpan = 0.5;

	boolean useEMMA = false;
	private final Map<String, Double> frequencies;
	double emmaEncodingTimeFactor = .006;
	double emmaEncodingExponentFactor = .4;
	final double emmaPreparationTime = .135;
	final double emmaDefaultFrequency = .01;
	final double emmaExecutionBaseTime = .070;
	final double emmaExecutionTimeIncrement = .002;

	Vision(Model model) {
		this.model = model;
		visicon = new HashMap<>();
		vislocs = new HashMap<>();
		finsts = new Vector<>();
		lastVisLocRequestX = lastVisLocRequestY = 0;
		eyeX = eyeY = 100;
		frequencies = new HashMap<>();
	}

	private class VisualObject {
		final Symbol id;
		final Symbol kind;
		final Symbol value;
		int x;
		int y;
		final int w;
		final int h;
		double d;
		boolean attended;
		double attendedTime;
		final double creationTime;
		Chunk visloc;
		double encodingStart;
		double encodingTime;

		VisualObject(Symbol id, Symbol kind, Symbol value, int x, int y, int w, int h, double d) {
			this.id = id;
			this.kind = kind;
			this.value = value;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.d = d;
			attended = false;
			attendedTime = 0;
			creationTime = model.getTime();
			visloc = null;
		}

		@Override
		public String toString() {
			return "(" + id + " kind " + kind + " screen-x " + x + " screen-y " + y + " value " + value + " :attended "
					+ (attended ? "t" : "nil") + ")";
		}
	}

	Chunk getVisualLocation(Symbol name) {
		VisualObject vo = vislocs.get(name);
		return vo != null ? vo.visloc : model.declarative.get(name);
	}

	/**
	 * Clears the visual module of all visual objects.
	 */
	public void clearVisual() {
		visicon.clear();
		model.noteTaskUpdated();
	}

	/**
	 * Adds a visual object for access by the vision module. The identifier
	 * should be unique for different sounds; if the same identifier is used,
	 * the new object will cancel the previous one (as might be useful for the
	 * movements of a single object).
	 * 
	 * @param id
	 *            the unique name of the visual object
	 * @param type
	 *            the type of the visual object (i.e., the "kind" slot value)
	 * @param value
	 *            the value of the visual object (i.e., the "value" slot value)
	 * @param x
	 *            the x coordinate of the visual object (i.e., the "screen-x"
	 *            slot value)
	 * @param y
	 *            the y coordinate of the visual object (i.e., the "screen-y"
	 *            slot value)
	 * @param w
	 *            the width of the visual object (i.e., the "width" slot value)
	 * @param h
	 *            the height of the visual object (i.e., the "height" slot
	 *            value)
	 * @param d
	 *            the distance of the visual object (i.e., the "distance" slot
	 *            value)
	 */
	public void addVisual(String id, String type, String value, int x, int y, int w, int h, double d) {
		VisualObject vo = new VisualObject(Symbol.get(id), Symbol.get(type), Symbol.get(value), x, y, w, h, d);
		visicon.put(Symbol.get(id), vo);
		if (model.bufferStuffing && (model.buffers.get(Symbol.visloc) == null
				|| model.buffers.get(Symbol.vislocState).get(Symbol.buffer) == Symbol.unrequested)) {
			double newDist = Utilities.distance(vo.x, vo.y, lastVisLocRequestX, lastVisLocRequestY);
			double curDist = 99999;
			try {
				Chunk curvisloc = model.buffers.get(Symbol.visloc);
				double sx = curvisloc.get(Symbol.screenx).toDouble();
				double sy = curvisloc.get(Symbol.screeny).toDouble();
				curDist = Utilities.distance(sx, sy, lastVisLocRequestX, lastVisLocRequestY);
			} catch (Exception e) {
			}

			if (newDist < curDist) {
				Chunk visloc = createVisLocChunk(vo);
				if (model.verboseTrace && model.buffers.get(Symbol.visloc) == null)
					model.output("vision", "unrequested [" + visloc.name().getString() + "]");
				model.buffers.set(Symbol.visloc, visloc);
				model.buffers.setSlot(Symbol.vislocState, Symbol.state, Symbol.free);
				model.buffers.setSlot(Symbol.vislocState, Symbol.buffer, Symbol.unrequested);
			}
		}
		model.noteTaskUpdated();
	}

	/**
	 * Adds a visual object for access by the vision module. The identifier
	 * should be unique for different sounds; if the same identifier is used,
	 * the new object will cancel the previous one (as might be useful for the
	 * movements of a single object).
	 * 
	 * @param id
	 *            the unique name of the visual object
	 * @param type
	 *            the type of the visual object (i.e., the "kind" slot value)
	 * @param value
	 *            the value of the visual object (i.e., the "value" slot value)
	 * @param x
	 *            the x coordinate of the visual object (i.e., the "screen-x"
	 *            slot value)
	 * @param y
	 *            the y coordinate of the visual object (i.e., the "screen-y"
	 *            slot value)
	 * @param w
	 *            the width of the visual object (i.e., the "width" slot value)
	 * @param h
	 *            the height of the visual object (i.e., the "height" slot
	 *            value)
	 */
	public void addVisual(String id, String type, String value, int x, int y, int w, int h) {
		addVisual(id, type, value, x, y, w, h, 0);
	}

	/**
	 * Moves the given visual object to a new location.
	 * 
	 * @param id
	 *            the unique name of the visual object
	 * @param x
	 *            the x coordinate of the visual object (i.e., the "screen-x"
	 *            slot value)
	 * @param y
	 *            the y coordinate of the visual object (i.e., the "screen-y"
	 *            slot value)
	 */
	public void moveVisual(String id, int x, int y) {
		VisualObject vo = visicon.get(Symbol.get(id));
		if (vo != null) {
			vo.x = x;
			vo.y = y;
		}
		model.noteTaskUpdated();
	}

	/**
	 * Moves the given visual object to a new location.
	 * 
	 * @param id
	 *            the unique name of the visual object
	 * @param x
	 *            the x coordinate of the visual object (i.e., the "screen-x"
	 *            slot value)
	 * @param y
	 *            the y coordinate of the visual object (i.e., the "screen-y"
	 *            slot value)
	 * @param d
	 *            the distance of the visual object (i.e., the "distance" slot
	 *            value)
	 */
	public void moveVisual(String id, int x, int y, double d) {
		VisualObject vo = visicon.get(Symbol.get(id));
		if (vo != null) {
			vo.x = x;
			vo.y = y;
			vo.d = d;
		}
		model.noteTaskUpdated();
	}

	/**
	 * Removes a particular visual object as specified by the identifier.
	 * 
	 * @param id
	 *            the unique name of the visual object
	 */
	public void removeVisual(String id) {
		visicon.remove(Symbol.get(id));
		model.noteTaskUpdated();
	}

	/**
	 * Gets the x coordinate of the eye's current location.
	 */
	public int getEyeX() {
		return eyeX;
	}

	/**
	 * Gets the y coordinate of the eye's current location.
	 */
	public int getEyeY() {
		return eyeY;
	}

	void setVisualFrequency(String string, double frequency) {
		frequencies.put(string, frequency);
	}

	private boolean matches(Symbol slot, Symbol value, VisualObject vo) {
		if (slot == Symbol.isa)
			return true;
		if (slot == Symbol.kind) {
			return value == vo.kind;
		}
		if (slot == Symbol.get("-kind")) {
			return value != vo.kind;
		}
		if (slot == Symbol.get(":nearest"))
			return true;

		if (slot == Symbol.get(":attended")) {
			return value == Symbol.get("new") ? !(vo.creationTime < model.getTime() - visualOnsetSpan) : value == Symbol.get(vo.attended);
		}

		if (value == Symbol.lowest || value == Symbol.highest)
			return true;

		double vi = 0;
		if (value == Symbol.current) {
			if (lastEncodedVisObj != null) {
				if (slot.getString().contains("-x"))
					vi = lastEncodedVisObj.x;
				else if (slot.getString().contains("-y"))
					vi = lastEncodedVisObj.y;
			}
		} else {
			try {
				vi = value.toDouble();
			} catch (Exception e) {
				return false;
			}
		}

		if (slot == Symbol.screenx && !(Math.abs(vo.x - vi) <= visualMovementTolerance))
			return false;
		if (slot == Symbol.screeny && !(Math.abs(vo.y - vi) <= visualMovementTolerance))
			return false;

		if (slot.getString().length() <= 8)
			return true;
		if (slot == Symbol.get("-screen-x") && !(vo.x != vi))
			return false;
		if (slot == Symbol.get("-screen-y") && !(vo.y != vi))
			return false;
		if (slot == Symbol.get("<screen-x") && !(vo.x < vi))
			return false;
		if (slot == Symbol.get("<screen-y") && !(vo.y < vi))
			return false;
		if (slot == Symbol.get(">screen-x") && !(vo.x > vi))
			return false;
		if (slot == Symbol.get(">screen-y") && !(vo.y > vi))
			return false;
		if (slot == Symbol.get("<=screen-x") && !(vo.x <= vi))
			return false;
		if (slot == Symbol.get("<=screen-y") && !(vo.y <= vi))
			return false;
		if (slot == Symbol.get(">=screen-x") && !(vo.x >= vi))
			return false;
		return slot != Symbol.get(">=screen-y") || vo.y >= vi;
	}

	private boolean matches(Chunk request, VisualObject vo) {
		Iterator<Symbol> slots = request.getSlotNames();
		while (slots.hasNext()) {
			Symbol slot = slots.next();
			Symbol value = request.get(slot);
			if (!matches(slot, value, vo))
				return false;
		}
		Iterator<SlotCondition> requestConditions = request.getRequestConditions();
		while (requestConditions.hasNext()) {
			SlotCondition sc = requestConditions.next();
			Symbol slot = (sc.getOperator() == null) ? sc.getSlot()
					: Symbol.get(sc.getOperator() + sc.getSlot().getString());
			Symbol value = sc.getValue();
			if (!matches(slot, value, vo))
				return false;
		}
		return true;
	}

	private Chunk createVisLocChunk(VisualObject vo) {
		if (vo == null)
			return null;
		Chunk visloc = new Chunk(Symbol.getUnique("vision"), model);
		visloc.set(Symbol.isa, Symbol.visloc);
		visloc.set(Symbol.kind, vo.kind);
		visloc.set(Symbol.screenx, Symbol.get(vo.x));
		visloc.set(Symbol.screeny, Symbol.get(vo.y));
		visloc.set(Symbol.width, Symbol.get(vo.w));
		visloc.set(Symbol.height, Symbol.get(vo.h));
		visloc.set(Symbol.distance, Symbol.get(vo.d));
		vo.visloc = visloc;
		vislocs.put(visloc.name(), vo);
		return visloc;
	}

	private static Symbol slotWithLowestHighest(Chunk request) {
		Iterator<Symbol> it = request.getSlotNames();
		while (it.hasNext()) {
			Symbol slot = it.next();
			Symbol value = request.get(slot);
			if (value == Symbol.lowest || value == Symbol.highest)
				return slot;
		}
		return null;
	}

	private Chunk findVisualLocation(Chunk request, Iterator<VisualObject> it) {
		try {
			Symbol nearest = request.get(Symbol.get(":nearest"));
			if (nearest != null) {
				VisualObject vo = vislocs.get(nearest);
				lastVisLocRequestX = vo.visloc.get(Symbol.screenx).toDouble();
				lastVisLocRequestY = vo.visloc.get(Symbol.screeny).toDouble();
			} else {
				lastVisLocRequestX = request.get(Symbol.screenx).toDouble();
				lastVisLocRequestY = request.get(Symbol.screeny).toDouble();
			}
		} catch (Exception e) {
			lastVisLocRequestX = 0;
			lastVisLocRequestY = 0;
		}

		Set<VisualObject> found = null;
		while (it.hasNext()) {
			VisualObject vo = it.next();
			if (matches(request, vo)) {
				if (found==null) found = new HashSet<>();
				found.add(vo);
			}
		}
		if (found == null)
			return null;

		Symbol nearest = request.get(Symbol.get(":nearest"));
		if (nearest == Symbol.nil) {
			Symbol lohiSlot = slotWithLowestHighest(request);
			if (lohiSlot != null) {
				VisualObject bestvo = null;
				int best = 0;
				for (VisualObject voTry : found) {
					int vovalue = (lohiSlot.getString().charAt(lohiSlot.getString().length() - 1) == 'x') ? voTry.x
						: voTry.y;
					if (request.get(lohiSlot) == Symbol.lowest) {
						if (bestvo == null || vovalue < best) {
							bestvo = voTry;
							best = vovalue;
						}
					} else // "highest"
					{
						if (bestvo == null || vovalue > best) {
							bestvo = voTry;
							best = vovalue;
						}
					}
				}
				request.set(lohiSlot, Symbol.get(best));
				return findVisualLocation(request, found.iterator());
			} else {
				VisualObject bestvo = found.iterator().next();
				return createVisLocChunk(bestvo);
			}
		} else {
			VisualObject vo = vislocs.get(nearest);
			if (vo == null) {
				model.outputError("*** :nearest value is not a visual-location");
				return createVisLocChunk(found.iterator().next());
			}
			Chunk vislocChunk = vo.visloc;
			double nearestX = vislocChunk.get(Symbol.screenx).toDouble();
			double nearestY = vislocChunk.get(Symbol.screeny).toDouble();
			VisualObject bestvo = null;
			double bestSq = Double.POSITIVE_INFINITY;
			for (VisualObject voTry : found) {
				double dx = voTry.x - nearestX;
				double dy = voTry.y - nearestY;
				double distSq = (dx * dx + dy * dy);
				if (bestvo == null || distSq < bestSq) {
					bestvo = voTry;
					bestSq = distSq;
				}
			}
			return createVisLocChunk(bestvo);
		}
	}

	private static double gaussianNoise(double sd) {
		double v = sd * sd;
		double s = Math.sqrt(3.0 * v) / Math.PI;
		return (s == 0) ? 0 : Utilities.getNoise(s);
	}

	private double computeEccentricity(VisualObject vo) {
		double dx = eyeX - vo.x;
		double dy = eyeY - vo.y;
		double dist = Math.sqrt(dx * dx + dy * dy);
		return Utilities.pixels2angle(dist);
	}

	private double computeEncodingTime(VisualObject vo) {
		Double freqdouble = frequencies.get(vo.value.getString());
		double frequency = (freqdouble != null) ? freqdouble : emmaDefaultFrequency;
		double t_enc = (emmaEncodingTimeFactor * (-Math.log(frequency))
				* Math.exp(emmaEncodingExponentFactor * computeEccentricity(vo)));
		double noise = gaussianNoise(t_enc / 3.0);
		if (noise < -2.0 * t_enc / 3.0)
			noise = -2.0 * t_enc / 3.0;
		t_enc += noise;
		return t_enc;
	}

	@Override
	public void update() {
		for (int i = 0; i < finsts.size(); i++) {
			VisualObject vo = finsts.get(i);
			if (vo.attendedTime < model.getTime() - visualFinstSpan) {
				vo.attended = false;
				vo.attendedTime = 0;
				finsts.remove(i);
			}
		}

		Chunk request = model.buffers.get(Symbol.visloc);
		if (request != null && request.isRequest()) {
			request.setRequest(false);
			model.buffers.clear(Symbol.visloc);
			Chunk visloc = findVisualLocation(request, visicon.values().iterator());
			if (visloc != null) {
				if (model.verboseTrace)
					model.output("vision", "find-location [" + visloc.name().getString() + "]");
				model.buffers.set(Symbol.visloc, visloc);
				model.buffers.setSlot(Symbol.vislocState, Symbol.state, Symbol.free);
				model.buffers.setSlot(Symbol.vislocState, Symbol.buffer, Symbol.requested);
			} else {
				if (model.verboseTrace)
					model.output("vision", "error");
				model.buffers.setSlot(Symbol.vislocState, Symbol.state, Symbol.error);
				model.buffers.setSlot(Symbol.vislocState, Symbol.buffer, Symbol.empty);
			}
		}

		request = model.buffers.get(Symbol.visual);
		if (request != null && request.isRequest() && request.get(Symbol.isa) == Symbol.get("move-attention")) {
			request.setRequest(false);
			model.buffers.clear(Symbol.visual);
			Symbol vislocName = request.get(Symbol.screenpos);
			if (vislocName == null) {
				model.outputWarning("bad visual location");
				return;
			}
			final VisualObject vo = vislocs.get(vislocName);
			Chunk visloc = vo.visloc;
			if (visloc == null) {
				model.outputWarning("bad visual location");
				return;
			}
			Symbol kind = visloc.get(Symbol.kind);
			final Chunk visual = new Chunk(Symbol.getUnique(kind.getString()), model);
			visual.set(Symbol.isa, kind);
			visual.set(Symbol.screenpos, vislocName);
			visual.set(Symbol.value, vo.value);
			visual.set(Symbol.width, Symbol.get(vo.w));
			visual.set(Symbol.height, Symbol.get(vo.h));
			if (model.verboseTrace)
				model.output("vision", "move-attention");
			model.buffers.setSlot(Symbol.visualState, Symbol.state, Symbol.busy);
			model.buffers.setSlot(Symbol.visualState, Symbol.buffer, Symbol.requested);

			double encodingTime = (useEMMA) ? computeEncodingTime(vo) : model.randomizeTime(visualAttentionLatency);
			vo.encodingStart = model.getTime();
			vo.encodingTime = encodingTime;
			lastEncodedVisObj = vo;

			model.addEvent(new Event(model.getTime() + encodingTime, "vision",
					"encoding-complete [" + visual.name() + "]") {
				@Override
				public void action() {
					model.getTask().moveAttention(vo.x, vo.y);
					vo.attended = true;
					vo.attendedTime = model.getTime();
					finsts.add(vo);
					if (finsts.size() > visualNumFinsts) {
						finsts.remove(0).attended = false;
					}
					model.buffers.set(Symbol.visual, visual);
					model.buffers.setSlot(Symbol.visualState, Symbol.state, Symbol.free);
					model.buffers.setSlot(Symbol.visualState, Symbol.buffer, Symbol.full);
				}
			});

			if (useEMMA)
				prepareEyeMovement(vo, visual);
		}
	}

	private void prepareEyeMovement(final VisualObject vo, final Chunk visual) {
		model.removeEvents("eye", "preparation-complete");

		model.addEvent(new Event(model.getTime() + model.randomizeTime(emmaPreparationTime), "eye",
				"preparation-complete [" + visual.name() + "]") {
			@Override
			public void action() {
				executeEyeMovement(vo, visual);
			}
		});
	}

	private void executeEyeMovement(final VisualObject vo, final Chunk visual) {
		double executionTime = model
				.randomizeTime(emmaExecutionBaseTime + emmaExecutionTimeIncrement * computeEccentricity(vo));

		model.addEvent(
				new Event(model.getTime() + executionTime, "eye", "execution-complete [" + visual.name() + "]") {
					@Override
					public void action() {
						double sd = .1 * Utilities.angle2pixels(computeEccentricity(vo));
						eyeX = vo.x + (int) (Math.round(gaussianNoise(sd)));
						eyeY = vo.y + (int) (Math.round(gaussianNoise(sd)));
						model.getTask().moveEye(eyeX, eyeY);

						if (vo.encodingStart + vo.encodingTime > model.getTime()) {
							double completed = (model.getTime() - vo.encodingStart) / vo.encodingTime;
							double newEncodingTime = computeEncodingTime(vo);
							double remainingEncodingTime = (1.0 - completed) * newEncodingTime;
							if (remainingEncodingTime <= 0)
								return;
							model.changeEventTime("vision", "encoding-complete",
									model.getTime() + remainingEncodingTime);
							vo.encodingStart = model.getTime();
							vo.encodingTime = remainingEncodingTime;
							prepareEyeMovement(vo, visual);
						}
					}
				});
	}

	/**
	 * Generates a multi-line string with all current visual objects.
	 */
	public String visualObjects() {
		String s = "";
		for (VisualObject visualObject : visicon.values()) s += visualObject + "\n";
		return s;
	}
}
