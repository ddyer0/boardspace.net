/* comproc.h
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

#ifndef COMPROC_H
#define COMPROC_H
#include <stdio.h> /* for FILE */

extern int com_rating_recalc(int, struct parameter *);
extern int com_more(int, struct parameter *);

extern int com_quit(int, struct parameter *);
extern int com_register(int, struct parameter *);
extern int com_ayt(int, struct parameter *);
extern int com_help(int, struct parameter *);
extern int com_info(int, struct parameter *);
extern int com_rank(int, struct parameter *);
extern int com_ranked(int, struct parameter *);
extern int com_infor(int, struct parameter *);
extern int com_adhelp(int, struct parameter *);
extern int com_shout(int, struct parameter *);
extern int com_ashout(int, struct parameter *);
extern int com_cshout(int, struct parameter *);
extern int com_gshout(int, struct parameter *);
extern int com_query(int, struct parameter *);
extern int com_it(int, struct parameter *);
extern int com_git(int, struct parameter *);
extern int com_tell(int, struct parameter *);
extern int com_say(int, struct parameter *);
extern int com_choice(int, struct parameter *);
extern int com_whisper(int, struct parameter *);
extern int com_kibitz(int, struct parameter *);
extern int com_set(int, struct parameter *);
extern int com_stats(int, struct parameter *);
extern int com_cstats(int, struct parameter *);
extern int com_password(int, struct parameter *);
extern int com_uptime(int, struct parameter *);
extern int com_date(int, struct parameter *);
extern int com_llogons(int, struct parameter *);
extern int com_logons(int, struct parameter *);
extern int com_who(int, struct parameter *);
extern int com_censor(int, struct parameter *);
extern int com_uncensor(int, struct parameter *);
extern int com_notify(int, struct parameter *);
extern int com_unnotify(int, struct parameter *);
extern int com_channel(int, struct parameter *);
extern int com_inchannel(int, struct parameter *);
extern int com_gmatch(int, struct parameter *);
extern int com_tmatch(int, struct parameter *);
extern int com_goematch(int, struct parameter *);
extern int com_cmatch(int, struct parameter *);
extern int com_decline(int, struct parameter *);
extern int com_withdraw(int, struct parameter *);
extern int com_pending(int, struct parameter *);
extern int com_accept(int, struct parameter *);
extern int com_refresh(int, struct parameter *);
extern int com_open(int, struct parameter *);
extern int com_bell(int, struct parameter *);
extern int com_style(int, struct parameter *);
extern int com_promote(int, struct parameter *);
extern int com_alias(int, struct parameter *);
extern int com_unalias(int, struct parameter *);
extern int create_new_gomatch(int, int, int, int, int, int, int, int);
extern int com_servers(int, struct parameter *);
extern int com_sendmessage(int, struct parameter *);
extern int com_messages(int, struct parameter *);
extern int com_clearmess(int, struct parameter *);
extern int com_variables(int, struct parameter *);
extern int com_mailsource(int, struct parameter *);
extern int com_mailhelp(int, struct parameter *);
extern int com_handles(int, struct parameter *);
extern int com_znotify(int, struct parameter *);
extern int com_addlist(int, struct parameter *);
extern int com_sublist(int, struct parameter *);
extern int com_showlist(int, struct parameter *);
extern int com_news(int, struct parameter *);
extern int com_beep(int, struct parameter *);
extern int com_qtell(int, struct parameter *);
extern int com_getpi(int, struct parameter *);
extern int com_getps(int, struct parameter *);
extern int com_pass(int, struct parameter *);
extern int com_undo(int, struct parameter *);
extern int com_komi(int, struct parameter *);
extern int com_handicap(int, struct parameter *);
extern int com_ladder(int, struct parameter *);
extern int com_join(int, struct parameter *);
extern int com_drop(int, struct parameter *);
extern int com_score(int, struct parameter *);
extern int com_awho(int, struct parameter *);
extern int com_review(int, struct parameter *);
extern int com_watching(int, struct parameter *);
extern int com_clntvrfy(int, struct parameter *);
extern int com_mailme(int, struct parameter *);
extern int com_me(int, struct parameter *);
extern int com_pme(int, struct parameter *);
extern int com_teach(int, struct parameter *);
extern int com_admins(int, struct parameter *);
extern int com_spair(int, struct parameter *);
extern int com_nocaps(int, struct parameter *);
extern int com_reset(int, struct parameter *);
extern int com_translate(int, struct parameter *);
extern int com_find(int, struct parameter *);
extern int com_best(int, struct parameter *);
extern int com_ctitle(int, struct parameter *);
extern int com_lock(int, struct parameter *);
extern int com_unlock(int, struct parameter *);
extern int com_invite(int, struct parameter *);
extern int com_emote(int, struct parameter *);
extern int com_mailmess(int, struct parameter *);
extern int com_suggest(int, struct parameter *);
extern int com_nsuggest(int, struct parameter *);
extern int com_note(int, struct parameter *);
extern int com_shownote(int, struct parameter *);
extern int com_ping(int, struct parameter *);

extern int com_nrating(int, struct parameter *);
#ifdef NNGSRATED
extern int com_rating(int, struct parameter *);
#endif
extern int plogins(int, FILE *);
extern int com_dnd(int, struct parameter *);
extern int com_which_client(int, struct parameter *);
extern void AutoMatch(int, int, int *, float *);
extern int in_list(const char *, const char *);
extern int in_list_match(const char *, const char *);
extern int com_lchan(int, struct parameter *);
extern int com_lashout(int, struct parameter *);

#define CLIENT_UNKNOWN		0
#define CLIENT_TELNET		1
#define CLIENT_XIGC		2
#define CLIENT_WINIGC		3
#define CLIENT_WIGC		4
#define CLIENT_CGOBAN		5
#define CLIENT_JAVA		6
#define CLIENT_TGIGC		7
#define CLIENT_TGWIN		8
#define CLIENT_FIGC		9
#define CLIENT_PCIGC		10
#define CLIENT_GOSERVANT	11
#define CLIENT_MACGO		12
#define CLIENT_AMIGAIGC		13
#define CLIENT_HAICLIENT	14
#define CLIENT_IGC		15
#define CLIENT_KGO		16
#define CLIENT_NEXTGO		17
#define CLIENT_OS2IGC		18
#define CLIENT_STIGC		19
#define CLIENT_XGOSPEL		20
#define CLIENT_TKGC		21
#define CLIENT_JAGOCLIENT	22
#define CLIENT_GTKGO		23

#endif /* COMPROC_H */
