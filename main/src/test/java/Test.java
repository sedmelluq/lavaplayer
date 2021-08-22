import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.concurrent.ExecutionException;

public class Test {

    public static void main(String[] args) {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        String query = "ytmsearch:resume jenevieve";
        try {
            playerManager.loadItem(query, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    System.out.println(track.getClass().getName());
                    System.out.println(track.getInfo());
                    System.out.println(track.getInfo().title);
                    System.out.println(track.getInfo().uri);
                    System.out.println(track.getInfo().length);
                    System.out.println(track.getSourceManager());
                    System.out.println(track.getInfo().artworkUrl);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    for (AudioTrack track : playlist.getTracks()) {
                        System.out.println(track.getInfo().title);
                        System.out.println(track.getInfo().author);
                        System.out.println(track.getInfo().uri);
                        System.out.println(track.getDuration());
                        System.out.println(track.getInfo().artworkUrl);
                        System.out.println("-----");
                    }
                }

                @Override
                public void noMatches() {
                    System.out.println("No matching items found");
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    exception.printStackTrace();
                }
            }).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
