#pragma once

typedef struct linked_node_s {
	struct linked_node_s* next;
	struct linked_node_s* prev;
	void* item;
} linked_node_t;

typedef struct linked_list_s {
	linked_node_t head;
} linked_list_t;

void* linked_node_remove(linked_node_t* node);
void linked_node_initialise(linked_node_t* node, void* item);
void* linked_list_remove_first(linked_list_t* list);
void* linked_list_remove_last(linked_list_t* list);
void* linked_list_peek_first(linked_list_t* list);
void* linked_list_peek_last(linked_list_t* list);
void linked_list_insert_first(linked_list_t* list, linked_node_t* node);
void linked_list_insert_last(linked_list_t* list, linked_node_t* node);
void linked_list_initialise(linked_list_t* list);