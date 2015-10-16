package com.layer.atlas.messages;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

/**
 * BasicImage handles non-ThreePartImage images.  It relies on the ThreePartImage RequestHandler and does not handle image rotation.
 */
public class BasicImage {
    public static class CellFactory extends AtlasCellFactory<BasicImage.CellFactory.CellHolder, BasicImage.CellFactory.ParsedContent> {
        private static final String TAG = CellFactory.class.getSimpleName();
        private static final int PLACEHOLDER_RES_ID = com.layer.atlas.R.drawable.atlas_message_item_cell_placeholder;

        private final Picasso mPicasso;
        private final Transformation mTransform;

        public CellFactory(Context context, Picasso picasso) {
            super(1024);
            mPicasso = picasso;
            float radius = context.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
            mTransform = new RoundedTransform(radius);
        }

        @Override
        public boolean isBindable(Message message) {
            for (MessagePart part : message.getMessageParts()) {
                if (part.getMimeType().startsWith("image/")) return true;
            }
            return false;
        }

        @Override
        public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
            return new CellHolder(layoutInflater.inflate(R.layout.cell_image, cellView, true));
        }

        @Override
        public void bindCellHolder(final CellHolder cellHolder, ParsedContent index, final Message message, CellHolderSpecs specs) {
            MessagePart part = message.getMessageParts().get(index.mIndex);
            mPicasso.load(part.getId()).tag(TAG).placeholder(PLACEHOLDER_RES_ID).noFade().centerInside()
                    .resize(specs.maxWidth, specs.maxHeight).onlyScaleDown().transform(mTransform)
                    .into(cellHolder.mImageView);
        }

        @Override
        public void onScrollStateChanged(int newState) {
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    mPicasso.pauseTag(TAG);
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                case RecyclerView.SCROLL_STATE_SETTLING:
                    mPicasso.resumeTag(TAG);
                    break;
            }
        }

        @Override
        public ParsedContent parseContent(Message message) {
            int i = 0;
            for (MessagePart part : message.getMessageParts()) {
                if (part.getMimeType().startsWith("image/")) return new ParsedContent(i);
                i++;
            }
            return null;
        }

        public static class CellHolder extends AtlasCellFactory.CellHolder {
            ImageView mImageView;

            public CellHolder(View view) {
                mImageView = (ImageView) view.findViewById(R.id.cell_image);
            }
        }

        public static class ParsedContent implements AtlasCellFactory.ParsedContent {
            public final int mIndex;

            public ParsedContent(int index) {
                mIndex = index;
            }

            @Override
            public int sizeOf() {
                return Integer.SIZE / Byte.SIZE;
            }
        }
    }


}
