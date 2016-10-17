#include "linked.h"
#include <stdlib.h>

static void linked_node_insert(linked_node_t* node, linked_node_t* after) {
	node->prev = after;
	node->next = after->next;
	after->next->prev = node;
	after->next = node;
}

void* linked_node_remove(linked_node_t* node) {
	node->next->prev = node->prev;
	node->prev->next = node->next;
	node->next = NULL;
	node->prev = NULL;
	return node->item;
}

void linked_node_initialise(linked_node_t* node, void* item) {
	node->item = item;
	node->next = NULL;
	node->prev = NULL;
}

static void* linked_list_remove_checked(linked_list_t* list, linked_node_t* node) {
	return node == &list->head ? NULL : linked_node_remove(node);
}

void* linked_list_remove_first(linked_list_t* list) {
	return linked_list_remove_checked(list, list->head.next);
}

void* linked_list_remove_last(linked_list_t* list) {
	return linked_list_remove_checked(list, list->head.prev);
}

static void* linked_list_peek_checked(linked_list_t* list, linked_node_t* node) {
	return node == &list->head ? NULL : node->item;
}

void* linked_list_peek_first(linked_list_t* list) {
	return linked_list_peek_checked(list, list->head.next);
}

void* linked_list_peek_last(linked_list_t* list) {
	return linked_list_peek_checked(list, list->head.prev);
}

void linked_list_insert_first(linked_list_t* list, linked_node_t* node) {
	linked_node_insert(node, &list->head);
}

void linked_list_insert_last(linked_list_t* list, linked_node_t* node) {
	linked_node_insert(node, list->head.prev);
}

void linked_list_initialise(linked_list_t* list) {
	list->head.item = NULL;
	list->head.next = &list->head;
	list->head.prev = &list->head;
}
