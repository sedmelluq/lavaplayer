package com.sedmelluq.lava.common.natives.architecture;

import com.sedmelluq.lava.common.natives.NativeProperties;

public class Architecture {
  public final ArchitectureType architectureType;
  public final OperatingSystemType osType;

  public Architecture(ArchitectureType architectureType, OperatingSystemType osType) {
    this.architectureType = architectureType;
    this.osType = osType;
  }

  public String formatSystemName() {
    if (osType.identifier() != null) {
      if (osType == DefaultOperatingSystemTypes.DARWIN) {
        return osType.identifier();
      } else {
        return osType.identifier() + "-" + architectureType.identifier();
      }
    } else {
      return architectureType.identifier();
    }
  }

  public String formatLibraryName(String libraryName) {
    return osType.libraryFilePrefix() + libraryName + osType.libraryFileSuffix();
  }

  public static Architecture detect(String libraryName) {
    String systemName = NativeProperties.get(libraryName, "system", null);

    if (systemName != null) {
      return new Architecture(
          () -> systemName,
          new UnknownOperatingSystem(
              NativeProperties.get(libraryName, "libPrefix", "lib"),
              NativeProperties.get(libraryName, "libSuffix", ".so")
          )
      );
    }

    OperatingSystemType osType = DefaultOperatingSystemTypes.detect();

    String explicitArchitecture = NativeProperties.get(libraryName, "arch", null);
    ArchitectureType architectureType = explicitArchitecture != null ? () -> explicitArchitecture :
        DefaultArchitectureTypes.detect();

    return new Architecture(architectureType, osType);
  }

  private static class UnknownOperatingSystem implements OperatingSystemType {
    private final String libraryFilePrefix;
    private final String libraryFileSuffix;

    private UnknownOperatingSystem(String libraryFilePrefix, String libraryFileSuffix) {
      this.libraryFilePrefix = libraryFilePrefix;
      this.libraryFileSuffix = libraryFileSuffix;
    }

    @Override
    public String identifier() {
      return null;
    }

    @Override
    public String libraryFilePrefix() {
      return libraryFilePrefix;
    }

    @Override
    public String libraryFileSuffix() {
      return libraryFileSuffix;
    }
  }
}
