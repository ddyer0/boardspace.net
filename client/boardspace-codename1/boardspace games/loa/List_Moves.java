package loa;

import online.game.commonMove;

import java.util.Vector;


class List_Moves extends Vector<commonMove> implements Move_Mapper
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	Stone_Type target_color;

    List_Moves(Stone_Type target)
    {
        target_color = target;
    }

    public void Move_Map(Line_Info info, Move_Spec move)
    {
        if (move.color == target_color)
        {
            commonMove cm = null;
            cm = move.Copy(cm);
            addElement(cm);
        }
    }
}
