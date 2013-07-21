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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import kn.uni.gis.dataimport.SQLFacade.DoWithin;
import kn.uni.gis.dataimport.util.CPoint;
import kn.uni.gis.dataimport.util.GeoUtil;
import kn.uni.gis.hardtasks.HardTasks;
import kn.uni.gis.softtasks.GisResource;
import kn.uni.gis.ui.Game.Player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.vol.Bounds;
import org.vaadin.vol.OpenLayersMap;
import org.vaadin.vol.OpenStreetMapLayer;
import org.vaadin.vol.Point;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.vaadin.Application;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.Window.Notification;

/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class GameApplication extends Application {

	static final int FPS = 2;

	private Window window;
	private Window adminWindow;

	private TextField textField_1;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(GameApplication.class);

	private final GeoUtil geoUtil;
	private final String password;

	private final Map<Game, GameComposite> gameMap = Maps.newHashMap();

	private OpenLayersMap map;
	private TabSheet tabsheet;

	private EventBus eventBus;

	private ProgressIndicator pi = new ProgressIndicator();

	private static final int MAX_STATES_IN_MEM = 15;

	public GameApplication(String password, GeoUtil geoUtil2) {
		eventBus = new EventBus();

		geoUtil = geoUtil2;
		this.password = password;
	}

	@Override
	public void init() {
		window = new Window("Fox and Hunter - Der Kohl sei mit euch!");
		setMainWindow(window);
		VerticalLayout verticalLayout = new VerticalLayout();

		map = getMap();

		window.setContent(verticalLayout);

		verticalLayout.setSizeFull();
		verticalLayout.addComponent(map);
		verticalLayout.setExpandRatio(map, 1);

		window.addWindow(createGameWindow());
		window.addWindow(createDownloadWindow());

		pi.setIndeterminate(true);
		pi.setPollingInterval(1000 / FPS);
		pi.setVisible(false);

	}

	private Window createDownloadWindow() {
		final DownloadWindow downloadWindow = new DownloadWindow();
		final int margin = 20;
		//
		downloadWindow.setPositionX((int) (window.getWidth()
				- downloadWindow.getWidth() - margin));
		downloadWindow.setPositionY(margin);

		downloadWindow.setWidth("150px");
		downloadWindow.setHeight("205px");
		downloadWindow.setDraggable(false);
		downloadWindow.setResizable(false);
		return downloadWindow;
	}

	private Window createGameWindow() {

		tabsheet = new TabSheet();
		tabsheet.setImmediate(true);
		tabsheet.setCloseHandler(new CloseHandler() {
			@Override
			public void onTabClose(TabSheet tabsheet, Component tabContent) {

				Game game = ((GameComposite) tabContent).getGame();

				GameComposite remove = gameMap.remove(game);

				// closes the game and the running thread!
				remove.getLayer().handleApplicationClosedEvent(
						new ApplicationClosedEvent());

				eventBus.unregister(remove);
				eventBus.unregister(remove.getLayer());

				map.removeLayer(remove.getLayer());

				tabsheet.removeComponent(tabContent);

				if (gameMap.isEmpty()) {
					pi.setVisible(false);
				}
			}
		});

		final Window mywindow = new Window("Games");
		mywindow.setPositionX(0);
		mywindow.setPositionY(0);
		mywindow.setHeight("50%");
		mywindow.setWidth("25%");
		VerticalLayout layout = new VerticalLayout();
		HorizontalLayout lay = new HorizontalLayout();

		final Button button_1 = new Button();
		button_1.setCaption("Open Game");
		button_1.setWidth("-1px");
		button_1.setHeight("-1px");

		button_1.addListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				final String id = textField_1.getValue().toString();

				if (id.length() < 5) {
					window.showNotification(
							"id must have at least 5 characters",
							Notification.TYPE_ERROR_MESSAGE);
				} else {
					String sql = String
							.format("select player_id,player_name,min(timestamp),max(timestamp) from %s"
									+ " where id LIKE ? group by player_id, player_name",
									GisResource.FOX_HUNTER);

					final Game game = new Game(id);
					final PreparedStatement statement = geoUtil
							.getConn()
							.prepareStatement(
									"select poly_geom,timestamp from "
											+ GisResource.FOX_HUNTER
											+ " where id LIKE ? and player_id=? and timestamp > ? order by timestamp LIMIT "
											+ MAX_STATES_IN_MEM);

					try {
						geoUtil.getConn().executeSafeQuery(sql, new DoWithin() {

							@Override
							public void doIt(ResultSet executeQuery)
									throws SQLException {

								while (executeQuery.next()) {
									if (statement == null) {

									}
									String playerId = executeQuery.getString(1);
									Timestamp min = executeQuery
											.getTimestamp(3);
									Timestamp max = executeQuery
											.getTimestamp(4);
									game.addPlayer(playerId, executeQuery
											.getString(2), min, max,
											new TimingIterator(geoUtil, id,
													playerId, min.getTime(),
													statement));
								}
							}
						}, id + "%");
					} catch (SQLException e) {
						LOGGER.info("error on sql!", e);

					}

					game.finish(statement);

					if (!!!gameMap.containsKey(game)) {
						if (game.getStates().size() == 0) {
							window.showNotification("game not found!");
						} else {
							LOGGER.info("received game info: {},{} ",
									game.getId(), game.getStates().size());

							GameVectorLayer gameVectorLayer = new GameVectorLayer(
									GameApplication.this, eventBus, game,
									createColorMap(game));

							final GameComposite gameComposite = new GameComposite(
									GameApplication.this, game,
									gameVectorLayer, eventBus);

							eventBus.register(gameComposite);
							eventBus.register(gameVectorLayer);

							map.addLayer(gameVectorLayer);
							gameMap.put(game, gameComposite);

							// Add the component to the tab sheet as a new tab.
							Tab addTab = tabsheet.addTab(gameComposite);
							addTab.setCaption(game.getId().substring(0, 5));
							addTab.setClosable(true);

							pi.setVisible(true);
							// pl.get
							PlayerState playerState = game.getStates()
									.get(game.getFox()).peek();
							map.zoomToExtent(new Bounds(CPOINT_TO_POINT
									.apply(playerState.getPoint())));
						}
					}
				}
			}

			private Map<Player, Integer> createColorMap(Game game) {
				Function<Double, Double> scale = HardTasks.scale(0, game
						.getStates().size());

				ImmutableMap.Builder<Player, Integer> builder = ImmutableMap
						.builder();

				int i = 0;

				for (Player play : game.getStates().keySet()) {
					builder.put(play, getColor(scale.apply((double) i++)));
				}

				return builder.build();
			}

			private Integer getColor(double dob) {

				int toReturn = 0;
				toReturn = toReturn | 255 - (int) Math.round(255 * dob);
				toReturn = toReturn | (int) ((Math.round(255 * dob)) << 16);
				return toReturn;
				// return (int) (10000 + 35000 * dob + 5000 * dob + 1000 * dob +
				// 5 * dob);
			}

		});

		Button button_2 = new Button();
		button_2.setCaption("All seeing Hunter");
		button_2.setImmediate(false);
		button_2.setWidth("-1px");
		button_2.setHeight("-1px");
		button_2.addListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				if (adminWindow == null) {
					adminWindow = new AdminWindow(password, geoUtil,
							new ItemClickListener() {
								@Override
								public void itemClick(ItemClickEvent event) {

									textField_1.setValue(event.getItemId()
											.toString());
									mywindow.bringToFront();
									button_1.focus();
								}
							});
					window.addWindow(adminWindow);
					adminWindow.setWidth("30%");
					adminWindow.setHeight("40%");
					adminWindow.addListener(new CloseListener() {
						@Override
						public void windowClose(CloseEvent e) {
							adminWindow = null;
						}
					});
				}
			}
		});

		lay.addComponent(button_1);
		textField_1 = new TextField();
		textField_1.setImmediate(false);
		textField_1.setWidth("-1px");
		textField_1.setHeight("-1px");
		lay.addComponent(textField_1);
		lay.addComponent(button_2);
		lay.addComponent(pi);

		lay.setComponentAlignment(pi, Alignment.TOP_RIGHT);

		layout.addComponent(lay);
		layout.addComponent(tabsheet);

		mywindow.addComponent(layout);
		mywindow.setClosable(false);

		/* Add the window inside the main window. */
		return mywindow;
	}

	@Override
	public void close() {
		eventBus.post(new ApplicationClosedEvent());
		super.close();
	}

	private OpenLayersMap getMap() {
		final OpenLayersMap map = new OpenLayersMap();
		map.setImmediate(true);
		/*
		 * Open street maps layer as a base layer. Note importance of the order,
		 * OSM layer now sets the projection to Spherical Mercator. If added eg.
		 * after markers or vectors, they might render with bad values.
		 */
		OpenStreetMapLayer osm = new OpenStreetMapLayer();

		map.setSizeFull();
		map.addLayer(osm);

		return map;
	}

	public static final Function<CPoint, Point> CPOINT_TO_POINT = new Function<CPoint, Point>() {

		@Override
		public Point apply(CPoint input) {
			return new Point(input.getLon(), input.getLat());
		}
	};
}
