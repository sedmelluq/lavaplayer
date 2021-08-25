package lavaplayer.source.vimeo;

import lavaplayer.container.mpeg.MpegAudioTrack;
import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.FriendlyException;
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

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track that handles processing Vimeo tracks.
 */
public class VimeoAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(VimeoAudioTrack.class);

    private final VimeoItemSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public VimeoAudioTrack(AudioTrackInfo trackInfo, VimeoItemSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            String playbackUrl = loadPlaybackUrl(httpInterface);

            log.debug("Starting Vimeo track from URL: {}", playbackUrl);

            try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(playbackUrl), null)) {
                processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }

    private String loadPlaybackUrl(HttpInterface httpInterface) throws IOException {
        JsonBrowser config = loadPlayerConfig(httpInterface);
        if (config == null) {
            throw new FriendlyException("Track information not present on the page.", SUSPICIOUS, null);
        }

        String trackConfigUrl = config.get("player").get("config_url").text();
        JsonBrowser trackConfig = loadTrackConfig(httpInterface, trackConfigUrl);

        return trackConfig.get("request").get("files").get("progressive").index(0).get("url").text();
    }

    private JsonBrowser loadPlayerConfig(HttpInterface httpInterface) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackInfo.identifier))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code for player config is " + statusCode));
            }

            return sourceManager.loadConfigJsonFromPageContent(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
        }
    }

    private JsonBrowser loadTrackConfig(HttpInterface httpInterface, String trackAccessInfoUrl) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(trackAccessInfoUrl))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
                    new IllegalStateException("Response code for track access info is " + statusCode));
            }

            return JsonBrowser.parse(response.getEntity().getContent());
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new VimeoAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public ItemSourceManager getSourceManager() {
        return sourceManager;
    }
}
