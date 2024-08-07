all:	error BoardSpaceServer

error:	error.c
	gcc -g -o error error.c

BoardSpaceServer:	migs.c migs-lib.c savegame.c websocket.c migs-declarations.h migs-lib.h threadmacros.h
	gcc -g -DX64 -DBOARDSPACE -o BoardSpaceServer migs.c migs-lib.c savegame.c websocket.c -lpthread -lcrypto -lssl

Map:	migs.c
	gcc -DX64 -c -o BoardspaceServer.o migs.c
	ld BoardspaceServer.o -M > BoardspaceServer.map -lpthread

#banter.exe:	banner.c
#	gcc -m32 -g -o banter.exe banner.c

