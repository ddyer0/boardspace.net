bin_PROGRAMS = nngssrv

noinst_LIBRARIES = libladder.a
libladder_a_SOURCES = ladder.c emote2.c


EXTRA_DIST =

INCLUDES = \
        -I$(top_srcdir)/nrat\
        -I$(top_srcdir)/memory

LDADD = \
        $(top_srcdir)/nrat/libnrat.a \
        $(top_srcdir)/memory/libmemory.a \
        $(top_srcdir)/src/libladder.a \
        $(top_srcdir)/mlrate/src/libmlr.a

nngssrv_SOURCES = \
	network.c \
	bm.c \
	channel.c \
	utils.c \
	language.c \
	variable.c \
	legal.c \
	gameproc.c \
	adminproc.c \
	nngsmain.c \
	multicol.c \
	mink.c \
	alias.c \
	censor.c \
	plan.c \
	ip_ban.c \
	chkaddr.c

noinst_HEADERS =

# Remove these files here... they are created locally
DISTCLEANFILES = *~ *%

@NNGS_ALL_WARNINGS@
