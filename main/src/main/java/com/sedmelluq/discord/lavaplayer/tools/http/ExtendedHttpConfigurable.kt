package com.sedmelluq.discord.lavaplayer.tools.http

import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable

interface ExtendedHttpConfigurable : HttpConfigurable {
    fun setHttpContextFilter(filter: HttpContextFilter)
}
