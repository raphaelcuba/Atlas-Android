package com.layer.atlas.simple.cells;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

public class MimeCellFactory extends AtlasCellFactory<MimeCellFactory.CellHolder, MimeCellFactory.ParsedContentString> {
    public MimeCellFactory() {
        super(32 * 1024);
    }

    @Override
    public boolean isBindable(Message message) {
        return true;
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_text, cellView));
    }

    @Override
    public ParsedContentString parseContent(Message message) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (MessagePart part : message.getMessageParts()) {
            if (i != 0) builder.append("\n");
            builder.append("MIME Type [").append(i).append("]: ").append(part.getMimeType());
            i++;
        }
        return new ParsedContentString(builder.toString());
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, ParsedContentString string, Message message, CellHolderSpecs specs) {
        cellHolder.mTextView.setText(string.toString());
    }

    class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }

    public static class ParsedContentString implements AtlasCellFactory.ParsedContent {
        private final String mString;
        private final int mSize;

        public ParsedContentString(String string) {
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
