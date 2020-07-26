OBJECTS = $(patsubst %.c,%.o,$(filter-out main.c,$(shell ls *.c)))

main:	main.o $(OBJECTS)
	gcc -g $^ -o $@

$(OBJECTS):	%.o:	%.c %.h
	gcc -g -c $< -o $@

main.o:	main.c
	gcc -g -c $< -o $@

clean:
	rm -rf $(OBJECTS) main main.o
