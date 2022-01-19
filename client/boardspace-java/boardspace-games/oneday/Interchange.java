package oneday;

import lib.G;
import oneday.OnedayBoard.OnedayLocation;

public class Interchange {
	Station station;
	Line from;
	Line to;
	double timeInMinutes;
	public Interchange(Station sta,Line fromline,Line toline,double timed)
	{
		station = sta;
		from = fromline;
		to = toline;
		timeInMinutes = timed;
	}
	public Interchange(String sta,String fromLine,String toLine,String timeString)
	{	if("aldgate east".equals(sta)) { sta = "aldgate"; }
		station = Station.getStation(sta);
		from = Line.getLine(fromLine);
		to = Line.getLine(toLine);
		timeInMinutes = G.DoubleToken(timeString);
		station.addInterchange(this);
	}
	
	static public long timeFromTo(OnedayLocation from,OnedayLocation to)
	{
		Station sta = from.getStation();
		Interchange interchange = sta.findInterchange(from.getLine(),to.getLine());
		return((int)(interchange.timeInMinutes*60*1000));
	}
}
