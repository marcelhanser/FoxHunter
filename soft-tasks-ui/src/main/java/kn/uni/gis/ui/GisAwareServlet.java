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
package kn.uni.gis.ui;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import kn.uni.gis.dataimport.SQLFacade;
import kn.uni.gis.dataimport.util.GeoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;

public class GisAwareServlet extends AbstractApplicationServlet {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(GisAwareServlet.class);

	private static final String DEFAULT_PWD = "admin";
	private static final String DEFAULT_USR = "postgres";
	private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/gis_soft";
	private static final String DEFAULT_PASSWORD = "secret";

	private static final long serialVersionUID = 1L;
	private GeoUtil geoUtil;
	private String admPassword;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		try {
			String initParameter = servletConfig
					.getInitParameter("util-provider");

			LOGGER.info("util provider: {}", initParameter);

			geoUtil = GeoUtil.getInstance() == null ? new GeoUtil(
					new SQLFacade(DEFAULT_URL, DEFAULT_USR, DEFAULT_PWD))
					: GeoUtil.getInstance();

			String password = servletConfig.getInitParameter("admPassword");
			admPassword = password != null ? password : DEFAULT_PASSWORD;

		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	@Override
	protected Application getNewApplication(HttpServletRequest request)
			throws ServletException {
		return new GameApplication(admPassword, geoUtil);
	}

	@Override
	protected Class<? extends Application> getApplicationClass()
			throws ClassNotFoundException {
		return GameApplication.class;
	}

}
