package lib;

/** a stack of some type of DrawableImage */
public class DrawableImageStack extends OStack<DrawableImage<?>>
{	public DrawableImage<?>[] newComponentArray(int n) 
	{ return(new DrawableImage[n]); 
	}
}
