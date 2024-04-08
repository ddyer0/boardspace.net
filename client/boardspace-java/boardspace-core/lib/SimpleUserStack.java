package lib;

public class SimpleUserStack extends OStack<SimpleUser>
{
	public SimpleUser[] newComponentArray(int sz) {
		return new SimpleUser[sz];
	}
	public boolean eq(SimpleUser a, SimpleUser o) { return a.name.equalsIgnoreCase(o.name); }

}
