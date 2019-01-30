#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdint.h>
#include <inttypes.h>

enum {
	SYSTEM_TOTAL,
	SYSTEM_USER,
	SYSTEM_KERNEL,
	PROCESS_USER,
	PROCESS_KERNEL
};

static void read_process_stats(jlong* process_user, jlong* process_kernel) {
	int64_t pid = (int64_t) getpid();
	char process_file[64];
	
	*process_user = 0;
	*process_kernel = 0;
	
	if (snprintf(process_file, sizeof(process_file), "/proc/%"PRId64"/stat", pid) < 0) {
		return;
	}
	
	FILE* process_stat_file = fopen(process_file, "r");
	
	if (process_stat_file != NULL) {
		int64_t user_time, kernel_time;
		
		if (fscanf(process_stat_file, "%*d %*s %*s %*d %*d %*d %*d %*d %*d %*d %*d %*d %*d %"PRId64" %"PRId64, &user_time, &kernel_time) == 2) {
			*process_user = user_time;
			*process_kernel = kernel_time;
		}
		
		fclose(process_stat_file);
	}
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_statistics_CpuStatisticsLibrary_getSystemTimes(JNIEnv *jni, jobject me, jlongArray valueArray) {
	FILE* stat_file = fopen("/proc/stat", "r");
	int64_t user_time, nice_time, system_time, idle_time, iowait_time, irq_time, softirq_time, steal_time;
	
	if (stat_file != NULL) {
		if (fscanf(stat_file, "%*s %"PRId64" %"PRId64" %"PRId64" %"PRId64" %"PRId64" %"PRId64" %"PRId64" %"PRId64,
				&user_time, &nice_time, &system_time, &idle_time, &iowait_time, &irq_time, &softirq_time, &steal_time) == 8) {

			jlong values[5];
			
			values[SYSTEM_USER] = user_time + nice_time;
			values[SYSTEM_KERNEL] = system_time + irq_time + softirq_time + steal_time;
			values[SYSTEM_TOTAL] = values[SYSTEM_USER] + values[SYSTEM_KERNEL] + idle_time + iowait_time;
			values[PROCESS_USER] = 0;
			values[PROCESS_KERNEL] = 0;
			
			read_process_stats(&values[PROCESS_USER], &values[PROCESS_KERNEL]);
			
			(*jni)->SetLongArrayRegion(jni, valueArray, 0, sizeof(values) / sizeof(*values), values);
		}
		
		fclose(stat_file);
	}
}
