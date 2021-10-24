package com.sedmelluq.discord.lavaplayer.container.mpeg.reader

/**
 * Information for one MP4 section (aka box)
 *
 * @param offset Absolute offset of the section
 * @param length Length of the section
 * @param type   Type (fourCC) of the section
 */
open class MpegSectionInfo(
    /**
     * Absolute offset of the section
     */
    @JvmField val offset: Long,
    /**
     * Length of the section
     */
    @JvmField val length: Long,
    /**
     * Type (fourCC) of the section
     */
    @JvmField val type: String
)
