package kn.uni.gis.hardtasks;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

public class FillingPolygonImpl extends MapPolygonImpl {
	private String name;

	public FillingPolygonImpl(List<Coordinate> points, Color color,
			Stroke stroke) {
		super(points, color, stroke);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon#paint(java.awt
	 * .Graphics, java.awt.Polygon)
	 */
	@Override
	public void paint(Graphics g, Polygon polygon) {
		// Prepare graphics
		Color oldColor = g.getColor();
		g.setColor(color);
		Stroke oldStroke = null;
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			oldStroke = g2.getStroke();

			g2.setStroke(stroke);
		}

		// Draw
		g.drawPolygon(polygon);
		g.fillPolygon(polygon);
		// Restore graphics
		g.setColor(oldColor);
		if (g instanceof Graphics2D) {
			((Graphics2D) g).setStroke(oldStroke);
		}

		if (name != null) {
			g.setColor(Color.black);
			g.setFont(new Font("Serif", Font.BOLD, 20));

			g.drawString(name, polygon.getBounds().x
					+ (polygon.getBounds().width / 4), polygon.getBounds().y
					+ (polygon.getBounds().height / 2));
		}
	}

	public String getName() {
		return name;
	}

	public void setLabel(String name) {
		this.name = name;
	}
}
