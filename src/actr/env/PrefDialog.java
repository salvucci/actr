package actr.env;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

class PrefDialog extends JDialog {
	private final Core core;
	private final Preferences prefs;
	private final ColorDialog colorDialog;
	private final JComboBox<String> fontCB;
	private final JComboBox<String> fontSizeCB;
	private final JCheckBox autoHiliteCB;
	private final JCheckBox autoIndentCB;
	private final ColorButton commandColorButton;
	private final ColorButton parameterColorButton;
	private final ColorButton productionColorButton;
	private final ColorButton chunkColorButton;
	private final ColorButton bufferColorButton;
	private final ColorButton commentColorButton;
	private final JComboBox<String> indentSpacesCB;

	static class ColorDialog extends JDialog {
		Color color;
		final JColorChooser colorChooser;

		ColorDialog(JDialog parent) {
			super(parent, true);
			colorChooser = new JColorChooser();
			JButton okButton = new JButton("OK");
			okButton.addActionListener(e -> {
				color = colorChooser.getColor();
				setVisible(false);
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(e -> setVisible(false));
			JPanel bottom = new JPanel();
			bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
			bottom.add(Box.createHorizontalGlue());
			bottom.add(cancelButton);
			bottom.add(Box.createHorizontalStrut(12));
			bottom.add(okButton);
			getContentPane().setLayout(new BorderLayout(12, 12));
			getContentPane().add(colorChooser, BorderLayout.CENTER);
			getContentPane().add(bottom, BorderLayout.SOUTH);
			getRootPane().setDefaultButton(okButton);
			getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
			pack();
			setLocation(200, 200);
			setVisible(false);
		}

		void show(Color color) {
			this.color = color;
			colorChooser.setColor(color);
			setVisible(true);
		}
	}

	class ColorButton extends JButton {
		final JPanel panel;

		ColorButton() {
			super();
			panel = new JPanel();
			panel.setBackground(getColor());
			add(panel);
			addActionListener(e -> {
				colorDialog.show(getColor());
				setColor(colorDialog.color);
				refresh();
				core.refreshEditors();
			});
		}

		Color getColor() {
			return null;
		}

		void setColor(Color color) {
		}

		void refresh() {
			panel.setBackground(getColor());
			repaint();
		}
	}

	PrefDialog(final Core core) {
		super(new JFrame(), "Preferences");

		this.core = core;
		prefs = core.getPreferences();

		colorDialog = new ColorDialog(this);

		String[] fonts = { "Courier", "Lucida Grande", "Menlo", "Monaco", "Myriad Pro", "Verdana" };
		fontCB = new JComboBox<>(fonts);
		fontCB.addItemListener(e -> {
			prefs.font = (String) fontCB.getSelectedItem();
			core.refreshEditors();
		});

		String[] fontSizes = { "9", "10", "11", "12", "13", "14" };
		fontSizeCB = new JComboBox<>(fontSizes);
		fontSizeCB.addItemListener(e -> {
			prefs.fontSize = Integer.parseInt((String) Objects.requireNonNull(fontSizeCB.getSelectedItem()));
			core.refreshEditors();
		});

		autoHiliteCB = new JCheckBox("Auto-Highlight");
		autoHiliteCB.addItemListener(e -> {
			prefs.autoHilite = autoHiliteCB.isSelected();
			core.refreshEditors();
		});

		commandColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.commandColor;
			}

			@Override
			void setColor(Color color) {
				prefs.commandColor = color;
			}
		};
		parameterColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.parameterColor;
			}

			@Override
			void setColor(Color color) {
				prefs.parameterColor = color;
			}
		};
		productionColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.productionColor;
			}

			@Override
			void setColor(Color color) {
				prefs.productionColor = color;
			}
		};
		chunkColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.chunkColor;
			}

			@Override
			void setColor(Color color) {
				prefs.chunkColor = color;
			}
		};
		bufferColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.bufferColor;
			}

			@Override
			void setColor(Color color) {
				prefs.bufferColor = color;
			}
		};
		commentColorButton = new ColorButton() {
			@Override
			Color getColor() {
				return prefs.commentColor;
			}

			@Override
			void setColor(Color color) {
				prefs.commentColor = color;
			}
		};

		autoIndentCB = new JCheckBox("Auto-Indent");
		autoIndentCB.addItemListener(e -> {
			prefs.autoIndent = autoIndentCB.isSelected();
			core.refreshEditors();
		});

		String[] indentOptions = { "0", "1", "2", "3", "4", "5", "6", "7", "8" };
		indentSpacesCB = new JComboBox<>(indentOptions);
		indentSpacesCB.addItemListener(e -> {
			prefs.indentSpaces = Integer.parseInt((String) Objects.requireNonNull(indentSpacesCB.getSelectedItem()));
			core.refreshEditors();
		});

		refresh();

		JPanel fontPanel = new JPanel();
		fontPanel.setLayout(new BoxLayout(fontPanel, BoxLayout.X_AXIS));
		fontPanel.setBorder(createBorder("Font"));
		fontPanel.add(createLabel("Font:"));
		fontPanel.add(fontCB);
		fontPanel.add(Box.createHorizontalStrut(6));
		fontPanel.add(createLabel("Size:"));
		fontPanel.add(fontSizeCB);

		JPanel hilitePanel = new JPanel();
		hilitePanel.setLayout(new GridBagLayout());
		hilitePanel.setBorder(createBorder("Highlighting"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 0;
		c.ipady = 6;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		hilitePanel.add(autoHiliteCB, c);
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.ipadx = 12;
		c.gridy = 1;
		c.gridx++;
		hilitePanel.add(createLabel("Commands:"), c);
		c.gridy++;
		hilitePanel.add(createLabel("Parameters:"), c);
		c.gridy++;
		hilitePanel.add(createLabel("Productions:"), c);
		c.gridy = 1;
		c.gridx++;
		hilitePanel.add(commandColorButton, c);
		c.gridy++;
		hilitePanel.add(parameterColorButton, c);
		c.gridy++;
		hilitePanel.add(productionColorButton, c);
		c.gridy = 1;
		c.gridx++;
		hilitePanel.add(createLabel("Chunks:"), c);
		c.gridy++;
		hilitePanel.add(createLabel("Buffers:"), c);
		c.gridy++;
		hilitePanel.add(createLabel("Comments:"), c);
		c.gridy = 1;
		c.gridx++;
		hilitePanel.add(chunkColorButton, c);
		c.gridy++;
		hilitePanel.add(bufferColorButton, c);
		c.gridy++;
		hilitePanel.add(commentColorButton, c);

		JPanel indentPanel = new JPanel();
		indentPanel.setLayout(new BoxLayout(indentPanel, BoxLayout.X_AXIS));
		indentPanel.add(Box.createHorizontalGlue());
		indentPanel.setBorder(createBorder("Indentation"));
		indentPanel.add(autoIndentCB);
		indentPanel.add(Box.createHorizontalStrut(6));
		indentPanel.add(indentSpacesCB);
		indentPanel.add(new JLabel("Spaces"));
		indentPanel.add(Box.createHorizontalGlue());

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		center.add(fontPanel);
		center.add(Box.createVerticalStrut(12));
		center.add(hilitePanel);
		center.add(Box.createVerticalStrut(12));
		center.add(indentPanel);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> setVisible(false));

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(e -> {
			prefs.setDefaults();
			refresh();
		});

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(resetButton);
		bottom.add(Box.createHorizontalGlue());
		bottom.add(closeButton);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout(12, 12));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		mainPanel.add(center, BorderLayout.CENTER);
		mainPanel.add(bottom, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPanel, BorderLayout.CENTER);

		getRootPane().registerKeyboardAction(e -> setVisible(false), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		getRootPane().setDefaultButton(closeButton);

		pack();
		setLocation(100, 100);
		setVisible(false);
	}

	static JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}

	static Border createBorder(String title) {
		Border lineB = BorderFactory.createLineBorder(Color.gray, 1);
		Border titleB = BorderFactory.createTitledBorder(lineB, title);
		Border emptyB = BorderFactory.createEmptyBorder(0, 6, 6, 6);
		return BorderFactory.createCompoundBorder(titleB, emptyB);
	}

	void refresh() {
		fontCB.setSelectedItem(prefs.font);
		fontSizeCB.setSelectedItem(String.valueOf(prefs.fontSize));
		autoHiliteCB.setSelected(prefs.autoHilite);
		autoIndentCB.setSelected(prefs.autoIndent);
		commandColorButton.refresh();
		parameterColorButton.refresh();
		productionColorButton.refresh();
		chunkColorButton.refresh();
		bufferColorButton.refresh();
		commentColorButton.refresh();
		indentSpacesCB.setSelectedItem(String.valueOf(prefs.indentSpaces));
	}
}
