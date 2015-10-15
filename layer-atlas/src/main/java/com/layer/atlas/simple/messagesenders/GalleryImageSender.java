package com.layer.atlas.simple.messagesenders;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.layer.atlas.R;
import com.layer.atlas.simple.cells.PreviewImageCellFactory;
import com.layer.sdk.messaging.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class GalleryImageSender extends AttachmentSender {
    private static final String TAG = GalleryImageSender.class.getSimpleName();
    public static final int REQUEST_CODE = 47000;

    private final WeakReference<Activity> mActivity;

    public GalleryImageSender(String title, Integer icon, Activity activity) {
        super(title, icon);
        mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    public boolean send() {
        Activity activity = mActivity.get();
        if (activity == null) {
            Log.e(TAG, "Activity went out of scope.");
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(Intent.createChooser(intent, "Attach Photo"), REQUEST_CODE);
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, "Got result code: " + requestCode + ", data: " + data);
            return true;
        }

        String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
        try {
            Message message = PreviewImageCellFactory.newThreePartMessage(getLayerClient(), getContext(), data.getData());
            message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
            getConversation().send(message);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return true;
    }
}
