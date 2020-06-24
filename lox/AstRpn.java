package com.craftinginterpreters.lox;

class AstRpn implements Expr.Visitor<String> {
    String polish(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return String.format("%s %s %s %s ",
                polish(expr.left),
                polish(expr.middle),
                polish(expr.right),
                "?:");
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return String.format("%s %s %s ",
                polish(expr.left),
                polish(expr.right),
                expr.operator.lexeme);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return polish(expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return String.format("%s ",
                expr.value == null ? "nil" : expr.value.toString());
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        String operator = expr.operator.lexeme;
        if (operator == "-") {
            operator = "~";
        }
        return String.format("%s %s ", polish(expr.right), operator);
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                    new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(123)
                    ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                    new Expr.Literal(45.67)
                    )
                );
        System.out.println(new AstRpn().polish(expression));
    }
}
