package lavaplayer.tools

import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun interface SuspendedCallable<T> {

    suspend fun call(): T

    fun asJavaCallable(coroutineContext: CoroutineContext = EmptyCoroutineContext) = Callable {
        runBlocking(coroutineContext) { call() }
    }

}
