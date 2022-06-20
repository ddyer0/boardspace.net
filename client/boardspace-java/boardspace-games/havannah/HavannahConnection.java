package havannah;

// maintain a connection between two blobs, connecting through a particular cell
public class HavannahConnection {
	HavannahBlob to;
	HavannahCell through;
	HavannahConnection next;
	public HavannahConnection(HavannahBlob connto,HavannahCell conthru,HavannahConnection nex)
	{	to=connto;
		through=conthru;
		next=nex;
	}
}
