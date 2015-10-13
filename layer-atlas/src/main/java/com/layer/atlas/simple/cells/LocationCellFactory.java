package com.layer.atlas.simple.cells;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationCellFactory extends AtlasCellFactory<LocationCellFactory.CellHolder, LocationCellFactory.Location> {
    public static final String TAG = LocationCellFactory.class.getSimpleName();
    private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;
    private static final double GOLDEN_RATIO = (1.0 + Math.sqrt(5.0)) / 2.0;
    private static final String MIME_TYPE = "location/coordinate";

    private final Picasso mPicasso;
    private final Transformation mTransform;
    private static GoogleApiClient sGoogleApiClient;

    public LocationCellFactory(Context context, Picasso picasso) {
        super(32 * 1024);
        mPicasso = picasso;
        float radius = context.getResources().getDimension(R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }


    //==============================================================================================
    // Cell construction, binding, and caching
    //==============================================================================================

    @Override
    public boolean isBindable(Message message) {
        return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_image, cellView, true));
    }

    @Override
    public Location parseContent(Message message) {
        try {
            JSONObject o = new JSONObject(new String(message.getMessageParts().get(0).getData()));
            Location c = new Location();
            c.mLatitude = o.getDouble("lat");
            c.mLongitude = o.getDouble("lon");
            return c;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, Location location, Message message, CellHolderSpecs specs) {
        // Google Static Map API has max dimension 640
        int mapWidth = Math.min(640, specs.maxWidth);
        int mapHeight = (int) Math.round((double) mapWidth / GOLDEN_RATIO);
        final String url = new StringBuilder()
                .append("https://maps.googleapis.com/maps/api/staticmap?zoom=16&maptype=roadmap&scale=2&")
                .append("center=").append(location.mLatitude).append(",").append(location.mLongitude).append("&")
                .append("size=").append(mapWidth).append("x").append(mapHeight).append("&")
                .append("markers=color:red%7C").append(location.mLatitude).append(",").append(location.mLongitude)
                .toString();

        int[] cellDims = PreviewImageCellFactory.scaleDownInside(specs.maxWidth, (int) Math.round((double) specs.maxWidth / GOLDEN_RATIO), specs.maxWidth, specs.maxHeight);
        cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
        mPicasso.load(url).tag(TAG).noFade().placeholder(PLACEHOLDER_RES_ID)
                .resize(cellDims[0], cellDims[1])
                .centerCrop().transform(mTransform).into(cellHolder.mImageView);
    }


    //==============================================================================================
    // Utility methods
    //==============================================================================================

    public static void init(Context context) {
        if (sGoogleApiClient != null) return;
        GoogleApiCallbacks googleApiCallbacks = new GoogleApiCallbacks();
        sGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(googleApiCallbacks)
                .addOnConnectionFailedListener(googleApiCallbacks)
                .addApi(LocationServices.API)
                .build();
        connectGoogleApi();
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
        LocationServices.FusedLocationApi.requestLocationUpdates(sGoogleApiClient, r, listener);
    }

    public static Message newLocationMessage(LayerClient layerClient, double latitude, double longitude) {
        MessagePart p = layerClient.newMessagePart(MIME_TYPE, ("{\"lat\":" + latitude + ",\"lon\":" + longitude + "}").getBytes());
        return layerClient.newMessage(p);
    }


    //==============================================================================================
    // Classes
    //==============================================================================================

    private static class GoogleApiCallbacks implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

    static class Location implements AtlasCellFactory.ParsedContent {
        double mLatitude;
        double mLongitude;

        @Override
        public int sizeOf() {
            return (Double.SIZE + Double.SIZE) / Byte.SIZE;
        }
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
        }
    }

}
