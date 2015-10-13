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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.layer.atlas.old.Utils.Tools;
import com.layer.atlas.old.cells.ImageCell;

/**
 * @author Oleg Orlov
 * @since 15 Jun 2015
 */
public class AtlasImageView extends View {
    private static final String TAG = AtlasImageView.class.getSimpleName();
    private static final boolean debug = false;

    private int defaultLayerType;

    private Drawable drawable;

    private int contentWidth;
    private int contentHeight;
    public int orientation;
    public float angle;

    // TODO: 
    // - support contentDimensions: 0x0
    // - support contentDimensions + MeasureSpec.EXACT sizes 
    // - support boundaries + drawable instead of contentDimensions + drawable 

    //----------------------------------------------------------------------------
    public AtlasImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    public AtlasImageView(Context context) {
        super(context);
        setupPaints();
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        int mWidthBefore = getMeasuredWidth();
        int mHeightBefore = getMeasuredHeight();
        super.onMeasure(widthSpec, heightSpec);
        int mWidthAfter = getMeasuredWidth();
        int mHeightAfter = getMeasuredHeight();

        if (debug) Log.w(TAG, "onMeasure() before: " + mWidthBefore + "x" + mHeightBefore
                        + ", spec: " + Tools.toStringSpec(widthSpec, heightSpec)
                        + ", after: " + mWidthAfter + "x" + mHeightAfter
                        + ", content: " + contentWidth + "x" + contentHeight + " h/w: " + (1.0f * contentHeight / contentWidth)
        );

        int widthMode = MeasureSpec.getMode(widthSpec);
        int heightMode = MeasureSpec.getMode(heightSpec);
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            if (debug)
                Log.w(TAG, "onMeasure() exact dimenstions, skipping " + Tools.toStringSpec(widthSpec, heightSpec));
        } else if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            if (debug)
                Log.w(TAG, "onMeasure() first pass, skipping " + Tools.toStringSpec(widthSpec, heightSpec));
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(w, (int) (1.0 * w * contentHeight / contentWidth));
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                if (contentWidth >= w) {
                    setMeasuredDimension(w, (int) (1.0 * w * contentHeight / contentWidth));
                } else {
                    setMeasuredDimension(contentWidth, contentHeight);
                }
            }
        } else {
            if (debug)
                Log.w(TAG, "onMeasure() unchanged. " + Tools.toStringSpec(widthSpec, heightSpec));
        }

        if (debug)
            Log.w(TAG, "onMeasure() final: " + getMeasuredWidth() + "x" + getMeasuredHeight());
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

    private void setupPaints() {
        this.defaultLayerType = getLayerType();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }

        if (drawable == null) return;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bmpDrw = (BitmapDrawable) drawable;
            if (bmpDrw.getBitmap() != null) {
                if (debug)
                    Log.w(TAG, "onDraw() bitmap: " + bmpDrw.getBitmap().getWidth() + "x" + bmpDrw.getBitmap().getHeight());
            } else {
                if (debug) Log.w(TAG, "onDraw() bitmap: null");
            }
        }

        if (debug) {
            Log.w(TAG,
                    "onDraw() bounds: " + drawable.getBounds() + ", orientation: " + orientation
                            + "            min: " + drawable.getMinimumWidth() + "x" + drawable.getMinimumHeight()
                            + "     instrinsic: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight()
            );
        }

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int imgWidth = contentWidth;
        int imgHeight = contentHeight;

        if (contentWidth == 0 && contentHeight == 0) {
            imgWidth = drawable.getIntrinsicWidth();
            imgHeight = drawable.getIntrinsicHeight();
        }
        int left = (viewWidth - imgWidth) / 2;
        int top = (viewHeight - imgHeight) / 2;
        int right = left + imgWidth;
        int bottom = top + imgHeight;
        if (debug)
            Log.w(TAG, "onDraw() left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom);
        drawable.setBounds(left, top, right, bottom);

        int saved = canvas.save();
        boolean iOSBug = true;
        if (iOSBug) {
            switch (orientation) {
                case ImageCell.ORIENTATION_1_CW_180:
                    canvas.rotate(180, 0.5f * drawable.getBounds().width(), 0.5f * drawable.getBounds().height());
                    break;
                case ImageCell.ORIENTATION_2_CW_90:
                    if (false) {
                        drawable.setBounds(-viewHeight / 2, -viewWidth / 2, viewHeight / 2, viewWidth / 2);
                        canvas.rotate(-90);
                        canvas.translate(viewWidth / 2, viewHeight / 2);
                    } else {
                        drawable.setBounds(0, 0, viewHeight, viewWidth);
                        canvas.translate(0, viewHeight);
                        canvas.rotate(-90);
                    }
                    break;
                case ImageCell.ORIENTATION_3_CCW_90:
                    drawable.setBounds(0, 0, viewHeight, viewWidth);
                    canvas.translate(viewWidth, 0);
                    canvas.rotate(90);
                    break;
                default:
                    canvas.rotate(angle, 0.5f * viewWidth, 0.5f * viewHeight);
            }
        } else {
            if (orientation == ImageCell.ORIENTATION_3_CCW_90 || orientation == ImageCell.ORIENTATION_1_CW_180) {
                drawable.setBounds(0, 0, viewHeight, viewWidth);
            }
            switch (orientation) {
                case ImageCell.ORIENTATION_1_CW_180:
                    canvas.rotate(-90, 0.5f * drawable.getBounds().width(), 0.5f * drawable.getBounds().height());
                    break;
                case ImageCell.ORIENTATION_2_CW_90:
                    canvas.rotate(180, 0.5f * viewWidth, 0.5f * viewHeight);
                    break;
                case ImageCell.ORIENTATION_3_CCW_90:
                    canvas.rotate(90, 0.5f * drawable.getBounds().width(), 0.5f * drawable.getBounds().height());
                    break;
                default:
                    canvas.rotate(angle, 0.5f * viewWidth, 0.5f * viewHeight);
            }
        }
        drawable.draw(canvas);
        canvas.restoreToCount(saved);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (debug) Log.w(TAG, "onTouch() event: " + Tools.toString(event));
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        boolean result = super.verifyDrawable(who);
        if (who == this.drawable) result = true;
        return result;
    }

    public void setBitmap(Bitmap bmp) {
        setDrawable(new BitmapDrawable(bmp));
    }

    public void setDrawable(Drawable drawable) {
        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }
        this.drawable = drawable;
        if (drawable != null) {
            this.drawable.setCallback(this);
        }
        if (drawable instanceof GIFDrawable) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(defaultLayerType, null);
        }
        invalidate();
    }

    public void setContentDimensions(int contentWidth, int contentHeight) {
        boolean requestLayout = false;
        if (this.contentWidth != contentWidth || this.contentHeight != contentHeight) {
            requestLayout = true;
        }
        if (debug)
            Log.w(TAG, "setContentDimensions() new: " + contentWidth + "x" + contentHeight + ", old: " + this.contentWidth + "x" + this.contentHeight);
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;

        if (requestLayout) {
            requestLayout();
        }
    }
}
