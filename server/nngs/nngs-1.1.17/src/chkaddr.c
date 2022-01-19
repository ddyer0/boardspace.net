/* chkaddr.c
**
** Per-Erik Martin (pem@nngs.cosmic.org) 1999-11-25
*/

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#include "chkaddr.h"

/* Characters allowed in both user and domain name parts. */
#define NAME_CHAR_UNION(C) \
	(('A' <= (C) && (C) <= 'Z') || /* Uppercase A-Z */ \
	 ('a' <= (C) && (C) <= 'z') || /* Lowercase a-z */ \
	 ('0' <= (C) && (C) <= '9') || /* 0-9 */ \
	 ((C) == '-'))		       /* - */

/* This is coded without the use of ctype functions
 * to make it independent of LOCALE
 * Returns 0 if it's not a likely email address,
 * 1 otherwise.
 */
int
chkaddr(const char *s)
{
  int dotcount;
  const char *p = s;

  /* We allow any sequence of [A-Za-z0-9._-] in the user name.
     this is rather liberal, but we have little control over what
     some systems allow here. */
  if (*p == '@')
    return 0;			/* Can't begin with @ */
  do {
    char c = *p++;

    /* This also captures the end of the string (\0) */
    if (!NAME_CHAR_UNION(c) &&
	(c != '.') &&
	(c != '_'))
      return 0;
  } while (*p != '@');

  /* Post condition: *p == '@' */
  if (*++p == '\0')
    return 0;			/* Can't end with @ */

  /* In the domain part, we're more strict.
     Only sequences of name parts separated by '.' (no leading or
     trailing '.' allowed), with at least one '.'. The name parts
     are [A-Za-z0-9_-] sequences. Note that '_' is NOT allowed in
     domain names. */
  dotcount = -1;
  do {

    if (*p == '.' || *p == '\0')
      return 0;		/* Dot in the wrong place (first, last or double) */
    dotcount += 1;
    do {
      char c = *p++;

      if (!NAME_CHAR_UNION(c))
	return 0;
    } while (*p != '.' && *p != '\0');

  } while (*p++ == '.');

  if (dotcount < 1)
    return 0;			/* Too few dots */
  return 1;
}

#ifdef TEST
int
main(int argc, char **argv)
{
  char buf[1024];

  while (fgets(buf, 1024, stdin))
  {
    buf[strlen(buf)-1] = '\0';
    if (chkaddr(buf))
      printf("+ %s\n", buf);
    else
      printf("- %s\n", buf);
  }
  main_exit(0);
}
#endif /* TEST */
