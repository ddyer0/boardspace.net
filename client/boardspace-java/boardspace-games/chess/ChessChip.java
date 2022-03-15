package chess;

import lib.Graphics;
import lib.ImageLoader;
import bridge.Config;
import lib.DrawableImageStack;
import lib.Random;
import online.game.chip;
import online.common.exCanvas;
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class ChessChip extends chip<ChessChip> implements ChessConstants,Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack allTiles = new DrawableImageStack();
	private static boolean imagesLoaded = false;

	public ChessId color;
	public ChessPiece piece;

	private int chipIndex;
	public int chipNumber() { return(chipIndex); }
	public static ChessChip getChipNumber(int id)
	{	return((ChessChip)allChips.elementAt(id));
	}

	// constructor for chips not expected to be part of the UI
	private ChessChip(String na,double scl[])
	{	this(na,scl,allChips);
	}
	// constructor for chips not expected to be part of the UI
	private ChessChip(String na,double scl[],DrawableImageStack stack)
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		stack.push(this);
	}

	public double getChipRotation(exCanvas canvas)
	{	int chips = canvas.getAltChipset();
		double rotation = 0;
		// alternate chipsets for chess, 1= white upside down 2=black upside down
		// this coding comes from getAltChipSet()
		switch(chips)
		{
		default:
				break;
		case 1:	if(color==ChessId.White_Chip_Pool) { rotation=Math.PI; }
				break;
		case 2: if(color==ChessId.Black_Chip_Pool) { rotation=Math.PI; }
				break;
		}
		return(rotation);
	}
	// constructor for chips expected to be part of the UI
	private ChessChip(String na,ChessId uid,ChessPiece p,double scl[])
	{	this(na,scl);
		color = uid;
		piece = p;
	}

	
	public String toString()
	{	return("<"+ color+piece+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(color==null?"":color.shortName+piece.toString()); 
	}

	static private ChessChip tiles[] =
		{
		new ChessChip("light-tile",new double[]{0.5,0.6,1.0},allTiles),	
    	new ChessChip("dark-tile",new double[]{0.5,0.6,1.0},allTiles),
		};
	
	public static ChessChip whitePawn = new ChessChip("white-pawn",
			ChessId.White_Chip_Pool,ChessPiece.Pawn,
			new double []{0.521,0.35,1.0});
	public static ChessChip whiteRook = new ChessChip("white-rook",
			ChessId.White_Chip_Pool,ChessPiece.Rook,
			new double []{0.464,0.494,1.0});
	public static ChessChip whiteKnight = new ChessChip("white-knight",
			ChessId.White_Chip_Pool,ChessPiece.Knight,
			new double []{0.516,0.5,1.0});
	public static ChessChip whiteBishop = new ChessChip("white-bishop",
			ChessId.White_Chip_Pool,ChessPiece.Bishop,
			new double []{0.527,0.545,1.0});
	public static ChessChip whiteQueen = new ChessChip("white-queen",
			ChessId.White_Chip_Pool,ChessPiece.Queen,
			new double []{0.505,0.508,0.978});
	public static ChessChip whiteKing = new ChessChip("white-king",
			ChessId.White_Chip_Pool,ChessPiece.King,
			new double []{0.505,0.49,0.891});

	public static ChessChip blackPawn = new ChessChip("black-pawn",
			ChessId.Black_Chip_Pool,ChessPiece.Pawn,
			new double []{0.521,0.45,1.0});
	public static ChessChip blackRook = new ChessChip("black-rook",
			ChessId.Black_Chip_Pool,ChessPiece.Rook,
			new double []{0.464,0.494,1.0});
	public static ChessChip blackKnight = new ChessChip("black-knight",
			ChessId.Black_Chip_Pool,ChessPiece.Knight,
			new double []{0.516,0.5,1.0});
	public static ChessChip blackBishop = new ChessChip("black-bishop",
			ChessId.Black_Chip_Pool,ChessPiece.Bishop,
			new double []{0.527,0.545,1.0});
	public static ChessChip blackQueen = new ChessChip("black-queen",
			ChessId.Black_Chip_Pool,ChessPiece.Queen,
			new double []{0.505,0.508,0.978});
	public static ChessChip blackKing = new ChessChip("black-king",
			ChessId.Black_Chip_Pool,ChessPiece.King,
			new double []{0.505,0.49,0.891});
	
	// pieces for Ultima
	public static ChessChip whiteCustodialPawn = new ChessChip("white-pawn",
			ChessId.White_Chip_Pool,ChessPiece.CustodialPawn,
			new double []{0.521,0.45,1.0});
	public static ChessChip whiteLongLeaper = new ChessChip("white-knight",
			ChessId.White_Chip_Pool,ChessPiece.LongLeaper,
			new double []{0.516,0.5,1.0});
	public static ChessChip whiteChamelion = new ChessChip("white-bishop",
			ChessId.White_Chip_Pool,ChessPiece.Chamelion,
			new double []{0.527,0.545,1.0});
	public static ChessChip whiteWithdrawer = new ChessChip("white-queen",
			ChessId.White_Chip_Pool,ChessPiece.Withdrawer,
			new double []{0.505,0.508,0.978});
	public static ChessChip whiteImmobilizer = new ChessChip("white-immobilizer",
			ChessId.White_Chip_Pool,ChessPiece.Immobilizer,
			new double []{0.427,0.49,1.0});
	public static ChessChip whiteCoordinator = new ChessChip("white-rook",
			ChessId.White_Chip_Pool,ChessPiece.Coordinator,
			new double []{0.427,0.49,1.0});
	public static ChessChip whiteUltimaKing = new ChessChip("white-king",
			ChessId.White_Chip_Pool,ChessPiece.UltimaKing,
			new double []{0.505,0.49,0.891});

	public static ChessChip blackCustodialPawn = new ChessChip("black-pawn",
			ChessId.Black_Chip_Pool,ChessPiece.CustodialPawn,
			new double []{0.521,0.45,1.0});
	public static ChessChip blackLongLeaper = new ChessChip("black-knight",
			ChessId.Black_Chip_Pool,ChessPiece.LongLeaper,
			new double []{0.516,0.5,1.0});
	public static ChessChip blackChamelion = new ChessChip("black-bishop",
			ChessId.Black_Chip_Pool,ChessPiece.Chamelion,
			new double []{0.527,0.545,1.0});
	public static ChessChip blackWithdrawer = new ChessChip("black-queen",
			ChessId.Black_Chip_Pool,ChessPiece.Withdrawer,
			new double []{0.505,0.508,0.978});
	public static ChessChip blackImmobilizer = new ChessChip("black-immobilizer",
			ChessId.Black_Chip_Pool,ChessPiece.Immobilizer,
			new double []{0.496,0.49,1.0});
	public static ChessChip blackCoordinator = new ChessChip("black-rook",
			ChessId.Black_Chip_Pool,ChessPiece.Coordinator,
			new double []{0.496,0.49,1.0});
	public static ChessChip blackUltimaKing = new ChessChip("black-king",
			ChessId.Black_Chip_Pool,ChessPiece.UltimaKing,
			new double []{0.505,0.49,0.891});

	// pieces for chess promotions
	public static ChessChip whitePromotedPawn = new ChessChip("white-pawn",
			ChessId.White_Chip_Pool,ChessPiece.Pawn,
			new double []{0.521,0.304,1.3});
	public static ChessChip blackPromotedPawn = new ChessChip("black-pawn",
			ChessId.Black_Chip_Pool,ChessPiece.Pawn,
			new double []{0.521,0.304,1.3});
	
	static private ChessChip chips[] = 
		{
		whitePawn,
		whiteRook,
		whiteKnight,
		whiteBishop,
		whiteQueen,
		whiteKing,
		whiteCustodialPawn,
		whiteLongLeaper,
		whiteChamelion,
		whiteWithdrawer,
		whiteCoordinator,
		whiteImmobilizer,
		whiteUltimaKing,
		
		blackPawn,
		blackRook,
		blackKnight,
		blackBishop,
		blackQueen,
		blackKing,
		
		blackCustodialPawn,
		blackLongLeaper,
		blackChamelion,
		blackWithdrawer,
		blackImmobilizer,
		blackCoordinator,
		blackUltimaKing,
		};
	
	public static ChessChip whiteInit[] = { whiteRook,whiteKnight,whiteBishop,whiteQueen,whiteKing,whiteBishop,whiteKnight,whiteRook};
	public static ChessChip blackInit[] = { blackRook,blackKnight,blackBishop,blackQueen,blackKing,blackBishop,blackKnight,blackRook};
	public static char kingRookCol = 'H';
	public static ChessChip whiteUltimaInit[] = { whiteImmobilizer,whiteLongLeaper,whiteChamelion,whiteUltimaKing,whiteWithdrawer,whiteChamelion,whiteLongLeaper,whiteCoordinator};
	public static ChessChip blackUltimaInit0[] = { blackImmobilizer,blackLongLeaper,blackChamelion,blackUltimaKing,blackWithdrawer,blackChamelion,blackLongLeaper,blackCoordinator};
	public static ChessChip blackUltimaInit1[] = { blackCoordinator,blackLongLeaper,blackChamelion,blackWithdrawer,blackUltimaKing,blackChamelion,blackLongLeaper,blackImmobilizer};

	public static ChessChip getTile(int color)
	{	return(tiles[color]);
	}
	public static ChessChip getChip(int color)
	{	return(chips[color]);
	}
	public boolean isKing()
	{	return((piece==ChessPiece.King)||(piece==ChessPiece.UltimaKing));
	}
	
	//
	// alternate chipsets for playtable.  The normal presentation is ok for side by side play
	// slightly disconcerting for face to face play.  This supports two alternates, one
	// with white pieces inverted, one with pieces facing left and right
	//
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	
		drawRotatedChip(gc,canvas,getChipRotation(canvas),SQUARESIZE,xscale,cx,cy,label);
	}
	static double noscale[]= {0,0,1};
	public static ChessChip backgroundTile = new ChessChip( "background-tile-nomask",null,allTiles);
	public static ChessChip backgroundReviewTile = new ChessChip( "background-review-tile-nomask",null,allTiles);
	public static ChessChip standard = new ChessChip("standard",noscale);
	public static ChessChip ultima = new ChessChip("ultima",noscale);
	public static ChessChip chess960 = new ChessChip("chess960",noscale);
	public static ChessChip QueenChip[] = new ChessChip[]{whiteQueen,blackQueen};
	public static ChessChip PawnChip[] = new ChessChip[]{whitePawn,blackPawn};
	public static ChessChip PromotedPawnChip[] = new ChessChip[]{whitePromotedPawn,blackPromotedPawn};
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips)
						&& forcan.load_masked_images(StonesDir, allTiles);
		}
	}


}
