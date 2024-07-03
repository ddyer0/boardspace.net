package bridge;

import lib.CanvasRotater;
import lib.DeferredEventManager;
import lib.G;
import lib.Image;
import lib.TopFrameProtocol;

/** this android version is not expected to be used becuase the underlying os
 * doesn't support overlapping windows well.
 */
public class JFrame extends Frame implements TopFrameProtocol
{
	public JFrame() { super(); }
	public JFrame(String name) { super(name); }
	
	public Image getIconAsImage() {
		return null;
	}

	public void removeFromMenuBar(JMenu actions) {
		
	}

	public void changeImageIcon(Image icon) {
		
	}

	public void addToMenuBar(JMenu m, DeferredEventManager l) {
		
	}


	public void setCloseable(boolean b) {
		
	}

	public CanvasRotater getCanvasRotater() {
		return null;
	}

	public void setHasSavePanZoom(boolean v) {
		
	}

	public void setEnableRotater(boolean v) {
		
	}

	public void setCanSavePanZoom(DeferredEventManager v) {
		
	}

	public void packAndCenter() {
		
	}

	public Container getParentContainer() {
		return null;
	}
	public void addC(ProxyWindow w) {
		G.Error("Not expected");
		
	}

	public void setInitialBounds(int inx, int iny, int inw, int inh) {
		
	}

	public void moveToFront() {
		MasterForm.moveToFront(this);
	}

	public void setTitle(String n) {
		
	}

	public void removeC(com.codename1.ui.Component c) {
		G.Error("Not expected");
		
	}

}
