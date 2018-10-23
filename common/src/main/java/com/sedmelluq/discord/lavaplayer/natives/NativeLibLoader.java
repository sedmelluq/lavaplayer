package com.sedmelluq.discord.lavaplayer.natives;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;

/**
 * Loads native libraries by name. Libraries are expected to be in classpath /natives/[arch]/[prefix]name[suffix]
 */
public class NativeLibLoader {
  public static final String OS_DARWIN = "darwin";
  public static final String OS_LINUX = "linux";
  public static final String OS_OSX = "osx";
  public static final String OS_SOLARIS = "solaris";
  public static final String OS_WINDOWS = "win";

  public static final String ARCH_ARM = "arm";
  public static final String ARCH_ARM_HF = "armhf";
  public static final String ARCH_ARMv8_32 = "aarch32";
  public static final String ARCH_ARMv8_64 = "aarch64";
  public static final String ARCH_MIPS_32 = "mips";
  public static final String ARCH_MIPS_32_LE = "mipsel";
  public static final String ARCH_MIPS_64 = "mips64";
  public static final String ARCH_MIPS_64_LE = "mips64el";
  public static final String ARCH_PPC_32 = "powerpc";
  public static final String ARCH_PPC_32_LE = "powerpcle";
  public static final String ARCH_PPC_64 = "ppc64";
  public static final String ARCH_PPC_64_LE = "ppc64le";
  public static final String ARCH_X86_32 = "x86";
  public static final String ARCH_X86_64 = "x86-64";

  public static final Map<String, String> archMap = new HashMap<String, String>() {{
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

  private static final Set<String> loadedLibraries = new HashSet<>();
  private static final String tempDir = String.valueOf(System.currentTimeMillis());
  private static final Architecture architecture = Architecture.detectArchitecture();

  /**
   * Load a library only if the current system type matches the specified one
   * @param name Name of the library
   * @param systemTypeFilter System type that should match current
   * @throws LinkageError When loading the library fails
   */
  public static void load(String name, String systemTypeFilter) {
    if (architecture.systemType.equals(systemTypeFilter)) {
      load(NativeLibLoader.class, name);
    }
  }

  /**
   * @param name Name of the library
   * @throws LinkageError When loading the library fails
   */
  public static void load(String name) {
    load(NativeLibLoader.class, name);
  }

  /**
   * Load a library only if the current system type matches the specified one
   * @param resourceBase The class to use for obtaining the resource stream
   * @param name Name of the library
   * @param systemTypeFilter System type that should match current
   * @throws LinkageError When loading the library fails
   */
  public static void load(Class<?> resourceBase, String name, String systemTypeFilter) throws LinkageError {
    if (architecture.systemType.equals(systemTypeFilter)) {
      load(resourceBase, name);
    }
  }

  /**
   * @param resourceBase The class to use for obtaining the resource stream
   * @param name Name of the library
   * @throws LinkageError When loading the library fails
   */
  public static void load(Class<?> resourceBase, String name) throws LinkageError, UnsatisfiedLinkError {
    synchronized (loadedLibraries) {
      if (!loadedLibraries.contains(name)) {
        String libPath = System.getProperty("lavaplayer." + name + ".path");
        String libRoot = System.getProperty("lavaplayer.native.dir");

        try {
          if (libRoot != null) {
            File archRoot = new File(libRoot, architecture.systemType);
            libPath = new File(archRoot, architecture.formatLibraryName(name)).getAbsolutePath();
          } else if (libPath == null) {
            libPath = extractLibrary(resourceBase, name).toFile().getAbsolutePath();
          }
          System.load(libPath);
          loadedLibraries.add(name);
        } catch (Exception e) {
          throw new LinkageError("Failed to load native library due to an exception.", e);
        }
      }
    }
  }

  private static Path extractLibrary(Class<?> resourceBase, String name) throws IOException {
    String path = "/natives/" + architecture.systemType + "/" + architecture.formatLibraryName(name);
    Path extractedLibrary;

    try (InputStream libraryStream = resourceBase.getResourceAsStream(path)) {
      if (libraryStream == null) {
        throw new UnsatisfiedLinkError("Required library at " + path + " was not found");
      }

      File temporaryContainer = new File(System.getProperty("java.io.tmpdir"), "lava-jni-natives/" + tempDir);

      if (!temporaryContainer.exists()) {
        try {
          createDirectoriesWithFullPermissions(temporaryContainer.toPath());
        } catch (FileAlreadyExistsException ignored) {
          // All is well
        } catch (IOException e) {
          throw new IOException("Failed to create directory for unpacked native library.", e);
        }
      }

      extractedLibrary = new File(temporaryContainer, architecture.formatLibraryName(name)).toPath();
      try (FileOutputStream fileStream = new FileOutputStream(extractedLibrary.toFile())) {
        IOUtils.copy(libraryStream, fileStream);
      }
    }

    return extractedLibrary;
  }

  private static void createDirectoriesWithFullPermissions(Path path) throws IOException {
    boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    if (!isPosix) {
      Files.createDirectories(path);
    } else {
      Files.createDirectories(path, asFileAttribute(fromString("rwxrwxrwx")));
    }
  }

  private static class Architecture {
    private final String osName;
    private final String archName;
    private final String libraryPrefix;
    private final String librarySuffix;
    private final String systemType;

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
      final String _os_arch = System.getProperty("os.arch");
      final String _os_name = System.getProperty("os.name");
      String archName = System.getProperty("lavaplayer.native.arch", archMap.get(_os_arch));
      String osName, libPrefix, libSuffix;

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

      return new Architecture(osName, archName, libPrefix, libSuffix);
    }

    private Architecture(String osName, String archName, String libraryPrefix, String librarySuffix) {
      this.osName = osName;
      this.archName = archName;
      this.libraryPrefix = libraryPrefix;
      this.librarySuffix = librarySuffix;

      this.systemType = System.getProperty("lavaplayer.native.system", formatSystemName(osName, archName));
    }
  }
}
