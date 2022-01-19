package loa;

import java.awt.Point;

import lib.G;
import lib.NamedObject;


public class Player_Info extends NamedObject implements Evaluator_Parameters,UIC
{
    static final double SEARCH_WIN = 1.0;
    int loaps_points=0;
    Stone_Type color;
    int n_stones;
    int n_groups;
    Point[] pos;
    int[] group_idx;
    Cache_State state = Cache_State.Invalid;
    Loa_Board board;
    Player_Info other_player;
    List_Moves append_current_move;
    Static_Evaluator static_evaluator = new Default_Static_Evaluator();
    int centroid_x;

    // incrementally maintained centroid of stones 
    int centroid_y;
    Quad_Count qc = new Quad_Count();
    int duplicate_position_count;
    public double so_weight = 1.0;

    // constructor 
    Player_Info(Loa_Board bd, Stone_Type cl)
    {
        board = bd;
        color = cl;
        append_current_move = new List_Moves(color);
        pos = new Point[bd.max_stones];
        group_idx = new int[bd.max_stones];
        Initialize();
    }

    public String getName()
    {
        return (color.getName());
    }

    double Sum_Offcenter()
    {
        {
            int sumsq = 0;

            /* not over, simple evaluation function */
            int cx = (centroid_x << 8) / n_stones;
            int cy = (centroid_y << 8) / n_stones;

            for (int i = 0; i < n_stones; i++)
            {
                int x = (G.Left(pos[i])) << 8;
                int y = (G.Top(pos[i])) << 8;
                int dx = (x - cx);
                int dy = (y - cy);
                sumsq += ((dx * dx) + (dy * dy));
            }

            {	double dis = Math.sqrt(sumsq);
                double val = (so_weight / dis);

                return (val);
            }
        }
    }

    public double Static_Eval_Current()
    {
        int cmoven = board.current_move_number;
        double eval = 0.0;

        /* game over? */
        if(board.setup==Setup_Type.LOAP)
        {
	        if(board.loaps_gameover())
	    	{ if(loaps_points > other_player.loaps_points)
	    	{eval = SEARCH_WIN + (SEARCH_WIN / cmoven);
	    	}
	    	else
	    	{eval =  -(SEARCH_WIN + (SEARCH_WIN / cmoven));
	    	}
	    	}
	    	else
	    	{
	    	    eval = (loaps_points - other_player.loaps_points)*LOAPS_WEIGHT
	        	+
	        	(static_evaluator.Evaluate(this) +
	                (DUPLICATE_WEIGHT * duplicate_position_count)) -
	                (static_evaluator.Evaluate(other_player) +
	                (2 * DUPLICATE_WEIGHT * duplicate_position_count));
	        }
        }
        else
        {
        if (Exactly_One_Group())
        {
            eval = SEARCH_WIN + (SEARCH_WIN / cmoven);
        }
        else if (other_player.Exactly_One_Group()) /* suicide move? */
        {
            eval = -(SEARCH_WIN + (SEARCH_WIN / cmoven));
        }
        else
        {
            eval = (static_evaluator.Evaluate(this) +
                (DUPLICATE_WEIGHT * duplicate_position_count)) -
                (static_evaluator.Evaluate(other_player) +
                (2 * DUPLICATE_WEIGHT * duplicate_position_count));
        }
        }
        return (eval);
    }
    
    double Evaluate_Position()
    {	
        int cmoven = board.current_move_number;
    	double eval=0.0;
        if(board.setup==Setup_Type.LOAP)
        {
	        if(board.loaps_gameover())
	    	{ if(loaps_points > other_player.loaps_points)
	    	{eval = SEARCH_WIN + (SEARCH_WIN / cmoven);
	    	}
	    	else
	    	{eval =  -(SEARCH_WIN + (SEARCH_WIN / cmoven));
	    	}
	    	}
	    	else
	    	{
	    	    eval = (loaps_points - other_player.loaps_points)*LOAPS_WEIGHT
	        	+
	        	(static_evaluator.Evaluate(this) +
	                (DUPLICATE_WEIGHT * duplicate_position_count)) -
	                (static_evaluator.Evaluate(other_player) +
	                (2 * DUPLICATE_WEIGHT * duplicate_position_count));
	        }
	    }
        else
        {
 
            /* game over? */
            if (Exactly_One_Group())
            {
                eval = SEARCH_WIN + (SEARCH_WIN / cmoven);
            }
            else if (other_player.Exactly_One_Group()) /* suicide move? */
            {
                eval = -(SEARCH_WIN + (SEARCH_WIN / cmoven));
             }
            else
            {
                eval = (static_evaluator.Evaluate(this) +
                    (DUPLICATE_WEIGHT * duplicate_position_count)) -
                    (static_evaluator.Evaluate(other_player) +
                    (2 * DUPLICATE_WEIGHT * duplicate_position_count));

            }
        }	
        return(eval);
    }

    double Evaluate_One_Move(Move_Spec mv)
    {	int cmoven = board.current_move_number;
        board.Make_Move(mv); //temporary move
        double eval = Evaluate_Position();
        board.Unmake_Move(mv);
        mv.move_number = cmoven;
        mv.setEvaluation(eval);
        mv.scored_p = true;
        //board.c.debug("Eval " + U.DisplayString(mv,board.size) + " = " + eval);
        return (eval);
    }

    synchronized void delay(int n)
    {
        try
        {
            wait(n);
        }
        catch (InterruptedException e)
        {
        }

    }

    List_Moves List_Of_Legal_Moves()
    {
        append_current_move.removeAllElements();
        board.Map_Over_Moves(append_current_move);

        return (append_current_move);
    }

    // re initialize 
    void Initialize()
    {
        n_stones = 0;
        n_groups = 0;
        centroid_x = 0;
        centroid_y = 0;
        loaps_points=0;	// loaps points

        duplicate_position_count = 0;
        state = Cache_State.Invalid;
        qc.Initialize();
    }

    void Set_Other_Player(Player_Info other)
    {
        other_player = other;
    }

    // player_info *make_player_info(loa_board *board,stonetype color);
    // void free_player_info(player_info *pl);
    // void invalidate_group_cache(player_info *neww);
    // void validate_group_cache(player_info *neww);
    void Swap_Into_Position(int x, int y, int position)
    {
        for (int i = position; i < n_stones; i++)
        {
            Point p = pos[i];

            if ((G.Left(p) == x) && (G.Top(p) == y))
            {
                if (i != position)
                {
                    pos[i] = pos[position];
                    pos[position] = p;
                }

                return;
            }
        }

        G.Assert(false, "Position " + x + "," + y + " not found");
    }
    private void loaps_score(int x,int y,int dir)
    {	int bs=board.size;
		int bs2=bs/2;
	    int edge = bs-2;
	       
		if((x==bs2)&&(y==bs2)) { loaps_points+=LOAPS_CENTER_BONUS*dir; }
		else if(((x==edge)&&((y==1)||(y==edge)))
    			|| ((x==1)&&((y==1)||(y==edge))))	{ loaps_points+=LOAPS_CORNER_BONUS*dir; }
    }
    void loaps_score_connect(int n)
    { if(Exactly_One_Group()) { loaps_points+=n; }
    }
    public void Add_Stone(int x, int y,boolean loap_score)
    {
        int oldidx = n_stones++;
        pos[oldidx] = new Point(x, y);
        centroid_x += x;
        centroid_y += y;
        if(loap_score && (board.setup==Setup_Type.LOAP)) 
        {	loaps_score(x,y,1);
        }
        /* if(debug>1)        */ board.Update_Quads(qc, color, x, y);

        state = Cache_State.Invalid;
    }

    public void Remove_Stone(int x, int y,boolean loap_score)
    {
        int oldidx = --n_stones;
        centroid_x -= x;
        centroid_y -= y;
        if(loap_score && (board.setup==Setup_Type.LOAP)) 
        {	loaps_score(x,y,-1);
        }
        for (int i = 0; i <= oldidx; i++)
        {
            Point p = pos[i];

            if ((G.Left(p) == x) && (G.Top(p) == y))
            {
                pos[i] = pos[oldidx];

                /* if(debug>1) */ board.Update_Quads(qc, color, x, y);
                state = Cache_State.Invalid;

                return;
            }
        }
        G.Assert(false, "remove " + x + "," + y + " failed");
    }

    boolean Exactly_One_Group()
    {
        boolean result = false;

        if ( /* debug>2 && */
            qc.Euler > 1)
        {
            result = false;
        }
        else if (state == Cache_State.Valid)
        {
            if (n_groups == 1)
            {
                result = true;
            }
        }
        else if (state == Cache_State.First_Group_Valid)
        {
            result = false;
        }
        else if (state == Cache_State.Invalid)
        {
            /* if(debug==0) { validate_group_cache(pl);} */
            n_groups = 0;

            if (n_stones == 0)
            { /* can be zero if in a custom setup */
                state = Cache_State.Valid;
            }
            else
            {
                board.sweep_counter++;

                {
                    int seed_x = G.Left(pos[0]);
                    int seed_y = G.Top(pos[0]);
                    int new_idx = board.Mark_Stones_In_Group(this, seed_x,
                            seed_y, 0);
                    int gidx = n_groups++;
                    group_idx[gidx] = new_idx;

                    if (new_idx == n_stones)
                    {
                        state = Cache_State.Valid;
                        result = true;
                    }
                    else
                    {
                        result = false;
                        state = Cache_State.First_Group_Valid;
                    }
                }
            }
        }
        else
        {
            G.Assert(false, "State " + state + " Not handled");
        }

        return (result);
    }

    // int n_stones(player_info *pl);
    // int n_groups(player_info *pl);
    // stonetype player_color(player_info *pl);
    // void set_other_player(player_info *pl,player_info *pl2);
    // player_info *other_player(player_info *pl);
    // list *list_of_legal_moves(player_info *pl);
    // double evaluate_one_move(loa_board *bd,player_info *pl,move_spec *mv);
}
