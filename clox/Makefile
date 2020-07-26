OBJECTS = $(patsubst %.c,%.o,$(shell ls *.c))

main:	$(OBJECTS)
	gcc $^ -o $@

$(OBJECTS):	%.o:	%.c
	gcc -c $< -o $@

clean:
	rm -rf $(OBJECTS) main
