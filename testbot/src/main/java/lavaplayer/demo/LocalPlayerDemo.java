package lavaplayer.demo;

import lavaplayer.format.AudioDataFormat;
import lavaplayer.format.AudioPlayerInputStream;
import lavaplayer.manager.AudioPlayer;
import lavaplayer.manager.AudioPlayerManager;
import lavaplayer.manager.DefaultAudioPlayerManager;
import lavaplayer.manager.FunctionalResultHandler;
import lavaplayer.source.AudioSourceManagers;

import javax.sound.sampled.*;
import java.io.IOException;

import static lavaplayer.format.StandardAudioDataFormats.COMMON_PCM_S16_BE;

public class LocalPlayerDemo {
    public static void main(String[] args) throws LineUnavailableException, IOException {
        AudioPlayerManager manager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(manager);
        manager.getConfiguration().setOutputFormat(COMMON_PCM_S16_BE);

        AudioPlayer player = manager.createPlayer();

        manager.loadItem("ytsearch:DHL frank ocean", new FunctionalResultHandler(null, playlist -> {
            player.playTrack(playlist.getTracks().get(0));
        }, null, null));

        AudioDataFormat format = manager.getConfiguration().getOutputFormat();
        AudioInputStream stream = AudioPlayerInputStream.createStream(player, format, 10000L, false);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(stream.getFormat());
        line.start();

        byte[] buffer = new byte[COMMON_PCM_S16_BE.maximumChunkSize()];
        int chunkSize;

        while ((chunkSize = stream.read(buffer)) >= 0) {
            line.write(buffer, 0, chunkSize);
        }
    }
}
