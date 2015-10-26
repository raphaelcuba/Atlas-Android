package com.layer.atlas.messagesenders;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.layer.atlas.R;
import com.layer.atlas.cellfactories.LocationCellFactory;
import com.layer.sdk.messaging.MessageOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class LocationSender extends AttachmentSender {
    private static final String TAG = LocationSender.class.getSimpleName();
    private static final int GOOGLE_API_REQUEST_CODE = 47000;

    private static GoogleApiClient sGoogleApiClient;
    private static AtomicInteger sTryCount = new AtomicInteger(3);

    public LocationSender(String title, Integer icon) {
        super(title, icon);
    }

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
        init(activity);
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

    @Override
    public boolean send() {
        getFreshLocation(new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                try {
                    String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
                    JSONObject o = new JSONObject().put("lat", location.getLatitude()).put("lon", location.getLongitude()).put("label", myName);
                    String notification = getContext().getString(R.string.atlas_notification_location, myName);
                    getConversation().send(getLayerClient().newMessage(new MessageOptions().pushNotificationMessage(notification), getLayerClient().newMessagePart(LocationCellFactory.MIME_TYPE, o.toString().getBytes())));
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
        return true;
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
