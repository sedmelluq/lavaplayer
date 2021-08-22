package com.sedmelluq.discord.lavaplayer.source

interface Sources {
    /**
     * The list of enabled source managers.
     */
    val sourceManagers: List<AudioSourceManager>

    /**
     * Registers an [AudioSourceManager]
     * @param sourceManager The source manager to register, which will be used for subsequent load item calls.
     */
    fun registerSourceManager(sourceManager: AudioSourceManager)

    /**
     * Shortcut for accessing a source manager of the specified class.
     *
     * @param klass The class of the source manager to return
     * @param T     The class of the source manager.
     *
     * @return The source manager of the specified class, or null if not registered.
     */
    fun <T : AudioSourceManager> source(klass: Class<T>): T?
}

/**
 * Shortcut for accessing a specific source manager.
 * @param T The source manager class.
 * @return The source manager of the specified class, or null if not registered.
 */
inline fun <reified T : AudioSourceManager> Sources.source(): T? {
    return source(T::class.java)
}
