package common;

public class Dummy implements SaltInterface
{

	public void loadChecksum(int n) {
		
	}

	public int checksumVersion() {
		return 0;
	}

	public String getSalt() {
		return null;
	}

	public String getTeaKey() {
		return null;
	}

}
