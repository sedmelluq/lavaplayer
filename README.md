# LavaPlayer - Audio player library for Discord

LavaPlayer is an audio player library written in Java which can load audio tracks from various sources and convert them
into a stream of Opus frames. It is designed for use with Discord bots, but it can be used anywhere where Opus format
output is required.

## Changes 

This is a heavily modified fork of lavaplayer, almost everything that isn't an internal api has been changed. If you
use this fork I promise you a partial rewrite of everything interacting with lavaplayer (both kotlin and java code)

### Fixes

- Fixes SoundCloud playback
- Fixes YouTube music searching
- Fixes YouTube 403 errors?

### New Features

- Added AudioTrackInfo#artworkUrl

### Breaking Changes

I have rewrote many systems because java sucks 

- Added `AudioTrackCollection` to replace `AudioPlaylist`
- Many files have been changed from java to kotlin
- Completely rewrote track loading.
  - Track loading now uses coroutines
  - Removed ordered track loading due to coroutines 
  - ItemSourceManager's can now send messages to the item loader (may be removed)
- Made track de/encoding separate from the AudioPlayerManager interface
- Made AudioPlayer and AudioPlayerManager use the kotlin coroutines api
- Renamed AudioSourceManager to ItemSourceManager
- com.sedmelluq.discord.lavaplayer -> lavaplayer
- Removed several useless modules
  - demo d4j & jda
  - node
  - stream-merger
  - extensions/format-xm
- Renamed youtube-rotator to ip-rotator
  - Removed references to "youtube" in unrelated classes

[comment]: <> (**Please read the [FAQ]&#40;FAQ.md&#41; in case of issues.**)

## Usage

### Maven package

Replace `x.y.z` with the latest version number: 1.4.6

* Repository: https://dimensional.jfrog.io/artifactory/maven

* Artifact: **com.sedmelluq:lavaplayer:x.y.z**

Using in Gradle:

```gradle
repositories {
  maven {
    url 'https://dimensional.jfrog.io/artifactory/maven'
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
        <id>dimensional</id>
        <name>dimensional</name>
        <url>https://dimensional.jfrog.io/artifactory/maven</url>
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

[comment]: <> (## Supported formats)

[comment]: <> (The set of sources where LavaPlayer can load tracks from is easily extensible, but the ones currently included by)

[comment]: <> (default are:)

[comment]: <> (* YouTube)

[comment]: <> (* SoundCloud)

[comment]: <> (* Bandcamp)

[comment]: <> (* Vimeo)

[comment]: <> (* Twitch streams)

[comment]: <> (* Local files)

[comment]: <> (* HTTP URLs)

[comment]: <> (The file formats that LavaPlayer can currently handle are &#40;relevant for file/url sources&#41;:)

[comment]: <> (* MP3)

[comment]: <> (* FLAC)

[comment]: <> (* WAV)

[comment]: <> (* Matroska/WebM &#40;AAC, Opus or Vorbis codecs&#41;)

[comment]: <> (* MP4/M4A &#40;AAC codec&#41;)

[comment]: <> (* OGG streams &#40;Opus, Vorbis and FLAC codecs&#41;)

[comment]: <> (* AAC streams)

[comment]: <> (* Stream playlists &#40;M3U and PLS&#41;)

[comment]: <> (## Resource usage)

[comment]: <> (What makes LavaPlayer unique is that it handles everything in the same process. Different sources and container formats)

[comment]: <> (are handled in Java, while decoding and encoding of audio are handled by embedded native libraries. This gives it a very)

[comment]: <> (fine-grained control over the resources that it uses, which means a low memory footprint as well as the chance to skip)

[comment]: <> (decoding and encoding steps altogether when the input format matches the output format. Some key things to remember:)

[comment]: <> (* Memory usage is both predictable and low. The amount of memory used per track when testing with YouTube was at most)

[comment]: <> (  350 kilobytes per track plus the off-heap memory for the thread stack, since there is one thread per playing track.)

[comment]: <> (* The most common format used in YouTube is Opus, which matches the exact output format required for Discord. When no)

[comment]: <> (  volume adjustment is applied, the packets from YouTube are directly passed to output, which saves CPU cycles.)

[comment]: <> (* Resource leaks are unlikely because there are no additional processes launched and only one thread per playing track.)

[comment]: <> (  When an audio player is not queried for an user-configured amount of time, then the playing track is aborted and the)

[comment]: <> (  thread cleaned up. This avoids thread leaks even when the audio player is not shut down as it is supposed to.)

[comment]: <> (## Features)

[comment]: <> (#### Precise seeking support)

[comment]: <> (Seeking is supported on all non-stream formats and sources. When a seek is performed on a playing track, the previously)

[comment]: <> (buffered audio samples will be provided until the seek is finished &#40;this is configurable&#41;. When a seek is performed on a)

[comment]: <> (track which has not yet started, it will start immediately from the chosen position.)

[comment]: <> (Due to media containers supporting seeking at different resolutions, the position that a media player can start reading)

[comment]: <> (data from might be several seconds from the location that the user actually wanted to seek to. LavaPlayer handles it by)

[comment]: <> (remembering the position where it was requested to seek to, jumping to the highest position which is not after that and)

[comment]: <> (then ignoring the audio until the actual position that was requested. This provides a millisecond accuracy on seeking.)

[comment]: <> (#### Easy track loading)

[comment]: <> (When creating an instance of an `AudioPlayerManager`, sources where the tracks should be loaded from with it must be)

[comment]: <> (manually registered. When loading tracks, you pass the manager an identifier and a handler which will get asynchronously)

[comment]: <> (called when the result has arrived. The handler has separate methods for receiving resolved tracks, resolved playlists,)

[comment]: <> (exceptions or being notified when nothing was found for the specified identifier.)

[comment]: <> (Since the tracks hold only minimal meta-information &#40;title, author, duration and identifier&#41;, loading playlists does not)

[comment]: <> (usually require the library to check the page of each individual track for sources such as YouTube or SoundCloud. This)

[comment]: <> (makes loading playlists pretty fast.)

[comment]: <> (#### Load balancing)

[comment]: <> (LavaPlayer includes the support for delegating the decoding/encoding/resampling operations to separate nodes running the)

[comment]: <> (lavaplayer-node Spring Boot application. These can be easily enabled by calling:)

[comment]: <> (```java)

[comment]: <> (manager.useRemoteNodes&#40;"somehost:8080","otherhost:8080"&#41;)

[comment]: <> (```)

[comment]: <> (The library will automatically assign the processing of new tracks to them by selecting a node based on the number of)

[comment]: <> (tracks they are currently processing and the CPU usage of the machine they are running on.)

[comment]: <> (#### Extensibility)

[comment]: <> (Any source that implements the `AudioSourceManager` interface can be registered to the player manager. These can be)

[comment]: <> (custom sources using either some of the supported containers and codecs or defining a totally new way the tracks are)

[comment]: <> (actually executed, such as delegating it to another process, should the set of formats supported by LavaPlayer by)

[comment]: <> (default not be enough.)

[comment]: <> (## Usage)

[comment]: <> (#### Creating an audio player manager)

[comment]: <> (First thing you have to do when using the library is to create a `DefaultAudioPlayerManager` and then configure it to)

[comment]: <> (use the settings and sources you want. Here is a sample:)

[comment]: <> (```java)

[comment]: <> (AudioPlayerManager playerManager=new DefaultAudioPlayerManager&#40;&#41;;)

[comment]: <> (    AudioSourceManagers.registerRemoteSources&#40;playerManager&#41;;)

[comment]: <> (```)

[comment]: <> (There are various configuration settings that can be modified:)

[comment]: <> (* Opus encoding and resampling quality settings.)

[comment]: <> (* Frame buffer duration: how much of audio is buffered in advance.)

[comment]: <> (* Stuck track threshold: when no data from a playing track comes in the specified time, an event is sent.)

[comment]: <> (* Abandoned player cleanup threshold: when the player is not queried in the specified amount of time, it is stopped.)

[comment]: <> (* Garbage collection monitoring: logs statistics of garbage collection pauses every 2 minutes. If the pauses are long)

[comment]: <> (  enough to cause a stutter in the audio, it will be logged with a warning level, so you could take action to optimize)

[comment]: <> (  your GC settings.)

[comment]: <> (If possible, you should use a single instance of a player manager for your whole application. A player manager manages)

[comment]: <> (several thread pools which make no sense to duplicate.)

[comment]: <> (#### Creating an audio player)

[comment]: <> (Once you have a player manager, you can create players from it. Generally you would want to create a player per every)

[comment]: <> (different target you might want to separately stream audio to. It is totally fine to create them even if they are)

[comment]: <> (unlikely to be used, as they do not use any resources on their own without an active track.)

[comment]: <> (Creating a player is rather simple:)

[comment]: <> (```java)

[comment]: <> (AudioPlayer player=playerManager.createPlayer&#40;&#41;;)

[comment]: <> (```)

[comment]: <> (Once you have an instance of an audio player, you need some way to receive events from it. For that you should register)

[comment]: <> (a listener to it which either extends the `AudioEventAdapter` class or implements `AudioEventListener`. Since that)

[comment]: <> (listener receives the events for starting and ending tracks, it makes sense to also make it responsible for scheduling)

[comment]: <> (tracks. Assuming `TrackScheduler` is a class that implements `AudioEventListener`:)

[comment]: <> (```java)

[comment]: <> (TrackScheduler trackScheduler=new TrackScheduler&#40;player&#41;;)

[comment]: <> (    player.addListener&#40;trackScheduler&#41;;)

[comment]: <> (```)

[comment]: <> (Now you have an audio player capable of playing instances of `AudioTrack`. However, what you don't have is audio tracks,)

[comment]: <> (which are the next things you have to obtain.)

[comment]: <> (#### Loading audio tracks)

[comment]: <> (To load a track, you have to call either the `loadItem` or `loadItemOrdered` method of an `AudioPlayerManager`)

[comment]: <> (. `loadItem` takes an identifier parameter and a load handler parameter. The identifier is a piece of text that should)

[comment]: <> (identify the track for some source. For example if it is a YouTube video ID, then YouTube source manager will load it,)

[comment]: <> (if it is a file path then the local file source will load it. The handler parameter is an instance)

[comment]: <> (of `AudioLoadResultHandler`, which has separate methods for different results of the loading process. You can either)

[comment]: <> (have a dedicated class for this or you can simply pass it an anonymous class as in the next example:)

[comment]: <> (```java)

[comment]: <> (ItemLoader loader = playerManager.createItemLoader&#40;identifier&#41;;)

[comment]: <> (loader.setResultHandler&#40;new ItemLoadResultAdapter&#40;&#41;{)

[comment]: <> (    @Override)

[comment]: <> (    public void onTrackLoaded&#40;AudioTrack track&#41;{)

[comment]: <> (        trackScheduler.queue&#40;track&#41;;)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onCollectionLoaded&#40;AudioTrackCollection collection&#41;{)

[comment]: <> (        for&#40;AudioTrack track : collection.getTracks&#40;&#41;&#41;{)

[comment]: <> (            trackScheduler.queue&#40;track&#41;;)

[comment]: <> (        })

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void noMatches&#40;&#41;{)

[comment]: <> (        // Notify the user that we've got nothing)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void loadFailed&#40;FriendlyException throwable&#41;{)

[comment]: <> (        // Notify the user that everything exploded)

[comment]: <> (    })

[comment]: <> (}&#41;;)

[comment]: <> (loader.loadAsync&#40;&#41;;)

[comment]: <> (```)

[comment]: <> (Most of these methods are rather obvious. In addition to everything exploding, `loadFailed` will also be called for)

[comment]: <> (example when a YouTube track is blocked or not available in your area. The `FriendlyException` class has a field)

[comment]: <> (called `severity`. If the value of this is `COMMON`, then it means that the reason is definitely not a bug or a network)

[comment]: <> (issue, but because the track is not available, such as the YouTube blocked video example. These message in this case can)

[comment]: <> (simply be forwarded as is to the user.)

[comment]: <> (The other method for loading tracks, `loadItemOrdered` is for cases where you want the tracks to be loaded in order for)

[comment]: <> (example within one player. `loadItemOrdered` takes an ordering channel key as the first parameter, which is simply any)

[comment]: <> (object which remains the same for all the requests that should be loaded in the same ordered queue. The most common use)

[comment]: <> (would probably be to just pass it the `AudioPlayer` instance that the loaded tracks will be queued for.)

[comment]: <> (#### Playing audio tracks)

[comment]: <> (In the previous example I did not actually start playing the loaded track yet, but sneakily passed it on to our)

[comment]: <> (fictional `TrackScheduler` class instead. Starting the track is however a trivial thing to do:)

[comment]: <> (```java)

[comment]: <> (player.playTrack&#40;track&#41;;)

[comment]: <> (```)

[comment]: <> (Now the track should be playing, which means buffered for whoever needs it to poll its frames. However, you would need)

[comment]: <> (to somehow react to events, most notably the track finishing, so you could start the next track.)

[comment]: <> (#### Handling events)

[comment]: <> (Events are handled by event handlers added to an `AudioPlayer` instance. The simplest way for creating the handler is to)

[comment]: <> (extend the `AudioEventAdapter` class. Here is a quick description of each of the methods it has, in the context of using)

[comment]: <> (it for a track scheduler:)

[comment]: <> (```java)

[comment]: <> (public class TrackScheduler extends AudioEventAdapter {)

[comment]: <> (    @Override)

[comment]: <> (    public void onPlayerPause&#40;AudioPlayer player&#41; {)

[comment]: <> (        // Player was paused)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onPlayerResume&#40;AudioPlayer player&#41; {)

[comment]: <> (        // Player was resumed)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onTrackStart&#40;AudioPlayer player, AudioTrack track&#41; {)

[comment]: <> (        // A track started playing)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onTrackEnd&#40;AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason&#41; {)

[comment]: <> (        if &#40;endReason.mayStartNext&#41; {)

[comment]: <> (            // Start next track)

[comment]: <> (        })

[comment]: <> (        // endReason == FINISHED: A track finished or died by an exception &#40;mayStartNext = true&#41;.)

[comment]: <> (        // endReason == LOAD_FAILED: Loading of a track failed &#40;mayStartNext = true&#41;.)

[comment]: <> (        // endReason == STOPPED: The player was stopped.)

[comment]: <> (        // endReason == REPLACED: Another track started playing while this had not finished)

[comment]: <> (        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a)

[comment]: <> (        //                       clone of this back to your queue)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onTrackException&#40;AudioPlayer player, AudioTrack track, FriendlyException exception&#41; {)

[comment]: <> (        // An already playing track threw an exception &#40;track end event will still be received separately&#41;)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public void onTrackStuck&#40;AudioPlayer player, AudioTrack track, long thresholdMs&#41; {)

[comment]: <> (        // Audio track has been unable to provide us any audio, might want to just start a new track)

[comment]: <> (    })

[comment]: <> (})

[comment]: <> (```)

[comment]: <> (#### JDA integration)

[comment]: <> (To use it with JDA 4, you would need an instance of `AudioSendHandler`. There is only the slight difference of no)

[comment]: <> (separate `canProvide` and `provide` methods in `AudioPlayer`, so the wrapper for this is simple:)

[comment]: <> (```java)

[comment]: <> (public class AudioPlayerSendHandler implements AudioSendHandler {)

[comment]: <> (    private final AudioPlayer audioPlayer;)

[comment]: <> (    private AudioFrame lastFrame;)

[comment]: <> (    public AudioPlayerSendHandler&#40;AudioPlayer audioPlayer&#41; {)

[comment]: <> (        this.audioPlayer = audioPlayer;)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public boolean canProvide&#40;&#41; {)

[comment]: <> (        lastFrame = audioPlayer.provide&#40;&#41;;)

[comment]: <> (        return lastFrame != null;)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public ByteBuffer provide20MsAudio&#40;&#41; {)

[comment]: <> (        return ByteBuffer.wrap&#40;lastFrame.getData&#40;&#41;&#41;;)

[comment]: <> (    })

[comment]: <> (    @Override)

[comment]: <> (    public boolean isOpus&#40;&#41; {)

[comment]: <> (        return true;)

[comment]: <> (    })

[comment]: <> (})

[comment]: <> (```)
