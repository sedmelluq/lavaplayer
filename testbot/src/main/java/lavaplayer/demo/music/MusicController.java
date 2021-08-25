package lavaplayer.demo.music;

import lavaplayer.demo.BotApplicationManager;
import lavaplayer.demo.BotGuildContext;
import lavaplayer.demo.MessageDispatcher;
import lavaplayer.demo.controller.BotCommandHandler;
import lavaplayer.demo.controller.BotController;
import lavaplayer.demo.controller.BotControllerFactory;
import lavaplayer.filter.equalizer.EqualizerFactory;
import lavaplayer.manager.AudioPlayer;
import lavaplayer.manager.AudioPlayerManager;
import lavaplayer.source.youtube.YoutubeItemSourceManager;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.PlayerLibrary;
import lavaplayer.tools.io.MessageInput;
import lavaplayer.tools.io.MessageOutput;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackCollection;
import lavaplayer.track.DecodedTrackHolder;
import lavaplayer.track.TrackMarker;
import lavaplayer.track.loading.ItemLoadResultAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import net.iharder.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class MusicController implements BotController {
    private static final float[] BASS_BOOST = {0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f,
        -0.1f, -0.1f, -0.1f, -0.1f};

    private final AudioPlayerManager manager;
    private final AudioPlayer player;
    private final AtomicReference<TextChannel> outputChannel;
    private final MusicScheduler scheduler;
    private final Guild guild;
    private final EqualizerFactory equalizer;

    public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild) {
        this.manager = manager.getPlayerManager();
        this.guild = guild;
        this.equalizer = new EqualizerFactory();

        player = manager.getPlayerManager().createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

        outputChannel = new AtomicReference<>();

        MessageDispatcher messageDispatcher = new GlobalDispatcher();
        scheduler = new MusicScheduler(player, messageDispatcher, manager.getExecutorService());

        player.addListener(scheduler);
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                if ("Testing".equals(voiceChannel.getName())) {
                    audioManager.openAudioConnection(voiceChannel);
                    return;
                }
            }

            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                return;
            }
        }
    }

    @BotCommandHandler
    private void add(Message message, String identifier) {
        addTrack(message, identifier, false);
    }

    @BotCommandHandler
    private void now(Message message, String identifier) {
        addTrack(message, identifier, true);
    }

    @BotCommandHandler
    private void hex(Message message, int pageCount) {
        manager.source(YoutubeItemSourceManager.class).setPlaylistPageCount(pageCount);
    }

    @BotCommandHandler
    private void serialize(Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutput outputStream = new MessageOutput(baos);

        for (AudioTrack track : scheduler.drainQueue()) {
            manager.encodeTrack(outputStream, track);
        }

        outputStream.finish();

        message.getChannel().sendMessage(Base64.encodeBytes(baos.toByteArray())).queue();
    }

    @BotCommandHandler
    private void deserialize(Message message, String content) throws IOException {
        outputChannel.set((TextChannel) message.getChannel());
        connectToFirstVoiceChannel(guild.getAudioManager());

        byte[] bytes = Base64.decode(content);

        MessageInput inputStream = new MessageInput(new ByteArrayInputStream(bytes));
        DecodedTrackHolder holder;

        while ((holder = manager.decodeTrack(inputStream)) != null) {
            scheduler.addToQueue(holder.decodedTrack);
        }
    }

    @BotCommandHandler
    private void eqsetup(Message message) {
        manager.getConfiguration().setFilterHotSwapEnabled(true);
        player.setFrameBufferDuration(500);
    }

    @BotCommandHandler
    private void eqstart(Message message) {
        player.setFilterFactory(equalizer);
    }

    @BotCommandHandler
    private void eqstop(Message message) {
        player.setFilterFactory(null);
    }

    @BotCommandHandler
    private void eqband(Message message, int band, float value) {
        equalizer.setGain(band, value);
    }

    @BotCommandHandler
    private void eqhighbass(Message message, float diff) {
        for (int i = 0; i < BASS_BOOST.length; i++) {
            equalizer.setGain(i, BASS_BOOST[i] + diff);
        }
    }

    @BotCommandHandler
    private void eqlowbass(Message message, float diff) {
        for (int i = 0; i < BASS_BOOST.length; i++) {
            equalizer.setGain(i, -BASS_BOOST[i] + diff);
        }
    }

    @BotCommandHandler
    private void volume(Message message, int volume) {
        player.setVolume(volume);
    }

    @BotCommandHandler
    private void skip(Message message) {
        scheduler.skip();
    }

    @BotCommandHandler
    private void forward(Message message, int duration) {
        forPlayingTrack(track -> track.setPosition(track.getPosition() + duration));
    }

    @BotCommandHandler
    private void back(Message message, int duration) {
        forPlayingTrack(track -> track.setPosition(Math.max(0, track.getPosition() - duration)));
    }

    @BotCommandHandler
    private void pause(Message message) {
        player.setPaused(true);
    }

    @BotCommandHandler
    private void resume(Message message) {
        player.setPaused(false);
    }

    @BotCommandHandler
    private void duration(Message message) {
        forPlayingTrack(track -> message.getChannel().sendMessage("Duration is " + track.getDuration()).queue());
    }

    @BotCommandHandler
    private void seek(Message message, long position) {
        forPlayingTrack(track -> track.setPosition(position));
    }

    @BotCommandHandler
    private void pos(Message message) {
        forPlayingTrack(track -> message.getChannel().sendMessage("Position is " + track.getPosition()).queue());
    }

    @BotCommandHandler
    private void marker(final Message message, long position, final String text) {
        forPlayingTrack(track -> track.setMarker(new TrackMarker(position, state -> message.getChannel().sendMessage("Trigger [" + text + "] cause [" + state.name() + "]").queue())));
    }

    @BotCommandHandler
    private void unmark(Message message) {
        forPlayingTrack(track -> track.setMarker(null));
    }

    @BotCommandHandler
    private void version(Message message) {
        message.getChannel().sendMessage(PlayerLibrary.VERSION).queue();
    }

    @BotCommandHandler
    private void leave(Message message) {
        guild.getAudioManager().closeAudioConnection();
    }

    private void addTrack(final Message message, final String identifier, final boolean now) {
        outputChannel.set((TextChannel) message.getChannel());

        var itemLoader = manager.getItemLoaders().createItemLoader(identifier);

        itemLoader.setResultHandler(new ItemLoadResultAdapter() {
            @Override
            public void onTrack(AudioTrack track) {
                connectToFirstVoiceChannel(guild.getAudioManager());

                message.getChannel().sendMessage("Starting now: " + track.getInfo().title + " (length " + track.getDuration() + ")").queue();
                if (now) {
                    scheduler.playNow(track, true);
                } else {
                    scheduler.addToQueue(track);
                }
            }

            @Override
            public void onTrackCollection(AudioTrackCollection playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                message.getChannel().sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")").queue();

                connectToFirstVoiceChannel(guild.getAudioManager());

                AudioTrack selected = playlist.getSelectedTrack();

                if (selected != null) {
                    message.getChannel().sendMessage("Selected track from playlist: " + selected.getInfo().title).queue();
                } else {
                    selected = tracks.get(0);
                    message.getChannel().sendMessage("Added first track from playlist: " + selected.getInfo().title).queue();
                }

                if (now) {
                    scheduler.playNow(selected, true);
                } else {
                    scheduler.addToQueue(selected);
                }

                for (int i = 0; i < Math.min(10, playlist.getTracks().size()); i++) {
                    if (tracks.get(i) != selected) {
                        scheduler.addToQueue(tracks.get(i));
                    }
                }
            }

            @Override
            public void onNoMatches() {
                message.getChannel().sendMessage("Nothing found for " + identifier).queue();
            }

            @Override
            public void onLoadFailed(FriendlyException throwable) {
                message.getChannel().sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")").queue();
            }
        });

        itemLoader.load();
    }

    private void forPlayingTrack(TrackOperation operation) {
        AudioTrack track = player.getPlayingTrack();

        if (track != null) {
            operation.execute(track);
        }
    }

    private interface TrackOperation {
        void execute(AudioTrack track);
    }

    public static class Factory implements BotControllerFactory<MusicController> {
        @Override
        public Class<MusicController> getControllerClass() {
            return MusicController.class;
        }

        @Override
        public MusicController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
            return new MusicController(manager, state, guild);
        }
    }

    private class GlobalDispatcher implements MessageDispatcher {
        @Override
        public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure) {
            TextChannel channel = outputChannel.get();

            if (channel != null) {
                channel.sendMessage(message).queue(success, failure);
            }
        }

        @Override
        public void sendMessage(String message) {
            TextChannel channel = outputChannel.get();

            if (channel != null) {
                channel.sendMessage(message).queue();
            }
        }
    }
}