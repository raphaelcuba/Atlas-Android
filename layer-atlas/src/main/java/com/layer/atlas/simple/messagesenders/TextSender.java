package com.layer.atlas.simple.messagesenders;

public abstract class TextSender extends MessageSender<String> {
    private static final String TAG = TextSender.class.getSimpleName();

    public abstract boolean send(String text);
}