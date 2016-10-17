#pragma once

#include <stdint.h>

int64_t timing_get_nanos(void);

void timing_sleep(int64_t nanos);
