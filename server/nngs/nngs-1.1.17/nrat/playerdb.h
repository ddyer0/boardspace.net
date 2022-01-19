/* playerdb.h
 *
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995  Erik Van Riper (geek@nngs.cosmic.org)
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


#ifndef PLAYERDB_H
#define PLAYERDB_H

#define PARRAY_SIZE 260
#define MAX_PENDING 5
#define MAX_OBSERVE 10
#define MAX_PLAN 10
#define MAX_FORMULA 9
#define MAX_CENSOR 10
#define MAX_NOTIFY 25
#define MAX_ALIASES 30
#define MAX_MESSAGES 80
#define MAX_INCHANNELS 50
#define MAX_CHANNEL_MEMBERS PARRAY_SIZE
#define PENDING_SIZE (MAX_PENDING*PARRAY_SIZE)

#define DEBUG_PLAYER_KICK 0
#define DEBUG_PLAYER_SLOT 0
#define DEBUG_GAME_SLOT 0

	/* These are for parray.pstatus */
#define PSTATUS_EMPTY    0 /* slot not in use */
#define PSTATUS_NEW      1 /* slot allocated, but not filled in yet */
#define PSTATUS_INQUEUE  2 /* unused */
#define PSTATUS_LOGIN    3 /* connection is logging in, parray[] almost valid */
#define PSTATUS_PASSWORD 4 /* idem */
#define PSTATUS_PROMPT   5 /* Normal mode, connected player */

	/* These are the two "opcodes" for write_loginout() */
#define P_LOGIN 0
#define P_LOGOUT 1

#define SORT_ALPHA 0
#define SORT_LADDER9 1
#define SORT_LADDER19 2

/*************************************************************************
                   The are the player record item sizes
*************************************************************************/
#define MAX_NAME 10
#define MIN_NAME 2
#define MAX_PASSWORD 14
#define MAX_FULLNAME 80
#define MAX_EMAIL 60
#define MAX_PROMPT 10
#define MAX_REGDATE 36
#define MAX_LASTFILE 80
#define MAX_SRANK 12
#define MAX_RANK 60
#define MAX_RANKED 12

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif

#include "pending.h"
/*
 * Note: the player array is used as a cache for the player data file.
 * An entry can refer to a currently unconnected player. This happens
 * when logging on, or when stats are reqested for someone who is absent.
 * In the original version of NNGS, offline players were put into
 * a temporal slot, which was read+used+discarded. These entries were
 * returned by the various player_xxx() function with negative indexes,
 * which needed a lot of special handling.
 * In this version, player data is kept in the array until the
 * space is reclaimed. Most of the writes to the player datafile
 * have been replaced by a call to player_dirty(), which just
 * sets the dirty-bit. The write is done later. Probably :-]
 *
 * Whenever a new slot is needed, it is basically allocated from the
 * top. The original server allocated the lowest *unconnected* slot.
 *
 * To mimic this behavior, some shuffling is done to move the unconnected
 * slots up. This is less costly then disk-i/o, anyway.
 *
 * Most of the 'slotstate' has been decomposed into separate (bit)fields.
 * This is easier to read, and easier to maintain, IMHO, then relying on
 * an aggregate (pstatus), which is there for other reasons.
 *
 * The fixcount-stuff is useless, currently. It could come into use
 * if we became multithreading. It does help to detect logical flaws.
 */
struct player_ {
  int socket;
  int pstatus;		/* The status of this slot eg PSTATUS_PROMPT */
  int protostate;	/* The state as reported in the protocol-lines */
  struct {
	unsigned  is_inuse:1;	/* Slot is in use */
	unsigned  is_valid:1;	/* data is valid (initialized or read) */
	unsigned  is_connected:1;	/* player is connected */
	unsigned  is_online:1;	/* Player is logged on and can receive */
	unsigned  is_registered:1;	/* registered Player, data should be saved */
	unsigned  is_dirty:1;	/* Data has changed */
	unsigned fixcount:2;	/* Reference count; for refleak testing */
	time_t timestamp;	/* For LRU allocation */
	} slotstat;
  char pname[MAX_NAME + 1];
  char login[MAX_NAME + 1];	/* login is the same as pname, but lowercase */
  char passwd[MAX_PASSWORD + 1];
  char fullname[MAX_FULLNAME + 1];
  char email[MAX_EMAIL + 1];
  char prompt[MAX_PROMPT + 1];
  char RegDate[MAX_REGDATE + 1];
  char last_file[MAX_LASTFILE + 1];
  char srank[MAX_SRANK + 1];
  char rank[MAX_RANK + 1];  
  char ranked[MAX_RANKED + 1];  
  int pass_tries;
  int water;
  int extprompt;
  int game;
  int gametype;
  int opponent; /* Only valid if game is >= 0 */
  int side;	/* Only valid if game is >= 0 */
  int last_tell;
  int last_pzz;
  int last_tell_from;
  int last_channel;
  int last_opponent;
  int language; /* Syncanph, AvK */
  int logon_time;
  int last_command_time;
  int outgoing;
  int incoming;
  int num_observe;
  int lastColor;
  int numgam;
  int d_height;
  int d_width;
  int last_file_line;
  int open;
  int looking;
  int rated;
  int rating;
  int orating;
  int ropen;
  int notifiedby;
  int bell;
  int client;
  int which_client;
  int i_login;
  int i_game;
  int i_shout;
  int i_gshout;
  int i_lshout;
  int last_problem;
  int i_tell;
  int i_robot;
  int i_kibitz;
  int i_verbose;
  int Private;
  int automail;
  int adminLevel;
  int teach;    /* A teaching account */
  int nochannels;
  int gonum_white;
  int gonum_black;
  int gowins;
  int golose;
  int bmuzzled;
  int muzzled;
  int gmuzzled;
  int tmuzzled;
  int kmuzzled;
  int def_time;
  int def_size;
  int def_byo_time;
  int def_byo_stones;
  int num_logons;
  int match_type;
  int silent_login;
  unsigned int thisHost; /* IP address in host byte order */
  unsigned int lastHost; /* IP address in host byte order */
  int observe_list[MAX_OBSERVE];
  char busy[100]; /* more than enough */
  struct alias * alias_list;
  struct censor * censor_list;
  struct plan * plan_lines;
} ;

extern struct player_ parray[PARRAY_SIZE];
extern int parray_top;

extern void player_init(void);
extern void player_array_init(void);

char * player_dumpslot(int p);

extern void player_forget(int);
extern void player_fix(int);
extern int player_fetch(const char *);
extern void player_dirty(int);

extern int player_new(void);
extern int player_delete(int);
extern int player_remove(int);

extern void player_clear(int);
extern void player_disconnect(int p);

extern int player_markdeleted(int);
extern int player_read(int);
extern void player_save(int);

extern int player_find_fd(int);
extern int player_find_login(const char *);
extern int player_find_part_login(const char *);
extern int player_find_sloppy(const char *);

extern int player_censored(int, int);
extern int check_censored(int, const char *);
extern int player_notified(int, int);
extern int player_notified_departure(int);
extern int player_notify_present (int);
extern int player_notify(int, char *, char *);

extern int player_count(void);
extern int player_idle(int);
extern int player_ontime(int);

extern void player_write_loginout(int, int);
extern time_t player_lastconnect(int);
extern int player_lastdisconnect(int);

extern int player_cmp(int, int, int);
extern void player_resort(void);

extern void player_pend_print(int, struct pending *);

struct pending * player_pending_new(int from, int to, int type);
void player_pending_delete(struct pending *ptr);

extern int player_add_request(int, int, int, int);
extern int player_remove_requests(int, int, int);

extern int player_decline_offers(int, int, int);
extern int player_withdraw_offers(int, int, int);

extern int player_is_observe(int, int);
extern int player_add_observe(int, int);
extern int player_remove_observe(int, int);
extern int player_game_ended(int);

extern int player_num_messages(int);
extern int player_add_message(int, int, char *);
extern int player_show_messages(int);
extern int player_clear_messages(int);

extern int sort_alpha[];
extern int sort_ladder9[];
extern int sort_ladder19[];

extern int player_kill(char *);
extern int player_rename(char *, char *);
extern int player_raise(char *);

#endif /* PLAYERDB_H */
