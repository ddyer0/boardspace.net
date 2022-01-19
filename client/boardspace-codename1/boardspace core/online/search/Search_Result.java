package online.search;

import lib.NamedObject;

/**
 * this is a class of singletons used to describe the internal state of a search.
 * @author ddyer
 *
 */
public class Search_Result extends NamedObject
{
    public static final Search_Result Active = new Search_Result("Active");
    public static final Search_Result Done = new Search_Result("Done");
    public static final Search_Result Level_Done = new Search_Result(
            "Level Done");

    Search_Result(String nam)
    {
        super(nam);
    }
}
