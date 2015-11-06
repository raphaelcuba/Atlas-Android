package com.layer.atlas;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

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
}
