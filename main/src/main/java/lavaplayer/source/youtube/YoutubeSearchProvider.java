package lavaplayer.source.youtube;

import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.ExceptionTools;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.ThumbnailTools;
import lavaplayer.tools.http.ExtendedHttpConfigurable;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.HttpInterfaceManager;
import lavaplayer.track.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handles processing YouTube searches.
 */
public class YoutubeSearchProvider implements YoutubeSearchResultLoader {
    private static final Logger log = LoggerFactory.getLogger(YoutubeSearchProvider.class);

    private final HttpInterfaceManager httpInterfaceManager;

    public YoutubeSearchProvider() {
        this.httpInterfaceManager = HttpClientTools.createCookielessThreadLocalManager();
    }

    public ExtendedHttpConfigurable getHttpConfiguration() {
        return httpInterfaceManager;
    }

    /**
     * @param query Search query.
     * @return Playlist of the first page of results.
     */
    @Override
    public AudioItem loadSearchResult(String query, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        log.debug("Performing a search with query {}", query);

        try (HttpInterface httpInterface = httpInterfaceManager.get()) {
            HttpPost post = new HttpPost(YoutubeConstants.SEARCH_URL);

            StringEntity payload = new StringEntity(String.format(YoutubeConstants.SEARCH_PAYLOAD, query.replace("\"", "\\\"")), "UTF-8");
            post.setEntity(payload);
            try (CloseableHttpResponse response = httpInterface.execute(post)) {
                HttpClientTools.assertSuccessWithContent(response, "search response");

                String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

                JsonBrowser jsonBrowser = JsonBrowser.parse(responseText);
                return extractSearchResults(jsonBrowser, query, trackFactory);
            }
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions(e);
        }
    }

    private AudioItem extractSearchResults(JsonBrowser jsonBrowser, String query,
                                           Function<AudioTrackInfo, AudioTrack> trackFactory) {
        List<AudioTrack> tracks;
        log.debug("Attempting to parse results from search page");
        try {
            tracks = extractSearchPage(jsonBrowser, trackFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        } else {
            return new BasicAudioTrackCollection("Search results for: " + query, new AudioTrackCollectionType.SearchResult(query), tracks, null);
        }
    }

    private List<AudioTrack> extractSearchPage(JsonBrowser jsonBrowser, Function<AudioTrackInfo, AudioTrack> trackFactory) throws IOException {
        ArrayList<AudioTrack> list = new ArrayList<>();
        jsonBrowser.get("contents")
            .get("sectionListRenderer")
            .get("contents")
            .index(0)
            .get("itemSectionRenderer")
            .get("contents")
            .values()
            .forEach(jsonTrack -> {
                AudioTrack track = extractPolymerData(jsonTrack, trackFactory);
                if (track != null) {
                    list.add(track);
                }
            });
        return list;
    }

    private AudioTrack extractPolymerData(JsonBrowser json, Function<AudioTrackInfo, AudioTrack> trackFactory) {
        json = json.get("compactVideoRenderer");
        if (json.isNull()) {
            return null; // Ignore everything which is not a track
        }

        String title = json.get("title").get("runs").index(0).get("text").text();
        String author = json.get("longBylineText").get("runs").index(0).get("text").text();
        if (json.get("lengthText").isNull()) {
            return null; // Ignore if the video is a live stream
        }

        long duration = DataFormatTools.durationTextToMillis(json.get("lengthText").get("runs").index(0).get("text").text());
        String videoId = json.get("videoId").text();

        AudioTrackInfo info = new AudioTrackInfo(
            title,
            author,
            duration,
            videoId,
            false,
            YoutubeConstants.WATCH_URL_PREFIX + videoId,
            ThumbnailTools.extractYouTube(json, videoId)
        );

        return trackFactory.apply(info);
    }
}
