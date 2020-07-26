OBJECTS = $(patsubst %.c,%.o,$(shell ls *.c))

main:	$(OBJECTS)
	gcc -g $^ -o $@

$(OBJECTS):	%.o:	%.c
	gcc -g -c $< -o $@

clean:
	rm -rf $(OBJECTS) main
