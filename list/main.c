#include <assert.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

typedef struct Node {
    char *value;
    struct Node *prev;
    struct Node *next;
} Node;

typedef struct {
    Node *null;
} List;

List make_list()
{
    // Create empty list.
    List list;
    list.null = malloc(sizeof(Node));
    list.null->prev = list.null;
    list.null->next = list.null;
    list.null->value = NULL;
    return list;
}

void insert(List *list, char const *str)
{
    // Insert new node in front of the list.
    assert(list && list->null && str);
    Node *node = malloc(sizeof(Node));
    node->value = strdup(str);
    node->prev = list->null;
    node->next = list->null->next;
    node->next->prev = node;
    list->null->next = node;
}

Node *find(List *list, char const *str)
{
    // Look for first occurrence of the string in the list.
    assert(list && list->null && str);
    for (Node *node = list->null->next; node != list->null; node = node->next) {
        assert(node->value);
        if (strcmp(node->value, str) == 0) {
            return node;
        }
    }
    return NULL;
}

void delete(List *list, char const *str)
{
    // Delete first occurrence of the string in the list.
    assert(list && list->null && str);
    Node *node = find(list, str);
    if (node) {
        node->prev->next = node->next;
        node->next->prev = node->prev;
        free(node->value);
        free(node);
    } 
}

int main()
{
    List list = make_list();
    assert(list.null);
    assert(!list.null->value);
    assert(list.null->prev == list.null);
    assert(list.null->next == list.null);
    assert(!find(&list, "hello"));

    insert(&list, "hello");
    insert(&list, "world");

    Node *hello = find(&list, "hello");
    assert(hello == list.null->prev);
    Node *world = find(&list, "world");
    assert(world == list.null->next);

    delete(&list, "world");
    assert(list.null->next == hello);
    assert(list.null->prev == hello);
}
