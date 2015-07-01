package actr.model;

/**
 * The abstract class for a model event.
 * 
 * @author Dario Salvucci
 */
public abstract class Event implements Comparable<Event> {
	public double time;
	public String module;
	public String description;

	private long uniqueID;

	/**
	 * Creates a new event.
	 * 
	 * @param time
	 *            the time at which the event should occur
	 * @param module
	 *            the module related to this event
	 * @param description
	 *            a description of the event for output traces
	 */
	public Event(double time, String module, String description) {
		this.time = time;
		this.module = module;
		this.description = description;
		uniqueID = Utilities.getUniqueID();
	}

	/**
	 * Gets the time of the event.
	 * 
	 * @return the time in seconds after the start of simulation
	 */
	public double getTime() {
		return time;
	}

	/**
	 * Gets the module associated with this event.
	 * 
	 * @return the module string
	 */
	public String getModule() {
		return module;
	}

	/**
	 * Gets the description of the event.
	 * 
	 * @return the description string
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Compares two events, primarily with respect to their start times.
	 * 
	 * @return -1 if this event comes before the given event, +1 if it comes
	 *         after the given event, and 0 if the events are equal (which
	 *         normally will not occur due to the unique identifiers)
	 */
	@Override
	public int compareTo(Event e2) {
		if (time < e2.time)
			return -1;
		else if (time > e2.time)
			return +1;
		else if (module.equals("task") && !e2.module.equals("task"))
			return -1;
		else if (!module.equals("task") && e2.module.equals("task"))
			return +1;
		else if (uniqueID < e2.uniqueID)
			return -1;
		else if (uniqueID > e2.uniqueID)
			return +1;
		else
			return 0;
		// 0 means events will be collapsed together!
	}

	/**
	 * The abstract action method. This method should be implemented with the
	 * code to perform the desired action at the time of the event.
	 */
	public abstract void action();

	/**
	 * Gets a string representation of the event including its time, module, and
	 * description.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return "[event: " + time + " " + module + "," + description + "]";
	}
}
