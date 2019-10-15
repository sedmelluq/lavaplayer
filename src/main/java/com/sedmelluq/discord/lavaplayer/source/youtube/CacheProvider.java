package com.sedmelluq.discord.lavaplayer.source.youtube;


public interface CacheProvider {

    /**
     * Caches the video format for the given identifier.
     * @param identifier The video ID.
     * @param format The track format, with a deciphered URL.
     * @param ttl The amount of time the URL lasts, in milliseconds.
     */
    void cacheVideoFormat(String identifier, YoutubeAudioTrack.FormatWithUrl format, long ttl);

    /**
     * Caches the unavailability reason for the given identifier.
     * @param identifier The video ID.
     * @param unavailableReason The reason why the video is unavailable.
     */
    void cacheUnavailableVideo(String identifier, String unavailableReason);

    /**
     * Called when a video format is unplayable for any reason, such as an invalid playback URL.
     * @param identifier The video ID.
     */
    void removeVideoFormat(String identifier);

    /**
     * @param identifier The video ID.
     * @return The JSON object, as a string, or null if it isn't cached.
     */
    YoutubeAudioTrack.FormatWithUrl getVideoFormat(String identifier);

    /**
     * @param identifier The video ID.
     * @return The reason why the video is unavailable, or null if it's not cached.
     */
    String checkUnavailable(String identifier);
}
