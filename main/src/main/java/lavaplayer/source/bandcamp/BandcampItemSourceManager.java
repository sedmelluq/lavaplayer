package lavaplayer.source.bandcamp;

import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.ExceptionTools;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpConfigurable;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.HttpInterfaceManager;
import lavaplayer.track.*;
import lavaplayer.track.loader.LoaderState;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lavaplayer.tools.FriendlyException.Severity.FAULT;
import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager that implements finding Bandcamp tracks based on URL.
 */
public class BandcampItemSourceManager implements ItemSourceManager, HttpConfigurable {
    private static final String URL_REGEX = "^(https?://(?:[^.]+\\.|)bandcamp\\.com)/(track|album)/([a-zA-Z0-9-_]+)/?(?:\\?.*|)$";
    private static final String ARTWORK_URL_FORMAT = "https://f4.bcbits.com/img/a%s_9.jpg";
    private static final Pattern urlRegex = Pattern.compile(URL_REGEX);

    private final HttpInterfaceManager httpInterfaceManager;

    /**
     * Create an instance.
     */
    public BandcampItemSourceManager() {
        httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }

    @Override
    public String getSourceName() {
        return "bandcamp";
    }

    @Override
    public AudioItem loadItem(LoaderState state, AudioReference reference) {
        UrlInfo urlInfo = parseUrl(reference.identifier);

        if (urlInfo != null) {
            if (urlInfo.isAlbum) {
                return loadAlbum(urlInfo);
            } else {
                return loadTrack(urlInfo);
            }
        }

        return null;
    }

    private UrlInfo parseUrl(String url) {
        Matcher matcher = urlRegex.matcher(url);

        if (matcher.matches()) {
            return new UrlInfo(url, matcher.group(1), "album".equals(matcher.group(2)));
        } else {
            return null;
        }
    }

    private AudioItem loadTrack(UrlInfo urlInfo) {
        return extractFromPage(urlInfo.fullUrl, (httpClient, text) -> {
            JsonBrowser trackListInfo = readTrackListInformation(text);
            String artist = trackListInfo.get("artist").safeText();
            String artworkUrl = extractArtwork(trackListInfo);

            return extractTrack(trackListInfo.get("trackinfo").index(0), urlInfo.baseUrl, artist, artworkUrl);
        });
    }

    private AudioItem loadAlbum(UrlInfo urlInfo) {
        return extractFromPage(urlInfo.fullUrl, (httpClient, text) -> {
            JsonBrowser trackListInfo = readTrackListInformation(text);
            String artist = trackListInfo.get("artist").safeText();
            String artworkUrl = extractArtwork(trackListInfo);

            List<AudioTrack> tracks = new ArrayList<>();
            for (JsonBrowser trackInfo : trackListInfo.get("trackinfo").values()) {
                tracks.add(extractTrack(trackInfo, urlInfo.baseUrl, artist, artworkUrl));
            }

            JsonBrowser albumInfo = readAlbumInformation(text);
            return new BasicAudioTrackCollection(
                albumInfo.get("current").get("title").safeText(),
                new AudioTrackCollectionType.Album(artist),
                tracks,
                null
            );
        });
    }

    private AudioTrack extractTrack(JsonBrowser trackInfo, String bandUrl, String artist, String artworkUrl) {
        String trackPageUrl = bandUrl + trackInfo.get("title_link").text();

        return new BandcampAudioTrack(new AudioTrackInfo(
            trackInfo.get("title").text(),
            artist,
            (long) (trackInfo.get("duration").asDouble() * 1000.0),
            bandUrl + trackInfo.get("title_link").text(),
            false,
            trackPageUrl,
            artworkUrl
        ), this);
    }

    private JsonBrowser readAlbumInformation(String text) throws IOException {
        String albumInfoJson = DataFormatTools.extractBetween(text, "data-tralbum=\"", "\"");

        if (albumInfoJson == null) {
            throw new FriendlyException("Album information not found on the Bandcamp page.", SUSPICIOUS, null);
        }

        albumInfoJson = albumInfoJson.replace("&quot;", "\"");
        return JsonBrowser.parse(albumInfoJson);
    }

    JsonBrowser readTrackListInformation(String text) throws IOException {
        String trackInfoJson = DataFormatTools.extractBetween(text, "data-tralbum=\"", "\"");

        if (trackInfoJson == null) {
            throw new FriendlyException("Track information not found on the Bandcamp page.", SUSPICIOUS, null);
        }

        trackInfoJson = trackInfoJson.replace("&quot;", "\"");
        return JsonBrowser.parse(trackInfoJson);
    }

    private AudioItem extractFromPage(String url, AudioItemExtractor extractor) {
        try (HttpInterface httpInterface = httpInterfaceManager.get()) {
            return extractFromPageWithInterface(httpInterface, url, extractor);
        } catch (Exception e) {
            throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a Bandcamp track failed.", FAULT, e);
        }
    }

    private AudioItem extractFromPageWithInterface(HttpInterface httpInterface, String url, AudioItemExtractor extractor) throws Exception {
        String responseText;

        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return new AudioReference(null, null);
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new IOException("Invalid status code for track page: " + statusCode);
            }

            responseText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        }

        return extractor.extract(httpInterface, responseText);
    }

    private String extractArtwork(JsonBrowser root) {
        String artId = root.get("art_id").text();
        if (artId != null) {
            if (artId.length() < 10) {
                StringBuilder builder = new StringBuilder(artId);
                while (builder.length() < 10) {
                    builder.insert(0, "0");
                }
                artId = builder.toString();
            }
            return String.format(ARTWORK_URL_FORMAT, artId);
        }

        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        // No special values to encode
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        return new BandcampAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        ExceptionTools.closeWithWarnings(httpInterfaceManager);
    }

    /**
     * @return Get an HTTP interface for a playing track.
     */
    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.get();
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    private interface AudioItemExtractor {
        AudioItem extract(HttpInterface httpInterface, String text) throws Exception;
    }

    private static class UrlInfo {
        public final String fullUrl;
        public final String baseUrl;
        public final boolean isAlbum;

        private UrlInfo(String fullUrl, String baseUrl, boolean isAlbum) {
            this.fullUrl = fullUrl;
            this.baseUrl = baseUrl;
            this.isAlbum = isAlbum;
        }
    }
}
