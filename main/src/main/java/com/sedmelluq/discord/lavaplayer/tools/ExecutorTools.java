package com.sedmelluq.discord.lavaplayer.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for working with executors.
 */
public class ExecutorTools {
  private static final Logger log = LoggerFactory.getLogger(ExecutorTools.class);

  private static final long WAIT_TIME = 1000L;

  /**
   * Shut down an executor and log the shutdown result. The executor is given a fixed amount of time to shut down, if it
   * does not manage to do it in that time, then this method just returns.
   *
   * @param executorService Executor service to shut down
   * @param description Description of the service to use for logging
   */
  public static void shutdownExecutor(ExecutorService executorService, String description) {
    if (executorService == null) {
      return;
    }

    log.debug("Shutting down executor {}", description);

    executorService.shutdownNow();

    try {
      if (!executorService.awaitTermination(WAIT_TIME, TimeUnit.SECONDS)) {
        log.debug("Executor {} did not shut down in {}", description, WAIT_TIME);
      } else {
        log.debug("Executor {} successfully shut down", description);
      }
    } catch (InterruptedException e) {
      log.debug("Received an interruption while shutting down executor {}", description);
      Thread.currentThread().interrupt();
    }
  }
}
