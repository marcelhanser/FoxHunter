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

import kn.uni.gis.foxhunt.context.Util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CompassImageView extends ImageView {

	private float foxDirection;
	private float northDirection;
	private Bitmap bMap;
	private Bitmap greenArrowMap;
	private Bitmap redArrowMap;
	private volatile boolean isFoxOutdated = false;

	public CompassImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CompassImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CompassImageView(Context context) {
		super(context);
		init();
	}

	private void init() {
		foxDirection = 18;
		northDirection = 0;

		bMap = Util.transformPixels(BitmapFactory.decodeResource(
				getResources(), R.drawable.compass),
				Util.TRANPARENTING_IF_DIFFERENCE_FROM_BORDER);

		greenArrowMap = Util.transformPixels(
				BitmapFactory.decodeResource(getResources(), R.drawable.arrow),
				Util.TRANPARENTING_IF_DIFFERENCE_FROM_BORDER);

		ColorMatrix doItRed = new ColorMatrix(new float[] { //
				2.5f, 0, 0, 0, //
						0, 0.9f, 0, 0, //
						0, 0, 0.9f, 0, //
						0, 0, 0, 0.9f, //
						0, 0, 0, 0 });

		// rotate arrow map about 270*
		matrix.setRotate(-270, greenArrowMap.getWidth() / 2,
				greenArrowMap.getHeight() / 2);

		greenArrowMap = Bitmap.createBitmap(greenArrowMap, 0, 0,
				greenArrowMap.getWidth(), greenArrowMap.getHeight(), matrix,
				false);

		redArrowMap = changeColor(greenArrowMap, doItRed);

		setBackgroundColor(0);
		setImageBitmap(bMap);
	}

	public static Bitmap changeColor(Bitmap bmpOriginal, ColorMatrix doItRe) {
		Bitmap bmp = Bitmap.createBitmap(bmpOriginal.getWidth(),
				bmpOriginal.getWidth(), Config.ARGB_8888);

		Canvas c = new Canvas(bmp);
		Paint paint = new Paint();
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(doItRe);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);

		return bmp;
	}

	private Matrix matrix = new Matrix();

	@Override
	protected void onDraw(Canvas canvas) {

		matrix.setRotate(northDirection, canvas.getWidth() / 2,
				canvas.getHeight() / 2);

		canvas.drawBitmap(bMap, matrix, null);

		float boardPosX = (canvas.getWidth() - greenArrowMap.getWidth()) / 2;
		float boardPosY = (canvas.getHeight() - greenArrowMap.getHeight()) / 2;

		matrix.setRotate((northDirection + foxDirection) % 360,
				canvas.getWidth() / 2, canvas.getHeight() / 2);

		canvas.setMatrix(matrix);
		canvas.drawBitmap(isFoxOutdated ? redArrowMap : greenArrowMap,
				boardPosX, boardPosY, null);

	}

	public float getNorthAngle() {
		return northDirection;
	}

	public void setNorthAngle(float northAngle) {
		this.northDirection = -1 * northAngle;
		postInvalidate();
	}

	public float getAziruth() {
		return foxDirection;
	}

	public void setFoxDirection(float winkel) {
		this.foxDirection = winkel;
		postInvalidate();
	}

	public void setFoxOutdated(boolean bool) {
		isFoxOutdated = bool;
	}

}
