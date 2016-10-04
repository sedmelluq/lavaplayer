package com.sedmelluq.discord.lavaplayer.tools;

import org.slf4j.Logger;

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
  public static FriendlyException wrapUnfriendlyExceptions(String message, FriendlyException.Severity severity, Throwable throwable) {
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
}
