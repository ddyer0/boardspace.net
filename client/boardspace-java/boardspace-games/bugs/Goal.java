package bugs;

import java.awt.Font;

import lib.CompareTo;
import lib.G;
import lib.Graphics;
import lib.Image;

public interface Goal extends CompareTo<Goal> {

	public int getUid();
	public void drawExtendedChip(Graphics gc, Font font, int xp, int yp, int w, int h,boolean x2);
	public String getHelpText();
	public Image getIllustrationImage();
	public double pointValue(BugsBoard b,BugCard bug);
	public String getCommonName();
	public String legend(boolean b);
	public boolean matches(BugsBoard board,BugCard bug, boolean b);
	public default int compareTo(Goal other)
	{
		return G.signum(getUid()-other.getUid());
	}

}
