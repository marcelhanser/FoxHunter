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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CPolygon implements Geo {
	private final List<CPoint> points;
	private static final Joiner JOINER = Joiner.on(",");

	public CPolygon(CPoint... points) {
		super();
		this.points = ImmutableList.copyOf(points);
	}

	public CPolygon(Iterable<CPoint> points) {
		super();
		this.points = ImmutableList.copyOf(points);
	}

	public List<CPoint> getPoints() {
		return points;
	}

	public String toString() {
		return Joiner.on(",").join(points);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("POLYGON((");
		JOINER.appendTo(builder, points);
		builder.append("))");
	}
}
