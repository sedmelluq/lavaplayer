package lavaplayer.source.getyarn;

import lavaplayer.source.ItemSourceManager;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.io.*;
import lavaplayer.track.AudioItem;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.info.AudioTrackInfoBuilder;
import lavaplayer.track.loader.LoaderState;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects getyarn.io tracks by URL.
 */
public class GetyarnItemSourceManager implements HttpConfigurable, ItemSourceManager {
    private static final Pattern GETYARN_REGEX = Pattern.compile("(?:http://|https://(?:www\\.)?)?getyarn\\.io/yarn-clip/(.*)");

    private final HttpInterfaceManager httpInterfaceManager;

    public GetyarnItemSourceManager() {
        httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
            HttpClientTools
                .createSharedCookiesHttpBuilder()
                .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
            HttpClientTools.DEFAULT_REQUEST_CONFIG
        );
    }

    @Override
    public String getSourceName() {
        return "getyarn.io";
    }

    @Nullable
    @Override
    public AudioItem loadItem(@NotNull LoaderState state, AudioReference reference) {
        final Matcher m = GETYARN_REGEX.matcher(reference.identifier);

        if (!m.matches()) {
            return null;
        }

        return extractVideoUrlFromPage(reference);
    }

    private AudioTrack createTrack(AudioTrackInfo trackInfo) {
        return new GetyarnAudioTrack(trackInfo, this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        // No custom values that need saving
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new GetyarnAudioTrack(trackInfo, this);
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }

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

    private AudioTrack extractVideoUrlFromPage(AudioReference reference) {
        try (final CloseableHttpResponse response = getHttpInterface().execute(new HttpGet(reference.identifier))) {
            final String html = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            final Document document = Jsoup.parse(html);

            final AudioTrackInfo trackInfo = AudioTrackInfoBuilder.invoke(builder -> {
                builder.setUri(reference.getUri());
                builder.setAuthor("Unknown");
                builder.setIdentifier(document.selectFirst("meta[property=og:video:secure_url]").attr("content"));
                builder.setTitle(document.selectFirst("meta[property=og:title]").attr("content"));
                return null;
            });

            return createTrack(trackInfo);
        } catch (IOException e) {
            throw new FriendlyException("Failed to load info for yarn clip", SUSPICIOUS, null);
        }
    }
}
