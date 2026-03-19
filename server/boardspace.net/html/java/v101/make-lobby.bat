copy G:\share\projects\eclipse\jzlib\jzlib\jzlib.jar .
copy G:\share\projects\eclipse\pf\pf-joi.jar .
copy G:\share\projects\eclipse\bsh-2.1.8.jar .\bsh.jar
set JAR = "f:\java\jdk-15.0.1\bin\jar.exe"
%JAR -cmf generic-manifest.txt Game.jar online/game/*.class online/game/export/*.class online/game/sgf/*.class online/game/sgf/export/*.class online/search/*.class online/search/nn/*.class

%JAR -cmf manifest.txt OnlineLobby.jar  util/*.class lib/*.class bridge/*.class online/common/*.class common/*.class rpc/*.class udp/*.class vnc/*.class online/images/*.jpg online/images/*.png online/language/*.class
%JAR -i OnlineLobby.jar

