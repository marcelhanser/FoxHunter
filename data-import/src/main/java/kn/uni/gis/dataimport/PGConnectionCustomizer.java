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

import org.postgresql.PGConnection;

public class PGConnectionCustomizer implements
		com.mchange.v2.c3p0.ConnectionCustomizer {

	@Override
	public void onAcquire(Connection c, String parentDataSourceIdentityToken)
			throws Exception {
		((PGConnection) c)
				.addDataType("geometry", org.postgis.PGgeometry.class); // add
																		// support
																		// for
																		// Geometry
																		// types
	}

	@Override
	public void onDestroy(Connection c, String parentDataSourceIdentityToken)
			throws Exception {

	}

	@Override
	public void onCheckOut(Connection c, String parentDataSourceIdentityToken)
			throws Exception {

	}

	@Override
	public void onCheckIn(Connection c, String parentDataSourceIdentityToken)
			throws Exception {

	}

}
