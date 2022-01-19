/*
    NNGS - The No Name Go Server
    Copyright (C) 1995 John Tromp (tromp@daisy.uwaterloo.ca/tromp@cwi.nl)

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

#ifdef HAVE_STDLIB_H
#include <stdlib.h>
extern int random(void);
#endif

#include <assert.h>

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_STRINGS_H
#include <strings.h>
#endif

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif


#include "nngsmain.h"
#define MINK_C 1
#include "mink.h"
#include "utils.h"

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif


#define MOVECOLOR(i)	(((i) & 1) ? MINK_BLACK : MINK_WHITE)
#define LASTCOLOR(g)	MOVECOLOR((g)->movenr)
#define OTHERCOL(c)	(MINK_BLACK + MINK_WHITE - (c))

/* the following macros are to be used only w.r.t. a game struct called g */
#define Vdiff		((g)->width+1)		/* vertical index difference */
#define Size		((g)->width * (g)->height)	/* board size */
#define ESize		(Vdiff*((g)->height + 2))	/* extra board size */
#define MAXWidth	25
#define MAXSize		((MAXWidth+1)*(MAXWidth+2))

static char BOARDCHARS[] = ".#O .xo-+><|";      /* territory at +4 offsets */

static const char *stars[] = {
"",				/* 0 */
"aa",				/* 1 */
"",				/* 2 */
"",				/* 3 */
"",				/* 4 */
"cc",				/* 5 */
"",				/* 6 */
"dd",				/* 7 */
"cccffcff",			/* 8 */
"ggcccggccegeecegee",		/* 9 */
"hhccchhc",			/* 10 */
"iiccciiccfiffcfiff",		/* 11 */
"iidddiid",			/* 12 */
"jjdddjjddgjggdgjgg",		/* 13 */
"kkdddkkd",			/* 14 */
"lldddllddhlhhdhlhh",		/* 15 */
"mmdddmmd",			/* 16 */
"nndddnnddiniidinii",		/* 17 */
"oodddood",			/* 18 */
"ppdddppddjpjjdjpjj",	/* 19 */
"",				/* 20 */
"",				/* 21 */
"",				/* 22 */
"",				/* 23 */
"",				/* 24 */
"",				/* 25 */
"",				/* 26 */
};

static void startgame(struct minkgame *g)
{
  int x,y;

  g->movenr = g->logid = 0;
  g->hash = 0L;
  g->caps[MINK_WHITE] = g->caps[MINK_BLACK] = 0;
  for (x=0; x<ESize; x++)
    g->board[x] = MINK_EMPTY;
  for (y=0; y<ESize; y+=Vdiff)
    g->board[y] = MINK_EDGE;
  for (x=1; x<Vdiff; x++)
    g->board[x] = g->board[x+ESize-Vdiff] = MINK_EDGE;
}

typedef xulong xulongpair[2];

static xulongpair *Zob;

void initmink()
{
  int i,j;

  Zob = malloc(MAXSize * sizeof *Zob);
  for (i=0; i<MAXSize; i++)
    for (j=0; j<2; j++)
      Zob[i][j] = (xulong)random() << (sizeof(xulong)-1)/2 
	        | (xulong)random();
}

struct minkgame *initminkgame(int width, int height, int rules)
{
  struct minkgame *g;

  g = malloc(sizeof *g);
  assert(g != NULL);
  g->width = width;
  g->height = height;
  g->rules = rules;
  g->handicap = 0;
#ifdef MINKKOMI
  g->komi = MINK_KOMI;
#endif
  g->board = calloc(ESize, sizeof *g->board);
  assert(g->board != NULL);
  g->mvsize = ESize/2;		/* need at least 1 for 0x0 board:) */
  g->moves = calloc(g->mvsize, sizeof *g->moves);
  assert(g->moves != NULL);
  g->moves[0].hash = 0L;
  g->uf = calloc(ESize, sizeof *g->uf);
  assert(g->uf != NULL);
  g->logsize = ESize;
  g->uflog = calloc(g->logsize, sizeof *g->uflog);
  assert(g->uflog != NULL);
/*  g->nocaps = 0; */
  startgame(g);
  return g;
}

void savegame(FILE *fp, struct minkgame *g, struct mvinfo *mi, int nmvinfos)
{
  int i,j,k,n,p,x,y,max;
  const char *star;
  struct kibitz *kp;

  if (g->rules == RULES_ING && (n = g->handicap)) {
    fprintf(fp, "HA[%d]", n);
  }
  for (k=0,i=1; i<=g->movenr; i++) {
    p = g->moves[i].point;
    if (p < -1) {
      if (g->rules == RULES_NET) {	/* should always be the case */
        n = g->handicap;
        if (mi != NULL) {
          fprintf(fp, "HA[%d]AB", n);
          star = stars[g->width];
          max = strlen(star) / 2;
          for (j=n; j; j--)
          {
            if (max == 9 && (n == 5 || n == 7) && j==1)
              star = stars[g->width]+(2*9-2);
            x = *star++;
            y = *star++ - 'a' + 1;
            fprintf(fp, "[%c%c]", x, 'a' + g->height - y);
          }
          fprintf(fp, "\n");
        } else {
          fprintf(fp,";B[z%c]", 'a' + n);
        }
      }
    } else {
      fprintf(fp,";%c[", "WB"[i&1]);
      if (p == MINK_PASS)
        fprintf(fp, "tt");
      else fprintf(fp,"%c%c", 'a'-1+ p%Vdiff, 'a' + g->height - p/Vdiff);
      fprintf(fp,"]");
    }
    if (i < nmvinfos && (kp = mi[i].kibitz) != NULL) {
      fprintf(fp,"C[\n");
        for (; kp; kp = kp->next) {	/* [PEM]: Must quote brackets. */
	  char *s;
	  for (s = kp->mess ; *s ; s++) {
	    switch (*s) {
            case '[': case ']': fputc('\\', fp);
              /* FALLTRU */
            default:
	      fputc(*s, fp);
            }
	  }
	  fputc('\n', fp);
	}
        fputs("]\n", fp);
      k = 0;
    } else if (++k == 12) {
      fputc('\n', fp);
      k = 0;
    }
  }
  if (k)
    fprintf(fp,"\n");
}

int loadgame(FILE *fp, struct minkgame *g)	/* return player_to_move if OK */
{
  char color, mfile, rank;

  while (fscanf(fp, "; %c [ %c %c ] ", &color, &mfile, &rank) == 3) {
    if (color != " WB"[LASTCOLOR(g)])
       return 0;
    if (mfile == 'z')
      sethcap(g, rank - 'a');
    else if (mfile == 't' && rank == 't')
       pass(g);
    else if (play(g, point(g, mfile - ('a'-1), g->height - (rank - 'a')),0) == 0)
       return 0;
  }
  return g->movenr & 1 ? MINK_WHITE: MINK_BLACK ;
}

int loadpos(FILE *fp, struct minkgame *g)	/* return player_to_move if OK */
{
  char color, mfile, rank;

  fscanf(fp, " ( ; Game[1]");
  while (fscanf(fp, " Add%c", &color) == 1) {
    while (fscanf(fp, " [ %c %c ] ", &mfile, &rank) == 2) {
      if (color != " WB"[LASTCOLOR(g)])
         pass(g);
      if (play(g, point(g, mfile - ('a'-1), g->height - (rank - 'a')),0) == 0)
        return 0;
    }
  }
  return g->movenr & 1 ? MINK_WHITE: MINK_BLACK ;
}

xulong gethash(struct minkgame *g)
{
  return g->hash;
}

#ifdef MINKKOMI
void setkomi(struct minkgame *g, float k)	/* set the komi */
{
  g->komi = k;
}

float getkomi(struct minkgame *g)		/* set the komi */
{
  return g->komi;
}
#endif

int movenum(struct minkgame *g)	/* return move number (#moves played in game) */
{
  return g->movenr;
}

void freeminkgame(struct minkgame *g)
{
  free(g->uflog);
  free(g->uf);
  free(g->moves);
  free(g->board);
  free(g);
}
/*
void setnocaps(struct minkgame *g, int value)
{
  g->nocaps = value;
}
*/
int sethcap(struct minkgame *g, int n)	/* returns 1 if succesful */
{
  int i,x,y,max;
  const char *star;

  if (g->movenr || n==0)
    return 0;
  g->handicap = n;
  if (g->rules == RULES_NET) {
    star = stars[g->width];
    max = strlen(star) / 2;
    if (n > max)
      return 0;
    for (i=n; i; i--)
    {
      if (max == 9 && (n == 5 || n == 7) && i==1)
        star = stars[g->width]+(2*9-2);
      x = *star++ - 'a' + 1;
      y = *star++ - 'a' + 1;
      play(g,point(g,x,y),0);
      g->movenr = 0;
    }
    g->moves[g->movenr = 1].point = -1-n;
  }
  return 1;
}


#if 0
static int libs(struct minkgame *g, int p)	/* find #liberty-edges of p's group */
{
  while ((p = g->uf[p]) > 0) ;
  return -p;
}
#endif


static void growlog(struct minkgame *g)
{
  if (g->logid >= g->logsize) {
    g->logsize *= 2;
    g->uflog = realloc(g->uflog, g->logsize * sizeof *g->uflog);
    assert(g->uflog != NULL);
  }
}


static void ufmark(struct minkgame *g)		/* put a move-marker on union-find log */
{
  growlog(g);
  g->uflog[g->logid++].index = 0;
}


static void ufset(struct minkgame *g, int i, int v)	/* logged change to union-find structure */
{
  growlog(g);
  g->uflog[g->logid].index = i;
  g->uflog[g->logid++].value = g->uf[i];
  g->uf[i] = v;
}

static void ufadd(struct minkgame *g, int i, int v)	/* logged change to union-find structure */
{
  growlog(g);
  g->uflog[g->logid].index = i;
  g->uflog[g->logid++].value = g->uf[i];
  g->uf[i] += v;
}

static void fill(struct minkgame *g, int p, int c)	/* fill empty space from point p with color c */
{
  if (g->board[p] == MINK_EMPTY) {
    g->hash ^= Zob[p][c];
    g->board[p] = c;
    g->caps[c]--;
    fill(g,p-Vdiff,c);
    fill(g,p-1,c);
    fill(g,p+1,c);
    fill(g,p+Vdiff,c);
  }
}

static int capture(struct minkgame *g, int p, int c)	/* return #stones captured */
{
  if (g->board[p] == MINK_EMPTY || g->board[p] == MINK_EDGE)
    return 0;
  if (g->board[p] == c) {
    g->hash ^= Zob[p][c];
    g->board[p] = MINK_EMPTY;
    g->caps[c]++;
    return 1 + capture(g,p-Vdiff,c) + capture(g,p-1,c)
             + capture(g,p+1,c) + capture(g,p+Vdiff,c);
  }
  for (; g->uf[p] >0; p = g->uf[p]) ;
  ufadd(g,p,-1);		/* give extra liberty(edge) */
  return 0;
}

/* exam nb of color c stone; return whether this nb gets captured */
static int neighbour(struct minkgame *g, int p, int c)
{
  int nc,nr;

  if ((nc = g->board[p]) == MINK_EDGE)
    return 0;
  if (nc == MINK_EMPTY) {
    ufadd(g,g->root,-1);	/* extra liberty */
    g->kostat = -1;
    return 0;
  }
  for (nr=p; g->uf[nr] > 0; nr = g->uf[nr]) ;	/* find neighbour root */
  if (nc == c) {
    if (nr == g->root)			/* same group; discount liberty */
      ufadd(g,nr,1);
    else {
      nc = g->uf[g->root] + g->uf[nr] + 1;	/* friendly group; share libs */
      if (g->uf[g->root] < g->uf[nr]) {		/* orig. group has more libs */
        ufset(g,g->root,nc);
        ufset(g,nr,g->root);
      } else {
        ufset(g,nr,nc);
        ufset(g,g->root,nr);
        g->root = nr;
      }
    }
    g->kostat = -1;
    return 0;
  }
  if (g->uf[nr] == -1) {
    if (capture(g,p,nc)==1 && !g->kostat)
      g->kostat = p;
    else g->kostat = -1;
    return 1;
  }
  ufadd(g,nr,1);
  return 0;
}

int back(struct minkgame *g)	/* return 1 on succes */
{
  int p,c,i;
  struct minkmove *mv;

  if (g->movenr == 0)
    return 0;
  c = LASTCOLOR(g);
  mv = &(g->moves[g->movenr--]);
  p = mv->point;
  if (p < -1) {
    startgame(g);
    return 1;
  }
  if (p == MINK_PASS)
    return 1;
/*  if (g->nocaps == 0) { */
    if (mv->self)
      fill(g,p,c);
    else { 
      if (mv->up)
        fill(g,p-Vdiff,OTHERCOL(c));
      if (mv->left)
        fill(g,p-1,OTHERCOL(c));
      if (mv->right)
        fill(g,p+1,OTHERCOL(c));
      if (mv->down)
        fill(g,p+Vdiff,OTHERCOL(c));
    }
/*  } */
  for (; (i = g->uflog[--g->logid].index); g->uf[i] = g->uflog[g->logid].value);
  g->board[p] = MINK_EMPTY;
  g->hash = (mv-1)->hash;
  return 1;
}

void forward(struct minkgame *g)
{
  int p;

  if ((p = g->moves[g->movenr+1].point) == MINK_PASS)
    g->movenr++;
  else play(g,p,0);
}

static void growmoves(struct minkgame *g)
{
  if (g->movenr >= g->mvsize - 1) {
    g->mvsize *= 2;
    g->moves = realloc(g->moves, g->mvsize * sizeof *g->moves);
    assert(g->moves != NULL);
  }
}

int go_move(struct minkgame *g, char *s)		/* return point != 0 if s is go_move */
{
  char mfile;
  int rank;

  if (strcmp(s, "pass") == 0)
    return 1;
  if (sscanf(s,"%c %d", &mfile, &rank) < 2)
    return 0;
#if UPPERCASE
  mfile = tolower(mfile);
#endif
  if (mfile < 'a' || mfile  == 'i')
    return 0;
  if (mfile > 'i')
    mfile--;
  if ((mfile -= ('a'-1)) > g->width || rank < 1 || rank > g->height)
    return 0;
  return point(g,mfile,rank);
}

int point(struct minkgame *g, int mfile, int rank)	/* convert coords to point */
{
  return rank * Vdiff + mfile;
}

static int superko(struct minkgame *g) /* return whether current position repeats older one */
{
  int *curboard,diff=1,i,j,n;

  for (i = g->movenr; (i-=2) >= 0;) {
    if (g->moves[i].hash == g->hash) { 
#ifdef HASHFAITH
      return 1;
#else
      curboard = calloc(ESize, sizeof *curboard);
      for (j=0; j<ESize; j++)
        curboard[j] = g->board[j];
      n = g->movenr;
      do back(g); while (g->movenr > i);
      for (j=0; j<ESize; j++)
        if ((diff = (curboard[j] != g->board[j])))
          break;
      do forward(g); while (g->movenr < n);
/* only works if forward doesn't check for superko:( */
      free(curboard);
      if (!diff)
        return 1;
#endif
    }
  }
  return 0;
}

int play(struct minkgame *g, int p, int ko)	/* return whether move is legal */
{
  int c;
  struct minkmove *mv;

  if (g->board[p] != MINK_EMPTY || p == g->moves[g->movenr].ko)
    return 0;
  if (g->rules == RULES_ING &&
     (g->movenr&1) && g->movenr < 2 * g->handicap)
    return 0;			/* w should pass handicap-1 turns */
  growmoves(g);
  mv = &(g->moves[++g->movenr]);
  g->board[p] = c = LASTCOLOR(g);
  g->hash ^= Zob[p][c];
  mv->point = p;
  ufmark(g);
  ufset(g,g->root=p,0);	/* zero liberties for the moment */
  g->kostat = 0;
/*  if (g->nocaps == 0) { */
    mv->up = neighbour(g,p-Vdiff,c);
    mv->left = neighbour(g,p-1,c);
    mv->right = neighbour(g,p+1,c);
    mv->down = neighbour(g,p+Vdiff,c);
    if ((mv->self = !g->uf[g->root])) {	/* suicide */
      capture(g,p,c); 
      if (g->rules == RULES_NET) {
        back(g);
        return 0;				/* forbidden:( */
      }
    }
/*  } */
  if (ko && superko(g)) {
    back(g);
    return 0;
  }
  mv->ko = g->kostat;
  mv->hash = g->hash;
  return 1;
}

int pass(struct minkgame *g)	/* if pass is i'th consecutive one, return i */
{
  int i;

  growmoves(g);
  g->moves[i = ++g->movenr].point = MINK_PASS;
  g->moves[g->movenr].ko = 0;
  g->moves[g->movenr].hash = g->moves[g->movenr-1].hash;
  while (g->moves[--i].point == MINK_PASS) ;
  return g->movenr - i;
}


/* remove group of color c at p; return whether succesful */
/* for NET compatibility, allow arbitrary removes for now */
int removedead(struct minkgame *g, int p, int c)
{
  if (0 && g->board[p] != c)
    return 0;
  return capture(g,p,g->board[p]) ? 1 : 0;
}


void replay(struct minkgame *g)	/* replay game, e.g. to undo all removes */
{
  int p,movecnt = g->movenr;

  Logit("replaying to move nr. %d.", movecnt);
  startgame(g);
  if (movecnt && (p = g->moves[1].point) < -1)
    sethcap(g,-1-p);
  while (g->movenr < movecnt) {
/*    Logit("at move %d now.", g->movenr); */
    forward(g);
  }
}


#if 0
static void showpass(struct minkgame *g)
{
  pass(g);
  printf("%c passes.\n",BOARDCHARS[LASTCOLOR(g)]);
}
#endif


static char cnv_file2ch(int i)
{
  return 'A' + i-1 >= 'I' ?  'A' + i : 'A' + i-1;
}


void listmove(struct minkgame *g, int i, char *buf)	/* list move i in game g */
{
  int pt;

  *buf++ = (i&1) ? 'B' : 'W';
  pt = g->moves[i].point;
  if (pt < -1)
    sprintf(buf,"Handicap %d",-1-pt);
  else if (pt == MINK_PASS)
    sprintf(buf,"Pass");
  else sprintf(buf, "%c%d", cnv_file2ch(pt%Vdiff), pt/Vdiff);
}


void printboard(struct minkgame *g, twodstring buf)
{
  int p,x,y,xpos;
  const char *star;

  sprintf(buf[0],"   ");
  for (x=1; x<=g->width; x++)
     sprintf(buf[0]+2*x+1," %c", cnv_file2ch(x));
  for (y = 1; y <= g->height; y++)
  {
    sprintf(buf[y],"%2d |", g->height+1-y);
    for (x=1, xpos=4; ; x++)
    {
      buf[y][xpos++] = BOARDCHARS[g->board[point(g,x,g->height+1-y)]];
      if (x == g->width)
        break;
      buf[y][xpos++] = ' ';
    }
    sprintf(buf[y]+xpos,"| %2d", g->height+1-y);
  }
  sprintf(buf[y],"   ");
  for (x=1; x<=g->width; x++)
     sprintf(buf[y]+2*x+1," %c", cnv_file2ch(x));
  p = g->moves[g->movenr].point;
  if (p > 0 && p != MINK_PASS ) {
    y = g->height + 1 - p / Vdiff;
    x = 2 * (p % Vdiff) + 2;
    buf[y][2] = BOARDCHARS[9];
    buf[y][x-1] = BOARDCHARS[9];
    buf[y][x+1] = BOARDCHARS[10];
    buf[y][2*g->width+4] = BOARDCHARS[10];
  }
  if (g->width < (int) sizeof stars)
  {
    star = stars[g->width];
    while (*star)
    {
      x = *star++ - 'a' + 1;
      y = *star++ - 'a' + 1;
      if (g->board[point(g,x,y)] == MINK_EMPTY)
        buf[g->height+1-y][x*2+2] = BOARDCHARS[8];
    }
  }
}

void statusdims(struct minkgame *g, int *width, int *height)
{
  *width = g->width;
  *height = g->height;
}

/* broken just like IGS protocol; only works for height==width */
void boardstatus(struct minkgame *g, twodstring buf)
{
  int x,y;

  for (y=0; y<g->height; y++) {
    for (x=0; x < g->width; x++)
      buf[y][x] = "201 3543"[g->board[point(g,y+1,g->height-x)]];
    buf[y][x] = '\0';
  }
}

static int findowner(struct minkgame *g, int p)
{
  if (g->board[p] == MINK_EDGE || g->board[p] == 4+MINK_EMPTY)
    return 0;
  if (g->board[p] != MINK_EMPTY)
    return g->board[p];
  g->board[p] = 4+MINK_EMPTY;
  return findowner(g,p-Vdiff) | findowner(g,p-1)
         | findowner(g,p+1) | findowner(g,p+Vdiff);
}

static void setowner(struct minkgame *g, int p, int c)
{
  if (g->board[p] == 4+MINK_EMPTY) {
    g->board[p] = 4 + c;
    setowner(g,p-Vdiff,c);
    setowner(g,p-1,c);
    setowner(g,p+1,c);
    setowner(g,p+Vdiff,c);
  }
}

void getcaps(struct minkgame *g, int *wh, int *bl)
{
  *wh = g->caps[MINK_WHITE];
  *bl = g->caps[MINK_BLACK];
}

void countscore(struct minkgame *g, twodstring buf, int *wt, int *bt, int *wo, int *bo)
{
  int p,own;

  *wt = *bt = *wo = *bo = 0;  /* territory and occupied */
  for (p=Vdiff+1; p<ESize-Vdiff; p++) {
    if (g->board[p] == MINK_EDGE)
      continue;
    if (g->board[p] == MINK_EMPTY && (own = findowner(g,p)))
      setowner(g,p,own);
    if (g->board[p] == MINK_BLACK)
      (*bo)++;
    else if (g->board[p] == 4+MINK_BLACK)
      (*bt)++;
    else if (g->board[p] == MINK_WHITE)
      (*wo)++;
    else if (g->board[p] == 4+MINK_WHITE)
      (*wt)++;
  }
  boardstatus(g, buf);
  for (p=Vdiff+1; p<ESize-Vdiff; p++)
    if (g->board[p] >= 4)
      g->board[p] = MINK_EMPTY;
}
