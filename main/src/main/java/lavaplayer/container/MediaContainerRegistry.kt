package lavaplayer.container

open class MediaContainerRegistry(vararg val probes: MediaContainerProbe) {
    companion object {
        @JvmField
        val DEFAULT_REGISTRY = MediaContainerRegistry(MediaContainer.asList())

        fun extended(vararg additional: MediaContainerProbe): MediaContainerRegistry {
            val probes = MediaContainer.asList()
            probes.addAll(probes.intersect(additional.toList()))
            return MediaContainerRegistry(probes)
        }
    }

    constructor(probes: List<MediaContainerProbe>) : this(*probes.toTypedArray())

    fun find(name: String): MediaContainerProbe? =
        probes.find { it.name == name }
}
