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
package kn.uni.gis.softtasks;

import java.util.Properties;

public class SSLSQLFacade extends kn.uni.gis.dataimport.SQLFacade {

	public SSLSQLFacade(String dbUrl, String dbUsr) {
		super(dbUrl, dbUsr, "");
	}

	@Override
	protected Properties propsHook(Properties newpo) {
		newpo.remove("password");
		newpo.setProperty("ssl", "true");
		newpo.setProperty("sslfactory",
				"org.postgresql.ssl.NonValidatingFactory");

		return newpo;
	}
};