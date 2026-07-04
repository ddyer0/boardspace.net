
This is the root for java based projects.  To get running, 

First, duplicate the "eclipse" folder.  I recommend calling it "myeclipse"
If you call it something else, add the name to the .gitignore file.
This is your private copy of the eclipse projects needed to build boardspace,
which git won't try to push back into the repository.

open eclipse
choose "new workspace"
navigate to this myeclipse folder
select file + open projects from file system.

you should be offered all projects in this directory, select them all.

the initial build state should be clean, with no errors and a bunch of warnings.
I configure eclipse to suppress these warnings, but you may have other preferences.
If any problems have crept in, they're probably errors in the build path thich
are easily corrected.

You're ready to go.  Try the debug configuration for "Offline Viewer for Hex".
This is the normal development mode for new games or working on existing games.
No external servers are needed. All the live play machinery is unnecessary
for routine work.

There are other launches; The starting launches for "prototype" and "offline"
are the other good starting points. There are other launches that connect to 
servers, which will be useful for live testing, but you can safely ignore
 all of that for now.  Note that as distributed, the files in the repository 
will not be able to connect to the live boardspace server without some additional magic.

bsh, joi, pf, and jzlib are minor supporting projects, with very little
dependency in the main java projects. 

bsh joi and pf are entirely optional and could be cut out completely with little effort.  
They're used only in some exotic and little used modes of debugging.

jzlib is a lightly hacked version of a zip file reader, needed because the standard
version had some trouble with file names containing unicode characters.  At some point
that may be unnecessary, but that would have to be verified. Don't revert to the standard
version without testing.

boardspace-cn1-prod and boardspace-cn2 are projects used to build the IOS and Android versions
of boardspace.  You can safely close these projects, and ignore all the source files in the
"boardspace-codename1" hierarchy.   These source files are almost copies of the main sources,
and you can safely ignore them in normal development.

the main projects are 

boardspace-core		the core boardspace.net files that support all the games.
boardspace		all the games
boardspace-cn1-prod	the production project for mobile release
boardspace-cn2		the debug project for mobile development, which shares all
			all sources with boardspace-cn1-prod
boardspace-strings	maintains the language translation database, in the boardspace "translation" table.

Generally, I recommend using the existing games as a template for anything you do, and
treat the way things are done as an idiom to be imitated.  The javadoc, while somewhat
useful, is generally much less useful than working examples.  Of course you're free to
invent your own, but the existing framework knits everything together in many synergistic
ways.


