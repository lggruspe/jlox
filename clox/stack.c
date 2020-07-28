#include "stack.h"
#include "memory.h"

void initVMStack(VMStack* stack) {
    stack->count = 0;
    stack->capacity = 0;
    stack->table = NULL;
}

void freeVMStack(VMStack* stack) {
    FREE_ARRAY(Value, stack->table, stack->capacity);
    initVMStack(stack);
}

void pushDynamic(VMStack* stack, Value value) {
    if (stack->capacity < stack->count + 1) {
        int oldCapacity = stack->capacity;
        stack->capacity = GROW_CAPACITY(oldCapacity);
        stack->table = GROW_ARRAY(Value, stack->table, oldCapacity, stack->capacity);
    }
    stack->table[stack->count] = value;
    stack->count++;
}

Value popDynamic(VMStack* stack) {
    stack->count--;
    return stack->table[stack->count];
}
