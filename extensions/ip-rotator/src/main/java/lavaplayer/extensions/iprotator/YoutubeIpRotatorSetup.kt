package lavaplayer.extensions.iprotator

import lavaplayer.extensions.iprotator.planner.AbstractRoutePlanner
import lavaplayer.manager.AudioPlayerManager
import lavaplayer.source.youtube.YoutubeHttpContextFilter
import lavaplayer.source.youtube.YoutubeItemSourceManager
import lavaplayer.tools.extensions.source
import lavaplayer.tools.http.ExtendedHttpClientBuilder
import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.tools.http.HttpContextFilter
import lavaplayer.tools.http.SimpleHttpClientConnectionManager
import org.apache.http.impl.client.HttpClientBuilder

class YoutubeIpRotatorSetup(private val routePlanner: AbstractRoutePlanner) {
    private val mainConfiguration: MutableList<ExtendedHttpConfigurable>
    private val searchConfiguration: MutableList<ExtendedHttpConfigurable>

    var retryLimit = DEFAULT_RETRY_LIMIT
    var mainDelegate = DEFAULT_DELEGATE
    var searchDelegate: HttpContextFilter? = null

    fun forConfiguration(configurable: ExtendedHttpConfigurable, isSearch: Boolean): YoutubeIpRotatorSetup {
        if (isSearch) {
            searchConfiguration.add(configurable)
        } else {
            mainConfiguration.add(configurable)
        }
        return this
    }

    fun forSource(sourceManager: YoutubeItemSourceManager): YoutubeIpRotatorSetup {
        forConfiguration(sourceManager.mainHttpConfiguration, false)
        forConfiguration(sourceManager.searchHttpConfiguration, true)
        forConfiguration(sourceManager.searchMusicHttpConfiguration, true)
        return this
    }

    fun forManager(playerManager: AudioPlayerManager): YoutubeIpRotatorSetup {
        val sourceManager = playerManager.source<YoutubeItemSourceManager>()
        sourceManager?.let { forSource(it) }
        return this
    }

    fun withRetryLimit(retryLimit: Int): YoutubeIpRotatorSetup {
        this.retryLimit = retryLimit
        return this
    }

    fun withMainDelegateFilter(filter: HttpContextFilter): YoutubeIpRotatorSetup {
        mainDelegate = filter
        return this
    }

    fun withSearchDelegateFilter(filter: HttpContextFilter?): YoutubeIpRotatorSetup {
        searchDelegate = filter
        return this
    }

    fun setup() {
        apply(mainConfiguration, IpRotatorFilter(mainDelegate, false, routePlanner, retryLimit))
        apply(searchConfiguration, IpRotatorFilter(searchDelegate, true, routePlanner, retryLimit))
    }

    protected fun apply(configurables: List<ExtendedHttpConfigurable>, filter: IpRotatorFilter?) {
        for (configurable in configurables) {
            configurable.configureBuilder { builder: HttpClientBuilder ->
                (builder as ExtendedHttpClientBuilder).setConnectionManagerFactory(::SimpleHttpClientConnectionManager)
            }

            configurable.configureBuilder { it: HttpClientBuilder ->
                it.setRoutePlanner(routePlanner)

                // No retry for some exceptions we know are hopeless for retry.
                it.setRetryHandler(RETRY_HANDLER)

                // Regularly cleans up per-route connection pool which gets huge due to many routes caused by
                // each request having a unique route.
                it.evictExpiredConnections()
            }

            configurable.setHttpContextFilter(filter!!)
        }
    }

    companion object {
        private const val DEFAULT_RETRY_LIMIT = 4
        private val DEFAULT_DELEGATE: HttpContextFilter = YoutubeHttpContextFilter()
        private val RETRY_HANDLER = IpRotatorRetryHandler()
    }

    init {
        mainConfiguration = ArrayList()
        searchConfiguration = ArrayList()
    }
}
