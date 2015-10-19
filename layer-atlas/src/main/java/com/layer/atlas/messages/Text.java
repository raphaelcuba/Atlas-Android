package com.layer.atlas.messages;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;

public class Text {

    public static class CellFactory extends AtlasCellFactory<CellFactory.CellHolder, CellFactory.ParsedContent> {

        public CellFactory() {
            super(2 * 1024 * 1024);
        }

        @Override
        public boolean isBindable(Message message) {
            return message.getMessageParts().get(0).getMimeType().startsWith("text/");
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

    public static class Sender extends TextSender {
        private static final String TAG = Sender.class.getSimpleName();

        private final int mMaxLength;

        public Sender(int maxNotificationLength) {
            mMaxLength = maxNotificationLength;
        }

        @Override
        public boolean send(String text) {
            if (text == null || text.trim().length() == 0) {
                Log.e(TAG, "No text to send");
                return false;
            }
            Message message = getLayerClient().newMessage(getLayerClient().newMessagePart(text));
            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            String notification = (text.length() < mMaxLength) ? text : (text.substring(0, mMaxLength) + "…");
            message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_text, myName, notification));
            getConversation().send(message);
            return true;
        }
    }


}
