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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import kn.uni.gis.foxhunt.context.HttpContext.EntityHandlerAdapter;
import kn.uni.gis.foxhunt.pojo.Game;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

public final class GameContext {

	private static Game currentGame;

	public static boolean isGameRunning() {
		return currentGame != null;
	}

	public static Game getGame() {
		return currentGame;
	}

	/**
	 * creates a new hunter game
	 * 
	 * @param id
	 * @param playerId
	 * @param playerName
	 */
	public static void newHunterGame(String id, String playerId,
			String playerName) throws GameException {
		currentGame = new Game(id, playerId, playerName, Util.createHunterUrl(
				id, playerId), Util.createGameUrl(id));
		if (!!!isGameActive()) {
			throw new GameException("Game is not active");
		}
	}

	/**
	 * creates a new fox game
	 * 
	 * @param id
	 * @param playerName
	 */
	public static void newFoxGame(String playerName) throws GameException {

		final AtomicReference<String> ref = new AtomicReference<String>();
		final AtomicReference<Exception> exc = new AtomicReference<Exception>();
		HttpContext.getInstance().put(
				SettingsContext.getInstance().getServerUrl(), null,
				new EntityHandlerAdapter() {
					@Override
					public void handleEntity(HttpEntity entity, int statusCode) {
						if (statusCode == HttpStatus.SC_OK) {
							try {
								ref.set(EntityUtils.toString(entity));

							} catch (ParseException e) {
								exc.set(e);
							} catch (IOException e) {
								exc.set(e);
							}
						} else {
							exc.set(new GameException("bad status code: "
									+ statusCode));
						}
					}

					@Override
					public void handleException(Exception exception) {
						exc.set(exception);
					}
				});

		if (ref.get() == null) {
			throw exc.get() != null ? new GameException(exc.get())
					: new GameException("unrecognized error code from server");
		}
		currentGame = new Game(ref.get(), playerName, Util.createFoxUrl(ref
				.get()), Util.createGameUrl(ref.get()));
	}

	public static void setGame(Game game) {
		currentGame = game;
	}

	public static void clearGame() {
		currentGame = null;
	}

	public static boolean isGameActive() {
		final AtomicBoolean bool = new AtomicBoolean(false);
		HttpContext.getInstance().get(currentGame.getGameUrl().toString(),
				new EntityHandlerAdapter() {
					@Override
					public void handleEntity(HttpEntity entity, int statusCode) {
						bool.set(statusCode == HttpStatus.SC_OK);
					}
				});

		return bool.get();
	}

}
