package bridge;

import java.awt.Component;

import lib.G;

public class ProxyWindow {
	public Component getComponent() { throw G.Error("not expected"); }
}
