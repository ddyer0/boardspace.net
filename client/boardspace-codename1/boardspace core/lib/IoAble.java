package lib;

import java.io.PrintStream;

public interface IoAble {
	/**
	 * save yourself in loadable form
	 * 
	 * @param out
	 * @return
	 */
	public boolean save(PrintStream out);
	/**
	 * load yourself from a printed representation
	 * @param tok
	 * @return
	 */
	public boolean load(Tokenizer tok);
	public void setName(String name);
	public static String IOABLE = "IoAble";
	public static String VERSION = "1";
	
	// save an arbitrary IoAble item
	@SuppressWarnings("deprecation")
	static public boolean saveWithId(PrintStream s,IoAble item)
	{
		s.println(IOABLE+" "+VERSION+" "+item.getClass().getName());
		item.save(s);
		return true;
	}
	// load an arbitrary IoAble item
	static public IoAble loadWithId(Tokenizer tok)
	{	if(IOABLE.equals(tok.nextElement())
			&& VERSION.equals(tok.nextElement()))
			{
			String classname = tok.nextElement();
			IoAble item = (IoAble)G.MakeInstance(classname);
			item.load(tok);
			return item;
			}	
		return null;
	}
}
