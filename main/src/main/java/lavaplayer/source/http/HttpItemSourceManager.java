package lavaplayer.source.http;

import lavaplayer.container.*;
import lavaplayer.source.ProbingItemSourceManager;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.Units;
import lavaplayer.tools.io.*;
import lavaplayer.track.AudioItem;
import lavaplayer.track.AudioReference;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.info.AudioTrackInfoBuilder;
import lavaplayer.track.loader.LoaderState;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Function;

import static lavaplayer.container.MediaContainerDetectionResult.refer;
import static lavaplayer.tools.FriendlyException.Severity.COMMON;
import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static lavaplayer.tools.io.HttpClientTools.getHeaderValue;

/**
 * Audio source manager which implements finding audio files from HTTP addresses.
 */
public class HttpItemSourceManager extends ProbingItemSourceManager implements HttpConfigurable {
    private final HttpInterfaceManager httpInterfaceManager;

    /**
     * Create a new instance with default media container registry.
     */
    public HttpItemSourceManager() {
        this(MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    /**
     * Create a new instance.
     */
    public HttpItemSourceManager(MediaContainerRegistry containerRegistry) {
        super(containerRegistry);

        httpInterfaceManager = new ThreadLocalHttpInterfaceManager(
            HttpClientTools
                .createSharedCookiesHttpBuilder()
                .setRedirectStrategy(new HttpClientTools.NoRedirectsStrategy()),
            HttpClientTools.DEFAULT_REQUEST_CONFIG
        );
    }

    public static AudioReference getAsHttpReference(AudioReference reference) {
        if (reference.getUri().startsWith("https://") || reference.identifier.startsWith("http://")) {
            return reference;
        } else if (reference.getUri().startsWith("icy://")) {
            return new AudioReference("http://" + reference.identifier.substring(6), reference.title);
        }
        return null;
    }

    @Override
    public String getSourceName() {
        return "http";
    }

    @Override
    public AudioItem loadItem(LoaderState state, AudioReference reference) {
        AudioReference httpReference = getAsHttpReference(reference);
        if (httpReference == null) {
            return null;
        }

        if (httpReference.containerDescriptor != null) {
            return createTrack(AudioTrackInfoBuilder.create(reference, null).build(), httpReference.containerDescriptor);
        } else {
            return handleLoadResult(detectContainer(httpReference));
        }
    }

    @Override
    protected AudioTrack createTrack(AudioTrackInfo trackInfo, MediaContainerDescriptor containerDescriptor) {
        return new HttpAudioTrack(trackInfo, containerDescriptor, this);
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

    private MediaContainerDetectionResult detectContainer(AudioReference reference) {
        MediaContainerDetectionResult result;

        try (HttpInterface httpInterface = getHttpInterface()) {
            result = detectContainerWithClient(httpInterface, reference);
        } catch (IOException e) {
            throw new FriendlyException("Connecting to the URL failed.", SUSPICIOUS, e);
        }

        return result;
    }

    private MediaContainerDetectionResult detectContainerWithClient(HttpInterface httpInterface, AudioReference reference) throws IOException {
        try (PersistentHttpStream inputStream = new PersistentHttpStream(httpInterface, new URI(reference.identifier), Units.CONTENT_LENGTH_UNKNOWN)) {
            int statusCode = inputStream.checkStatusCode();
            String redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.getCurrentResponse());

            if (redirectUrl != null) {
                return refer(null, new AudioReference(redirectUrl, null));
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return null;
            } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new FriendlyException("That URL is not playable.", COMMON, new IllegalStateException("Status code " + statusCode));
            }

            MediaContainerHints hints = MediaContainerHints.from(getHeaderValue(inputStream.getCurrentResponse(), "Content-Type"), null);
            return new MediaContainerDetection(containerRegistry, reference, inputStream, hints).detectContainer();
        } catch (URISyntaxException e) {
            throw new FriendlyException("Not a valid URL.", COMMON, e);
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        encodeTrackFactory(((HttpAudioTrack) track).getContainerTrackFactory(), output);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        MediaContainerDescriptor containerTrackFactory = decodeTrackFactory(input);

        if (containerTrackFactory != null) {
            return new HttpAudioTrack(trackInfo, containerTrackFactory, this);
        }

        return null;
    }

    @Override
    public void shutdown() {
        // Nothing to shut down
    }
}
