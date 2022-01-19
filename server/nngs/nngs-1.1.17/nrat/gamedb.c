/* gamedb.c
 *
 */
/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1996 Erik Van Riper (geek@willent.com)
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
#include <unistd.h>

#ifdef HAVE_CTYPE_H
#include <ctype.h>
#endif

#ifdef HAVE_STRING_H
#include <string.h> /* strchr() and friends */
#endif

#ifdef HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif

#ifdef HAVE_TIME_H
#include <time.h>
#endif


#include "nngsconfig.h"
#include "nngsmain.h"
#include "gamedb.h"
#include "gameproc.h"
#include "common.h"
#include "command.h"
#include "servercodes.h"
#include "playerdb.h"
#include "utils.h"

#ifdef NNGSRATED
#include "rdbm.h"		/* PEM */
#endif

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif

#ifndef DEBUG_GAME_SLOT
#define DEBUG_GAME_SLOT 0
#endif

static int get_empty_game_slot(void);
static void game_zero(struct game *g, int size);
static void game_free(struct game *g);

/* [PEM]: A desperate attempt to fix the komi bug when loading
** adjourned games. :-(
**
** Note: This is not a complete string-to-float parser, it doesn't
**       handle exponents.
*/
static float
nngs_strtof(char *str, char **p)
{
  int ok = 0, sign = 1;
  float f = 0.0;
  char *s = str;

  while (isspace((int)*s))
    ++s;
  if (*s == '+')
    ++s;
  else if (*s == '-')
  {
    sign = -1;
    ++s;
  }
  while (isdigit((int)*s))
  {
    f = 10 * f + (*s - '0');
    ok = 1;
    ++s;
  }
  if (*s == '.')
  {
    float d = 10.0;

    ++s;
    while (isdigit((int)*s))
    {
      f += (*s - '0')/d;
      d *= 10;
      ok = 1;
      ++s;
    }
  }
  if (ok)
  {
    *p = s;
    return sign*f;
  }
  *p = str;
  return 0.0;
}

struct game *garray = NULL;
int garray_top = 0;

static int get_empty_game_slot(void)
{
  int i;

  for (i = 0; i < garray_top; i++) {
#if (DEBUG_GAME_SLOT >= 2)
    Logit("empty_game_slot:= %d/%d st=%d", i, garray_top, garray[i].gstatus );
#endif
    if (garray[i].gstatus != GSTATUS_EMPTY) continue;
#if DEBUG_GAME_SLOT
    /* if(Debug) */
      Logit("get_empty_game_slot:= %d/%d (Found one)", i, garray_top);
#endif
    return i;
  }
  garray_top++;
  if (!garray) {	/* garray_top was 0 before */
    garray = malloc(sizeof *garray * garray_top);
  } else {
    garray = realloc(garray, sizeof *garray * garray_top);
  }
  if(Debug)
    Logit("get_empty_game_slot, garray_top = %d, i = %d (Had to alloc)", garray_top,i);
  return garray_top - 1;
}

int game_new(int type, int size)
{
  int i; 
  
  i = get_empty_game_slot();
  if(Debug) Logit("In game_new, i = %d", i);
  game_zero(&garray[i], size);
  garray[i].gotype = type;
  return i;
}

static void game_zero(struct game *g, int size)
{
  memset(g, 0, sizeof *g);
  g->gotype = GAMETYPE_GO;
  g->white.pnum = -1;
  g->black.pnum = -1;
  g->white.old_num = -1;
  g->black.old_num = -1;
  g->gstatus = (size > 0) ? GSTATUS_NEW : GSTATUS_EMPTY;
  g->rated = 1;
  /* g->nocaps = 0; */
  g->Private = 0;
  g->komi = 0.5;
  g->result = END_NOTENDED;
  g->num_pass = 0;
  g->gtitle = NULL;
  g->gevent = NULL;
  g->GoGame = NULL;
  g->Teach = 0;
  g->Teach2 = 0;
  g->ts.time_type = TIMETYPE_UNTIMED;
  g->ts.totalticks = SECS2TICS(300);
  g->ts.byoticks = SECS2TICS(300);
  g->ts.byostones = 25;
  g->white.ticksleft = SECS2TICS(300);
  g->white.byostones = -1;
  g->white.byoperiods = 0;
  g->black.ticksleft = SECS2TICS(300);
  g->black.byostones = -1;
  g->black.byoperiods = 0;
  g->Ladder9 = 0;
  g->Ladder19 = 0;
  g->Tourn = 0;
  g->Ladder_Possible = 0;
#ifdef PAIR
  g->pairwith = 0;;
  g->pairstate = NOTPAIRED;
#endif
  if(size) {
    g->mvinfos = malloc(sizeof *g->mvinfos);
    g->mvinfos[0].kibitz = NULL;
    g->mvinfos[0].last = &(g->mvinfos[0].kibitz);
    g->nmvinfos = 1;
  } else {
    g->mvinfos = NULL;
    g->nmvinfos = 0;
  }
}

static void freekib(struct kibitz *k)
{
  if (k == NULL)
    return;
  freekib(k->next);
  free(k->mess);
  free(k);
}

static void freemvinfos(struct game *g)
{
  int i;

  for (i=0; i<g->nmvinfos; i++)
    freekib(g->mvinfos[i].kibitz);
  free(g->mvinfos);
  g->mvinfos = NULL;
}

static void game_free(struct game *g)
{
  if(g->gotype >= GAMETYPE_GO) {
    g->gotype = 0;
    if (g->GoGame) freeminkgame(g->GoGame);
    if (g->mvinfos) freemvinfos(g);
    free(g->gtitle); g->gtitle = NULL;
    free(g->gevent); g->gevent = NULL;
  }
}


int game_remove(int g)
{

  /* Should remove game from players observation list */
  game_free(&garray[g]);
  game_zero(&garray[g],0);
  garray[g].gstatus = GSTATUS_EMPTY; 
  return 0;
}

/* This may eventually save old games for the 'oldmoves' command */
int game_finish(int g)
{
  player_game_ended(g);	/* Alert playerdb that game ended */
  NewOldGame(g);	/* remember game until both players disconnect */
  return 0;
}

void add_kib(struct game *g, int movecnt, char *s)
{
  int i;
  struct kibitz **k;

  if(s == NULL) {Logit("s is null"); return; }
  /*Logit("Adding kibitz >%s< at move %d", s, movecnt); */
  while (movecnt >= g->nmvinfos) {
    i = g->nmvinfos;
    g->nmvinfos *= 2;
    g->mvinfos = realloc(g->mvinfos, g->nmvinfos * sizeof *g->mvinfos);
    for (; i< g->nmvinfos; i++) {
      g->mvinfos[i].kibitz = NULL;
      g->mvinfos[i].last = &(g->mvinfos[i].kibitz);
    }
  }
  k = g->mvinfos[movecnt].last;
  *k = malloc(sizeof **k);
  (*k)->next = NULL;
  (*k)->mess = mystrdup(s);
  g->mvinfos[movecnt].last = &((*k)->next);
}

void send_go_board_to(int g, int p)
{
  int side, count;
  int observing = 0;
  int yy, wc, bc;
  twodstring statstring;
  char buf[20];
  char bbuf[20], wbuf[20];

  if (parray[p].game == g) {
    side = parray[p].side;
  } else {
    observing = 1;
    side = PLAYER_WHITE;
  }
  /*game_update_time(g);*/
  getcaps(garray[g].GoGame, &wc, &bc);
  bbuf[0] = wbuf[0] = 0;
  if(parray[p].i_verbose) {
    if(garray[g].black.byostones > 0) sprintf(bbuf, " B %d", garray[g].black.byostones);
    if(garray[g].white.byostones > 0) sprintf(wbuf, " B %d", garray[g].white.byostones);
    printboard(garray[g].GoGame, statstring);
    count = movenum(garray[g].GoGame);
    if(count > 0) listmove(garray[g].GoGame, count, buf);
    pcn_out(p, CODE_CR1|CODE_OBSERVE, FORMAT_GAME_d_I_s_ss_VS_s_ss_n,
                /*SendCode(p, CODE_BEEP),*/
		g + 1,
		parray[garray[g].white.pnum].pname,
		parray[garray[g].white.pnum].srank,
                parray[garray[g].white.pnum].rated ? "*" : " ",
		parray[garray[g].black.pnum].pname,
		parray[garray[g].black.pnum].srank,
                parray[garray[g].black.pnum].rated ? "*" : " ");
    for(yy=0; yy<(garray[g].GoGame->height) + 2; yy++){
      if(yy == 0) 
        pcn_out(p, CODE_OBSERVE, FORMAT_s_H_CAP_d_KOMI_fn, 
		/*SendCode(p, CODE_BEEP),*/
		statstring[yy],
		garray[g].GoGame->handicap,
		garray[g].komi);
      else if(yy==1)
	pcn_out(p, CODE_OBSERVE, FORMAT_s_CAPTURED_BY_dn, statstring[yy], wc);
      else if(yy==2)
	pcn_out(p, CODE_OBSERVE, FORMAT_s_CAPTURED_BY_O_dn, statstring[yy], bc);
      else if((yy==4) && (garray[g].GoGame->height > 3))
	pcn_out(p, CODE_OBSERVE, FORMAT_s_WH_TIME_ssn,
                statstring[yy], hms(TICS2SECS(garray[g].white.ticksleft), 1, 1, 0),
                wbuf );
      else if((yy==5) && (garray[g].GoGame->height > 4))
	pcn_out(p, CODE_OBSERVE, FORMAT_s_BL_TIME_ssn,
                statstring[yy], hms(TICS2SECS(garray[g].black.ticksleft), 1, 1, 0),
                bbuf);
      else if((yy==7) && (garray[g].GoGame->height > 6))
	if(count == 0)
	  pcn_out(p, CODE_OBSERVE, FORMAT_s_LAST_MOVE_n, statstring[yy]);
 	else
	  pcn_out(p, CODE_OBSERVE, FORMAT_s_LAST_MOVE_sn, statstring[yy], buf + 1);
      else if((yy==8) && (garray[g].GoGame->height > 7))
 	if(count == 0)
	  pcn_out(p, CODE_OBSERVE, FORMAT_s_0_O_WHITE_n, statstring[yy]);
   	else
	  pcn_out(p, CODE_OBSERVE, FORMAT_s_d_s_s_n, statstring[yy],
                      count, (buf[0] == 'B') ? "#" : "O", 
		      (buf[0] == 'B') ? "Black" : "White");
      else if((yy>=10) && (garray[g].GoGame->height > (yy - 1))) {
	if(count > 1) {
	  count--;
          listmove(garray[g].GoGame, count, buf);
          pcn_out(p, CODE_OBSERVE, FORMAT_s_c_d_sn, statstring[yy],
		      buf[0], count, buf + 1);
	}
	else pcn_out(p, CODE_OBSERVE, FORMAT_sn, statstring[yy]);
      }
      else pcn_out(p, CODE_OBSERVE, FORMAT_sn, statstring[yy]);
    }
  }
  pprintf_prompt(p, ""); 
}

void send_go_boards(int g, int players_only)
{
  int p, bc, wc, wp, bp;
  char buf[20], outStr[1024];

  listmove(garray[g].GoGame, movenum(garray[g].GoGame), buf); 
  getcaps(garray[g].GoGame, &wc, &bc);

  bp = garray[g].black.pnum;
  wp = garray[g].white.pnum;
  sprintf(outStr, "Game %d %s: %s (%d %d %d) vs %s (%d %d %d)\n",
        g + 1, "I",
        parray[wp].pname, bc,
        TICS2SECS(garray[g].white.ticksleft),
        garray[g].white.byostones,

        parray[bp].pname, wc,
        TICS2SECS(garray[g].black.ticksleft),
        garray[g].black.byostones);

  if((parray[wp].i_verbose) && (garray[g].Teach == 0)) 
    send_go_board_to(g, wp);

  else if((parray[wp].protostate != STAT_SCORING) && (garray[g].Teach == 0))  { 
    pcn_out(wp, CODE_MOVE, FORMAT_s,outStr);
    if((movenum(garray[g].GoGame) - 1) >= 0) {
      pcn_out(wp, CODE_MOVE, FORMAT_d_c_sn,
        movenum(garray[g].GoGame) - 1, buf[0], buf + 1); 
    }
    if(parray[wp].bell) pcn_out_prompt(wp, CODE_CR1|CODE_BEEP, FORMAT_n);
    else pcn_out_prompt(wp, CODE_CR1|CODE_NONE,FORMAT_n);
  }

  if(parray[bp].i_verbose) send_go_board_to(g, bp);

  else if(parray[bp].protostate != STAT_SCORING) {
    pcn_out(bp, CODE_MOVE, FORMAT_s, outStr);
    if((movenum(garray[g].GoGame) - 1) >= 0) {
      pcn_out(bp, CODE_MOVE, FORMAT_d_c_sn,
        movenum(garray[g].GoGame) - 1, buf[0], buf + 1); 
    }
    if(parray[bp].bell) pcn_out_prompt(bp, CODE_BEEP, FORMAT_n);
    else pcn_out_prompt(bp, CODE_NONE, FORMAT_n);
  }

  if(players_only) return;

  for (p = 0; p < parray_top; p++) {
    if (!parray[p].slotstat.is_online) continue;
    if (!player_is_observe(p, g)) continue;
    if (parray[p].game == g) continue;
    if (parray[p].i_verbose) send_go_board_to(g, p);
    else {
      pcn_out(p, CODE_MOVE, FORMAT_s, outStr);
      pcn_out(p, CODE_MOVE, FORMAT_d_c_sn,
         movenum(garray[g].GoGame) - 1, buf[0], buf + 1);
      if(parray[p].bell) pcn_out_prompt(p, CODE_BEEP, FORMAT_n);
      else pcn_out_prompt(p, CODE_NONE, FORMAT_n);
    }
  }
} 


int game_get_num_ob(int g)
{
  int p, t, count = 0;

  for(p = 0; p < parray_top; p++) {
    for(t = 0; t < parray[p].num_observe; t++) {
      if(parray[p].observe_list[t] == g) count++;
      }
  }
  return count;
}

static int oldGameArray[MAXOLDGAMES];
static int numOldGames = 0;

static int RemoveOldGame(int g)
{
  int i;

  for (i = 0; i < numOldGames; i++) {
    if (oldGameArray[i] == g) break;
  }
  if (i == numOldGames)
    return -1;			/* Not found! */
  for (; i < numOldGames - 1; i++)
    oldGameArray[i] = oldGameArray[i + 1];
  numOldGames--;
  game_remove(g);
  return 0;
}

static int AddOldGame(int g)
{
  if (numOldGames == MAXOLDGAMES)	/* Remove the oldest */
    RemoveOldGame(oldGameArray[0]);
  oldGameArray[numOldGames] = g;
  numOldGames++;
  return 0;
}

int FindOldGameFor(int p)
{
  int i;

  if (p < 0)
    return -1;
  for (i = numOldGames - 1; i >= 0; i--) {
    if (garray[oldGameArray[i]].white.old_num == p) return oldGameArray[i];
    if (garray[oldGameArray[i]].black.old_num == p) return oldGameArray[i];
  }
  return -1;
}

/* This just removes the game if both players have new-old games */
int RemoveOldGamesForPlayer(int p)
{
  int g;

  g = FindOldGameFor(p);
  if (g < 0)
    return 0;
  if (garray[g].white.old_num == p) garray[g].white.old_num = -1;
  if (garray[g].black.old_num == p) garray[g].black.old_num = -1;
  if (garray[g].white.old_num == -1 && garray[g].black.old_num == -1) {
    RemoveOldGame(g);
  }
  return 0;
}

/* This recycles any old games for players who disconnect */
int ReallyRemoveOldGamesForPlayer(int p)
{
  int g;

  g = FindOldGameFor(p);
  if (g < 0)
    return 0;
  RemoveOldGame(g);
  return 0;
}

int NewOldGame(int g)
{
  RemoveOldGamesForPlayer(garray[g].white.pnum);
  RemoveOldGamesForPlayer(garray[g].black.pnum);
  garray[g].white.old_num = garray[g].white.pnum;
  garray[g].black.old_num = garray[g].black.pnum;
  garray[g].gstatus = GSTATUS_STORED;
  AddOldGame(g);
  return 0;
}

void game_disconnect(int g, int p)
{
#ifdef PAIR
  if(paired(parray[p].game)) {
    game_ended(garray[g].pairwith, PLAYER_NEITHER, END_LOSTCONNECTION);
  }
#endif /* PAIR */
  game_ended(g, PLAYER_NEITHER, END_LOSTCONNECTION);
}

static void savekib(FILE *fp, struct game *g)
{
  int i;
  struct kibitz *kp;

  fprintf(fp, "kibitz: oink\n");
  for (i=0; i < g->nmvinfos; i++) {
    for (kp = g->mvinfos[i].kibitz; kp; kp = kp->next) {
       fprintf(fp,"%d %s\n", i, kp->mess);
    }
  }
  fputc('\n', fp);
}

static void loadkib(FILE *fp, struct game *g)
{
  char buf[256];
  int i,k;

  while (fgets(buf, sizeof buf, fp)) {
    buf[strlen(buf) - 1] = '\0';	/* strip '\n' */
    if (buf[0] == '\0') {
      if(Debug) Logit("Got my blank line in loadkib");
      break;
    }
    if (buf[1] == '\0') {
      Logit("PANIC! Got my blank line in loadkib; buf[0]=%d",buf[0]);
      break;
    }
    sscanf(buf, "%d %n", &i,&k);
    add_kib(g, i, buf+k);
  }
}

static int got_attr_value(struct game *g, char *attr, char *value, FILE * fp, char *fname)
{
  if (!strcmp(attr, "timestart:")) {
    g->timeOfStart = atoi(value);
  } else if (!strcmp(attr, "timetype:")) {
    g->ts.time_type = atoi(value);
  } else if (!strcmp(attr, "rules:")) {
    g->rules = atoi(value);
  } else if (!strcmp(attr, "totalticks:")) {
    g->ts.totalticks = atoi(value);
  } else if (!strcmp(attr, "b_penalty:")) {
    g->black.penalty = atoi(value);
  } else if (!strcmp(attr, "b_over:")) {
    g->black.byoperiods = atoi(value);
  } else if (!strcmp(attr, "w_penalty:")) {
    g->white.penalty = atoi(value);
  } else if (!strcmp(attr, "w_over:")) {
    g->white.byoperiods = atoi(value);
  } else if (!strcmp(attr, "title:")) {
    g->gtitle = mystrdup(value);
  } else if (!strcmp(attr, "event:")) {
    g->gevent = mystrdup(value);
  } else if (!strcmp(attr, "handicap:")) {
    /*sethcap(g->GoGame, atoi(value))*/;
  } else if (!strcmp(attr, "size:")) {
    g->size = atoi(value);
    g->GoGame = initminkgame(g->size, g->size, g->rules);
  } else if (!strcmp(attr, "onmove:")) {
    ; /* PEM: Ignore. g->onMove = atoi(value); */
  } else if (!strcmp(attr, "ladder9:")) {
    g->Ladder9 = atoi(value);
  } else if (!strcmp(attr, "teach2:")) {
    g->Teach2 = atoi(value);
  } else if (!strcmp(attr, "teach:")) {
    g->Teach = atoi(value);
  } else if (!strcmp(attr, "ladder_possible:")) {
    g->Ladder_Possible = atoi(value);
  } else if (!strcmp(attr, "ladder19:")) {
    g->Ladder19 = atoi(value);
  } else if (!strcmp(attr, "tourn:")) {
    g->Tourn = atoi(value);
    if(g->Tourn == 1) {
      parray[g->white.pnum].match_type = GAMETYPE_TNETGO;
      parray[g->black.pnum].match_type = GAMETYPE_TNETGO;
    }
  } else if (!strcmp(attr, "w_time:")) {
    g->white.ticksleft = atoi(value);
  } else if (!strcmp(attr, "b_time:")) {
    g->black.ticksleft = atoi(value);
  } else if (!strcmp(attr, "byo:")) {
    g->ts.byoticks = atoi(value);
  } else if (!strcmp(attr, "byos:")) {
    g->ts.byostones = atoi(value);
  } else if (!strcmp(attr, "w_byo:")) {
    g->white.byoperiods = atoi(value);
  } else if (!strcmp(attr, "b_byo:")) {
    g->black.byoperiods = atoi(value);
  } else if (!strcmp(attr, "w_byostones:")) {
    g->white.byostones = atoi(value);
  } else if (!strcmp(attr, "b_byostones:")) {
    g->black.byostones = atoi(value);
  } else if (!strcmp(attr, "clockstopped:")) {
    g->clockStopped = atoi(value);
  } else if (!strcmp(attr, "rated:")) {
    g->rated = atoi(value);
/*  } else if (!strcmp(attr, "nocaps:")) {
    g->nocaps = atoi(value); */
  } else if (!strcmp(attr, "private:")) {
    g->Private = atoi(value);
  } else if (!strcmp(attr, "type:")) {
    g->type = atoi(value);
  } else if (!strcmp(attr, "time_type:")) {
    g->ts.time_type = atoi(value);
  } else if (!strcmp(attr, "gotype:")) {
    g->gotype = atoi(value);
  } else if (!strcmp(attr, "numpass:")) {
    g->num_pass = atoi(value);
  } else if (!strcmp(attr, "komi:")) {
    /*
    ** PEM: Changed to the locally defined nngs_strtof().
    */
    char *pp;

    g->komi = nngs_strtof(value, &pp);
    if (pp == value || g->komi < -12.0 || 12.0 < g->komi)
    {
      if (pp == value)
	Logit("Bad komi value \"%s\"", value);
      else
	Logit("Bad komi value \"%s\" --> %g", value, g->komi);
      g->komi = 5.5;
    }
  } else if (!strcmp(attr, "movesrnext:")) {  /* value meaningless */
    /* PEM: Get the true onMove. */
    switch (loadgame(fp, g->GoGame)) {
    case MINK_BLACK:
      g->onMove = PLAYER_BLACK;
      break;
    case MINK_WHITE:
      g->onMove = PLAYER_WHITE;
      break;
    }
  } else if (!strcmp(attr, "kibitz:")) {  /* value meaningless */
    loadkib(fp, g);
  } else {
    Logit("Error bad attribute >%s< from file %s", attr, fname);
  }
  /* setnocaps(g->GoGame, g->nocaps); */
  return 0;
}


#define MAX_GLINE_SIZE 1024
int game_read(struct game *g, int wp, int bp)
{
  FILE *fp;
  int len;
  char *attr, *value;
  char line[MAX_GLINE_SIZE];

  g->white.pnum = wp;
  g->black.pnum = bp;
  g->white.old_num = -1;
  g->black.old_num = -1;
  /* g->gameNum = g; */

  fp = xyfopen(FILENAME_GAMES_ws_s, "r", parray[wp].login, parray[bp].login);
  if (!fp) {
    return -1;
  }
  /* Read the game file here */
  while (fgets(line, sizeof line, fp)) {
    if ((len = strlen(line)) <= 1) continue;
    line[len - 1] = '\0';
    attr = eatwhite(line);
    if (attr[0] == '#') continue;			/* Comment */
    if (attr[0] == ';') {
      Logit("Read move %s from game record %s!", attr, filename() );
      continue;			/* Move!  Should not get here! */
    }
    value = eatword(attr);
    if (!*value) {
      Logit("Error reading file %s", filename() );
      continue;
    }
    *value = '\0';
    value++;
    value = eatwhite(value);
    if (!*value) {
      Logit("NNGS: Error reading file %s", filename() );
      continue;
    }
    stolower(attr);
    if (got_attr_value(g, attr, value, fp, filename() )) {
      fclose(fp);
      return -1;
    }
  }

  fclose(fp);
  g->gstatus = GSTATUS_ACTIVE;
  g->starttick = globclock.tick;
  g->lastMovetick = g->starttick;
  g->lastDectick = g->starttick;

  /* PEM: This used to be done when saving, but that broke things. */
  if (g->num_pass >= 2) {
    back(g->GoGame);
    g->num_pass = 1;
    if (g->onMove == PLAYER_WHITE) g->onMove = PLAYER_BLACK;
    else g->onMove = PLAYER_WHITE;
  }
  /* Need to do notification and pending cleanup */
  return 0;
}

int game_delete(int wp, int bp)
{

  xyunlink(FILENAME_GAMES_bs_s, parray[wp].login, parray[bp].login);
  if(wp != bp) 
  xyunlink(FILENAME_GAMES_ws_s, parray[wp].login, parray[bp].login);
  return 0;
}

int game_save_complete(int g, FILE *fp, twodstring statstring)
{
  time_t now;
  int wp, bp, owp = 0, obp = 0;	/* Init. to shut up warnings. */
  char resu[10];
  char command[MAX_FILENAME_SIZE];
  char *tmp;

  if (!fp) return -1;
  wp = garray[g].white.pnum;
  bp = garray[g].black.pnum;
#ifdef PAIR
  if(paired(g)) {
    owp = garray[garray[g].pairwith].white.pnum;
    obp = garray[garray[g].pairwith].black.pnum;
  }
#endif
  now = globclock.time;
	/* This is ugly, depends on global filename ... */
  tmp = strrchr(filename(), '/');
  if(garray[g].gresult == 0.0) sprintf(resu, "Resign");
  else if(garray[g].gresult == -1.0) sprintf(resu, "Time");
  else sprintf(resu, "%.1f", garray[g].gresult);
  fprintf(fp, "\n(;\n");
  fprintf(fp, "GM[1]FF[4]AP[NNGS:%s]\n", version_string);
  fprintf(fp, "US[Brought to you by No Name Go Server]\n");
  fprintf(fp, "CP[\n\
  Copyright This game was played on the No Name Go Server\n\
  Permission to reproduce this game is given,\n\
  as long as this copyright notice is preserved.]\n");
  if((garray[g].Ladder9 == 1) || (garray[g].Ladder19 == 1)) {
    fprintf(fp, "GN[%s-%s(B) NNGS (LADDER RATED)]\n",
     parray[wp].pname, parray[bp].pname);
#ifdef PAIR
  } else if (paired(g)) {
    fprintf(fp, "GN[%s-%s vs %s-%s(B) NNGS (RENGO)]\n",
     parray[wp].pname, parray[owp].pname,
     parray[bp].pname, parray[obp].pname);
#endif
  } else if (garray[g].Tourn == 1) {
    fprintf(fp, "GN[%s-%s(B) NNGS (Tournament)]\n",
     parray[wp].pname, parray[bp].pname);
  } else {
    fprintf(fp, "GN[%s-%s(B) NNGS]\n",
     parray[wp].pname, parray[bp].pname);
  }
  fprintf(fp, "EV[%s]\n", IFNULL(garray[g].gevent, "none") );
  fprintf(fp, "RE[%s+%s]\n",
     (garray[g].winner == garray[g].white.pnum ? "W" : "B"), resu);
  fprintf(fp, "PW[%s]WR[%s%s]\n", 
               parray[wp].pname, 
               parray[wp].slotstat.is_registered ? parray[wp].srank : "UR",
               parray[wp].rated ? "*" : " ");
  fprintf(fp, "PB[%s]BR[%s%s]\n", 
               parray[bp].pname, 
               parray[bp].slotstat.is_registered ? parray[bp].srank : "UR",
               parray[bp].rated ? "*" : " ");
  fprintf(fp, "PC[%s: %s]\n", server_name, server_address);
  fprintf(fp, "DT[%s]\n", strDTtime(&now));
  fprintf(fp, "SZ[%d]TM[%d]KM[%.1f]\n\n", garray[g].GoGame->width,
     TICS2SECS(garray[g].ts.totalticks), garray[g].komi);
  if(Debug) Logit("garray[g].nmvinfos = %d", garray[g].nmvinfos);
  savegame(fp, garray[g].GoGame, garray[g].mvinfos, garray[g].nmvinfos);

  fprintf(fp, ";");
  if (statstring) {   /* record territory in SGF file */
    int x, y, n;      /*  - added on 11/19/98 by nic */

    n = 0;
    for (x = 0; x < garray[g].GoGame->height; x++)
      for (y = 0; y < garray[g].GoGame->width; y++)
        if (statstring[x][y] == '5') {
          if (!(n%18)) fprintf(fp, "\n");
          if (!n++) fprintf(fp, "TB");
          fprintf(fp, "[%c%c]", 'a' + x, 'a' + y);
        }

    n = 0;
    for (x = 0; x < garray[g].GoGame->height; x++)
      for (y = 0; y < garray[g].GoGame->width; y++)
        if (statstring[x][y] == '4') {
          if (!(n%18)) fprintf(fp, "\n");
          if (!n++) fprintf(fp, "TW");
          fprintf(fp, "[%c%c]", 'a' + x, 'a' + y);
        }
    fprintf(fp, "\n");
  }

  fprintf(fp, ")\n\n---\n");
  fclose(fp);
  if(!garray[g].Teach)
    if(parray[garray[g].white.pnum].automail) {
      sprintf(command, "%s -s \"%s\" %s < %s&", MAILPROGRAM,
          tmp + 1, parray[wp].email, filename() );
     system(command);
    }
  if(parray[garray[g].black.pnum].automail) {
    sprintf(command, "%s -s \"%s\" %s < %s&", MAILPROGRAM,
        tmp + 1, parray[bp].email, filename() );
    system(command);
  }

  return 1;
}

int game_save(int g)
{
  FILE *fp;
  int wp, bp;
  
  wp = garray[g].white.pnum;
  bp = garray[g].black.pnum;
  
  if(movenum(garray[g].GoGame) < 3) return 1;
  fp = xyfopen(FILENAME_GAMES_ws_s, "w",parray[wp].login,parray[bp].login);
  if (!fp) {
    return -1;
  }
  /* Create link for easier stored game finding */
  if (wp!=bp)
    xylink(FILENAME_GAMES_bs_s, parray[wp].login,parray[bp].login);

  fprintf(fp, "Rules: %d\n", garray[g].rules);
  fprintf(fp, "B_Penalty: %d\n", garray[g].black.penalty);
  fprintf(fp, "W_Penalty: %d\n", garray[g].white.penalty);
  fprintf(fp, "Size: %d\n", (int) garray[g].GoGame->width);
  fprintf(fp, "TimeStart: %d\n", (int) garray[g].timeOfStart);
  fprintf(fp, "W_Time: %d\n", garray[g].white.ticksleft);
  fprintf(fp, "B_Time: %d\n", garray[g].black.ticksleft);
  fprintf(fp, "Byo: %d\n", garray[g].ts.byoticks);
  fprintf(fp, "ByoS: %d\n", garray[g].ts.byostones);
  fprintf(fp, "W_Byo: %d\n", garray[g].white.byoperiods);
  fprintf(fp, "B_Byo: %d\n", garray[g].black.byoperiods);
  fprintf(fp, "W_ByoStones: %d\n", garray[g].white.byostones);
  fprintf(fp, "B_ByoStones: %d\n", garray[g].black.byostones);
  fprintf(fp, "ClockStopped: %d\n", garray[g].clockStopped);
  fprintf(fp, "Rated: %d\n", garray[g].rated);
  fprintf(fp, "Private: %d\n", garray[g].Private);
  fprintf(fp, "Type: %d\n", garray[g].type);
  fprintf(fp, "TimeType: %d\n", garray[g].ts.time_type);
  fprintf(fp, "Totalticks: %d\n", garray[g].ts.totalticks);
  fprintf(fp, "GoType: %d\n", garray[g].gotype);
  fprintf(fp, "NumPass: %d\n", garray[g].num_pass);
  fprintf(fp, "Komi: %.1f\n", garray[g].komi);
  fprintf(fp, "Teach: %d\n", garray[g].Teach);
  fprintf(fp, "Teach2: %d\n", garray[g].Teach2);
  fprintf(fp, "Ladder9: %d\n", garray[g].Ladder9);
  fprintf(fp, "Ladder19: %d\n", garray[g].Ladder19);
  fprintf(fp, "Tourn: %d\n", garray[g].Tourn);
  if(garray[g].gtitle)
    fprintf(fp, "Title: %s\n", garray[g].gtitle);
  if(garray[g].gevent)
    fprintf(fp, "Event: %s\n", garray[g].gevent);
 /* if(garray[g].GoGame->handicap > 0)
    fprintf(fp, "Handicap: %d\n", garray[g].GoGame->handicap); */
  fprintf(fp, "Ladder_Possible: %d\n", garray[g].Ladder_Possible);
  fprintf(fp, "OnMove: %d\n", garray[g].onMove);
  fprintf(fp, "MovesRNext: oinkoink\n");
  savegame(fp, garray[g].GoGame,NULL,0);
  savekib(fp, &garray[g]);
  fclose(fp);
  return 0;
}

int write_g_out(int g, FILE *fp, int maxlines, char *fdate)
{
  int wp, bp;
  char wrnk[6], brnk[6];
  char resu[10];

  if(!fp) return 0;
  wp = garray[g].white.pnum;
  bp = garray[g].black.pnum;

  if ((!parray[wp].slotstat.is_registered) || (!parray[bp].slotstat.is_registered)) {
    fclose(fp);
    return 0;
    }

  sprintf(wrnk, "%3.3s%s", parray[wp].srank, parray[wp].rated ? "*" : " ");
  sprintf(brnk, "%3.3s%s", parray[bp].srank, parray[bp].rated ? "*" : " ");

  if(garray[g].gresult == 0.0) sprintf(resu, "Resign");
  else if(garray[g].gresult == -1.0) sprintf(resu, "Time");
  else sprintf(resu, "%.1f", garray[g].gresult);
  fprintf(fp, "%-10s [%s](%s) : %-10s [%s](%s) H %d K %.1f %dx%d %s+%s %s\n",
    (garray[g].winner == garray[g].white.pnum ? parray[wp].pname : parray[bp].pname),
    (garray[g].winner == garray[g].white.pnum ? wrnk : brnk),
    (garray[g].winner == garray[g].white.pnum ? "W" : "B"),
    (garray[g].winner == garray[g].white.pnum ? parray[bp].pname : parray[wp].pname),
    (garray[g].winner == garray[g].white.pnum ? brnk : wrnk),
    (garray[g].winner == garray[g].white.pnum ? "B" : "W"),
     garray[g].GoGame->handicap,
     garray[g].komi,
     garray[g].GoGame->width, garray[g].GoGame->width,
    (garray[g].winner == garray[g].white.pnum ? "W" : "B"),
     resu, fdate);
  fclose(fp);
    /* AvK: sorry, this is ugly ... */
  truncate_file(filename(), maxlines);
  return 1;
}

int pgames(int p, FILE *fp)
{
  char line[1000];
  
  if (!fp) {
    return COM_OK;
  }
  if(parray[p].client) pcn_out(p, CODE_THIST, FORMAT_FILEn);
  while (fgets(line, sizeof line, fp)) {
    pprintf(p, "%s",line);
  }
  fclose(fp);
  if(parray[p].client) pcn_out(p, CODE_THIST, FORMAT_FILEn);
  return COM_OK;
}

void game_write_complete(int g, twodstring statstring)
{
  char fdate[40];
  int wp, bp;
  int now = globclock.time;
  char wname[sizeof parray[0].pname], bname[sizeof parray[0].pname];
  FILE *fp;

  wp = garray[g].white.pnum;
  bp = garray[g].black.pnum;
  strcpy(wname, parray[wp].pname);
  strcpy(bname, parray[bp].pname);

  stolower(wname);
  stolower(bname);
  sprintf(fdate, strtime_file((time_t *) &now));
  fp=xyfopen(FILENAME_PLAYER_cs_GAMES, "a", wname);
  write_g_out(g, fp, 23, fdate);
  if(wp != bp) {
    fp=xyfopen(FILENAME_PLAYER_cs_GAMES, "a", bname);
    write_g_out(g, fp, 23, fdate);
  }
  if ((garray[g].rated)
     && (parray[wp].slotstat.is_registered)
     && (parray[bp].slotstat.is_registered)
     && (garray[g].Teach != 1)
#ifdef PAIR
     && (!paired(g))
#endif
     && (movenum(garray[g].GoGame) >= 20)
     && (garray[g].GoGame->width == 19)) {
    fp=xyfopen(FILENAME_PLAYER_cs_GAMES, "a", wname);
    write_g_out(g, fp, 23, fdate);
    fp=xyfopen(FILENAME_PLAYER_cs_GAMES, "a", bname);
    write_g_out(g, fp, 23, fdate);
  }
  fp=xyfopen(FILENAME_RESULTS, "a" );
  write_g_out(g, fp, 250, fdate);

  fp=xyfopen(FILENAME_CGAMES_ws_s_s, "w", wname, bname, fdate);
  game_save_complete(g, fp, statstring);
  xylink(FILENAME_CGAMES_bs_s_s, wname, bname, fdate);

  if ((!parray[wp].slotstat.is_registered)
     || (!parray[bp].slotstat.is_registered)
     || (!garray[g].rated)
     || (garray[g].Teach == 1)
#ifdef PAIR
     || (paired(g))
#endif
     || (movenum(garray[g].GoGame) <= 20)
     || (garray[g].GoGame->width != 19))  return;

  fp = xyfopen(FILENAME_RESULTS, "a");
  if(!fp) {
    return;
  }
  fprintf(fp, "%s -- %s -- %d %.1f %s %s\n", 
               parray[garray[g].white.pnum].pname,
               parray[garray[g].black.pnum].pname,
               garray[g].GoGame->handicap,
               garray[g].komi,
               (garray[g].winner == garray[g].white.pnum ? "W" : "B"),
               ResultsDate(fdate));
  fclose(fp);
#ifdef NNGSRATED
  /* [PEM]: New results file for nrating. */
  {
    rdbm_t rdb;
    rdbm_player_t rp;
    char wrank[8], brank[8];
    struct tm *tp = localtime((time_t *)&now);

    if ((rdb = rdbm_open(NRATINGS_FILE,0)) == NULL)
    {
      strcpy(wrank, "-");
      strcpy(brank, "-");
    }
    else
    {
      if (rdbm_fetch(rdb, parray[garray[g].white.pnum].pname, &rp))
	strcpy(wrank, rp.rank);
      else
	strcpy(wrank, "-");
      if (rdbm_fetch(rdb, parray[garray[g].black.pnum].pname, &rp))
	strcpy(brank, rp.rank);
      else
	strcpy(brank, "-");
      rdbm_close(rdb);
    }

    fp = xyfopen(FILENAME_NRESULTS, "a");
    if (!fp) { return; }
    fprintf(fp, "\"%s\" %s \"%s\" %s %u %.1f %c %02u-%02u-%02u\n",
	    parray[garray[g].white.pnum].pname,
	    wrank,
	    parray[garray[g].black.pnum].pname,
	    brank,
	    garray[g].GoGame->handicap,
	    garray[g].komi,
	    (garray[g].winner == garray[g]. white.pnum ? 'W' : 'B'),
	    tp->tm_year + 1900, tp->tm_mon + 1, tp->tm_mday);
    fclose(fp);
  }
#endif /* NNGSRATED */
}

int game_count()
{
  int g, count = 0;

  for (g = 0; g < garray_top; g++) {
    if (garray[g].gstatus != GSTATUS_ACTIVE) continue;
    count++;
  }
  if (count > game_high) game_high = count;
  return count;
}
