# Change Log

## [1.2.13] - 2017-02-22
### Fixed
- Fixed a regression with YouTube dashmpd-only tracks not working.

## [1.2.12] - 2017-02-18
### Added
- Support for alternative YouTube JSON format (adaptive_fmts -> url_encoded_fmt_stream_map).

## [1.2.11] - 2017-02-18
### Fixed
- Fixed interrupts from previous tracks may be carried over to executors of new tracks.

## [1.2.10] - 2017-02-11
### Added
- Beam.pro source manager.

## [1.2.9] - 2017-02-09
### Fixed
- Fixed reused clients restricting the number of concurrent connections to a very low value.
- Fixed track stop or seek throwing an exception in some conditions due to InterruptedException getting wrapped.

## [1.2.8] - 2017-02-09
### Changed
- Track info loader queue full exception is passed to loadFailed instead of being thrown directly.

### Fixed
- Fixed using wrong version of lavaplayer-common dependency (again).

## [1.2.7] - 2017-02-09
### Fixed
- Fixed track loading queue throwing "queue full" exception when the queue is not empty.

## [1.2.6] - 2017-02-08
### Fixed
- Fixed using wrong version of lavaplayer-common dependency.

## [1.2.5] - 2017-02-08
### Added
- Added the option to increase the number of threads used to load tracks.

## Changed
- Reusing HTTP client objects as much as possible.
- Limited the size of the track info loader queue.

## [1.2.4] - 2017-02-08
### Fixed
- Fixed YouTube live streams opening a new connection for each request.
- Fixed no read timeout applied to requests to nodes.
- Fixed track getting locked when seek gets stuck.
- Fixed attempting to read the entire response on closing a stream mid-way.

## [1.2.3] - 2017-02-07
### Changed
- Increased timeout for terminating the tracks of a node if its processing is stuck.

## [1.2.2] - 2017-02-06
### Fixed
- Fixed Vorbis MKV tracks not working if the channel count was not specified in MKV audio section.

## [1.2.1] - 2017-02-05
### Added
- Partial support for YouTube live streams (only MP4).

## [1.2.0] - 2017-02-04
### Added
- Method to poll frames from AudioPlayer in a blocking way.
- Support for different audio output formats.
- Utility class for getting an AudioInputStream from AudioPlayer.

## [1.1.47] - 2017-02-01
### Changed
- Fixed another smaller leak with Vorbis.

## [1.1.46] - 2017-02-01
### Fixed
- Fixed a native memory leak with Vorbis tracks.

## [1.1.45] - 2017-01-28
### Changed
- ARM binaries are now loaded from natives/linux-arm and natives/linux-aarch64 directories instead of x86 ones.

## [1.1.44] - 2017-01-28
### Added
- WAV file support (16-bit PCM).

## [1.1.43] - 2017-01-28
### Added
- Loading unlisted SoundCloud tracks.
- Searching on SoundCloud with scsearch: prefix.
- Option to specify maximum number of YouTube playlist pages to load (was hardcoded to 6).

### Fixed
- Fixed SoundCloud playlist tracks in wrong order.
- Fixed paid movies appearing in YouTube search results.
- Fixed YouTube playlists with UU prefix not working.

## [1.1.42] - 2017-01-16
### Added
- Support for OS X (native library).

## [1.1.41] - 2017-01-15
### Fixed
- Fixed YouTube tracks broken when player.js URL was given without hostname.

## [1.1.40] - 2017-01-14
### Fixed
- Fixed constant delay on processing node messages.
- Fixed making a new HTTP connection for each node request.

## [1.1.39] - 2017-01-14
### Changed
- Reduced playing track count effect on node balancing.

## [1.1.38] - 2017-01-09
### Changed
- Node balancing takes CPU and latency more seriously.
- Node messaging changed, requires node update to 1.1.38.

## [1.1.37] - 2017-01-04
### Changed
- MKV file handling refactored to be more lightweight.

## [1.1.36] - 2017-01-01
### Fixed
- Fixed track marker and position reset when starting the track.

## [1.1.35] - 2016-12-31
### Fixed
- Fixed loading HTTP urls with local redirects.

## [1.1.34] - 2016-12-29
### Fixed
- YouTube search results no longer include ads.

## [1.1.33] - 2016-12-27
### Fixed
- Fixed a regression with loading native libraries on Windows.

## [1.1.32] - 2016-12-24
### Fixed
- Fixed an issue when JDA-NAS and LavaPlayer were used together with different classloaders.

## [1.1.31] - 2016-12-22
### Added
- Made node balancing weight data available.

### Changed
- Request time to nodes also used for balancing between nodes.

## [1.1.30] - 2016-12-19
### Fixed
- Fixed an issue with MP3 files sometimes being detected as ADTS streams.

## [1.1.29] - 2016-12-17
### Added
- Made various statistics about remote nodes available.

## [1.1.28] - 2016-12-13
### Changed
- Made requests to nodes tolerate higher latency without stuttering.

## [1.1.27] - 2016-12-12
### Changed
- Changing remote node list does not stop tracks on unaffected nodes.

## [1.1.26] - 2016-12-11
### Changed
- Dependency restructuring for compatibility with JDA-NAS.

## [1.1.25] - 2016-12-08
### Added
- Support for legacy MP3 ID3v2.2 tags.

### Fixed
- Fixed nonexistent YouTube tracks being tried as raw HTTP urls.

## [1.1.24] - 2016-12-08
### Added
- Title and artist information for MP4/M4A files.

## [1.1.23] - 2016-12-01
### Added
- Can check if playlist is a search result.

### Fixed
- Fixed SoundCloud not working because because of SoundCloud client ID expiring.

## [1.1.22] - 2016-11-19
### Added
- Option to disable YouTube searches.

## [1.1.21] - 2016-11-18
### Added
- Support YouTube searches by using "ytsearch: query" as identifier.

## [1.1.20] - 2016-11-18
### Added
- Made it simple to register all bundled sources through the methods of AudioSourceManagers.

## [1.1.19] - 2016-11-18
### Changed
- Allow redirects from raw HTTP urls to other source providers.

### Fixed
- Fixed an exception on the end of some MP4 tracks.

## [1.1.18] - 2016-11-18
### Added
- Added mayEndTrack field to end reasons.

### Changed
- Special end reason for failed track initialization.

## [1.1.17] - 2016-11-16
### Fixed
- Fixed an issue with resolving correct YouTube stream URL due to a bug in cipher detection.

## [1.1.16] - 2016-11-13
### Changed
- Lowered default resampling quality.

## [1.1.15] - 2016-11-12
### Fixed
- Fixed some tracks mistakenly detected as MP3 streams.

## [1.1.14] - 2016-11-11
### Added
- Allow icy:// urls, which are used in some radio stream playlists.

## [1.1.13] - 2016-11-11
### Fixed
- Fixed exception on OGG streams when they end.
- SoundCloud fix + provider more resilient to site updates.

## [1.1.12] - 2016-11-11
### Added
- Track markers.

### Removed
- Loop feature. It can be done with markers.

## [1.1.11] - 2016-11-06
### Added
- SoundCloud liked tracks page loadable as a playlist.

## [1.1.10] - 2016-11-05
### Changed
- Common exception in case a playlist is private.

### Fixed
- Fixed YouTube track not loaded if it is part of a private playlist.

## [1.1.9] - 2016-11-05
### Added
- Twitch stream support.

## [1.1.8] - 2016-11-05
### Added
- Vimeo support.
- Support for the more common non-fragmented format of MP4/M4A.

## [1.1.7] - 2016-11-03
### Added
- IDv2.3 tag support.

### Fixed
- Fixed exception on long ID3 tags on streams.

## [1.1.6] - 2016-11-03
### Added
- Plain text files with an URL loaded as radio stream playlist.

### Fixed
- Fixed SoundCloud not working due to site update.

## [1.1.5] - 2016-11-03
### Added
- Support for PLS playlists for radio streams.
- Support for ICY protocol to fix some SHOUTcast streams.

## [1.1.4] - 2016-11-01
### Added
- Support for ADTS+AAC radio streams.

## [1.1.3] - 2016-10-31
### Added
- Bundled some LetsEncrypt SSL root certificate missing on some JDK installations.
- Support for adding custom SSL certificates.

### Fixed
- Fixed SoundCloud URLs not working with a slash in the end.

## [1.1.2] - 2016-10-30
### Added
- BandCamp support.

## [1.1.1] - 2016-10-28
### Added
- M3U support for radio streams.

## [1.1.0] - 2016-10-27
### Added
- MPEG2+layerIII MP3 format support.

### Fixed
- Fixed an exception on OGG stream playback.

## [1.0.14] - 2016-10-27
### Added
- OGG stream support.

### Fixed
- Fixed exception on loading age-restricted YouTube videos.
- Fixed an exception on decoding some FLAC files.

## [1.0.13] - 2016-10-23
### Fixed
- Fixed huge CPU load on tracks from local files.

## [1.0.12] - 2016-10-23
### Added
- FLAC support.

## [1.0.11] - 2016-10-22
### Added
- MP3 stream support.

### Fixed
- Fixed NPE on missing Content-Length in HTTP responses.

## [1.0.10] - 2016-10-22
### Added
- HTTP source for loading any URL as a track.
- Loading artist and title from MP3 tags.
- Custom executor support.

### Fixed
- Fixed track end not triggering on exception.

## [1.0.9] - 2016-10-22
### Added
- Accurate seeking and duration for VBR MP3 files.

## [1.0.8] - 2016-10-22
### Added
- Track serialization.

### Fixed
- Fixed only one thread used for loading track info.

## [1.0.7] - 2016-10-21
### Added
- Shutting down player managers and source managers.
- Cleaning up abandoned players.
- File type detection based on content.
- Track end reasons.

### Fixed
- Fixed TrackStartEvent event not called.

## [1.0.6] - 2016-10-17
### Added
- Audio output hook for experimental native UDP packet scheduling.

## [1.0.5] - 2016-10-17
### Fixed
- Fixed huge CPU load from GC monitor.
- Fixed track duration reported in microsecons.

## [1.0.4] - 2016-10-16
### Added
- GC pause monitor for logging the number of GC pauses in different duration ranges.

## [1.0.3] - 2016-10-16
### Changed
- Made null a valid argument for playTrack with identical behavior to stopTrack.

## [1.0.2] - 2016-10-15
### Added
- SoundCloud playlist support.

## [1.0.1] - 2016-10-15
### Fixed
- Fixed YouTube tracks which use dash XML and are protected with cipher.

## [1.0.0] - 2016-10-14
### Added
- Offloading audio processing to remote nodes with load balancing.

### Changed
- Refactoring of track execution.

## [0.1.9] - 2016-10-08
### Added
- Method for loading items in order.
- Opus encoding quality setting.

### Fixed
- Fixed SoundCloud tracks with underscores not working. 

## [0.1.8] - 2016-10-07
### Added
- Resampling quality setting.

## [0.1.7] - 2016-10-07
### Changed
- Track length info provided in milliseconds instead of seconds.

### Fixed
- Fixed getPosition always returning 0 when setPosition was called before start.

## [0.1.6] - 2016-10-07
### Added
- Tracks can be cloned (for replay).

## [0.1.5] - 2016-10-06
### Added
- SoundCloud support (single tracks)
- Support for special playlist types on YouTube (liked videos, favorites, mixes).
- MP3 support.

## [0.1.4] - 2016-10-05
### Changed
- YouTube track is loaded as a playlist if playlist is referenced in the URL.

## [0.1.3] - 2016-10-05
### Fixed
- Fixed broken special characters in track info when system default charset is not UTF8.

## [0.1.2] - 2016-10-04
### Added
- Severity levels for FriendlyException.
- Report YouTube errors with the proper error message and COMMON severity.

### Fixed
- Fixed smooth transition not working from volume 0.

## [0.1.1] - 2016-10-04
### Added
- Volume support with smooth transition.
- Support for nonstandard opus streams, such as mono.

### Fixed
- Fixed onTrackException not being called.

## [0.1.0] - 2016-10-02
### Added
- Initial release.
