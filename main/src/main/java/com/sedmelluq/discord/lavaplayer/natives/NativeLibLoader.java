package com.sedmelluq.discord.lavaplayer.natives;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads native libraries by name. Libraries are expected to be in classpath /natives/[arch]/[prefix]name[suffix]
 */
public class NativeLibLoader {
  private static final Set<String> loadedLibraries;
  private static final String systemType;
  private static final String libraryPrefix;
  private static final String librarySuffix;

  static {
    String osName = System.getProperty("os.name");
    String bits = System.getProperty("sun.arch.data.model");

    loadedLibraries = new HashSet<>();

    if (osName.startsWith("Linux")) {
      systemType = "64".equals(bits) ? "linux-x86-64" : "linux-x86";
      libraryPrefix = "lib";
      librarySuffix = ".so";
    } else if ((osName.startsWith("Mac") || osName.startsWith("Darwin")) && "64".equals(bits)) {
      systemType = "darwin";
      libraryPrefix = "lib";
      librarySuffix = ".dylib";
    } else if (osName.startsWith("Windows") && !osName.startsWith("Windows CE")) {
      systemType = "64".equals(bits) ? "win-x86-64" : "win-x86";
      libraryPrefix = "";
      librarySuffix = ".dll";
    } else {
      throw new IllegalStateException("Native libraries not supported on this platform.");
    }
  }

  /**
   * @param name Name of the library
   * @throws LinkageError When loading the library fails
   */
  public static void load(String name) {
    synchronized (loadedLibraries) {
      if (!loadedLibraries.contains(name)) {
        try {
          System.load(extractLibrary(name).toFile().getAbsolutePath());

          loadedLibraries.add(name);
        } catch (Exception e) {
          throw new LinkageError("Failed to load native library due to an exception.", e);
        }
      }
    }
  }

  private static Path extractLibrary(String name) throws IOException {
    String path = "/natives/" + systemType + "/" + libraryPrefix + name + librarySuffix;
    Path extractedLibrary;

    try (InputStream libraryStream = NativeLibLoader.class.getResourceAsStream(path)) {
      if (libraryStream == null) {
        throw new UnsatisfiedLinkError("Required library at " + path + " was not found");
      }

      File temporaryContainer = new File(System.getProperty("java.io.tmpdir"), "lava-jni-natives");
      if (!temporaryContainer.mkdirs() && !temporaryContainer.exists()) {
        throw new IOException("Failed to create directory for unpacked native library.");
      }

      extractedLibrary = Files.createTempFile(temporaryContainer.toPath(), libraryPrefix + name, librarySuffix);
      try (FileOutputStream fileStream = new FileOutputStream(extractedLibrary.toFile())) {
        IOUtils.copy(libraryStream, fileStream);
      }
    }

    return extractedLibrary;
  }
}
