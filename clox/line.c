#include "memory.h"
#include "line.h"

void initLineArray(LineArray* array) {
    array->lines = NULL;
    array->capacity = 0;
    array->count = 0;
}

void freeLineArray(LineArray* array) {
    FREE_ARRAY(int, array->lines, array->capacity);
    initLineArray(array);
}

static int lineIndex(LineArray* array, int line) {
    for (int i = 0; i < array->count; ++i) {
        if (array->lines[i].lineno == line) {
            return i;
        }
    }
    return -1;
}

void incrementLineCounter(LineArray* array, int line) {
    int index = lineIndex(array, line);
    if (index >= 0) {
        array->lines[index].counter++;
        return;
    }

    if (array->capacity < array->count + 1) {
        int oldCapacity = array->capacity;
        array->capacity = GROW_CAPACITY(oldCapacity);
        array->lines = GROW_ARRAY(Line, array->lines, oldCapacity, array->capacity);
    }
    array->lines[array->count] = (Line){line, 1};
    array->count++;
}

int getLine(LineArray* array, int index) {
    int count = 0;  // if instructions checked
    int lineno = 0;
    while (index >= count) {
        Line line = array->lines[lineno];
        count += line.counter;
        lineno = line.lineno;
    }
    return lineno;
}
