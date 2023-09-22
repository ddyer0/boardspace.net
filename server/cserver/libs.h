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
#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/timeb.h>

#if WIN32
#include <process.h>
#define snprintf _snprintf
#else
#include <stdarg.h>
#include <pthread.h>
#endif
#include <string.h>
#include <stdarg.h>

/* This must be a typedef not a #define! */
#ifdef NOVOID
typedef char *pointer;
#else
typedef void *pointer;
#endif

typedef int *intp;
typedef int BOOL;
#define FALSE 0
#define TRUE -1
typedef unsigned char BYTE;

void FatalError(char *msg,...); 
void BugMsg(char *file,long line,char *msg,...);
#define BUG(msg) BugMsg(__FILE__,__LINE__,msg)
#define BUG1(msg,arg) BugMsg(__FILE__,__LINE__,msg,(long)arg)
#define BUG2(msg,arg1,arg2) BugMsg(__FILE__,__LINE__,msg,(long)arg1,(long)arg2)
#define BUG3(msg,arg1,arg2,arg3) BugMsg(__FILE__,__LINE__,msg,(long)arg1,(long)arg2,(long)arg3)
#define BUG4(msg,arg1,arg2,arg3,arg4) BugMsg(__FILE__,__LINE__,msg,(long)arg1,(long)arg2,(long)arg3,(long)arg4)

BOOL MyStrncpy(char *dst,const char *src,long num);
int lvsprintf(long space,char *dest,char *format,va_list args);
long lsprintf(long size,char *buf,char *fmt, ...);
