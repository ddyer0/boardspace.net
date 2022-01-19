namespace com.boardspace.dtest{


public class SystemTimeImpl : ISystemTimeImpl {
    public long currentNanoTime() {
        return 0;
    }

    public bool isSupported() {
        return false;
    }

}
}
