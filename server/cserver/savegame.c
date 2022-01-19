
#define CURRENT_THREAD SAVEGAME
///
// this code normally executes in a separate thread and has to be very careful
// about the global state it uses.  The items in the "write pool" are copies of
// GameBuffers which are not read by anyone else.  The write pool itself if a 
// single writer, single reader ring buffer, where the filling is done by the main
// thread and the emptying by this thread.  Normally gamebuffers are copied to files
// as simple write, but when "hashed" is false, the game is no longer restartable
// and is deleted instead.
// 
// the overall rate of operations is limited by a configuration variable "gameCacheRate"
// so the load on the file system can never get out of control, no matter how busy the
// server becomes.
//
#include "migs-declarations.h"


// file names have lots of forbidden characters.
static void sanitizeName(char *name,char *out,long outsize)
{
	while(outsize-- > 0)
	{
	char ch =*name++;
	switch(ch)
	{
	default: *out++ = ch; break;
	case (char)0: *out++ = ch; return;
	case '\\':
	case ':':
	case '/':
	case '<':
	case '>':
	case '|': *out++ = '-'; break;

	}
	}
	// ran out of buffer
	*out=(char)0;
}


// this is executed in the save_games thread.  Save one game or delete it.
//
static void saveDirtyGameWriteStep(SaveGameInfo *G)
{	long current_index = THREAD_READ(G,GameBuffer_Write_Pool_Emptier);
	if(THREAD_READ(G,GameBuffer_Write_Pool_Filler)!=current_index)
	{
	long next_index = current_index+1;
	if(next_index==ARRAY_NUMBER_OF_ELEMENTS(THREAD_READ(G,GameBuffer_Write_Pool))) 
		{ next_index = 0; 
		}

	// here we save one game
	if(THREAD_READ(G,gameCacheDir) && *THREAD_READ(G,gameCacheDir))
	{	GameBuffer *g = &THREAD_READ(G,GameBuffer_Write_Pool)[current_index];
		char gameName[SMALLBUFSIZE];
		char sanitizedName[SMALLBUFSIZE];
		sanitizeName(g->idString,sanitizedName,sizeof(sanitizedName));
		lsprintf(sizeof(gameName),gameName,"%sG%s." GAMESUFFIX,THREAD_READ(G,gameCacheDir),sanitizedName);
		
		g->magic = GAMEBUFMAGIC;
		
		if(THREAD_READ(g,preserved))
		{	// still hashed, save a new version
		FILE *of = fopen(gameName,"wb");	// read/write access, preserve the original data if crashed now
		if(of!=NULL)
			{
			size_t goff = (size_t)&(((GameBuffer*)0)->gamePtr);	// first part of gamebuffer
			size_t len1 = fwrite(g,1,goff,of);				// write the descriptive data
			size_t len = (len1!=goff) ? 0 : fwrite(g->gamePtr,1,g->gamePtrSize,of);			// write the actual game data
			if(len!=g->gamePtrSize)
			{
			logEntry(&mainLog,"[%s] write for save GameBuffer failed file %s\n",
					timestamp(),gameName);
			THREAD_INCREMENT(G,n_dirty_problems);
			}
			else
			{THREAD_INCREMENT(G,n_dirty_writes);
			}
			fclose(of);
			}
			else
			{
			THREAD_INCREMENT(G,n_dirty_problems);
			logEntry(&mainLog,"[%s] open for save GameBuffer failed file %s\n",
					timestamp(),gameName);
			}
		}
		else
		{	// no longer preserved, delete it
		int val = unlink(gameName);
		if(logging>=log_errors)
		{
			logEntry(&mainLog,"[%s] unlink for %s\n",
					timestamp(),gameName);
		}
		if(val!=0)  
		{
			THREAD_INCREMENT(G,n_dirty_problems);
			logEntry(&mainLog,"[%s] unlink for GameBuffer failed file %s\n",
					timestamp(),gameName);

		}
		}

	}

	THREAD_WRITE(G,GameBuffer_Write_Pool_Emptier,next_index);
	}
}

// this is executed in the save_games thread, or in the main thread
// if we are not using threads
void saveDirtyGamesNow(SaveGameInfo *G)
{	long rate = THREAD_READ(G,gameCacheRate);
	while((rate-- > 0) 
			&& (THREAD_READ(G,GameBuffer_Write_Pool_Emptier)!=THREAD_READ(G,GameBuffer_Write_Pool_Filler)))
	{
		saveDirtyGameWriteStep(G);
	}
}


// 
// as the play progresses and game states change, the main thread marks gamebuffers
// as "dirty" and queues then in a list headed by first_dirty_gamebuffer.  As part
// of a cleanup phase in the even loop, these are copied to a write buffer, which is
// a single filler, single emptier ring.  This function empties the writebuffer.
#if WIN32
void saveDirtyGameThread(void *args)
#else
void *saveDirtyGameThread(void *args)
#endif
{	SaveGameInfo *G = (SaveGameInfo *)args;
	while(!killThreads)
	{
	usleep(THREADSLEEP_US);
	saveDirtyGamesNow(G);
	}
#if !WIN32
	return(0);
#endif

}

