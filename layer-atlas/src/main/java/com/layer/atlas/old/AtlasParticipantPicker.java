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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.Participant;
import com.layer.atlas.ParticipantProvider;
import com.layer.atlas.R;
import com.layer.atlas.old.Utils.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author Oleg Orlov
 * @since 27 Apr 2015
 */
public class AtlasParticipantPicker extends FrameLayout {

    private static final String TAG = AtlasParticipantPicker.class.getSimpleName();
    private static final boolean debug = false;

    // participants picker
    private View rootView;
    private EditText textFilter;
    private ListView participantsList;
    private ViewGroup selectedParticipantsContainer;

    private ParticipantProvider participantProvider;

    private TreeSet<String> skipUserIds = new TreeSet<String>();
    private final Map<String, Participant> filteredParticipants = new HashMap<String, Participant>();
    private ArrayList<String> selectedParticipantIds = new ArrayList<String>();
    private final ArrayList<ParticipantEntry> participantsForAdapter = new ArrayList<ParticipantEntry>();

    private BaseAdapter participantsAdapter;

    // styles
    private int inputTextColor;
    private Typeface inputTextTypeface;
    private int inputTextStyle;
    private int listTextColor;
    private Typeface listTextTypeface;
    private int listTextStyle;
    private int chipBackgroundColor;
    private int chipTextColor;
    private Typeface chipTextTypeface;
    private int chipTextStyle;

    private Bitmap maskSingleBmp = Bitmap.createBitmap((int) Tools.getPxFromDp(32, getContext()), (int) Tools.getPxFromDp(32, getContext()), Config.ARGB_8888);
    private Paint avatarPaint = new Paint();
    private Paint maskPaint = new Paint();
    private int avatarBackgroundColor;
    private Bitmap maskSmallBmp = Bitmap.createBitmap((int) Tools.getPxFromDp(24, getContext()), (int) Tools.getPxFromDp(24, getContext()), Config.ARGB_8888);

    public AtlasParticipantPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasParticipantPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasParticipantPicker(Context context) {
        super(context);
        setupPaints();
    }

    public AtlasParticipantPicker init(ParticipantProvider participantProvider, String... userIdToSkip) {
        if (participantProvider == null)
            throw new IllegalArgumentException("ParticipantProvider cannot be null");
        if (participantsList != null)
            throw new IllegalStateException("AtlasParticipantPicker is already initialized!");

        LayoutInflater.from(getContext()).inflate(R.layout.old_atlas_participants_picker, this);

        this.participantProvider = participantProvider;
        if (userIdToSkip != null) {
            for (String userId : userIdToSkip) {
                if (userId != null) skipUserIds.add(userId);
            }
        }

        // START OF -------------------- Participant Picker ----------------------------------------
        this.rootView = this;
        textFilter = (EditText) rootView.findViewById(R.id.atlas_participants_picker_text);
        participantsList = (ListView) rootView.findViewById(R.id.atlas_participants_picker_list);
        selectedParticipantsContainer = (ViewGroup) rootView.findViewById(R.id.atlas_participants_picker_names);

        if (rootView.getVisibility() == View.VISIBLE) {
            textFilter.requestFocus();
        }

        // log focuses
        final View scroller = rootView.findViewById(R.id.atlas_participants_picker_scroll);
        scroller.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "scroller.onFocusChange() hasFocus: " + hasFocus);
            }
        });
        selectedParticipantsContainer.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "names.onFocusChange()    hasFocus: " + hasFocus);
            }
        });

        // If filter.requestFocus is called from .onClickListener - filter receives focus, but
        // NamesLayout receives it immediately after that. So filter lose it.
        // XXX: scroller also receives focus 
        selectedParticipantsContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "names.onTouch() event: " + event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) // ACTION_UP never comes if  
                    textFilter.requestFocus(); //   there is no .onClickListener
                return false;
            }
        });

        textFilter.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                View focused = selectedParticipantsContainer.hasFocus() ? selectedParticipantsContainer : selectedParticipantsContainer.findFocus();
                if (debug)
                    Log.w(TAG, "filter.onFocusChange()   hasFocus: " + hasFocus + ", focused: " + focused);
                if (hasFocus) {
                    participantsList.setVisibility(View.VISIBLE);
                }
                v.post(new Runnable() { // check focus runnable
                    @Override
                    public void run() {
                        if (debug)
                            Log.w(TAG, "filter.onFocusChange.run()   filter.focus: " + textFilter.hasFocus());
                        if (debug)
                            Log.w(TAG, "filter.onFocusChange.run()    names.focus: " + selectedParticipantsContainer.hasFocus());
                        if (debug)
                            Log.w(TAG, "filter.onFocusChange.run() scroller.focus: " + scroller.hasFocus());

                        // check focus is on any descendants and hide list otherwise  
                        View focused = selectedParticipantsContainer.hasFocus() ? selectedParticipantsContainer : selectedParticipantsContainer.findFocus();
                        if (focused == null) {
                            participantsList.setVisibility(View.GONE);
                            textFilter.setText("");
                        }
                    }
                });
            }
        });

        participantsList.setAdapter(participantsAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.old_atlas_view_participants_picker_convert, parent, false);
                }

                TextView name = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_name);
                TextView avatarText = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_ava);
                ImageView avatarImgView = (ImageView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_avatar_img);
                Bitmap avatarBmp = null;
                if (avatarImgView.getDrawable() instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) avatarImgView.getDrawable();
                    avatarBmp = bitmapDrawable.getBitmap();
                } else {
                    avatarBmp = Bitmap.createBitmap(maskSingleBmp.getWidth(), maskSingleBmp.getHeight(), Config.ARGB_8888);
                }
                Canvas avatarCanvas = new Canvas(avatarBmp);
                avatarCanvas.drawColor(avatarBackgroundColor, Mode.CLEAR);
                avatarCanvas.drawColor(avatarBackgroundColor);

                ParticipantEntry entry = participantsForAdapter.get(position);

                if (entry != null) {
                    name.setText(entry.participant.getName());
//                    Drawable avatarDrawable = entry.participant.getAvatarDrawable();
//                    if (entry.participant != null && avatarDrawable != null) {
//                        avatarDrawable.setBounds(0, 0, avatarBmp.getWidth(), avatarBmp.getHeight());
//                        avatarDrawable.draw(avatarCanvas);
//                        avatarText.setVisibility(View.INVISIBLE);
//                    } else {
                    avatarText.setVisibility(View.VISIBLE);
                    avatarText.setText(Utils.getInitials(entry.participant));
//                    }
                } else {
                    name.setText("Unknown user");
                    avatarText.setText("?");
                }
                avatarCanvas.drawBitmap(maskSingleBmp, 0, 0, maskPaint);
                avatarImgView.setImageBitmap(avatarBmp);

                // apply styles
                name.setTextColor(listTextColor);
                name.setTypeface(listTextTypeface, listTextStyle);
                avatarText.setTextColor(listTextColor);
                avatarText.setTypeface(listTextTypeface, listTextStyle);
                return convertView;
            }

            public long getItemId(int position) {
                return participantsForAdapter.get(position).id.hashCode();
            }

            public Object getItem(int position) {
                return participantsForAdapter.get(position);
            }

            public int getCount() {
                return participantsForAdapter.size();
            }
        });

        participantsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParticipantEntry entry = participantsForAdapter.get(position);
                selectedParticipantIds.add(entry.id);
                refreshParticipants(selectedParticipantIds);
                textFilter.setText("");
                textFilter.requestFocus();
                filterParticipants("");                 // refresh participantList
            }

        });

        // track text and filter participant list
        textFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (debug)
                    Log.w(TAG, "beforeTextChanged() s: " + s + " start: " + start + " count: " + count + " after: " + after);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debug)
                    Log.w(TAG, "onTextChanged()     s: " + s + " start: " + start + " before: " + before + " count: " + count);

                final String filter = s.toString().toLowerCase();
                filterParticipants(filter);
            }

            public void afterTextChanged(Editable s) {
                if (debug) Log.w(TAG, "afterTextChanged()  s: " + s);
            }
        });

        // select last added participant when press "Backspace/Del"
        textFilter.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (debug) Log.w(TAG, "onKey() keyCode: " + keyCode + ", event: " + event);
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && textFilter.getText().length() == 0 && selectedParticipantIds.size() > 0) {

                    selectedParticipantIds.remove(selectedParticipantIds.size() - 1);
                    refreshParticipants(selectedParticipantIds);
                    filterParticipants("");
                    textFilter.requestFocus();
                }
                return false;
            }
        });
        // END OF ---------------------- Participant Picker ---------------------------------------- 

        filterParticipants("");

        applyStyle();
        return this;
    }

    private void refreshParticipants(final ArrayList<String> selectedParticipantIds) {

        // remove name_converts first. Better to keep editText in place rather than add/remove that force keyboard to blink
        for (int i = selectedParticipantsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = selectedParticipantsContainer.getChildAt(i);
            if (child != textFilter) {
                selectedParticipantsContainer.removeView(child);
            }
        }
        if (debug)
            Log.w(TAG, "refreshParticipants() childs left: " + selectedParticipantsContainer.getChildCount());
        for (String id : selectedParticipantIds) {
            Participant participant = participantProvider.getParticipant(id);
            View participantView = LayoutInflater.from(selectedParticipantsContainer.getContext()).inflate(R.layout.old_atlas_view_participants_picker_name_convert, selectedParticipantsContainer, false);

            TextView avaText = (TextView) participantView.findViewById(R.id.atlas_view_participants_picker_name_convert_ava);
//            ImageView avatarImgView = (ImageView) participantView.findViewById(R.id.atlas_view_participants_picker_name_convert_avatar_img);
//            Bitmap avatarBmp = Bitmap.createBitmap(maskSmallBmp.getWidth(), maskSmallBmp.getHeight(), Config.ARGB_8888);
//            Canvas avatarCanvas = new Canvas(avatarBmp);
//            avatarCanvas.drawColor(R.color.atlas_shape_avatar_gray);
//            if (participant.getAvatarDrawable() != null) {
//                participant.getAvatarDrawable().setBounds(0, 0, avatarBmp.getWidth(), avatarBmp.getHeight());
//                participant.getAvatarDrawable().draw(avatarCanvas);
//                avaText.setVisibility(View.INVISIBLE);
//            } else {
            avaText.setText(Utils.getInitials(participant));
//            }
//            avatarCanvas.drawBitmap(maskSmallBmp, 0, 0, maskPaint);
//            avatarImgView.setImageBitmap(avatarBmp);

            TextView nameText = (TextView) participantView.findViewById(R.id.atlas_view_participants_picker_name_convert_name);
            nameText.setText(participant.getName());
            participantView.setTag(participant);

            selectedParticipantsContainer.addView(participantView, selectedParticipantsContainer.getChildCount() - 1);
            if (debug)
                Log.w(TAG, "refreshParticipants() child added: " + participantView + ", for: " + participant);

            // apply styles
            avaText.setTextColor(chipTextColor);
            avaText.setTypeface(chipTextTypeface, chipTextStyle);
            nameText.setTextColor(chipTextColor);
            nameText.setTypeface(chipTextTypeface, chipTextStyle);
            View container = participantView.findViewById(R.id.atlas_view_participants_picker_name_convert);
            GradientDrawable drawable = (GradientDrawable) container.getBackground();
            drawable.setColor(chipBackgroundColor);

        }
        if (selectedParticipantIds.size() == 0) {
            LayoutParams params = new LayoutParams(textFilter.getLayoutParams());
            params.width = LayoutParams.MATCH_PARENT;
        }
        selectedParticipantsContainer.requestLayout();
    }

    private void filterParticipants(final String filter) {
        filteredParticipants.clear();
        participantProvider.getMatchingParticipants(filter, filteredParticipants);
        if (debug)
            Log.w(TAG, "filterParticipants() filtered: " + filteredParticipants.size() + ", filter: " + filter);
        participantsForAdapter.clear();
        for (Map.Entry<String, Participant> entry : filteredParticipants.entrySet()) {
            if (selectedParticipantIds.contains(entry.getKey())) continue;
            if (skipUserIds.contains(entry.getKey())) continue;
            participantsForAdapter.add(new ParticipantEntry(entry.getValue(), entry.getKey()));
        }
        Collections.sort(participantsForAdapter, new ParticipantEntryFilteringComparator(filter));
        if (debug)
            Log.w(TAG, "filterParticipants() participants to show: " + participantsForAdapter.size());
        participantsAdapter.notifyDataSetChanged();
    }

    private void setupPaints() {
        avatarPaint.setAntiAlias(true);
        avatarPaint.setDither(true);

        maskPaint.setAntiAlias(true);
        maskPaint.setDither(true);
        maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

        Paint paintCircle = new Paint();
        paintCircle.setStyle(Style.FILL_AND_STROKE);
        paintCircle.setColor(Color.CYAN);
        paintCircle.setAntiAlias(true);

        Canvas maskSingleCanvas = new Canvas(maskSingleBmp);
        maskSingleCanvas.drawCircle(0.5f * maskSingleBmp.getWidth(), 0.5f * maskSingleBmp.getHeight(), 0.5f * maskSingleBmp.getWidth(), paintCircle);

        avatarBackgroundColor = getResources().getColor(R.color.atlas_shape_avatar_gray);
    }

    private void parseStyle(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AtlasConversationLauncher, R.attr.AtlasParticipantPicker, defStyle);
        this.inputTextColor = ta.getColor(R.styleable.AtlasConversationLauncher_inputTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.inputTextStyle = ta.getInt(R.styleable.AtlasConversationLauncher_inputTextStyle, Typeface.NORMAL);
        String inputTextTypefaceName = ta.getString(R.styleable.AtlasConversationLauncher_inputTextTypeface);
        this.inputTextTypeface = inputTextTypefaceName != null ? Typeface.create(inputTextTypefaceName, inputTextStyle) : null;

        this.listTextColor = ta.getColor(R.styleable.AtlasConversationLauncher_listTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.listTextStyle = ta.getInt(R.styleable.AtlasConversationLauncher_listTextStyle, Typeface.NORMAL);
        String listTextTypefaceName = ta.getString(R.styleable.AtlasConversationLauncher_listTextTypeface);
        this.listTextTypeface = listTextTypefaceName != null ? Typeface.create(listTextTypefaceName, inputTextStyle) : null;

        this.chipBackgroundColor = ta.getColor(R.styleable.AtlasConversationLauncher_chipBackgroundColor, context.getResources().getColor(R.color.atlas_background_gray));
        this.chipTextColor = ta.getColor(R.styleable.AtlasConversationLauncher_chipTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.chipTextStyle = ta.getInt(R.styleable.AtlasConversationLauncher_chipTextStyle, Typeface.NORMAL);
        String chipTextTypefaceName = ta.getString(R.styleable.AtlasConversationLauncher_chipTextTypeface);
        this.chipTextTypeface = chipTextTypefaceName != null ? Typeface.create(chipTextTypefaceName, inputTextStyle) : null;
        ta.recycle();
    }

    private void applyStyle() {
        refreshParticipants(selectedParticipantIds);
        participantsAdapter.notifyDataSetChanged();
        textFilter.setTextColor(inputTextColor);
        textFilter.setTypeface(inputTextTypeface, inputTextStyle);
    }

    public String[] getSelectedUserIds() {
        String[] userIds = new String[selectedParticipantIds.size()];
        int i = 0;
        for (String id : selectedParticipantIds) {
            userIds[i++] = id;
        }
        return userIds;
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            textFilter.requestFocus();
        }
    }

    private static final class FilteringComparator implements Comparator<Participant> {
        private final String filter;

        /**
         * @param filter - the less indexOf(filter) the less order of participant
         */
        public FilteringComparator(String filter) {
            this.filter = filter;
        }

        @Override
        public int compare(Participant lhs, Participant rhs) {
            return subCompareCaseInsensitive(lhs.getName(), rhs.getName());
        }

        private int subCompareCaseInsensitive(String lhs, String rhs) {
            int left = lhs != null ? lhs.toLowerCase().indexOf(filter) : -1;
            int right = rhs != null ? rhs.toLowerCase().indexOf(filter) : -1;

            if (left == -1 && right == -1) return 0;
            if (left != -1 && right == -1) return -1;
            if (left == -1 && right != -1) return 1;
            if (left - right != 0) return left - right;
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    }

    private static final class ParticipantEntryFilteringComparator implements Comparator<ParticipantEntry> {
        FilteringComparator comparator;

        public ParticipantEntryFilteringComparator(String filter) {
            this.comparator = new FilteringComparator(filter);
        }

        @Override
        public int compare(ParticipantEntry lhs, ParticipantEntry rhs) {
            return comparator.compare(lhs.participant, rhs.participant);
        }
    }

    private static class ParticipantEntry {
        final Participant participant;
        final String id;

        public ParticipantEntry(Participant participant, String id) {
            if (participant == null)
                throw new IllegalArgumentException("Participant cannot be null");
            if (id == null) throw new IllegalArgumentException("ID cannot be null");
            this.participant = participant;
            this.id = id;
        }
    }

}