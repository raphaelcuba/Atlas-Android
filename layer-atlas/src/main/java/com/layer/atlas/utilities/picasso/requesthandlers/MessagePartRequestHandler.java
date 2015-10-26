package com.layer.atlas.utilities.picasso.requesthandlers;

import android.net.Uri;
import android.util.Log;

import com.layer.sdk.LayerClient;
import com.layer.sdk.listeners.LayerProgressListener;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Queryable;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles Picasso load requests for Layer MessagePart content.  If the content is not ready
 * (e.g. MessagePart.isContentReady() is `false`), registers a LayerProgressListener, downloads
 * the part, and waits for completion.
 */
public class MessagePartRequestHandler extends com.squareup.picasso.RequestHandler {
    private final static String TAG = MessagePartRequestHandler.class.getSimpleName();
    private final LayerClient mLayerClient;

    public MessagePartRequestHandler(LayerClient layerClient) {
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
