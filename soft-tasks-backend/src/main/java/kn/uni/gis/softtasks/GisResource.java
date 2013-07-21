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
package kn.uni.gis.softtasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import kn.uni.gis.dataimport.util.GeoFactory;
import kn.uni.gis.dataimport.util.GeoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterators;

@Path("/")
public class GisResource {

	public static final String FOX_HUNTER = "fox_hunter";

	private static final Logger LOGGER = LoggerFactory
			.getLogger(GisResource.class);
	private GeoUtil geoRec;

	private Cache<String, String> cachedGames = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.HOURS).build();

	public static final String FOX_GAMER_ID = "0815_FOX_ASDFISNOTUSED_FROM_OTHER_GUYS_OR_I_WILL_PERSONALLY_KILL_THEM";

	private static final File APP_DIR = new File("lib");

	private final DatatypeFactory dataFactory;

	public GisResource(GeoUtil geoRec) {
		super();
		try {
			dataFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw Throwables.propagate(e);
		}
		this.geoRec = geoRec;
	}

	@PUT
	@Path("/game")
	@Produces(MediaType.APPLICATION_XML)
	public Response createNewGame() {
		cachedGames.cleanUp();
		return Response.ok(getNewGame()).build();
	}

	@GET
	@Path("/game")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getApp() {
		File[] listFiles = APP_DIR.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".apk");
			}
		});
		if (listFiles == null || listFiles.length == 0) {
			return Response.status(Status.NOT_FOUND).build();
		}
		try {
			ResponseBuilder ok = Response.ok(new FileInputStream(listFiles[0]));
			ok.header("Content-Disposition",
					"attachment; filename=\"fox-hunting.apk\"");
			return ok.build();
		} catch (FileNotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path("/game/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Response postFoxPosition(@PathParam("id") String shortGameId,
			Location location) throws SQLException {

		if (!!!insertGamerPosition(getLongId(shortGameId), FOX_GAMER_ID,
				location)) {
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok().build();
	}

	@GET
	@Path("/game/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Response isGameRunning(@PathParam("id") String shortGameId)
			throws SQLException {
		return getLongId(shortGameId) == null ? Response.status(
				Status.NOT_FOUND).build() : Response.ok().build();
	}

	@POST
	@Path("/game/{id}/{gamer}")
	@Produces(MediaType.APPLICATION_XML)
	public Response postHunterPosition(@PathParam("id") String shortGameId,
			@PathParam("gamer") String gamerId, Location location)
			throws SQLException {
		String currentGameId = getLongId(shortGameId);
		if (!!!insertGamerPosition(currentGameId, gamerId, location)) {
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok(getAzimuthToFox(currentGameId, location)).build();
	}

	private String getLongId(String shortId) {
		cachedGames.cleanUp();
		return cachedGames.getIfPresent(shortId);
	}

	private Azimuth getAzimuthToFox(String gameId, Location location) {
		final AtomicReference<Timestamp> date = new AtomicReference<Timestamp>();
		Float next = Iterators
				.getNext(
						geoRec.receiveGeos(
								String.format(Locale.US,
										"select ST_AZIMUTH(a.poly_geom,ST_POINT(%f,%f))/(2*pi()) * 360, a.timestamp "
												+ "from %s a "
												+ "inner join( "
												+ "select id,player_id,max(timestamp) as t from fox_hunter "
												+ "WHERE id='%s' "
												+ "and player_id='%s' "
												+ "group by id,player_id "
												+ ") fh on fh.id=a.id and fh.player_id=a.player_id and t=a.timestamp;",
										location.getLon(), location.getLat(),
										FOX_HUNTER, gameId, FOX_GAMER_ID),
								new GeoFactory<Float>() {

									@Override
									public Float createGeo(
											ResultSet executeQuery)
											throws SQLException {
										date.set(executeQuery.getTimestamp(2));
										return executeQuery.getFloat(1);
									}
								}).iterator(), null);

		if (next == null) {
			LOGGER.warn("strange result! for AZIMUTH! {}", gameId);
		}
		Azimuth azimuth = new Azimuth();
		azimuth.setValue(next == null ? 0f : next);
		if (date.get() != null) {
			GregorianCalendar toSet = new GregorianCalendar();
			toSet.setTimeInMillis(date.get().getTime());
			azimuth.setTimestamp(dataFactory.newXMLGregorianCalendar(toSet));
		}
		return azimuth;
	}

	private boolean insertGamerPosition(String gameId, String gamerId,
			Location location) throws SQLException {
		if (gameId == null) {
			return false;
		} else {
			geoRec.getConn().execute(
					createInsertStatement(gameId, gamerId, location));
			return true;
		}
	}

	private String createInsertStatement(String gameId, String gamerId,
			Location location) {

		return String
				.format(Locale.US,
						"insert into \"%s\" values ('%s','%s','%s','%s', ST_GeomFromText('POINT(%f %f)',-1));",
						FOX_HUNTER, gameId, gamerId, location.getGamerName(),
						getTimeStamp(location), location.getLon(),
						location.getLat());
	}

	private String getTimeStamp(Location location) {
		return location.getTimestamp() == null ? new Timestamp(
				System.currentTimeMillis()).toString() : new Timestamp(location
				.getTimestamp().toGregorianCalendar().getTime().getTime())
				.toString();
	}

	private synchronized String getNewGame() {

		String longId = getRandomID();
		String shortId = longId.substring(0, 5);

		while (cachedGames.getIfPresent(shortId) != null) {
			longId = getRandomID();
			shortId = longId.substring(0, 5);
		}
		cachedGames.put(shortId, longId);
		return shortId;
	}

	private String getRandomID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
}