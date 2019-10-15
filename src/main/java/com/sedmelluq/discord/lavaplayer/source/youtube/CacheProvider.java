package com.sedmelluq.discord.lavaplayer.source.youtube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CacheProvider {

    /**
     * Caches the video format for the given identifier.
     * @param identifier The video ID.
     * @param format The track format, with a deciphered URL.
     * @param ttl The amount of time the URL lasts, in milliseconds.
     */
    void cacheVideoFormat(@Nonnull String identifier, @Nonnull YoutubeAudioTrack.FormatWithUrl format, long ttl);

    /**
     * Caches the unavailability reason for the given identifier.
     * @param identifier The video ID.
     * @param unavailableReason The reason why the video is unavailable.
     */
    void cacheUnavailableVideo(@Nonnull String identifier, @Nonnull String unavailableReason);

    /**
     * Called when a video format is unplayable for any reason, such as an invalid playback URL.
     * @param identifier The video ID.
     */
    void removeVideoFormat(@Nonnull String identifier);

    /**
     * @param identifier The video ID.
     * @return The JSON object, as a string, or null if it isn't cached.
     */
    @Nullable
    YoutubeAudioTrack.FormatWithUrl getVideoFormat(@Nonnull String identifier);

    /**
     * @param identifier The video ID.
     * @return The reason why the video is unavailable, or null if it's not cached.
     */
    @Nullable
    String checkUnavailable(@Nonnull String identifier);
}
