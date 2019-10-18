package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class CacheProviderImpl implements CacheProvider {

    private static final Cache<String, YoutubeAudioTrack.FormatWithUrl> playbackFormatCache = CacheBuilder.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .build();

    private static final Cache<String, String> unavailableCache = CacheBuilder.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .build();

    @Override
    public void cacheVideoFormat(String identifier, YoutubeAudioTrack.FormatWithUrl format, long ttl) {
        playbackFormatCache.put(identifier, format);
    }

    @Override
    public void cacheUnavailableVideo(String identifier, String unavailableReason) {
        unavailableCache.put(identifier, unavailableReason);
    }

    @Override
    public void removeVideoFormat(String identifier) {
        playbackFormatCache.invalidate(identifier);
    }

    @Override
    public YoutubeAudioTrack.FormatWithUrl getVideoFormat(String identifier) {
        return playbackFormatCache.getIfPresent(identifier);
    }

    @Override
    public String checkUnavailable(String identifier) {
        return unavailableCache.getIfPresent(identifier);
    }
}
