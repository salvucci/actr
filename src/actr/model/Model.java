package actr.model;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import actr.env.Frame;
import actr.task.Task;

/**
 * The highest-level class representing an ACT-R model.
 * 
 * @author Dario Salvucci
 */
public class Model {
	public final Declarative declarative;
	public final Procedural procedural;
	public final Vision vision;
	public final Audio audio;
	public final Motor motor;
	public final Speech speech;
	public final Imaginal imaginal;
	public final Temporal temporal;
	public final Bold bold;
	public final Buffers buffers;
	public final Events events;
	private Task task;
	private double time;
	private boolean stop;
	private boolean taskUpdated;
	private int currentThreadID = 1;
	public final List<ParseError> errors;
	public final Frame frame;

	private Fatigue fatigue;

	boolean realTime = false;
	double realTimeMultiplier = 1;
	boolean verboseTrace = true;
	boolean runUntilStop = false;
	final boolean bufferStuffing = true;
	boolean randomizeTime = false;
	double randomizeTimeValue = 3;

	private Model(Frame frame) {
		this.frame = frame;
		errors = new ArrayList<>();
		declarative = new Declarative(this);
		procedural = new Procedural(this);
		vision = new Vision(this);
		audio = new Audio(this);
		motor = new Motor(this);
		speech = new Speech(this);
		imaginal = new Imaginal(this);
		temporal = new Temporal(this);
		bold = new Bold(this);
		buffers = new Buffers(this);
		events = new Events();
		time = 0;
		task = new Task();
		taskUpdated = false;

		fatigue = new Fatigue(this);

		initialize();
	}

	/**
	 * Compiles a model from a string, while also overriding the given task. The
	 * enclosing frame is also needed to provide a way to print output to the
	 * screen.
	 * 
	 * @param text
	 *            the model text
	 * @param frame
	 *            the enclosing frame
	 * @param taskOverride
	 *            the given task
	 * @return the compiled model
	 * @throws ParseException
	 */
	public static Model compile(String text, Frame frame, String taskOverride) {
		Model model = new Model(frame);
		new Parser(text).parse(model, taskOverride);
		return model;
	}

	/**
	 * Compiles a model from a string. The enclosing frame is also needed to
	 * provide a way to print output to the screen.
	 * 
	 * @param text
	 *            the model text
	 * @param frame
	 *            the enclosing frame
	 * @return the compiled model
	 * @throws ParseException
	 */
	public static Model compile(String text, Frame frame) {
		return compile(text, frame, null);
	}

	/**
	 * Compiles a model from a file. The enclosing frame is also needed to
	 * provide a way to print output to the screen.
	 * 
	 * @param file
	 *            the model file
	 * @param frame
	 *            the enclosing frame
	 * @return the compiled model
	 * @throws ParseException
	 */
	public static Model compile(File file, Frame frame, String taskOverride) {
		Model model = new Model(frame);
		new Parser(file).parse(model, taskOverride);
		return model;
	}

	public static Model compile(File file, Frame frame) {
		return compile(file, frame, null);
	}

	/**
	 * Gets the fatigue module.
	 *
	 * @return the fatigue module
	 */
	public Fatigue getFatigue() {
		return fatigue;
	}

	/**
	 * Gets the declarative module.
	 * 
	 * @return the declarative module
	 */
	public Declarative getDeclarative() {
		return declarative;
	}

	/**
	 * Gets the procedural module.
	 * 
	 * @return the procedural module
	 */
	public Procedural getProcedural() {
		return procedural;
	}

	/**
	 * Gets the vision module.
	 * 
	 * @return the vision module
	 */
	public Vision getVision() {
		return vision;
	}

	/**
	 * Gets the audio module.
	 * 
	 * @return the audio module
	 */
	public Audio getAudio() {
		return audio;
	}

	/**
	 * Gets the motor module.
	 * 
	 * @return the motor module
	 */
	public Motor getMotor() {
		return motor;
	}

	/**
	 * Gets the speech module.
	 * 
	 * @return the speech module
	 */
	public Speech getSpeech() {
		return speech;
	}

	/**
	 * Gets the imaginal module.
	 * 
	 * @return the imaginal module
	 */
	public Imaginal getImaginal() {
		return imaginal;
	}

	/**
	 * Gets the temporal module.
	 * 
	 * @return the temporal module
	 */
	public Temporal getTemporal() {
		return temporal;
	}

	/**
	 * Gets the BOLD module.
	 * 
	 * @return the BOLD module
	 */
	public Bold getBold() {
		return bold;
	}

	/**
	 * Gets the current buffers.
	 * 
	 * @return the buffers
	 */
	public Buffers getBuffers() {
		return buffers;
	}

	/**
	 * Gets the current events queue.
	 * 
	 * @return the events queue
	 */
	public Events getEvents() {
		return events;
	}

	/**
	 * Gets the task for the model to perform.
	 * 
	 * @return the current task
	 */
	public Task getTask() {
		return task;
	}

	/**
	 * Sets the task for the model to perform.
	 * 
	 * @param task
	 *            the task to be performed by the model
	 */
	public void setTask(Task task) {
		this.task = task;
		task.setModel(this);
	}

	int getCurrentThreadID() {
		return currentThreadID;
	}

	int getNewThreadID() {
		return (++currentThreadID);
	}

	public Iterator<ParseError> getErrors() {
		return errors.iterator();
	}

	public boolean hasFatalErrors() {
		Iterator<ParseError> it = getErrors();
		while (it.hasNext())
			if (it.next().isFatal())
				return true;
		return false;
	}

	void clearErrors() {
		errors.clear();
	}

	/**
	 * Returns the current time of the model simulation.
	 * 
	 * @return the current time in seconds from the start of simulation
	 */
	public double getTime() {
		return time;
	}

	double randomizeTime(double time) {
		if (randomizeTime) {
			double min = time * ((randomizeTimeValue - 1) / randomizeTimeValue);
			double max = time * ((randomizeTimeValue + 1) / randomizeTimeValue);
			return min + (max - min) * Utilities.random.nextDouble();
		} else
			return time;
	}

	/**
	 * Returns true if the model is running in real time.
	 * 
	 * @return true if the model is running in real time
	 */
	public boolean getRealTime() {
		return realTime;
	}

	Chunk createBufferStateChunk(String buffer, boolean hasBuffer) {
		Chunk c = new Chunk(Symbol.get(buffer), this);
		c.set(Symbol.get("isa"), Symbol.get("buffer-state"));
		c.set(Symbol.get("state"), Symbol.get("free"));
		if (hasBuffer)
			c.set(Symbol.get("buffer"), Symbol.get("empty"));
		return c;
	}

	void initialize() {
		Symbol.reset();

		declarative.initialize();
		procedural.initialize();
		vision.initialize();
		audio.initialize();
		motor.initialize();
		speech.initialize();
		imaginal.initialize();
		fatigue.initialize();

		buffers.set(Symbol.goalState, createBufferStateChunk("goal", true));
		buffers.set(Symbol.retrievalState, createBufferStateChunk("retrieval-state", true));
		buffers.set(Symbol.vislocState, createBufferStateChunk("visloc-state", true));
		buffers.set(Symbol.visualState, createBufferStateChunk("visual-state", true));
		buffers.set(Symbol.aurlocState, createBufferStateChunk("aurloc-state", true));
		buffers.set(Symbol.auralState, createBufferStateChunk("aural-state", true));
		buffers.set(Symbol.manualState, createBufferStateChunk("manual-state", false));
		buffers.set(Symbol.vocalState, createBufferStateChunk("vocal-state", false));
		buffers.set(Symbol.imaginalState, createBufferStateChunk("imaginal-state", true));
		buffers.set(Symbol.temporalState, createBufferStateChunk("temporal-state", true));

		buffers.setSlot(Symbol.manualState, Symbol.where, Symbol.keyboard);
	}

	void update() {
		vision.update();
		audio.update();
		motor.update();
		speech.update();
		temporal.update();
		declarative.update();
		imaginal.update();
		procedural.update();
		// fatigue.update();
	}

	/**
	 * Adds an event to the model event queue.
	 * 
	 * @param event
	 *            the event to add
	 */
	public void addEvent(Event event) {
		events.add(event);
	}

	/**
	 * Checks whether there is currently an event scheduled for a particular
	 * module.
	 * 
	 * @param module
	 *            the module
	 * @return <tt>true</tt> if there is an event scheduled for the module, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean hasEvent(String module) {
		return events.scheduled(module);
	}

	/**
	 * Checks whether there is currently an event scheduled for a particular
	 * module with a description that starts with the given prefix.
	 * 
	 * @param module
	 *            the module
	 * @param prefix
	 *            the prefix string
	 * @return <tt>true</tt> if there is an event scheduled for the module and
	 *         prefix, or <tt>false</tt> otherwise
	 */
	public boolean hasEvent(String module, String prefix) {
		return events.scheduled(module, prefix);
	}

	/**
	 * Changes the event time for the given module with a description that
	 * starts with the given prefix.
	 * 
	 * @param module
	 *            the module
	 * @param prefix
	 *            the prefix string
	 * @param t
	 *            the new time
	 */
	public void changeEventTime(String module, String prefix, double t) {
		events.changeTime(module, prefix, t);
	}

	/**
	 * Removes all events for the given module.
	 * 
	 * @param module
	 *            the module
	 */
	public void removeEvents(String module) {
		events.removeModuleEvents(module);
	}

	/**
	 * Removes all events for the given module with a description that starts
	 * with the given prefix.
	 * 
	 * @param module
	 *            the module
	 * @param prefix
	 *            the prefix string
	 */
	public void removeEvents(String module, String prefix) {
		events.removeModuleEvents(module, prefix);
	}

	/**
	 * Runs a model simulation until there are no more events on the event
	 * queue, or until <tt>stop()</tt> is explicitly called.
	 */
	public void run() {
		run(true);
	}

	/**
	 * Run a model simulation until there are no more events on the event queue,
	 * or until <tt>stop()</tt> is called.
	 * 
	 * @param reset
	 *            <tt>true</tt> to start the model from time zero, or
	 *            <tt>false</tt> to resume from the current state
	 */
	public void run(boolean reset) {
		stop = false;
		taskUpdated = false;

		if (reset) {
			task.start();
			addEvent(new Event(0.0, "procedural", "start") {
				@Override
				public void action() {
					procedural.findInstantiations(buffers);
					if (bold.brainImaging)
						bold.start();
				}
			});
		}

		while (!stop && (events.hasMoreEvents() || runUntilStop)) {
			Event event = events.next();
			if (realTime && (event.getTime() > time))
				incrementalSleep(Math.round(
						1000 * (event.getTime() - time) * (1.0 / (realTimeMultiplier == 0 ? 1 : realTimeMultiplier))));
			time = event.getTime();

			taskUpdated = false;
			if (verboseTrace && !event.module.equals("task") && !event.module.equals("bold")
					&& !event.module.isEmpty())
				output(event.module, event.description);
			event.action();

			if (!event.module.equals("procedural") && !event.module.equals("bold")
					&& (taskUpdated || !event.module.equals("task")) && !events.scheduled("procedural"))
				procedural.findInstantiations(buffers);
		}
		if (verboseTrace) {
			if (events.hasMoreEvents())
				output("------", "stop");
			else
				output("------", "done");
		}
	}

	void incrementalSleep(long ms) {
		int increment = 100;
		while (!stop && (ms > 0)) {
			try {
				Thread.sleep(Math.min(ms, increment));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ms -= increment;
		}
	}

	/**
	 * Terminates the simulation of the model (potentially before its
	 * completion).
	 */
	public void stop() {
		stop = true;
	}

	/**
	 * Resumes the simulation of the model (equivalent to <tt>run(false)</tt>).
	 */
	public void resume() {
		run(false);
	}

	/**
	 * Checks whether the model simulation has completed (i.e., there are no
	 * more events in the event queue).
	 * 
	 * @return <tt>true</tt> if the model has completed, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean isDone() {
		return !events.hasMoreEvents();
	}

	/**
	 * Checks whether the model verbose trace is on.
	 * 
	 * @return <tt>true</tt> if verbose trace is on, or <tt>false</tt> otherwise
	 */
	public boolean isVerbose() {
		return verboseTrace;
	}

	void setParameter(String parameter, String value, Tokenizer t) {
		switch (parameter) {
			case ":esc":
				if (nil(value))
					recordWarning("unsupported parameter value: " + parameter + " " + value, t);
				break;
			case ":v":
				verboseTrace = !nil(value);
				break;
			case ":real-time":
				realTime = !nil(value);
				realTimeMultiplier = (!nil(value)) ? Double.parseDouble(value) : 0;
				break;
			case ":rus":
				runUntilStop = !nil(value);
				break;
			case ":randomize-time":
				randomizeTime = !nil(value);
				if (randomizeTime) {
					try {
						randomizeTimeValue = Double.parseDouble(value);
					} catch (NumberFormatException e) {
						randomizeTimeValue = 3;
					}
				}
				break;
			case ":dat":
				procedural.actionTime = Double.parseDouble(value);
				break;
			case ":vpft":
				procedural.variableProductionFiringTime = !nil(value);
				break;
			case ":ul":
				procedural.utilityLearning = !nil(value);
				break;
			case ":egs":
				procedural.utilityNoiseS = Double.parseDouble(value);
				break;
			case ":alpha":
				procedural.utilityLearningAlpha = Double.parseDouble(value);
				break;
			case ":epl":
				procedural.productionLearning = !nil(value);
				break;
			case ":iu":
				procedural.initialUtility = Double.parseDouble(value);
				break;
			case ":tt":
				procedural.productionCompilationThresholdTime = Double.parseDouble(value);
				break;
			case ":nu":
				procedural.productionCompilationNewUtility = Double.parseDouble(value);
				break;
			case ":cst":
				procedural.conflictSetTrace = !nil(value);
				break;
			case ":pct":
				procedural.productionCompilationTrace = !nil(value);
				break;
			case ":rt":
				declarative.retrievalThreshold = Double.parseDouble(value);
				break;
			case ":lf":
				declarative.latencyFactor = Double.parseDouble(value);
				break;
			case ":bll":
				declarative.baseLevelLearning = (!nil(value));
				declarative.baseLevelDecayRate = (!nil(value)) ? Double.parseDouble(value) : 0;
				break;
			case ":ol":
				declarative.optimizedLearning = !nil(value);
				break;
			case ":optimized-fan":
				declarative.optimizedFan = !nil(value);
				break;
			case ":ans":
				declarative.activationNoiseS = Double.parseDouble(value);
				break;
			case ":ga":
				declarative.goalActivation = Double.parseDouble(value);
				break;
			case ":imaginal-activation":
				declarative.imaginalActivation = Double.parseDouble(value);
				break;
			case ":mas":
				declarative.spreadingActivation = (!nil(value));
				declarative.maximumAssociativeStrength = (!nil(value)) ? Double.parseDouble(value) : 0;
				break;
			case ":mp":
				declarative.partialMatching = (!nil(value));
				declarative.mismatchPenalty = (!nil(value)) ? Double.parseDouble(value) : 0;
				break;
			case ":declarative-num-finsts":
				declarative.declarativeNumFinsts = Integer.parseInt(value);
				break;
			case ":declarative-finst-span":
				declarative.declarativeFinstSpan = Double.parseDouble(value);
				break;
			case ":act":
				declarative.activationTrace = !nil(value);
				break;
			case ":visual-attention-latency":
				vision.visualAttentionLatency = Double.parseDouble(value);
				break;
			case ":visual-movement-tolerance":
				vision.visualMovementTolerance = Double.parseDouble(value);
				break;
			case ":visual-num-finsts":
				vision.visualNumFinsts = Integer.parseInt(value);
				break;
			case ":visual-finst-span":
				vision.visualFinstSpan = Double.parseDouble(value);
				break;
			case ":visual-onset-span":
				vision.visualOnsetSpan = Double.parseDouble(value);
				break;
			case ":tone-detect-delay":
				audio.toneDetectDelay = Double.parseDouble(value);
				break;
			case ":tone-recode-delay":
				audio.toneRecodeDelay = Double.parseDouble(value);
				break;
			case ":digit-detect-delay":
				audio.digitDetectDelay = Double.parseDouble(value);
				break;
			case ":digit-recode-delay":
				audio.digitRecodeDelay = Double.parseDouble(value);
				break;
			case ":motor-feature-prep-time":
				motor.featurePrepTime = Double.parseDouble(value);
				break;
			case ":motor-initiation-time":
				motor.movementInitiationTime = Double.parseDouble(value);
				break;
			case ":motor-burst-time":
				motor.burstTime = Double.parseDouble(value);
				break;
			case ":peck-fitts-coeff":
				motor.peckFittsCoeff = Double.parseDouble(value);
				break;
			case ":mouse-fitts-coeff":
				motor.mouseFittsCoeff = Double.parseDouble(value); // new
				break;
			case ":min-fitts-time":
				motor.minFittsTime = Double.parseDouble(value);
				break;
			case ":default-target-width":
				motor.defaultTargetWidth = Double.parseDouble(value);
				break;
			case ":syllable-rate":
				speech.syllableRate = Double.parseDouble(value);
				break;
			case ":char-per-syllable":
				speech.charsPerSyllable = Integer.parseInt(value);
				break;
			case ":subvocalize-detect-delay":
				speech.subvocalizeDetectDelay = Double.parseDouble(value);
				break;
			case ":imaginal-delay":
				imaginal.imaginalDelay = Double.parseDouble(value);
				break;

			// below are new parameters
			case ":add-chunk-on-new-request":
				declarative.addChunkOnNewRequest = !nil(value);
				break;
			case ":buffer-chunk-decay":
				buffers.bufferChunkDecay = (!nil(value));
				buffers.bufferChunkLife = (!nil(value)) ? Double.parseDouble(value) : 0;
				break;
			case ":tct":
				procedural.threadedCognitionTrace = (!nil(value));
				break;
			case ":pc-threaded":
				procedural.productionCompilationThreaded = (!nil(value));
				break;
			case ":pc-add-utilities":
				procedural.productionCompilationAddUtilities = (!nil(value));
				break;
			case ":emma":
				vision.useEMMA = (!nil(value));
				break;
			case ":emma-enc-fac":
				vision.emmaEncodingTimeFactor = Double.parseDouble(value);
				break;
			case ":emma-enc-exp":
				vision.emmaEncodingExponentFactor = Double.parseDouble(value);
				break;
			case ":brain-imaging":
				bold.brainImaging = (!nil(value));
				break;
			case ":case-sensitive":
				t.caseSensitive = (!nil(value));
				break;
			default:
				// Fatigue parameters
		else if (parameter.equals(":fatigue")) {
				fatigue.setFatigueEnabled(!value.equals("nil"));
				procedural.utilityUseThreshold = !value.equals("nil");
			}
			else if (parameter.equals(":fatigue-partial-matching"))
				fatigue.setFatiguePartialMatching(!value.equals("nil"));
			else if (parameter.equals(":fp-dec"))
				fatigue.setFatigueFPDec(Double.valueOf(value));
			else if (parameter.equals(":fp-dec-sleep1"))
				fatigue.setFatigueFPDecSleep1(Double.valueOf(value));
			else if (parameter.equals(":fp-dec-sleep2"))
				fatigue.setFatigueFPDecSleep2(Double.valueOf(value));
			else if (parameter.equals(":fp"))
				fatigue.setFatigueFP(Double.valueOf(value));
			else if (parameter.equals(":fd-dec"))
				fatigue.setFatigueFDDec(Double.valueOf(value));
			else if (parameter.equals(":fd"))
				fatigue.setFatigueFD(Double.valueOf(value));
			else if (parameter.equals(":fpbmc"))
				fatigue.setFatigueFPBMC(Double.valueOf(value));
			else if (parameter.equals(":fpmc0"))
				fatigue.setFatigueFPMC0(Double.valueOf(value));
			else if (parameter.equals(":fpmc"))
				fatigue.setFatigueFPMC(Double.valueOf(value));
			else if (parameter.equals(":utbmc"))
				fatigue.setFatigueUTBMC(Double.valueOf(value));
			else if (parameter.equals(":utmc"))
				fatigue.setFatigueUTMC(Double.valueOf(value));
			else if (parameter.equals(":utmc0"))
				fatigue.setFatigueUTMC0(Double.valueOf(value));
			else if (parameter.equals(":ut0"))
				fatigue.setFatigueUT0(Double.valueOf(value));
			else if (parameter.equals(":fdbmc"))
				fatigue.setFatigueFDBMC(Double.valueOf(value));
			else if (parameter.equals(":fdc"))
				fatigue.setFatigueFDC(Double.valueOf(value));
			else if (parameter.equals(":hour"))
				fatigue.setFatigueStartTime(Double.valueOf(value));
			else if (parameter.equals(":p0"))
				fatigue.fatigueP0 = Double.valueOf(value);
			else if (parameter.equals(":u0"))
				fatigue.fatigueU0 = Double.valueOf(value);
			else if (parameter.equals(":k0"))
				fatigue.fatigueK0 = Double.valueOf(value);
			else
				recordWarning("ignoring parameter " + parameter, t);
				break;
		}
	}

	private static boolean nil(String value) {
		return value.equals("nil");
	}

	/**
	 * Sets a global ACT-R parameter (equivalent to
	 * <tt>(sgp \<parameter\> \<value\>)</tt> in the model code).
	 * 
	 * @param parameter
	 *            the parameter to set, specified as a String and beginning with
	 *            ':'
	 * @value value the new value of the given parameter
	 */
	public void setParameter(String parameter, String value) {
		setParameter(parameter, value, null);
	}

	void noteTaskUpdated() {
		taskUpdated = true;
	}

	/**
	 * Compiles and runs an ACT-R command specified as a string (e.g.,
	 * <tt>(add-dm (goal isa task))</tt>).
	 * 
	 * @param cmd
	 *            the command specified as a string
	 */
	public void runCommand(String cmd) {
		new Parser(cmd).parse(this);
	}

	/**
	 * Prints declarative memory to the output panel.
	 */
	public void outputDeclarative() {
		output(declarative.toString());
	}

	/**
	 * Prints production rules to the output panel.
	 */
	public void outputProcedural() {
		output(procedural.toString());
	}

	/**
	 * Prints a "why not" trace to the output panel.
	 */
	public void outputWhyNot() {
		boolean old = procedural.whyNotTrace;
		procedural.whyNotTrace = true;
		procedural.findInstantiations(buffers);
		procedural.whyNotTrace = old;
	}

	/**
	 * Prints a listing of all buffer contents to the output panel.
	 */
	public void outputBuffers() {
		output(buffers.toString());
	}

	/**
	 * Prints a listing of all visual items to the output panel.
	 */
	public void outputVisualObjects() {
		output(vision.visualObjects());
	}

	/**
	 * Prints a string to the output panel for a particular module.
	 * 
	 * @param module
	 *            the module
	 * @param s
	 *            the message string
	 */
	public void output(String module, String s) {
		output(String.format("%9.3f   %-15s   %s", time, module, s));
	}

	/**
	 * Prints a string to the output panel.
	 * 
	 * @param s
	 *            the message string
	 */
	public void output(String s) {
		if (frame != null)
			frame.output(s);
		else
			System.out.println(s);
	}

	/**
	 * Prints a string to the output panel without newline at the end.
	 *
	 * @param s
	 *            the message string
	 */
	public void outputInLine(String s) {
		if (frame != null)
			frame.outputInLine(s);
		else
			System.out.println(s);
	}

	/**
	 * Clears the output panel.
	 */
	public void clearOutput() {
		if (frame != null)
			frame.clearOutput();
	}

	public void outputError(String s) {
		output("Error: " + s);
	}

	void outputWarning(String s) {
		output("Warning: " + s);
	}

	void recordWarning(String s, Tokenizer t) {
		String text = "Warning: " + s;
		int offset = t.getLastOffset();
		int line = t.getLastLine();
		errors.add(new ParseError(text, offset, line, false));
	}

	void recordError(String s, Tokenizer t) throws Exception {
		String text = "Error: " + s;
		int offset = t.getLastOffset();
		int line = t.getLastLine();
		errors.add(new ParseError(text, offset, line, true));
		throw new Exception();
	}

	void recordError(Tokenizer t) throws Exception {
		recordError("syntax error", t);
	}

	void updateVisuals() {
		frame.updateVisuals();
	}

	/**
	 * Gets a string representation of the entire model.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "Model:\n\n";
		s += "DM:\n" + declarative;
		s += "\nPS:\n" + procedural;
		s += "\nBuffers:\n" + buffers;
		return s;
	}
}
