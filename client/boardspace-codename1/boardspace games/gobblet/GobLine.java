package gobblet;

public class GobLine {
	public int myBigCups=0;
	public int myCups=0;
	public int hisBigCups=0;
	public int hisCups=0;
//	int finalv = dumbot ? mycups : mycups-hiscups-((hisbigcups>0)?(mycups-mybigcups):0);
	public int lineScore()
	{	return((hisBigCups>0)?((myBigCups>0)?1:0):myCups*myCups);
	}
}
