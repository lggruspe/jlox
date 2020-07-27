#ifndef clox_line_h
#define clox_line_h

#include "common.h"

typedef struct {
    int lineno;
    int counter;
} Line;

typedef struct {
    int capacity;
    int count;
    Line* lines;
} LineArray;

void initLineArray(LineArray* array);
void freeLineArray(LineArray* array);

void incrementLineCounter(LineArray* array, int line);
int getLine(LineArray* array, int index);

#endif
