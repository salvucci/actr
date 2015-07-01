package actr.env;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

import resources.Resources;

class Toolbar extends JToolBar {
	private String speedup, iterations;

	interface Setter {
		void set(String s);
	}

	Toolbar(Frame frame, Actions actions) {
		super();

		setBorder(BorderFactory.createLineBorder(Color.gray, 1));
		setFloatable(false);
		setFocusable(false);
		setRollover(true);

		if (Main.inApplication()) {
			addButton(actions.newAction);
			addButton(actions.openAction);
			addButton(actions.saveAction);
			addButton(actions.printAction);

			addSeparator();
		}

		addButton(actions.undoAction);
		addButton(actions.redoAction);

		addSeparator();

		addButton(actions.cutAction);
		addButton(actions.copyAction);
		addButton(actions.pasteAction);

		addSeparator();

		addButton(actions.findAction);
		addButton(actions.findNextAction);
		addButton(actions.findPreviousAction);

		addSeparator();

		JButton runButton = addButton(actions.runAction, 20);
		final String[][] speedOptions = { { "5x Slower", "0.2" },
				{ "3x Slower", "0.33" }, { "Real-Time", "1" },
				{ "3x Faster", "3" }, { "5x Faster", "5" },
				{ "10x Faster", "10" } };
		addDropButton("Run Speed", runButton, speedOptions, 2, new Setter() {
			@Override
			public void set(String s) {
				speedup = s;
			}
		});

		add(Box.createHorizontalStrut(6));

		JButton runAnalysisButton = addButton(actions.runAnalysisAction, 20);
		final String[][] iterationOptions = { { "Task Default", "" },
				{ "1", "1" }, { "3", "3" }, { "5", "5" }, { "10", "10" },
				{ "20", "20" }, { "50", "50" }, { "100", "100" } };
		addDropButton("Number of Iterations", runAnalysisButton,
				iterationOptions, 0, new Setter() {
					@Override
					public void set(String s) {
						iterations = s;
					}
				});

		add(Box.createHorizontalStrut(6));

		// addSeparator();

		addButton(actions.stopAction);
		addButton(actions.resumeAction);

		addSeparator();

		add(Box.createHorizontalGlue());
	}

	JButton addButton(Action action, int width) {
		final JButton button = new JButton(action);
		if (button.getIcon() != null)
			button.setText(null);
		button.setToolTipText((String) action.getValue(Action.NAME));
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (button.isEnabled())
					button.setBorder(BorderFactory
							.createBevelBorder(BevelBorder.RAISED));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBorder(null);
			}
		});
		button.setFocusable(false);
		button.setBorder(null);
		button.setPreferredSize(new Dimension(width, 26));
		add(button);
		return button;
	}

	JButton addButton(Action action) {
		return addButton(action, 26);
	}

	JButton addDropButton(String title, final JButton primary,
			String[][] options, int defaultOption, final Setter setter) {
		final JButton button = new JButton(Resources.getIcon("DropArrow16.gif"));
		final JPopupMenu menu = new JPopupMenu();
		JMenuItem titleItem = new JMenuItem(title);
		titleItem.setEnabled(false);
		menu.add(titleItem);
		menu.addSeparator();
		ButtonGroup group = new ButtonGroup();
		for (int i = 0; i < options.length; i++) {
			final String pair[] = options[i];
			JMenuItem item = new JRadioButtonMenuItem(pair[0]);
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setter.set(pair[1]);
				}
			});
			group.add(item);
			menu.add(item);
			if (i == defaultOption) {
				item.setSelected(true);
				setter.set(pair[1]);
			}
		}
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				menu.show(button, 0, button.getHeight());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mousePressed(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBorder(BorderFactory
						.createBevelBorder(BevelBorder.RAISED));
				primary.setBorder(BorderFactory
						.createBevelBorder(BevelBorder.RAISED));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBorder(null);
				primary.setBorder(null);
			}
		});
		button.setFocusable(false);
		button.setBorder(null);
		button.setPreferredSize(new Dimension(10, 25));
		add(button);
		return button;
	}

	String getSpeedup() {
		return speedup;
	}

	String getIterations() {
		return iterations;
	}
}
