package lavaplayer.tools.io;

import lavaplayer.tools.http.ExtendedHttpConfigurable;

import java.io.Closeable;

/**
 * A thread-safe manager for HTTP interfaces.
 */
interface HttpInterfaceManager : ExtendedHttpConfigurable, Closeable {
    /**
     * @return An HTTP interface for use by the current thread.
     */
    fun get(): HttpInterface;
}
