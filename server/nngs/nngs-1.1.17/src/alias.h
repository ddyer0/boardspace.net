#ifndef ALIAS_H
#define ALIAS_H

#include <stdio.h>	/* for the size_t definition */

/* Creates a alias list. */
extern struct alias * alias_init(void);

/* Returns the number of aliases in list. */
extern size_t alias_count(struct alias * p);

/* Returns a pointer to the alias of cmd if found, NULL otherwise. */
extern char *alias_lookup(const char *cmd, struct alias *p);

/* Adds the alias for cmd. Returns 1 if it replaced an old entry,
** or 0 if it was a new entry.
*/
extern int alias_add(const char *cmd, const char *alias, struct alias *p);

/* Removes alias for cmd from list. Returns 1 if it was there,
** or 0 if it wasn't found.
*/
extern int alias_rem(const char *cmd, struct alias *p);

/* Initiates an iteration through the list.
** A typical iteration (through the list 'al') looks like this:
**
** {
**   char *c, *a;
**
**   alias_start(al);
**   while (alias_next(&c, &a, al))
**     do_something_with(c, a);
** }
*/
extern void alias_start(struct alias *p);

/* Gives the command/alias pair for the next entry in turn.
** Returns 1 if there was one, 0 if we reached the end.
** The pointers 'cp' and 'ap' may be NULL. A non-null pointer
** is updated to the next value if 1 was returned.
*/
extern int alias_next(char **cp, char **ap, struct alias *p);

/* Destroy the list
*/
extern void alias_free(struct alias *p);

#endif /* ALIAS_H */
