package com.layer.atlas.typingindicators;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.layer.atlas.AtlasTypingIndicator;
import com.layer.atlas.R;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BubbleTypingIndicatorFactory implements AtlasTypingIndicator.TypingIndicatorFactory {
    private static final String TAG = BubbleTypingIndicatorFactory.class.getSimpleName();

    private static final int DOT_RES_ID = R.drawable.atlas_typing_indicator_dot;
    private static final float DOT_ON_ALPHA = 0.31f;
    private static final long ANIMATION_PERIOD = 600;
    private static final long ANIMATION_OFFSET = ANIMATION_PERIOD / 3;

    private View sDot1;
    private View sDot2;
    private View sDot3;

    private Set<String> mLastTypists = new HashSet<String>();

    @Override
    public View onCreateView(Context context) {
        Resources r = context.getResources();

        int minWidth = r.getDimensionPixelSize(R.dimen.atlas_message_item_cell_min_width);
        int minHeight = r.getDimensionPixelSize(R.dimen.atlas_message_item_cell_min_height);
        int dotSize = r.getDimensionPixelSize(R.dimen.atlas_typing_indicator_dot_size);
        int dotSpace = r.getDimensionPixelSize(R.dimen.atlas_typing_indicator_dot_space);

        LinearLayout l = new LinearLayout(context);
        l.setMinimumWidth(minWidth);
        l.setMinimumHeight(minHeight);
        l.setGravity(Gravity.CENTER);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutParams(new AtlasTypingIndicator.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        l.setBackgroundDrawable(r.getDrawable(R.drawable.atlas_message_item_cell_them));

        ImageView v;
        LinearLayout.LayoutParams vp;

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        vp.setMargins(0, 0, dotSpace, 0);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        sDot1 = v;

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        vp.setMargins(0, 0, dotSpace, 0);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        sDot2 = v;

        v = new ImageView(context);
        vp = new LinearLayout.LayoutParams(dotSize, dotSize);
        v.setLayoutParams(vp);
        v.setBackgroundDrawable(r.getDrawable(DOT_RES_ID));
        sDot3 = v;

        l.addView(sDot1);
        l.addView(sDot2);
        l.addView(sDot3);

        return l;
    }

    @Override
    public void onBindView(AtlasTypingIndicator indicator, Map<String, LayerTypingIndicatorListener.TypingIndicator> typingUserIds) {
        // If an animation is running, just pay attention to the set of active typists, not their typing state.
        if (sDot1.getAnimation() != null && equalSets(mLastTypists, typingUserIds.keySet())) return;
        mLastTypists.clear();
        mLastTypists.addAll(typingUserIds.keySet());

        // Initialize dot animations.
        sDot1.setAlpha(DOT_ON_ALPHA);
        sDot2.setAlpha(DOT_ON_ALPHA);
        sDot3.setAlpha(DOT_ON_ALPHA);
        startAnimation(sDot1, ANIMATION_PERIOD, 0);
        startAnimation(sDot2, ANIMATION_PERIOD, ANIMATION_OFFSET);
        startAnimation(sDot3, ANIMATION_PERIOD, ANIMATION_OFFSET + ANIMATION_OFFSET);
    }

    /**
     * Starts a repeating fade out / fade in with the given period and offset in milliseconds.
     *
     * @param view        View to start animating.
     * @param period      Length of time in milliseconds for the fade out / fade in period.
     * @param startOffset Length of time in milliseconds to delay the initial start.
     */
    private void startAnimation(final View view, long period, long startOffset) {
        final AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(period / 2);
        fadeOut.setStartOffset(startOffset);
        fadeOut.setInterpolator(COSINE_INTERPOLATOR);

        final AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(period / 2);
        fadeIn.setStartOffset(0);
        fadeIn.setInterpolator(COSINE_INTERPOLATOR);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeIn.setStartOffset(0);
                fadeIn.reset();
                view.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeOut.setStartOffset(0);
                fadeOut.reset();
                view.startAnimation(fadeOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.startAnimation(fadeOut);
    }

    /**
     * Ease in/out
     */
    private final Interpolator COSINE_INTERPOLATOR = new Interpolator() {
        @Override
        public float getInterpolation(float input) {
            return (float) (1.0 - Math.cos(input * Math.PI / 2.0));
        }
    };

    private static boolean equalSets(Set<String> a, Set<String> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (String v : a) {
            if (!b.contains(v)) return false;
        }
        return true;
    }
}
