package lavaplayer.container.matroska;

import lavaplayer.container.common.AacPacketRouter;
import lavaplayer.container.matroska.format.MatroskaFileTrack;
import lavaplayer.container.mpeg.MpegAacTrackConsumer;
import lavaplayer.natives.aac.AacDecoder;
import lavaplayer.track.playback.AudioProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Consumes AAC track data from a matroska file.
 */
public class MatroskaAacTrackConsumer implements MatroskaTrackConsumer {
    private static final Logger log = LoggerFactory.getLogger(MpegAacTrackConsumer.class);

    private final MatroskaFileTrack track;
    private final ByteBuffer inputBuffer;
    private final AacPacketRouter packetRouter;

    /**
     * @param context Configuration and output information for processing
     * @param track   The MP4 audio track descriptor
     */
    public MatroskaAacTrackConsumer(AudioProcessingContext context, MatroskaFileTrack track) {
        this.track = track;
        this.inputBuffer = ByteBuffer.allocateDirect(4096);
        this.packetRouter = new AacPacketRouter(context, this::configureDecoder);
    }

    @Override
    public void initialise() {
        log.debug("Initialising AAC track with expected frequency {} and channel count {}.",
            track.audio.samplingFrequency, track.audio.channels);
    }

    @Override
    public MatroskaFileTrack getTrack() {
        return track;
    }

    @Override
    public void seekPerformed(long requestedTimecode, long providedTimecode) {
        packetRouter.seekPerformed(requestedTimecode, providedTimecode);
    }

    @Override
    public void flush() throws InterruptedException {
        packetRouter.flush();
    }

    @Override
    public void consume(ByteBuffer data) throws InterruptedException {
        while (data.hasRemaining()) {
            int chunk = Math.min(data.remaining(), inputBuffer.capacity());
            ByteBuffer chunkBuffer = data.duplicate();
            chunkBuffer.limit(chunkBuffer.position() + chunk);

            inputBuffer.clear();
            inputBuffer.put(chunkBuffer);
            inputBuffer.flip();

            packetRouter.processInput(inputBuffer);

            data.position(chunkBuffer.position());
        }
    }

    @Override
    public void close() {
        packetRouter.close();
    }

    private void configureDecoder(AacDecoder decoder) {
        decoder.configure(track.codecPrivate);
    }
}
