/* command.h
 *
 */

/*
    NNGS - The No Name Go Server
    Copyright (C) 1995-1997  Erik Van Riper (geek@midway.com)
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

#ifndef COMMAND_H
#define COMMAND_H

extern const char *mess_dir;
extern const char *help_dir;
extern const char *comhelp_dir;
extern const char *info_dir;
extern const char *ahelp_dir;
extern const char *stats_dir;
extern const char *config_dir;
extern const char *player_dir;
extern const char *game_dir;
extern const char *cgame_dir;
extern const char *problem_dir;
extern const char *board_dir;
extern const char *def_prompt;
extern const char *source_dir;
extern const char *lists_dir;
extern const char *news_dir;

extern const char *server_name;
extern const char *server_address;
extern const char *server_email;
extern const char *server_http;
extern const char *geek_email;

extern const char *intergo_file;
extern const char *ratings_file;
extern const char *results_file;
extern const char *nresults_file;
extern const char *emotes_file;
extern const char *note_file;
extern const char *log_file;
extern const char *ladder9_file;
extern const char *ladder19_file;

extern const char *stats_logons;
extern const char *stats_messages;
extern const char *stats_games;
extern const char *stats_rgames;
extern const char *stats_cgames;

extern const char *version_string;

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#ifdef HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif

extern mode_t mode_for_dir;

extern time_t startuptime;
extern int player_high;
extern int game_high;
extern int MailGameResult;

extern struct stat RatingsBuf1, RatingsBuf2;

/* Maximum length of a login name */
#define MAX_LOGIN_NAME 10

/* Maximum number of parameters per command */
#define MAXNUMPARAMS 10

/* Maximum string length of a single command word */
#define MAX_COM_LENGTH 50

/* Maximum string length of the whole command line */
#define MAX_STRING_LENGTH 1024

#define COM_BADFD -1
#define COM_OK 0
#define COM_FAILED 1
#define COM_ISMOVE 2
#define COM_AMBIGUOUS 3
#define COM_BADPARAMETERS 4
#define COM_BADCOMMAND 5
#define COM_LOGOUT 6
#define COM_FLUSHINPUT 7
#define COM_RIGHTS 8
#define COM_OK_NOPROMPT 9
#define COM_NOSUCHUSER 10
#define COM_NOSUCHGAME 11
#define COM_OKN 12

#define ADMIN_USER	0
#define ADMIN_GUEST	1
#define ADMIN_REGISTERED_USER	2
#define ADMIN_ADMIN	20
#define ADMIN_MASTER	50
#define ADMIN_DEMIGOD   60
#define ADMIN_GOD	100
#define EXISTING_USERS_ARE_REGISTERED 1

#define TYPE_NULL 0
#define TYPE_WORD 1
#define TYPE_STRING 2
#define TYPE_INT 3
#define TYPE_FLOAT 4

struct parameter {
  int type;
  union {
    char *word;
    char *string;
    float f;
    int integer;
  } val;
} ;

/* typedef parameter param_list[MAXNUMPARAMS]; */
/* defined to avoid compiler warnings */
#define UNUSED(_p) (void)(_p)

struct command_type {
  const char *comm_name;
  const char *param_string;
  int (*comm_func)(int p, struct parameter * a);
  int adminLevel;
} ;

struct alias_type {
  const char *comm_name;
  const char *alias;
} ;

extern int commanding_player; /* The player whose command your in */

extern int process_input(int fd, char *str);
extern void process_new_connection(int fd, unsigned int addr);
extern void process_disconnection(int fd);
extern int process_incomplete(int fd, char * line);
extern int process_heartbeat(int *fdp);

extern void commands_init(void);

extern void TerminateCleanup(void);
extern int process_command(int, char *);

extern void command_init(void);
extern int command_admin_level(int c);
extern int match_command(const char *);

#endif /* COMMAND_H */
