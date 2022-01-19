package bridge;

public class SystemTimeImpl implements bridge.SystemTime{
    public long currentNanoTime() {
    	return System.nanoTime();
    }

    public boolean isSupported() {
        return true;
    }

}
