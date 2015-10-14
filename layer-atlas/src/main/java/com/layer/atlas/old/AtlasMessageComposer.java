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
package com.layer.atlas.old;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerTypingIndicatorListener;
import com.layer.sdk.messaging.Conversation;

import java.util.ArrayList;

public class AtlasMessageComposer extends FrameLayout {
    private static final String TAG = AtlasMessageComposer.class.getSimpleName();

    private EditText mMessageText;
    private Button mSendButton;
    private ImageView mAttachButton;

    private OnSendClickListener mSendListener;
    private Conversation mConversation;
    private LayerClient mLayerClient;

    private ArrayList<AttachmentHandler> mAttachmentHandlers = new ArrayList<AttachmentHandler>();

    // styles
    private int mTextColor;
    private float mTextSize;
    private Typeface mTypeface;
    private int mTextStyle;
    private boolean mEnabled;

    public AtlasMessageComposer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
    }

    public AtlasMessageComposer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasMessageComposer(Context context) {
        super(context);
    }

    public void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasMessageComposer, R.attr.AtlasMessageComposer, defStyle);
        mTextColor = ta.getColor(R.styleable.AtlasMessageComposer_composerTextColor, context.getResources().getColor(R.color.atlas_text_black));
        //this.mTextSize  = ta.getDimension(R.styleable.AtlasMessageComposer_composerTextSize, context.getResources().getDimension(R.dimen.atlas_text_size_general));
        mTextStyle = ta.getInt(R.styleable.AtlasMessageComposer_composerTextStyle, Typeface.NORMAL);
        String typeFaceName = ta.getString(R.styleable.AtlasMessageComposer_composerTextTypeface);
        mTypeface = typeFaceName != null ? Typeface.create(typeFaceName, mTextStyle) : null;
        mEnabled = ta.getBoolean(R.styleable.AtlasMessageComposer_android_enabled, true);
        ta.recycle();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mAttachButton != null) mAttachButton.setEnabled(enabled);
        if (mMessageText != null) mMessageText.setEnabled(enabled);
        if (mSendButton != null) {
            mSendButton.setEnabled(enabled && (mMessageText != null) && (mMessageText.getText().length() > 0));
        }
        super.setEnabled(enabled);
    }

    /**
     * Initialization is required to engage MessageComposer with LayerClient.
     *
     * @param client - must be not null
     */
    public AtlasMessageComposer init(LayerClient client) {
        if (client == null) {
            throw new IllegalArgumentException("LayerClient cannot be null");
        }
        if (mMessageText != null) {
            throw new IllegalStateException("AtlasMessageComposer is already initialized!");
        }

        mLayerClient = client;

        LayoutInflater.from(getContext()).inflate(R.layout.old_atlas_message_composer, this);

        mAttachButton = (ImageView) findViewById(R.id.atlas_message_composer_upload);
        mAttachButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final PopupWindow popupWindow = new PopupWindow(v.getContext());
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                LinearLayout menu = (LinearLayout) inflater.inflate(R.layout.old_atlas_view_message_composer_menu, null);
                popupWindow.setContentView(menu);

                for (AttachmentHandler item : mAttachmentHandlers) {
                    View itemConvert = inflater.inflate(R.layout.old_atlas_view_message_composer_menu_convert, menu, false);
                    TextView titleView = ((TextView) itemConvert.findViewById(R.id.altas_view_message_composer_convert_title));
                    titleView.setText(item.mTitle);
                    itemConvert.setTag(item);
                    itemConvert.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            popupWindow.dismiss();
                            AttachmentHandler item = (AttachmentHandler) v.getTag();
                            if (item.mClickListener != null) {
                                item.mClickListener.onClick(v);
                            }
                        }
                    });
                    if (item.mIcon != null) {
                        ImageView iconView = ((ImageView) itemConvert.findViewById(R.id.altas_view_message_composer_convert_icon));
                        iconView.setImageResource(item.mIcon);
                        iconView.setVisibility(VISIBLE);
                        Drawable d = DrawableCompat.wrap(iconView.getDrawable());
                        DrawableCompat.setTint(d, R.color.atlas_background_gray);
                    }
                    menu.addView(itemConvert);
                }
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popupWindow.setOutsideTouchable(true);
                int[] viewXYWindow = new int[2];
                v.getLocationInWindow(viewXYWindow);

                menu.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int menuHeight = menu.getMeasuredHeight();
                popupWindow.showAtLocation(v, Gravity.NO_GRAVITY, viewXYWindow[0], viewXYWindow[1] - menuHeight);
            }
        });

        mMessageText = (EditText) findViewById(R.id.atlas_message_composer_text);
        mMessageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mConversation == null || mConversation.isDeleted()) return;
                try {
                    if (s.length() > 0) {
                        mSendButton.setEnabled(isEnabled());
                        mConversation.send(LayerTypingIndicatorListener.TypingIndicator.STARTED);
                    } else {
                        mSendButton.setEnabled(false);
                        mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
                    }
                } catch (LayerException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        mSendButton = (Button) findViewById(R.id.atlas_message_composer_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String text = mMessageText.getText().toString();
                if (mSendListener == null) {
                    Log.w(TAG, "No OnSendListener registered");
                }
                if (mSendListener.onSendClick(AtlasMessageComposer.this, mConversation, text)) {
                    mConversation.send(LayerTypingIndicatorListener.TypingIndicator.FINISHED);
                    mMessageText.setText("");
                    mSendButton.setEnabled(false);
                }
            }
        });
        applyStyle();
        return this;
    }

    private void applyStyle() {
        //mMessageText.setTextSize(mTextSize);
        mMessageText.setTypeface(mTypeface, mTextStyle);
        mMessageText.setTextColor(mTextColor);
        setEnabled(mEnabled);

        ColorStateList list = getResources().getColorStateList(R.color.atlas_message_composer_attach_button);
        Drawable d = DrawableCompat.wrap(mAttachButton.getDrawable().mutate());
        DrawableCompat.setTintList(d, list);
        mAttachButton.setImageDrawable(d);
    }

    public AtlasMessageComposer registerAttachmentHandler(String title, Integer icon, OnClickListener clickListener) {
        if (title == null && icon == null) {
            throw new NullPointerException("Attachment handlers must have at least a title or icon specified.");
        }
        AttachmentHandler handler = new AttachmentHandler();
        handler.mTitle = title;
        handler.mIcon = icon;
        handler.mClickListener = clickListener;
        mAttachmentHandlers.add(handler);
        mAttachButton.setVisibility(View.VISIBLE);
        return this;
    }

    public AtlasMessageComposer setOnSendListener(OnSendClickListener listener) {
        mSendListener = listener;
        return this;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    public AtlasMessageComposer setConversation(Conversation conv) {
        mConversation = conv;
        return this;
    }

    public interface OnSendClickListener {
        boolean onSendClick(AtlasMessageComposer messageComposer, Conversation conversation, String text);
    }

    private static class AttachmentHandler {
        String mTitle;
        Integer mIcon;
        OnClickListener mClickListener;
    }
}
