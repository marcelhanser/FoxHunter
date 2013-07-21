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
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import kn.uni.gis.dataimport.SQLFacade.DoWithin;
import kn.uni.gis.dataimport.util.CPoint;
import kn.uni.gis.dataimport.util.GeoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.PeekingIterator;

/**
 * NOT thread safe!!! because only running in exactly one instance of a
 * {@link GameVectorLayer}
 * 
 * @author NanoHome
 * 
 */
public class TimingIterator implements PeekingIterator<PlayerState> {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TimingIterator.class);
	private final GeoUtil util;
	private final String playerId;
	private final String gameId;

	private final PreparedStatement statement;

	private Deque<PlayerState> queue = new LinkedList<PlayerState>();

	private long currentTime;

	private int lastTried = 0;
	private int currentTried = 0;

	// corresponds to 5 seconds
	private static final int MAX_TRIED = 10;

	public TimingIterator(GeoUtil util, String gameId, String playerId,
			long time, PreparedStatement statement) {
		super();
		this.util = util;
		this.gameId = gameId;
		this.playerId = playerId;
		// to receive also the first point ;)
		this.currentTime = time - 100;
		this.statement = statement;
	}

	private boolean shouldTry() {
		return currentTried++ >= lastTried;
	}

	private void receiveNew() {
		try {
			statement.setString(1, gameId + "%");
			statement.setString(2, playerId);
			statement.setTimestamp(3, new Timestamp(currentTime));
			util.getConn().executeQuery(statement, doWithin);

			if (lastTried <= MAX_TRIED)
				lastTried++;

			currentTried = 0;

			LOGGER.info("received new points: {},{}", playerId, queue);

			// are there some results?
			if (queue.size() > 0) {
				currentTime = queue.getLast().getTimestamp();
				lastTried = 0;
			}
		} catch (SQLException e) {
			LOGGER.error("error on receiving next timeslots", e);
		}
	}

	private final DoWithin doWithin = new DoWithin() {
		@Override
		public void doIt(ResultSet executeQuery) throws SQLException {
			while (executeQuery.next()) {
				CPoint pointAt = GeoUtil.pointAt(executeQuery, 1);
				queue.offer(new PlayerState(pointAt, executeQuery.getTimestamp(
						2).getTime()));
			}
		}
	};

	public void setTime(long time) {
		currentTime = time;
		queue.clear();
		lastTried = 0;
	}

	@Override
	public boolean hasNext() {
		if (queue.isEmpty()) {
			if (shouldTry()) {
				receiveNew();
			}
		}
		return !!!queue.isEmpty();
	}

	@Override
	public PlayerState peek() {
		if (hasNext()) {
			return queue.peek();
		}
		throw new NoSuchElementException();
	}

	@Override
	public PlayerState next() {
		if (hasNext()) {
			return queue.poll();
		}
		throw new NoSuchElementException();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
