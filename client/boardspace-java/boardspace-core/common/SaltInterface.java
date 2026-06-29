package common;

public interface SaltInterface {
	public void loadChecksum(String n);
	public int checksumVersion();
	public String getSalt();
	public String getTeaKey();
}
