package kn.uni.gis.hardtasks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.OsmMercator;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.RangeMap;

public class TransformationPolygonImpl extends MapPolygonImpl {
	private static final int ZOOM_LEVEL = 10;
	private Tupel[][] distanceArray;
	private int offset_x;
	private int offset_y;
	private final JMapViewer viewer;

	public TransformationPolygonImpl(JMapViewer viewer, int maxDistance,
			List<Coordinate> list, RangeMap<Integer, Color> colorMap) {

		super(Lists.newArrayList(new Coordinate(1, 5), new Coordinate(1, 5),
				new Coordinate(1, 5)));

		this.viewer = viewer;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;

		for (Coordinate point : list) {
			maxX = Math.max(maxX, getX(point));
			maxY = Math.max(maxY, getY(point));
			minX = Math.min(minX, getX(point));
			minY = Math.min(minY, getY(point));
		}

		maxX += maxDistance;
		maxY += maxDistance;
		minX -= maxDistance;
		minY -= maxDistance;

		offset_x = minX;
		offset_y = minY;

		distanceArray = new Tupel[maxY - minY][maxX - minX];

		for (int i = 0; i < distanceArray.length; i++) {
			for (int j = 0; j < distanceArray[i].length; j++) {
				distanceArray[i][j] = new Tupel(i, j, Integer.MAX_VALUE - 2);
			}
		}

		for (Coordinate point : list) {
			int lonToX = getX(point) - minX;
			int latToY = getY(point) - minY;
			circleArround(distanceArray, latToY, lonToX);
		}

		for (int i = 0; i < distanceArray.length; i++) {
			for (int j = 0; j < distanceArray[i].length; j++) {
				distanceArray[i][j].distance = getMin(
						distanceArray[i][j].distance, i, j, distanceArray);
			}
		}

		for (int i = distanceArray.length - 1; i >= 0; i--) {
			for (int j = distanceArray[i].length - 1; j >= 0; j--) {
				distanceArray[i][j].distance = getMin(
						distanceArray[i][j].distance, i, j, distanceArray);
			}
		}

		// insert colors
		for (int i = 0; i < distanceArray.length; i++) {
			for (int j = 0; j < distanceArray[i].length; j++) {
				distanceArray[i][j].color = colorMap
						.get(distanceArray[i][j].distance);
			}
		}

	}

	private void circleArround(Tupel[][] distanceArray2, int latToY, int lonToX) {
		distanceArray[latToY][lonToX].distance = 0;

		for (int i = latToY - 1; i <= latToY + 1; i++) {
			for (int j = lonToX - 1; j <= lonToX + 1; j++) {
				distanceArray[i][j].distance = 0;
			}
		}

		distanceArray[latToY + 2][lonToX].distance = 0;
		distanceArray[latToY][lonToX + 2].distance = 0;
		distanceArray[latToY - 2][lonToX].distance = 0;
		distanceArray[latToY][lonToX - 2].distance = 0;
	}

	private int getY(Coordinate point) {
		return OsmMercator.LatToY(point.getLat(), ZOOM_LEVEL);
	}

	private int getX(Coordinate point) {
		return OsmMercator.LonToX(point.getLon(), ZOOM_LEVEL);
	}

	private double getLat(int y) {
		return OsmMercator.YToLat(y, ZOOM_LEVEL);
	}

	private double getLon(int x) {
		return OsmMercator.XToLon(x, ZOOM_LEVEL);
	}

	@Override
	public void paint(Graphics g, List<Point> points) {
		super.paint(g, points);
		Color back = g.getColor();
		((Graphics2D) g).setStroke(new BasicStroke(0));
		int currZoom = viewer.getZoom();
		int width = getDefaultLength(currZoom);

		for (int i = 0; i < distanceArray.length; i++) {

			int height = -1;

			for (int j = 0; j < distanceArray[i].length; j++) {

				Tupel distance = distanceArray[i][j];
				Color color2 = distance.color;

				if (color2 != null) {

					Coordinate corrd = distance.coord;

					Point mapPosition = viewer.getMapPosition(corrd.getLat(),
							corrd.getLon());

					if (mapPosition != null) {

						height = height != -1 ? height : getHeight(
								distanceArray, mapPosition, i, j, viewer);

						g.setColor(color2);
						g.fillRect(mapPosition.x, mapPosition.y, width, height);
					}
				}
			}
		}
		g.setColor(back);
	}

	private int getHeight(Tupel[][] distanceArray2, Point position, int i,
			int j, JMapViewer viewer) {

		if (i >= distanceArray2.length - 1) {
			return getDefaultLength(viewer.getZoom());
		}

		Point mapPosition = viewer
				.getMapPosition(distanceArray2[i + 1][j].coord);

		if (mapPosition == null) {
			return getDefaultLength(viewer.getZoom());
		}

		int currHeight = mapPosition.y - position.y;

		return currHeight;
	}

	private int getDefaultLength(int currZoom) {
		return (int) Math.pow(2, currZoom - ZOOM_LEVEL < 0 ? 0 : currZoom
				- ZOOM_LEVEL);
	}

	private int getMin(int value, int i, int j, Tupel[][] distanceArray2) {
		if (value == 0)
			return 0;

		// List<int[]> ints = Lists.new
		int min = value;
		int y_length = distanceArray2.length;
		int x_length = distanceArray2[i].length;

		int i_max = (i + 1) > y_length - 1 ? y_length - 1 : i + 1;
		int i_min = (i - 1) < 0 ? 0 : i - 1;

		int j_max = (j + 1) > x_length - 1 ? x_length - 1 : j + 1;
		int j_min = (j - 1) < 0 ? 0 : j - 1;

		for (int fi = i_min; fi <= i_max; fi++) {
			for (int fj = j_min; fj <= j_max; fj++) {
				int k = distanceArray2[fi][fj].distance;
				min = Math.min(min, k);
			}
		}

		return min + 1;
	}

	@Override
	public String toString() {
		StringBuilder to = new StringBuilder();
		for (int i = 0; i < distanceArray.length; i++) {
			to.append(Arrays.toString(distanceArray[i])).append('\n');
		}
		return to.toString();
	}

	public static final Function<Integer, Integer> scale(final int max) {
		return new Function<Integer, Integer>() {

			private final int max_min = max - 0;

			@Override
			public Integer apply(Integer input) {
				return (input) / max_min;
			}
		};
	}

	private class Tupel {
		private Coordinate coord;
		private int distance;
		private Color color;

		private Tupel(Coordinate coord, int distance) {
			super();
			this.coord = coord;
			this.distance = distance;
		}

		public Tupel(int i, int j, int distance) {
			this(new Coordinate(getLat(i + offset_y), getLon(j + offset_x)),
					distance);
		}
	}
}
