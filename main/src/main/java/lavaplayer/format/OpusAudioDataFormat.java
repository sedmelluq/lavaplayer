package lavaplayer.format;

import lavaplayer.format.transcoder.AudioChunkDecoder;
import lavaplayer.format.transcoder.AudioChunkEncoder;
import lavaplayer.format.transcoder.OpusChunkDecoder;
import lavaplayer.format.transcoder.OpusChunkEncoder;
import lavaplayer.manager.AudioConfiguration;

/**
 * An {@link AudioDataFormat} for OPUS.
 */
public class OpusAudioDataFormat extends AudioDataFormat {
    public static final String CODEC_NAME = "OPUS";

    private static final byte[] SILENT_OPUS_FRAME = new byte[]{(byte) 0xFC, (byte) 0xFF, (byte) 0xFE};

    private final int maximumChunkSize;
    private final int expectedChunkSize;

    /**
     * @param channelCount     Number of channels.
     * @param sampleRate       Sample rate (frequency).
     * @param chunkSampleCount Number of samples in one chunk.
     */
    public OpusAudioDataFormat(int channelCount, int sampleRate, int chunkSampleCount) {
        super(channelCount, sampleRate, chunkSampleCount);

        this.maximumChunkSize = 32 + 1536 * chunkSampleCount / 960;
        this.expectedChunkSize = 32 + 512 * chunkSampleCount / 960;
    }

    @Override
    public String codecName() {
        return CODEC_NAME;
    }

    @Override
    public byte[] silenceBytes() {
        return SILENT_OPUS_FRAME;
    }

    @Override
    public int expectedChunkSize() {
        return expectedChunkSize;
    }

    @Override
    public int maximumChunkSize() {
        return maximumChunkSize;
    }

    @Override
    public AudioChunkDecoder createDecoder() {
        return new OpusChunkDecoder(this);
    }

    @Override
    public AudioChunkEncoder createEncoder(AudioConfiguration configuration) {
        return new OpusChunkEncoder(configuration, this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
