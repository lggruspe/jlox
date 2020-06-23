all:	com

com:	*.java
	javac $^ -d .

.PHONY:	lox
lox:	com
	java com.craftinginterpreters.lox.Lox

.PHONY:	clean
clean:
	rm -rf com
