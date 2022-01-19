
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

#ifdef HAVE_STRING_H
#include <string.h>
#endif


#include "playerdb.h" /* for PENDING_SIZE */
#include "pending.h"
#include "utils.h"

#ifndef DEBUG_PENDING
#define DEBUG_PENDING 0
#endif

static struct pending pendingarray[PENDING_SIZE]; 
static struct pending *pendlist=NULL;
static struct pending *pendfree=NULL;
static struct pending **pendtail=NULL;
static struct{
	int avail , valid ;
	int free , used;
	} pendcnt = {0,0,0,0};

static struct pending * pending_alloc(void);
static void pending_free(struct pending * ptr);
static void pending_ins(struct pending **hnd, struct pending * ins);
static void pending_cut(struct pending * ptr);
static void pending_add(struct pending * ins);
#if DEBUG_PENDING
static char * pending_dmp(void);
#endif /* DEBUG_PENDING */

	/*
	** Initialize array. create linked list under freelist.
	*/
void pending_init(void)
{
  struct pending **hnd;
  size_t ii;

  pendlist= NULL;
  pendtail = &pendlist;
  pendfree= NULL;
  hnd= &pendfree;
  for(ii=0; ii < COUNTOF(pendingarray); ii++ ) {
    pendingarray[ii].valid = 0;
    pendingarray[ii].seq = 0;
    pendingarray[ii].hnd = hnd;
    *hnd = & pendingarray[ii];
    hnd = & pendingarray[ii].nxt;
  }
  *hnd = NULL;
pendcnt.avail = ii;
#if DEBUG_PENDING
Logit("Pending_init : %s", pending_dmp());
#endif /* DEBUG_PENDING */
}


static struct pending * pending_alloc(void)
{
  struct pending * ptr;

  ptr= pendfree;
  if(!ptr) return NULL;
  pending_cut(ptr);
  pendcnt.avail--;
  pendcnt.valid++;
  ptr->valid=1;
  return ptr;
}


static void pending_free(struct pending * ptr)
{
  if(!ptr) return;
  if(pendtail == &ptr->nxt) pendtail = NULL;
  pending_cut(ptr);
  pending_ins(&pendfree,ptr);
  pendcnt.avail++;
  if(ptr->valid) pendcnt.valid++;
  ptr->valid = 0;
}
	/* cut ptr out of linked list. */
static void pending_cut(struct pending * ptr)
{
  struct pending * nxt;

  if(!ptr) return;
  nxt = ptr->nxt;
  if(nxt==ptr) nxt= NULL;
  if(ptr->hnd) *(ptr->hnd) = nxt;
  if(nxt) nxt->hnd = ptr->hnd;
#if 1
  ptr->hnd=NULL;
  ptr->nxt=NULL;
#endif
}

	/* insert ins before *hnd */
static void pending_ins(struct pending **hnd, struct pending * ins)
{
  if(!ins) return;
  ins->nxt = *hnd;
  ins->hnd= hnd;
  *hnd = ins;
  if(ins->nxt) {
    hnd = &ins->nxt;
    ins = ins->nxt;
    ins->hnd= hnd;
  }
}

	/* append at tail */
static void pending_add(struct pending * ins)
{
  if(!ins) return;
  if(!pendtail) pendtail = &pendlist;

  while (*pendtail) pendtail = &(*pendtail)->nxt;
#if 0
  ins->hnd = pendtail;
  *pendtail = ins;
#else
  pending_ins(pendtail,ins);
#endif
  while (*pendtail) pendtail = &(*pendtail)->nxt;
}

	/* Allocate pending structure */
struct pending *pending_new(int from, int to, int type)
{
  struct pending *ptr;

  ptr= pending_alloc();
  if(ptr) {
    ptr->seq=0;
    ptr->whofrom = from;ptr->whoto = to;ptr->type = type;
    pending_add(ptr);
  }
#if DEBUG_PENDING
Logit("Pending_new(%d,%d,%d) := %p%s", from,to,type,ptr,pending_dmp() );
#endif /* DEBUG_PENDING */
  return ptr;
}

	/* Free pending structure instance by setting it's valid field
        ** to zero.
        ** Later , the actual cutting it out of it's current LL 
	** and inserting it into freelist is done by pending_count()
	** all this is done to support the silly count/find/next
	** iterator.
	*/
void pending_delete(struct pending *ptr)
{
  if (!ptr) return;
  if(ptr->valid) pendcnt.valid--;
  ptr->valid=0;
#if 0
  pending_free(ptr);
#endif
#if DEBUG_PENDING
Logit("Pending_delete(%p:%d,%d,%d) : %s"
	, ptr,ptr->whofrom,ptr->whoto,ptr->type, pending_dmp() );
#endif /* DEBUG_PENDING */
  return ;
}


struct pending *pending_find(int from, int to, int type)
{
  struct pending *ptr;
  struct pending *nxt;

  for (ptr=pendlist; ptr; ptr=nxt) {
    if(ptr->nxt==ptr) ptr->nxt=NULL;
    nxt = ptr->nxt;
    if (!ptr->valid) {
      pending_free(ptr);
      continue;
    }
    if (ptr->whofrom != from && from >= 0) continue;
    if (ptr->whoto != to && to >= 0) continue;
    if (ptr->type != type && type >= 0) continue;
    ptr->seq = 1;
    break;
  }
#if DEBUG_PENDING
if(ptr)
	Logit("Pending_find(%d,%d,%d):= %p:{%d,%d,%d}",from,to,type
	,ptr,ptr->whofrom,ptr->whoto,ptr->type);
else
	Logit("Pending_find(%d,%d,%d):= NULL",from,to,type);
#endif /* DEBUG_PENDING */
  return ptr;
}

struct pending * pending_next(struct pending *ptr, int from, int to, int type)
{
  unsigned seq=0;
  struct pending *nxt;

  if(!ptr) return NULL;
  seq=ptr->seq;
  for (ptr=ptr->nxt; ptr ; ptr=nxt) {
    if(ptr->nxt==ptr) ptr->nxt=NULL;
    nxt = ptr->nxt;
    if (!ptr->valid) {
      pending_free(ptr);
      continue;
    }
    if (ptr->whofrom != from && from >= 0) continue;
    if (ptr->whoto != to && to >= 0) continue;
    if (ptr->type != type && type >= 0) continue;
    ptr->seq = ++seq;
    break;
  }
#if DEBUG_PENDING
if(ptr)
	Logit("Pending_next(%d,%d,%d):= %p:{%d,%d,%d}",from,to,type
	,ptr,ptr->whofrom,ptr->whoto,ptr->type);
else
	Logit("Pending_next(%d,%d,%d):= %p:{%d,%d,%d}",from,to,type);
#endif /* DEBUG_PENDING */
  return ptr;
}

int pending_count(int from, int to, int type)
{
  struct pending *ptr, *nxt;
  int count=0;

  for (ptr=pendlist; ptr; ptr=nxt) {
    if(ptr->nxt==ptr) ptr->nxt=NULL;
    nxt=ptr->nxt;
    if (!ptr->valid) {
      pending_free(ptr);
      continue;
    }
    if (ptr->whofrom != from && from >= 0) continue;
    if (ptr->whoto != to && to >= 0) continue;
    if (ptr->type != type && type >= 0) continue;
    count++;
  }
#if DEBUG_PENDING
Logit("Pending_count(%d,%d,%d):= %d:%s",from,to,type,count, pending_dmp() );
#endif /* DEBUG_PENDING */
  return count;
}


#if DEBUG_PENDING
static char * pending_dmp(void)
{
  static char buff[100];
  struct pending *ptr;

  pendcnt.used=0;
  for (ptr=pendlist; ptr; ptr=ptr->nxt) {
    pendcnt.used++;
  }

  pendcnt.free=0;
  for (ptr=pendfree; ptr; ptr=ptr->nxt) {
    pendcnt.free++;
  }

sprintf(buff,"Avail=%d,Valid=%d,Free=%d,Used=%d"
	, pendcnt.avail,pendcnt.valid,pendcnt.free,pendcnt.used);
return buff;
}
#endif /* DEBUG_PENDING */
