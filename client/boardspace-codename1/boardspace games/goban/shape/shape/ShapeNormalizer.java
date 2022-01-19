package goban.shape.shape;

import java.util.*;

/*
// header - edit "Data/yourJavaHeader" to customize
// contents - edit "EventHandlers/Java file/onCreate" to customize
//
*/
public class ShapeNormalizer extends SimpleShape implements ShapeProtocol
{
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	OneShape	reference_shape;
	int permutation_code;
	
	/* constructor */
	ShapeNormalizer(OneShape shape,LocationProvider pts[],int code)
	{
		reference_shape = shape;
		name = shape.name + " p" + code;
		points = pts;
		permutation_code = code;
	}
	
	/* return the result code array for a particular position, permuting it approprately */
	private ResultCode resultsarray(Move_Order move_spec,X_Position x_pos,Y_Position y_pos)
	{ 	//System.out.println("location " + x_pos + " " + y_pos);
		
		if((x_pos==X_Position.Center) || (y_pos==Y_Position.Center))
		{/* at an edge */
			int side = (y_pos == Y_Position.Top) ? 0 
				: (y_pos==Y_Position.Bottom) ? 2 
				: (x_pos==X_Position.Right) ? 1 : 3;
			side -= permutation_code&3;	//rotate back however many steps
			switch(side&3)
			{
			case 0: y_pos=Y_Position.Top; x_pos=X_Position.Center; break;
			case 1: x_pos=X_Position.Right; y_pos=Y_Position.Center; break;
			case 2: y_pos=Y_Position.Bottom; x_pos = X_Position.Center; break;
			case 3:	x_pos=X_Position.Left; y_pos=Y_Position.Center; break;
			default:
			}
		}else
		{/* at a corner */
			int corner = (x_pos == X_Position.Left) //encode the starting corner
				? ((y_pos==Y_Position.Top) ? 0 : 3)
				: (y_pos==Y_Position.Top) ? 1 : 2;
			corner -= (permutation_code&3);	//rotate back some number of steps
			switch(corner&3)
			{case 0: x_pos = X_Position.Left; y_pos = Y_Position.Top; break;
			case 1: x_pos = X_Position.Right; y_pos = Y_Position.Top; break;
			case 2: x_pos = X_Position.Right; y_pos= Y_Position.Bottom; break;
			case 3: x_pos = X_Position.Left; y_pos = Y_Position.Bottom; break;
			default:
			}
		}
		
		if((permutation_code&4)!=0)
		{
			if(y_pos==Y_Position.Top) { y_pos=Y_Position.Bottom; }
			else if (y_pos==Y_Position.Bottom) { y_pos=Y_Position.Top; }
		}
		
		// System.out.println("  becomes " + x_pos + " " + y_pos);
		return(reference_shape.Fate_Of_Shape_Results(move_spec,x_pos,y_pos));
	}
	
	private ResultProtocol Fate_Of_Shape_Internal(Move_Order move_spec,X_Position x_pos,Y_Position y_pos,int introns)
	{	ResultCode r=resultsarray(move_spec,x_pos,y_pos);
		return(r!=null? r.results[introns] : null);
	}
	public ResultProtocol Fate_Of_Shape(Move_Order move_spec,X_Position x_pos,Y_Position y_pos,int introns)
	{
		return(Fate_Of_Shape_Internal(move_spec,x_pos,y_pos,introns));
	}
	public SingleResult Exact_Fate_Of_Shape( Move_Order move_spec, X_Position x_pos, Y_Position y_pos,int introns,int libs)
	{	
		ResultProtocol	r = Fate_Of_Shape(move_spec,x_pos,y_pos,introns);
		return((r!=null) ? r.Fate_for_N_Liberties(libs) : null);
	}
	
	public OneShape NormalizedCopy()
	{	Hashtable<ResultCode,ResultCode> v=new Hashtable<ResultCode,ResultCode>();
		for(Move_Order mv : Move_Order.values())
		{
			for(X_Position mx : X_Position.values())
			{ 
				for(Y_Position my : Y_Position.values())
				{ 
					ResultCode results = resultsarray(mv,mx,my);
					if(results!=null) 
					{ int geocode = zhash.Encode_Move_Geometry(mv,mx,my);
						ResultCode old = (ResultCode)(v.get(results));
						if(old!=null) { old.positions |= geocode; }
						else { old = new ResultCode(geocode,results.results);
							v.put(results,old);
						}
					}
				}
			}}
		{ int i=0 ;
			ResultCode res[] = new ResultCode[v.size()];
			for(Enumeration<ResultCode> e = v.elements(); e.hasMoreElements(); i++)
			{ ResultCode r = e.nextElement();
				res[i] = r;
			}
			return(new OneShape(this.name +" ncopy",points,res));
		}
	}
}
