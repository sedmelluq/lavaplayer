package lavaplayer.tools.exception

import lavaplayer.tools.PlayerLibrary
import java.lang.Exception

class EnvironmentInformation private constructor(message: String) : Exception(message, null, false, false) {
    companion object {
        private val PROPERTIES = arrayOf(
            "os.arch",
            "os.name",
            "os.version",
            "java.vendor",
            "java.version",
            "java.runtime.version",
            "java.vm.version"
        )

        val INSTANCE = create()

        private fun create(): EnvironmentInformation {
            val builder = DetailMessageBuilder()
            builder.appendField("lavaplayer.version", PlayerLibrary.VERSION)
            for (property in PROPERTIES) {
                builder.appendField(property, System.getProperty(property))
            }

            return EnvironmentInformation(builder.toString())
        }
    }
}
