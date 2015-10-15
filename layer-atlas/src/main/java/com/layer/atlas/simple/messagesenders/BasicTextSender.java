package com.layer.atlas.simple.messagesenders;

import android.util.Log;

import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;

public class BasicTextSender extends TextSender {
    private static final String TAG = BasicTextSender.class.getSimpleName();

    private final int mMaxLength;

    public BasicTextSender(int maxNotificationLength) {
        mMaxLength = maxNotificationLength;
    }

    @Override
    public boolean send(String text) {
        if (text == null || text.trim().length() == 0) {
            Log.e(TAG, "No text to send");
            return false;
        }
        Message message = getLayerClient().newMessage(getLayerClient().newMessagePart(text));
        String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
        String notification = (text.length() < mMaxLength) ? text : (text.substring(0, mMaxLength) + "…");
        message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_text, myName, notification));
        getConversation().send(message);
        return true;
    }
}
