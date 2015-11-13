package actr.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * The motor module representing ACT-R's manual movements.
 * 
 * @author Dario Salvucci
 */
public class Motor extends Module {
	private Model model;
	private Chunk lastMovement;
	private double lastMovementTime;
	private int mx, my;
	private Map<String, Point> keys;
	private Map<String, String> keycmds;
	private Point leftHand, rightHand;
	private Vector<Chunk> queuedMovements;

	double featurePrepTime = .050;
	double movementInitiationTime = .050;
	double burstTime = .050;
	double peckFittsCoeff = .075;
	double mouseFittsCoeff = .100;
	double minFittsTime = .100;
	double defaultTargetWidth = 1.0;

	double maxPrepTimeDifference = 5.0; // new XXX was 10.0 (change from 5 to 10 by Ehsan for fatigue module)
	final double keyClosureTime = .010;
	final Point leftHomeKey = new Point(4, 4);
	final Point rightHomeKey = new Point(7, 4);
	final Point mouseKey = new Point(28, 2);

	Motor(Model model) {
		this.model = model;
		lastMovement = null;
		mx = my = 0;
		keys = new HashMap<String, Point>();
		keycmds = new HashMap<String, String>();
		leftHand = leftHomeKey;
		rightHand = rightHomeKey;
		queuedMovements = new Vector<Chunk>();
		populateKeys();
		populateKeyCommands();
	}

	@Override
	void initialize() {
		// model.getTask().moveMouse (mx, my);
	}

	private class Point {
		int x, y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		Point copy() {
			return new Point(x, y);
		}

		boolean equals(Point p2) {
			return (x == p2.x && y == p2.y);
		}

		Point translate(int dx, int dy) {
			x += dx;
			y += dy;
			return this;
		}

		Point translate(double r, double theta) {
			x = (int) Math.round(x + (r * Math.cos(theta)));
			y = (int) Math.round(y + (r * Math.sin(theta)));
			return this;
		}

		double distanceTo(Point to) {
			return Math.sqrt((x - to.x) * (x - to.x) + (y - to.y) * (y - to.y));
		}

		double angleTo(Point to) {
			if (distanceTo(to) == 0)
				return 0;
			else
				return Math.atan2(to.y - y, to.x - x);
		}

		@Override
		public String toString() {
			return "(" + x + " " + y + ")";
		}
	}

	void moveHandToMouse() {
		rightHand = mouseKey;
		model.getBuffers().setSlot(Symbol.manualState, Symbol.where, Symbol.mouse);
	}

	void moveHandToHome() {
		rightHand = rightHomeKey;
		model.getBuffers().setSlot(Symbol.manualState, Symbol.where, Symbol.keyboard);
	}

	private class IncrementalMove {
		int startx, starty, endx, endy;
		double startTime, endTime;
		int fracIndex;

		IncrementalMove(double execTime, int startx, int starty, int endx, int endy) {
			this.startx = startx;
			this.starty = starty;
			this.endx = endx;
			this.endy = endy;
			startTime = model.getTime();
			endTime = startTime + execTime;
			fracIndex = 0;
		}
	}

	private int countFeaturesToPrepare(Chunk request) {
		if (request.get(Symbol.isa) == Symbol.get("clear"))
			return 1;
		int n = request.slotCount();
		if (lastMovement == null || (model.getTime() - lastMovementTime) > maxPrepTimeDifference)
			return n;
		if (request.get(Symbol.isa) != lastMovement.get(Symbol.isa))
			return n;
		n--;
		if (request.get(Symbol.hand) != lastMovement.get(Symbol.hand))
			return n;
		n--;
		if (request.get(Symbol.finger) != Symbol.nil) {
			if (request.get(Symbol.finger) != lastMovement.get(Symbol.finger))
				return n;
			n--;
		}
		Iterator<Symbol> it = request.getSlotNames();
		while (it.hasNext()) {
			Symbol slot = it.next();
			if (slot == Symbol.isa || slot == Symbol.hand || slot == Symbol.finger)
				continue;
			if (slot == Symbol.get("r")) {
				Symbol rv = request.get(slot);
				Symbol lv = lastMovement.get(slot);
				if (rv != null && lv != null && Math.abs(rv.toDouble() - lv.toDouble()) <= 2.0)
					n--;
			} else if (slot == Symbol.get("theta")) {
				Symbol rv = request.get(slot);
				Symbol lv = lastMovement.get(slot);
				if (rv != null && lv != null && Math.abs(rv.toDouble() - lv.toDouble()) <= Math.PI / 4)
					n--;
			} else if (request.get(slot) == lastMovement.get(slot))
				n--;
		}
		return n;
	}

	private final double cumulative[] = { 0.000, 0.063, 0.269, 0.506, 0.697, 0.826, 0.905, 0.950, 0.975, 0.987, 1.0 };

	private void incrementalMove(final IncrementalMove im) {
		final double timeFrac = 1.0 * im.fracIndex / cumulative.length;
		final double moveFrac = cumulative[im.fracIndex];
		double eventTime = ((1.0 - timeFrac) * im.startTime) + (timeFrac * im.endTime);
		model.addEvent(new Event(eventTime, "task", "update") {
			@Override
			public void action() {
				mx = (int) Math.round(((1.0 - moveFrac) * im.startx) + (moveFrac * im.endx));
				my = (int) Math.round(((1.0 - moveFrac) * im.starty) + (moveFrac * im.endy));
				model.getTask().moveMouse(mx, my);
				if (im.fracIndex < cumulative.length - 1) {
					im.fracIndex++;
					incrementalMove(im);
				}
			}
		});
	}

	private double prepareMovement(double time, Chunk request) {
		time += model.randomizeTime(featurePrepTime * countFeaturesToPrepare(request));
		model.getBuffers().setSlot(Symbol.manualState, Symbol.preparation, Symbol.busy);
		model.getBuffers().setSlot(Symbol.manualState, Symbol.processor, Symbol.busy);
		model.getBuffers().setSlot(Symbol.manualState, Symbol.state, Symbol.busy);
		model.addEvent(new Event(time, "motor", "preparation-complete") {
			@Override
			public void action() {
				model.getBuffers().setSlot(Symbol.manualState, Symbol.preparation, Symbol.free);
			}
		});
		lastMovement = request;
		lastMovementTime = time;
		return time;
	}

	private double initiateMovement(double time) {
		time += model.randomizeTime(movementInitiationTime);
		model.addEvent(new Event(time, "motor", "initiation-complete") {
			@Override
			public void action() {
				model.getBuffers().setSlot(Symbol.manualState, Symbol.processor, Symbol.free);
				model.getBuffers().setSlot(Symbol.manualState, Symbol.execution, Symbol.busy);
			}
		});
		return time;
	}

	private void finishMovement(double time) {
		model.addEvent(new Event(time, "motor", "finish-movement") {
			@Override
			public void action() {
				if (queuedMovements.size() == 0) {
					model.getBuffers().setSlot(Symbol.manualState, Symbol.execution, Symbol.free);
					model.getBuffers().setSlot(Symbol.manualState, Symbol.state, Symbol.free);
					model.getBuffers().setSlot(Symbol.manualState, Symbol.where,
							(rightHand.equals(mouseKey) ? Symbol.mouse : Symbol.keyboard));
				} else {
					Chunk c = queuedMovements.firstElement();
					queuedMovements.removeElementAt(0);
					model.getBuffers().set(Symbol.manual, c);
					update();
				}
			}
		});
	}

	@Override
	void update() {
		Chunk request = model.getBuffers().get(Symbol.manual);
		if (request == null || !request.isRequest())
			return;
		request.setRequest(false);
		model.getBuffers().clear(Symbol.manual);

		double eventTime = model.getTime();
		double targetWidth = defaultTargetWidth;
		boolean seen = false;

		if (request.get(Symbol.isa) == Symbol.get("clear")) {
			if (model.verboseTrace)
				model.output("motor", "clear");
			model.getBuffers().setSlot(Symbol.manualState, Symbol.preparation, Symbol.busy);
			model.getBuffers().setSlot(Symbol.manualState, Symbol.state, Symbol.busy);
			model.addEvent(new Event(model.getTime() + featurePrepTime, "motor", "change state last none prep free") {
				@Override
				public void action() {
					lastMovement = null;
					model.getBuffers().setSlot(Symbol.manualState, Symbol.preparation, Symbol.free);
					model.getBuffers().setSlot(Symbol.manualState, Symbol.state, Symbol.free);
				}
			});
			return;
		}

		if (request.get(Symbol.isa) == Symbol.get("type")) {
			Symbol stringsym = request.get(Symbol.get("string"));
			if (stringsym == null) {
				model.outputWarning("string not specified");
				return;
			}
			String s = stringsym.getString().replaceAll("\"", "").toLowerCase();
			if (!seen && model.verboseTrace)
				model.output("motor", "type " + s);
			queuedMovements.removeAllElements();
			for (int i = 0; i < s.length(); i++) {
				String key = "" + s.charAt(i);
				String slots = getKeyCommand(key);
				if (slots == null) {
					model.outputWarning("bad key '" + key + "'");
					return;
				}
				Chunk c = Parser.parseNewChunk(model, Symbol.getUnique("press-key"), "isa " + slots);
				c.setRequest(true);
				queuedMovements.addElement(c);
			}
			if (queuedMovements.size() > 0) {
				request = queuedMovements.firstElement();
				queuedMovements.removeElementAt(0);
			} else
				return;
			seen = true;
		}

		if (request.get(Symbol.isa) == Symbol.get("press-key")) {
			Symbol keysym = request.get(Symbol.get("key"));
			if (keysym == null) {
				model.outputWarning("key not specified");
				return;
			}
			String key = keysym.getString().toLowerCase();
			if (!seen && model.verboseTrace)
				model.output("motor", "press-key " + key);
			String slots = getKeyCommand(key);
			if (slots == null) {
				model.outputWarning("bad key '" + key + "'");
				return;
			}
			request = Parser.parseNewChunk(model, Symbol.getUnique("press-key"), "isa " + slots);
			seen = true;
		}

		if (request.get(Symbol.isa) == Symbol.get("hand-to-mouse")) {
			if (!seen && model.verboseTrace)
				model.output("motor", "hand-to-mouse");
			String slots = "isa point-hand-at-key hand right to-key mouse";
			request = Parser.parseNewChunk(model, Symbol.getUnique("hand-to-mouse"), slots);
			targetWidth = 4.0;
			seen = true;
		}

		if (request.get(Symbol.isa) == Symbol.get("hand-to-home")) {
			if (!seen && model.verboseTrace)
				model.output("motor", "hand-to-home");
			String slots = "isa point-hand-at-key hand right to-key j";
			request = Parser.parseNewChunk(model, Symbol.getUnique("hand-to-home"), slots);
			targetWidth = 4.0;
			seen = true;
		}

		if (request.get(Symbol.isa) == Symbol.get("move-cursor")) {
			Symbol locName = request.get(Symbol.get("loc"));
			if (!seen && model.verboseTrace)
				model.output("motor", "move-cursor " + locName);
			if (!rightHand.equals(mouseKey)) {
				model.outputWarning("hand not on mouse");
				return;
			}
			Chunk loc = model.getVision().getVisualLocation(locName);
			final Point locpt = new Point(loc.get(Symbol.get("screen-x")).toInt(),
					loc.get(Symbol.get("screen-y")).toInt());
			Point mousePoint = new Point(mx, my);
			double r = Utilities.pixels2angle(mousePoint.distanceTo(locpt));
			if (r == 0)
				return;
			double theta = mousePoint.angleTo(locpt);
			double width = approachWidth(loc, theta);
			double fittsTime = fitts(mouseFittsCoeff, r, width);
			final double moveTime = Math.max(burstTime, fittsTime);
			String slots = "isa ply hand right r " + r + " theta " + theta;
			Chunk featRequest = Parser.parseNewChunk(model, Symbol.getUnique("move-cursor"), slots);

			eventTime = prepareMovement(eventTime, featRequest);
			eventTime = initiateMovement(eventTime);
			if (model.realTime)
				model.addEvent(new Event(eventTime, "task", "none") {
					@Override
					public void action() {
						incrementalMove(new IncrementalMove(moveTime, mx, my, locpt.x, locpt.y));
					}
				});
			eventTime += model.randomizeTime(moveTime);
			model.addEvent(new Event(eventTime, "motor", "move cursor " + locpt) {
				@Override
				public void action() {
					model.getTask().moveMouse(locpt.x, locpt.y);
				}
			});
			eventTime += model.randomizeTime(burstTime);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("click-mouse")) {
			if (!seen && model.verboseTrace)
				model.output("motor", "click-mouse");
			if (!rightHand.equals(mouseKey)) {
				model.outputWarning("hand not on mouse");
				return;
			}
			String slots = "isa punch hand right finger index";
			Chunk featRequest = Parser.parseNewChunk(model, Symbol.getUnique("click-mouse"), slots);

			eventTime = prepareMovement(eventTime, featRequest);
			eventTime = initiateMovement(eventTime);
			double actualKeyClosureTime = model.randomizeTime(keyClosureTime);
			eventTime += actualKeyClosureTime;
			model.addEvent(new Event(eventTime, "motor", "output mouse click") {
				@Override
				public void action() {
					model.getTask().clickMouse();
				}
			});
			eventTime -= actualKeyClosureTime;
			eventTime += model.randomizeTime(2 * burstTime);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("point-hand-at-key")) {
			Symbol toKeySym = request.get(Symbol.get("to-key"));
			if (toKeySym == Symbol.nil) {
				model.outputWarning("to-key not specified");
				return;
			}
			String toKey = toKeySym.getString().toLowerCase();
			final Symbol hand = request.get(Symbol.hand);
			if (!seen && model.verboseTrace)
				model.output("motor", "point-hand-at-key " + hand + " " + toKey);
			final Point keypos = getKeyPosition(toKey).copy();
			double r = (hand == Symbol.right) ? rightHand.distanceTo(keypos) : leftHand.distanceTo(keypos);
			double theta = (hand == Symbol.right) ? rightHand.angleTo(keypos) : leftHand.angleTo(keypos);
			double fittsTime = fitts(mouseFittsCoeff, r, targetWidth);
			double moveTime = Math.max(burstTime, fittsTime);
			String slots = "isa ply hand right r " + r + " theta " + theta;
			Chunk featRequest = Parser.parseNewChunk(model, Symbol.getUnique("point-hand-at-key"), slots);

			eventTime = prepareMovement(eventTime, featRequest);
			eventTime = initiateMovement(eventTime);
			eventTime += model.randomizeTime(Math.max(burstTime, moveTime));
			model.addEvent(new Event(eventTime, "motor", "move hand " + toKey) {
				@Override
				public void action() {
					if (hand == Symbol.right)
						rightHand = keypos;
					else
						leftHand = keypos;
				}
			});
			eventTime += model.randomizeTime(burstTime);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("punch")) {
			Symbol hand = request.get(Symbol.hand);
			Symbol finger = request.get(Symbol.finger);
			if (!seen && model.verboseTrace)
				model.output("motor", "punch " + hand + " " + finger);
			if (hand == Symbol.right && rightHand.equals(mouseKey)) {
				model.outputWarning("hand not on keyboard");
				return;
			}
			Point pt = getFingerPosition(hand, finger);
			final String key = getMappedKey(pt);

			eventTime = prepareMovement(eventTime, request);
			eventTime = initiateMovement(eventTime);
			double actualKeyClosureTime = model.randomizeTime(keyClosureTime);
			eventTime += actualKeyClosureTime;
			model.addEvent(new Event(eventTime, "motor", "output key " + key) {
				@Override
				public void action() {
					if (key.equals("mouse"))
						model.getTask().clickMouse();
					else
						model.getTask().typeKey(convertToChar(key));
				}
			});
			eventTime -= actualKeyClosureTime;
			eventTime += model.randomizeTime(2 * burstTime);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("peck")) {
			Symbol hand = request.get(Symbol.hand);
			Symbol finger = request.get(Symbol.finger);
			Symbol r = request.get(Symbol.get("r"));
			Symbol theta = request.get(Symbol.get("theta"));
			if (!seen && model.verboseTrace)
				model.output("motor", "peck " + hand + " " + finger + " " + r + " " + theta);
			if (hand == Symbol.right && rightHand.equals(mouseKey)) {
				model.outputWarning("hand not on keyboard");
				return;
			}
			Point pt = getFingerPosition(hand, finger);
			pt = pt.translate(r.toDouble(), theta.toDouble());
			final String key = getMappedKey(pt);
			double fittsTime = fitts(peckFittsCoeff, r.toDouble(), 1.0);

			eventTime = prepareMovement(eventTime, request);
			eventTime = initiateMovement(eventTime);
			eventTime += model.randomizeTime(Math.max(burstTime, fittsTime));
			model.addEvent(new Event(eventTime, "motor", "output key " + key) {
				@Override
				public void action() {
					model.getTask().typeKey(convertToChar(key));
				}
			});
			eventTime += model.randomizeTime(burstTime);
			finishMovement(eventTime);
		}

		else if (request.get(Symbol.isa) == Symbol.get("peck-recoil")) {
			Symbol hand = request.get(Symbol.hand);
			Symbol finger = request.get(Symbol.finger);
			Symbol r = request.get(Symbol.get("r"));
			Symbol theta = request.get(Symbol.get("theta"));
			if (!seen && model.verboseTrace)
				model.output("motor", "peck-recoil " + hand + " " + finger + " " + r + " " + theta);
			if (hand == Symbol.right && rightHand.equals(mouseKey)) {
				model.outputWarning("hand not on keyboard");
				return;
			}
			Point pt = getFingerPosition(hand, finger);
			pt = pt.translate(r.toDouble(), theta.toDouble());
			final String key = getMappedKey(pt);
			double fittsTime = fitts(peckFittsCoeff, r.toDouble(), 1.0);
			double moveTime = Math.max(burstTime, fittsTime);

			eventTime = prepareMovement(eventTime, request);
			eventTime = initiateMovement(eventTime);
			eventTime += model.randomizeTime(Math.max(burstTime, moveTime));
			model.addEvent(new Event(eventTime, "motor", "output key " + key) {
				@Override
				public void action() {
					model.getTask().typeKey(convertToChar(key));
				}
			});
			eventTime += model.randomizeTime(burstTime + moveTime);
			finishMovement(eventTime);
		}
	}

	private String getMappedKey(Point pt) {
		if (pt.equals(mouseKey))
			return "mouse";
		if (pt.y > keymap.length)
			return "error";
		if (pt.x > keymap[pt.y].length)
			return "error";
		return keymap[pt.y][pt.x];
	}

	private Point getFingerPosition(Symbol hand, Symbol finger) {
		if (hand == Symbol.right) {
			Point pt = rightHand.copy();
			if (finger == Symbol.get("middle"))
				return pt.translate(1, 0);
			else if (finger == Symbol.get("ring"))
				return pt.translate(2, 0);
			else if (finger == Symbol.get("pinkie"))
				return pt.translate(3, 0);
			else if (finger == Symbol.get("thumb"))
				return pt.translate(-1, 2);
			else
				return pt;
		} else {
			Point pt = leftHand.copy();
			if (finger == Symbol.get("middle"))
				return pt.translate(-1, 0);
			else if (finger == Symbol.get("ring"))
				return pt.translate(-2, 0);
			else if (finger == Symbol.get("pinkie"))
				return pt.translate(-3, 0);
			else if (finger == Symbol.get("thumb"))
				return pt.translate(1, 2);
			else
				return pt;
		}
	}

	private Point getKeyPosition(String key) {
		key = key.toLowerCase();
		return keys.get(key);
	}

	private char convertToChar(String keyname) {
		keyname = keyname.replaceAll("\"", "").toLowerCase();
		if (keyname.length() == 0)
			return '-';
		else if (keyname.equals("space"))
			return ' ';
		else if (keyname.equals("return"))
			return '\n';
		else if (keyname.equals("period"))
			return '.';
		else if (keyname.equals("comma"))
			return ',';
		else
			return keyname.charAt(0);
	}

	private String getKeyCommand(String keyname) {
		keyname = keyname.replaceAll("\"", "").toLowerCase();
		return keycmds.get(keyname);
	}

	private double fitts(double coeff, double d, double w) {
		if (w == 0)
			w = Utilities.pixels2angle(1.0);
		double f = Math.log((d / w) + .5) / Math.log(2);
		return Math.max(minFittsTime, coeff * f);
	}

	private double approachWidth(Chunk loc, double theta) {
		Symbol s = loc.get(Symbol.get("width"));
		if (s == Symbol.nil)
			return 0;
		double x = s.toDouble();
		s = loc.get(Symbol.get("height"));
		if (s == Symbol.nil)
			return 0;
		double y = s.toDouble();
		double criticalTheta = Math.atan2(y, x);
		theta = Math.abs(theta);
		if (theta > Math.PI / 2)
			theta = Math.PI - theta;
		double retWidth = 0;
		if (theta == 0)
			retWidth = x;
		else if (theta == Math.PI / 2)
			retWidth = y;
		else if (theta == criticalTheta)
			retWidth = Math.sqrt(x * x + y * y);
		else if (theta < criticalTheta)
			retWidth = x / Math.cos(theta);
		else
			retWidth = y / Math.cos((Math.PI / 2) - theta);
		return Utilities.pixels2angle(retWidth);
	}

	String keymap[][] = {
			{ "escape", "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", "f13", "f14",
					"f15" },
			{},
			{ "backquote", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "hyphen", "=", "delete", "help",
					"home", "pageup", "clear", "=", "/", "*" },
			{ "tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "backslash", "forward-delete", "end",
					"page-up", "keypad-7", "keypad-8", "keypad-9", "keypad-hyphen" },
			{ "caps-lock", "a", "s", "d", "f", "g", "h", "j", "k", "l", "semicolon", "quote", "return", "keypad-4",
					"keypad-5", "keypad-6", "keypad-plus" },
			{ "shift", "z", "x", "c", "v", "b", "n", "m", "comma", "period", "dot", "/", "right-shift", "up-arrow",
					"keypad-1", "keypad-2", "keypad-3", "keypad-enter" },
			{ "left-control", "left-option", "left-command", "spc", "spc", "spc", "spc", "spc", "spc", "spc", "spc",
					"right-command", "right-option", "right-control", "left-arrow", "down-arrow", "right-arrow",
					"keypad-0", "keypad-period", "enter" } };

	private void populateKeys() {
		for (int y = 0; y < keymap.length; y++)
			for (int x = 0; x < keymap[y].length; x++)
				keys.put(keymap[y][x], new Point(x, y));
		keys.put("mouse", mouseKey);
	}

	private void populateKeyCommands() {
		keycmds.put("space", "punch hand left finger thumb");
		keycmds.put("backquote", "peck-recoil hand left finger pinkie r 2.24 theta -2.03");
		keycmds.put("tab", "peck-recoil hand left finger pinkie r 1.41 theta -2.36");
		keycmds.put("1", "peck-recoil hand left finger pinkie r 2 theta -1.57");
		keycmds.put("q", "peck-recoil hand left finger pinkie r 1 theta -1.57");
		keycmds.put("a", "punch hand left finger pinkie");
		keycmds.put("z", "peck-recoil hand left finger pinkie r 1 theta 1.57");
		keycmds.put("2", "peck-recoil hand left finger ring r 2 theta -1.57");
		keycmds.put("w", "peck-recoil hand left finger ring r 1 theta -1.57");
		keycmds.put("s", "punch hand left finger ring");
		keycmds.put("x", "peck-recoil hand left finger ring r 1 theta 1.57");
		keycmds.put("3", "peck-recoil hand left finger middle r 2 theta -1.57");
		keycmds.put("e", "peck-recoil hand left finger middle r 1 theta -1.57");
		keycmds.put("d", "punch hand left finger middle");
		keycmds.put("c", "peck-recoil hand left finger middle r 1 theta 1.57");
		keycmds.put("4", "peck-recoil hand left finger index r 2 theta -1.57");
		keycmds.put("r", "peck-recoil hand left finger index r 1 theta -1.57");
		keycmds.put("f", "punch hand left finger index");
		keycmds.put("v", "peck-recoil hand left finger index r 1 theta 1.57");
		keycmds.put("5", "peck-recoil hand left finger index r 2.24 theta -1.11");
		keycmds.put("t", "peck-recoil hand left finger index r 1.41 theta -0.79");
		keycmds.put("g", "peck-recoil hand left finger index r 1 theta 0");
		keycmds.put("b", "peck-recoil hand left finger index r 1.41 theta 0.79");
		keycmds.put("6", "peck-recoil hand right finger index r 2.24 theta -2.03");
		keycmds.put("y", "peck-recoil hand right finger index r 1.41 theta -2.36");
		keycmds.put("h", "peck-recoil hand right finger index r 1 theta 3.14");
		keycmds.put("n", "peck-recoil hand right finger index r 1.41 theta 2.36");
		keycmds.put("7", "peck-recoil hand right finger index r 2 theta -1.57");
		keycmds.put("u", "peck-recoil hand right finger index r 1 theta -1.57");
		keycmds.put("j", "punch hand right finger index");
		keycmds.put("m", "peck-recoil hand right finger index r 1 theta 1.57");
		keycmds.put("8", "peck-recoil hand right finger middle r 2 theta -1.57");
		keycmds.put("i", "peck-recoil hand right finger middle r 1 theta -1.57");
		keycmds.put("k", "punch hand right finger middle");
		keycmds.put("comma", "peck-recoil hand right finger middle r 1 theta 1.57");
		keycmds.put("9", "peck-recoil hand right finger ring r 2 theta -1.57");
		keycmds.put("o", "peck-recoil hand right finger ring r 1 theta -1.57");
		keycmds.put("l", "punch hand right finger ring");
		keycmds.put("period", "peck-recoil hand right finger ring r 1 theta 1.57");
		keycmds.put("0", "peck-recoil hand right finger pinkie r 2 theta -1.57");
		keycmds.put("p", "peck-recoil hand right finger pinkie r 1 theta -1.57");
		keycmds.put("semicolon", "punch hand right finger pinkie");
		keycmds.put("slash", "peck-recoil hand right finger pinkie r 1 theta 1.57");
		keycmds.put("hyphen", "peck-recoil hand right finger pinkie r 2.24 theta -1.11");
		keycmds.put("-", "peck-recoil hand right finger pinkie r 2.24 theta -1.11");
		keycmds.put("[", "peck-recoil hand right finger pinkie r 1.41 theta -0.78");
		keycmds.put("quote", "peck-recoil hand right finger pinkie r 1 theta 0");
		keycmds.put("return", "peck-recoil hand right finger pinkie r 2 theta 0");
		keycmds.put("mouse", "punch hand right finger index");
	}
}
