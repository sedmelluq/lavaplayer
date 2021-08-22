package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;
import java.util.function.Supplier;

public interface SoundCloudSegmentDecoder extends AutoCloseable {
    void prepareStream(boolean beginning) throws IOException;

    void resetStream() throws IOException;

    void playStream(
        AudioProcessingContext context,
        long startPosition,
        long desiredPosition
    ) throws InterruptedException, IOException;

    interface Factory {
        SoundCloudSegmentDecoder create(Supplier<SeekableInputStream> nextStreamProvider);
    }
}
