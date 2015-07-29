package actr.env;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

class FindPanel extends JPanel {
	private Frame frame;
	private boolean forEditor;
	private JTextField findTF, replaceTF;
	private JCheckBox ignoreCaseCB, wrapAroundCB;
	private JButton nextButton, previousButton, replaceButton, replaceAllButton;
	private int searchStart;
	private boolean inPanel;

	FindPanel(final Frame frame, final boolean forEditor) {
		super();

		this.frame = frame;
		this.forEditor = forEditor;

		findTF = new JTextField(20);
		replaceTF = new JTextField(20);
		makeSmall(findTF);
		makeSmall(replaceTF);

		findTF.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				find(true);
				update();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				find(true);
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				find(true);
				update();
			}
		});
		findTF.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				JTextComponent textComponent = (forEditor) ? frame.getEditor() : frame.getOutputArea();
				searchStart = textComponent.getSelectionStart();
			}

			@Override
			public void focusLost(FocusEvent e) {
			}
		});

		JLabel findLabel = new JLabel("Find:");
		findLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		makeSmall(findLabel);

		JLabel replaceLabel = new JLabel("Replace:");
		replaceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		makeSmall(replaceLabel);

		ignoreCaseCB = new JCheckBox("Ignore case");
		ignoreCaseCB.setSelected(true);
		makeSmall(ignoreCaseCB);

		wrapAroundCB = new JCheckBox("Wrap around");
		wrapAroundCB.setSelected(false);
		makeSmall(wrapAroundCB);

		JPanel left = new JPanel();
		left.setLayout(new GridLayout(0, 1));
		left.add(findLabel);
		if (forEditor)
			left.add(replaceLabel);

		JPanel right = new JPanel();
		right.setLayout(new GridLayout(0, 1));
		right.add(findTF);
		if (forEditor)
			right.add(replaceTF);

		JPanel cbPanel = new JPanel();
		cbPanel.setLayout(new BoxLayout(cbPanel, BoxLayout.X_AXIS));
		cbPanel.add(Box.createHorizontalStrut(left.getPreferredSize().width));
		cbPanel.add(ignoreCaseCB);
		cbPanel.add(Box.createHorizontalStrut(10));
		cbPanel.add(wrapAroundCB);
		cbPanel.add(Box.createHorizontalGlue());

		nextButton = new JButton("Next");
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				findNext();
			}
		});
		nextButton.setEnabled(false);
		makeSmall(nextButton);

		previousButton = new JButton("Previous");
		previousButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				findPrevious();
			}
		});
		previousButton.setEnabled(false);
		makeSmall(previousButton);

		replaceButton = new JButton("Replace");
		replaceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				replace();
			}
		});
		replaceButton.setEnabled(false);
		makeSmall(replaceButton);

		replaceAllButton = new JButton("Replace All");
		replaceAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				replaceAll();
			}
		});
		replaceAllButton.setEnabled(false);
		makeSmall(replaceAllButton);

		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(0, 2));
		buttons.add(previousButton);
		buttons.add(nextButton);
		if (forEditor)
			buttons.add(replaceButton);
		if (forEditor)
			buttons.add(replaceAllButton);

		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());
		main.add(left, BorderLayout.WEST);
		main.add(right, BorderLayout.CENTER);
		main.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);

		inPanel = false;

		final JPanel self = this;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				inPanel = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				inPanel = false;
				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				Rectangle rect = new Rectangle(self.getWidth() - 12, 0, 12, 12);
				if (rect.contains(e.getX(), e.getY()))
					frame.findHide();
			}
		});

		setBorder(new AbstractBorder() {
			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				g.setColor(Color.gray);
				g.drawRect(x, y, width - 1, height - 1);
				if (inPanel)
					g.drawString("x", width - 10, 10);
			}

			@Override
			public Insets getBorderInsets(Component c) {
				return new Insets(6, 18, 6, 12);
			}
		});

		setVisible(false);
	}

	JComponent makeSmall(JComponent component) {
		component.putClientProperty("JComponent.sizeVariant", "small");
		return component;
	}

	void scrollTo(JTextComponent tc, int pos, double proportion) {
		double top = .20;
		double bottom = .60;
		try {
			Rectangle r1 = tc.modelToView(pos);
			if (r1 == null)
				return;
			int h = tc.getVisibleRect().height;
			Rectangle r = new Rectangle(r1.x, r1.y - ((int) (top * h)), r1.width, ((int) ((top + bottom) * h)));
			tc.scrollRectToVisible(r);
		} catch (Exception e) {
		}
	}

	void find(boolean forward) {
		JTextComponent textComponent = (forEditor) ? frame.getEditor() : frame.getOutputArea();
		String findText = findTF.getText();
		if (findText.equals(""))
			return;
		String text = textComponent.getText();
		if (ignoreCaseCB.isSelected()) {
			findText = findText.toLowerCase();
			text = text.toLowerCase();
		}
		int pos = -1;
		if (forward) {
			pos = text.indexOf(findText, searchStart);
			if (pos == -1)
				pos = text.indexOf(findText);
		} else {
			pos = text.lastIndexOf(findText, searchStart);
			if (pos == -1)
				pos = text.lastIndexOf(findText);
		}
		textComponent.setCaretPosition((pos == -1) ? 0 : pos);
		textComponent.moveCaretPosition((pos == -1) ? 0 : pos + findText.length());
		scrollTo(textComponent, (pos == -1) ? 0 : pos, 0.25);
	}

	void findNext() {
		JTextComponent textComponent = (forEditor) ? frame.getEditor() : frame.getOutputArea();
		searchStart = textComponent.getSelectionStart() + 1;
		if (searchStart >= textComponent.getText().length())
			searchStart = 0;
		find(true);
	}

	void findPrevious() {
		JTextComponent textComponent = (forEditor) ? frame.getEditor() : frame.getOutputArea();
		searchStart = textComponent.getSelectionStart() - 1;
		if (searchStart < 0)
			searchStart = textComponent.getText().length() - 1;
		find(false);
	}

	void replace() {
		if (!forEditor)
			return;

		int lastPos = frame.getEditor().getSelectionStart();
		int selectionLength = frame.getEditor().getSelectionEnd() - frame.getEditor().getSelectionStart();
		if (lastPos == -1 || selectionLength == 0)
			return;
		String replaceText = replaceTF.getText();
		frame.getDocument().saveState();
		frame.getDocument().disableExtras();
		try {
			frame.getDocument().remove(lastPos, selectionLength);
			frame.getDocument().insertString(lastPos, replaceText, Document.styleNormal);
		} catch (Exception ex) {
		}
		frame.getDocument().enableExtras();
		findNext();
	}

	void replaceAll() {
		if (!forEditor)
			return;

		String findText = findTF.getText();
		if (findText.equals(""))
			return;
		String replaceText = replaceTF.getText();
		String text = frame.getEditor().getText();
		if (ignoreCaseCB.isSelected()) {
			findText = findText.toLowerCase();
			text = text.toLowerCase();
		}
		int pos = text.lastIndexOf(findText);
		if (pos >= 0) {
			frame.getDocument().saveState();
			frame.getDocument().disableExtras();
			while (pos >= 0 && pos < text.length()) {
				try {
					frame.getDocument().replace(pos, findText.length(), replaceText, Document.styleNormal);
				} catch (Exception ex) {
				}
				text = text.substring(0, pos);
				pos = text.lastIndexOf(findText);
			}
			frame.getDocument().enableExtras();
			frame.getEditor().setCaretPosition(0);
			frame.getEditor().moveCaretPosition(0);
			update();
		} else
			Toolkit.getDefaultToolkit().beep();
	}

	@Override
	public void grabFocus() {
		findTF.grabFocus();
		frame.getRootPane().setDefaultButton(nextButton);
	}

	@Override
	public boolean hasFocus() {
		return findTF.hasFocus();
	}

	boolean isFindNextPossible() {
		return (!findTF.getText().equals(""));
	}

	void update() {
		JTextComponent textComponent = (forEditor) ? frame.getEditor() : frame.getOutputArea();
		if (textComponent == null)
			return;
		boolean findEnabled = (!findTF.getText().equals(""));
		boolean replaceEnabled = findEnabled && (textComponent.getSelectionEnd() > textComponent.getSelectionStart());
		nextButton.setEnabled(findEnabled);
		previousButton.setEnabled(findEnabled);
		replaceAllButton.setEnabled(forEditor && findEnabled);
		replaceButton.setEnabled(forEditor && replaceEnabled);
		frame.update();
	}
}
