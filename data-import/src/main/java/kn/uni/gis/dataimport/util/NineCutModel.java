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

import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public enum NineCutModel {

	DISJOINT("FF.FF...."), MEETS("FT.......", "F..T.....", "F...T...."), OVERLAPS(
			"T.T...T..", "T.T...T.."), COVERS("T.....FF.", ".T....FF.",
			"...T..FF.", "....T.FF."), CONTAINS("T.....FF."), COVERED_BY(
			"T.F..F...", ".TF..F...", "..FT.F...", "..F.TF..."), INSIDE(
			"T.F..F..."), EQUALS("T.F..FFF.");

	private NineCutModel(String... pattern) {
		checkLenghts(pattern);
		this.pattern = Pattern.compile(Joiner.on('|').join(pattern));
	}

	private void checkLenghts(String[] pattern2) {
		for (String a : pattern2) {
			Preconditions.checkArgument(a.length() == 9,
					"pattern: %s has a bad length of %d", pattern2, a.length());
		}
	}

	private Pattern pattern;

	public static NineCutModel forDE9IM(String de9im, boolean b) {
		de9im = de9im.replace('2', 'T');
		de9im = de9im.replace('1', 'T');
		NineCutModel toReturn = null;
		for (NineCutModel model : values()) {
			if (model.pattern.matcher(de9im).matches()) {
				toReturn = model;
			}
		}

		if (toReturn == null)
			toReturn = MEETS;
		Preconditions.checkArgument(toReturn != null,
				"no model found for matrix: %s", de9im);

		if (toReturn == CONTAINS && b) {
			toReturn = COVERS;
		}
		if (toReturn == INSIDE && b) {
			toReturn = COVERED_BY;
		}
		return toReturn;
	}
}
