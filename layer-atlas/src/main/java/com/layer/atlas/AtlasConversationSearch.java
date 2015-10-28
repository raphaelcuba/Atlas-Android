package com.layer.atlas;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.layer.atlas.provider.Participant;
import com.layer.atlas.provider.ParticipantProvider;
import com.layer.atlas.utilities.views.EmptyDelEditText;
import com.layer.atlas.utilities.views.FlowLayout;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.query.CompoundPredicate;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.RecyclerViewController;
import com.layer.sdk.query.SortDescriptor;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AtlasConversationSearch extends LinearLayout {
    private LayerClient mLayerClient;
    private ParticipantProvider mParticipantProvider;
    private Picasso mPicasso;

    private OnConversationClickListener mOnConversationClickListener;
    private OnParticipantSelectionChangeListener mOnParticipantSelectionChangeListener;

    private FlowLayout mSelectedParticipantLayout;
    private EmptyDelEditText mFilter;
    private RecyclerView mParticipantList;
    private AvailableConversationAdapter mAvailableConversationAdapter;
    private final Set<String> mSelectedParticipantIds = new LinkedHashSet<String>();

    public AtlasConversationSearch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AtlasConversationSearch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.atlas_conversation_launcher, this, true);
        mSelectedParticipantLayout = (FlowLayout) findViewById(R.id.selected_participant_group);
        mFilter = (EmptyDelEditText) findViewById(R.id.filter);
        mSelectedParticipantLayout.setStretchChild(mFilter);
        mParticipantList = (RecyclerView) findViewById(R.id.participant_list);
        setOrientation(VERTICAL);
    }

    public AtlasConversationSearch init(LayerClient layerClient, ParticipantProvider participantProvider, Picasso picasso) {
        mLayerClient = layerClient;
        mParticipantProvider = participantProvider;
        mPicasso = picasso;

        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mParticipantList.setLayoutManager(manager);
        mAvailableConversationAdapter = new AvailableConversationAdapter(mLayerClient, mParticipantProvider, mPicasso);
        mParticipantList.setAdapter(mAvailableConversationAdapter);

        // Hitting backspace with an empty search string deletes the last selected participant
        mFilter.setOnEmptyDelListener(new EmptyDelEditText.OnEmptyDelListener() {
            @Override
            public boolean onEmptyDel(EmptyDelEditText editText) {
                removeLastSelectedParticipant();
                return true;
            }
        });

        // Refresh available participants and conversations with every search string change
        mFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                refresh();
            }
        });
        return this;
    }

    public AtlasConversationSearch setOnConversationClickListener(OnConversationClickListener onConversationClickListener) {
        mOnConversationClickListener = onConversationClickListener;
        return this;
    }

    public AtlasConversationSearch setOnParticipantSelectionChangeListener(OnParticipantSelectionChangeListener onParticipantSelectionChangeListener) {
        mOnParticipantSelectionChangeListener = onParticipantSelectionChangeListener;
        return this;
    }

    public List<String> getSelectedParticipanIds() {
        return new ArrayList<String>(mSelectedParticipantIds);
    }

    public AtlasConversationSearch refresh() {
        if (mAvailableConversationAdapter == null) return this;
        mAvailableConversationAdapter.refresh(getSearchFilter(), mSelectedParticipantIds);
        return this;
    }

    private void selectParticipant(String participantId) {
        if (mSelectedParticipantIds.contains(participantId)) return;
        mSelectedParticipantIds.add(participantId);
        ParticipantChip chip = new ParticipantChip(getContext(), mParticipantProvider, participantId, mPicasso);
        mSelectedParticipantLayout.addView(chip, mSelectedParticipantLayout.getChildCount() - 1);
        mFilter.setText(null);
        refresh();
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<String>(mSelectedParticipantIds));
        }
    }

    private void unselectParticipant(ParticipantChip chip) {
        if (!mSelectedParticipantIds.contains(chip.mParticipantId)) return;
        mSelectedParticipantIds.remove(chip.mParticipantId);
        mSelectedParticipantLayout.removeView(chip);
        refresh();
        if (mOnParticipantSelectionChangeListener != null) {
            mOnParticipantSelectionChangeListener.onParticipantSelectionChanged(this, new ArrayList<String>(mSelectedParticipantIds));
        }
    }

    private String getSearchFilter() {
        String s = mFilter.getText().toString();
        return s.trim().isEmpty() ? null : s;
    }

    private String removeLastSelectedParticipant() {
        ParticipantChip lastChip = null;
        for (int i = 0; i < mSelectedParticipantLayout.getChildCount(); i++) {
            View v = mSelectedParticipantLayout.getChildAt(i);
            if (v instanceof ParticipantChip) lastChip = (ParticipantChip) v;
        }
        if (lastChip == null) return null;
        unselectParticipant(lastChip);
        return lastChip.mParticipantId;
    }

    /**
     * Automatically refresh on resume
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) return;
        refresh();
    }

    /**
     * Save selected participants
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mSelectedParticipantIds.isEmpty()) return superState;
        SavedState savedState = new SavedState(superState);
        savedState.mSelectedParticipantIds = new ArrayList<String>(mSelectedParticipantIds);
        return savedState;
    }

    /**
     * Restore selected participants
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        if (savedState.mSelectedParticipantIds != null) {
            mSelectedParticipantIds.clear();
            for (String participantId : savedState.mSelectedParticipantIds) {
                selectParticipant(participantId);
            }
        }
    }

    private static class SavedState extends BaseSavedState {
        List<String> mSelectedParticipantIds;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringList(mSelectedParticipantIds);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            mSelectedParticipantIds = in.createStringArrayList();
        }
    }

    /**
     * ParticipantChip implements the View used to populate the selected participant FlowLayout.
     */
    private class ParticipantChip extends LinearLayout {
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
            inflater.inflate(R.layout.atlas_participant_chip, this, true);
            mAvatar = (AtlasAvatar) findViewById(R.id.avatar);
            mName = (TextView) findViewById(R.id.name);
            mRemove = (ImageView) findViewById(R.id.remove);

            // Set layout
            int height = r.getDimensionPixelSize(R.dimen.atlas_selected_participant_height);
            int margin = r.getDimensionPixelSize(R.dimen.atlas_selected_participant_margin);
            FlowLayout.LayoutParams p = new FlowLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            p.setMargins(margin, margin, margin, margin);
            setLayoutParams(p);
            setOrientation(HORIZONTAL);
            setBackgroundDrawable(r.getDrawable(R.drawable.atlas_participant_chip_background));

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

    /**
     * AvailableConversationAdapter provides items for individual Participants and existing
     * Conversations.  Items are filtered by a participant filter string and by a set of selected
     * Participants.
     */
    private class AvailableConversationAdapter extends RecyclerView.Adapter<AvailableConversationAdapter.ViewHolder> implements RecyclerViewController.Callback {
        protected final LayerClient mLayerClient;
        protected final ParticipantProvider mParticipantProvider;
        protected final Picasso mPicasso;
        private final RecyclerViewController<Conversation> mQueryController;

        private final ArrayList<String> mParticipantIds = new ArrayList<String>();
        private final ArrayList<Participant> mParticipants = new ArrayList<Participant>();
        private final Map<String, Participant> mParticipantMap = new HashMap<String, Participant>();

        public AvailableConversationAdapter(LayerClient client, ParticipantProvider participantProvider, Picasso picasso) {
            this(client, participantProvider, picasso, null);
        }

        public AvailableConversationAdapter(LayerClient client, ParticipantProvider participantProvider, Picasso picasso, Collection<String> updateAttributes) {
            mQueryController = client.newRecyclerViewController(null, updateAttributes, this);
            mLayerClient = client;
            mParticipantProvider = participantProvider;
            mPicasso = picasso;
            setHasStableIds(false);
        }

        /**
         * Refreshes this adapter by re-querying the ParticipantProvider and filtering Conversations
         * to return only those Conversations with the given set of selected Participants.
         */
        public void refresh(String filter, Set<String> selectedParticipantIds) {
            // Apply text search filter to available participants
            synchronized (mParticipantIds) {
                mParticipantProvider.getMatchingParticipants(filter, mParticipantMap);
                mParticipants.clear();
                for (Map.Entry<String, Participant> entry : mParticipantMap.entrySet()) {
                    // Don't show participants we've already selected
                    if (selectedParticipantIds.contains(entry.getKey())) continue;
                    mParticipants.add(entry.getValue());
                }
                Collections.sort(mParticipants);

                mParticipantIds.clear();
                for (Participant p : mParticipants) {
                    mParticipantIds.add(p.getId());
                }
            }
            // TODO: compute add/remove/move and notify those instead of complete notify
            notifyDataSetChanged();

            // Filter down to only those conversations including the selected participants, hiding one-on-one conversations
            Query.Builder<Conversation> builder = Query.builder(Conversation.class)
                    .sortDescriptor(new SortDescriptor(Conversation.Property.LAST_MESSAGE_SENT_AT, SortDescriptor.Order.DESCENDING));
            if (selectedParticipantIds.isEmpty()) {
                builder.predicate(new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2));
            } else {
                builder.predicate(new CompoundPredicate(CompoundPredicate.Type.AND,
                        new Predicate(Conversation.Property.PARTICIPANT_COUNT, Predicate.Operator.GREATER_THAN, 2),
                        new Predicate(Conversation.Property.PARTICIPANTS, Predicate.Operator.IN, selectedParticipantIds)));
            }
            mQueryController.setQuery(builder.build()).execute();
        }


        //==============================================================================================
        // Adapter
        //==============================================================================================

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewHolder viewHolder = new ViewHolder(parent);
            viewHolder.mAvatar.init(mParticipantProvider, mPicasso);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            synchronized (mParticipantIds) {
                switch (getType(position)) {
                    case PARTICIPANT: {
                        position = adapterPositionToParticipantPosition(position);
                        String participantId = mParticipantIds.get(position);
                        Participant participant = mParticipants.get(position);
                        viewHolder.mTitle.setText(participant.getName());
                        viewHolder.itemView.setTag(participantId);
                        viewHolder.itemView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectParticipant((String) v.getTag());
                            }
                        });
                        viewHolder.mAvatar.setParticipants(participantId);
                    }
                    break;

                    case CONVERSATION: {
                        position = adapterPositionToConversationPosition(position);
                        Conversation conversation = mQueryController.getItem(position);
                        String userId = mLayerClient.getAuthenticatedUserId();
                        List<String> names = new ArrayList<String>();
                        Set<String> ids = new LinkedHashSet<String>();
                        for (String participantId : conversation.getParticipants()) {
                            if (participantId.equals(userId)) continue;
                            ids.add(participantId);
                            Participant p = mParticipantProvider.getParticipant(participantId);
                            if (p == null) continue;
                            names.add(p.getName());
                        }
                        viewHolder.mTitle.setText(TextUtils.join(", ", names));
                        viewHolder.itemView.setTag(conversation);
                        viewHolder.itemView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mOnConversationClickListener == null) return;
                                mOnConversationClickListener.onConversationClick(AtlasConversationSearch.this, (Conversation) v.getTag());
                            }
                        });
                        viewHolder.mAvatar.setParticipants(ids);
                    }
                    break;
                }
            }
        }

        // first are participants; then are conversations
        Type getType(int position) {
            synchronized (mParticipantIds) {
                return (position < mParticipantIds.size()) ? Type.PARTICIPANT : Type.CONVERSATION;
            }
        }

        int adapterPositionToParticipantPosition(int position) {
            return position;
        }

        int adapterPositionToConversationPosition(int position) {
            synchronized (mParticipantIds) {
                return position - mParticipantIds.size();
            }
        }

        int conversationPositionToAdapterPosition(int position) {
            synchronized (mParticipantIds) {
                return position + mParticipantIds.size();
            }
        }

        @Override
        public int getItemCount() {
            synchronized (mParticipantIds) {
                return mQueryController.getItemCount() + mParticipantIds.size();
            }
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

        protected class ViewHolder extends RecyclerView.ViewHolder {
            private AtlasAvatar mAvatar;
            private TextView mTitle;

            public ViewHolder(ViewGroup parent) {
                super(LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_conversation_launcher_item, parent, false));
                mAvatar = (AtlasAvatar) itemView.findViewById(R.id.avatar);
                mTitle = (TextView) itemView.findViewById(R.id.title);
            }
        }
    }


    public interface OnConversationClickListener {
        void onConversationClick(AtlasConversationSearch conversationLauncher, Conversation conversation);
    }

    public interface OnParticipantSelectionChangeListener {
        void onParticipantSelectionChanged(AtlasConversationSearch conversationLauncher, List<String> participantIds);
    }
}
