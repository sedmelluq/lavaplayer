package lavaplayer.source.soundcloud

import lavaplayer.container.playlists.HlsStreamSegment
import lavaplayer.container.playlists.HlsStreamSegmentParser
import lavaplayer.source.soundcloud.SoundCloudHelper.loadPlaybackUrl
import lavaplayer.tools.http.HttpStreamTools.streamContent
import lavaplayer.tools.io.ChainedInputStream
import lavaplayer.tools.io.HttpInterface
import lavaplayer.tools.io.NonSeekableInputStream
import lavaplayer.tools.io.SeekableInputStream
import lavaplayer.track.AudioTrackInfo
import lavaplayer.track.DelegatedAudioTrack
import lavaplayer.track.playback.LocalAudioTrackExecutor
import mu.KotlinLogging
import org.apache.http.client.methods.HttpGet
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class SoundCloudM3uAudioTrack(
    trackInfo: AudioTrackInfo,
    private val httpInterface: HttpInterface,
    private val m3uInfo: SoundCloudM3uInfo
) : DelegatedAudioTrack(trackInfo) {
    companion object {
        private val log = KotlinLogging.logger { }
        private val SEGMENT_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(10)
    }

    @Throws(Exception::class)
    override fun process(executor: LocalAudioTrackExecutor) {
        createSegmentTracker().use { segmentTracker ->
            segmentTracker.decoder!!.prepareStream(true)
            executor.executeProcessingLoop({
                segmentTracker.decoder!!.playStream(
                    executor.processingContext,
                    segmentTracker.streamStartPosition,
                    segmentTracker.desiredPosition
                )
            }, { timecode: Long -> segmentTracker.seekToTimecode(timecode) }, true)
        }
    }

    @Throws(IOException::class)
    private fun loadSegments(): MutableList<HlsStreamSegment> {
        val playbackUrl = loadPlaybackUrl(httpInterface, m3uInfo.lookupUrl)
        return HlsStreamSegmentParser.parseFromUrl(httpInterface, playbackUrl)
    }

    @Throws(IOException::class)
    private fun createSegmentTracker(): SegmentTracker {
        val initialSegments = loadSegments()
        val tracker = SegmentTracker(initialSegments)
        tracker.setupDecoder(m3uInfo.decoderFactory)

        return tracker
    }

    private inner class SegmentTracker(private val segments: MutableList<HlsStreamSegment>) : AutoCloseable {
        var desiredPosition: Long = 0
        var streamStartPosition: Long = 0
        var decoder: SoundCloudSegmentDecoder? = null

        private var lastUpdate: Long
        private var segmentIndex = 0
        private val nextStream: InputStream?
            get() {
                val segment = nextSegment
                    ?: return null

                return streamContent(httpInterface, HttpGet(segment.url))
            }
        private val nextSegment: HlsStreamSegment?
            get() {
                val current = segmentIndex++
                return if (current < segments.size) {
                    checkSegmentListUpdate()
                    segments[current]
                } else {
                    null
                }
            }

        init {
            lastUpdate = System.currentTimeMillis()
        }

        fun setupDecoder(factory: SoundCloudSegmentDecoder.Factory) {
            decoder = factory.create { createChainedStream() }
        }

        @Throws(IOException::class)
        fun seekToTimecode(timecode: Long) {
            var segmentTimecode: Long = 0
            for (i in segments.indices) {
                val duration = segments[i].duration ?: break
                val nextTimecode = segmentTimecode + duration
                if (timecode in segmentTimecode until nextTimecode) {
                    seekToSegment(i, timecode, segmentTimecode)
                    return
                }

                segmentTimecode = nextTimecode
            }

            seekToEnd()
        }

        @Throws(Exception::class)
        override fun close() {
            decoder!!.resetStream()
        }

        @Throws(IOException::class)
        private fun seekToSegment(index: Int, requestedTimecode: Long, segmentTimecode: Long) {
            decoder!!.resetStream()
            segmentIndex = index
            desiredPosition = requestedTimecode
            streamStartPosition = segmentTimecode
            decoder!!.prepareStream(streamStartPosition == 0L)
        }

        @Throws(IOException::class)
        private fun seekToEnd() {
            decoder!!.resetStream()
            segmentIndex = segments.size
        }

        private fun updateSegmentList() {
            try {
                val newSegments: List<HlsStreamSegment> = loadSegments()
                if (newSegments.size != segments.size) {
                    log.error { "For ${info.identifier}, received different number of segments on update, skipping." }
                    return
                }

                for (i in segments.indices) {
                    if (newSegments[i].duration != segments[i].duration) {
                        log.error { "For ${info.identifier}, segment $i has different length than previously on update." }
                        return
                    }
                }

                for (i in segments.indices) {
                    segments[i] = newSegments[i]
                }
            } catch (e: Exception) {
                log.error(e) { "For ${info.identifier}, failed to update segment list, skipping." }
            }
        }

        private fun checkSegmentListUpdate() {
            val now = System.currentTimeMillis()
            val delta = now - lastUpdate
            if (delta > SEGMENT_UPDATE_INTERVAL) {
                log.debug { "For ${info.identifier}, ${delta}ms has passed since last segment update, updating" }
                updateSegmentList()
                lastUpdate = now
            }
        }

        private fun createChainedStream(): SeekableInputStream {
            return NonSeekableInputStream(ChainedInputStream { nextStream })
        }
    }
}
