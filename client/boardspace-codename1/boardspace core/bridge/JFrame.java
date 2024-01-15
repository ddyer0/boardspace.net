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
	
	@Override
	public Image getIconAsImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeFromMenuBar(JMenu actions) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changeImageIcon(Image icon) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addToMenuBar(JMenu m, DeferredEventManager l) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setCloseable(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CanvasRotater getCanvasRotater() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHasSavePanZoom(boolean v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnableRotater(boolean v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCanSavePanZoom(DeferredEventManager v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void packAndCenter() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Container getParentContainer() {
		// TODO Auto-generated method stub
		return null;
	}
	public void addC(ProxyWindow w) {
		G.Error("Not expected");
		
	}
	@Override
	public void setInitialBounds(int inx, int iny, int inw, int inh) {
		// TODO Auto-generated method stub
		
	}

	public void moveToFront() {
		MasterForm.moveToFront(this);
	}

	public void setTitle(String n) {
		
	}

}
