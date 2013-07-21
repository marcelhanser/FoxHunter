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

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

import kn.uni.gis.softtasks.GisResource;

import com.google.common.collect.Maps;

public class Game implements Closeable {

	private final Map<Player, TimingIterator> playerIterators = Maps
			.newHashMap();

	private final Map<String, Player> playerIds = Maps.newHashMap();

	private PreparedStatement iteratorStatement;

	private String id;
	private Player fox;

	private long first = Long.MAX_VALUE;
	private long last = Long.MIN_VALUE;

	public Game(String id) {
		super();
		this.id = id;
	}

	public Game addPlayer(String playerId, String playerName, Timestamp min,
			Timestamp max, TimingIterator playerStates) {
		Player player = playerIds.get(playerId);

		player = player == null ? new Player(playerId, playerName) : player;

		playerIds.put(playerId, player);

		if (player.id.equals(GisResource.FOX_GAMER_ID)) {
			fox = player;
		}

		playerIterators.put(player, playerStates);
		first = Math.min(first, min.getTime());
		last = Math.max(last, max.getTime());
		return this;
	}

	public Game finish(PreparedStatement statement) {
		iteratorStatement = statement;
		return this;
	}

	public Map<Player, TimingIterator> getStates() {
		return Collections.unmodifiableMap(playerIterators);
	}

	public Map<Player, TimingIterator> getStatesTiming() {
		return playerIterators;
	}

	public Player getPlayer(String playerId) {
		return playerIds.get(playerId);
	}

	public static class Player {
		private final String id;
		private final String name;

		private Player(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Player other = (Player) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Game other = (Game) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public Player getFox() {
		return fox;
	}

	public String getId() {
		return id;
	}

	public long getFirst() {
		return first;
	}

	public long getLast() {
		return last;
	}

	@Override
	public void close() throws IOException {
		try {
			Connection connection = iteratorStatement.getConnection();
			iteratorStatement.close();
			connection.close();
		} catch (Exception e) {
		}
	}
}
