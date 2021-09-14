package lavaplayer.source.soundcloud

import lavaplayer.tools.io.HttpInterface
import java.io.IOException

interface SoundCloudHtmlDataLoader {
    @Throws(IOException::class)
    fun load(httpInterface: HttpInterface, url: String): SoundCloudRootDataModel?
}
