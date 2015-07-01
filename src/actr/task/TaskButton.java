package actr.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * The class that defines a button for a task interface. A subclass of this
 * class can override the <tt>doClick()</tt> method to specify the response to a
 * mouse click.
 * 
 * @author Dario Salvucci
 */
public class TaskButton extends JButton implements TaskComponent {
	/**
	 * Creates a new task button.
	 * 
	 * @param text
	 *            the text on the button
	 * @param x
	 *            the x coordinate
	 * @param y
	 *            the y coordinate
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 */
	public TaskButton(String text, int x, int y, int width, int height) {
		super(text);
		setBounds(x, y, width, height);
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doClick();
			}
		});
	}

	/**
	 * Gets the kind of component (i.e., the "kind" slot of the ACT-R visual
	 * object).
	 * 
	 * @return the kind string
	 */
	@Override
	public String getKind() {
		return "oval";
	}

	/**
	 * Gets the value of component (i.e., the "value" slot of the ACT-R visual
	 * object).
	 * 
	 * @return the value string
	 */
	@Override
	public String getValue() {
		return getText();
	}

	/**
	 * The method called when the button is clicked. This should be overridden
	 * to provide task-specific functionality.
	 */
	@Override
	public void doClick() {
	}
}
