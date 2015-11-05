package com.layer.atlas.imagepopup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.layer.atlas.R;
import com.layer.atlas.cellfactories.ThreePartImageCellFactory;
import com.layer.sdk.LayerClient;

public class AtlasImagePopupActivity extends Activity {
    SubsamplingScaleImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(R.color.atlas_image_popup_background);
        setContentView(R.layout.atlas_image_popup);
        mImageView = (SubsamplingScaleImageView) findViewById(R.id.image_popup);

        mImageView.setPanEnabled(true);
        mImageView.setZoomEnabled(true);
        mImageView.setDoubleTapZoomDpi(160);
        mImageView.setMinimumDpi(80);
        mImageView.setBitmapDecoderClass(MessagePartDecoder.class);
        mImageView.setRegionDecoderClass(MessagePartRegionDecoder.class);

        Intent intent = getIntent();
        if (intent == null) return;
        Uri fullId = intent.getParcelableExtra("fullId");
        Uri previewId = intent.getParcelableExtra("previewId");
        ThreePartImageCellFactory.ParsedContent info = intent.getParcelableExtra("info");

        if (previewId != null && info != null) {
            switch (info.orientation) {
                case ThreePartImageCellFactory.ORIENTATION_0:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_0);
                    break;
                case ThreePartImageCellFactory.ORIENTATION_90:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_270);
                    break;
                case ThreePartImageCellFactory.ORIENTATION_180:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_180);
                    break;
                case ThreePartImageCellFactory.ORIENTATION_270:
                    mImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
                    break;
            }
            mImageView.setImage(
                    ImageSource.uri(fullId).dimensions(info.width, info.height),
                    ImageSource.uri(previewId));
        } else {
            // BasicImage
            mImageView.setImage(ImageSource.uri(fullId));
        }
    }

    public static void init(LayerClient layerClient) {
        MessagePartDecoder.init(layerClient);
        MessagePartRegionDecoder.init(layerClient);
    }
}
