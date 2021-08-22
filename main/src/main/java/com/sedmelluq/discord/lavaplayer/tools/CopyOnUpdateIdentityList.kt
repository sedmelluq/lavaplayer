package com.sedmelluq.discord.lavaplayer.tools;

import java.util.*
import kotlin.collections.ArrayList

/**
 * Wrapper for keeping a list which can be updated from the same thread while being iterated. The list can contain only
 * one of each item (based on identity). Not backed by a set as that would be overkill for the use cases this class is
 * intended for. Not thread-safe.
 */
class CopyOnUpdateIdentityList<T> {
    @JvmField
    var items: List<T> = Collections.emptyList();

    fun add(item: T) {
        for (existingItem in items) {
            if (existingItem == item) {
                // No duplicates, do not add again.
                return;
            }
        }

        val updated = ArrayList<T>(items.size + 1);
        updated.addAll(items);
        updated.add(item);
        items = updated;
    }

    fun remove(item: T) {
        val updated = ArrayList<T>(items.size);
        for (existingItem in items) {
            if (existingItem != item) {
                updated.add(existingItem);
            }
        }

        items = updated;
    }
}
