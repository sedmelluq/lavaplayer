package com.sedmelluq.discord.lavaplayer.tools.exception

import java.util.function.IntPredicate

class DetailMessageBuilder {
    val builder = StringBuilder()

    fun appendHeader(header: String): DetailMessageBuilder {
        builder.append(header)
        return this
    }

    fun appendField(name: String, value: Any?): DetailMessageBuilder {
        builder.append("\n  ").append(name).append(": ")

        if (value == null) {
            builder.append("<unspecified>")
        } else {
            builder.append(value.toString())
        }

        return this
    }

    fun appendArray(label: String, alwaysPrint: Boolean, array: Array<*>, check: IntPredicate): DetailMessageBuilder {
        var started = false

        for (i in array.indices) {
            if (check.test(i)) {
                if (!started) {
                    builder.append("\n  ").append(label).append(": ")
                    started = true
                }

                builder.append(array[i]).append(", ")
            }
        }

        if (started) {
            builder.setLength(builder.length - 2)
        } else if (alwaysPrint) {
            appendField(label, "NONE")
        }

        return this
    }

    override fun toString(): String {
        return builder.toString()
    }
}
