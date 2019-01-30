package com.sedmelluq.lava.common.natives;

import java.util.Map;
import java.util.HashMap;

public class Architecture {
  private static final String OS_DARWIN = "darwin";
  private static final String OS_LINUX = "linux";
  private static final String OS_OSX = "osx";
  private static final String OS_SOLARIS = "solaris";
  private static final String OS_WINDOWS = "win";

  private static final String ARCH_ARM = "arm";
  private static final String ARCH_ARM_HF = "armhf";
  private static final String ARCH_ARMv8_32 = "aarch32";
  private static final String ARCH_ARMv8_64 = "aarch64";
  private static final String ARCH_MIPS_32 = "mips";
  private static final String ARCH_MIPS_32_LE = "mipsel";
  private static final String ARCH_MIPS_64 = "mips64";
  private static final String ARCH_MIPS_64_LE = "mips64el";
  private static final String ARCH_PPC_32 = "powerpc";
  private static final String ARCH_PPC_32_LE = "powerpcle";
  private static final String ARCH_PPC_64 = "ppc64";
  private static final String ARCH_PPC_64_LE = "ppc64le";
  private static final String ARCH_X86_32 = "x86";
  private static final String ARCH_X86_64 = "x86-64";

  private static final Map<String, String> abiAliasMap = new HashMap<String, String>() {{
    put("arm", ARCH_ARM);
    put("armeabi", ARCH_ARM);
    put("armv7b", ARCH_ARM);
    put("armv7l", ARCH_ARM);

    put("armeabihf", ARCH_ARM_HF);
    put("armeabi-v7a", ARCH_ARM_HF);

    put("armv8b", ARCH_ARMv8_32);
    put("armv8l", ARCH_ARMv8_32);

    put("arm64", ARCH_ARMv8_64);
    put("aarch64", ARCH_ARMv8_64);
    put("aarch64_be", ARCH_ARMv8_64);
    put("arm64-v8a", ARCH_ARMv8_64);

    put("mips", ARCH_MIPS_32);
    put("mipsel", ARCH_MIPS_32_LE);
    put("mipsle", ARCH_MIPS_32_LE);
    put("mips64", ARCH_MIPS_64);
    put("mips64el", ARCH_MIPS_64_LE);
    put("mips64le", ARCH_MIPS_64_LE);

    put("ppc", ARCH_PPC_32);
    put("powerpc", ARCH_PPC_32);
    put("ppcel", ARCH_PPC_32_LE);
    put("ppcle", ARCH_PPC_32_LE);
    put("ppc64", ARCH_PPC_64);
    put("ppc64el", ARCH_PPC_64_LE);
    put("ppc64le", ARCH_PPC_64_LE);

    put("x86", ARCH_X86_32);
    put("i386", ARCH_X86_32);
    put("i486", ARCH_X86_32);
    put("i586", ARCH_X86_32);
    put("i686", ARCH_X86_32);

    put("x86_64", ARCH_X86_64);
    put("amd64", ARCH_X86_64);
  }};

  public final String systemType;
  public final String libraryPrefix;
  public final String librarySuffix;

  private static String formatSystemName(String osName, String archName) {
    if (osName == OS_OSX && archName == ARCH_X86_64) {
      return OS_DARWIN;
    } else {
      return osName + "-" + archName;
    }
  }

  public String formatLibraryName(String libName) {
    return this.libraryPrefix + libName + this.librarySuffix;
  }

  public static Architecture detectArchitecture() throws IllegalStateException, IllegalArgumentException {
    String systemType = System.getProperty("lavaplayer.native.system");
    String libPrefix, libSuffix;

    if (systemType == null) {
      final String _os_arch = System.getProperty("os.arch");
      final String _os_name = System.getProperty("os.name");
      String osName;
      String archName = System.getProperty("lavaplayer.native.arch", abiAliasMap.get(_os_arch));

      if (archName == null) {
        throw new IllegalArgumentException("Unknown architecture: " + _os_arch);
      }

      if (_os_name.startsWith("Windows")) {
        osName = OS_WINDOWS;
        libPrefix = "";
        libSuffix = ".dll";
      } else if (_os_name.startsWith("Mac OS X")) {
        osName = OS_OSX;
        libPrefix = "lib";
        libSuffix = ".dylib";
      } else if (_os_name.startsWith("Solaris")) {
        osName = OS_SOLARIS;
        libPrefix = "lib";
        libSuffix = ".so";
      } else if (_os_name.toLowerCase().startsWith("linux")) {
        osName = OS_LINUX;
        libPrefix = "lib";
        libSuffix = ".so";
      } else {
        throw new IllegalArgumentException("Unknown operating system: " + _os_name);
      }

      systemType = formatSystemName(osName, archName);
    } else {
      // Assume UNIX library filename conventions
      libPrefix = System.getProperty("lavaplayer.native.libPrefix", "lib");
      libSuffix = System.getProperty("lavaplayer.native.libSuffix", ".so");
    }

    return new Architecture(systemType, libPrefix, libSuffix);
  }

  private Architecture(String systemType, String libraryPrefix, String librarySuffix) {
    this.systemType = systemType;
    this.libraryPrefix = libraryPrefix;
    this.librarySuffix = librarySuffix;
  }
}
