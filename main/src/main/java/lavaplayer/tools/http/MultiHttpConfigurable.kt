package lavaplayer.tools.http

import lavaplayer.tools.io.BuilderConfigurator
import lavaplayer.tools.io.RequestConfigurator

open class MultiHttpConfigurable(private val configurables: Collection<ExtendedHttpConfigurable>) :
    ExtendedHttpConfigurable {
    override fun setHttpContextFilter(filter: HttpContextFilter) {
        for (configurable in configurables) {
            configurable.setHttpContextFilter(filter)
        }
    }

    override fun configureRequests(configurator: RequestConfigurator) {
        for (configurable in configurables) {
            configurable.configureRequests(configurator)
        }
    }

    override fun configureBuilder(configurator: BuilderConfigurator) {
        for (configurable in configurables) {
            configurable.configureBuilder(configurator)
        }
    }
}
