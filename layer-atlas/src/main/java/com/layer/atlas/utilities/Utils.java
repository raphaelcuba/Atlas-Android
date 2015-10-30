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
package com.layer.atlas.utilities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.layer.atlas.cellfactories.BasicImageCellFactory;
import com.layer.atlas.cellfactories.LocationCellFactory;
import com.layer.atlas.cellfactories.MimeCellFactory;
import com.layer.atlas.cellfactories.TextCellFactory;
import com.layer.atlas.cellfactories.ThreePartImageCellFactory;
import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static final String METADATA_KEY_CONVERSATION_TITLE = "conversationName";
    public static final int TIME_HOURS_24 = 24 * 60 * 60 * 1000;
    public static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a");
    public static final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEE, LLL dd,");
    public static final String[] TIME_WEEKDAYS_NAMES = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public static String getLastMessageString(Message msg) {
        if (TextCellFactory.isType(msg)) return TextCellFactory.getPreview(msg);
        if (ThreePartImageCellFactory.isType(msg)) return ThreePartImageCellFactory.getPreview(msg);
        if (LocationCellFactory.isType(msg)) return LocationCellFactory.getPreview(msg);
        if (BasicImageCellFactory.isType(msg)) return BasicImageCellFactory.getPreview(msg);
        return MimeCellFactory.getPreview(msg);
    }

    public static String getConversationTitle(LayerClient client, ParticipantProvider provider, Conversation conversation) {
        String metadataTitle = (String) conversation.getMetadata().get(METADATA_KEY_CONVERSATION_TITLE);
        if (metadataTitle != null && !metadataTitle.trim().isEmpty()) return metadataTitle.trim();

        StringBuilder sb = new StringBuilder();
        String userId = client.getAuthenticatedUserId();
        for (String participantId : conversation.getParticipants()) {
            if (participantId.equals(userId)) continue;
            Participant participant = provider.getParticipant(participantId);
            if (participant == null) continue;
            String initials = conversation.getParticipants().size() > 2 ? getInitials(participant) : participant.getName();
            if (sb.length() > 0) sb.append(", ");
            sb.append(initials);
        }
        return sb.toString().trim();
    }

    public static String getInitials(Participant p) {
        return getInitials(p.getName());
    }

    public static String getInitials(String fullName) {
        if (fullName.contains(" ")) {
            String[] names = fullName.split(" ");
            int count = 0;
            StringBuilder b = new StringBuilder();
            for (String name : names) {
                String t = name.trim();
                if (t.isEmpty()) continue;
                b.append(("" + t.charAt(0)).toUpperCase());
                if (++count >= 2) break;
            }
            return b.toString();
        } else {
            return ("" + fullName.trim().charAt(0)).toUpperCase();
        }
    }

    /**
     * @return if Today: time. If Yesterday: "Yesterday", if within one week: day of week, otherwise: dateFormat.format()
     */
    public static String formatTimeShort(Date dateTime, DateFormat timeFormat, DateFormat dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeText = null;
        if (dateTime.getTime() > todayMidnight) {
            timeText = timeFormat.format(dateTime.getTime());
        } else if (dateTime.getTime() > yesterMidnight) {
            timeText = "Yesterday";
        } else if (dateTime.getTime() > weekAgoMidnight) {
            cal.setTime(dateTime);
            timeText = TIME_WEEKDAYS_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeText = dateFormat.format(dateTime);
        }
        return timeText;
    }

    /**
     * Today, Yesterday, Weekday or Weekday + date
     */
    public static String formatTimeDay(Date sentAt) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayMidnight = cal.getTimeInMillis();
        long yesterMidnight = todayMidnight - TIME_HOURS_24;
        long weekAgoMidnight = todayMidnight - TIME_HOURS_24 * 7;

        String timeBarDayText = null;
        if (sentAt.getTime() > todayMidnight) {
            timeBarDayText = "Today";
        } else if (sentAt.getTime() > yesterMidnight) {
            timeBarDayText = "Yesterday";
        } else if (sentAt.getTime() > weekAgoMidnight) {
            cal.setTime(sentAt);
            timeBarDayText = TIME_WEEKDAYS_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1];
        } else {
            timeBarDayText = sdfDayOfWeek.format(sentAt);
        }
        return timeBarDayText;
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
                scaledHeight = (int) Math.round((double) inHeight / widthRatio);
            } else {
                scaledHeight = maxHeight;
                scaledWidth = (int) Math.round((double) inWidth / heightRatio);
            }
        }
        return new int[]{scaledWidth, scaledHeight};
    }

    public static Message newThreePartImageMessage(LayerClient layerClient, Context context, Uri imageUri) throws IOException {
        Cursor cursor = context.getContentResolver().query(imageUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return newThreePartImageMessage(layerClient, new File(cursor.getString(0)));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static Message newThreePartImageMessage(LayerClient layerClient, File imageFile) throws IOException {
        if (imageFile == null) throw new IllegalArgumentException("Null image file");
        if (!imageFile.exists()) throw new IllegalArgumentException("Image file does not exist");
        if (!imageFile.canRead()) throw new IllegalArgumentException("Cannot read image file");
        if (imageFile.length() <= 0) throw new IllegalArgumentException("Image file is empty");

        // Try parsing Exif data.
        int orientation = ThreePartImageCellFactory.ORIENTATION_0;
        try {
            ExifInterface exifInterface = new ExifInterface(imageFile.getAbsolutePath());
            int exifOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Found Exif orientation: " + exifOrientation);
            }
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    orientation = ThreePartImageCellFactory.ORIENTATION_0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    orientation = ThreePartImageCellFactory.ORIENTATION_180;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = ThreePartImageCellFactory.ORIENTATION_270;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = ThreePartImageCellFactory.ORIENTATION_90;
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return newThreePartImageMessage(layerClient, orientation, imageFile);
    }

    /**
     * Creates a new ThreePartImage Message.  The full image is attached untouched, while the
     * preview is created from the full image by loading, resizing, and compressing.
     *
     * @param client
     * @param file   Image file
     * @return
     */
    private static Message newThreePartImageMessage(LayerClient client, int orientation, File file) throws IOException {
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

        MessagePart info = client.newMessagePart(ThreePartImageCellFactory.MIME_INFO, ("{\"orientation\":" + orientation + ", \"width\":" + fullWidth + ", \"height\":" + fullHeight + "}").getBytes());

        MessagePart preview;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Creating PreviewImage from '" + file.getAbsolutePath() + "': " + new String(info.getData()));
        }

        // Determine preview size
        int[] previewDim = Utils.scaleDownInside(fullWidth, fullHeight, ThreePartImageCellFactory.PREVIEW_MAX_WIDTH, ThreePartImageCellFactory.PREVIEW_MAX_HEIGHT);
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
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, ThreePartImageCellFactory.PREVIEW_COMPRESSION_QUALITY, previewStream);
        previewBitmap.recycle();
        preview = client.newMessagePart(ThreePartImageCellFactory.MIME_PREVIEW, previewStream.toByteArray());
        previewStream.close();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(Locale.US, "PreviewImage full bytes: %d, preview bytes: %d, info bytes: %d", full.getSize(), preview.getSize(), info.getSize()));
        }

        MessagePart[] parts = new MessagePart[3];
        parts[ThreePartImageCellFactory.PART_INDEX_FULL] = full;
        parts[ThreePartImageCellFactory.PART_INDEX_PREVIEW] = preview;
        parts[ThreePartImageCellFactory.PART_INDEX_INFO] = info;
        return client.newMessage(parts);
    }
}