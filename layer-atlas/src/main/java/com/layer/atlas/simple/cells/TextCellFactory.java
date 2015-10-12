package com.layer.atlas.simple.cells;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TextCellFactory implements AtlasCellFactory<TextCellFactory.CellHolder> {
    private final Map<String, String> mCache = new ConcurrentHashMap<String, String>();

    @Override
    public boolean isBindable(Message message) {
        return message.getMessageParts().get(0).getMimeType().startsWith("text/");
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        Context context = cellView.getContext();

        View v = layoutInflater.inflate(R.layout.cell_text, cellView, true);
        v.setBackgroundResource(isMe ? R.drawable.atlas_message_item_cell_me : R.drawable.atlas_message_item_cell_them);

        TextView t = (TextView) v.findViewById(R.id.cell_text);
        t.setTextColor(context.getResources().getColor(isMe ? R.color.atlas_text_white : R.color.atlas_text_black));
        return new CellHolder(v);
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, Message message, CellHolderSpecs specs) {
        onCache(message);
        cellHolder.mTextView.setText(mCache.get(message.getId().toString()));
    }

    @Override
    public void onCache(Message message) {
        String id = message.getId().toString();
        if (mCache.containsKey(id)) return;
        mCache.put(id, new String(message.getMessageParts().get(0).getData()));
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }
}
