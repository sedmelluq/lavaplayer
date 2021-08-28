package lavaplayer.container

class MediaContainerRegistry(val all: List<MediaContainerProbe>) {
    companion object {
        @JvmField
        val DEFAULT_REGISTRY = MediaContainerRegistry(MediaContainer.asList())

        fun extended(vararg additional: MediaContainerProbe): MediaContainerRegistry {
            val probes = MediaContainer.asList()
            probes.addAll(probes.intersect(additional.toList()))
            return MediaContainerRegistry(probes)
        }
    }

    fun find(name: String): MediaContainerProbe? =
        all.find { it.name == name }
}
