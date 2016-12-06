#include <stdlib.h>
#include <pthread.h>

#include "../mutex.h"

mutex_t mutex_create(void) {
	pthread_mutex_t* mutex = malloc(sizeof(*mutex));
	if (mutex) {
		pthread_mutex_init(mutex, NULL);
	}
	return mutex;
}

void mutex_destroy(mutex_t mutex) {
	pthread_mutex_destroy(mutex);
}

void mutex_lock(mutex_t mutex) {
	pthread_mutex_lock(mutex);
}

void mutex_unlock(mutex_t mutex) {
	pthread_mutex_unlock(mutex);
}
