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
extern fileBuffer mainLog;
extern fileBuffer chatLog; 
extern fileBuffer securityLog;	
extern int statusThreadRunning;
extern int killThreads;

void MyStrncpy(char *dest,const char *src,size_t destsize);
int lsprintf
	(size_t size,
	 char *buf,
	 char *fmt,
	 ...);
int lvsprintf
	(size_t space,
	 char *dest,
	 char *format,
	 va_list args);
typedef int *intp;
void logEntry(fileBuffer *B,char *str,...);
char *timestamp();
#if WIN32
void usleep(int n);
#endif

#if WIN32
void Start_Winsock();
#endif

void ExitWith(int val);
void error(char *inStr, int err);
extern int totalAllocations;
extern int allocations;
extern size_t allocatedSize;
void *ALLOC(size_t size);
void FREE(void *obj,size_t size);
void CHECK(void *obj,size_t size);
void MEMCPY(void *dest,void *src,size_t siz);
void MEMMOVE(void *dest,void *src,size_t siz);
