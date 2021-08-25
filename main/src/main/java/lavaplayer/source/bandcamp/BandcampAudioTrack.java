package lavaplayer.source.bandcamp;

import lavaplayer.container.mp3.Mp3AudioTrack;
import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.PersistentHttpStream;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Audio track that handles processing Bandcamp tracks.
 */
public class BandcampAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(BandcampAudioTrack.class);

    private final BandcampItemSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public BandcampAudioTrack(AudioTrackInfo trackInfo, BandcampItemSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            log.debug("Loading Bandcamp track page from URL: {}", trackInfo.identifier);

            String trackMediaUrl = getTrackMediaUrl(httpInterface);
            log.debug("Starting Bandcamp track from URL: {}", trackMediaUrl);

            try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(trackMediaUrl), null)) {
                processDelegate(new Mp3AudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }

    private String getTrackMediaUrl(HttpInterface httpInterface) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackInfo.identifier))) {
            HttpClientTools.assertSuccessWithContent(response, "track page");

            String responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            JsonBrowser trackInfo = sourceManager.readTrackListInformation(responseText);

            return trackInfo.get("trackinfo").index(0).get("file").get("mp3-128").text();
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new BandcampAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public ItemSourceManager getSourceManager() {
        return sourceManager;
    }
}
