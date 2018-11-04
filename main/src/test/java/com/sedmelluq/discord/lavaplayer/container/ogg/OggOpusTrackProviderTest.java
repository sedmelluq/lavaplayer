package com.sedmelluq.discord.lavaplayer.container.ogg;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OggOpusTrackProviderTest {

    @Test
    void readValidTag() {
        byte[] data = "Foo=Bar".getBytes();
        Optional<Map.Entry<String, String>> entry = OggOpusTrackProvider.readTag(data);
        assertTrue(entry.isPresent());
        assertEquals("Foo", entry.get().getKey());
        assertEquals("Bar", entry.get().getValue());
    }

    @Test
    void readWeirdTag() {
        byte[] data = "FooBar=".getBytes();
        Optional<Map.Entry<String, String>> entry = OggOpusTrackProvider.readTag(data);
        assertTrue(entry.isPresent());
        assertEquals("FooBar", entry.get().getKey());
        assertEquals("", entry.get().getValue());
    }

    @Test
    void readOtherWeirdTag() {
        byte[] data = "=FooBar".getBytes();
        Optional<Map.Entry<String, String>> entry = OggOpusTrackProvider.readTag(data);
        assertTrue(entry.isPresent());
        assertEquals("", entry.get().getKey());
        assertEquals("FooBar", entry.get().getValue());
    }

    @Test
    void readBrokenTag() {
        byte[] data = "FooBar".getBytes();
        Optional<Map.Entry<String, String>> entry = OggOpusTrackProvider.readTag(data);
        assertFalse(entry.isPresent());
    }
}