
This is the root for java based projects.  To get running, 

open eclipse
choose "new workspace"
navigate to this folder
select import/general/exisiting projects

you should be offered all projects in this directory, select them all.

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

the files in the codename1 and desktop branches are almost identical.  My practice
is to do development in the desktop branch, then use winmerge to migrate the changes
to the codename1 branch.  This is a bit inconvenient, but provides a check against 
coding accidents.

