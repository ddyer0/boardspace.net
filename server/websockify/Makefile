TARGETS=websockify echoserver
CFLAGS += -fPIC

all: $(TARGETS)

echoserver: echo.c
	$(CC) $(LDFLAGS) $^ -lssl -lcrypto -o $@

websockify: websockify.o websocket.o
	$(CC) $(LDFLAGS) $^ -lssl -lcrypto -o $@

websocket.o: websocket.c websocket.h
websockify.o: websockify.c websocket.h

clean:
	rm -f websockify *.o echoserver

