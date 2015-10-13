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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.layer.atlas.old.Utils.Tools;

/**
 * @author Oleg Orlov
 * @since 09 Jun 2015
 */
public class AtlasProgressView extends View {
    private static final String TAG = AtlasProgressView.class.getSimpleName();
    private static final boolean debug = false;

    private float progress;
    private int colorMain = Color.argb(0xA0, 0xFF, 0xFF, 0xFF);

    private float pieRadiusDp = 20;
    private float spacingWidthDp = 5;

    // working stuff
    private Paint ringPaint;
    private Paint piePaint;
    private RectF pieBounds = new RectF();

    //----------------------------------------------------------------------------
    public AtlasProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    public AtlasProgressView(Context context) {
        super(context);
        setupPaints();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mWidthBefore = getMeasuredWidth();
        int mHeightBefore = getMeasuredHeight();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int mWidthAfter = getMeasuredWidth();
        int mHeightAfter = getMeasuredHeight();

        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int defaultWidth = (int) Tools.getPxFromDp(getDefaultWidthDp(), getContext());
        int defaultHeight = (int) Tools.getPxFromDp(getDefaultHeightDp(), getContext());

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            setMeasuredDimension(w, h);
        } else {
            setMeasuredDimension(defaultWidth, defaultHeight);
        }

        if (debug) Log.w(TAG, "onMeasure() before: " + mWidthBefore + "x" + mHeightBefore
                + ", spec: " + Tools.toStringSpec(widthMeasureSpec) + "x" + Tools.toStringSpec(heightMeasureSpec)
                + ", after: " + mWidthAfter + "x" + mHeightAfter
                + ", final: " + getMeasuredWidth() + "x" + getMeasuredHeight());
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (debug)
            Log.d(TAG, "onLayout() changed: " + changed + " left: " + left + " top: " + top + " right: " + right + " bottom: " + bottom);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (debug)
            Log.w(TAG, "onSizeChanged() w: " + w + " h: " + h + " oldw: " + oldw + " oldh: " + oldh);
    }

    private float getDefaultWidthDp() {
        return pieRadiusDp * 2 + spacingWidthDp * 2;
    }

    ;

    private float getDefaultHeightDp() {
        return pieRadiusDp * 2 + spacingWidthDp * 2;
    }

    ;

    private void setupPaints() {
        ringPaint = new Paint();
        ringPaint.setColor(colorMain);
        ringPaint.setStyle(Style.STROKE);
        ringPaint.setAntiAlias(true);

        piePaint = new Paint();
        piePaint.setStyle(Style.FILL);
        piePaint.setColor(colorMain);
        piePaint.setAntiAlias(true);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (progress < 0.001f || progress > 0.999f) return;

        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float outerRadiusPx = (viewWidth + viewHeight) / 2;// + viewHeight;
        float innerRadiusPx = Tools.getPxFromDp(pieRadiusDp, getContext());
        float centerX = viewWidth / 2;
        float centerY = viewHeight / 2;

        pieBounds.set(centerX - innerRadiusPx, centerY - innerRadiusPx, centerX + innerRadiusPx, centerY + innerRadiusPx);
        canvas.drawArc(pieBounds, -90 + (360 * progress), 360 - (360 * progress), true, piePaint);   // sweepAngle is a diff, not absolute value

        float ringInnerRadiusPx = innerRadiusPx + Tools.getPxFromDp(spacingWidthDp, getContext());

        // calculate ring parameters
        float ringRadiusPx = 0.5f * (ringInnerRadiusPx + outerRadiusPx);
        float strokeWidth = outerRadiusPx - ringInnerRadiusPx;

        ringPaint.setStrokeWidth(strokeWidth);

        canvas.drawCircle(viewWidth / 2, viewHeight / 2, ringRadiusPx, ringPaint);

        if (debug)
            Log.w(TAG, "onDraw() out_R: " + outerRadiusPx + ", in_R: " + innerRadiusPx + ", ring_R: " + ringRadiusPx + ", strokeWidth: " + strokeWidth + ", progress: " + progress);

    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }
}
