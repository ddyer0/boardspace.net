package loa;

import online.game.commonMove;


public class Stone_Spec extends commonMove
{
    int x;
    int y;
    Stone_Type color;

    // initializers 
    Stone_Spec()
    {
        /* return an uninitialized spec */
    }

    Stone_Spec(int cx, int yy, Stone_Type col)
    {
        this.x = cx;
        this.y = yy;
        this.color = col;
    }

    public int fromX()
    {
        return (x);
    }

    public int fromY()
    {
        return (y);
    }

    // implement cloneable 
    Stone_Spec Copy_Slots(Stone_Spec to)
    {
        super.Copy_Slots(to);
        to.x = x;
        to.y = y;
        to.color = color;

        return (to);
    }

    public commonMove Copy(commonMove to)
    {
        if (to == null)
        {
            to = new Stone_Spec();
        }

        Copy_Slots((Stone_Spec) to);

        return (to);
    }

    boolean Equal(Stone_Spec other)
    {
        return ((x == other.x) && (y == other.y) && (color == other.color));
    }

    public boolean Same_Move_P(commonMove other)
    {
        return (Equal((Stone_Spec) other));
    }

    public String shortMoveString()
    {
        return (null);
    }

    public String moveString()
    {
        return (null);
    }
}