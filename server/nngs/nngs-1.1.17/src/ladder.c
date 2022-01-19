/*
    NNGS - The No Name Go Server
    Copyright (C) 1995  J. Alan Eldridge (alane@wozzle.york.cuny.edu)

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

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif


#include "utils.h"
#include "common.h"
#include "ladder.h"

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif

struct ladder {
  int n;
  int max;
  int fNameSorted;
  struct player **byPosn;
  struct player **byName;
} ;

int dbmax;
static struct ladder *db;


void LadderDel(int idx)
{
  if (db[idx].max) {
    int i;

    for (i = 0; i < db[idx].n; i++) {
      free(db[idx].byPosn[i]);
    }

    free(db[idx].byPosn);
  }

  db[idx].n = db[idx].max = 0;
  db[idx].byPosn = db[idx].byName = 0;
}

void LadderInit(int n)
{
  assert((db = calloc(dbmax = n, sizeof *db)) != NULL);
}


static int _LadderNew(int idx, int n)
{
  db[idx].n = 0;
  db[idx].max = n;
  db[idx].fNameSorted = 0;
  assert(db[idx].byPosn = calloc(n, sizeof *db[idx].byPosn));
  assert(db[idx].byName = calloc(n, sizeof *db[idx].byName));

  return idx;
}

int LadderNew(int n)
{
  int idx;

  for (idx = 0; idx < dbmax; idx++) {
    if (db[idx].max == 0) {
      break;
    }
  }

  if (idx == dbmax) {
    return 0;
  }

  return _LadderNew(idx, n);
}

#ifdef WANT_UNUSED
static int PlayerCnt(int idx)
{
  return db[idx].n;
}
#endif /* WANT_UNUSED */

const struct player *PlayerAt(int idx, int i)
{
  if (i >= 0 && i < db[idx].n) {
    return db[idx].byPosn[ i ];
  }
  return 0;
}

void PlayerUpdTime(int idx, int i, time_t t)
{
  if (i >= 0 && i < db[idx].n) {
    db[idx].byPosn[i]->tLast = t;
  }
}

#ifdef WANT_UNUSED
static void PlayerUpdData(int idx, int i, void *pv)
{
  if (i >= 0 && i < db[idx].n) {
    db[idx].byPosn[i]->pvUser = pv;
  }
}
#endif /* WANT_UNUSED */

void PlayerAddWin(int idx, int i)
{
  if (i >= 0 && i < db[idx].n) {
    db[idx].byPosn[i]->nWins++;
  }
}

void PlayerAddLoss(int idx, int i)
{
  if (i >= 0 && i < db[idx].n) {
    db[idx].byPosn[i]->nLosses++;
  }
}

static void copy(int idx, int dst, int src)
{
  db[idx].byPosn[ dst ] = db[idx].byPosn[ src ];
  db[idx].byPosn[ dst ]->idx = dst;
}

void PlayerKillAt(int idx, int target)
{
  if (target >= 0 && target < db[idx].n) {
    int i;
    int lim = --db[idx].n;

    db[idx].fNameSorted = 0;
    free(db[idx].byPosn[ target ]);
    for (i = target; i < lim; i++) {
      copy(idx, i, i + 1);
    }
  }
}

void PlayerRotate(int idx, int from, int to)
{
  if (from >= 0 && to >= 0 && from < db[idx].n && to < db[idx].n 
      && from < to) {
    int i;
    int lim = from + 1;
    struct player *p = db[idx].byPosn[ to ];

    for (i = to; i >= lim; i--) {
      copy(idx, i, i - 1);
    }

    db[idx].byPosn[ from ] = p; p->idx = from;
  }
}

const struct player *PlayerNamed(int idx, const char *psz)
{
  int i;

  for (i = 0; i < db[idx].n; i++) {
    if (!strcmp(db[idx].byPosn[ i ]->szName, psz)) {
      return db[idx].byPosn[ i ];
    }
  }

  return 0;
}

const struct player * const *PlayersSortedByPosn(int idx)
{
  return db[idx].n ? (const struct player * const *)db[idx].byPosn : 0;
}

static int cmpByName(const void *p1, const void *p2)
{
#define P1 (*(const struct player * const *)p1)
#define P2 (*(const struct player * const *)p2)

  return strcmp(P1->szName, P2->szName);

#undef P1
#undef P2  
}

const struct player * const *PlayersSortedByName(int idx)
{
  if (db[idx].n) {
    if (!db[idx].fNameSorted) {
      db[idx].fNameSorted = 1;
      memset(db[idx].byName, 0, db[idx].max * sizeof *db[idx].byName);
      memcpy(db[idx].byName, db[idx].byPosn, db[idx].n * sizeof *db[idx].byName);
      qsort(db[idx].byName, db[idx].n, sizeof *db[idx].byName, cmpByName);
    }
    return (const struct player * const *)db[idx].byName;
  }

  return 0;
}

int PlayerNew(int idx, const char *psz)
{
  struct player *p;

  if (db[idx].n >= db[idx].max) {
    return 0;
  }

  assert(p = malloc(sizeof *p));

  p->tLast = 0;
  p->pvUser = 0;
  p->szName = mystrdup(psz);
  p->nWins = p->nLosses = 0;
  db[idx].byPosn[ p->idx = db[idx].n++ ] = p;
  db[idx].fNameSorted = 0;
  
  return 1;
}

void PlayerDump(FILE *pf, const struct player *pp)
{
  fprintf(pf, "%03d \"%s\" %lu %d %d\n", pp->idx, pp->szName, pp->tLast,
	  pp->nWins, pp->nLosses);
}

int PlayerSave(FILE *pf, int idx)
{
  int i;

  fprintf(pf, "Max: %d\n", db[idx].max);
  for (i = 0; i < db[idx].n; i++) {
    PlayerDump(pf, db[idx].byPosn[ i ]);
  }
  return db[idx].n;
}

static void setstats(int idx, int at, int wins, int losses)
{
  struct player *p = db[idx].byPosn[at];

  p->nWins = wins;
  p->nLosses = losses;
}

int PlayerLoad(FILE *pf, int idx)
{
  int max;
  time_t tLast;
  char name[ 100 ];

  char linebuff[ 200 ];

  if (fgets(linebuff, sizeof linebuff, pf)) { 
    int i = 0, dummy, nWins, nLosses;
    if(0>= sscanf(linebuff, "Max: %d", &max)) return -1;

    LadderDel(idx);
    _LadderNew(idx, max);
    while (fgets(linebuff, sizeof linebuff, pf)) {
      if(0 >= sscanf(linebuff, "%d \"%[^\"]\" %lu %d %d", 
		  &dummy, name, &tLast, &nWins, &nLosses) ) break;
      PlayerNew(idx, name);
      PlayerUpdTime(idx, i, tLast);
      setstats(idx, i++, nWins, nLosses);
    }

    return db[idx].n;
  }
  return -1;
}

static void renumber(int idx)
{
  int i;
  for (i = 0; i < db[idx].n ; i++) {
    db[idx].byPosn[i]->idx = i;
  }
}

void PlayerSift(int idx, int nDays)
{
  int n;
  int nCurr;
  int *pCurr;
  int nLate;
  int *pLate;
  time_t tNow = globclock.time;
  const time_t tDay = (time_t)(24 * 60 * 60);
  const time_t tCut = tNow - (nDays * tDay);
  struct player **pp;

  nCurr = nLate = 0;
  pCurr = calloc(db[idx].n, sizeof *pCurr);
  pLate = calloc(db[idx].n, sizeof *pLate);
  pp = calloc(db[idx].n, sizeof *pp);

  for (n = 0; n < db[idx].n ; n++) {
    if ((db[idx].byPosn[n]->tLast < tCut) || 
       ((db[idx].byPosn[n]->nWins == 0)   && 
        (db[idx].byPosn[n]->nLosses == 0))) {
      pLate[ nLate++ ] = n;
    } else {
      pCurr[ nCurr++ ] = n;
    }
  }

  for (n = 0; n < nCurr; n++) {
    pp[ n ] = db[idx].byPosn[ pCurr[n] ];
  }
  for (n = 0; n < nLate; n++) {
    pp[ n + nCurr ] = db[idx].byPosn[ pLate[n] ];
    /*pp[ n + nCurr ]->tLast = tNow - tDay;*/
  }
  memcpy(db[idx].byPosn, pp, db[idx].n * sizeof *db[idx].byPosn );
  renumber(idx);
  free(pp);
  free(pCurr);
  free(pLate);
}

