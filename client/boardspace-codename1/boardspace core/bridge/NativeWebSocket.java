package bridge;

import lib.G;

public class NativeWebSocket {
	  public String read(int sock) { throw G.Error("shouldn't be called"); }
	  public void send(int sock,String message){ throw G.Error("shouldn't be called"); }
	  public boolean isConnected(int socket){ throw G.Error("shouldn't be called"); }   
	  public int connect(String host,int socket){ throw G.Error("shouldn't be called"); };

}
