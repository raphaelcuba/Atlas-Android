/**
 Based on: https://github.com/blazsolar/FlowLayout

 Copyright 2013 Blaž Šolar

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/

package com.layer.atlas.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {
    private final static String TAG = FlowLayout.class.getSimpleName();

    private final List<List<View>> mLines = new ArrayList<List<View>>();
    private final List<Integer> mLineHeights = new ArrayList<Integer>();
    private final List<Integer> mLineMargins = new ArrayList<Integer>();

    private View mStretchChild;

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FlowLayout setStretchChild(View v) {
        mStretchChild = v;
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = getPaddingTop() + getPaddingBottom();

        int lineWidth = 0;
        int lineHeight = 0;

        int lastChildIndex = getChildCount() - 1;
        boolean lineContainsStretchChild = false;
        for (int childIndex = 0; childIndex <= lastChildIndex; childIndex++) {
            View child = getChildAt(childIndex);
            if (child == mStretchChild) lineContainsStretchChild = true;
            if (child.getVisibility() == View.GONE) {
                if (childIndex == lastChildIndex) {
                    width = Math.max(width, lineWidth);
                    height += lineHeight;
                }
                continue;
            }

            measureChildWithMargins(child, widthMeasureSpec, lineWidth, heightMeasureSpec, height);

            LayoutParams childLayoutParams = (LayoutParams) child.getLayoutParams();

            int childWidthMode = MeasureSpec.AT_MOST;
            int childWidthSize = sizeWidth;

            int childHeightMode = MeasureSpec.AT_MOST;
            int childHeightSize = sizeHeight;

            if (childLayoutParams.width == LayoutParams.MATCH_PARENT) {
                childWidthMode = MeasureSpec.EXACTLY;
                childWidthSize -= childLayoutParams.leftMargin + childLayoutParams.rightMargin;
            } else if (childLayoutParams.width >= 0) {
                childWidthMode = MeasureSpec.EXACTLY;
                childWidthSize = childLayoutParams.width;
            }

            if (childLayoutParams.height >= 0) {
                childHeightMode = MeasureSpec.EXACTLY;
                childHeightSize = childLayoutParams.height;
            } else if (modeHeight == MeasureSpec.UNSPECIFIED) {
                childHeightMode = MeasureSpec.UNSPECIFIED;
                childHeightSize = 0;
            }

            int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeightSize, childHeightMode);
            child.measure(MeasureSpec.makeMeasureSpec(childWidthSize, childWidthMode), childHeightSpec);
            int childWidth = child.getMeasuredWidth() + childLayoutParams.leftMargin + childLayoutParams.rightMargin;

            if (lineWidth + childWidth > sizeWidth) {
                // Wrap to next line
                if (lineContainsStretchChild && (child != mStretchChild)) {
                    stretchMeasuredWidth(mStretchChild, sizeWidth, lineWidth);
                    lineContainsStretchChild = false;
                }

                width = Math.max(width, lineWidth);
                lineWidth = childWidth;

                height += lineHeight;
                lineHeight = child.getMeasuredHeight() + childLayoutParams.topMargin + childLayoutParams.bottomMargin;
            } else {
                // No line wrap
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight() + childLayoutParams.topMargin + childLayoutParams.bottomMargin);
            }

            if (childIndex == lastChildIndex) {
                width = Math.max(width, lineWidth);
                height += lineHeight;
                if (lineContainsStretchChild) {
                    stretchMeasuredWidth(mStretchChild, sizeWidth, lineWidth);
                    lineContainsStretchChild = false;
                }
            }
        }

        width += getPaddingLeft() + getPaddingRight();

        setMeasuredDimension(
                (modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height);
    }

    private void stretchMeasuredWidth(View v, int lineTotalWidth, int lineCurrentWidth) {
        int remaining = lineTotalWidth - lineCurrentWidth;
        int newWidth = v.getMeasuredWidth() + remaining;
        v.measure(MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY), v.getMeasuredHeightAndState());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mLines.clear();
        mLineHeights.clear();
        mLineMargins.clear();

        int width = getWidth();

        int lineWidth = 0;
        int lineHeight = 0;
        List<View> lineViews = new ArrayList<View>();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;

            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.bottomMargin + lp.topMargin;

            if (lineWidth + childWidth > width) {
                mLineHeights.add(lineHeight);
                mLines.add(lineViews);
                mLineMargins.add(getPaddingLeft());

                lineHeight = 0;
                lineWidth = 0;
                lineViews = new ArrayList<View>();
            }

            lineWidth += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
            lineViews.add(child);
        }

        mLineHeights.add(lineHeight);
        mLines.add(lineViews);
        mLineMargins.add(getPaddingLeft());

        int numLines = mLines.size();

        int left;
        int top = getPaddingTop();

        for (int i = 0; i < numLines; i++) {
            lineHeight = mLineHeights.get(i);
            lineViews = mLines.get(i);
            left = mLineMargins.get(i);

            int children = lineViews.size();

            for (int j = 0; j < children; j++) {

                View child = lineViews.get(j);

                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                // if height is match_parent we need to remeasure child to line height
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    int childWidthMode = MeasureSpec.AT_MOST;
                    int childWidthSize = lineWidth;

                    if (lp.width == LayoutParams.MATCH_PARENT) {
                        childWidthMode = MeasureSpec.EXACTLY;
                    } else if (lp.width >= 0) {
                        childWidthMode = MeasureSpec.EXACTLY;
                        childWidthSize = lp.width;
                    }

                    child.measure(
                            MeasureSpec.makeMeasureSpec(childWidthSize, childWidthMode),
                            MeasureSpec.makeMeasureSpec(lineHeight - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY)
                    );
                }

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();

                child.layout(left + lp.leftMargin,
                        top + lp.topMargin,
                        left + childWidth + lp.leftMargin,
                        top + childHeight + lp.topMargin);

                left += childWidth + lp.leftMargin + lp.rightMargin;
            }
            top += lineHeight;
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p) && p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

}