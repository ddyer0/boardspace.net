copy G:\share\projects\eclipse\jzlib\jzlib\jzlib.jar .
copy G:\share\projects\eclipse\pf\pf-joi.jar .
copy G:\share\projects\eclipse\bsh-2.1.8.jar .\bsh.jar
set JAR = "f:\java\jdk-15.0.1\bin\jar.exe"

%JAR -cmf manifest.txt OnlineLobby.jar  util/*.class lib/*.class bridge/*.class online/common/*.class common/*.class rpc/*.class udp/*.class vnc/*.class online/images/*.jpg online/images/*.png online/language/*.class
%JAR -cmf manifest.txt Launcher.jar util/*.class lib/*.class bridge/*.class online/common/OnlineConstants*.class
%JAR -cmf miniloader-manifest.txt boardspace.jar *.class
%JAR -cmf generic-manifest.txt Game.jar online/game/*.class online/game/export/*.class online/game/sgf/*.class online/game/sgf/export/*.class online/search/*.class online/search/nn/*.class
%JAR -cmf generic-manifest.txt Sounds.jar bfms/*.au
%JAR -cmf generic-manifest.txt Icons.jar  icons
%JAR -cmf generic-manifest.txt OnlineGame.jar zertz/common/*.class zertz/images/*.jpg
%JAR -cmf generic-manifest.txt OnlineLoa.jar loa/*.class loa/images/*.jpg
%JAR -cmf generic-manifest.txt OnlinePlateau.jar plateau/common/*.class plateau/images/*.jpg
%JAR -cmf generic-manifest.txt OnlineYinsh.jar yinsh/common/*.class yinsh/images/*.jpg
%JAR -cmf generic-manifest.txt Hex.jar hex/*.class hex/images/*.jpg
%JAR -cmf generic-manifest.txt Trax.jar trax/*.class trax/images/*.jpg
%JAR -cmf generic-manifest.txt Punct.jar punct/*.class punct/images/*.jpg
%JAR -cmf generic-manifest.txt Gobblet.jar gobblet/*.class gobblet/images/*.jpg
%JAR -cmf generic-manifest.txt Hive.jar hive/*.class hive/images/*.jpg
%JAR -cmf generic-manifest.txt Exxit.jar exxit/*.class exxit/images/*.jpg
%JAR -cmf generic-manifest.txt Hnefatafl.jar tablut/*.class tablut/images/*.jpg
%JAR -cmf generic-manifest.txt Tumble.jar tumble/*.class tumble/images/*.jpg 
%JAR -cmf generic-manifest.txt Truchet.jar truchet/*.class truchet/images/*.jpg 
%JAR -cmf generic-manifest.txt Dipole.jar dipole/*.class dipole/images/*.jpg 
%JAR -cmf generic-manifest.txt Fanorona.jar fanorona/*.class fanorona/images/*.jpg
%JAR -cmf generic-manifest.txt Volcano.jar volcano/*.class volcano/images/*.jpg
%JAR -cmf generic-manifest.txt Traboulet.jar kuba/*.class kuba/images/*.jpg
%JAR -cmf generic-manifest.txt Dvonn.jar dvonn/*.class dvonn/images/*.jpg
%JAR -cmf generic-manifest.txt Tzaar.jar tzaar/*.class tzaar/images/*.jpg
%JAR -cmf generic-manifest.txt Qyshinsu.jar qyshinsu/*.class qyshinsu/images/*.jpg
%JAR -cmf generic-manifest.txt Knockabout.jar knockabout/*.class knockabout/images/*.jpg
%JAR -cmf generic-manifest.txt Gipf.jar gipf/*.class gipf/images/*.jpg
%JAR -cmf generic-manifest.txt Palago.jar palago/*.class palago/images/*.jpg
%JAR -cmf generic-manifest.txt Santorini.jar santorini/*.class santorini/images/*.jpg
%JAR -cmf generic-manifest.txt Spangles.jar spangles/*.class spangles/images/*.jpg
%JAR -cmf generic-manifest.txt Micropul.jar micropul/*.class micropul/images/*.jpg
rem %JAR -cmf generic-manifest.txt Medina.jar medina/*.class medina/images/*.jpg
%JAR -cmf generic-manifest.txt Cannon.jar cannon/*.class cannon/images/*.jpg
%JAR -cmf generic-manifest.txt Warp6.jar warp6/*.class warp6/images/*.jpg warp6/images/*.au
%JAR -cmf generic-manifest.txt Triad.jar triad/*.class triad/images/*.jpg
%JAR -cmf generic-manifest.txt Che.jar che/*.class che/images/*.jpg
%JAR -cmf generic-manifest.txt Mutton.jar mutton/*.class mutton/images/*.jpg mutton/images/*.png mutton/sounds/*.au
%JAR -cmf generic-manifest.txt Octiles.jar octiles/*.class octiles/images/*.jpg
%JAR -cmf generic-manifest.txt Frogs.jar frogs/*.class frogs/images/*.jpg
%JAR -cmf generic-manifest.txt Xiangqi.jar xiangqi/*.class  xiangqi/images/*.jpg
%JAR -cmf generic-manifest.txt BreakingAway.jar breakingaway/*.class  breakingaway/images/*.jpg
%JAR -cmf generic-manifest.txt Container.jar container/*.class container/images/*.jpg
%JAR -cmf generic-manifest.txt Arimaa.jar arimaa/*.class arimaa/images/*.jpg arimaa/images/*.au
%JAR -cmf generic-manifest.txt Crossfire.jar crossfire/*.class crossfire/images/*.jpg
%JAR -cmf generic-manifest.txt Quinamid.jar quinamid/*.class quinamid/images/*.jpg quinamid/images/*.au
rem %JAR -cmf generic-manifest.txt Lehavre.jar lehavre/view/*.class lehavre/view/menus/*.class lehavre/view/labels/*.class lehavre/util/*.class lehavre/model/*.class lehavre/model/goods/*.class lehavre/model/buildings/standard/*.class lehavre/model/buildings/start/*.class lehavre/model/buildings/*.class
rem jar -uf Lehavre.jar lehavre/*.class lehavre/model/buildings/special/*.class lehavre/main/*.class
rem jar -uf Lehavre.jar lehavre/images/neutral/bits/*.jpg lehavre/images/neutral/symbols/*.jpg lehavre/images/en/*.jpg 
rem jar -uf Lehavre.jar lehavre/images/en/chits/supply/*.jpg lehavre/images/en/chits/goods/*.jpg 
rem jar -uf Lehavre.jar lehavre/images/en/cards/*.jpg lehavre/images/en/cards/ships/*.jpg lehavre/images/en/cards/rounds/*.jpg 
rem jar -uf Lehavre.jar lehavre/images/en/cards/overview/*.jpg lehavre/images/en/cards/butteries/*.jpg lehavre/images/en/cards/buildings/*.jpg
rem jar -uf Lehavre.jar lehavre/images/de/cards/buildings/*.jpg
%JAR -cmf generic-manifest.txt Entrapment.jar entrapment/images/*.jpg entrapment/*.class
%JAR -cmf generic-manifest.txt Yspahan.jar yspahan/*.class yspahan/images/*.jpg yspahan/images/*.au
%JAR -cmf generic-manifest.txt Gounki.jar gounki/*.class gounki/images/*.jpg
%JAR -cmf generic-manifest.txt Volo.jar volo/*.class volo/images/*.jpg
%JAR -cmf generic-manifest.txt Cookie.jar cookie/*.class cookie/images/*.jpg
rem %JAR -cmf generic-manifest.txt Snakes.jar snakes/*.class snakes/images/*.jpg
rem %JAR -cmf generic-manifest.txt TicTacNine.jar tictacnine/*.class tictacnine/images/*.jpg
%JAR -cmf generic-manifest.txt Universe.jar universe/*.class universe/images/*.jpg
%JAR -cmf generic-manifest.txt Raj.jar raj/*.class raj/images/*.jpg
%JAR -cmf generic-manifest.txt Khet.jar khet/*.class khet/images/*jpg
rem %JAR -cmf generic-manifest.txt Kamisado.jar kamisado/*.class kamisado/images/*.jpg
%JAR -cmf generic-manifest.txt Syzygy.jar syzygy/*.class syzygy/images/*.jpg
%JAR -cmf generic-manifest.txt Carnac.jar carnac/*.class carnac/images/*.jpg
%JAR -cmf generic-manifest.txt Takojudo.jar takojudo/*.class takojudo/images/*.jpg
%JAR -cmf generic-manifest.txt Gyges.jar gyges/*.class gyges/images/*.jpg
%JAR -cmf generic-manifest.txt Mogul.jar mogul/*.class mogul/images/*.jpg
%JAR -cmf generic-manifest.txt Rithmomachy.jar rithmomachy/*.class rithmomachy/images/*.jpg
%JAR -cmf generic-manifest.txt Shogi.jar shogi/*.class shogi/images/*.jpg
%JAR -cmf generic-manifest.txt OnedayInLondon.jar oneday/*.class oneday/images/*.jpg
%JAR -cmf generic-manifest.txt Morelli.jar morelli/*.class morelli/images/*.jpg
%JAR -cmf generic-manifest.txt Colorito.jar colorito/*.class colorito/images/*.jpg
%JAR -cmf generic-manifest.txt Euphoria.jar euphoria/*.class euphoria/images/*.jpg euphoria/sounds/*.au euphoria/images/artifacts/*.jpg euphoria/images/dice/*.jpg euphoria/images/markets/*.jpg euphoria/images/dilemmas/*.jpg euphoria/images/recruits-v12/*.jpg euphoria/images/recruits-iib/*.jpg
%JAR -cmf generic-manifest.txt Ponte.jar ponte/*.class ponte/images/*.jpg
%JAR -cmf generic-manifest.txt Tammany.jar tammany/*.class tammany/images/*.jpg
%JAR -cmf generic-manifest.txt Majorities.jar majorities/*.class majorities/images/*.jpg
%JAR -cmf generic-manifest.txt Proteus.jar proteus/*.class proteus/images/*.jpg
cp \share\projects\boardspace-java\boardspace-games\goban\shape\data\shape-data.zip goban\shape\data\shape-data.zip
%JAR -cmf generic-manifest.txt Go.jar goban/*.class goban/shape/shape/*.class goban/shape/data/*.zip goban/images/*.jpg
%JAR -cmf generic-manifest.txt Stac.jar stac/*.class stac/images/*.jpg
%JAR -cmf generic-manifest.txt Checkers.jar checkerboard/*.class checkerboard/images/*.jpg
%JAR -cmf generic-manifest.txt Morris.jar morris/*.class morris/images/*.jpg
%JAR -cmf generic-manifest.txt Sixmaking.jar sixmaking/*.class sixmaking/images/*.jpg
%JAR -cmf generic-manifest.txt Veletas.jar veletas/*.class veletas/images/*.jpg
%JAR -cmf generic-manifest.txt Modx.jar modx/*.class modx/images/*.jpg
%JAR -cmf generic-manifest.txt Lyngk.jar lyngk/*.class lyngk/images/*.jpg
%JAR -cmf generic-manifest.txt Chess.jar chess/*.class chess/images/*.jpg
%JAR -cmf generic-manifest.txt Magnet.jar magnet/*.class magnet/images/*.jpg
%JAR -cmf generic-manifest.txt Tintas.jar tintas/*.class tintas/images/*.jpg
%JAR -cmf generic-manifest.txt Barca.jar barca/*.class barca/images/*.jpg
%JAR -cmf generic-manifest.txt Twixt.jar twixt/*.class twixt/images/*.jpg
%JAR -cmf generic-manifest.txt Qe.jar qe/*.class qe/images/*.jpg
%JAR -cmf generic-manifest.txt Blooms.jar blooms/*.class blooms/images/*.jpg
%JAR -cmf generic-manifest.txt Mbrane.jar mbrane/*.class mbrane/images/*.jpg
%JAR -cmf generic-manifest.txt Viticulture.jar viticulture/images/*.au viticulture/*.class viticulture/images/automata/back.jpg  viticulture/images/automata/back-mask.jpg viticulture/images/*.jpg viticulture/images/choice/*.jpg viticulture/images/mamas-cropped/*.jpg viticulture/images/papas-cropped/*.jpg viticulture/images/specialworkers-cropped/*.jpg viticulture/images/structurecards-cropped/*.jpg viticulture/images/summer-cropped/*.jpg viticulture/images/vinecards-cropped/*.jpg viticulture/images/wineorders-cropped/*.jpg viticulture/images/wintervisitor-cropped/*.jpg
%JAR -cmf generic-manifest.txt Kulami.jar kulami/*.class kulami/images/*.jpg
%JAR -cmf generic-manifest.txt BlackDeath.jar blackdeath/*.class blackdeath/images/*.jpg
%JAR -cmf generic-manifest.txt Pushfight.jar pushfight/*.class pushfight/images/*.jpg
%JAR -cmf generic-manifest.txt Crosswords.jar crosswords/*.class crosswords/images/*.jpg
%JAR -cmf generic-manifest.txt Crosswordle.jar crosswordle/*.class crosswordle/images/*.jpg crosswordle/images/*.gz
%JAR -cmf generic-manifest.txt Dictionary.jar dictionary/*.class dictionary/words/*.gz
%JAR -cmf generic-manifest.txt Language.jar languages/translations/*.data
%JAR -cmf generic-manifest.txt Dice.jar dice/*.class dice/images/*.jpg dice/images/*.au
%JAR -cmf generic-manifest.txt Stones.jar stones/images/*.jpg
%JAR -cmf generic-manifest.txt Wyps.jar wyps/*.class wyps/*.class wyps/images/*.jpg
%JAR -cmf generic-manifest.txt Jumbulaya.jar jumbulaya/*.class jumbulaya/*.class jumbulaya/images/*.jpg
%JAR -cmf generic-manifest.txt Ygame.jar ygame/*.class ygame/images/*.jpg
%JAR -cmf generic-manifest.txt Stymie.jar stymie/*.class stymie/images/*.jpg
%JAR -cmf generic-manifest.txt Imagine.jar imagine/*.class imagine/images/*.jpg imagine/images/*.au imagine/images/deck1/*.jpg
%JAR -cmf generic-manifest.txt Mijnlieff.jar mijnlieff/*.class mijnlieff/images/*.jpg
%JAR -cmf generic-manifest.txt DayAndNight.jar dayandnight/*.class dayandnight/images/*.jpg
%JAR -cmf generic-manifest.txt KingsColor.jar kingscolor/*.class kingscolor/images/*.jpg
%JAR -cmf generic-manifest.txt Iro.jar iro/*.class iro/images/*.jpg
%JAR -cmf generic-manifest.txt Havannah.jar havannah/*.class havannah/images/*.jpg
%JAR -cmf generic-manifest.txt Tamsk.jar tamsk/*.class tamsk/images/*.jpg
%JAR -cmf generic-manifest.txt Tumbleweed.jar tweed/*.class tweed/images/*.jpg
%JAR -cmf generic-manifest.txt Ordo.jar ordo/*.class ordo/images/*.jpg
%JAR -cmf generic-manifest.txt Meridians.jar meridians/*.class meridians/images/*.jpg
%JAR -cmf generic-manifest.txt Sprint.jar sprint/*.class sprint/images/*.jpg
%JAR -cmf generic-manifest.txt Trike.jar trike/*.class trike/images/*.jpg
%JAR -cmf generic-manifest.txt Trench.jar trench/*.class trench/images/*.jpg
%JAR -i OnlineLobby.jar
%JAR -i Launcher.jar
sign.bat
