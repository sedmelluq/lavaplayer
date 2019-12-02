package com.sedmelluq.lavaplayer.core.source;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAudioSourceRegistry implements AudioSourceRegistry {
  private final Map<String, AudioSource> sources = new LinkedHashMap<>();

  @Override
  public void registerSource(AudioSource source) {
    if (sources.putIfAbsent(source.getName(), source) != null) {
      throw new IllegalStateException("Source with name " + source.getName() + " already present.");
    }
  }

  @Override
  public Iterable<AudioSource> getAllSources() {
    return sources.values();
  }

  @Override
  public <T extends AudioSource> T findSource(Class<T> klass) {
    for (AudioSource source : sources.values()) {
      if (klass.isAssignableFrom(source.getClass())) {
        return klass.cast(source);
      }
    }

    return null;
  }

  @Override
  public AudioSource findSource(String name) {
    return sources.get(name);
  }

  @Override
  public void close() throws Exception {
    for (AudioSource source : sources.values()) {
      source.close();
    }
  }
}
