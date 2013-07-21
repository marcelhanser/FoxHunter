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
package kn.uni.gis.ui;

import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Window;

import fi.jasoft.qrcode.QRCode;

public class DownloadWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DownloadWindow() {
		super("Download App");

	};

	@Override
	public void attach() {
		addComponent(downloadComponent());
	}

	private Component downloadComponent() {
		GridLayout layout = new GridLayout(2, 2);

		String baseUrl = getApplication().getURL().toString();
		StringBuilder b = new StringBuilder(getApplication().getURL()
				.toString());
		b.replace(baseUrl.lastIndexOf("ui"), baseUrl.lastIndexOf("ui") + 2,
				"game");

		QRCode code = new QRCode("", b.toString());
		layout.addComponent(code);

		return layout;
	}
}
