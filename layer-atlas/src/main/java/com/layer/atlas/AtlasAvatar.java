package com.layer.atlas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.utilities.Utils;
import com.layer.atlas.utilities.picasso.transformations.CircleTransform;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class AtlasAvatar extends View {
    public static final String TAG = AtlasAvatar.class.getSimpleName();

    private final static CircleTransform SINGLE_TRANSFORM = new CircleTransform(TAG + ".single");
    private final static CircleTransform MULTI_TRANSFORM = new CircleTransform(TAG + ".multi");

    private static final Paint PAINT_TRANSPARENT = new Paint();
    private static final Paint PAINT_BITMAP = new Paint();

    private final Paint mPaintInitials = new Paint();
    private final Paint mPaintBorder = new Paint();
    private final Paint mPaintBackground = new Paint();

    // TODO: make these styleable
    private static final int MAX_AVATARS = 3;
    private static final float BORDER_SIZE_DP = 1f;
    private static final float MULTI_FRACTION = 26f / 40f;

    static {
        PAINT_TRANSPARENT.setARGB(0, 255, 255, 255);
        PAINT_TRANSPARENT.setAntiAlias(true);

        PAINT_BITMAP.setARGB(255, 255, 255, 255);
        PAINT_BITMAP.setAntiAlias(true);
    }

    {
        mPaintInitials.setAntiAlias(true);
        mPaintInitials.setSubpixelText(true);
        mPaintBorder.setAntiAlias(true);
        mPaintBackground.setAntiAlias(true);
    }

    private ParticipantProvider mParticipantProvider;
    private Picasso mPicasso;

    // Initials and Picasso image targets by user ID
    private final Object mLock = new Object();
    private final Map<String, ImageTarget> mImageTargets = new HashMap<String, ImageTarget>();
    private final Map<String, String> mInitials = new HashMap<String, String>();
    private final List<ImageTarget> mPendingLoads = new ArrayList<ImageTarget>();

    // Sizing set in setClusterSizes() and used in onDraw()
    private float mOuterRadius;
    private float mInnerRadius;
    private float mCenterX;
    private float mCenterY;
    private float mDeltaX;
    private float mDeltaY;
    private float mTextSize;

    private Rect mRect = new Rect();
    private RectF mContentRect = new RectF();

    public AtlasAvatar(Context context) {
        super(context);
        mPaintBackground.setColor(context.getResources().getColor(R.color.atlas_avatar_background));
        mPaintBorder.setColor(context.getResources().getColor(R.color.atlas_avatar_border));
        mPaintInitials.setColor(context.getResources().getColor(R.color.atlas_avatar_text));
    }

    public AtlasAvatar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaintBackground.setColor(context.getResources().getColor(R.color.atlas_avatar_background));
        mPaintBorder.setColor(context.getResources().getColor(R.color.atlas_avatar_border));
        mPaintInitials.setColor(context.getResources().getColor(R.color.atlas_avatar_text));
    }

    public AtlasAvatar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaintBackground.setColor(context.getResources().getColor(R.color.atlas_avatar_background));
        mPaintBorder.setColor(context.getResources().getColor(R.color.atlas_avatar_border));
        mPaintInitials.setColor(context.getResources().getColor(R.color.atlas_avatar_text));
    }

    public AtlasAvatar init(ParticipantProvider participantProvider, Picasso picasso) {
        mParticipantProvider = participantProvider;
        mPicasso = picasso;
        return this;
    }

    public AtlasAvatar setParticipants(String... participantIds) {
        LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
        for (String participantId : participantIds) {
            hashSet.add(participantId);
        }
        return setParticipants(hashSet);
    }

    public AtlasAvatar setParticipants(Set<String> participantIds) {
        // Limit to MAX_AVATARS valid avatars, prioritizing participants with avatars.
        if (participantIds.size() > MAX_AVATARS) {
            Queue<String> withAvatars = new LinkedList<String>();
            Queue<String> withoutAvatars = new LinkedList<String>();
            for (String participantId : participantIds) {
                Participant participant = mParticipantProvider.getParticipant(participantId);
                if (participant == null) continue;
                if (participant.getAvatarUrl() != null) {
                    withAvatars.add(participantId);
                } else {
                    withoutAvatars.add(participantId);
                }
            }

            participantIds = new LinkedHashSet<String>();
            int numWithout = Math.min(MAX_AVATARS - withAvatars.size(), withoutAvatars.size());
            for (int i = 0; i < numWithout; i++) {
                participantIds.add(withoutAvatars.remove());
            }
            int numWith = Math.min(MAX_AVATARS, withAvatars.size());
            for (int i = 0; i < numWith; i++) {
                participantIds.add(withAvatars.remove());
            }
        }

        synchronized (mLock) {
            Diff diff = diff(mInitials.keySet(), participantIds);
            List<ImageTarget> toLoad = new ArrayList<ImageTarget>(participantIds.size());

            List<ImageTarget> recyclableTargets = new ArrayList<ImageTarget>();
            for (String removed : diff.removed) {
                mInitials.remove(removed);
                ImageTarget target = mImageTargets.remove(removed);
                if (target != null) {
                    mPicasso.cancelRequest(target);
                    recyclableTargets.add(target);
                }
            }

            for (String added : diff.added) {
                Participant participant = mParticipantProvider.getParticipant(added);
                if (participant == null) continue;
                mInitials.put(added, Utils.getInitials(participant));

                final ImageTarget target;
                if (recyclableTargets.isEmpty()) {
                    target = new ImageTarget(this);
                } else {
                    target = recyclableTargets.remove(0);
                }
                target.setUrl(participant.getAvatarUrl());
                mImageTargets.put(added, target);
                toLoad.add(target);
            }

            // Cancel existing in case the size or anything else changed.
            // TODO: make caching intelligent wrt sizing
            for (String existing : diff.existing) {
                Participant participant = mParticipantProvider.getParticipant(existing);
                if (participant == null) continue;
                ImageTarget existingTarget = mImageTargets.get(existing);
                mPicasso.cancelRequest(existingTarget);
                toLoad.add(existingTarget);
            }
            for (ImageTarget target : mPendingLoads) {
                mPicasso.cancelRequest(target);
            }
            mPendingLoads.clear();
            mPendingLoads.addAll(toLoad);

            setClusterSizes();
            return this;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!changed) return;
        setClusterSizes();
    }

    private boolean setClusterSizes() {
        synchronized (mLock) {
            int avatarCount = mInitials.size();
            if (avatarCount == 0) return false;
            ViewGroup.LayoutParams params = getLayoutParams();
            if (params == null) return false;
            boolean hasBorder = (avatarCount != 1);

            int drawableWidth = params.width - (getPaddingLeft() + getPaddingRight());
            int drawableHeight = params.height - (getPaddingTop() + getPaddingBottom());
            float dimension = Math.min(drawableWidth, drawableHeight);
            float density = getContext().getResources().getDisplayMetrics().density;
            float fraction = (avatarCount > 1) ? MULTI_FRACTION : 1;

            mOuterRadius = fraction * dimension / 2f;
            mInnerRadius = mOuterRadius - (density * BORDER_SIZE_DP);
            mTextSize = mInnerRadius * 4f / 5f;
            mCenterX = getPaddingLeft() + mOuterRadius;
            mCenterY = getPaddingTop() + mOuterRadius;

            float outerMultiSize = fraction * dimension;
            mDeltaX = (drawableWidth - outerMultiSize) / (avatarCount - 1);
            mDeltaY = (drawableHeight - outerMultiSize) / (avatarCount - 1);

            synchronized (mPendingLoads) {
                if (!mPendingLoads.isEmpty()) {
                    int size = Math.round(hasBorder ? (mInnerRadius * 2f) : (mOuterRadius * 2f));
                    for (ImageTarget imageTarget : mPendingLoads) {
                        mPicasso.load(imageTarget.getUrl())
                                .tag(AtlasAvatar.TAG).noPlaceholder().noFade()
                                .centerCrop().resize(size, size)
                                .transform((avatarCount > 1) ? MULTI_TRANSFORM : SINGLE_TRANSFORM)
                                .into(imageTarget);
                    }
                    mPendingLoads.clear();
                }
            }
            return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (mLock) {
            // Clear canvas
            int avatarCount = mInitials.size();
            canvas.drawRect(0f, 0f, canvas.getWidth(), canvas.getHeight(), PAINT_TRANSPARENT);
            if (avatarCount == 0) return;
            boolean hasBorder = (avatarCount != 1);
            float contentRadius = hasBorder ? mInnerRadius : mOuterRadius;

            // Draw avatar cluster
            float cx = mCenterX;
            float cy = mCenterY;
            mContentRect.set(cx - contentRadius, cy - contentRadius, cx + contentRadius, cy + contentRadius);
            for (Map.Entry<String, String> entry : mInitials.entrySet()) {
                // Border / background
                if (hasBorder) canvas.drawCircle(cx, cy, mOuterRadius, mPaintBorder);

                // Initials or bitmap
                ImageTarget imageTarget = mImageTargets.get(entry.getKey());
                Bitmap bitmap = (imageTarget == null) ? null : imageTarget.getBitmap();
                if (bitmap == null) {
                    String initials = entry.getValue();
                    mPaintInitials.setTextSize(mTextSize);
                    mPaintInitials.getTextBounds(initials, 0, initials.length(), mRect);
                    canvas.drawCircle(cx, cy, contentRadius, mPaintBackground);
                    canvas.drawText(initials, cx - mRect.centerX(), cy - mRect.centerY() - 1f, mPaintInitials);
                } else {
                    canvas.drawBitmap(bitmap, mContentRect.left, mContentRect.top, PAINT_BITMAP);
                }

                // Translate for next avatar
                cx += mDeltaX;
                cy += mDeltaY;
                mContentRect.offset(mDeltaX, mDeltaY);
            }
        }
    }

    private static class ImageTarget implements Target {
        private final static AtomicLong sCounter = new AtomicLong(0);
        private final long mId;
        private final AtlasAvatar mCluster;
        private String mUrl;
        private Bitmap mBitmap;

        public ImageTarget(AtlasAvatar cluster) {
            mId = sCounter.incrementAndGet();
            mCluster = cluster;
        }

        public ImageTarget setUrl(String url) {
            mUrl = url;
            return this;
        }

        public String getUrl() {
            return mUrl;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mCluster.invalidate();
            mBitmap = bitmap;
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            mCluster.invalidate();
            mBitmap = null;
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            mBitmap = null;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageTarget target = (ImageTarget) o;
            return mId == target.mId;
        }

        @Override
        public int hashCode() {
            return (int) (mId ^ (mId >>> 32));
        }
    }

    private static Diff diff(Set<String> oldSet, Set<String> newSet) {
        Diff diff = new Diff();
        for (String old : oldSet) {
            if (newSet.contains(old)) {
                diff.existing.add(old);
            } else {
                diff.removed.add(old);
            }
        }
        for (String newItem : newSet) {
            if (!oldSet.contains(newItem)) {
                diff.added.add(newItem);
            }
        }
        return diff;
    }

    private static class Diff {
        public List<String> existing = new ArrayList<String>();
        public List<String> added = new ArrayList<String>();
        public List<String> removed = new ArrayList<String>();
    }
}
