/**
 * Copyright (C) 2013 Marcel Hanser & Martin Koelbl <nanohome.de@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kn.uni.gis.dataimport.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import kn.uni.gis.dataimport.SQLFacade;
import kn.uni.gis.dataimport.SQLFacade.DoWithin;

import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class GeoUtil {

	private static GeoUtil instance;

	private SQLFacade conn;

	public enum Location {
		INSIDE, OUTSIDE, BORDER;

		public static Location forPosition(boolean inside, boolean border) {
			if (border)
				return BORDER;
			if (inside)
				return INSIDE;
			return OUTSIDE;
		}
	}

	public GeoUtil(SQLFacade conn) throws Exception {
		this.conn = conn;
		conn.openDatabase();
		instance = this;
	}

	public List<CPolygon> receivePolygons(String dataTable) {
		return receivePolygons(dataTable, null);
	}

	public CPolygon receivePolygon(String dataTable, String whereClause) {
		return Iterators.getNext(receivePolygons(dataTable, whereClause)
				.iterator(), null);
	}

	public List<CPolygon> receivePolygons(String dataTable, String whereClause) {
		return receiveGeos(dataTable, whereClause, new GeoFactory<CPolygon>() {

			@Override
			public CPolygon createGeo(ResultSet object) throws SQLException {
				int index = 1;

				CPolygon cPolygon = polygonAt(object, index);

				return cPolygon;
			}

		});
	}

	public Location coordIn(CPoint point, CPolygon poly, final double e) {
		return Iterators
				.getNext(
						receiveGeos(
								select(within(gFromT(point), gFromT(poly)),
										distance(gFromT(point),
												boundary(gFromT(poly)))),
								new GeoFactory<Location>() {
									@Override
									public Location createGeo(ResultSet object)
											throws SQLException {
										System.out.println(object.getMetaData()
												.getColumnCount());
										return Location.forPosition(
												object.getBoolean(1),
												(object.getDouble(2) < e));
									}
								}).iterator(), null);
	}

	public <T> List<T> receiveGeos(String dataTable, String whereClause,
			GeoFactory<T> factory) {
		String toExecute;
		if (whereClause == null) {
			toExecute = String.format("SELECT poly_geom from %s;", dataTable);
		} else {
			toExecute = String.format("SELECT poly_geom from %s WHERE %s;",
					dataTable, whereClause);
		}

		return receiveGeos(toExecute, factory);
	}

	public <T> T receiveGeo(final String sql, final GeoFactory<T> factory) {
		return Iterators.getNext(receiveGeos(sql, factory).iterator(), null);
	}

	public <T> List<T> receiveGeos(final String sql, final GeoFactory<T> factory) {

		final ImmutableList.Builder<T> builder = ImmutableList.builder();

		try {
			conn.executeQuery(sql, new DoWithin() {

				@Override
				public void doIt(ResultSet executeQuery) throws SQLException {
					while (executeQuery.next()) {
						builder.add(factory.createGeo(executeQuery));
					}
				}

			});

		} catch (SQLException e) {
			Throwables.propagate(e);
		}

		return builder.build();
	}

	public NineCutModel spatialRelation(CPolygon a, CPolygon b) {
		return Iterators.getOnlyElement(receiveGeos(
				select(relate(gFromT(a), gFromT(b)),
						intersects(boundary(gFromT(a)), boundary(gFromT(b)))),
				new GeoFactory<NineCutModel>() {
					@Override
					public NineCutModel createGeo(ResultSet executeQuery)
							throws SQLException {
						return NineCutModel.forDE9IM(executeQuery.getString(1),
								executeQuery.getBoolean(2));
					}
				}).iterator());
	}

	private static PointIterator pointIterator(Geometry geo) {
		return new PointIterator(geo);
	}

	private static class PointIterator implements Iterator<Point> {
		private Geometry geometry;
		private int currIndex = 0;

		private PointIterator(Geometry geometry) {
			super();
			this.geometry = geometry;
		}

		@Override
		public boolean hasNext() {
			return currIndex < geometry.numPoints();
		}

		@Override
		public Point next() {
			return geometry.getPoint(currIndex++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private static Geo boundary(final Geo a) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				builder.append("ST_boundary(");
				a.toSql(builder);
				builder.append(")");
			}
		};
	}

	private static Geo gFromT(final Geo a) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				builder.append("ST_GeomFromText('");
				a.toSql(builder);
				builder.append("')");
			}
		};
	}

	private static Geo within(final Geo a, final Geo b) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				defaultFunc(builder, "ST_within", a, b);
			}
		};
	}

	private static Geo relate(final Geo a, final Geo b) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				defaultFunc(builder, "ST_relate", a, b);
			}
		};
	}

	private static Geo distance(final Geo a, final Geo b) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				defaultFunc(builder, "ST_distance", a, b);
			}
		};
	}

	private static Geo intersects(final Geo a, final Geo b) {
		return new Geo() {
			@Override
			public void toSql(StringBuilder builder) {
				defaultFunc(builder, "ST_intersects", a, b);
			}
		};
	}

	private static void defaultFunc(StringBuilder builder, String functionName,
			Geo a, Geo b) {
		builder.append(functionName);
		builder.append("(");
		a.toSql(builder);
		builder.append(",");
		b.toSql(builder);
		builder.append(")");
	}

	// private stag

	private static String select(final Geo... a) {
		StringBuilder builder = new StringBuilder("select ");
		for (SqlAble able : a) {
			able.toSql(builder);
			builder.append(',');
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append(';');
		return builder.toString();
	}

	public static String toString(ResultSet resultSet) {
		try {
			StringBuilder builder = new StringBuilder();
			int columnCount = resultSet.getMetaData().getColumnCount();
			while (resultSet.next()) {
				for (int i = 0; i < columnCount;) {
					builder.append(resultSet.getString(i + 1));
					if (++i < columnCount)
						builder.append(",");
				}
				builder.append("\r\n");
			}
			return builder.toString();
		} catch (SQLException e) {
			throw Throwables.propagate(e);
		}
	}

	public static CPolygon polygonAt(ResultSet object, int index)
			throws SQLException {
		PGgeometry geo = (PGgeometry) object.getObject(index);
		CPolygon cPolygon = new CPolygon(ImmutableList.copyOf((Iterators
				.transform(pointIterator(geo.getGeometry()),
						new Function<Point, CPoint>() {
							@Override
							public CPoint apply(Point input) {
								return new CPoint(input.getX(), input.getY());
							}
						}))));
		return cPolygon;
	}

	public static CPoint pointAt(ResultSet object, int index)
			throws SQLException {
		PGgeometry geo = (PGgeometry) object.getObject(index);
		Point firstPoint = geo.getGeometry().getFirstPoint();
		return new CPoint(firstPoint.getX(), firstPoint.getY());
	}

	public SQLFacade getConn() {
		return conn;
	}

	public static GeoFactory<String> stringAt(final int i) {
		return new GeoFactory<String>() {

			@Override
			public String createGeo(ResultSet executeQuery) throws SQLException {
				return executeQuery.getString(i);
			}
		};
	}

	public static GeoUtil getInstance() {
		return instance;
	}
}
