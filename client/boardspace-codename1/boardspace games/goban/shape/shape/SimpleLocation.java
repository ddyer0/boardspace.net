package goban.shape.shape;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import com.codename1.io.Externalizable;


public class SimpleLocation implements LocationProvider,Serializable,Externalizable
{	
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public SimpleLocation() { } // for externalizable
	int loc;
	public int getX() { return(loc&0xffff); }
	public int getY() { return(loc>>16); }
	public SimpleLocation(int x,int y) { loc = y<<16|x; }
	public boolean equals(LocationProvider o) { return(getX()==o.getX()) && (getY()==o.getY()); }
	public boolean equals(int x,int y) { return((getX()==x) && (getY()==y)); }
	public String toString() { return("<location "+getX()+" "+getY()+">"); }

	public int getVersion() {
		return((int)serialVersionUID);
	}
	public void externalize(DataOutputStream out) throws IOException {
		out.writeInt(loc);
	}
	public void internalize(int version, DataInputStream in) throws IOException {
		loc = in.readInt();
	}
	public String getObjectId() {
		return("SimpleLocation");
	}
	
}
