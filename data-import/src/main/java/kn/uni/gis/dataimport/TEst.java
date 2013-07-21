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

import java.util.Arrays;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class TEst {
	public static void main(String[] args) {
		String asdf = "23	 13.04706760	 52.43844910	0	0	water		";
		String nect = "8	 13.02856360	 52.39731200	0	0	water	Maschinenteich	";
		System.out.println(asdf);
		System.out.println(Arrays.toString(asdf.split("\t")));
		System.out.println(Splitter.on("\t").split(nect));
		System.out.println(Joiner.on(";").join(
				new String[] { "", "asdf", "", "" }));

		Splitter.on("\t").split(nect).iterator().remove();

	}
}
