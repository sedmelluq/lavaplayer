package com.sedmelluq.discord.lavaplayer.container.mpeg.reader

/**
 * Stop checker which is called before and after parsing each section in an MP4 file to check if parsing should be
 * stopped.
 */
interface MpegParseStopChecker {
    /**
     * @param child Section before or after which this is called.
     * @param start Whether this is called before (true) or after (false).
     * @return True to stop, false to continue.
     */
    fun check(child: MpegSectionInfo, start: Boolean): Boolean
}
