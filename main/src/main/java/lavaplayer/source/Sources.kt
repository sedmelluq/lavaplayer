package lavaplayer.source

interface Sources {
    /**
     * The list of enabled source managers.
     */
    val sourceManagers: List<ItemSourceManager>

    /**
     * Registers an [ItemSourceManager]
     * @param sourceManager The source manager to register, which will be used for subsequent load item calls.
     */
    fun registerSourceManager(sourceManager: ItemSourceManager)

    /**
     * Shortcut for accessing a source manager of the specified class.
     *
     * @param klass The class of the source manager to return
     * @param T     The class of the source manager.
     *
     * @return The source manager of the specified class, or null if not registered.
     */
    fun <T : ItemSourceManager> source(klass: Class<T>): T?
}
