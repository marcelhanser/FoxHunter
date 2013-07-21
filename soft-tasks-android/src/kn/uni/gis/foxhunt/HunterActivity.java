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

import kn.uni.gis.foxhunt.context.GameContext;
import kn.uni.gis.foxhunt.context.GameException;
import kn.uni.gis.foxhunt.context.Util;
import kn.uni.gis.foxhunt.context.Util.PerformConstraint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class HunterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hunter);

		final String deviceId = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
				.getDeviceId();

		Util.setEditText(this, R.id.ED_HUNTER_DEVICE_ID, deviceId);

		Util.addNavigationToButton(this, R.id.BT_HUNTER_START,
				GameActivity.class, new PerformConstraint() {

					@Override
					public boolean perform() {
						String hunterId = ((EditText) findViewById(R.id.ED_HUNTER_GAME_ID))
								.getText().toString();

						String playerName = ((EditText) findViewById(R.id.ED_HUNTER_PLAYER_NAME))
								.getText().toString();

						if (Util.hasValidLengthAndShowToastIfNot(
								HunterActivity.this,
								hunterId,
								"Player name is to short, please enter at least 3 characters",
								3)
								&& Util.hasValidLengthAndShowToastIfNot(
										HunterActivity.this, hunterId,
										"Game id must have 5 characters", 5)) {

							try {
								GameContext.newHunterGame(hunterId, deviceId,
										playerName);
								return true;
							} catch (GameException e) {
								Log.e(FoxActivity.class.getName(),
										"error on creating new game", e);
								Toast.makeText(
										HunterActivity.this,
										"Problem on connecting to game"
												+ e.getMessage(),
										Toast.LENGTH_SHORT).show();
							}
						}
						return false;
					}
				});
	}

}
