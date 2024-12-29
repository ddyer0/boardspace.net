/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package common;

import java.util.Hashtable;

public class NamedClasses {
    //
    // class.forName will work only for classes that are explicitly
    // references, no matter that they were built
    //
    public static Hashtable<String,Class<?>>classes = new Hashtable<String,Class<?>>();
    static {
    	// if you see an error on this line, just comment it out.  "Salt.java" is part
    	// of the defenses of the live site boardspace against accidental damage from
    	// experiments using sources cloned from github.
    	classes.put("common.Salt",common.Salt.class);
    	
    	classes.put("vnc.AuxViewer",vnc.AuxViewer.class);
    	classes.put("online.common.SeatingViewer",online.common.SeatingViewer.class);
    	classes.put("online.search.UCTTreeViewer", online.search.UCTTreeViewer.class);
    	classes.put("lib.commonPanel", lib.commonPanel.class);
    	classes.put("online.common.commonLobby",online.common.commonLobby.class);
    	classes.put("online.game.Game",online.game.Game.class);
    	classes.put("online.common.lobbyCanvas",online.common.lobbyCanvas.class);
     	classes.put("online.common.LobbyMapViewer",online.common.LobbyMapViewer.class);
    	classes.put("online.language.englishStrings",online.language.englishStrings.class);
    	classes.put("online.language.frenchStrings",online.language.frenchStrings.class);
    	classes.put("online.language.swedishStrings",online.language.swedishStrings.class);
    	classes.put("online.language.chineseStrings",online.language.chineseStrings.class);
    	classes.put("online.language.japaneseStrings",online.language.japaneseStrings.class);
    	classes.put("online.language.portugueseStrings",online.language.portugueseStrings.class);
    	classes.put("online.language.frenchStrings",online.language.frenchStrings.class);
    	classes.put("online.language.spanishStrings",online.language.spanishStrings.class);
    	classes.put("online.language.catalaStrings",online.language.catalaStrings.class);
    	classes.put("online.language.russianStrings",online.language.russianStrings.class);
    	classes.put("online.language.norwegianStrings",online.language.norwegianStrings.class);
    	classes.put("online.language.polishStrings",online.language.polishStrings.class);
    	classes.put("online.language.esperantoStrings",online.language.esperantoStrings.class);
    	classes.put("online.language.dutchStrings",online.language.dutchStrings.class);
    	classes.put("online.language.romanianStrings",online.language.romanianStrings.class);
    	classes.put("online.language.germanStrings",online.language.germanStrings.class);
    	classes.put("online.language.czechStrings",online.language.czechStrings.class);

    
    	// game classes
    	classes.put("circle.CircleViewer",circle.CircleViewer.class);
    	classes.put("epaminondas.EpaminondasViewer",epaminondas.EpaminondasViewer.class);
    	classes.put("manhattan.ManhattanViewer",manhattan.ManhattanViewer.class);
    	classes.put("gametimer.GameTimerViewer",gametimer.GameTimerViewer.class);
    	classes.put("honey.HoneyViewer",honey.HoneyViewer.class);
    	classes.put("trench.TrenchViewer",trench.TrenchViewer.class);
    	classes.put("trike.TrikeViewer",trike.TrikeViewer.class);
    	classes.put("sprint.SprintViewer",sprint.SprintViewer.class);
    	classes.put("meridians.MeridiansViewer",meridians.MeridiansViewer.class);
    	classes.put("ordo.OrdoViewer",ordo.OrdoViewer.class);
    	classes.put("tweed.TweedViewer",tweed.TweedViewer.class);
    	classes.put("crosswordle.CrosswordleViewer",crosswordle.CrosswordleViewer.class);
    	classes.put("tamsk.TamskViewer",tamsk.TamskViewer.class);
    	classes.put("iro.IroViewer",iro.IroViewer.class);
    	classes.put("havannah.HavannahViewer",havannah.HavannahViewer.class);
    	classes.put("dayandnight.DayAndNightViewer",dayandnight.DayAndNightViewer.class);
    	classes.put("jumbulaya.JumbulayaViewer",jumbulaya.JumbulayaViewer.class);
    	classes.put("mijnlieff.MijnlieffViewer",mijnlieff.MijnlieffViewer.class);
    	classes.put("imagine.ImagineViewer",imagine.ImagineViewer.class);
    	classes.put("wyps.WypsViewer",wyps.WypsViewer.class);
    	classes.put("barca.BarcaViewer",barca.BarcaViewer.class);
    	classes.put("blackdeath.BlackDeathViewer",blackdeath.BlackDeathViewer.class);
     	classes.put("blooms.BloomsViewer",blooms.BloomsViewer.class);
    	classes.put("checkerboard.CheckerGameViewer",checkerboard.CheckerGameViewer.class);
    	classes.put("chess.ChessViewer",chess.ChessViewer.class);
    	// kamisado removed by request
    	// classes.put("kamisado.KamisadoViewer",kamisado.KamisadoViewer.class);

    	classes.put("fanorona.FanoronaGameViewer",fanorona.FanoronaGameViewer.class);
    	classes.put("frogs.FrogViewer",frogs.FrogViewer.class);
    	classes.put("colorito.ColoritoViewer",colorito.ColoritoViewer.class);
    	classes.put("goban.GoViewer",goban.GoViewer.class);
       	classes.put("cannon.CannonViewer",cannon.CannonViewer.class);
       	classes.put("container.ContainerViewer",container.ContainerViewer.class);
    	classes.put("crosswords.CrosswordsViewer",crosswords.CrosswordsViewer.class);
       	classes.put("euphoria.EuphoriaViewer",euphoria.EuphoriaViewer.class);
       	classes.put("dvonn.DvonnViewer",dvonn.DvonnViewer.class);
    	classes.put("arimaa.ArimaaViewer",arimaa.ArimaaViewer.class);
    	classes.put("crossfire.CrossfireViewer",crossfire.CrossfireViewer.class);
    	classes.put("che.CheViewer",che.CheViewer.class);
    	classes.put("entrapment.EntrapmentViewer",entrapment.EntrapmentViewer.class);
    	classes.put("knockabout.KnockaboutViewer",knockabout.KnockaboutViewer.class);
    	classes.put("kuba.KubaViewer",kuba.KubaViewer.class);	
    	classes.put("khet.KhetViewer",khet.KhetViewer.class);
       	classes.put("loa.LoaViewer",loa.LoaViewer.class);
       	classes.put("hex.HexGameViewer",hex.HexGameViewer.class);
     	classes.put("gipf.GipfViewer",gipf.GipfViewer.class);
       	classes.put("cookie.CookieViewer",cookie.CookieViewer.class);
    	classes.put("gobblet.GobGameViewer",gobblet.GobGameViewer.class);
    	classes.put("hive.HiveGameViewer",hive.HiveGameViewer.class);
    	classes.put("carnac.CarnacViewer",carnac.CarnacViewer.class);
       	classes.put("yspahan.YspahanViewer",yspahan.YspahanViewer.class);
    	classes.put("breakingaway.BreakingAwayViewer",breakingaway.BreakingAwayViewer.class);
    	classes.put("gounki.GounkiViewer",gounki.GounkiViewer.class);
    	classes.put("gyges.GygesViewer",gyges.GygesViewer.class);
    	classes.put("dipole.DipoleGameViewer",dipole.DipoleGameViewer.class);
    	classes.put("exxit.ExxitGameViewer",exxit.ExxitGameViewer.class);
     	classes.put("kulami.KulamiViewer",kulami.KulamiViewer.class);    	
     	classes.put("kingscolor.KingsColorViewer",kingscolor.KingsColorViewer.class);    	
     	classes.put("lyngk.LyngkViewer",lyngk.LyngkViewer.class);
     	classes.put("magnet.MagnetViewer",magnet.MagnetViewer.class);
    	classes.put("majorities.MajoritiesViewer",majorities.MajoritiesViewer.class);
    	classes.put("mbrane.MbraneViewer",mbrane.MbraneViewer.class);
    	classes.put("mogul.MogulViewer",mogul.MogulViewer.class);
    	// removed Oct 10 2021
    	//classes.put("medina.MedinaViewer",medina.MedinaViewer.class);
    	//
    	classes.put("modx.ModxViewer",modx.ModxViewer.class);
    	classes.put("micropul.MicropulViewer",micropul.MicropulViewer.class);
    	classes.put("morelli.MorelliViewer",morelli.MorelliViewer.class);
    	classes.put("morris.MorrisViewer",morris.MorrisViewer.class);
    	classes.put("mutton.MuttonGameViewer",mutton.MuttonGameViewer.class);
    	classes.put("oneday.OnedayViewer",oneday.OnedayViewer.class);
    	classes.put("octiles.OctilesViewer",octiles.OctilesViewer.class);
     	classes.put("palago.PalagoViewer",palago.PalagoViewer.class);
     	classes.put("plateau.common.PlateauGameViewer",plateau.common.PlateauGameViewer.class);
     	classes.put("ponte.PonteViewer",ponte.PonteViewer.class);
     	classes.put("proteus.ProteusViewer",proteus.ProteusViewer.class);
     	classes.put("punct.PunctGameViewer",punct.PunctGameViewer.class);
    	classes.put("pushfight.PushfightViewer",pushfight.PushfightViewer.class);
       	classes.put("qe.QEViewer", qe.QEViewer.class);
     	classes.put("quinamid.QuinamidViewer",quinamid.QuinamidViewer.class);
     	classes.put("qyshinsu.QyshinsuViewer",qyshinsu.QyshinsuViewer.class);
     	classes.put("raj.RajViewer",raj.RajViewer.class);
     	classes.put("rithmomachy.RithmomachyViewer",rithmomachy.RithmomachyViewer.class);
     	classes.put("santorini.SantoriniViewer",santorini.SantoriniViewer.class);
     	classes.put("shogi.ShogiViewer",shogi.ShogiViewer.class);
     	classes.put("sixmaking.SixmakingViewer",sixmaking.SixmakingViewer.class);
     	classes.put("spangles.SpanglesViewer",spangles.SpanglesViewer.class);
     	classes.put("stac.StacViewer",stac.StacViewer.class);
     	classes.put("stymie.StymieViewer", stymie.StymieViewer.class);
     	classes.put("syzygy.SyzygyViewer",syzygy.SyzygyViewer.class);
     	classes.put("takojudo.TakojudoViewer",takojudo.TakojudoViewer.class);
     	classes.put("tablut.TabGameViewer",tablut.TabGameViewer.class);
     	classes.put("tammany.TammanyViewer",tammany.TammanyViewer.class);
     	classes.put("tintas.TintasViewer",tintas.TintasViewer.class);
     	classes.put("trax.TraxGameViewer",trax.TraxGameViewer.class); 
     	classes.put("triad.TriadViewer",triad.TriadViewer.class);
     	classes.put("truchet.TruGameViewer",truchet.TruGameViewer.class);
    	classes.put("tumble.TumbleGameViewer",tumble.TumbleGameViewer.class);
    	classes.put("twixt.TwixtViewer",twixt.TwixtViewer.class);
     	classes.put("tzaar.TzaarViewer",tzaar.TzaarViewer.class);
     	classes.put("universe.UniverseViewer",universe.UniverseViewer.class);
     	classes.put("veletas.VeletasViewer",veletas.VeletasViewer.class);
     	classes.put("viticulture.ViticultureViewer",viticulture.ViticultureViewer.class);
     	
     	classes.put("volcano.VolcanoGameViewer",volcano.VolcanoGameViewer.class);
     	classes.put("volo.VoloViewer",volo.VoloViewer.class);
      	classes.put("warp6.Warp6Viewer",warp6.Warp6Viewer.class);
      	classes.put("xiangqi.XiangqiViewer",xiangqi.XiangqiViewer.class);
      	classes.put("ygame.YViewer", ygame.YViewer.class);
     	classes.put("yinsh.common.YinshGameViewer",yinsh.common.YinshGameViewer.class);
     	classes.put("zertz.common.ZertzGameViewer",zertz.common.ZertzGameViewer.class);
     	
    	// below here not converted yet
    	
    	//classes.put("tajii.TajiiViewer",tajii.TajiiViewer.class);
    	//classes.put("lehavre.LehavreViewer",lehavre.LehavreViewer.class);
     	//classes.put("tictacnine.TicTacNineViewer",tictacnine.TicTacNineViewer.class);
 
    	}
}