package lavaplayer.source.twitch;

import lavaplayer.source.ItemSourceManager;
import lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static lavaplayer.source.twitch.TwitchStreamItemSourceManager.getChannelIdentifierFromUrl;

/**
 * Audio track that handles processing Twitch tracks.
 */
public class TwitchStreamAudioTrack extends MpegTsM3uStreamAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(TwitchStreamAudioTrack.class);

    private final TwitchStreamItemSourceManager sourceManager;
    private final M3uStreamSegmentUrlProvider segmentUrlProvider;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public TwitchStreamAudioTrack(AudioTrackInfo trackInfo, TwitchStreamItemSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
        this.segmentUrlProvider = new TwitchStreamSegmentUrlProvider(getChannelName(), sourceManager);
    }

    /**
     * @return Name of the channel of the stream.
     */
    public String getChannelName() {
        return getChannelIdentifierFromUrl(trackInfo.identifier);
    }

    @Override
    protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
        return segmentUrlProvider;
    }

    @Override
    protected HttpInterface getHttpInterface() {
        return sourceManager.getHttpInterface();
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        log.debug("Starting to play Twitch channel {}.", getChannelName());

        super.process(localExecutor);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new TwitchStreamAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public ItemSourceManager getSourceManager() {
        return sourceManager;
    }
}