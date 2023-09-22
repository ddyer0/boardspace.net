/* copyright notice */package loa;

import lib.G;


public class Stone_Type extends Stone
{
    static final Stone_Type Empty = new Stone_Type("Empty", '.');
    static final Stone_Type Black = new Stone_Type("Black", 'B');
    static final Stone_Type White = new Stone_Type("White", 'W');

    //	static final Stone_Type Both = new Stone_Type("Both",'?');
    static final Stone_Type Blocked = new Stone_Type("Blocked", 'x');
    private char char_name;
    private String string_name;
    
    public Stone_Type(String nam, char cn)
    {
        super(nam);
        char_name = cn;
        string_name= ""+cn;
    }

    //  static final Stone_Type Unknown = new Stone_Type("Unknown",'U');
    public char Name_as_Char()
    {
        return (char_name);
    }
    public String Name_as_String()
    {	return(string_name);
    }

    public boolean Is_Colored()
    {
        return (!Is_Empty());
    }

    public boolean Is_Empty()
    {
        return ((this==Empty)||(this==Blocked));
    }

    public boolean Assert_Colored()
    {	
        G.Assert(Is_Colored(), this + "is not a colored stone");
        return (false);
    }

    public boolean Assert_Empty()
    {
        G.Assert(Is_Empty(), this + "is not an Empty stone");

        return (false);
    }

    public Player_Info Player(Loa_Board bd)
    {
        if (this == White)
        {
            return (bd.white);
        }

        if (this == Black)
        {
            return (bd.black);
        }

        G.Assert(false, this + "Does not have a player");

        return (null);
    }
}
