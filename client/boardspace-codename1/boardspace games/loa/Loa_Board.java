package loa;

import online.game.*;

import java.util.*;

import lib.G;


public class Loa_Board extends BaseBoard implements BoardProtocol,UIC,Play2Constants
{	
	public int REVISION = 100;		// revision 100 adds rev numbers and the random setup
	
	public int getMaxRevisionLevel() { return(REVISION); }

	private LoaState board_state = LoaState.Play;
	public BoardState getState() { return board_state;}
    static Integer one = Integer.valueOf(1);
    Object getTheOne() { return(one); }
    boolean isTheOne(Object x) 
    	{ return(x==getTheOne()); 
    	}
    Setup_Type setup = Setup_Type.Standard;
    int size;
    int ndiags;
    int max_stones;
    int current_move_number = 1;
    Square_State[] squares;
    Stone_Type emptiedColor = Stone_Type.Empty;
    Winning_Move_Counter winning_move_counter = new Winning_Move_Counter();
    public int movingObjectIndex() { return(NothingMoving); }

    // loa board doesn't have a "wait for done" state
    public boolean DoneState() { return(false); }
    public boolean DigestState() { return(board_state==LoaState.Play);}
    public void SetDrawState() { board_state = LoaState.GameOver; }  
    // bookeeping for lines caches 
    int state;
    int sequence;
    Cache_State line_cache;
    Line_Info[] row_info;

    // rows on the board 
    Line_Info[] col_info;

    // columns down the board 
    Line_Info[] ldiag_info;

    // left diagonals, from 0,0 - n,n 
    Line_Info[] rdiag_info;

    // right diagonals, from n,0 to 0,n 
    Player_Info white;
    Player_Info black;
    Player_Info next_player;
    Player_Info previous_player;
    Player_Info winner_by_resignation;
    Vector<Stone_Spec> nonstandard_setup = new Vector<Stone_Spec>();
    int sweep_counter;
    int hashcode;
    Integer hashcodeInteger;
    int[] black_zhash;
    int[] white_zhash;
    Hashtable<Integer,Integer> hashed_positions;

    // constructor 
    public Loa_Board(String init_option,int map[],long random)
    {	setColorMap(map);
    	randomKey = random;
    	revision = REVISION;
        doInit(init_option);
    }
    public void doInit() { doInit(setup.name); }
    
    
    public void doInit(String typ,long key)
	{ 	StringTokenizer tok = new StringTokenizer(typ);
		String name = tok.nextToken();
		long key1 = tok.hasMoreElements() ? G.LongToken(tok) : key;
		int rev = tok.hasMoreElements() ? G.IntToken(tok) : revision;
		doInit(name,key1,rev); 
	}

    public Loa_Board cloneBoard() 
	{ Loa_Board dup = new Loa_Board(gametype,getColorMap(),randomKey); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((Loa_Board)b); }
    public Loa_Board(int sz)
    {
        setBoardSize(sz);
    }

    final int LDIAG_INDEX(int x, int y)
    {
        return ((x + size) - 1 - y);
    }

    final int RDIAG_INDEX(int x, int y)
    {
        return (x + y);
    }

    final int ROW_INDEX(int x, int y)
    {
        return (y);
    }

    final int COL_INDEX(int x, int y)
    {
        return (x);
    }

    final int BOARD_INDEX(int x, int y)
    {
        return (x + (size * y));
    }

    public void doInit(String type,long key,int rev)
    {	randomKey =key;
		adjustRevision(rev);
    	board_state = LoaState.Puzzle;
    	setCurrentSetup(type);
    	setBoardSize(setup.boardSize);
    	setWhoseTurn(next_player.color);
     }

    public int boardSize()
    {
        return (size);
    }


    public boolean WinForPlayer(int idx)
    { boolean over = GameOver() ;
	  if(over) 
		{ int map[]=getColorMap();
		  return(Winner() ==  ((map[idx] == BLACK_INDEX) ? black : white));
		}
	  return(false);
    }
    public void sameboard(BoardProtocol b) {};
    public long Digest()
    {
        int siz = squares.length;
        long v = 0;
        Random r = new Random(64 * 1000);
        if(setup==Setup_Type.LOAP)
        {
        	for(int j=0;j<white.loaps_points;j++) { v^=r.nextLong(); }
           	for(int j=0;j<black.loaps_points;j++) { v^=r.nextLong(); }
        }
        for (int x = 0; x < siz; x++)
        {
            Stone_Type oldcolor = squares[x].contents;
            long r1 = r.nextLong();
            long r2 = r.nextLong();
            long r3 = r.nextLong();

            /* select 3 random integers each time, use one of them to modify the hash.
               we depend on the sequence being fixed by the random seed but the same
               for all implementations */
            if (oldcolor == Stone_Type.White)
            {
                v ^= r1;
            }
            else if (oldcolor == Stone_Type.Black)
            {
                v ^= r2;
            }
            else if (oldcolor == Stone_Type.Empty)
            {
                v ^= r3;
            }
        }

        return (v);
    }

    /* this needs to be synchronized so refreshes don't catch it in the act */
    public synchronized void setBoardSize(int sz)
    {
        int sqlen = sz * sz;
        size = sz;
        ndiags = (size + size) - 1;
        max_stones = size * size; /* for custom setups, anything can happen */
        winner_by_resignation = null;
        squares = new Square_State[sqlen];
        black_zhash = new int[sqlen];
        white_zhash = new int[sqlen];
        Random r = new Random(234265);
        for (int i = 0; i < squares.length; i++)
        {
            squares[i] = new Square_State();
            black_zhash[i] = (int) (0x7fffffff * r.nextInt());
            white_zhash[i] = (int) (0x7fffffff * r.nextInt());
        }
        for(int x=0;x<sz;x++)
        {
        	for(int y=0;y<sz;y++)
        	{
        		Square_State s = squares[BOARD_INDEX(x,y)];
        		s.x = x;
        		s.y = y;
        	}
        }
        row_info = Line_Info.Make_Ortho_Line_Info(this, "row", size, 0, 0, 1, 0);
        col_info = Line_Info.Make_Ortho_Line_Info(this, "col", size, 0, 0, 0, 1);
        ldiag_info = Line_Info.Make_Diagonal_Line_Info(this, "ldiag", size, 0,
                size - 1, 1, 1);
        rdiag_info = Line_Info.Make_Diagonal_Line_Info(this, "rdiag", size, 0,
                0, -1, 1);
        black = new Player_Info(this, Stone_Type.Black);
        white = new Player_Info(this, Stone_Type.White);
        black.Set_Other_Player(white);
        white.Set_Other_Player(black);
        setCurrentSetup(setup.name);
        current_move_number = 1;
    }

    private void Check_Board()
    {
        int n_black = black.n_stones;
        int n_white = white.n_stones;
        int totalstones = n_black + n_white;
        G.Assert(n_black <= max_stones, "too many black stones");
        G.Assert(n_white <= max_stones, "too many white stones");
        /* check that sum of all row counts = total stones */
        {
            int i;
            int rowcount;

            for (i = 0, rowcount = 0; i < size; i++)
            {
                rowcount += row_info[i].n_stones;
            }

            G.Assert(rowcount == totalstones, "sum of all rows != rowstones");
        }
        /* check that sum of all col counts = total stones */
        {
            int i;
            int colcount;

            for (i = 0, colcount = 0; i < size; i++)
            {
                colcount += col_info[i].n_stones;
            }

            G.Assert(colcount == totalstones,
                "sum of all colums != total stones");
        }
        /* check that sum of all ldiag counts = total stones */
        {
            int i;
            int ldiagcount;

            for (i = 0, ldiagcount = 0; i < ndiags; i++)
            {
                ldiagcount += ldiag_info[i].n_stones;
            }

            G.Assert(ldiagcount == totalstones,
                "sum of left diagonals != total stones");
        }
        /* check that sum of all rdiag counts = total stones */
        {
            int i;
            int rdiagcount;

            for (i = 0, rdiagcount = 0; i < ndiags; i++)
            {
                rdiagcount += rdiag_info[i].n_stones;
            }

            G.Assert(rdiagcount == totalstones,
                "sum of right diagonals = total stones");
        }

        {
            int wh_stone = 0;
            int bl_stone = 0;
            int nsquares = squares.length;

            /* check that total stones of each color is correct */
            for (int i = 0; i < nsquares; i++)
            {
                Stone_Type stone = squares[i].contents;

                if (stone == Stone_Type.Black)
                {
                    bl_stone++;
                }
                else if (stone == Stone_Type.White)
                {
                    wh_stone++;
                }
            }

            G.Assert(bl_stone == n_black, "number of black stones is incorrect");
            G.Assert(wh_stone == n_white, "number of white stones is correct");
        }
    }

    void Invalidate_All_Move_Caches()
    {
        for (int i = 0, sz = size; i < sz; i++)
        {
            row_info[i].Invalidate_Move_Cache();
            col_info[i].Invalidate_Move_Cache();
        }

        for (int i = 0, dsz = ndiags; i < dsz; i++)
        {
            ldiag_info[i].Invalidate_Move_Cache();
            rdiag_info[i].Invalidate_Move_Cache();
        }
    }

    void Initialize()
    {	if(size==0) { setBoardSize(8); }
        int nsquares = squares.length;
        int i;

        /* set all squares to empty */
        for (i = 0; i < nsquares; i++)
        {
            squares[i].contents = Stone_Type.Empty;
            squares[i].sweep_counter = 0;
        }
  
        /* say there are no stones on the board */
        state = 0;
        sequence = 0;
        line_cache = Cache_State.Invalid;
        Line_Info.Initialize(col_info);
        Line_Info.Initialize(row_info);
        Line_Info.Initialize(ldiag_info);
        Line_Info.Initialize(rdiag_info);

        black.Initialize();
        white.Initialize();

        current_move_number = 1;

        next_player = black;
        whoseTurn = BLACK_INDEX;
        previous_player = white;
        sweep_counter = 0;
        hashcode = 0;
        hashcodeInteger = Integer.valueOf(0);
        hashed_positions = new Hashtable<Integer,Integer>();

        Check_Board();
    }

    private final int line_count(int from_x, int from_y, int dx, int dy)
    {
        int count = -1;

        if (dx == 0)
        { /* a column move */
            count = col_info[COL_INDEX(from_x, from_y)].n_stones;
        }
        else if (dy == 0)
        { /* a row move */
            count = row_info[ROW_INDEX(from_x, from_y)].n_stones;
        }
        else if (dx == -dy)
        { /* a right diagonal move */
            count = rdiag_info[RDIAG_INDEX(from_x, from_y)].n_stones;
        }
        else if (dx == dy)
        { /* a ldiag move */
            count = ldiag_info[LDIAG_INDEX(from_x, from_y)].n_stones;
        }
        else
        {
            G.Assert(false, "move is in an illegal direction");
        }

        return (count);
    }

    Move_Reason Test_Move_From_To(int from_x, int from_y, int to_x, int to_y,
        Move_Spec cc)
    {
        int dist_x = to_x - from_x;
        int dist_y = to_y - from_y;
        int dx = 0;
        int dy = 0;

        if (dist_x != 0)
        {
            if (dist_x < 0)
            {
                dist_x = -dist_x;
                dx = -1;
            }
            else
            {
                dx = 1;
            }
        }

 
        if (dist_y != 0)
        {
            if (dist_y < 0)
            {
                dist_y = -dist_y;
                dy = -1;
            }
            else
            {
                dy = 1;
            }
        }


        if ((from_x < 0) || (from_y < 0) || (to_x >= size) || (to_y >= size))
        {
            cc.color = Stone_Type.Empty;
            cc.capture_p = false;

            return (Move_Reason.Bad_Coordinates);
        }

 
        {
            int dist = (dist_x > dist_y) ? dist_x : dist_y;

            if (((from_x + (dx * dist)) != to_x) ||
                    ((from_y + (dy * dist)) != to_y))
            {
                cc.color = Stone_Type.Empty;
                cc.capture_p = false;

                return (Move_Reason.Bad_Direction);
            }

            /* determine which line, make sure op is correct */
            if (line_count(from_x, from_y, dx, dy) != dist)
            {
                cc.color = Stone_Type.Empty;
                cc.capture_p = false;

                return (Move_Reason.Wrong_Count);
            }


            cc.x = from_x;
            cc.y = from_y;
            cc.dx = dx;
            cc.dy = dy;
            cc.op = dist;

            {
                Move_Reason ok_so_far = Legal_Move_P(cc);

                if ((cc.color != null) && (ok_so_far == Move_Reason.Ok))
                {
                    Stone_Type piece = cc.color;

                    if (piece != next_player.color)
                    {
                        ok_so_far = Move_Reason.Wrong_Color;
                    }

                }

                return (ok_so_far);
            }
        }
    }

    // test a move spec for legality.  This is used when reloading games 
    Move_Reason Test_Move(Move_Spec mv)
    {
        int sx = mv.x;
        int sy = mv.y;
        int n = mv.op;
        Move_Reason reason = (n <= 0)
            ? Move_Reason.Ok /* one of the special moves */
            : Test_Move_From_To(sx, sy, sx + (n * mv.dx), sy + (n * mv.dy), mv);

        if (reason != Move_Reason.Ok)
        {
            G.Assert(false, "Move " + mv + "is Illegal, reason: " + reason);
        }

        return (reason);
    }

    /* this is an internal function; usually, call "Test_Move" */
    Move_Reason Legal_Move_P(Move_Spec move)
    {
        int nstones = move.op;
        int from_x = move.x;
        int from_y = move.y;
        int dx = move.dx;
        int dy = move.dy;
        int initial_index = BOARD_INDEX(from_x, from_y);
        int delta_index = BOARD_INDEX(dx, dy);

        if ((dx < -1) || (dy < -1) || (dx > 1) || (dy > 1) ||
                ((dx == 0) && (dy == 0)))
        {
            return (Move_Reason.Bad_Direction);
        }
        /* we have a stone */
        {
            int to_x = from_x + (nstones * dx);
            int to_y = from_y + (nstones * dy);

            if ((to_x < 0) || (to_x >= size) || (to_y < 0) || (to_y >= size))
            {
                return (Move_Reason.Off_Board);
            }
        }

        if ((from_x < 0) || (from_x >= size) || (from_y < 0) ||
                (from_y >= size))
        {
            return (Move_Reason.Off_Board);
        }

        Square_State[] stones = squares;
        Stone_Type local_stone = stones[initial_index].contents;

        move.capture_p = false;
        move.color = local_stone;

        if (local_stone.Is_Empty())
        {
            return (Move_Reason.From_Empty);
        }

        {
            Stone_Type to_color = stones[initial_index +
                (nstones * delta_index)].contents;

            if (to_color.Is_Colored())
            {
                if (to_color == local_stone)
                {
                    return (Move_Reason.Land_On_Friend);
                }

                move.capture_p = true;
            }

            if (to_color == Stone_Type.Blocked)
            {
                return (Move_Reason.Blocked_Square);
            }
        }

        {
            boolean ok = true;

            /* check that skipped stones are same color or empty */
            for (int i = 1; (i < nstones) && ok; i++)
            {
                Stone_Type skip_stone = stones[initial_index +
                    (i * delta_index)].contents;

                if (!(skip_stone.Is_Empty() || (skip_stone == local_stone)))
                {
                    return (Move_Reason.Skip_Enemy);
                }
            }
        }

 
        return (Move_Reason.Ok);
    }

    //+ count the three other neighbors, which combined withthe state of the
    //+   changins stone in the center are the information needed to update the 
    //.   quads 
    static final int QCOUNT(Stone_Type a, Stone_Type b, Stone_Type c,
        Stone_Type color)
    {
        return (((a == color) ? 1 : 0) + (((b) == color) ? 1 : 0) +
        (((c) == color) ? 1 : 0));
    }

    void Update_Quads(Quad_Count qc, Stone_Type color, int x, int y)
    {
        int idx = BOARD_INDEX(x, y);
        int sp1 = size + 1;
        int sm1 = size - 1;
        Stone_Type rm1_l;
        Stone_Type rm1_c;
        Stone_Type rm1_r;
        Stone_Type r_l;
        Stone_Type r_c;
        Stone_Type r_r;
        Stone_Type rp1_l;
        Stone_Type rp1_c;
        Stone_Type rp1_r;

        // build neighborhood for previous row
        if (y == 0)
        {
            rm1_l = rm1_c = rm1_r = Stone_Type.Empty;
        }
        else
        {
            if (x == 0)
            {
                rm1_l = Stone_Type.Empty;
            }
            else
            {
                rm1_l = squares[idx - sp1].contents;
            }

            if (x == sm1)
            {
                rm1_r = Stone_Type.Empty;
            }
            else
            {
                rm1_r = squares[idx - sm1].contents;
            }

            rm1_c = squares[idx - size].contents;
        }

        // build neighborhood for next row
        if (y == sm1)
        {
            rp1_l = rp1_c = rp1_r = Stone_Type.Empty;
        }
        else
        {
            if (x == 0)
            {
                rp1_l = Stone_Type.Empty;
            }
            else
            {
                rp1_l = squares[idx + sm1].contents;
            }

            if (x == sm1)
            {
                rp1_r = Stone_Type.Empty;
            }
            else
            {
                rp1_r = squares[idx + sp1].contents;
            }

            rp1_c = squares[idx + size].contents;
        }
        // build neighborhood for current row
        {
            if (x == 0)
            {
                r_l = Stone_Type.Empty;
            }
            else
            {
                r_l = squares[idx - 1].contents;
            }

            if (x == sm1)
            {
                r_r = Stone_Type.Empty;
            }
            else
            {
                r_r = squares[idx + 1].contents;
            }

            r_c = squares[idx].contents;
        }
        /* the neighborhoods are all expanded, now, if the center is the
        desired color, we are adding quads, if it's empty we're subtracting
        */
        {
            int count_left_minus = QCOUNT(rm1_l, rm1_c, r_l, color);
            int count_right_minus = QCOUNT(rm1_c, rm1_r, r_r, color);
            int count_left_plus = QCOUNT(r_l, rp1_l, rp1_c, color);
            int count_right_plus = QCOUNT(r_r, rp1_c, rp1_r, color);

            if (r_c == color)
            {
                qc.add_quad(count_left_minus, rm1_l, color);
                qc.add_quad(count_right_minus, rm1_r, color);
                qc.add_quad(count_left_plus, rp1_l, color);
                qc.add_quad(count_right_plus, rp1_r, color);
            }
            else
            { /* subtracting quads */
                qc.sub_quad(count_left_minus, rm1_l, color);
                qc.sub_quad(count_right_minus, rm1_r, color);
                qc.sub_quad(count_left_plus, rp1_l, color);
                qc.sub_quad(count_right_plus, rp1_r, color);
            }

            qc.Calculate_Euler();
        }
    }

    void Xor_Hashcode(int idx, Stone_Type color)
    {
        int xhash = 0;

        if (color == Stone_Type.White)
        {
            xhash = white_zhash[idx];
        }
        else if (color == Stone_Type.Black)
        {
            xhash = black_zhash[idx];
        }
        else
        {
            G.Assert(false, "Stone color " + color + " not handled");
        }

        hashcode ^= xhash;
        hashcodeInteger = Integer.valueOf(hashcode);
    }

    public Stone_Type Add_Stone(int x, int y, Stone_Type color,boolean loap_score)
    {
        int idx = BOARD_INDEX(x, y);
        Stone_Type oldcolor = squares[idx].contents;

        oldcolor.Assert_Empty();

        squares[idx].contents = color;

        line_cache = Cache_State.Invalid;
        row_info[ROW_INDEX(x, y)].Change_Count(1);
        col_info[COL_INDEX(x, y)].Change_Count(1);
        ldiag_info[LDIAG_INDEX(x, y)].Change_Count(1);
        rdiag_info[RDIAG_INDEX(x, y)].Change_Count(1);
        Xor_Hashcode(idx, color);
        color.Player(this).Add_Stone(x, y,loap_score);

        //not needed now that its debugged
        //Check_Board();
        return (color);
    }

    public Stone_Type Remove_Stone(int x, int y, Stone_Type newcolor,boolean loap_score)
    {
        int idx = BOARD_INDEX(x, y);
        Stone_Type oldcolor = squares[idx].contents;

        oldcolor.Assert_Colored();

        squares[idx].contents = newcolor;

        line_cache = Cache_State.Invalid;

        row_info[ROW_INDEX(x, y)].Change_Count(-1);
        col_info[COL_INDEX(x, y)].Change_Count(-1);
        ldiag_info[LDIAG_INDEX(x, y)].Change_Count(-1);
        rdiag_info[RDIAG_INDEX(x, y)].Change_Count(-1);

        Xor_Hashcode(idx, oldcolor);
        oldcolor.Player(this).Remove_Stone(x, y,loap_score);

        //not needed now that its debugged
        //Check_Board();
        return (oldcolor);
    }

    // call only when in custom setup mode 
    void make_square_be_color(int x, int y, Stone_Type color)
    {
        Vector<Stone_Spec> stones = nonstandard_setup;
        G.Assert(setup == Setup_Type.Custom, "Setup_Type is not Custom");

        for (Enumeration<Stone_Spec> st = stones.elements(); st.hasMoreElements();)
        {
            Stone_Spec s = st.nextElement();

            if ((s.x == x) && (s.y == y))
            {
                /* remove old setup stone */
                Remove_Stone(x, y, Stone_Type.Empty,false);
                nonstandard_setup.removeElement(s);
            }
        }

        if (!color.Is_Empty())
        {
            Stone_Spec s = new Stone_Spec(x, y, color);
            nonstandard_setup.addElement(s);
            Add_Stone(x, y, color,false);
        }
    }

    /**prepare the board for the standard game */
    void prepare_standard_setup(boolean normal)
    {
        Stone_Type bl = ((normal != true) ? Stone_Type.White : Stone_Type.Black);
        Stone_Type wh = ((normal != true) ? Stone_Type.Black : Stone_Type.White);
        Initialize();

        for (int i = 1, sz = size - 1; i < sz; i++)
        {
            Add_Stone(i, 0, bl,false);
            Add_Stone(i, sz, bl,false);
            Add_Stone(0, i, wh,false);
            Add_Stone(sz, i, wh,false);
        }
    }
    /**prepare the board for the standard game */
    void prepare_loap_setup(boolean normal)
    {	int mid=size/2;
    	if(size==0) { setBoardSize(7); }
        Stone_Type bl = ((normal != true) ? Stone_Type.White : Stone_Type.Black);
        Stone_Type wh = ((normal != true) ? Stone_Type.Black : Stone_Type.White);
        Initialize();

        for (int i = 1, sz = size - 1; i < sz; i++)
        {	if(i!=mid)
	        {
	            Add_Stone(i, 0, bl,false);
	            Add_Stone(i, sz, bl,false);
	            Add_Stone(0, i, wh,false);
	            Add_Stone(sz, i, wh,false);
	        }
	        }
    }
    /**prepare the board for a scrambled eggs variation game */
    void prepare_scrambled_setup(boolean normal)
    {
        int key = ((normal == true) ? 1 : 0);
        Initialize();

        for (int i = 1, sz = size - 1; i < sz; i++)
        {
            Add_Stone(i, 0,
                (((i & 1) != key) ? Stone_Type.Black : Stone_Type.White),false);
            Add_Stone(i, sz,
                (((i & 1) == key) ? Stone_Type.Black : Stone_Type.White),false);
            Add_Stone(0, i,
                (((i & 1) == key) ? Stone_Type.Black : Stone_Type.White),false);
            Add_Stone(sz, i,
                (((i & 1) != key) ? Stone_Type.Black : Stone_Type.White),false);
        }
    }

    /**prepare the board for a parachute game */
    void prepare_parachute_setup(boolean normal)
    {
        int key = (normal == true) ? 1 : 0;
        Initialize();

        for (int i = 1, sz = size - 1; i < sz; i++)
        {
            Add_Stone(0, i,
                (((i & 1) == key) ? Stone_Type.Black : Stone_Type.White),false);
            Add_Stone(sz, i,
                (((i & 1) != key) ? Stone_Type.Black : Stone_Type.White),false);
        }
    }
    /**prepare the board for a parachute game */
    void prepare_random_setup(Random r)
    {
        Initialize();

        for (int i = 1, sz = size - 1; i < sz; i++)
        {	Stone_Type s1 = r.nextInt(100)>=50 ? Stone_Type.Black : Stone_Type.White;
        	Stone_Type s2 = r.nextInt(100)>=50 ? Stone_Type.Black : Stone_Type.White;
        	Stone_Type s1a = s1==Stone_Type.Black ? Stone_Type.White : Stone_Type.Black;
        	Stone_Type s2a = s2==Stone_Type.Black ? Stone_Type.White : Stone_Type.Black;
       	
            Add_Stone(0, i, s1,false);
            Add_Stone(sz, sz-i,s1a,false);
            
            Add_Stone(i, 0,s2,false);
            Add_Stone( sz-i, sz,s2a,false);
           
            
        }
    }

    /**prepare the board an arbitrary set of stones, as for a problem rather than a game */
    void prepare_custom_setup()
    {
        Initialize();
        nonstandard_setup.removeAllElements();
    }

    void init_for_current_setup()
    {
        emptiedColor = Stone_Type.Empty;
        switch(setup)
        {
        case LOAP:
        	prepare_loap_setup(true);
        	break;
        case Loa_12:
        case Standard:
        case Loa:
            prepare_standard_setup(true);
            break;
        case Gemma:
        	prepare_standard_setup(true);
        	emptiedColor = Stone_Type.Blocked;
        	break;
        case Scrambled:
           prepare_scrambled_setup(true);
           break;
        case Parachute:
        	prepare_parachute_setup(true);
        	break;
        case Reversed:
            prepare_standard_setup(false);
        	break;
        case Reversed_Scrambled:
            prepare_scrambled_setup(false);
            break;
        case Reversed_Parachute:
            prepare_parachute_setup(false);
            break;
        case Custom:
            prepare_custom_setup();
            break;
        case Random:
        	prepare_random_setup(new Random(randomKey));
        	break;
        default:
            G.Assert(false,
                "Missing initialization information for setup " + setup);
        }
    }

    void Scan_For_Moves()
    {
        if (line_cache != Cache_State.Valid)
        {
            /* scan for rows and columns */
            for (int i = 0; i < size; i++)
            {
                row_info[i].Scan_For_Moves();
                col_info[i].Scan_For_Moves();
            }


            /* scan for diagonals. note that the first and last
            diagonals never have legal moves */
            for (int i = 1, dsz = ndiags - 1; i < dsz; i++)
            {
                ldiag_info[i].Scan_For_Moves();
                rdiag_info[i].Scan_For_Moves();
            }

            line_cache = Cache_State.Valid;
        }
    }
    boolean loaps_gameover()
    { return((current_move_number>=LOAPS_MAX_MOVES) || previous_player.Exactly_One_Group() || next_player.Exactly_One_Group());
    }
    // is this move a winning move? 
    boolean winning_move_p(Move_Spec move)
    {
        boolean val = false;
        Player_Info pp = previous_player;

        /* we let the previous player move again, see if it wins */
        if (pp.color == move.color)
        {
            Make_Move(move);

            if(setup==Setup_Type.LOAP)
            { if(loaps_gameover()) 
            	{ val = (current_move_number<LOAPS_MAX_MOVES) && (pp.loaps_points > next_player.loaps_points); 
            	}
            }
            else if (pp.Exactly_One_Group())
            {   
                val = true;
            }

            Unmake_Move(move);
        }

        return (val);
    }

    // the number of ways the other guy could win 
    int N_Check_Moves()
    {
        Object lock = winning_move_counter.Lock_Counter("n_check_moves");
        Map_Over_Moves(winning_move_counter);

        return (winning_move_counter.Unlock_Counter(lock));
    }

    void Map_Over_Moves(Move_Mapper res)
    {
        Scan_For_Moves();

        for (int i = 0; i < size; i++)
        {
            row_info[i].Map_Over_Moves(res);
            col_info[i].Map_Over_Moves(res);
        }


        /* note that the first and last diagonals never have legal moves */
        for (int i = 1, dsz = ndiags - 1; i < dsz; i++)
        {
            ldiag_info[i].Map_Over_Moves(res);
            rdiag_info[i].Map_Over_Moves(res);
        }
    }

    void Set_New_Player(int direction)
    {
        
        current_move_number += direction;
        setWhoseTurn(previous_player.color);

    }

    void Make_Move_From_To(int fx, int fy, int tx, int ty, boolean capture)
    {
        Stone_Type color = Remove_Stone(fx, fy, emptiedColor,false);

        if (capture)
        {	next_player.loaps_points++;
            Remove_Stone(tx, ty, Stone_Type.Empty,false);
        }

        Add_Stone(tx, ty, color,true);
        if(setup==Setup_Type.LOAP) 
        	{ next_player.loaps_score_connect(LOAPS_CONNECTIVITY_BONUS);
        	  if(capture) { previous_player.loaps_score_connect(LOAPS_CONNECTIVITY_BONUS); }
        	}
        Set_New_Player(1);
    }

    void Make_Vacate_Move(int fx, int fy)
    { /* Gemma's escape move */
        Remove_Stone(fx, fy, emptiedColor,false);
        Set_New_Player(1);
    }

    void Unmake_Move_From_To(int sx, int sy, int tx, int ty, boolean capture)
    {
       if(setup==Setup_Type.LOAP) 
       	{ previous_player.loaps_score_connect(-LOAPS_CONNECTIVITY_BONUS); 
  	      if(capture) { next_player.loaps_score_connect(-LOAPS_CONNECTIVITY_BONUS); }

       	}
       Stone_Type color = Remove_Stone(tx, ty, Stone_Type.Empty,true);
       Add_Stone(sx, sy, color,false);

        if (capture)
        {   previous_player.loaps_points--;
            Add_Stone(tx, ty,
                (color == Stone_Type.Black) ? Stone_Type.White : Stone_Type.Black,false);
        }
        Set_New_Player(-1);
    }

    void Unmake_Vacate_Move(int fx, int fy)
    {
        Add_Stone(fx, fy, next_player.color,true);
        Set_New_Player(-1);
    }
    
    public boolean GameOver()
    {	if (winner_by_resignation != null) 
    		{return(true);
    		}
    	if(board_state==LoaState.GameOver) { return(true); } 
    	if(setup==Setup_Type.LOAP) return(loaps_gameover());
    	return(previous_player.Exactly_One_Group() || next_player.Exactly_One_Group());
    }
 
    Player_Info Winner()
    {	
        if (winner_by_resignation != null)
        {	board_state = LoaState.GameOver;
            return (winner_by_resignation);
        }
        else if(setup==Setup_Type.LOAP) 
        	{ if(loaps_gameover()) 
        		{ if(current_move_number>=LOAPS_MAX_MOVES) return(null);
        		  board_state = LoaState.GameOver;
        		  return((next_player.loaps_points > previous_player.loaps_points)?next_player : previous_player);
        		}
        		return(null); 
        	}
        else if (previous_player.Exactly_One_Group())
        {	board_state = LoaState.GameOver;
            return (previous_player);
        }
        else if (next_player.Exactly_One_Group())
        {	board_state = LoaState.GameOver;
            return (next_player);
        }
        else
        {
            return (null);
        }
    }

    Stone_Type Next_Color()
    {
        return (next_player.color);
    }

    Stone_Type Previous_Color()
    {
        return (previous_player.color);
    }

    void Set_Nonstandard_Setup(Vector<Stone_Spec> l)
    {
        nonstandard_setup = l;
    }

    Vector<Stone_Spec> Nonstandard_Setup()
    {
        return (nonstandard_setup);
    }

    int Mark_Stones_In_Group(Player_Info pl, int seed_x, int seed_y,
        int stone_idx)
    {
        int sidx = stone_idx + 1;
        int idx = BOARD_INDEX(seed_x, seed_y);
        Stone_Type color = pl.color;
        squares[idx].sweep_counter = sweep_counter;

        {
            int first_x = seed_x - 1;
            int first_y = seed_y - 1;
            int last_x = seed_x + 1;
            int last_y = seed_y + 1;

            if (first_x < 0)
            {
                first_x = 0;
            }

            if (first_y < 0)
            {
                first_y = 0;
            }

            if (last_y >= size)
            {
                last_y = size - 1;
            }

            if (last_x >= size)
            {
                last_x = size - 1;
            }

            for (int newy = first_y; newy <= last_y; newy++)
            {
                for (int newx = first_x; newx <= last_x; newx++)
                {
                    if ((newx != seed_x) || (newy != seed_y))
                    {
                        int newidx = BOARD_INDEX(newx, newy);

                        if (squares[newidx].sweep_counter != sweep_counter)
                        {
                            squares[newidx].sweep_counter = sweep_counter;

                            if (squares[newidx].contents == color)
                            {
                                /* swap this new stone into the next slot in the stone List */
                                pl.Swap_Into_Position(newx, newy, sidx);

                                /* recursion step */
                                sidx = Mark_Stones_In_Group(pl, newx, newy, sidx);
                            } /* end of new stone in group */} /* end of new square to investigate */} /* end of not center */}
            } /* end of 3x3 neighborhood */}

        return (sidx);
    }

    public Setup_Type currentSetup()
    {
        return (setup);
    }

    public void setCurrentSetup(String name)
    {	Setup_Type n = Setup_Type.find(name);
    	G.Assert(n!=null,"Setup type %s not found",name);
    	gametype = n.name;
    	setup = n;
    	init_for_current_setup();
    }


    public Stone_Type whoseTurnStone()
    {
        return (next_player.color);
    }


    public void setWhoseTurn(int who)
    {
        next_player = ((who == BLACK_INDEX) ? black : white);
        whoseTurn = who;
    }

    public void setWhoseTurn(Stone who)
    {
        if (next_player.color != who)
        {
            if (previous_player.color == who)
            {	
                next_player = previous_player;
                previous_player = next_player.other_player;
            }
            else
            {
                G.Assert(false, "No player matches " + who);
            }
        }
        int map[]=getColorMap();
        whoseTurn = map[(who==Stone_Type.White) ? WHITE_INDEX : BLACK_INDEX];

    }

    public Stone blockedStone()
    {
        return (Stone_Type.Blocked);
    }

    public Stone emptyStone()
    {
        return (Stone_Type.Empty);
    }

    public Stone whiteStone()
    {
        return (Stone_Type.White);
    }

    public Stone blackStone()
    {
        return (Stone_Type.Black);
    }

    /* this needs to be synchronized so refreshes won't catch it in an unstable state */
    public synchronized final Stone_Type squareContents(int x, int y)
    {
        Stone_Type cont = squares[BOARD_INDEX(x, y)].contents;

        return (cont);
    }
    public synchronized final Square_State getSquare(int x,int y)
    {
    	return(squares[BOARD_INDEX(x,y)]);
    }
    public String stonesOfColor(Stone c)
    {
        return (U.StonesOfColor(this, c));
    }

    public Stone winner()
    {
        boolean over = GameOver();
        if(over) { Player_Info p = Winner();
        		   if(p!=null) { return(p.color); }
        }
        return(null);
    }

    public int moveNumber()
    {
        return (current_move_number);
    }

    public LoaMove isLegalMove(int from_x, int from_y, int to_x, int to_y)
    {
        Move_Spec mv = new Move_Spec(next_player.color);
        Move_Reason v = Test_Move_From_To(from_x, from_y, to_x, to_y, mv);

        return ((v == Move_Reason.Ok) ? mv : null);
    }

    void Make_Move(LoaMove move)
    {
        int n = move.N();

        if (n > 0)
        { /* standard move */
            Make_Move_From_To(move.fromX(), move.fromY(), move.toX(),
                move.toY(), move.captures());
        }
        else if (n == M_Start)
        {
        	board_state = LoaState.Play;
        }
        else if (n==M_Edit)
		{
        	board_state = LoaState.Puzzle;
        }
        else if (n == M_Pass)
        {
            Set_New_Player(1); /* pass */
        }
        else if (n == M_Vacate)
        {
            Make_Vacate_Move(move.fromX(), move.fromY());
        }
        else if ((n == M_Forfeit) || (n == M_Resign))
        {	board_state = LoaState.GameOver;
            winner_by_resignation = previous_player; /* forfeit or resignation */
        }
        else if (n==MOVE_GAMEOVERONTIME)
        {	board_state = LoaState.GameOver;
        	winner_by_resignation = next_player;
        }
        else
        {
            G.Assert(false, "Move code " + n + " not handled");
        }

        add_update_hashduplicates();
    }

    public boolean Execute(commonMove move,replayMode replay)
    {
        Make_Move((LoaMove)move);
        return(true);
    }

    void Unmake_Move(LoaMove move)
    {
        int n = move.N();

        /** revert a move */
        remove_update_hashduplicates();
        board_state = LoaState.Play;
        if (n > 0)
        { /* standard move */
            Unmake_Move_From_To(move.fromX(), move.fromY(), move.toX(),
                move.toY(), move.captures());
        }
        else if ((n == M_Start)||(n==M_Edit))
        {
        }
        else if (n == M_Pass)
        {
            Set_New_Player(-1); /* pass */
        }
        else if ((n == M_Forfeit) || (n == M_Resign))
        {
            winner_by_resignation = null; /* resign or forfeit */
            Set_New_Player(-1);
        }
        else if (n == M_Vacate)
        {
            Unmake_Vacate_Move(move.fromX(), move.fromY());
        }
        else
        {
            G.Assert(false, "unMove code " + n + " not handled");
        }
    }

    /* add one to hashtable if this is a duplicate position */
    public void add_update_hashduplicates()
    {
        Object val = hashed_positions.get(hashcodeInteger);

        if (val != null)
        {
            previous_player.duplicate_position_count++;
            hashed_positions.put(hashcodeInteger,
               Integer.valueOf(1 + ((Integer) val).intValue()));
        }
        else
        {
            hashed_positions.put(hashcodeInteger, one);
        }
    }

    /* subtract one to hashtable if this was a duplicate position */
    public void remove_update_hashduplicates()
    {
    	Integer val = hashed_positions.get(hashcodeInteger);

        if (!isTheOne(val))
        {
            G.Assert(val != null, "hashcount must not be null");

            int oldcount = val.intValue();
            hashed_positions.put(hashcodeInteger,
                (oldcount == 2) ? one : Integer.valueOf(oldcount - 1));
            previous_player.duplicate_position_count--;
        }
        else
        {
            hashed_positions.remove(hashcodeInteger);
        }
    }

    public Enumeration<Move_Spec> getLegalMoves(int fromx, int fromy)
    {
        Stone s = squareContents(fromx, fromy);
        Move_Finder m = new Move_Finder(s, fromx, fromy);
        Map_Over_Moves(m);

        return (m.elements());
    }

    public void copyFrom(Loa_Board from)
    {
        doInit(from.gametype);
        board_state = from.board_state;
        setBoardSize(from.boardSize());
        setCurrentSetup("Custom");
        current_move_number = from.current_move_number;

        for (int x = 0; x < size; x++)
        {
            for (int y = 0; y < size; y++)
            {
                make_square_be_color(x, y, from.squareContents(x, y));
            }
        }
        white.loaps_points=from.white.loaps_points;
        black.loaps_points=from.black.loaps_points;
        setup = from.currentSetup();

        setWhoseTurn(from.whoseTurnStone());
        Check_Board();
    }
	public String gameTypeString() {
		if(revision<100) { return gametype; }
		return gametype+" "+randomKey+" "+revision;
	}

}