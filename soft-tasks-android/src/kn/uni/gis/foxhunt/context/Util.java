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

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Util {

	private Util() {

	}

	// public static String SERVER_URL = "http://192.168.113.1:9090/game";

	public static URL createGameUrl(String gameId) {
		try {
			return new URL(SettingsContext.getInstance().getServerUrl() + "/"
					+ gameId);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static URL createFoxUrl(String gameId) {
		return createGameUrl(gameId);
	}

	public static URL createHunterUrl(String gameId, String playerId) {
		try {
			return new URL(SettingsContext.getInstance().getServerUrl() + "/"
					+ gameId + "/" + playerId);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static final PerformConstraint ALWAYS_TRUE = new PerformConstraint() {
		@Override
		public boolean perform() {
			return true;
		}
	};

	public static void addNavigationToButton(final Activity act, int btMFox,
			final Class<? extends Activity> class1) {
		addNavigationToButton(act, btMFox, class1, ALWAYS_TRUE);
	}

	public static void addNavigationToButton(final Activity act, int btMFox,
			final Class<? extends Activity> class1,
			final PerformConstraint activity) {
		((Button) act.findViewById(btMFox))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (activity.perform()) {
							act.startActivityForResult(
									new Intent(v.getContext(), class1), 0);
						}

					}
				});
	}

	public interface PerformConstraint {
		boolean perform();
	}

	public interface PixelFunction {

		void apply(Bitmap toreturn, int i, int j);
	}

	public static boolean hasValidLengthAndShowToastIfNot(Activity act,
			String tocheck, String msg, int lenght) {
		if (tocheck.length() < lenght) {
			Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	public static void setEditText(final Activity act, final int edGameGameId,
			final String id) {
		act.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Editable text = ((EditText) act.findViewById(edGameGameId))
						.getText();
				text.clear();
				text.append(id);
			}
		});
	}

	public static void setVisible(final Activity act, final int id,
			final int visible) {
		act.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				View view = act.findViewById(id);
				view.setVisibility(visible);
			}
		});
	}

	public static final PixelFunction TRANPARENTING_IF_DIFFERENCE_FROM_BORDER = new PixelFunction() {
		@Override
		public void apply(Bitmap map, int i, int j) {
			if ((map.getPixel(i, j) == map.getPixel(map.getWidth() - 1,
					map.getHeight() - 1))) {
				map.setPixel(i, j, Color.TRANSPARENT);
			}
		}
	};

	public static Bitmap transformPixels(Bitmap map, PixelFunction contraint) {
		Bitmap toreturn = map.copy(Bitmap.Config.ARGB_8888, true);
		for (int i = 0; i < toreturn.getWidth(); i++) {
			for (int j = 0; j < toreturn.getHeight(); j++) {
				contraint.apply(toreturn, i, j);
			}
		}
		return toreturn;
	}

}
