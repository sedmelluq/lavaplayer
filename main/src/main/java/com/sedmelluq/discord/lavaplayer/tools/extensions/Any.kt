package com.sedmelluq.discord.lavaplayer.tools.extensions

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.wait() = (this as Object).wait()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.wait(timeout: Long) = (this as Object).wait(timeout)

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.notify() = (this as Object).notify()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.notifyAll() = (this as Object).notifyAll()
