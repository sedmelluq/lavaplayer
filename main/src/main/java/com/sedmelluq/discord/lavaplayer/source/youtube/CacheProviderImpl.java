package com.sedmelluq.discord.lavaplayer.source.youtube;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class CacheProviderImpl implements CacheProvider {

  private static final Cache<String, YoutubeAudioTrack.FormatWithUrl> playbackFormatCache = Caffeine.newBuilder()
      .expireAfterWrite(4, TimeUnit.HOURS)
      .build();

  private static final Cache<String, String> unavailableCache = Caffeine.newBuilder()
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
