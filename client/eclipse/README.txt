
This is the root for java based projects.  To get running, 

First, make a copy of the "eclipse" folder.  I recommend calling it "myeclipse"
If you call it something else, add the name to the .gitignore file.
This is your private copy of the eclipse projects needed to build boardspace.

open eclipse
choose "new workspace"
navigate to this myeclipse folder
select import/general/exisiting projects

you should be offered all projects in this directory, select them all.

the initial build state should contain a few build path problems that
Need to be edited to your file system. There also should be "Missing"
Class files common/Salt.java.  These are missing by design, as they contain
Crypto keys to communicate with the real boardspace.net server.  Replace
These files with a copy of Dummy.java

You should end up with a project with no errors and a finite number of
warnings, the exact number depending on your eclipse settings.

bsh, joi, pf, and jzlib are minor supporting projects, with very little
dependency in the main java projects. 

bsh joi and pf are entirely optional and could be cut out completely with little effort.  
They're used only in some exotic and little used modes of debugging.

jzlib is a lightly hacked version of a zip file reader, needed because the standard
version had some trouble with file names containing unicode characters.  At some point
that may be unnecessary, but that would have to be verified. Don't revert to the standard
version without testing.

the main projects are 

boardspace-core		the core boardspace.net files that support all the games.
boardspace		all the games
boardspace-cn1-prod	the production project for mobile release
boardspace-cn2		the debug project for mobile development, which shares all
			all sources with boardspace-cn1-prod
boardspace-strings	maintains the language translation database, in the boardspace "translation" table.

the files in the codename1 and desktop branches are almost identical.  My practice
is to do development in the desktop branch, then use winmerge to migrate the changes
to the codename1 branch.  This is a bit inconvenient, but provides a check against 
coding accidents.

The starting launches for "prototype" and "offline" are the starting points.



