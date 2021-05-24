# LavaPlayer - Audio player library for Discord

LavaPlayer is an audio player library written in Java which can load audio tracks from various sources and convert them into a stream of Opus frames. It is designed for use with Discord bots, but it can be used anywhere where Opus format output is required.

**Please read the [FAQ](FAQ.md) in case of issues.**

#### Maven package

Replace `x.y.z` with the latest version number: 1.3.77 

* Repository: https://m2.dv8tion.net/releases
* Artifact: **com.sedmelluq:lavaplayer:x.y.z**

Using in Gradle:
```gradle
repositories {
  maven {
    url 'https://m2.dv8tion.net/releases'
  }
}

dependencies {
  implementation 'com.sedmelluq:lavaplayer:x.y.z'
}
```

Using in Maven:
```xml
<repositories>
  <repository>
    <id>dv8tion</id>
    <name>m2-dv8tion</name>
    <url>https://m2.dv8tion.net/releases</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.sedmelluq</groupId>
    <artifactId>lavaplayer</artifactId>
    <version>x.y.z</version>
  </dependency>
</dependencies>
```


## Supported formats

The set of sources where LavaPlayer can load tracks from is easily extensible, but the ones currently included by default are:

* YouTube
* SoundCloud
* Bandcamp
* Vimeo
* Twitch streams
* Local files
* HTTP URLs

The file formats that LavaPlayer can currently handle are (relevant for file/url sources):

* MP3
* FLAC
* WAV
* Matroska/WebM (AAC, Opus or Vorbis codecs)
* MP4/M4A (AAC codec)
* OGG streams (Opus, Vorbis and FLAC codecs)
* AAC streams
* Stream playlists (M3U and PLS)

## Resource usage

What makes LavaPlayer unique is that it handles everything in the same process. Different sources and container formats are handled in Java, while decoding and encoding of audio are handled by embedded native libraries. This gives it a very fine-grained control over the resources that it uses, which means a low memory footprint as well as the chance to skip decoding and encoding steps altogether when the input format matches the output format. Some key things to remember:

* Memory usage is both predictable and low. The amount of memory used per track when testing with YouTube was at most 350 kilobytes per track plus the off-heap memory for the thread stack, since there is one thread per playing track.
* The most common format used in YouTube is Opus, which matches the exact output format required for Discord. When no volume adjustment is applied, the packets from YouTube are directly passed to output, which saves CPU cycles.
* Resource leaks are unlikely because there are no additional processes launched and only one thread per playing track. When an audio player is not queried for an user-configured amount of time, then the playing track is aborted and the thread cleaned up. This avoids thread leaks even when the audio player is not shut down as it is supposed to.

## Features

#### Precise seeking support

Seeking is supported on all non-stream formats and sources. When a seek is performed on a playing track, the previously buffered audio samples will be provided until the seek is finished (this is configurable). When a seek is performed on a track which has not yet started, it will start immediately from the chosen position.

Due to media containers supporting seeking at different resolutions, the position that a media player can start reading data from might be several seconds from the location that the user actually wanted to seek to. LavaPlayer handles it by remembering the position where it was requested to seek to, jumping to the highest position which is not after that and then ignoring the audio until the actual position that was requested. This provides a millisecond accuracy on seeking.

#### Easy track loading

When creating an instance of an `AudioPlayerManager`, sources where the tracks should be loaded from with it must be manually registered. When loading tracks, you pass the manager an identifier and a handler which will get asynchronously called when the result has arrived. The handler has separate methods for receiving resolved tracks, resolved playlists, exceptions or being notified when nothing was found for the specified identifier.

Since the tracks hold only minimal meta-information (title, author, duration and identifier), loading playlists does not usually require the library to check the page of each individual track for sources such as YouTube or SoundCloud. This makes loading playlists pretty fast.

#### Load balancing

LavaPlayer includes the support for delegating the decoding/encoding/resampling operations to separate nodes running the lavaplayer-node Spring Boot application. These can be easily enabled by calling:

```java
manager.useRemoteNodes("somehost:8080", "otherhost:8080")
```

The library will automatically assign the processing of new tracks to them by selecting a node based on the number of tracks they are currently processing and the CPU usage of the machine they are running on.

#### Extensibility

Any source that implements the `AudioSourceManager` interface can be registered to the player manager. These can be custom sources using either some of the supported containers and codecs or defining a totally new way the tracks are actually executed, such as delegating it to another process, should the set of formats supported by LavaPlayer by default not be enough.

## Usage

#### Creating an audio player manager

First thing you have to do when using the library is to create a `DefaultAudioPlayerManager` and then configure it to use the settings and sources you want. Here is a sample:

```java
AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
AudioSourceManagers.registerRemoteSources(playerManager);
```

There are various configuration settings that can be modified:
* Opus encoding and resampling quality settings.
* Frame buffer duration: how much of audio is buffered in advance.
* Stuck track threshold: when no data from a playing track comes in the specified time, an event is sent.
* Abandoned player cleanup threshold: when the player is not queried in the specified amount of time, it is stopped.
* Garbage collection monitoring: logs statistics of garbage collection pauses every 2 minutes. If the pauses are long enough to cause a stutter in the audio, it will be logged with a warning level, so you could take action to optimize your GC settings.

If possible, you should use a single instance of a player manager for your whole application. A player manager manages several thread pools which make no sense to duplicate.

#### Creating an audio player

Once you have a player manager, you can create players from it. Generally you would want to create a player per every different target you might want to separately stream audio to. It is totally fine to create them even if they are unlikely to be used, as they do not use any resources on their own without an active track.

Creating a player is rather simple:

```java
AudioPlayer player = playerManager.createPlayer();
```

Once you have an instance of an audio player, you need some way to receive events from it. For that you should register a listener to it which either extends the `AudioEventAdapter` class or implements `AudioEventListener`. Since that listener receives the events for starting and ending tracks, it makes sense to also make it responsible for scheduling tracks. Assuming `TrackScheduler` is a class that implements `AudioEventListener`:

```java
TrackScheduler trackScheduler = new TrackScheduler(player);
player.addListener(trackScheduler);
```

Now you have an audio player capable of playing instances of `AudioTrack`. However, what you don't have is audio tracks, which are the next things you have to obtain.

#### Loading audio tracks

To load a track, you have to call either the `loadItem` or `loadItemOrdered` method of an `AudioPlayerManager`. `loadItem` takes an identifier parameter and a load handler parameter. The identifier is a piece of text that should identify the track for some source. For example if it is a YouTube video ID, then YouTube source manager will load it, if it is a file path then the local file source will load it. The handler parameter is an instance of `AudioLoadResultHandler`, which has separate methods for different results of the loading process. You can either have a dedicated class for this or you can simply pass it an anonymous class as in the next example:

```java
playerManager.loadItem(identifier, new AudioLoadResultHandler() {
  @Override
  public void trackLoaded(AudioTrack track) {
    trackScheduler.queue(track);
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    for (AudioTrack track : playlist.getTracks()) {
      trackScheduler.queue(track);
    }
  }

  @Override
  public void noMatches() {
    // Notify the user that we've got nothing
  }

  @Override
  public void loadFailed(FriendlyException throwable) {
    // Notify the user that everything exploded
  }
});
```

Most of these methods are rather obvious. In addition to everything exploding, `loadFailed` will also be called for example when a YouTube track is blocked or not available in your area. The `FriendlyException` class has a field called `severity`. If the value of this is `COMMON`, then it means that the reason is definitely not a bug or a network issue, but because the track is not available, such as the YouTube blocked video example. These message in this case can simply be forwarded as is to the user.

The other method for loading tracks, `loadItemOrdered` is for cases where you want the tracks to be loaded in order for example within one player. `loadItemOrdered` takes an ordering channel key as the first parameter, which is simply any object which remains the same for all the requests that should be loaded in the same ordered queue. The most common use would probably be to just pass it the `AudioPlayer` instance that the loaded tracks will be queued for.

#### Playing audio tracks

In the previous example I did not actually start playing the loaded track yet, but sneakily passed it on to our fictional `TrackScheduler` class instead. Starting the track is however a trivial thing to do:

```java
player.playTrack(track);
```

Now the track should be playing, which means buffered for whoever needs it to poll its frames. However, you would need to somehow react to events, most notably the track finishing, so you could start the next track.

#### Handling events

Events are handled by event handlers added to an `AudioPlayer` instance. The simplest way for creating the handler is to extend the `AudioEventAdapter` class. Here is a quick description of each of the methods it has, in the context of using it for a track scheduler:

```java
public class TrackScheduler extends AudioEventAdapter {
  @Override
  public void onPlayerPause(AudioPlayer player) {
    // Player was paused
  }

  @Override
  public void onPlayerResume(AudioPlayer player) {
    // Player was resumed
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    // A track started playing
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (endReason.mayStartNext) {
      // Start next track
    }

    // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
    // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
    // endReason == STOPPED: The player was stopped.
    // endReason == REPLACED: Another track started playing while this had not finished
    // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
    //                       clone of this back to your queue
  }

  @Override
  public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    // An already playing track threw an exception (track end event will still be received separately)
  }

  @Override
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    // Audio track has been unable to provide us any audio, might want to just start a new track
  }
}
```

#### JDA integration

To use it with JDA 4, you would need an instance of `AudioSendHandler`. There is only the slight difference of no separate `canProvide` and `provide` methods in `AudioPlayer`, so the wrapper for this is simple:

```java
public class AudioPlayerSendHandler implements AudioSendHandler {
  private final AudioPlayer audioPlayer;
  private AudioFrame lastFrame;

  public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
  }

  @Override
  public boolean canProvide() {
    lastFrame = audioPlayer.provide();
    return lastFrame != null;
  }

  @Override
  public ByteBuffer provide20MsAudio() {
    return ByteBuffer.wrap(lastFrame.getData());
  }

  @Override
  public boolean isOpus() {
    return true;
  }
}
```
