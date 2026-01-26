package online.game;

import com.codename1.ui.Font;

import lib.Text;

public interface SequenceElement
{

	boolean ignoredInLogs();
	int player();
	String[] gameEvents();
	boolean getLineBreak();
	Text shortMoveText(commonCanvas canvas, Font font);
	int nVariations();
	String getSliderNumString();
	Text censoredMoveText(commonCanvas canvas, BoardProtocol bb, Font font);
	
}
