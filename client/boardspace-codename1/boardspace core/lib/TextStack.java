package lib;

public class TextStack extends OStack<Text>
{
	public Text[]newComponentArray(int n) { return(new Text[n]); }
	public boolean eq(Text o,Text x) { return((o==null) ? x==o : o.equals(x)); }
}
