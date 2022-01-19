
package goban.shape.shape;

import lib.G;

public interface Globals
{	public boolean CheckDuplicates=true;
	public static final int shape_database_generation = 7;		//shapes up to this size are in the db
	public static final int max_board_size = 19;				//maximum go board size
	
	enum Move_Order { First(0),Second(1); int codeValue; Move_Order(int v){ codeValue=v; } };
	enum X_Position { Left(0), Center(2), Right(4); int codeValue; X_Position(int v) { codeValue=v; } }
	enum Y_Position { Top(0), Center(6), Bottom(12); int codeValue; Y_Position(int v) { codeValue=v; } }
	
	
	/* decodes of integer values for fate */
	enum Fate {
		Alive,
		Alive_In_Ko,
		Seki,
		Dead, 
		Dead_In_Ko,
		Repetition,
		Indeterminate,
		No_Eyes,
		Alive_With_Eye,
		Dead_With_Eye,
		Unknown,
		Impossible;
		public static Fate find(int v)
		{ for(Fate val : values())
			{ if(val.ordinal()==v) { return(val); }
			}
		  throw G.Error("No fate with value %s",v);
		}
	}
	enum Aux_Fate {
		Null(""),
		Ko("Ko"),
		NoMoves("No Legal Moves"),
		Outnumber("Outnumbered"),
		Super("Numerical Superiority"),
		Benson("Absolutely safe"),
		Error("** Undefined **");
		String name;
		Aux_Fate(String m) { name = m; }
		public static Aux_Fate find(int val) 
			{ for(Aux_Fate v:values())
				{
				if(val==v.ordinal()) { return(v); }
				}
			throw G.Error("No aux fate with value %s",val);
			}
	}
	
	public static final String Ordinal_Move_String[]=
	{ "@0", "@1","@2","@3","@4","@5","@6", 											    //these should occur
		" db error @7", "db error @8", "db error @9", 								  //these should not occur
			" db error @10", "db error@11", "db error@12", "db error @13",	//these should not occur
			" pass",		//special code, 14=pass
			" outside" //special code 15=play outside liberty
		};
	public static final int ordinal_move_pass = 14;		//position of "pass" above
	public static final int ordinal_move_outside = 15;//position of "outside" above
	
	/* color codes, defined to they can be used as a bitmask */
	public static final int WhiteColor=1;	//white stone
	public static final int BlackColor=2;	//black stone
	public static final int EmptyColor=4;	//neither color
	public static final int BorderColor=8;//off the board
	
}
