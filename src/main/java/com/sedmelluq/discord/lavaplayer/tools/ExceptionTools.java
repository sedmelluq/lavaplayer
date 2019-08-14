package com.sedmelluq.discord.lavaplayer.tools;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains common helper methods for dealing with exceptions.
 */
public class ExceptionTools {
  private static final Logger log = LoggerFactory.getLogger(ExceptionTools.class);

  /**
   * Sometimes it is necessary to catch Throwable instances for logging or reporting purposes. However, unless for
   * specific known cases, Error instances should not be blocked from propagating, so rethrow them.
   *
   * @param throwable The Throwable to check, it is rethrown if it is an Error
   */
  public static void rethrowErrors(Throwable throwable) {
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
  }

  /**
   * If the exception is not a FriendlyException, wrap with a FriendlyException with the given message
   *
   * @param message Message of the new FriendlyException if needed
   * @param severity Severity of the new FriendlyException
   * @param throwable The exception to potentially wrap
   * @return Original or wrapped exception
   */
  public static FriendlyException wrapUnfriendlyExceptions(String message, Severity severity, Throwable throwable) {
    if (throwable instanceof FriendlyException) {
      return (FriendlyException) throwable;
    } else {
      return new FriendlyException(message, severity, throwable);
    }
  }

  /**
   * If the exception is not a FriendlyException, wrap with a RuntimeException
   *
   * @param throwable The exception to potentially wrap
   * @return Original or wrapped exception
   */
  public static RuntimeException wrapUnfriendlyExceptions(Throwable throwable) {
    if (throwable instanceof FriendlyException) {
      return (FriendlyException) throwable;
    } else {
      return new RuntimeException(throwable);
    }
  }

  /**
   * Finds the first exception which is an instance of the specified class from the throwable cause chain.
   *
   * @param throwable Throwable to scan.
   * @param klass The throwable class to scan for.
   * @param <T> The throwable class to scan for.
   * @return The first exception in the cause chain (including itself) which is an instance of the specified class.
   */
  public static <T extends Throwable> T findDeepException(Throwable throwable, Class<T> klass) {
    while (throwable != null) {
      if (klass.isAssignableFrom(throwable.getClass())) {
        return (T) throwable;
      }

      throwable = throwable.getCause();
    }

    return null;
  }

  /**
   * Makes sure thread is set to interrupted state when the throwable is an InterruptedException
   * @param throwable Throwable to check
   */
  public static void keepInterrupted(Throwable throwable) {
    if (throwable instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Log a FriendlyException appropriately according to its severity.
   * @param log Logger instance to log it to
   * @param exception The exception itself
   * @param context An object that is included in the log
   */
  public static void log(Logger log, FriendlyException exception, Object context) {
    switch (exception.severity) {
      case COMMON:
        log.debug("Common failure for {}: {}", context, exception.getMessage());
        break;
      case SUSPICIOUS:
        log.warn("Suspicious exception for {}", context, exception);
        break;
      case FAULT:
      default:
        log.error("Error in {}", context, exception);
        break;
    }
  }

  /**
   * Encode an exception to an output stream
   * @param output Data output
   * @param exception Exception to encode
   * @throws IOException On IO error
   */
  public static void encodeException(DataOutput output, FriendlyException exception) throws IOException {
    List<Throwable> causes = new ArrayList<>();
    Throwable next = exception.getCause();

    while (next != null) {
      causes.add(next);
      next = next.getCause();
    }

    for (int i = causes.size() - 1; i >= 0; i--) {
      Throwable cause = causes.get(i);
      output.writeBoolean(true);

      String message;

      if (cause instanceof DecodedException) {
        output.writeUTF(((DecodedException) cause).className);
        message = ((DecodedException) cause).originalMessage;
      } else {
        output.writeUTF(cause.getClass().getName());
        message = cause.getMessage();
      }

      output.writeBoolean(message != null);
      if (message != null) {
        output.writeUTF(message);
      }

      encodeStackTrace(output, cause);
    }

    output.writeBoolean(false);
    output.writeUTF(exception.getMessage());
    output.writeInt(exception.severity.ordinal());

    encodeStackTrace(output, exception);
  }

  /**
   * Closes the specified closeable object. In case that throws an error, logs the error with WARN level, but does not
   * rethrow.
   *
   * @param closeable Object to close.
   */
  public static void closeWithWarnings(AutoCloseable closeable) {
    try {
      closeable.close();
    } catch (Exception e) {
      log.warn("Failed to close.", e);
    }
  }

  private static void encodeStackTrace(DataOutput output, Throwable throwable) throws IOException {
    StackTraceElement[] trace = throwable.getStackTrace();
    output.writeInt(trace.length);

    for (StackTraceElement element : trace) {
      output.writeUTF(element.getClassName());
      output.writeUTF(element.getMethodName());

      String fileName = element.getFileName();
      output.writeBoolean(fileName != null);
      if (fileName != null) {
        output.writeUTF(fileName);
      }
      output.writeInt(element.getLineNumber());
    }
  }

  /**
   * Decode an exception from an input stream
   * @param input Data input
   * @return Decoded exception
   * @throws IOException On IO error
   */
  public static FriendlyException decodeException(DataInput input) throws IOException {
    DecodedException cause = null;

    while (input.readBoolean()) {
      cause = new DecodedException(input.readUTF(), input.readBoolean() ? input.readUTF() : null, cause);
      cause.setStackTrace(decodeStackTrace(input));
    }

    FriendlyException exception = new FriendlyException(input.readUTF(), Severity.class.getEnumConstants()[input.readInt()], cause);
    exception.setStackTrace(decodeStackTrace(input));
    return exception;
  }

  private static StackTraceElement[] decodeStackTrace(DataInput input) throws IOException {
    StackTraceElement[] trace = new StackTraceElement[input.readInt()];

    for (int i = 0; i < trace.length; i++) {
      trace[i] = new StackTraceElement(input.readUTF(), input.readUTF(), input.readBoolean() ? input.readUTF() : null, input.readInt());
    }

    return trace;
  }
}
