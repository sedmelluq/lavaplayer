#include <time.h>

#include "../timing.h"

int64_t timing_get_nanos(void) {
	struct timespec tv;
	clock_gettime(CLOCK_REALTIME, &tv);
	return tv.tv_sec * 1000000000LL + tv.tv_nsec;
}

void timing_sleep(int64_t nanos) {
	struct timespec tv = {
		nanos / 1000000000LL,
		nanos % 1000000000LL
	};
	nanosleep(&tv, NULL);
}
