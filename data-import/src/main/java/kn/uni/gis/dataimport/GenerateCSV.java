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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

public class GenerateCSV {
	private static final String PATH = "./";
	static final String OUTPUT = "src/main/resources/";

	public static void main(String[] args) throws IOException,
			InterruptedException {
		for (File file : new File(PATH).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".shp");
			}
		})) {
			ProcessBuilder builder = new ProcessBuilder("lib/shp2text",
					"--spreadsheet", file.getAbsolutePath());

			System.out.println("executing: " + builder.command());

			final Process start = builder.start();

			new StreamConsumer(new InputSupplier<Reader>() {
				public Reader getInput() throws IOException {
					return new InputStreamReader(start.getInputStream());
				}
			}, Files.newWriterSupplier(new File(OUTPUT + file.getName()
					+ ".csv"), Charsets.UTF_8)).start();
			new StreamConsumer(new InputSupplier<Reader>() {
				public Reader getInput() throws IOException {
					return new InputStreamReader(start.getErrorStream());
				}
			}, Files.newWriterSupplier(new File(OUTPUT + file.getName()
					+ ".csv"), Charsets.UTF_8)).start();
			Preconditions.checkState(start.waitFor() == 0, "error ");
		}
	}

	private static class StreamConsumer extends Thread {

		private InputSupplier<Reader> is;

		private OutputSupplier<OutputStreamWriter> out;

		private StreamConsumer(InputSupplier<Reader> inputSupplier,
				OutputSupplier<OutputStreamWriter> outputSupplier) {
			this.is = inputSupplier;
			this.out = outputSupplier;
		}

		public void run() {
			try {
				CharStreams.copy(is, out);

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
