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
package kn.uni.gis.foxhunt.context;

import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.Matcher;
import org.simpleframework.xml.transform.Transform;

public final class XmlUtil {

	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);

	private static final Matcher DATE_MATCH = new Matcher() {

		@SuppressWarnings("rawtypes")
		@Override
		public Transform match(Class arg0) throws Exception {
			if (Date.class.isAssignableFrom(arg0)) {
				return new Transform<Date>() {

					@Override
					public Date read(String arg0) throws Exception {
						return simpleDateFormat.parse(arg0.subSequence(0,
								arg0.length() - 3)
								+ "00");
					}

					@Override
					public String write(Date arg0) throws Exception {
						String format = simpleDateFormat.format(arg0);
						return format.substring(0, format.length() - 2) + ":00";
					}
				};
			}
			return null;
		}
	};

	private static final Persister PERSISTER = new Persister(DATE_MATCH);

	public static String marshall(Object obj) {
		StringWriter stringWriter = new StringWriter();
		try {
			PERSISTER.write(obj, stringWriter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return stringWriter.toString();
	}

	public static <T> T unmarshall(String obj, Class<T> clazz) {
		try {
			return PERSISTER.read(clazz, obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T unmarshall(InputStream obj, Class<T> clazz) {
		try {
			return PERSISTER.read(clazz, obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
