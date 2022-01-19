package online.game.sgf;

import java.io.PrintStream;

import lib.G;


/** class sgf_property is a generic sgf format property.  The external representation
of a property is name[value], where "name" is a symbol containing one or two upper
case characters.  The properties associated with a node are stored in a linked list.
*/
public class sgf_property
{
    public sgf_property next;
    private String name;
    private Object value;
    private String key;

    /* constructor */
    public sgf_property(String nam, Object val)
    {
        value = val;
        makeKey(nam);
    }

    public sgf_property(String nam, Object valu, sgf_property nex)
    {   next = nex;
        value = valu;
        makeKey(nam);
    }

    public String toString() { return("<sgf prop "+name+"="+value+">"); }
    /* public accessors */

    /** getKey returns the short (one or two uppercase characters) name. */
    public String getKey()
    {
        return (key);
    }

    public Object getValue()
    {
        return (value);
    }

    public String getStringValue()
    {
        return ((String) value);
    }

    public String getName()
    {
        return (name);
    }

    public void setName(String newname)
    {
        makeKey(newname);
    }

    public void setValue(Object newvalue)
    {
        value = newvalue;
    }

    /** print this property in a form acceptable to the reader */
    public void sgf_print(PrintStream out)
    {
        out.print(name);
        print_bracketed_value(out);
        out.println();
    }

    /** locate a property with the specified name */
    public static sgf_property get_sgf_property(sgf_property p, String k)
    {
        while (p != null)
        {
            if (p.key.equals(k))
            {
                return (p);
            }

            p = p.next;
        }

        return (null);
    }

    /* remove the specified property from the original list, and return a new head
    for the list */
    public static sgf_property remove_sgf_property(sgf_property p,
        sgf_property rem)
    {
        if (p == rem)
        {
            sgf_property n = rem.next;
            rem.next = null;

            return (n);
        }

        sgf_property pn = p;

        while (pn != null)
        {
            if (pn.next == rem)
            {
                pn.next = rem.next;
                rem.next = null;

                break;
            }

            pn = pn.next;
        }

        return (p);
    }

    /* innards */
    private void makeKey(String newname)
    { /* scan for one or two upper case characters. throw error if none or more than 2 */

        char char1 = 0;
        char char2 = 0;
        char char3 = 0;
        for (int i = 0; i < newname.length(); i++)
        {
            char ch = newname.charAt(i);

            if (Character.isUpperCase(ch))
            {	if (char3 != 0)
            	{
            	throw G.Error("Too Many upper case chars in " + newname);
            	}
            else if (char2 != 0)
                {
                	char3 = ch;
                }
                else if (char1 != 0)
                {
                    char2 = ch;
                }
                else
                {
                    char1 = ch;
                }
            }
        }
        if(char1!=0)
        {
        key = ""+ char1;
         if(char2!=0) { key += char2; }
         if(char3!=0) { key += char3; }
        }
        else
        {
        	throw G.Error("No upper case characters in " + newname);
        }

        name = newname; /* only get here if no errors */
    }

    private void print_bracketed_value(PrintStream out)
    {
        String val = this.value.toString();
        printBracketedString(val,out);
    }
    public static String bracketedString(String str)
    {	String val = "";
    	for(int i=0,len=str.length(); i<len; i++)
    	{	char ch = str.charAt(i);
    		switch(ch)
    		{
    		case '\\': 
    		case ']':
    		case '[': val += '\\';
				//$FALL-THROUGH$
			default: val += ch;
      		}
    	}
    	return(val);
    }
    public static void printBracketedString(String val,PrintStream out)
    {
        out.print("[");
        out.print(bracketedString(val));
        out.print("]");
    }
}
