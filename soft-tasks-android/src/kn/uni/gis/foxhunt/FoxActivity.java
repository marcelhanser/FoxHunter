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
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class FoxActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fox);

		Util.addNavigationToButton(this, R.id.BT_FOX_START, GameActivity.class,
				new PerformConstraint() {

					@Override
					public boolean perform() {
						String text = ((EditText) findViewById(R.id.ED_FOX_GAMER_NAME))
								.getText().toString();
						if (Util.hasValidLengthAndShowToastIfNot(
								FoxActivity.this,
								text,
								"Player name is to short, please enter at least 3 characters",
								3)) {

							try {
								GameContext.newFoxGame(text.toString());
								return true;
							} catch (GameException e) {
								Log.e(FoxActivity.class.getName(),
										"error on creating new game", e);
								Toast.makeText(
										FoxActivity.this,
										"Problem on creating new game"
												+ e.getMessage(),
										Toast.LENGTH_SHORT).show();
							}
						}
						return false;
					}
				});
	}
}
