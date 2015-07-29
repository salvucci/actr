package actr.env;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.JPanel;

import actr.model.Symbol;
import actr.resources.Resources;

class Brain extends JPanel {
	private Frame frame;
	private Vector<Region> regions = new Vector<Region>();

	private final Image images[] = { Resources.getImage("brain1.png"), Resources.getImage("brain2.png"),
			Resources.getImage("brain3.png") };
	private final double scale = .75;
	private final int sizes[][] = { { 110, 130 }, { 115, 125 }, { 120, 120 } };
	private final int centers[][] = { { 0, -150 }, { 0, 0 }, { 0, 150 } };
	private final int shift[][] = { { -1, -10 }, { 0, -1 }, { 8, -7 } };

	private class Region {
		String buffer;
		int x, y, z;
		@SuppressWarnings("unused")
		Color color;

		Region(String buffer, int x, int y, int z, Color color) {
			this.buffer = buffer;
			this.x = (int) (x * scale);
			this.y = (int) (y * scale);
			this.z = (int) (z * scale);
			this.color = color;
		}
	}

	Brain(final Frame frame) {
		this.frame = frame;

		// regions from Anderson, Qin, Jung, & Carter, Cognitive Psychology,
		// 2007
		// origin assumed at anterior commissure
		regions.add(new Region("retrieval", 40, 21, 21, Color.cyan)); // prefrontal
		regions.add(new Region("imaginal", 23, -64, 34, Color.yellow)); // parietal
		regions.add(new Region("manual", 37, -25, 47, Color.red)); // motor1
		regions.add(new Region("goal", 5, 10, 38, Color.green)); // anterior
																	// cingulate
		regions.add(new Region("procedural", 15, 9, 2, Color.magenta)); // caudate
		regions.add(new Region("visual", 42, -60, -8, Color.blue)); // fusiform
																	// gyrus
		regions.add(new Region("aural", 47, -22, 4, Color.green)); // auditory
																	// cortex
		regions.add(new Region("vocal", 44, -12, 29, Color.blue)); // motor2

		setPreferredSize(new Dimension(200, 200));
		setSize(new Dimension(200, 200));
	}

	private Color getColor(double bold) {
		float h, s, b;
		if (bold < .25) {
			h = 0;
			s = 1;
			b = (float) (.5 + 2 * bold);
		} else if (bold < .75) {
			h = (float) (2 * (bold - .25) * .167);
			s = 1;
			b = 1;
		} else {
			h = .167f;
			s = (float) (1 - 4 * (bold - .75));
			b = 1;
		}
		Color c = Color.getHSBColor(h, s, b);
		int alpha = (int) (384 * bold);
		if (alpha > 255)
			alpha = 255;
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	private Color[] getColors(double bold) {
		bold = Math.min(Math.max(bold, 0), 1);
		Color colors[] = new Color[3];
		colors[0] = getColor(bold);
		colors[1] = getColor(.5 * bold);
		colors[2] = new Color(128, 128, 128, 0);
		return colors;
	}

	private void paintActivation(Graphics2D g2d, int x, int y, Color colors[], Region region) {
		float fractions[] = { .4f, .7f, 1.0f };
		int r = 10;
		RadialGradientPaint paint = new RadialGradientPaint(x, y, r, fractions, colors);
		g2d.setPaint(paint);
		g2d.fillOval(x - r, y - r, 2 * r, 2 * r);

		// r = 5;
		// g2d.setPaint (region.color);
		// g2d.drawRect (x-r, y-r, 2*r, 2*r);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		Graphics2D g2d = (Graphics2D) g;
		AffineTransform saved = g2d.getTransform();

		g2d.translate(getWidth() / 2, getHeight() / 2);

		for (int i = 0; i < images.length; i++) {
			int x = centers[i][0];
			int y = centers[i][1];
			int w = sizes[i][0];
			int h = sizes[i][1];
			g2d.drawImage(images[i], x - w / 2, y - h / 2, w, h, Color.black, null);
		}

		if (frame.getModel() != null) {
			for (int n = 0; n < regions.size(); n++) {
				Region region = regions.elementAt(n);
				double bold = frame.getModel().getBold().getValue(Symbol.get(region.buffer));

				if (bold > 0) {
					Color colors[] = getColors(bold);

					int x = centers[0][0] + shift[0][0] + region.x;
					int y = centers[0][1] + shift[0][1] - region.y;
					paintActivation(g2d, x, y, colors, region);
					x = centers[0][0] + shift[0][0] - region.x;
					paintActivation(g2d, x, y, colors, region);

					x = centers[1][0] + shift[1][0] + region.x;
					y = centers[1][1] + shift[1][1] - region.z;
					paintActivation(g2d, x, y, colors, region);
					x = centers[1][0] + shift[1][0] - region.x;
					paintActivation(g2d, x, y, colors, region);

					x = centers[2][0] + shift[2][0] + region.y;
					y = centers[2][1] + shift[2][1] - region.z;
					paintActivation(g2d, x, y, colors, region);
				}
			}
		}

		g2d.setTransform(saved);
	}
}
