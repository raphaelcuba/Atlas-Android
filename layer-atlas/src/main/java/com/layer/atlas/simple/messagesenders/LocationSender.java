package com.layer.atlas.simple.messagesenders;

import android.location.Location;

import com.google.android.gms.location.LocationListener;
import com.layer.atlas.R;
import com.layer.atlas.simple.cells.LocationCellFactory;
import com.layer.sdk.messaging.Message;

public class LocationSender extends AttachmentSender {
    private static final String TAG = LocationSender.class.getSimpleName();

    public LocationSender(String title, Integer icon) {
        super(title, icon);
    }

    @Override
    public boolean send() {
        LocationCellFactory.getFreshLocation(new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
                Message message = LocationCellFactory.newLocationMessage(getLayerClient(), location.getLatitude(), location.getLongitude());
                message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_location, myName));
                getConversation().send(message);
            }
        });
        return true;
    }
}
