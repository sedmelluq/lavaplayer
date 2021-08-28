package lavaplayer.tools

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

/**
 * Contains constants with metadata about the library.
 */
object PlayerLibrary {
    /**
     * The currently loaded version of the library.
     */
    @JvmField
    val VERSION = readVersion()

    private fun readVersion(): String {
        val stream = PlayerLibrary::class.java.getResourceAsStream("version.txt")
        try {
            if (stream != null) {
                return IOUtils.toString(stream, StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            // Something went wrong.
        }
        return "UNKNOWN"
    }
}
