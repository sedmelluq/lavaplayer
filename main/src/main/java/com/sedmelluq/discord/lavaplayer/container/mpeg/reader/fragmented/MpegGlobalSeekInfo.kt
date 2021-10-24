package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented

/**
 * Describes the seek info for a fragmented MP4 file
 *
 * @param timescale  The value of the internal timecodes that corresponds to one second
 * @param baseOffset The file offset of the first segment
 * @param entries    Size and duration information for each segment
 */
class MpegGlobalSeekInfo(
    /**
     * The value of the internal timecodes that corresponds to one second
     */
    @JvmField val timescale: Int,
    baseOffset: Long,
    /**
     * Size and duration information for each segment
     */
    @JvmField val entries: Array<MpegSegmentEntry>
) {
    /**
     * Absolute timecode offset of each segment
     */
    @JvmField
    val timeOffsets: LongArray = LongArray(entries.size)

    /**
     * Absolute file offset of each segment
     */
    @JvmField
    val fileOffsets: LongArray = LongArray(entries.size)

    init {
        fileOffsets[0] = baseOffset
        for (i in 1 until entries.size) {
            timeOffsets[i] = timeOffsets[i - 1] + entries[i - 1].duration
            fileOffsets[i] = fileOffsets[i - 1] + entries[i - 1].size
        }
    }
}
