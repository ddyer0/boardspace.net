/* utils.h
 *
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995  Erik Van Riper (geek@imageek.york.cuny.edu)
    and John Tromp (tromp@daisy.uwaterloo.ca/tromp@cwi.nl)

    Adapted from:
    fics - An internet chess server.
    Copyright (C) 1993  Richard V. Nash

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

#ifndef UTILS_H
#define UTILS_H

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>	/* for FILE */
#include <stdarg.h>	/* for va_list varargs */

#ifdef HAVE_SYS_STAT_H
#include <sys/stat.h>   /* struct stat */
#endif

#ifdef HAVE_TIME_H
#include <time.h>
#endif

#ifdef HAVE_DIRENT_H
#include <dirent.h>	/* DIR, struct dirent */
#endif

#include "bm.h"
#include "language.h"
#include "formats.h"
#include "files.h"

#define MAX_WORD_SIZE 1024

/* Maximum length of an output line */
#define MAX_LINE_SIZE 1024

/* Maximum size of a filename */
#ifdef FILENAME_MAX
#  define MAX_FILENAME_SIZE FILENAME_MAX
#else
#  define MAX_FILENAME_SIZE 1024
#endif
	/* AvK: macro to get number of elements in array
	** The cast is to suppress "comparing int to unsigned"
	** warnings :-[
	*/
#define COUNTOF(a) (int)(sizeof(a)/sizeof(a)[0]) 

	/* AvK: Usage: printf("%s", IFNULL(cp, "Not Provided")); */
#define IFNULL(a,b) (a) ? (a) : (b)
	/* For arrays: this replaces the test by a[0] */
#define IFNULL0(a,b) ((a)&&(a)[0]) ? (a) : (b)


extern int iswhitespace(int);
extern char *getword(char *);
/* Returns a pointer to the first whitespace in the argument */
extern char *eatword(char *);
/* Returns a pointer to the first non-whitespace char in the argument */
extern char *eatwhite(char *);
/* Returns the next word in a given string >eatwhite(eatword(foo))< */
extern char *nextword(char *);

extern int mail_string_to_address(const char *, const char *, const char *);
extern int mail_string_to_user(int, char *);
extern int pcommand(int, const char *, ...);
extern int pprintf(int, const char *, ...);
extern int cpprintf(int, int, const char *, ...);
extern int pprintf2(int, int, const char *);
extern int Logit(const char *, ...);
extern int pprintf_prompt(int, const char *, ...);
extern int cpprintf_prompt(int, int, const char *, ...);
extern int my_vsnprintf(char *, size_t, const char *, va_list);
extern int my_snprintf(char *, size_t, const char *, ...);
#if (!HAVE_VSNPRINTF)
#define vsnprintf my_vsnprintf
#endif
#if (!HAVE_SNPRINTF)
#define snprintf my_snprintf
#endif
extern int psend_raw_file(int, const char *, const char *);
extern int psend_file(int, const char *, const char *);
extern int pxysend_raw_file(int, int, ...);
extern int pxysend_file(int p, int num, ...);
extern int pmore_file(int);
extern int pmail_file(int, const char *, const char *);
/* extern int psend_command(int, const char *, char *); */
extern int xpsend_command(int, const char *, char *, int num, ...);

extern char *stolower(char *);
extern char *stoupper(char *);

extern int safechar(int);
extern int safestring(const char *);
extern int safefilename(const char *path);
extern int alphastring(const char *);
extern int printablestring(const char *);
extern char *mystrdup(const char *);
extern char * mycrypt(const char *passwd, const char * salt);

extern char *hms(int, int, int, int);
extern char *newhms(int);
extern char *strhms(int);

extern char *DTdate(const struct tm *);
extern char *strDTtime(const time_t *);
extern char *ResultsDate(char *);
extern char *strltime(const time_t *);
extern char *strgtime(const time_t *);
extern char *strtime_file(const time_t *);

/* extern unsigned read_tick(void); */
extern char *tenth_str(unsigned int, int);
extern int untenths(unsigned int);
extern int do_copy(char *, const char *, int);

extern int truncate_file(char *, int);

extern int file_has_pname(const char *, const char*);
extern const char *file_wplayer(const char *);
extern const char *file_bplayer(const char *);

extern int xyfilename(char *dst, int num, ...);
extern FILE * xfopen(const char *,const char *);
extern FILE * xyfopen(int num, const char *, ...);
extern FILE * pxyfopen(int p, int num, const char *, ...);
extern int xyrename(int num1, int num2, ...);
extern int xylines_file(int num, ...);
extern int xylink(int num, ...);
extern int xyunlink(int num, ...);
extern int xystat(struct stat * sb, int num, ...);
extern int xytouch(int num, ...);
extern DIR * xyopendir(int num, ...);
extern char * filename(void);
extern int pcn_out(int, int, int, ...);
extern int pcn_out_prompt(int p, int code, int num, ...);

extern char *dotQuad(unsigned int);
int asc2ipaddr(char *str, unsigned *add);

extern int available_space(void);
extern int file_exists(char *);

extern int search_directory(char *, int, char *, int, ...);
extern int display_directory(int, const char *, int);
extern const char *SendCode(int, int);
extern char *KillTrailWhiteSpace(char *str);
extern char *strlwr(char *psz);
extern void bldsearchdata(char *psz);

int parse_rank(int num, int ch);

#ifndef TICSPERSEC
#define TICSPERSEC (10)
#define SECS2TICS(s) ((s)*TICSPERSEC)
#define TICS2SECS(t) ((t)/TICSPERSEC)
#endif

struct ticker {
  time_t time;
  unsigned tick;
  };
extern struct ticker globclock;
extern unsigned refetch_ticker(void);

#define SDATA_USER 1
#define SDATA_HOST 2

struct searchdata {
  int where;
  struct boyermoore bmData;
} ;

struct searchresult {
  char szPlayer[100];
  char szMailAddr[200];
} ;

extern const struct searchresult *search(char *);
extern int blank(char *);

#endif /* UTILS_H */
