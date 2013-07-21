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
package kn.uni.gis.server;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.Application;

import kn.uni.gis.dataimport.SQLFacade;
import kn.uni.gis.dataimport.util.GeoUtil;
import kn.uni.gis.softtasks.GisResource;
import kn.uni.gis.softtasks.SSLSQLFacade;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class ServerMain {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ServerMain.class);
	// CREATE INDEX %s_gix ON %s USING GIST (poly_geom)

	private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gis_soft";
	private static final String PROP = "etc/db.properties";
	// jdbc:postgresql:database";//jdbc:mysql://localhost:3306/versuch1";

	// private static final String DEFAULT_URL =
	// "jdbc:postgresql://91.228.52.101:5432/soft_gis";//

	// jdbc:postgresql:database";//jdbc:mysql://localhost:3306/versuch1";

	private static final String USE_SSL = "ssl";
	private static final String USER_KEY = "user";
	private static final String URL_KEY = "url";
	private static final String PWD_KEY = "password";

	private static final String DEFAULT_PWD = "admin";
	private static final String DEFAULT_USR = "postgres";

	public static GeoUtil UTIL;

	public static void main(String[] args) throws Exception {

		SQLFacade conn;
		Properties pro = new Properties();

		LOGGER.warn("loading properties server");

		pro.load(Resources.newInputStreamSupplier(
				new File(PROP).toURI().toURL()).getInput());

		if (Boolean.valueOf(pro.getProperty(USE_SSL, Boolean.FALSE.toString()))) {
			conn = new SSLSQLFacade(pro.getProperty(URL_KEY, DEFAULT_URL),
					pro.getProperty(USER_KEY, DEFAULT_USR));
		} else {
			conn = new SQLFacade(pro.getProperty(URL_KEY, DEFAULT_URL),
					pro.getProperty(USER_KEY, DEFAULT_USR), pro.getProperty(
							PWD_KEY, DEFAULT_PWD));
		}

		UTIL = new GeoUtil(conn);
		LOGGER.info("creating db table");

		conn.execute(createTable(GisResource.FOX_HUNTER));
		conn.execute(createGeometryColumn(GisResource.FOX_HUNTER));

		startJetty(new GisResource(UTIL));
	}

	private static void startJetty(final GisResource gisResource) {
		try {

			Server server = new Server(9090);

			QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setMaxThreads(100);
			server.setThreadPool(threadPool);

			ContextHandlerCollection co = new ContextHandlerCollection();

			co.addHandler(createResourceHandler(gisResource));
			co.addHandler(createWebAppHandler());

			server.setHandler(co);
			LOGGER.info("starting jetty");
			server.start();
			LOGGER.info("jetty STARTED");
			server.join();

		} catch (Exception e) {
			LOGGER.error("error on starting jetty server", e);
		}
	}

	public static GeoUtil getGeoUtil() {
		return UTIL;
	}

	private static Handler createWebAppHandler() {
		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setInitParameter("util-provider",
				ServerMain.class.getName());
		webAppContext.setWar("lib/soft-tasks-ui-0.0.1-SNAPSHOT.war");
		webAppContext.setContextPath("/ui");
		return webAppContext;
	}

	private static ServletContextHandler createResourceHandler(
			final GisResource gisResource) {
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);

		context.setContextPath("/");
		context.addServlet(new ServletHolder(new ServletContainer(
				new Application() {
					@Override
					public Set<Object> getSingletons() {
						return Collections.<Object> singleton(gisResource);
					}

				})), "/*");

		return context;
	}

	private static String createTable(String tableName) {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE ");
		builder.append(tableName);
		builder.append(" (");
		builder.append("\"id\" VARCHAR,\n");

		builder.append("\"player_id\" VARCHAR");
		builder.append(",\n");
		builder.append("\"player_name\" VARCHAR");
		builder.append(",\n");

		builder.append("\"timestamp\" timestamp with time zone");

		builder.append(" );");
		return builder.toString();
	}

	private static String createGeometryColumn(String foxHunter) {
		return String.format(
				"SELECT AddGeometryColumn('','%s','poly_geom','-1','%s',2);",
				foxHunter, "POINT");
	}
}
