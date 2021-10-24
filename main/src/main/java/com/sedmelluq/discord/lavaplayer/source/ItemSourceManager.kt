package com.sedmelluq.discord.lavaplayer.source

import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.loader.LoaderState
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Manager for a source of audio items.
 */
interface ItemSourceManager {
    /**
     * Every source manager implementation should have its unique name as it is used to determine which source manager
     * should be able to decode a serialized audio track.
     *
     * @return The name of this source manager
     */
    val sourceName: String

    /**
     * Returns an audio track for the input string. It should return null if it can immediately detect that there is no
     * track for this identifier for this source. If checking that requires more expensive operations, then it should
     * return a track instance and check that in InternalAudioTrack#loadTrackInfo.
     *
     * @param state      The state of the loader that's requesting {@param reference} to be loaded???
     * @param reference  The reference with the identifier which the source manager should find the track with
     * @return The loaded item or null on unrecognized identifier
     */
    suspend fun loadItem(state: LoaderState, reference: AudioReference): AudioItem?

    /**
     * Returns whether the specified track can be encoded. The argument is always a track created by this manager. Being
     * encodable also means that it must be possible to play this track on a different node, so it should not depend on
     * any resources that are only available on the current system.
     *
     * @param track The track to check
     * @return True if it is encodable
     */
    fun isTrackEncodable(track: AudioTrack): Boolean

    /**
     * Encodes an audio track into the specified output. The contents of AudioTrackInfo do not have to be included since
     * they are written to the output already before this call. This will only be called for tracks which were loaded by
     * this source manager and for which isEncodable() returns true.
     *
     * @param track  The track to encode
     * @param output Output where to write the decoded format to
     * @throws IOException On write error.
     */
    @Throws(IOException::class)
    fun encodeTrack(track: AudioTrack, output: DataOutput) {
    }

    /**
     * Decodes an audio track from the encoded format encoded with encodeTrack().
     *
     * @param trackInfo The track info
     * @param input     The input where to read the bytes of the encoded format
     * @return The decoded track
     * @throws IOException On read error.
     */
    @Throws(IOException::class)
    fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack?

    /**
     * Shut down the source manager, freeing all associated resources and threads. A source manager is not responsible for
     * terminating the tracks that it has created.
     */
    fun shutdown() {}
}
