package com.layer.atlas.senders;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

public abstract class AttachmentSender extends MessageSender {
    private static final String TAG = AttachmentSender.class.getSimpleName();

    private final String mTitle;
    private final Integer mIcon;

    private Conversation mConversation;

    private Context mContext;
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;

    public AttachmentSender(String title, Integer icon) {
        mTitle = title;
        mIcon = icon;
    }

    public abstract boolean send();

    public void init(Context context, LayerClient layerClient, ParticipantProvider participantProvider) {
        mContext = context;
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
    }

    public String getTitle() {
        return mTitle;
    }

    public Integer getIcon() {
        return mIcon;
    }

    public Context getContext() {
        return mContext;
    }

    public LayerClient getLayerClient() {
        return mLayerClient;
    }

    public ParticipantProvider getParticipantProvider() {
        return mParticipantProvider;
    }

    public void setConversation(Conversation conversation) {
        mConversation = conversation;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    /**
     * Override if saved instance state is required.
     *
     * @param savedInstanceState
     */
    public void onActivityCreate(Bundle savedInstanceState) {

    }

    /**
     * Override to save instance state.
     *
     * @param outState
     */
    public void onActivitySaveInstanceState(Bundle outState) {

    }

    /**
     * Override to handle results from onActivityResult.
     *
     * @return true if the result was handled, or false otherwise.
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }
}
