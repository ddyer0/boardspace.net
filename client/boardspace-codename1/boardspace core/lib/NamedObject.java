package lib;


/**
 * NamedObject is a simple extension to Object,  it
 * has a user-supplied name.  That name will appear whenever the
 * object is printed, so you'll see #<object@nnn Good Stuff> instead
 * of #<object@xyx>
 *
 * You will thank yourself if you use static variables derived from NamedObject
 * instead of integers as "Enum" replacements
 * <p>
 * for example:
 * <pre>
 *         class Cases extends NamedObject
 *         { public static final NormalCase = new Cases("normal");
 *           public static final Othercase = new Cases("unusual");
 *           public Cases(String name) { super(name); }
 *         }
 * </pre>
 * <br>
 * then, in your code
 * <pre>
 *         class Foo implements Cases
 *         {
 *          public test(Cases thiscase)
 *          { if(thiscase == NormalCase) return(true) else return(false);
 *          }
 *         }
 * </pre>
 * <p>
 * the only downside of this is that you can't use "switch" statements on Cases
 *
 *
 * @author Dave Dyer <ddyer@netcom.com>
 * @version 1.0, August 1996
 *
 *
 */
public class NamedObject implements NameProvider
{
    private static int Unnamed_Sequence = 0;
    public String name;

    // constructor 
    public NamedObject()
    {
        name = "unnamed_" + Unnamed_Sequence++;
    }

    public NamedObject(String nam)
    {
        name = nam;
    }

    public String getName()
    {
        return (name);
    }

    public void setName(String n) { name = n; }
    
    public String toString()
    {
        return ("<" + getClass().toString() + ":" + name + ">");
    }
}
