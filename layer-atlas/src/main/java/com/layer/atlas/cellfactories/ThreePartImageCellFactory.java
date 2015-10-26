package com.layer.atlas.cellfactories;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.utils.Utils;
import com.layer.atlas.utils.picasso.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * ThreePartImage handles image Messages with three parts: full image, preview image, and
 * image metadata.  The image metadata contains full image dimensions and rotation information used
 * for sizing and rotating images efficiently.
 */
public class ThreePartImageCellFactory extends AtlasCellFactory<ThreePartImageCellFactory.CellHolder, ThreePartImageCellFactory.ParsedContent> {
    private static final String TAG = ThreePartImageCellFactory.class.getSimpleName();
    private static final String TAG_FULL = TAG + ".full";

    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 3;
    public static final int ORIENTATION_180 = 1;
    public static final int ORIENTATION_270 = 2;

    public static final int PREVIEW_COMPRESSION_QUALITY = 50;
    public static final int PREVIEW_MAX_WIDTH = 512;
    public static final int PREVIEW_MAX_HEIGHT = 512;

    public static final String MIME_PREVIEW = "image/jpeg+preview";
    public static final String MIME_INFO = "application/json+imageSize";

    public static final int PART_INDEX_FULL = 0;
    public static final int PART_INDEX_PREVIEW = 1;
    public static final int PART_INDEX_INFO = 2;

    private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;

    private final Picasso mPicasso;
    private final Transformation mTransform;

    public ThreePartImageCellFactory(Context context, Picasso picasso) {
        super(32 * 1024);
        mPicasso = picasso;
        float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }

    public static boolean isType(Message message) {
        List<MessagePart> parts = message.getMessageParts();
        return parts.size() == 3 &&
                parts.get(PART_INDEX_FULL).getMimeType().startsWith("image/") &&
                parts.get(PART_INDEX_PREVIEW).getMimeType().equals(MIME_PREVIEW) &&
                parts.get(PART_INDEX_INFO).getMimeType().equals(MIME_INFO);
    }

    public static String getPreview(Message message) {
        return "Attachment: Image";
    }

    public static Uri getFullImageId(Message message) {
        return message.getMessageParts().get(PART_INDEX_FULL).getId();
    }

    public static Uri getPreviewImageId(Message message) {
        return message.getMessageParts().get(PART_INDEX_PREVIEW).getId();
    }

    @Override
    public boolean isBindable(Message message) {
        return ThreePartImageCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
    }

    @Override
    public ParsedContent parseContent(Message message) {
        try {
            ParsedContent info = new ParsedContent();
            JSONObject infoObject = new JSONObject(new String(message.getMessageParts().get(PART_INDEX_INFO).getData()));
            info.orientation = infoObject.getInt("orientation");
            info.width = infoObject.getInt("width");
            info.height = infoObject.getInt("height");
            return info;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, ParsedContent info, final Message message, CellHolderSpecs specs) {
        // Get rotation and scaled dimensions
        final float rotation;
        final int[] cellDims;
        switch (info.orientation) {
            case ORIENTATION_0:
                rotation = 0f;
                cellDims = Utils.scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            case ORIENTATION_90:
                rotation = -90f;
                cellDims = Utils.scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
            case ORIENTATION_180:
                rotation = 180f;
                cellDims = Utils.scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            default:
                rotation = 90f;
                cellDims = Utils.scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
        }

        // Load preview, followed by full image
        mPicasso.load(getPreviewImageId(message)).tag(TAG).placeholder(PLACEHOLDER_RES_ID)
                .noFade().centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                .transform(mTransform).into(cellHolder.mImageView, new Callback() {
            @Override
            public void onSuccess() {
                mPicasso.load(getFullImageId(message)).tag(TAG_FULL).noPlaceholder()
                        .noFade().centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                        .transform(mTransform).into(cellHolder.mImageView);
            }

            @Override
            public void onError() {
                Log.e(TAG, "Failed to load preview image for: " + message);
            }
        });
    }

    @Override
    public void onScrollStateChanged(int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                mPicasso.pauseTag(TAG_FULL);
                mPicasso.pauseTag(TAG);
                break;
            case RecyclerView.SCROLL_STATE_IDLE:
                mPicasso.resumeTag(TAG_FULL);
            case RecyclerView.SCROLL_STATE_SETTLING:
                mPicasso.resumeTag(TAG);
                break;
        }
    }

    static class ParsedContent implements AtlasCellFactory.ParsedContent {
        public int orientation;
        public int width;
        public int height;

        @Override
        public int sizeOf() {
            return (Integer.SIZE + Integer.SIZE + Integer.SIZE) / Byte.SIZE;
        }
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
        }
    }

}
