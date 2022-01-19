/* command.c
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
#include <assert.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_STRINGS_H
#include <strings.h>
#endif

#ifdef HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif

#ifdef HAVE_CTYPE_H
#include <ctype.h>
#endif


#include "nngsconfig.h"
#include "common.h"
#include "command.h"
#include "servercodes.h"

#define COMMAND_C 1
#include "command_list.h"

#include "nngsmain.h"
#include "utils.h"
#include "playerdb.h"
#include "gamedb.h"
#include "network.h"
#include "channel.h"
#include "alias.h"
#include "ip_ban.h"

#ifdef NNGSRATED
#include "rdbm.h"
#endif /* NNGSRATED */

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif

const char *ahelp_dir = AHELP_DIR;
const char *help_dir = HELP_DIR;
const char *mess_dir = MESSAGE_DIR;
const char *info_dir = INFO_DIR;
const char *stats_dir = STATS_DIR;
const char *player_dir = PLAYER_DIR;
const char *game_dir = GAME_DIR;
const char *cgame_dir = CGAME_DIR;
const char *problem_dir = PROBLEM_DIR;
const char *lists_dir = LIST_DIR;
const char *news_dir = NEWS_DIR;

const char *ratings_file = RATINGS_FILE;
const char *intergo_file = INTERGO_FILE;
const char *results_file = RESULTS_FILE;
const char *nresults_file = NRESULTS_FILE;
const char *emotes_file = EMOTES_FILE;
const char *note_file = NOTE_FILE;
const char *log_file = LOG_FILE;
const char *ladder9_file = LADDER9_FILE;
const char *ladder19_file = LADDER19_FILE;

const char *stats_logons = STATS_LOGONS;
const char *stats_messages = STATS_MESSAGES;
const char *stats_games = STATS_GAMES;
const char *stats_rgames = STATS_RGAMES;
const char *stats_cgames = STATS_CGAMES;

const char *def_prompt = DEFAULT_PROMPT;

const char *server_name = SERVER_NAME;
const char *server_address = SERVER_ADDRESS;
const char *server_email = SERVER_EMAIL;
const char *server_http = SERVER_HTTP;
const char *geek_email = GEEK_EMAIL;

const char *version_string = VERSION;

time_t startuptime;
int player_high;
int game_high;
int MailGameResult;
#ifndef MODE_FOR_DIR
#define MODE_FOR_DIR 0
#endif /* MODE_FOR_DIR */
mode_t mode_for_dir = MODE_FOR_DIR;
char orig_command[MAX_STRING_LENGTH];

int commanding_player = -1;	/* The player whose command your in */

static int lastCommandFound = -1;

static int parse_command(char *com_string, char **comm, char **parameters);
static void alias_substitute(int p, char *com_str, char **alias_str);
static int command_cmp(const void * l, const void *r);
static int get_parameters(int command, char *string, struct parameter * params);
static void printusage(int p, const char *command_str);
static void boot_out (int p,int p1);

static void process_login(int p, char *login);
static int process_password(int p, char *password);
static int process_prompt(int p, char *command);

/* Copies command into comm, and returns pointer to parameters in
* parameters
 */
static int parse_command(char *com_string, char **comm, char **parameters)
{
  *comm = com_string;
  *parameters = eatword(com_string);
  if (**parameters) {
    **parameters = '\0';
    (*parameters)++;
    *parameters = eatwhite(*parameters);
  }
  if (strlen(*comm) >= MAX_COM_LENGTH) {
    return COM_BADCOMMAND;
  }
  return COM_OK;
}

/* Puts alias substitution into alias_string */
static void alias_substitute(int p, char *com_str, char **alias_str)
{
  char *save_str = com_str;
  char tmp[MAX_COM_LENGTH];
  static char outalias[MAX_STRING_LENGTH];
  int i = 0, done = 0;
  static struct alias * i_alias_list = NULL; /* The internal, sorted alias list. */
  char *alias;

  if (!i_alias_list)		/* The first time, update from global list. */
  {
    struct alias_type *ap;

    i_alias_list = alias_init();
    assert(i_alias_list != NULL);
    for (ap = g_alias_list ; ap->comm_name ; ap++)
      alias_add(ap->comm_name, ap->alias, i_alias_list);
  }

  while (com_str && !iswhitespace(*com_str) && !done) {
    if (i >= MAX_COM_LENGTH) {	/* Too long for an alias */
      *alias_str = save_str;
      return;
    }
    tmp[i] = *com_str;
    com_str++;
    if (ispunct((int)tmp[i]))
      done = 1;
    i++;
  }
  tmp[i] = '\0';

  if ((alias = alias_lookup(tmp, parray[p].alias_list))) {
    if (com_str)
      sprintf(outalias, "%s%s", alias, com_str);
    else
      sprintf(outalias, "%s", alias);
    *alias_str = outalias;
  } else if ((alias = alias_lookup(tmp, i_alias_list))) {
    if (com_str)
      sprintf(outalias, "%s%s", alias, com_str);
    else
      sprintf(outalias, "%s", alias);
    *alias_str = outalias;
  } else
    *alias_str = save_str;
}


static int command_count = 0;

/* Initializes. Called once at start. */
void command_init(void)
{
  if (!command_count)
  {
    command_count = COUNTOF(command_list);
    qsort(command_list, command_count, sizeof command_list[0], &command_cmp);
  }
}

/* AvK callback for qsort */
static int command_cmp(const void * l, const void *r)
{
  const struct command_type *pl = (const struct command_type *) l;
  const struct command_type *pr = (const struct command_type *) r;

  return strcmp(pl->comm_name,pr->comm_name);
}

/* Returns the admin level of cmd, or -1 of cmd is out of bounds. */
int command_admin_level(int cmd)
{
  if (cmd < 0 || cmd >= command_count)
    return -1;
  else
    return command_list[cmd].adminLevel;
}


/* Returns pointer to command that matches */
int match_command(const char *comm)
{
  int left, right;
  int len = strlen(comm);

  /* Binary search. */
  left = 0;                     /* Left most element. */
  right = command_count;            /* Beyond right most element. */
  while (left < right)
  {
    int x, i = (left+right)/2; /* Check the middle element. */

    x = strncmp(comm, command_list[i].comm_name, len);
    if (x < 0)
      right = i;              /* It's on the left side. */
    else if (x > 0)
      left = i+1;             /* It's on the right side. */
    else
    {                       /* Found it. */
      /* Check if it's ambiguous. */
      if (i > 0 && !strncmp(comm, command_list[i-1].comm_name, len))
	return -COM_AMBIGUOUS;
      if (i+1 < command_count && !strncmp(comm, command_list[i+1].comm_name, len))
	return -COM_AMBIGUOUS;
      lastCommandFound = i;
      return i;
    }
  }
  return -COM_BADCOMMAND;
}

/* Gets the parameters for this command */
static int get_parameters(int command, char *string, struct parameter * params)
{
  int i, parlen;
  int paramLower;
  int ch;
  static char punc[2];

  punc[1] = '\0';		/* Holds punc parameters */
  for (i = 0; i < MAXNUMPARAMS; i++)
    params[i].type = TYPE_NULL;	/* Set all parameters to NULL */

  parlen = strlen(command_list[command].param_string);

  for (i = 0; i < parlen; i++) {
    ch = command_list[command].param_string[i];

    if (isupper(ch)) {
      paramLower = 0;
      ch = tolower(ch);
    } else {
      paramLower = 1;
    }

    switch (ch) {
      case 'w':			/* word */
	string = eatwhite(string);
	if (!*string)
	  return COM_BADPARAMETERS;
      case 'o':			/* optional word */
	string = eatwhite(string);
	if (!*string)
	  return COM_OK;
	params[i].val.word = string;
	params[i].type = TYPE_WORD;
	if (ispunct(*string)) {
	  punc[0] = *string;
	  params[i].val.word = punc;
	  string++;
	} else {
	  string = eatword(string);
	  if (*string) {
	    *string = '\0';
	    string++;
	  }
	}
	if (paramLower)
	  stolower(params[i].val.word);
	break;
      case 'd':			/* integer */
	string = eatwhite(string);
	if (!*string)
	  return COM_BADPARAMETERS;
      case 'p':			/* optional integer */
	string = eatwhite(string);
	if (!*string)
	  return COM_OK;
	if (sscanf(string, "%d", &params[i].val.integer) != 1)
	  return COM_BADPARAMETERS;
	params[i].type = TYPE_INT;
	string = eatword(string);
	if (*string) {
	  *string = '\0';
	  string++;
	}
	break;
      case 'f':
	string = eatwhite(string);
	if (!*string)
	  return COM_OK;
	if (sscanf(string, "%f", &params[i].val.f) != 1)
	  return COM_BADPARAMETERS;
	params[i].type = TYPE_FLOAT;
	string = eatword(string);
	if (*string) {
	  *string = '\0';
	  string++;
	}
	break;
      case 'i':			/* word or integer */
	string = eatwhite(string);
	if (!*string)
	  return COM_BADPARAMETERS;
      case 'n':			/* optional word or integer */
	string = eatwhite(string);
	if (!*string)
	  return COM_OK;
	if (sscanf(string, "%d", &params[i].val.integer) != 1) {
	  params[i].val.word = string;
	  params[i].type = TYPE_WORD;
	} else {
	  params[i].type = TYPE_INT;
	}
	if (ispunct(*string)) {
	  punc[0] = *string;
	  params[i].val.word = punc;
	  params[i].type = TYPE_WORD;
	  string++;
	} else {
	  string = eatword(string);
	  if (*string) {
	    *string = 0;
	    string++;
	  }
	}
	if (params[i].type == TYPE_WORD)
	  if (paramLower)
	    stolower(params[i].val.word);
	break;
      case 's':			/* string to end */
	if (!*string)
	  return COM_BADPARAMETERS;
      case 't':			/* optional string to end */
	if (!*string)
	  return COM_OK;
	params[i].val.string = string;
	params[i].type = TYPE_STRING;
	while (*string)
	  string = nextword(string);
	if (paramLower)
	  stolower(params[i].val.string);
	break;
    }
  }

  if (*string)
    return COM_BADPARAMETERS;
  else
    return COM_OK;
}

static void printusage(int p, const char *command_str)
{
  int i, parlen;
  int command;
  int ch;

  if ((command = match_command(command_str)) < 0) {
    pcn_out(p, CODE_ERROR, FORMAT_UNKNOWN_COMMANDn);
    return;
  }
  parlen = strlen(command_list[command].param_string);
  for (i = 0; i < parlen; i++) {
    ch = command_list[command].param_string[i];
    if (isupper(ch))
      ch = tolower(ch);
    switch (ch) {
      case 'w':			/* word */
	pprintf(p, " word");
	break;
      case 'o':			/* optional word */
	pprintf(p, " [word]");
	break;
      case 'd':			/* integer */
	pprintf(p, " integer");
	break;
      case 'p':			/* optional integer */
	pprintf(p, " [integer]");
	break;
      case 'i':			/* word or integer */
	pprintf(p, " {word, integer}");
	break;
      case 'n':			/* optional word or integer */
	pprintf(p, " [{word, integer}]");
	break;
      case 's':			/* string to end */
	pprintf(p, " string");
	break;
      case 't':			/* optional string to end */
	pprintf(p, " [string]");
	break;
    }
  }
  pprintf(p, "\n");
}


int process_command(int p, char *com_string)
{
  int which_command, retval;
  struct parameter params[MAXNUMPARAMS];
  char *alias_string;
  char *comm, *rest;

  if (!com_string)
  {
    Logit("ERROR: process_command called with NULL string!: %s",
      player_dumpslot(p));
    return COM_BADCOMMAND;
  }

  if (Debug > 8) {
    Logit("DEBUG: %s > %s <", player_dumpslot(p), com_string);
  }

  KillTrailWhiteSpace(com_string);
  alias_substitute(p, com_string, &alias_string);

  if (Debug > 9) {
    if (com_string != alias_string)
      Logit("DEBUG: %s -alias-: > %s <", parray[p].pname, alias_string);
  }

  if ((retval = parse_command(alias_string, &comm, &rest)))
    return retval;

  stolower(comm);               /* All commands are case-insensitive */

  if(parray[p].game >= 0) {
    if(go_move(garray[parray[p].game].GoGame, comm)) {
      return COM_ISMOVE;
    }
  }

  /* find the command and return a pointer to it */
  if ((which_command = match_command(comm)) < 0)
    return -which_command;

  /* check if the user has the privs to execute it */
  if (parray[p].adminLevel < command_admin_level(which_command))
    return COM_RIGHTS;

  if ((retval = get_parameters(which_command, rest, params)))
    return retval;

  return command_list[which_command].comm_func(p, params);
}


static void process_login(int p, char *login)
{
  int failed = 1;
  int i;
  size_t len;

  login = eatwhite(login);
  len=strlen(login);

  if (len) {
    if ((!alphastring(login)) || (!printablestring(login))) {
      pprintf(p, "\nSorry, names can only consist of lower "
                 "and upper case letters.  Try again.\n");
      Logit("LOGIN: Bad userid: %s", login);
    } else if (len < MIN_NAME) {
      pprintf(p, "\nA name should be at least %d characters long! "
                 "Try again.\n", MIN_NAME);
      Logit("LOGIN: Bad userid: %s", login);
    } else if (len >= sizeof parray[0].pname) {
      pprintf(p, "\nSorry, names may be at most %d characters long. "
                 "Try again.\n", (int) sizeof parray[0].pname -1);
      Logit("LOGIN: Bad userid: %s", login);
    } else {
      stolower(login);
      do_copy(parray[p].login,login,sizeof parray[p].login);
      if (player_read(p)) {
#ifdef NOGUESTS
        Logit("LOGIN: Unknown userid: %s", login);
        pprintf(p, "\nUser unknown.  Send mail to %s", SERVER_EMAIL);
        pprintf(p, "\nfor registration information\n");
#else
        failed = 0;
        parray[p].pstatus = PSTATUS_PASSWORD;
	do_copy(parray[p].pname, login, sizeof parray[0].pname); 
	pprintf(p, "\n\"%s\" is not a registered name.  You may use this name to play unrated games.\n(After logging in, do \"help register\" for more info on how to register.)\n\nThis is a guest account.\nYour account name is %s.\n", 
	    parray[p].pname,
	    parray[p].pname);
	channel_add(CHANNEL_SHOUT, p);
	channel_add(CHANNEL_GSHOUT, p);
	channel_add(CHANNEL_LSHOUT, p);
	channel_add(CHANNEL_LOGON, p);
	channel_add(CHANNEL_GAME, p);
#endif
      } else {
        failed = 0;
	pprintf(p, "\n%s",parray[p].client ? "1 1\n" : "Password: ");
        net_echoOff(parray[p].socket);
        parray[p].pstatus = PSTATUS_PASSWORD;
      }
      /* player_resort(); */
    }
  }

  if (failed) {
    for (i = 0; i < MAX_CHANNELS; i++) channel_remove(i, p);
    pprintf(p, "Login: ");
  }
  else if(!parray[p].slotstat.is_registered) pcommand(p, "\n");
  return;
}

static void boot_out (int p,int p1)
{
  int fd;

  pprintf (p, "\n **** %s is already logged in - kicking other copy out. ****\n", parray[p].pname);
  pprintf (p1, "**** %s has arrived - you can't both be logged in. ****\n", parray[p].pname);
  fd = parray[p1].socket;
  process_disconnection(fd);
  /* This may cause some dirty data for p1 to be lost !!!
   * Damage is limited, however, since parray is flushed on logons.
   */
  player_clear(p1);
}

static int process_password(int p, char *password)
{
  int p1;
  char salt[3];
  int fd;
  unsigned int fromHost;
  int messnum, i, j;
  char tmptext[256];
  char ctmptext[256];
  int len, clen;

  net_echoOn(parray[p].socket);

  if (parray[p].slotstat.is_registered && parray[p].passwd[0]) {
    if(strlen(password) < 2) {
      parray[p].pstatus = PSTATUS_PASSWORD;
      pprintf(p, "\n%s", parray[p].client ? "1 1\n" : "Password: ");
      net_echoOff(parray[p].socket);
      return COM_OKN;
    }

    memcpy(salt, parray[p].passwd, 2);
    salt[2]=0;
    if (strcmp(mycrypt(password, salt), parray[p].passwd)) {
      Logit("%s tried to log in from %s with a bad password!", 
	  parray[p].pname,
	  dotQuad(parray[p].thisHost));
      fd = parray[p].socket;
      fromHost = parray[p].thisHost;
      for (i=0; i < MAX_OCHANNELS; i++) {
	if (on_channel(i, p)) {
	  channel_remove(i, p);
	}
      }
      parray[p].logon_time = globclock.time;
      parray[p].pstatus = PSTATUS_LOGIN;
      /* player_resort(); */
      parray[p].socket = fd;
      parray[p].thisHost = fromHost;
      if (*password) {
	pprintf(p, "\n\nInvalid password!\n\n");
      }
      pprintf(p, "Login: ");
      if(parray[p].pass_tries++ > 3) {
        player_clear(p);
	return COM_LOGOUT;
      }
      return COM_OKN;
    }
  }

  /* This should really be a hash! */
  for(p1 = 0; p1 < parray_top; p1++) {
    if (p == p1) continue;
    if (!parray[p1].slotstat.is_inuse) continue;
    if (strcmp (parray[p].login, parray[p1].login)) continue;
#if DEBUG_PLAYER_KICK
    Logit("Slot %s #%d NEW ", player_dumpslot(p), p);
    Logit("Slot %s #%d OLD ", player_dumpslot(p1), p1);
#endif
    if (!parray[p1].slotstat.is_connected) {
      player_clear(p1); /* this may lose dirty data(can it be dirty?) */
      continue;
      }
    if (!parray[p].slotstat.is_registered) {
      pprintf (p, "\n*** Sorry %s is already logged in ***\n", parray[p1].pname);
      return COM_LOGOUT;
    }
#if DEBUG_PLAYER_KICK
    Logit("Ass %d<<==Foot %d", p1, p);
#endif  
    boot_out(p,p1);
  }

  /* Check if the user is really an administrator */
  if (parray[p].adminLevel >= ADMIN_ADMIN
    && !in_list("admin", parray[p].pname)) {
    Logit("Admin %d reduced to %d for user %s"
      , parray[p].adminLevel, ADMIN_USER, parray[p].pname);
    parray[p].adminLevel = ADMIN_USER;
  }

  parray[p].pstatus = PSTATUS_PROMPT;
  pprintf(p, "%s\n", parray[p].client ? "1 5" : "");

  pprintf(p, "%s\n", parray[p].client ? "9 File" : "");
  if (parray[p].adminLevel >= ADMIN_ADMIN) {
    pxysend_raw_file(p, FILENAME_MESS_AMOTD);
  } else {
    pxysend_raw_file(p, FILENAME_MESS_MOTD);
  }

  pprintf(p, "\n%s\n", parray[p].client ? "9 File" : "");

  if (parray[p].slotstat.is_registered && !parray[p].passwd[0])
    pcn_out(p, CODE_ERROR, FORMAT_YOU_HAVE_NO_PASSWORD_PLEASE_SET_ONE_WITH_THE_PASSWORD_COMMAND_n);

  if (!parray[p].slotstat.is_registered)
    pxysend_raw_file(p, FILENAME_MESS_UNREGISTERED);
  if(Debug) Logit("About to resort");
  player_resort();
  if(Debug) Logit("About to write login");
  player_write_loginout(p, P_LOGIN);
  parray[p].slotstat.is_online = 1;
  if(Debug) Logit("Made it to announce");
  sprintf(tmptext, "{%s [%3.3s%s] has connected.}\n",
      parray[p].pname,
      parray[p].srank,
      parray[p].rated ? "*" : " ");
  sprintf(ctmptext, "%d %s", CODE_SHOUT, tmptext);

  len = strlen(tmptext);
  clen = strlen(ctmptext);

   for (i = 0; i < carray[CHANNEL_LOGON].count; i++) {
     p1 = carray[CHANNEL_LOGON].members[i];
     if (p1 == p) continue;
     if (!parray[p1].slotstat.is_online) continue ;
     if (!parray[p1].i_login) continue;
     if (parray[p].silent_login) continue;
     if (parray[p1].adminLevel >= ADMIN_ADMIN) {
       pcn_out_prompt(p1, CODE_SHOUT, FORMAT_s_ss_ss_s_HAS_CONNECTED_n,
         parray[p].pname,
         parray[p].srank,
         parray[p].rated ? "*" : " ",
         parray[p].slotstat.is_registered ? "R" : "U",
         (parray[p].adminLevel>=ADMIN_ADMIN) ? "*" : "",
         dotQuad(parray[p].thisHost));
     } else {
      pcn_out_prompt(p1, CODE_SHOUT, FORMAT_s_ss_HAS_CONNECTED_n,
      parray[p].pname,
      parray[p].srank,
      parray[p].rated ? "*" : " ");
    }

  }
  for (i = 0; i < MAX_NCHANNELS; i++) {
    if(carray[i].other) continue;
    if (on_channel(i, p)) {
      pcn_out(p, CODE_INFO, FORMAT_CHANNEL_d_TOPIC_sn, i, carray[i].ctitle);
      for (j = 0; j < carray[i].count; j++) {
        if(carray[i].other) continue;
        p1 = carray[i].members[j];
	if(p1 == p) continue;
	if (!parray[p1].slotstat.is_online) continue;
	pcn_out(p1, CODE_INFO, FORMAT_n );
	pcn_out_prompt(p1, CODE_INFO, FORMAT_s_HAS_JOINED_CHANNEL_d_n,
	    parray[p].pname, i);
      }
      for (j = 0; j < carray[i].count; j++) {
	pprintf(p, " %s", parray[carray[i].members[j]].pname);
      }
      pprintf(p, "\n");
    }
  }
  if (parray[p].adminLevel >= ADMIN_ADMIN) {
    pcn_out(p, CODE_INFO, FORMAT_WELCOME_TO_THE_ADMIN_CHANNEL_TOPIC_IS_sn,
	carray[CHANNEL_ASHOUT].ctitle);
    for(j = 0; j < carray[CHANNEL_ASHOUT].count; j++) {
      p1 = carray[CHANNEL_ASHOUT].members[j];
      if(!parray[p1].slotstat.is_online) continue;
      if(p1 == p) continue;
      pcn_out_prompt(p1, CODE_CR1|CODE_INFO,
          FORMAT_s_HAS_JOINED_THE_ADMIN_CHANNEL_n,
	  parray[p].pname);
    }
  }

  num_logins++;
  if(Debug) Logit("Doing messages...");
  messnum = player_num_messages(p);
  if(Debug) Logit("Done with messages...");
  if (messnum) {
    pcn_out(p, CODE_INFO, FORMAT_YOU_HAVE_d_MESSAGES_TYPE_qMESSAGESq_TO_DISPLAY_THEMn, 
	messnum);
  }
#ifdef WANT_NOTIFY
  player_notify_present(p);
  player_notify(p, "arrived", "arrival"); 
#endif /* WANT_NOTIFY */
#ifdef CHECK_LAST_HOST /*  No real need to check this anymore */
  if (parray[p].slotstat.is_registered && (parray[p].lastHost != 0) &&
      (parray[p].lastHost != parray[p].thisHost)) {
    Logit("Player %s: Last login: %s ", parray[p].pname,
	dotQuad(parray[p].lastHost));
  }
#endif
  parray[p].lastHost = parray[p].thisHost;
  parray[p].protostate = STAT_WAITING;
  pcn_out_prompt(p, CODE_MVERSION, FORMAT_NO_NAME_GO_SERVER_NNGS_VERSION_sn, version_string);
  if(!parray[p].slotstat.is_registered) parray[p].water = 0;
  return 0;
}


static int process_prompt(int p, char *command)
{
  int retval;

  command = eatwhite(command);
  if (!*command) {
    if(!parray[p].client) pprintf(p, "%s", parray[p].prompt);
    else pprintf_prompt(p, "\n");
    return COM_OK;
  }

  retval = process_command(p, command);

  switch (retval) {
    case COM_OKN:
      retval = COM_OK;
      pprintf_prompt(p, "");
      break;
    case COM_OK:
      retval = COM_OK;
      pprintf_prompt(p, "\n");
      break;
    case COM_NOSUCHGAME:
      pcn_out_prompt(p, CODE_ERROR, FORMAT_THERE_IS_NO_SUCH_GAME_n );
      retval = COM_OK;
      break;
    case COM_OK_NOPROMPT:
      retval = COM_OK;
      break;
    case COM_ISMOVE:
      retval = COM_OK;
#ifdef PAIR
      process_move(p, command, 1);
#else
      process_move(p, command);
#endif
      break;
    case COM_RIGHTS:
      /* [PEM]: This seems more natural: If you don't have the right to do it,
	 the command doesn't exists. */
      pcn_out_prompt(p, CODE_ERROR, FORMAT_s_INACCESSIBLE_COMMAND_n, command);
      retval = COM_OK;
      break;
    case COM_AMBIGUOUS:
      {
	int len = strlen(command);
	int i ;
	pcn_out( p, CODE_ERROR, FORMAT_AMBIGUOUS_COMMAND_MATCHES_);
	for (i=0;command_list[i].comm_name; i++) {
	  if (strncmp(command_list[i].comm_name, command, len)) continue;
	  pprintf( p, " %s", command_list[i].comm_name );
	}
      }
      pcn_out_prompt(p, CODE_CR1, FORMAT_empty);
      retval = COM_OK;
      break;
    case COM_BADPARAMETERS:
      pcn_out(p, CODE_ERROR, FORMAT_USAGE_n);
      pcn_out(p, CODE_ERROR, FORMAT_s,
	  command_list[lastCommandFound].comm_name);
      printusage(p, command_list[lastCommandFound].comm_name);
      pcn_out_prompt(p, CODE_ERROR, FORMAT_SEE_HELP_s_FOR_A_COMPLETE_DESCRIPTION_n,
	  command_list[lastCommandFound].comm_name);
      retval = COM_OK;
      break;
    case COM_FAILED:
      pcn_out_prompt(p, CODE_ERROR, FORMAT_s_COMMAND_FAILED_n, command);
      retval = COM_OK;
      break;
    case COM_BADCOMMAND:
      pcn_out_prompt(p, CODE_ERROR, FORMAT_s_UNKNOWN_COMMAND_n, command);
      retval = COM_OK;
      break;
    case COM_LOGOUT:
      retval = COM_LOGOUT;
      break;
  }
  return retval;
}

int process_input(int fd, char *com_string)
{
  int p;
  int retval = COM_OK;
  char statstr[40];
  const char *msg=NULL;

  p = player_find_fd(fd);
  if (p < 0) {
    Logit("Input from a player not in array(fd=%d,string=%s)!", fd, com_string);
    return COM_BADFD;
  }

  commanding_player = p;
  strcpy(orig_command, com_string);
  parray[p].last_command_time = globclock.time;
  switch (parray[p].pstatus) {
    default:
      sprintf(statstr,".status=%d", parray[p].pstatus);
      msg=statstr;
      break;
    case PSTATUS_EMPTY:
      msg= "empty";
      break;
    case PSTATUS_NEW:
      msg= "new";
      break;
    case PSTATUS_INQUEUE:
      /* Ignore input from player in queue */
      msg= "inqueue";
      break;
    case PSTATUS_LOGIN:
      process_login(p, com_string);
      break;
    case PSTATUS_PASSWORD:
      retval = process_password(p, com_string);
      break;
    case PSTATUS_PROMPT:
      retval = process_prompt(p, com_string);
      break;
  }
  if(msg) {
    Logit("Command from Fd=%d %s player! : Slot %s", fd, msg, player_dumpslot(p));
  }
  commanding_player = -1;
  return retval;
}

void process_new_connection(int fd, unsigned int fromHost)
{
  int p;

  p = range_check(fromHost,fromHost);
  if (p) {
    Logit("Revoked connection from %s :%d.", 
    dotQuad(parray[p].thisHost), p);
    net_close(fd);
    return;
  }
  p = player_new();

  parray[p].pstatus = PSTATUS_LOGIN;  /* Set status to LOGIN */
  parray[p].socket = fd;            /* track the FD */
  parray[p].slotstat.is_connected = 1;
  parray[p].slotstat.is_online = 0;
  parray[p].logon_time = globclock.time;   /* Set the login time */
  parray[p].thisHost = fromHost;    /* set the host FROM field */
  parray[p].pass_tries = 0;         /* Clear number of password attempts */
  pxysend_raw_file(p, FILENAME_MESS_LOGIN); /* Send the login file */
  pprintf(p, "Login: ");            /* Give them a prompt */
  return;
}

void process_disconnection(int fd)
{
  int p;
  int p1, i, j;

  p = player_find_fd(fd);
  if (p < 0) {
    Logit("Disconnect from fd=%d player not in array!", fd);
    net_close(fd);
    return;
  }

  if (!net_isalive(fd)) parray[p].slotstat.is_connected = 0;

  player_remove_requests(p,-1,-1);
  player_remove_requests(-1,p,-1);
  commanding_player = p;
  if (parray[p].game>=0)
    game_disconnect(parray[p].game, p);

  if (parray[p].slotstat.is_online) {
    for (i = 0; i < carray[CHANNEL_LOGON].count; i++) {
      if(parray[p].silent_login) break;
      p1 = carray[CHANNEL_LOGON].members[i];
      if (!parray[p1].slotstat.is_online) continue; 
      if (p1 == p) continue; 
      if (!parray[p1].i_login) continue; 
      pcn_out_prompt(p1, CODE_SHOUT, FORMAT_s_HAS_DISCONNECTED_n, 
	  parray[p].pname);
    }
#ifdef WANT_NOTIFY
    player_notify(p, "departed", "departure");
    player_notify_departure(p); 
#endif /* WANT_NOTIFY */
    player_write_loginout(p, P_LOGOUT);
    player_dirty(p);
    num_logouts++;
  }

  for (i = 0; i < MAX_NCHANNELS; i++) {
    if (!on_channel(i, p)) continue;
    for (j = 0; j < carray[i].count; j++) {
      p1 = carray[i].members[j];
      if (!parray[p1].slotstat.is_online) continue;
      if (p1 == p) continue;
      pcn_out_prompt(p1, CODE_CR1|CODE_INFO, FORMAT_s_HAS_LEFT_CHANNEL_d_n,
        parray[p].pname, i);
    }
  }
  player_disconnect(p);
  player_forget(p);
  net_close(fd);
  return;
}

int process_incomplete(int fd, char *com_string)
{
  int p;
  char last_char;
  int len;
  char *quit = "quit";

  p = player_find_fd(fd);
  if (p < 0) {
    Logit("Incomplete command from player not in array! (Fd=%d,str=%s) ", fd,
          com_string);
    return COM_BADFD;
  }

  commanding_player = p;
  len = strlen(com_string);
  if (len)
    last_char = com_string[len - 1];
  else
    last_char = '\0';
  if (len == 1 && last_char == '\4') {	/* ctrl-d */
    if (parray[p].pstatus == PSTATUS_PROMPT)
      process_input(fd, quit);
    return COM_LOGOUT;
  }
  if (last_char == '\3') {	/* ctrl-c */
    if (parray[p].pstatus == PSTATUS_PROMPT) {
      if(!parray[p].client) pprintf(p, "\n%s", parray[p].prompt);
      else pprintf(p, "\n1 %d\n",parray[p].protostate);
      return COM_FLUSHINPUT;
    } else {
      return COM_LOGOUT;
    }
  }
  return COM_OK;
}

  /* Called every few seconds */
int process_heartbeat(int *fdp)
{
  static int last_ratings = 0;
  static int lastcalled = 0;
  static int last_results = 0;
  int time_since_last;
  int p;
  static int last_idle_check;
  static int resu = 0;
  int now = globclock.time;
  static struct stat ratingsbuf1, ratingsbuf2;
#ifdef NNGSRATED
  rdbm_t rdb;
#endif /* NNGSRATED */

  game_update_times();

  time_since_last = (lastcalled) ? now - lastcalled : 1;
  lastcalled = now;
  if (time_since_last == 0)
    return COM_OK;

    /* Check for timed out connections */
  if(last_idle_check > 60) {  /* Now only check every minute for idle */
    for (p = 0; p < parray_top; p++) {
      if (!parray[p].slotstat.is_inuse) continue;
      if (!parray[p].slotstat.is_connected) continue;
      if (!parray[p].slotstat.is_online 
          && (player_idle(p) > MAX_LOGIN_IDLE))    { 
        *fdp = parray[p].socket;
        return COM_LOGOUT;
      }
      if (parray[p].adminLevel >= ADMIN_ADMIN) continue;
      if (parray[p].i_robot) continue;	/* [PEM]: Don't timeout robots. */
      if ((player_idle(p) > MAX_IDLE)  
        && (parray[p].protostate == STAT_WAITING)
        && (parray[p].pstatus == PSTATUS_PROMPT) ) {
        pcommand(p, "quit");
      }
    }
    last_idle_check = 0;
  } else {
    last_idle_check++;
  }
  if (last_ratings == 0) {
    last_ratings = (now - (5 * 60 * 60)) + 120;	/* Do one in 2 minutes */
  }
  else {
    if (last_ratings + 6 * 60 * 60 < now) { /* Every 6 hours */
      last_ratings = now;
#ifdef LADDERSIFT
      Logit("Sifting 19x19 ladder");
      PlayerSift(Ladder19, 14);  /* Do the ladder stuff, 19x19 is 14 days */
      Logit("Sifting 9x9 ladder");
      PlayerSift(Ladder9, 7); /* Do the ladder stuff, 9x9 is 7 days */
#endif
    }
  }
#ifdef NNGSRATED
  if (!last_results) {
    last_results = now;
    stat(NRATINGS_FILE, &ratingsbuf1);  /* init the stat buf */
    stat(NRATINGS_FILE, &ratingsbuf2);
  } else {
    if (last_results + (60 * 2) < now) { /* every 2 minutes */
      last_results = now;
      stat(NRATINGS_FILE, &ratingsbuf2);
      resu = ratingsbuf2.st_mtime - ratingsbuf1.st_mtime;
      if(resu > 0) {
        stat(NRATINGS_FILE, &ratingsbuf1);  /* re-init the stat buf */
        stat(NRATINGS_FILE, &ratingsbuf2);  /* re-init the stat buf */
        Logit("Time to re-read the ratings file.  :)");
        if ((rdb = rdbm_open(NRATINGS_FILE, 0)) != NULL) {
          for (p = 0; p < parray_top; p++) {
            rdbm_player_t rp;
	    int orat;

            if (!parray[p].slotstat.is_online)     continue;
            if (!strcmp(parray[p].ranked, "NR")) continue;
            if (!parray[p].slotstat.is_registered) {
	      pcn_out_prompt(p, CODE_INFO, FORMAT_PLEASE_SEE_qHELP_REGISTERqn);
	      continue;
            }
            if (!rdbm_fetch(rdb, parray[p].pname, &rp)) continue;

	    do_copy(parray[p].srank, rp.rank, sizeof parray[0].srank);
	    if (rp.star)
	      parray[p].rated = 1;
	    orat = parray[p].rating;
	    parray[p].rating = parray[p].orating = (int)(rp.rating * 100);
	    parray[p].numgam = rp.wins + rp.losses;
	    if (orat == parray[p].rating) {
	      pcn_out_prompt(p, CODE_INFO, FORMAT_RATINGS_UPDATE_YOU_ARE_STILL_ss_d_d_RATED_GAMES_n,
	      parray[p].srank,
	      parray[p].rated ? "*" : "",
	      parray[p].rating,
	      parray[p].numgam);
	    } else if (orat == 0 && parray[p].rating > 100) {
	      pcn_out_prompt(p, CODE_INFO, FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_d_RATED_GAMES_n, 
	      parray[p].srank, 
	      parray[p].rated ? "*" : "", 
	      parray[p].rating, 
	      parray[p].numgam);
	    } else {
	      pcn_out_prompt(p, CODE_INFO, FORMAT_RATINGS_UPDATE_YOU_ARE_NOW_ss_d_WERE_d_d_RATED_GAMES_n, 
	        parray[p].srank, 
	        parray[p].rated ? "*" : "", 
	        parray[p].rating, 
	        orat, 
	        parray[p].numgam);
	    }
          } /* for (p = 0 ; p < parray_top ; p++) */
          rdbm_close(rdb);
        } /* if ((rdb = rdbm_open()) != NULL) */
      }	/* if (resu > 0) */
    }
  }
#endif /* NNGSRATED */
  ShutHeartBeat();
  return COM_OK;
}

void commands_init()
{
  FILE *fp, *afp;
  int i = 0;

  fp = xyfopen(FILENAME_CMDS, "w");
  if (!fp) {
    return;
  }
  afp = xyfopen(FILENAME_ACMDS, "w");
  if (!afp) {
    fclose(fp);
    return;
  }
  for(i=0; command_list[i].comm_name; i++) {
    if (command_list[i].adminLevel >= ADMIN_ADMIN) {
      fprintf(afp, "%s\n", command_list[i].comm_name);
    } else {
      fprintf(fp, "%s\n", command_list[i].comm_name);
    }
  }
  fclose(fp);
  fclose(afp);
}

void TerminateCleanup()
{
  int p1;
  int g;

  for (g = 0; g < garray_top; g++) {
    if (garray[g].gstatus != GSTATUS_ACTIVE) continue;
    game_ended(g, PLAYER_NEITHER, END_ADJOURN);
  }
  for (p1 = 0; p1 < parray_top; p1++) {
    if (!parray[p1].slotstat.is_inuse) continue;
    player_save(p1);
    if (parray[p1].slotstat.is_online) {
      pcn_out(p1, CODE_DOWN, FORMAT_LOGGING_YOU_OUT_n);
      pxysend_raw_file(p1, FILENAME_MESS_LOGOUT);
    }
    if (parray[p1].slotstat.is_connected) {
      player_write_loginout(p1, P_LOGOUT);
      close(parray[p1].socket);
    }
  }
}

