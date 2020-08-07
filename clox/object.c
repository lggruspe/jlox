#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType)

#define ALLOCATE_OBJ_STRING(length) \
    (ObjString*)allocateObject(sizeof(ObjString) + \
            sizeof(char[(length) + 1]), OBJ_STRING)

static Obj* allocateObject(size_t size, ObjType type) {
    Obj* object = (Obj*)reallocate(NULL, 0, size);
    object->type = type;

    object->next = vm.objects;
    vm.objects = object;
    return object;
}

ObjString* allocateString(int length) {
    ObjString* string = ALLOCATE_OBJ_STRING(length);
    string->length = length;
    string->chars[0] = '\0';
    string->chars[length] = '\0';
    return string;
}

void insertChars(ObjString* string, const char* chars, int start, int length) {
    memcpy(string->chars + start, chars, length);
    string->chars[start + length + 1] = '\0';
}

ObjString* copyString(const char* chars, int length) {
    ObjString* string = allocateString(length);
    insertChars(string, chars, 0, length);
    return string;
}

void printObject(Value value) {
    switch (OBJ_TYPE(value)) {
    case OBJ_STRING:
        printf("%s", AS_CSTRING(value));
        break;
    }
}
