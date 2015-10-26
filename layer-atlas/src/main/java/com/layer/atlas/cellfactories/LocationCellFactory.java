package com.layer.atlas.cellfactories;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.layer.atlas.R;
import com.layer.atlas.utilities.Utils;
import com.layer.atlas.utilities.picasso.transformations.RoundedTransform;
import com.layer.sdk.messaging.Message;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class LocationCellFactory extends AtlasCellFactory<LocationCellFactory.CellHolder, LocationCellFactory.ParsedContent> {
    private static final String TAG = LocationCellFactory.class.getSimpleName();
    public static final String MIME_TYPE = "location/coordinate";
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

    public static boolean isType(Message message) {
        return message.getMessageParts().get(0).getMimeType().equals(MIME_TYPE);
    }

    public static String getPreview(Message message) {
        return "Attachment: Location";
    }

    @Override
    public boolean isBindable(Message message) {
        return LocationCellFactory.isType(message);
    }

    @Override
    public CellHolder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater) {
        return new CellHolder(layoutInflater.inflate(R.layout.atlas_message_item_cell_image, cellView, true));
    }

    @Override
    public ParsedContent parseContent(Message message) {
        try {
            JSONObject o = new JSONObject(new String(message.getMessageParts().get(0).getData()));
            ParsedContent c = new ParsedContent();
            c.mLatitude = o.optDouble("lat", 0);
            c.mLongitude = o.optDouble("lon", 0);
            c.mLabel = o.optString("label", null);
            return c;
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void bindCellHolder(final CellHolder cellHolder, final ParsedContent location, Message message, CellHolderSpecs specs) {
        // Google Static Map API has max dimension 640
        int mapWidth = Math.min(640, specs.maxWidth);
        int mapHeight = (int) Math.round((double) mapWidth / GOLDEN_RATIO);
        final String url = new StringBuilder()
                .append("https://maps.googleapis.com/maps/api/staticmap?zoom=16&maptype=roadmap&scale=2&")
                .append("center=").append(location.mLatitude).append(",").append(location.mLongitude).append("&")
                .append("size=").append(mapWidth).append("x").append(mapHeight).append("&")
                .append("markers=color:red%7C").append(location.mLatitude).append(",").append(location.mLongitude)
                .toString();

        final int[] cellDims = Utils.scaleDownInside(specs.maxWidth, (int) Math.round((double) specs.maxWidth / GOLDEN_RATIO), specs.maxWidth, specs.maxHeight);
        cellHolder.mImageView.setLayoutParams(new FrameLayout.LayoutParams(cellDims[0], cellDims[1]));
        mPicasso.load(url).tag(TAG).noFade().placeholder(PLACEHOLDER_RES_ID)
                .resize(cellDims[0], cellDims[1])
                .centerCrop().transform(mTransform).into(cellHolder.mImageView);

        // TODO: register click listeners with CellFactories?
        cellHolder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String encodedLabel = (location.mLabel == null) ? URLEncoder.encode("Shared Marker") : URLEncoder.encode(location.mLabel);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + location.mLatitude + "," + location.mLongitude + "(" + encodedLabel + ")&z=16"));
                cellHolder.mImageView.getContext().startActivity(intent);
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

    static class ParsedContent implements AtlasCellFactory.ParsedContent {
        double mLatitude;
        double mLongitude;
        String mLabel;

        @Override
        public int sizeOf() {
            return (mLabel == null ? 0 : mLabel.getBytes().length) + ((Double.SIZE + Double.SIZE) / Byte.SIZE);
        }
    }

    static class CellHolder extends AtlasCellFactory.CellHolder {
        ImageView mImageView;

        public CellHolder(View view) {
            mImageView = (ImageView) view.findViewById(R.id.cell_image);
        }
    }
}
