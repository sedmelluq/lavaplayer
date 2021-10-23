package lavaplayer.natives

import lavaplayer.common.natives.NativeLibraryLoader
import lavaplayer.common.natives.architecture.DefaultOperatingSystemTypes

/**
 * Methods for loading the connector library.
 */
object ConnectorNativeLibLoader {
    private val loaders = listOf(
        NativeLibraryLoader.createFiltered(ConnectorNativeLibLoader::class.java, "libmpg123-0") {
            it.osType === DefaultOperatingSystemTypes.WINDOWS
        },
        NativeLibraryLoader.create(ConnectorNativeLibLoader::class.java, "connector")
    )

    /**
     * Loads the connector library with its dependencies for the current system
     */
    fun loadConnectorLibrary() {
        for (loader in loaders) {
            loader.load()
        }
    }
}
