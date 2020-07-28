#ifndef clox_stack_h
#define clox_stack_h

#include "common.h"
#include "value.h"

typedef struct {
    int count;
    int capacity;
    Value* table;
} VMStack;

void initVMStack(VMStack* stack);
void freeVMStack(VMStack* stack);
void pushDynamic(VMStack* stack, Value value);
Value popDynamic(VMStack* stack);

#endif
