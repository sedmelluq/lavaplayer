package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreSourceName;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;

public class YoutubeTrackInfoFactory {
  private static final AudioTrackProperty sourceProperty = coreSourceName("youtube");

  public static YoutubeAudioTrackInfo create(
      AudioTrackInfoTemplate template,
      String videoId,
      String uploader,
      String title,
      long duration,
      boolean isStream
  ) {
    return new YoutubeAudioTrackInfo(AudioTrackInfoBuilder.fromTemplate(template)
        .with(sourceProperty)
        .with(coreIdentifier(videoId))
        .with(coreIsStream(isStream))
        .with(coreTitle(title))
        .with(coreAuthor(uploader))
        .with(coreLength(duration))
        .build());
  }
}
