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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import kn.uni.gis.foxhunt.context.GameContext;
import kn.uni.gis.foxhunt.context.SettingsContext;
import kn.uni.gis.foxhunt.context.Util;
import kn.uni.gis.foxhunt.context.XmlUtil;
import kn.uni.gis.foxhunt.pojo.Game;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity {

	private static final String DATA_FILE = "last_game";

	private static final String SERVER_URL_KEY = "PREF_SERVER_URL";
	private static final String USE_NETWORK_KES = "PREF_USE_NETWORK_LOCATION";

	private static final int RESULT_SETTINGS = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Util.addNavigationToButton(this, R.id.BT_M_FOX, FoxActivity.class);
		Util.addNavigationToButton(this, R.id.BT_M_HUNTER, HunterActivity.class);
		Util.addNavigationToButton(this, R.id.BT_M_RESUME, GameActivity.class);

		loadGame();
		updateSettings();

		activateResumeButton();
	}

	@Override
	protected void onResume() {
		super.onResume();
		activateResumeButton();
	}

	private void activateResumeButton() {
		if (GameContext.isGameRunning() && GameContext.isGameActive()) {
			((Button) findViewById(R.id.BT_M_RESUME)).setEnabled(true);
			((Button) findViewById(R.id.BT_M_RESUME))
					.setVisibility(View.VISIBLE);
			((Button) findViewById(R.id.BT_M_RESUME)).setText(String.format(
					"Resume Game %s as %s \n and %s", GameContext.getGame()
							.getId(), GameContext.getGame().getPlayerName(),
					GameContext.getGame().getType().toString()));
		} else {
			((Button) findViewById(R.id.BT_M_RESUME)).setEnabled(false);
			((Button) findViewById(R.id.BT_M_RESUME))
					.setVisibility(View.INVISIBLE);
		}
	}

	private void loadGame() {
		try {
			GameContext.setGame(XmlUtil.unmarshall(openFileInput(DATA_FILE),
					Game.class));
		} catch (FileNotFoundException e1) {
		} catch (Exception e) {
			Log.i(MainActivity.class.getName(), "problem on loading data_file",
					e);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (GameContext.isGameRunning()) {
			try {
				FileOutputStream openFileOutput = openFileOutput(DATA_FILE,
						Context.MODE_PRIVATE);
				openFileOutput.write(XmlUtil.marshall(GameContext.getGame())
						.getBytes());
			} catch (Exception e) {
				Toast.makeText(this, "Problem on saving current game",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;

		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RESULT_SETTINGS:
			updateSettings();
			break;

		}
	}

	private void updateSettings() {
		SettingsContext.getInstance().setServerUrl(
				readProperty(SERVER_URL_KEY,
						getResources().getString(R.string.PREF_URL_DEFAULT),
						String.class));

		SettingsContext.getInstance().setUseNetwork(
				readProperty(
						USE_NETWORK_KES,
						Boolean.valueOf(getResources().getString(
								R.string.PREF_USER_NETWORK_DEFAULT)),
						Boolean.class));
	}

	@SuppressWarnings("unchecked")
	public <T> T readProperty(String string, T defaults, Class<T> clazz) {

		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		if (Boolean.class == clazz) {
			return (T) Boolean.valueOf(defaultSharedPreferences.getBoolean(
					string, (Boolean) defaults));
		}
		if (String.class == clazz) {
			return (T) defaultSharedPreferences.getString(string,
					(String) defaults);
		}
		throw new IllegalArgumentException();

	}

}
