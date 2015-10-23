package com.layer.atlas.utilviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

public class EmptyDelEditText extends EditText {
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
        return new EmptyDelInputConnection(super.onCreateInputConnection(outAttrs), true);
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
         * This seems to work on Android 5 devices
         */
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (getText().length() == 0 && beforeLength == 1 && afterLength == 0) {
                if (mListener != null) return mListener.onEmptyDel(EmptyDelEditText.this);
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }

        /**
         * This seems to work on Android 4 devices
         */
        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_DEL && getText().length() == 0) {
                if (mListener != null) return mListener.onEmptyDel(EmptyDelEditText.this);
            }
            return super.sendKeyEvent(event);
        }
    }

    public interface OnEmptyDelListener {
        boolean onEmptyDel(EmptyDelEditText editText);
    }
}
