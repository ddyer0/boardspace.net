package loa;

import java.util.*;


class Move_Finder implements Move_Mapper
{
    Stone color;
    int fromx;
    int fromy;
    Vector<Move_Spec> v = new Vector<Move_Spec>();

    Move_Finder(Stone s, int x, int y)
    {
        this.color = s;
        this.fromx = x;
        this.fromy = y;
    }

    public void Move_Map(Line_Info L, Move_Spec M)
    {
        if ((M.fromX() == fromx) && (M.fromY() == fromy) && (M.color == color))
        {
            v.addElement(M);
        }
    }

    public Enumeration<Move_Spec> elements()
    {
        return (v.elements());
    }
}
