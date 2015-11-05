package com.layer.atlas.imagepopup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;
import com.layer.atlas.utilities.Utils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.MessagePart;

import java.util.concurrent.TimeUnit;

public class MessagePartDecoder implements ImageDecoder {
    private static final String TAG = MessagePartDecoder.class.getSimpleName();

    private static LayerClient sLayerClient;

    public static void init(LayerClient layerClient) {
        sLayerClient = layerClient;
    }

    @Override
    public Bitmap decode(Context context, Uri messagePartId) throws Exception {
        MessagePart part = (MessagePart) sLayerClient.get(messagePartId);
        if (part == null) {
            Log.e(TAG, "No message part with ID: " + messagePartId);
            return null;
        }
        if (part.getMessage().isDeleted()) {
            Log.e(TAG, "Message part is deleted: " + messagePartId);
            return null;
        }
        if (!Utils.downloadMessagePart(sLayerClient, part, 3, TimeUnit.MINUTES)) {
            Log.e(TAG, "Timed out while downloading: " + messagePartId);
            return null;
        }

        return BitmapFactory.decodeStream(part.getDataStream());
    }
}
