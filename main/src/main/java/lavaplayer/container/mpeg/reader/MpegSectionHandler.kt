package lavaplayer.container.mpeg.reader

import java.io.IOException

/**
 * Handles one MPEG section which has no version info
 */
interface MpegSectionHandler {
    /**
     * @param child The section
     * @throws IOException On read error
     */
    @Throws(IOException::class)
    fun handle(child: MpegSectionInfo)
}
