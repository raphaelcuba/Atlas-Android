/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas.messenger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.layer.atlas.old.Utils;
import com.layer.atlas.Participant;
import com.layer.atlas.old.Utils.Tools;
import com.layer.atlas.old.AtlasMessageComposer;
import com.layer.atlas.old.AtlasMessageListOld;
import com.layer.atlas.old.cells.Cell;
import com.layer.atlas.old.AtlasMessageListOld.ItemClickListener;
import com.layer.atlas.old.AtlasParticipantPicker;
import com.layer.atlas.AtlasTypingIndicator;
import com.layer.atlas.old.cells.ImageCell;
import com.layer.atlas.messenger.MessengerApp.keys;
import com.layer.atlas.simple.typingindicators.BubbleTypingIndicatorFactory;
import com.layer.sdk.LayerClient;
import com.layer.sdk.changes.LayerChangeEvent;
import com.layer.sdk.listeners.LayerChangeEventListener;
import com.layer.sdk.messaging.Conversation;
import com.layer.sdk.messaging.ConversationOptions;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;
import com.layer.sdk.query.Predicate;
import com.layer.sdk.query.Query;
import com.layer.sdk.query.SortDescriptor;

/**
 * @author Oleg Orlov
 * @since 14 Apr 2015
 */
public class MessengerMessagesScreen extends Activity {

    private static final String TAG = MessengerMessagesScreen.class.getSimpleName();
    private static final boolean debug = false;
    
    public static final String EXTRA_CONVERSATION_IS_NEW = "conversation.new";
    public static final String EXTRA_CONVERSATION_URI = keys.CONVERSATION_URI;
    
    public static final int REQUEST_CODE_SETTINGS = 101;
    public static final int REQUEST_CODE_GALLERY  = 111;
    public static final int REQUEST_CODE_CAMERA   = 112;
    
    /** Switch it to <code>true</code> to see {@link AtlasMessageListOld} Query support in action */
    private static final boolean USE_QUERY = false;
        
    private volatile Conversation conv;
    
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private Handler uiHandler;
    
    private AtlasMessageListOld messagesList;
    private AtlasMessageComposer messageComposer;
    private AtlasParticipantPicker participantsPicker;
    private AtlasTypingIndicator typingIndicator;
    
    private MessengerApp app;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.uiHandler = new Handler();
        this.app = (MessengerApp) getApplication();
        
        setContentView(R.layout.atlas_screen_messages);

        boolean convIsNew = getIntent().getBooleanExtra(EXTRA_CONVERSATION_IS_NEW, false);
        String convUri = getIntent().getStringExtra(EXTRA_CONVERSATION_URI);
        if (convUri != null) {
            Uri uri = Uri.parse(convUri);
            conv = app.getLayerClient().getConversation(uri);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(convUri.hashCode()); // Clear notifications for this Conversation
        }

        participantsPicker = (AtlasParticipantPicker) findViewById(R.id.atlas_screen_messages_participants_picker);
        participantsPicker.init(app.getParticipantProvider(), app.getLayerClient().getAuthenticatedUserId());
        if (convIsNew) {
            participantsPicker.setVisibility(View.VISIBLE);
        }
        
        messageComposer = (AtlasMessageComposer) findViewById(R.id.atlas_screen_messages_message_composer);
        messageComposer.init(app.getLayerClient()).setConversation(conv);
        messageComposer.setListener(new AtlasMessageComposer.Listener() {
            public boolean onBeforeSend(Message message) {
                boolean conversationReady = ensureConversationReady();
                if (!conversationReady) return false;

                // push
                preparePushMetadata(message);
                return true;
            }

        });
        
        messageComposer.registerAttachmentHandler("Photo", null, new OnClickListener() {
            public void onClick(View v) {
                if (!ensureConversationReady()) return;

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                String fileName = "cameraOutput" + System.currentTimeMillis() + ".jpg";
                photoFile = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);
                final Uri outputUri = Uri.fromFile(photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
                if (debug)
                    Log.w(TAG, "onClick() requesting photo to file: " + fileName + ", uri: " + outputUri);
                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
            }
        });

        messageComposer.registerAttachmentHandler("Image", null, new OnClickListener() {
            public void onClick(View v) {
                if (!ensureConversationReady()) return;

                // in onCreate or any event where your want the user to select a file
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_GALLERY);
            }
        });
        
        messageComposer.registerAttachmentHandler("Location", null, new OnClickListener() {
            public void onClick(View v) {
                if (!ensureConversationReady()) return;

                if (lastKnownLocation == null) {
                    Toast.makeText(v.getContext(), "Inserting Location: Location is unknown yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                String locationString = "{\"lat\":" + lastKnownLocation.getLatitude() + ", \"lon\":" + lastKnownLocation.getLongitude() + "}";
                MessagePart part = app.getLayerClient().newMessagePart(Utils.MIME_TYPE_ATLAS_LOCATION, locationString.getBytes());
                Message message = app.getLayerClient().newMessage(Arrays.asList(part));

                preparePushMetadata(message);
                conv.send(message);

                if (debug) Log.w(TAG, "onSendLocation() loc:  " + locationString);
            }
        });
        
        messagesList = (AtlasMessageListOld) findViewById(R.id.atlas_screen_messages_messages_list);
        messagesList.init(app.getLayerClient(), app.getParticipantProvider());
        if (USE_QUERY) {
            Query<Message> query = Query.builder(Message.class)
                    .predicate(new Predicate(Message.Property.CONVERSATION, Predicate.Operator.EQUAL_TO, conv))
                    .sortDescriptor(new SortDescriptor(Message.Property.POSITION, SortDescriptor.Order.ASCENDING))
                    .build();
            messagesList.setQuery(query);
        } else {
            messagesList.setConversation(conv);
        }
        
        messagesList.setItemClickListener(new ItemClickListener() {
            public void onItemClick(Cell cell) {
                if (Utils.MIME_TYPE_ATLAS_LOCATION.equals(cell.messagePart.getMimeType())) {
                    String jsonLonLat = new String(cell.messagePart.getData());
                    JSONObject json;
                    try {
                        json = new JSONObject(jsonLonLat);
                        double lon = json.getDouble("lon");
                        double lat = json.getDouble("lat");
                        Intent openMapIntent = new Intent(Intent.ACTION_VIEW);
                        String uriString = String.format(Locale.ENGLISH, "geo:%f,%f?z=%d&q=%f,%f", lat, lon, 18, lat, lon);
                        final Uri geoUri = Uri.parse(uriString);
                        openMapIntent.setData(geoUri);
                        if (openMapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(openMapIntent);
                            if (debug) Log.w(TAG, "onItemClick() starting Map: " + uriString);
                        } else {
                            if (debug) Log.w(TAG, "onItemClick() No Activity to start Map: " + geoUri);
                        }
                    } catch (JSONException ignored) {
                    }
                } else if (cell instanceof ImageCell) {
                    Intent intent = new Intent(MessengerMessagesScreen.this.getApplicationContext(), MessengerImageViewScreen.class);
                    app.setParam(intent, cell);
                    startActivity(intent);
                }
            }
        });
        
        typingIndicator = (AtlasTypingIndicator)findViewById(R.id.atlas_screen_messages_typing_indicator);
        typingIndicator.init(app.getLayerClient())
                .setConversation(conv)
                .setTypingIndicatorFactory(new BubbleTypingIndicatorFactory());
        
        // location manager for inserting locations:
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        prepareActionBar();
    }
    
    private void updateValues() {
        if (conv == null) {
            Log.e(TAG, "updateValues() no conversation set");
            return;
        }

        TextView titleText = (TextView) findViewById(R.id.atlas_actionbar_title_text);
        titleText.setText(Utils.getTitle(conv, app.getParticipantProvider(), app.getLayerClient().getAuthenticatedUserId()));
    }
    
    private boolean ensureConversationReady() {
        if (conv != null) return true;
        
        // create new one
        String[] userIds = participantsPicker.getSelectedUserIds();
        
        // no people, no conversation
        if (userIds.length == 0) {          
            Toast.makeText(this, "Conversation cannot be created without participants", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        conv = app.getLayerClient().newConversation(new ConversationOptions().distinct(false), userIds);
        participantsPicker.setVisibility(View.GONE);
        messageComposer.setConversation(conv);
        messagesList.setConversation(conv);
        typingIndicator.setConversation(conv);
        updateValues();
        return true;
    }
    
    private void preparePushMetadata(Message message) {
        Participant me = app.getParticipantProvider().getParticipant(app.getLayerClient().getAuthenticatedUserId());
        String senderName = me.getName();
        String text = Utils.Tools.toString(message);
        if (!text.isEmpty()) {
            if (senderName != null && !senderName.isEmpty()) {
                message.getOptions().pushNotificationMessage(senderName + ": " + text);
            } else {
                message.getOptions().pushNotificationMessage(text);
            }
        }
    }

    
    /** used to take photos from camera */
    private File photoFile = null; 
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) Log.w(TAG, "onActivityResult() requestCode: " + requestCode
                    + ", resultCode: " + resultCode
                    + ", uri: "  + (data == null ? "" : data.getData())
                    + ", data: " + (data == null ? "" : MessengerApp.toString(data.getExtras())) );
        
        if (resultCode != Activity.RESULT_OK) return;
        
        final LayerClient layerClient = ((MessengerApp) getApplication()).getLayerClient();
        
        switch (requestCode) {
            case REQUEST_CODE_CAMERA  :
                
                if (photoFile == null) {
                    if (debug) Log.w(TAG, "onActivityResult() taking photo, but output is undefined... ");
                    return;
                }
                if (!photoFile.exists()) {
                    if (debug) Log.w(TAG, "onActivityResult() taking photo, but photo file doesn't exist: " + photoFile.getPath());
                    return;
                }
                if (photoFile.length() == 0) {
                    if (debug) Log.w(TAG, "onActivityResult() taking photo, but photo file is empty: " + photoFile.getPath());
                    return;
                }
                
                try {
                    // prepare original
                    final File originalFile = photoFile;
                    FileInputStream fisOriginal = new FileInputStream(originalFile) {
                        public void close() throws IOException {
                            super.close();
                            boolean deleted = originalFile.delete();
                            if (debug) Log.w(TAG, "close() original file is" + (!deleted ? " not" : "") + " removed: " + originalFile.getName());
                            photoFile = null;
                        }
                    };
                    final MessagePart originalPart = layerClient.newMessagePart(Utils.MIME_TYPE_IMAGE_JPEG, fisOriginal, originalFile.length());
                    File tempDir = getCacheDir();
                    
                    MessagePart[] previewAndSize = Utils.buildPreviewAndSize(originalFile, layerClient, tempDir);
                    if (previewAndSize == null) {
                        Log.e(TAG, "onActivityResult() cannot build preview, cancel send...");
                        return;
                    }
                    Message msg = layerClient.newMessage(originalPart, previewAndSize[0], previewAndSize[1]);
                    if (debug) Log.w(TAG, "onActivityResult() sending photo... ");
                    preparePushMetadata(msg);
                    conv.send(msg);
                } catch (Exception e) {
                    Log.e(TAG, "onActivityResult() cannot insert photo" + e);
                }
                break;
            case REQUEST_CODE_GALLERY :
                if (data == null) {
                    if (debug) Log.w(TAG, "onActivityResult() insert from gallery: no data... :( ");
                    return;
                }
                // first check media gallery
                Uri selectedImageUri = data.getData();
                // TODO: Mi4 requires READ_EXTERNAL_STORAGE permission for such operation
                String selectedImagePath = getGalleryImagePath(selectedImageUri);
                String resultFileName = selectedImagePath;
                if (selectedImagePath != null) {
                    if (debug) Log.w(TAG, "onActivityResult() image from gallery selected: " + selectedImagePath);
                } else if (selectedImageUri.getPath() != null) { 
                    if (debug) Log.w(TAG, "onActivityResult() image from file picker appears... "  + selectedImageUri.getPath());
                    resultFileName = selectedImageUri.getPath();
                }
                
                if (resultFileName != null) {
                    String mimeType = Utils.MIME_TYPE_IMAGE_JPEG;
                    if (resultFileName.endsWith(".png")) mimeType = Utils.MIME_TYPE_IMAGE_PNG;
                    if (resultFileName.endsWith(".gif")) mimeType = Utils.MIME_TYPE_IMAGE_GIF;
                    
                    // test file copy locally
                    try {
                        // create message and upload content
                        InputStream fis = null;
                        File fileToUpload = new File(resultFileName);
                        if (fileToUpload.exists()) {
                            fis = new FileInputStream(fileToUpload);
                        } else {
                            if (debug) Log.w(TAG, "onActivityResult() file to upload doesn't exist, path: " + resultFileName + ", trying ContentResolver");
                            fis = getContentResolver().openInputStream(data.getData());
                            if (fis == null) {
                                if (debug) Log.w(TAG, "onActivityResult() cannot open stream with ContentResolver, uri: " + data.getData());
                            }
                        }
                        
                        String fileName = "galleryFile" + System.currentTimeMillis() + ".jpg";
                        final File originalFile = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName);

                        OutputStream fos = new FileOutputStream(originalFile);
                        int totalBytes = Tools.streamCopyAndClose(fis, fos);
                        
                        if (debug) Log.w(TAG, "onActivityResult() copied " + totalBytes + " to file: " + originalFile.getName());
                        
                        FileInputStream fisOriginal = new FileInputStream(originalFile) {
                            public void close() throws IOException {
                                super.close();
                                boolean deleted = originalFile.delete();
                                if (debug) Log.w(TAG, "close() original file is" + (!deleted ? " not" : "") + " removed: " + originalFile.getName());
                            }
                        };
                        final MessagePart originalPart = layerClient.newMessagePart(mimeType, fisOriginal, originalFile.length());
                        File tempDir = getCacheDir();
                        
                        MessagePart[] previewAndSize = Utils.buildPreviewAndSize(originalFile, layerClient, tempDir);
                        if (previewAndSize == null) {
                            Log.e(TAG, "onActivityResult() cannot build preview, cancel send...");
                            return;
                        }
                        Message msg = layerClient.newMessage(originalPart, previewAndSize[0], previewAndSize[1]);
                        if (debug) Log.w(TAG, "onActivityResult() uploaded " + originalFile.length() + " bytes");
                        preparePushMetadata(msg);
                        conv.send(msg);
                    } catch (Exception e) {
                        Log.e(TAG, "onActivityResult() cannot upload file: " + resultFileName, e);
                        return;
                    }
                }
                break;

            default :
                break;
        }
    }

    /**
     * pick file name from content provider with Gallery-flavor format
     */
    public String getGalleryImagePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor == null) {
            return null;        // uri could be not suitable for ContentProviders, i.e. points to file 
        }
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        updateValues();
        messagesList.jumpToLastMessage();
        
        // restore location tracking
        int requestLocationTimeout = 1 * 1000; // every second
        int distance = 100;
        Location loc = null;
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) { 
            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from gps: " + loc);
        }
        if (loc == null && locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (debug) Log.w(TAG, "onResume() location from network: " + loc);
        } 
        if (loc != null && loc.getTime() < System.currentTimeMillis() + LOCATION_EXPIRATION_TIME) {
            locationTracker.onLocationChanged(loc);
        }
        if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }
        if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, requestLocationTimeout, distance, locationTracker);
        }
        
        app.getLayerClient().registerEventListener(messagesList);
        app.getLayerClient().registerTypingIndicator(typingIndicator.clear());
        
        // when something changed
        app.getLayerClient().registerEventListener(new LayerChangeEventListener.MainThread() {
            public void onEventMainThread(LayerChangeEvent event) {
                updateValues();
            }
        });
    }
    
    private static final int LOCATION_EXPIRATION_TIME = 60 * 1000; // 1 minute 
    
    LocationListener locationTracker = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastKnownLocation = location;
                    if (debug) Log.d(TAG, "onLocationChanged() location: " + location);
                }
            });
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        
        locationManager.removeUpdates(locationTracker);
        
        app.getLayerClient().unregisterEventListener(messagesList);
        app.getLayerClient().unregisterTypingIndicator(typingIndicator.clear());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (debug) Log.w(TAG, "onConfigurationChanged() newConfig: " + newConfig);
        updateValues();
        messagesList.jumpToLastMessage();
    }
    
    private void prepareActionBar() {
        ImageView menuBtn = (ImageView) findViewById(R.id.atlas_actionbar_left_btn);
        menuBtn.setImageResource(R.drawable.atlas_ctl_btn_back);
        menuBtn.setVisibility(View.VISIBLE);
        menuBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setText("Messages");
        ((TextView)findViewById(R.id.atlas_actionbar_title_text)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                messagesList.requestRefreshValues(true, false);
            }
        });
        ImageView settingsBtn = (ImageView) findViewById(R.id.atlas_actionbar_right_btn);
        settingsBtn.setImageResource(R.drawable.atlas_ctl_btn_detail);
        settingsBtn.setVisibility(View.VISIBLE);
        settingsBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (conv == null) return; 
                MessengerConversationSettingsScreen.conv = conv;
                Intent intent = new Intent(v.getContext(), MessengerConversationSettingsScreen.class);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
            }
        });
        Tools.setStatusBarColor(getWindow(), getResources().getColor(R.color.atlas_background_blue_dark));
    }

}
