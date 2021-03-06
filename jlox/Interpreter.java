package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    static class BreakException extends RuntimeException {}
    static class ContinueException extends RuntimeException {}
    static class RuntimeErrorInFunction extends RuntimeException {
        final String message;
        RuntimeErrorInFunction(String message) {
            this.message = message;
        }
    }

    final Environment globals = new Environment();
    private Environment environment = globals;
    private static final Object uninitialized = new Object();
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(
                    Interpreter interpreter,
                    List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("open", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(
                    Interpreter interpreter,
                    List<Object> arguments) {
                try {
                    String path = (String)(arguments.get(0));
                    LoxFile.FileMode mode = null;
                    switch ((String)(arguments.get(1))) {
                    case "r": mode = LoxFile.FileMode.READ; break;
                    case "w": mode = LoxFile.FileMode.WRITE; break;
                    default:
                        throw new RuntimeErrorInFunction("Invalid file mode.");
                    }
                    return new LoxFile(path, mode);
                } catch (FileNotFoundException error) {
                    throw new RuntimeErrorInFunction("Failed to open file.");
                } catch (IOException error) {
                    throw new RuntimeErrorInFunction("Failed to open file.");
                }
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("close", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(
                    Interpreter interpreter,
                    List<Object> arguments) {
                LoxFile file = (LoxFile)(arguments.get(0));
                file.close();
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("read", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(
                    Interpreter interpreter,
                    List<Object> arguments) {
                LoxFile file = (LoxFile)(arguments.get(0));
                return file.read();
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("write", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(
                    Interpreter interpreter,
                    List<Object> arguments) {
                LoxFile file = (LoxFile)(arguments.get(0));
                String text = (String)(arguments.get(1));
                file.write(text);
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        List<LoxClass> superclasses = new ArrayList<>();
        for (Expr.Variable mixin : stmt.mixins) {
            Object superclass = evaluate(mixin);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(mixin.name,
                        "Superclass must be a class.");
            }
            superclasses.add((LoxClass)superclass);
        }

        environment.define(stmt.name.lexeme, null);

        if (!superclasses.isEmpty()) {
            environment = new Environment(environment);
            // super refers to first superclass
            environment.define("super", superclasses.get(0));

        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment,
                    method.name.lexeme.equals("init"), false);
            methods.put(method.name.lexeme, function);
        }

        Map<String, LoxFunction> getters = new HashMap<>();
        for (Stmt.Function getter : stmt.getters) {
            LoxFunction function = new LoxFunction(getter, environment, false, true);
            getters.put(getter.name.lexeme, function);
        }

        Map<String, LoxFunction> statics = new HashMap<>();
        for (Stmt.Function method : stmt.statics) {
            LoxFunction function = new LoxFunction(method, environment, false, false);
            statics.put(method.name.lexeme, function);
        }

        LoxClass cls = new LoxClass(stmt.name.lexeme, superclasses, methods, getters, statics);

        if (!superclasses.isEmpty()) {
            environment = environment.enclosing;
        }
        environment.assign(stmt.name, cls);
        return null;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = uninitialized;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueException();
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                try {
                    execute(stmt.body);
                } catch (ContinueException error) {
                }
            }
        } catch (BreakException error) {
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object left = evaluate(expr.left);
        return isTruthy(left) ? evaluate(expr.middle) : evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case GREATER:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left > (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return stringCompare(left, right) > 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left >= (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return stringCompare(left, right) >= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left < (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return stringCompare(left, right) < 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left <= (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return stringCompare(left, right) <= 0;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }
        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }
        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    String.format("Expected %d arguments but got %d.",
                        function.arity(),
                        arguments.size()));
        }
        try {
            return function.call(this, arguments);
        } catch (RuntimeErrorInFunction error) {
            throw new RuntimeError(expr.paren, error.message);
        }
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            Object value = ((LoxInstance)object).get(expr.name);
            if (value instanceof LoxFunction) {
                LoxFunction function = (LoxFunction)value;
                if (function.isGetter) {
                    try {
                        value = function.call(this, new ArrayList<Object>());
                    } catch (RuntimeErrorInFunction error) {
                        throw new RuntimeError(expr.name, error.message);
                    }
                }
            }
            return value;
        } else if (object instanceof LoxClass) {
            return ((LoxClass)object).findStaticMethod(expr.name.lexeme);
        }
        throw new RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new LoxLambda(expr, environment);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }
        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superclass = (LoxClass)environment.getAt(
                distance, "super");
        LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");
        LoxFunction method = superclass.findMethod(expr.method.lexeme);
        // how about getters and staticmethods?
        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }
        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        Object value = null;
        if (distance != null) {
            value = environment.getAt(distance, name.lexeme);
        } else {
            value = globals.get(name);
        }

        if (value == uninitialized) {
            throw new RuntimeError(name,
                    "Uninitialized variable '" + name.lexeme + "'.");
        }
        return value;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be a number");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    private int stringCompare(Object left, Object right) {
        return ((String)left).compareTo((String)right);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
}
