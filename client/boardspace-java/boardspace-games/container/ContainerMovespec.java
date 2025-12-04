/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package container;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lib.ExtendedHashtable;

public class ContainerMovespec extends commonMPMove implements ContainerConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
	static ExtendedHashtable E = new ExtendedHashtable(true);
	static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        	"Move",MOVE_FROM_TO,
        	"PickC",MOVE_PICKC,
        	"MoveC",MOVEC_FROM_TO,
			"Fund",MOVE_FUND,
			"Decline",MOVE_DECLINE,
			"Bid",MOVE_BID,
		
			"EFund",MOVE_EPHEMERAL_FUND,
			"EDecline",MOVE_EPHEMERAL_DECLINE,
			"EBid",MOVE_EPHEMERAL_AUCTION_BID,
			"ELoanBid",MOVE_EPHEMERAL_LOAN_BID,
		
			"Accept",MOVE_ACCEPT,
			"Buy",MOVE_BUY,
			"AcceptLoan",MOVE_ACCEPT_LOAN,
			"DeclineLoan",MOVE_DECLINE_LOAN,
		
			"BLACK",CONTAINER_BLACK,
			"WHITE",CONTAINER_WHITE,
			"TAN",CONTAINER_TAN,
			"BROWN",CONTAINER_BROWN,
			"ORANGE",CONTAINER_ORANGE,
			"GOLD",CONTAINER_GOLD,

			"Sea",ContainerId.AtSeaLocation.IID(),
			"Dock",ContainerId.AtDockLocation.IID(),
			"ShipGoods",ContainerId.ShipGoodsLocation.IID(),
			"IslandGoods",ContainerId.IslandGoodsLocation.IID(),		
			"FactoryGoods",ContainerId.FactoryGoodsLocation.IID(),
			"WarehouseGoods",ContainerId.WarehouseGoodsLocation.IID(),
			"Machine",ContainerId.MachineLocation.IID(),		// cells containing unsole machines
			"Warehouse",ContainerId.WarehouseLocation.IID(),	// cells containing unsole warehouses
	    	"Container",ContainerId.ContainerLocation.IID(),	// cells containing unsold containers
	    	"Auction", ContainerId.AuctionLocation.IID(),			// active auction slot
	    	"Park",ContainerId.AtIslandParkingLocation.IID(),		// parking lot at the island
	    	"Loan",ContainerId.LoanLocation.IID());
	    
		E.putInt("Sea",ContainerId.AtSeaLocation.IID());
		E.putInt("Dock",ContainerId.AtDockLocation.IID());
		E.putInt("SF",ContainerId.ShipGoodsLocation.IID());
		E.putInt("IG",ContainerId.IslandGoodsLocation.IID());		
		E.putInt("Factory",ContainerId.FactoryGoodsLocation.IID());
		E.putInt("Warehouse",ContainerId.WarehouseGoodsLocation.IID());
		E.putInt("M",ContainerId.MachineLocation.IID());	// cells containing unsole machines
		E.putInt("W",ContainerId.WarehouseLocation.IID());	// cells containing unsole warehouses
	    E.putInt("C",ContainerId.ContainerLocation.IID());	// cells containing unsold containers
	    E.putInt("Auction", ContainerId.AuctionLocation.IID());				// active auction slot
	    E.putInt("Island",ContainerId.AtIslandParkingLocation.IID());		// parking lot at the island
	    E.putInt("L",ContainerId.LoanLocation.IID());

   }

    
    ContainerId source; // where from
	char from_col; //for from-to moves, the source column
	long from_row; // for from-to moves, the source row
	int container;	// container color code, after execution containins color | (position<<12)
	ContainerId dest;	// where to
    char to_col; // for from-to moves, the destination column
    long to_row; // for from-to moves, the destination row
    int undoInfo;	// the state of the move before state, for UNDO
    int undoInfo2;	// more undo info
    int extra_search_depth = 0;
    int eval_clock = 0;
    
    public ContainerMovespec()
    {
    } // default constructor

    /* constructor */
    public ContainerMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public ContainerMovespec(int opc,int p)
    {	op = opc;
    	player = p;
    }
    // constructor for bid moves
    public ContainerMovespec(int opc,int bid,int p)
    {	op = opc;
    	to_row = bid;
    	player = p;
    }

    // for the robot
    public ContainerMovespec(ContainerId sour,char fc,int fr,ContainerId des,char tc,int tr,int pla)
    {	op = MOVE_FROM_TO;
    	source = sour;
    	player = pla;
    	from_col = fc;
    	dest = des;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    	//if((source==PalaceLocation) && (from_col=='A') && (from_row==0) && (to_col=='B') && (to_row==12))
    	//{G.print("here "+this);
    	//}
    }
    // for the robot
    public ContainerMovespec(ContainerId sour,char fc,int fr,int contain,ContainerId des,char tc,int tr,int pla)
    {	G.Assert((contain>=CONTAINER_OFFSET) && (contain<=(CONTAINER_OFFSET+CONTAINER_COLORS)),"is a container code");
    	op = MOVEC_FROM_TO;
    	source = sour;
    	container = contain;
    	player = pla;
    	from_col = fc;
    	dest = des;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    	//if((source==PalaceLocation) && (from_col=='A') && (from_row==0) && (to_col=='B') && (to_row==12))
    	//{G.print("here "+this);
    	//}
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	ContainerMovespec other = (ContainerMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (undoInfo == other.undoInfo)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (container == other.container)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ContainerMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.dest = dest;
        to.undoInfo = undoInfo;
        to.source = source;
        to.container = container;
        to.extra_search_depth = extra_search_depth;
        to.eval_clock = eval_clock;
    }

    public commonMove Copy(commonMove to)
    {
    	ContainerMovespec yto = (to == null) ? new ContainerMovespec() : (ContainerMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    
    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
     * */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        
        case MOVEC_FROM_TO:
            source = ContainerId.get(D.getInt(msg.nextToken()));
            from_col = msg.charToken();
            from_row = msg.intToken();
            container = D.getInt(msg.nextToken());
            dest = ContainerId.get(D.getInt(msg.nextToken()));
            to_col = msg.charToken();
            to_row = msg.intToken();
            break;
        case MOVE_BUY:
        case MOVE_ACCEPT:
        case MOVE_ACCEPT_LOAN:
        case MOVE_BID:
        	source = ContainerId.CashLocation;
        	to_row = msg.intToken();
        	break;
        case MOVE_FROM_TO:
            source = ContainerId.get(D.getInt(msg.nextToken()));
            from_col = msg.charToken();
            from_row = msg.intToken();
  			//$FALL-THROUGH$
		case MOVE_DROP:
            dest = ContainerId.get(D.getInt(msg.nextToken()));
            to_col = msg.charToken();
            to_row = msg.intToken();
            break;

        case MOVE_PICK:
            source = ContainerId.get(D.getInt(msg.nextToken()));
            from_col = msg.charToken();
            from_row = msg.intToken();
            break;
        case MOVE_PICKC:
            source = ContainerId.get(D.getInt(msg.nextToken()));
            from_col = msg.charToken();
            from_row = msg.intToken();
            container = D.getInt(msg.nextToken());
            break;
           
        case MOVE_EPHEMERAL_LOAN_BID:
        case MOVE_EPHEMERAL_AUCTION_BID:
        	source = ContainerId.CashLocation;
        	to_row = msg.intToken();
			//$FALL-THROUGH$
		case MOVE_EPHEMERAL_FUND:
        case MOVE_EPHEMERAL_DECLINE:
        case MOVE_START:
          player = D.getInt(msg.nextToken());

            break; 
       default:
            break;
        }
    }
    public Text repriceMove(commonCanvas v)
    {	boolean same = sameAction();
		switch(source)
		{
		default: throw G.Error("not expecting %s",this);
		case WarehouseGoodsLocation:
			{
			Text primary = TextGlyph.create("xx",ContainerChip.getContainer(container&0xfff),v,new double[]{1.0,1.0,0,-0.3});
			if(same) { return(primary); }
			return(TextChunk.join(
					TextChunk.create("Reprice "),
					TextGlyph.create("xx",ContainerChip.getWarehouse(),v,new double[]{1.0,1.2,0,-0.2}),
					primary));
			}
		case FactoryGoodsLocation: 
			Text primary = TextGlyph.create("xx",ContainerChip.getContainer(container&0xfff),v,new double[]{1.0,1.0,0,-0.3});
			if(same) { return(primary); }
			return(TextChunk.join(
					TextChunk.create("Reprice "),
					TextGlyph.create("xx",ContainerChip.getMachine(0),v,new double[]{1.0,1.2,0,-0.2}),
					primary));
					
		}

    }
    public Text buyWarehouse(commonCanvas v)
    {
    	return(TextChunk.join(
				TextChunk.create("Buy"),
				TextGlyph.create("xx",ContainerChip.getWarehouse(),v,new double[]{1.0,1.2,0,-0.2})));
    }
    public Text buyMachine(commonCanvas v)
    {
    	return(TextChunk.join(
				TextChunk.create("Buy"),
				TextGlyph.create("xx",ContainerChip.getMachine((int)(from_row&0xfff)),v,new double[]{1.0,1.1,-0.1,-0.2})));
    }
    public Text produce(commonCanvas v)
    {
    	return(TextChunk.join(
				TextChunk.create(sameAction()?"":"Produce"),
				TextGlyph.create("xx",ContainerChip.getContainer((int)(from_row&0xfff)),v,new double[]{1.0,1.0,0,-0.5})));
				
    }
    public Text buyOrLoad(commonCanvas v,String msg)
    {	return(TextChunk.join(
    		TextChunk.create(sameAction()?"":msg),
    		TextGlyph.create("xx",ContainerChip.getContainer(container&0xfff),v,new double[]{1.0,1.0,0,-0.3})
    	));
    }
    public Text shipGlyph(commonCanvas v,int pl)
    {	int color = v.getBoard().getColorMap()[pl];
    	return(TextGlyph.create("xxxx",ContainerChip.getShip(color),v,new double[]{1.0,1.1,-0.0,-0.15}));
    }
    
    static ContainerMovespec prevAction = null;
    boolean sameAction()
    {
    	boolean v = (prevAction!=null)&&(prevAction.op==op);
    	prevAction = this;
    	return(v);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {


        case MOVE_DROP:
        	switch(dest)
        	{
        	case AtSeaLocation:
        	case AtIslandParkingLocation:
        	case AuctionLocation:
        		return(TextChunk.create(E.findUnique(dest.IID())));
        	case AtDockLocation:
        		return(TextChunk.join(shipGlyph(v,to_col-'A'),TextChunk.create("Dock")));
        	case WarehouseLocation:
        	case MachineLocation:
        		return(TextChunk.create("#"+(to_row+1)));
        	case ContainerLocation:
        		return(TextChunk.create("Return "+D.findUnique(container&0xfff)));
        	case FactoryGoodsLocation:
        	case WarehouseGoodsLocation:
        		if(source==dest) { return(repriceMove(v)); }
        		if(dest==ContainerId.WarehouseGoodsLocation) { return(buyOrLoad(v,"Buy")); }
        		if(source==ContainerId.FactoryGoodsLocation) { return(buyOrLoad(v,"Load")); }
        		return(TextChunk.create(""));
        	case ShipGoodsLocation:
        		return(buyOrLoad(v,"Load"));
        	case LoanLocation: 
        		return(TextChunk.create(""));
        	default:
        		return(TextChunk.create(E.findUnique(dest.IID())+" "+to_col+to_row));
        	}
        case MOVE_PICKC:
        	switch(source)
        	{
        	case WarehouseGoodsLocation:
        	case FactoryGoodsLocation:
        		return(TextChunk.create(""));
        	case ContainerLocation:
        		return(produce(v));
        	default:
        		return (TextChunk.create("C "+D.findUnique(container&0xfff)));
        	}
        case MOVE_PICK:
            switch(source)
            {
            case LoanLocation:
            	if(from_col=='@') {return(TextChunk.create("Request Loan"));}
            	else { return(TextChunk.create("Repay Loan")); }
        	case MachineLocation:
        		return(buyMachine(v));
        	case WarehouseLocation:
        		return(buyWarehouse(v));
        	case ContainerLocation:
        		return(produce(v));
        	case AtSeaLocation:
        	case AtIslandParkingLocation:
        	case AtDockLocation:
        	case AuctionLocation:
        		return(TextChunk.join(shipGlyph(v,player),TextChunk.create("to ")));
        	default: ;
            	
            }
            return (TextChunk.create(D.findUnique(source.IID())+" "+from_col+" "+from_row));

		case MOVEC_FROM_TO:
			
			if(source==dest)
			{	// reprice
				return(repriceMove(v));
			}
			else
			{
			if(dest==ContainerId.ContainerLocation)
			{ return(TextChunk.create("Trade "+D.findUnique(container&0xfff)));
			}
			else {
			switch(source)
			{
			case WarehouseGoodsLocation:	return(buyOrLoad(v,"Load"));
			case FactoryGoodsLocation: return(buyOrLoad(v,"Buy"));
			default:
				break;
			}}}
			return(TextChunk.create(D.findUnique(op) + " " +E.findUnique(source.IID())
					+ " " + from_col+from_row
					+ " " + D.findUnique(container&0xfff)
					+ " - " + E.findUnique(dest.IID())
					+ " " + to_col + to_row));

        case MOVE_FROM_TO:
        	switch(source)
        	{
        	case LoanLocation:
            	if(from_col=='@') {return(TextChunk.create("Request Loan"));}
            	else { return(TextChunk.create("Repay Loan")); }
            case MachineLocation:
        		return(buyMachine(v));

            case WarehouseLocation:
        		return(buyWarehouse(v));
        		
        	case ContainerLocation:
        		return(produce(v));
        	case AtSeaLocation:
        	case AtIslandParkingLocation:
        	case AtDockLocation:
        	case AuctionLocation:
        	{	Text col = (dest==ContainerId.AtDockLocation) 
        						? shipGlyph(v,to_col-'A')
        						: TextChunk.create("");
        		return(TextChunk.join(
        				shipGlyph(v,player),
        				TextChunk.create("to"),
        				col,
        				TextChunk.create(E.findUnique(dest.IID()))));
        	}
        	default:	;
        	}
        	return(TextChunk.create(E.findUnique(source.IID())+" "+from_col+from_row+"-"+E.findUnique(dest.IID())+" "+to_col + to_row));
        case MOVE_DONE:
        	sameAction();
            return (TextChunk.create(""));
        case MOVE_ACCEPT:
        	return(TextChunk.join(TextChunk.create("Accept $"+((from_row>>1)&0x3ff)+" from "),
        			shipGlyph(v,(int)(to_row&0xfff))));
        case MOVE_BUY:
        	return(TextChunk.create("Buy for "+to_row));
        case MOVE_BID:
        case MOVE_EPHEMERAL_AUCTION_BID:
        case MOVE_EPHEMERAL_LOAN_BID:
        	return(TextChunk.create(D.findUnique(op)+" "+to_row));
        case MOVE_DECLINE_LOAN:
        	return(TextChunk.create("Decline Loan"));
        case MOVE_ACCEPT_LOAN:
        	return(TextChunk.create("Accept Loan"));
        default:
            return (TextChunk.create(D.findUniqueTrans(op)));

       }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
 
		case MOVEC_FROM_TO:
			return(opname +D.findUnique(source.IID())
					+ " " + from_col+" "+from_row
					+ " " + D.findUnique(container&0xfff)
					+ " " + D.findUnique(dest.IID())
					+ " " + to_col + " " + to_row);

		case MOVE_FROM_TO:
			return(opname +D.findUnique(source.IID())
					+ " " + from_col+" "+from_row
					+ " " + D.findUnique(dest.IID())
					+ " " + to_col + " " + to_row);

	    case MOVE_DROP:
            return (opname+D.findUnique(dest.IID())+ " " + to_col+" "+to_row);

	    case MOVE_PICK:
            return (opname+D.findUnique(source.IID())+ " " + from_col+" "+from_row);

	    case MOVE_PICKC:
            return (opname+D.findUnique(source.IID())+ " " + from_col+" "+from_row+" "
            		+D.findUnique(container&0xfff));

        case MOVE_EPHEMERAL_DECLINE:
        case MOVE_EPHEMERAL_FUND:
        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_EPHEMERAL_AUCTION_BID:
        case MOVE_EPHEMERAL_LOAN_BID:
        	return (opname+to_row+" P"+player);
        case MOVE_ACCEPT_LOAN:
        case MOVE_BID:
        case MOVE_ACCEPT:
        case MOVE_BUY:
        	return( opname+to_row);
        default:
            return (opname);
        }
    }

}
