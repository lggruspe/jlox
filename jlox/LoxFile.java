package com.craftinginterpreters.lox;

import java.io.Closeable;
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

    void close() {
        Closeable file = (Closeable)(this.file);
        try {
            file.close();
        } catch (IOException error) {
        }
    }

    String read() {
        if (!(file instanceof FileReader)) {
            return "";
        }
        FileReader reader = (FileReader)file;
        StringBuilder sb = new StringBuilder();
        try {
            for (int c = reader.read(); c != -1; c = reader.read()) {
                sb.append((char)c);
            }
        } catch (IOException error) {
        }
        return sb.toString();
    }

    void write(String text) {
        if (!(file instanceof FileWriter)) {
            return;
        }
        FileWriter writer = (FileWriter)file;
        try {
            writer.write(text, 0, text.length());
        } catch (IOException error) {
        }
    }
}
