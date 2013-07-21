package kn.uni.gis.hardtasks;

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kn.uni.gis.dataimport.SQLFacade;
import kn.uni.gis.dataimport.util.CPoint;
import kn.uni.gis.dataimport.util.CPolygon;
import kn.uni.gis.dataimport.util.GeoFactory;
import kn.uni.gis.dataimport.util.GeoUtil;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.postgis.PGgeometry;
import org.postgis.Point;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableRangeMap.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

/**
 * 
 * Demonstrates the usage of {@link JMapViewer}
 * 
 * @author Jan Peter Stotz
 * 
 */
public class HardTasks extends JFrame {

	private static final Color B_COLOR = new Color(0.8f, 0.1f, 0.8f, 0.4f);

	private static final Color A_COLOR = new Color(0.1f, 0.8f, 0.1f, 0.4f);

	private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gis_hard";// jdbc:postgresql:database";//jdbc:mysql://localhost:3306/versuch1";
	private static final String DEFAULT_USR = "postgres";
	private static final String DEFAULT_PWD = "admin";

	private static final Coordinate NULL_COORD = new Coordinate(0, 0);

	private static final long serialVersionUID = 1L;

	private JMapViewer map = null;

	private SQLFacade conn = new SQLFacade(DEFAULT_URL, DEFAULT_USR,
			DEFAULT_PWD);

	private GeoUtil geoRec;

	private MouseListener listener = null;

	private JLabel result = null;

	private static final Random COLOR_RANDOM = new Random();
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(5);

	public HardTasks() throws SecurityException, IOException {
		super("JMapViewer Demo");
		setSize(400, 400);

		map = new JMapViewer();
		map.setTileLoader(new OsmFileCacheTileLoader(map));

		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		JPanel panel = new JPanel();
		JPanel panelTop = new JPanel();

		result = new JLabel();

		add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(2, 4));
		panel.add(panelTop, BorderLayout.NORTH);

		add(map, BorderLayout.CENTER);

		try {
			geoRec = new GeoUtil(conn);
		} catch (Exception e1) {
			Throwables.propagate(e1);
		}

		final JComboBox hardTaskComboBox = new JComboBox(Tasks.values());
		hardTaskComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (ItemEvent.SELECTED == e.getStateChange()) {
					map.removeAllMapMarkers();
					map.removeAllMapPolygons();
					map.removeAllMapRectangles();
					if (listener != null) {
						map.removeMouseListener(listener);
					}
					((Task) e.getItem()).perform(HardTasks.this);
				}
			}
		});

		JTextField jTextField = new JTextField(3);
		panelTop.add(jTextField);

		panelTop.add(hardTaskComboBox);
		panelTop.add(result);

	}

	private void addAllPolygons(List<MapPolygon> transform) {
		for (MapPolygon pol : transform) {
			map.addMapPolygon(pol);
		}
	}

	private void addAllMarkers(List<MapMarker> transform) {
		for (MapMarker pol : transform) {
			map.addMapMarker(pol);
		}
	}

	private enum Tables {
		BERLIN_ADMINISTRATIVE, BERLIN_HIGHWAY, BERLIN_LOCATION, BERLIN_NATURAL, BERLIN_POI, BERLIN_WATER, BUILDINGS, LANDUSE, FLICKR_BERLIN;
	}

	private interface Task {
		void perform(HardTasks task);
	}

	private enum Tasks implements Task {
		IDLE, SOFT_TEST {
			@Override
			public void perform(final HardTasks task) {
				task.listener = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						Coordinate asdf = task.map.getPosition(e.getPoint());
						System.out.println("clicked! " + asdf);
						task.map.addMapMarker(new MapMarkerDot(asdf.getLat(),
								asdf.getLon()));
					}
				};
				task.map.addMouseListener(task.listener);
			}
		},
		TASK_A {

			@Override
			public void perform(HardTasks task) {

				List<MapMarker> receiveGeo = task.geoRec
						.receiveGeos(
								"SELECT f.id,ST_DISTANCE(b.poly_geom, f.poly_geom),f.poly_geom FROM buildings b, flickr_berlin f where b.name LIKE 'Branden%Tor' order by ST_DISTANCE(b.poly_geom, f.poly_geom) DESC",
								new GeoFactory<MapMarker>() {
									private double max = 0;

									@Override
									public MapMarker createGeo(
											ResultSet executeQuery)
											throws SQLException {
										double distance = executeQuery
												.getDouble(2);

										max = Math.max(
												executeQuery.getDouble(2), max);

										Point point = ((PGgeometry) executeQuery
												.getObject(3)).getGeometry()
												.getPoint(0);

										int size = (int) ((distance / max) * 50);

										return new SizeableMapMarker(
												Color.CYAN, point.getY(), point
														.getX(), size);
									}
								});

				for (MapMarker marker : receiveGeo) {
					task.map.addMapMarker(marker);
				}

			}
		},
		TASK_B {
			@Override
			public void perform(final HardTasks task) {
				final CPolygon receivePolygon = task.geoRec.receivePolygon(
						Tables.BERLIN_ADMINISTRATIVE.toString(), null);
				task.map.addMapPolygon(CPOLY_TO_MAPPOLY.apply(receivePolygon));

				task.listener = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getSource() == task.map) {
							Coordinate position = task.map.getPosition(e
									.getPoint());

							// determine the polygon border lengths in meters
							double maxError = determinePolygonBoundaryWidth(task);

							task.result.setText(task.geoRec.coordIn(
									COORD_TO_CPOINT.apply(position),
									receivePolygon, maxError).toString());
						}
					}

					private double determinePolygonBoundaryWidth(
							final HardTasks task) {
						// this is used per default on the
						return ((DEFAULT_STROKE.getLineWidth() * task.map
								.getMeterPerPixel()) / 100000);
					}
				};

				task.map.addMouseListener(task.listener);
			}
		},
		TASK_C {
			@Override
			public void perform(final HardTasks task) {

				final MapRectangleImpl first = new MapRectangleImpl(NULL_COORD,
						NULL_COORD, A_COLOR, DEFAULT_STROKE);
				final MapRectangleImpl second = new MapRectangleImpl(
						NULL_COORD, NULL_COORD, B_COLOR, DEFAULT_STROKE);

				task.map.addMapRectangle(first);
				task.map.addMapRectangle(second);

				task.listener = new MouseAdapter() {

					private MapRectangleImpl current = first;

					java.awt.Point start;

					public void mousePressed(MouseEvent e) {
						if (MouseEvent.BUTTON1 == e.getButton()) {
							start = e.getPoint();
						}
					};

					public void mouseReleased(MouseEvent e) {
						if (MouseEvent.BUTTON1 == e.getButton()) {
							current = current == first ? second : first;
							start = null;
							task.result.setText(task.geoRec.spatialRelation(
									MAPREC_TO_CPOLY.apply(first),
									MAPREC_TO_CPOLY.apply(second)).toString());
						}
					};

					public void mouseDragged(MouseEvent e) {
						if (start != null) {

							current.setTopLeft(task.map
									.getPosition(floor(start)));
							current.setBottomRight(task.map.getPosition(floor(e
									.getPoint())));
							task.map.repaint();
						}
					}

				};

				task.map.addMouseListener(task.listener);
				task.map.addMouseMotionListener((MouseMotionListener) task.listener);
			}

		},
		TASK_D_A {
			@Override
			public void perform(final HardTasks task) {

				String sql = "select a.lor,sum(ST_Area(b.poly_geom))/ST_Area(a.poly_geom),a.poly_geom "
						+ "FROM buildings b, berlin_administrative a "
						+ "WHERE b.type='commercial' AND ST_Intersects( b.poly_geom , a.poly_geom ) "
						+ "GROUP BY a.id "
						+ "ORDER BY sum(ST_Area(b.poly_geom))/ST_Area(a.poly_geom)";

				final List<Double> values = Lists.newArrayList();
				final List<String> names = Lists.newArrayList();
				List<MapPolygon> transform = Lists.transform(
						task.geoRec.receiveGeos(sql,
								new GeoFactory<CPolygon>() {

									@Override
									public CPolygon createGeo(
											ResultSet executeQuery)
											throws SQLException {

										values.add(executeQuery.getDouble(2));
										names.add(executeQuery.getString(1));

										return GeoUtil.polygonAt(executeQuery,
												3);
									}
								}),
						toTransparentMapPolygon(
								scalingColorIterator(values, 0.5f),
								new BasicStroke(4)));

				// transform
				Function<Double, Double> scaling = scale(values.get(0),
						values.get(values.size() - 1));

				for (int i = 0; i < transform.size(); i++) {
					MapPolygon mapPolygon = transform.get(i);
					((FillingPolygonImpl) mapPolygon).setLabel(String.format(
							"%s:%.2f", names.get(i),
							scaling.apply(values.get(i))));
					task.map.addMapPolygon(mapPolygon);
				}
			}
		},
		TASK_D_B {
			@Override
			public void perform(final HardTasks task) {

				String sql = "select a.poly_geom "
						+ "FROM berlin_natural a "
						+ "WHERE (a.type='forest') and (a.name LIKE '%park%' or a.name LIKE '%garten%') and exists( "
						+ "	select b.id from berlin_water b where ST_DWithin(a.poly_geom, b.poly_geom, 50,true)"
						+ " ) " + "GROUP BY a.id";

				List<MapPolygon> transform = Lists.transform(task.geoRec
						.receiveGeos(sql, new GeoFactory<CPolygon>() {

							@Override
							public CPolygon createGeo(ResultSet executeQuery)
									throws SQLException {

								// System.out.println(executeQuery.getFloat(1));
								return GeoUtil.polygonAt(executeQuery, 1);
							}
						}), CPOLY_TO_MAPPOLY_ONE_COLOR);
				System.out.println(transform.size());
				task.addAllPolygons(transform);
			}
		},
		TASK_D_C {
			@Override
			public void perform(final HardTasks task) {
				String sql = "select count(f.poly_geom), a.poly_geom "
						+ "FROM berlin_administrative a, flickr_berlin f "
						+ "WHERE ST_covers(a.poly_geom,f.poly_geom) "
						+ "GROUP BY a.id " + "ORDER BY count(f.poly_geom);";

				final List<Double> values = Lists.newArrayList();
				List<MapPolygon> transform = Lists.transform(
						task.geoRec.receiveGeos(sql,
								new GeoFactory<CPolygon>() {

									@Override
									public CPolygon createGeo(
											ResultSet executeQuery)
											throws SQLException {

										values.add(executeQuery.getDouble(1));

										return GeoUtil.polygonAt(executeQuery,
												2);
									}
								}),
						toTransparentMapPolygon(
								scalingColorIterator(values, 0.5f),
								new BasicStroke(4)));

				// transform
				Function<Double, Double> scaling = scale(values.get(0),
						values.get(values.size() - 1));

				for (int i = 0; i < transform.size(); i++) {
					MapPolygon mapPolygon = transform.get(i);
					((FillingPolygonImpl) mapPolygon).setLabel(String.format(
							"%.4f", scaling.apply(values.get(i))));
					task.map.addMapPolygon(mapPolygon);
				}
			}
		},
		TASK_D_D {
			@Override
			public void perform(final HardTasks task) {
				String sql = "select a.poly_geom " + "FROM berlin_poi a "
						+ "WHERE a.name LIKE 'Museum:%' ";

				List<CPoint> receiveGeo = task.geoRec.receiveGeos(sql,
						new GeoFactory<CPoint>() {
							@Override
							public CPoint createGeo(ResultSet executeQuery)
									throws SQLException {
								return GeoUtil.pointAt(executeQuery, 1);
							}
						});
				TransformationPolygonImpl transformationPolygonImpl = new TransformationPolygonImpl(
						task.map, 30, Lists.transform(receiveGeo,
								CPOINT_TO_COORD), createColoredRangeMap());
				task.addAllMarkers(Lists.transform(receiveGeo,
						CPOINT_TO_MAP_MARKER));
				task.addAllPolygons(Collections
						.<MapPolygon> singletonList(transformationPolygonImpl));

			}

			private RangeMap<Integer, Color> createColoredRangeMap() {
				@SuppressWarnings("unchecked")
				List<Range<Integer>> ranges = Lists.newArrayList(
						Range.closed(0, 1), Range.closed(2, 3),
						Range.closed(4, 5), Range.closed(6, 8),
						Range.closed(9, 11), Range.closed(12, 14),
						Range.closed(15, 17), Range.closed(18, 21),
						Range.closed(22, 40));

				Builder<Integer, Color> builder = ImmutableRangeMap.builder();

				Function<Double, Double> scaledFunc = scale(0,
						ranges.get(ranges.size() - 1).lowerEndpoint());

				for (Range<Integer> rang : ranges) {
					builder.put(
							rang,
							new Color(scaledFunc.apply(
									rang.lowerEndpoint().doubleValue())
									.floatValue(), scaledFunc.apply(
									rang.lowerEndpoint().doubleValue())
									.floatValue(), 0f, 0.6f));
				}

				return builder.build();
			}
		};

		@Override
		public void perform(HardTasks task) {
			// NOOP
		}
	}

	private static java.awt.Point floor(java.awt.Point start) {
		return new java.awt.Point(start.x - (start.x % 20), start.y
				- (start.y % 20));
	};

	public static final Function<MapPolygon, CPolygon> MAPPOLY_TO_CPOLY = new Function<MapPolygon, CPolygon>() {
		@Override
		public CPolygon apply(MapPolygon input) {
			return new CPolygon(Lists.transform(input.getPoints(),
					COORD_TO_CPOINT));
		}
	};

	public static final Function<MapRectangle, CPolygon> MAPREC_TO_CPOLY = new Function<MapRectangle, CPolygon>() {
		@Override
		public CPolygon apply(MapRectangle input) {

			double min_lon = Math.min(input.getTopLeft().getLon(), input
					.getBottomRight().getLon());
			double min_lat = Math.min(input.getTopLeft().getLat(), input
					.getBottomRight().getLat());
			double max_lon = Math.max(input.getTopLeft().getLon(), input
					.getBottomRight().getLon());
			double max_lat = Math.max(input.getTopLeft().getLat(), input
					.getBottomRight().getLat());

			return new CPolygon(new CPoint(min_lon, min_lat), new CPoint(
					min_lon, max_lat), new CPoint(max_lon, max_lat),
					new CPoint(max_lon, min_lat), new CPoint(min_lon, min_lat));
		}
	};

	public static final Function<CPolygon, MapPolygon> CPOLY_TO_MAPPOLY = new Function<CPolygon, MapPolygon>() {
		@Override
		public MapPolygon apply(CPolygon input) {
			return new MapPolygonImpl(Lists.transform(input.getPoints(),
					CPOINT_TO_COORD), new Color(COLOR_RANDOM.nextInt()),
					DEFAULT_STROKE);
		}
	};

	public static final Function<CPolygon, MapPolygon> CPOLY_TO_MAPPOLY_ONE_COLOR = new Function<CPolygon, MapPolygon>() {
		@Override
		public MapPolygon apply(CPolygon input) {
			return new MapPolygonImpl(Lists.transform(input.getPoints(),
					CPOINT_TO_COORD), Color.red, DEFAULT_STROKE);
		}
	};

	public static final Function<CPolygon, MapPolygon> toTransparentMapPolygon(
			final Iterator<Color> colorIterator, final Stroke stroke) {

		return new Function<CPolygon, MapPolygon>() {

			@Override
			public MapPolygon apply(CPolygon input) {
				return new FillingPolygonImpl(Lists.transform(
						input.getPoints(), CPOINT_TO_COORD),
						colorIterator.next(), stroke);
			}
		};
	}

	public static Iterator<Color> scalingColorIterator(List<Double> values,
			final float alpha) {
		final Function<Double, Double> scaling = scale(values.get(0),
				values.get(values.size() - 1));

		return Iterators.transform(values.iterator(),
				new Function<Double, Color>() {

					@Override
					public Color apply(Double input) {
						float curr = scaling.apply(input).floatValue();
						return new Color(curr, 1.0f - curr, 0f, alpha);
					}
				});
	}

	public static final Function<Double, Double> scale(final double min,
			final double max) {
		return new Function<Double, Double>() {

			private final double max_min = max - min;

			@Override
			public Double apply(@Nullable Double input) {
				return (input - min) / max_min;
			}
		};
	}

	public static final Function<Coordinate, CPoint> COORD_TO_CPOINT = new Function<Coordinate, CPoint>() {
		@Override
		public CPoint apply(Coordinate input) {
			return new CPoint(input.getLon(), input.getLat());
		}
	};

	public static final Function<CPoint, MapMarker> CPOINT_TO_MAP_MARKER = new Function<CPoint, MapMarker>() {
		@Override
		public MapMarker apply(CPoint input) {
			return new MapMarkerDot(input.getLat(), input.getLon());
		}
	};

	public static final Function<CPoint, Coordinate> CPOINT_TO_COORD = new Function<CPoint, Coordinate>() {
		@Override
		public Coordinate apply(CPoint input) {
			return new Coordinate(input.getLat(), input.getLon());
		}
	};

	/**
	 * @param args
	 * @throws IOException
	 * @throws SecurityException
	 */
	public static void main(String[] args) throws SecurityException,
			IOException {
		new HardTasks().setVisible(true);
	}

}
