package com.layer.atlas.simple.cells;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

public class MimeCellFactory implements AtlasCellFactory<MimeCellFactory.CellHolder> {
    @Override
    public boolean isBindable(Message message) {
        return true;
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_text, cellView));
    }

    @Override
    public void bindCellHolder(CellHolder cellHolder, Message message, CellHolderSpecs specs) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (MessagePart part : message.getMessageParts()) {
            if (i != 0) builder.append("\n");
            builder.append("MIME Type [").append(i).append("]: ")
                    .append(part.getMimeType());
            i++;
        }
        cellHolder.mTextView.setText(builder.toString());
    }

    @Override
    public void onCache(Message message) {

    }

    class CellHolder extends AtlasCellFactory.CellHolder {
        TextView mTextView;

        public CellHolder(View view) {
            mTextView = (TextView) view.findViewById(R.id.cell_text);
        }
    }
}
