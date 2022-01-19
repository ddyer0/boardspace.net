package hex;

// maintain a connection between two blobs, connecting through a particular cell
public class hexConnection {
	hexblob to;
	hexCell through;
	hexConnection next;
	public hexConnection(hexblob connto,hexCell conthru,hexConnection nex)
	{	to=connto;
		through=conthru;
		next=nex;
	}
}
