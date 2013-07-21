package kn.uni.gis.hardtasks;

import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.OsmMercator;

import com.google.common.collect.Lists;

public class FillingRectangle extends FillingPolygonImpl {

	public FillingRectangle(Point x, Point y, Color color, Stroke stroke,
			int zoomLevel) {
		super(createPoints(x, y, zoomLevel), color, stroke);
	}

	private static List<Coordinate> createPoints(Point x, Point y, int zoomLevel) {
		int x_min = Math.min(x.x, y.x);
		int y_min = Math.min(x.y, y.y);
		int x_max = Math.max(x.x, y.x);
		int y_max = Math.max(x.y, y.y);

		return Lists.newArrayList(coord(x_min, y_min, zoomLevel),
				coord(x_min, y_max, zoomLevel), coord(x_max, y_max, zoomLevel),
				coord(x_max, y_min, zoomLevel));
	}

	private static Coordinate coord(int x, int y, int zoomLevel) {
		return new Coordinate(OsmMercator.YToLat(y, zoomLevel),
				OsmMercator.XToLon(x, zoomLevel));
	}
}
