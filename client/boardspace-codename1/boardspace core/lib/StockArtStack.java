package lib;

public class StockArtStack extends OStack<DrawableImage<StockArt>> 
{ 
	public StockArt[] newComponentArray(int n) 
	{ return(new StockArt[n]); 
	}
}