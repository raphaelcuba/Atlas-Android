package com.layer.atlas;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.atlas.layouts.FlowLayout;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AtlasParticipantPicker extends LinearLayout {
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;
    private Picasso mPicasso;

    private FlowLayout mSelectedParticipantLayout;
    private EditText mSearchText;
    private RecyclerView mParticipantList;
    private AvailableConversationAdapter mAvailableConversationAdapter;
    private final Set<String> mSelectedParticipantIds = new HashSet<String>();

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
        mParticipantList = (RecyclerView) findViewById(R.id.participant_list);
        setOrientation(VERTICAL);
    }

    public AtlasParticipantPicker init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;

        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mParticipantList.setLayoutManager(manager);
        mAvailableConversationAdapter = new AvailableConversationAdapter(getContext(), mLayerClient, mParticipantProvider, mPicasso);
        mParticipantList.setAdapter(mAvailableConversationAdapter);

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                if (e == null || e.toString().trim().isEmpty()) {
                    mAvailableConversationAdapter.refresh(null, mSelectedParticipantIds);
                    return;
                }
                mAvailableConversationAdapter.refresh(e.toString(), mSelectedParticipantIds);
            }
        });

        mAvailableConversationAdapter.refresh(null, mSelectedParticipantIds);
        return this;
    }

    private void selectParticipant(String participantId) {
        if (mSelectedParticipantIds.contains(participantId)) return;
        mSelectedParticipantIds.add(participantId);
        ParticipantChip chip = new ParticipantChip(getContext(), mParticipantProvider, participantId, mPicasso);
        mSelectedParticipantLayout.addView(chip, mSelectedParticipantLayout.getChildCount() - 1);
        mSearchText.setText(null);
        mAvailableConversationAdapter.refresh(null, mSelectedParticipantIds);
    }

    private void unselectParticipant(String participantId) {
        if (!mSelectedParticipantIds.contains(participantId)) return;
        mSelectedParticipantIds.remove(participantId);
        for (int i = 0; i < mSelectedParticipantLayout.getChildCount(); i++) {
            View v = mSelectedParticipantLayout.getChildAt(i);
            if (!(v instanceof ParticipantChip)) continue;
            ParticipantChip chip = (ParticipantChip) v;
            if (!chip.mParticipantId.equals(participantId)) continue;
            mSelectedParticipantLayout.removeView(chip);
        }
        mAvailableConversationAdapter.refresh(null, mSelectedParticipantIds);
    }

    private void unselectParticipant(ParticipantChip chip) {
        if (!mSelectedParticipantIds.contains(chip.mParticipantId)) return;
        mSelectedParticipantIds.remove(chip.mParticipantId);
        mSelectedParticipantLayout.removeView(chip);
        mAvailableConversationAdapter.refresh(null, mSelectedParticipantIds);
    }

    class ParticipantChip extends LinearLayout {
        private String mParticipantId;

        private AtlasAvatar mAvatar;
        private TextView mName;
        private ImageView mRemove;

        public ParticipantChip(Context context, ParticipantProvider participantProvider, String participantId, Picasso picasso) {
            super(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            Resources r = getContext().getResources();
            mParticipantId = participantId;

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

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    unselectParticipant(ParticipantChip.this);
                }
            });
        }
    }

    private enum Type {
        PARTICIPANT,
        CONVERSATION
    }

    private class AvailableConversationAdapter extends RecyclerView.Adapter<AvailableConversationAdapter.ViewHolder> implements RecyclerViewController.Callback {
        protected final LayerClient mLayerClient;
        protected final ParticipantProvider mParticipantProvider;
        protected final Picasso mPicasso;
        private final RecyclerViewController<Conversation> mQueryController;
        private final LayoutInflater mInflater;

        private final ArrayList<String> mParticipantIds = new ArrayList<String>();
        private final Map<String, Participant> mParticipants = new LinkedHashMap<String, Participant>();

        public AvailableConversationAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso) {
            this(context, client, participantProvider, picasso, null);
        }

        public AvailableConversationAdapter(Context context, LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Collection<String> updateAttributes) {
            Query<Conversation> query = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING))
                    .build();
            mQueryController = client.newRecyclerViewController(query, updateAttributes, this);
            mLayerClient = client;
            mParticipantProvider = participantProvider;
            mPicasso = picasso;
            mInflater = LayoutInflater.from(context);
            setHasStableIds(false);
        }

        /**
         * Refreshes this adapter by re-running the underlying Query.
         */
        public void refresh(String filter, Set<String> selectedParticipantIds) {
            // Apply text search filter to available participants
            mParticipantProvider.getMatchingParticipants(filter, mParticipants);
            List<String> newParticipantIds = new ArrayList<String>(mParticipants.size());
            for (Map.Entry<String, Participant> entry : mParticipants.entrySet()) {
                // Don't show participants we've already selected
                if (selectedParticipantIds.contains(entry.getKey())) continue;

                newParticipantIds.add(entry.getKey());
            }

            // TODO: compute add/removes and notify those
            mParticipantIds.clear();
            mParticipantIds.addAll(newParticipantIds);
            notifyDataSetChanged();

            // Filter down to only those conversations including the selected participants
            Query.Builder<Conversation> builder = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING));
            if (!selectedParticipantIds.isEmpty()) {
                builder.predicate(new Predicate(Conversation.Property.PARTICIPANTS, Predicate.Operator.IN, selectedParticipantIds));
            }
            mQueryController.setQuery(builder.build()).execute();
        }


        //==============================================================================================
        // Adapter
        //==============================================================================================

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(parent);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            switch (getType(position)) {
                case PARTICIPANT:
                    position = adapterPositionToparticipantPosition(position);
                    String participantId = mParticipantIds.get(position);
                    Participant participant = mParticipants.get(participantId);
                    viewHolder.mTitle.setText("Participant: " + participant.getName());
                    viewHolder.itemView.setTag(participantId);
                    viewHolder.itemView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectParticipant((String) v.getTag());
                        }
                    });
                    break;

                case CONVERSATION:
                    position = adapterPositionToConversationPosition(position);
                    Conversation conversation = mQueryController.getItem(position);
                    viewHolder.mTitle.setText("Conversation: " + conversation.getId().toString());
                    viewHolder.itemView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
                    break;
            }
        }

        // first are participants; then are conversations
        Type getType(int position) {
            return (position < mParticipantIds.size()) ? Type.PARTICIPANT : Type.CONVERSATION;
        }

        int adapterPositionToparticipantPosition(int position) {
            return position;
        }

        int adapterPositionToConversationPosition(int position) {
            return position - mParticipantIds.size();
        }

        int conversationPositionToAdapterPosition(int position) {
            return position + mParticipantIds.size();
        }

        @Override
        public int getItemCount() {
            return mQueryController.getItemCount() + mParticipantIds.size();
        }


        //==============================================================================================
        // Conversation UI update callbacks
        //==============================================================================================

        @Override
        public void onQueryDataSetChanged(RecyclerViewController controller) {
            notifyDataSetChanged();
        }

        @Override
        public void onQueryItemChanged(RecyclerViewController controller, int position) {
            notifyItemChanged(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeChanged(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeChanged(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemInserted(RecyclerViewController controller, int position) {
            notifyItemInserted(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeInserted(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeInserted(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemRemoved(RecyclerViewController controller, int position) {
            notifyItemRemoved(conversationPositionToAdapterPosition(position));
        }

        @Override
        public void onQueryItemRangeRemoved(RecyclerViewController controller, int positionStart, int itemCount) {
            notifyItemRangeRemoved(conversationPositionToAdapterPosition(positionStart), itemCount);
        }

        @Override
        public void onQueryItemMoved(RecyclerViewController controller, int fromPosition, int toPosition) {
            notifyItemMoved(conversationPositionToAdapterPosition(fromPosition), conversationPositionToAdapterPosition(toPosition));
        }


        //==============================================================================================
        // Inner classes
        //==============================================================================================

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mTitle;

            public ViewHolder(ViewGroup parent) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_conversation_item, parent, false));
                mTitle = (TextView) itemView.findViewById(R.id.atlas_conversation_view_convert_participant);
            }

        }
    }

}
