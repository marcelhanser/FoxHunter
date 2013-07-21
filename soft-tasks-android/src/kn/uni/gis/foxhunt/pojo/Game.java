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

import java.net.URL;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class Game {
	@Element
	private String id;
	@Element(required = false)
	private String playerId;
	@Element
	private String playerName;
	@Element
	private URL pushUrl;
	@Element
	private URL gameUrl;
	@Element
	private Type type;

	public Game() {
	};

	public Game(String id, String playerId, String playerName, URL pushUrl,
			URL gameUrl) {
		super();
		this.id = id;
		this.playerId = playerId;
		this.playerName = playerName;
		this.pushUrl = pushUrl;
		this.gameUrl = gameUrl;
		type = Type.HUNTER;
	}

	public Game(String id, String playerName, URL pushUrl, URL gameUrl) {
		super();
		this.id = id;
		this.gameUrl = gameUrl;
		this.playerId = null;
		this.playerName = playerName;
		this.pushUrl = pushUrl;
		type = Type.FOX;
	}

	public Type getType() {
		return type;
	}

	public String getPlayerId() {
		return playerId;
	}

	public String getId() {
		return id;
	}

	public String getPlayerName() {
		return playerName;
	}

	public URL getPushUrl() {
		return pushUrl;
	}

	public URL getGameUrl() {
		return gameUrl;
	}

	public enum Type {
		HUNTER, FOX
	}
}