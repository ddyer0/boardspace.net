/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;
import java.awt.Font;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;


/**
 * Main controlling class.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
final class Control
{
   private static boolean showAreaLines = true;

   private static GuiMainWindow mainWindow;

   private static Match match = null;

   private static final int DEFAULT_FONT_SZ = 13;
   /**
    * Private empty cons'tor.
    */
   private Control()
   {
   }

   /**
    * Load Match from file.
    */
   public static void loadGame()
   {
      LoadSave.loadGame(match);
   }

   /**
    * Save Match to file.
    */
   public static void saveGame()
   {
      LoadSave.saveGame(match);
   }

   /**
    * Start new Match.
    */
   public static void newGame()
   {
      // open dialog
      MatchData newMatch = NewDialog.showNewDialog(mainWindow, match.getMatchData(), false);
      if (newMatch != null)
      {
         match.prepareNewMatch(newMatch, true);
      }
   }

   /**
    * A Match is changed.
    */
   public static void changeGame()
   {
      // open dialog
      MatchData changedMatch = NewDialog.showNewDialog(mainWindow, match.getMatchData(), true);
      if (changedMatch != null)
      {
         match.updateMatchData(changedMatch);
         if (match.computerVsComputer())
         {
            match.getMatchData().setDefaultNames(true);
         }
         match.evaluateAndUpdateGui();
      }
   }

   /**
    * Change playing strenth of computer.
    */
   public static void changeLevel()
   {
      StrengthDialog.showStrengthDialog(mainWindow);
   }

   /**
    * Set SansSerif as default font.
    */
   private static void setNiceFont()
   {

      Font f = new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SZ);

      java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
      while (keys.hasMoreElements())
      {
         Object key = keys.nextElement();
         Object value = UIManager.get(key);
         if (value instanceof FontUIResource)
         {
            UIManager.put(key, f);
         }
      }

   }
   
   /**
    *
    * Main-Method.
    * 
    * @param args input parameters - optional 1st parameter sets locale
    */
   public static void main(final String[] args)
   {
      if (args.length > 0)
      {
         Locale.setDefault(new Locale(args[0]));
      }
      System.out.println(Messages.getString("Control.Welcome")); //$NON-NLS-1$

      //set a fixed look&Feel
      try
      {
         UIManager.setLookAndFeel(
               UIManager.getCrossPlatformLookAndFeelClassName());         
      } catch (Exception e)
      {
         e.printStackTrace();
      }
      //set a nicer font
      setNiceFont();

      CheckPattern.getInstance().loadPattern();

      Zobrist.getInstance().initialize();
      
      match = new Match();
      MatchData matchData = new MatchData();
      matchData.loadPreferences();

      /* Program never starts with computer vs. computer */
      if (!matchData.mdYhuman && !matchData.mdXhuman)
      {
         matchData.mdYhuman = true;
         matchData.mdPlayerY = Messages.getString("MatchData.Player");
      }

      match.prepareNewMatch(matchData, false);
      mainWindow = new GuiMainWindow(match);
      // mainWindow.show();
      mainWindow.setVisible(true);

      match.computeMove();
   }
   /**
    * Check if arealines are to be shown.
    * @return Returns the showAreaLines.
    */
   public static boolean isShowAreaLines()
   {
      return showAreaLines;
   }
   /**
    * Set if arealines should be shown.
    * @param inShowAreaLines The showAreaLines to set.
    */
   public static void setShowAreaLines(final boolean inShowAreaLines)
   {
      showAreaLines = inShowAreaLines;
   }

   /**
    * Get MainWindow.
    * @return GuiMainWindow
    */
   public static GuiMainWindow getMainWindow()
   {
      return mainWindow;
   }
}