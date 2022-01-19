/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1997  J. Alan Eldridge (alane@wozzle.york.cuny.edu)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

#ifndef LADDER_H
#define LADDER_H

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>   /* for the FILE declarations below */

#ifdef HAVE_TIME_H
#include <time.h>
#endif


#ifdef __cplusplus
extern "C" {
#endif /* C++ */

struct player {
  int idx;
  char *szName;
  time_t tLast;
  void *pvUser;
  int nWins, nLosses;
} ;

void LadderInit(int nLadders);

int LadderNew(int maxPlayers);
void LadderDel(int id);

int LadderCnt(int id);

const struct player *PlayerAt(int id, int at);

void PlayerSift(int idx, int nDays);
void PlayerUpdTime(int id, int at, time_t t);
void PlayerSetData(int id, int at, void *pv);
void PlayerAddWin(int id, int at);
void PlayerAddLoss(int id, int at);

void PlayerKillAt(int id, int at);
void PlayerRotate(int id, int from, int to);

const struct player *PlayerNamed(int id, const char *psz);

const struct player * const *PlayersSortedByPosn(int id); /* just the array, in order,
						 with a lot of const shit so
						 you can't f*** it up */

const struct player * const *PlayersSortedByName(int id);

int PlayerNew(int id, const char *szName); /* add at end - return 0 if full */

void PlayerDump(FILE *pf, const struct player *p);

int PlayerSave(FILE *pf, int id);
int PlayerLoad(FILE *pf, int id);

#ifdef __cplusplus
}
#endif /* C++ */

#endif /* LADDER_H */
