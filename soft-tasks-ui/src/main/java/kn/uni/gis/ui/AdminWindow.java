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

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import kn.uni.gis.dataimport.util.GeoFactory;
import kn.uni.gis.dataimport.util.GeoUtil;
import kn.uni.gis.softtasks.GisResource;

import com.google.common.collect.Lists;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class AdminWindow extends Window {

	private static final long serialVersionUID = 1L;
	private final GeoUtil geoUtil;
	private final String password;
	private Table table;
	private ItemClickListener listener;

	public AdminWindow(String password, GeoUtil geoUtil,
			ItemClickListener listener) {
		super("All seeing Hunter");
		this.password = password;
		this.geoUtil = geoUtil;
		this.listener = listener;

		addComponent(loginComponent());

	};

	private Component loginComponent() {
		final VerticalLayout layout = new VerticalLayout();

		final PasswordField passwordField = new PasswordField("Old Hunters Age");
		passwordField.setWidth("-1px");
		passwordField.setHeight("-1px");
		passwordField.focus();

		Button button_2 = new Button();
		button_2.setCaption("Login");
		button_2.setImmediate(false);
		button_2.setWidth("-1px");
		button_2.setHeight("-1px");
		button_2.addListener(new ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				if (password.equals(passwordField.getValue())) {
					AdminWindow.this.removeComponent(layout);
					AdminWindow.this.addComponent(createTable());
				} else {
					AdminWindow.this.showNotification("Wrong age!");
				}
			}
		});

		layout.addComponent(passwordField);
		layout.addComponent(button_2);

		layout.setComponentAlignment(passwordField, Alignment.TOP_CENTER);
		layout.setComponentAlignment(button_2, Alignment.BOTTOM_CENTER);

		return layout;
	}

	private Component createTable() {
		VerticalLayout layout = new VerticalLayout();
		this.addComponent(layout);

		table = new Table("All huntings");

		// Define two columns for the built-in container
		table.addContainerProperty("Id", String.class, null);
		table.addContainerProperty("Fox Name", String.class, null);
		table.addContainerProperty("Date", Date.class, null);

		// Add a few other rows using shorthand addItem()

		refreshTable();

		// Show 5 rows
		table.setPageLength(30);
		layout.addComponent(table);
		return layout;
	}

	private void refreshTable() {
		String sql = String
				.format("select id,max(player_name),max(timestamp) from %s where player_id='%s' group by id",
						GisResource.FOX_HUNTER, GisResource.FOX_GAMER_ID);

		final List<String> names = Lists.newArrayList();
		final List<Date> dates = Lists.newArrayList();
		List<String> ids = geoUtil.receiveGeos(sql, new GeoFactory<String>() {

			@Override
			public String createGeo(ResultSet executeQuery) throws SQLException {

				names.add(executeQuery.getString(2));
				dates.add(executeQuery.getDate(3));
				return executeQuery.getString(1);
			}
		});

		table.removeAllItems();
		table.addListener(listener);

		for (int i = 0; i < ids.size(); i++) {
			table.addItem(
					new Object[] { ids.get(i), names.get(i), dates.get(i) },
					ids.get(i));
		}

	}
}
