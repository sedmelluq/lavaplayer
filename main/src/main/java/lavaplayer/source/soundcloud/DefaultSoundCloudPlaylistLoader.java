package lavaplayer.source.soundcloud;

import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.HttpInterfaceManager;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.AudioTrackCollection;
import lavaplayer.track.AudioTrackCollectionType;
import lavaplayer.track.BasicAudioTrackCollection;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultSoundCloudPlaylistLoader implements SoundCloudPlaylistLoader {
    protected static final String PLAYLIST_URL_REGEX = "^(?:http://|https://|)(?:www\\.|)(?:m\\.|)soundcloud\\.com/([a-zA-Z0-9-_]+)/sets/([a-zA-Z0-9-_]+)(?:\\?.*|)$";
    protected static final Pattern playlistUrlPattern = Pattern.compile(PLAYLIST_URL_REGEX);
    private static final Logger log = LoggerFactory.getLogger(DefaultSoundCloudPlaylistLoader.class);
    protected final SoundCloudHtmlDataLoader htmlDataLoader;
    protected final SoundCloudDataReader dataReader;
    protected final SoundCloudFormatHandler formatHandler;

    public DefaultSoundCloudPlaylistLoader(
        SoundCloudHtmlDataLoader htmlDataLoader,
        SoundCloudDataReader dataReader,
        SoundCloudFormatHandler formatHandler
    ) {
        this.htmlDataLoader = htmlDataLoader;
        this.dataReader = dataReader;
        this.formatHandler = formatHandler;
    }

    @Override
    public AudioTrackCollection load(
        String identifier,
        HttpInterfaceManager httpInterfaceManager,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        String url = SoundCloudHelper.nonMobileUrl(identifier);

        if (playlistUrlPattern.matcher(url).matches()) {
            return loadFromSet(httpInterfaceManager, url, trackFactory);
        } else {
            return null;
        }
    }

    protected AudioTrackCollection loadFromSet(
        HttpInterfaceManager httpInterfaceManager,
        String playlistWebUrl,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        try (HttpInterface httpInterface = httpInterfaceManager.get()) {
            JsonBrowser rootData = htmlDataLoader.load(httpInterface, playlistWebUrl);
            JsonBrowser playlistData = dataReader.findPlaylistData(rootData);

            return new BasicAudioTrackCollection(
                dataReader.readPlaylistName(playlistData),
                AudioTrackCollectionType.Playlist.INSTANCE,
                loadPlaylistTracks(httpInterface, playlistData, trackFactory),
                null
            );
        } catch (IOException e) {
            throw new FriendlyException("Loading playlist from SoundCloud failed.", SUSPICIOUS, e);
        }
    }

    protected List<AudioTrack> loadPlaylistTracks(
        HttpInterface httpInterface,
        JsonBrowser playlistData,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) throws IOException {
        String playlistId = dataReader.readPlaylistIdentifier(playlistData);

        List<String> trackIds = dataReader.readPlaylistTracks(playlistData).stream()
            .map(dataReader::readTrackId)
            .collect(Collectors.toList());

        int numTrackIds = trackIds.size();
        List<JsonBrowser> trackDataList = new ArrayList<>();

        for (int i = 0; i < numTrackIds; i += 50) {
            int last = Math.min(i + 50, numTrackIds);
            List<String> trackIdSegment = trackIds.subList(i, last);

            try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(buildTrackListUrl(trackIdSegment)))) {
                HttpClientTools.assertSuccessWithContent(response, "track list response");

                JsonBrowser trackList = JsonBrowser.parse(response.getEntity().getContent());
                trackDataList.addAll(trackList.values());
            }
        }

        sortPlaylistTracks(trackDataList, trackIds);

        int blockedCount = 0;
        List<AudioTrack> tracks = new ArrayList<>();

        for (JsonBrowser trackData : trackDataList) {
            if (dataReader.isTrackBlocked(trackData)) {
                blockedCount++;
            } else {
                try {
                    tracks.add(trackFactory.apply(dataReader.readTrackInfo(
                        trackData,
                        formatHandler.buildFormatIdentifier(
                            formatHandler.chooseBestFormat(dataReader.readTrackFormats(trackData))
                        )
                    )));
                } catch (Exception e) {
                    log.error("In soundcloud playlist {}, failed to load track", playlistId, e);
                }
            }
        }

        if (blockedCount > 0) {
            log.debug("In soundcloud playlist {}, {} tracks were omitted because they are blocked.",
                playlistId, blockedCount);
        }

        return tracks;
    }

    protected URI buildTrackListUrl(List<String> trackIds) {
        try {
            StringJoiner joiner = new StringJoiner(",");
            for (String trackId : trackIds) {
                joiner.add(trackId);
            }

            return new URIBuilder("https://api-v2.soundcloud.com/tracks")
                .addParameter("ids", joiner.toString())
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sortPlaylistTracks(List<JsonBrowser> trackDataList, List<String> trackIds) {
        Map<String, Integer> positions = new HashMap<>();

        for (int i = 0; i < trackIds.size(); i++) {
            positions.put(trackIds.get(i), i);
        }

        trackDataList.sort(Comparator.comparingInt(trackData ->
            positions.getOrDefault(dataReader.readTrackId(trackData), Integer.MAX_VALUE)
        ));
    }
}