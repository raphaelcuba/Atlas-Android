package com.layer.atlas.utilities.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

public class EmptyDelEditText extends EditText {
    private static final String TAG = EmptyDelEditText.class.getSimpleName();
    private OnEmptyDelListener mListener;

    public EmptyDelEditText(Context context) {
        super(context);
    }

    public EmptyDelEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyDelEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnEmptyDelListener(OnEmptyDelListener listener) {
        mListener = listener;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection c = super.onCreateInputConnection(outAttrs);
        // If not enabled, super returns null
        if (c == null) return null;
        return new EmptyDelInputConnection(c, true);
    }

    private class EmptyDelInputConnection extends InputConnectionWrapper {
        public EmptyDelInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean performEditorAction(int editorAction) {
            return super.performEditorAction(editorAction);
        }

        /**
         * Works with soft keyboard on Android 5 devices
         */
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (getText().length() == 0 && beforeLength == 1 && afterLength == 0) {
                Log.v(TAG, "deleteSurroundingText");
                if (mListener != null) return mListener.onEmptyDel(EmptyDelEditText.this);
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        /**
         * Works with soft keyboard on Android 4 devices
         */
        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_DEL && getText().length() == 0) {
                Log.v(TAG, "sendKeyEvent");
                if (mListener != null) return mListener.onEmptyDel(EmptyDelEditText.this);
            }
            return super.sendKeyEvent(event);
        }
    }

    /**
     * Works with hardware keyboard
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_DEL && getText().length() == 0) {
            Log.v(TAG, "onKeyUp");
            if (mListener != null) return mListener.onEmptyDel(EmptyDelEditText.this);
        }
        return super.onKeyUp(keyCode, event);
    }

    public interface OnEmptyDelListener {
        boolean onEmptyDel(EmptyDelEditText editText);
    }
}
