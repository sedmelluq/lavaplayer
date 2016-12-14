#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>
#include <stdint.h>
#include <inttypes.h>
#include <string.h>
#include "hashmap.h"
#include "mutex.h"
#include "timing.h"
#include "linked.h"

#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>

typedef SOCKET socket_t;
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

typedef int socket_t;
#define closesocket close
#endif

typedef struct queued_packet_s {
	uint8_t* data;
	size_t data_length;
} queued_packet_t;

typedef struct unsent_packet_s {
	queued_packet_t packet;
	struct addrinfo* address;
} unsent_packet_t;

typedef struct queue_s {
	uint64_t key;
	hashmap_entry_t entry;
	int64_t next_due_time;
	queued_packet_t* packet_buffer;
	size_t buffer_index;
	size_t buffer_size;
	size_t buffer_capacity;
	struct addrinfo* address;
	linked_node_t link;
} queue_t;

typedef struct queue_manager_s {
	hashmap_t* queues;
	size_t queue_buffer_capacity;
	int64_t packet_interval;
	mutex_t* lock;
	mutex_t* process_lock;
	bool shutting_down;
	linked_list_t queue_linked;
} queue_manager_t;

static void queue_entry_free(void* entry) {
	queue_t* queue = entry;
	
	if (queue->packet_buffer != NULL) {
		for (size_t i = 0; i < queue->buffer_size; i++) {
			size_t index = (queue->buffer_index + i) % queue->buffer_capacity;
			free(queue->packet_buffer[index].data);
		}

		free(queue->packet_buffer);
	}

	if (queue->address != NULL) {
		freeaddrinfo(queue->address);
	}

	if (queue->link.next != NULL) {
		linked_node_remove(&queue->link);
	}
}

static void queue_pop_packet(queue_t* queue, unsent_packet_t* packet_out) {
	queued_packet_t* packet = &queue->packet_buffer[queue->buffer_index];

	queue->buffer_index = (queue->buffer_index + 1) % queue->buffer_capacity;
	queue->buffer_size--;

	packet_out->packet = *packet;
	packet_out->address = queue->address;

	packet->data = NULL;
	packet->data_length = 0;
}

static void manager_destroy(queue_manager_t* manager) {
	if (manager->lock != NULL && manager->process_lock != NULL) {
		mutex_lock(manager->lock);
		manager->shutting_down = true;
		mutex_unlock(manager->lock);

		mutex_lock(manager->process_lock);
		mutex_unlock(manager->process_lock);
	}

	mutex_destroy(manager->lock);
	mutex_destroy(manager->process_lock);

	if (manager->queues != NULL) {
		hashmap_destroy(manager->queues);
	}

	free(manager);
}

static queue_manager_t* manager_create(size_t queue_buffer_capacity, int64_t packet_interval) {
	queue_manager_t* manager = calloc(1, sizeof(*manager));
	
	if (manager == NULL) {
		return NULL;
	}
	
	manager->lock = mutex_create();
	manager->process_lock = mutex_create();
	
	if (manager->lock == NULL || manager->process_lock == NULL) {
		manager_destroy(manager);
		return NULL;
	}

	linked_list_initialise(&manager->queue_linked);
	
	manager->queue_buffer_capacity = queue_buffer_capacity;
	manager->packet_interval = packet_interval;
	manager->queues = hashmap_create(sizeof(queue_t), 100, queue_entry_free);
	
	if (manager->queues == NULL) {
		manager_destroy(manager);
		return NULL;
	}

	return manager;
}

static size_t manager_get_remaining_capacity(queue_manager_t* manager, uint64_t key) {
	mutex_lock(manager->lock);
	
	queue_t* queue = hashmap_get(manager->queues, key);
	size_t remaining = queue == NULL ? (jint) manager->queue_buffer_capacity : queue->buffer_capacity - queue->buffer_size;
	
	mutex_unlock(manager->lock);
	
	return remaining;
}

static struct addrinfo* manager_resolve_address(const char* address, int32_t port) {
	char port_text[32];

	struct addrinfo hints;

	memset(&hints, 0, sizeof(hints));
	hints.ai_flags = AI_NUMERICHOST | AI_NUMERICSERV;
	hints.ai_socktype = SOCK_DGRAM;
	hints.ai_protocol = IPPROTO_UDP;

	snprintf(port_text, sizeof(port_text), "%" PRId32, port);

	struct addrinfo* result = NULL;
	getaddrinfo(address, port_text, &hints, &result);

	return result;
}

static bool manager_queue_packet_locked(queue_manager_t* manager, uint64_t key, const char* address, int32_t port, uint8_t* data, size_t data_length) {
	bool existed;
	queue_t* queue = hashmap_put(manager->queues, key, &existed);

	if (queue == NULL) {
		return false;
	}
	else if (!existed) {
		queue->key = key;
		queue->next_due_time = 0;
		queue->buffer_capacity = manager->queue_buffer_capacity;
		queue->address = manager_resolve_address(address, port);
		queue->buffer_index = 0;
		queue->buffer_size = 0;
		queue->packet_buffer = calloc(queue->buffer_capacity, sizeof(*queue->packet_buffer));

		if (queue->address == NULL || queue->packet_buffer == NULL) {
			queue_entry_free(queue);
			hashmap_remove(manager->queues, key);
			return false;
		}

		linked_node_initialise(&queue->link, queue);
		linked_list_insert_first(&manager->queue_linked, &queue->link);
	}

	if (queue->buffer_size >= queue->buffer_capacity) {
		return false;
	}

	size_t next_index = (queue->buffer_index + queue->buffer_size) % queue->buffer_capacity;
	queue->packet_buffer[next_index].data = data;
	queue->packet_buffer[next_index].data_length = data_length;
	queue->buffer_size++;
	return true;
}

static bool manager_queue_packet(queue_manager_t* manager, uint64_t key, const char* address, int32_t port, void* data, size_t data_length) {
	uint8_t* bytes = malloc(data_length);
	if (bytes == NULL) {
		return false;
	}

	memcpy(bytes, data, data_length);

	mutex_lock(manager->lock);
	bool result = manager_queue_packet_locked(manager, key, address, port, bytes, data_length);
	mutex_unlock(manager->lock);

	if (!result) {
		free(bytes);
	}

	return result;
}

static int64_t manager_get_target_time(queue_manager_t* manager, int64_t current_time) {
	queue_t* queue = linked_list_peek_first(&manager->queue_linked);

	return queue == NULL ? current_time + manager->packet_interval : queue->next_due_time;
}

static int64_t manager_process_next_locked(queue_manager_t* manager, unsent_packet_t* packet_out, int64_t current_time) {
	queue_t* queue = linked_list_peek_first(&manager->queue_linked);

	packet_out->packet.data = NULL;
	packet_out->address = NULL;

	if (queue == NULL) {
		return current_time + manager->packet_interval;
	}

	if (queue->buffer_size == 0) {
		hashmap_remove(manager->queues, queue->key);
		queue_entry_free(queue);
		return manager_get_target_time(manager, current_time);
	}

	if (queue->next_due_time == 0) {
		queue->next_due_time = current_time;
	}
	else if (queue->next_due_time - current_time >= 1500000LL) {
		return queue->next_due_time;
	}

	queue_pop_packet(queue, packet_out);

	linked_list_remove_first(&manager->queue_linked);
	linked_list_insert_last(&manager->queue_linked, &queue->link);

	current_time = timing_get_nanos();

	if (current_time - queue->next_due_time >= 2 * manager->packet_interval) {
		queue->next_due_time = current_time + manager->packet_interval;
	}
	else {
		queue->next_due_time += manager->packet_interval;
	}
	
	return manager_get_target_time(manager, current_time);
}

static void manager_dispatch_packet(queue_manager_t* manager, socket_t socketv4, socket_t socketv6, unsent_packet_t* unsent_packet) {
	socket_t socketvx = unsent_packet->address->ai_family == AF_INET ? socketv4 : socketv6;

	sendto(socketvx, (const char*) unsent_packet->packet.data, (int) unsent_packet->packet.data_length, 0, unsent_packet->address->ai_addr, sizeof(*unsent_packet->address->ai_addr));

	free(unsent_packet->packet.data);
	unsent_packet->packet.data = NULL;
	unsent_packet->packet.data_length = 0;
}

static void manager_process(queue_manager_t* manager) {
	socket_t socketv4 = socket(AF_INET, SOCK_DGRAM, 0);
	socket_t socketv6 = socket(AF_INET6, SOCK_DGRAM, 0);

	mutex_lock(manager->process_lock);

	while (true) {
		mutex_lock(manager->lock);

		if (manager->shutting_down) {
			mutex_unlock(manager->lock);
			break;
		}

		int64_t current_time = timing_get_nanos();
		unsent_packet_t packet_to_send = { 0 };

		int64_t target_time = manager_process_next_locked(manager, &packet_to_send, current_time);
		mutex_unlock(manager->lock);

		if (packet_to_send.packet.data != NULL) {
			manager_dispatch_packet(manager, socketv4, socketv6, &packet_to_send);
			current_time = timing_get_nanos();
		}

		int64_t wait_time = target_time - current_time;

		if (wait_time >= 1500000LL) {
			timing_sleep(wait_time);
		}
	}

	mutex_unlock(manager->process_lock);

	closesocket(socketv4);
	closesocket(socketv6);
}

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_create(JNIEnv* jni, jobject me, jint queue_buffer_capacity, jlong packet_interval) {
	return (jlong) manager_create((size_t) queue_buffer_capacity, packet_interval);
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_destroy(JNIEnv* jni, jobject me, jlong instance) {
	manager_destroy((queue_manager_t*) instance);
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_getRemainingCapacity(JNIEnv* jni, jobject me, jlong instance, jlong key) {
	return (jint) manager_get_remaining_capacity((queue_manager_t*) instance, (uint64_t) key);
}

JNIEXPORT jboolean JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_queuePacket(JNIEnv* jni, jobject me, jlong instance, jlong key, jstring address_string,
		jint port, jobject data_buffer, jint data_length) {

	const char* address = (*jni)->GetStringUTFChars(jni, address_string, NULL);
	void* bytes = (*jni)->GetDirectBufferAddress(jni, data_buffer);

	bool result = manager_queue_packet((queue_manager_t*) instance, (uint64_t) key, address, port, bytes, (size_t) data_length);

	(*jni)->ReleaseStringUTFChars(jni, address_string, address);

	return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_process(JNIEnv* jni, jobject me, jlong instance) {
	manager_process((queue_manager_t*) instance);
}

jint JNICALL waiting_iterate_callback(jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
	int wait_duration = *((int*) user_data);
	timing_sleep(wait_duration * 1000000LL);
	return JVMTI_VISIT_ABORT;
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_udpqueue_natives_UdpQueueManagerLibrary_pauseDemo(JNIEnv* jni, jclass me, jint length) {
	jvmtiEnv* jvmti;
	JavaVM* vm;

	if ((*jni)->GetJavaVM(jni, &vm) != JNI_OK) {
		return;
	}

	if ((*vm)->GetEnv(vm, (void**) &jvmti, JVMTI_VERSION_1_2) != JNI_OK) {
		return;
	}

	jvmtiCapabilities capabilities;
	memset(&capabilities, 0, sizeof(capabilities));
	capabilities.can_tag_objects = 1;
	(*jvmti)->AddCapabilities(jvmti, &capabilities);

	jvmtiHeapCallbacks callbacks;
	memset(&callbacks, 0, sizeof(callbacks));
	callbacks.heap_iteration_callback = waiting_iterate_callback;

	(*jvmti)->IterateThroughHeap(jvmti, 0, NULL, &callbacks, &length);
	(*jvmti)->DisposeEnvironment(jvmti);
}
