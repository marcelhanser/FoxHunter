package kn.uni.gis.hardtasks;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

public class SizeableMapMarker implements MapMarker {

	private double lat;
	private double lon;
	private Color color;
	private int size_h;

	public SizeableMapMarker(Color color, double lat, double lon, int size) {
		this.color = color;
		this.lat = lat;
		this.lon = lon;
		this.size_h = size;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public void paint(Graphics g, Point position) {
		int size = this.size_h * 2;
		g.setColor(color);
		g.fillOval(position.x - size_h, position.y - size_h, size, size);
		g.setColor(Color.BLACK);
		g.drawOval(position.x - size_h, position.y - size_h, size, size);
	}

	@Override
	public String toString() {
		return "SizeableMapMarker at " + lat + " " + lon;
	}

}
