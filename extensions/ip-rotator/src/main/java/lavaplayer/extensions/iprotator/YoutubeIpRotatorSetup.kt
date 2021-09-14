package lavaplayer.extensions.iprotator

import lavaplayer.extensions.iprotator.planner.AbstractRoutePlanner
import lavaplayer.source.common.SourceRegistry
import lavaplayer.source.youtube.YoutubeHttpContextFilter
import lavaplayer.source.youtube.YoutubeItemSourceManager
import lavaplayer.tools.extensions.source
import lavaplayer.tools.http.ExtendedHttpConfigurable
import lavaplayer.tools.http.HttpContextFilter

class YoutubeIpRotatorSetup(routePlanner: AbstractRoutePlanner) : IpRotatorSetup(routePlanner) {
    companion object {
        private const val DEFAULT_RETRY_LIMIT = 4
        private val DEFAULT_DELEGATE: HttpContextFilter = YoutubeHttpContextFilter()
        private val RETRY_HANDLER = IpRotatorRetryHandler()

        operator fun invoke(routePlanner: AbstractRoutePlanner, build: YoutubeIpRotatorSetup.() -> Unit): YoutubeIpRotatorSetup {
            return YoutubeIpRotatorSetup(routePlanner)
                .apply(build)
        }
    }

    private val mainConfiguration = mutableListOf<ExtendedHttpConfigurable>()
    private val searchConfiguration = mutableListOf<ExtendedHttpConfigurable>()

    var retryLimit = DEFAULT_RETRY_LIMIT
    var mainDelegate = DEFAULT_DELEGATE
    var searchDelegate: HttpContextFilter? = null

    override val retryHandler: IpRotatorRetryHandler = RETRY_HANDLER

    /**
     * Applies this ip-rotator configuration to the supplied [sourceManager]
     *
     * @param sourceManager The [YoutubeItemSourceManager] to apply to.
     */
    fun applyTo(sourceManager: YoutubeItemSourceManager): YoutubeIpRotatorSetup {
        useConfiguration(sourceManager.mainHttpConfiguration, false)
        useConfiguration(sourceManager.searchHttpConfiguration, true)
        useConfiguration(sourceManager.searchMusicHttpConfiguration, true)
        return this
    }

    /**
     * Applies this ip-rotator configuration to the supplied [registry]
     *
     * @param registry The [SourceRegistry] to apply to.
     */
    fun applyTo(registry: SourceRegistry): YoutubeIpRotatorSetup {
        val sourceManager = registry.source<YoutubeItemSourceManager>()
        sourceManager?.let { applyTo(it) }
        return this
    }

    fun withRetryLimit(limit: Int): YoutubeIpRotatorSetup {
        retryLimit = limit
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

    override fun setup() {
        apply(mainConfiguration, IpRotatorFilter(mainDelegate, false, routePlanner, retryLimit))
        apply(searchConfiguration, IpRotatorFilter(searchDelegate, true, routePlanner, retryLimit))
    }

    private fun useConfiguration(configurable: ExtendedHttpConfigurable, isSearch: Boolean): YoutubeIpRotatorSetup {
        val configurations = if (isSearch) searchConfiguration else mainConfiguration
        configurations.add(configurable)
        return this
    }
}
