/**
 * Based on: https://github.com/blazsolar/FlowLayout
 *
 * Copyright 2013 Blaž Šolar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.layer.atlas.utilities.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class MaxHeightScrollView extends ScrollView {
    private final static String TAG = MaxHeightScrollView.class.getSimpleName();

    private int mMaxHeight;

    public MaxHeightScrollView(Context context) {
        super(context);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MaxHeightScrollView setMaximumHeight(int height) {
        mMaxHeight = height;
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}