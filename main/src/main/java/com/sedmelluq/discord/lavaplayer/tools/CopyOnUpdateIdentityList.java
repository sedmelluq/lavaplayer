package com.sedmelluq.discord.lavaplayer.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for keeping a list which can be updated from the same thread while being iterated. The list can contain only
 * one of each item (based on identity). Not backed by a set as that would be overkill for the use cases this class is
 * intended for. Not thread-safe.
 */
public class CopyOnUpdateIdentityList<T> {
    public List<T> items = Collections.emptyList();

    public void add(T item) {
        for (T existingItem : items) {
            if (existingItem == item) {
                // No duplicates, do not add again.
                return;
            }
        }

        List<T> updated = new ArrayList<>(items.size() + 1);
        updated.addAll(items);
        updated.add(item);
        items = updated;
    }

    public void remove(T item) {
        List<T> updated = new ArrayList<>(items.size());

        for (T existingItem : items) {
            if (existingItem != item) {
                updated.add(existingItem);
            }
        }

        items = updated;
    }
}
