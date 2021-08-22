package lavaplayer.common.natives;

import lavaplayer.common.natives.architecture.SystemType;

import java.io.InputStream;

public interface NativeLibraryBinaryProvider {
    /**
     * @param systemType  Detected system type.
     * @param libraryName Name of the library to load.
     * @return Stream to the library binary. <code>null</code> causes failure.
     */
    InputStream getLibraryStream(SystemType systemType, String libraryName);
}
