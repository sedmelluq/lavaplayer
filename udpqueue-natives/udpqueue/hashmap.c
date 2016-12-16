#include "hashmap.h"

hashmap_t* hashmap_create(size_t entry_size, size_t initial_capacity, entry_free_fn free_func) {
	hashmap_t* map = calloc(1, sizeof(*map));
	
	if (map == NULL) {
		return NULL;
	}
	
	map->size = 0;
	map->entry_size = entry_size;
	map->free_func = free_func;
	
	map->bucket_count = 1;
	
	size_t minimum_bucket_count = initial_capacity * 4 / 3;
	
	while (map->bucket_count <= minimum_bucket_count) {
		map->bucket_count <<= 1;
	}
	
	map->buckets = calloc(map->bucket_count, sizeof(*map->buckets));
	
	if (map->buckets == NULL) {
		free(map);
		return NULL;
	}
	
	return map;
}

void hashmap_destroy(hashmap_t* map) {
	for (size_t i = 0; i < map->bucket_count; i++) {
		hashmap_entry_t* next;
		
		for (hashmap_entry_t* entry = map->buckets[i]; entry != NULL; entry = next) {
			next = entry->next;
			map->free_func(entry);
			free(entry);
		}
	}
	
	free(map->buckets);
	free(map);
}

static uint32_t hashmap_hash_uint64(uint64_t key) {
	return (uint32_t) (key ^ key >> 32);
}

static size_t hashmap_index_from_hash(size_t bucket_count, uint32_t hash) {
	return ((size_t) hash) & (bucket_count - 1);
}

void* hashmap_get(hashmap_t* map, uint64_t key) {
	size_t index = hashmap_index_from_hash(map->bucket_count, hashmap_hash_uint64(key));
	
	for (hashmap_entry_t* entry = map->buckets[index]; entry != NULL; entry = entry->next) {
		if (entry->key == key) {
			return entry;
		}
	}
	
	return NULL;
}

static void hashmap_check_resize(hashmap_t* map) {
	if (map->size <= map->bucket_count * 4 / 3) {
		return;
	}
	
	size_t new_bucket_count = map->bucket_count << 1;
	hashmap_entry_t** new_buckets = calloc(new_bucket_count, sizeof(*new_buckets));
	
	if (new_buckets == NULL) {
		return;
	}
	
	for (size_t i = 0; i < map->bucket_count; i++) {
		hashmap_entry_t* next;
		
		for (hashmap_entry_t* entry = map->buckets[i]; entry != NULL; entry = next) {
			next = entry->next;
			
			size_t new_index = hashmap_index_from_hash(new_bucket_count, hashmap_hash_uint64(entry->key));
			entry->next = new_buckets[new_index];
			new_buckets[new_index] = entry;
		}
	}
	
	free(map->buckets);
	map->buckets = new_buckets;
	map->bucket_count = new_bucket_count;
}

void* hashmap_put(hashmap_t* map, uint64_t key, bool* existed) {
	size_t index = hashmap_index_from_hash(map->bucket_count, hashmap_hash_uint64(key));
	hashmap_entry_t** reference = &(map->buckets[index]);
	
	while (*reference != NULL) {
		if ((*reference)->key == key) {
			*existed = true;
			return *reference;
		}

		reference = &((*reference)->next);
	}
	
	*existed = false;
	
	hashmap_entry_t* new_entry = calloc(1, map->entry_size);
	
	if (new_entry == NULL) {
		return NULL;
	}
	
	new_entry->key = key;
	*reference = new_entry;
	
	hashmap_check_resize(map);
	return new_entry;
}

void* hashmap_remove(hashmap_t* map, uint64_t key) {
	size_t index = hashmap_index_from_hash(map->bucket_count, hashmap_hash_uint64(key));
	hashmap_entry_t** reference = &(map->buckets[index]);
	
	while (*reference != NULL) {
		if ((*reference)->key == key) {
			hashmap_entry_t* result = *reference;
			*reference = (*reference)->next;
			return result;
		}

		reference = &((*reference)->next);
	}
	
	return NULL;
}

void hashmap_iterate(hashmap_t* map, entry_iterate_fn iterate_func, void* context) {
	for (size_t i = 0; i < map->bucket_count; i++) {
		hashmap_entry_t* next;
		
		for (hashmap_entry_t* entry = map->buckets[i]; entry != NULL; entry = next) {
			next = entry->next;
			
			if (!iterate_func(entry, context)) {
				return;
			}
		}
	}
}
