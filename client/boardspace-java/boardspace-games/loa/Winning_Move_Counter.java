/* copyright notice */package loa;

import lib.G;


class Winning_Move_Counter implements Move_Mapper
{
    private Object locker;
    private int counter;

    public Object Lock_Counter(Object who)
    {
        if (locker != null)
        {
        	throw G.Error("Counter already locked by " + locker +
                " attempting new lock by " + who);
        }
        else
        {
            locker = who;
            counter = 0;
        }

        return (locker);
    }

    public int Unlock_Counter(Object who)
    {
        if (who != locker)
        {
        	throw G.Error("attempt to Unlock by " + who + " was locked by " + locker);
        }

        {
            int c = counter;
            locker = null;

            return (c);
        }
    }

    public void Increment_Counter(int n)
    {
        counter += n;
    }

    public void Move_Map(Line_Info line, Move_Spec mv)
    {
        if (line.board.winning_move_p(mv))
        {
            Increment_Counter(1);
        }
    }
}
