package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
    final String name;
    final List<LoxClass> superclasses;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> getters;
    private final Map<String, LoxFunction> statics;

    LoxClass(String name,
            List<LoxClass> superclasses,
            Map<String, LoxFunction> methods,
            Map<String, LoxFunction> getters,
            Map<String, LoxFunction> statics) {
        this.name = name;
        this.superclasses = superclasses;
        this.methods = methods;
        this.getters = getters;
        this.statics = statics;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        for (LoxClass superclass : superclasses) {
            LoxFunction result = superclass.findMethod(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    LoxFunction findGetter(String name) {
        if (getters.containsKey(name)) {
            return getters.get(name);
        }
        for (LoxClass superclass : superclasses) {
            LoxFunction result = superclass.findGetter(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    LoxFunction findStaticMethod(String name) {
        if (statics.containsKey(name)) {
            return statics.get(name);
        }
        for (LoxClass superclass : superclasses) {
            LoxFunction result = superclass.findStaticMethod(name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }
}
