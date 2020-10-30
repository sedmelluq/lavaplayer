package com.sedmelluq.lavaplayer.core.source.youtube.format;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeSignatureResolver;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeTrackJsonData;
import java.util.List;

public interface OfflineYoutubeTrackFormatExtractor extends YoutubeTrackFormatExtractor {
  List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data);

  @Override
  default List<YoutubeTrackFormat> extract(
      YoutubeTrackJsonData data,
      HttpInterface httpInterface,
      YoutubeSignatureResolver signatureResolver
  ) {
    return extract(data);
  }
}
