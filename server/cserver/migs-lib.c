/*
	Copyright 2006-2023 by Dave Dyer

	This file is part of the Boardspace project.

	Boardspace is free software: you can redistribute it and/or modify it under the terms of
	the GNU General Public License as published by the Free Software Foundation,
	either version 3 of the License, or (at your option) any later version.

	Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
	without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
	See the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with Boardspace.
	If not, see https://www.gnu.org/licenses/.
 */
#include "migs-declarations.h"

#if WIN32
static int Winsock_Started=0;
void Start_Winsock()
{
  static WSADATA data;
  unsigned short version = MAKEWORD(2,0);
  int val = WSAStartup(version,&data);
  if(val==0) {  Winsock_Started=1; }
  else { 
	  error("WSAStartup error ",val); 
  }
}

#endif

void ExitWith(int v)
{
  static int once = 0;
  killThreads=TRUE;
  usleep(2*THREADSLEEP_US);
  if(once++==0) { flushLog(&mainLog); flushLog(&chatLog); flushLog(&securityLog); }
#if WIN32
	if(Winsock_Started) { WSACleanup(); }
#endif
	exit(v);
}


void    error(char *inStr, int err) {       /* reports fatal errors */
  logEntry(&mainLog, "[%s] %s %d.\n", timestamp(),inStr, err);
#if HISTORY
  DumpHistory();
#endif
  ExitWith(1);
}


// global variables and structures
fileBuffer mainLog;	//@global | mainLog | the main log file
fileBuffer chatLog; //@global | chatLog | log of all chat activity
fileBuffer securityLog;	//@global | securityLog | log of security and bug related events
int statusThreadRunning=FALSE;
int killThreads = FALSE;

void MyStrncpy(char *dest,const char *src,size_t destsize)
{	assert((dest!=NULL) && (src!=NULL) && (destsize>0));
	dest[0]=(char)0;				// default to a null in the buffer
	strncpy(dest,src,destsize);		// copy
	dest[destsize-1]=(char)0;		// make sure it's terminated
}

static void fmtbug(int space, char *format, char *tmpfmt)
{	char tmpbuf[30];
	MyStrncpy(tmpbuf,format,sizeof(tmpbuf));
	logEntry(&securityLog,"lvsprintf: buffer size %d exceeded, format \"%s\" at %s",space,tmpbuf,tmpfmt);
}

int lvsprintf_err
	(BOOLEAN fatal,
	 int space,	//the buffer size
	 char *dest,	//the buffer
	 char *format,	//the format specifier
	 va_list args)	//variable arg list
{	char *start_format = format;
    char *buffp = dest;
 	int spaceleft = space-1;	//space left
    char c;
    char *tp;
    char tempfmt[64];		//temporary format
	char tempbuf[64];		//temporary printing
#ifndef LONGINT
    int longflag;
#endif

#ifndef _DEBUG
	if(space<=4) 
	  { logEntry(&securityLog,"Buffer too small - possibly (sizeof *char)"); 
	  }
#endif

    tempfmt[0] = '%';
    while((spaceleft>0) && ((c = *format++)!=0) )
	{
	if(c=='%') {
	    tp = &tempfmt[1];
#ifndef LONGINT
	    longflag = 0;
#endif
continue_format:
	    switch(c = *format++) {
		case 's':
		    *tp++ = c;
		    *tp = '\0';
			{ char * str = va_arg(args, char *);
			if(str)
			{
			  if(((int)strlen(str))>spaceleft) 
			  { if(fatal) {fmtbug(space,start_format,tempfmt); }
				spaceleft=0;
			  }
			  else
			  {
			  int len = sprintf(buffp,tempfmt,str);
			  {buffp += len;
			   spaceleft -= len;
			  }
			  }
			}
			}
		    break;
		case 'u':
		case 'x':
		case 'o':
		case 'X':
#ifdef UNSIGNEDSPECIAL
		    *tp++ = c;
		    *tp = '\0';
#ifndef LONGINT
		    if(longflag)
			{int len = sprintf(tempbuf, tempfmt, va_arg(args, unsigned int));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			 else
			 {
			 strcpy(buffp,tempbuf)
			 buffp += len;
			 spaceleft -= len;
			 }
			}
		    else
#endif
			{
			int len = sprintf(tempbuf, tempfmt, va_arg(args, unsigned));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf)
			buffp += len;
			spaceleft -= len;
			}}
		    break;
#endif
		case 'd':
		case 'c':
		case 'i':
		    *tp++ = c;
		    *tp = '\0';
#ifndef LONGINT
		    if(longflag)
			{int len = lsprintf(sizeof(tempbuf),tempbuf, tempfmt, va_arg(args, int));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt);}
			 spaceleft=0; 
			}
			else
			{
			MyStrncpy(buffp,tempbuf,spaceleft);
			buffp += len;
			spaceleft -= len;
			}}
		    else
#endif
			{
			int len = sprintf(tempbuf,tempfmt, va_arg(args, int));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); }
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case 'f':
		case 'e':
		case 'E':
		case 'g':
		case 'G':
		    *tp++ = c;
		    *tp = '\0';
			{int len = lsprintf(sizeof(tempbuf),tempbuf, tempfmt, va_arg(args, double));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case 'p':
		    *tp++ = c;
		    *tp = '\0';
			{int len = lsprintf(sizeof(tempbuf),tempbuf, tempfmt, va_arg(args, void *));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case '-':
		case '+':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case '.':
		case ' ':
		case '#':
		case 'h':
		    *tp++ = c;
		    goto continue_format;
		case 'l':
#ifndef LONGINT
		    longflag = 1;
		    *tp++ = c;
#endif
		    goto continue_format;
		case '*':
		    tp += sprintf(tp, "%d", va_arg(args, int));
		    goto continue_format;
		case 'n':
		    *va_arg(args, intp) = (int)(buffp - dest);
		    break;
		case '%':
		default:
			if(spaceleft<=0) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  
			}
			else
			{
		    *buffp++ = c;
			spaceleft--;
			}
		    break;
	    }
	} 
	else
	{ 
	  if(spaceleft<=0) 
		  { if(fatal) {fmtbug(space,start_format,tempfmt); }
	  }
	  else {
		  *buffp++ = c;
		   spaceleft--;
	  }
	}
    }
	if(spaceleft<0) 
		{ if(fatal) {fmtbug(space,start_format,tempfmt); }}
	*buffp = '\0';
    return (int)(buffp - dest);
}

/* @func
This is vsprintf with the addition of a buffer length argument, so you won't crash
or generate unpredictable results if you accidentally exceed the size of the buffer.
<nl>Overview: <l Printing>
*/
int lvsprintf
	(size_t space,	//@parm the buffer size
	 char *dest,	//@parm the buffer
	 char *format,	//@parm the format specifier
	 va_list args)	//@parm variable arg list
{	return(lvsprintf_err(TRUE,space,dest,format,args));
}

/* @func
This is sprintf with the addition of a buffer length argument, so you won't crash
or generate unpredictable results if you accidentally exceed the size of the buffer.
<nl>Overview: <l server>
*/
int lsprintf
	(size_t size,	//@parm the buffer size
	 char *buf,	//@parm the buffer
	 char *fmt,	//@parm the format string
	 ...)		//@parmvar the arguments
{	int rval = 0;
	va_list lst;
	va_start(lst,fmt);
	rval = lvsprintf(size,buf,fmt,lst);
	va_end(lst);
	assert((size>0) && (buf!=NULL) && (fmt!=NULL));
	return(rval);
}


/* @func
add something to the log buffer.  If the emptier isn't
in a critical section, check and possible reset the buffer
back to empty 

Note that the log filler is single threaded, and only competes
with the emptier thread.  The filler's thread will never block,
but might stop logging if the emptier can't catch up
<nl>Overview: <l server>
*/

void logEntry
	(fileBuffer *B,	//@parm the <t fileBuffer> structure
	char *str,		//@parm the printf control string
	...)			//@parm variable | args | printf arguments
{	va_list args;
	va_start(args,str);
	B->writeLock=TRUE;			// exclude the emptier
	assert(B->logXtend==0);
	if(!B->flushLock)			// unless the emptier is busy
	{	//only attempt this if we're the only one in.  If the emptier
		//has caught up, then reset the buffer to start over
		if(B->logTake==B->logIndex) 
		{ B->logTake=0;B->logIndex=0;
		  if(B->logBreak>0) 
			{ int idx = lsprintf(sizeof(B->logBuffer)-B->logIndex,B->logBuffer+B->logIndex,"<<break in log, %d entries missing>>\n",B->logBreak); 
			  assert((idx>=0) && ((idx+B->logIndex)<sizeof(B->logBuffer)));
			  B->logIndex += idx;
			  B->logBreak=0; 
			}
 
		}
	}
	B->writeLock=FALSE;	// let the emptier back in

	if((B->logIndex+LOGSLOP)<sizeof(B->logBuffer))
	{ int idx =  vsprintf(B->logBuffer+B->logIndex,str,args);
	  assert((B->logXtend==0)&&(idx>=0) && ((idx+B->logIndex)<sizeof(B->logBuffer)));
	  B->logIndex += idx;
	  if(!statusThreadRunning) { flushLog(B); }
	}
	else
	{ B->logBreak++;	//log buffer is full
	}
}

#if WIN32
void usleep(int n)
{ Sleep(n/1000);
}
#endif
// Print GMT date and time into a string
int ptime(char *str,int ss)
{	time_t now = time(NULL);
	struct tm *temp=gmtime(&now);
	char *timestr = asctime(temp);	//get 26 character time
									//Wed Jan 02 02:03:55 1980\n\0
	timestr[19]=(char)0;
	return(lsprintf(ss,str,&timestr[4]));
}
/* @func
return a static timestamp string.  Time is in GMT
<nl>Overview: <l server>
*/
char *timestamp()
{	static char buf[30];
	ptime(&buf[0],sizeof(buf));
	return(&buf[0]);
}
size_t allocatedSize = 0;
int allocations = 0;
int totalAllocations = 0;

// used to check the integrety of the heap
typedef struct headGuard
{	int data;
	size_t size;
} headGuard;
#define GUARDINT 0x5927531e

void *ALLOC(size_t size)
{	unsigned char *newptr = malloc(size+2*sizeof(headGuard));
	if(newptr==NULL)
	{
		error("allocation failed",errno);
	}
	{
	headGuard *guard = (headGuard *)newptr;
	unsigned char *main = newptr+sizeof(headGuard);
	headGuard *tail = (headGuard *)(main+size);
	guard->size = size;
	guard->data = GUARDINT;
	tail->size = size;
	tail->data = GUARDINT;
	allocations++;
	totalAllocations++;
	allocatedSize += size;
	if(logging>=log_all)
			{
				logEntry(&mainLog,"[%s] allocate #%d-%d %d : %x\n",
							timestamp(),
							totalAllocations,
							allocations,
							size,
							main);
			}
	return(main);
	}
}
void CHECK(void *obj,size_t size)
{	if(obj!=NULL)
	{
	headGuard *head = (headGuard *)((unsigned char *)obj-sizeof(headGuard));
	headGuard *tail = (headGuard *)((unsigned char *)obj+size);
	assert((head->size==size) && (head->data==GUARDINT));
	assert((tail->size==size) && (tail->data==GUARDINT));
	}
}
void FREE(void *obj,size_t size)
{	if(obj!=NULL)
	{
	headGuard *head = (headGuard *)((unsigned char *)obj-sizeof(headGuard));
	headGuard *tail = (headGuard *)((unsigned char *)obj+size);
	assert((head->size==size) && (head->data==GUARDINT));
	assert((tail->size==size) && (tail->data==GUARDINT));
	allocations--;
	allocatedSize -= size;
	if(logging>=log_all)
			{
				logEntry(&mainLog,"[%s] deallocate #%d %d : %x\n",
							timestamp(),
							allocations,
							size,
							obj);
			}
	free(head);
	}
}

void MEMCPY(void *dest,void *src,size_t siz)
{	assert((dest!=NULL) && (src!=NULL) && (siz>0) && ((char *)dest<(char*)src ? ((char*)dest+siz)<=(char*)src : ((char*)src+siz)<=(char*)dest));
	memcpy(dest,src,siz);
}

void MEMMOVE(void *dest,void *src,size_t siz)
{	assert((dest!=NULL) && (src!=NULL) && (siz>0));
	memmove(dest,src,siz);
}
