package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass cls;

    LoxInstance(LoxClass cls) {
        this.cls = cls;
    }

    @Override
    public String toString() {
        return cls.name + " instance";
    }
}
