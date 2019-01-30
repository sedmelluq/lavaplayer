package com.sedmelluq.lava.common.natives;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;

/**
 * Loads native libraries by name. Libraries are expected to be in classpath /natives/[arch]/[prefix]name[suffix]
 */
public class NativeLibLoader {
  private static final Set<String> loadedLibraries = new HashSet<>();
  private static File extractionDirectory = new File(System.getProperty("java.io.tmpdir"), "lava-jni-natives/" + String.valueOf(System.currentTimeMillis()));
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
          } else {
            libPath = new File(libPath).getAbsolutePath();
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

      if (!extractionDirectory.exists()) {
        try {
          createDirectoriesWithFullPermissions(extractionDirectory.toPath());
        } catch (FileAlreadyExistsException ignored) {
          // All is well
        } catch (IOException e) {
          throw new IOException("Failed to create directory for unpacked native library.", e);
        }
      }

      extractedLibrary = new File(extractionDirectory, architecture.formatLibraryName(name)).toPath();
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
}
