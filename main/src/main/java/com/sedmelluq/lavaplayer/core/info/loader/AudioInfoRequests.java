package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.property.AudioTrackProperty;
import com.sedmelluq.lavaplayer.core.info.request.generic.DefaultGenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.request.generic.GenericAudioInfoRequestBuilder;
import java.util.Collections;

public class AudioInfoRequests {
  public static GenericAudioInfoRequest generic(String hint, AudioInfoResponseHandler consumer) {
    return orderedGeneric(hint, consumer, null);
  }

  public static GenericAudioInfoRequest orderedGeneric(String hint, AudioInfoResponseHandler consumer, Object orderKey) {
    return new DefaultGenericAudioInfoRequest(consumer, null, orderKey, null,
        AudioTrackProperty.Flag.fullMask(), Collections.emptyList(), hint);
  }

  public static GenericAudioInfoRequestBuilder genericBuilder(String hint) {
    return new GenericAudioInfoRequestBuilder()
        .withHint(hint);
  }
}
