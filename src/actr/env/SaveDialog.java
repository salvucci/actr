package actr.env;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import resources.Resources;

class SaveDialog extends JDialog {
	SaveDialog dialog;
	boolean cancel = false;
	boolean save = true;

	SaveDialog(Frame frame, String filename) {
		super(frame, true);
		setUndecorated(true);

		dialog = this;

		Icon icon = Resources.getIcon("actr.png");

		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
		left.add(new JLabel(icon));
		left.add(Box.createVerticalGlue());

		String text = "<html><font size=3><b>Do you want to save the changes you made in<br>the document \""
				+ filename
				+ "\"?</b></font><font size=2><br><br>Your changes will be lost if you don't save them.</font></html>";

		JButton dontSaveButton = new JButton("Don't Save");
		dontSaveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save = false;
				dialog.setVisible(false);
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel = true;
				dialog.setVisible(false);
			}
		});

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save = true;
				dialog.setVisible(false);
			}
		});

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(dontSaveButton);
		bottom.add(Box.createHorizontalGlue());
		bottom.add(cancelButton);
		bottom.add(saveButton);

		JPanel center = new JPanel();
		center.setLayout(new BorderLayout(24, 24));
		center.add(new JLabel(text), BorderLayout.CENTER);
		center.add(bottom, BorderLayout.SOUTH);

		JPanel main = new JPanel();
		main.setLayout(new BorderLayout(24, 24));
		main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		main.add(left, BorderLayout.WEST);
		main.add(center, BorderLayout.CENTER);

		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save = false;
				dialog.setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.META_MASK),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancel = true;
				dialog.setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		getRootPane().setDefaultButton(saveButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(main, BorderLayout.CENTER);

		pack();

		Point pt = frame.getLocationOnScreen();
		setLocation((int) (pt.getX() + frame.getWidth() / 2 - getWidth() / 2),
				(int) pt.getY() + 23);

		setVisible(true);
	}
}
