package actr.task;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;

import actr.model.Model;

/**
 * The core class for a task to be performed by an ACT-R model.
 * <p>
 * This class defines the default task with no interface components; an instance
 * of this task is used when no task is defined in an ACT-R model. To create a
 * custom task, this class should be extended and the subclass should override
 * relevant methods to define the functionality of the new task. For example:
 * <ul>
 * <li>To construct a custom interface, the new task subclass should override
 * the <tt>Task()</tt> constructor to create new components and add them to the
 * interface (after calling <tt>super()</tt> to ensure proper initialization of
 * other settings).</li>
 * <li>To initialize a custom task when the model begins its simulation, the
 * task subclass should override the <tt>start()</tt> method and perform
 * initialization in this method.</li>
 * <li>To update the custom task during simulation, the task subclass should
 * override the <tt>update()</tt> method, which receives the current time and
 * can thus update the task accordingly. All calls to the <tt>update()</tt> must
 * be scheduled in the event queue: one-time calls can be scheduled using the
 * <tt>addUpdate()</tt> method, and periodic calls can be scheduled using the
 * <tt>addPeriodicUpdate()</tt> method.</li>
 * <li>To perform final cleanup and/or analysis, the task subclass should
 * override the <tt>finish()</tt> method and perform all finalization in this
 * method.</li>
 * </ul>
 * 
 * @author Dario Salvucci
 */
public class Task extends JPanel {
	private String name;
	protected Model model;
	private boolean showMouse;
	private int mouseX, mouseY;
	private boolean showAttention, showEye;
	private int attentionX, attentionY;
	private int eyeX, eyeY;

	/**
	 * Constructs a new task.
	 */
	public Task() {
		super();

		setLayout(null);
		setBackground(Color.white);

		this.name = "Task";

		showMouse = false;
		mouseX = mouseY = 0;
		showAttention = false;
		showEye = false;
		attentionX = attentionY = 0;
	}

	/**
	 * Creates an instance of the given task named by its name string. The task
	 * name should be the combined package name and class name for the class
	 * that defines the task -- for example,
	 * <tt>"actr.tasks.mypackage.MyTaskClass"</tt>.
	 * 
	 * @param taskName
	 *            the task name string
	 * @return an instance of the task
	 */
	public static Task createTaskInstance(String taskName) {
		try {
			Task task = (Task) (Class.forName(taskName).getConstructor().newInstance());
			task.name = (taskName.contains(".")) ? taskName.substring(taskName.lastIndexOf('.') + 1) : taskName;
			return task;
		} catch (Exception e) {
			return null;
		}
	}

	public static String[] allTaskClasses() {
		Vector<String> strings = new Vector<>();
		try {
			File file = new File(Task.class.getResource("Task.class").toURI());
			String tasks = file.getParentFile().getParent() + File.separator + "tasks";
			allTasksHelper(new File(tasks), "actr.", strings);
		} catch (Exception e) {
		}
		String[] array = new String[strings.size()];
		strings.toArray(array);
		return array;
	}

	static void allTasksHelper(File file, String prefix, Vector<String> strings) {
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files == null)
				return;
			for (String s : files)
				allTasksHelper(new File(file.getPath() + File.separator + s), prefix + file.getName() + ".",
					strings);
		} else {
			String name = file.getName();
			if (!name.endsWith(".class") || name.contains("$"))
				return;
			name = prefix + name.substring(0, name.indexOf(".class"));
			Task task = createTaskInstance(name);
			if (task != null)
				strings.add(name);
		}
	}

	/**
	 * Gets the model performing the task.
	 * 
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Gets the number of model iterations to perform during an analysis. This
	 * method can be overridden to specify the desired number of iterations. The
	 * default is 1.
	 * 
	 * @return the number of iterations
	 */
	public int analysisIterations() {
		return 1;
	}

	/**
	 * Analyzes a batch of tasks and, if desired, outputs the analysis. This
	 * method can be overridden to accept an array of tasks (usually with a
	 * length equal to the number returned by <tt>analysisIterations()</tt>) and
	 * perform the desired analysis over the entire set of tasks. If the
	 * <tt>output</tt> parameter is <tt>true</tt>, the method can use
	 * <tt>getModel().output()</tt> to output the details of the analysis to the
	 * screen.
	 * 
	 * @param tasks
	 *            the set of tasks to analyze
	 * @param output
	 *            a flag indicating whether to output the details of analysis to
	 *            the screen
	 * @return the analysis result
	 */
	public Result analyze(Task[] tasks, boolean output) {
		return new Result();
	}

	/**
	 * Gets the name of the task.
	 * 
	 * @return the task name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the model for the task.
	 * 
	 * @param model
	 *            the model
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	/**
	 * Starts the task. This method can be overridden to allow a custom task to
	 * initialize the interfaces and any necessary data structures.
	 */
	public void start() {
	}

	/**
	 * Updates the task. This method can be overridden to allow a custom task to
	 * update the interface and/or other relevant data structures. The current
	 * model time is passed into the method as an argument.
	 * 
	 * @param time
	 *            the current time in seconds from the start of simulation
	 */
	public void update(double time) {
	}

	/**
	 * Finalizes the task. This method can be overridden to allow a custom task
	 * to finalize any necessary data structures and/or perform relevant
	 * analysis on task performance.
	 */
	public void finish() {
	}

	/**
	 * Computes a task-specific evaluation. When the model calls for an
	 * evaluation using the <tt>!eval!</tt> operator, this method is called with
	 * an iterator containing the string tokens in the evaluation (including
	 * parentheses as separate tokens).
	 * 
	 * @param it
	 *            the token iterator
	 */
	public void eval(Iterator<String> it) {
	}

	/**
	 * Computes a task-specific evaluation condition. When the model calls for
	 * an evaluation using the <tt>!eval!</tt> operator, this method is called
	 * with an iterator containing the string tokens in the evaluation
	 * (including parentheses as separate tokens).
	 * 
	 * @param it
	 *            the token iterator
	 * @return the boolean value of the computed condition
	 */
	public static boolean evalCondition(Iterator<String> it) {
		return false;
	}

	/**
	 * Computes a task-specific binding. When the model calls for an eval
	 * binding using the <tt>!eval!</tt> operator, this method is called with an
	 * iterator containing the string tokens in the evaluation (including
	 * parentheses as separate tokens). The returned value is then bound to the
	 * associated variable in the production rule.
	 * 
	 * @param it
	 *            the token iterator
	 * @return the double value of the eval binding
	 */
	public static double bind(Iterator<String> it) {
		return 0;
	}

	/**
	 * Adds the given component to the task interface.
	 * 
	 * @param comp
	 *            the component
	 * @return the component
	 */
	@Override
	public Component add(Component comp) {
		return super.add(comp);
	}

	/**
	 * Processes the task display by clearing the model's current visual objects
	 * and then re-adding all existing task components back into the model.
	 */
	public void processDisplay() {
		model.vision.clearVisual();
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++) {
			Component c = components[i];
			if ((c instanceof TaskComponent) && c.isVisible()) {
				TaskComponent tc = (TaskComponent) components[i];
				String id = tc.getKind() + i;
				String type = tc.getKind();
				String value = tc.getValue();
				model.vision.addVisual(id, type, value, centerX(c), centerY(c), c.getWidth(), c.getHeight());
			}
		}
	}

	/**
	 * Processes the task display by moving all existing task components to
	 * their (possibly new) locations.
	 */
	public void processDisplayNoClear() {
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++) {
			Component c = components[i];
			if (c instanceof TaskComponent && c.isVisible()) {
				TaskComponent tc = (TaskComponent) components[i];
				String id = tc.getKind() + i;
				model.vision.moveVisual(id, centerX(c), centerY(c));
			}
		}
	}

	/**
	 * Get the center x coordinate of the component.
	 * 
	 * @param c
	 *            the component
	 * @return the center x coordinate
	 */
	public static int centerX(Component c) {
		return c.getX() + c.getWidth() / 2;
	}

	/**
	 * Get the center y coordinate of the component.
	 * 
	 * @param c
	 *            the component
	 * @return the center y coordinate
	 */
	public static int centerY(Component c) {
		return c.getY() + c.getHeight() / 2;
	}

	/**
	 * Gets the current x coordinate of the mouse location.
	 * 
	 * @return the current mouse x coordinate
	 */
	public double getMouseX() {
		return mouseX;
	}

	/**
	 * Gets the current y coordinate of the mouse location.
	 * 
	 * @return the current mouse y coordinate
	 */
	public double getMouseY() {
		return mouseY;
	}

	/**
	 * Moves the task mouse to the given coordinates.
	 * 
	 * @param x
	 *            the new x coordinate
	 * @param y
	 *            the new y coordinate
	 */
	public void moveMouse(int x, int y) {
		showMouse = true;
		mouseX = x;
		mouseY = y;
		repaint();
	}

	/**
	 * Clicks the task mouse at its current location, calling the
	 * <tt>doClick()</tt> method of any buttons at that location.
	 */
	public void clickMouse() {
		Point mousePoint = new Point(mouseX, mouseY);
		Component[] components = getComponents();
		for (Component component : components) {
			if ((component instanceof TaskButton) && component.isVisible()) {
				TaskButton taskButton = (TaskButton) component;
				Rectangle bounds = component.getBounds();
				if (bounds.contains(mousePoint)) {
					taskButton.doClick();
					repaint();
					return;
				}
			}
		}
	}

	/**
	 * Registers a task keystroke. This method can be overridden to allow a
	 * custom task to act upon model keystrokes.
	 * 
	 * @param c
	 *            the typed character
	 */
	public void typeKey(char c) {
	}

	/**
	 * Speaks a given string. This method can be overridden to allow a custom
	 * task to act upon model speech.
	 * 
	 * @param s
	 *            the spoken string
	 */
	public void speak(String s) {
	}

	/**
	 * Moves the attentional spotlight to the given coordinates.
	 * 
	 * @param x
	 *            the new x coordinate
	 * @param y
	 *            the new y coordinate
	 */
	public void moveAttention(int x, int y) {
		showAttention = true;
		attentionX = x;
		attentionY = y;
		repaint();
	}

	/**
	 * Moves the eyes to the given coordinates.
	 * 
	 * @param x
	 *            the new x coordinate
	 * @param y
	 *            the new y coordinate
	 */
	public void moveEye(int x, int y) {
		showEye = true;
		eyeX = x;
		eyeY = y;
		repaint();
	}

	/**
	 * Adds an aural sound for access by the aural module at the current time.
	 * The identifier should be unique for different sounds; if the same
	 * identifier is used, the new sound will cancel the previously scheduled
	 * one.
	 * 
	 * @param id
	 *            the unique name of the aural object
	 * @param type
	 *            the type of the aural object (i.e., the "kind" slot value)
	 * @param content
	 *            the content of the aural object (i.e., the "content" slot
	 *            value)
	 */
	public void addAural(String id, String type, String content) {
		model.audio.addAural(id, type, content);
	}

	/**
	 * Adds an aural sound for access by the aural module at the current time
	 * plus the given time delta. The identifier should be unique for different
	 * sounds; if the same identifier is used, the new sound will cancel the
	 * previously scheduled one.
	 * 
	 * @param timeDelta
	 *            the future time ahead of the current time at which to schedule
	 *            the event
	 * @param id
	 *            the unique name of the aural object
	 * @param type
	 *            the type of the aural object (i.e., the "kind" slot value)
	 * @param content
	 *            the content of the aural object (i.e., the "content" slot
	 *            value)
	 */
	public void addAural(double timeDelta, final String id, final String type, final String content) {
		model.addEvent(new actr.model.Event(model.getTime() + timeDelta, "task", "update") {
			@Override
			public void action() {
				addAural(id, type, content);
			}
		});
	}

	/**
	 * Adds the given event to the model's event queue.
	 * 
	 * @param event
	 *            the event
	 */
	public void addEvent(actr.model.Event event) {
		model.addEvent(event);
	}

	/**
	 * Adds a call to the <tt>update()</tt> method at the current time plus the
	 * given time delta.
	 * 
	 * @param timeDelta
	 *            the future time ahead of the current time at which to schedule
	 *            the event
	 */
	public void addUpdate(final double timeDelta) {
		final double now = model.getTime();
		model.addEvent(new actr.model.Event(now + timeDelta, "task", "update") {
			@Override
			public void action() {
				update(time);
			}
		});
	}

	/**
	 * Adds a periodic call to the <tt>update()</tt> method every timeDelta
	 * seconds.
	 * 
	 * @param timeDelta
	 *            the period of the calls to the <tt>update()</tt> method
	 */
	public void addPeriodicUpdate(final double timeDelta) {
		final double now = model.getTime();
		update(now);
		model.addEvent(new actr.model.Event(now + timeDelta, "task", "update") {
			@Override
			public void action() {
				addPeriodicUpdate(timeDelta);
			}
		});
	}

	/**
	 * Paints the task display by painting each of the associated components.
	 * 
	 * @param g
	 *            the graphics display
	 */
	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		super.paintComponent(g);
	}

	/**
	 * Paints the extras on the task display, such as the mouse, attentional
	 * spotlight, and eye location. This method should not normally be
	 * overridden.
	 * 
	 * @param g
	 *            the graphics display
	 */
	@Override
	public void paintChildren(Graphics g) {
		super.paintChildren(g);
		Graphics2D g2d = (Graphics2D) g;

		Composite oldComp = g2d.getComposite();
		Composite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f);

		if (showAttention) {
			g2d.setComposite(alphaComp);
			g2d.setPaint(Color.yellow);
			Shape circle = new Ellipse2D.Double(attentionX - 20, attentionY - 20, 40, 40);
			g2d.fill(circle);
			g2d.setComposite(oldComp);
		}

		if (showEye) {
			g2d.setComposite(alphaComp);
			g2d.setPaint(Color.blue);
			Shape circle = new Ellipse2D.Double(eyeX - 10, eyeY - 10, 20, 20);
			g2d.fill(circle);
			g2d.setComposite(oldComp);
		}

		if (showMouse) {
			g2d.setPaint(Color.black);
			Polygon cursor = new Polygon();
			cursor.addPoint(0, 0);
			cursor.addPoint(0, 13);
			cursor.addPoint(3, 10);
			cursor.addPoint(5, 16);
			cursor.addPoint(7, 15);
			cursor.addPoint(5, 10);
			cursor.addPoint(10, 10);
			cursor.translate(mouseX, mouseY);
			g2d.fill(cursor);
		}
	}

	/**
	 * Gets a string representation the task, defaulting to its name string.
	 * 
	 * @return the task string
	 */
	@Override
	public String toString() {
		return name;
	}
}
