package com.sedmelluq.lavaplayer.core.http;

import java.io.Closeable;

/**
 * A thread-safe manager for HTTP interfaces.
 */
public interface HttpInterfaceManager extends ExtendedHttpConfigurable, Closeable {
  /**
   * @return An HTTP interface for use by the current thread.
   */
  HttpInterface getInterface();
}
