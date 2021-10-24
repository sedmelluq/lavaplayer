package com.sedmelluq.discord.lavaplayer.container.ogg.vorbis;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class VorbisCommentParser {
    public static Map<String, String> parse(ByteBuffer tagBuffer, boolean truncated) {
        Map<String, String> tags = new HashMap<>();

        int vendorLength = Integer.reverseBytes(tagBuffer.getInt());
        if (vendorLength < 0) {
            throw new IllegalStateException("Ogg comments vendor length is negative.");
        }

        tagBuffer.position(tagBuffer.position() + vendorLength);

        int itemCount = Integer.reverseBytes(tagBuffer.getInt());

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            if (tagBuffer.remaining() < Integer.BYTES) {
                if (!truncated) {
                    throw new IllegalArgumentException("Invalid tag buffer - tag size field out of bounds.");
                } else {
                    // The buffer is truncated, it may cut off at an arbitrary point.
                    break;
                }
            }

            int itemLength = Integer.reverseBytes(tagBuffer.getInt());

            if (itemLength < 0) {
                throw new IllegalStateException("Ogg comments tag item length is negative.");
            } else if (tagBuffer.remaining() < itemLength) {
                if (!truncated) {
                    throw new IllegalArgumentException("Invalid tag buffer - tag size field out of bounds.");
                } else {
                    // The buffer is truncated, it may cut off at an arbitrary point.
                    break;
                }
            }

            byte[] data = new byte[itemLength];
            tagBuffer.get(data);

            storeTagToMap(tags, data);
        }

        return tags;
    }

    private static void storeTagToMap(Map<String, String> tags, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '=') {
                tags.put(new String(data, 0, i, UTF_8), new String(data, i + 1, data.length - i - 1, UTF_8));
                break;
            }
        }
    }
}
