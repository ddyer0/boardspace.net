package bridge;

public class NativeWebSocket {
	  public native String read(int sock); // poll this to read
	  public native void send(int sock,String message);
	  public native boolean isConnected(int socket);   
	  public native  int connect(String host,int socket);

}
