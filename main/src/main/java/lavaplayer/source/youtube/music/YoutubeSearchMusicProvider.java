package lavaplayer.source.youtube.music;

import lavaplayer.source.youtube.YoutubeConstants;
import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.ExceptionTools;
import lavaplayer.tools.ThumbnailTools;
import lavaplayer.tools.http.ExtendedHttpConfigurable;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.HttpInterfaceManager;
import lavaplayer.tools.json.JsonBrowser;
import lavaplayer.track.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handles processing YouTube Music searches.
 */
public class YoutubeSearchMusicProvider implements YoutubeSearchMusicResultLoader {
    private static final Logger log = LoggerFactory.getLogger(YoutubeSearchMusicProvider.class);

    private final HttpInterfaceManager httpInterfaceManager;

    public YoutubeSearchMusicProvider() {
        this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
    }

    public ExtendedHttpConfigurable getHttpConfiguration() {
        return httpInterfaceManager;
    }

    /**
     * @param query        Search query.
     * @param trackFactory
     * @return Playlist of the first page of music results.
     */
    @NotNull
    @Override
    public AudioItem loadSearchMusicResult(@NotNull String query, @NotNull AudioTrackFactory trackFactory) {
        log.debug("Performing a search music with query {}", query);

        try (HttpInterface httpInterface = httpInterfaceManager.get()) {
            HttpPost post = new HttpPost(YoutubeConstants.MUSIC_SEARCH_URL);
            StringEntity payload = new StringEntity(String.format(YoutubeConstants.MUSIC_SEARCH_PAYLOAD, query.replace("\"", "\\\"")), "UTF-8");
            post.setHeader("Referer", "music.youtube.com");
            post.setEntity(payload);

            try (CloseableHttpResponse response = httpInterface.execute(post)) {
                HttpClientTools.assertSuccessWithContent(response, "search music response");

                String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

                JsonBrowser jsonBrowser = JsonBrowser.parse(responseText);
                return extractSearchResults(jsonBrowser, query, trackFactory);
            }
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyException(e);
        }
    }

    private AudioItem extractSearchResults(JsonBrowser jsonBrowser, String query,
                                           AudioTrackFactory trackFactory) {
        List<AudioTrack> tracks;
        log.debug("Attempting to parse results from music search page");
        try {
            tracks = extractMusicSearchPage(jsonBrowser, trackFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        } else {
            return new BasicAudioTrackCollection("Search results for: " + query, new AudioTrackCollectionType.SearchResult(query), tracks, null);
        }
    }

    private List<AudioTrack> extractMusicSearchPage(JsonBrowser jsonBrowser, AudioTrackFactory trackFactory) throws IOException {
        ArrayList<AudioTrack> list = new ArrayList<>();
        JsonBrowser tracks = jsonBrowser.get("contents")
            .get("tabbedSearchResultsRenderer")
            .get("tabs")
            .index(0)
            .get("tabRenderer")
            .get("content")
            .get("sectionListRenderer")
            .get("contents")
            .index(0)
            .get("musicShelfRenderer")
            .get("contents");

        if (tracks == JsonBrowser.NULL_BROWSER) {
            tracks = jsonBrowser.get("contents")
                .get("tabbedSearchResultsRenderer")
                .get("tabs")
                .index(0)
                .get("tabRenderer")
                .get("content")
                .get("sectionListRenderer")
                .get("contents")
                .index(1)
                .get("musicShelfRenderer")
                .get("contents");
        }

        tracks.values().forEach(jsonTrack -> {
            AudioTrack track = extractMusicTrack(jsonTrack, trackFactory);
            if (track != null) {
                list.add(track);
            }
        });

        return list;
    }

    private AudioTrack extractMusicTrack(JsonBrowser jsonBrowser, AudioTrackFactory trackFactory) {
        JsonBrowser columns = jsonBrowser.get("musicResponsiveListItemRenderer").get("flexColumns");
        if (columns.isNull()) {
            // Somehow don't get track info, ignore
            return null;
        }

        JsonBrowser firstColumn = columns.index(0).get("musicResponsiveListItemFlexColumnRenderer").get("text").get("runs").index(0);
        String title = firstColumn.get("text").text();
        String videoId = firstColumn.get("navigationEndpoint").get("watchEndpoint").get("videoId").text();

        if (videoId == null) {
            // If track is not available on YouTube Music videoId will be empty
            return null;
        }

        List<JsonBrowser> secondColumn = columns.index(1).get("musicResponsiveListItemFlexColumnRenderer").get("text").get("runs").values();
        String author = secondColumn.get(0).get("text").safeText();
        JsonBrowser lastElement = secondColumn.get(secondColumn.size() - 1);

        if (!lastElement.get("navigationEndpoint").isNull()) {
            // The duration element should not have this key, if it does, then duration is probably missing, so return
            return null;
        }

        long duration = DataFormatTools.durationTextToMillis(lastElement.get("text").text());
        AudioTrackInfo info = new AudioTrackInfo(
            title,
            author,
            duration,
            videoId,
            YoutubeConstants.WATCH_URL_PREFIX + videoId,
            ThumbnailTools.extractYouTubeMusic(jsonBrowser, videoId),
            false
        );

        return trackFactory.create(info);
    }
}
