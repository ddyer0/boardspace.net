package loa;

import lib.NamedObject;


final public class Cache_State extends NamedObject
{
    static final Cache_State Invalid = new Cache_State("Invalid");
    static final Cache_State Valid = new Cache_State("Valid");
    static final Cache_State First_Group_Valid = new Cache_State("First Valid");
    static final Cache_State[] all_cache_states = 
        {
            Invalid, Valid, First_Group_Valid
        };

    private Cache_State(String nam)
    {
        super(nam);
    }
}
