package com.layer.atlas.cellfactories;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;

public class TextCellFactory extends AtlasCellFactory<TextCellFactory.CellHolder, TextCellFactory.ParsedContent> {
    public final static String MIME_TYPE = "text/plain";

    public TextCellFactory() {
        super(2 * 1024 * 1024);
    }

    public static boolean isType(Message message) {
        return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    public static String getPreview(Message message) {
        return new String(message.getMessageParts().get(0).getData());
    }

    @Override
    public boolean isBindable(Message message) {
        return TextCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        Context context = cellView.getContext();

        View v = layoutInflater.inflate(R.layout.atlas_message_item_cell_text, cellView, true);
        v.setBackgroundResource(isMe ? R.drawable.atlas_message_item_cell_me : R.drawable.atlas_message_item_cell_them);

        TextView t = (TextView) v.findViewById(R.id.cell_text);
        t.setTextColor(context.getResources().getColor(isMe ? R.color.atlas_text_white : R.color.atlas_text_black));
        return new CellHolder(v);
    }

    @Override
    public ParsedContent parseContent(Message message) {
        return new ParsedContent(new String(message.getMessageParts().get(0).getData()));
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, ParsedContent string, Message message, CellHolderSpecs specs) {
        cellHolder.mTextView.setText(string.toString());
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }

    public static class ParsedContent implements AtlasCellFactory.ParsedContent {
        private final String mString;
        private final int mSize;

        public ParsedContent(String string) {
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
