package com.layer.atlas.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasAvatar;
import com.layer.atlas.ParticipantProvider;
import com.layer.atlas.R;
import com.layer.atlas.old.Utils;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Collection;
import java.util.HashSet;

public class AtlasConversationsAdapter extends RecyclerView.Adapter<AtlasConversationsAdapter.ViewHolder> implements RecyclerViewController.Callback {
    protected final LayerClient mLayerClient;
    protected final ParticipantProvider mParticipantProvider;
    protected final Picasso mPicasso;
    private final RecyclerViewController<Conversation> mQueryController;
    private final LayoutInflater mInflater;
    private long mInitialHistory = 0;

    private OnConversationClickListener mConversationClickListener;
    private ViewHolder.OnClickListener mViewHolderClickListener;

    private final DateFormat mDateFormat;
    private final DateFormat mTimeFormat;

    public AtlasConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso) {
        this(context, client, participantProvider, picasso, null);
    }

    public AtlasConversationsAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Collection<String> updateAttributes) {
        Query<Conversation> query = Query.builder(Conversation.class)
                .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING))
                .build();
        mQueryController = client.newRecyclerViewController(query, updateAttributes, this);
        mLayerClient = client;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;
        mInflater = LayoutInflater.from(context);
        mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        mViewHolderClickListener = new ViewHolder.OnClickListener() {
            @Override
            public void onClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return;
                mConversationClickListener.onConversationClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }

            @Override
            public boolean onLongClick(ViewHolder viewHolder) {
                if (mConversationClickListener == null) return false;
                return mConversationClickListener.onConversationLongClick(AtlasConversationsAdapter.this, viewHolder.getConversation());
            }
        };
        setHasStableIds(false);
    }

    /**
     * Refreshes this adapter by re-running the underlying Query.
     */
    public void refresh() {
        mQueryController.execute();
    }


    //==============================================================================================
    // Initial message history
    //==============================================================================================

    public AtlasConversationsAdapter setInitialMessageHistory(long initialHistory) {
        mInitialHistory = initialHistory;
        return this;
    }

    private void syncInitialMessages(final int start, final int length) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long desiredHistory = mInitialHistory;
                if (desiredHistory <= 0) return;
                for (int i = start; i < start + length; i++) {
                    try {
                        final Conversation conversation = getItem(i);
                        if (conversation == null || conversation.getHistoricSyncStatus() != Conversation.HistoricSyncStatus.MORE_AVAILABLE) {
                            continue;
                        }
                        Query<Message> localCountQuery = Query.builder(Message.class)
                                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                                .build();
                        long delta = desiredHistory - mLayerClient.executeQueryForCount(localCountQuery);
                        if (delta > 0) conversation.syncMoreHistoricMessages((int) delta);
                    } catch (IndexOutOfBoundsException e) {
                        // Concurrent modification
                    }
                }
            }
        }).start();
    }


    //==============================================================================================
    // Listeners
    //==============================================================================================

    public AtlasConversationsAdapter setOnConversationClickListener(OnConversationClickListener conversationClickListener) {
        mConversationClickListener = conversationClickListener;
        return this;
    }


    //==============================================================================================
    // Adapter
    //==============================================================================================

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder viewHolder = new ViewHolder(mInflater.inflate(ViewHolder.RESOURCE_ID, parent, false));
        viewHolder.setClickListener(mViewHolderClickListener);
        viewHolder.mAvatarCluster.init(mParticipantProvider, mPicasso);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Conversation conversation = mQueryController.getItem(position);

        String userId = mLayerClient.getAuthenticatedUserId();
        Message lastMessage = conversation.getLastMessage();

        viewHolder.setConversation(conversation);
        HashSet<String> participantIds = new HashSet<String>(conversation.getParticipants());
        participantIds.remove(userId);
        viewHolder.mAvatarCluster.setParticipants(participantIds);

        viewHolder.mTitleView.setText(Utils.getTitle(conversation, mParticipantProvider, userId));

        if (lastMessage == null) {
            viewHolder.mMessageView.setText(null);
            viewHolder.mTimeView.setText(null);
        } else {
            viewHolder.mMessageView.setText(Utils.Tools.toString(lastMessage));
            viewHolder.mTitleView.setTypeface(null, (viewHolder.mConversation.getTotalUnreadMessageCount() > 0)? Typeface.BOLD : Typeface.NORMAL);
            if (lastMessage.getSentAt() == null) {
                viewHolder.mTimeView.setText(null);
            } else {
                viewHolder.mTimeView.setText(Utils.formatTimeShort(lastMessage.getSentAt(), mTimeFormat, mDateFormat));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mQueryController.getItemCount();
    }

    public Integer getPosition(Conversation conversation) {
        return mQueryController.getPosition(conversation);
    }

    public Integer getPosition(Conversation conversation, int lastPosition) {
        return mQueryController.getPosition(conversation, lastPosition);
    }

    public Conversation getItem(int position) {
        return mQueryController.getItem(position);
    }

    public Conversation getItem(RecyclerView.ViewHolder viewHolder) {
        return ((ViewHolder) viewHolder).getConversation();
    }


    //==============================================================================================
    // UI update callbacks
    //==============================================================================================

    @Override
    public void onQueryDataSetChanged(RecyclerViewController controller) {
        syncInitialMessages(0, getItemCount());
        notifyDataSetChanged();
    }

    @Override
    public void onQueryItemChanged(RecyclerViewController controller, int position) {
        notifyItemChanged(position);
    }

    @Override
    public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onQueryItemInserted(RecyclerViewController controller, int position) {
        syncInitialMessages(position, 1);
        notifyItemInserted(position);
    }

    @Override
    public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
        syncInitialMessages(positionStart, itemCount);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onQueryItemRemoved(RecyclerViewController controller, int position) {
        notifyItemRemoved(position);
    }

    @Override
    public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // Layout to inflate
        public final static int RESOURCE_ID = R.layout.atlas_conversation_item;

        // View cache
        protected TextView mTitleView;
        protected AtlasAvatar mAvatarCluster;
        protected TextView mMessageView;
        protected TextView mTimeView;

        protected Conversation mConversation;
        protected OnClickListener mClickListener;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            mAvatarCluster = (AtlasAvatar) itemView.findViewById(R.id.atlas_conversation_item_avatar);
            mTitleView = (TextView) itemView.findViewById(R.id.atlas_conversation_view_convert_participant);
            mMessageView = (TextView) itemView.findViewById(R.id.atlas_conversation_view_last_message);
            mTimeView = (TextView) itemView.findViewById(R.id.atlas_conversation_view_convert_time);
        }

        protected ViewHolder setClickListener(OnClickListener clickListener) {
            mClickListener = clickListener;
            return this;
        }

        public Conversation getConversation() {
            return mConversation;
        }

        public void setConversation(Conversation conversation) {
            mConversation = conversation;
        }

        @Override
        public void onClick(View v) {
            if (mClickListener == null) return;
            mClickListener.onClick(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mClickListener == null) return false;
            return mClickListener.onLongClick(this);
        }

        interface OnClickListener {
            void onClick(ViewHolder viewHolder);

            boolean onLongClick(ViewHolder viewHolder);
        }
    }

    /**
     * Listens for item clicks on an IntegrationConversationsAdapter.
     */
    public interface OnConversationClickListener {
        /**
         * Alerts the listener to item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item clicked.
         * @param conversation The item clicked.
         */
        void onConversationClick(AtlasConversationsAdapter adapter, Conversation conversation);

        /**
         * Alerts the listener to long item clicks.
         *
         * @param adapter      The IntegrationConversationsAdapter which had an item long-clicked.
         * @param conversation The item long-clicked.
         * @return true if the long-click was handled, false otherwise.
         */
        boolean onConversationLongClick(AtlasConversationsAdapter adapter, Conversation conversation);
    }
}