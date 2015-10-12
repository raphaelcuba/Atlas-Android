package com.layer.atlas.simple.cells;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreePartImageCellFactory implements AtlasCellFactory<ThreePartImageCellFactory.CellHolder> {
    private static final String TAG = ThreePartImageCellFactory.class.getSimpleName();

    private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;

    private final Picasso mPicasso;
    private final Transformation mTransform;
    private final Map<String, ThreePartImageUtils.ThreePartImageInfo> mInfoCache = new ConcurrentHashMap<String, ThreePartImageUtils.ThreePartImageInfo>();

    public ThreePartImageCellFactory(Context context, Picasso picasso) {
        mPicasso = picasso;
        float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }

    @Override
    public boolean isBindable(Message message) {
        return ThreePartImageUtils.isThreePartImage(message);
    }

    @Override
    public void onCache(Message message) {
        String id = message.getId().toString();
        if (mInfoCache.containsKey(id)) return;
        mInfoCache.put(id, ThreePartImageUtils.getInfo(message));
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_image, cellView, true));
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, final Message message, CellHolderSpecs specs) {
        // Parse into part
        onCache(message);
        ThreePartImageUtils.ThreePartImageInfo info = mInfoCache.get(message.getId().toString());

        // Get rotation and scaled dimensions
        final float rotation;
        final int[] cellDims;
        switch (info.orientation) {
            case ThreePartImageUtils.ORIENTATION_0:
                rotation = 0f;
                cellDims = ThreePartImageUtils.scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            case ThreePartImageUtils.ORIENTATION_90:
                rotation = -90f;
                cellDims = ThreePartImageUtils.scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
            case ThreePartImageUtils.ORIENTATION_180:
                rotation = 180f;
                cellDims = ThreePartImageUtils.scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            default:
                rotation = 90f;
                cellDims = ThreePartImageUtils.scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
        }

        if (ThreePartImageUtils.isFullImageReady(message)) {
            // Full image is ready, load it directly.
            mPicasso.load(ThreePartImageUtils.getFullImageId(message))
                    .tag(ThreePartImageUtils.TAG).placeholder(PLACEHOLDER_RES_ID).noFade()
                    .centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                    .transform(mTransform).into(cellHolder.mImageView);
        } else {
            // Full image is not ready, so start by loading the preview...
            mPicasso.load(ThreePartImageUtils.getPreviewImageId(message))
                    .tag(ThreePartImageUtils.TAG).placeholder(PLACEHOLDER_RES_ID).noFade()
                    .centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                    .transform(mTransform).into(cellHolder.mImageView, new Callback() {
                @Override
                public void onSuccess() {
                    // ...Then load in the full image.
                    mPicasso.load(ThreePartImageUtils.getFullImageId(message))
                            .tag(ThreePartImageUtils.TAG).noPlaceholder().noFade()
                            .centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                            .transform(mTransform).into(cellHolder.mImageView, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError() {
                        }
                    });
                }

                @Override
                public void onError() {
                }
            });
        }
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
        }
    }

}
