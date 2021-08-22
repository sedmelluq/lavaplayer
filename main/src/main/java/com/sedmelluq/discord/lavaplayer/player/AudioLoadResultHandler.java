package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackCollection;

/**
 * Handles the result of loading an item from an audio player manager.
 */
public interface AudioLoadResultHandler {
    /**
     * Called when the requested item is a track and it was successfully loaded.
     *
     * @param track The loaded track
     */
    void trackLoaded(AudioTrack track);

    /**
     * Called when the requested item is a track collection and was successfully loaded.
     *
     * @param collection The loaded collection.
     */
    void collectionLoaded(AudioTrackCollection collection);

    /**
     * Called when there were no items found by the specified identifier.
     */
    void noMatches();

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    void loadFailed(FriendlyException exception);
}
