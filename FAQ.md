# FAQ and common issues

## Errors

**ALWAYS** include stack traces and the track that was played (in case of an URL) when reporting an error.

### `NoSuchMethodError` and `NoClassDefFoundError`

If Maven is used for building, then this is likely a problem with the way Maven handles version conflicts. When there are two things which require different versions of a dependency, Maven uses the braindead strategy of choosing the one which is the least transitive. This way it will likely choose an older version than the one required by LavaPlayer, which will cause runtime errors.

Unfortunately it is [not possible](https://stackoverflow.com/questions/34201120/maven-set-dependency-mediation-strategy-to-newest-rather-than-nearest) to currently change the version conflict resolution strategy in Maven, so the only solution is to [check the dependency tree](https://maven.apache.org/plugins/maven-dependency-plugin/examples/resolving-conflicts-using-the-dependency-tree.html) and manually declare a dependency on the highest version of the affected package that can be seen in the tree.

An alternative is to use Gradle, which has a sane default (and if necessary, highly configurable) version conflict resolution strategy.

### Error `Something went wrong when...`

This is just a wrapper exception around all other exceptions, check the full stack trace.

### `UnsatisfiedLinkError` <sup><sup><sub><a name="8d9389a5" href="#8d9389a5">8d9389a5</a></sub></sup></sup>

LavaPlayer uses native libraries for some audio encoding and decoding operations. The only platforms that it is compiled for out of the box are:

* Windows x86-64
* Linux x86-64, only compatible with libc, not with musl (Alpine uses musl)
* OS X x86-64

Only file a bug if you are sure you are running the library on a supported platform, but are still getting the exception. Keep in mind that you have to make sure that you are also running a 64-bit JDK. It is a common mistake to run a 32-bit JDK on 64-bit Windows.

For other platforms, there are third-party sources for the native libraries compiled for other platforms. Using those usually involves including an additional dependency in your project.

Third-party provided libraries for support for additional platforms:

* Bundle from various sources
  * Platforms: `linux-aarch32`, `linux-aarch64`, `linux-arm`, `linux-armhf`, `linux-x86`
  * Repository: `https://dl.bintray.com/sedmelluq/com.sedmelluq`
  * Dependency: `com.sedmelluq:lavaplayer-natives-extra:1.3.13`

### YouTube response code 429, or `JsonParseException` <sup><sup><sub><a name="4f89cc01" href="#4f89cc01">4f89cc01</a></sub></sup></sup>

For `JsonParseException`, only applicable if the message starts with `Unexpected character ('<' (code 60))`.

Your IP is being rate limited or blocked by YouTube. The most reliable way to get around this is to get an IPv6 block for your bot and then make use of the `youtube-rotator` plugin. For further details, you could ask in JDA Discord server (also check pins), as there are people around there who are using such a setup.   

### Exception with last cause being `Read timed out` <sup><sup><sub><a name="63a9aae9" href="#63a9aae9">63a9aae9</a></sub></sup></sup>

Could be a network issue on either side. If it happens rarely, then there is not much to do about it as it is unlikely to ever find out what caused the delay at that specific time.

If this happens all the time, I would suspect your network is somehow adding some massive latency. You could try if changing the socket timeouts makes any difference:

```
manager.setHttpRequestConfigurator(config ->
    RequestConfig.copy(config)
        .setSocketTimeout(10000)
        .setConnectTimeout(10000)
        .build()
);
```

For specific sources you can do the same thing by doing by calling `sourceManager.configureRequests` method, which is available for sources that make HTTP requests directly;

## Issues

### Does not work and no exceptions <sup><sup><sub><a name="a7c7989f" href="#a7c7989f">a7c7989f</a></sub></sup></sup>

I don't believe you. Make sure you check, double check and triple check all the following things:

* You have logging enabled and configured - you can verify by either seeing `INFO` logs about native libraries getting loaded, or when you have HTTP source enabled, you get a `WARN` log if you try to load `http://random.garbage.url`
* You are handling `AudioLoadResultHandler#loadFailed` and outputting the exceptions in there or at least in some way reliably making sure you know when it gets called.
* You are handling `AudioLoadResultHandler#noMatches` and making sure it is not getting called when you except a track/playlist.
* You are handling both `AudioLoadResultHandler#trackLoaded` and `AudioLoadResultHandler#playlistLoaded` in case you got playlist when you expected a track or vice versa.
* You are handling `AudioEventAdapter#onTrackException` (or `TrackExceptionEvent` in `AudioEventListener`) and outputting the exception or at least in some way reliably making sure you know when it gets called.
* If the player appears to "do nothing", check with `AudioPlayer#getPlayingTrack` if it is aware of the track you provided.
  * If true, check that you are actually calling `AudioPlayer#provide`, if you are running locally, set a breakpoint. If you are sure, take a thread dump and post it.
  * If false, there was an end event sent to your event handler (assuming you actually called `playTrack` or `startTrack`). Log it, look at it, think about it, and if it makes no sense to you, post it.

### Everything triggers `noMatches`.

Did you register source managers? For playing remote tracks (URLs including YouTube):

```
AudioSourceManagers.registerRemoteSources(playerManager);
```

For playing local tracks (files on disk):

```
AudioSourceManagers.registerLocalSource(playerManager);
```

### Sources are registered, but all YouTube tracks trigger `noMatches`.

Make sure you do not convert the input you pass to `AudioPlayerManager#loadItem` to lowercase as YouTube URLs are case-sensitive.

### Playback stutters <sup><sup><sub><a name="45aba0a6" href="#45aba0a6">45aba0a6</a></sub></sup></sup>

In the class that calls `AudioPlayer#provide`, record the number of times it returns a `null`. If the count constantly increases over time (excluding around when a track starts when it is fine to happen), it might be LavaPlayer lagging behind (possibly the CPU is under too much load).

Otherwise, it is an issue with either packet sending or network. Packet sending issues might be caused by garbage collection pauses in the JVM - for JDA this is mitigated by [JDA-NAS](https://github.com/sedmelluq/jda-nas). For other Discord libraries, you should compare with a test bot with JDA+JDA-NAS on the same machine to verify it is not an issue with the library.