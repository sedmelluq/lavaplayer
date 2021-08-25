package lavaplayer.source.nico;

import lavaplayer.container.mpeg.MpegAudioTrack;
import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.PersistentHttpStream;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.DelegatedAudioTrack;
import lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static lavaplayer.tools.DataFormatTools.convertToMapLayout;

/**
 * Audio track that handles processing NicoNico tracks.
 */
public class NicoAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(NicoAudioTrack.class);

    private final NicoItemSourceManager sourceManager;

    /**
     * @param trackInfo     Track info
     * @param sourceManager Source manager which was used to find this track
     */
    public NicoAudioTrack(AudioTrackInfo trackInfo, NicoItemSourceManager sourceManager) {
        super(trackInfo);

        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
        sourceManager.checkLoggedIn();

        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            loadVideoMainPage(httpInterface);
            String playbackUrl = loadPlaybackUrl(httpInterface);

            log.debug("Starting NicoNico track from URL: {}", playbackUrl);

            try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(playbackUrl), null)) {
                processDelegate(new MpegAudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }

    private void loadVideoMainPage(HttpInterface httpInterface) throws IOException {
        HttpGet request = new HttpGet("http://www.nicovideo.jp/watch/" + trackInfo.identifier);

        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new IOException("Unexpected status code from video main page: " + statusCode);
            }

            EntityUtils.consume(response.getEntity());
        }
    }

    private String loadPlaybackUrl(HttpInterface httpInterface) throws IOException {
        HttpGet request = new HttpGet("http://flapi.nicovideo.jp/api/getflv/" + trackInfo.identifier);

        try (CloseableHttpResponse response = httpInterface.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new IOException("Unexpected status code from playback parameters page: " + statusCode);
            }

            String text = EntityUtils.toString(response.getEntity());
            Map<String, String> format = convertToMapLayout(URLEncodedUtils.parse(text, StandardCharsets.UTF_8));

            return format.get("url");
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new NicoAudioTrack(trackInfo, sourceManager);
    }

    @Override
    public ItemSourceManager getSourceManager() {
        return sourceManager;
    }
}
