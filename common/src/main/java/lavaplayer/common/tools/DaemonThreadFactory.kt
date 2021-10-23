package lavaplayer.common.tools

import kotlinx.atomicfu.atomic
import org.slf4j.LoggerFactory
import java.lang.Runnable
import java.util.concurrent.ThreadFactory

/**
 * Thread factory for daemon threads.
 *
 * @param name         Name that will be included in thread names.
 * @param exitCallback Runnable to be executed when the thread exits.
 * @param nameFormat   Runnable to be executed when the thread exits.
 */
class DaemonThreadFactory @JvmOverloads constructor(
    name: String?,
    exitCallback: Runnable? = null,
    nameFormat: String? = DEFAULT_NAME_FORMAT
) : ThreadFactory {
    companion object {
        private val log = LoggerFactory.getLogger(DaemonThreadFactory::class.java)
        private var poolNumber by atomic(1)
        var DEFAULT_NAME_FORMAT = "lava-daemon-pool-%s-%d-thread-"
    }

    private val group: ThreadGroup
    private var threadNumber by atomic(1)
    private val namePrefix: String
    private val exitCallback: Runnable?

    init {
        val securityManager = System.getSecurityManager()
        group = if (securityManager != null) securityManager.threadGroup else Thread.currentThread().threadGroup
        namePrefix = String.format(nameFormat!!, name, poolNumber++)

        this.exitCallback = exitCallback
    }

    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(group, getThreadRunnable(runnable), namePrefix + threadNumber++, 0)
        thread.isDaemon = true
        thread.priority = Thread.NORM_PRIORITY

        return thread
    }

    private fun getThreadRunnable(target: Runnable): Runnable {
        return if (exitCallback == null) target else ExitCallbackRunnable(target)
    }

    private inner class ExitCallbackRunnable(private val original: Runnable) : Runnable {
        override fun run() {
            try {
                original.run()
            } finally {
                wrapExitCallback()
            }
        }

        private fun wrapExitCallback() {
            val wasInterrupted = Thread.interrupted()
            try {
                exitCallback?.run()
            } catch (throwable: Throwable) {
                log.error("Thread exit notification threw an exception.", throwable)
            } finally {
                if (wasInterrupted) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
}
