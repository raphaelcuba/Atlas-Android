package com.layer.atlas;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.layer.atlas.adapters.AtlasBaseAdapter;
import com.layer.sdk.query.Queryable;


public class AtlasSwipeableItem extends FrameLayout {
    private static final int[] STATES_SWIPING_ACTIVE = {R.attr.state_swiping};

    private boolean mSwipingActive = false;

    public AtlasSwipeableItem(Context context) {
        super(context);
    }

    public AtlasSwipeableItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AtlasSwipeableItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (mSwipingActive) {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + STATES_SWIPING_ACTIVE.length);
            mergeDrawableStates(drawableState, STATES_SWIPING_ACTIVE);
            return drawableState;
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }

    public AtlasSwipeableItem setSwipingActive(boolean swipingActive) {
        if (mSwipingActive == swipingActive) return this;
        mSwipingActive = swipingActive;
        refreshDrawableState();
        return this;
    }

    /**
     * Listens for item swipes on an AtlasConversationsList.
     */
    public static abstract class OnSwipeListener<Tquery extends Queryable> extends ItemTouchHelper.SimpleCallback {
        final float SWIPE_RATIO = 0.33f;
        final float DELETE_RATIO = 0.67f;
        final float MIN_ALPHA = 0.2f;
        final float MAX_ALPHA = 1f;
        private AtlasBaseAdapter<Tquery> mAdapter;

        public OnSwipeListener() {
            super(0, ItemTouchHelper.RIGHT);
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return DELETE_RATIO;
        }

        /**
         * Alerts the listener to item swipes.
         *
         * @param item The item swiped.
         */
        public abstract void onSwipe(Tquery item, int direction);

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if (mAdapter == null) return;
            onSwipe(mAdapter.getItem(viewHolder), direction);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            View swipeable = viewHolder.itemView.findViewById(R.id.swipeable);
            if (swipeable == null) {
                super.clearView(recyclerView, viewHolder);
                return;
            }
            swipeable.setTranslationX(0);
            ((AtlasSwipeableItem) viewHolder.itemView).setSwipingActive(false);
            View leavebehind = viewHolder.itemView.findViewById(R.id.leavebehind);
            if (leavebehind == null) return;
            leavebehind.setVisibility(GONE);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            float maxSwipeDelta = viewHolder.itemView.getWidth() * SWIPE_RATIO;
            float maxDeleteDelta = viewHolder.itemView.getWidth() * DELETE_RATIO;

            float swipeDelta = Math.min(Math.abs(dX), maxSwipeDelta);
            float deleteDelta = Math.min(Math.abs(dX), maxDeleteDelta);

            final float dir = Math.signum(dX);

            View swipeable = viewHolder.itemView.findViewById(R.id.swipeable);
            if (swipeable == null) {
                super.onChildDraw(c, recyclerView, viewHolder, dir * swipeDelta, dY, actionState, isCurrentlyActive);
                return;
            }

            // Backgrounds
            ((AtlasSwipeableItem) viewHolder.itemView).setSwipingActive(dir != 0 || isCurrentlyActive);

            if (dir == 0) {
                swipeable.setTranslationX(0);
            } else {
                swipeable.setTranslationX(dir * swipeDelta);
            }

            View leavebehind = viewHolder.itemView.findViewById(R.id.leavebehind);
            if (leavebehind == null) return;

            if (dir == 0) {
                leavebehind.setVisibility(GONE);
            } else {
                leavebehind.setVisibility(VISIBLE);
                float alpha = MIN_ALPHA + ((MAX_ALPHA - MIN_ALPHA) * deleteDelta / maxDeleteDelta);
                leavebehind.setAlpha(alpha);
            }
        }

        protected void setBaseAdapter(AtlasBaseAdapter<Tquery> adapter) {
            mAdapter = adapter;
        }
    }
}
