package com.layer.atlas.messagesenders;

import android.util.Log;

import com.layer.atlas.R;
import com.layer.sdk.messaging.MessageOptions;

public class TextSender extends MessageSender {
    private static final String TAG = TextSender.class.getSimpleName();

    private final int mMaxLength;

    public TextSender(int maxNotificationLength) {
        mMaxLength = maxNotificationLength;
    }

    public boolean send(String text) {
        if (text == null || text.trim().length() == 0) {
            Log.e(TAG, "No text to send");
            return false;
        }
        String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
        String notification = getContext().getString(R.string.atlas_notification_text, myName, (text.length() < mMaxLength) ? text : (text.substring(0, mMaxLength) + "…"));
        getConversation().send(getLayerClient().newMessage(new MessageOptions().pushNotificationMessage(notification), getLayerClient().newMessagePart(text)));
        return true;
    }
}