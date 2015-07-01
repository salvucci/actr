package actr.env;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.text.Position;

class Marker extends JPanel {
	private String text;
	private Position pos;
	private boolean fatal;

	Marker(String text, Position pos, boolean fatal) {
		this.text = text;
		this.pos = pos;
		this.fatal = fatal;
		setToolTipText(text);
		setCursor(Cursor.getDefaultCursor());
	}

	Marker(String text, Position pos) {
		this(text, pos, false);
	}

	String getText() {
		return text;
	}

	Position getPosition() {
		return pos;
	}

	int getOffset() {
		return pos.getOffset();
	}

	@Override
	public String toString() {
		return text;
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle r = new Rectangle(2, getHeight() / 2 - 3, 7, 7);
		g2d.setColor(fatal ? Color.red : Color.yellow);
		g2d.fill(r);
		g2d.setColor(Color.gray);
		g2d.draw(r);
	}
}
