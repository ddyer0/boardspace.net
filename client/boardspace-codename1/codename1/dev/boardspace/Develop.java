package dev.boardspace;



public class Develop extends com.boardspace.Launch {
	public boolean isDevelopmentVersion() { return(true); }
	public void init(Object context)
	{
		super.init(context);
	}
	public void start() { super.start(); }
	public void stop() { super.stop(); }
	public void destroy() { super.destroy(); }
	
	//public static void main(String[]args) { }
}
