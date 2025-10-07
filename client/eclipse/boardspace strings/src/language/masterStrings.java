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
package language;

import lib.BSDate;
import lib.ChatWidget;
import colorito.ColoritoConstants;
import common.GameInfo;
import breakingaway.BreakingAwayConstants;
import bugs.BugsConstants;
import cannon.CannonConstants;
import container.ContainerConstants;
import cookie.CookieConstants;
import kamisado.KamisadoConstants;
import khet.KhetConstants;
import kingscolor.KingsColorConstants;
import knockabout.KnockaboutConstants;
import kuba.KubaConstants;
import kulami.KulamiConstants;
import hive.HiveConstants;
import honey.HoneyConstants;
import imagine.ImagineConstants;
import iro.IroConstants;
import jumbulaya.JumbulayaConstants;
import lib.TimeControl;
import lyngk.LyngkConstants;
import magnet.MagnetConstants;
import lib.FileSelector;
import lib.FileSource;
import lib.GearMenu;
import lib.InternationalStrings;
import lib.SeatingChart;
import crossfire.CrossfireConstants;
import crosswordle.CrosswordleConstants;
import dayandnight.DayAndNightConstants;
import dipole.DipoleConstants;
import dvonn.DvonnConstants;
import entrapment.EntrapmentConstants;
import epaminondas.EpaminondasConstants;
import carnac.CarnacConstants;
import checkerboard.CheckerConstants;
import chess.ChessConstants;
import circle.CircleConstants;
import exxit.ExxitConstants;
import fanorona.FanoronaConstants;
import gipf.GipfConstants;
import goban.GoConstants;
import gobblet.GobConstants;
import gounki.GounkiConstants;
import gyges.GygesConstants;
import havannah.HavannahConstants;
import hex.HexConstants;
import majorities.MajoritiesConstants;
import manhattan.ManhattanConstants;
import mbrane.MbraneConstants;
import medina.MedinaConstants;
import meridians.MeridiansConstants;
import micropul.MicropulConstants;
import mijnlieff.MijnlieffConstants;
import modx.ModxConstants;
import mogul.MogulConstants;
import morelli.MorelliConstants;
import morris.MorrisConstants;
import mutton.MuttonConstants;
import euphoria.EuphoriaConstants;
import octiles.OctilesConstants;
import online.common.LobbyConstants;
import online.common.SeatingViewer;
import online.common.Session;
import online.common.TurnBasedViewer;
import online.game.AnnotationMenu;
import online.game.NumberMenu;
import online.game.Opcodes;
import ordo.OrdoConstants;
import palago.PalagoConstants;
import pendulum.PendulumConstants;
import plateau.common.PlateauConstants;
import oneday.OnedayConstants;
import rithmomachy.RithmomachyConstants;
import rpc.RpcListener;
import santorini.SantoriniConstants;
import tablut.TabConstants;
import takojudo.TakojudoConstants;
import tammany.TammanyConstants;
import tamsk.TamskConstants;
import tintas.TintasConstants;
import trax.TraxConstants;
import trench.TrenchConstants;
import triad.TriadConstants;
import trike.TrikeConstants;
import tweed.TweedConstants;
import twixt.TwixtConstants;
import tzaar.TzaarConstants;
import universe.UniverseConstants;
import veletas.VeletasConstants;
import viticulture.ViticultureConstants;
import vnc.VNCConstants;
import volcano.VolcanoConstants;
import wyps.WypsConstants;
import xiangqi.XiangqiConstants;
import ygame.YConstants;
import shogi.ShogiConstants;
import sixmaking.SixmakingConstants;
import slither.SlitherConstants;
import sprint.SprintConstants;
import crosswords.CrosswordsConstants;
import stac.StacConstants;
import stymie.StymieConstants;
import syzygy.SyzygyConstants;
import zertz.common.GameConstants;
import ponte.PonteConstants;
import proteus.ProteusConstants;
import pushfight.PushfightConstants;
import qe.QEConstants;
import quinamid.QuinamidConstants;
import qyshinsu.QyshinsuConstants;
import raj.RajConstants;
import yinsh.common.YinshConstants;
import yspahan.YspahanConstants;
import blooms.BloomsConstants;
import che.CheConstants;
import frogs.FrogConstants;
import gametimer.GameTimerConstants;
import warp6.Warp6Constants;

import static util.PasswordCollector.LoginStrings;

import arimaa.ArimaaConstants;
import barca.BarcaConstants;
import blackdeath.BlackDeathConstants;
import static online.game.Game.GameStrings;
import static online.game.Game.GameStringPairs;

/**
 * this is the master list of places to grab strings to feed into the
 * translation database.   In some legacy cases, it contains the actual
 * translations.  The preferred "modern" method is that a "putStrings()"
 * method with no parameters does the work.
 * 
 */
//
// available font families: 
//  serif (timesroman)
//  sansserif (helvetica)
//  monospaced (courier)
//
public class masterStrings extends InternationalStrings
{   public void readData() 
	{ 
	}
    static
    {	clearData();
    	setContext("VNC network");
    	put(VNCConstants.VncStrings);
    	
    	setContext("lobby");
        put("fontfamily", "sansserif");
        put(TimeControl.TimeControlStrings);
        put(TimeControl.TimeControlStringPairs);
        Session.Mode.putStrings();
        Session.PlayMode.putStrings();
        GearMenu.putStrings();
        put(FileSource.FileSourceStrings);
        put(FileSelector.FileSelectorStrings);
        put(LobbyConstants.LobbyMessages);
        put(LobbyConstants.LobbyMessagePairs);
        SeatingChart.putStrings();
        RpcListener.putStrings();
        put(Session.SessionStrings);
        put(SeatingViewer.SeatingStrings);
        BSDate.putStrings();
        TurnBasedViewer.putStrings();
        
        put(SeatingViewer.SeatingStringPairs);
        put(LoginStrings);
        put(InternationalStrings.languages)
        ;
        setContext("Game Timer");
        GameTimerConstants.putStrings();

        setContext("slither");
        SlitherConstants.putStrings();
        
        setContext("bugs");
        BugsConstants.putStrings();
        
        setContext("pendulum");
        PendulumConstants.putStrings();
        
        setContext("circle");
        CircleConstants.putStrings();

        setContext("epaminondas");
        EpaminondasConstants.putStrings();
        
        setContext("Manhattan");
        ManhattanConstants.putStrings();
        
        setContext("Trench");
        TrenchConstants.putStrings();

        setContext("Trike");
        TrikeConstants.putStrings();
        
        setContext("Sprint");
        SprintConstants.putStrings();
        
        setContext("HoneyComb");
        HoneyConstants.putStrings();
        
        setContext("Meridians");
        MeridiansConstants.putStrings();
        
        setContext("Ordo");
        OrdoConstants.putStrings();
        
        setContext("Tumbleweed");
        TweedConstants.putStrings();
        
        setContext("Tamsk");
        TamskConstants.putStrings();
        
        setContext("Havannah");
        HavannahConstants.putStrings();
        
        setContext("Iro");
        IroConstants.putStrings();
        
        setContext("KingsColor");
        KingsColorConstants.putStrings();
        
        setContext("Go");
        GoConstants.putStrings();	// also saves strings and string pairs

        setContext("chess");
        ChessConstants.putStrings();
 
        setContext("dayandnight");
        DayAndNightConstants.putStrings();
        
        setContext("jumbulaya");
        JumbulayaConstants.putStrings();
        
        setContext("mijnlieff");
        MijnlieffConstants.putStrings();

        setContext("stymie");
        put(StymieConstants.StymieStrings);
        put(StymieConstants.StymieStringPairs);
        
        setContext("pushfight");
        put(PushfightConstants.PushfightStrings);
        put(PushfightConstants.PushfightStringPairs);
        
        setContext("Mbrane");
        MbraneConstants.putStrings();
        
        
        setContext("BlackDeath");
        BlackDeathConstants.putStrings();
        
        setContext("Wyps");
        WypsConstants.putStrings();
 
        setContext("Crosswords");
        CrosswordsConstants.putStrings();
 
        setContext("Crosswordle");
        CrosswordleConstants.putStrings();

        setContext("Imagine");
        ImagineConstants.putStrings();
 
        setContext("QE");
        put(QEConstants.QEStrings);
        put(QEConstants.QEStringPairs);
        
        setContext("Tintas");
        put(TintasConstants.TintasStrings);
        put(TintasConstants.TintasStringPairs);
   
        setContext("Magnet");
        MagnetConstants.putStrings();
        
        setContext("Lyngk");
        LyngkConstants.putStrings();
        
        setContext("Modx");
        put(ModxConstants.ModxStrings);
        put(ModxConstants.ModxStringPairs);
        
        setContext("Sixmaking");
        put(SixmakingConstants.SixmakingStrings);
        put(SixmakingConstants.SixmakingStringPairs);

        
        setContext("Stac");
        put(StacConstants.StacStrings);
        put(StacConstants.StacStringPairs);
        
        setContext("Proteus");
        ProteusConstants.putStrings();
         
        setContext("Tammany Hall");
        put(TammanyConstants.TammanyStrings);
        put(TammanyConstants.TammanyStringPairs);
        
        EuphoriaConstants.putStrings();
        
        ViticultureConstants.ViticultureState.putStrings();

        setContext("Majorities");
        MajoritiesConstants.putStrings();
        
        setContext("Ponte");    
        PonteConstants.putStrings();
      
        
        setContext("Shogi");    
        put(ShogiConstants.ShogiStrings);
        put(ShogiConstants.ShogiStringPairs);
        
        setContext("Xiangqi");    
        put(XiangqiConstants.XiangqiStrings);
        put(XiangqiConstants.XiangqiStringPairs);

        setContext("Morelli");
        put(MorelliConstants.MorelliStrings);
        put(MorelliConstants.MorelliStringPairs);
        
        setContext("Colorito");
        ColoritoConstants.putStrings();
 
        setContext("Rithmomachy");
        put(RithmomachyConstants.RithmomachyStrings);
        put(RithmomachyConstants.RithmomachyStringPairs);
        
        setContext("Mogul");
        put(MogulConstants.MogulStrings);
        put(MogulConstants.MogulStringPairs);
        
        setContext("Gyges");
        GygesConstants.putStrings();
        
        setContext("Takojudo");
        put(TakojudoConstants.TakojudoStrings);
        put(TakojudoConstants.TakojudoStringPairs);
        
        setContext("Carnac");
        CarnacConstants.putStrings();
        
        setContext("Khet");
        KhetConstants.putStrings();
        
        setContext("Traboulet");
        KubaConstants.putStrings();
        
        setContext("Kulami");
        KulamiConstants.putStrings();
        
        setContext("Kamisado");
        KamisadoConstants.putStrings();
        
        setContext("Hive");
        HiveConstants.putStrings();
        
        setContext(GameInfo.PolyominoGames);
        put(UniverseConstants.PolyominoStrings);
        put(UniverseConstants.PolyominoStringPairs);
       
         
        setContext("CookieDisco");
        CookieConstants.putStrings();

        setContext("Syzygy");
        SyzygyConstants.putStrings();
        
        setContext("Volcano");
        put(VolcanoConstants.VolcanoStrings);
        put(VolcanoConstants.VolcanoStringPairs);
        
        setContext("Volo");
        put("Volo");
        put("Volo_family","Volo");
        put("Volo_variation","standard Volo");
        put("Volo-84_variation","small board Volo");
        put("Volo-84","small board Volo");
           	
        put("LeHavre_variation","standard LeHavre");
        put("LeHavre_family","LeHavre");
        put("LeHavre","LeHavre");
        
        
      
        put("Punct","Pünct");
        put("Punct_variation","standard Pünct");
        put("TBA1_variation","game to be announced");
        put("TBA2_variation","game to be announced");
        put("TBA3_variation","game to be announced");
        put("unsupported game_variation","a game not available on this platform");
        
 
     	put("Punct_family","Pünct");
        put("Spangles_family","Spangles");
        put("Truchet_family","Truchet");
        put("TumblingDown_family","Tumbling Down");
        put("TumblingDown","Tumbling Down");

        put("Tajii","Tajii");
        put("Tajii_family","Tajii");
        put("Truchet_variation","standard Truchet");
        put("TumblingDown_variation","standard 8x8 TumblingDown");
        put("Truchet","Truchet");
         // variation to game
        put("Spangles_variation","standard Spangles");
        put("Spangles","Spangles");
  
        GameInfo.putStrings();

 
        //lobby messages

        setContext("chat");
        //used by chat applet       
        put(ChatWidget.ChatStrings);
        
 
        setContext("games");
        put(GameStrings);
        put(GameStringPairs);
       	AnnotationMenu.putStrings();

        // common game strings
        // system error
        setContext("CommonGames");
        Opcodes.putStrings();
        put(online.game.commonCanvas.CanvasStrings);
        put(online.game.commonCanvas.commonStringPairs);
        put(online.game.BaseBoard.BoardState.StateStrings);
        put(lib.XFrame.XFrameMessages);
        put(lib.exCanvas.CanvasMessages);
        put(online.common.commonLobby.LobbyMessages);
        put(online.common.commonLobby.LobbyMessagePairs);
        put(online.common.lobbyCanvas.LCStrings);
        put(online.common.lobbyCanvas.LCMessagePairs);
        NumberMenu.putStrings();
        
        setContext("Color Names");
        
        put(online.game.ColorNames.ColorNames);
        
        setContext("Tzaar");
        put(TzaarConstants.TzaarStrings);
        put(TzaarConstants.TzaarStringPairs);
        
        setContext("Blooms");
        BloomsConstants.putStrings();
 
        setContext("zertz");
        GameConstants.putStrings();
        
        // loa strings
        setContext("loa");
        loa.UIC.putStrings();        
 
        // plateau strings
        setContext("plateau");
        // for plateau
        put(PlateauConstants.PlateauStrings);
        put(PlateauConstants.PlateauStringPairs);
        
        setContext("yinsh");
        put(YinshConstants.YinshStrings);
        put(YinshConstants.YinshStringPairs);

        setContext("Y");
        put(YConstants.YStrings);
        put(YConstants.YStringPairs);
        //
        // hex strings
        //
        setContext("hex");
        HexConstants.putStrings();
        
        setContext("veletas");
        put(VeletasConstants.VeletasStrings);
        put(VeletasConstants.VeletasStringPairs);
         
        // names of game variations for the lobby
        setContext("trax");
        put(TraxConstants.TraxStrings);
        put(TraxConstants.TraxStringPairs);
        
        // punct strings
        setContext("punct");
        put("Click on the placed piece to rotate it, or Click Done");
        put("Place a piece on the board, or move a piece already on the board");
        put("punctgoal","connect opposite sides of the board, or play your last piece while controlling more of the center");
        put("#1 left","#1 left");
        put("#1 center","#1 center");
     
        // gobblet strings
        setContext("gobblet");
        GobConstants.putStrings();
        
         
        // exxit strings
        setContext("exxit");
        ExxitConstants.putStrings();
        
        // new strings for tablut
		setContext("tablut");
		put(TabConstants.TablutStrings);
		put(TabConstants.TablutStringPairs);
         
        //dipole
        setContext("dipole");
        DipoleConstants.putStrings();
        
         //tumblingdown
        
        setContext("tumblingdown");
        put("Capture your opponent's tallest king stack","Capture your opponent's tallest king stack");
        put("Pick the stack to move","Pick the stack to move");

        
        //dash
        setContext("dash");
        put("Occupy 3 enemy bases","Occupy 3 enemy bases");
        put("Flip a tile, or Move Split or Merge a stack",
        		"Flip a tile, or Move Split or Merge a stack");
        put("Move, Split or Merge a stack","Move, Split or Merge a stack");
        put("Split or Merge this stack, or click Done",
        		"Split or Merge this stack, or click Done");
        put("Split this stack, or click Done",
        		"Split this stack, or click Done");
        put("Merge this stack, or click Done","Merge this stack, or click Done");
        put("Complete the split","Complete the split");
        put("Complete the merge","Complete the merge");
        put("Complete a split or merge","Complete a split or merge");
        put("Continue merging or click Done","Continue merging or click Done");
        
        setContext("fanorona");
        FanoronaConstants.putStrings();

                 
        // dvonn
        setContext("dvonn");
        DvonnConstants.putStrings();
        
        // twixt
        setContext("twixt");
        put(TwixtConstants.TwixtStrings);
        put(TwixtConstants.TwixtStringPairs);

        // qyshinsu
        setContext("qyshinsu");
        put(QyshinsuConstants.QyshinsuStrings);
        put(QyshinsuConstants.QyshinsuStringPairs);
      
        // knockabout
        setContext("knockabout");
        put(KnockaboutConstants.KnockaboutStrings);
        put(KnockaboutConstants.KnockaboutStringPairs);
       		
        
        // gipf
        setContext("gipf");
        GipfConstants.putStrings();
        
        // palago
        setContext("palago");
        PalagoConstants.putStrings();
        
        // santorini
        setContext("santorini");
        put(SantoriniConstants.SantoriniStrings);
        put(SantoriniConstants.SantoriniStringPairs);     
          
        // "spangles"
        setContext("spangles");
        put("form a larger triangle with your color at the tips");

         
        setContext("micropul");
        MicropulConstants.putStrings();


        //Medina
        setContext("medina");
 	   	MedinaConstants.putStrings();

        
        // cannon
        setContext("cannon");
        CannonConstants.putStrings();
        
        // warp 6
        setContext("warp6");
        put("Warp6_family","Warp 6");
        put(Warp6Constants.Warp6Strings);
        put(Warp6Constants.Warp6StringPairs);

         
        // Triad
        setContext("triad");
        put(TriadConstants.TriadStrings);
        put(TriadConstants.TriadStringPairs);
         
        //Che
        setContext("che");
        CheConstants.putStrings();
        
        
        // mutton
        setContext("mutton");
        put(MuttonConstants.MuttonStrings);
        put(MuttonConstants.MuttonStringPairs);
        
        // octiles
        setContext("octiles");
        OctilesConstants.putStrings();
        
        // army of frogs
        setContext("frogs");
        FrogConstants.putStrings();
       
        // checkers
        setContext("checkers");
        CheckerConstants.putStrings();
         
        // 9 men morris
        setContext("9 men morris");
        put(MorrisConstants.MorrisStrings);
        put(MorrisConstants.MorrisStringPairs);
  
        
        // breaking away
        setContext("barca");
        BarcaConstants.putStrings();

    
       // breaking away
        setContext("breakingaway");
        BreakingAwayConstants.putStrings();
     
       
        // container
        setContext("container");
        ContainerConstants.putStrings();
        
        // arimaa
        setContext("arimaa");
        ArimaaConstants.putStrings();
                    
        
        // crossfire
        setContext("crossfire");
        CrossfireConstants.putStrings();
        
        // entrapment
        setContext("entrapment");
        EntrapmentConstants.putStrings();
  	   
 	   setContext("quinamid");
 	   put(QuinamidConstants.QuinamidStrings);
 	   put(QuinamidConstants.QuinamidStringPairs);
    	
    	setContext("yspahan");
    	YspahanConstants.putStrings();
    	 		
		setContext("Gounki");
		GounkiConstants.putStrings();
		
		setContext("Volo");
		put("connect all your birds into one flock");
		put("Place a bird on an empty cell");
		put("Designate a zone to clear");
        put("Place a bird on an empty cell, or designate a bird to fly");
        put("Click on the end of the line to move");
        put("Move the line of birds");
		
        setContext("Raj");
        put(RajConstants.RajStrings);
        put(RajConstants.RajStringPairs);
        
         
        setContext("TicTacNine");
        
        put("TicTacNine");
        put("TicTacNine_variation","experimental TicTacNine board");
        put("TicTacNine_family","TicTacNine");
        put("TicTacNine Games");

        
        setContext("OnedayInLondon");
        put(OnedayConstants.OnedayStrings);
        put(OnedayConstants.OnedayStringPairs);
		setContext(null);
     }
 
 
}