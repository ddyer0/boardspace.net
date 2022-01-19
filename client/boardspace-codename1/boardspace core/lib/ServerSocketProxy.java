package lib;

import java.io.IOException;

public interface ServerSocketProxy {
	public SocketProxy acceptProxy() throws IOException;
	public void closeQuietly();
}
