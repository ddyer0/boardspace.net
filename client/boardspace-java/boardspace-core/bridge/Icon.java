package bridge;

import java.awt.Component;
import lib.Graphics;

public interface Icon extends javax.swing.Icon
{
	void paintIcon(Component c, Graphics g, int x, int y);
}
