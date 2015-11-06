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
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

import com.layer.atlas.adapters.AtlasConversationsAdapter;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.squareup.picasso.Picasso;

public class AtlasConversationsList extends RecyclerView {
    private static final String TAG = AtlasConversationsList.class.getSimpleName();

    AtlasConversationsAdapter mAdapter;
    private final Style mStyle;
    private ItemTouchHelper mSwipeItemTouchHelper;

    public AtlasConversationsList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mStyle = new Style(context, attrs, defStyle);
    }

    public AtlasConversationsList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasConversationsList(Context context) {
        super(context);
        mStyle = new Style(context, null, 0);
    }

    public AtlasConversationsList init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        manager.setStackFromEnd(false);
        setLayoutManager(manager);

        mAdapter = new AtlasConversationsAdapter(getContext(), layerClient, participantProvider, picasso);
        super.setAdapter(mAdapter);

        return this;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        throw new RuntimeException("AtlasConversationList sets its own Adapter");
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
     * Convenience pass-through to this list's AtlasConversationsAdapter.
     *
     * @see AtlasConversationsAdapter#setOnConversationClickListener(AtlasConversationsAdapter.OnConversationClickListener)
     */
    public AtlasConversationsList setOnConversationClickListener(AtlasConversationsAdapter.OnConversationClickListener listener) {
        mAdapter.setOnConversationClickListener(listener);
        return this;
    }

    public AtlasConversationsList setOnConversationSwipeListener(AtlasSwipeableItem.OnSwipeListener<Conversation> listener) {
        if (mSwipeItemTouchHelper != null) {
            mSwipeItemTouchHelper.attachToRecyclerView(null);
        }
        if (listener == null) {
            mSwipeItemTouchHelper = null;
        } else {
            listener.setBaseAdapter((AtlasConversationsAdapter) getAdapter());
            mSwipeItemTouchHelper = new ItemTouchHelper(listener);
            mSwipeItemTouchHelper.attachToRecyclerView(this);
        }
        return this;
    }

    /**
     * Convenience pass-through to this list's AtlasConversationsAdapter.
     *
     * @see AtlasConversationsAdapter#setInitialMessageHistory(long)
     */
    public AtlasConversationsList setInitialHistory(long initialHistory) {
        mAdapter.setInitialMessageHistory(initialHistory);
        return this;
    }

    public Style getStyle() {
        return mStyle;
    }

    public static class Style {
        private final int mTitleTextColor;
        private final int mTitleTextStyle;
        private final Typeface mTitleTextTypeface;
        private final int mTitleUnreadTextColor;
        private final int mTitleUnreadTextStyle;
        private final Typeface mTitleUnreadTextTypeface;
        private final int mSubtitleTextColor;
        private final int mSubtitleTextStyle;
        private final Typeface mSubtitleTextTypeface;
        private final int mSubtitleUnreadTextColor;
        private final int mSubtitleUnreadTextStyle;
        private final Typeface mSubtitleUnreadTextTypeface;
        private final int mCellBackgroundColor;
        private final int mCellUnreadBackgroundColor;
        private final int mDateTextColor;
        private final int mAvatarTextColor;
        private final int mAvatarBackgroundColor;

        public Style(Context context, AttributeSet attrs, int defStyle) {
            TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationsList, R.attr.AtlasConversationList, defStyle);
            mTitleTextColor = ta.getColor(R.styleable.AtlasConversationsList_cellTitleTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mTitleTextStyle = ta.getInt(R.styleable.AtlasConversationsList_cellTitleTextStyle, Typeface.NORMAL);
            String titleTextTypefaceName = ta.getString(R.styleable.AtlasConversationsList_cellTitleTextTypeface);
            mTitleTextTypeface = titleTextTypefaceName != null ? Typeface.create(titleTextTypefaceName, mTitleTextStyle) : null;

            mTitleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationsList_cellTitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mTitleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationsList_cellTitleUnreadTextStyle, Typeface.BOLD);
            String titleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationsList_cellTitleUnreadTextTypeface);
            mTitleUnreadTextTypeface = titleUnreadTextTypefaceName != null ? Typeface.create(titleUnreadTextTypefaceName, mTitleUnreadTextStyle) : null;

            mSubtitleTextColor = ta.getColor(R.styleable.AtlasConversationsList_cellSubtitleTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mSubtitleTextStyle = ta.getInt(R.styleable.AtlasConversationsList_cellSubtitleTextStyle, Typeface.NORMAL);
            String subtitleTextTypefaceName = ta.getString(R.styleable.AtlasConversationsList_cellSubtitleTextTypeface);
            mSubtitleTextTypeface = subtitleTextTypefaceName != null ? Typeface.create(subtitleTextTypefaceName, mSubtitleTextStyle) : null;

            mSubtitleUnreadTextColor = ta.getColor(R.styleable.AtlasConversationsList_cellSubtitleUnreadTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mSubtitleUnreadTextStyle = ta.getInt(R.styleable.AtlasConversationsList_cellSubtitleUnreadTextStyle, Typeface.NORMAL);
            String subtitleUnreadTextTypefaceName = ta.getString(R.styleable.AtlasConversationsList_cellSubtitleUnreadTextTypeface);
            mSubtitleUnreadTextTypeface = subtitleUnreadTextTypefaceName != null ? Typeface.create(subtitleUnreadTextTypefaceName, mSubtitleUnreadTextStyle) : null;

            mCellBackgroundColor = ta.getColor(R.styleable.AtlasConversationsList_cellBackgroundColor, Color.TRANSPARENT);
            mCellUnreadBackgroundColor = ta.getColor(R.styleable.AtlasConversationsList_cellUnreadBackgroundColor, Color.TRANSPARENT);
            mDateTextColor = ta.getColor(R.styleable.AtlasConversationsList_dateTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mAvatarTextColor = ta.getColor(R.styleable.AtlasConversationsList_avatarTextColor, context.getResources().getColor(R.color.atlas_text_black));
            mAvatarBackgroundColor = ta.getColor(R.styleable.AtlasConversationsList_avatarBackgroundColor, context.getResources().getColor(R.color.atlas_avatar_background));

            ta.recycle();
        }

        public int getTitleTextColor() {
            return mTitleTextColor;
        }

        public int getTitleTextStyle() {
            return mTitleTextStyle;
        }

        public Typeface getTitleTextTypeface() {
            return mTitleTextTypeface;
        }

        public int getTitleUnreadTextColor() {
            return mTitleUnreadTextColor;
        }

        public int getTitleUnreadTextStyle() {
            return mTitleUnreadTextStyle;
        }

        public Typeface getTitleUnreadTextTypeface() {
            return mTitleUnreadTextTypeface;
        }

        public int getSubtitleTextColor() {
            return mSubtitleTextColor;
        }

        public int getSubtitleTextStyle() {
            return mSubtitleTextStyle;
        }

        public Typeface getSubtitleTextTypeface() {
            return mSubtitleTextTypeface;
        }

        public int getSubtitleUnreadTextColor() {
            return mSubtitleUnreadTextColor;
        }

        public int getSubtitleUnreadTextStyle() {
            return mSubtitleUnreadTextStyle;
        }

        public Typeface getSubtitleUnreadTextTypeface() {
            return mSubtitleUnreadTextTypeface;
        }

        public int getCellBackgroundColor() {
            return mCellBackgroundColor;
        }

        public int getCellUnreadBackgroundColor() {
            return mCellUnreadBackgroundColor;
        }

        public int getDateTextColor() {
            return mDateTextColor;
        }

        public int getAvatarTextColor() {
            return mAvatarTextColor;
        }

        public int getAvatarBackgroundColor() {
            return mAvatarBackgroundColor;
        }
    }
}
