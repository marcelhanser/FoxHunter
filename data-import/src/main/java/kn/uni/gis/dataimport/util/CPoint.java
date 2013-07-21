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

public final class CPoint implements Geo {
	// x
	private final double lon;
	// y
	private final double lat;

	public CPoint(double lon, double lat) {
		super();
		this.lon = lon;
		this.lat = lat;
	}

	public CPoint(double lat, double lon, boolean dings) {
		super();
		this.lon = lon;
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public String toString() {
		return lon + " " + lat;
	}

	@Override
	public void toSql(StringBuilder app) {
		app.append("POINT( ");
		app.append(lon);
		app.append(" ");
		app.append(lat);
		app.append(" )");
	}
}
