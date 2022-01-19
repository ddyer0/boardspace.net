helpdatadir=${prefix}/share/${PACKAGE}/help
helpdata_DATA=

LANGUAGES=en ch fr pl


## This is from autobook 14.3 install-data-local:
install-data-hook:
	@for l in ${LANGUAGES}; do\
	for f in $$l/* ; do\
	  d="`echo $$f | sed -e 's,/[^/]*$$,,'`"; \
	  $(mkinstalldirs) $(DESTDIR)$(helpdatadir)/$$d; \
	  if test -f $(srcdir)/$$f; then p=$(srcdir)/$$f; else p=$$f; fi; \
	  $(INSTALL_DATA) $$p $(DESTDIR)$(helpdatadir)/$$f; \
	  done \
	done

# Remove these files here... they are created locally
DISTCLEANFILES = *~

@NO_WARNINGS@
