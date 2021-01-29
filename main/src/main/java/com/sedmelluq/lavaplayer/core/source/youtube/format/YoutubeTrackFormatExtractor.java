package com.sedmelluq.lavaplayer.core.source.youtube.format;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.source.youtube.signature.YoutubeSignatureResolver;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.lavaplayer.core.source.youtube.YoutubeTrackJsonData;
import java.util.List;

public interface YoutubeTrackFormatExtractor {
  String DEFAULT_SIGNATURE_KEY = "signature";

  List<YoutubeTrackFormat> extract(
      YoutubeTrackJsonData response,
      HttpInterface httpInterface,
      YoutubeSignatureResolver signatureResolver
  );
}
