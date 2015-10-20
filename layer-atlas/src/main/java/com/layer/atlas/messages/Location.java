package com.layer.atlas.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;

public class Location {
    private static final String TAG = Location.class.getSimpleName();
    private static final int GOOGLE_API_REQUEST_CODE = 47000;
    private static final String MIME_TYPE = "location/coordinate";

    private static GoogleApiClient sGoogleApiClient;
    private static AtomicInteger sTryCount = new AtomicInteger(3);


    public static void init(final Activity activity) {
        if (sTryCount.decrementAndGet() <= 0) {
            Log.e(TAG, "Giving up updating Google Play Services.");
            return;
        }

        // If the client has already been created, ensure connected and return.
        if (sGoogleApiClient != null) {
            if (!sGoogleApiClient.isConnected()) sGoogleApiClient.connect();
            return;
        }

        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity);

        // If the correct Google Play Services are available, connect and return. 
        if (errorCode == ConnectionResult.SUCCESS) {
            sTryCount.set(3);
            GoogleApiCallbacks googleApiCallbacks = new GoogleApiCallbacks();
            sGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(googleApiCallbacks)
                    .addOnConnectionFailedListener(googleApiCallbacks)
                    .addApi(LocationServices.API)
                    .build();
            connectGoogleApi();
            return;
        }

        // If the correct Google Play Services are not available, redirect to proper solution.
        if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(activity, errorCode, GOOGLE_API_REQUEST_CODE)
                    .show();
            return;
        }

        Log.e(TAG, "Cannot update Google Play Services: " + errorCode);
    }

    public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != GOOGLE_API_REQUEST_CODE) return false;
        Location.init(activity);
        return true;
    }

    public static void connectGoogleApi() {
        sGoogleApiClient.connect();
    }

    public static void disconnectGoogleApi() {
        sGoogleApiClient.disconnect();
    }

    public static void getFreshLocation(LocationListener listener) {
        LocationRequest r = new LocationRequest()
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(10000)
                .setMaxWaitTime(10000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(sGoogleApiClient, r, listener);
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static class CellFactory extends AtlasCellFactory<CellFactory.CellHolder, CellFactory.ParsedContent> {
        private static final String TAG = CellFactory.class.getSimpleName();
        private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;
        private static final double GOLDEN_RATIO = (1.0 + Math.sqrt(5.0)) / 2.0;

        private final Picasso mPicasso;
        private final Transformation mTransform;

        public CellFactory(Context context, Picasso picasso) {
            super(32 * 1024);
            mPicasso = picasso;
            float radius = context.getResources().getDimension(R.dimen.atlas_message_item_cell_radius);
            mTransform = new RoundedTransform(radius);
        }

        @Override
        public boolean isBindable(Message message) {
            return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
        }

        @Override
        public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
            return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
        }

        @Override
        public ParsedContent parseContent(Message message) {
            try {
                JSONObject o = new JSONObject(new String(message.getMessageParts().get(0).getData()));
                ParsedContent c = new ParsedContent();
                c.mLatitude = o.optDouble("lat", 0);
                c.mLongitude = o.optDouble("lon", 0);
                c.mLabel = o.optString("label", null);
                return c;
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        public void bindCellHolder(final CellHolder cellHolder, final ParsedContent location, Message message, CellHolderSpecs specs) {
            // Google Static Map API has max dimension 640
            int mapWidth = Math.min(640, specs.maxWidth);
            int mapHeight = (int) Math.round((double) mapWidth / GOLDEN_RATIO);
            final String url = new StringBuilder()
                    .append("https://maps.googleapis.com/maps/api/staticmap?zoom=16&maptype=roadmap&scale=2&")
                    .append("center=").append(location.mLatitude).append(",").append(location.mLongitude).append("&")
                    .append("size=").append(mapWidth).append("x").append(mapHeight).append("&")
                    .append("markers=color:red%7C").append(location.mLatitude).append(",").append(location.mLongitude)
                    .toString();

            final int[] cellDims = ThreePartImage.scaleDownInside(specs.maxWidth, (int) Math.round((double) specs.maxWidth / GOLDEN_RATIO), specs.maxWidth, specs.maxHeight);
            cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
            mPicasso.load(url).tag(TAG).noFade().placeholder(PLACEHOLDER_RES_ID)
                    .resize(cellDims[0], cellDims[1])
                    .centerCrop().transform(mTransform).into(cellHolder.mImageView);

            // TODO: register click listeners with CellFactories?
            cellHolder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String encodedLabel = (location.mLabel == null) ? URLEncoder.encode("Shared Marker") : URLEncoder.encode(location.mLabel);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + location.mLatitude + "," + location.mLongitude + "(" + encodedLabel + ")&z=16"));
                    cellHolder.mImageView.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public void onScrollStateChanged(int newState) {
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    mPicasso.pauseTag(TAG);
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                case RecyclerView.SCROLL_STATE_SETTLING:
                    mPicasso.resumeTag(TAG);
                    break;
            }
        }

        static class ParsedContent implements AtlasCellFactory.ParsedContent {
            double mLatitude;
            double mLongitude;
            String mLabel;

            @Override
            public int sizeOf() {
                return (mLabel == null ? 0 : mLabel.getBytes().length) + ((Double.SIZE + Double.SIZE) / Byte.SIZE);
            }
        }

        static class CellHolder extends AtlasCellFactory.CellHolder {
            ImageView mImageView;

            public CellHolder(View view) {
                mImageView = (ImageView) view.findViewById(R.id.cell_image);
            }
        }
    }

    public static class Sender extends AttachmentSender {
        private static final String TAG = Sender.class.getSimpleName();

        public Sender(String title, Integer icon) {
            super(title, icon);
        }

        @Override
        public boolean send() {
            getFreshLocation(new LocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {
                    String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
                    try {
                        JSONObject o = new JSONObject()
                                .put("lat", location.getLatitude())
                                .put("lon", location.getLongitude())
                                .put("label", myName);
                        MessagePart p = getLayerClient().newMessagePart(MIME_TYPE, o.toString().getBytes());
                        Message message = getLayerClient().newMessage(p);
                        message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_location, myName));
                        getConversation().send(message);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
            return true;
        }
    }

    private static class GoogleApiCallbacks implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            Log.v(TAG, "GoogleApiClient connected: " + bundle);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.v(TAG, "GoogleApiClient suspended: " + i);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.v(TAG, "GoogleApiClient failed to connect: " + connectionResult);
        }
    }
}
