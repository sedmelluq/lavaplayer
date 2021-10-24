package com.sedmelluq.discord.lavaplayer.source.common

import java.util.regex.Pattern

data class Extractor<R : LinkRoutes>(val pattern: Pattern, val router: ExtractorRouter<R>)
