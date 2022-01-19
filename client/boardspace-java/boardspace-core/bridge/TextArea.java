package bridge;

import lib.AppendInterface;

@SuppressWarnings("serial")
public class TextArea extends java.awt.TextArea implements AppendInterface
{	public TextArea() { super(); }
	public TextArea(String a) { super(a); }
}
