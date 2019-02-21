package com.sedmelluq.lava.common.natives;

import com.sedmelluq.lava.common.natives.architecture.Architecture;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;

/**
 * Loads native libraries by name. Libraries are expected to be in classpath /natives/[arch]/[prefix]name[suffix]
 */
public class NativeLibraryLoader {
  private static final Logger log = LoggerFactory.getLogger(NativeLibraryLoader.class);

  private static final String NATIVE_RESOURCES_ROOT = "/natives/";

  private final Class<?> classLoaderSample;
  private final String libraryName;
  private final String systemNameFilter;
  private final Object lock;
  private volatile RuntimeException previousFailure;
  private volatile Boolean previousResult;

  private NativeLibraryLoader(Class<?> classLoaderSample, String libraryName, String systemNameFilter) {
    this.classLoaderSample = classLoaderSample;
    this.libraryName = libraryName;
    this.systemNameFilter = systemNameFilter;
    this.lock = new Object();
  }

  public static NativeLibraryLoader create(Class<?> classLoaderSample, String libraryName) {
    return new NativeLibraryLoader(classLoaderSample, libraryName, null);
  }

  public static NativeLibraryLoader createConditional(Class<?> classLoaderSample, String libraryName,
                                                      String systemNameFilter) {

    return new NativeLibraryLoader(classLoaderSample, libraryName, systemNameFilter);
  }

  public void load() {
    Boolean result = previousResult;

    if (result == null) {
      synchronized (lock) {
        result = previousResult;

        if (result == null) {
          loadAndRemember();
          return;
        }
      }
    }

    if (!result) {
      throw previousFailure;
    }
  }

  private void loadAndRemember() {
    log.info("Native library {}: loading with filter {}", libraryName, systemNameFilter);

    try {
      loadInternal();
      previousResult = true;
    } catch (Throwable e) {
      log.error("Native library {}: loading failed.", e);

      previousFailure = new RuntimeException(e);
      previousResult = false;
    }
  }

  private void loadInternal() {
    String explicitPath = NativeProperties.get(libraryName, "path", null);

    if (explicitPath != null) {
      log.debug("Native library {}: explicit path provided {}", libraryName, explicitPath);

      loadFromFile(Paths.get(explicitPath).toAbsolutePath());
    } else {
      Architecture architecture = detectMatchingArchitecture();

      if (architecture != null) {
        String explicitDirectory = NativeProperties.get(libraryName, "dir", null);

        if (explicitDirectory != null) {
          log.debug("Native library {}: explicit directory provided {}", libraryName, explicitDirectory);

          loadFromFile(Paths.get(explicitDirectory, architecture.formatLibraryName(libraryName)).toAbsolutePath());
        } else {
          loadFromFile(extractLibraryFromResources(architecture));
        }
      }
    }
  }

  private void loadFromFile(Path libraryFilePath) {
    log.debug("Native library {}: attempting to load library at {}", libraryName, libraryFilePath);
    System.load(libraryFilePath.toAbsolutePath().toString());
    log.info("Native library {}: successfully loaded.", libraryName);
  }

  private Path extractLibraryFromResources(Architecture architecture) {
    String resourcePath = NATIVE_RESOURCES_ROOT + architecture.formatSystemName() + "/" +
        architecture.formatLibraryName(libraryName);

    log.debug("Native library {}: trying to find from resources at {} with {} as classloader reference", libraryName,
        resourcePath, classLoaderSample.getName());

    try (InputStream libraryStream = classLoaderSample.getResourceAsStream(resourcePath)) {
      if (libraryStream == null) {
        throw new UnsatisfiedLinkError("Required library at " + resourcePath + " was not found");
      }

      Path extractedLibraryPath = prepareExtractionDirectory().resolve(architecture.formatLibraryName(libraryName));

      try (FileOutputStream fileStream = new FileOutputStream(extractedLibraryPath.toFile())) {
        IOUtils.copy(libraryStream, fileStream);
      }

      return extractedLibraryPath;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Path prepareExtractionDirectory() throws IOException {
    Path extractionDirectory = detectExtractionBaseDirectory().resolve(String.valueOf(System.currentTimeMillis()));

    if (!Files.isDirectory(extractionDirectory)) {
      log.debug("Native library {}: extraction directory {} does not exist, creating.", libraryName,
          extractionDirectory);

      try {
        createDirectoriesWithFullPermissions(extractionDirectory);
      } catch (FileAlreadyExistsException ignored) {
        // All is well
      } catch (IOException e) {
        throw new IOException("Failed to create directory for unpacked native library.", e);
      }
    } else {
      log.debug("Native library {}: extraction directory {} already exists, using.", libraryName, extractionDirectory);
    }

    return extractionDirectory;
  }

  private Path detectExtractionBaseDirectory() {
    String explicitExtractionBase = NativeProperties.get(libraryName, "extractPath", null);

    if (explicitExtractionBase != null) {
      log.debug("Native library {}: explicit extraction path provided - {}", libraryName, explicitExtractionBase);
      return Paths.get(explicitExtractionBase).toAbsolutePath();
    }

    Path path = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"), "lava-jni-natives")
        .toAbsolutePath();

    log.debug("Native library {}: detected {} as base directory for extraction.", libraryName, path);
    return path;
  }

  private Architecture detectMatchingArchitecture() {
    Architecture architecture;

    try {
      architecture = Architecture.detect(libraryName);
    } catch (IllegalArgumentException e) {
      if (systemNameFilter != null) {
        log.info("Native library {}: could not detect architecture, but system filter is {} - assuming it does " +
            "not match and skipping library.", libraryName, systemNameFilter);

        return null;
      } else {
        throw e;
      }
    }

    if (systemNameFilter != null && !systemNameFilter.equals(architecture.formatSystemName())) {
      log.debug("Native library {}: system filter {} does not match detected system {], skipping", libraryName,
          systemNameFilter, architecture.formatSystemName());
      return null;
    }

    return architecture;
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
