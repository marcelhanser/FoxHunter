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
package kn.uni.gis.foxhunt.pojo;

import java.util.Date;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root
@Namespace(reference = "http://soft.gis/v1")
public class Azimuth {

	@Attribute(required = true)
	private float value;

	@Attribute(required = false)
	private Date timestamp;

	public Azimuth() {
	}

	public float getValue() {
		return value;
	}

	public Date getTimestamp() {
		return timestamp;
	}
}
