package online.search;

public interface Constants
{
    static final double INFINITY = 1.0E10;
    static final double NaN = 0.0/0.0;
    
    enum Stop_Reason
    {
    	Dont_Stop, Alpha_Cutoff, Good_Enough
    }
    enum Search_Result
    {
        Active,Done,Level_Done
    }
}
