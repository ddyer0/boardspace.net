/* utils.c
 *
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1996 Erik Van Riper (geek@nngs.cosmic.org)
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

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_STRINGS_H
#include <strings.h>
#endif

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_CTYPE_H
#include <ctype.h>
#endif

#ifdef HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif

#ifdef HAVE_TIME_H
#include <time.h>
#endif

#ifdef HAVE_SYS_TIME_H
#include <sys/time.h>
#endif

#ifdef TIME_WITH_SYS_TIME
#include <sys/time.h>
#endif

#ifdef HAVE_UTIME_H
#include <utime.h>
#endif

#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif

#ifdef HAVE_CRYPT_H
#include <crypt.h>
#endif

#include "missing.h"
#include "nngsconfig.h"
#include "nngsmain.h"
#include "utils.h"
#include "common.h"
#include "command.h"
#include "servercodes.h"
#include "playerdb.h"
#include "multicol.h"
#include "network.h"
#include "language.h"


#ifdef SGI
#include <sex.h>
#endif

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif


struct searchdata mysearchdata;
static char filename1[MAX_FILENAME_SIZE];
static char filename2[MAX_FILENAME_SIZE];

static int pcvprintf(int, int, const char *, va_list);

static int mkdir_p(const char *);
static int pprompt(int);
static size_t vafilename(char *buf,int num, va_list ap);
static int lines_file(char *file);
static FILE * vafopen(int num, const char * mode, va_list ap);
static FILE * pvafopen(int p, int num, const char * mode, va_list ap);

int iswhitespace(int c)
{
#if 0
  if ((c < ' ') || (c == '\b') || (c == '\n') ||
      (c == '\t') || (c == ' ')) {	/* white */
    return 1;
  } else {
    return 0;
  }
#else
  /* PEM's whitespace. Note that c may be < 0 for 8-bit chars. */
  /* Another note:  Some code depend on c == '\0' being whitespace. /pem */
  return (((0 <= c) && (c <= ' ')) || (c == 127));
#endif
}

char *KillTrailWhiteSpace(char *str)
{
  int len, stop;
  stop = 0;
  len = strlen(str);

  while (!stop) {
    if (iswhitespace(str[len - 1])) {
      str[len - 1] = '\0';
      len--;
    }
    else stop = 1;
  }
  return str;
}

char *getword(char *str)
{
  int i;
  static char word[MAX_WORD_SIZE];

  i = 0;
  while (*str && !iswhitespace(*str)) {
    word[i] = *str;
    str++;
    i++;
    if (i == sizeof word) {
      i = i - 1;
      break;
    }
  }
  word[i] = '\0';
  return word;
}

/* This code defines the TYPE of message that is to be sent by adding
   a prefix to the message. */
const char *SendCode(int p, int Code)
{
  static char word[MAX_WORD_SIZE];

  if (!parray[p].client) {
    switch(Code) {
      case CODE_SHOUT: strcpy(word, "\n");
                  break;
      default: word[0]= 0;
                  break;
    }
    return word;
  }

  switch(Code) {
    case CODE_MOVE:
    case CODE_INFO: sprintf(word, "%d ", Code);
               break;
    case CODE_SHOUT: sprintf(word, "\n%d ", Code);
               break;
    default: sprintf(word, "%d ", Code);
               break;
  }
  return word;
}

char *eatword(char *str)
{
  while (*str && !iswhitespace(*str))
    str++;
  return str;
}

char *eatwhite(char *str)
{
  while (*str && iswhitespace(*str))
    str++;
  return str;
}

char *nextword(char *str)
{
  return eatwhite(eatword(str));
}

int mail_string_to_address(const char *addr, const char *subj, const char *str)
{
  char com[1000];
  FILE *fp;

  if (!safestring(addr))
    return -1;
  sprintf(com, "%s -s \"%s\" %s", MAILPROGRAM, subj, addr);
  Logit("Mail command: %s",com);
  fp = popen(&com[0], "w");
  if (!fp)
    return -2;
  fprintf(fp, "From: %s\n", server_email);
  fprintf(fp, "%s", str);
  pclose(fp);
  return 0;
}

int mail_string_to_user(int p, char *str)
{

  if (parray[p].email[0]) {
    return mail_string_to_address(parray[p].email, "NNGS game report", str);
  } else {
    return -1;
  }
}

/* Process a command for a user */
int pcommand(int p, const char *comstr, ...)
{
  va_list ap;
  char tmp[MAX_LINE_SIZE];
  int retval;
  int fd = parray[p].socket;

  va_start(ap, comstr);
  vsnprintf(tmp, sizeof tmp, comstr, ap);
  va_end(ap);

  retval = process_input(fd, tmp);
  if (retval == COM_LOGOUT) {
    process_disconnection(fd);
    net_close(fd);
  }
  return retval;
}

int Logit(const char *format,...)
{
  va_list ap;
  FILE *fp;
  char fname[MAX_FILENAME_SIZE];
  char tmp[10 * MAX_LINE_SIZE];	/* Make sure you can handle 10 lines worth of
				   stuff */
  int retval;
  time_t time_in;
  static int in_logit=0;

#ifdef DOKILL
  return 0;
#endif

  time_in = globclock.time;
  va_start(ap, format);

  retval = vsprintf(tmp, format, ap);
  if (strlen(tmp) >= sizeof tmp) {
    fprintf(stderr, "Logit buffer overflow (format=\"%s\")\n", format);
    tmp[sizeof tmp -1] = 0;
  }
  switch(port) {
  case 9696:
    sprintf(fname, "%s/%s", stats_dir, log_file);
    break;
  default:
    sprintf(fname, "%s/%s%d", stats_dir, log_file, port);
    break;
  }
	/* Terminate the mutual recursion Logit() <--> xfopen()
	** (this only happens if the logfile does not exist)
	*/
  if (in_logit++) {
     /* fprintf(stderr, "In_logit(recursion(%d)) '%s'\n",in_logit, fname); */
  } else if ((fp = xfopen(fname, "a")) == NULL) {
     int err;
     err=errno;
     fprintf(stderr, "Error opening logfile '%s': %d(%s)\n"
       ,fname, err, strerror(err) );
  } else  {
     fprintf(fp, "%s %s", tmp, asctime(localtime(&time_in)));
     fflush (fp);
     fclose(fp);
  }
  in_logit--;
  va_end(ap);
  return retval;
}


int pprintf(int p, const char *format, ...)
{
  va_list ap;
  char tmp[10 * MAX_LINE_SIZE];	/* Make sure you can handle 10 lines worth of
				   stuff */
  int retval;
  size_t len;
  va_start(ap, format);
	/* AvK: the strlen could be avoided
	** if we could trust stdlib on all platforms 
	** (the printf() family _is_supposed_ to return 
	** the strlen() of the resulting string, or -1 on error )
	*/
  retval = vsprintf(tmp, format, ap);
  if ((len=strlen(tmp)) >= sizeof tmp) {
    Logit("pprintf buffer overflow");
    len=sizeof tmp -1; tmp[len] = 0;
  }
  net_send(parray[p].socket, tmp, len);
  va_end(ap);
  return retval; /* AvK: should be equal to len, but is always ignored anyway */
}

static int pcvprintf(int p, int code, const char *format, va_list ap)
{
  char bigtmp[10 * MAX_LINE_SIZE];

  int rc, len;
  int idx=0;

#if SUPPRESS_SYMPTOMS /* :-) */
  memset(bigtmp,0,sizeof bigtmp);
#endif

	/* A leading \n can be printed *before* the sendcode */
  if (code & CODE_CR1) {
    code &= ~CODE_CR1;
    idx = sprintf(bigtmp,"\n");
  }
  if (code) {
    const char *cp;
    cp= SendCode(p, code);
    len = strlen(cp);
    memcpy(bigtmp+idx,cp,len);
    idx += len;
/* debugging info
    fprintf(stderr,"format='%s'",format);
    fprintf(stderr,"cp='%s' idx=%d bigtmp='%s'\n",cp,idx,bigtmp);
*/
  }

  rc = vsnprintf(bigtmp+idx,sizeof bigtmp -idx, format, ap);

  if (rc < 0) {
    Logit("pcvprintf buffer overflow code==%d, format==\"%s\"",code,format);
    len=sizeof bigtmp -1; bigtmp[len] = 0;
    strcpy(NULL, "myabort()" );
  }
  else len = idx+rc;

  net_send(parray[p].socket, bigtmp, len);

  return len;
}

#if FUTURE
int sncpprintf(char *buff, size_t bufflen, int p, int code, const char *format, ...)
{
  va_list ap;
  int retval;

  va_start(ap, format);

  retval = vsnprintf(buff, bufflen, format, ap);

  va_end(ap);
  return retval;
}
#endif

#if (!HAVE_VSNPRINTF)
/* this is a simple, robust (and clumsy ...)
 * substitution for the [v]snprintf() functions, which
 * still seem to be absent on some systems.
 * We use a temp file, which cannot cause any buffer-overrun.
 * The trick of unlinking the file immediately after creation
 * is, of course, a unixism. Other platforms may lose.
 * We keep the file/inode open all the time and rewind it each
 * time it is used.
 *
 * Don't complain about performance, instead upgrade your libc.
 * A better, but very big implementation can be found in the
 * Apache sources.
 */
int my_vsnprintf(char *dst, size_t siz, const char *format, va_list ap)
{
  static FILE * dummy = NULL;
  int len;


  if (!dummy) {
    char *name;
    name = tempnam(NULL, NULL);
    dummy = fopen(name, "w+");
    if (!dummy) Logit("Could not open tempfile '%s'", name);
    unlink(name);
  }
  rewind(dummy);
  len = vfprintf(dummy, format, ap);
  fflush(dummy);
  if (len >= siz) { *dst = 0; return -1; }
  rewind(dummy);
  len = fread(dst, 1, (size_t) len, dummy);
  dst[len] = 0;
  return len;
}
#endif

#if (!HAVE_SNPRINTF)
int my_snprintf(char *dst, size_t siz, const char *format, ... )
{
  va_list ap;
  int rc;

  va_start(ap, format);
  rc = vsnprintf(dst, siz, format, ap);
  va_end(ap);

  return rc;
}
#endif

int cpprintf(int p, int code, const char *format, ...)
{
  va_list ap;
  int retval;

  va_start(ap, format);

  retval = pcvprintf(p, code, format, ap);

  va_end(ap);
  return retval;
}


int pprintf_prompt(int p, const char *format,...)
{
  va_list ap;
  char tmp[10 * MAX_LINE_SIZE];	/* Make sure you can handle 10 lines worth of
				   stuff */
#if 0
  char tmp2[100];
#endif
  int retval;
  size_t len;

  va_start(ap, format);

  retval = vsprintf(tmp, format, ap);
  if ((len = strlen(tmp)) >= sizeof tmp) {
    Logit("pprintf_prompt buffer overflow");
    len =sizeof tmp -1; tmp[len] = 0;
  }
  net_send(parray[p].socket, tmp, len);

  pprompt(p);
  va_end(ap);
  return retval;
}

int cpprintf_prompt(int p, int code, const char *format,...)
{
  va_list ap;
  int retval;

  va_start(ap, format);

  retval = pcvprintf(p, code, format, ap);

  pprompt(p);

  va_end(ap);
  return retval;
}

static int pprompt(int p)
{
  char tmp[MAX_LINE_SIZE];
  int len=0;

  if (parray[p].client) {
    len=sprintf(tmp, "%d %d\n", CODE_PROMPT, parray[p].protostate);
  }
  else if (parray[p].protostate == STAT_SCORING) {
    len=sprintf(tmp,"Enter Dead Group: "); 
  } else {
    if (parray[p].extprompt) {
      len=sprintf(tmp, "|%s/%d%s| %s ", 
        parray[p].last_tell >= 0 ? parray[parray[p].last_tell].pname : "",
        parray[p].last_channel, 
        parray[p].busy[0] ? "(B)" : "",
        parray[p].prompt);
    }
    else len=sprintf(tmp, "%s",parray[p].prompt);
  }
 if (len>0) net_send(parray[p].socket, tmp, len);
  return len;
}


static int
is_regfile(char *path)
{
  struct stat sbuf;

  if (stat(path, &sbuf) < 0)
    return 0;
  else
    return S_ISREG(sbuf.st_mode);
}

int psend_raw_file(int p, const char *dir, const char *file)
{
  FILE *fp;
  char tmp[MAX_LINE_SIZE * 2];
  char fname[MAX_FILENAME_SIZE];
  int num;

  if (dir) sprintf(fname, "%s/%s", dir, file);
  else     strcpy(fname, file);

  if (!is_regfile(fname)) return -1;

  fp = xfopen(fname, "r");
  if (!fp)
  {
    fprintf(stderr,"psend_raw_file: File '%s' not found!\n",fname);
    return -1;
  }
  while ((num = fread(tmp, sizeof(char), sizeof tmp, fp)) > 0) {
    net_send(parray[p].socket, tmp, num);
  }
  fclose(fp);
  return 0;
}

int psend_file(int p, const char *dir, const char *file)
{
  FILE *fp;
  char tmp[MAX_LINE_SIZE * 2];
  char fname[MAX_FILENAME_SIZE];
  int lcount=1;
  char *cp;

  parray[p].last_file[0] = '\0';
  parray[p].last_file_line = 0;  

  if (dir) sprintf(fname, "%s/%s", dir, file);
  else     strcpy(fname, file);
  fp = xfopen(fname, "r");
  if (!fp)
  {
    fprintf(stderr,"psend_file: File '%s' not found!\n",fname);
    return -1;
  }

  if (Debug) Logit("Opened \"%s\"", fname);
  if (parray[p].client) pcn_out(p, CODE_HELP, FORMAT_FILEn);
  while ((cp=fgets( tmp, sizeof tmp, fp))) {
    if (lcount >= (parray[p].d_height-1)) break;
    net_sendStr(parray[p].socket, tmp);
    lcount++;
  }
  if (cp) {
    do_copy(parray[p].last_file, fname, sizeof parray[0].last_file);
    parray[p].last_file_line = (parray[p].d_height-1);
    if (parray[p].client) pcn_out(p, CODE_HELP, FORMAT_FILEn);
    pcn_out(p, CODE_INFO, FORMAT_TYPE_OR_qNEXTq_TO_SEE_NEXT_PAGE_n);
  }
  else {
    if (parray[p].client) pcn_out(p, CODE_HELP, FORMAT_FILEn);
    }
  fclose(fp);
  return 0;
}

int pxysend_raw_file(int p, int num, ...)
{
  va_list ap;
  FILE *fp;
  int cnt;
  char tmp[MAX_LINE_SIZE * 2];
  va_start(ap, num);

  fp = pvafopen(p, num, "r", ap);

  if (!fp)
  {
    Logit("pxysend_raw_file: File '%s' not found!\n",filename1);
    return -1;
  }

  if (!is_regfile(filename1)) { fclose(fp); return -1; }

  while ((cnt = fread(tmp, sizeof(char), sizeof tmp, fp)) > 0) {
    net_send(parray[p].socket, tmp, cnt);
  }
  fclose(fp);
  return 0;
}

int pxysend_file(int p, int num, ...)
{
  va_list ap;
  FILE *fp;
  int rc=0;
#if WANT_OLD_VERSION
  char tmp[MAX_LINE_SIZE * 2];
  int lcount=1;
  char *cp;
#endif /* WANT_OLD_VERSION */

  va_start(ap, num);

  parray[p].last_file[0] = '\0';
  parray[p].last_file_line = 0;  

  fp = pvafopen(p, num, "r", ap);
  if (!fp)
  {
    Logit("pxysend_file: File '%s' not found!\n",filename() );
    va_end(ap);
    return -1;
  }
  do_copy(parray[p].last_file, filename(), sizeof parray[0].last_file);

  fclose(fp);
  rc=pmore_file( p );
  va_end(ap);
  return rc;
}

int pmore_file( int p )
{  
  FILE *fp;
  char tmp[MAX_LINE_SIZE * 2];
  int lcount=1;
  char *cp;

  if (!parray[p].last_file[0]) {
    pcn_out(p, CODE_ERROR, FORMAT_THERE_IS_NO_MORE_n);
    return -1;
  }  
  
  fp = xfopen(parray[p].last_file, "r" );
  if (!fp) {
    pcn_out(p, CODE_ERROR, FORMAT_FILE_NOT_FOUND_n);
    return -1;
  }
  
  if (parray[p].client) {
  pcn_out(p, CODE_HELP, FORMAT_FILEn);
  }
  while((cp=fgets(tmp, sizeof tmp, fp))) {
    if (lcount >= (parray[p].last_file_line + parray[p].d_height-1)) break;
    if (lcount >= parray[p].last_file_line) 
      net_sendStr(parray[p].socket, tmp);
    lcount++;
  }
  if (cp) {
    parray[p].last_file_line += parray[p].d_height-1;
    if (parray[p].client) {
    pcn_out(p, CODE_HELP, FORMAT_FILEn);
    }
    pcn_out(p, CODE_INFO, FORMAT_TYPE_qNEXTq_OR_TO_SEE_NEXT_PAGE_n);
  }
  else {
    parray[p].last_file[0] = '\0';
    parray[p].last_file_line = 0;
    if (parray[p].client) {
    pcn_out(p, CODE_HELP, FORMAT_FILEn);
    }
  }
  fclose(fp);
  return 0;
}

int pmail_file(int p, const char *subj, const char *fname)
{
  FILE *infile, *outfile;
  char buffer[MAX_STRING_LENGTH];
  char com[MAX_STRING_LENGTH];
  char subcopy[MAX_FILENAME_SIZE];
  int num, ret;

  ret = 0;

  /* AvK: avoid writing const strings ... */
  strcpy(subcopy,subj);
 
  Logit("pmail_file(%d,%s,%s)", p, subj, fname);
  if (!(infile = xfopen(fname, "r")))
    return -1;

  if (parray[p].email[0] && safestring(parray[p].email)) {
    sprintf(com, "%s -s \"%s\" %s", 
                  MAILPROGRAM, eatwhite(subcopy), parray[p].email);
    outfile = popen(&com[0], "w");
    fputc('\n', outfile);
    while ((num = fread(buffer, 1, 1000, infile))) {
      fwrite(buffer, 1, num, outfile);
    }
    fwrite("\n\n---\n", 1, 6, outfile);
    pclose(outfile);
  }
  else ret = -1;
  fclose(infile);
  return ret;
}

int xpsend_command(int p, const char *command, char *input, int num, ...)
{
  va_list ap;
  FILE *fp;
  char tmp[MAX_LINE_SIZE];
  char cmdline[MAX_FILENAME_SIZE];
  int cnt;

  va_start(ap, num);
  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  sprintf(cmdline,command,filename1);
  Logit("xpsend_command(%d,%s,%d):%s", p, IFNULL(input,"{Null}"), num, cmdline);

  if (input)
    fp = popen(&cmdline[0], "w");
  else
    fp = popen(&cmdline[0], "r");
  if (!fp) {
    va_end(ap);
    return -1;
  }
  /* AvK: added 1+, to guarantee writing a nul-terminated string */
  if (input) {
    fwrite(input, sizeof(char), 1+strlen(input), fp);
  } else {
    while ((cnt = fread(tmp, sizeof(char), sizeof tmp, fp))) {
      net_send(parray[p].socket, tmp, cnt);
    }
  }
  pclose(fp);
  va_end(ap);
  return 0;
}

char *stoupper(char *str)
{
  int i;

  if (!str)
    return NULL;
  for (i = 0; str[i]; i++) {
    if (islower((int)str[i])) {
      str[i] = toupper(str[i]);
    }
  }
  return str;
}

char *stolower(char *str)
{
  int i;

  if (!str)
    return NULL;
  for (i = 0; str[i]; i++) {
    if (isupper((int)str[i])) {
      str[i] = tolower(str[i]);
    }
  }
  return str;
}

static char unsafechars[] = ">!&*?/<|`$;()[]" ;
int safechar(int c)
{
#if 0
  if ((c == '>') || (c == '!') || (c == '&') || (c == '*') || (c == '?') ||
      (c == '/') || (c == '<') || (c == '|') || (c == '`') || (c == '$') ||
      (c == ';') || (c == '(') || (c == ')') || (c == '[') || (c == ']'))
#else
  if (strchr(unsafechars, c))
#endif
    return 0;
  return 1;
}

int safestring(const char *str)
{

  if (!str)
    return 1;
#if 0
  for ( ; *str; str++) {
    if (!safechar(*str))
      return 0;
  }
#else
  if (str[strcspn(str,unsafechars)])
    return 0;
  else return 1;
#endif
  return 1;
}

/* [PEM]: Don't allow dots and slashes in filenames. */
int safefilename(const char *path)
{
  if (!safestring(path))
    return 0;
  for (	;*path; path++) {
    if (*path == '.' || *path == '/') return 0;
    if (*path == '%' || *path == '\\') return 0; /* Dont want these. AvK */
  }
  return 1;
}

/* [PEM]: This is used to check player names when registering.
** Allowing '-' breaks file_[bw]player() below. Possibly other
** strange characters can break things too.
** The new version allows names of the regexp: [A-Za-z]+[A-Za-z0-9]*
*/
int alphastring(const char *str)
{
  /* [PEM]: Seemed easiest to rewrite it. */
  if (!str || !isalpha((int)*str))
    return 0;
  while (*++str)
    if (!isalnum((int)*str))
      return 0;
  return 1;
}

int printablestring(const char *str)
{

  if (!str) return 1;

  for (; *str; str++) {
    if ((!isprint((int)*str)) && (*str != '\t') && (*str != '\n'))
      return 0;
  }
  return 1;
}

char * mystrdup(const char *str)
{
  char *tmp;
  size_t len;

  if (str == NULL) {
    Logit("Attempt to strdup a NULL string");
    return NULL;
  }
  len = strlen(str);
  tmp = malloc(len + 1);
  memcpy(tmp, str, len); tmp[len] = 0;
  return tmp;
}

char *newhms(int t)
{
  static char tstr[20];
  int h, m, s;

  h = t / 3600;
  t = t % 3600;
  m = t / 60;
  s = t % 60;
  if (h > 99) h = 99;
  if (h) {
      sprintf(tstr, "%dh", h);
  } else if (m) {
    sprintf(tstr, "%dm", m);
  } else {
      sprintf(tstr, "%ds", s);
  }
  return tstr;
}

char *strhms(int t)
{
  static char tstr[60];
  int h, m, s;

  h = t / 3600;
  t = t % 3600;
  m = t / 60;
  s = t % 60;

  sprintf(tstr, "%d hours, %d minutes, %d seconds", h ? h:0, m ? m:0, s ? s:0);
  return tstr;
}

char *hms(int t, int showhour, int showseconds, int spaces)
{
  static char tstr[20];
  char tmp[10];
  int h, m, s;

  h = t / 3600;
  t = t % 3600;
  m = t / 60;
  s = t % 60;
  if (h && showhour) {
    if (spaces)
      sprintf(tstr, "%d : %02d", h, m);
    else
      sprintf(tstr, "%d:%02d", h, m);
  } else {
    sprintf(tstr, "%d", m);
  }
  if (showseconds) {
    if (spaces)
      sprintf(tmp, " : %02d", s);
    else
      sprintf(tmp, ":%02d", s);
    strcat(tstr, tmp);
  }
  return tstr;
}


static const char *dayarray[] =
{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

static const char *montharray[] =
{"Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug",
"Sep", "Oct", "Nov", "Dec"};

static char *strtime(struct tm * stm)
{
  static char tstr[100];

  sprintf(tstr, "%s %3.3s %2d %02d:%02d:%02d %4d",
	  dayarray[stm->tm_wday],
	  montharray[stm->tm_mon],
	  stm->tm_mday,
	  stm->tm_hour,
	  stm->tm_min,
          stm->tm_sec,
          stm->tm_year + 1900);
  return tstr;
}

char *DTdate(const struct tm * stm)
{
  static char tstr[12];

  sprintf(tstr, "%4d-%02d-%02d",
          stm->tm_year + 1900,
          stm->tm_mon + 1,
          stm->tm_mday);
  return tstr;
}

char *ResultsDate(char *fdate)
{
  static char tstr[12];

  sprintf(tstr, "%c%c/%c%c/%c%c%c%c", fdate[4], fdate[5], fdate[6], fdate[7],
                                      fdate[0], fdate[1], fdate[2], fdate[3]);

  return tstr;
}

char *strtime_file(const time_t * clk)
{
  static char tstr[14];
  struct tm *stm = gmtime(clk);

  sprintf(tstr, "%04d%02d%02d%02d%02d",
          stm->tm_year + 1900,
          stm->tm_mon + 1,
	  stm->tm_mday,
	  stm->tm_hour,
	  stm->tm_min);
  return tstr;
}

char *strDTtime(const time_t * clk)
{
  struct tm *stm = localtime(clk);
  return DTdate(stm);
}

char *strltime(const time_t * clk)
{
  struct tm *stm = localtime(clk);
  return strtime(stm);
}

char *strgtime(const time_t * clk)
{
  struct tm *stm = gmtime(clk);
  return strtime(stm);
}

/*
 * To supply a uniform "transaction time", which is constant
 * within one iteration of the select() loop, we store the time
 * in the global variable globclock.
 * Both granularities (1sec, 0.1 sec) are maintained.
 * The returnvalue is the number of seconds since the last call.
 * As a side effect, this will reduce the number of systemcalls ...
 */
/* Ticks are used only for relative timeing (game time limits)
 * since it reports seconds since about 5:00pm on Feb 16, 1994
 */

#define NNGS_EPOCH 331939277
struct ticker globclock = {0,0};
unsigned refetch_ticker(void)
{
  struct timeval tp;
  unsigned elapsed;

  gettimeofday(&tp, NULL);
/* .1 seconds since 1970 almost fills a 32 bit int! So lets subtract off
 * the time right now */
  elapsed = tp.tv_sec - globclock.time;
  globclock.time = tp.tv_sec;
  globclock.tick = SECS2TICS(tp.tv_sec - NNGS_EPOCH)
   + tp.tv_usec / (1000000/TICSPERSEC);
  return elapsed;
}

#if 0
unsigned read_tick()
{
  struct timeval tp;
  /* struct timezone tzp; */

  gettimeofday(&tp, NULL);
/* .1 seconds since 1970 almost fills a 32 bit int! So lets subtract off
 * the time right now */
  return SECS2TICS(tp.tv_sec - NNGS_EPOCH)
   + tp.tv_usec / (1000000/TICSPERSEC);
}

/* This is to translate tenths-secs ticks back into 1/1/70 time in full
 * seconds, because vek didn't read utils.c when he programmed new ratings.
   1 sec since 1970 fits into a 32 bit int OK. 
*/
int untenths(unsigned int tenths)
{
  return tenths/10 + NNGS_EPOCH;
}

char *tenth_str(unsigned int t, int spaces)
{
  return hms((t + 5) / 10, 0, 1, spaces);	/* Round it */
}
#endif

#define MAX_TRUNC_SIZE 100


/* Warning, if lines in the file are greater than 1024 bytes in length, this
   won't work! */
int truncate_file(char *file, int lines)
{
  FILE *fp;
  int bptr = 0, ftrunc = 0, i;
  char tBuf[MAX_TRUNC_SIZE][MAX_LINE_SIZE];
  char *cp;
  size_t len;

  if (lines > MAX_TRUNC_SIZE)
    lines = MAX_TRUNC_SIZE;
  fp = xfopen(file, "r+");
  if (!fp)
  {
    fprintf(stderr,"truncate_file: File '%s' not found!\n",file);
    return 1;
  }
  if (Debug) Logit("Opened %s", file);
  while ((cp=fgets(tBuf[bptr], MAX_LINE_SIZE, fp))) {
    len = strlen(cp); if (len < 1) continue;
    if (tBuf[bptr][len-1] != '\n') {	/* Line too long */
      fclose(fp);
      return -1;
    }
    bptr++;
    if (bptr == lines) {
      ftrunc = 1;
      bptr = 0;
    }
  }
  if (ftrunc) {
    fseek(fp, 0, SEEK_SET);
    ftruncate(fileno(fp), 0);
    for (i = 0; i < lines; i++) {
      fputs(tBuf[bptr], fp);
      bptr++;
      if (bptr == lines) {
	bptr = 0;
      }
    }
  }
  fclose(fp);
  return 0;
}


#ifdef NEWTRUNC
int truncate_file(char *file, int lines)
{
  FILE *fp;
  int bptr = 0, trunc = 0, i;
  char tBuf[MAX_TRUNC_SIZE+10][MAX_LINE_SIZE];

  if (lines > MAX_TRUNC_SIZE)
    lines = MAX_TRUNC_SIZE;
  fp = xfopen(file, "r");
  if (!fp)
  {
    fprintf(stderr,"truncate_file: File '%s' not found!\n",file);
    return 1;
  }
  if (Debug) Logit("Opened %s", file);
  while (fgets(tBuf[bptr], MAX_LINE_SIZE, fp)) {
    if (tBuf[bptr][strlen(tBuf[bptr]) - 1] != '\n') {
      fclose(fp);
      return -1;
    }
    bptr++;
    if (bptr == lines) {
      trunc = 1;
    }
  }
  if (trunc) {
    fclose(fp);
    fp = xfopen(file, "w");    
    for (i = bptr-lines; i < bptr; i++) {
      fputs(tBuf[i], fp);
    }
  }
  fclose(fp);
  return 0;
} 
#endif

int xylines_file(int num,...)
{
  va_list ap;
  int cnt;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  cnt=lines_file(filename1);

  va_end(ap);
  return cnt;
}
/* Warning, if lines in the file are greater than 1024 bytes in length, this
   won't work! */
static int lines_file(char *file)
{
  FILE *fp;
  int lcount = 0;
  char tmp[MAX_LINE_SIZE];

  fp = xfopen(file, "r");
  if (!fp)
    return 0;
  while (fgets(tmp, sizeof tmp, fp))
    lcount++;

  fclose(fp);
  return lcount;
}

int file_has_pname(const char *fname, const char *plogin)
{
  if (!strstr(fname, plogin)) return 0;
  if (!strcmp(file_wplayer(fname), plogin))
    return 2;
  if (!strcmp(file_bplayer(fname), plogin))
    return 1;
  return 0;
}

const char *file_wplayer(const char *fname)
{
  static char tmp[sizeof parray[0].pname];
  const char *ptr;
  size_t len;

	/* skip leading directory part */
  ptr = strrchr(fname, '/');
  if (!ptr) ptr = fname;
  else ptr++;
  len = strcspn(ptr, "-");
  do_copy(tmp, ptr, sizeof tmp);
  ptr = strrchr(tmp, '-');
  if (!ptr) return "";
  if (len < sizeof tmp) tmp[len] = 0;
  return (const char *) tmp;
}


const char *file_bplayer(const char *fname)
{
  static char tmp[sizeof parray[0].pname];
  const char *ptr;
  size_t len;

	/* skip leading directory part */
  ptr = strrchr(fname, '/');
  if (!ptr) ptr = fname;
  else ptr++;
  ptr = strrchr(fname, '-');
  if (!ptr) return "";
  else ptr++;
  len = strcspn(ptr, "-");
  do_copy(tmp, ptr, sizeof tmp);
  if (len < sizeof tmp) tmp[len] = 0;
  return (const char *) tmp;
}

#ifdef HAVE_ENDIAN_H
#include <endian.h>
#endif
char *dotQuad(unsigned int a)
{
  static char buff[40];
  static char *tmp = NULL;

  tmp = (tmp == buff) ? buff+20: buff;
#if !(BYTE_ORDER==LITTLE_ENDIAN)
  sprintf(tmp, "%d.%d.%d.%d", (a & 0xff),
	  (a & 0xff00) >> 8,
	  (a & 0xff0000) >> 16,
	  (a & 0xff000000) >> 24);
#else
  sprintf(tmp, "%d.%d.%d.%d", (a & 0xff000000) >> 24,
	  (a & 0xff0000) >> 16,
	  (a & 0xff00) >> 8,
	  (a & 0xff));
#endif
  return tmp;
}

#if OBSOLETE_SOURCE
int available_space(void)
{
#if defined(__linux__)
#include <sys/vfs.h>
  int rc;
  struct statfs buf;

  rc=statfs(player_dir, &buf);
  return (rc)? 0: ((buf.f_bsize/256) * (buf.f_bavail/4));
#elif defined(SYSTEM_NEXT)
  struct statfs buf;

  statfs(player_dir, &buf);
  return ((buf.f_bsize/ 256) * (buf.f_bavail/4));
#elif defined(SYSTEM_ULTRIX)
  struct fs_data buf;

  statfs(player_dir, &buf);
  return ((buf.bfreen));
#else
   return 100000000;		/* Infinite space */
#endif
}

int file_exists(char *fname)
{
  FILE *fp;

  fp = xfopen(fname, "r");
  if (!fp)
    return 0;
  fclose(fp);
  return 1;
}
#endif /* OBSOLETE_SOURCE */

/* read a directory into memory. Strings are stored successively in buffer */
/* returns -1 on error, or a count of strings stored */
/* A filter can be passed too */
int search_directory(char *buffer, int buffersize, char *filter, int num, ...)
{
  va_list ap;
  FILE *fp;
  char command[MAX_FILENAME_SIZE];
  char temp[MAX_LINE_SIZE];
  char *s = buffer;
  int count = 0;
  int bytecount = 0;
  int filtlen;
  int len;
  int diff;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);

  sprintf(command, "ls -1 %s", filename1);
  fp = popen( &command[0] , "r");
  if (!fp) {
    va_end(ap);
    return -1;
    }
  filtlen=strlen(filter);
  while (fgets(temp, sizeof temp, fp)) {
    len = strlen(temp);
    if (bytecount + len >= buffersize) { break; }
    diff= filter ? strncmp(filter, temp, filtlen) :0;
    if (diff<0) { break; } /* already past filenames that could match */
    if (diff>0) { continue; }
    strcpy(s, temp);
    count++;
    bytecount += len;
    s += len;
    *(s - 1) = '\0';
  }
  pclose(fp);
  va_end(ap);
  return count;
}

int display_directory(int p, const char *buffer, int count)
/* buffer contains 'count' 0-terminated strings in succession. */
{
#define MAX_DISP 800		/* max. no. filenames to display */

  const char *s = buffer;
  struct multicol *m = multicol_start(MAX_DISP);
  int i;

  for (i = 0; (i < count && i < MAX_DISP); i++) {
    multicol_store(m, s);
    s += strlen(s) + 1;
  }
  multicol_pprint(m, p, 78, 1);
  multicol_end(m);
  return i;
}


void bldsearchdata(char *psz)
{
  if (psz[0] == '-') {
    psz++;
    mysearchdata.where = SDATA_HOST;
  } else {
    mysearchdata.where = SDATA_USER;
  }
  bmInit(strlwr(psz), &mysearchdata.bmData);
}

char *strlwr(char *psz)
{
  char *ret = psz;
  while (*psz) {
    *psz = tolower(*psz);
    psz++;
  }
  return ret;
}

const struct searchresult *search(char *psz)
{
  static struct searchresult sr;
  char *pcStr = 0;
  char *pcTmp = strchr(psz, ':');

  if (!pcTmp)			/* PEM */
    return 0;
  *pcTmp = 0;
  strcpy(sr.szPlayer,pcTmp + 1);
  strcpy(sr.szMailAddr, psz);

  pcTmp = strchr(psz, '@');
  if (!pcTmp)			/* PEM */
    return 0;
  *pcTmp = 0;

  if (mysearchdata.where == SDATA_HOST) {
    psz = pcTmp + 1;
  }

  pcStr = bmSrch(strlwr(psz), &mysearchdata.bmData);

  if (pcStr) {
    return &sr;
  }

  return 0;
}

int blank(char *psz)
{
  while (*psz) {
    if (!isspace((int)*psz)) {
      return 0;
    } else {
      psz++;
    }
  }
  return 1;
}


int do_copy(char *dest, const char *s, int max)
{
  /* [PEM]: If the logging is not really necessary, it's probably
     more efficient to skip this. */
  /* AvK: changed semantics: max is the declared/allocated
  ** size for dest[] , so dest[max-1] is the last element
  ** and will *always* be set to '\0' */
#if 1
  int i;

  i = strlen(s);
  if (i >= max) {
    Logit("Attempt to copy large string %s (len = %d, max = %d)", s, i, max);
    i = max -1;
  }

  memcpy(dest, s, i);
  dest[i] = '\0';
#else
  if (max > 0) max -= 1;
  strncpy(dest, s, max);
  dest[max] = '\0';		/* [PEM]: Make sure it's terminated. */
#endif
  return i;
}

/* AvK: this function is a wrapper around fopen.
** If mode is not read and a directory does not exists,
** it will create the entire path to the file (if allowed).
** This feature is only intended as an aid for testers, cause
** getting all the paths right takes a lot of time.
** In a "production" server all the directories should exist.
** Behaviour depends on the global variable mode_for_dirs:
** when zero, no directories are created.
** The function writes to the logfile, if
** *) file not found && writemode.
** *) for each directory it creates.
*/
FILE * xfopen(const char * name, const char * mode)
{
  FILE *fp;
  int err;

  do {
    fp=fopen(name,mode);
    if (fp) break;
    err=errno;

/* Don't log missing files when mode is read.
** (unregistered players, missing messagefiles, etc)
*/
    if (*mode=='r') break;

    Logit("Xfopen: fopen(\"%s\", \"%s\") failed: %d, %s"
	  ,name,mode, err,strerror(err) );

    if (*mode=='r') break;

    switch(err) {
    case ENOENT:
      if (mode_for_dir) {
        err=mkdir_p(name);
        if (err> 0) continue;
	}
    default:
      goto quit;
    }
  } while(!fp);

quit:
  return fp;
}


FILE * xyfopen(int num, const char * mode, ...)
{
  va_list ap;
  FILE *fp;

  va_start(ap, mode);

  fp = vafopen(num, mode, ap);

  va_end(ap);
  return fp;
}


static FILE * vafopen(int num, const char * mode, va_list ap)
{
  FILE *fp;

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  fp = xfopen(filename1, mode);

  return fp;
}


FILE * pxyfopen(int p, int num, const char * mode, ...)
{
  va_list ap;
  FILE *fp;
  va_start(ap, mode);

  fp = pvafopen(p, num, mode, ap);

  va_end(ap);
  return fp;
}

static FILE * pvafopen(int p, int num, const char * mode, va_list ap)
{
  FILE *fp;
  const char *pre;
  char *nam;
  int lang;

  switch(num) {
	/* These are the language-dependant directories.
	** Lookup the language, and insert it into the arglist
	** , before the given varargs.
	*/
  case FILENAME_HELP_q:
  case FILENAME_AHELP_q:
    lang = parray[p].language;
    pre = language_num2prefix(lang);
    fp=xyfopen(num+2, mode, pre); /* num+2 := ..HELP_s */
    if (fp) break;
    pre = language_num2prefix(LANGUAGE_DEFAULT);
    fp=xyfopen(num+2, mode, pre);
    break;
  case FILENAME_HELP_q_s:
  case FILENAME_AHELP_q_s:
    nam= va_arg(ap,char*);
    lang = parray[p].language;
    pre = language_num2prefix(lang);
    fp=xyfopen(num+2, mode, pre, nam); /* num+2 := ..HELP_s_s */
    if (fp) break;
    pre = language_num2prefix(LANGUAGE_DEFAULT);
    fp=xyfopen(num+2, mode, pre, nam);
    break;
  default:
    Logit("Pvafopen(%d,%d,...) : not langage-dependant", p,num);
  case FILENAME_MESS_LOGIN:
  case FILENAME_MESS_LOGOUT:
  case FILENAME_MESS_UNREGISTERED:
  case FILENAME_MESS_MOTD:
  case FILENAME_MESS_MOTDs:
  case FILENAME_MESS_AMOTD:
    fp=vafopen(num, mode, ap);
    break;
  }
  return fp;
}

int xyrename(int num1, int num2, ...)
{
  va_list ap1,ap2;
  int rc;

  va_start(ap1, num2);
  va_start(ap2, num2);

  memset(filename1,0,sizeof filename1);
  memset(filename2,0,sizeof filename2);
  vafilename(filename1,num1, ap1);
  vafilename(filename2,num2, ap2);
  rc = rename(filename1, filename2);

  va_end(ap1);
  va_end(ap2);
  return rc;
}

int xyunlink(int num,...)
{
  va_list ap;
  int rc;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  rc=unlink(filename1);

  va_end(ap);
  return rc;
}

  /* AvK: Sorry, this function uses the current value of the static 
  ** filename1[], so it relies on the caller having called one 
  ** of the other xy...() functions first.
  */
int xylink(int num, ...)
{
  va_list ap;
  int rc;

  va_start(ap, num);

  memset(filename2,0,sizeof filename2);
  vafilename(filename2,num, ap);
  if ((rc=strcmp(filename1, filename2)))
    rc=link(filename1, filename2);

  va_end(ap);
  return rc;
}

int xyfilename(char *buf,int num, ...)
{
  va_list ap;
  int rc;

  va_start(ap, num);

  rc=vafilename(buf,num, ap);

  va_end(ap);
  return rc;
}

static size_t vafilename(char *buf, int num, va_list ap)
{
size_t len;
  const char *cp1, *cp2,*cp3;
  int i1,i2;

  switch(num) {
  case FILENAME_CMDS :
    len=sprintf(buf, "%s/commands", help_dir);
    break;
  case FILENAME_ACMDS :
    len=sprintf(buf, "%s/admin_commands", ahelp_dir);
    break;
  case FILENAME_INFO:
    len=sprintf(buf, "%s", info_dir);
    break;

  case FILENAME_HELP:
    len=sprintf(buf, "%s", help_dir);
    break;
  case FILENAME_HELP_p:
    i2= va_arg(ap,int);
    i1=parray[i2].language;
    goto filename_help_1;
  case FILENAME_HELP_l:
    i1= va_arg(ap,int);
    goto filename_help_1;
  filename_help_1:
    cp1=language_num2prefix(i1);
    goto filename_help_0;
  case FILENAME_HELP_s:
    cp1= va_arg(ap,char*);
    goto filename_help_0;
  filename_help_0:
    len=sprintf(buf, "%s/%s", help_dir, cp1);
    break;
  case FILENAME_HELP_l_index:
    i1= va_arg(ap,int);
    cp1=language_num2prefix(i1);
    goto filename_help_l_index_0;
  case FILENAME_HELP_s_index:
    cp1= va_arg(ap,char*);
  filename_help_l_index_0:
    len=sprintf(buf, "%s/%s/%s", help_dir, cp1, ".index" );
    break;
  case FILENAME_HELP_l_s:
    i1= va_arg(ap,int);
    cp1=language_num2prefix(i1);
    goto filename_help_l_0;
  case FILENAME_HELP_s_s:
    cp1= va_arg(ap,char*);
  filename_help_l_0:
    cp2= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s/%s", help_dir, cp1, cp2);
    break;

  case FILENAME_AHELP:
    len=sprintf(buf, "%s", ahelp_dir);
    break;
  case FILENAME_AHELP_p:
    i2= va_arg(ap,int);
    i1=parray[i2].language;
    goto filename_ahelp_s_1;
  case FILENAME_AHELP_l:
    i1= va_arg(ap,int);
    goto filename_ahelp_s_1;
  filename_ahelp_s_1:
    cp1=language_num2prefix(i1);
    goto filename_ahelp_s_0;
  case FILENAME_AHELP_s:
    cp1= va_arg(ap,char*);
    goto filename_ahelp_s_0;
  filename_ahelp_s_0:
    len=sprintf(buf, "%s/%s", ahelp_dir, cp1);
    break;
  case FILENAME_AHELP_s_s:
    cp1= va_arg(ap,char*);
    cp2= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s/%s", ahelp_dir, cp1, cp2);
    break;
  case FILENAME_AHELP_l_index:
    i1= va_arg(ap,int);
    cp1=language_num2prefix(i1);
    goto filename_ahelp_l_index_0;
  case FILENAME_AHELP_s_index:
    cp1= va_arg(ap,char*);
filename_ahelp_l_index_0:
    len=sprintf(buf, "%s/%s/%s", ahelp_dir, cp1, ".index" );
    break;

  case FILENAME_PLAYER :
    len=sprintf(buf, "%s", player_dir);
    break;
  case FILENAME_PLAYER_cs :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s", player_dir, cp1[0], cp1);
    break;
  case FILENAME_PLAYER_cs_DELETE :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s.delete", player_dir, cp1[0], cp1);
    break;
  case FILENAME_PLAYER_cs_LOGONS :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s.%s", player_dir, cp1[0], cp1, stats_logons);
    break;
  case FILENAME_PLAYER_cs_MESSAGES :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s.%s", player_dir, cp1[0], cp1, stats_messages);
    break;
  case FILENAME_PLAYER_cs_GAMES:
    cp1 = va_arg(ap,char*);
    len=sprintf(buf,"%s/player_data/%c/%s.%s", stats_dir,cp1[0]
    , cp1, STATS_GAMES);
    break;

  case FILENAME_GAMES_s:
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s" , game_dir, cp1);
    break;
  case FILENAME_GAMES_c :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c" , game_dir, cp1[0]);
    break;
  case FILENAME_GAMES_bs_s :
    cp1= va_arg(ap,char*);
    cp2= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s-%s" , game_dir, cp2[0], cp1, cp2);
    break;
  case FILENAME_GAMES_ws_s :
    cp1= va_arg(ap,char*);
    cp2= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s-%s" , game_dir, cp1[0], cp1, cp2);
    break;

  case FILENAME_CGAMES:
    len=sprintf(buf, "%s" , cgame_dir );
    break;
  case FILENAME_CGAMES_c:
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c" , cgame_dir, cp1[0]);
    break;
  case FILENAME_CGAMES_cs:
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s" , cgame_dir, cp1[0], cp1);
    break;
  case FILENAME_CGAMES_ws_s_s:
    cp1= va_arg(ap,char*);
    cp2= va_arg(ap,char*);
    cp3= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s-%s-%s" , cgame_dir
		, cp1[0], cp1, cp2, cp3);
    break;
  case FILENAME_CGAMES_bs_s_s:
    cp1= va_arg(ap,char*);
    cp2= va_arg(ap,char*);
    cp3= va_arg(ap,char*);
    len=sprintf(buf, "%s/%c/%s-%s-%s" , cgame_dir
		, cp2[0], cp1, cp2, cp3);
    break;

  case FILENAME_RATINGS :
    len=sprintf(buf,"%s", ratings_file);
    break;
  case FILENAME_RESULTS :
    len=sprintf(buf, "%s", results_file);
    break;
  case FILENAME_NRESULTS :
    len=sprintf(buf, "%s", nresults_file);
    break;

  case FILENAME_LADDER9 :
    len=sprintf(buf, "%s", ladder9_file);
    break;
  case FILENAME_LADDER19 :
    len=sprintf(buf, "%s", ladder19_file);
    break;

  case FILENAME_NEWS_s :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/news.%s", news_dir, cp1);
    break;
  case FILENAME_NEWSINDEX :
    len=sprintf(buf, "%s/news.index", news_dir);
    break;
  case FILENAME_ADMINNEWSINDEX :
    len=sprintf(buf, "%s/adminnews.index", news_dir);
    break;
  case FILENAME_ADMINNEWS_s :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/adminnews.%s", news_dir, cp1);
    break;
  case FILENAME_NOTEFILE :
    len=sprintf(buf, "%s", note_file);
    break;
  case FILENAME_LOGONS :
    len=sprintf(buf, "%s/%s", stats_dir, stats_logons);
    break;

  case FILENAME_MESS_LOGIN:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_LOGIN);
    break;
  case FILENAME_MESS_LOGOUT:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_LOGOUT);
    break;
  case FILENAME_MESS_WELCOME:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_WELCOME);
    break;
  case FILENAME_MESS_UNREGISTERED:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_UNREGISTERED);
    break;
  case FILENAME_MESS_MOTD:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_MOTD);
    break;
  case FILENAME_MESS_MOTDs:
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s.%s", mess_dir, MESS_MOTD, cp1);
    break;
  case FILENAME_MESS_AMOTD:
    len=sprintf(buf, "%s/%s", mess_dir, MESS_AMOTD);
    break;
  case FILENAME_EMOTE:
    len=sprintf(buf, "%s", emotes_file) ;
    break;

  case FILENAME_FIND :
    len=sprintf(buf, "%s", FIND_FILE);
    break;
  case FILENAME_LISTINDEX :
    len=sprintf(buf, "%s/index", lists_dir);
    break;
  case FILENAME_LIST_s :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s", lists_dir, cp1);
    break;
  case FILENAME_LIST_s_OLD :
    cp1= va_arg(ap,char*);
    len=sprintf(buf, "%s/%s.old", lists_dir, cp1);
    break;
  case FILENAME_LIST_BAN :
    len=sprintf(buf, "%s/%s", lists_dir, "ban");
    break;

  case FILENAME_PROBLEM_d :
    i1= va_arg(ap,int);
    len=sprintf(buf, "%s/xxqj%d.sgf", problem_dir, i1);
    break;
  default: /* this will fail on open, and appear in the log ... */
    len=sprintf(buf, "/There/was/a/default/filename:%d", num);
    break;
  }

#if 0
if (strstr(buf,"home/nngs/nngs/share/nngssrv/stats/player_data/j/joop"))
	raise(5 /* SIGTRAP */ );
#endif

return len;
}

char * filename(void)
{
  return filename1;
}

	/* This function does about the same as "mkdir -p"
	** It assumes *name to be a pathname/filename.
	** all the nodes in the pathname-part are created
	** (if they don't exist yet)
	** Function fails on first unrecoverable) error.
	** returns :
	** (return >= 0) := number of nodes that were created
	** (return < 0) := -errno
	*/
static int mkdir_p(const char * name)
{
  int err=0;
  size_t len;
  int rc=0;
  int cnt=0;
  char *slash;
  char buff[MAX_FILENAME_SIZE];
  struct stat statbuff;

  len = strlen(name);
  memcpy(buff,name,len);
  buff[len] = 0;

  for(slash=buff; (slash=strchr(slash+1, '/' )); ) {
	/* this is to catch double / in paths */
    if (slash[-1] == '/') continue;
    *slash=0 ;
    rc=stat(buff, &statbuff);
    if (!rc) err=EEXIST; /* this is used to skip existing prefix */
    else {
      rc=mkdir(buff, mode_for_dir);
      err=(rc) ? errno: 0;
      }
    switch(err) {
    case 0:
      cnt++;
      Logit("Mkdir_p[%d](\"%s\", %04o) := Ok", cnt, buff, mode_for_dir );
    case EEXIST: 
      break;
    case ENOTDIR: 
    case EACCES: 
    default:
      Logit("Mkdir_p(\"%s\", %04o) := [rc=%d] Err= %d (%s)"
           , buff, mode_for_dir, rc, err, strerror(err) );
      *slash = '/' ;
      goto quit;
    }
    *slash = '/' ;
  }
quit:
  return (err) ? -err : cnt;
}


int pcn_out(int p, int code, int num, ...)
{
  va_list ap;
  int retval;
  const char *format;

  va_start(ap, num);

  format = find_format(parray[p].language,num);
  retval = pcvprintf(p, code, format, ap);

  va_end(ap);
  return retval;
}


int pcn_out_prompt(int p, int code, int num,...)
{
  va_list ap;
  int retval;
  const char *format;

  va_start(ap, num);

  format = find_format(parray[p].language,num);
  retval = pcvprintf(p, code, format, ap);

  pprompt(p);

  va_end(ap);
  return retval;
}

/* AvK This is just a wrapper around crypt.
** some platforms have no working crypt()
** , but do contain a stub, which only returns NULL.
**
** We first call crypt(), if it returns NULL: we issue a warning and bail out.
** Desperate people could return something usefull here.
*/
char * mycrypt(const char *passwd, const char * salt)
{
  char *cp;

  cp=crypt(passwd, salt);
  if (!cp) {
    fprintf(stderr,"\n%s,line %d: Need a working crypt() function!\n"
           , __FILE__,__LINE__);
    Logit("%s,line %d: Need a working crypt() function!"
           , __FILE__,__LINE__);
    main_exit(0);
  }
  return cp;
}

int xystat(struct stat * sp, int num, ...)
{
  va_list ap;
  int rc;
  struct stat local;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  if (!sp) sp = &local;
  rc = stat(filename1, sp);

  va_end(ap);
  return rc;
}

int xytouch(int num, ...)
{
  va_list ap;
  int rc;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  rc=utime(filename1, NULL);

  va_end(ap);
  return rc;
}

DIR * xyopendir(int num, ...)
{
  va_list ap;
  DIR * dirp;

  va_start(ap, num);

  memset(filename1,0,sizeof filename1);
  vafilename(filename1,num, ap);
  dirp=opendir(filename1);

  va_end(ap);
  return dirp;
}

int parse_rank(int num, int ch)
{

  switch (ch) {
  case 'k': case 'K': num = 31 - num; break;
  case 'd': case 'D': num += 30; break;
  case 'p': case 'P': num += 40; break;
  default: /* Bad rating. */ break;
  }
  return num;
}


int asc2ipaddr(char *str, unsigned *add)
{
  int rc;
  unsigned vals[4];

  str += strspn(str,"0xX");
  rc = sscanf(str, "%u.%u.%u.%u", vals,vals+1,vals+2, vals+3);
  switch(rc) {
  case 0:
  case 1:
    rc = sscanf(str, "%x", vals);
    if (rc != 1) break;
    *add = vals[0];
    return 0;
  default: break;
  case 4:
    *add
      =(vals[0]&0xff) <<24
      |(vals[1]&0xff) <<16
      |(vals[2]&0xff) <<8
      |(vals[3]&0xff)  ;
    return 0;
  }
  return -1;
}
