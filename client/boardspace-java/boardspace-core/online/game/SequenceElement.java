package online.game;

import lib.Text;

public interface SequenceElement
{

	boolean ignoredInLogs();
	int player();
	String[] gameEvents();
	boolean getLineBreak();
	Text shortMoveText(commonCanvas canvas);
	int nVariations();
	String getSliderNumString();
	Text censoredMoveText(commonCanvas canvas, BoardProtocol bb);
	
}
