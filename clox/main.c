#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[])
{
    Chunk chunk;
    initChunk(&chunk);

    for (int i = 0; i < 260; ++i) {
        writeConstant(&chunk, 1.2, i);
    }
    writeChunk(&chunk, OP_RETURN, 260);

    disassembleChunk(&chunk, "test chunk");
    freeChunk(&chunk);
    return 0;
}
