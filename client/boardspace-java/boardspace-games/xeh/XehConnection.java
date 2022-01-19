package xeh;

// maintain a connection between two blobs, connecting through a particular cell
public class XehConnection {
	XehBlob to;
	XehCell through;
	XehConnection next;
	public XehConnection(XehBlob connto,XehCell conthru,XehConnection nex)
	{	to=connto;
		through=conthru;
		next=nex;
	}
}
