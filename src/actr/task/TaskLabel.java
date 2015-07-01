package actr.task;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * The class that defines a label for a task interface.
 * 
 * @author Dario Salvucci
 */
public class TaskLabel extends JLabel implements TaskComponent {
	/**
	 * Creates a new label.
	 * 
	 * @param text
	 *            the label text
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 */
	public TaskLabel(String text, int x, int y, int width, int height) {
		super(text);
		setBounds(x, y, width, height);
		setHorizontalAlignment(SwingConstants.CENTER);
		setVerticalAlignment(SwingConstants.CENTER);
	}

	/**
	 * Gets the kind of component (i.e., the "kind" slot of the ACT-R visual
	 * object).
	 * 
	 * @return the kind string
	 */
	@Override
	public String getKind() {
		return "text";
	}

	/**
	 * Gets the value of component (i.e., the "value" slot of the ACT-R visual
	 * object).
	 * 
	 * @return the value string
	 */
	@Override
	public String getValue() {
		return '"' + getText() + '"';
	}
}
