/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1997 John Tromp (tromp@daisy.uwaterloo.ca/tromp@cwi.nl)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

#ifndef MINK_H
#define MINK_H

#include <stdio.h>	/* for the FILE declarations below */

#if 0
#ifndef __CYGWIN__
#include <sys/types.h>	/* for the ulong declarations below */
#else
typedef unsigned long ulong; /* cygwin doesn't have it in sys/types.h */
#endif
#else
typedef unsigned long xulong; /* better create our own typedef names */
#endif

#define MINK_EMPTY	0		/* these 3 colors must be in [0..3] */
#define MINK_BLACK	1
#define MINK_WHITE	2

#define MINK_EDGE	-1
#define MINK_PASS	-1
#define MINK_KOMI 5.5		/* default value for komi */

#define FIXEDHCAP 1             /* If you want fixed handicaps by default */

struct uf_log_ {
  int index, value;		/* logs every change to uf for later undo */
} ;

struct minkmove {
  int point,ko;			/* point played (or PASS), forbidden nextmove */
  unsigned int left:1, right:1, up:1, down:1, self:1;	/* captures made inc. suicide */
  xulong hash;
} ;

struct minkgame {
  int width, height;	/* board dimensions */
  int rules;		/* what rules are we playing by? */
  struct minkmove *moves;		/* move history */
  int mvsize,movenr;	/* size of moves and number of moves played */
  int *board;		/* current state of go-board */
  int *uf;		/* union-find structure */
  struct uf_log_ *uflog;	/* log of changes to uf */
  int logsize,logid;	/* size of uflog & number of changes to uf */
#ifdef MINKKOMI
  float komi;
#endif
  /*int nocaps; */		/* don't make captures, e.g. to play other games */
  int handicap;         /* handicap */
  int caps[4];		/* # stones captured */
  xulong hash;
  int root,kostat;	/* root is temporary variable used in group merging */
} ;

/* public functions */
extern void initmink(void);
extern struct minkgame *initminkgame(int, int, int);	/* initialize game record */
extern void savegame(FILE *fp, struct minkgame *g, struct mvinfo *mi, int nmvinfos);
extern int loadgame(FILE *fp, struct minkgame *g);	/* returns player_to_move if OK */
extern void freeminkgame(struct minkgame *g);	/* frees up game record */
extern int sethcap(struct minkgame *g, int n);     /* returns 1 if succesful */
extern void setnocaps(struct minkgame *g, int val);     /* returns 1 if succesful */
extern xulong gethash(struct minkgame *g); /* get a hashcode for current board */
#ifdef MINKKOMI
extern void setkomi(struct minkgame *g, float k);      /* set the komi */
extern float getkomi(struct minkgame *g);              /* get the komi */
#endif
extern int movenum(struct minkgame *g);    /* return move number (#moves played in game) */
extern int back(struct minkgame *g);	        /* take back last move */
extern void forward(struct minkgame *g);	/* move forward one move */
extern int go_move(struct minkgame *g, char *s);   /* return point != 0 if s is go_move */
extern void listmove(struct minkgame *g, int i, char *buf);  /* list move i in game g */
extern int point(struct minkgame *g, int x, int y);	/* convert coords to point */
extern int play(struct minkgame *g, int p, int ko);	/* play at position p */
extern int removedead(struct minkgame *g, int p, int c);
extern void replay(struct minkgame *g);   /* replay game so as to undo all removes */
extern int pass(struct minkgame *g);   /* if pass is i'th consecutive one, return i */
extern void printboard(struct minkgame *g, twodstring buf); /* print ascii representation of go board */
extern void statusdims(struct minkgame *g, int *width, int *height);
extern void boardstatus(struct minkgame *g, twodstring buf);
extern void countscore(struct minkgame *g, twodstring buf, int *wt, int *bt, int *wo, int *bo);	/* count w/b territory and w/b occupied */
extern void getcaps(struct minkgame *g, int *wh, int *bl); /* #w/b stones captured */
extern int loadpos(FILE *, struct minkgame *);

/* private functions */
#if MINK_C
static char cnv_file2ch(int i);	/* convert file number 0-18 to character */
static void ufmark(struct minkgame *g);/* put a move-marker on union-find log */
#endif /* MINK_C */
/* none of yer business:) */

#endif /* MINK_H */
