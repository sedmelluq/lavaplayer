package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import java.util.concurrent.Future;

public interface AudioInfoRequestHandler extends AutoCloseable {
  Future<Void> request(AudioInfoRequest request);
}
