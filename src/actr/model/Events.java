package actr.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A queue of upcoming events sorted by time.
 * 
 * @author Dario Salvucci
 */
public class Events {
	private TreeSet<Event> events;

	Events() {
		events = new TreeSet<Event>();
	}

	/**
	 * Checks whether there are any events in the queue.
	 * 
	 * @return <tt>true</tt> if there are events in the queue, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean hasMoreEvents() {
		return !events.isEmpty();
	}

	Event next() {
		Event e = events.first();
		events.remove(e);
		return e;
	}

	/**
	 * Gets the time of the next event.
	 * 
	 * @return the time in seconds after the start of simulation
	 */
	public double nextTime() {
		Event e = events.first();
		return e.time;
	}

	void add(Event event) {
		events.add(event);
	}

	/**
	 * Checks whether there are any events scheduled for the given module.
	 * 
	 * @param module
	 *            the module string
	 * @return <tt>true</tt> if there are events scheduled for the module, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean scheduled(String module) {
		return scheduled(module, "");
	}

	/**
	 * Checks whether there are any events scheduled for the given module whose
	 * description begins with the given prefix string.
	 * 
	 * @param module
	 *            the module string
	 * @param prefix
	 *            the prefix string
	 * @return <tt>true</tt> if there are events scheduled for the module with
	 *         the given prefix, or <tt>false</tt> otherwise
	 */
	public boolean scheduled(String module, String prefix) {
		Iterator<Event> it = events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			if (e.module.equals(module) && e.description.startsWith(prefix))
				return true;
		}
		return false;
	}

	void changeTime(String module, String prefix, double newTime) {
		Iterator<Event> it = events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			if (e.module.equals(module) && e.description.startsWith(prefix)) {
				events.remove(e);
				e.time = newTime;
				// remove and add to re-sort the events
				events.add(e);
				return;
			}
		}
	}

	void removeModuleEvents(String module, String prefix) {
		Set<Event> moduleEvents = new HashSet<Event>();
		Iterator<Event> it = events.iterator();
		while (it.hasNext()) {
			Event e = it.next();
			if (e.module.equals(module) && e.description.startsWith(prefix))
				moduleEvents.add(e);
		}
		events.removeAll(moduleEvents);
	}

	void removeModuleEvents(String module) {
		removeModuleEvents(module, "");
	}

	/**
	 * Gets a string representation of the scheduled events.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		String s = "Events:\n";
		Iterator<Event> it = events.iterator();
		while (it.hasNext())
			s += it.next() + "\n";
		return s + "\n";
	}
}
