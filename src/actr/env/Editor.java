package actr.env;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledEditorKit;

import actr.model.ParseError;

class Editor extends JEditorPane {
	private Frame frame;
	private Preferences prefs;
	private Document document;
	private final Color highlightColor = new Color(240, 240, 255);
	private final Color sidebarColor = new Color(240, 240, 240);

	Editor(final Frame frame, Preferences prefs) {
		final Editor editor = this;
		this.frame = frame;
		this.prefs = prefs;

		setEditorKit(new StyledEditorKit());
		document = new Document(frame, prefs, false);
		setDocument(document);

		addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				frame.update();
			}
		});

		InputMap inputMap = getInputMap();
		int accelerator = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, accelerator),
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						moveCaretPreviousBlock(false);
					}
				});
		inputMap.put(
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, accelerator
						+ ActionEvent.SHIFT_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						moveCaretPreviousBlock(true);
					}
				});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, accelerator),
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						moveCaretNextBlock(false);
					}
				});
		inputMap.put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, accelerator
						+ ActionEvent.SHIFT_MASK), new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						moveCaretNextBlock(true);
					}
				});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (editor.getSelectionStart() != editor
								.getSelectionEnd())
							editor.setCaretPosition(editor.getSelectionStart());
						else {
							int pos = editor.getCaretPosition();
							if (pos > 0)
								editor.setCaretPosition(pos - 1);
						}
					}
				});
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (editor.getSelectionStart() != editor
								.getSelectionEnd())
							editor.setCaretPosition(editor.getSelectionEnd());
						else {
							int pos = editor.getCaretPosition();
							if (pos < editor.getModelDocument().getLength())
								editor.setCaretPosition(pos + 1);
						}
					}
				});

		setOpaque(false);
		setLayout(null);
		setMargin(new Insets(0, 12, 0, 0));
	}

	Editor createCopy() {
		Editor copy = new Editor(frame, prefs);
		copy.document = document.createCopy();
		return copy;
	}

	Document getModelDocument() {
		return document;
	}

	boolean openHelp(InputStream in, boolean suppressStyling) {
		try {
			document = new Document(frame, prefs, suppressStyling);
			document.disableExtras();
			getEditorKit().read(in, document, 0);
			setDocument(document);
			if (!suppressStyling) {
				document.enableExtras();
				document.restyleNow();
				document.resetUndo();
			}
			frame.update();
			in.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	boolean open(File file, boolean suppressStyling) {
		try {
			return openHelp(new FileInputStream(file), suppressStyling);
		} catch (Exception e) {
			return false;
		}
	}

	boolean open(URL url) {
		try {
			return openHelp(url.openStream(), false);
		} catch (Exception e) {
			return false;
		}
	}

	void scrollToVisible(int pos) {
		double top = .20;
		double bottom = .60;
		try {
			Rectangle r1 = modelToView(pos);
			if (r1 == null)
				return;
			int h = getVisibleRect().height;
			Rectangle r = new Rectangle(r1.x, r1.y - ((int) (top * h)),
					r1.width, ((int) ((top + bottom) * h)));
			scrollRectToVisible(r);
		} catch (Exception e) {
		}
	}

	void moveCaretNextBlock(boolean select) {
		int start = document.findNextMarker(getSelectionEnd(),
				document.getCommandMarkers());
		if (select)
			moveCaretPosition(start);
		else
			setCaretPosition(start);
		scrollToVisible(start);
	}

	void moveCaretPreviousBlock(boolean select) {
		int start = document.findPreviousMarker(getSelectionStart(),
				document.getCommandMarkers());
		if (select)
			moveCaretPosition(start);
		else
			setCaretPosition(start);
		scrollToVisible(start);
	}

	void clearMarkers() {
		removeAll();
	}

	void addMarkers(Iterator<ParseError> pes, boolean output) {
		int lastLine = -1;
		while (pes.hasNext()) {
			ParseError pe = pes.next();
			try {
				Marker marker = new Marker(pe.getText(),
						document.createPosition(pe.getOffset()), pe.isFatal());
				if (pe.getLine() == lastLine)
					remove(getComponentCount() - 1);
				add(marker);
				updateMarker(marker);
				if (output)
					frame.output(pe.getText() + " on line " + pe.getLine());
			} catch (BadLocationException e) {
			}
			lastLine = pe.getLine();
		}
		repaint();
	}

	void updateMarker(Marker marker) {
		try {
			Rectangle r = modelToView(marker.getOffset());
			if (r == null)
				return;
			r.x = 0;
			r.width = 11;
			marker.setBounds(r);
		} catch (BadLocationException e) {
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		try {
			Graphics2D g2d = (Graphics2D) g;

			Rectangle r = modelToView(getSelectionStart());
			Rectangle r2 = modelToView(getSelectionEnd());
			if (r.y == r2.y) {
				r.x = 0;
				r.width = getVisibleRect().width;
				g2d.setColor(highlightColor);
				g2d.fill(r);
			}

			r = getVisibleRect();
			r.x = 0;
			r.width = 12;
			g2d.setColor(sidebarColor);
			g2d.fill(r);

			for (int i = 0; i < getComponentCount(); i++) {
				Marker marker = (Marker) (getComponent(i));
				updateMarker(marker);
			}
		} catch (Exception e) {
		}

		super.paintComponent(g);
	}
}
