package bridge;

public class KeyEvent extends Event
{	public static int CTRL_DOWN_MASK = 1024;
	private int modifiers =0;
	private char theChar = 0;
	int theCode = 0;
	public KeyEvent(int code)
	{
		theCode = code;
		theChar = (char)code;
		if(code<0x20)
		{
			modifiers = CTRL_DOWN_MASK;
		}
	}
	public int getKeyCode()
	{
		return(theCode);
	}
	public int getModifiersEx()
	{
		return(modifiers);
	}
	public char getKeyChar()
	{
		return(theChar);
	}
	public int getExtendedKeyCode() {
		return(getKeyCode());
	}
}
