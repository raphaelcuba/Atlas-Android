package com.layer.atlas.cellfactories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

/**
 * Mime handles all MIME Types by simply displaying all MessagePart MIME Types as text.
 */
public class MimeCellFactory extends AtlasCellFactory<MimeCellFactory.CellHolder, MimeCellFactory.ParsedContent> {
    public MimeCellFactory() {
        super(32 * 1024);
    }

    public static String getPreview(Message message) {
        StringBuilder b = new StringBuilder();
        boolean isFirst = true;
        b.append("[");
        for (MessagePart part : message.getMessageParts()) {
            if (!isFirst) b.append(", ");
            isFirst = false;
            b.append(part.getSize()).append("-byte ").append(part.getMimeType());
        }
        b.append("]");
        return b.toString();
    }

    @Override
    public boolean isBindable(Message message) {
        return true;
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_text, cellView));
    }

    @Override
    public ParsedContent parseContent(Message message) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (MessagePart part : message.getMessageParts()) {
            if (i != 0) builder.append("\n");
            builder.append("MIME Type [").append(i).append("]: ").append(part.getMimeType());
            i++;
        }
        return new ParsedContent(builder.toString());
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, ParsedContent string, Message message, CellHolderSpecs specs) {
        cellHolder.mTextView.setText(string.toString());
    }

    public class CellHolder extends AtlasCellFactory.CellHolder {
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
