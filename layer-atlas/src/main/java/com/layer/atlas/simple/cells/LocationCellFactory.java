package com.layer.atlas.simple.cells;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.layer.atlas.AtlasCellFactory;
import com.layer.atlas.R;
import com.layer.atlas.simple.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.List;

import static com.layer.atlas.simple.cells.ThreePartImageUtils.scaleDownInside;

public class LocationCellFactory extends AtlasCellFactory<LocationCellFactory.CellHolder, LocationUtils.Location> {
    public static final String TAG = LocationCellFactory.class.getSimpleName();
    private static final int PLACEHOLDER_RES_ID = R.drawable.atlas_message_item_cell_placeholder;
    private static final double GOLDEN_RATIO = (1.0 + Math.sqrt(5.0)) / 2.0;

    private final Picasso mPicasso;
    private final Transformation mTransform;

    public LocationCellFactory(Context context, Picasso picasso) {
        super(32 * 1024);
        mPicasso = picasso;
        float radius = context.getResources().getDimension(R.dimen.atlas_message_item_cell_radius);
        mTransform = new RoundedTransform(radius);
    }

    @Override
    public boolean isBindable(Message message) {
        List<MessagePart> parts = message.getMessageParts();
        return parts.size() == 1 && parts.get(0).getMimeType().equals(LocationUtils.MIME_TYPE);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.cell_image, cellView, true));
    }

    @Override
    public LocationUtils.Location cache(Message message) {
        return LocationUtils.getMessageLocation(message);
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, LocationUtils.Location location, final Message message, CellHolderSpecs specs) {
        // Google Static Map API has max dimension 640
        int mapWidth = Math.min(640, specs.maxWidth);
        int mapHeight = (int) Math.round((double) mapWidth / GOLDEN_RATIO);
        final String url = new StringBuilder()
                .append("https://maps.googleapis.com/maps/api/staticmap?zoom=16&maptype=roadmap&scale=2&")
                .append("center=").append(location.mLatitude).append(",").append(location.mLongitude).append("&")
                .append("size=").append(mapWidth).append("x").append(mapHeight).append("&")
                .append("markers=color:red%7C").append(location.mLatitude).append(",").append(location.mLongitude)
                .toString();

        int[] cellDims = scaleDownInside(specs.maxWidth, (int) Math.round((double) specs.maxWidth / GOLDEN_RATIO), specs.maxWidth, specs.maxHeight);
        cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
        mPicasso.load(url).tag(TAG).noFade().placeholder(PLACEHOLDER_RES_ID)
                .resize(cellDims[0], cellDims[1])
                .centerCrop().transform(mTransform).into(cellHolder.mImageView);
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
        }
    }

}
