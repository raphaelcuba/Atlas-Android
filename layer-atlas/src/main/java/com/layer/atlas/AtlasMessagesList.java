/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

import com.layer.atlas.adapters.AtlasMessagesAdapter;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

public class AtlasMessagesList extends RecyclerView {
    private static final String TAG = AtlasMessagesList.class.getSimpleName();

    private AtlasMessagesAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ItemTouchHelper mSwipeItemTouchHelper;

    //styles
    public int mMyBubbleColor;
    public int mMyTextColor;
    public int mMyTextStyle;
    private float mMyTextSize;
    public Typeface mMyTextTypeface;

    public int mOtherBubbleColor;
    public int mOtherTextColor;
    public int mOtherTextStyle;
    private float mOtherTextSize;
    public Typeface mOtherTextTypeface;

    private int mDateTextColor;
    private int mAvatarTextColor;
    private int mAvatarBackgroundColor;

    public AtlasMessagesList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
    }

    public AtlasMessagesList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasMessagesList(Context context) {
        super(context);
    }

    public AtlasMessagesList init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mLayoutManager.setStackFromEnd(true);
        setLayoutManager(mLayoutManager);

        // Create an adapter that auto-scrolls if we're already at the bottom
        mAdapter = new AtlasMessagesAdapter(getContext(), layerClient, participantProvider, picasso)
                .setRecyclerView(this)
                .setOnMessageAppendListener(new AtlasMessagesAdapter.OnMessageAppendListener() {
                    @Override
                    public void onMessageAppend(AtlasMessagesAdapter adapter, Message message) {
                        autoScroll();
                    }
                });
        super.setAdapter(mAdapter);

        return this;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        throw new RuntimeException("AtlasMessageList sets its own Adapter");
    }

    /**
     * Automatically refresh on resume
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        if (mAdapter != null) mAdapter.refresh();
    }

    /**
     * Updates the underlying AtlasMessagesAdapter with a Query for Messages in the given
     * Conversation.
     *
     * @param conversation Conversation to display Messages for.
     * @return This AtlasMessageList.
     */
    @SuppressWarnings("unchecked")
    public AtlasMessagesList setConversation(Conversation conversation) {
        mAdapter.setQuery(Query.builder(Message.class)
                .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conversation))
                .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                .build()).refresh();
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#setOnItemClickListener(AtlasMessagesAdapter.OnMessageClickListener)
     */
    public AtlasMessagesList setOnMessageClickListener(AtlasMessagesAdapter.OnMessageClickListener listener) {
        mAdapter.setOnItemClickListener(listener);
        return this;
    }

    public AtlasMessagesList setOnMessageSwipeListener(AtlasMessagesList.OnMessageSwipeListener listener) {
        if (mSwipeItemTouchHelper != null) {
            mSwipeItemTouchHelper.attachToRecyclerView(null);
        }
        if (listener == null) {
            mSwipeItemTouchHelper = null;
        } else {
            listener.setMessagesList(this);
            mSwipeItemTouchHelper = new ItemTouchHelper(listener);
            mSwipeItemTouchHelper.attachToRecyclerView(this);
        }
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#registerCellFactories(AtlasCellFactory...)
     */
    public AtlasMessagesList registerCellFactories(AtlasCellFactory... cellFactories) {
        mAdapter.registerCellFactories(cellFactories);
        return this;
    }

    /**
     * Convenience pass-through to this list's LinearLayoutManager.
     *
     * @see LinearLayoutManager#findLastVisibleItemPosition()
     */
    public int findLastVisibleItemPosition() {
        return mLayoutManager.findLastVisibleItemPosition();
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#setFooterView(View)
     */
    public AtlasMessagesList setFooterView(View footerView) {
        mAdapter.setFooterView(footerView);
        autoScroll();
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasMessagesAdapter.
     *
     * @see AtlasMessagesAdapter#getFooterView()
     */
    public View getFooterView() {
        return mAdapter.getFooterView();
    }

    /**
     * Scrolls if the user is at the end
     */
    private void autoScroll() {
        int end = mAdapter.getItemCount() - 1;
        if (end <= 0) return;
        int visible = findLastVisibleItemPosition();
        if (visible >= (end - 1)) scrollToPosition(end);
    }

    public void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasMessagesList, R.attr.AtlasMessageList, defStyle);
        mMyTextColor = ta.getColor(R.styleable.AtlasMessagesList_myTextColor, context.getResources().getColor(R.color.atlas_text_black));
        mMyTextStyle = ta.getInt(R.styleable.AtlasMessagesList_myTextStyle, Typeface.NORMAL);
        String myTextTypefaceName = ta.getString(R.styleable.AtlasMessagesList_myTextTypeface);
        mMyTextTypeface = myTextTypefaceName != null ? Typeface.create(myTextTypefaceName, mMyTextStyle) : null;
        //mMyTextSize = ta.getDimension(R.styleable.AtlasMessageList_myTextSize, context.getResources().getDimension(R.dimen.atlas_text_size_general));

        mOtherTextColor = ta.getColor(R.styleable.AtlasMessagesList_theirTextColor, context.getResources().getColor(R.color.atlas_text_black));
        mOtherTextStyle = ta.getInt(R.styleable.AtlasMessagesList_theirTextStyle, Typeface.NORMAL);
        String otherTextTypefaceName = ta.getString(R.styleable.AtlasMessagesList_theirTextTypeface);
        mOtherTextTypeface = otherTextTypefaceName != null ? Typeface.create(otherTextTypefaceName, mOtherTextStyle) : null;
        //mOtherTextSize = ta.getDimension(R.styleable.AtlasMessageList_theirTextSize, context.getResources().getDimension(R.dimen.atlas_text_size_general));

        mMyBubbleColor = ta.getColor(R.styleable.AtlasMessagesList_myBubbleColor, context.getResources().getColor(R.color.atlas_bubble_blue));
        mOtherBubbleColor = ta.getColor(R.styleable.AtlasMessagesList_theirBubbleColor, context.getResources().getColor(R.color.atlas_background_gray));

        mDateTextColor = ta.getColor(R.styleable.AtlasMessagesList_dateTextColor, context.getResources().getColor(R.color.atlas_text_gray));
        mAvatarTextColor = ta.getColor(R.styleable.AtlasMessagesList_avatarTextColor, context.getResources().getColor(R.color.atlas_text_black));
        mAvatarBackgroundColor = ta.getColor(R.styleable.AtlasMessagesList_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_background_gray));
        ta.recycle();
    }

    /**
     * Listens for item swipes on an AtlasConversationsList.
     */
    public static abstract class OnMessageSwipeListener extends ItemTouchHelper.SimpleCallback {
        private AtlasMessagesList mMessagesList;

        /**
         * @param directions Directions to allow swiping in.
         * @see ItemTouchHelper#LEFT
         * @see ItemTouchHelper#RIGHT
         * @see ItemTouchHelper#UP
         * @see ItemTouchHelper#DOWN
         */
        public OnMessageSwipeListener(int directions) {
            super(0, directions);
        }

        /**
         * Alerts the listener to item swipes.
         *
         * @param messagesList The AtlasMessagesList which had an item swiped.
         * @param message      The item swiped.
         * @param direction    The direction swiped.
         */
        public abstract void onMessageSwipe(AtlasMessagesList messagesList, Message message, int direction);

        @Override
        public void onSwiped(ViewHolder viewHolder, int direction) {
            if (mMessagesList == null) return;
            onMessageSwipe(mMessagesList, ((AtlasMessagesAdapter) mMessagesList.getAdapter()).getItem(viewHolder), direction);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
            return false;
        }

        protected void setMessagesList(AtlasMessagesList messagesList) {
            mMessagesList = messagesList;
        }
    }
}
