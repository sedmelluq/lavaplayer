package com.sedmelluq.lavaplayer.core.source;

public interface AudioSourceRegistry extends AutoCloseable {
  void registerSource(AudioSource sourceManager);

  Iterable<AudioSource> getAllSources();

  <T extends AudioSource> T findSource(Class<T> klass);

  AudioSource findSource(String name);
}
