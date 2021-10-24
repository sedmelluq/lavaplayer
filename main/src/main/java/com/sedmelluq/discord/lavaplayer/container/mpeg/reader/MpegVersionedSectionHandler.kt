package com.sedmelluq.discord.lavaplayer.container.mpeg.reader

import java.io.IOException

/**
 * Handles one MPEG section which has version info
 */
interface MpegVersionedSectionHandler {
    /**
     * @param child The versioned section
     * @throws IOException On read error
     */
    @Throws(IOException::class)
    fun handle(child: MpegVersionedSectionInfo)
}
