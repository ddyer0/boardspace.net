/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.util.prefs.Preferences;

/**
 * Store all the data of a match, but not the moves.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class MatchData implements Cloneable
{
   public static final String PREFS_PATH = "/twixt/net/schwagereit/t1j";

   protected String mdPlayerY = Messages.getString("MatchData.Player") + " 1"; //$NON-NLS-1$
   protected String mdPlayerX = Messages.getString("MatchData.Player") + " 2"; //$NON-NLS-1$
   protected int mdYsize = Board.DEFAULTDIM;
   protected int mdXsize = Board.DEFAULTDIM;
   protected boolean mdYhuman = true;
   protected boolean mdXhuman = true;
   protected boolean mdYstarts = true;
   protected final static boolean mdLetterDir = true;
   protected boolean mdPieRule = true;
   protected boolean mdGameOver = false;


   /**
    * Cons'tor.
    */
   public MatchData()
   {
   }

   /** (non-Javadoc)
    * @see java.lang.Object#clone()
    */
   public Object clone()
   {
      try
      {
         return super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return null;
      }
   }

   /**
    * Load preferences.
    */
   public void loadPreferences()
   {
      Preferences userPrefs = Preferences.userRoot().node(PREFS_PATH);
      mdPlayerY = userPrefs.get("NameY", Messages.getString("MatchData.Player") + " 1");
      mdPlayerX = userPrefs.get("NameX", Messages.getString("MatchData.Player") + " 2");
      mdYsize = userPrefs.getInt("SizeY", Board.DEFAULTDIM);
      mdXsize = userPrefs.getInt("SizeX", Board.DEFAULTDIM);
      mdYhuman = userPrefs.getBoolean("HumanY", true);
      mdXhuman = userPrefs.getBoolean("HumanX", true);
      mdYstarts = userPrefs.getBoolean("StartsY", true);
      mdPieRule = userPrefs.getBoolean("PieRule", true);
      correct();
   }

   /**
    * Save preferences.
    */
   public void savePreferences()
   {
      Preferences userPrefs = Preferences.userRoot().node(PREFS_PATH);
      userPrefs.put("NameY", mdPlayerY);
      userPrefs.put("NameX", mdPlayerX);
      userPrefs.putInt("SizeY", mdYsize);
      userPrefs.putInt("SizeX", mdXsize);
      userPrefs.putBoolean("HumanY", mdYhuman);
      userPrefs.putBoolean("HumanX", mdXhuman);
      userPrefs.putBoolean("StartsY", mdYstarts);
      userPrefs.putBoolean("PieRule", mdPieRule);
   }

   /**
    * Correct any illegal data.
    */
   public void correct()
   {
      if (mdYsize < Board.MINDIM)
      {
         mdYsize = Board.MINDIM;
      }
      if (mdXsize < Board.MINDIM)
      {
         mdXsize = Board.MINDIM;
      }
      if (mdYsize > Board.MAXDIM)
      {
         mdYsize = Board.MAXDIM;
      }
      if (mdXsize > Board.MAXDIM)
      {
         mdXsize = Board.MAXDIM;
      }
      if ("".equals(mdPlayerY) || "".equals(mdPlayerX))
         setDefaultNames(false);
   }
   /**
    * Set nice default names for the two players if they are computer players.
    * @param always always change names
    */
   public void setDefaultNames(boolean always)
   {
      if (always)
      {
         mdPlayerY = "";
         mdPlayerX = "";
      }
      if (mdPlayerY == null || mdPlayerY.equals("")) //$NON-NLS-1$
      {
         mdPlayerY = Messages.getString("MatchData.Player") + " 1"; //$NON-NLS-1$
      }
      if (mdPlayerX == null || mdPlayerX.equals("")) //$NON-NLS-1$
      {
         mdPlayerX = Messages.getString("MatchData.Player") + " 2"; //$NON-NLS-1$
      }
      if (!mdYhuman && !mdXhuman)
      { // two Computer players
         mdPlayerY = Messages.getString("MatchData.Computer")  + " 1"; //$NON-NLS-1$ 
         mdPlayerX = Messages.getString("MatchData.Computer")  + " 2"; //$NON-NLS-1$
      }
      else if (!mdYhuman)
      { // one Computer player
         mdPlayerY = Messages.getString("MatchData.Computer"); //$NON-NLS-1$
      }
      else if (!mdXhuman)
      { // one Computer player
         mdPlayerX = Messages.getString("MatchData.Computer"); //$NON-NLS-1$
      }
   }
}
