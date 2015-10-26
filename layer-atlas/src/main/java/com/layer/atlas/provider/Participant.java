package com.layer.atlas.provider;

/**
 * Participant allows Atlas classes to display information about users, like Message senders,
 * Conversation participants, TypingIndicator users, etc.
 */
public interface Participant {
    /**
     * Returns the name of this Participant.
     *
     * @return The name of this Participant.
     */
    String getName();

    /**
     * Returns the URL for an avatar image for this Participant.
     *
     * @return the URL for an avatar image for this Participant.
     */
    String getAvatarUrl();
}
