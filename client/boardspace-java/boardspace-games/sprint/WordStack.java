/* copyright notice */package sprint;

import dictionary.Entry;
import lib.OStack;

/*
 * in addition to the standard stack, this class has some features
 * to assist collecting plausible moves.
 */
public class WordStack extends OStack<Word>
{
	public Word[] newComponentArray(int sz) {
		return new Word[sz];
	}	
	public int bestScore = 0;				// best score so far
	public int leastScore = 0;				// the lowest score currently in the stack
	
	public static final double trimSize = 0.8;			// size to trim to
	public static final double threshold = 0.5;			// threshold from current best to allow addition
	public static int sizeLimit = 10;					// max entries
	int accepted = 0;
	int declined = 0;
	int prevLeastScore = 0;
	int prevBestScore = 0;
	public void clear()
	{
		super.clear();
		bestScore = 0;
		leastScore = 0;
		accepted = 0;
		declined = 0;
		prevLeastScore = 0;
		prevBestScore = 0;
	}
	public void unAccept(Word w)
	{	if(w==top())
		{
		accepted--;
		leastScore = prevLeastScore;
		bestScore = prevBestScore;
		pop();
		}
	else {
		remove(w,true);
		}
	}
	// record a candidate word if it is a plausible candidate.
	// trim the active list to the prescribed size
	public Word recordCandidate(String message,SprintCell c,String s,int direction,int score,Entry e)
	{	if(score>=bestScore*threshold && score>leastScore)
		{
		for(int lim=size()-1;lim>=0;lim--)
		{
			Word entry = elementAt(lim);
			if(entry.name.equals(e.word) && entry.seed==c) 
			{
				return(null);
			}
		}
		trimToSize();
		Word w = new Word(c,s,direction);
		push(w);
		w.entry = e;
		w.points = score;
		w.comment = message;
		prevBestScore = bestScore;
		bestScore = Math.max(score, bestScore);
		accepted++;
		return(w);
		}
		declined++;
		return(null);
	}


	public void trimToSize()
	{
		if(size()>=sizeLimit)
		{
			sort(true);
			setSize((int)(sizeLimit*trimSize));
			leastScore = top().points;
			prevLeastScore = leastScore;
		}
	}
}