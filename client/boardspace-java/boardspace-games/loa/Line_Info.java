/* copyright notice */package loa;

import lib.NamedObject;


public class Line_Info extends NamedObject implements UIC
{
    public Loa_Board board;

    // our associated board 
    private int initial_x;

    // x coord for first position on line 
    private int initial_y;

    // y coord for first stone on line 
    private int dx;

    // delta x to next square 
    private int dy;

    // delta y to next square 
    int n_stones;

    // number of stones on the line 
    private Move_Spec[] moves;

    // legal moves on line 
    private int n_moves;

    // howmany legal moves 
    private Cache_State state;

    // valid? 
    private Line_Info(Loa_Board b, String Name, int len, int fx, int fy,
        int ldx, int ldy)
    {
        super(Name);
        board = b;
        this.initial_x = fx;
        this.initial_y = fy;
        this.dx = ldx;
        this.dy = ldy;
        this.moves = new Move_Spec[len];

        for (int i = 0; i < len; i++)
        {
            moves[i] = new Move_Spec();
        }
    }

    public void Invalidate_Move_Cache()
    {
        state = Cache_State.Invalid;
    }

    public static void Initialize(Line_Info[] line)
    {
        for (int i = 0; i < line.length; i++)
        {
            line[i].n_stones = 0;
            line[i].state = Cache_State.Invalid;
            line[i].n_moves = 0;
        }
    }

    // make a line-info array for a horizontal or vertical line 
    static Line_Info[] Make_Ortho_Line_Info(Loa_Board bd, String name,
        int size, int ix, int iy, int dx, int dy)
    {
        Line_Info[] newl = new Line_Info[size];

        for (int i = 0, ixx = ix, iyy = iy; i < size;
                i++, ixx = ixx + dy, iyy = iyy + dx)
        {
            newl[i] = new Line_Info(bd, name, size, ixx, iyy, dx, dy);
        }

        Initialize(newl);

        return (newl);
    }

    // make a line info array for a diagonal line 
    static Line_Info[] Make_Diagonal_Line_Info(Loa_Board bd, String name,
        int size, int ix, int iy, int dx, int dy)
    {
        int dsize = (size + size) - 1; // diagonal size
        int ex;
        int ey;
        int eix;
        int eiy;
        int dix;
        int diy;
        Line_Info[] newl = new Line_Info[dsize];

        if ((ix == 0) && (iy == 0))
        {
            /* construct origins along the top and right */
            ex = size - 1;
            ey = size - 1;
            dix = 1;
            diy = 0;
            eix = 0;
            eiy = -1;
        }
        else
        { /* contstruct origins along the left and top */
            ex = size - 1;
            ey = 0;
            dix = 0;
            diy = -1;
            eix = -1;
            eiy = 0;
        }

        for (int i = 0, ip = 1; i < size; i++, ip++)
        {
            newl[i] = new Line_Info(bd, name, ip, ix + (i * dix),
                    iy + (i * diy), dx, dy);

            if (((size & 1) == 0) || (i < (size - 1)))
            {
                int idx = dsize - ip;
                newl[idx] = new Line_Info(bd, name, ip, ex + (i * eix),
                        ey + (i * eiy), dx, dy);
            }
        }

        Initialize(newl);

        return (newl);
    }

    void Scan_For_Moves()
    {
        n_moves = 0;

        if (n_stones > 0)
        {
            Move_Spec[] specs = moves;
            int linelen = specs.length;
            int linelim = linelen - n_stones;
            int xp = initial_x;
            int yp = initial_y;
            int xl = xp + (dx * (linelen - 1));
            int yl = yp + (dy * (linelen - 1));

            for (int offset = 0, plus_stones_seen = 0, minus_stones_seen = 0;
                    offset < linelim; offset++)
            {
                {
                    Move_Spec legal = specs[n_moves];
                    legal.x = xp;
                    legal.y = yp;
                    legal.op = n_stones;
                    legal.dx = dx;
                    legal.dy = dy;

                    if ((plus_stones_seen < n_stones) &&
                            (board.Legal_Move_P(legal) == Move_Reason.Ok))
                    {
                        n_moves++;
                    }

                    if (legal.color != Stone_Type.Empty)
                    {
                        plus_stones_seen++;
                    }
                }


                {
                    Move_Spec legal = specs[n_moves];
                    legal.x = xl;
                    legal.y = yl;
                    legal.dx = -dx;
                    legal.dy = -dy;
                    legal.op = n_stones;

                    if ((minus_stones_seen < n_stones) &&
                            (board.Legal_Move_P(legal) == Move_Reason.Ok))
                    {
                        n_moves++;
                    }

                    if (legal.color != Stone_Type.Empty)
                    {
                        minus_stones_seen++;
                    }
                }

                xp += dx;
                yp += dy;
                xl -= dx;
                yl -= dy;
            }
        }

        state = Cache_State.Valid;
    }

    void Change_Count(int dif)
    {
        n_stones += dif;
        n_moves = -1;
        state = Cache_State.Invalid;
    }

    void Map_Over_Moves(Move_Mapper res)
    {
        if (state != Cache_State.Valid)
        {
            Scan_For_Moves();
        }

        for (int i = 0; i < n_moves; i++)
        {
            res.Move_Map(this, moves[i]);
        }
    }
}

/* class Line_Info  */
