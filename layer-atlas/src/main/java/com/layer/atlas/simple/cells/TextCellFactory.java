package com.layer.atlas.simple.cells;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;

public class TextCellFactory extends AtlasCellFactory<TextCellFactory.CellHolder, TextCellFactory.CacheableString> {
    public TextCellFactory() {
        super(2 * 1024 * 1024);
    }

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
    public CacheableString cache(Message message) {
        return new CacheableString(new String(message.getMessageParts().get(0).getData()));
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, CacheableString string, Message message, CellHolderSpecs specs) {
        cellHolder.mTextView.setText(string.toString());
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }

    public static class CacheableString implements AtlasCellFactory.Cacheable {
        private final String mString;
        private final int mSize;

        public CacheableString(String string) {
            mString = string;
            mSize = mString.getBytes().length;
        }

        public String getString() {
            return mString;
        }

        @Override
        public int sizeOf() {
            return mSize;
        }

        @Override
        public String toString() {
            return mString;
        }
    }
}
