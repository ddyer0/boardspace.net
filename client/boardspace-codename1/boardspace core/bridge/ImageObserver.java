package bridge;

public interface ImageObserver {
	static final int ERROR = 1;
	static final int ABORT = 2;
	static final int ALLBITS = ERROR|ABORT;
}
