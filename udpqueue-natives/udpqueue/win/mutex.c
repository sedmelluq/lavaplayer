#include "../mutex.h"
#include <stdlib.h>

#define WIN32_LEAN_AND_MEAN
#include <Windows.h>

mutex_t mutex_create(void) {
	CRITICAL_SECTION* section = calloc(1, sizeof(*section));
	
	if (section != NULL) {
		InitializeCriticalSection(section);
	}

	return section;
}

void mutex_destroy(mutex_t mutex) {
	if (mutex != NULL) {
		DeleteCriticalSection(mutex);
		free(mutex);
	}
}

void mutex_lock(mutex_t mutex) {
	EnterCriticalSection(mutex);
}

void mutex_unlock(mutex_t mutex) {
	LeaveCriticalSection(mutex);
}
