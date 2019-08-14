package com.sedmelluq.discord.lavaplayer.container.common;

import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.natives.aac.AacDecoder;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

public class AacPacketRouter {
  private final AudioProcessingContext context;
  private final Consumer<AacDecoder> decoderConfigurer;

  private Long initialRequestedTimecode;
  private Long initialProvidedTimecode;
  private ShortBuffer outputBuffer;
  private AudioPipeline downstream;
  private AacDecoder decoder;

  public AacPacketRouter(AudioProcessingContext context, Consumer<AacDecoder> decoderConfigurer) {
    this.context = context;
    this.decoderConfigurer = decoderConfigurer;
  }

  public void processInput(ByteBuffer inputBuffer) throws InterruptedException {
    if (decoder == null) {
      decoder = new AacDecoder();
      decoderConfigurer.accept(decoder);
    }

    decoder.fill(inputBuffer);

    if (downstream == null) {
      AacDecoder.StreamInfo streamInfo = decoder.resolveStreamInfo();

      if (streamInfo != null) {
        downstream = AudioPipelineFactory.create(context, new PcmFormat(streamInfo.channels, streamInfo.sampleRate));
        outputBuffer = ByteBuffer.allocateDirect(2 * streamInfo.frameSize * streamInfo.channels)
            .order(ByteOrder.nativeOrder()).asShortBuffer();

        if (initialRequestedTimecode != null) {
          downstream.seekPerformed(initialRequestedTimecode, initialProvidedTimecode);
        }
      }
    }

    if (downstream != null) {
      while (decoder.decode(outputBuffer, false)) {
        downstream.process(outputBuffer);
        outputBuffer.clear();
      }
    }
  }

  public void seekPerformed(long requestedTimecode, long providedTimecode) {
    if (downstream != null) {
      downstream.seekPerformed(requestedTimecode, providedTimecode);
    } else {
      this.initialRequestedTimecode = requestedTimecode;
      this.initialProvidedTimecode = providedTimecode;
    }

    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
  }

  public void flush() throws InterruptedException {
    if (downstream != null) {
      while (decoder.decode(outputBuffer, true)) {
        downstream.process(outputBuffer);
        outputBuffer.clear();
      }
    }
  }

  public void close() {
    try {
      if (downstream != null) {
        downstream.close();
      }
    } finally {
      if (decoder != null) {
        decoder.close();
      }
    }
  }
}
