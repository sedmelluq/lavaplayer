package com.sedmelluq.discord.lavaplayer.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * An audio filter which may consist of a number of other filters.
 */
public abstract class CompositeAudioFilter implements UniversalPcmAudioFilter {
  private static final Logger log = LoggerFactory.getLogger(CompositeAudioFilter.class);

  @Override
  public void seekPerformed(long requestedTime, long providedTime) {
    for (AudioFilter filter : getFilters()) {
      try {
        filter.seekPerformed(requestedTime, providedTime);
      } catch (Exception e) {
        log.error("Notifying filter {} of seek failed with exception.", filter.getClass(), e);
      }
    }
  }

  @Override
  public void flush() throws InterruptedException {
    for (AudioFilter filter : getFilters()) {
      try {
        filter.flush();
      } catch (Exception e) {
        log.error("Flushing filter {} failed with exception.", filter.getClass(), e);
      }
    }
  }

  @Override
  public void close() {
    for (AudioFilter filter : getFilters()) {
      try {
        filter.close();
      } catch (Exception e) {
        log.error("Closing filter {} failed with exception.", filter.getClass(), e);
      }
    }
  }

  protected abstract List<AudioFilter> getFilters();
}
