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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class SQLFacade {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SQLFacade.class);

	private static final String dbDriver = "org.postgresql.Driver";
	protected final String dbUrl;
	protected final String dbUsr;
	protected final String dbPwd;

	private ComboPooledDataSource cpds;

	public SQLFacade(String dbUrl, String dbUsr, String dbPwd) {
		super();
		this.dbUrl = dbUrl;
		this.dbUsr = dbUsr;
		this.dbPwd = dbPwd;
	}

	/**
	 * öffnet Datenbank für einen Vorgang
	 * 
	 * @return
	 */
	public void openDatabase() throws Exception {

		cpds = new ComboPooledDataSource();

		cpds.setDriverClass(dbDriver); // loads the jdbc driver

		Properties newpo = new Properties();
		newpo.setProperty("user", dbUsr);
		newpo.setProperty("password", dbPwd);

		cpds.setJdbcUrl(dbUrl);
		cpds.setProperties(propsHook(newpo));
		cpds.setConnectionCustomizerClassName(PGConnectionCustomizer.class
				.getName());
		cpds.setMaxPoolSize(20);

		// to
		// database
		// withusername=postgres

	}

	protected Properties propsHook(Properties newpo) {
		return newpo;
	}

	public void executeQuery(String statement, DoWithin dowithin)
			throws SQLException {

		LOGGER.debug("executing statement: {} ", statement);
		Connection connection = cpds.getConnection();
		Statement s = connection.createStatement(); // create query
													// statement
		try {
			try {
				dowithin.doIt(s.executeQuery(statement));
			} catch (Exception e) {
				LOGGER.error("error in dowithin should not happen", e);
			}
		} finally {
			s.close();
			connection.close();
		}
	}

	public void executeQuery(PreparedStatement sql, DoWithin dowithin) {
		LOGGER.debug("executing statement: {} ", sql);
		try {
			dowithin.doIt(sql.executeQuery());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void executeSafeQuery(String statement, DoWithin dowithin,
			String... param) throws SQLException {

		LOGGER.debug("executing statement: {} ", statement);
		Connection connection = cpds.getConnection();

		PreparedStatement prepareStatement = connection
				.prepareStatement(statement);

		for (int i = 0; i < param.length; i++) {
			prepareStatement.setString(i + 1, param[i]);
		}

		try {
			try {
				dowithin.doIt(prepareStatement.executeQuery());
			} catch (Exception e) {
				LOGGER.error("error in dowithin should not happen", e);
			}
		} finally {
			prepareStatement.close();
			connection.close();
		}
	}

	public PreparedStatement prepareStatement(String statement) {
		Connection connection = null;
		try {
			connection = cpds.getConnection();
			PreparedStatement prepareStatement = connection
					.prepareStatement(statement);
			return prepareStatement;
		} catch (SQLException e) {
			LOGGER.error("error on prepare statement", e);
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e1) {
				}
			}
		}
		return null;
	}

	public boolean execute(String statement) throws SQLException {
		LOGGER.debug("executing statement: {} ", statement);
		Connection connection = cpds.getConnection();
		Statement s = connection.createStatement(); // create query
													// statement
		try {
			try {
				return s.execute(statement);
			} catch (Exception e) {
				LOGGER.error("error in dowithin should not happen", e);
			}
		} finally {
			s.close();
			connection.close();
		}
		return false;
	}

	public int[] executeBatch(String... statement) throws SQLException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("executing statment: {} ", Arrays.toString(statement));
		}
		Connection connection = cpds.getConnection();
		Statement s = connection.createStatement(); // create query

		try {
			try {
				for (String curr : statement) {
					s.addBatch(curr);
				}
				return s.executeBatch();

			} catch (Exception e) {
				LOGGER.error("error in dowithin should not happen", e);
			}
		} finally {
			s.close();
			connection.close();
		}
		return null;
	}

	public boolean closeDatabase() {
		try {
			cpds.close();
		} catch (Exception ex) {/* nothing to do */
		}
		return true;
	}

	public interface DoWithin {
		void doIt(ResultSet set) throws SQLException;
	}

};