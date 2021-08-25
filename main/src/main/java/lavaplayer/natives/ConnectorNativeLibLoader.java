package lavaplayer.natives;

import lavaplayer.common.natives.NativeLibraryLoader;
import lavaplayer.common.natives.architecture.DefaultOperatingSystemTypes;

/**
 * Methods for loading the connector library.
 */
public class ConnectorNativeLibLoader {
    private static final NativeLibraryLoader[] loaders = new NativeLibraryLoader[]{
        NativeLibraryLoader.createFiltered(ConnectorNativeLibLoader.class, "libmpg123-0",
            it -> it.osType == DefaultOperatingSystemTypes.WINDOWS),
        NativeLibraryLoader.create(ConnectorNativeLibLoader.class, "connector")
    };

    /**
     * Loads the connector library with its dependencies for the current system
     */
    public static void loadConnectorLibrary() {
        for (NativeLibraryLoader loader : loaders) {
            loader.load();
        }
    }
}
