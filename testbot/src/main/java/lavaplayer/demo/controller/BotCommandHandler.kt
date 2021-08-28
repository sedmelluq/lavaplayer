package lavaplayer.demo.controller

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class BotCommandHandler(val name: String = "", val usage: String = "")
