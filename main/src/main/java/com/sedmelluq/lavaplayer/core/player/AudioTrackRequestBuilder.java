package com.sedmelluq.lavaplayer.core.player;

import com.sedmelluq.lavaplayer.core.player.marker.TrackMarker;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.source.AudioSource;

public class AudioTrackRequestBuilder implements AudioTrackRequest {
  private final AudioTrackInfo trackInfo;
  private AudioSource sourceManager;
  private boolean replaceExisting = true;
  private TrackMarker initialMarker;
  private long initialPosition;
  private Object initialUserData;

  public AudioTrackRequestBuilder(AudioTrackInfo trackInfo) {
    this.trackInfo = trackInfo;
  }

  public AudioTrackRequestBuilder withSourceManager(AudioSource sourceManager) {
    this.sourceManager = sourceManager;
    return this;
  }

  public AudioTrackRequestBuilder withReplaceExisting(boolean replaceExisting) {
    this.replaceExisting = replaceExisting;
    return this;
  }

  public AudioTrackRequestBuilder withInitialPosition(long initialPosition) {
    this.initialPosition = initialPosition;
    return this;
  }

  public AudioTrackRequestBuilder withUserData(Object initialUserData) {
    this.initialUserData = initialUserData;
    return this;
  }

  public AudioTrackRequestBuilder withInitialMarker(TrackMarker initialMarker) {
    this.initialMarker = initialMarker;
    return this;
  }

  public AudioTrackRequestBuilder build() {
    // Request is always handled synchronously in the same thread.
    return this;
  }

  public AudioTrackInfo getTrackInfo() {
    return trackInfo;
  }

  @Override
  public long getInitialPosition() {
    return initialPosition;
  }

  @Override
  public TrackMarker getInitialMarker() {
    return initialMarker;
  }

  @Override
  public Object getUserData() {
    return initialUserData;
  }

  @Override
  public AudioSource getSource() {
    return sourceManager;
  }

  @Override
  public boolean getReplaceExisting() {
    return replaceExisting;
  }
}
