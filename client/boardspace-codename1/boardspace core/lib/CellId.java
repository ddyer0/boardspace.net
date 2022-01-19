package lib;
/**
 * This is the base type for the action codes associated with buttons and movable objects.
 * Usually these are implemented by Enums.  
 * @author ddyer
 *
 */
public interface CellId {
	public static final String IdNotFoundError = "Id %s not found";	// not a translated string
	public String name();
	public String shortName();
}
