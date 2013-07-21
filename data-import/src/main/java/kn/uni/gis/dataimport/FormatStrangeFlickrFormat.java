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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

public class FormatStrangeFlickrFormat {

	private static final String INPUT = "src/main/resources/flickr_berlin.org";
	private static final String OUTPUT = "src/main/resources/flickr_berlin.csv";

	public static void main(String[] args) throws IOException {
		Iterable<String> readLines = filterNulls(concatLines(Files.readLines(
				new File(INPUT), Charsets.UTF_8)));
		// BufferedReader reader = Files
		// .newReader(new File(INPUT), Charsets.UTF_8);
		// 1,20,12
		Files.write(
				Joiner.on("\n")
						.skipNulls()
						.join(Iterables.transform(readLines,
								new Function<String, String>() {

									@Override
									public String apply(String input) {
										// System.out.println(input);
										String[] split = input.split(";");

										if (equalss(split[0], "524", "567",
												"2284", "2720")) {
											return null;
										}

										assertNumbers(split);

										String asdf = Joiner.on("\t").join(
												split[0], split[19], split[20],
												"Z", "M", split[3], "");

										System.out.println(asdf);
										return asdf;
									}

									private void assertNumbers(String[] split) {
										if (!!!split[0].equals("Field1")) {
											Preconditions.checkArgument(
													Double.valueOf(split[19]
															.replace(',', '.')) > 13,
													split[19]
															+ Arrays.toString(split));
											Preconditions.checkArgument(
													Double.valueOf(split[20]
															.replace(',', '.')) > 52,
													split[20]
															+ Arrays.toString(split));
										}
									}
								})).replaceAll(",", "."), new File(OUTPUT),
				Charsets.UTF_8);
	}

	private static boolean equalss(String string, String... string2) {
		for (String a : string2) {
			if (a.equals(string)) {
				return true;
			}
		}
		return false;
	}

	private static Iterable<String> filterNulls(Iterable<String> concatLines) {
		return Iterables.filter(concatLines, new Predicate<String>() {

			@Override
			public boolean apply(String input) {
				return !!!input.startsWith("0");
			}

		});
	}

	private static List<String> concatLines(List<String> readLines) {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		int countOfSep = 0;
		StringBuilder builder = new StringBuilder();

		for (String line : readLines) {
			line = replcaseXml(line);
			countOfSep += countOccurrences(line, ';');
			builder.append(line);
			if (countOfSep > 30) {
				countOfSep = 0;
				list.add(builder.toString());
				builder.delete(0, builder.length());
			}
		}
		return list.build();
	}

	private static String replcaseXml(String string) {
		return string.replaceAll("&quot;", "").replaceAll("&amp;", "")
				.replaceAll("&lt;", "").replaceAll("&gt;", "")
				.replaceAll(";-\\)", "").replaceAll("&laquo;", "")
				.replaceAll(";\\)", "").replaceAll("jackets;", "")
				.replaceAll("Hauptbahnhof in Berlin;", "");
	}

	public static int countOccurrences(String haystack, char needle) {
		int count = 0;
		for (int i = 0; i < haystack.length(); i++) {
			if (haystack.charAt(i) == needle) {
				count++;
			}
		}
		return count;
	}
}
