package lavaplayer.filter

import mu.KotlinLogging

/**
 * An audio filter which may consist of a number of other filters.
 */
abstract class CompositeAudioFilter : UniversalPcmAudioFilter {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    protected abstract val filters: List<AudioFilter>

    override fun seekPerformed(requestedTime: Long, providedTime: Long) {
        for (filter in filters) {
            try {
                filter.seekPerformed(requestedTime, providedTime)
            } catch (e: Exception) {
                log.error(e) { "Notifying filter ${filter.javaClass} of seek failed with exception." }
            }
        }
    }

    @Throws(InterruptedException::class)
    override fun flush() {
        for (filter in filters) {
            try {
                filter.flush()
            } catch (e: Exception) {
                log.error(e) { "Flushing filter ${filter.javaClass} failed with exception." }
            }
        }
    }

    override fun close() {
        for (filter in filters) {
            try {
                filter.close()
            } catch (e: Exception) {
                log.error(e) { "Closing filter ${filter.javaClass} failed with exception." }
            }
        }
    }
}
