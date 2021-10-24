package com.sedmelluq.lava.common.natives.architecture;

public interface OperatingSystemType {
    /**
     * @return Identifier used in directory names (combined with architecture) for this OS
     */
    String identifier();

    /**
     * @return Prefix used for library file names. <code>lib</code> on most Unix flavors.
     */
    String libraryFilePrefix();

    /**
     * @return Suffix (extension) used for library file names.
     */
    String libraryFileSuffix();
}
