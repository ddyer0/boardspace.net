package common;

public interface SaltInterface {
	public void loadChecksum(int n);
	public int checksumVersion();
	public String getSalt();
	public String getTeaKey();
}
