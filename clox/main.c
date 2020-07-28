#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(int argc, const char* argv[])
{
    initVM();

    Chunk chunk;
    initChunk(&chunk);

    for (int i = 0; i < 100; ++i) {
        int constant = addConstant(&chunk, 1.0);
        writeChunk(&chunk, OP_CONSTANT, 123);
        writeChunk(&chunk, constant, 123);
    }
    for (int i = 0; i < 99; ++i) {
        writeChunk(&chunk, OP_ADD, 123);
    }
    writeChunk(&chunk, OP_RETURN, 123);

    disassembleChunk(&chunk, "test chunk");
    interpret(&chunk);
    freeVM();
    freeChunk(&chunk);
    return 0;
}
