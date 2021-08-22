package lavaplayer.source.youtube;

import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.ThumbnailTools;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.AudioTrackCollection;
import lavaplayer.track.AudioTrackCollectionType;
import lavaplayer.track.BasicAudioTrackCollection;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static lavaplayer.source.youtube.YoutubeHttpContextFilter.PBJ_PARAMETER;
import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Handles loading of YouTube mixes.
 */
public class YoutubeMixProvider implements YoutubeMixLoader {
    public static final String YOUTUBE_MUSIC_MIX = "https://music.youtube.com/playlist?list=";
    public static final String YOUTUBE_MUSIC_VIDEO = "https://music.youtube.com/watch?v=";

    /**
     * Loads tracks from mix in parallel into a playlist entry.
     *
     * @param mixId           ID of the mix
     * @param selectedVideoId Selected track, {@link AudioTrackCollection#getSelectedTrack()} will return this.
     * @return Playlist of the tracks in the mix.
     */
    public AudioTrackCollection load(
        HttpInterface httpInterface,
        String mixId,
        String selectedVideoId,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        String playlistTitle = "YouTube mix";
        List<AudioTrack> tracks = new ArrayList<>();

        String mixUrl = "https://www.youtube.com/watch?v=" + selectedVideoId + "&list=" + mixId + PBJ_PARAMETER;
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(mixUrl))) {
            HttpClientTools.assertSuccessWithContent(response, "mix response");

            JsonBrowser body = JsonBrowser.parse(response.getEntity().getContent());
            JsonBrowser playlist = body.index(3).get("response")
                .get("contents")
                .get("twoColumnWatchNextResults")
                .get("playlist")
                .get("playlist");

            JsonBrowser title = playlist.get("title");

            if (!title.isNull()) {
                playlistTitle = title.text();
            }

            extractPlaylistTracks(playlist.get("contents"), tracks, trackFactory);
        } catch (IOException e) {
            throw new FriendlyException("Could not read mix page.", SUSPICIOUS, e);
        }

        if (tracks.isEmpty()) {
            throw new FriendlyException("Could not find tracks from mix.", SUSPICIOUS, null);
        }

        AudioTrack selectedTrack = findSelectedTrack(tracks, selectedVideoId);
        return new BasicAudioTrackCollection(
            playlistTitle,
            AudioTrackCollectionType.Playlist.INSTANCE,
            tracks,
            selectedTrack
        );
    }

    private void extractPlaylistTracks(
        JsonBrowser browser,
        List<AudioTrack> tracks,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    ) {
        for (JsonBrowser video : browser.values()) {
            JsonBrowser renderer = video.get("playlistPanelVideoRenderer");
            String title = renderer.get("title").get("simpleText").text();
            String author = renderer.get("longBylineText").get("runs").index(0).get("text").text();
            String identifier = renderer.get("videoId").text();
            String durationStr = renderer.get("lengthText").get("simpleText").text();
            long duration = DataFormatTools.parseDuration(durationStr);

            AudioTrackInfo trackInfo = new AudioTrackInfo(
                title,
                author,
                duration,
                identifier,
                false,
                "https://youtube.com/watch?v=" + identifier,
                ThumbnailTools.extractYouTube(renderer, identifier)
            );

            tracks.add(trackFactory.apply(trackInfo));
        }
    }

    private AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
        if (selectedVideoId != null) {
            for (AudioTrack track : tracks) {
                if (selectedVideoId.equals(track.getIdentifier())) {
                    return track;
                }
            }
        }

        return null;
    }
}
