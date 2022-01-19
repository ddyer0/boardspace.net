package language;
import java.util.Calendar;

public class test 
{
	    public static void main(String[] args) {
	        // length of a day
	        long DAY_MILLIS = 1000 * 60 * 60 * 24;

	        Calendar cal = Calendar.getInstance();

	        cal.set(1900, 0, 1, 0, 0, 0);
	        System.out.println(cal.getTime());

	        cal.setTimeInMillis(cal.getTimeInMillis() + DAY_MILLIS);
	        System.out.println(cal.getTime());
	    }
}
