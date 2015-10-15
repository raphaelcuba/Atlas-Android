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
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import com.layer.atlas.ParticipantProvider;
import com.layer.atlas.R;
import com.layer.atlas.simple.messagesenders.AttachmentSender;
import com.layer.atlas.simple.messagesenders.TextSender;
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

    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;
    private Conversation mConversation;

    private TextSender mTextSender;
    private ArrayList<AttachmentSender> mAttachmentSenders = new ArrayList<AttachmentSender>();

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
     */
    public AtlasMessageComposer init(LayerClient layerClient, ParticipantProvider participantProvider) {
        LayoutInflater.from(getContext()).inflate(R.layout.old_atlas_message_composer, this);

        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;

        mAttachButton = (ImageView) findViewById(R.id.atlas_message_composer_upload);
        mAttachButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final PopupWindow popupWindow = new PopupWindow(v.getContext());
                popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                LinearLayout menu = (LinearLayout) inflater.inflate(R.layout.old_atlas_view_message_composer_menu, null);
                popupWindow.setContentView(menu);

                for (AttachmentSender sender : mAttachmentSenders) {
                    View itemConvert = inflater.inflate(R.layout.old_atlas_view_message_composer_menu_convert, menu, false);
                    TextView titleView = ((TextView) itemConvert.findViewById(R.id.altas_view_message_composer_convert_title));
                    titleView.setText(sender.getTitle());
                    itemConvert.setTag(sender);
                    itemConvert.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            popupWindow.dismiss();
                            ((AttachmentSender) v.getTag()).send();
                        }
                    });
                    if (sender.getIcon() != null) {
                        ImageView iconView = ((ImageView) itemConvert.findViewById(R.id.altas_view_message_composer_convert_icon));
                        iconView.setImageResource(sender.getIcon());
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
                if (!mTextSender.send(mMessageText.getText().toString())) return;
                mMessageText.setText("");
                mSendButton.setEnabled(false);
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

    public AtlasMessageComposer setTextSender(TextSender textSender) {
        mTextSender = textSender;
        mTextSender.init(this.getContext().getApplicationContext(), mLayerClient, mParticipantProvider);
        mTextSender.setConversation(mConversation);
        return this;
    }

    public AtlasMessageComposer registerAttachmentSender(AttachmentSender sender) {
        if (sender.getTitle() == null && sender.getIcon() == null) {
            throw new NullPointerException("Attachment handlers must have at least a title or icon specified.");
        }
        sender.init(this.getContext().getApplicationContext(), mLayerClient, mParticipantProvider);
        sender.setConversation(mConversation);
        mAttachmentSenders.add(sender);
        mAttachButton.setVisibility(View.VISIBLE);
        return this;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    public AtlasMessageComposer setConversation(Conversation conversation) {
        mConversation = conversation;
        if (mTextSender != null) mTextSender.setConversation(conversation);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.setConversation(conversation);
        }
        return this;
    }

    public AtlasMessageComposer onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) return this;
        mTextSender.onActivityCreate(savedInstanceState);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.onActivityCreate(savedInstanceState);
        }
        return this;
    }

    public AtlasMessageComposer onSaveInstanceState(Bundle outState) {
        mTextSender.onActivitySaveInstanceState(outState);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.onActivitySaveInstanceState(outState);
        }
        return this;
    }

    public AtlasMessageComposer onActivityResult(int requestCode, int resultCode, Intent data) {
        mTextSender.onActivityResult(requestCode, resultCode, data);
        for (AttachmentSender sender : mAttachmentSenders) {
            sender.onActivityResult(requestCode, resultCode, data);
        }
        return this;
    }
}
