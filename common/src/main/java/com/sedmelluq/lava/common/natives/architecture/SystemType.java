package com.sedmelluq.lava.common.natives.architecture;

import com.sedmelluq.lava.common.natives.NativeLibraryProperties;
import java.util.Optional;

public class SystemType {
  public final ArchitectureType architectureType;
  public final OperatingSystemType osType;

  public SystemType(ArchitectureType architectureType, OperatingSystemType osType) {
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

  public static SystemType detect(NativeLibraryProperties properties) {
    String systemName = properties.getSystemName();

    if (systemName != null) {
      return new SystemType(
          () -> systemName,
          new UnknownOperatingSystem(
              Optional.ofNullable(properties.getLibraryFileNamePrefix()).orElse("lib"),
              Optional.ofNullable(properties.getLibraryFileNameSuffix()).orElse(".so")
          )
      );
    }

    OperatingSystemType osType = DefaultOperatingSystemTypes.detect();

    String explicitArchitecture = properties.getArchitectureName();
    ArchitectureType architectureType = explicitArchitecture != null ? () -> explicitArchitecture :
        DefaultArchitectureTypes.detect();

    return new SystemType(architectureType, osType);
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
