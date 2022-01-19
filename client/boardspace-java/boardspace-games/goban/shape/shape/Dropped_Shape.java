package goban.shape.shape;


/** this is an extension of OneShape to handle cases where adding a white stone
creates multiple eye shapes without connecting the entire group. These "drop-in shapes"
used to be handled by some elaborate special cases in the search logic, but are now
rolled into the standard mechanism.

The old lisp-based library contains dropped shapes associated with their parent
shapes.  
*/
public class Dropped_Shape extends OneShape implements Globals,ShapeProtocol
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	int play_at;
	int dropped_position;
	ShapeProtocol parent_shape;
	
	/** constructor */
	public Dropped_Shape(LocationProvider p[],ResultCode res[],int play,int positions,ShapeProtocol parent)
	{ 	super(parent.getName(),p,res);
		play_at = play;
		dropped_position = positions;
		name= name + "@ " + play_at + ResultCode.decode_position_bit(dropped_position);
		parent_shape = parent;	
	}


}
