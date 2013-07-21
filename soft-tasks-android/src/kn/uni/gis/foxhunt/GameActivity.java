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
package kn.uni.gis.foxhunt;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import kn.uni.gis.foxhunt.context.GameContext;
import kn.uni.gis.foxhunt.context.HttpContext;
import kn.uni.gis.foxhunt.context.HttpContext.EntityHandler;
import kn.uni.gis.foxhunt.context.SettingsContext;
import kn.uni.gis.foxhunt.context.Util;
import kn.uni.gis.foxhunt.context.XmlUtil;
import kn.uni.gis.foxhunt.pojo.Azimuth;
import kn.uni.gis.foxhunt.pojo.Game;
import kn.uni.gis.foxhunt.pojo.Game.Type;
import kn.uni.gis.foxhunt.pojo.Location;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;

public class GameActivity extends Activity implements SensorEventListener,
		LocationListener {

	private static final int FOX_TIMEOUT = 1000 * 60 * 2;
	private static final int BUFFER_SIZE = 20;

	private static final int DEFAULT_TIME = 3000;
	private static final int MAX_TIME = 15000;

	private static final DateFormat SIMPLE_DEFAULT_DATE_FORMAT = SimpleDateFormat
			.getTimeInstance(SimpleDateFormat.LONG);

	private CompassImageView view;
	private SensorManager sensorManager;
	private LocationManager locationManager;
	private Sensor accelerometer;

	private final ScheduledExecutorService scheduledService = Executors
			.newScheduledThreadPool(1);
	private List<Location> locationStack = new ArrayList<Location>(30);

	private volatile android.location.Location lastLocation;
	private Azimuth lastFoxLocation;

	private Runnable postJob;
	private ScheduledFuture<?> scheduledPostJob;

	private int currentDelay = DEFAULT_TIME;
	private int multiplyer = 2;
	private AtomicBoolean locationChanged = new AtomicBoolean(false);

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		if (!!!GameContext.isGameRunning()) {
			Log.e(GameActivity.class.getName(),
					"no game running and we are in the game activity -.-");
			finish();
		}

		setContentView(R.layout.game);

		Util.setEditText(this, R.id.ED_GAME_GAME_ID, GameContext.getGame()
				.getId());
		Util.setEditText(this, R.id.ED_GAME_GAME_TYPE, GameContext.getGame()
				.getType().toString());

		registerOnGPS();
		view = (CompassImageView) findViewById(R.id.IV_COMPASS_GAME);

		if (Type.HUNTER == GameContext.getGame().getType()) {
			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			accelerometer = sensorManager
					.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			Util.setVisible(this, R.id.IV_COMPASS_GAME, View.VISIBLE);
		} else {
			Util.setVisible(this, R.id.IV_COMPASS_GAME, View.INVISIBLE);
			Util.setVisible(this, R.id.ED_GAME_FOX_LAST_UPDATE, View.INVISIBLE);
			Util.setVisible(this, R.id.TV_GAME_FOX_LAST_UPDATE, View.INVISIBLE);
		}

		postJob = postLocation();
		scheduledPostJob = scheduledService.scheduleAtFixedRate(postJob, 0,
				3000, TimeUnit.MILLISECONDS);

		disableConnectionButton(R.id.GAME_RB_CONNECTED);
		disableConnectionButton(R.id.GAME_RB_GPS);
	}

	private void registerOnGPS() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!!!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(intent);
		}
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				500, 2f, this);
		if (SettingsContext.getInstance().isUseNetwork()) {
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 500, 2f, this);
		}
	}

	protected void onResume() {
		super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (GameContext.getGame().getType() == Type.HUNTER) {
			sensorManager.registerListener(this, accelerometer,
					SensorManager.SENSOR_DELAY_UI);
		}
	}

	protected void onPause() {
		super.onPause();
		if (GameContext.getGame().getType() == Type.HUNTER) {
			sensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(getString(R.string.GAME_BACK_DESC));
		builder.setTitle(getString(R.string.GAME_BACK_TITLE));
		builder.setNegativeButton(R.string.GAME_BACK_OK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				GameActivity.this.finish();
			}
		})
				.setPositiveButton(getString(R.string.GAME_BACK_CANCEL),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// NOOP
							}
						}).create().show();
	}

	private Runnable postLocation() {

		final Game game = GameContext.getGame();
		final GameHttpHandler handler = game.getType() == Type.HUNTER ? hunterHandler()
				: foxHandler();

		return new Runnable() {
			@Override
			public void run() {
				List<Location> locations = getLocations();
				handler.callSuccessful = true;
				for (int i = 0; i < locations.size(); i++) {
					Location location = locations.get(i);
					// Log.i(GameActivity.class.getName(), "sending: " +
					// location);
					HttpContext.getInstance().post(
							game.getPushUrl().toString(), location, handler);
					if (handler.callSuccessful) {
						locations.remove(i);
					} else {
						// Log.i(GameActivity.class.getName(),
						// "buffering: " + location + " buffer size: "
						// + locations.size());
						break;
					}
				}
				if (lastFoxLocation != null
						&& lastFoxLocation.getTimestamp() != null) {
					Util.setEditText(GameActivity.this,
							R.id.ED_GAME_FOX_LAST_UPDATE,
							SIMPLE_DEFAULT_DATE_FORMAT.format(lastFoxLocation
									.getTimestamp()));
					view.setFoxOutdated(!!!(System.currentTimeMillis()
							- lastFoxLocation.getTimestamp().getTime() < TWO_MINUTES));
				} else {
					Util.setEditText(GameActivity.this,
							R.id.ED_GAME_FOX_LAST_UPDATE, "--:--:--");
				}
			}
		};
	}

	private GameHttpHandler foxHandler() {
		return new GameHttpHandler() {
			@Override
			public void doHandleEntity(HttpEntity entity, int statusCode) {
				if (statusCode != HttpStatus.SC_OK) {
					Log.e(GameActivity.class.getName(),
							"something went wrong: status" + statusCode);
				}
			}
		};
	}

	private GameHttpHandler hunterHandler() {
		return new GameHttpHandler() {
			@Override
			public void doHandleEntity(HttpEntity entity, int statusCode) {
				if (statusCode != HttpStatus.SC_OK) {
					Log.e(GameActivity.class.getName(),
							"something went wrong: status" + statusCode);
				} else {
					try {
						Azimuth unmarshall = XmlUtil.unmarshall(
								entity.getContent(), Azimuth.class);
						view.setFoxDirection(unmarshall.getValue());
						lastFoxLocation = unmarshall;
						view.postInvalidate();
					} catch (Exception e) {
						Log.e(this.getClass().getName(),
								"error on unmarshalling", e);
					}
				}
			}
		};
	}

	private List<Location> getLocations() {
		Location location = getLocation();
		if (location != null) {
			if (locationStack.size() >= BUFFER_SIZE) {
				removeNearest();
			}
			locationStack.add(locationStack.size(), location);
			Util.setEditText(this, R.id.ED_GAME_BUFFERED,
					"" + locationStack.size());
		}
		return locationStack;
	}

	private void removeNearest() {
		long mintime_distance = Long.MAX_VALUE;
		int nearestIndex = 0;
		for (int i = 1; i < locationStack.size() - 1; i++) {
			Location last = locationStack.get(i - 1);
			Location curr = locationStack.get(i);
			Location next = locationStack.get(i + 1);

			long distToLast = Math.abs(last.getTimestamp().getTime()
					- curr.getTimestamp().getTime());
			long distToNext = Math.abs(curr.getTimestamp().getTime()
					- next.getTimestamp().getTime());

			long distance = distToLast + distToNext;
			if (distance < mintime_distance) {
				mintime_distance = distance;
				nearestIndex = i;
			}
		}

		Log.i(GameActivity.class.getName(),
				"removing " + locationStack.get(nearestIndex));

		locationStack.remove(nearestIndex);
	}

	private Location getLocation() {

		android.location.Location lastKnownLocation2 = lastLocation;
		if (locationChanged.compareAndSet(true, false)) {
			return new Location(GameContext.getGame().getPlayerName(),
					(float) lastKnownLocation2.getLongitude(),
					(float) lastKnownLocation2.getLatitude(), new Date(
							lastKnownLocation2.getTime()));
		}

		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		scheduledService.shutdown();
		locationManager.removeUpdates(this);
	}

	public void onSensorChanged(SensorEvent evt) {
		view.setNorthAngle(evt.values[0]);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onLocationChanged(android.location.Location location) {
		Log.d(GameActivity.class.getName(), "locations changed to: ");
		if (isBetterLocation(location, lastLocation)) {
			lastLocation = location;
			locationChanged.set(true);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				enableConnectionButton(R.id.GAME_RB_GPS);
				break;
			default:
				disableConnectionButton(R.id.GAME_RB_GPS);
				break;
			}
		}
		Log.d(GameActivity.class.getName(), "status changed to " + provider
				+ " [" + status + "]");
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(GameActivity.class.getName(), "provider enabled " + provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(GameActivity.class.getName(), "provider disabled " + provider);
	}

	private void disableConnectionButton(final int id) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RadioButton button = ((RadioButton) findViewById(id));
				button.setChecked(false);
			}
		});
	}

	private void enableConnectionButton(final int id) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RadioButton button = ((RadioButton) findViewById(id));
				button.setChecked(true);
			}
		});
	}

	private abstract class GameHttpHandler implements EntityHandler {

		private boolean callSuccessful = false;

		@Override
		public final void handleEntity(HttpEntity entity, int statusCode) {
			enableConnectionButton(R.id.GAME_RB_CONNECTED);
			if (currentDelay != DEFAULT_TIME) {
				currentDelay = DEFAULT_TIME;
				scheduledPostJob.cancel(false);
				scheduledPostJob = scheduledService.scheduleAtFixedRate(
						postJob, 0, currentDelay, TimeUnit.MILLISECONDS);
			}
			android.location.Location curr = lastLocation;
			if (curr != null) {
				Util.setEditText(GameActivity.this, R.id.ED_GAME_LAST_UPDATE,
						SIMPLE_DEFAULT_DATE_FORMAT.format(curr.getTime()));
			}
			doHandleEntity(entity, statusCode);
			callSuccessful = true;
		}

		@Override
		public final void handleException(Exception exception) {
			Log.e(GameActivity.class.getName(), "something went wrong",
					exception);
			disableConnectionButton(R.id.GAME_RB_CONNECTED);
			if (currentDelay < MAX_TIME) {
				currentDelay *= multiplyer;
				scheduledPostJob.cancel(false);
				scheduledPostJob = scheduledService.scheduleAtFixedRate(
						postJob, 0, currentDelay, TimeUnit.MILLISECONDS);
			}
			callSuccessful = false;
		}

		public abstract void doHandleEntity(HttpEntity entity, int statusCode);
	}

	private static final int TWO_MINUTES = FOX_TIMEOUT;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(android.location.Location location,
			android.location.Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
