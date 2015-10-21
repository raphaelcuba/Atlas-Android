package com.layer.atlas;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.layer.atlas.layouts.FlowLayout;
import com.layer.sdk.LayerClient;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class AtlasParticipantPicker extends LinearLayout {
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;
    private Picasso mPicasso;

    private FlowLayout mSelectedParticipantLayout;
    private EditText mSearchText;
    private ListView mParticipantList;

    public AtlasParticipantPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasParticipantPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.atlas_participant_picker, this, true);
        mSelectedParticipantLayout = (FlowLayout) findViewById(R.id.selected_participant_group);
        mSearchText = (EditText) findViewById(R.id.participant_search);
        mSelectedParticipantLayout.setStretchChild(mSearchText);
        mParticipantList = (ListView) findViewById(R.id.participant_list);
        setOrientation(VERTICAL);
    }

    public AtlasParticipantPicker init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;

        Map<String, Participant> participants = new HashMap<String, Participant>();
        mParticipantProvider.getMatchingParticipants(null, participants);

        for (Map.Entry<String, Participant> entry : participants.entrySet()) {
            ParticipantChip chip = new ParticipantChip(getContext(), participantProvider, entry.getKey(), picasso);
            mSelectedParticipantLayout.addView(chip, mSelectedParticipantLayout.getChildCount() - 1);
        }

        return this;
    }

    public static class ParticipantChip extends LinearLayout {
        private AtlasAvatar mAvatar;
        private TextView mName;
        private ImageView mRemove;

        public ParticipantChip(Context context, ParticipantProvider participantProvider, String participantId, Picasso picasso) {
            super(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            Resources r = getContext().getResources();

            // Inflate and cache views
            inflater.inflate(R.layout.atlas_selected_participant_item, this, true);
            mAvatar = (AtlasAvatar) findViewById(R.id.participant_avatar);
            mName = (TextView) findViewById(R.id.participant_name);
            mRemove = (ImageView) findViewById(R.id.participant_remove);

            // Set layout
            int height = r.getDimensionPixelSize(R.dimen.atlas_selected_participant_height);
            int margin = r.getDimensionPixelSize(R.dimen.atlas_selected_participant_margin);
            FlowLayout.LayoutParams p = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            p.setMargins(margin, margin, margin, margin);
            setLayoutParams(p);
            setOrientation(HORIZONTAL);
            setBackgroundDrawable(r.getDrawable(R.drawable.atlas_selected_participant_background));

            // Initialize participant data
            Participant participant = participantProvider.getParticipant(participantId);
            mName.setText(participant.getName());
            mAvatar.init(participantProvider, picasso).setParticipants(participantId);

            mRemove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ParticipantChip chip = ParticipantChip.this;
                    ((ViewGroup) chip.getParent()).removeView(chip);
                }
            });
        }
    }


}
