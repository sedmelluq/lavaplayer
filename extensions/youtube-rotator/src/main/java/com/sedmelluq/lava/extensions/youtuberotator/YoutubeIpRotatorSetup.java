package com.sedmelluq.lava.extensions.youtuberotator;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpClientBuilder;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.http.SimpleHttpClientConnectionManager;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import java.util.ArrayList;
import java.util.List;

public class YoutubeIpRotatorSetup {
  private static final int DEFAULT_RETRY_LIMIT = 4;
  private static final HttpContextFilter DEFAULT_DELEGATE = new YoutubeHttpContextFilter();
  private static final YoutubeIpRotatorRetryHandler RETRY_HANDLER = new YoutubeIpRotatorRetryHandler();

  private final AbstractRoutePlanner routePlanner;
  private final List<ExtendedHttpConfigurable> mainConfiguration;
  private final List<ExtendedHttpConfigurable> searchConfiguration;
  private int retryLimit = DEFAULT_RETRY_LIMIT;
  private HttpContextFilter mainDelegate = DEFAULT_DELEGATE;
  private HttpContextFilter searchDelegate = null;

  public YoutubeIpRotatorSetup(AbstractRoutePlanner routePlanner) {
    this.routePlanner = routePlanner;
    mainConfiguration = new ArrayList<>();
    searchConfiguration = new ArrayList<>();
  }

  public YoutubeIpRotatorSetup forConfiguration(ExtendedHttpConfigurable configurable, boolean isSearch) {
    if (isSearch) {
      searchConfiguration.add(configurable);
    } else {
      mainConfiguration.add(configurable);
    }

    return this;
  }

  public YoutubeIpRotatorSetup forSource(YoutubeAudioSourceManager sourceManager) {
    forConfiguration(sourceManager.getMainHttpConfiguration(), false);
    forConfiguration(sourceManager.getSearchHttpConfiguration(), true);
    forConfiguration(sourceManager.getSearchMusicHttpConfiguration(), true);
    return this;
  }

  public YoutubeIpRotatorSetup forManager(AudioPlayerManager playerManager) {
    YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);

    if (sourceManager != null) {
      forSource(sourceManager);
    }

    return this;
  }

  public YoutubeIpRotatorSetup withRetryLimit(int retryLimit) {
    this.retryLimit = retryLimit;
    return this;
  }

  public YoutubeIpRotatorSetup withMainDelegateFilter(HttpContextFilter filter) {
    this.mainDelegate = filter;
    return this;
  }

  public YoutubeIpRotatorSetup withSearchDelegateFilter(HttpContextFilter filter) {
    this.searchDelegate = filter;
    return this;
  }

  public void setup() {
    apply(mainConfiguration, new YoutubeIpRotatorFilter(mainDelegate, false, routePlanner, retryLimit));
    apply(searchConfiguration, new YoutubeIpRotatorFilter(searchDelegate, true, routePlanner, retryLimit));
  }

  protected void apply(List<ExtendedHttpConfigurable> configurables, YoutubeIpRotatorFilter filter) {
    for (ExtendedHttpConfigurable configurable : configurables) {
      configurable.configureBuilder(builder ->
          ((ExtendedHttpClientBuilder) builder).setConnectionManagerFactory(SimpleHttpClientConnectionManager::new)
      );

      configurable.configureBuilder(it -> {
        it.setRoutePlanner(routePlanner);
        // No retry for some exceptions we know are hopeless for retry.
        it.setRetryHandler(RETRY_HANDLER);
        // Regularly cleans up per-route connection pool which gets huge due to many routes caused by
        // each request having an unique route.
        it.evictExpiredConnections();
      });

      configurable.setHttpContextFilter(filter);
    }
  }
}
