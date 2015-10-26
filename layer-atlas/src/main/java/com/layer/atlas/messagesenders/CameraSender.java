package com.layer.atlas.messagesenders;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.layer.atlas.R;
import com.layer.atlas.utilities.Utils;
import com.layer.sdk.messaging.Message;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

public class CameraSender extends AttachmentSender {
    private static final String TAG = CameraSender.class.getSimpleName();
    public static final int REQUEST_CODE = 47002;

    private final WeakReference<Activity> mActivity;
    private final AtomicReference<String> mPhotoFilePath = new AtomicReference<String>(null);

    public CameraSender(String title, Integer icon, Activity activity) {
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

        String fileName = "cameraOutput" + System.currentTimeMillis() + ".jpg";
        File file = new File(getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);
        mPhotoFilePath.set(file.getAbsolutePath());
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final Uri outputUri = Uri.fromFile(file);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        activity.startActivityForResult(cameraIntent, REQUEST_CODE);
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, "Got result code: " + requestCode + ", data: " + data);
            return true;
        }

        try {
            String myName = getParticipantProvider().getParticipant(getLayerClient().getAuthenticatedUserId()).getName();
            Message message = Utils.newThreePartImageMessage(getLayerClient(), new File(mPhotoFilePath.get()));
            message.getOptions().pushNotificationMessage(getContext().getString(R.string.atlas_notification_image, myName));
            getConversation().send(message);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return true;
    }

    @Override
    public void onActivityCreate(Bundle savedInstanceState) {
        mPhotoFilePath.set(savedInstanceState.getString(CameraSender.class.getSimpleName() + ".photoFilePath", null));
    }

    @Override
    public void onActivitySaveInstanceState(Bundle outState) {
        outState.putString(CameraSender.class.getSimpleName() + ".photoFilePath", mPhotoFilePath.get());
    }
}
