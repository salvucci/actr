package actr.env;

/**
 * Defines the interface for custom startup of the ACT-R system. The class that
 * implements this interface must be named <tt>Starter</tt> and must be placed
 * in the <tt>actr.tasks</tt> package.
 * 
 * @author Dario Salvucci
 */
public interface Starter {
	/**
	 * This method should be overridden to execute a custom startup of the
	 * system.
	 * 
	 * @param core
	 *            the system core
	 */
	public void startup(Core core);
}
