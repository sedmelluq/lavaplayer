#include "../timing.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

static int64_t timing_get_frequency(void) {
	static int64_t frequency = 0;

	if (frequency == 0) {
		QueryPerformanceFrequency((LARGE_INTEGER*) &frequency);
	}

	return frequency;
}

int64_t timing_get_nanos(void) {
	int64_t counter;

	QueryPerformanceCounter((LARGE_INTEGER*) &counter);

	return counter * 1000000LL / timing_get_frequency() * 1000LL;
}

void timing_sleep(int64_t nanos) {
	Sleep((DWORD) (nanos / 1000000LL));
}
