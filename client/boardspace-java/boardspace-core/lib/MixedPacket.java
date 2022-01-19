package lib;

public class MixedPacket
{	public String message;
	public int sequence=0;
	public byte[] payload;
	public MixedPacket() { }
	public MixedPacket(String s) { message = s; }
	public MixedPacket(String s,byte []pay) { message = s; payload = pay; }
	public String toString() { return("<MixedPacket #"+sequence+" "+message+">"); }
}
