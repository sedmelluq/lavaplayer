package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;

public interface ExtendedHttpConfigurable extends HttpConfigurable {
    void setHttpContextFilter(HttpContextFilter filter);
}
