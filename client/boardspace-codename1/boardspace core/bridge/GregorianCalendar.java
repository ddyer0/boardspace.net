package bridge;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

public class GregorianCalendar
{
	Calendar inst = Calendar.getInstance();

	public int get(int item) {
		return(inst.get(item));
	}

	public void setTime(Date stime) {
		inst.setTime(stime);
	}

	public void setTimeZone(TimeZone timeZone) {
		inst.setTimeZone(timeZone);
	}

}
