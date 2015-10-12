package com.layer.atlas.simple.cells;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationUtils {
    private static final String TAG = LocationUtils.class.getSimpleName();
    public static final String MIME_TYPE = "location/coordinate";
    private static GoogleApiClient sGoogleApiClient;

    public static void generateGoogleApiClient(Context context) {
        if (sGoogleApiClient != null) return;
        Callbacks callbacks = new Callbacks();
        sGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(callbacks)
                .addOnConnectionFailedListener(callbacks)
                .addApi(LocationServices.API)
                .build();
    }

    public static void connect() {
        sGoogleApiClient.connect();
    }

    public static void disconnect() {
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

    public static Location getMessageLocation(Message message) {
        try {
            JSONObject o = new JSONObject(new String(message.getMessageParts().get(0).getData()));
            LocationUtils.Location c = new LocationUtils.Location();
            c.mLatitude = o.getDouble("lat");
            c.mLongitude = o.getDouble("lon");
            return c;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private static class Callbacks implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
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

    static class Location {
        double mLatitude;
        double mLongitude;
    }
}