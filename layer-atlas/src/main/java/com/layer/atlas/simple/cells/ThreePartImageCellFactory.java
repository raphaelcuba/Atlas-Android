package com.layer.atlas.simple.cells;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Queryable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ThreePartImageCellFactory handles image Messages with three parts: full image, preview image, and
 * image metadata.  The image metadata contains full image dimensions and rotation information used
 * for sizing and rotating images efficiently.
 */
public class ThreePartImageCellFactory extends AtlasCellFactory<ThreePartImageCellFactory.CellHolder, ThreePartImageCellFactory.ThreePartImageInfo> {
    public static final String TAG = ThreePartImageCellFactory.class.getSimpleName();

    private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;

    private static final String MIME_PREVIEW_SUFFIX = "+preview";
    private static final String MIME_INFO = "application/json+imageSize";

    private static final int PART_INDEX_FULL = 0;
    private static final int PART_INDEX_PREVIEW = 1;
    private static final int PART_INDEX_INFO = 2;

    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 3;
    public static final int ORIENTATION_180 = 1;
    public static final int ORIENTATION_270 = 2;

    private static final int COMPRESSION_QUALITY_FULL = 90;
    private static final int COMPRESSION_QUALITY_PREVIEW = 50;

    private static final int PREVIEW_MAX_WIDTH = 512;
    private static final int PREVIEW_MAX_HEIGHT = 512;

    private final Picasso mPicasso;
    private final Transformation mTransform;

    public ThreePartImageCellFactory(Context context, Picasso picasso) {
        super(32 * 1024);
        mPicasso = picasso;
        float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }


    //==============================================================================================
    // Cell construction, binding, and caching
    //==============================================================================================

    @Override
    public boolean isBindable(Message message) {
        List<MessagePart> parts = message.getMessageParts();
        return parts.size() == 3 &&
                parts.get(PART_INDEX_FULL).getMimeType().startsWith("image/") &&
                parts.get(PART_INDEX_PREVIEW).getMimeType().startsWith("image/") &&
                parts.get(PART_INDEX_PREVIEW).getMimeType().endsWith(MIME_PREVIEW_SUFFIX) &&
                parts.get(PART_INDEX_INFO).getMimeType().equals(MIME_INFO);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_image, cellView, true));
    }

    @Override
    public ThreePartImageInfo parseContent(Message message) {
        try {
            ThreePartImageInfo info = new ThreePartImageInfo();
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
    public void bindCellHolder(final CellHolder cellHolder, ThreePartImageInfo info, final Message message, CellHolderSpecs specs) {
        // Get rotation and scaled dimensions
        final float rotation;
        final int[] cellDims;
        switch (info.orientation) {
            case ORIENTATION_0:
                rotation = 0f;
                cellDims = scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            case ORIENTATION_90:
                rotation = -90f;
                cellDims = scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
            case ORIENTATION_180:
                rotation = 180f;
                cellDims = scaleDownInside(info.width, info.height, specs.maxWidth, specs.maxHeight);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
                break;
            default:
                rotation = 90f;
                cellDims = scaleDownInside(info.width, info.height, specs.maxHeight, specs.maxWidth);
                cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[1], cellDims[0]));
                break;
        }

        if (isFullImageReady(message)) {
            // Full image is ready, load it directly.
            mPicasso.load(getFullImageId(message)).tag(TAG).placeholder(PLACEHOLDER_RES_ID)
                    .noFade().centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                    .transform(mTransform).into(cellHolder.mImageView);
        } else {
            // Full image is not ready, so start by loading the preview...
            mPicasso.load(getPreviewImageId(message)).tag(TAG).placeholder(PLACEHOLDER_RES_ID)
                    .noFade().centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                    .transform(mTransform).into(cellHolder.mImageView, new Callback() {
                @Override
                public void onSuccess() {
                    // ...Then load in the full image.
                    mPicasso.load(getFullImageId(message)).tag(TAG).noPlaceholder()
                            .noFade().centerCrop().resize(cellDims[0], cellDims[1]).rotate(rotation)
                            .transform(mTransform).into(cellHolder.mImageView);
                }

                @Override
                public void onError() {
                }
            });
        }
    }


    //==============================================================================================
    // Utility methods
    //==============================================================================================

    public static boolean isFullImageReady(Message message) {
        return message.getMessageParts().get(PART_INDEX_FULL).isContentReady();
    }

    public static boolean isPreviewImageReady(Message message) {
        return message.getMessageParts().get(PART_INDEX_PREVIEW).isContentReady();
    }

    public static Uri getFullImageId(Message message) {
        return message.getMessageParts().get(PART_INDEX_FULL).getId();
    }

    public static Uri getPreviewImageId(Message message) {
        return message.getMessageParts().get(PART_INDEX_PREVIEW).getId();
    }

    public static Message newThreePartMessage(LayerClient layerClient, File imageFile) {
        if (imageFile == null) throw new IllegalArgumentException("Null image file");
        if (!imageFile.exists()) throw new IllegalArgumentException("Image file does not exist");
        if (!imageFile.canRead()) throw new IllegalArgumentException("Cannot read image file");
        if (imageFile.length() <= 0) throw new IllegalArgumentException("Image file is empty");

        // Try parsing Exif data.
        int orientation = ORIENTATION_0;
        try {
            ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
            int exifOrientation = exifInterface.getAttributeInt("Orientation", 1);
            switch (exifOrientation) {
                case 1:
                case 2:
                    orientation = ORIENTATION_0;
                    break;
                case 3:
                case 4:
                    orientation = ORIENTATION_180;
                    break;
                case 5:
                case 6:
                    orientation = ORIENTATION_270;
                    break;
                case 7:
                case 8:
                    orientation = ORIENTATION_90;
                    break;

            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return newThreePartMessage(layerClient, orientation, BitmapFactory.decodeFile(imageFile.getAbsolutePath()));
    }

    public static Message newThreePartMessage(LayerClient layerClient, Context context, Uri imageUri) throws IOException {
        if (imageUri == null) throw new IllegalArgumentException("Null image Uri");

        // Try getting the file so we can parse Exif orientiation.
        try {
            Cursor metaCursor = context.getContentResolver().query(imageUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        return newThreePartMessage(layerClient, new File(metaCursor.getString(0)));
                    }
                } finally {
                    metaCursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        // Fall back to default orientation.
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            return newThreePartMessage(layerClient, ORIENTATION_0, BitmapFactory.decodeStream(inputStream));
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }

    /**
     * TODO: reduce memory.  Potentially only work with files.
     *
     * @param client
     * @param fullBitmap
     * @return
     */
    private static Message newThreePartMessage(LayerClient client, int orientation, Bitmap fullBitmap) {
        if (client == null) throw new IllegalArgumentException("Null LayerClient");
        if (fullBitmap == null) throw new IllegalArgumentException("Null Bitmap");

        ByteArrayOutputStream fullStream = new ByteArrayOutputStream();
        fullBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY_FULL, fullStream);

        boolean isResize;
        int previewWidth = PREVIEW_MAX_WIDTH;
        int previewHeight = PREVIEW_MAX_HEIGHT;
        int fullWidth = fullBitmap.getWidth();
        int fullHeight = fullBitmap.getHeight();
        if (fullWidth <= PREVIEW_MAX_WIDTH && fullHeight <= PREVIEW_MAX_HEIGHT) {
            isResize = false;
        } else {
            isResize = true;
            double heightRatio = (double) fullHeight / (double) PREVIEW_MAX_HEIGHT;
            double widthRatio = (double) fullWidth / (double) PREVIEW_MAX_WIDTH;
            if (heightRatio > widthRatio) {
                previewWidth = (int) Math.round((double) fullWidth / heightRatio);
            } else {
                previewHeight = (int) Math.round((double) fullHeight / widthRatio);
            }
        }

        MessagePart full = client.newMessagePart("image/jpeg", fullStream.toByteArray());
        MessagePart info = client.newMessagePart(MIME_INFO, ("{\"orientation\":" + orientation + ", \"width\":" + fullWidth + ", \"height\":" + fullHeight + "}").getBytes());
        MessagePart preview;
        if (!isResize) {
            fullBitmap.recycle();
            preview = client.newMessagePart("image/jpeg" + MIME_PREVIEW_SUFFIX, fullStream.toByteArray());
            try {
                fullStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                fullStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap previewBitmap = Bitmap.createScaledBitmap(fullBitmap, previewWidth, previewHeight, true);
            fullBitmap.recycle();
            ByteArrayOutputStream previewStream = new ByteArrayOutputStream();
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY_PREVIEW, previewStream);
            preview = client.newMessagePart("image/jpeg" + MIME_PREVIEW_SUFFIX, previewStream.toByteArray());
            try {
                previewStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MessagePart[] parts = new MessagePart[3];
        parts[PART_INDEX_FULL] = full;
        parts[PART_INDEX_PREVIEW] = preview;
        parts[PART_INDEX_INFO] = info;
        return client.newMessage(parts);
    }

    /**
     * Returns int[] {scaledWidth, scaledHeight} for dimensions that fit within the given maxWidth,
     * maxHeight at the given inWidth, inHeight aspect ratio.  If the in dimensions fit fully inside
     * the max dimensions, no scaling is applied.  Otherwise, at least one scaled dimension is set
     * to a max dimension, and the other scaled dimension is scaled to fit.
     *
     * @param inWidth
     * @param inHeight
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static int[] scaleDownInside(int inWidth, int inHeight, int maxWidth, int maxHeight) {
        int scaledWidth;
        int scaledHeight;
        if (inWidth <= maxWidth && inHeight <= maxHeight) {
            scaledWidth = inWidth;
            scaledHeight = inHeight;
        } else {
            double widthRatio = (double) inWidth / (double) maxWidth;
            double heightRatio = (double) inHeight / (double) maxHeight;
            if (widthRatio > heightRatio) {
                scaledWidth = maxWidth;
                scaledHeight = (int) Math.round((double) scaledWidth * (double) inHeight / (double) inWidth);
            } else {
                scaledHeight = maxHeight;
                scaledWidth = (int) Math.round((double) scaledHeight * (double) inWidth / (double) inHeight);
            }
        }
        return new int[]{scaledWidth, scaledHeight};
    }

    /**
     * Handles Picasso load requests for Layer MessagePart content.  If the content is not ready
     * (e.g. MessagePart.isContentReady() is `false`), registers a LayerProgressListener, downloads
     * the part, and waits for completion.
     */
    public static class RequestHandler extends com.squareup.picasso.RequestHandler {
        private final static String TAG = RequestHandler.class.getSimpleName();
        private final LayerClient mLayerClient;

        public RequestHandler(LayerClient layerClient) {
            mLayerClient = layerClient;
        }

        @Override
        public boolean canHandleRequest(Request data) {
            Uri uri = data.uri;
            if (!"layer".equals(uri.getScheme())) return false;
            List<String> segments = uri.getPathSegments();
            if (segments.size() != 4) return false;
            if (!segments.get(2).equals("parts")) return false;
            return true;
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            Queryable queryable = mLayerClient.get(request.uri);
            if (!(queryable instanceof MessagePart)) return null;
            MessagePart part = (MessagePart) queryable;

            if (part.isContentReady()) {
                // No need to download, just return.
                return new Result(part.getDataStream(), Picasso.LoadedFrom.MEMORY);
            }

            // Must download; make it synchronous here.
            final CountDownLatch latch = new CountDownLatch(1);
            final LayerProgressListener listener = new LayerProgressListener() {
                @Override
                public void onProgressStart(MessagePart messagePart, Operation operation) {

                }

                @Override
                public void onProgressUpdate(MessagePart messagePart, Operation operation, long l) {

                }

                @Override
                public void onProgressComplete(MessagePart messagePart, Operation operation) {
                    latch.countDown();
                }

                @Override
                public void onProgressError(MessagePart messagePart, Operation operation, Throwable throwable) {
                    latch.countDown();
                }
            };
            try {
                mLayerClient.registerProgressListener(part, listener);
                if (!part.isContentReady()) {
                    part.download(null);
                    try {
                        latch.await(1, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } finally {
                mLayerClient.unregisterProgressListener(part, listener);
            }
            if (part.isContentReady()) {
                return new Result(part.getDataStream(), Picasso.LoadedFrom.NETWORK);
            }
            return null;
        }
    }

    /**
     * Parsed image metadata.
     */
    static class ThreePartImageInfo implements AtlasCellFactory.ParsedContent {
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
