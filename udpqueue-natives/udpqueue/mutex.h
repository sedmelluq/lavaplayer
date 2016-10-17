#pragma once

typedef void* mutex_t;

mutex_t mutex_create(void);
void mutex_destroy(mutex_t mutex);
void mutex_lock(mutex_t mutex);
void mutex_unlock(mutex_t mutex);
