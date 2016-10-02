package com.sedmelluq.discord.lavaplayer.tools;

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
   * @param throwable The exception to potentially wrap
   * @return Original or wrapped exception
   */
  public static FriendlyException wrapUnfriendlyExceptions(String message, Throwable throwable) {
    if (throwable instanceof FriendlyException) {
      return (FriendlyException) throwable;
    } else {
      return new FriendlyException(message, throwable);
    }
  }
}
