package com.sedmelluq.lava.extensions.youtuberotator.tools

class RateLimitException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
