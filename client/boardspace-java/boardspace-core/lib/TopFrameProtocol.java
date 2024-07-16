package lib;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.WindowListener;

import bridge.JMenu;
import bridge.ProxyWindow;

public interface TopFrameProtocol {

	void setOpaque(boolean b);

	void setFrameBounds(int i, int j, int w, int h);
	Rectangle getFrameBounds();

	void setVisible(boolean v);

	Image getIconAsImage();

	void dispose();

	void removeFromMenuBar(JMenu actions);

	void addWindowListener(WindowListener who);

	void revalidate();

	void changeImageIcon(Image icon);

	int getWidth();

	int getHeight();

	int getX();

	int getY();

	void addC(Component p);
	
	void addC(String where,Component p);
	
	void repaint();

	void addToMenuBar(JMenu m, DeferredEventManager l);

	void setCloseable(boolean b);

	CanvasRotater getCanvasRotater();

	void setHasSavePanZoom(boolean v);

	void setEnableRotater(boolean v);

	void setCanSavePanZoom(DeferredEventManager v);

	void setLayout(LayoutManager x);

	Container getContentPane();

	void addC(ProxyWindow w);

	String tabName();

	void setSize(Dimension newps);

	void packAndCenter();
	
	void setContentPane(Container p);

	Container getParentContainer();

	void setInitialBounds(int inx, int iny, int inw, int inh);

	void moveToFront();

	void setTitle(String n);

	Insets getInsets();

}