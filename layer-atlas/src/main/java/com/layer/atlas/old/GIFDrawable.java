/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas.old;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * @author Oleg Orlov
 * @since 24 Jun 2015
 */
public class GIFDrawable extends Drawable {
    private static final String TAG = GIFDrawable.class.getSimpleName();
    private static final boolean debug = false;

    /**
     * By default first frame to show wouldn't depend on time of Drawable creation
     */
    private static final long defaultCreatedAt = System.currentTimeMillis();

    private Movie gif;
    private long createdAt = defaultCreatedAt;

    public GIFDrawable(Movie gif) {
        this.gif = gif;
    }

    public GIFDrawable(Movie gif, long createdAt) {
        this.gif = gif;
        this.createdAt = createdAt;
    }

    @Override
    public void draw(Canvas canvas) {
        if (debug)
            Log.w(TAG, "draw() gif: " + gif.width() + "x" + gif.height() + " @" + gif.duration());
        long time = 0;
        if (gif.duration() > 0) {
            time = (System.currentTimeMillis() - createdAt) % gif.duration();
        }
        gif.setTime((int) time);

        int saved = canvas.save();
        // prepare bounds
        int gifWidth = gif.width();
        int gifHeight = gif.height();
        int boundsWidth = getBounds().width();
        int boundsHeight = getBounds().height();
        if (boundsWidth != gifWidth || boundsHeight != gifHeight) {
            canvas.scale(1.0f * boundsWidth / gifWidth, 1.0f * boundsHeight / gifHeight);
        }
        gif.draw(canvas, getBounds().left, getBounds().top);
        canvas.restoreToCount(saved);
        if (debug)
            Log.w(TAG, "draw() gif: " + gif.width() + "x" + gif.height() + " @" + gif.duration()
                    + " time: " + time + ", bounds: " + getBounds().left + "x" + getBounds().top);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return gif.width();
    }

    @Override
    public int getIntrinsicHeight() {
        return gif.height();
    }

}
