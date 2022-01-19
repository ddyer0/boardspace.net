/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * Load and Save files of T1-format.
 * This is a 'static' class
 *
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class LoadSave
{
   /** constant for newline. */
   private static final String LINESEP = System.getProperty("line.separator");

   private static BufferedReader reader;
   private static BufferedWriter writer;
   private static String filename = "saved.T1";
   private static File directory = new File(System.getProperty("user.dir"));


   /**
    * private constructor.
    */
   private LoadSave()
   {
   }

// --Commented out by Inspection START (10.09.05 18:31):
//   /**
//    * Process one of the read lines.
//    *
//    * @param line Line read
//    */
//   static void processLine(final String line)
//   {
//      System.out.println(line);
//   }
// --Commented out by Inspection STOP (10.09.05 18:31)

   /**
    * Read a line from the file if possible.
    * @return String or null, if nothing found
    */
   private static String getLine()
   {
      String line;
      try
      {
         while ((line = reader.readLine()) != null)
         {
            int pos = line.indexOf("#");
            if (pos >= 0)
            {
               line = line.substring(0, pos);
            }
            line = line.trim();
            if (line.length() > 0)
            {
               return line;
            }
         }
      }
      catch (IOException e)
      {
         System.out.println("IO-Error reading file '" + filename + "'.");
      }
      do
      {

      } while (false);
      return null;
   }

   /**
    * Read Data of saved game.
    * @param match The match to fill
    */
   private static void getGameData(final Match match)
   {
      MatchData loadData = new MatchData();

      try
      {
         getLine(); // version of file-format (ignored)
         loadData.mdPlayerY = getLine(); // Name 1
         loadData.mdPlayerX = getLine(); // Name 2

         loadData.mdYsize = Integer.parseInt(getLine());
         loadData.mdXsize = Integer.parseInt(getLine());

         loadData.mdYhuman    = (getLine().charAt(0) == 'H');
         loadData.mdXhuman    = (getLine().charAt(0) == 'H');

         loadData.mdYstarts   = (getLine().charAt(0) == '1');

         getLine(); // mdLetterDir (ignored)
         loadData.mdPieRule   = (getLine().charAt(0) == 'Y');
         loadData.mdGameOver  = (getLine().charAt(0) == 'Y');

         //take loaded data as new data
         loadData.correct();
         loadData.savePreferences();
         match.prepareNewMatch(loadData, false);
         // the meta-data was loaded, now lets start with the moves
         String line;
         while ((line = getLine()) != null)
         {
            match.setlastMove(line.charAt(0) - 'A', Integer.parseInt(line.substring(1)) - 1);
         }
         match.evaluateAndUpdateGui();


      } catch (Exception e)
      {
         System.out.println("File '" + filename + "' has wrong format.");
      }
   }

   /**
    * Write Data to file.
    * @param match The match to write
    */
   private static void writeGameData(final Match match)
   {
      try
      {
         MatchData saveData = match.getMatchData();

         writer.write("# File created by T1j" + LINESEP);
         writer.write(
               "# T1j is a program to play TwixT (mail@johannes-schwagereit.de)" + LINESEP);
         writer.write("1 # version of file-format" + LINESEP);

         writer.write(saveData.mdPlayerY + "# Name of Player 1" + LINESEP);
         writer.write(saveData.mdPlayerX + "# Name of Player 2" + LINESEP);
         writer.write(saveData.mdYsize + "# y-size of board" + LINESEP);
         writer.write(saveData.mdXsize + "# x-size of board" + LINESEP);
         writer.write(
               (saveData.mdYhuman ? "H" : "C") + "# player 1 human or computer" + LINESEP);
         writer.write(
               (saveData.mdXhuman ? "H" : "C") + "# player 2 human or computer" + LINESEP);
         writer.write((saveData.mdYstarts ? "1" : "2")
               + "# starting player (1 plays top-down)" + LINESEP);
         writer.write("V# Direction of letters" + LINESEP);
         writer.write((saveData.mdPieRule ? "Y" : "N") + "# pierule?" + LINESEP);
         writer.write((saveData.mdGameOver ? "Y" : "N") + "# game already over?" + LINESEP);

         //write moves
         int x, y;
         for (int i = 1; i <= match.getMoveNr(); i++)
         {
            x = match.getMoveX(i);
            y = match.getMoveY(i);
            writer.write(GuiBoard.getHoleName(x, y, false) + LINESEP);
         }

      } catch (Exception e)
      {
         System.out.println("Problem while writing file '" + filename + "'.");
      }
   }


   /**
    * Load a saved game.
    *
    * @param match The match to fill
    */
   static void loadGame(final Match match)
   {
      boolean status = true;
      String message = "";
      try
      {
         JFileChooser chooser = new JFileChooser();
         chooser.addChoosableFileFilter(new FileFilter()
         {
            public boolean accept(final File f)
            {
               return f.isDirectory() || f.getName().toLowerCase().endsWith(".t1");
            }

            public String getDescription()
            {
               return "T1-Files";
            }
         });
         chooser.setCurrentDirectory(directory);
         chooser.setMultiSelectionEnabled(false);
         if (chooser.showOpenDialog(Control.getMainWindow()) == JFileChooser.APPROVE_OPTION)
         {
            directory = chooser.getCurrentDirectory();
            filename = chooser.getSelectedFile().toString();
         }
         else
         {
            return; // Cancel
         }

         reader = new BufferedReader(new FileReader(filename));

         getGameData(match);

         reader.close();
      } catch (FileNotFoundException e)
      {
         message = "File '" + filename + "' not found.";
         status = false;
      }
      catch (IOException e)
      {
         message = "IO-Error reading file '" + filename + "'.";
         status = false;
      }
      finally
      {
         reader = null;
      }
      if (!status)
      {
         JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
      }
   }

   /**
    * Save a game.
    *
    * @param match The match to save
    */
   static void saveGame(final Match match)
   {
      boolean status = true;
      String message = "";
      try
      {
         JFileChooser chooser = new JFileChooser();
         chooser.addChoosableFileFilter(new FileFilter()
         {
            public boolean accept(final File f)
            {
               return f.isDirectory() || f.getName().toLowerCase().endsWith(".t1");
            }

            public String getDescription()
            {
               return "T1-Files";
            }
         });
         chooser.setCurrentDirectory(directory);
         chooser.setSelectedFile(new File(filename));
         chooser.setMultiSelectionEnabled(false);
         if (chooser.showSaveDialog(Control.getMainWindow()) == JFileChooser.APPROVE_OPTION)
         {
            directory = chooser.getCurrentDirectory();
            filename = chooser.getSelectedFile().toString();
            //if FILENAME has no type, T1 is added
            if (filename.indexOf('.') < 0)
            {
               filename += ".T1";
            }
         }
         else
         {
            return; // Cancel
         }

         writer = new BufferedWriter(new FileWriter(filename));
         
         writeGameData(match);
                  
         writer.close();
      } catch (IOException e)
      {
         message = "IO-Error writing file '" + filename + "'.";
         status = false;
      }
      finally
      {
         writer = null;
      }
      if (!status)
      {
         JOptionPane.showMessageDialog(Control.getMainWindow(), message, "Error",
               JOptionPane.ERROR_MESSAGE);
      }
   }

}
