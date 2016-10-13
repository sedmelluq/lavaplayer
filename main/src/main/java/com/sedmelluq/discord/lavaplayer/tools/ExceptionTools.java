package com.sedmelluq.discord.lavaplayer.tools;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import org.slf4j.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains common helper methods for dealing with exceptions.
 */
public class ExceptionTools {
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

    while(next != null) {
      causes.add(next);
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
    }

    output.writeBoolean(false);
    output.writeInt(exception.severity.ordinal());
    output.writeUTF(exception.getMessage());
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
    }

    return new FriendlyException(input.readUTF(), Severity.class.getEnumConstants()[input.readInt()], cause);
  }
}
