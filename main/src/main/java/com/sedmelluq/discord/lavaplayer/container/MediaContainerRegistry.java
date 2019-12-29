package com.sedmelluq.discord.lavaplayer.container;

import java.util.List;

public class MediaContainerRegistry {
  public static final MediaContainerRegistry DEFAULT_REGISTRY = new MediaContainerRegistry(MediaContainer.asList());

  private final List<MediaContainerProbe> probes;

  public MediaContainerRegistry(List<MediaContainerProbe> probes) {
    this.probes = probes;
  }

  public MediaContainerProbe find(String name) {
    for (MediaContainerProbe probe : probes) {
      if (name.equals(probe.getName())) {
        return probe;
      }
    }

    return null;
  }

  public List<MediaContainerProbe> getAll() {
    return probes;
  }

  public static MediaContainerRegistry extended(MediaContainerProbe... additional) {
    List<MediaContainerProbe> probes = MediaContainer.asList();

    for (MediaContainerProbe probe : additional) {
      probes.add(probe);
    }

    return new MediaContainerRegistry(probes);
  }
}
