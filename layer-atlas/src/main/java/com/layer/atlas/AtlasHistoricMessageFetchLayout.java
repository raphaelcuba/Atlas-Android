package com.layer.atlas;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChange;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;

public class AtlasHistoricMessageFetchLayout extends SwipeRefreshLayout implements LayerChangeEventListener.BackgroundThread.Weak {
    private LayerClient mLayerClient;
    private Conversation mConversation;
    private int mSyncAmount = 25;

    public AtlasHistoricMessageFetchLayout(Context context) {
        super(context);
    }

    public AtlasHistoricMessageFetchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AtlasHistoricMessageFetchLayout init(LayerClient layerClient) {
        mLayerClient = layerClient;
        setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mConversation.getHistoricSyncStatus() == Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
                    mConversation.syncMoreHistoricMessages(mSyncAmount);
                }
            }
        });
        return this;
    }

    /**
     * Automatically refresh on resume.
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        refresh();
    }

    /**
     * Sets the `Conversation` this `AtlasMessagesSwipeSyncLayout` synchronizes historic `Messages`
     * for.
     *
     * @param conversation The `Conversation` to synchronize historic `Messages` for.
     * @return This `AtlasMessagesSwipeSyncLayout`.
     */
    public AtlasHistoricMessageFetchLayout setConversation(Conversation conversation) {
        mConversation = conversation;
        mLayerClient.registerEventListener(this);
        refresh();
        return this;
    }

    /**
     * Sets the number of historic Messages to synchronize from the current Conversation when
     * pulled down.
     *
     * @param syncAmount Number of historic Messages to synchronize.
     * @return This `AtlasMessagesSwipeSyncLayout`.
     */
    public AtlasHistoricMessageFetchLayout setHistoryPerSync(int syncAmount) {
        mSyncAmount = syncAmount;
        return this;
    }

    /**
     * Refreshes the state of this `AtlasMessagesSwipeSyncLayout`.
     *
     * @return This `AtlasMessagesSwipeSyncLayout`.
     */
    private AtlasHistoricMessageFetchLayout refresh() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mConversation == null) {
                    setEnabled(false);
                    setRefreshing(false);
                    return;
                }
                Conversation.HistoricSyncStatus status = mConversation.getHistoricSyncStatus();
                setEnabled(status == Conversation.HistoricSyncStatus.MORE_AVAILABLE);
                setRefreshing(status == Conversation.HistoricSyncStatus.SYNC_PENDING);
            }
        });
        return this;
    }

    @Override
    public void onChangeEvent(LayerChangeEvent layerChangeEvent) {
        for (LayerChange change : layerChangeEvent.getChanges()) {
            if (change.getObject() != mConversation) continue;
            if (change.getChangeType() != LayerChange.Type.UPDATE) continue;
            if (!change.getAttributeName().equals("historicSyncStatus")) continue;
            refresh();
        }
    }
}
