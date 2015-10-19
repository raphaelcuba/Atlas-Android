package com.layer.atlas.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PreviewImageCellFactory handles image Messages with three parts: full image, preview image, and
 * image metadata.  The image metadata contains full image dimensions and rotation information used
 * for sizing and rotating images efficiently.
 */
public class ThreePartImage {
    private static final String TAG = ThreePartImage.class.getSimpleName();

    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 3;
    public static final int ORIENTATION_180 = 1;
    public static final int ORIENTATION_270 = 2;

    private static final int PREVIEW_COMPRESSION_QUALITY = 50;
    private static final int PREVIEW_MAX_WIDTH = 512;
    private static final int PREVIEW_MAX_HEIGHT = 512;

    public static final String MIME_PREVIEW = "image/jpeg+preview";
    public static final String MIME_INFO = "application/json+imageSize";

    private static final int PART_INDEX_FULL = 0;
    private static final int PART_INDEX_PREVIEW = 1;
    private static final int PART_INDEX_INFO = 2;

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
                scaledHeight = (int) Math.round((double) inHeight / widthRatio);
            } else {
                scaledHeight = maxHeight;
                scaledWidth = (int) Math.round((double) inWidth / heightRatio);
            }
        }
        return new int[]{scaledWidth, scaledHeight};
    }

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

    public static Message newThreePartMessage(LayerClient layerClient, Context context, Uri imageUri) throws IOException {
        Cursor cursor = context.getContentResolver().query(imageUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return newThreePartMessage(layerClient, new File(cursor.getString(0)));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static Message newThreePartMessage(LayerClient layerClient, File imageFile) throws IOException {
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

        return newThreePartMessage(layerClient, orientation, imageFile);
    }

    /**
     * Creates a new PreviewImage Message.  The full image is attached untouched, while the
     * preview is created from the full image by loading, resizing, and compressing.
     *
     * @param client
     * @param file   Image file
     * @return
     */
    private static Message newThreePartMessage(LayerClient client, int orientation, File file) throws IOException {
        if (client == null) throw new IllegalArgumentException("Null LayerClient");
        if (file == null) throw new IllegalArgumentException("Null image file");
        if (!file.exists()) throw new IllegalArgumentException("No image file");
        if (!file.canRead()) throw new IllegalArgumentException("Cannot read image file");

        BitmapFactory.Options justBounds = new BitmapFactory.Options();
        justBounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), justBounds);

        int fullWidth = justBounds.outWidth;
        int fullHeight = justBounds.outHeight;
        MessagePart full = client.newMessagePart("image/jpeg", new FileInputStream(file), file.length());
        MessagePart info = client.newMessagePart(MIME_INFO, ("{\"orientation\":" + orientation + ", \"width\":" + fullWidth + ", \"height\":" + fullHeight + "}").getBytes());
        MessagePart preview;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Creating PreviewImage from '" + file.getAbsolutePath() + "': " + new String(info.getData()));
        }

        // Determine preview size
        int[] previewDim = scaleDownInside(fullWidth, fullHeight, PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "PreviewImage preview size: " + previewDim[0] + "x" + previewDim[1]);
        }

        // Determine sample size for preview
        int sampleSize = 1;
        int sampleWidth = fullWidth;
        int sampleHeight = fullHeight;
        while (sampleWidth > previewDim[0] && sampleHeight > previewDim[1]) {
            sampleWidth >>= 1;
            sampleHeight >>= 1;
            sampleSize <<= 1;
        }
        if (sampleSize != 1) sampleSize >>= 1; // Back off 1 for scale-down instead of scale-up
        BitmapFactory.Options previewOptions = new BitmapFactory.Options();
        previewOptions.inSampleSize = sampleSize;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "PreviewImage sampled size: " + (sampleWidth << 1) + "x" + (sampleHeight << 1));
        }

        // Create low-quality preview
        Bitmap sampledBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), previewOptions);
        Bitmap previewBitmap = Bitmap.createScaledBitmap(sampledBitmap, previewDim[0], previewDim[1], false);
        sampledBitmap.recycle();
        ByteArrayOutputStream previewStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, PREVIEW_COMPRESSION_QUALITY, previewStream);
        previewBitmap.recycle();
        preview = client.newMessagePart(MIME_PREVIEW, previewStream.toByteArray());
        previewStream.close();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(Locale.US, "PreviewImage full bytes: %d, preview bytes: %d, info bytes: %d", full.getSize(), preview.getSize(), info.getSize()));
        }

        MessagePart[] parts = new MessagePart[3];
        parts[PART_INDEX_FULL] = full;
        parts[PART_INDEX_PREVIEW] = preview;
        parts[PART_INDEX_INFO] = info;
        return client.newMessage(parts);
    }

    public static class CellFactory extends AtlasCellFactory<CellFactory.CellHolder, CellFactory.ParsedContent> {
        private static final String TAG = CellFactory.class.getSimpleName();
        private static final String TAG_FULL = CellFactory.class.getSimpleName() + ".full";

        private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;

        private final Picasso mPicasso;
        private final Transformation mTransform;

        public CellFactory(Context context, Picasso picasso) {
            super(32 * 1024);
            mPicasso = picasso;
            float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
            mTransform = new RoundedTransform(radius);
        }

        @Override
        public boolean isBindable(Message message) {
            List<MessagePart> parts = message.getMessageParts();
            return parts.size() == 3 &&
                    parts.get(PART_INDEX_FULL).getMimeType().startsWith("image/") &&
                    parts.get(PART_INDEX_PREVIEW).getMimeType().equals(MIME_PREVIEW) &&
                    parts.get(PART_INDEX_INFO).getMimeType().equals(MIME_INFO);
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

    public static class CameraSender extends AttachmentSender {
        private static final String TAG = CameraSender.class.getSimpleName();
        public static final int REQUEST_CODE = 47001;

        private final WeakReference<Activity> mActivity;
        private final AtomicReference<String> mPhotoFilePath = new AtomicReference<String>(null);

        public CameraSender(String title, Integer icon, Activity activity) {
            super(title, icon);
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public boolean send() {
            Activity activity = mActivity.get();
            if (activity == null) {
                Log.e(TAG, "Activity went out of scope.");
                return false;
            }

            String fileName = "cameraOutput" + System.currentTimeMillis() + ".jpg";
            File file = new File(getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);
            mPhotoFilePath.set(file.getAbsolutePath());
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            final Uri outputUri = Uri.fromFile(file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            activity.startActivityForResult(cameraIntent, REQUEST_CODE);
            return true;
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_CODE) return false;
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Got result code: " + requestCode + ", data: " + data);
                return true;
            }

            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            try {
                Message message = newThreePartMessage(getLayerClient(), new File(mPhotoFilePath.get()));
                message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
                getConversation().send(message);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return true;
        }

        @Override
        public void onActivityCreate(Bundle savedInstanceState) {
            mPhotoFilePath.set(savedInstanceState.getString(CameraSender.class.getSimpleName() + ".photoFilePath", null));
        }

        @Override
        public void onActivitySaveInstanceState(Bundle outState) {
            outState.putString(CameraSender.class.getSimpleName() + ".photoFilePath", mPhotoFilePath.get());
        }
    }

    public static class GallerySender extends AttachmentSender {
        private static final String TAG = GallerySender.class.getSimpleName();
        public static final int REQUEST_CODE = 47000;

        private final WeakReference<Activity> mActivity;

        public GallerySender(String title, Integer icon, Activity activity) {
            super(title, icon);
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public boolean send() {
            Activity activity = mActivity.get();
            if (activity == null) {
                Log.e(TAG, "Activity went out of scope.");
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(Intent.createChooser(intent, "Attach Photo"), REQUEST_CODE);
            return true;
        }

        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_CODE) return false;
            if (resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Got result code: " + requestCode + ", data: " + data);
                return true;
            }

            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            try {
                Message message = newThreePartMessage(getLayerClient(), getContext(), data.getData());
                message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
                getConversation().send(message);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return true;
        }
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
}
