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
package kn.uni.gis.dataimport;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Files;

public class Main {

	private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gis_hard";// jdbc:postgresql:database";//jdbc:mysql://localhost:3306/versuch1";
	private static final String DEFAULT_USR = "postgres";
	private static final String DEFAULT_PWD = "admin";

	private static final int ID_INDEX = 0;

	private static final String INPUT_DIR = GenerateCSV.OUTPUT;

	public static void main(String[] args) throws Exception {

		SQLFacade sqlConnection = new SQLFacade(DEFAULT_URL, DEFAULT_USR,
				DEFAULT_PWD);
		try {
			sqlConnection.openDatabase();

			updateDB(sqlConnection, INPUT_DIR);
			// where lor like '%s'

		} finally {
			sqlConnection.closeDatabase();
		}
	}

	private static void updateDB(SQLFacade sqlConnection, String inputDir)
			throws IOException, SQLException {

		for (File file : new File(inputDir).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		})) {

			String tableName = file.getName().substring(0,
					file.getName().indexOf("."));
			List<String> readLines = Files.readLines(file, Charsets.UTF_8);
			createTables(sqlConnection, tableName, readLines);

			insertData(sqlConnection, tableName, readLines);

			createIndeces(sqlConnection, tableName);

			System.out.println(String.format("read file: %s",
					file.getAbsolutePath()));
		}
	}

	private static void createIndeces(SQLFacade sqlConnection, String tableName)
			throws SQLException {
		sqlConnection.execute(String.format(
				"CREATE INDEX %s_gix ON %s USING GIST (poly_geom);", tableName,
				tableName));
	}

	private static void insertData(SQLFacade sqlConnection,
			final String tableName, final List<String> readLines)
			throws SQLException {
		// Mapping from id -> multiple (parsed) lines
		Multimap<String, List<String>> index = Multimaps.index(Lists.transform(
				readLines, new Function<String, List<String>>() {
					@Override
					public List<String> apply(String input) {
						ArrayList<String> newArrayList = Lists
								.newArrayList(Splitter.on("\t").trimResults()
										.split(input));
						newArrayList.remove(newArrayList.size() - 1);
						return newArrayList;

					}

				}), new Function<List<String>, String>() {
			@Override
			public String apply(List<String> input) {
				return input.get(ID_INDEX);
			}
		});

		// Collapse now to a mapping id -> one line
		Map<String, List<String>> collapsedValues = Maps.transformValues(
				index.asMap(),
				new Function<Collection<List<String>>, List<String>>() {

					@Override
					public List<String> apply(Collection<List<String>> input) {
						List<String> toReturn = Lists.newArrayList();

						// set id
						List<String> firstLine = input.iterator().next();
						toReturn.add(firstLine.get(0));

						// set coordinates
						StringBuilder stringBuilder = new StringBuilder();

						for (List<String> ret : input) {
							stringBuilder.append(ret.get(1));
							stringBuilder.append(" ");
							stringBuilder.append(ret.get(2));
							stringBuilder.append(",");
						}
						String coords = stringBuilder.toString();

						// add additional line data
						for (int i = 5; i < firstLine.size(); i++) {
							toReturn.add("'"
									+ firstLine.get(i).replaceAll("'", "''")
									+ "'");
						}

						// delete last ',' and add GeoInformation as last column
						toReturn.add(GeoColumn.getGeoColumn(tableName)
								.getGeoInsertionSQL(
										coords.substring(0,
												coords.lastIndexOf(','))));
						return toReturn;
					}

				});

		List<String> toexecute = Lists.newArrayList();
		for (Map.Entry<String, List<String>> entry : collapsedValues.entrySet()) {
			toexecute.add(String.format("insert into \"%s\" values (%s);",
					tableName, Joiner.on(",").join(entry.getValue())));
		}
		sqlConnection.executeBatch(toexecute.toArray(new String[toexecute
				.size()]));

	}

	private static void createTables(SQLFacade sqlConnection, String tableName,
			List<String> readLines) throws SQLException {
		String[] warLine = readLines.remove(0).split("\t");
		String[] columnsNames = Arrays.copyOfRange(warLine, 5, warLine.length);

		sqlConnection.execute(dropTable(tableName));
		sqlConnection.execute(createTable(tableName, columnsNames));
		sqlConnection.execute(GeoColumn.getGeoColumn(tableName)
				.getGeoColumnSQL(tableName));
	}

	private static String dropTable(String tableName) {
		StringBuilder builder = new StringBuilder();
		builder.append("DROP TABLE ");
		builder.append(tableName);
		builder.append(";");
		return builder.toString();
	}

	private static String createTable(String tableName, String[] columnsNames) {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(tableName);
		builder.append(" (");
		builder.append("\"id\" int PRIMARY KEY\n");

		Iterator<String> forArray = Iterators.forArray(columnsNames);
		if (forArray.hasNext()) {
			do {
				builder.append(",\n");
				builder.append(forArray.next().replaceAll(":", "_")
						.replaceAll("NATURAL", "NAT"));
				builder.append(" character varying(256)");
			} while (forArray.hasNext());
		}
		builder.append(" );");
		return builder.toString();
	}

	public enum GeoColumn implements GeoColumnCreation {

		POLYGON("POLYGON", "water", "administrative", "natural", "buildings",
				"landuse"), MULTINE("LINESTRING", "highway"), POINT("POINT",
				"poi", "location", "flickr_berlin");

		private final String type;
		private String[] patterns;

		private GeoColumn(String type, String... patterns) {
			this.type = type;
			this.patterns = patterns;
		}

		public static GeoColumn getGeoColumn(String tableName) {
			for (GeoColumn col : values()) {
				for (String curr : col.patterns) {
					if (tableName.contains(curr)) {
						return col;
					}
				}
			}
			throw new IllegalArgumentException("tablename: " + tableName);
		}

		@Override
		public String getGeoColumnSQL(String tableName) {
			return String
					.format("SELECT AddGeometryColumn('','%s','poly_geom','-1','%s',2);",
							tableName, type);
		}

		@Override
		public String getGeoInsertionSQL(String points) {
			if (type.equals(MULTINE.type) || type.equals(POINT.type)) {
				return String.format("ST_GeomFromText('%s(%s)',-1)", type,
						points);
			}
			return String
					.format("ST_GeomFromText('%s((%s))',-1)", type, points);
		}
	}

	private interface GeoColumnCreation {
		String getGeoColumnSQL(String tableName);

		String getGeoInsertionSQL(String points);

	}

}
