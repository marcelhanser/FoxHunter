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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import kn.uni.gis.ui.Game.Player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.vol.Point;
import org.vaadin.vol.PolyLine;
import org.vaadin.vol.Style;
import org.vaadin.vol.StyleMap;
import org.vaadin.vol.VectorLayer;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.Application;

public class GameVectorLayer extends VectorLayer {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(GameVectorLayer.class);

	private static final Point NULL_POINT = new Point(12, 12);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Game game;
	private PaintThread thread;
	private Map<Player, Tupel> players = Maps.newConcurrentMap();

	private Tupel currentSelected;

	private final EventBus eventBus;

	private final static Style SELECTED_STYLE = createStyle(0xD2691E, 7);

	private static final int MAX_POINTS = 40;

	private final ScheduledExecutorService threadExecutor = Executors
			.newSingleThreadScheduledExecutor();

	public GameVectorLayer(GameApplication application, EventBus eventBus,
			Game game, final Map<Player, Integer> playerColorMap) {
		super();
		this.eventBus = eventBus;
		setCaption(game.getId().substring(0, 5));
		this.game = game;

		setSelectionMode(SelectionMode.SIMPLE);

		for (Player pl : game.getStates().keySet()) {
			Integer color = playerColorMap.get(pl);

			LOGGER.info("GETTING PLAYER!: " + pl.getName());

			PolyLine polyLine = new PolyLine();

			TimingIterator timingIterator = game.getStates().get(pl);

			Style createStyle = createStyle(color);

			players.put(pl, new Tupel(application, polyLine, timingIterator,
					createStyle));

			polyLine.setCustomStyle(createStyle);
			addComponent(polyLine);
		}
		addDefaultStyle();

		thread = new PaintThread();
		schedule(thread);
	}

	@Override
	public void attach() {
		super.attach();
	}

	@Override
	public void detach() {
		super.detach();
		threadExecutor.shutdown();
	}

	public void setTimeMultiplayer(double multiplyer) {
		LOGGER.info("setting multiplyer to: {} ", multiplyer);
		thread.timeMultiplyer = multiplyer;
	}

	@Subscribe
	public void handlePlaySelected(PlayerSelectionEvent event) {
		if (game.getId().equals(event.getGameId())) {
			synchronized (getApplication()) {
				LOGGER.info("SELECTED");

				if (currentSelected != null) {
					currentSelected.line.setCustomStyle(currentSelected.style);
				}

				currentSelected = players.get(event.getSelected());
				currentSelected.line.setCustomStyle(SELECTED_STYLE);
			}
		}
	}

	@Subscribe
	public void handleTimeSetted(TimeChangedEvent event) {
		if (game.getId().equals(event.getGameId())) {
			LOGGER.debug("setting new time: " + event.getTimestamp());
			thread.newTimeToSet.set(event.getTimestamp());
			if (!!!thread.running) {
				schedule(thread);
			}
		}
	}

	@Subscribe
	public void handleApplicationClosedEvent(ApplicationClosedEvent event) {
		threadExecutor.shutdown();
		try {
			LOGGER.info("closing game: {}", game.getId());
			game.close();
		} catch (IOException e) {
		}
	}

	private class PaintThread implements Runnable {
		private volatile double timeMultiplyer;
		private volatile boolean running;
		private long time;
		private long systime;
		private final long startTime;
		private AtomicReference<Long> newTimeToSet = new AtomicReference<Long>();

		public PaintThread() {

			systime = System.currentTimeMillis();
			startTime = game.getFirst();
			time = startTime;

			eventBus.post(new TimeEvent(game.getId(), time));

			timeMultiplyer = 1.3;
		}

		public void run() {
			running = true;

			boolean somethingChanged = true;

			long timePassed = System.currentTimeMillis() - systime;
			Long newtime = newTimeToSet.getAndSet(null);
			if (newtime != null) {
				LOGGER.info("setting time to new Event!{}", new Timestamp(
						newtime));
				for (Map.Entry<Player, Tupel> pl : players.entrySet()) {
					pl.getValue().stateIterator.setTime(newtime);
					pl.getValue().initStates();
					pl.getValue().initPolyLine();
					if (pl.getValue().current == null) {
						LOGGER.info(pl.getKey().getName() + ":WARNUNG");

					}
				}
				time = newtime;
			}

			systime += timePassed;

			if (timeMultiplyer > 0) {
				time += (long) (timeMultiplyer * timePassed);
				eventBus.post(new TimeEvent(game.getId(), time));

				somethingChanged = false;

				for (Tupel pl : players.values()) {
					if (pl.current != null && pl.next != null) {

						somethingChanged = true;

						long lastTime = pl.current.getTimestamp();

						long nextTime = pl.next.getTimestamp();

						long currentTime = time - lastTime;

						// LOGGER.info("l {}, c {}, n {} time!, index {}",
						// lastTime, currentTime, nextTime,
						// currentIndex);

						double quot = currentTime
								/ (double) (nextTime - lastTime);

						if (quot > 0) {
							quot = quot >= 1 ? 1 : quot;

							double latDif = pl.next.getPoint().getLat()
									- pl.current.getPoint().getLat();
							double lonDif = pl.next.getPoint().getLon()
									- pl.current.getPoint().getLon();

							Point p = new Point(pl.current.getPoint().getLon()
									+ quot * lonDif, pl.current.getPoint()
									.getLat() + quot * latDif);

							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("ADDING point {} ", p);
							}

							addPointToLine(pl.line, p);
						}
						if (quot >= 1) {
							pl.current = pl.next;
							pl.next = null;
							while (pl.stateIterator.hasNext()) {

								pl.next = pl.stateIterator.next();

								if (pl.next.getTimestamp() > (time))
									break;
							}
						}
					}
				}
			}
			if (somethingChanged) {
				schedule(this);
			} else {
				running = false;
				LOGGER.info("stopping thread for game {}", game.getId());
			}
		}

		private void addPointToLine(PolyLine line, Point p) {
			synchronized (getApplication()) {
				Point[] points = line.getPoints();
				if (points[0] == NULL_POINT) {
					Arrays.fill(points, p);
				} else {
					System.arraycopy(points, 0, points, 1, points.length - 1);
				}
				points[0] = p;
				line.requestRepaint();
			}
		}
	}

	public void setPlayerVisible(Player pl, boolean val) {
		PolyLine line = players.get(pl).line;
		if (val) {
			addComponent(line);
		} else {
			removeComponent(line);
		}
	}

	private void schedule(PaintThread thread) {
		threadExecutor.schedule(thread, 1000 / GameApplication.FPS,
				TimeUnit.MILLISECONDS);
	}

	private static class Tupel {
		private final PolyLine line;
		private final TimingIterator stateIterator;
		private final Style style;
		private PlayerState current;
		private PlayerState next;
		private Application app;

		public Tupel(Application app, PolyLine polyLine,
				TimingIterator iterator, Style color) {
			this.app = app;
			line = polyLine;
			this.style = color;
			this.stateIterator = iterator;
			initStates();
			initPolyLine();
		}

		public void initStates() {
			current = Iterators.getNext(stateIterator, null);
			next = Iterators.getNext(stateIterator, null);
		}

		public void initPolyLine() {
			synchronized (app) {
				Point[] points = new Point[MAX_POINTS];

				Point toFill = current == null ? NULL_POINT : new Point(current
						.getPoint().getLon(), current.getPoint().getLat());
				Arrays.fill(points, 0, points.length, toFill);

				line.setPoints(points);
			}
		}
	}

	private static Style createStyle(int color, int stroke) {
		Style vaadinColors = new Style();

		String hexString = Integer.toHexString(color);

		String prefix = "#" + Strings.repeat("0", 6 - hexString.length());

		vaadinColors.setStrokeColor(prefix + hexString);
		vaadinColors.setFillColor(prefix + hexString);
		vaadinColors.setFillOpacity(0.4);
		vaadinColors.setStrokeWidth(stroke);
		return vaadinColors;
	}

	private static Style createStyle(int color) {
		return createStyle(color, 3);
	}

	private void addDefaultStyle() {
		Style defaultstyle = new Style();
		/* Set stroke color to green, otherwise like default style */
		defaultstyle.extendCoreStyle("default");
		defaultstyle.setStrokeColor("#00b963");
		defaultstyle.setStrokeWidth(4);

		// Make borders of selected graphs bigger
		Style selectStyle = new Style();
		selectStyle.setStrokeWidth(30);

		StyleMap stylemap = new StyleMap(defaultstyle, selectStyle, null);
		// make selectStyle inherit attributes not explicitly set
		stylemap.setExtendDefault(true);
		setStyleMap(stylemap);
	}

}
