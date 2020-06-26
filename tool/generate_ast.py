from argparse import ArgumentParser
from os.path import join

parser = ArgumentParser("generate_ast")
parser.add_argument("output", help="output directory")
args = parser.parse_args()

def define_type(base_name, class_name, fields):
    definition = f"    static class {class_name} extends {base_name} {{\n"
    definition += f"        {class_name}({fields}) {{\n"

    field_list = fields.split(", ")
    for field in field_list:
        name = field.split(' ')[1]
        definition += f"            this.{name} = {name};\n"
    definition += "        }\n\n"

    definition += "        @Override\n"
    definition += "        <R> R accept(Visitor<R> visitor) {\n"
    definition += f"            return visitor.visit{class_name}{base_name}(this);\n"
    definition += "        }\n\n"

    for field in field_list:
        definition += f"        final {field};\n"
    definition += "    }\n\n"
    return definition

def define_visitor(base_name, types):
    definition = "    interface Visitor<R> {\n"
    for t in types:
        type_name = t.split(':')[0].strip()
        definition += f"        R visit{type_name}{base_name}({type_name} {base_name.lower()});\n"
    definition += "    }\n"
    return definition

def define_ast(output_directory, base_name, types):
    path = join(output_directory, base_name + ".java")
    with open(path, "w") as file:
        print("package com.craftinginterpreters.lox;\n", file=file)
        print("import java.util.List;\n", file=file)
        print("abstract class {} {{".format(base_name), file=file)
        print(define_visitor(base_name, types), file=file)
        body = ""
        for t in types:
            class_name = t.split(':')[0].strip()
            fields = t.split(':')[1].strip()
            body += define_type(base_name, class_name, fields)
        print(body, file=file)
        print("    abstract <R> R accept(Visitor<R> visitor);", file=file)
        print("}", file=file)

define_ast(args.output, "Expr", [
    "Ternary    : Expr left, Expr middle, Expr right",
    "Binary     : Expr left, Token operator, Expr right",
    "Grouping   : Expr expression",
    "Literal    : Object value",
    "Unary      : Token operator, Expr right",
])

define_ast(args.output, "Stmt", [
    "Expression : Expr expression",
    "Print      : Expr expression",
])