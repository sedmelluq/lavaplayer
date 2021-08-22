package lavaplayer.container.ogg.flac;

import lavaplayer.container.flac.FlacTrackInfo;
import lavaplayer.container.flac.frame.FlacFrameReader;
import lavaplayer.container.ogg.OggPacketInputStream;
import lavaplayer.container.ogg.OggTrackHandler;
import lavaplayer.filter.AudioPipeline;
import lavaplayer.filter.AudioPipelineFactory;
import lavaplayer.filter.PcmFormat;
import lavaplayer.tools.io.BitStreamReader;
import lavaplayer.track.playback.AudioProcessingContext;

import java.io.IOException;

/**
 * OGG stream handler for FLAC codec.
 */
public class OggFlacTrackHandler implements OggTrackHandler {
    private final FlacTrackInfo info;
    private final OggPacketInputStream packetInputStream;
    private final BitStreamReader bitStreamReader;
    private final int[] decodingBuffer;
    private final int[][] rawSampleBuffers;
    private final short[][] sampleBuffers;
    private AudioPipeline downstream;

    /**
     * @param info              FLAC track info
     * @param packetInputStream OGG packet input stream
     */
    public OggFlacTrackHandler(FlacTrackInfo info, OggPacketInputStream packetInputStream) {
        this.info = info;
        this.packetInputStream = packetInputStream;
        this.bitStreamReader = new BitStreamReader(packetInputStream);
        this.decodingBuffer = new int[FlacFrameReader.TEMPORARY_BUFFER_SIZE];
        this.rawSampleBuffers = new int[info.stream.channelCount][];
        this.sampleBuffers = new short[info.stream.channelCount][];

        for (int i = 0; i < rawSampleBuffers.length; i++) {
            rawSampleBuffers[i] = new int[info.stream.maximumBlockSize];
            sampleBuffers[i] = new short[info.stream.maximumBlockSize];
        }
    }

    @Override
    public void initialise(AudioProcessingContext context, long timecode, long desiredTimecode) {
        downstream = AudioPipelineFactory.create(context,
            new PcmFormat(info.stream.channelCount, info.stream.sampleRate));
        downstream.seekPerformed(desiredTimecode, timecode);
    }

    @Override
    public void provideFrames() throws InterruptedException {
        try {
            while (packetInputStream.startNewPacket()) {
                int sampleCount = readFlacFrame();

                if (sampleCount == 0) {
                    throw new IllegalStateException("Not enough bytes in packet.");
                }

                downstream.process(sampleBuffers, 0, sampleCount);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int readFlacFrame() throws IOException {
        return FlacFrameReader.readFlacFrame(packetInputStream, bitStreamReader, info.stream, rawSampleBuffers, sampleBuffers, decodingBuffer);
    }

    @Override
    public void seekToTimecode(long timecode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        if (downstream != null) {
            downstream.close();
        }
    }
}
