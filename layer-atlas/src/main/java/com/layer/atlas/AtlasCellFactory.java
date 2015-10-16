package com.layer.atlas;

import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.layer.sdk.messaging.Message;

/**
 * CellFactories manage one or more types ot Messages for display within an AtlasMessagesAdapter.
 */
public abstract class AtlasCellFactory<Tholder extends AtlasCellFactory.CellHolder, Tcache extends AtlasCellFactory.ParsedContent> {
    private final LruCache<String, Tcache> mCache;

    public AtlasCellFactory(int cacheBytes) {
        mCache = new LruCache<String, Tcache>(cacheBytes) {
            @Override
            protected int sizeOf(String key, Tcache value) {
                return value.sizeOf();
            }
        };
    }

    /**
     * Returns `true` if this CellFactory can create and bind a CellHolder for the given Message, or
     * `false` otherwise.
     *
     * @param message Message to analyze for manageability.
     * @return `true` if this CellFactory manages the given Message, or `false` otherwise.
     */
    public abstract boolean isBindable(Message message);

    /**
     * This method must perform two actions.  First, any required View hierarchy for rendering this
     * CellFactory's Messages must be added to the provided `cellView` - either by inflating a
     * layout (e.g. <merge>...</merge>), or by adding Views programmatically - and second, creating
     * and returning a CellHolder.  The CellHolder gets passed into bindCellHolder() when rendering
     * a Message and should contain all View references necessary for rendering the Message there.
     *
     * @param cellView       ViewGroup to add necessary Message Views to.
     * @param isMe`true`     if this Message was sent by the authenticated user, or `false`.
     * @param layoutInflater Convenience Inflater for inflating layouts.
     * @return CellHolder with all View references required for binding Messages to Views.
     */
    public abstract Tholder createCellHolder(ViewGroup cellView, boolean isMe, LayoutInflater layoutInflater);

    /**
     * Provides an opportunity to parse this AtlasCellFactory Message data in a background thread.
     *
     * @param message Message to parse
     * @return Cacheable parsed object generated from the given Message
     */
    public abstract Tcache parseContent(Message message);

    /**
     * Renders a Message by applying data to the provided CellHolder.  The CellHolder was previously
     * created in createCellHolder().
     *
     * @param cellHolder CellHolder to bind with Message data.
     * @param cached
     * @param message    Message to bind to the CellHolder.
     * @param specs      Information about the CellHolder.
     */
    public abstract void bindCellHolder(Tholder cellHolder, Tcache cached, Message message, CellHolderSpecs specs);

    /**
     * Override to handle RecyclerView scrolling.  Example: pause and resume Picasso loads.
     *
     * @param newState Scroll state of the RecyclerView.
     * @see RecyclerView#SCROLL_STATE_IDLE
     * @see RecyclerView#SCROLL_STATE_DRAGGING
     * @see RecyclerView#SCROLL_STATE_SETTLING
     */
    public void onScrollStateChanged(int newState) {

    }

    /**
     * Returns previously parsed content for this Message, or calls parseContent() if it has not
     * been previously parsed.
     *
     * @param message Message to return parsed content object for.
     * @return Parsed content object for the given Message.
     */
    public Tcache getParsedContent(Message message) {
        String id = message.getId().toString();
        Tcache value = mCache.get(id);
        if (value != null) return value;
        value = parseContent(message);
        if (value != null) mCache.put(id, value);
        return value;
    }

    /**
     * CellHolders maintain a reference to their Message, and allow the capture of user interactions
     * with their messages (e.g. clicks).  CellHolders can be extended to act as View caches, where
     * createCellHolder() might populate a CellHolder with references to Views for use in future
     * calls to bindCellHolder().
     */
    public static abstract class CellHolder implements View.OnClickListener, View.OnLongClickListener {
        private OnClickListener mClickListener;
        private Message mMessage;

        public CellHolder setClickableView(View clickableView) {
            clickableView.setOnClickListener(this);
            clickableView.setOnLongClickListener(this);
            return this;
        }

        public CellHolder setMessage(Message message) {
            mMessage = message;
            return this;
        }

        public Message getMessage() {
            return mMessage;
        }

        public CellHolder setClickListener(OnClickListener clickListener) {
            mClickListener = clickListener;
            return this;
        }

        @Override
        public void onClick(View v) {
            if (mClickListener == null) return;
            mClickListener.onClick(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mClickListener == null) return false;
            return mClickListener.onLongClick(this);
        }

        public interface OnClickListener {
            void onClick(CellHolder cellHolder);

            boolean onLongClick(CellHolder cellHolder);
        }
    }

    /**
     * CellHolderSpecs contains CellHolder specifications for use during binding.
     */
    public static class CellHolderSpecs {
        // True if the CellHolder is for my message, or false if for a remote user.
        public boolean isMe;

        // Position of the CellHolder in the AtlasMessagesList.
        public int position;

        // Maximum width allowed for the CellHolder, useful when resizing images.
        public int maxWidth;

        // Maximum height allowed for the CellHolder, useful when resizing images.
        public int maxHeight;
    }

    /**
     * Object intended to hold parsed content generated from a Message's MessagePart data arrays.
     * When parsing takes time, like parsing serialized JSON, this can improve UI performance by
     * pre-caching parsed content off the main thread, and providing the parsed content when binding
     * cell holders.
     */
    public interface ParsedContent {
        /**
         * Returns the size of this ParsedContent in bytes.
         *
         * @return The size of this ParsedContent in bytes.
         */
        int sizeOf();
    }
}
