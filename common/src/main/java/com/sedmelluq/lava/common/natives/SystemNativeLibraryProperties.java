package com.sedmelluq.lava.common.natives;

public class SystemNativeLibraryProperties implements NativeLibraryProperties {
    private final String libraryName;
    private final String propertyPrefix;

    public SystemNativeLibraryProperties(String libraryName, String propertyPrefix) {
        this.libraryName = libraryName;
        this.propertyPrefix = propertyPrefix;
    }

    @Override
    public String getLibraryPath() {
        return get("path");
    }

    @Override
    public String getLibraryDirectory() {
        return get("dir");
    }

    @Override
    public String getExtractionPath() {
        return get("extractPath");
    }

    @Override
    public String getSystemName() {
        return get("system");
    }

    @Override
    public String getArchitectureName() {
        return get("arch");
    }

    @Override
    public String getLibraryFileNamePrefix() {
        return get("libPrefix");
    }

    @Override
    public String getLibraryFileNameSuffix() {
        return get("libSuffix");
    }

    private String get(String property) {
        return System.getProperty(
            propertyPrefix + libraryName + "." + property,
            System.getProperty(propertyPrefix + property)
        );
    }
}
