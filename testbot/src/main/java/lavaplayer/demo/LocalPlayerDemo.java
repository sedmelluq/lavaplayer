package lavaplayer.demo;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.manager.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.manager.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.manager.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.common.SourceRegistry;
import com.sedmelluq.discord.lavaplayer.track.loader.DelegatedItemLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.loader.ItemLoader;

import javax.sound.sampled.*;
import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats.COMMON_PCM_S16_BE;

public class LocalPlayerDemo {
    public static void main(String[] args) throws LineUnavailableException, IOException {
        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        SourceRegistry.registerRemoteSources(manager);
        manager.getConfiguration().setOutputFormat(COMMON_PCM_S16_BE);

        AudioPlayer player = manager.createPlayer();

        /* load items. */
        ItemLoader itemLoader = manager.getItems().createItemLoader("https://www.youtube.com/watch?v=R76_7N4gyDA");
        itemLoader.setResultHandler(new DelegatedItemLoadResultHandler(
            player::playTrack,
            playlist -> player.playTrack(playlist.getTracks().get(0)),
            null,
            null
        ));

        itemLoader.loadAsync();

        /* do some more bullshit lol */
        AudioDataFormat format = manager.getConfiguration().getOutputFormat();
        AudioInputStream stream = AudioPlayerInputStream.createStream(player, format, 10000L, false);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(stream.getFormat());
        line.start();

        byte[] buffer = new byte[COMMON_PCM_S16_BE.getMaximumChunkSize()];
        int chunkSize;

        while ((chunkSize = stream.read(buffer)) >= 0) {
            line.write(buffer, 0, chunkSize);
        }
    }
}
