package com.layer.atlas.utils.picasso.transformations;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.squareup.picasso.Transformation;

public class CircleTransform implements Transformation {
    private final String mKey;

    public CircleTransform(String key) {
        mKey = key;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        float srcRadius = (float) Math.min(srcWidth, srcHeight) / 2f;
        Bitmap out = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, out.getWidth(), out.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(srcRadius, srcRadius, srcRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);
        source.recycle();
        return out;
    }

    @Override
    public String key() {
        return CircleTransform.class.getSimpleName() + "." + mKey;
    }
}
