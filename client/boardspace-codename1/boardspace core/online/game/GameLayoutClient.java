package online.game;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import online.common.SeatingChart.DefinedSeating;

public interface GameLayoutClient 
{

	Rectangle createPlayerGroup(int player, int x, int y, double rotation,int unit);	
	commonPlayer getPlayerOrTemp(int n);
	DefinedSeating seatingChart();
	int standardFontSize();
	void SetupVcrRects(int left, int top, int width, int height);
	boolean isZoomed();
	void positionTheChat(Rectangle actual, Color back,Color fore);
}
