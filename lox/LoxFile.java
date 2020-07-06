package com.craftinginterpreters.lox;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

class LoxFile {
    enum FileMode { READ, WRITE } 

    private final String path;
    private final FileMode mode;
    private final Object file;

    LoxFile(String path, FileMode mode) throws FileNotFoundException, IOException {
        this.path = path;
        this.mode = mode;
        file = mode == FileMode.WRITE
            ? new FileWriter(path)
            : new FileReader(path);
    }

    String read(Token token, String text) throws IOException {
        if (!(file instanceof FileReader)) {
            throw new RuntimeError(token,
                    "Cannot read from file '" + path + "'.");
        }
        FileReader reader = (FileReader)file;
        StringBuilder sb = new StringBuilder();
        for (int c = reader.read(); c != -1; c = reader.read()) {
            sb.append((char)c);
        }
        return sb.toString();
    }

    void write(Token token, String text) throws IOException {
        if (!(file instanceof FileWriter)) {
            throw new RuntimeError(token,
                    "Cannot write to file '" + path + "'.");
        }
        FileWriter writer = (FileWriter)file;
        writer.write(text, 0, text.length());
    }
}
