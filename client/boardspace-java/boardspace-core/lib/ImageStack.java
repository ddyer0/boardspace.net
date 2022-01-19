package lib;

public class ImageStack extends OStack<Image> 
{
	public Image[] newComponentArray(int sz) {
		return new Image[sz];
	}

}
