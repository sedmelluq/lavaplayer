package com.sedmelluq.lava.common.natives;

import com.sedmelluq.lava.common.natives.architecture.SystemType;

public interface NativeLibraryProperties {
    /**
     * @return Explicit filesystem path for the library. If this is set, this is loaded directly and no resource
     * extraction and/or system name detection is performed. If this returns <code>null</code>, library directory
     * is checked next.
     */
    String getLibraryPath();

    /**
     * @return Explicit directory containing the native library. The specified directory must contain the system name
     * directories, thus the library to be loaded is actually located at
     * <code>directory/{systemName}/{libPrefix}{libName}{libSuffix}</code>. If this returns <code>null</code>,
     * then {@link NativeLibraryBinaryProvider#getLibraryStream(SystemType, String)} is called to obtain the
     * stream to the library file, which is then written to disk for loading.
     */
    String getLibraryDirectory();

    /**
     * @return Base directory where to write the library if it is obtained through
     * {@link NativeLibraryBinaryProvider#getLibraryStream(SystemType, String)}. The library file itself will
     * actually be written to a subdirectory with a randomly generated name. The specified directory does not
     * have to exist, but in that case the current process must have privileges to create it. If this returns
     * <code>null</code>, then <code>{tmpDir}/lava-jni-natives</code> is used.
     */
    String getExtractionPath();

    /**
     * @return System name. If this is set, no operating system or architecture detection is performed.
     */
    String getSystemName();

    /**
     * @return Library file name prefix to use. Only used when {@link #getSystemName()} is provided.
     */
    String getLibraryFileNamePrefix();

    /**
     * @return Library file name suffix to use. Only used when {@link #getSystemName()} is provided.
     */
    String getLibraryFileNameSuffix();

    /**
     * @return Architecture name to use. If this is set, operating system detection is still performed.
     */
    String getArchitectureName();
}
