/* nngsmain.c
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

#ifdef HAVE_SIGNAL_H
#include <signal.h>
#endif

#include "nngsconfig.h"
#include "nngsmain.h"
#include "common.h"
#include "network.h"
#include "command.h"
#include "channel.h"
#include "playerdb.h"
#include "utils.h"
#include "ladder.h"
#include "emote.h"
#include "mink.h"
#include "ip_ban.h"

#ifdef USING_DMALLOC
#include <dmalloc.h>
#define DMALLOC_FUNC_CHECK 1
#endif

/* Arguments */
int port, Ladder9, Ladder19, num_19, num_9, completed_games,
       num_logins, num_logouts, new_players, Debug;
#ifdef WANT_BYTE_COUNT
unsigned long byte_count = 0L;
#endif

/* int globClock = 0; */

void player_array_init(void);
void player_init(void);
static void usage(char *);

static void usage(char *progname);
static void GetArgs(int argc, char *argv[]);
static void main_event_loop(void);
static void TerminateServer(int sig);
static void BrokenPipe(int sig);
static void read_ban_ip_list(void);


static void usage(char *progname) {
  fprintf(stderr, "Usage: %s [-p port] [-h]\n", progname);
  fprintf(stderr, "\t\t-p port\t\tSpecify port. (Default=%d)\n", DEFAULT_PORT);
  fprintf(stderr, "\t\t-h\t\tDisplay this information.\n");
  main_exit(1);
}

static void GetArgs(int argc, char *argv[])
{
  int i;

  port = DEFAULT_PORT;

  for (i = 1; i < argc; i++) {
    if (argv[i][0] == '-') {
      switch (argv[i][1]) {
      case 'p':
	if (i == argc - 1)
	  usage(argv[0]);
	i++;
	if (sscanf(argv[i], "%d", &port) != 1)
	  usage(argv[0]);
	break;
      case 'h':
	usage(argv[0]);
	break;
      }
    } else {
      usage(argv[0]);
    }
  }
}


static void main_event_loop(void)
{
  net_select(1);
}


static void TerminateServer(int sig)
{
  fprintf(stderr, "Got signal %d\n", sig);
  Logit("Got signal %d", sig);
  TerminateCleanup();
  net_closeAll();
  main_exit(1);
}


static void BrokenPipe(int sig)
{
  static time_t lasttime=0;
  static unsigned count=0;

  signal(SIGPIPE, BrokenPipe);
  count++;
  if (globclock.time - lasttime > 10) {
    Logit("Got %u Broken Pipes in %d seconds: sig %d\n"
	, count, globclock.time - lasttime, sig);
    lasttime=globclock.time;
    count=0;
    }
}


int main(int argc, char *argv[])
{
  FILE *fp;

#ifdef USING_DMALLOC
  dmalloc_debug(1);
#endif

  GetArgs(argc, argv);
  signal(SIGTERM, TerminateServer);
  signal(SIGINT, TerminateServer);
#if 0
  signal(SIGPIPE, SIG_IGN);
#else
  signal(SIGPIPE, BrokenPipe);
#endif
  read_ban_ip_list();
  if (net_init(port)) {
    fprintf(stderr, "Network initialize failed on port %d.\n", port);
    main_exit(1);
  }
  startuptime = time(NULL);
  player_high = 0;
  game_high = 0;
#ifdef WANT_BYTE_COUNT
  byte_count = 0;
#endif
  srand(startuptime);

#ifdef SGI
  /*mallopt(100, 1);*/  /* Turn on malloc(3X) debugging (Irix only) */
#endif
  Logit("Initialized on port %d.", port);
  command_init();
  EmoteInit(emotes_file);
  help_init();
  /*Logit("commands_init()");*/
  commands_init();
  /*Logit("channel_init()");*/
  channel_init();
  /* Ochannel_init(); */
  /*Logit("player_array_init()");*/
  player_array_init();
  player_init();
  LadderInit(NUM_LADDERS);
  Ladder9 = LadderNew(LADDERSIZE);
  Ladder19 = LadderNew(LADDERSIZE);

  Debug = 0;
  completed_games = 0;
  num_logins = num_logouts = new_players = 0;

  num_9 = 0;
  fp = xyfopen(FILENAME_LADDER9, "r");
  if(fp) {
    num_9 = PlayerLoad(fp, Ladder9);
    Logit("%d players loaded from file %s", num_9, filename() );
    fclose(fp);
  }

  num_19 = 0;
  fp = xyfopen(FILENAME_LADDER19, "r");
  if(fp) {
    num_19 = PlayerLoad(fp, Ladder19);
    Logit("%d players loaded from file %s", num_19, filename() );
    fclose(fp);
  }

  initmink();
  Logit("Server up and running at");
  main_event_loop();
  Logit("Closing down.");
  net_closeAll();
  main_exit(0);
  return 0;
}

void main_exit(int code)
{

#ifdef USING_DMALLOC
  dmalloc_log_unfreed();
  dmalloc_shutdown();
#endif
  exit(code);
}


static void read_ban_ip_list(void)
{
  FILE *fp;
  int rc,cnt=0;
  char buff[100];
  unsigned bot, top;

  fp = xyfopen(FILENAME_LIST_BAN, "r");
  if (!fp) return;
  while(fgets(buff, sizeof buff, fp)) {
    rc = sscanf(buff, "%x %x", &bot, &top);
    if (rc < 2) continue;
    cnt += range_add(bot,top);
  }
  fclose(fp);
  Logit("Ipban %d from %s", cnt, filename() );
}
