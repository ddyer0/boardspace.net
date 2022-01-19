/* $Id: rdbm.c,v 1.2 2002/01/27 16:02:56 pem Exp $
**
** Per-Erik Martin (pem@pem.nu) 1997-12.
**
**   Copyright (C) 1998-2002  Per-Erik Martin
**
**   This program is free software; you can redistribute it and/or modify
**   it under the terms of the GNU General Public License as published by
**   the Free Software Foundation; either version 2 of the License, or
**   (at your option) any later version.
**
**   This program is distributed in the hope that it will be useful,
**   but WITHOUT ANY WARRANTY; without even the implied warranty of
**   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**   GNU General Public License for more details.
**
**   You should have received a copy of the GNU General Public License
**   along with this program; if not, write to the Free Software
**   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
*/

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <ctype.h>
#include <assert.h>		/* Disable with -DNDEBUG */
#include "player.h"

/* "rdbm.h" redifines things in "xdbm.c". */
#include "rdbm.h"
#define _xdbm_c_included
#include "xdbm.c"

void
rdbm_printheader(FILE *fp)
{
  int i;

  fprintf(fp, "%-*s Rank  Rate     W+L   =Games  -Low  +High\n",
	  PLAYER_MAX_NAMELEN, "Name");
  for (i = 0 ; i < PLAYER_MAX_NAMELEN ; i++)
    putc('-', fp);
  fputs(" ----- -----  --------------  ----- -----\n", fp);
}

void
rdbm_printline(FILE *fp, rdbm_player_t *rp)
{
  fprintf(fp, "%-*s %4s%c %5.2f  %3u+%-3u =%5u  %+5.2f %+5.2f\n",
	  PLAYER_MAX_NAMELEN,
	  rp->name, rp->rank, (rp->star ? '*' : ' '), rp->rating,
	  rp->wins, rp->losses, rp->wins+rp->losses,
	  rp->low, rp->high);
}

void
rdbm_wprintline(FILE *fp, rdbm_player_t *rp)
{
  fprintf(fp, "%-*s %4s%c %5.2f  %5.1f+%-5.1f =%7.1f  %+5.2f %+5.2f\n",
	  PLAYER_MAX_NAMELEN,
	  rp->name, rp->rank, (rp->star ? '*' : ' '), rp->rating,
	  rp->wwins, rp->wlosses, rp->wwins+rp->wlosses,
	  rp->low, rp->high);
}
