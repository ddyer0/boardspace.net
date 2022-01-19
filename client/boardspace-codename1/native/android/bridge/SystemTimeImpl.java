package bridge;

import android.os.SystemClock;

public class SystemTimeImpl {
    public long currentNanoTime() {
    	return SystemClock.elapsedRealtimeNanos();
    }

    public boolean isSupported() {
        return true;
    }

}
