#pragma once

#include <stdint.h>
#include <stdlib.h>
#include <stdbool.h>

typedef void (*entry_free_fn) (void* entry);
typedef bool (*entry_iterate_fn) (void* entry, void* context);

typedef struct hashmap_entry_s {
	uint64_t key;
	struct hashmap_entry_s* next;
} hashmap_entry_t;

typedef struct hashmap_s {
	size_t size;
	size_t entry_size;
	size_t bucket_count;
	hashmap_entry_t** buckets;
	entry_free_fn free_func;
} hashmap_t;

hashmap_t* hashmap_create(size_t entry_size, size_t initial_capacity, entry_free_fn free_func);
void hashmap_destroy(hashmap_t* map);
void* hashmap_get(hashmap_t* map, uint64_t key);
void* hashmap_put(hashmap_t* map, uint64_t key, bool* existed);
void* hashmap_remove(hashmap_t* map, uint64_t key);
void hashmap_iterate(hashmap_t* map, entry_iterate_fn iterate_func, void* context);
