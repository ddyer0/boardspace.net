/* adminproc.h
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

#ifndef ADMINPROC_H
#define ADMINPROC_H

#include "command.h"

extern int com_addplayer(int, struct parameter*);
extern int com_arank(int, struct parameter*);
extern int com_nuke(int, struct parameter*);
extern int com_shutdown(int, struct parameter*);
extern void ShutHeartBeat(void);
extern void ShutDown(void);
extern int com_pose(int, struct parameter*);
extern int com_announce(int, struct parameter*);
extern int com_muzzle(int, struct parameter*);
extern int com_gmuzzle(int, struct parameter*);
extern int com_bmuzzle(int, struct parameter*);
extern int com_asetrealname(int, struct parameter*);
extern int com_asetsilent(int, struct parameter*);
extern int com_asetpasswd(int, struct parameter*);
extern int com_asetemail(int, struct parameter*);
extern int com_asethandle(int, struct parameter*);
extern int com_asetadmin(int, struct parameter*);
extern int com_asetwater(int, struct parameter*);
extern int server_shutdown(int, char *);
extern int com_chk_ip(int, struct parameter*);
extern int com_chk_player(int, struct parameter*);
extern int com_chk_socket(int, struct parameter*);
extern int com_remplayer(int, struct parameter*);
extern int com_raisedead(int, struct parameter*);
extern int strcmpwild(char *, char *);
extern int com_anews(int, struct parameter*);
extern int com_actitle(int, struct parameter*);
extern int com_reload_ladders(int, struct parameter*);
extern int com_createnews(int, struct parameter*);
extern int com_createadmnews(int, struct parameter*);
extern int com_asetdebug(int, struct parameter*);
extern int com_hide(int, struct parameter*);
extern int com_high(int, struct parameter*);
extern int com_unhide(int, struct parameter*);
extern int com_ausers(int, struct parameter*);
extern int com_adrop(int, struct parameter*);
extern int com_noshout(int, struct parameter*);
extern int com_rating_recalc(int, struct parameter*);
extern int Show_Admin_Command(int p, const char *comm, const char *command);
extern int com_aban(int p, struct parameter* param);
#endif /* ADMINPROC_H */
