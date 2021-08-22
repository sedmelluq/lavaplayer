package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;

import java.io.Closeable;

/**
 * A thread-safe manager for HTTP interfaces.
 */
interface HttpInterfaceManager : ExtendedHttpConfigurable, Closeable {
    /**
     * @return An HTTP interface for use by the current thread.
     */
    fun getInterface(): HttpInterface;
}
