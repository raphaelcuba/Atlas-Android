package com.layer.atlas.cellfactories;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.imagepopup.AtlasImagePopupActivity;
import com.layer.atlas.utilities.picasso.transformations.RoundedTransform;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;

/**
 * BasicImage handles non-ThreePartImage images.  It relies on the ThreePartImage RequestHandler and does not handle image rotation.
 */
public class BasicImageCellFactory extends AtlasCellFactory<BasicImageCellFactory.CellHolder, BasicImageCellFactory.ParsedContent> {
    private static final String TAG = BasicImageCellFactory.class.getSimpleName();
    private static final int PLACEHOLDER_RES_ID = com.layer.atlas.R.drawable.atlas_message_item_cell_placeholder;

    private final WeakReference<Activity> mActivity;
    private final LayerClient mLayerClient;
    private final Picasso mPicasso;
    private final Transformation mTransform;

    public BasicImageCellFactory(Activity activity, LayerClient layerClient, Picasso picasso) {
        super(1024);
        mActivity = new WeakReference<Activity>(activity);
        mLayerClient = layerClient;
        mPicasso = picasso;
        float radius = activity.getResources().getDimension(com.layer.atlas.R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }

    @Override
    public boolean isBindable(Message message) {
        return BasicImageCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, ParsedContent index, Message message, CellHolderSpecs specs) {
        final MessagePart part = message.getMessageParts().get(index.mIndex);
        mPicasso.load(part.getId()).tag(TAG).placeholder(PLACEHOLDER_RES_ID).noFade().centerInside()
                .resize(specs.maxWidth, specs.maxHeight).onlyScaleDown().transform(mTransform)
                .into(cellHolder.mImageView);

        cellHolder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AtlasImagePopupActivity.init(mLayerClient);
                Activity activity = mActivity.get();
                if (activity == null) return;
                Intent intent = new Intent(activity, AtlasImagePopupActivity.class);
                intent.putExtra("fullId", part.getId());

                if (Build.VERSION.SDK_INT >= 21) {
                    activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity, cellHolder.mImageView, "image").toBundle());
                } else {
                    activity.startActivity(intent);
                }
            }
        });
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


    //==============================================================================================
    // Static utilities
    //==============================================================================================

    public static boolean isType(Message message) {
        for (MessagePart part : message.getMessageParts()) {
            if (part.getMimeType().startsWith("image/")) return true;
        }
        return false;
    }

    public static String getMessagePreview(Message message) {
        return "Attachment: Image";
    }


    //==============================================================================================
    // Inner classes
    //==============================================================================================

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
