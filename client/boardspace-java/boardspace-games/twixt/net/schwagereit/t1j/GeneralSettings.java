/**
 *
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
package twixt.net.schwagereit.t1j;

import java.util.prefs.Preferences;

/**
 * Store and save general parameter, which are not stored as Matchdata.
 * This is a singleton.
 */
public class GeneralSettings
{
   private static final GeneralSettings ourInstance = new GeneralSettings();

   boolean mdFixedPly = true;

   private static final int DEFAULT_PLY = 5;

   int mdPly = DEFAULT_PLY; // searchdepth
   int mdTime = DEFAULT_PLY; // alternatively: time per computer-move
   int mdColorscheme = 0; // selected colorschme

   /**
    * Return the GeneralSettings-Object.
    *
    * @return GeneralSettings-Object
    */
   public static GeneralSettings getInstance()
   {
      return ourInstance;
   }

   /**
    * Constructor - no external instance.
    */
   private GeneralSettings()
   {
      Preferences userPrefs = Preferences.userRoot().node(MatchData.PREFS_PATH);
      mdFixedPly = userPrefs.getBoolean("FixedPly", true);
      mdPly = userPrefs.getInt("Ply", DEFAULT_PLY);
      mdTime = userPrefs.getInt("Time", DEFAULT_PLY);
      mdColorscheme = userPrefs.getInt("Colorscheme", 0);
      correct();
   }

   /**
    * Save preferences.
    */
   public void savePreferences()
   {
      Preferences userPrefs = Preferences.userRoot().node(MatchData.PREFS_PATH);
      userPrefs.putBoolean("FixedPly", mdFixedPly);
      userPrefs.putInt("Ply", mdPly);
      userPrefs.putInt("Time", mdTime);
      userPrefs.putInt("Colorscheme", mdColorscheme);
   }

   /**
    * Correct any illegal data.
    */
   private void correct()
   {
      if (mdPly < 1)
      {
         mdPly = 5;
      }
      if (mdTime < 1)
      {
         mdTime = 5;
      }
      if (mdColorscheme > GuiMainWindow.SCHEME_NUMBER)
      {
         mdColorscheme = 0;
      }
   }




}
