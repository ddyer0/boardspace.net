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

import java.util.*;

import lib.*;
import lib.Random;

class CellStack extends OStack<ContainerCell>
{
	public ContainerCell[] newComponentArray(int n) { return(new ContainerCell[n]); }
}

// class for container calculations
class CC 
{	static boolean second_shipment = false;
	static final int goalCardComplete[] = {10,10,6,4,2};	// values if all colors are present
	static final int goalCardIncomplete[] = {10,5,6,4,2};	// values if not all colors are present
	static final double ROBOT_ISLAND_GOODS_VALUES[] = {6.5, 3.0, 3.0, 4.0, 6.5};
	static final int STANDARD_COLORS = goalCardComplete.length;
	//
	// data for island scoring.  The container indexes and card indexes must agree with the artwork.
	//
   static final int black_container = 0;	// these are indexes into the islandGoods array
   static final int white_container = 1;
   static final int tan_container = 2;
   static final int brown_container = 3;
   static final int orange_container = 4;
   static final int gold_container = 5;
   static final int goalcardValues[][] = {
		    {black_container,brown_container,orange_container,tan_container,white_container},
		    {brown_container,orange_container,tan_container,white_container,black_container},
		    {orange_container,tan_container,white_container,black_container,brown_container},
		    {tan_container,white_container,black_container,brown_container,orange_container},
		    {white_container,black_container,brown_container,orange_container,tan_container}
		};
   static int realGoalCardValues[][] = null;
   static double preestGoalCardValues[][] = null;
   static final int NUMBER_OF_GOAL_CARDS = goalcardValues.length;


	// true if the islandGoods contain all 5 colors.
	static public boolean hasAllContainerColors(ContainerCell islandGoods[],ContainerChip addedGood)
	{for(int i=0;i<STANDARD_COLORS;i++) 
		{ if((islandGoods[i].topChip()==null) 
				&&  ((addedGood==null) || (addedGood.getContainerIndex()!=i)))
				{ return(false); 
				}
		}
	  return(true);
	}
	static public int maxContainerColors(ContainerCell islandGoods[],int indexes[])
	{
		return(maxContainerColors(islandGoods,indexes,-1));
	}
	// return the index of the container color that will be lost because it has the most
	// if addedGood is not null, add one of that type to the tally
	static public int maxContainerColors(ContainerCell islandGoods[],int indexes[],int addedIndex)
	{ int max = -1;
	  int maxval = -1;
	  int totalHeight = 0;
	  for(int i=0;i<islandGoods.length;i++) 	// include the gold container stack
	  	{ int fiveandten = indexes[1];
	  	  int index = (i==STANDARD_COLORS)?i:indexes[i];	
	  	  			// i is positional 10 5/10 6 4 2 gold 
	  	  			// index is the color with that price
	  	  int thish = islandGoods[index].height();
	  	  int h = thish+(index==addedIndex?1:0);
	  	  totalHeight += thish;
	  	  if(h>=maxval)
	  	  { if(h==maxval) 
	  	  		{ // ties
	  		      if((index==STANDARD_COLORS)||(max!=fiveandten)) { max=index; }// preserve index 1 as max except against gold
	  		    }		
	  	    else 
	  	    	{ max = index; 
	  	    	  maxval = h; 
	  	    	}
	  	  }
	  	}
	  if((totalHeight==1) && second_shipment) { max = -1; }
	  return(max);
	}
	// value of the gold on one cell
	static public int valueOfGold(ContainerCell c,int added)
	{ 	int h = c.height()+added;
		return(h*h*2);
	}
    // value of all the gold on some island
    static public int valueOfGold(ContainerCell islandGoods[])
    {	return(valueOfGold(islandGoods[gold_container],0));
    }
	static int scoreGold(ContainerCell islandGoods[],ContainerChip addedGood)
	{	int sum = 0;
		if(islandGoods.length>STANDARD_COLORS)
			{	// if there is gold
			ContainerCell c = islandGoods[gold_container];	// the gold bar is 
			int val = valueOfGold(c,addedGood==ContainerChip.GOLD_CONTAINER?1:0);
			c.value = val;
		    sum += val;
			}
		return(sum);
	}
	static public int islandGoodsValue(ContainerChip goalCard,ContainerCell islandGoods[])
	{
		return(islandGoodsValue(goalCard,islandGoods,null));
	}
	static public double[] reorderGoalValues(int cardindex,double values[])
	{	// values are the alternate values for 10,5/10,6,4,2 goods
		// result is those values, reorders according to the values on the goal card
		// so we can index by the continer type.
		double result[] = new double[values.length];
		int indexes[] = goalcardValues[cardindex];
		for(int i=0;i<indexes.length;i++) { result[indexes[i]]=values[i]; }
		return(result);
	}
	static public int[] reorderGoalValues(int cardindex,int values[])
	{	// values are the alternate values for 10,5/10,6,4,2 goods
		// result is those values, reorders according to the values on the goal card
		// so we can index by the continer type.
		int result[] = new int[values.length];
		int indexes[] = goalcardValues[cardindex];
		for(int i=0;i<indexes.length;i++) { result[indexes[i]]=values[i]; }
		return(result);
	}
	static {
		// generate the reverse lookup tables we need
		realGoalCardValues = new int[goalcardValues.length][];
		preestGoalCardValues = new double[goalcardValues.length][];
		for(int i=0;i<goalcardValues.length;i++)
		{
			realGoalCardValues[i]=reorderGoalValues(i,goalCardComplete);
			preestGoalCardValues[i]=reorderGoalValues(i,ROBOT_ISLAND_GOODS_VALUES);
		}
	}
	// return the value of the island goods according to the supplied card. If addedGood is supplied, add one to the total for that good
	static public int islandGoodsValue(ContainerChip goalCard,ContainerCell islandGoods[],ContainerChip addedGood)
	{	boolean hasAll = hasAllContainerColors(islandGoods,addedGood);
		int sum = 0;
		int totalgoods = 0;
		int containerValues[] = hasAll ? goalCardComplete : goalCardIncomplete;
		int colorIndexes[] = goalcardValues[goalCard.getCardIndex()];
		int addedIndex = (addedGood==null) ? -1 : addedGood.getContainerIndex();
		int dropIndex = maxContainerColors(islandGoods,colorIndexes,addedIndex);
		if(dropIndex>=0) { islandGoods[dropIndex].value = 0; }
		for(int i=0;i<STANDARD_COLORS;i++)
		{	int goodIndex = colorIndexes[i];
			ContainerCell c = islandGoods[goodIndex];
			int cheight = c.height();
			totalgoods += cheight;
			if(goodIndex!=dropIndex)
			{	int height = cheight;
				int val =  (height+((addedIndex==goodIndex)?1:0)) * containerValues[i];
				c.value = val;
				sum += val;
			}
			else { c.value = 0; }
		}
		if((addedGood!=null) && (totalgoods==0)) 
			{	// special logic for the first good on the island
			if(addedIndex<containerValues.length) { sum+=containerValues[0]; }
			else { sum += 2; }
			}
		// 2x the square of the gold count.
		if(dropIndex!=STANDARD_COLORS) { sum += scoreGold(islandGoods,addedGood); }
		return(sum);
	}
	//
	// this is a calculation which gives all goods the same value, except gold.
	// it's used in the early stage of the game, and interpolated toward the actual values.
	//
	static public double fixedIslandGoodsValue(double valuePerGood[],ContainerCell islandGoods[])
	{	double val = 0.0;
		val += scoreGold(islandGoods,null);
	   	for(int i=0; i<STANDARD_COLORS; i++)
		{	ContainerCell c = islandGoods[i];
			val += (c.height()*valuePerGood[i]);	// 5 per good across the board, ignore the fact that something gets dropped
		}
		return(val);
	}
	/** return the index of the good we have the most of if it is taller
	 * if there are most of the most than of anything else. 
	 * this is good, because it means there's headroom to add more of 
	 * everything else.
	 * @param islandGoods
	 * @return
	 */
	static public int unbalancedMost(ContainerCell islandGoods[])
	{	int most = -1;
		int imost = -1;
		int nmost = 0;
		for(int i=islandGoods.length-1; i>=0; i--)
		{	int h = islandGoods[i].height();
			if(h>most) { most = h; nmost=1; imost = i; }
			else if (h==most) { nmost++; }
		}
		return((nmost<=1)?imost:-1);
	}
}


/**
 * ContainerBoard knows all about the game of Container.
 * This class implements "Second ShipMent" rules rather than the original game rules.
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *    
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * 
 * 2DO: record the goal cards and initial machines in the game records
 * this will disentangle the archival game records from the random number generator.
 * 
 * @author ddyer
 *
 */

public class ContainerBoard extends RBoard<ContainerCell> implements BoardProtocol,ContainerConstants
{	
	static final int MAX_LOANS = 2;
	static final int LOAN_INTEREST_PAYMENT = 1;
	static final int ENDGAME_SHIPGOODS_VALUE = 3;
	static final int ENDGAME_WAREHOUSEGOODS_VALUE = 2;
	
	static final int STARTING_CASH = 20;	// cash to start
	static final int COST_TO_PRODUCE = 1;	// cost of production
	static final int ContainerCount[] = {0,0,0,12,16,20};	// start with N containers index by number of players
	static final int FACTORY_GOOD_PRICES[] = { 1, 2, 3, 4};	// cost of goods on the factory
	static final int WAREHOUSE_GOOD_PRICES[] = { 2, 3, 4, 5, 6};	// cost of goods in the warehouses
	static final int MACHINE_PRICES[] = {0,6,9,12};			// cost of new machines
	static final int WAREHOUSE_PRICES[] = {0,4,5,6,7};		// cost of new warehouses
	static final int MAX_WAREHOUSES = WAREHOUSE_PRICES.length;	// number of slots for warehouses
	static final int FACTORYLIMITS[][] = 
	{ {0,0,0,0},	// no machines
		{0,1,99,99},
		{0,2,99,99},
		{1,99,99,99},
		{99,99,99,99}};
	

	static final int WAREHOUSELIMITS[][] = 
	{ {0,0,0,0,0},	// no warehouses
		{0,0,99,99,99},
		{0,1,99,99,99},
		{1,1,99,99,99},
		{2,2,99,99,99},
		{99,99,99,99,99}};
	

	ContainerState unresign;
	ContainerState board_state;
	public ContainerState getState() {return(board_state); }
	public void setState(ContainerState st) 
	{ 	unresign = (st==ContainerState.RESIGN_STATE)?board_state:null;
	board_state = st;
	}	
	boolean VERSION_1_FILE = false;
	boolean VERSION_1_DATE = false;
	public StringStack complaints = new StringStack();
	public void setVersion(int n,String date)
		{ VERSION_1_FILE = (n==1); 
		  VERSION_1_DATE = false;
		  try {
				@SuppressWarnings("deprecation")
				BSDate dd = new BSDate(date);
				@SuppressWarnings("deprecation")
				BSDate tt = new BSDate("March 8 2011");
				VERSION_1_DATE = dd.before(tt);
			} catch (IllegalArgumentException e) {
				G.print("Illegal date format "+date+" "+e);
		  }
		  complaints.clear(); 
		}

	/** a ContainerGoalSet is one goal for each player.  We use them to score actual player behavior to guess which goal set is the real set. */
	class ContainerGoalSet implements lib.CompareTo<ContainerGoalSet>
	 {
		 public ContainerChip goals[] = null;
		 public int scores[] = null;
		 public boolean win[] = null;
		 public int ordinal = -1;
		 public int sumIslandValues = 0;
		 public int max_bids[] = null;
		 public int expected_bids[] = null;
		 ContainerGoalSet (int nplayers)
		 {	scores = new int[nplayers];
		 	win = new boolean[nplayers];
			 goals = new ContainerChip[nplayers];
			 max_bids = new int[nplayers];
			 expected_bids = new int[nplayers];
		 }
		 public boolean equals(ContainerGoalSet other)
		 {
			 return(other!=null && AR.sameArrayContents(goals,other.goals));
		 }
		 public boolean equals(Object o)
		 {
			 return( (o instanceof ContainerGoalSet)? equals((ContainerGoalSet)o) : false);
		 }
		 public int hashCode()
		 {
			 int v = 0;
			 for(ContainerChip goal : goals) { v ^= goal.hashCode(); }
			 return(v);
		 }
		 int islandGoodsValue(playerBoard bd)
		 {
			 ContainerChip goal = goals[bd.player];
			 return(CC.islandGoodsValue(goal,bd.islandGoods));
		 }
		 void copyFrom(ContainerGoalSet other)
		 {	sumIslandValues = other.sumIslandValues;
			 for(int i=goals.length-1; i>=0;i--) { goals[i] = other.goals[i]; }
			 // max_bids and expected_bids are working storage
		 }
		 public String toString() 
		 { String msg = "";
		   if(goals!=null) 
		   	{ msg += "#"+ordinal;
		   	  for(int i=0;i<goals.length;i++) { msg += " "+goals[i].contentsString();}
		   	  msg += " = " +sumIslandValues;
		   	}
		   return("<goalset"+msg+">");
		 }

		 public int compareTo(ContainerGoalSet o)
		 {	
			 return(-Integer.signum(sumIslandValues-o.sumIslandValues));
		 }
		 public int altCompareTo(ContainerGoalSet o)
		 {
			 return(-Integer.signum(sumIslandValues-o.sumIslandValues));
		 }

	 }
	class GuiBoard
	{
	    /**
	     * this is the scheme to support pseudo-parallel moves for
	     * loans and auctions in normal play and table play modes.
	     * In these states, each player has his own board and they
	     * are live for everyone all the time.  The user makes his selection
	     * which turns on "pendingDone". pendingDone arms "Done". Hitting
	     * "done" turns on "pendingDoneDone".  All this is strictly inside
	     * the user interface, nothing is transmitted.   
	     * 
	     * When our turn arrives, if "PendingDoneDone" is set the move
	     * is transmitted, otherwise we just wait.  The actual transmission
	     * is done from ViewerRun(wait);  The effect is that the turns move
	     * normally through the player list, but the movement stalls untill
	     * the relevant action is taken asynchronously.
	     */
		int pendingLoanBid = 0;
	    boolean pendingLoanDone = false;
	    boolean pendingLoanDoneDone = false;
	    ContainerId pendingLoanDoneCode = null;
	    String pendingLoanMove = null;	// pending loan move (response to someone else's loan request)
	    String ephemeralLoanMove = null;
	    int pendingAuctionBid = 0;
	    boolean pendingAuctionDone = false;
	    boolean pendingAuctionDoneDone = false;
	    ContainerId pendingAuctionDoneCode = null;
	    String pendingAuctionMove = null;		// pending bid (response to someone elses auction)
	    String ephemeralAuctionMove = null;
		boolean ephemeral_auction_showHandDown;
		boolean ephemeral_loan_showHandDown;
		
		public void reset()
		{
			if(!pendingLoanDoneDone) { clearPendingLoanMoves(); }
    		if(!pendingAuctionDoneDone) { clearPendingAuctionMoves(); }
		}
		  public void clearPendingLoanMoves()
		  {	pendingLoanBid = 0;
		  	pendingLoanMove = null;
		  	ephemeralLoanMove = null;
		  	pendingLoanDoneDone = false;
		  	pendingLoanDoneCode = null;
		  	pendingLoanDone = false;
		  	ephemeral_loan_showHandDown = false;
		  }
		  public void clearPendingAuctionMoves()
		  {	pendingAuctionBid = 0;
		  	pendingAuctionMove = null;
		  	ephemeralAuctionMove = null;
		  	pendingAuctionDoneDone=false;
		  	pendingAuctionDone= false;
		  	pendingAuctionDoneCode = null;
		  	ephemeral_auction_showHandDown = false;
		  }
	}
	
	public class playerBoard
	{	ContainerCell shipLocation = null;
		ContainerChip myShip = null;
		ContainerChip myGoal = null;
		ContainerCell goalCell = null;		// a random cell container myGoal, so viewer can always view cells


		ContainerGoalSet possibleGoalSets[] = null;		// this is all the goalsets that are compatible with myGoal
		int player = 0;					// index into the playerBoard array
		int cash = STARTING_CASH;		// cash in hand
		int machine_turns = 0;			// actions used manageing the factory
		int machine_cash_out = 0;		// cash spent buying machines or producing goods
		int machine_cash_in = 0;		// cash received from machine goods or from production payments
		int warehouse_turns = 0;		// actions used managing the warehouse
		int warehouse_cash_out = 0;		// cash spent loading warehouses from factories and buying warehouses
		int warehouse_cash_in = 0;		// cash received selling goods from warehouses
		int ship_turns = 0;				// actions used managing the ship
		int ship_cash_out = 0;			// cash spent loading ships
		int ship_cash_in = 0;			// cash received from auctions of ship goods
		int current_ship_cost = 0;		// the cost of goods on the current ship
		int current_ship_island_value = -1;	// estimated island value of the current ship
		int current_ship_changed = 0;	// turn at which the ship contents changed
		int island_cash_out = 0;		// cash spent to put goods on the island.  This includes winning auctions
										// and self-auctions plus the cost of the goods on the ship.
		int interest_out = 0;			// cash paid in interest
		int interest_in = 0;			// cash received as interest
		int pass_count = 0;				// number of times we've passed
		int virtual_cash = 0;			// random bonus/penalty points * 100
		boolean requestingLoan = false;	//this player is requesting a loan
		boolean ephemeral_wontFundLoan = false;	// ephemeral info is transmitted asynchronously
		boolean ephemeral_willFundLoan = false;	// and doesn't count in consistency checks
		boolean hasTakenLoan = false;	//has taken out a loan this turn.  this is to prevent repaying it immediately.
		boolean declinedLoan = false;	//this player has declined a loan
		boolean willFundLoan = false;	//willing to be financier
		boolean requestingBid = false;	// we're condicting an auction and need bids
		private int bidAmount = 0;				// amount of the bid we made
		public int bidAmount() { return(bidAmount); }
		public void setBidAmount(int n)
		{	bidAmount = n;
		}
		private int loanBidAmount = 0;		// static copy of bidamount, cash committed but not spent, not available for loans
		public int loanBidAmount() { return(loanBidAmount); }
		public void setLoanBidAmount(int n) 
		{ loanBidAmount = n; 
		}
		boolean cannotRebid = false;	// can't bid either because we're the seller or we have bid or we declined.
		boolean bidReceived = false;	// true if we made a bid
		boolean bidReady = false;		// bid is ready to transmit
		boolean selectedAsFunder = false;	// winner
		boolean showHandDown = false;
		boolean showHandUp = false;
		String showHandMsg = null;
		boolean hidden = true;			// hidden information display
		ContainerCell shipGoods = null;
		
		int loans_taken = 0;
		int total_loan_amount = 0;
		int loans_made = 0;
		int total_loans_made = 0;
		private boolean unpaid_interest[] = new boolean[MAX_LOANS];
		

		// extract the subset of all goal sets that are compatible with our actual goal.
		void extractPossibleGoalSets(ContainerGoalSet sets[])
		{
			int num = sets.length/CC.NUMBER_OF_GOAL_CARDS;
			possibleGoalSets = new ContainerGoalSet[num];
			for(int idx = sets.length-1; idx>=0; idx--)
			{	ContainerGoalSet aset = sets[idx];
				if(aset.goals[player]==myGoal) { possibleGoalSets[--num] = aset; }
			}
			G.Assert(num==0,"found them all");
		}
		// return a mask describing unpaid interest status for each loan
		int getUnpaidStatus()
		{	int val = 0;
			for(int i=0;i<MAX_LOANS;i++) { val = val<<1; if(unpaid_interest[i]) { val |= 1; }}
			return(val);
		}
		// set unpaid interest status for a masked group of loans
		void setUnpaidStatus(int n)
		{	for(int i=MAX_LOANS-1; i>=0;i--)
			{	unpaid_interest[i] = ((n&1)!=0);
				n = n>>1;
			}
		}
		void setUnpaidStatus(ContainerCell src)
		{	int idx = loan_index[src.row];
			unpaid_interest[idx]=true;
		}
		
		private int loan_funder[] = new int[MAX_LOANS];
		private int loan_amount[] = new int[MAX_LOANS];
		private int loan_index[] = new int[MAX_LOANS];
		ContainerCell loans[] = new ContainerCell[MAX_LOANS];
		boolean loanIsActive(ContainerCell c)
		{	return((c.topChip()!=null)&& (c.rackLocation()==ContainerId.LoanLocation) && (loan_index[c.row]>=0));
		}
		
		int loanAmount(ContainerCell c)
		{	G.Assert(loanIsActive(c),"loan is active");
			return(activeLoanAmount(c));
		}
		
		int loanRepaymentAmount(ContainerCell c)
		{
			int ind = loan_index[c.row];
			int amount = loan_amount[ind];
			if(unpaid_interest[ind]) { amount+=LOAN_INTEREST_PAYMENT; }
			return(amount);
		}
		int activeLoanAmount(ContainerCell c)
		{
			int ind = loan_index[c.row];
			int amount = loan_amount[ind];
			return(amount);
		}
		int loanFunder(ContainerCell c)
		{	G.Assert(loanIsActive(c),"loan is active");
			return(loan_funder[loan_index[c.row]]);
		}

		ContainerCell pendingLoanCell()
		{	for(int i=0;i<MAX_LOANS;i++)
			{
			ContainerCell c = loans[i];
			if((c.topChip()!=null) && (loan_index[i]==-1))
				{	return(loans[i]);
				}
			}
			throw G.Error("can't find a pending loan");
		}
		ContainerCell assignLoanIndex(int amount,playerBoard funder)
		{	ContainerCell loan = pendingLoanCell();
			int idx = loan.row;
			for(int j=0;j<MAX_LOANS;j++)
			{	if(loan_index[j]==-1) 
				{ loan_index[idx]=j;
				  loan_amount[j]=amount;
			      loan_funder[j]=(funder==null)?-1:funder.player;
			      unpaid_interest[j]=false;
			      return(loan); 
				}	
			}
			throw G.Error("can't find an empty loan slot");
		}

		// establish a loan tobe funded by "funder".  null indicates the bank.
		// the loan card is identified by not having a loan index assigned yet.
		// the loan card is assigned to a loan index by hunting for a spare slot.
		//
		void fundLoan(ContainerCell loanCard)
		{	int idx = loan_index[loanCard.row];
			int funder = loan_funder[idx];
			int amount = loan_amount[idx];
			if(funder<0) { transfer_from_bank(amount,this); }
				else 
				{ playerBoard pb = playerBoard[funder];
				  transfer(amount,pb,this,false);
				  pb.total_loans_made += amount;
				  pb.loans_made++;
				}
			total_loan_amount += amount;
			loans_taken++;	
		}

		// do the cash part of repaying a loan, but keep the bookkeeping in place
		void repayLoan(ContainerCell source)
		{	int idx = loan_index[source.row];
			int paidInterest = (unpaid_interest[idx]) ? LOAN_INTEREST_PAYMENT : 0;
			int amount = loan_amount[idx]+paidInterest;
			interest_out += LOAN_INTEREST_PAYMENT;
			if(loan_funder[idx]<0) { transfer_to_bank(amount,this,false); }
			else {
				playerBoard pb = playerBoard[loan_funder[idx]];
				transfer(amount,this,pb,false);
				pb.total_loans_made -= amount;
				pb.loans_made--;
				pb.interest_in += paidInterest;
			}
			total_loan_amount -= amount;
			loans_taken--;
		}
		void cancelLoan(int i)
		{
			int idx = loan_index[i];
			loan_funder[idx] = -1;
			loan_amount[idx] = 0;
			unpaid_interest[idx]=false;
			loan_index[i] = -1;
		}
		void cancelLoan()
		{	for(int i=0;i<MAX_LOANS; i++)
			{	if((loan_index[i]>=0) && (loans[i].topChip()==null))
				{
				cancelLoan(i);
				return;
				}
			}
		}
		//
		// pay all interest due if possible.  If not enough cash, do nothing.
		// return true for all-paid.  We implement a modified loan rule.  If you owe
		// interest, you MUST pay even if that involves taking out another loan. 
		// if you already have 2 loans, your unpaid interest is added to the 
		// principal.  Of course, you are broke and in a hopeless position.
		//
	    boolean payInterest(boolean overdraft)
	    {	//G.print("pay "+this+overdraft+cash);
	    	if(!overdraft && (cash < LOAN_INTEREST_PAYMENT*loans_taken) && (loans_taken<MAX_LOANS))
	    	{	// forced to take out a loan
	    		return(false);
	    	}
	    	else
	    	{
	    	for(int i=0; i<MAX_LOANS; i++)
	    	{	if(loan_index[i]>=0)	// if this loan is active
	    		{ 
	    		if((cash>=LOAN_INTEREST_PAYMENT)||overdraft)
	    		{
	    		int funder = loan_funder[i];
	    		interest_out += LOAN_INTEREST_PAYMENT;
	    		if(funder<0) { transfer_to_bank(LOAN_INTEREST_PAYMENT,this,overdraft); }
	    		else 
	    		{ playerBoard bd = playerBoard[funder];
	    		  bd.interest_in += LOAN_INTEREST_PAYMENT;
	    		  transfer(LOAN_INTEREST_PAYMENT,this,bd,overdraft);
	    		}
	    		unpaid_interest[i]=false;
	    		}
	    		else
	    			{ loan_amount[i]+=LOAN_INTEREST_PAYMENT;
	    			  int funder = loan_funder[i];
	    			  if(funder>=0) { playerBoard[funder].interest_in += LOAN_INTEREST_PAYMENT; }
	    			  unpaid_interest[i]=true; 
	    			}
	    		}
	    	}
	    	return(true);
	    	}
	    }
	    //
	    // undo paying interest.
	    //
	    void unpayInterest()
	    {	//G.print("Unpay "+this+cash);
	    	for(int i=0; i<MAX_LOANS; i++)
	    	{	if(loan_index[i]>=0)	// loan is active
	    		{int funder = loan_funder[i];
	    		if(!unpaid_interest[i])
	    		{
	    		if(funder<0) { transfer_from_bank(LOAN_INTEREST_PAYMENT,this); interest_out -= LOAN_INTEREST_PAYMENT; }
	    		else 
	    		{ playerBoard bd = playerBoard[funder];
	    		  transfer(LOAN_INTEREST_PAYMENT,bd,this,false);
	    		  bd.interest_in -= LOAN_INTEREST_PAYMENT;
	    		}
	    		}
	    		else 
	    			{ loan_amount[i] -= LOAN_INTEREST_PAYMENT; 
	    			  interest_out -= LOAN_INTEREST_PAYMENT;
	    			  if(funder>=0) { playerBoard[funder].interest_in -= LOAN_INTEREST_PAYMENT; }
	    			  unpaid_interest[i]=false;
	    			}
	    		}
	    	}
	    }
	    
		void pushLoanState(IStack on)
		{	for(int i=0,lim=MAX_LOANS; i<lim;i++)
			{
				on.push(loan_funder[i]);
				on.push(loan_amount[i]);
				on.push(loan_index[i]);
			}
			on.push(loans_taken);
			on.push(total_loan_amount);
			on.push(loans_made);
			on.push(total_loans_made);
			on.push(cash);
		}
		void popLoanState(IStack on,boolean discard)
		{	int cc = on.pop();
			if(!discard) { cash = cc; }
			
			cc = on.pop();
			if(!discard) { total_loans_made = cc; }
			
			cc = on.pop();
			if(!discard) { loans_made = cc; }
			
			cc = on.pop();
			if(!discard) { total_loan_amount = cc; }
			
			cc = on.pop();
			if(!discard) { loans_taken = cc; }
			
			for(int i=MAX_LOANS-1;i>=0;i--)
			{
			int v1 = on.pop();
			int v2 = on.pop();
			int v3 = on.pop();
			if(!discard) 
				{loan_index[i] = v1;
				 loan_amount[i] = v2;
				 loan_funder[i] = v3;
				}
			}
		}
		
		ContainerCell factoryGoods[] = new ContainerCell[FACTORY_GOOD_PRICES.length];		// one cell per price
		ContainerCell warehouseGoods[] = new ContainerCell[MAX_WAREHOUSES];	// one cell per good
		ContainerCell islandGoods[] = null;		// one stack per good type
		ContainerCell machines[] = new ContainerCell[MACHINE_PRICES.length];
		boolean machineHasProduced[] = new boolean[machines.length];
		ContainerCell warehouses[] = new ContainerCell[MAX_WAREHOUSES];
		ContainerCell docks[] = new ContainerCell[MAX_PLAYERS-1];

		public String toString() {return( "<playerboard #"+player+">"); };
		

		// return the current value of all island goods.  Values of colors are specified
		// by the goal card, you get 10 for slot 1 if you have all 5 colors, 5 otherwise.
		// you lose all of your most common good, the cheaper one in case of ties, except 
		// if it's the 5/10 cell, you lose the 5/10
		//
		public int islandGoodsValue()
		{	return(CC.islandGoodsValue(myGoal,islandGoods));
		}
		public int islandGoodsValue(ContainerGoalSet goalset)
		{	return(CC.islandGoodsValue(goalset.goals[player],islandGoods));
		}

		boolean canBuyMachine(ContainerChip ch)
		{	if(first_shipment)
			{
			for(int i=0; i<machines.length;i++)
				{	if(machines[i].topChip()==ch) { return(false); }	// no duplicates
				}
			}
			return(true);
		}
		public int getBidInfo() 
		{	int info = loanBidAmount();
			info = (info<<12) | bidAmount();
			info = (info<<1) | (selectedAsFunder?1:0);
			info = (info<<1) | (requestingBid?1:0);
			info = (info<<1) | (cannotRebid?1:0);
			info = (info<<1) | (requestingLoan?1:0);
			info = (info<<1) | (declinedLoan?1:0);
			info = (info<<1) | (willFundLoan?1:0);
			info = (info<<1) | (bidReceived?1:0);
			info = (info<<1) | (bidReady?1:0);
			info = (info<<1) | (showHandDown?1:0);
			info = (info<<1) | (showHandUp?1:0);
			return(info);
		}
		public void setBidInfo(int info)
		{	
			showHandUp = (info&1)!=0;
			info = info>>1;
			showHandDown = (info&1)!=0;
			info = info>>1;
			bidReady = (info&1)!=0;
			info = info>>1;
			bidReceived = (info&1)!=0;
			info = info>>1;
			willFundLoan = (info&1)!=0;
			info = info>>1;
			declinedLoan = (info&1)!=0;
			info = info>>1;
			requestingLoan = (info&1)!=0;
			info = info>>1;
			cannotRebid = (info&1)!=0;
			info = info>>1;
			requestingBid = (info&1)!=0;
			info = info>>1;
			selectedAsFunder = (info&1)!=0;
			info = info>>1;
			setBidAmount(info&0xfff);
			info = info>>12;
			setLoanBidAmount(info);
			
		}
		// transfer the contents of a ship cell onto the island
		public long transferShipToIsland(ContainerCell ship,replayMode replay)
		{	long code = 0;
			while (ship.topChip()!=null)
			{	ContainerChip top = ship.removeTop();
				int slot = top.getContainerIndex();
				ContainerCell dest = islandGoods[slot];
				dest.addChip(top);
				code = (code<<3) | (1+slot);
				if(replay.animate)
				{
					animationStack.push(auctionBlock);
					animationStack.push(dest);
				}
			}
			code = (code<<3) | player;
			return(code);
		}
		// load a ship from the island, used to undo loads
		public void transferIslandToShip(ContainerCell ship,long code)
		{	int pl =(int)( code & 0x7);
			G.Assert(pl==player,"it's us");
			code = code>>3;
			while(code!=0)
			{	int ind = (int)(code & 0x7)-1;
				code = code>>3;
				islandGoods[ind].removeTop();
				ship.addChip(ContainerChip.getContainer(ind));
			}
		}
		void copyFrom(playerBoard other)
		{	
			shipLocation = getCell(other.shipLocation);
			cash = other.cash;
			requestingLoan = other.requestingLoan;
			cannotRebid = other.cannotRebid;
			willFundLoan = other.willFundLoan;
			declinedLoan = other.declinedLoan;
			hasTakenLoan = other.hasTakenLoan;
			requestingBid = other.requestingBid;
			setBidAmount(other.bidAmount());
			setLoanBidAmount(other.loanBidAmount());
			bidReceived = other.bidReceived;
			bidReady = other.bidReady;
			selectedAsFunder = other.selectedAsFunder;
			loans_taken = other.loans_taken;
			total_loan_amount = other.total_loan_amount;
			loans_made = other.loans_made;
			total_loans_made = other.total_loans_made;
			AR.copy(loan_amount,other.loan_amount);
			AR.copy(loan_funder,other.loan_funder);
			AR.copy(unpaid_interest,other.unpaid_interest);
			AR.copy(loan_index,other.loan_index);
			shipGoods.copyFrom(other.shipGoods);
			AR.copy(machineHasProduced,other.machineHasProduced);
			ContainerCell.copyFrom(loans,other.loans);
			ContainerCell.copyFrom(warehouses,other.warehouses);
			ContainerCell.copyFrom(factoryGoods,other.factoryGoods);
			ContainerCell.copyFrom(warehouseGoods,other.warehouseGoods);
			ContainerCell.copyFrom(islandGoods,other.islandGoods);
			ContainerCell.copyFrom(machines,other.machines);
			ContainerCell.copyFrom(docks,other.docks);
			
			 machine_turns = other.machine_turns;
			 warehouse_turns = other.warehouse_turns;
			 ship_turns = other.ship_turns;
			 
			 machine_cash_out = other.machine_cash_out;
			 machine_cash_in = other.machine_cash_in;
			 warehouse_cash_out = other.warehouse_cash_out;
			 warehouse_cash_in = other.warehouse_cash_in;
			 ship_cash_out = other.ship_cash_out;
			 ship_cash_in = other.ship_cash_in;
			 current_ship_cost = other.current_ship_cost;
			 current_ship_changed = other.current_ship_changed;
			 current_ship_island_value = other.current_ship_island_value;
			 interest_in = other.interest_in;
			 interest_out = other.interest_out;
			 pass_count = other.pass_count;
			 virtual_cash = other.virtual_cash;
			 island_cash_out = other.island_cash_out;

			 
		}

		public long Digest(Random r)
		{	long v = myShip.Digest();
			v += myGoal.Digest();
			v += player^r.nextLong();
			v += cash*r.nextLong();
			v += (loans_taken+1)*r.nextLong();
			v += total_loan_amount*r.nextLong();
			v += loans_made*r.nextLong();
			v += total_loans_made*r.nextLong();
			v += (selectedAsFunder?1:0)*r.nextLong();
			v += machine_turns*r.nextLong();
			v += warehouse_turns*r.nextLong();
			v += ship_turns*r.nextLong();
			v += pass_count*r.nextLong();
			v += virtual_cash*r.nextLong();
			for(int i=0;i<loans_taken;i++)
				{ v += loan_funder[i]*r.nextLong();
				  v += loan_amount[i]*r.nextLong();
				  v += loan_index[i]*r.nextLong();
				  v += (unpaid_interest[i]?1:2)*r.nextLong();
				}

			v += (requestingLoan?1:2)*r.nextLong();
			v += (hasTakenLoan?1:2)*r.nextLong();
			v += (cannotRebid?1:2)*r.nextLong();
			v += (willFundLoan?1:2)*r.nextLong();
			v += (requestingBid?1:2)*r.nextLong();
			v += (bidReceived ? 1 : 2)*r.nextLong();
			v += (bidReady ? 1 : 2)*r.nextLong();
			v += (bidAmount()+1) * r.nextLong();
			v += (declinedLoan?1:2)*r.nextLong();
			v += shipGoods.Digest(r);
			for(int i=0,lim=machineHasProduced.length; i<lim; i++) { v += (machineHasProduced[i]?1:2)*r.nextLong(); }
			v += ContainerCell.Digest(r,loans);
			v += ContainerCell.Digest(r,factoryGoods);
			v += ContainerCell.Digest(r,warehouseGoods);
			v += ContainerCell.Digest(r,islandGoods);
			v += ContainerCell.Digest(r,warehouses);
			v += ContainerCell.Digest(r,docks);
			v += ContainerCell.Digest(r,machines);
			v += (board_state.ordinal()+1)*(whoseTurn+1);
			return(v);
		}
		void sameBoard(playerBoard other)
		{	
			G.Assert(cash == other.cash,"cash matches");
			G.Assert(requestingLoan==other.requestingLoan,"requesting loan");
			G.Assert(cannotRebid==other.cannotRebid,"elgible to bid");
			G.Assert(willFundLoan==other.willFundLoan,"fund loan");
			G.Assert(requestingBid==other.requestingBid,"requesting bids");
			G.Assert(selectedAsFunder==other.selectedAsFunder,"selected as funder");
			G.Assert(bidAmount()==other.bidAmount(),"bid amount");
			G.Assert(loanBidAmount()==other.loanBidAmount(), "same loanBidAmount");
			G.Assert(bidReceived==other.bidReceived,"bid received");
			G.Assert(bidReady==other.bidReady,"bid ready");
			G.Assert(declinedLoan==other.declinedLoan,"declined loan");
			G.Assert(hasTakenLoan==other.hasTakenLoan,"taken loan");
			G.Assert(pass_count==other.pass_count, "pass count matches");
			G.Assert(virtual_cash==other.virtual_cash, "virtual_cash matches, is %d from %d",virtual_cash,other.virtual_cash);

			G.Assert(machine_turns==other.machine_turns, "machine turns match");
			G.Assert(warehouse_turns==other.warehouse_turns, "warehouse turns match");
			G.Assert(ship_turns==other.ship_turns,"ship turns match");
			shipGoods.sameCell(other.shipGoods);
			G.Assert(loans_taken==other.loans_taken,"same number of loans");
			G.Assert(total_loans_made==other.total_loans_made,"loans made amount");
			G.Assert(loans_made==other.loans_made,"loans made");
			G.Assert(total_loan_amount==other.total_loan_amount,"debts match");
			G.Assert(AR.sameArrayContents(loan_funder,other.loan_funder),"same loan funders");
			G.Assert(AR.sameArrayContents(unpaid_interest,other.unpaid_interest),"same unpaid interest");
			G.Assert(AR.sameArrayContents(loan_amount,other.loan_amount),"same loan amounts");
			G.Assert(AR.sameArrayContents(loan_index,other.loan_index),"same loan index");
			ContainerCell.sameContents(loans,other.loans);
			ContainerCell.sameContents(factoryGoods,other.factoryGoods);
			ContainerCell.sameContents(warehouseGoods,other.warehouseGoods);
			ContainerCell.sameContents(islandGoods,other.islandGoods);
			ContainerCell.sameContents(warehouses,other.warehouses);
			ContainerCell.sameContents(machines,other.machines);
			ContainerCell.sameContents(docks,other.docks);
			G.Assert(AR.sameArrayContents(machineHasProduced,other.machineHasProduced),"machineHasProduced matches");
			
			G.Assert((player == other.player) &&
					(myGoal == other.myGoal) &&
					(myShip==other.myShip),
					"Player Boards the same");	
		}
		playerBoard(Random r,int pla,ContainerChip boat,ContainerCell loc)
		{	myShip = boat;
			shipLocation = loc;
			player = pla;
			shipGoods = new ContainerCell(r,pla,ContainerId.ShipGoodsLocation,0);			// one cell per good
			goalCell = new ContainerCell(r,pla,ContainerId.CashLocation,0);
			islandGoods = new ContainerCell[CONTAINER_COLORS];
			for(int i=0,lim=loans.length; i<lim;i++) 
			{	loans[i]=new ContainerCell(r,pla,ContainerId.LoanLocation,i);
				loan_index[i] = -1;
				loan_amount[i] = 0;
				loan_funder[i] = -1;
			}
			for(int i=0,lim=factoryGoods.length; i<lim; i++)  
			{ factoryGoods[i]=new ContainerCell(r, pla,ContainerId.FactoryGoodsLocation,i);
			}
			for(int i=0,lim=warehouseGoods.length; i<lim; i++)  
			{ warehouseGoods[i]=new ContainerCell(r, pla,ContainerId.WarehouseGoodsLocation,i);
			}
			for(int i=0,lim=islandGoods.length; i<lim; i++)  
			{ islandGoods[i]=new ContainerCell(r, pla,ContainerId.IslandGoodsLocation,i);
			  islandGoods[i].colorName = ContainerChip.getContainer(i).getColor();
			}
			for(int i=0,lim=machines.length; i<lim; i++)  
			{ machines[i]=new ContainerCell(r, pla,ContainerId.MachineLocation,i);
			}
			for(int i=0,lim=warehouses.length; i<lim; i++)  
			{ warehouses[i]=new ContainerCell(r, pla,ContainerId.WarehouseLocation,i);
			}
			for(int i=0,lim=docks.length; i<lim; i++)  
			{ docks[i]=new ContainerCell(r, pla,ContainerId.AtDockLocation,i);
			}

		}
		// count the factory storage spaces used.
		int factoryStorageUsed()
		{ 	int stored=0;
			for(int i=0,lim=factoryGoods.length; i<lim; i++) { stored += factoryGoods[i].height(); }
			return(stored);
		}
		int numberOfActiveMachines()
		{	int capacity = 0;
			for(int i=0,lim=machines.length; i<lim; i++) { if(machines[i].topChip()!=null) { capacity++; }}
			return(capacity);
		}
		// 2x the number of machines we have
		int factoryStorageCapacity()
		{	return(numberOfActiveMachines()*2);
		}
		// has factory goods at this price or less
		boolean hasFactoryGoods(int price,boolean canBeGold)
		{	for(int i=0,lim=factoryGoods.length; i<lim; i++)
			{	if(FACTORY_GOOD_PRICES[i]<=price)
					{ 
					ContainerCell factoryCell = factoryGoods[i];
					if(canBeGold) 
							{ if (factoryCell.topChip()!=null) { return(true); }
							}
					else {	// have to check for a non-gold item
						for(int idx = factoryCell.height()-1; idx>=0; idx--)
						{
							if(!factoryCell.chipAtIndex(idx).isGoldContainer()) { return(true); }
						}
					}
				}
			}
			return(false);
		}
		// true if another container can be produced
		boolean factoryCanStoreMore()
		{	int capacity = factoryStorageCapacity();
			int stored = factoryStorageUsed();
			return(capacity>stored);
		}
		// true if this machine can produce (if there is still stock, not checked here)
		boolean machineCanProduce(int idx,ContainerChip type)
		{	ContainerChip thisMachine = machines[idx].topChip();
			if((thisMachine!=null)
				&& !machineHasProduced[idx]
                && factoryCanStoreMore())
                {
				ContainerCell storage = containerStorage[thisMachine.getMachineIndex()];
				return((storage.topChip()!=null)||((type==null)?false:(type.getContainerIndex()==storage.row)));
				};
			return(false);
		}
		boolean someMachineCanProduce()
		{	for(int i=0,lim=machines.length; i<lim; i++)
			{
			if(machineCanProduce(i,null)) { return(true); }
			}
			return(false);
		}
		
		// return an encoded integer in 0-15 that indicates which machines have produced.
		int getProducers()
		{	int val=0;
			for(int i=0,lim=machines.length; i<lim; i++)
			{	val = (val<<1) | (machineHasProduced[i]?1:0);
			}
			return(val);	
		}
		// conversely, set the production values
		void setProducers(int val)
		{	
			for(int i=machines.length-1; i>=0; i--)
			{ machineHasProduced[i] = ((val&1)==0)?false:true;
			 val = val>>1;
			}
		}
		
		// 
		// does this rack already contain gold?
		//
		
		public boolean warehouseContainsGold()
		{	
			ContainerChip gold = ContainerChip.getContainer(CONTAINER_GOLD);
			for(int priceIdx=0,plim=warehouseGoods.length; priceIdx<plim; priceIdx++)
			{	ContainerCell c = warehouseGoods[priceIdx];
				for(int i=0;i<=c.chipIndex;i++)
				{	ContainerChip ch = c.chipAtIndex(i);
					if(ch==gold) { return(true); }	// already has a gold container
				}
			}
			return(false);
		}
		
		// 
		// to be elgible to trade for gold, the warehouse or factory
		// storage has to contain no gold, and at least 2 different colors of container.
		//
		private boolean canTradeForGold(ContainerCell to[],ContainerChip type)
		{	
			int colorMask = 0;
			int ncolors = 0;
			if(type!=null) { ncolors++; colorMask = 1<<type.getContainerIndex(); }
			ContainerChip gold = ContainerChip.getContainer(CONTAINER_GOLD);
			for(int priceIdx=0,plim=to.length; priceIdx<plim; priceIdx++)
			{	ContainerCell c = to[priceIdx];
				for(int i=0;i<=c.chipIndex;i++)
				{	ContainerChip ch = c.chipAtIndex(i);
					if(ch==gold) { return(false); }	// already has a gold container
					int mask = (1<<ch.getContainerIndex());
					if((colorMask&mask)==0) 
						{ 
						  ncolors++;
						  colorMask |= mask;
						}
				}
			}
			return(ncolors>=2);
		}
		
		boolean canTradeForFactoryGold(ContainerCell source,ContainerChip type)
		{	if((source==null) 
				|| ((source.rackLocation==ContainerId.FactoryGoodsLocation) && (source.playerIndex()==player)))
				{return(canTradeForGold(factoryGoods,type));
				}
			return(false);
		}
		boolean canTradeForWarehouseGold(ContainerCell source,ContainerChip type)
		{	if((source==null)||((source.rackLocation==ContainerId.WarehouseGoodsLocation)&&(source.playerIndex()==player)))
			{return(canTradeForGold(warehouseGoods,type));
			}
			return(false);
		}	
		
		//
		// note that some machine has produced. Do this from 0-n and unset (below) from n-0 so 
		// when there are duplicate machines, the same one will be targeted for unset as was set.
		//
		void setMachineHasProduced(ContainerChip mach)
		{	for(int i=0,lim=machines.length; i<lim; i++)
			{
			ContainerCell c = machines[i];
			if((c!=null) && (c.topChip()==mach) && (machineHasProduced[i]==false)) 
				{ machineHasProduced[i] = true; 
				return; 
				}
			}
			throw G.Error("No machine found %s",mach);
		}		
		// note that some machine has not produced - unreverse order so set/unset will hit the same 
		// machine if this is a choice because two machines have the same color.
		void unsetMachineHasProduced(ContainerChip mach)
		{	if(mach!=null)	// will be null for gold containers
			{for(int i=machines.length-1; i>=0; i--)
			{
			ContainerCell c = machines[i];
			if((c!=null) && (c.topChip()==mach) && (machineHasProduced[i]==true)) 
				{ machineHasProduced[i] = false; 
				return; 
				}
			}
			throw G.Error("No machine found %s",mach);
			}
		}
		
		// true if any machine has produced, which locks us into the production state
		boolean someMachineHasProduced()
		{	for(int i=0,lim=machineHasProduced.length; i<lim; i++) { if (machineHasProduced[i]) { return(true); }}
			return(false);
		}
		ContainerCell getFactoryStorageLocation(int i)
		{	return(factoryGoods[i]);
		}
		ContainerCell getWarehouseStorageLocation(int i)
		{	return(warehouseGoods[i]);
		}
		ContainerCell getMachineLocation(int i)
		{	return(machines[i]);
		}
		ContainerCell getWarehouseLocation(int i)
		{	return(warehouses[i]);
		}
		// clear the machine-has-produced flags
		void clearProduced() 
		{ 	
			for(int i=0,lim=machineHasProduced.length; i<lim;i++) { machineHasProduced[i]=false; }
		}

		// count the factory storage spaces used.
		int warehouseStorageUsed()
		{ 	int stored=0;
			for(int i=0,lim=warehouseGoods.length; i<lim; i++) { stored += warehouseGoods[i].height(); }
			return(stored);
		}
		boolean hasWarehouseGoods(int price)
		{	for(int i=0,lim=warehouseGoods.length; i<lim; i++) 
				{ if((WAREHOUSE_GOOD_PRICES[i]<=price) && warehouseGoods[i].height()>0) { return(true); } }
			return(false);
		}
		// 2x the number of machines we have
		int warehouseStorageCapacity()
		{	int capacity = 0;
			for(int i=0,lim=warehouses.length; i<lim; i++) { if(warehouses[i].topChip()!=null) { capacity+=1; }}
			return(capacity);
		}
		int numberOfActiveWarehouses()
		{	return(warehouseStorageCapacity());
		}
		// true if another container can be stored on the dock
		boolean warehouseCanStoreMore()
		{	int capacity = warehouseStorageCapacity();
			int stored = warehouseStorageUsed();
			return(capacity>stored);
		}
	
		// the destination for the next machine
		ContainerCell nextEmptyMachine()
		{	return(ContainerCell.nextEmpty(machines));
		}
		// the destination for the next warehouse
		ContainerCell nextEmptyWarehouse()
		{	return(ContainerCell.nextEmpty(warehouses));
		}
		// the destination for the next ship to dock
		ContainerCell nextEmptyDock()
		{	return(ContainerCell.nextEmpty(docks));
		}
		ContainerCell getDockLocation(int idx)
		{	return(docks[idx]);
		}
		// the destination for the next warehouse
		ContainerCell nextEmptyLoan()
		{	return(ContainerCell.nextEmpty(loans));
		}

		// apply the second shipment rules for factory goods
		boolean canPlaceFactoryGood(int index,boolean second)
		{	if(second)
				{int nfac = numberOfActiveMachines();
				int limits[] = FACTORYLIMITS[nfac];
				int limit = limits[index];
				int stored = factoryGoods[index].height();
				return(stored<limit);
				}
			return(true);
		}
		// apply the second shipment rules for warehouse goods
		boolean canPlaceWarehouseGood(int index,boolean second)
		{	if(second)
			{
			int nfac = numberOfActiveWarehouses();
			int limits[] = WAREHOUSELIMITS[nfac];
			int limit = limits[index];
			int stored = warehouseGoods[index].height();
			return(stored<limit);
			}
			return(true);
		}
		public boolean canLoadShip()
		{	return(shipGoods.height()<MAX_SHIP_GOODS);
		}
	}

	// generate a numnbered permutation and set a container goal to that permutation of goals
	private void permut(ContainerGoalSet set,int list[],int permn,int div,int lvl,int idx)
	{
		int rem = permn / div;
		int sel = permn % div;
		// select one of the remaining ints to swap with idx
		int x = list[sel+idx];
		list[sel+idx]=list[idx];
		list[idx]=x;
		set.goals[idx] = ContainerChip.getCard(x);
		// recurse to reorganize the next slot
		if(idx+1<lvl) { permut(set,list,rem,div-1,lvl,idx+1); }
	}
	private void setupPermutation(ContainerGoalSet set,int n, int div,int idx)
	{	int list[] = new int[div];
		for(int i=0;i<div;i++) { list[i]=i; }
		permut(set,list,n,div,idx,0);
	}
	private ContainerGoalSet[] allPossibleGoalSets(int numPlayers)
	{	int perm = G.permutations(CC.NUMBER_OF_GOAL_CARDS,numPlayers);
		ContainerGoalSet set[] = new ContainerGoalSet[perm]; 
		for(int idx = 0;idx<perm; idx++)
		{
			ContainerGoalSet nextSet = set[idx] = new ContainerGoalSet(numPlayers);
			setupPermutation(nextSet,idx,CC.NUMBER_OF_GOAL_CARDS,numPlayers);
			nextSet.ordinal = idx;
		}
		return(set);
	   }

	int warehousesInPlay() 
	{	int s=0;
		for(int i=playerBoard.length-1; i>=0;i--) { s += playerBoard[i].warehouseStorageCapacity(); }
		return(s);
	}
	int machinesInPlay() 
	{	int s=0;
		for(int i=playerBoard.length-1; i>=0;i--) { s += playerBoard[i].factoryStorageCapacity(); }
		return(s/2);
	}

	/* 
	 * these are important state variables in addition to board_state
	 */
    boolean hasProduced = false;			// has produced as one of his actions
    int factoryGoodsProduced  = 0;
    boolean hasRepricedFactory = false;		// has reprices goods this turn
    boolean hasBoughtWarehouseGoods = false;	// has bought goods for the warehouse
    int warehouseGoodsBought = 0;
    boolean hasRepricedWarehouse = false;	// has repriced goods this turn
    boolean mustPayLoan = false;			// some interest is due
    boolean hasProducedLuxury = false;		// has produced a luxury container
    boolean isSecondAction = false;			// is now the second action
    boolean hasBoughtMachine = false;		// has bought a machine this turn
    boolean hasBoughtWarehouse = false;		// has bought a warehouse this turn
    
    // moves that shouldn't be repeated
    int movedToSeaFrom = -1;
    int loadedShipFrom = -1;
    int loadedWarehouseFrom = -1;
    boolean passed_this_turn = false;
    // gold production
    ContainerId produceLuxuryFromRack = null;
	ContainerChip produceLuxuryFirstColor = null;
    
    int playerSource = -1;		// player that's the source in our current transaction
 
    public CellStack animationStack = new CellStack();
    public boolean second_shipment = true;	//
    public boolean first_shipment = false;	//
    private playerBoard playerBoard[] = null;	// player boards, one per player
    public playerBoard getShipOwner(ContainerChip ship)
    {
    	G.Assert(ship.isShip(),"must be a ship");
    	for(playerBoard b : playerBoard)
    	{
    		if(b.myShip==ship) { return(b); }
    	}
    	return(null);
    }
    //
    // cells where a boat can be
    //
    public ContainerCell auctionBlock = null;	// the auction black
	public ContainerCell loanCards = null;
    public ContainerCell islandParking[] = null;	// parking at the island
    public ContainerCell atSea[] = null;		// at sea
    //
    // other storage locations
    //
	public ContainerCell containerStorage[];		// unsold cubes
	public ContainerCell machineStorage[]; 			// unsold machines
	public ContainerCell warehouseStorage;			// unsold warehouses
	boolean nextIntCompatibility = false;
    public String gameType() { return(gametype+" "+(players_in_game+(nextIntCompatibility?100:0))+" "+randomKey); }
    public void SetDrawState()
    	{ 	// shouln't happen, but harmless
    	}
   //
    // private variables
    //
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public int last_sale_move = 0;
    public int sales_this_move = 0;
    public int ship_loads_this_move = 0;
    public ContainerChip pickedObject = null;
    private ContainerState stackEmptyState = ContainerState.PUZZLE_STATE;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private IStack pickedIndex = new IStack();

    public ContainerChip lastDroppedObject = null;
    private boolean ROBOT_BOARD = false;		// set in robot boards to inhibit various expensive operations 
    private long shipLoad = 0;			// temporary for undo - coded contents of the ship
    private int shuffleCount = 0;		// temporary when in reprice mode
    private StateStack undoStateStack=new StateStack();
    private IStack undoStack=new IStack();
    private IStack robotStack = new IStack();
    private StateStack robotState = new StateStack();
    private StateStack robotStateStack=new StateStack();
    private IStack robotStack2 = new IStack();	// a second robot stack to avoid problems with undo order
    public ContainerGoalSet masterGoalSet = null;		// the master goalset is the real goal set
    private ContainerGoalSet allGoalSets[] = null;		// this is a list of all possible goal sets for this number of players
    
    private static final int VALUE_NOT_SET = 123456;	// impossible value for island goods
    private int islandValueBaseline[] = null;			// one per player
    private int islandValueAdded[] = null;				// one per type of good
    private int islandValueTo[] = null;					// the player to whom this value applies
    boolean makeFastLoans = false;		// used to accelerate robot search of loans
    public int bank = 0;		// balance the books
    GuiBoard guiBoards[] = null;
    
   public ContainerBoard(String init,int players,long ran,int map[]) // default constructor
    { 
	   setColorMap(map, players);
	   doInit(init,ran,players); // do the initialization 
       guiBoards = new GuiBoard[MAX_PLAYERS];
       for(int i=0;i<MAX_PLAYERS;i++) { guiBoards[i]=new GuiBoard(); }
    }

    public ContainerBoard cloneBoard() 
	{ ContainerBoard dup = new ContainerBoard(gametype,players_in_game,randomKey,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((ContainerBoard)b); }

    private int machines_per_color()
    {
    	return(first_shipment?4:players_in_game-1);	
    }

    private int machines_sold(ContainerChip ch)
    {	ContainerCell c = machineStorage[ch.getMachineIndex()];
    	return(machines_per_color()-c.height());
    }
    // standard initialization for Container.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game,long ran,int numAndCompat)
    { 	
    	Random r = new Random(63432234);
    	int num = numAndCompat%100;
    	nextIntCompatibility = numAndCompat/100==1;	// compatibility kludge for old games, bundled with number of players
    	Random.setNextIntCompatibility(nextIntCompatibility);
    	
    	if(Container_First_Init.equalsIgnoreCase(game))
    		{ first_shipment = true;
    		  second_shipment=false; 
    		}
    	else if(Container_Second_Init.equalsIgnoreCase(game) || Container_INIT.equalsIgnoreCase(game)) 
    		{ first_shipment = false;
    		  second_shipment = true;
    		}
    	else { throw G.Error(WrongInitError,game); }
    	CC.second_shipment = second_shipment;
        gametype = game;
        players_in_game = num;
        randomKey = ran;
        win = new boolean[num];
        animationStack.clear();
        setState(ContainerState.PUZZLE_STATE);
        undoStack.clear();
        undoStateStack.clear();
        robotStack.clear();
        robotStateStack.clear();
        robotStack2.clear();
        last_sale_move = 0;
        sales_this_move = 0;
        hasBoughtMachine = false;
        hasBoughtWarehouse = false;
        ship_loads_this_move = 0;
        isSecondAction=false;
        playerSource = -1;
        passed_this_turn = false;
        warehouseGoodsBought = factoryGoodsProduced =0;
        hasBoughtWarehouseGoods = hasRepricedFactory = hasRepricedWarehouse = hasProduced=false;
        loadedWarehouseFrom = -1;
        loadedShipFrom = -1;
        movedToSeaFrom = -1;

        mustPayLoan = false;
        produceLuxuryFromRack = ContainerId.FactoryGoodsLocation;
        shipLoad = 0;
        shuffleCount = 0;
        produceLuxuryFirstColor = null;
        pickedObject=null;
        bank = 0;
        pickedSourceStack.clear();
        droppedDestStack.clear();
        pickedIndex.clear();
        whoseTurn = FIRST_PLAYER_INDEX;
        stackEmptyState=ContainerState.PUZZLE_STATE;
        moveNumber = 1;
		loanCards = new ContainerCell(r,ContainerId.LoanLocation,0);
		for(int i=0;i<num*MAX_LOANS;i++) { loanCards.addChip(ContainerChip.getLoan()); }
        
        containerStorage = new ContainerCell[CONTAINER_COLORS];
        for(int i=0,lim=containerStorage.length; i<lim;i++)
        {	ContainerCell c = containerStorage[i] = new ContainerCell(r,ContainerId.ContainerLocation,i);
        	ContainerChip p = ContainerChip.getContainer(i);
        	int numOfEach = ContainerCount[players_in_game];
        	if(second_shipment || !p.isGoldContainer()) { for(int j=0;j<numOfEach; j++) { c.addChip(p); }}
        	c.colorName = p.getColor();
        }
        machineStorage = new ContainerCell[MACHINE_COLORS];
        for(int i=0,lim=machineStorage.length; i<lim; i++)
        {
        	ContainerCell c = machineStorage[i] = new ContainerCell(r,ContainerId.MachineLocation,i); 
        	ContainerChip p = ContainerChip.getMachine(i);
        	int numOfEach = machines_per_color();
        	for(int j=0;j<numOfEach;j++) { c.addChip(p); }
        	c.colorName = p.getColor();
        }
        
        {
        warehouseStorage = new ContainerCell(r,ContainerId.WarehouseLocation,0);
        ContainerChip p = ContainerChip.getWarehouse();
        for(int i=0;i<num*MAX_WAREHOUSES;i++) { warehouseStorage.addChip(p); }
        }
        
        auctionBlock = new ContainerCell(r,ContainerId.AuctionLocation,0);
        islandParking = new ContainerCell[num];
        atSea = new ContainerCell[num];
        playerBoard = new playerBoard[num];
        
        // working storage for ScoreForPlayer
        islandValueBaseline = new int[num];
        islandValueAdded = new int[CONTAINER_COLORS];
        islandValueTo = new int[CONTAINER_COLORS];
        int map[]=getColorMap();
        for(int i=0;i<num;i++)
        {	ContainerCell sea = atSea[i] = new ContainerCell(r,ContainerId.AtSeaLocation,i);
        	ContainerChip ship = ContainerChip.getShip(map[i]);
        	sea.addChip(ship);
         	playerBoard[i] = new playerBoard(r,i,ship,sea);
        	islandParking[i] = new ContainerCell(r,ContainerId.AtIslandParkingLocation,i);
         	                                                                     
        }
        // and now the actual setup
        initGoalSets(num,ran);
    }
    private void initGoalSets(int num,long ran)
        {
        Random random = new Random(ran);
        ContainerChip machineColor[] = new ContainerChip[MAX_PLAYERS];
        ContainerChip goalCard[] = new ContainerChip[MAX_PLAYERS];
        for(int i=0;i<MAX_PLAYERS;i++) 
        	{ machineColor[i]=ContainerChip.getMachine(i); 
        	  goalCard[i]=ContainerChip.getCard(i);
        	}
        random.shuffle(machineColor);
        random.shuffle(goalCard);
    	ContainerGoalSet mset = new ContainerGoalSet(num);
    	masterGoalSet = null;
    	allGoalSets = allPossibleGoalSets(num); 
        for(int i=0;i<num;i++)
        {	playerBoard board = playerBoard[i];
        	// give each player $20, a machine, a goal, and a warehouse
        	mset.goals[i] = board. myGoal = goalCard[i];
        	board.extractPossibleGoalSets(allGoalSets);
        	board.goalCell.addChip(board.myGoal);
        	
        	board.cash = STARTING_CASH;
        	bank -= STARTING_CASH;
        	int mindex = machineColor[i].getMachineIndex();
        	ContainerCell mtype = machineStorage[mindex];
        	ContainerChip ch = first_shipment? mtype.topChip():mtype.removeTop();
        	board.machines[0].addChip(ch);
        	board.factoryGoods[1].addChip(containerStorage[mindex].removeTop());
        	board.warehouses[0].addChip(warehouseStorage.removeTop());
        }
        for(int i=allGoalSets.length-1; i>=0; i--) 
        	{ if(mset.equals(allGoalSets[i])) 
        		{ G.Assert(masterGoalSet==null, "first match of master goal set"); 
        		masterGoalSet = allGoalSets[i]; 
        		}
        	}
        G.Assert(masterGoalSet!=null,"Master set found");
        }
    
	public void sameboard(BoardProtocol b) { sameboard((ContainerBoard)b); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ContainerBoard from_b)
    {
    	super.sameboard(from_b);
        auctionBlock.sameCell(from_b.auctionBlock);	// the auction black
        G.Assert(last_sale_move==from_b.last_sale_move,"last sale move matches");
        ContainerCell.sameCell(islandParking,from_b.islandParking);
        ContainerCell.sameCell(atSea,from_b.atSea);
        ContainerCell.sameCell(containerStorage,from_b.containerStorage);
        ContainerCell.sameCell(machineStorage,from_b.machineStorage);
        ContainerCell.sameCell(warehouseStorage,from_b.warehouseStorage);
        ContainerCell.sameCell(loanCards,from_b.loanCards);
        G.Assert(first_shipment == from_b.first_shipment,"first shipment");
        G.Assert(second_shipment== from_b.second_shipment,"second shipment");
        G.Assert(produceLuxuryFirstColor == from_b.produceLuxuryFirstColor,"same luxury color");
        G.Assert(produceLuxuryFromRack == from_b.produceLuxuryFromRack,"same luxury rack");
        for(int i=0,lim=playerBoard.length; i<lim; i++)
        {	playerBoard[i].sameBoard(from_b.playerBoard[i]);
        }
        G.Assert(undoStack.sameContents(from_b.undoStack),"same undo stack");
        G.Assert(undoStateStack.eqContents(from_b.undoStateStack),"same undo state stack");
        G.Assert(robotStack.sameContents(from_b.robotStack),"same robotStack");
        G.Assert(robotStateStack.eqContents(from_b.robotStateStack),"same robotStateStack");
        G.Assert(robotStack2.sameContents(from_b.robotStack2),"same robotStack2");
        G.Assert((hasProduced == from_b.hasProduced), "hasProduced not the same");
        G.Assert(hasRepricedFactory==from_b.hasRepricedFactory, "repriced factory matches");
        G.Assert(hasRepricedWarehouse==from_b.hasRepricedWarehouse, "repriced factory matches");
        G.Assert(hasBoughtWarehouseGoods==from_b.hasBoughtWarehouseGoods, "bought warehouse goods matches");
        G.Assert(factoryGoodsProduced==from_b.factoryGoodsProduced, "factoryGoodsProduced matches");
        G.Assert(warehouseGoodsBought==from_b.warehouseGoodsBought, "warehouseGoodsBought matches");
       
        G.Assert(loadedShipFrom==from_b.loadedShipFrom,"Same loaded ship from");
        G.Assert(loadedWarehouseFrom == from_b.loadedWarehouseFrom,"Same load warehouse from");
        G.Assert(movedToSeaFrom == from_b.movedToSeaFrom,"Same Move to sea from");

        G.Assert((mustPayLoan == from_b.mustPayLoan),"mustPayLoan not the same");
        G.Assert((hasProducedLuxury == from_b.hasProducedLuxury), "hasProducedLuxury not the same");
        G.Assert((isSecondAction == from_b.isSecondAction), "isSecondAction not the same");
        G.Assert((shipLoad==from_b.shipLoad),"ship loading is same");
        G.Assert((shuffleCount==from_b.shuffleCount),"shuffleCount matches");
        G.Assert((isSecondAction == from_b.isSecondAction),"isSecondAction matches");
        G.Assert(playerSource == from_b.playerSource,"playerSource matches");
        G.Assert((hasProducedLuxury == from_b.hasProducedLuxury),"hasproducedluxury matches");
        G.Assert((hasBoughtMachine == from_b.hasBoughtMachine),"hasBoughtMachine matches");
        G.Assert((hasBoughtWarehouse == from_b.hasBoughtWarehouse),"hasBoughtWarehouse matches");
		G.Assert(masterGoalSet.equals(from_b.masterGoalSet),"master goal set matches");

    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return a long digest
     */
   public long Digest()
    {	
	   long v = 0;

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        
        for(int i=0,lim = playerBoard.length;  i<lim; i++)
        {	v += playerBoard[i].Digest(r);
        }
        v += ContainerCell.Digest(r,islandParking);
        v += ContainerCell.Digest(r,atSea);
        v += ContainerCell.Digest(r,containerStorage);
        v += ContainerCell.Digest(r,machineStorage);
        v += auctionBlock.Digest(r);
        v += warehouseStorage.Digest(r);
        v += shipLoad + r.nextLong();
        v += loanCards.height()*r.nextLong();
        v += last_sale_move^r.nextLong();	// and do not include sales_this_move
        v += (produceLuxuryFirstColor==null)?0:produceLuxuryFirstColor.Digest();
        v += produceLuxuryFromRack.IID()*r.nextLong();
        v += undoStack.Digest(r);
        // for most games, we should also digest whose turn it is
		for(int i=0;i<players_in_game;i++)
		{ long v0 = r.nextLong();
		  if(i==whoseTurn) { v^=v0; }
		}
		v += (first_shipment?1:2)^r.nextLong();
		v += (second_shipment?1:2)^r.nextLong();
		v += (hasProduced ? 1 : 2)^r.nextLong();
		v += (hasProducedLuxury ? 1 : 2)^r.nextLong();
		v += (hasBoughtWarehouse ? 1 : 2)*r.nextLong();
		v += (hasBoughtMachine ? 1 : 2)* r.nextLong();
		v += (isSecondAction ? 1 : 2)^r.nextLong();
		v += (playerSource+1)*r.nextLong();
		v += (mustPayLoan?1:2)*r.nextLong();
		
        v += loadedShipFrom*r.nextLong();
        v += loadedWarehouseFrom*r.nextLong();
        v += movedToSeaFrom*r.nextLong();
        v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ContainerBoard from_board)
    {	super.copyFrom(from_board);
        ContainerBoard from_b = from_board;
        VERSION_1_FILE = from_b.VERSION_1_FILE;
        VERSION_1_DATE = from_b.VERSION_1_DATE;
        first_shipment = from_b.first_shipment;
        second_shipment = from_b.second_shipment;
        board_state = from_b.board_state;
        hasProduced = from_b.hasProduced;
        loadedShipFrom = from_b.loadedShipFrom;
        loadedWarehouseFrom = from_b.loadedWarehouseFrom;
        movedToSeaFrom = from_b.movedToSeaFrom;
        
        hasRepricedFactory = from_b.hasRepricedFactory;
        hasRepricedWarehouse = from_b.hasRepricedWarehouse;
        hasBoughtWarehouseGoods = from_b.hasBoughtWarehouseGoods;
        factoryGoodsProduced = from_b.factoryGoodsProduced;
        warehouseGoodsBought = from_b.warehouseGoodsBought;
        mustPayLoan = from_b.mustPayLoan;
        hasProducedLuxury = from_b.hasProducedLuxury;
        hasBoughtMachine = from_b.hasBoughtMachine;
        hasBoughtWarehouse = from_b.hasBoughtWarehouse;
        isSecondAction = from_b.isSecondAction;
        undoStack.copyFrom(from_b.undoStack);
        undoStateStack.copyFrom(from_b.undoStateStack);
        robotStack.copyFrom(from_b.robotStack);
        robotStateStack.copyFrom(robotStateStack);
        robotStack2.copyFrom(from_b.robotStack2);
        playerSource = from_b.playerSource;
        passed_this_turn = from_b.passed_this_turn;
        shipLoad = from_b.shipLoad;
        shuffleCount = from_b.shuffleCount;
        last_sale_move = from_b.last_sale_move;
        sales_this_move = from_b.sales_this_move;
        ship_loads_this_move = from_b.ship_loads_this_move;
        produceLuxuryFirstColor = from_b.produceLuxuryFirstColor;
        produceLuxuryFromRack = from_b.produceLuxuryFromRack;

        loanCards.copyFrom(from_b.loanCards);
        ContainerCell.copyFrom(islandParking,from_b.islandParking);
        ContainerCell.copyFrom(atSea,from_b.atSea);
        ContainerCell.copyFrom(islandParking,from_b.islandParking);
        ContainerCell.copyFrom(containerStorage,from_b.containerStorage);
        ContainerCell.copyFrom(machineStorage,from_b.machineStorage);
        auctionBlock.copyFrom(from_b.auctionBlock);
        warehouseStorage.copyFrom(from_b.warehouseStorage);
        
        for(int i=allGoalSets.length-1; i>=0; i--) 
        	{ ContainerGoalSet newset = allGoalSets[i];
        	  ContainerGoalSet fromset = from_b.allGoalSets[i];
        	  newset.copyFrom(fromset);
        	  if(fromset == from_board.masterGoalSet ) { masterGoalSet = newset; }
        	}
       
        for(int i=0,lim = playerBoard.length;  i<lim; i++)
        {	playerBoard[i].copyFrom(from_b.playerBoard[i]);
         	playerBoard[i].extractPossibleGoalSets(allGoalSets);
        }
        //
        // copy the stack and stackindex so the robot knows where to start,
        // but don't include it int the sameboard and digest checks
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        pickedIndex.copyFrom(from_b.pickedIndex);      
        stackEmptyState = from_b.stackEmptyState;
        if(G.debug()) { sameboard(from_b); }
    }
    public void doInit()
    {	// must use the live value of nextIntCompatibility
    	doInit(gametype,randomKey,players_in_game+(Random.nextIntCompatibility()?100:0));
    }
    /* initialize a board back to initial empty state */
    public void doInit(String tp,long rv)
    {
    	doInit(tp,rv,players_in_game+(nextIntCompatibility?100:0));
    }
    public void doInit(String gtype,long ranv,int num)
    {	
   		Init_Standard(gtype,ranv,num);
    }
    public void doInitSpec(String gtype)
    {	Tokenizer tok = new Tokenizer(gtype);
    	String nam = tok.nextToken();
    	int num = tok.hasMoreTokens()?tok.intToken():players_in_game;
    	long ran = tok.hasMoreTokens()?tok.longToken():0;
        doInit(nam,ran,num);
        // note that firstPlayer is NOT initialized here
    }


    public int getNextPlayer() { return((whoseTurn+1)%players_in_game); }
    public int getNextPlayer(int pl)  { return((pl+1)%players_in_game); }
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player from state %s",board_state);
        case REBID_STATE:
        case FINANCEER_STATE:
        	// in rebid state, several players made the same bid and get to increase their offers.
        	// the other players are not elgible to bid and do not get a turn.  
        	do {
        		whoseTurn=(whoseTurn+1)%players_in_game;
        	} while(!playerBoard[whoseTurn].requestingBid && playerBoard[whoseTurn].cannotRebid);
        	break;
        case FUND_LOAN_STATE:
        case AUCTION_STATE:
        	// in fund and auction state, some player started the activity, then
        	// each other player in turn gets to accept or decline.
        	whoseTurn=(whoseTurn+1)%players_in_game;
        	break;
        case GAMEOVER_STATE:
        case PUZZLE_STATE:
            break;
		case CONFIRM_AUCTION_STATE:
        case CONFIRM_STATE:
        case REPRICE_FACTORY_STATE:
        case LOAD_SHIP_STATE:
        case REPRICE_WAREHOUSE_STATE:
        case LOAD_WAREHOUSE_GOODS_STATE:
        case RESIGN_STATE:
        	{
        	playerBoard bd = playerBoard[whoseTurn];
            moveNumber++; //the move is complete in these states
           
            // add penalties to virtual cash
       		if(hasBoughtMachine && !hasProduced) 
       			{ bd.virtual_cash +=  (int)(100*BLANK_MACHINE_PENALTY); }
          	if(hasBoughtWarehouse && !hasBoughtWarehouseGoods)
          		{  bd.virtual_cash += (int)(100*BLANK_WAREHOUSE_PENALTY);}
          	// penalize incomplete production
          	if(factoryGoodsProduced>0) 
          	{	int machines = bd.numberOfActiveMachines();
          		if(machines!=factoryGoodsProduced)
          		{
          		int dif = (int)(Math.abs(factoryGoodsProduced-machines)*UNDER_PRODUCTION_PENALTY*100);
          		bd.virtual_cash += dif;
          		//G.print("VF up " + dif);
         		}
          	}
          	if(warehouseGoodsBought>0) 
          	{	int warehouses = bd.numberOfActiveWarehouses();
          		if(warehouses!=warehouseGoodsBought)
          		{
          		int dif = (int)(Math.abs(warehouseGoodsBought-warehouses)*UNDER_PURCHASE_PENALTY*100);
          		bd.virtual_cash += dif;
          		//G.print("VW up " + dif);
          		}
          	}

            bd.clearProduced();		// clear the production flags
			whoseTurn=(whoseTurn+1)%players_in_game;
			isSecondAction=false;
			playerSource = -1;
			hasBoughtWarehouseGoods = hasRepricedFactory = hasRepricedWarehouse = hasProduced=false;
	        loadedShipFrom = -1;
	        loadedWarehouseFrom = -1;
	        movedToSeaFrom = -1;

			mustPayLoan=false;
			produceLuxuryFirstColor=null;
			produceLuxuryFromRack = ContainerId.FactoryGoodsLocation;
			hasProducedLuxury = false;
			hasBoughtMachine = false;
			hasBoughtWarehouse = false;
			warehouseGoodsBought = factoryGoodsProduced = 0;
			stackEmptyState = ContainerState.PLAY1_STATE;
			setState(ContainerState.PLAY1_STATE);
        	}
            return;
        }
    }
    
    void payInterest()
    {	clearBidInfo();
		playerBoard bd = playerBoard[whoseTurn];
		if(!bd.payInterest(false))
			{	// can't pay
			setState(ContainerState.TAKE_LOAN_STATE);
			mustPayLoan = true;
			}
			else
			{ mustPayLoan=false; 
			}				
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return true if the current board state allows a Done
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {
         case CONFIRM_STATE:
         case CONFIRM_AUCTION_STATE:
         case LOAD_SHIP_STATE:
         case REPRICE_FACTORY_STATE:
         case REPRICE_WAREHOUSE_STATE:
         case LOAD_WAREHOUSE_GOODS_STATE:	// buying goods, we can always stop buying
            return (true);

        default:
            return (false);
        }
    }
    
    public boolean DigestState()
    {	switch(board_state)
    	{
    	case PLAY1_STATE:
    	case PLAY2_STATE:
    		return(true);
    	default: 
    		return(false);
    	}
    }
    boolean gameOverNow()
    {	int empty = 0;
    	if((sales_this_move==0) && ((moveNumber-last_sale_move)>players_in_game)) 
    		{ return(true); }
    	// don't check the gold containers in the original game.
    	for(int i=0,lim=second_shipment?containerStorage.length:containerStorage.length-1; i<lim;i++) 
    		{ if(containerStorage[i].topChip()==null) { empty++; }
    		}
    	return(empty>=2);
    }
    void setGameOver()
    {	setState(ContainerState.GAMEOVER_STATE);
     }

    public void setScores(ContainerGoalSet goalSet)
    {	int score[] = goalSet.scores;
    	boolean win[] = goalSet.win;
    	for(int i=0,lim=players_in_game;i<lim; i++)
		{	score[i] = currentScoreForPlayer(i,goalSet);
		}
       	for(int lim=playerBoard.length-1; lim>=0; lim--)
		{ win[lim] = WinForPlayerInternal(lim,score);  
		}
    }
    public boolean WinForPlayerInternal(int player,int score[])
    {	if(board_state==ContainerState.GAMEOVER_STATE)
    	{	int myscore = -1000;
    		int maxscore = -1000;
    		// return "win" only if it is unique. tie scores are not a win
    		for(int i=0,lim=players_in_game;i<lim; i++)
    			{	int sc = score[i];
    				score[i] = sc;
    				if(i==player) { myscore = sc; }
    				else { maxscore = Math.max(maxscore,sc); }
    			}
    		return(myscore>maxscore);
    	}
    	return(false);
    }
    public boolean WinForPlayer(int player,ContainerGoalSet goalSet)
    {	setScores(goalSet);
    	return(WinForPlayerInternal(player,goalSet.scores));
    }

    /**
     * this is the real score, as if the game just ended.
     * @param pl
     * @return
     */
    public int scoreForPlayer(int pl)
    {	if(pl<playerBoard.length) { return(currentScoreForPlayer(pl,masterGoalSet)); }
    	else { return(0); }
    }
    int currentScoreForPlayer(int pl,ContainerGoalSet goalset)
    {	// the actual score has several components.  
    	// Cash minus loans + vig
    	// goods on the island
    	// salvage value of containers on the ships and in the warehouse
    	playerBoard bd = playerBoard[pl];
    	int islandScore = bd.islandGoodsValue(goalset);
    	int shipGoods = bd.shipGoods.height()*ENDGAME_SHIPGOODS_VALUE;
    	int warehouseGoods = bd.warehouseStorageUsed()*ENDGAME_WAREHOUSEGOODS_VALUE;
    	return(bd.cash + islandScore + shipGoods + warehouseGoods - bd.total_loan_amount + bd.total_loans_made);
    }

    /**
     * this is an estimate of the score, used by the robot, based on "stage of game" heuristics
     * and a goal set which may be a guess.
     * @param pl
     * @param game_stage
     * @param goalSet
     * @return
     */
    double currentEstimatedScoreForPlayer_v4(int pl,double game_stage,ContainerGoalSet goalSet)
    {	// the actual score has several components.  
    	// Cash minus loans + vig
    	// goods on the island
    	// salvage value of containers on the ships and in the warehouse
    	playerBoard bd = playerBoard[pl];
    	ContainerChip goal = goalSet.goals[pl];
    	double islandScore = estimatedIslandGoodsValue_v4(goal,bd.islandGoods,game_stage,ROBOT_UNIFORM_ISLAND_GOODS);
    	int shipGoods = bd.shipGoods.height()*ENDGAME_SHIPGOODS_VALUE;
    	int warehouseGoods = bd.warehouseStorageUsed()*ENDGAME_WAREHOUSEGOODS_VALUE;
    	return(bd.cash + islandScore + shipGoods + warehouseGoods - bd.total_loan_amount + bd.total_loans_made);
    }
    // make a rough guess how many more turns we get
    double estimated_turns_remaining_v4()
    {
    	ContainerCell least = containerStorage[0];
    	ContainerCell second = containerStorage[1];
    	if(least.height()>second.height()) { ContainerCell c = least; least = second; second = c; }
    	
    	for(int i=2;i<(first_shipment?MACHINE_COLORS:containerStorage.length);i++)
    	{	ContainerCell c = containerStorage[i];
    		int h = c.height();
    		if(h<=least.height()) { second = least; least=c; }
    		else if(h<second.height()) { second = c; }
    	}
    	double hv = 2.0*(least.height()+second.height()+1)/players_in_game;
    	return(hv);
    }
    // make a rough guess how many more turns we get
    double estimated_turns_remaining_v5()
    {
    	ContainerCell least = containerStorage[0];
    	ContainerCell second = containerStorage[1];
    	if(least.height()>second.height()) { ContainerCell c = least; least = second; second = c; }
    	
    	for(int i=2;i<(first_shipment?MACHINE_COLORS:containerStorage.length);i++)
    	{	ContainerCell c = containerStorage[i];
    		int h = c.height();
    		if(h<=least.height()) { second = least; least=c; }
    		else if(h<second.height()) { second = c; }
    	}
    	double hv = 3.5*(least.height()+second.height())/players_in_game;
    	return(hv);
    }
    /**
     * game_stage is a number 0.0-1.0 representing a guess what percentage complete the game is.
     * It's used to interpolate between heuristics appropriate for the beginning of the game and
     * those appropriate for the end.
     * @param turns
     * @return an estimate of the percent done the game is
     */
    public double game_stage_v4(double turns)
    {
    	double game_stage = (1.0-Math.min(1.0,Math.max(0.0,turns/16)));	// 0.0 for beginning 1.0 for end
    	return(game_stage);
    }
    public double game_stage_v4()
    {
    	return(game_stage_v4(estimated_turns_remaining_v4()));
    }
    public double game_stage_v5(double turns)
    {
    	double game_stage = (1.0-Math.min(1.0,Math.max(0.0,turns/25)));	// 0.0 for beginning 1.0 for end
    	return(game_stage*game_stage);
    }
    public double game_stage_v5()
    {
    	return(game_stage_v5(estimated_turns_remaining_v5()));
    }
    public double estimatedIslandGoodsValue_v4(ContainerChip goal,ContainerCell goods[],double game_stage,double preest[])
    {
    	int realV = CC.islandGoodsValue(goal,goods);
    	return(estimatedIslandGoodsValue_v4(goods,game_stage,preest,realV));
    }
    public double estimatedIslandGoodsValue_v5(ContainerChip goal,ContainerCell goods[],double game_stage)
    {
    	int realV = CC.islandGoodsValue(goal,goods);
    	int index = goal.getCardIndex();
    	return(estimatedIslandGoodsValue_v5(goods,game_stage,CC.preestGoalCardValues[index],realV,CC.realGoalCardValues[index]));
    }
    //
    // this is used to set bid amounts
    //
    public double estimatedIslandGoodsValue_v4(ContainerCell goods[],double game_stage,double preest[],int realV)
    {
    	// cash value for all island goods we own
    	double baseV = 0.0;
    	double fakeV = CC.fixedIslandGoodsValue(preest,goods);
       	// special logic for the opening stages; if the real value is zero, value the first couple of goods at full value.
    	if(realV==0)
    	{	double tot = 0.0;
    		for(int lim=preest.length-1; lim>=0; lim--) { tot += preest[lim]; }
    		baseV = Math.min(fakeV,2*tot/preest.length);
    		fakeV -= baseV;
    	} else
    	{
    	int unbalanced = CC.unbalancedMost(goods);
    	if((unbalanced>=0)&&(unbalanced<MACHINE_COLORS)) 
    		{ double pre = preest[unbalanced];
    		  baseV += pre; 
    		}
    	}
     	double sum = baseV+((1.0-game_stage)*fakeV)+(game_stage*realV);
    	return(sum);			// gradually increase the importance of actual island goods
    }
    
    double currentEstimatedScoreForPlayer_v5(int pl,double game_stage,ContainerGoalSet goalSet)
    {	// the actual score has several components.  
    	// Cash minus loans + vig
    	// goods on the island
    	// salvage value of containers on the ships and in the warehouse
    	playerBoard bd = playerBoard[pl];
    	ContainerChip goal = goalSet.goals[pl];
    	double islandScore = estimatedIslandGoodsValue_v5(goal,bd.islandGoods,game_stage);
    	int shipGoods = bd.shipGoods.height()*ENDGAME_SHIPGOODS_VALUE;
    	int warehouseGoods = bd.warehouseStorageUsed()*ENDGAME_WAREHOUSEGOODS_VALUE;
    	return(bd.cash + islandScore + shipGoods + warehouseGoods - bd.total_loan_amount + bd.total_loans_made);
    }
    
    public double estimatedIslandGoodsValue_v5(ContainerCell goods[],double game_stage,double preest[],
    		int realV,int realValueSet[])
    {
    	// cash value for all island goods we own
    	double baseV = 0.0;
    	double fakeV = CC.fixedIslandGoodsValue(preest,goods);
       	// special logic for the opening stages; if the real value is zero, value the first couple of goods at full value.
    	if(realV==0)
    	{	double tot = 0.0;
    		for(int lim=preest.length-1; lim>=0; lim--) { tot += preest[lim]; }
    		baseV = Math.min(fakeV,2*tot/preest.length);
    		fakeV -= baseV;
    	} else
    	{
    	int unbalanced = CC.unbalancedMost(goods);
    	if((unbalanced>=0)&&(unbalanced<MACHINE_COLORS)) 
    		{ double pre = preest[unbalanced];
    		  if(realValueSet[unbalanced]>=5) { pre = -pre*2; }
    		  baseV += pre; 
    		}
    	}
     	double sum = baseV+((1.0-game_stage)*fakeV)+(game_stage*realV);
    	return(sum);			// gradually increase the importance of actual island goods
    }   
    // estimate the value of the next gold bar on the island
    public int currentValueOfGold()
    {	int max = 0;
    	for(int i=playerBoard.length-1; i>=0; i--)
    		{	playerBoard bd = playerBoard[i];
    			max = Math.max(max,CC.valueOfGold(bd.islandGoods));
    		}
    	return(max);
    }
    //
    // return an array of weights for types of goods on the island
    // estimated by assuming that scarce goods are more valuable.
    //
    public double[] islandGoodValues()
    {	double goodsOfType[] = new double[CONTAINER_COLORS];
    	for(int i=0;i<CONTAINER_COLORS; i++) { goodsOfType[i]=1.0; }
    	return(goodsOfType);
    }
    public double [] isLandGoodValues0()
    {	double goodsOfType[] = new double[CONTAINER_COLORS];
    	int totalGoods = CONTAINER_COLORS;
    	for(int lim=playerBoard.length-1; lim>=0; lim--)
    	{	playerBoard bd = playerBoard[lim];
    		ContainerCell goods[] = bd.islandGoods;
    		for(int i=0;i<CONTAINER_COLORS; i++)
    		{	int siz = goods[i].height();
    			totalGoods += siz;
    			goodsOfType[i] += siz;
    		}
     	} 
    	for(int i=0;i<CONTAINER_COLORS; i++) 
    		{ goodsOfType[i] = totalGoods/(CONTAINER_COLORS*(goodsOfType[i]+1)); 
    		}
    	return(goodsOfType);
    }
    public double[] shipGoodValues(double []igValues)
    {	return(igValues);
    }
    public double[] warehouseGoodValues(double []igValues)
    {	return(igValues);
    }
    public double[] factoryGoodValues(double []wgValues)
    {	return(wgValues);
    }
    
   
    // setup the island value estimates.
    // return the real value of the current player island
   int setupIslandGoodsValues(int forPlayer,ContainerGoalSet goalSet)
   {
	   for(int i=islandValueBaseline.length-1; i>=0; i--)
		   {
		   // get the estimated net value of each island slow
		   islandValueBaseline[i] = CC.islandGoodsValue(goalSet.goals[i],playerBoard[i].islandGoods);
		   }
	   for(int i=islandValueAdded.length-1; i>=0; i--)  { islandValueAdded[i] = VALUE_NOT_SET; }
	   return(islandValueBaseline[forPlayer]);
   }
   // estimate the value of a good at the island - which we assume to be the maximum
   // value it would add to any player's zone.
   int estimatedIslandGoodValue_v4(ContainerGoalSet goalSet,ContainerChip good)
   {
   
	   int index = good.getContainerIndex();
   		int val = islandValueAdded[index];
   		if(val==VALUE_NOT_SET)
   		{
   			// not cached yet, value the good for each of the players, diff with the baseline for that player
   		   int maxv = VALUE_NOT_SET;
   		   for(int i=islandValueBaseline.length-1; i>=0; i--)
   		   {
   			   int dif = CC.islandGoodsValue(goalSet.goals[i],playerBoard[i].islandGoods,good) - islandValueBaseline[i];
   			   if((maxv==VALUE_NOT_SET)||(dif>maxv)) { maxv = dif; islandValueTo[index]=i; }
   		   }
   		   val = islandValueAdded[index] = maxv;
   		}
   		return(val);
   }
   // estimate the value of a good at the island - which we assume to be the maximum
   // value it would add to any player's zone.
   int estimatedIslandGoodValue_v5(ContainerGoalSet goalSet,ContainerChip good,int forplayer)
   {
	    int index = good.getContainerIndex();
   		int val = islandValueAdded[index];
   		if(val==VALUE_NOT_SET)
   		{
   			// not cached yet, value the good for each of the players, diff with the baseline for that player
   		   int maxv = VALUE_NOT_SET;
   		   int maxi = -1;
   		   for(int i=islandValueBaseline.length-1; i>=0; i--)
   		   { if(i!=forplayer)
   		   	{
   			   int dif = CC.islandGoodsValue(goalSet.goals[i],playerBoard[i].islandGoods,good) - islandValueBaseline[i];
   			   if((maxv==VALUE_NOT_SET)||(dif>maxv)) { maxv = dif; maxi=i; }
   		   	}
   		   }
   		   val = islandValueAdded[index] = maxv;
   		   islandValueTo[index]=maxi;
   		}
   		return(val);
   }
    // estimate the score of a player.  this version uses the "best guess" of the player's goal 
    // rather than the actual goal, which is technically legal.  This version includes an estimate
    // of the value of ship goods at auction and an estimate for the value of goods on the warehouse
    // and factory
  double FACTORY_GOODS_PRICE_MULTIPLIER = 0.17;
  double WAREHOUSE_GOODS_PRICE_MULTIPLIER = 0.4;
  double ROBOT_UNIFORM_ISLAND_GOODS[] = {5.5, 5.5, 5.5, 5.5, 5.5};
  int ROBOT_CAPITAL_TARGET = 25;

  // this version is frozen for "dumbot"
  public double ScoreForPlayer4(ContainerPlay robot,int scoreForPlayer,boolean print)
  { ContainerGoalSet myGoalSet = robot.robotGoalSet();
	double FIXED_ASSET_MULTIPLIER = 1.5;
  	double OFF_PRICE_PENALTY = -0.1;
  	playerBoard bd = playerBoard[scoreForPlayer];
  	double turns = robot.estimated_turns_remaining();
      double horizon = Math.sqrt(turns);	// rough guess at the time remaining
  	double game_stage = robot.game_stage(turns);
  	double endgame_stage = (game_stage<0.8)?1.0:((1.0-game_stage)/0.2);
  	double machine_ratio = (machinesInPlay()+0.0)/warehousesInPlay();
  	double finalv=0;
  	double factoryV = 0;
  	double warehouseV = 0;
  	int realV = setupIslandGoodsValues(scoreForPlayer,myGoalSet);
  	String msg = "";
   	int cash = bd.cash;
   	int active_loans = bd.loans_taken;
  	int fixed_assets = 0;
  	int working_capital = cash+((MAX_LOANS-active_loans)*STANDARD_LOAN_AMOUNT)-active_loans;
  	machine_ratio = machine_ratio*machine_ratio;	// exaggerate the difference.
   	finalv += cash;
   	// the differential based on cash is to discoutage the robot from taking loans it doesn't really need.
  	finalv -= bd.total_loan_amount + active_loans*horizon*((cash>=10)?3:2);	// penalty of $1 for each loan forever
  	finalv += bd.total_loans_made + horizon*2*bd.loans_made;	// bonus of $2 for each loan made
  	if(print) 
  	{	msg += "Cash "+cash +" Hor "+horizon+" Stage "+game_stage+" endgame "+endgame_stage;
  	}
  	finalv = finalv*Math.sqrt(game_stage);		// cash is trash, value the future
  	// insist on working capital though.  without cash you are screwed.
  	if(working_capital<ROBOT_CAPITAL_TARGET) { finalv -= (ROBOT_CAPITAL_TARGET-working_capital) * (1.0-game_stage); }
  	
  	// cash potential of all the machines we own
  	{
  	double machine_weights[] = {1.4,1.4,1.0,0.6,0.4};
  	double machine = 0.0;
  	for(int i=0;i<bd.machines.length;i++)
  	{	ContainerCell m = bd.machines[i];
  		ContainerChip ch = m.topChip();
  		if(ch!=null)
  		{
  		int machines_sold = machines_sold(ch);
  		// make machines more valuable if there are fewer of them in use.
  		double machine_weight = machine_weights[machines_sold];
  		double mv = (machine_weight*turns*endgame_stage)/machine_ratio; // $2 per machine per turn except near endgame
  		machine += mv;
  		finalv += mv;
  		fixed_assets += MACHINE_PRICES[i];
  		}	
  	}
  		if(print) { G.print("Future machine4 "+ machine+" in "+turns+ " turns ");}
  		}
  	
  	// cash potential of all the warehouses we own
  	{
  	double warehouse_weights[] = {1.0,1.4,1.4,0.3,0.2};
  	for(int i=0;i<bd.warehouses.length;i++)
  	{	ContainerCell m = bd.warehouses[i];
  		if(m.topChip()!=null)
  			{ double v = (warehouse_weights[i]*horizon*endgame_stage)*machine_ratio; // $2 per warehouse per turn
   			  finalv += v; 
  			  fixed_assets += WAREHOUSE_PRICES[i];
  			}		
  	}}
  	
  	if(fixed_assets*FIXED_ASSET_MULTIPLIER>working_capital)
  	{
  	finalv -= (fixed_assets*FIXED_ASSET_MULTIPLIER-working_capital)*2;	// penalty for excess fixed assets
  	}  	 
    // cash value of all factory goods we own
  	for(int i=0;i<bd.factoryGoods.length; i++)
  	{	ContainerCell c = bd.factoryGoods[i];
  		for(int h = c.height()-1; h>=0; h--)
  			{ ContainerChip ch = c.chipAtIndex(h);
  			  int goodValue = estimatedIslandGoodValue_v4(myGoalSet,ch);
  			  // value at 1/4 the value at the island, less near the endgame or of there are too many machines
  			  double vv = (goodValue * FACTORY_GOODS_PRICE_MULTIPLIER * endgame_stage) / machine_ratio;
  			  if(((int)vv)!=FACTORY_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
  			  factoryV += vv;
  			  finalv += 2*vv;
  			}
  	}
  	// cash value for all warehouse goods we own
    for(int i=0;i<bd.warehouseGoods.length; i++)
  	{	ContainerCell c = bd.warehouseGoods[i];
			for(int h = c.height()-1; h>=0; h--)
			{ ContainerChip ch = c.chipAtIndex(h);
			  int goodValue = estimatedIslandGoodValue_v4(myGoalSet,ch);
			  // value at 1/2 the value at the island, less near the endgame or of there are too many warehouses
			  double vv = (goodValue * WAREHOUSE_GOODS_PRICE_MULTIPLIER * endgame_stage) * machine_ratio;
			  if(endgame_stage<1.0) { vv = Math.max(vv,ENDGAME_WAREHOUSEGOODS_VALUE); }
			  if(((int)vv)!=WAREHOUSE_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
			  warehouseV += vv;
			  finalv += vv;
			}
  	}
    	if(bd.shipLocation.rackLocation()==ContainerId.AtIslandParkingLocation)
    	{
    		finalv -= 0.5;		// penalty for hanging around in the parking lot
    	}
  	// cash value for all ship goods we own
  	{	ContainerCell c = bd.shipGoods;
  		int nGoods  = c.height();
  		if(nGoods>0)
  		{
  		double shipgoodvalue = bd.current_ship_island_value;
  		if(shipgoodvalue<-99)
  			{
  			int val = bd.current_ship_island_value = valueAtAuction_v4(scoreForPlayer,game_stage,myGoalSet,print);
  			if(endgame_stage<1.0)
  				{ shipgoodvalue = Math.max(c.height()*ENDGAME_SHIPGOODS_VALUE,val*endgame_stage);	// downgrade near endgame
  				}
    			}
  		if(print) { G.print("Auction "+shipgoodvalue); }
  		
  		switch(bd.shipLocation.rackLocation())
  		{
  		default: throw G.Error("Not expecting a ship in location %s",bd.shipLocation.rackLocation);
  		case AtIslandParkingLocation: shipgoodvalue = 0.0; break;				// in the lot after selling - there won't be any containers
  		case AuctionLocation: shipgoodvalue -= nGoods*0.25; break;			// 6 per container on the block
  		case AtSeaLocation:	shipgoodvalue -= nGoods*1.0; break;				// 5 per container at sea
  		case AtDockLocation: shipgoodvalue -= nGoods*1.75; break;			// 4 per container on the dock
  		}
  		finalv += shipgoodvalue;								// $4 per ship good
  		}
  		
  	}
  	finalv += estimatedIslandGoodsValue_v4(bd.islandGoods,game_stage,ROBOT_UNIFORM_ISLAND_GOODS,realV);
  	if(print)
  	{ System.out.println("S "+scoreForPlayer+" Fac "+factoryV+" War "+warehouseV+" =" + finalv + " "+msg);
  	}
  	return(finalv);
  }

  // fake values for the opening, designed to emphasize the 2 and 10 columns
  double PASS_PENALTY = -2.5;
  double BLANK_MACHINE_PENALTY = -2.0;		// penalty for not using a new machine
  double BLANK_WAREHOUSE_PENALTY = -2.0;	// penalty for not using a new warehouse
  double UNDER_PRODUCTION_PENALTY = -1.5;	// penalty for not producing a full complement
  double UNDER_PURCHASE_PENALTY = -2.0;		// penalty for premature refill of warehouse
  
  private int abundance[] = new int[CONTAINER_COLORS];
  public double ScoreForPlayer5(ContainerPlay robot,int scoreForPlayer,boolean print)
  { ContainerGoalSet myGoalSet = robot.robotGoalSet();
  	int goalCardIndex = myGoalSet.goals[scoreForPlayer].getCardIndex();
  	double preestValueSet[] = CC.preestGoalCardValues[goalCardIndex];
  	int realValueSet[] = CC.realGoalCardValues[goalCardIndex];
	double FIXED_ASSET_MULTIPLIER = 1.5;
  	double OFF_PRICE_PENALTY = -0.1;
  	playerBoard bd = playerBoard[scoreForPlayer];
  	double turns = robot.estimated_turns_remaining();
      double horizon = Math.sqrt(turns);	// rough guess at the time remaining
  	double game_stage = robot.game_stage(turns);
  	double endgame_stage = (game_stage<0.8)?1.0:((1.0-game_stage)/0.2);
  	double pmachine_ratio = (machinesInPlay()+0.0)/warehousesInPlay();
  	double machine_ratio = pmachine_ratio*pmachine_ratio;	// exaggerate the difference.
 	int working_capital = estimate_available_cash(bd,game_stage,true);
   	int cash = bd.cash;
   	// consider the ship cost with the cash in this calculation, so the lost cash
   	// doesn't directly make us look poorer.
   	int ship_age = Math.max(0, moveNumber - bd.current_ship_changed);
   	int maxaga = 3*(playerBoard.length-1);
   	// this is an incentive to get goods to auction.  The ship appears to lose
   	// value each turn it is not added to or moved to the island.
   	double age_multiplier = (bd.shipLocation.rackLocation==ContainerId.AuctionLocation)
   								?1.0
   								:(maxaga-Math.min(ship_age,maxaga))/(double)maxaga;
   	double ship_value = bd.current_ship_cost*age_multiplier;
  	double finalv=(cash+ship_value)*Math.sqrt(game_stage);	// downgrade the importance of immdiate cash
  	double factoryV = 0;
  	double warehouseV = 0;
  	
  	int realV = setupIslandGoodsValues(scoreForPlayer,myGoalSet);
  	String msg = "";
   	int active_loans = bd.loans_taken;
  	int fixed_assets = 0;
  	double loan_penalty = -(bd.total_loan_amount + active_loans*horizon*2);	// penalty of $1 for each loan forever
   	// the differential based on cash is to discoutage the robot from taking loans it doesn't really need.
  	finalv += loan_penalty;
  	finalv += bd.total_loans_made + horizon*2*bd.loans_made;	// bonus of $2 for each loan made
  	finalv += bd.pass_count*PASS_PENALTY;
  	double vcash = (bd.virtual_cash/100.0);
  	finalv += vcash;
  	if(print) 
  	{	msg += "Cash "+cash +" "+((int)vcash)+" Loan "+loan_penalty+" Hor "+horizon+" Stage "+game_stage+" endgame "+endgame_stage + " = "+finalv;
  	}
  	// insist on working capital though.  without cash you are screwed.
  	if(working_capital<ROBOT_CAPITAL_TARGET) 
  		{ double working = (ROBOT_CAPITAL_TARGET-working_capital) * (1.0-game_stage);
  		  if(print) { G.print("Working "+working); }
  		  finalv -= working;
  		}
  	
  	// cash potential of all the machines we own
  	{
  	double machine_weights[] = {1.0,1.2,1.1,0.9,0.5};	// these weights needed to be adjusted for the more accurate game length in v5
  	double machine = 0.0;
  	for(int i=0;i<bd.machines.length;i++)
  	{	ContainerCell m = bd.machines[i];
  		ContainerChip ch = m.topChip();
  		if(ch!=null)
  		{
  		int machines_sold = machines_sold(ch);
  		// make machines more valuable if there are fewer of them in use.
  		double machine_weight = machine_weights[machines_sold];
  		double mv = (machine_weight*turns*endgame_stage)/machine_ratio; 
  		machine +=mv;
  		finalv += mv;
  		//if(print) { G.print("Ma sold "+machines_sold+" weight "+machine_weight+" val "+mv+" stage "+endgame_stage+" ratio "+machine_ratio);}
  		fixed_assets += MACHINE_PRICES[i];
  		}
  	}
  		if(print) { G.print("Future machine5 "+ machine+" in "+turns+ " turns ");}
  	}
  	// cash potential of all the warehouses we own
  	{
  	double warehouse_weights[] = {0.5,0.4,0.3,0.15,0.1}; // these weights needed to be adjusted for the more accurate game length in v5
  	double ware = 0.0;
  	for(int i=0;i<bd.warehouses.length;i++)
  	{	ContainerCell m = bd.warehouses[i];
  		if(m.topChip()!=null)
  			{ double v = (warehouse_weights[i]*horizon*endgame_stage)*machine_ratio; // $2 per warehouse per turn
   			  finalv += v;
   			  ware += v;
  			  fixed_assets += WAREHOUSE_PRICES[i];
  			}		
  	}
		if(print) { G.print("Future warehouse5 "+ ware+" in "+turns+ " turns ");}
  	}
  	
  	if(fixed_assets*FIXED_ASSET_MULTIPLIER>working_capital)
  	{
  	double fixed = (fixed_assets*FIXED_ASSET_MULTIPLIER-working_capital)*2;	// penalty for excess fixed assets
  	if(print) { G.print("Fixed asset "+fixed); }
  	finalv -= fixed;
  	}  	 
    	// cash value of all factory goods we own
  	for(int i=0;i<CONTAINER_COLORS;i++) {abundance[i]=0; }
  	for(int i=bd.factoryGoods.length-1; i>=0; i--)
  	{	ContainerCell c = bd.factoryGoods[i];
  		for(int h = c.height()-1; h>=0; h--)
  			{ ContainerChip ch = c.chipAtIndex(h);
  				// our estimate of the ultimate value of the container isn't good enough
  				// and if we let the value fall, the robot won't produce
  			  int goodValue = Math.max(STANDARD_CONTAINER_VALUE,estimatedIslandGoodValue_v5(myGoalSet,ch,scoreForPlayer));
  			  int chipIndex = ch.getContainerIndex();
  			  // value at 1/4 the value at the island, less near the endgame or of there are too many machines
  			  // abundance is a small penalty for second and subsequent containers of a same color, to
  			  // encourage diversity - ie; replace what was bought
  			  double vv = ((goodValue-abundance[chipIndex]*0.6) * FACTORY_GOODS_PRICE_MULTIPLIER * endgame_stage);
  			  abundance[chipIndex]++;
  			  if(((int)(vv+0.5))!=FACTORY_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
  			  factoryV += vv;
  			  finalv += vv;
  			}
  	}
  	// cash value for all warehouse goods we own
  	for(int i=0;i<CONTAINER_COLORS;i++) {abundance[i]=0; }
   	for(int i=bd.warehouseGoods.length-1; i>=0; i--)
  	{	ContainerCell c = bd.warehouseGoods[i];
			for(int h = c.height()-1; h>=0; h--)
			{ ContainerChip ch = c.chipAtIndex(h);
			  int chipIndex = ch.getContainerIndex();
			  int goodValue = estimatedIslandGoodValue_v5(myGoalSet,ch,scoreForPlayer);
			  // value at 1/2 the value at the island, less near the endgame or of there are too many warehouses
 			  // abundance is a small penalty for second and subsequent containers of a same color, to
  			  // encourage diversity - ie; replace what was bought
			  double vv = ((goodValue-abundance[chipIndex]*0.1) * WAREHOUSE_GOODS_PRICE_MULTIPLIER * endgame_stage);
			  if(endgame_stage<1.0) { vv = Math.max(vv,ENDGAME_WAREHOUSEGOODS_VALUE); }
			  if(((int)(vv+0.5))!=WAREHOUSE_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
			  abundance[chipIndex]++;
			  warehouseV += vv;
			  finalv += vv;
			}
  	}
    	if(bd.shipLocation.rackLocation==ContainerId.AtIslandParkingLocation)
    	{
    		finalv -= 0.5;		// penalty for hanging around in the parking lot
    	}
  	// cash value for all ship goods we own
  	{	ContainerCell c = bd.shipGoods;
  		int nGoods  = c.height();
  		if(nGoods>0)
  		{
  		double shipgoodvalue = bd.current_ship_island_value;
  		if(shipgoodvalue<-99)
  			{
  			// valueAtAuction takes the other player's cash into account, and their
  			// likelyhood ot use credit to purchase goods for the island.
  			shipgoodvalue = bd.current_ship_island_value = valueAtAuction_v5(scoreForPlayer,game_stage,myGoalSet,print);
   			}
  		if(print) { G.print("Auction "+shipgoodvalue); }
  		
  		switch(bd.shipLocation.rackLocation())
  		{
  		default: throw G.Error("Not expecting a ship in location %s",bd.shipLocation.rackLocation);
  		case AtIslandParkingLocation: shipgoodvalue = 0.0; break;				// in the lot after selling - there won't be any containers
  		case AuctionLocation: shipgoodvalue -= nGoods*0.5; break;			// 6 per container on the block
  		case AtSeaLocation:	shipgoodvalue -= nGoods*1.0; break;				// 5 per container at sea
  		case AtDockLocation: shipgoodvalue -= nGoods*1.5; break;			// 4 per container on the dock
  		}
  		finalv += shipgoodvalue;								// $4 per ship good
  		}
  		
  	}
  	double island = estimatedIslandGoodsValue_v5(bd.islandGoods,game_stage,preestValueSet,realV,realValueSet);
  	if(print) { G.print("Island "+island); }
  	finalv += island;
  	if(print)
  	{ System.out.println("S "+scoreForPlayer+" Fac "+factoryV+" War "+warehouseV+" =" + finalv + " "+msg);
  	}
  	return(finalv);
  }
  
  double PRODUCTION_DUTY_CYCLE = 0.5;
  
  public double ScoreForPlayer6(ContainerPlay robot,int scoreForPlayer,boolean print)
  { ContainerGoalSet myGoalSet = robot.robotGoalSet();
  	int goalCardIndex = myGoalSet.goals[scoreForPlayer].getCardIndex();
  	double preestValueSet[] = CC.preestGoalCardValues[goalCardIndex];
  	int realValueSet[] = CC.realGoalCardValues[goalCardIndex];
	double FIXED_ASSET_MULTIPLIER = 1.5;
  	double OFF_PRICE_PENALTY = -0.1;
  	playerBoard bd = playerBoard[scoreForPlayer];
  	double turns = robot.estimated_turns_remaining();
      double horizon = Math.sqrt(turns);	// rough guess at the time remaining
  	double game_stage = robot.game_stage(turns);
  	double endgame_stage = (game_stage<0.8)?1.0:((1.0-game_stage)/0.2);
  	double pmachine_ratio = (machinesInPlay()+0.0)/warehousesInPlay();
  	double machine_ratio = pmachine_ratio*pmachine_ratio;	// exaggerate the difference.
 	int working_capital = estimate_available_cash(bd,game_stage,true);
   	int cash = bd.cash;
   	// consider the ship cost with the cash in this calculation, so the lost cash
   	// doesn't directly make us look poorer.
   	int ship_age = Math.max(0, moveNumber - bd.current_ship_changed);
   	int maxaga = 3*(playerBoard.length-1);
   	// this is an incentive to get goods to auction.  The ship appears to lose
   	// value each turn it is not added to or moved to the island.
   	double age_multiplier = (bd.shipLocation.rackLocation()==ContainerId.AuctionLocation)
   								?1.0
   								:(maxaga-Math.min(ship_age,maxaga))/(double)maxaga;
   	double ship_value = bd.current_ship_cost*age_multiplier;
  	double finalv=(cash+ship_value)*Math.sqrt(game_stage);	// downgrade the importance of immdiate cash
  	double factoryV = 0;
  	double warehouseV = 0;
  	
  	int realV = setupIslandGoodsValues(scoreForPlayer,myGoalSet);
  	String msg = "";
   	int active_loans = bd.loans_taken;
  	int fixed_assets = 0;
  	double loan_penalty = -(bd.total_loan_amount + active_loans*horizon*2);	// penalty of $1 for each loan forever
   	// the differential based on cash is to discoutage the robot from taking loans it doesn't really need.
  	finalv += loan_penalty;
  	finalv += bd.total_loans_made + horizon*2*bd.loans_made;	// bonus of $2 for each loan made
  	finalv += bd.pass_count*PASS_PENALTY;
  	double vcash = (bd.virtual_cash/100.0);
  	finalv += vcash;
  	if(print) 
  	{	msg += "Cash "+cash +" "+((int)vcash)+" Loan "+loan_penalty+" Hor "+horizon+" Stage "+game_stage+" endgame "+endgame_stage + " = "+finalv;
  	}
  	// insist on working capital though.  without cash you are screwed.
  	if(working_capital<ROBOT_CAPITAL_TARGET) 
  		{ double working = (ROBOT_CAPITAL_TARGET-working_capital) * (1.0-game_stage);
  		  if(print) { G.print("Working "+working); }
  		  finalv -= working;
  		}
  	
  	// cash potential of all the machines we own
  	{
  	double machine_weights[] = {1.0,1.2,1.1,0.9,0.5};	// these weights needed to be adjusted for the more accurate game length in v5
  	double machine = 0.0;
  	for(int i=0;i<bd.machines.length;i++)
  	{	ContainerCell m = bd.machines[i];
  		ContainerChip ch = m.topChip();
  		if(ch!=null)
  		{
  		int machines_sold = machines_sold(ch);
  		// make machines more valuable if there are fewer of them in use.
  		double machine_weight = machine_weights[machines_sold];
  		// the PRODUCTION_DUTY_CYCLE below reflects the assumption that we only get to produce
  		// on half of the turns, just because there's lots else to do.
  		// this is intended to prevent overvaluing of machines.
  		double mv = (machine_weight*turns*endgame_stage*PRODUCTION_DUTY_CYCLE)/machine_ratio; 
  		machine +=mv;
  		finalv += mv;
  		//if(print) { G.print("Ma sold "+machines_sold+" weight "+machine_weight+" val "+mv+" stage "+endgame_stage+" ratio "+machine_ratio);}
  		fixed_assets += MACHINE_PRICES[i];
  		}
  	}
  		if(print) { G.print("Future machine5 "+ machine+" in "+turns+ " turns ");}
  	}
  	// cash potential of all the warehouses we own
  	{
  	double warehouse_weights[] = {0.5,0.4,0.3,0.15,0.1}; // these weights needed to be adjusted for the more accurate game length in v5
  	double ware = 0.0;
  	for(int i=0;i<bd.warehouses.length;i++)
  	{	ContainerCell m = bd.warehouses[i];
  		if(m.topChip()!=null)
  			{ double v = (warehouse_weights[i]*horizon*endgame_stage)*machine_ratio; // $2 per warehouse per turn
   			  finalv += v;
   			  ware += v;
  			  fixed_assets += WAREHOUSE_PRICES[i];
  			}		
  	}
		if(print) { G.print("Future warehouse5 "+ ware+" in "+turns+ " turns ");}
  	}
  	
  	if(fixed_assets*FIXED_ASSET_MULTIPLIER>working_capital)
  	{
  	double fixed = (fixed_assets*FIXED_ASSET_MULTIPLIER-working_capital)*2;	// penalty for excess fixed assets
  	if(print) { G.print("Fixed asset "+fixed); }
  	finalv -= fixed;
  	}  	 
    	// cash value of all factory goods we own
  	for(int i=0;i<CONTAINER_COLORS;i++) {abundance[i]=0; }
  	for(int i=bd.factoryGoods.length-1; i>=0; i--)
  	{	ContainerCell c = bd.factoryGoods[i];
  		for(int h = c.height()-1; h>=0; h--)
  			{ ContainerChip ch = c.chipAtIndex(h);
  				// our estimate of the ultimate value of the container isn't good enough
  				// and if we let the value fall, the robot won't produce
  			  int goodValue = Math.max(STANDARD_CONTAINER_VALUE,estimatedIslandGoodValue_v5(myGoalSet,ch,scoreForPlayer));
  			  int chipIndex = ch.getContainerIndex();
  			  // value at 1/4 the value at the island, less near the endgame or of there are too many machines
  			  // abundance is a small penalty for second and subsequent containers of a same color, to
  			  // encourage diversity - ie; replace what was bought
  			  double vv = ((goodValue-abundance[chipIndex]*0.6) * FACTORY_GOODS_PRICE_MULTIPLIER * endgame_stage);
  			  abundance[chipIndex]++;
  			  if(((int)(vv+0.5))!=FACTORY_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
  			  factoryV += vv;
  			  finalv += vv;
  			}
  	}
  	// cash value for all warehouse goods we own
  	for(int i=0;i<CONTAINER_COLORS;i++) {abundance[i]=0; }
   	for(int i=bd.warehouseGoods.length-1; i>=0; i--)
  	{	ContainerCell c = bd.warehouseGoods[i];
			for(int h = c.height()-1; h>=0; h--)
			{ ContainerChip ch = c.chipAtIndex(h);
			  int chipIndex = ch.getContainerIndex();
			  int goodValue = estimatedIslandGoodValue_v5(myGoalSet,ch,scoreForPlayer);
			  // value at 1/2 the value at the island, less near the endgame or of there are too many warehouses
 			  // abundance is a small penalty for second and subsequent containers of a same color, to
  			  // encourage diversity - ie; replace what was bought
			  double vv = ((goodValue-abundance[chipIndex]*0.1) * WAREHOUSE_GOODS_PRICE_MULTIPLIER * endgame_stage);
			  if(endgame_stage<1.0) { vv = Math.max(vv,ENDGAME_WAREHOUSEGOODS_VALUE); }
			  if(((int)(vv+0.5))!=WAREHOUSE_GOOD_PRICES[i]) { vv+=OFF_PRICE_PENALTY; }
			  abundance[chipIndex]++;
			  warehouseV += vv;
			  finalv += vv;
			}
  	}
    	if(bd.shipLocation.rackLocation()==ContainerId.AtIslandParkingLocation)
    	{
    		finalv -= 0.5;		// penalty for hanging around in the parking lot
    	}
  	// cash value for all ship goods we own
  	{	ContainerCell c = bd.shipGoods;
  		int nGoods  = c.height();
  		if(nGoods>0)
  		{
  		double shipgoodvalue = bd.current_ship_island_value;
  		if(shipgoodvalue<-99)
  			{
  			// valueAtAuction takes the other player's cash into account, and their
  			// likelyhood ot use credit to purchase goods for the island.
  			shipgoodvalue = bd.current_ship_island_value = valueAtAuction_v5(scoreForPlayer,game_stage,myGoalSet,print);
   			}
  		if(print) { G.print("Auction "+shipgoodvalue); }
  		
  		switch(bd.shipLocation.rackLocation())
  		{
  		default: throw G.Error("Not expecting a ship in location %s",bd.shipLocation.rackLocation);
  		case AtIslandParkingLocation: shipgoodvalue = 0.0; break;				// in the lot after selling - there won't be any containers
  		case AuctionLocation: shipgoodvalue -= nGoods*0.5; break;			// 6 per container on the block
  		case AtSeaLocation:	shipgoodvalue -= nGoods*1.0; break;				// 5 per container at sea
  		case AtDockLocation: shipgoodvalue -= nGoods*1.5; break;			// 4 per container on the dock
  		}
  		finalv += shipgoodvalue;								// $4 per ship good
  		}
  		
  	}
  	double island = estimatedIslandGoodsValue_v5(bd.islandGoods,game_stage,preestValueSet,realV,realValueSet);
  	if(print) { G.print("Island "+island); }
  	finalv += island;
  	if(print)
  	{ System.out.println("S "+scoreForPlayer+" Fac "+factoryV+" War "+warehouseV+" =" + finalv + " "+msg);
  	}
  	return(finalv);
  }

  //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        pickedSourceStack.clear();
        droppedDestStack.clear();
        pickedIndex.clear();
     }
    //
    // undo the drop, restore the moving object to moving status and revert the
    // board state to the previous board state
    //
    private boolean unDropObject()
    {
    ContainerCell dr = getDest();
    if(dr!=null)
    	{
    	switch(board_state)
    	{
    	default:
    		throw G.Error("Not expecting undrop in state %s",board_state);
    	case LOAD_LUXURY_STATE:
    	case PLAY1_STATE:
    	case PLAY2_STATE:
    	case CONFIRM_STATE:
		case CONFIRM_AUCTION_STATE:
    	case LOAD_SHIP_STATE:
    	case LOAD_SHIP_1_STATE:
    	case LOAD_FACTORY_GOODS_STATE:
    	case TRADE_CONTAINER_STATE:
    	case REPRICE_FACTORY_STATE:
    	case REPRICE_WAREHOUSE_STATE:
    	case LOAD_WAREHOUSE_GOODS_STATE:
    		ContainerCell dest = droppedDestStack.pop();
    		pickedObject = dest.removeTop();
    	
	    	
	    if(board_state==ContainerState.LOAD_LUXURY_STATE)	// has dropped the trade goods, needs to pick up a gold
		{									// but instead picks up the trade goods again
		   	setState(ContainerState.TRADE_CONTAINER_STATE);
		  	return(true);
		}
		    
	    ContainerCell source = pickedSourceStack.top();
	    	
	    if((source.rackLocation==ContainerId.ContainerLocation)&&(source.row==MACHINE_COLORS))
	    {	// completed the gold transaction but picked up the gold instead of clicking done
	    	setState(ContainerState.LOAD_LUXURY_STATE);
	    	return(true);
	    }
    	
	    switch(source.rackLocation())
    	{
    	case LoanLocation:
    		{	if(dest==loanCards)
    			{	// repaying a loan, now cancelled
    				//playerBoard bd = playerBoard[whoseTurn];
    				//bd.fundLoan(source);
    				unstackCashInfo(undoStack,false);
    				setState(ContainerState.PLAY1_STATE);
    			}
    			else if (source==loanCards)
    			{
    			ContainerState old = undoStateStack.pop();	// cancel taking a loan
    			setState(old);
    			}
    		} 
    		break;
    	case MachineLocation:
     		// undo pay for a new machine
    		{	int price = MACHINE_PRICES[dr.row];
       			playerBoard srcBd = getPlayerBoard(dr);
       			sales_this_move--;
       			if(!ROBOT_BOARD)
       				{
       				srcBd.machine_turns--;
       				srcBd.machine_cash_out -= price;
       				}
       			hasBoughtMachine = false;
    			transfer_from_bank(price,srcBd);
    			setState(stackEmptyState);
    		}
    		break; 
    		
    	case WarehouseLocation:
     		// undo pay for a new warehouse
    		{	int price = WAREHOUSE_PRICES[dr.row];
       			playerBoard srcBd = getPlayerBoard(source);
       			playerBoard myBd = playerBoard[whoseTurn];
       			if(!ROBOT_BOARD) { myBd.warehouse_turns--; }
       			if(srcBd==myBd)
       			{
       			if(pickedSourceStack.size()==0) { setState(stackEmptyState); }
       			}
       			else
       			{
       			sales_this_move--;
    			transfer_from_bank(price,myBd);
    			myBd.warehouse_cash_out -= price;
    			hasBoughtWarehouse = false;
    			setState(stackEmptyState);
       			}
    		}
    		break;
    	case FactoryGoodsLocation:
    		{	// pick up a container from your factory.  If this was the first container, back up into free move 
    		// state. Otherwise remain in load warehouse state.  Also Reverse the payment.
    		int price = FACTORY_GOOD_PRICES[source.row];
    		playerBoard sourceBd = getPlayerBoard(source);
    		playerBoard myBd = playerBoard[whoseTurn];
    		if(sourceBd==myBd)
    		{
    		if(droppedDestStack.size()==0) 
    			{ 
    			  if(dr.rackLocation()==ContainerId.ContainerLocation)
    			  {	produceLuxuryFromRack = ContainerId.FactoryGoodsLocation;
    				produceLuxuryFirstColor = null;
    			  }
    			  else if(!ROBOT_BOARD && (stackEmptyState!=ContainerState.REPRICE_FACTORY_STATE)) 
      			  { sourceBd.machine_turns--; } 
    			  setState(stackEmptyState);
    			  
    			}
    		}
    		else
    		{
    		sales_this_move--;
    		warehouseGoodsBought--;
    		G.Assert(warehouseGoodsBought>=0, "zero limit");
       		transfer(price,sourceBd,myBd,false);		// give the money back
       		sourceBd.machine_cash_in -= price;
       		myBd.warehouse_cash_out -= price;
       		if(droppedDestStack.size()==0)
       		{
        		if(!ROBOT_BOARD) { myBd.warehouse_turns--; }
      			setState(stackEmptyState);
       			playerSource = -1;
       		}
       		else
       		{
    		setState(ContainerState.LOAD_WAREHOUSE_GOODS_STATE);
       		}
    		}}
    		break;
    	case AtSeaLocation:
    	case AtDockLocation:
    	case AtIslandParkingLocation:
			{
			if(!ROBOT_BOARD) { playerBoard[whoseTurn].ship_turns--; }
			setState(stackEmptyState);
			}
			break;
    	case ContainerLocation:
    		{
    		playerBoard bd = playerBoard[whoseTurn];
    		ContainerChip machine = pickedObject.getMachine();
    		bd.unsetMachineHasProduced(machine);
    		boolean some = bd.someMachineHasProduced();
    		factoryGoodsProduced--;
    		if(!some && !pickedObject.isGoldContainer()) 
    			{ sales_this_move--;
    			  G.Assert(factoryGoodsProduced>=0, "zero limit");
    			  playerBoard prevbd = playerBoard[prevPlayer(whoseTurn)];
    			  transfer(COST_TO_PRODUCE,prevbd,bd,false); 
    			  if(!ROBOT_BOARD) { bd.machine_cash_out -= COST_TO_PRODUCE; }
    			}
    		if(!ROBOT_BOARD && !some )
    			{ bd.machine_turns--; }
    		setState(some
    				?ContainerState.LOAD_FACTORY_GOODS_STATE
    				: stackEmptyState);
    		}
    		break;
    	case WarehouseGoodsLocation:
    		{
    		int price = WAREHOUSE_GOOD_PRICES[source.row];
    		playerBoard sourceBd = getPlayerBoard(source);
    		playerBoard myBd = playerBoard[whoseTurn];
    		if(dr.rackLocation()==ContainerId.ContainerLocation)
    		{	if(droppedDestStack.size()==0) 
    				{ // undo produce luxury from warehouse
    				  produceLuxuryFromRack = ContainerId.FactoryGoodsLocation;
    				  produceLuxuryFirstColor = null;
    				  setState(stackEmptyState);
    				}
    		}
    		else if(sourceBd==myBd)
    		{
    			// reprice operation
        		if(droppedDestStack.size()==0) 
    			{ 
    			  setState(stackEmptyState);
    			  if(!ROBOT_BOARD && (stackEmptyState!=ContainerState.REPRICE_WAREHOUSE_STATE)) { sourceBd.warehouse_turns--; } 
    			}
    		}
    		else 
    		{
    		sales_this_move--;
    		ship_loads_this_move--;
    		transfer(price,sourceBd,myBd,false);			// undo the transfer
    		sourceBd.warehouse_cash_in -= price;
    		myBd.ship_cash_out -= price;
    		myBd.current_ship_cost -= price;
    		myBd.current_ship_island_value = -100;
    		switch(pickedSourceStack.size())
    		{
    		case 0:	// empty stack, back to move or second move
    			if(!ROBOT_BOARD) { myBd.ship_turns--; }
    			setState(stackEmptyState);
    			break;
    		case 1:	// back to exactly one
    			switch(source.rackLocation())
    			{	case AtSeaLocation:
    						setState(ContainerState.LOAD_SHIP_1_STATE);
    						break;
    				case WarehouseGoodsLocation:
    						if(!ROBOT_BOARD && (stackEmptyState!=ContainerState.LOAD_SHIP_1_STATE)) { myBd.ship_turns--; }
    						setState(stackEmptyState);
    						break;
    				default: throw G.Error("initial location not expected");
    			}
			  break;

    		default: 
    			setState(ContainerState.LOAD_SHIP_STATE);
    			break;
    		}}
    		}

    		break;
    	default: throw G.Error("Not expecting undrop from source %s",source);
    	}
        if(pickedObject.isShip())
        {	playerBoard board = getShipOwner(pickedObject);
       		board.shipLocation = source;
       		setState(stackEmptyState) ;
        }}
        return(true);	// we did something
    	}
    	return(false);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private boolean unPickObject()
    {	ContainerChip po = pickedObject;
    	if(po!=null)
    	{
    	ContainerCell ps = pickedSourceStack.pop();
    	int pickedI = pickedIndex.pop();
    	lastDroppedObject = pickedObject;
    	pickedObject = null;
    	ps.insertChipAtIndex(pickedI,po);
    	return(true);
    	}
    	return(false);
     }
    public playerBoard getPlayer(int n)
    {
    	if(n<playerBoard.length) { return(playerBoard[n]); }
    	return(null);
    }
    // get the player board associated with C, or null
    private playerBoard getPlayerBoard(ContainerCell c)
    {	return((c==null)
    			? null 
    			: (c.col=='@')
    				? null
    				: getPlayerBoard(c.col)); 
    }
    private playerBoard getPlayerBoard(char col)
    {
    	switch(col)
    	{
    	 case 'A': case 'B': case 'C': case 'D': case 'E':
    	 	{	int pl = col-'A';
    	 		return(playerBoard[pl]);
    	 	}
    	default: throw G.Error("Not expected");
    	}
    }
    public ContainerCell getCell(ContainerCell c)
    {	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
    // 
    // drop the floating object.
    //
    private ContainerCell getCell(ContainerId type,char col,int row)
    {
    	switch(type)
    	{
    	default: throw G.Error("Not expecting dest %s",type);
    	case MachineLocation:
    		// a machine in storage or on a player's mat
    		if(col=='@') { return(machineStorage[row]); }
    		else { return(getPlayerBoard(col).machines[row]); }
    	case LoanLocation:
    		if(col=='@') { return(loanCards); }
    		else { return(getPlayerBoard(col).loans[row]); }
    	case WarehouseLocation:	// cells containing unsold warehouses
    		if(col=='@') { return(warehouseStorage); }
    		return(getPlayerBoard(col).warehouses[row]);
    	case AtSeaLocation:
    		return(atSea[row]);
    	
    	case AtDockLocation:
			return(getPlayerBoard(col).docks[row]);
    		
    	case ShipGoodsLocation:
    		// goods on a ship
			return(getPlayerBoard(col).shipGoods);
    		
    	case IslandGoodsLocation:
    		// island goods owned by a player
			return(getPlayerBoard(col).islandGoods[row]);
    		
    	case FactoryGoodsLocation:
    		// factory goods
			return(getPlayerBoard(col).factoryGoods[row]);
    	case WarehouseGoodsLocation:
    		// goods in a warehouse
    		return(getPlayerBoard(col).warehouseGoods[row]);
    	case ContainerLocation:	// cells containing unsold containers
    		return(containerStorage[row]);
    	case AuctionLocation:			// active auction slot
    		return(auctionBlock);
    	case AtIslandParkingLocation:		// parking lot at the island
    		return(islandParking[row]);
    	}
    }

    private void dropObject(ContainerCell dr)
    {
       G.Assert(pickedObject!=null,"ready to drop");
       droppedDestStack.push(dr);
       lastDroppedObject = pickedObject;
       dr.addChip(pickedObject);
       if(pickedObject.isShip())
       {	playerBoard board = getShipOwner(pickedObject);
       		board.shipLocation = dr;
       }
       pickedObject = null;
    }
    // drop a container into a particular slot in a cell. This is used
    // to undo picks from a particular position.
    private void dropObject(ContainerCell dr,int ind)
    {
       G.Assert(pickedObject!=null,"ready to drop");
       droppedDestStack.push(dr);
       lastDroppedObject= pickedObject;
       dr.insertChipAtIndex(ind,pickedObject);
       if(pickedObject.isShip())
       {	playerBoard board = getShipOwner(pickedObject);
       		board.shipLocation = dr;
       }
       pickedObject = null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ContainerCell cell)
    {	return(getDest()==cell);
    }
    public ContainerCell getDest()
    {    	return(droppedDestStack.top());	
    }
    public ContainerCell getSource()
    {	return(pickedSourceStack.top());
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	ContainerChip c = pickedObject;
    	if(c!=null)
    		{ return(c.chipNumber());
    		}
        return (NothingMoving);
    }
   
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.

    private ContainerChip pickObject(ContainerCell sr)
    {	G.Assert(pickedObject==null,"ready to pick");
 		if(pickedSourceStack.size()==0) { stackEmptyState = board_state; }
 		pickedSourceStack.push(sr);
    	pickedIndex.push(sr.chipIndex);
    	pickedObject = sr.removeTop();
    	return(pickedObject);
    }
    // remove a container of a specified color
    private int pickObjectLike(ContainerCell sr,ContainerChip ob)
    {	G.Assert(pickedObject==null,"ready to pick");
     	pickedSourceStack.push(sr);
     	int ind = sr.chipIndexFor(ob);
     	pickedIndex.push(ind);
     	pickedObject = sr.removeChipAtIndex(ind);
    	return(ind);
   }
    // true if some warehouse owned by the same player has goods for sale
    boolean dockHasGoods(ContainerCell c)
    {
    	playerBoard board = getPlayerBoard(c);
    	return(board.hasWarehouseGoods(99));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private int prevPlayer(int pl)
    {	int p = pl-1;
    	if(p<0) { p = players_in_game-1; }
    	return(p);
     }
    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case REPRICE_FACTORY_STATE:
        case REPRICE_WAREHOUSE_STATE:
        	break;
        case LOAD_LUXURY_STATE:
        	setState(ContainerState.CONFIRM_STATE);
        	break;
        case TRADE_CONTAINER_STATE:
        	setState(ContainerState.LOAD_LUXURY_STATE);
        	break;
        case PLAY1_STATE:
        case LOAD_SHIP_STATE:
        case LOAD_SHIP_1_STATE:
        case LOAD_FACTORY_GOODS_STATE:
        case LOAD_WAREHOUSE_GOODS_STATE:
        case ACCEPT_BID_STATE:
        case AUCTION_STATE:
        case REBID_STATE:
        case TAKE_LOAN_STATE:
        case PLAY2_STATE:
        	// if this is a simple move, enter confirm state.  Otherwise enter one of the
        	// specialized states for more complex cases
        	if(droppedDestStack.size()==0) { setState(ContainerState.CONFIRM_STATE); }
        	else
        	{
        	ContainerCell src = pickedSourceStack.top();
        	ContainerCell dest = droppedDestStack.top();
        	if(dest.rackLocation()==ContainerId.ContainerLocation)
        	{	// moving containers back to the storage area
        		setState(ContainerState.TRADE_CONTAINER_STATE);
        		produceLuxuryFirstColor = dest.topChip();
        		produceLuxuryFromRack = src.rackLocation();
        	}
        	else
        	{
        	switch(src.rackLocation())
        	{
        	default:	throw G.Error("Not expecting source %s",src.rackLocation());
        	case ShipGoodsLocation:
        		break;
        	case LoanLocation:
         		// take out a loan
        		if(dest==loanCards)
        		{	// repaying a loan
        			stackCashInfo(undoStack);
        			playerBoard[whoseTurn].repayLoan(src);
        			setState(ContainerState.CONFIRM_STATE);
        		}
        		else if(src==loanCards)
        		{
        		undoStateStack.push(board_state);		// undo to this state
        		setState(ContainerState.CONFIRM_STATE);
        		}
        		else {throw G.Error("unexpected loan state"); }
        		break;
        	case ContainerLocation:
        		// starting production or conversion of containers from standard to gold
        		{
        		playerBoard bd = playerBoard[whoseTurn];
        		ContainerChip mach = dest.topChip().getMachine();
        		if(mach!=null) 
        			{ 	if(!bd.someMachineHasProduced()) 
        					{ playerBoard prev = playerBoard[prevPlayer(whoseTurn)];
        					  sales_this_move++;
        					  if(!ROBOT_BOARD)
        						  {bd.machine_turns++;
        						   bd.machine_cash_out += COST_TO_PRODUCE;
        						  }
        					  transfer(COST_TO_PRODUCE,bd,prev,false); 
        					}
       					factoryGoodsProduced++;
        				bd.setMachineHasProduced(mach);
        				
        				setState(bd.someMachineCanProduce() ? ContainerState.LOAD_FACTORY_GOODS_STATE : ContainerState.CONFIRM_STATE);
        			}
        		else 
        			{	// no machine, must be a luxury container 
        			setState(ContainerState.LOAD_LUXURY_STATE);
        			}
        		}
        		
        		break;
        	case MachineLocation:
         		// make him pay for a new machine
        		{	int price = MACHINE_PRICES[dest.row];
           			playerBoard srcBd = getPlayerBoard(dest);
           			sales_this_move++;
           			hasBoughtMachine = true;
           			if(!ROBOT_BOARD)
           				{srcBd.machine_turns++;
           				 srcBd.machine_cash_out += price;
           				}
        			transfer_to_bank(price,srcBd,false);
        			setState(ContainerState.CONFIRM_STATE);
        		}
        		break; 
        		
        	case WarehouseLocation:
         		// make him pay for a new warehouse
        		{	int price = WAREHOUSE_PRICES[dest.row];
           			playerBoard srcBd = getPlayerBoard(dest);
           			sales_this_move++;
           			hasBoughtWarehouse = true;
           			if(!ROBOT_BOARD)
           				{
           				srcBd.warehouse_turns++;
           				srcBd.warehouse_cash_out += price;
           				}
        			transfer_to_bank(price,srcBd,false);
        			setState(ContainerState.CONFIRM_STATE);
        		}
        		break; 
        	case FactoryGoodsLocation:
        		// buying from some user's factory storage
        		{
        		playerBoard srcBd = getPlayerBoard(src);
        		playerBoard destBd = getPlayerBoard(dest);
        		
        		if(srcBd==destBd)
        		{	// repricing our own goods
        			if(board_state==ContainerState.PLAY1_STATE) { hasRepricedFactory = true; }
        			if(board_state!=ContainerState.REPRICE_FACTORY_STATE)
        				{
        				if(!ROBOT_BOARD) { destBd.machine_turns++; }
        				setState(ContainerState.REPRICE_FACTORY_STATE);
        				shuffleCount = srcBd.factoryStorageUsed();
        				}
        		}
        		else
        		{
        		// make him pay
        		{	int price = FACTORY_GOOD_PRICES[src.row];
        			sales_this_move++;
        			if(!ROBOT_BOARD && (board_state!=ContainerState.LOAD_WAREHOUSE_GOODS_STATE)) { destBd.warehouse_turns++; }
        			destBd.warehouse_cash_out += price;
        			srcBd.machine_cash_in += price;
        			transfer(price,destBd,srcBd,false);
        			warehouseGoodsBought++;
        		}
        		hasBoughtWarehouseGoods = (warehouseGoodsBought>0);

        		if(destBd.warehouseCanStoreMore() 
        				&& srcBd.hasFactoryGoods(destBd.cash,!destBd.warehouseContainsGold()))
        		{	// we can hold more goods, and he has more to sell
        			setState(ContainerState.LOAD_WAREHOUSE_GOODS_STATE);
        			playerSource = srcBd.player;		// and we can only buy here.
        		}
        		else 
        		{	setState(ContainerState.CONFIRM_STATE);	// end of the line
        		}}
        		}
        		break;
  
      		
        	case WarehouseGoodsLocation:
        		
        		{
            		playerBoard srcBd = getPlayerBoard(src);
            		playerBoard destBd = getPlayerBoard(dest);
            		if(srcBd==destBd)
            		{
           			hasRepricedWarehouse = true;
            		// shuffling warehouse goods
               		if(!ROBOT_BOARD && (board_state!=ContainerState.REPRICE_WAREHOUSE_STATE))
      					{ 
       					  destBd.warehouse_turns++; 
       					}
               		setState(ContainerState.REPRICE_WAREHOUSE_STATE);
               		shuffleCount = srcBd.warehouseStorageUsed();
             		}
            		else
            		{
           			// buying from a warehouse, load onto a ship
            		// make him pay
            		int price = WAREHOUSE_GOOD_PRICES[src.row];
            		sales_this_move++;
            		ship_loads_this_move++;
            		srcBd.warehouse_cash_in += price;
            		destBd.ship_cash_out += price;
            		destBd.current_ship_cost += price;
             		destBd.current_ship_island_value = -100;
            		transfer(price,destBd,srcBd,false);
            		if(!ROBOT_BOARD && (board_state!=ContainerState.LOAD_SHIP_1_STATE)&&(board_state!=ContainerState.LOAD_SHIP_STATE)) { destBd.ship_turns++; }
            		if(srcBd.hasWarehouseGoods(destBd.cash) && destBd.canLoadShip())
            		{	setState(ContainerState.LOAD_SHIP_STATE);
            		}
            		else { setState(ContainerState.CONFIRM_STATE); }
            		}
        		}
        		break;
        	case AtIslandParkingLocation:
        		switch(dest.rackLocation())
        		{
        		default: throw G.Error("Can only go to sea from the island");
        		case AtSeaLocation:
        			if(!ROBOT_BOARD) { playerBoard[whoseTurn].ship_turns++; }
        			setState(ContainerState.CONFIRM_STATE);
        			break;
        		}
        		break;
        	case AtSeaLocation:	
        		// starting at sea
        		if(!ROBOT_BOARD) { playerBoard[whoseTurn].ship_turns++; }
        		switch(dest.rackLocation())
        		{
        		default: throw G.Error("Not expecting dest");
        		case AtDockLocation:
        			// if the dock has goods, load em up.  Otherwise just confirm
        			setState(dockHasGoods(dest) ? ContainerState.LOAD_SHIP_1_STATE : ContainerState.CONFIRM_STATE);
        			break;
        		case AuctionLocation:
        			setState(ContainerState.CONFIRM_STATE);
        		}
        		break;
        	case AtDockLocation:
        		if(!ROBOT_BOARD) { playerBoard[whoseTurn].ship_turns++; }
        		G.Assert(dest.rackLocation==ContainerId.AtSeaLocation,"moving to sea");
        		setState(ContainerState.CONFIRM_STATE);
        		break;
        	}
        	}
        	}
        break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    // undo cash transfers and sticky state changes for the robot
    private void unSetStateAfterDrop(ContainerState prev_state,ContainerCell src,ContainerCell dest,ContainerChip picked)
    {
        switch (prev_state)
        {
        case PLAY1_STATE:
        case PLAY2_STATE:
        case LOAD_FACTORY_GOODS_STATE:
        case LOAD_WAREHOUSE_GOODS_STATE:
        case LOAD_SHIP_STATE:
        case LOAD_SHIP_1_STATE:
        case TAKE_LOAN_STATE:
        case AUCTION_STATE:
        case ACCEPT_BID_STATE:
        case REBID_STATE:
        	// if this is a simple move, enter confirm state.  Otherwise enter one of the
        	// specialized states for more complex cases

        	if(dest.rackLocation()==ContainerId.ContainerLocation)
        	{	// moving containers back to the storage area (was starting gold production)
        		
        	}
        	else
        	{
        	switch(src.rackLocation())
        	{
        	case ContainerLocation:
        		// starting ordinary container production
        		{
        		playerBoard bd = playerBoard[whoseTurn];
        		ContainerChip mach = picked.getMachine();
        		if(mach!=null)	// if there are two macines of the same color, be sure to unset the one we set 
        			{ //bd.unsetMachineHasProduced(mach);
        			  factoryGoodsProduced--;
        			  G.Assert(factoryGoodsProduced>=0, "zero limit");
        			  if(!bd.someMachineHasProduced()) 
        					{ playerBoard prev = playerBoard[prevPlayer(whoseTurn)];
        					  sales_this_move--;
        					  // one sided machine_cash change - the cash in is a windfall, not an action
        					  bd.machine_cash_out -= COST_TO_PRODUCE;
        					  transfer(COST_TO_PRODUCE,prev,bd,false); 
        					}
         			}
        		else
        		{	//has not produced luxury goods
        			produceLuxuryFirstColor = null;
        			produceLuxuryFromRack = ContainerId.FactoryGoodsLocation;
        			hasProducedLuxury = false;
        		}
        		}
        		break;
        	case MachineLocation:
         		// undo pay for a new machine
        		{	int price = MACHINE_PRICES[dest.row];
           			playerBoard srcBd = getPlayerBoard(dest);
           			sales_this_move--;
           			hasBoughtMachine = false;
           			srcBd.machine_cash_out -= price;
        			transfer_from_bank(price,srcBd);
        		}
        		break; 
        		
        	case WarehouseLocation:
         		// undo pay for a new warehouse
        		{	int price = WAREHOUSE_PRICES[dest.row];
           			playerBoard srcBd = getPlayerBoard(dest);
       			sales_this_move--;
       			hasBoughtWarehouse = false;
       			srcBd.warehouse_cash_out -= price;
       			transfer_from_bank(price,srcBd);
        		}
        		break; 
        	case FactoryGoodsLocation:
        		// buying from some user's factory storage
        		{
        		playerBoard srcBd = getPlayerBoard(src);
        		playerBoard destBd = getPlayerBoard(dest);
        		if(srcBd==destBd)
        		{	// repricing our own goods
        		if(prev_state==ContainerState.PLAY1_STATE) { hasRepricedFactory = false; }
           		}
        		else
        		{
        		//give the money back
        		
        		{	int price = FACTORY_GOOD_PRICES[src.row];
        			sales_this_move--;
        			warehouseGoodsBought--;
        	   		G.Assert(warehouseGoodsBought>=0, "zero limit");
        	   
        			srcBd.machine_cash_in -= price;
        			destBd.warehouse_cash_out -= price;
        			transfer(price,srcBd,destBd,false);
        		}
        		hasBoughtWarehouseGoods = (warehouseGoodsBought!=0);
        		}}
        		break;
  
        	case LoanLocation:
        		{
         		if(dest==loanCards)
        		{	// repaying a loan
        			unstackCashInfo(undoStack,false);
        		}
        		else {
               		/*int old = */undoStateStack.pop();
               		//this check failed when in forced loan state.  I think it's ok.
               		//game reference botlock.sgf 11/8/2010 game with leonie
               		//G.Assert(old==prev_state,"expected undo state");
        			}
    			  break;
        		}
        	case WarehouseGoodsLocation:
        		// buying from a warehouse, load onto a ship
        		{
            		playerBoard srcBd = getPlayerBoard(src);
            		playerBoard destBd = getPlayerBoard(dest);
            		if(srcBd==destBd)
            		{	// repricing our own goods
                		if(prev_state==ContainerState.PLAY1_STATE) { hasRepricedWarehouse = false; }

            		}
            		else
            		{
            		// make him give the money back
            		int price = WAREHOUSE_GOOD_PRICES[src.row];
            			sales_this_move--;
            			ship_loads_this_move--;
            			srcBd.warehouse_cash_in -= price;
            			destBd.ship_cash_out -= price;
            			destBd.current_ship_cost -= price;
            			destBd.current_ship_island_value = -100;
            			transfer(price,srcBd,destBd,false);
              		}
        		}
        		break;
        	case AtSeaLocation:	

        		break;
        	case AtDockLocation:
        		break;
			default:
				break;
        	}
        	}
        	break;
        case ACCEPT_LOAN_STATE:
        break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
		default:
			break;
        }
    }
 
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ContainerCell c)
    {    return (c==pickedSourceStack.top());
    }
    void setNextPlayState()
    {
    	if(isSecondAction) { setNextPlayer(); if(gameOverNow()) { setGameOver(); } else { payInterest(); }}
    	else 
    	{ isSecondAction=true;
    	  playerSource = -1;
    	  stackEmptyState = ContainerState.PLAY2_STATE;
    	  setState(ContainerState.PLAY2_STATE);
    	}
    }
    // set the new board state after completing the loan process
    void setLoanPlayState()
    {	ContainerState old = undoStateStack.pop();
    	if(old==ContainerState.TAKE_LOAN_STATE) { old=ContainerState.PLAY1_STATE; mustPayLoan=false;}
    	setState(old);
    	if(pickedSourceStack.size()==0) { stackEmptyState=old; }
    }
    
    private boolean pendingLoanRepayment(ContainerMovespec m)
    {	return((m.container==Done_Repay_Loan)
    			|| (droppedDestStack.top()==loanCards));
    }
    public playerBoard selectedAsFunder()
    {	for(int i=0,lim=playerBoard.length; i<lim; i++) 
    	{ playerBoard bd = playerBoard[i];
    	  if(bd.selectedAsFunder) { return(bd); }
    	}
    	return(null);
    }
    public void selectAsFunder(playerBoard sel)
    {	for(int i=0,lim=playerBoard.length; i<lim; i++) 
    	{ playerBoard bd = playerBoard[i];
    	  bd.selectedAsFunder = (bd==sel);
    	}
	//G.print("Select "+sel);
    }
    // do the bookkeeping to finish accept or reject of a loan
    void doneFinishLoan(playerBoard bd)
    {
	  	   boolean accepted = bd.willFundLoan;
	  	   boolean declined = bd.declinedLoan;
	  	   playerBoard fund = selectedAsFunder();
	  	   int high = getHighBid();
	  	   unstackBidInfo(undoStack);
	  	   if(declined)
	  		{
	  		ContainerCell cancelled = bd.pendingLoanCell();
	  		loanCards.addChip(cancelled.removeTop());
	  		bd.declinedLoan = true;
	  		bd.ephemeral_wontFundLoan = bd.ephemeral_willFundLoan = bd.willFundLoan = false;
	  		setLoanPlayState();
	  		}
	  		else if(accepted)
	  		{
				ContainerCell src = (fund==null) 
										? bd.assignLoanIndex(STANDARD_LOAN_AMOUNT,null)
										: bd.assignLoanIndex(high,fund);
				bd.fundLoan(src);
		  	    setLoanPlayState();
		  	    // don't set if taking loan during an auction
				if(board_state==ContainerState.PLAY1_STATE) { bd.hasTakenLoan = true; }
				bd.setUnpaidStatus(src);
	  		}
	  		else {throw G.Error("Loan was not accepted or declined");}	
    }   
    // do the bookkeeping to finish accept or reject of a loan
    void doneFinishFastLoan(playerBoard bd)
    {	ContainerCell src = bd.assignLoanIndex(STANDARD_LOAN_AMOUNT,null);
    	bd.fundLoan(src);
 	    setLoanPlayState();
  	    // don't set if taking loan during an auction
 	   	if(board_state==ContainerState.PLAY1_STATE) { bd.hasTakenLoan = true; }
    }
    
    private void scoreBidSets()
    {	// compare the actual bids with the predicted bids and score all the goalsets 
    	int actualBids[] = new int[playerBoard.length];
    	for(int i=playerBoard.length-1; i>=0; i--) { actualBids[i] = playerBoard[i].loanBidAmount(); }
    	for(int goalIdx=allGoalSets.length-1; goalIdx>=0; goalIdx--)
    	{
    		//ContainerGoalSet set = allGoalSets[goalIdx];
    		
    	}
    	
    }
    private void scoreGoalSets()
    {	// calculate the score for each goalset for the current set of goods.  
    	// Theoretically, the actual goal set should rise to the top if the 
    	// players are rational and the scoreing algorithm is good.
    	//
    	// first score the goods on each island against each of the goals
    	int valueOfGoods[][] = new int[playerBoard.length][CC.NUMBER_OF_GOAL_CARDS];
    	for(int pla = 0; pla<playerBoard.length; pla++)
    	{	ContainerCell goods[] = playerBoard[pla].islandGoods;
    		for(int goal = 0; goal<CC.NUMBER_OF_GOAL_CARDS; goal++)
    		{	valueOfGoods[pla][goal] = CC.islandGoodsValue(ContainerChip.getCard(goal),goods);
    		}
    	}
    	// now assign those values to working storage in the goalsets
    	for(int setidx=allGoalSets.length-1; setidx>=0; setidx--)
    	{
    		ContainerGoalSet set = allGoalSets[setidx];
    		ContainerChip goals[]  = set.goals;
    		int sum = 0;
    		for(int pla = goals.length-1; pla>=0; pla--)
    		{
    			sum += valueOfGoods[pla][goals[pla].getCardIndex()];	// value of this players goods for this goal
    		}
    		set.sumIslandValues = sum;
    	}
    	
    	// now sort the lists of goals.  the real set of goals ought to rise to the top
    	Sort.sort(allGoalSets);
    	for(int i=allGoalSets.length-1; i>=0; i--) { allGoalSets[i].ordinal = i+1; }
    	for(int i=playerBoard.length-1; i>=0; i--) { Sort.sort(playerBoard[i].possibleGoalSets); }
    }
    private static final int Done_Do_Nothing = 1000;
    private static final int Done_Start_Loan_0 = 1001;
    private static final int Done_Start_Loan_1 = 1002;
    private static final int Done_Start_Auction = 1003;
    private static final int Done_Repay_Loan = 1004;
    private static final int Done_Finish_Loan = 1005;
    private static final int Done_Produce = 1007;
    private static final int Done_Load_Warehouse = 1008;
    private static final int Done_Finish_Auction = 1009;
    private static final int Done_Produce_Luxury = 1010;
    private static final int Done_Reprice = 1011;
    private static final int Done_Move_Ship_0 = 1012;		// moved a ship from a player board
    private static final int Done_Move_Ship_1 = 1013;
    private static final int Done_Move_Ship_2 = 1014;
    private static final int Done_Move_Ship_3 = 1015;
    private static final int Done_Move_Ship_4 = 1016;
    private static final int Done_Load_Ship_0 = 1017;		// loaded a ship from a player board
    private static final int Done_Load_Ship_1 = 1018;
    private static final int Done_Load_Ship_2 = 1019;
    private static final int Done_Load_Ship_3 = 1020;
    private static final int Done_Load_Ship_4 = 1021;

   
    
    private void doDoneCode(int code)
    {	acceptPlacement(); 
		shipLoad = 0;
		shuffleCount = 0;
		passed_this_turn = false;
		if(sales_this_move>0) { last_sale_move = moveNumber; sales_this_move = 0; }
		if(ship_loads_this_move>0) 
			{	if(!ROBOT_BOARD)
				{
					playerBoard bd = playerBoard[whoseTurn];
					bd.current_ship_changed = moveNumber;
				}
				ship_loads_this_move = 0;
			}
    	switch(code)
    	{
    	default: throw G.Error("Donecode %s",code);
    	case Done_Load_Ship_0:
    	case Done_Load_Ship_1:
    	case Done_Load_Ship_2:
    	case Done_Load_Ship_3:
    	case Done_Load_Ship_4:
    		loadedShipFrom = code-Done_Load_Ship_0;
    		setNextPlayState();
    		break;
   		
    	case Done_Move_Ship_0:
    	case Done_Move_Ship_1:
    	case Done_Move_Ship_2:
    	case Done_Move_Ship_3:
    	case Done_Move_Ship_4:
    		movedToSeaFrom = code-Done_Move_Ship_0;
    		setNextPlayState();
    		break;
    	case Done_Finish_Auction:
    		{
    		playerBoard bd = playerBoard[whoseTurn];
    		unstackBidInfo(undoStack);
    		bd.current_ship_cost = 0;
    		bd.current_ship_changed = 0;
    		bd.current_ship_island_value = -100;
    		if(!ROBOT_BOARD) { scoreGoalSets(); }		// score the new outcome against all the goalsets
    		for(playerBoard pb : playerBoard) 
    			{ pb.setLoanBidAmount(0);
    			  pb.setBidAmount(0); 
    			}
    		setNextPlayState();
    		}
    		break;
    	case Done_Finish_Loan:
    		doneFinishLoan(playerBoard[whoseTurn]);
    		break;
    	case Done_Start_Loan_1:
     	case Done_Start_Loan_0:
			{
			if(makeFastLoans || first_shipment)
				{ doneFinishFastLoan(playerBoard[whoseTurn]);
				}
			else
			{
			stackBidInfo(undoStack);							// save everyone's bid status
			clearBidInfo();
			playerBoard bd = playerBoard[whoseTurn];
			bd.requestingLoan = bd.requestingBid = true;

			setState(ContainerState.FUND_LOAN_STATE);
			setNextPlayer();
			}}
			break;
			
    	case Done_Repay_Loan:
    		{
    		unstackCashInfo(undoStack,true);	// discard the state we saved
    		playerBoard bd = playerBoard[whoseTurn];
    		bd.cancelLoan();
    		setState(isSecondAction?ContainerState.PLAY2_STATE:ContainerState.PLAY1_STATE);
    		}
    		break;
    	case  Done_Start_Auction:
    		{
    		playerBoard bd = playerBoard[whoseTurn];
    		stackBidInfo(undoStack);
    		setState(ContainerState.AUCTION_STATE);
    		shipLoad = 0;
			for(playerBoard bb : playerBoard)
			{	
				bb.cannotRebid = bb.requestingBid = (bd==bb);
				bb.bidReceived = false;
				bb.bidReady = false;
				bd.showHandDown = bd.showHandUp = false;
				guiBoards[bd.player].ephemeral_auction_showHandDown = false;
				bb.setBidAmount(0);
				bd.setLoanBidAmount(0);
				}
			setNextPlayer();
    		}
    		break;
    	case Done_Produce_Luxury:
    		// finished swapping for a luxury container, which is a pre-turn
    		// action that doesn't count as an action
    		hasProducedLuxury=true;
    		produceLuxuryFromRack=ContainerId.FactoryGoodsLocation;
    		produceLuxuryFirstColor = null;
    		setState(ContainerState.PLAY1_STATE);
    		break;
    		
    	case Done_Produce:
    		{
			playerBoard bd = playerBoard[whoseTurn];
			hasProduced = bd.someMachineHasProduced();
			setState(ContainerState.REPRICE_FACTORY_STATE);
			shuffleCount = bd.factoryStorageUsed();
			stackEmptyState= ContainerState.REPRICE_FACTORY_STATE;
    		}
    		break;
    	case Done_Load_Warehouse:
            loadedWarehouseFrom = playerSource;
    		playerSource = -1;
    		if(VERSION_1_FILE && (board_state!=ContainerState.CONFIRM_STATE)) 
    			{ // this was a bug from the beginning until 3/22/2011 when version 2 files were introduced.
    			  // the bug was that if you stopped loading a warehouse from factory before you had to stop
    			  // whatever reason, you were cheated out of the repriceing phase.  Leeaving this code here
    			  // forever keeps the old game records readable.
    			String msg = "Version 1 compatability; missing an adjust price phase at move #"+moveNumber;
    			boolean ok = false;
    			for(int i=0;!ok && (i<complaints.size()); i++) { if(complaints.elementAt(i).equals(msg)) { ok=true; };}
    			if(!ok) { complaints.push(msg); }
    			setState(ContainerState.CONFIRM_STATE); setNextPlayState(); 
    			}
    		else 
    		{
    		setState(ContainerState.REPRICE_WAREHOUSE_STATE);
    		stackEmptyState= ContainerState.REPRICE_WAREHOUSE_STATE;
    		shuffleCount = playerBoard[whoseTurn].warehouseStorageUsed();
    		}
    		break;
    	case Done_Do_Nothing:
    	case Done_Reprice:
    		setState(ContainerState.CONFIRM_STATE);
    		setNextPlayState();
    		break;
    	}
    }
    private int getDoneCode()
    {	int doneCode = Done_Do_Nothing;
    
    	if(droppedDestStack.size()==0)
    	{ 
    	  if(playerBoard[whoseTurn].requestingLoan)
		  	{  doneCode = Done_Finish_Loan;
		  	}
    	  else if(playerBoard[whoseTurn].requestingBid)
    	  { doneCode = Done_Finish_Auction;
    	  }
    	}
    	else if((board_state==ContainerState.REPRICE_WAREHOUSE_STATE)||(board_state==ContainerState.REPRICE_FACTORY_STATE)) 
    		{ doneCode = Done_Reprice; }
    	else
    	{
		ContainerCell dest = droppedDestStack.top();
		ContainerCell src = pickedSourceStack.top();

		switch(dest.rackLocation())
		{
		case AtSeaLocation:
			if(src.col>='A') { doneCode = Done_Move_Ship_0+(src.col-'A'); }
			break;
		case ShipGoodsLocation:
			doneCode = Done_Load_Ship_0+(src.col-'A');
			break;
		case LoanLocation:
			{
			if(src==loanCards)		// taking out a loan
				{
				switch(dest.row)
				{
				default: throw G.Error("Not expecting loan dest row %s",dest.row);
				case 0: doneCode = Done_Start_Loan_0; break;
				case 1: doneCode = Done_Start_Loan_1; break;
				}
				}
				else
				{//repaid a loan
				doneCode = Done_Repay_Loan;
				}
			}
			// initiating the most complex transaction
			break;
		case AuctionLocation:	
			{
			doneCode = Done_Start_Auction;
			}
			break;

		case FactoryGoodsLocation:
			{
    		if(src==containerStorage[CONTAINER_GOLD-CONTAINER_OFFSET])
	    		{
	    		// finished swapping for a luxury container, which is a pre-turn
	    		// action that doesn't count as an action
	    		doneCode = Done_Produce_Luxury;
	     		}
	    		else
	    		{
	    		doneCode = Done_Produce;
	    		}
			}
			break;
		case WarehouseGoodsLocation:
			if(src==containerStorage[CONTAINER_GOLD-CONTAINER_OFFSET])
			{
       		// finished swapping for a luxury container, which is a pre-turn
       		// action that doesn't count as an action
			doneCode = Done_Produce_Luxury;
			}
			else
			{
			doneCode = Done_Load_Warehouse;
			}
			break;
		default: 
			break;
		}}
		return(doneCode);
    }
    private int setNextStateAfterDone(int doneCode)
    {	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case PLAY2_STATE:	// shouldn't happen, but no harm
    	case PLAY1_STATE:	// shouldn't happen, but no harm
    	case LOAD_WAREHOUSE_GOODS_STATE:
    	case REPRICE_FACTORY_STATE:
    	case LOAD_FACTORY_GOODS_STATE:
    	case REPRICE_WAREHOUSE_STATE:
    	case LOAD_SHIP_1_STATE:
    	case LOAD_SHIP_STATE:
		case CONFIRM_AUCTION_STATE:
		case CONFIRM_STATE:
    		doDoneCode(doneCode);
    		break;
    	case GAMEOVER_STATE: 
    		break;

    	}
       	return(doneCode);
    }

    // same as undoBid but without some bookkeeping
    long undoTestBid(long code)
    {
   		long ship = code;
   		int amount = (int)(ship&0x3ff);
   		if((amount&0x200)!=0){ amount |= ~0x3ff; }
   		ship = ship>>10;
   		int toplayer = (int)(ship&0x7);
   		playerBoard tobd =playerBoard[toplayer];
		long load = ship>>3;
		int fromplayer = (int)(load&0x7);
		playerBoard fromBd = playerBoard[fromplayer];
		fromBd.transferIslandToShip(tobd.shipGoods,load);

		if(amount!=0)
		{
		if(fromBd==tobd)
			{
			// undo a self-auction
			// the cost of these goods is the auction price plus the amount we paid for them.
			transfer_from_bank(amount,tobd);
			}
			else
			{	//undo an auction. Under rare circumstances, this can result in a return
				//to an overdraft state, so allow overdrafts
				transfer_to_bank(amount,tobd,true);
				transfer(amount,tobd,fromBd,true);
			}}
		return(toplayer);
    }
    // undo a bid with global bookkeeping
    int undoBid(long code)
    {
   		long ship = code;
   		int amount = (int)( ship&0x3ff);
   		ship = ship>>10;
   		int toplayer = (int)(ship&0x7);
   		playerBoard tobd =playerBoard[toplayer];
		long load = ship>>3;
		int fromplayer = (int)(load&0x7);
		playerBoard fromBd = playerBoard[fromplayer];
		fromBd.transferIslandToShip(tobd.shipGoods,load);
		tobd.requestingBid = true;
		sales_this_move--;
		fromBd.selectedAsFunder=false;
		if(fromBd==tobd)
			{
			// undo a self-auction
			// the cost of these goods is the auction price plus the amount we paid for them.
			tobd.island_cash_out -= amount;
			tobd.island_cash_out -= tobd.current_ship_cost;
			tobd.ship_cash_out += tobd.current_ship_cost;
			transfer_from_bank(amount,tobd);
			}
			else
			{	//undo an auction
				tobd.ship_cash_in -= amount*2;
				fromBd.island_cash_out -= amount;
				transfer_to_bank(amount,tobd,false);
				transfer(amount,tobd,fromBd,false);
			}
		return(toplayer);
    }
    void undoParking(int toplayer)
    {
   		playerBoard tobd =playerBoard[toplayer];
		// indicating a "done" after a ship auction
    	ContainerChip shipowner = islandParking[toplayer].removeTop();
    	auctionBlock.addChip(shipowner);
    	tobd.shipLocation = auctionBlock;

    }
    //undo the ship movement and container movement associated with an auction.
    public boolean undoAuction(long code)
    {
    	if(code!=0)
    	{	
    	isSecondAction = (code&1)!=0;
    	code = code>>1;
    	int toplayer = undoBid(code);
		undoParking(toplayer);
		return(true);
    	}
   		return(false);
   	}

    void moveShipToParking(replayMode replay)
    {
    	// finishing a ship auction
		ContainerChip ship = auctionBlock.removeTop();
		playerBoard owner = getShipOwner(ship);
		ContainerCell dest = islandParking[owner.player];
		owner.shipLocation = dest;
		dest.addChip(ship);
		if(replay.animate)
		{
			animationStack.push(auctionBlock);
			animationStack.push(dest);
		}
		isSecondAction = true;
    }
    boolean canOnlyUndrop()
    {
    	if(VERSION_1_FILE)
    	{
    	switch(board_state)
    		{
    		case LOAD_SHIP_STATE:
    			return(!VERSION_1_DATE);
    		case REPRICE_FACTORY_STATE:
    		case REPRICE_WAREHOUSE_STATE:	
    			return(!VERSION_1_DATE);
    		default: return(true);
    		}
    	}
    	return(false);
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	ContainerMovespec m = (ContainerMovespec)mm;

    	//G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_BUY:
       	{
          	undoAuction(shipLoad);		// undo any previous auction resolution
        	shipLoad = 0;
        	//undoAuction();		// undo any previous auction resolution
        	// buy the auction goods yourself
        	playerBoard bd = playerBoard[whoseTurn];
        	G.Assert(bd.requestingBid,"is the auctionerr");
        	long containerCode = bd.transferShipToIsland(bd.shipGoods,replay);
        	int highBid = (int)(0xffff&m.to_row);
        	bd.selectedAsFunder = true;
        	containerCode = (containerCode<<3)|bd.player;
        	containerCode = (containerCode<<10)|highBid;
        	containerCode = (containerCode<<1)|(isSecondAction?1:0);
        	m.from_row = shipLoad = containerCode;	// move the goods, and generate an undo code
       		sales_this_move++;
       		// buying own goods, change the island account with the auction price
       		// plus the cost of the goods on the ship. 
       		bd.setLoanBidAmount(highBid);	// this is remembering the liability for possible undo
       		bd.island_cash_out += highBid;
       		bd.island_cash_out += bd.current_ship_cost;
       		bd.current_ship_island_value = -100;
       		bd.ship_cash_out -= bd.current_ship_cost;	// and the ship account is no longer charged
       		transfer_to_bank(highBid,bd,false);								// pay for the goods
        	moveShipToParking(replay);
       		setState(ContainerState.CONFIRM_AUCTION_STATE);
        	}
        	break;
        case MOVE_ACCEPT:	// accept an auction bid for goods on a ship
        	{
          	undoAuction(shipLoad);		// undo any previous auction resolution
        	shipLoad = 0;
       		playerBoard bd = playerBoard[whoseTurn];
       		playerBoard frombd = playerBoard[(int)(0xf&m.to_row)];
       		G.Assert(bd.requestingBid,"is the auctionerr");
       		frombd.selectedAsFunder = true;
       		long containerCode = frombd.transferShipToIsland(bd.shipGoods,replay);
       		int highBid = frombd.bidAmount();
       		bd.setLoanBidAmount(0);
        	containerCode = (containerCode<<3)|bd.player;
        	containerCode = (containerCode<<10)|highBid;
        	containerCode = (containerCode<<1)|(isSecondAction?1:0);
       		m.from_row = shipLoad = containerCode;	// move the goods, and generate an undo code
       		sales_this_move++;
       		frombd.island_cash_out += highBid;
       		bd.ship_cash_in += highBid*2;
       		transfer(highBid,frombd,bd,false);						// pay for the goods
       		transfer_from_bank(highBid,bd);							// and matching money from the bank
       		moveShipToParking(replay);
        	setState(ContainerState.CONFIRM_AUCTION_STATE);
        	}
        	break;
        case MOVE_DECLINE_LOAN:
        	{ playerBoard bd = playerBoard[whoseTurn];
        		bd.declinedLoan = true;
        		bd.willFundLoan = false;
        		//bd.ephemeral_wontFundLoan = bd.ephemeral_willFundLoan = bd.willFundLoan = false;
        		selectAsFunder(null);
         		setState(ContainerState.CONFIRM_STATE);
        	}
        	break;
        case MOVE_ACCEPT_LOAN:
	       	{ playerBoard bd = playerBoard[whoseTurn];
			bd.declinedLoan = false;
			bd.willFundLoan = true;
			//bd.ephemeral_wontFundLoan = bd.ephemeral_willFundLoan = bd.willFundLoan = true;
			selectAsFunder(m.to_row>=0?playerBoard[(int)(0xf&m.to_row)]:null);
			setState(ContainerState.CONFIRM_STATE);
	       	}
	        break;
        case MOVE_DONE:
        	if(m.container==0) { m.container = getDoneCode(); }
        	setNextStateAfterDone(m.container);	// remember what this "done" did for the robot
        	selectAsFunder(null);
            break;
        case MOVE_EPHEMERAL_DECLINE:
	    	{	playerBoard bd = playerBoard[m.player];
	    		bd.ephemeral_willFundLoan = false;
	    		bd.ephemeral_wontFundLoan = true;
	    		break;
	    	}
       case MOVE_EPHEMERAL_FUND:
        	{	playerBoard bd = playerBoard[m.player];
        		bd.ephemeral_willFundLoan = true;
        		bd.ephemeral_wontFundLoan = false;
        		break;
        	}
       case MOVE_EPHEMERAL_LOAN_BID:
		   	{
		       	playerBoard bd = playerBoard[m.player];
            	GuiBoard gui = guiBoards[bd.player];
            	// this is necessary so remote viewers can trigger the bid properly
            	gui.ephemeralLoanMove = null;
            	int am = (int)(0xffff&m.to_row);
            	gui.pendingLoanBid = am;
            	gui.pendingLoanMove = "bid "+am;
		       	gui.ephemeral_loan_showHandDown = true;
		   	}
		   	break;
       case MOVE_EPHEMERAL_AUCTION_BID:
        	{
            	playerBoard bd = playerBoard[m.player];
            	GuiBoard gui = guiBoards[bd.player];
            	// this is necessary so remote viewers can trigger the bid properly
            	gui.ephemeralAuctionMove = null;
            	int am = (int)(0xffff&m.to_row);
            	gui.pendingAuctionBid = am;
            	gui.pendingAuctionMove = "bid "+am;
            	gui.ephemeral_auction_showHandDown = true;
        	}
        	break;
        case MOVE_BID:
        	{
        	playerBoard bd = playerBoard[whoseTurn];
        	if(board_state==ContainerState.FINANCEER_STATE) 
        		{ guiBoards[bd.player].ephemeral_loan_showHandDown = false; }
        		else
        			{
        			guiBoards[bd.player].ephemeral_auction_showHandDown = false; 
        			}
        	bd.bidReceived = true;
        	bd.showHandDown = true;
        	bd.showHandUp = false;
        	
        	bd.setBidAmount((int)(0xffff&m.to_row));
        	if(board_state!=ContainerState.FINANCEER_STATE) { bd.setLoanBidAmount(bd.bidAmount()); }

      		setNextPlayer();
      		bd = playerBoard[whoseTurn];
      		if(bd.requestingBid)			// got back to the player requesting the bid
      		{	// auction over
      			int ba = -1;
      			int bidders = 0;
      			for(int i=0,lim=playerBoard.length; i<lim;i++)
      			{	playerBoard bm = playerBoard[i];
      				if(i!=whoseTurn && !bm.cannotRebid)
      				{	
      					G.Assert(bm.bidReceived,"has bid");
      					bm.bidReceived = false;
      					bm.bidReady = false;
      					bm.showHandUp = true;
      					bm.showHandMsg = "$ "+bm.bidAmount();
      					if(bm.bidAmount() > ba)
      					{	bidders = 1;
      						ba = bm.bidAmount();
      					}
      					else if(bm.bidAmount()==ba) { bidders++; }
      				}
      			}
      			if(board_state==ContainerState.FINANCEER_STATE)
      			{ setState(ContainerState.ACCEPT_LOAN_STATE);
      			}
      			else
      			{
      			switch(bidders)
      			{
     			default:
     					if(board_state!=ContainerState.REBID_STATE) 
     						{ 
     						// we're in a first bid and there are multiple bids the same.
     						// mark the lower bidders as inelgible and repeat the auction.
     						setState(ContainerState.REBID_STATE); 
     						
     						for(int i=0,lim=playerBoard.length;i<lim;i++)
     						{	playerBoard bb = playerBoard[i];
     							bb.cannotRebid |= (playerBoard[i].bidAmount()!=ba);
     						    bb.bidReceived = false;
     						    bb.bidReady = false;
     						}
     						setNextPlayer();
     						break; 
     						}
     					// otherwise accept
     					//$FALL-THROUGH$
				case 1: setState(ContainerState.ACCEPT_BID_STATE);
      					if(!ROBOT_BOARD) { scoreBidSets(); }		// score the bids against predictions
      					break;
       			}}
      			
      			
      		}
        	}
        	break;
        case MOVE_FUND:
        	{	playerBoard bd = playerBoard[whoseTurn];
        		bd.ephemeral_willFundLoan = bd.willFundLoan=true;
        		bd.showHandMsg = "$ 10";
        		bd.showHandUp=true;
        	}
			//$FALL-THROUGH$
		case MOVE_DECLINE:
        	{
        	// fund or decline to fund a loan
        	playerBoard oldBd = playerBoard[whoseTurn];
        	oldBd.bidReceived = true;
        	oldBd.cannotRebid = false;
        	oldBd.setBidAmount(10);
        	oldBd.ephemeral_wontFundLoan = !oldBd.willFundLoan;
    		setNextPlayer();
    		{	playerBoard bd = playerBoard[whoseTurn];
    			if(bd.requestingLoan)
    			{	// back to the original requestor
    				int funders = 0;
    				for(int i=0,lim=playerBoard.length; i<lim; i++)
    				{	if(playerBoard[i].willFundLoan) { funders++; }
    				}
    				switch(funders)
    				{
    				case 0:		// single funding from the bank
    					if(mustPayLoan) { bd.payInterest(true); }
    					setState(ContainerState.ACCEPT_LOAN_STATE);
    					break;
    				case 1:		// single funding from a player
    					if(mustPayLoan) { bd.payInterest(true); }
    					setState(ContainerState.ACCEPT_LOAN_STATE);
    					break;
    				default:	// multiple funders, secondary bid
   	    			for(int i=0,lim=playerBoard.length; i<lim;i++)
    	    			{	playerBoard bb = playerBoard[i];
    	    				bb.requestingBid = (bb==bd);
    	    				bb.cannotRebid = (bb==bd)||!bb.willFundLoan;
    	    				bb.bidReceived = false;
    	    				bb.bidReady = false;
    	    				bb.showHandUp = bb.showHandDown = false;
    	    				bb.bidAmount = 10;
    	     			}
   	    				// make the players bid for the loan and go around again
    					setState(ContainerState.FINANCEER_STATE);
    					setNextPlayer();
    				}
    			}
    		}}
        	break;
        case MOVEC_FROM_TO:
	        	switch(board_state)
	        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
	        		case REPRICE_FACTORY_STATE:
	        		case REPRICE_WAREHOUSE_STATE:
	        			shuffleCount--;
		        		//$FALL-THROUGH$
					case GAMEOVER_STATE:
	        		case PLAY1_STATE:
	        		case PLAY2_STATE:
	        		case LOAD_WAREHOUSE_GOODS_STATE:
	        		case TRADE_CONTAINER_STATE:
	        		case LOAD_SHIP_1_STATE:
	        		case LOAD_LUXURY_STATE:
	        		case LOAD_SHIP_STATE:
	        			{
	        			acceptPlacement();
	                	ContainerCell src = getCell(m.source,m.from_col,(int)(0xffff&m.from_row));
	                	G.Assert(src.canHoldContainers(),"holds containers");
	                	int ind = pickObjectLike(src,ContainerChip.getContainer(m.container&0xfff));
	                	m.container |= ind<<12;
	                	ContainerCell dest = getCell(m.dest, m.to_col, (int)(0xffff&m.to_row));
	                    dropObject(dest); 
	                    if(replay.animate)
	                    {
	                    	animationStack.push(src);
	                    	animationStack.push(dest);
	                    }
	                    setNextStateAfterDrop(); 
	        			}
	                    break;
	        	}
	        	break;
        	case MOVE_FROM_TO:
           	switch(board_state)
	        	{	default: throw G.Error("Not expecting state %s",board_state);
	        		case PLAY1_STATE:
	        		case PLAY2_STATE:
	        		case AUCTION_STATE:
	        		case ACCEPT_BID_STATE:
	        		case LOAD_FACTORY_GOODS_STATE:
	        		case LOAD_WAREHOUSE_GOODS_STATE:
	        		case TAKE_LOAN_STATE:
	        		case FUND_LOAN_STATE:
	        		case LOAD_SHIP_STATE:
	        		case REBID_STATE:
	        		case LOAD_SHIP_1_STATE:
	        		case LOAD_LUXURY_STATE:
	        			{
	        			acceptPlacement();
	        			ContainerCell src = getCell(m.source, m.from_col,(int)(0xfff& m.from_row));
	        			ContainerCell dest = getCell(m.dest, m.to_col, (int)(0xffff&m.to_row));
	                    pickObject(src);
	                    dropObject(dest); 
	                    if(replay.animate)
	                    {
	                    	animationStack.push(src);
	                    	animationStack.push(dest);
	                    }
	                    setNextStateAfterDrop(); 
	        			}
	                    break;
	        	}
        	break;

         case MOVE_PASS:
        	 playerBoard[whoseTurn].pass_count++;
        	 passed_this_turn = true;
        	 setNextStateAfterDrop();
            break;


        case MOVE_DROP: // drop on chip pool;
            {
            m.container = pickedObject.isContainer()? (pickedObject.getContainerIndex()+CONTAINER_OFFSET) : 0;
            ContainerCell dest = getCell(m.dest,m.to_col,(int)(0xffff&m.to_row));
            if(isSource(dest)) { unPickObject(); }
            else 
            { ContainerCell source = getSource();
              if(replay==replayMode.Single)
              {
            	  animationStack.push(source);
            	  animationStack.push(dest);
              }
              if(source!=null) 
              	{// save these for the gamelog
            	 m.from_col = source.col;
            	 m.from_row = source.row;
            	 m.source = source.rackLocation();
              	}
               dropObject(dest); 
              setNextStateAfterDrop(); 
            }
            }
            break;

        case MOVE_PICKC:
    		{
        	ContainerCell src = getCell(m.source,m.from_col,(int)(0xffff&m.from_row));
        	G.Assert(src.canHoldContainers(),"holds containers");
        	ContainerChip chipType = ContainerChip.getContainer(m.container&0xfff);
        	if(isDest(src) && ((chipType==src.topChip()) || canOnlyUndrop())) { unDropObject(); }
        	else 
        		{   int ind = pickObjectLike(src,chipType);
        			m.container |= ind<<12;
        	}
        	}
        	break;
        case MOVE_PICK:
        	{
        	ContainerCell src = getCell(m.source,m.from_col,(int)(0xffff&m.from_row));
        	G.Assert(!src.canHoldContainers(),"should use pickb");
        	if(isDest(src)) { unDropObject(); }
        	else 
        	{   pickObject(src);
        		m.container = 0;
        	}
        	}
             break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            stackEmptyState = ContainerState.PLAY1_STATE;
            setState(ContainerState.PLAY1_STATE); 
             
            break;

        case MOVE_EDIT:
    		acceptPlacement();
            setState(ContainerState.PUZZLE_STATE);

            break;
            
        case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(ContainerState.GAMEOVER_STATE);
			break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    public Hashtable<ContainerCell,ContainerCell> getSources()
    {	Hashtable<ContainerCell,ContainerCell> hh = null;
    	if(pickedObject==null)
    	{
    	switch(board_state)
    	{
    	case PUZZLE_STATE:  break;	
    	case CONFIRM_STATE:
		case CONFIRM_AUCTION_STATE:

    		{
    		hh = new Hashtable<ContainerCell,ContainerCell>();
    		ContainerCell c = getDest();
    		if(c!=null) { hh.put(c,c); }
    		}
    		break;
    	default:
    		{
    		hh = new Hashtable<ContainerCell,ContainerCell>();
    		CommonMoveStack  all = GetListOfMoves(whoseTurn,null,0,masterGoalSet);
	    	{ ContainerCell prev = getDest();
	    	  if(prev!=null) { hh.put(prev,prev); }
	    	}
	    	for(int i=0,lim=all.size(); i<lim; i++)
	    	{	ContainerMovespec m = (ContainerMovespec)(all.elementAt(i)); 
	    		if((m.op==MOVE_FROM_TO)||(m.op==MOVEC_FROM_TO))
	    			{ ContainerCell c = getCell(m.source,m.from_col,(int)(0xffff&m.from_row));
	    			  hh.put(c,c);
	    			}
	    	}}
    	}}
     	return(hh);
    }
    private void addAll(Hashtable<ContainerCell,ContainerCell> hh,ContainerCell ar[])
    {	for(int i=0,lim=ar.length; i<lim; i++) { hh.put(ar[i],ar[i]); }
    }
    private void addAllEmpty(Hashtable<ContainerCell,ContainerCell> hh,ContainerCell ar[])
    {	for(int i=0,lim=ar.length; i<lim; i++) 
    		{ ContainerCell cc = ar[i];
    		  if(cc.topChip()==null) { hh.put(ar[i],ar[i]); }
    		}
    }
	public Hashtable<ContainerCell,ContainerCell> getDests()
	{	Hashtable<ContainerCell,ContainerCell> hh = new Hashtable<ContainerCell,ContainerCell>();
		if(pickedObject!=null)
		{
			if(board_state==ContainerState.PUZZLE_STATE)
			{
			// in puzzle mode, allow any destination that is consistent with the picked object\
			if(pickedObject.isMachine())
			{	ContainerCell home = machineStorage[pickedObject.getMachineIndex()];
				hh.put(home,home);
				for(int i=0,lim=playerBoard.length; i<lim; i++)
				{	ContainerCell pl = playerBoard[i].nextEmptyMachine();
					if(pl!=null) { hh.put(pl,pl); }
				}
			}
			else if(pickedObject.isWarehouse())
			{	
				hh.put(warehouseStorage,warehouseStorage);
				for(int i=0,lim=playerBoard.length; i<lim; i++)
				{	ContainerCell pl = playerBoard[i].nextEmptyWarehouse();
					if(pl!=null) { hh.put(pl,pl); }
				}
			}
			else if(pickedObject.isContainer())
			{	int idx = pickedObject.getContainerIndex();
				ContainerCell home = containerStorage[idx];
				hh.put(home,home);
				
				for(int i=0,lim=playerBoard.length; i<lim; i++)
				{	playerBoard pl = playerBoard[i];
					addAll(hh,pl.warehouseGoods);
					addAll(hh,pl.factoryGoods);
					ContainerCell ispot = pl.islandGoods[idx];
					hh.put(ispot,ispot);
					hh.put(pl.shipGoods,pl.shipGoods);
				}
			}
			else if(pickedObject.isShip())
			{	addAllEmpty(hh,atSea);
				addAllEmpty(hh,islandParking);
				if(auctionBlock.topChip()==null) { hh.put(auctionBlock,auctionBlock); }
				for(int i=0,lim=playerBoard.length; i<lim; i++)
				{	playerBoard bd = playerBoard[i];
					addAllEmpty(hh,bd.docks);
				}
			}
			else if(pickedObject.isLoan())
			{	
				for(int i=0,lim=playerBoard.length; i<lim; i++)
				{	playerBoard bd = playerBoard[i];
					addAllEmpty(hh,bd.loans);
				}
				hh.put(loanCards,loanCards);
			}
			else { throw G.Error("Not expected");}
			}
			else
			{	// a playing state
				CommonMoveStack  all = GetListOfMoves(pickedSourceStack.top(),
											pickedObject,null,0,masterGoalSet);
		    	hh = new Hashtable<ContainerCell,ContainerCell>();
		    	for(int i=0,lim=all.size(); i<lim; i++)
		    	{	ContainerMovespec m = (ContainerMovespec)(all.elementAt(i)); 
		    		if((m.op==MOVE_FROM_TO)||(m.op==MOVEC_FROM_TO))
		    			{ ContainerCell c = getCell(m.dest,m.to_col,(int)(0xffff&m.to_row));
		    			  hh.put(c,c);
		    			}
		    	}
	
			}
		}
		return(hh);
	}
	
    public boolean LegalToHitBoard(ContainerCell cell)
    {	
        switch (board_state)
        {
        case PUZZLE_STATE:
        	return(true);
        default:
        	throw G.Error("Not expecting state %s", board_state);
		case GAMEOVER_STATE:
			return(false);
		case CONFIRM_STATE:
		case CONFIRM_AUCTION_STATE:
			return(isDest(cell));
 
         }
    }
  public boolean canDropOn(ContainerCell cell)
  {	
  	return((pickedObject!=null)				// something moving
  			&&(pickedSourceStack.top().onBoard 
  					? (cell!=(pickedSourceStack.top()))	// dropping on the board, must be to a different cell 
  					: (cell==(pickedSourceStack.top())))	// dropping in the rack, must be to the same cell
  				);
  }
  private void clearBidInfo()
  { 
		for(int i=0,lim=playerBoard.length; i<lim; i++)
		{	playerBoard bd = playerBoard[i];
			bd.requestingLoan = false;
			bd.showHandUp = bd.showHandDown = false;
			bd.declinedLoan = false;
			bd.hasTakenLoan = false;
			bd.willFundLoan = false;
			bd.bidReceived = false;
			bd.bidReady = false;
			bd.setBidAmount(0);
			// do not clear committed cash.  We get here when starting a sub-auction
			guiBoards[bd.player].ephemeral_auction_showHandDown = false;
			guiBoards[bd.player].ephemeral_loan_showHandDown = false;
			bd.ephemeral_willFundLoan=false;
			bd.ephemeral_wontFundLoan=false;
		}  
  }
  // restore everyone's bid info from the undo stack
  private void unstackBidInfo(IStack fr)
  { 
	  for(int i=playerBoard.length-1; i>=0; i--)
	  	{ int val = fr.pop(); 
	  	   playerBoard[i].setBidInfo(val); 
	  	}  
  }
  // save everyone's bid info on the undo stack
  private void stackBidInfo(IStack on)
  {	
    for(int i=0,lim=playerBoard.length; i<lim; i++) { on.push(playerBoard[i].getBidInfo()); }
  }
  private void copyLoanInfo(IStack from,IStack to)
  {	int size = from.size();
  	for(int i=playerBoard.length; i>0;i--) 
  	{	to.push(from.elementAt(size-i));	
  	}
  }
  private void copyBidInfo(IStack from,IStack to)
  {	int size = from.size();
  	for(int i=playerBoard.length; i>0;i--) 
  	{	to.push(from.elementAt(size-i));	// copy one more than the number of players.  The extra is the restore board_state
  	}
  } 
  // move N items preserving stack order.
  void moveBidInfo(IStack from,IStack to,int n)
  {
    int val = from.pop();
  	if(n>1)
  	{
	 moveBidInfo(from,to,n-1);
  	}
  	to.push(val);
  }
  void moveBidInfo(IStack from,IStack to)
  {	moveBidInfo(from,to,playerBoard.length);
  }
  void moveLoanInfo(IStack from,IStack to)
  {	moveBidInfo(from,to,playerBoard.length);
  }
  private int restoreGlobalUndo(ContainerMovespec m)
  {	switch(m.op)
	  {
	  case MOVE_DONE:
		  
	       	switch(m.container)
        	{
	       	default: throw G.Error("not expecting done code %s",m.container);
            case Done_Finish_Auction:
            	unstackBidInfo(robotStack);
            	moveBidInfo(robotStack,undoStack);
            	break;
            case Done_Produce_Luxury:
            case Done_Produce:
            case Done_Load_Warehouse:
            case Done_Reprice:
        	case Done_Do_Nothing: 
        	case Done_Repay_Loan:
        	case Done_Finish_Loan:
        	case Done_Start_Auction:
         	case Done_Start_Loan_0:
        	case Done_Start_Loan_1:
        	case Done_Move_Ship_0:
        	case Done_Move_Ship_1:
        	case Done_Move_Ship_2:
        	case Done_Move_Ship_3:
        	case Done_Move_Ship_4:
        	case Done_Load_Ship_0:
        	case Done_Load_Ship_1:
        	case Done_Load_Ship_2:
        	case Done_Load_Ship_3:
        	case Done_Load_Ship_4:
        		break;
        	}
		  shipLoad = m.to_row;
		  if(m.from_row>=1) 
		  	{ unstackCashInfo(robotStack,false);
		  	  if(m.from_row==1) 
		  	  	{ copyCashInfo(robotStack,undoStack); unstackCashInfo(robotStack,true); 
		  	  	}
		  	  if(m.from_row==2)
		  	  { 
		  	  	unstackBidInfo(robotStack);
		  	  	moveLoanInfo(robotStack,undoStack); 
		  	  	undoStateStack.push(robotStateStack.pop());
		  	  }
		  	}
		  break;
	  case MOVE_DECLINE:
	  case MOVE_FUND:
	  case MOVE_DECLINE_LOAN:
		  unstackCashInfo(robotStack,false);
		  unstackBidInfo(robotStack);
		  break;
	  case MOVE_ACCEPT:
	  case MOVE_ACCEPT_LOAN:
	  case MOVE_BUY:
	  case MOVE_BID:
		  unstackBidInfo(robotStack);
		  break;
	  default:
		break;
	  }
  
  int undoInfo = m.undoInfo;
  int undoinfo2 = m.undoInfo2>>1;			// ignore the undo info bit

  hasBoughtMachine = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;

  hasBoughtWarehouse = (undoinfo2&1)!=0;
  undoinfo2 = (undoinfo2>>1);
  

  boolean declinedLoan = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  boolean hasTakenLoan = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  hasRepricedWarehouse = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  hasBoughtWarehouseGoods = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  hasRepricedFactory = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  int unpaid = undoinfo2 & ~(-1<<(MAX_LOANS*2));
  undoinfo2 = undoinfo2>>(MAX_LOANS*2);
  
  mustPayLoan = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;

  hasProducedLuxury = (undoinfo2&1)!=0;
  undoinfo2 = undoinfo2>>1;
  
  int undolux = undoinfo2 &0xf;
  undoinfo2 = undoinfo2 >> 4;
        	
  if(undolux!=0)
  	{ switch(undolux&1)
	  {
  		case 0: produceLuxuryFromRack =  ContainerId.FactoryGoodsLocation; break;
  		case 1: produceLuxuryFromRack = ContainerId.WarehouseGoodsLocation; break;
	default:
		break;
	  }
  	 undolux = undolux>>1;
  	 produceLuxuryFirstColor = ContainerChip.getContainer(undolux-1);
  	}
   else {   produceLuxuryFromRack = ContainerId.FactoryGoodsLocation ; produceLuxuryFirstColor = null; }
  
  
  movedToSeaFrom = (undoinfo2&7)-1;
  undoinfo2 = undoinfo2>>3;
  
  loadedWarehouseFrom = (undoinfo2&7)-1;
  undoinfo2 = undoinfo2>>3;
        	
  loadedShipFrom = (undoinfo2&7)-1;
  undoinfo2 = undoinfo2>>3;
				
  playerSource = (undoinfo2&0x7)-1;
  setState(robotState.pop());
  

  undoInfo = undoInfo>>6;
  whoseTurn = undoInfo &0x7;
  undoInfo = undoInfo>>3;
  moveNumber = undoInfo&0xfff;
  undoInfo = undoInfo>>12;
   
  playerBoard bd = playerBoard[whoseTurn];
  bd.hasTakenLoan = hasTakenLoan;
  bd.declinedLoan = declinedLoan;
  bd.bidReceived = (undoInfo&1)!=0;
  undoInfo = undoInfo>>1;
  bd.requestingBid = (undoInfo&1)!=0;
  undoInfo = undoInfo>>1;
  bd.cannotRebid = (undoInfo&1)!=0;
  undoInfo = undoInfo>>1;
  bd.setProducers(undoInfo&0xf);
  undoInfo = undoInfo>>4;
  isSecondAction = ((undoInfo&1)!=0);
  undoInfo = undoInfo>>1;
  hasProduced = (undoInfo&1)!=0;
  
//  boughtWarehouseGoodsFrom = from_b.boughtWarehouseGoodsFrom;
//  boughtFactoryGoodsFrom = from_b.boughtFactoryGoodsFrom;
//  movedToSeaFrom = from_b.movedToSeaFrom;

  return(unpaid);
  }

  private void stackCashInfo(IStack on)
  {	for(int i=0,lim=playerBoard.length; i<lim; i++) 
  		{ playerBoard[i].pushLoanState(on);
  		}
  	on.push(bank);
  }
  private void unstackCashInfo(IStack on,boolean discard)
  {	int b = on.pop();
    if(!discard) { bank = b; }
  	for(int i=playerBoard.length-1; i>=0; i--) 
  		{ playerBoard[i].popLoanState(on,discard); }
  }
  private void copyCashInfo(IStack from,IStack to)
  {	int toidx = to.size();
  	int fromidx = from.size();
  	stackCashInfo(to);
  	int siz = to.size()-toidx;	// get the size of the stacked goods
  	fromidx -= siz;
  	for(int i=0;i<siz;i++) { to.setElementAt(toidx+i,from.elementAt(fromidx+i)); }
  }
  private void saveGlobalUndo(ContainerMovespec m)
  {	  int goods =  factoryGoodsProduced*10 + warehouseGoodsBought;
  	  robotStack.push(shuffleCount);
  	  robotStack.push(goods);
  	  robotStack.push(playerBoard[whoseTurn].virtual_cash);
  	  robotStack.push(last_sale_move);
  	  robotStack.push(sales_this_move);
      switch(m.op)
      {
      case MOVE_DONE:
    	  {		if(m.container==0) { m.container = getDoneCode(); }
    	       	switch(m.container)
            	{
    	       	default: throw G.Error("not expecting done code %s",m.container);
                case Done_Finish_Auction:
                	copyBidInfo(undoStack,robotStack);
                	stackBidInfo(robotStack);
                	break;
            	case Done_Start_Loan_0:
            	case Done_Start_Loan_1:
            		ContainerState ps = undoStateStack.pop();
            		undoStateStack.push(ps);
            		robotStateStack.push(ps);
            		break;
            	case Done_Finish_Loan:
                case Done_Produce_Luxury:
                case Done_Produce:
                case Done_Load_Warehouse:
            	case Done_Do_Nothing: 
            	case Done_Reprice:
            	case Done_Repay_Loan:
            	case Done_Start_Auction:
            	case Done_Move_Ship_0:
            	case Done_Move_Ship_1:
            	case Done_Move_Ship_2:
            	case Done_Move_Ship_3:
            	case Done_Move_Ship_4:
            	case Done_Load_Ship_0:
            	case Done_Load_Ship_1:
            	case Done_Load_Ship_2:
            	case Done_Load_Ship_3:
            	case Done_Load_Ship_4:
          		
            			break;
            	}
    	       	
    	  // note, this should probably be rolled into the switch above.
       	  playerBoard bd = playerBoard[m.player];
    	  boolean pending = pendingLoanRepayment(m);
    	  boolean reqesting = bd.requestingLoan;
    	  m.to_row = shipLoad;
    	  m.from_row = reqesting ? 2 : pending ?1:0;
    	  if(pending || reqesting) 
    	  	{ if(reqesting) 
    	  		{ copyLoanInfo(undoStack,robotStack); 
    	  		  robotStateStack.push(undoStateStack.top());
    	  		  stackBidInfo(robotStack); 
    	  		 }
    	  	  if(pending)
    	  	  	{ copyCashInfo(undoStack,robotStack);
    	  	  	}
	  		  stackCashInfo(robotStack);  
   	  	  
    	  	}
    	  }
    	  break;
      case MOVE_FUND:
      case MOVE_DECLINE:
      case MOVE_DECLINE_LOAN:
    	  stackBidInfo(robotStack);
    	  stackCashInfo(robotStack);
    	  break;
      case MOVE_ACCEPT:
      case MOVE_ACCEPT_LOAN:
      case MOVE_BUY:
      case MOVE_BID:
      	stackBidInfo(robotStack);
		break;
	default:
		break;
      }
  	playerBoard bd = playerBoard[m.player];
  	
  	// note we have only 32 bits to play with, so this int is maxed out
    int undoInfo = (hasProduced?1:0);
    undoInfo = (isSecondAction?1:0) | (undoInfo<<1);
    undoInfo = bd.getProducers()|(undoInfo<<4);				// which machines have produced
    undoInfo = (bd.cannotRebid?1:0) | (undoInfo<<1); 
    undoInfo = (bd.requestingBid?1:0) | (undoInfo<<1);
    undoInfo = (bd.bidReceived?1:0) | (undoInfo<<1);
    undoInfo = moveNumber | (undoInfo<<12);
    undoInfo = whoseTurn | (undoInfo<<3);
    undoInfo =  (undoInfo<<6);
    m.undoInfo = undoInfo;	  
    robotState.push(board_state);
    int undoinfo2 = playerSource+1;								// 3 bits
    undoinfo2 = (undoinfo2<<3)+(loadedShipFrom+1);				// 6 bits
    undoinfo2 = (undoinfo2<<3)+(loadedWarehouseFrom+1);			// 9 bits
    undoinfo2 = (undoinfo2<<3)+(movedToSeaFrom+1);				// 12 bits
    
    
    int lux = (produceLuxuryFirstColor==null)?0:produceLuxuryFirstColor.getContainerIndex()+1;
    if(lux!=0) 
    	{ lux = lux<<1;
    	  switch(produceLuxuryFromRack)
    	  {	default: throw G.Error("not expecting location %s",produceLuxuryFromRack);
    	    case WarehouseGoodsLocation: lux |= 1; break;
    	    case FactoryGoodsLocation: break;
    	  }
    	}
    undoinfo2 = (undoinfo2<<4) | lux;						// 16 bits
    
    undoinfo2 = (undoinfo2<<1) | (hasProducedLuxury?1:0);	// 17 bits
    undoinfo2 = (undoinfo2<<1) | (mustPayLoan?1:0);			// 18 bits
    {
    playerBoard nexbd = playerBoard[getNextPlayer()];
    undoinfo2 = (undoinfo2<<MAX_LOANS) | bd.getUnpaidStatus();			// 20 bits
    undoinfo2 = (undoinfo2<<MAX_LOANS) | nexbd.getUnpaidStatus();		// 22 bits
    }
    undoinfo2 = (undoinfo2<<1) | (hasRepricedFactory?1:0);				// 23 bits
    undoinfo2 = (undoinfo2<<1) | (hasBoughtWarehouseGoods?1:0);			// 24 bits
    undoinfo2 = (undoinfo2<<1) | (hasRepricedWarehouse?1:0);			// 25 bits
    
    undoinfo2 = (undoinfo2<<1) | (bd.hasTakenLoan?1:0);					// 26 bits
    undoinfo2 = (undoinfo2<<1) | (bd.declinedLoan?1:0);					// 27 bits
    undoinfo2 = (undoinfo2<<1) | (hasBoughtWarehouse?1:0);				// 28 bits
    undoinfo2 = (undoinfo2<<1) | (hasBoughtMachine?1:0);				// 29 bits
    m.undoInfo2 = undoinfo2<<1;		// leave 1 extra bit for the stack undo bit		30 bits
  }

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ContainerMovespec m)
    {	
    	// some opcodes add or remove bid info from the undo stack, so we have to be able to reverse those changes.
     	int stack = undoStack.size();
    	saveGlobalUndo(m);
        // to undo state transistions is to simple put the original state back.
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m+m.search_clock);

        if (Execute(m,replayMode.Replay))
        {	boolean changedStack = (undoStack.size()!=stack);
        	switch(m.op)
        	{
        	case MOVE_DONE:
        		//if(changedStack) { stackBidInfo(robotStack2); }
        		break;
        	case MOVE_FROM_TO:
        	case MOVEC_FROM_TO:
        	case MOVE_PASS:
        	case MOVE_BID:
        		break;
        	case MOVE_DECLINE:
        	case MOVE_DECLINE_LOAN:
        	case MOVE_ACCEPT_LOAN:
        	case MOVE_FUND:
        		// fund or decline changes everything when the last player decides.
        		if(changedStack) { stackBidInfo(robotStack2); }
        		break;
        	case MOVE_ACCEPT:
        	case MOVE_BUY:
        		// these four moves remove items from the board undo stack.
        		// save them on a separate stack so we can undo them in the opposite order
        		//stackBidInfo(robotStack2);
        		break;
        	default:
        		throw G.Error("Robot Execute not fully handled %s",m);


        	}
        m.undoInfo2 |= (changedStack?1:0);
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(ContainerMovespec m)
    {
    	ContainerState prev_state = board_state;
    	int prev_move = moveNumber;
    	int prev_player = whoseTurn;
    	boolean changedStack = (m.undoInfo2&1)!=0;
    	
	    int unpaid = restoreGlobalUndo(m);
	    //U P0[bid 0]45886   	
	    //G.print("U "+m+m.search_clock);
        
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

    	case MOVE_DECLINE:
    	case MOVE_DECLINE_LOAN:
   	    case MOVE_ACCEPT_LOAN:
    	case MOVE_FUND:
    		if(changedStack) 
    			{	// fund or decline removed items from the undo stack, we must put them back
    			moveBidInfo(robotStack2,undoStack);
    			}
    		break;
    	case MOVE_BUY:
   	    case MOVE_ACCEPT:
   	    	{
       		selectAsFunder(null);
   	    	shipLoad = m.from_row;
   	    	undoAuction(shipLoad);
   	    	shipLoad = 0;
   	    	break;
   	    	}
        case MOVE_DONE:
        	switch(m.container)
        	{
        	case Done_Move_Ship_0:
        	case Done_Move_Ship_1:
        	case Done_Move_Ship_2:
        	case Done_Move_Ship_3:
        	case Done_Move_Ship_4:
           	case Done_Load_Ship_0:
        	case Done_Load_Ship_1:
        	case Done_Load_Ship_2:
        	case Done_Load_Ship_3:
        	case Done_Load_Ship_4:
           	case Done_Load_Warehouse:
        		break;
        		
        	case Done_Finish_Loan:
            case Done_Produce_Luxury:
            case Done_Produce:
            case Done_Finish_Auction:
        	case Done_Do_Nothing: 
        	case Done_Reprice:
        	case Done_Repay_Loan:
        		break; 
        	case Done_Start_Auction:
        		unstackBidInfo(undoStack);
        		break;
        	case Done_Start_Loan_0:
        	case Done_Start_Loan_1:
        		// (prev_state==FINANCEER_STATE)||(prev_state==FUND_LOAN_STATE)||(prev_state==AUCTION_STATE)
        		if((makeFastLoans || first_shipment))
        			{
        			playerBoard bd=playerBoard[whoseTurn];
        			switch(m.container)
        				{
        				case Done_Start_Loan_0:
        					{
        					ContainerState ps = robotStateStack.pop();
        					undoStateStack.push(ps);
        					bd.repayLoan(bd.loans[0]);
        					bd.cancelLoan(0);
        					}
        					break;
        				case Done_Start_Loan_1:
        					{
        					ContainerState ps = robotStateStack.pop();
        					undoStateStack.push(ps);
        					bd.repayLoan(bd.loans[1]);
        					bd.cancelLoan(1);
       					}
        					break;
					default:
						break;
        				}
        			}
        		else
        			{ unstackBidInfo(undoStack);
        			};		// added bid info to the stack
        		break;
        	default: throw G.Error("Not expecting done undo code %s",m.container);
        	}

        	if(moveNumber!=prev_move)
        	{	if(prev_state==ContainerState.PLAY1_STATE)
        		{ playerBoard[prev_player].unpayInterest();
        		}
        	}
        	break;
        case MOVE_BID:
        	break;
        case MOVE_PASS:
        	playerBoard[whoseTurn].pass_count--; 
             break;
        case MOVEC_FROM_TO:
          	switch(prev_state)
        	{	default: throw G.Error("Not expecting robot in state %s",prev_state);
        		case GAMEOVER_STATE:
        		case PLAY1_STATE:
        		case CONFIRM_STATE:
        		case CONFIRM_AUCTION_STATE:
        		case TRADE_CONTAINER_STATE:
        		case REPRICE_FACTORY_STATE:
        		case REPRICE_WAREHOUSE_STATE:
        		case LOAD_LUXURY_STATE:
        		case LOAD_WAREHOUSE_GOODS_STATE:
        		case LOAD_SHIP_STATE:
        		case LOAD_SHIP_1_STATE:
        			
        			ContainerCell dest = getCell(m.dest,m.to_col,(int)(0xffff&m.to_row));
        			ContainerCell src = getCell(m.source,m.from_col, (int)(0xffff&m.from_row));
        			int ind = m.container>>12;
        			ContainerChip picked = pickObject(dest);
        			dropObject(src,ind);		// resstore to the original position
        			unSetStateAfterDrop(board_state,src,dest,picked);
       			    acceptPlacement();
                    break;
        	}
        	break;

        case MOVE_FROM_TO:
           	switch(prev_state)
        	{	default: throw G.Error("Not expecting robot in state %s",prev_state);
        		case GAMEOVER_STATE:
        		case PLAY1_STATE:
        		case CONFIRM_STATE:
        		case LOAD_FACTORY_GOODS_STATE:
        		case LOAD_SHIP_1_STATE:
        		case LOAD_WAREHOUSE_GOODS_STATE:
        		case AUCTION_STATE:
        		case FINANCEER_STATE:
        		case REBID_STATE:
        		case LOAD_SHIP_STATE:
        			ContainerCell dest = getCell(m.dest,m.to_col,(int)(0xffff&m.to_row));
        			ContainerCell src = getCell(m.source,m.from_col, (int)(0xffff&m.from_row));
        			ContainerChip picked = pickObject(dest);
        			dropObject(src);
        			unSetStateAfterDrop(board_state,src,dest,picked);
       			    acceptPlacement();
                    break;
        	}
        	break;
        }
        sales_this_move = robotStack.pop();
        last_sale_move = robotStack.pop();
         {	
        	
        	playerBoard bd = playerBoard[whoseTurn];
        	bd.virtual_cash =robotStack.pop();
            playerBoard nextbd = playerBoard[getNextPlayer()];
  	  		nextbd.setUnpaidStatus(unpaid&(~(-1<<MAX_LOANS)));
  	  		unpaid = unpaid>>MAX_LOANS;
  	  		bd.setUnpaidStatus(unpaid);
        }
         {int goods = robotStack.pop();
	  		warehouseGoodsBought = goods%10;
	  		factoryGoodsProduced = goods/10;
		}
         shuffleCount = robotStack.pop();
    }

/**
 * player gesture is to drag a loan card to his mat.  This enters a special loan bid mode
 * 
 * @param all
 * @param who
 */
void  getLoanMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	playerBoard bd = playerBoard[who];
    if(!bd.declinedLoan && ((robot!=null) ? (bd.cash<STANDARD_LOAN_AMOUNT) : true))
    {
	ContainerCell loan = bd.nextEmptyLoan();
	ContainerCell from = loanCards;
	if((loan!=null)&&((source==null)||(source==loanCards)))
	{	all.addElement(new ContainerMovespec(from.rackLocation(),from.col,from.row,
				loan.rackLocation(),loan.col,loan.row,who));
	}}
}

/**
 * player gesture is to drag a loan card from his mat.  This enters a special loan bid mode
 * 
 * @param all
 * @param who
 */
void  getRepayLoanMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip) 
{	playerBoard bd = playerBoard[who];
	if(!bd.hasTakenLoan)
		{
		for(int i=0;i<MAX_LOANS;i++)
		{	ContainerCell c = bd.loans[i];
		if(((c==source)||(c.topChip()!=null)) && (bd.cash>=bd.loanRepaymentAmount(c)))
			{	all.addElement(new ContainerMovespec(c.rackLocation(),c.col,c.row,loanCards.rackLocation(),loanCards.col,loanCards.row,who));
			}
		}
	}
}

void  getFundLoanMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	playerBoard bd = playerBoard[who];
	int available_cash = bd.cash-bd.loanBidAmount;
	all.addElement(new ContainerMovespec(MOVE_DECLINE,who));
	if(available_cash>=((robot!=null)?(STANDARD_LOAN_AMOUNT+9):STANDARD_LOAN_AMOUNT))
	{	all.addElement(new ContainerMovespec(MOVE_FUND,who));
	}
}
int valueAtAuction_v4(int auctioneer,double game_stage,ContainerGoalSet goalSet,boolean print)
{	int highBid = -1;
	int highWinner = -1;
	int secondBid = -1;
	for(int buyer=playerBoard.length-1; buyer>=0; buyer--)
	{	
		playerBoard obd = playerBoard[buyer];
		int cash  = Math.max(0,obd.cash+((obd.loans_taken<MAX_LOANS)?STANDARD_LOAN_AMOUNT-1:0)-obd.loans_taken);
		int fair = fairBid_v4(auctioneer,buyer,cash,print,goalSet,game_stage);
		if(fair>highBid) { secondBid = highBid; highBid = fair; highWinner = buyer; }
		else if(fair>secondBid) { secondBid = fair; }
	}
	if((highWinner == auctioneer) && (highBid>(secondBid*2)))
	 { return(highBid); 
	 }	// we can always buy for ourselves
	if(highBid>secondBid) { secondBid++; } 
	return(secondBid*2);
}
void  getAuctionMoves_v4(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,ContainerGoalSet goalSet) 
{	playerBoard bd = playerBoard[who];

	int available_cash = bd.cash;
	// for the robot, we make just one bid, based on a fairly

		// elaborate estimate of the bid's value.  Assuming all the
		// other players bid the most they can afford, we bid just 
		// a little more (unless that's too much for us)
		int active_loans = bd.loans_taken;
		double stage = robot.game_stage();

		int minbid = bd.bidAmount;	// in case of rebid
		available_cash -= (active_loans==MAX_LOANS)?2*MAX_LOANS:active_loans;
	    available_cash=Math.max(available_cash,minbid);			// save money for the interest
		int requesting = playerRequestingBid();
		int fair = Math.max(minbid,fairBid_v4(requesting,who,available_cash,false,goalSet,stage));
		int max_other_fair = -1000;
		for(int i=playerBoard.length-1; i>=0; i--)
		{	if(i!=who)
			{
			playerBoard obd = playerBoard[i];
			int cash  = Math.max(0,obd.cash-(1+obd.loans_taken)+((obd.loans_taken==0)?STANDARD_LOAN_AMOUNT:0));
			int other_fair = fairBid_v4(requesting,i,cash,false,goalSet,stage);
			if(other_fair>max_other_fair) { max_other_fair = other_fair; }
			}
		}
		int finalbid = Math.max(0, Math.min(available_cash, Math.max(minbid,robot.overbidAmount()+(fair+max_other_fair+(minbid>0?2:1))/2)));
		
		//G.print("p"+who+" fair "+fair+" o "+max_other_fair+" f "+finalbid);
		//include goods on the ship and loans we could take out as part of working capital
		int working_capital = available_cash
				+(STANDARD_LOAN_AMOUNT*(MAX_LOANS-bd.loans_taken)
				+(int)(bd.shipGoods.height()*ROBOT_UNIFORM_ISLAND_GOODS[0]));
	    if( ((working_capital-finalbid)<ROBOT_CAPITAL_TARGET)&&(finalbid>minbid))
	    {	// watch for cash starvation, so unless we have money on the ship we
	    	// can't bid much
	    int dif = (finalbid-minbid)/2;
	    if(minbid>0) { all.addElement(new ContainerMovespec(MOVE_BID,minbid,who)); }	
	    if(dif>0) {all.addElement(new ContainerMovespec(MOVE_BID,finalbid-dif,who)); }	
	    }
	    G.Assert(finalbid<=bd.cash,"legal bid");
		//
		// make one bid which is the lowest bid we can make and still win and make money
		//
		all.addElement(new ContainerMovespec(MOVE_BID,finalbid,who));
}
// cash estimate, including ship goods and loans
int estimate_available_cash(playerBoard obd,double game_stage,boolean include_loans)
{
	double shipcash = 0.5*obd.shipGoods.height()*ROBOT_UNIFORM_ISLAND_GOODS[0];		// easy cash by auctioning the boat
	double loancash = include_loans?(MAX_LOANS-obd.loans_taken)*STANDARD_LOAN_AMOUNT:0;
	// give credit for goods on the ship that can be converted to cash fast,
	// and for emergency loans, which are more likely to be used to put goods on the island
	// later in the game.
	return(Math.max(0,obd.cash-obd.loans_taken*LOAN_INTEREST_PAYMENT + (int)(shipcash+loancash)));
}

// this is used to guestimate the value we can get from auctioning a ship
int valueAtAuction_v5(int auctioneer,double game_stage,ContainerGoalSet goalSet,boolean print)
{	int highBid = -1;
	int highWinner = -1;
	int secondBid = -1;
	for(int buyer=playerBoard.length-1; buyer>=0; buyer--)
	{	
		playerBoard obd = playerBoard[buyer];
		// give credit for goods on the ship that can be converted to cash fast,
		// and for emergency loans, which are more likely to be used to put goods on the island
		// later in the game.
		int cash  = estimate_available_cash(obd,game_stage,true);
		double min_fair_bid = (1.0-game_stage)*minimum_fair_bid_v5(cash,buyer);		// generic estimate
		double other_fair_bid = game_stage*fairBid_v5(auctioneer,buyer,cash,print,goalSet,game_stage);
		int fair = (int)(min_fair_bid+other_fair_bid);
		//
		// consider the cash situation
		int cash_shortfall = ((cash-fair)-ROBOT_CAPITAL_TARGET);
	    if(cash_shortfall<0)
	    {	// watch for cash starvation, so unless we have money on the ship we
	    	// can't bid much
		int halfbid = (fair+1)/2;
	    fair = Math.max(halfbid,(fair+cash_shortfall));
	    }
		
		
		if(fair>highBid) { secondBid = highBid; highBid = fair; highWinner = buyer; }
		else if(fair>secondBid) { secondBid = fair; }
	}
	if(highWinner == auctioneer)
	 { return(Math.max(highBid-secondBid*2,secondBid*2)); 
	 }	// we can always buy for ourselves
	return(secondBid*2);
}

static final double FAIR_AUCTION_VALUE_V5 = 3.5;
private double minimum_fair_bid_v5(int cash, int seller)
{
	playerBoard bd = playerBoard[seller];
	return(Math.min(cash,FAIR_AUCTION_VALUE_V5*bd.shipGoods.height()));
}
void  getAuctionMoves_v5(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,ContainerGoalSet goalSet) 
{	playerBoard bd = playerBoard[who];
	double stage = robot.game_stage();
	int minbid = bd.bidAmount;	// in case of rebid
	int available_cash = Math.max(minbid, estimate_available_cash(bd,stage,true));
	
	// for the robot, we make just one bid, based on a fairly
	// elaborate estimate of the bid's value.  Assuming all the
	// other players bid the most they can afford, we bid just 
	// a little more (unless that's too much for us)
	int requesting = playerRequestingBid();
	double min_fair = (1.0-stage)*minimum_fair_bid_v5(available_cash,requesting);
	double our_fair = stage*fairBid_v5(requesting,who,available_cash,false,goalSet,stage);
	int fair = (int)(min_fair+our_fair);
	int max_other_fair = -1000;
	for(int i=playerBoard.length-1; i>=0; i--)
		{	if(i!=who)
			{
			playerBoard obd = playerBoard[i];
			int cash  = Math.max(0,estimate_available_cash(obd,stage,true));
			double min_fair_bid = (1.0-stage)*minimum_fair_bid_v5(cash,requesting);		// generic estimate
			double other_fair_bid = stage*fairBid_v5(requesting,i,cash,false,goalSet,stage);	// specific estimate
			int other_fair = (int)(min_fair_bid+other_fair_bid);
			if(other_fair>max_other_fair) { max_other_fair = other_fair;  }
			}
		}
		int preferred_bid = minbid;
		if(fair<max_other_fair) 
		{	// bid up the price, lose a little, in proportion to the stage of the game.
			preferred_bid = fair;
		}
		else if(fair==max_other_fair) { preferred_bid = fair+1; }
		else { // we can afford to pay more, so plan to beat out the competition, not at full price
			preferred_bid = (int)(max_other_fair+1+(fair-max_other_fair)*stage); 
		}
		int finalbid = Math.max(minbid, Math.min(bd.cash, robot.overbidAmount()+preferred_bid));
		
		//G.print("p"+who+" fair "+fair+" o "+max_other_fair+" f "+finalbid);
		//include goods on the ship and loans we could take out as part of working capital
		int cash_shortfall = ((available_cash-finalbid)-ROBOT_CAPITAL_TARGET);
	    if( (cash_shortfall<0) &&(finalbid>minbid))
	    {	// watch for cash starvation, so unless we have money on the ship we
	    	// can't bid much
		int halfbid = (finalbid-(finalbid-minbid)/2);
	    int shortbid = Math.max(halfbid,(finalbid+cash_shortfall));
	    if(minbid>0) { all.addElement(new ContainerMovespec(MOVE_BID,minbid,who)); }	
	    if(shortbid>minbid) { finalbid = shortbid; }	
	    }
	    G.Assert(finalbid<=bd.cash,"legal bid");
		//
		// make one bid which is the lowest bid we can make and still win and make money
		//
		all.addElement(new ContainerMovespec(MOVE_BID,finalbid,who));	


}
void getAllCashBids(CommonMoveStack  all,int who,boolean forLoan)
{	playerBoard bd = playerBoard[who];
	int available_cash = bd.cash;
	for(int i=bd.bidAmount,lim=Math.min(20,available_cash); i<=lim;i++)
		{ all.addElement(new ContainerMovespec(MOVE_BID,i,who));
		}
}
void  getAuctionMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,ContainerGoalSet goalSet)
{	playerBoard bd = playerBoard[who];
	if(!bd.requestingBid && !bd.cannotRebid)
	{
	if((robot!=null)&&!robot.MONTEBOT) { robot.getAuctionMoves(all,who,source,chip,goalSet); }
	else
	{	// all possible bids for the humans
		
		getAllCashBids(all,who,false);
	}
	}
}
void getLoanAuctionMoves(CommonMoveStack  all,int who,ContainerPlay robot)
{
	playerBoard bd = playerBoard[who];
	if(!bd.requestingBid && !bd.cannotRebid)
	{
		if(robot!=null)
		{
		int boost = (int)Math.sqrt(Math.max(0,(bd.cash-bd.loanBidAmount-5)/10));	// increase from base amount if we're flush
		all.addElement(new ContainerMovespec(MOVE_BID,STANDARD_LOAN_AMOUNT+boost,who));	
		}
		else 
		{
			getAllCashBids(all,who,true);
		}
	}
}
//
// get the current high bid, which is only meaningful when the bidding
// process is complete.
//
public int getHighBid()
{	int highbid=-1;
	for(int i=0,lim=playerBoard.length; i<lim; i++)
	{	playerBoard bd = playerBoard[i];
		if(!bd.requestingBid) { highbid = Math.max(highbid,bd.bidAmount); }
	}
	return(highbid);
}
public boolean someoneRequestingLoan()
{
	for(int i=0,lim=playerBoard.length; i<lim; i++) { if(playerBoard[i].requestingLoan) { return(true); }}
	return(false);
}
public int playerRequestingBid()
{
	for(int i=0,lim=playerBoard.length; i<lim; i++) { if(playerBoard[i].requestingBid) { return(i); }}
	return(-1);
}
//
// bids (and rebids) have been made.  The alternative is to accepr any of the high bid amounts,
// or reject the bids and pay the amount yourself.
//
void  getAcceptBidMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip) 
{	playerBoard us = playerBoard[whoseTurn];
	G.Assert(us.requestingBid,"we are the auctioneer");
	int highbid = getHighBid();
	for(int i=0,lim=playerBoard.length; i<lim; i++)
	{	if(i!=who)
		{
		playerBoard bd = playerBoard[i];
		if(bd.bidAmount==highbid) { all.addElement(new ContainerMovespec(MOVE_ACCEPT,i,who)); }
		}
	}
	if(us.cash>=highbid) 
		{ all.addElement(new ContainerMovespec(MOVE_BUY,highbid,who)); 
		}
}
void  getAcceptLoanMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	playerBoard us = playerBoard[whoseTurn];
	G.Assert(us.requestingLoan,"we are the requestor");
	int added=0;
	int high = getHighBid();
	for(int i=0,lim=playerBoard.length;i<lim;i++)
	{	playerBoard bd = playerBoard[i];
		if(bd.willFundLoan && (bd.bidAmount==high)) 
		{	all.addElement(new ContainerMovespec(MOVE_ACCEPT_LOAN,i,who)); 
			added++;
		}
	}
	if(added==0) 
		{ all.addElement(new ContainerMovespec(MOVE_ACCEPT_LOAN,-1,who)); 
		}
	if((robot==null) && (board_state!=ContainerState.TAKE_LOAN_STATE))
	{	// in take loan state, we have no choice. 
	all.addElement(new ContainerMovespec(MOVE_DECLINE_LOAN,who));
	}
}
/**
 * player gesture is to move a macine to his mat.
 * @param all
 * @param who
 */
void  getBuyMachineMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip) 
{	playerBoard pb = playerBoard[who];
	ContainerCell e = pb.nextEmptyMachine();
	if(e!=null)
	{	
		if(pb.cash>=MACHINE_PRICES[e.row])
		{
		for(int i=0,lim=machineStorage.length; i<lim;i++)
		{	ContainerCell c = machineStorage[i];
			ContainerChip t = c.topChip();
			if( pb.canBuyMachine(t) && ((source==null)&& (t!=null)) || (c==source))
			{	all.addElement(new ContainerMovespec(c.rackLocation(),c.col,c.row,
														e.rackLocation(),e.col,e.row,who));
			}
		}}
	}
}
/** 
 * player gesture is to move a warehouse to his mat
 * @param all
 * @param who
 */
void  getBuyWarehouseMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip) 
{	playerBoard pb = playerBoard[who];
	ContainerCell e = pb.nextEmptyWarehouse();
	if(e!=null)
	{	if(pb.cash>=WAREHOUSE_PRICES[e.row])
		{
		ContainerCell c = warehouseStorage;
		ContainerChip t = c.topChip();
		if( ((t!=null)&&(source==null)) || (source==c))
		{	all.addElement(new ContainerMovespec(c.rackLocation(),c.col,c.row,
													e.rackLocation(),e.col,e.row,who));
		}}
	}
}

/**
 * move the boat
 * @param all
 * @param who
 */
void  getMoveShipMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	playerBoard pb = playerBoard[who];
	ContainerCell ship = pb.shipLocation;
	ContainerCell goods = pb.shipGoods;
	int nGoods = goods.height();
	if( (source==null) || (source==ship))
	{
	switch(ship.rackLocation())
	{
	default: throw G.Error("Not expting ship location");
	case AuctionLocation:
		// waving the mouse around during an auction
		break;
	case AtSeaLocation:
		if(nGoods<MAX_SHIP_GOODS)
		{
		// disallow moving to a dock if we are fully loaded, there's nothing to 
		// do there.  also disallow moving to a dock that is empty
		for(int i=0,lim=playerBoard.length; i<lim;i++)
		{	playerBoard dest = playerBoard[i];
			if((dest!=pb)								// not our own dock
				&& ((robot==null)||(i!=movedToSeaFrom))	// not the place the robot just left
				&&  dest.hasWarehouseGoods(pb.cash))	// and has goods
			{
			if(source!=null)
			{	// as a matter of style, allow a ship to dock at any of the docks
				for(int dockIdx=0,dlim=MAX_PLAYERS-1; dockIdx<dlim; dockIdx++)
				{	ContainerCell dock = dest.getDockLocation(dockIdx);
					if(dock.topChip()==null)
					{
						all.addElement(new ContainerMovespec(ship.rackLocation(),ship.col,ship.row,
								dock.rackLocation(),dock.col,dock.row,who));
					}
				}
			}
			else {
				ContainerCell dock = dest.nextEmptyDock();
				all.addElement(new ContainerMovespec(ship.rackLocation(),ship.col,ship.row,
								dock.rackLocation(),dock.col,dock.row,who));
			}
				
			}
			
		}}
		// we can go to the auction block if we have goods
		if((nGoods>0) && ((robot==null) || (board_state==ContainerState.PLAY2_STATE)))
		{// move robot to action only on second moves
		all.addElement(new ContainerMovespec(ship.rackLocation(),ship.col,ship.row,
						auctionBlock.rackLocation(),auctionBlock.col,auctionBlock.row,who));
		}
		break;
	case AtIslandParkingLocation:
	case AtDockLocation:
		// we can always go to sea
		if(source!=null)
		{	// as a matter of style, let him take whatever atsea parking slot he wants
		for(int idx=0,lim=atSea.length; idx<lim;idx++)
		{	ContainerCell dest = atSea[idx];
			if(dest.topChip()==null)
			{
				all.addElement(new ContainerMovespec(ship.rackLocation(),ship.col,ship.row,
						dest.rackLocation(),dest.col,dest.row,who));
			}
		}
		}
		else
		{
		ContainerCell dest = ContainerCell.nextEmpty(atSea);
		all.addElement(new ContainerMovespec(ship.rackLocation(),ship.col,ship.row,
				dest.rackLocation(),dest.col,dest.row,who));
		
		}
		break;
		
	}}
	
}

// get moves to produce a partucular type of container
void getProduceMoves(CommonMoveStack  all,playerBoard bd,ContainerCell src,int who)
{
	for(int priceIdx=0,plim=FACTORY_GOOD_PRICES.length; priceIdx<plim; priceIdx++)
	{
		if(bd.canPlaceFactoryGood(priceIdx,second_shipment))
		{
		ContainerCell dest = bd.getFactoryStorageLocation(priceIdx);
		
		all.addElement(new ContainerMovespec(src.rackLocation(),src.col,src.row,
					dest.rackLocation(),dest.col,dest.row,who));
		}
	}	
}
void getLoadLuxuryMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip)
{	ContainerCell gold = containerStorage[CONTAINER_GOLD-CONTAINER_OFFSET];
	if((source==null) || (source==gold))
	{
	playerBoard bd = playerBoard[who];
	switch(produceLuxuryFromRack)
	{
	case FactoryGoodsLocation: getProduceMoves(all,bd,gold,who);
		break;
	case WarehouseGoodsLocation: buyFactoryGood(who,all,gold,gold.topChip());
		break;
	default:
		break;
	}
	}
}
/**
 * player gesture is to move a container to his mat.
 * @param all
 * @param who
 */
void  getProduceMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip) 
{	playerBoard bd = playerBoard[who];
	int lim = bd.numberOfActiveMachines();
	if(((bd.cash>=COST_TO_PRODUCE)	|| bd.someMachineHasProduced()) 
		&& ((source==null) || (source.rackLocation()==ContainerId.ContainerLocation))) 
	{	// it costs $1 to produce
	int usedMachines = 0;
	for(int i=0;i<lim;i++)
	{	if(bd.machineCanProduce(i,chip))
		{	ContainerChip fac = bd.getMachineLocation(i).topChip();
			int machineIndex = fac.getMachineIndex();
			int mmask = (1<<machineIndex);
			if((usedMachines&mmask)==0)	// if we haven't yet produced with a machine of this color
			{
			ContainerCell src = containerStorage[machineIndex];
			usedMachines |= mmask;				// avoid producing twice with machines of the same color, which would be a duplicate move
			if((src.topChip()!=null) 	// there are some left
					&& (source==null)||(src==source))
			{
			getProduceMoves(all,bd,src,who);
			}}

		}
	}}
} 
/**
 * reprice factory moves
 * @param all
 * @param who
 * @param source
 */
void  getRepriceFactoryMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,ContainerGoalSet goalSet) 
{	playerBoard bd = playerBoard[who];
	int flim = FACTORY_GOOD_PRICES.length;
	if((robot!=null))
	{	setupIslandGoodsValues(who,goalSet);
	}
	for(int priceIdx=0,plim=flim; priceIdx<plim; priceIdx++)
	{
	if(bd.canPlaceFactoryGood(priceIdx,second_shipment || (robot!=null)))
		{
		ContainerCell dest = bd.getFactoryStorageLocation(priceIdx);
		for(int fromIdx=0;fromIdx<flim; fromIdx++)
		{
		if(fromIdx!=priceIdx)
		{
		ContainerCell src = bd.getFactoryStorageLocation(fromIdx);
		if(src==source)
		{	if(dest!=src)
			{
			all.addElement(new ContainerMovespec(src.rackLocation(),src.col,src.row,CONTAINER_OFFSET+chip.getContainerIndex(),
					dest.rackLocation(),dest.col,dest.row,who));
			}
		}
		else if(source==null)
		{
		int colors = 0;
		for(int chipIndex = 0;chipIndex<=src.chipIndex; chipIndex++)
		{	
		ContainerChip container = src.chipAtIndex(chipIndex);
		int ind = container.getContainerIndex();
		int mask = 1<<ind;
		boolean newmove = (colors&mask)==0; 
		
		if(robot!=null)
			{	// never price anything at 4
				
				double val =  robot.estimatedIslandGoodValue(goalSet,container,who)*FACTORY_GOODS_PRICE_MULTIPLIER;
				int newprice = WAREHOUSE_GOOD_PRICES[priceIdx];
				int oldprice =  WAREHOUSE_GOOD_PRICES[fromIdx];
				if((newprice==4) || (Math.abs(val-newprice)>=Math.abs(val-oldprice))) 
					{ // not moving closer to the ideal price
					newmove = false; 
					}
			}
		
		if(newmove)
		{
		colors |= mask;	// each color only once
		all.addElement(new ContainerMovespec(src.rackLocation(),src.col,src.row,CONTAINER_OFFSET+ind,
					dest.rackLocation(),dest.col,dest.row,who));
		}}
		}}
		}}
	}
} 

/**
 * reprice warehouse moves
 * @param all
 * @param who
 * @param source
 */
void  getRepriceWarehouseMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,ContainerGoalSet goalSet) 
{	playerBoard bd = playerBoard[who];
	int flim = WAREHOUSE_GOOD_PRICES.length;
	if(robot!=null)
	{	setupIslandGoodsValues(who,goalSet);
	}
	for(int priceIdx=0,plim=flim; priceIdx<plim; priceIdx++)
	{
	if(bd.canPlaceWarehouseGood(priceIdx,second_shipment || (robot!=null)))
		{
		ContainerCell dest = bd.getWarehouseStorageLocation(priceIdx);
		for(int fromIdx=0;fromIdx<flim; fromIdx++)
		{
		if(fromIdx!=priceIdx)
		{
		ContainerCell src = bd.getWarehouseStorageLocation(fromIdx);
		if(src==source)
		{	if(dest!=src)
			{
			all.addElement(new ContainerMovespec(src.rackLocation(),src.col,src.row,CONTAINER_OFFSET+chip.getContainerIndex(),
					dest.rackLocation(),dest.col,dest.row,who));
			}
		}
		else if(source==null)
		{
		int colors = 0;
		for(int chipIndex = 0;chipIndex<=src.chipIndex; chipIndex++)
		{	
		ContainerChip container = src.chipAtIndex(chipIndex);
		int ind = container.getContainerIndex();
		int mask = 1<<ind;
		boolean newmove = (colors&mask)==0; 
			
		if(robot!=null)
			{	// never price anything at 6
				double val =  robot.estimatedIslandGoodValue(goalSet,container,who)*WAREHOUSE_GOODS_PRICE_MULTIPLIER;
				int newprice = WAREHOUSE_GOOD_PRICES[priceIdx];
				int oldprice =  WAREHOUSE_GOOD_PRICES[fromIdx];
				if((newprice==6) || (Math.abs(val-newprice)>=Math.abs(val-oldprice))) 
					{ newmove = false;	// would move further away from the ideal price 
					}
				
			}

		if(newmove)
		{
		colors |= mask;	// each color only once
		all.addElement(new ContainerMovespec(src.rackLocation(),src.col,src.row,CONTAINER_OFFSET+ind,
					dest.rackLocation(),dest.col,dest.row,who));
		}}
		}}
		}}
	}
} 
void transfer(int amount,playerBoard from,playerBoard to,boolean overdraft)
{	//int st = to.cash;
	//int fr = from.cash;
	G.Assert(overdraft || from.cash>=amount,"has money");
	from.cash -= amount;
	to.cash += amount;
	//G.print("trans "+amount+" "+from+" "+fr+":"+from.cash+" >> "+to+" "+st+":"+to.cash);
	G.Assert(overdraft || from.cash>=0,"has cash");
}
void transfer_to_bank(int amount,playerBoard from,boolean overdraft)
{	//int st = from.cash;
	from.cash -= amount;
	//G.print("bank <- "+amount+from+" "+st+":"+from.cash);
	bank += amount;
	G.Assert(overdraft || from.cash>=0,"has cash");
}
void transfer_from_bank(int amount,playerBoard to)
{	//int st = to.cash;
	to.cash += amount;
	//G.print("bank-> "+amount+to+" "+st+":"+to.cash);
	bank -= amount;
}
// buy a particular container and place it on any elgible warehouse cell
private void buyFactoryGood(int who,CommonMoveStack  all,ContainerCell factory,ContainerChip good)
{	playerBoard myBoard = playerBoard[who];
	int container_code = CONTAINER_OFFSET+good.getContainerIndex();
	for(int sellPriceIdx=0,plim=WAREHOUSE_GOOD_PRICES.length; sellPriceIdx<plim; sellPriceIdx++)
	{
		if(myBoard.canPlaceWarehouseGood(sellPriceIdx,second_shipment))
		{
		ContainerCell dest = myBoard.getWarehouseStorageLocation(sellPriceIdx);
		all.addElement(new ContainerMovespec(factory.rackLocation(),factory.col,factory.row,container_code,
					dest.rackLocation(),dest.col,dest.row,who));
		}
	}	
}
// buy a particular warehouse good and load it onto your ship
private void buyWarehouseGood(int who,CommonMoveStack  all,ContainerCell warehouse,ContainerChip good)
{	playerBoard myBoard = playerBoard[who];
	int container_code = CONTAINER_OFFSET+good.getContainerIndex();
	ContainerCell ship = myBoard.shipGoods;
	all.addElement(new ContainerMovespec(warehouse.rackLocation(),warehouse.col,warehouse.row,container_code,
			ship.rackLocation(),ship.col,ship.row,who));
}
public boolean canBuyFactoryGold()
{	
	return((board_state==ContainerState.PUZZLE_STATE) || !playerBoard[whoseTurn].warehouseContainsGold());
}
/**
 * get moves where you buy factory goods for your dock
 * @param all
 * @param who
 * @param source
 */
void  getBuyFactoryGoodsMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	playerBoard myBoard = playerBoard[who];
	if(myBoard.warehouseCanStoreMore())
	{
	boolean hasGold = myBoard.warehouseContainsGold();
	if(source==null) 
		{
		playerBoard owner = playerSource>=0 ? playerBoard[playerSource] : null;	// playerSource is the locked-in player
		for(int from=0,lim=playerBoard.length; from<lim; from++)		// enumerate the players, except ourselves
		{	playerBoard fromBoard = playerBoard[from];
		if((fromBoard!=myBoard) && ((owner==null) || (owner==fromBoard)))
		{	// can't be your own factory, and if a factory is already established
			// then must stick to the same.
			for(int buyPriceIdx = 0,priceLim = FACTORY_GOOD_PRICES.length; buyPriceIdx<priceLim; buyPriceIdx++)
			{
			if((myBoard.cash >= FACTORY_GOOD_PRICES[buyPriceIdx])
					&& ((robot==null)||(from!=loadedWarehouseFrom)))

			{
			ContainerCell factory = fromBoard.getFactoryStorageLocation(buyPriceIdx);
			int number_available = factory.height();
			int colors = 0;
			for(int goodsIndex = 0; goodsIndex < number_available; goodsIndex++)
			{	ContainerChip good = factory.chipAtIndex(goodsIndex);
				if(!hasGold || !good.isGoldContainer())
				{
				int goodColor = (1<<good.getContainerIndex());
				if((colors&goodColor)==0)	// not the same color as a previous container from this slot
				{
				colors |= goodColor;
				buyFactoryGood(who,all,factory,good);
				}}
			}}
			}
			}
		}
	}
	else
	{
	if((source.rackLocation==ContainerId.FactoryGoodsLocation) && (source.playerIndex()!=whoseTurn))
	{  
		buyFactoryGood(who,all,source,chip);
	}
    }
	}
}

void  getLoadShipMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot)
{	playerBoard myBoard = playerBoard[who];
	ContainerCell ship = myBoard.shipLocation;
	if((ship.rackLocation==ContainerId.AtDockLocation)
			&& myBoard.canLoadShip())
	{	playerBoard fromBd = getPlayerBoard(ship);
		if((robot==null)||(loadedShipFrom!=fromBd.player))
		{
		if(source!=null)
		{
			if((source.col==ship.col) && (source.rackLocation==ContainerId.WarehouseGoodsLocation))
			{
			buyWarehouseGood(who,all,source,chip);
			}
		}
		else
		{
		for(int windex = 0;windex < WAREHOUSE_GOOD_PRICES.length; windex++)
		{	if(myBoard.cash>=WAREHOUSE_GOOD_PRICES[windex])
			{	ContainerCell warehouse = fromBd.getWarehouseStorageLocation(windex);
				int height = warehouse.height();
				int colors = 0;
				for(int containerIndex=0; containerIndex<height; containerIndex++)
				{	ContainerChip good = warehouse.chipAtIndex(containerIndex);
					int goodColor = (1<<good.getContainerIndex());
					if((goodColor&colors)==0)	// not the same color as a previous container at the same price
					{
					colors |= goodColor;
					buyWarehouseGood(who,all,warehouse,good);
					}
				}
			}
		}}
	}}
}
void returnToStoreMove(CommonMoveStack  all,ContainerCell c,int chiptype,int who)
{
	if(chiptype!=(CONTAINER_GOLD-CONTAINER_OFFSET))
		{
		ContainerCell dest = containerStorage[chiptype];
		all.addElement(new ContainerMovespec(
			c.rackLocation(),c.col,c.row,CONTAINER_OFFSET+chiptype,
			dest.rackLocation(),dest.col,dest.row,who));
		}
}

void returnToStoreMoves(CommonMoveStack  all,ContainerCell from[],int who,ContainerChip notChip)
{
	for(int idx=0,lim=from.length; idx<lim; idx++)
	{	ContainerCell c = from[idx];
		int colors = 0;
		if(notChip!=null) { colors |= (1<<notChip.getContainerIndex()); }
		for(int ci = 0;ci<=c.chipIndex; ci++)
		{	ContainerChip chip = c.chipAtIndex(ci);
			if(!chip.isGoldContainer())
			{
			int chiptype = chip.getContainerIndex();
			int mask = 1<<chiptype;
			if((colors&mask)==0)
			{	colors |= mask;
				returnToStoreMove(all,c,chiptype,who);
			}}
		}
	}
}

// trade a second container, with a different color from the first container
void getTradeContainerMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip)
{	
	G.Assert(produceLuxuryFirstColor!=null,"first color established");
	playerBoard bd = playerBoard[who];
	switch(produceLuxuryFromRack)
	{
	default: throw G.Error("shouldn't get here");
	case FactoryGoodsLocation:
		if(source!=null)
		{
		returnToStoreMove(all,source,chip.getContainerIndex(),who);
		}
		else
		{returnToStoreMoves(all,bd.factoryGoods,who,produceLuxuryFirstColor);
		}
		break;
	case WarehouseGoodsLocation:
		if(source!=null)
		{
		returnToStoreMove(all,source,chip.getContainerIndex(),who);
		}
		else 
		{returnToStoreMoves(all,bd.warehouseGoods,who,produceLuxuryFirstColor);
		}
	}
}
//
// produce a luxury container, starting by moving a container from
// player storage back to the global storage
//
void  getProduceLuxuryMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot) 
{	if(!hasProducedLuxury)
	{
	ContainerCell gold_reserves = containerStorage[CONTAINER_GOLD-CONTAINER_OFFSET];
	if(gold_reserves.height()>0)
	{	playerBoard bd = playerBoard[who];
		if(bd.canTradeForFactoryGold(source,chip))
		{	// start by moving some good to the storage area
			if(source!=null)
			{	returnToStoreMove(all,source,chip.getContainerIndex(),who);
			}
			else
			{
				returnToStoreMoves(all,bd.factoryGoods,who,null);
			}
		}
		if(bd.canTradeForWarehouseGold(source,chip)
			&& ((robot==null) || (currentValueOfGold()>6)))
		{
			if(source!=null)
			{
				returnToStoreMove(all,source,chip.getContainerIndex(),who);
			}
			else
			{
				returnToStoreMoves(all,bd.warehouseGoods,who,null);
			}
		}
	}}
}

 // this is for the robot, and it insists that there be actual
 // moves.
CommonMoveStack  GetListOfMoves(int search_depth,ContainerPlay robot)
 {	CommonMoveStack  v = GetListOfMoves(whoseTurn,robot,search_depth,robot.robotGoalSet());
 	ROBOT_BOARD = true;
	return(v);
 }
CommonMoveStack  GetListOfMoves(int who,ContainerPlay robot,int search_depth,ContainerGoalSet goalSet)
 {	CommonMoveStack v = new CommonMoveStack();
 	getMoves(v,who,robot,search_depth,goalSet);
 	return(v);
 }
 void addDoneMove(CommonMoveStack  all,int who)
 {	ContainerMovespec dm = new ContainerMovespec(MOVE_DONE,who);
 	// for the special case of first moves in a search, this can be vital
 	dm.container = getDoneCode();
 	all.addElement(dm);
 }
 void addPassMove(CommonMoveStack  all,int who,boolean mandatory)
 {	int size = mandatory?0:all.size();
 	switch(size)
	 {	default: 
			{
		 	playerBoard bd = playerBoard[who];
		 	// if we can move the boat don't allow a pass
		 	if(bd.shipLocation.rackLocation==ContainerId.AtIslandParkingLocation) { break; }
		 	if(bd.shipGoods.height()>0) { break; }
			}
	 	//$FALL-THROUGH$
	case 0: all.addElement(new ContainerMovespec(MOVE_PASS,who));
	 }
 }
 int getMoves(CommonMoveStack  all,int who,ContainerPlay robot,int search_depth,ContainerGoalSet goalSet)
 {	int nmoves = 0;
 	switch(board_state)
 	{
 	case PUZZLE_STATE:
 	default: throw G.Error("Not implemented");
 	case GAMEOVER_STATE:
 		break;
 	case CONFIRM_STATE:
	case CONFIRM_AUCTION_STATE:

 		addDoneMove(all,who);
 		break;
 	case TAKE_LOAN_STATE:
  	case TRADE_CONTAINER_STATE:
 	case LOAD_SHIP_1_STATE:
 	case LOAD_LUXURY_STATE:
 	case FUND_LOAN_STATE:
 	case ACCEPT_BID_STATE:
 	case AUCTION_STATE:
 	case REBID_STATE:
 	case FINANCEER_STATE:
 	case REPRICE_FACTORY_STATE:
 	case REPRICE_WAREHOUSE_STATE:
 	case LOAD_SHIP_STATE:
  	case LOAD_FACTORY_GOODS_STATE:
	case LOAD_WAREHOUSE_GOODS_STATE:
 	case PLAY1_STATE:
 	case PLAY2_STATE:
 	case ACCEPT_LOAN_STATE:
 		getListOfMoves(all,who,null,null,robot,search_depth,goalSet);
 	}
 	
   	return(nmoves);
 }
 CommonMoveStack  GetListOfMoves(ContainerCell source,ContainerChip chip,ContainerPlay robot,int search_depth,ContainerGoalSet goalSet)
 {	 CommonMoveStack all = new CommonMoveStack();
	 getListOfMoves(all,whoseTurn,source,chip,robot,search_depth,goalSet);
	 return(all);
 }
 void getListOfMoves(CommonMoveStack  all,int who,ContainerCell source,ContainerChip chip,ContainerPlay robot,int search_depth,ContainerGoalSet goalSet)
 { 	boolean mustAllowPass = false;
 	boolean allowRepriceFactory = false;
 	boolean allowRepriceWarehouse = false;
	 switch(board_state)
	 {
	 default: throw G.Error("Not expecting state %s",board_state);
	 case TAKE_LOAN_STATE:
		 getLoanMoves(all,who,source,chip,null);	// if we are in this mode, we must take a loan
		 break;
	 case ACCEPT_LOAN_STATE:
		 getAcceptLoanMoves(all,who,source,chip,robot);
		 break;
	 case ACCEPT_BID_STATE:
		 if(search_depth==0) { getLoanMoves(all,who,source,chip,robot); }
		 getAcceptBidMoves(all,who,source,chip);
		 break;
	 case LOAD_LUXURY_STATE:
		 getLoadLuxuryMoves(all,who,source,chip);
		 break;
	 case TRADE_CONTAINER_STATE:
		 getTradeContainerMoves(all,who,source,chip);
		 break;
	 case FINANCEER_STATE:
		 getLoanAuctionMoves(all,who,robot);
		 break;
	 case CONFIRM_AUCTION_STATE:
	 case AUCTION_STATE:
	 case REBID_STATE:
	 	{
		 if(search_depth==0) { getLoanMoves(all,who,source,chip,robot); }
		 getAuctionMoves(all,who,source,chip,robot,goalSet);
	 	}
		 break;
	 case REPRICE_FACTORY_STATE:
		 // only consider reprice-after-purchase as a top level move, ie; when we just did the purchase and now
		 // are about to actually reprice.  The main reason for this is that since you can make an indefinite
		 // number of reprice "moves", the horizon effect had the perverse consequence that the worst possible
		 // purchase, followed by a lot of reshuffling, would mask the undesirable consequences of later moves.
		 // this improcement also remove much of the strange behavior where the robot would place goods at high
		 // prices and then immediately reshuffle them in multiple steps.
		 if(search_depth==0) { getRepriceFactoryMoves(all,who,source,chip,robot,goalSet); }
		 if((robot!=null) && (all.size()==0)) 
		 	{ addDoneMove(all,who); }
		 break;
	 case REPRICE_WAREHOUSE_STATE:
		 // only consider reprice-after-purchase as a top level move, ie; when we just did the purchase and now
		 // are about to actually reprice.  The main reason for this is that since you can make an indefinite
		 // number of reprice "moves", the horizon effect had the perverse consequence that the worst possible
		 // purchase, followed by a lot of reshuffling, would mask the undesirable consequences of later moves.
		 // this improcement also remove much of the strange behavior where the robot would place goods at high
		 // prices and then immediately reshuffle them in multiple steps.
		 if(search_depth==0)
		 	{ getRepriceWarehouseMoves(all,who,source,chip,robot,goalSet); }
		 if((robot!=null) && (all.size()==0)) 
		 	{ addDoneMove(all,who); }
		 break;
	 case LOAD_FACTORY_GOODS_STATE:
		getProduceMoves(all,who,source,chip); 
		break;
	 case LOAD_WAREHOUSE_GOODS_STATE:
		getBuyFactoryGoodsMoves(all,who,source,chip,robot);		// must keep the same source
	 	break;
	 case LOAD_SHIP_STATE:					// load second and subsequent goods onto the ship
		 addDoneMove(all,who);
		//$FALL-THROUGH$
	case LOAD_SHIP_1_STATE:
		 if(search_depth==0) { getLoanMoves(all,who,source,chip,robot); } 
		 getLoadShipMoves(all,who,source,chip,robot);
		 break;
	 case FUND_LOAN_STATE:
		 getFundLoanMoves(all,who,source,chip,robot);
		 break;
	 case PLAY1_STATE:
		 getProduceLuxuryMoves(all,who,source,chip,robot);	// only as a pre-turn

		 // if we have a ship at sea, we must allow passing to avoid strange 
		 // warehouse purchase and loan activity.
		 mustAllowPass = (playerBoard[who].shipLocation.rackLocation==ContainerId.AtSeaLocation);

		 if((robot==null) 
		    // don't let the robot consider repayment except at top level and when no new loans exist
		    // this will prevent ping-pong loan/repay loops.
			|| ((search_depth==0) && (playerBoard[who].getUnpaidStatus()==0))) 
			 { getRepayLoanMoves(all,who,source,chip);
			 }
		//$FALL-THROUGH$
	 case PLAY2_STATE:
		 if(search_depth==0) { getLoanMoves(all,who,source,chip,robot); } 
		 if((robot==null) || (board_state==ContainerState.PLAY1_STATE)) 
		 	{ // if we're the robot, it doesn't make sense to buy a machine as a second action
			 getBuyMachineMoves(all,who,source,chip); 
		 	}
		 if((robot==null) || (board_state==ContainerState.PLAY1_STATE))  
		 	{ // if we're the robot, it doesn't make sense to buy a new warehouse as a second action
			 getBuyWarehouseMoves(all,who,source,chip); 
		 	}
		 int questionableMoves = all.size();
		 if((robot==null) || !hasRepricedFactory)
		 	{ // if we're the robot, it doesn't make sense to produce after we repriced the goods
			 int size = all.size();
			 getProduceMoves(all,who,source,chip);
			 allowRepriceFactory = size==all.size();
		 	} 
		 getMoveShipMoves(all,who,source,chip,robot);
		 if((robot==null) || !hasRepricedWarehouse) 
		 	{ // if we're the robot, it doesn't make sense to buy more goods after we repriced
			 int size = all.size();
			 getBuyFactoryGoodsMoves(all,who,source,chip,robot);
			 allowRepriceWarehouse = size==all.size();
		 	}
		 getLoadShipMoves(all,who,source,chip,robot);
		 if(search_depth==0) 
		 { if((robot==null) || (!hasRepricedFactory && allowRepriceFactory)) { getRepriceFactoryMoves(all,who,source,chip,robot,goalSet); }  
		   if((robot==null) || (!hasRepricedWarehouse && allowRepriceWarehouse)) { getRepriceWarehouseMoves(all,who,source,chip,robot,goalSet);}
		 }
		 addPassMove(all,who,mustAllowPass || (all.size()==questionableMoves));
		 break;
	 }

	 G.Assert(all.size()>0,"some moves available");
 }

 
 // 
 // make a bid and assume it was successful.
 //
 long testBid(int auctioneer,int buyer,int bidamount)
 {
		playerBoard bd = playerBoard[auctioneer];
   		playerBoard frombd = playerBoard[buyer];
   		long containerCode = frombd.transferShipToIsland(bd.shipGoods,replayMode.Replay);
    	containerCode = (containerCode<<3)|bd.player;
    	containerCode = (containerCode<<10)|(bidamount&0x3ff);
    	if(bidamount!=0)
    		{
    		if(buyer==auctioneer)
    	   	{
    			transfer_to_bank(bidamount,bd,true);
    	   	}
    		else
    		{
    			transfer_from_bank(bidamount,bd);							// and matching money from the bank
    			transfer(bidamount,frombd,bd,true);						// pay for the goods
    		}
    		}
   		return(containerCode);
 }

 
 //
 // calculate an estimated evaluation of this position for this player - that is, based on
 // the actual cash, loans, and island goods, but with a fudge on the value of the island
 // goods based on the stage of the game.  As we near the end, this converges on the regular
 // static evaluation.  This is used in figuring out what is a fair bid for a lot of goods.
 //
 public double estimatedStaticEvalPosition_v4(int who,double game_stage,boolean print,ContainerGoalSet goalSet)
 {
  	int nplay = players_in_game;
  	double scoreFor0 = currentEstimatedScoreForPlayer_v4(0,game_stage,goalSet);
 	double scoreFor1 = currentEstimatedScoreForPlayer_v4(1,game_stage,goalSet);
 	double scoreFor2 = currentEstimatedScoreForPlayer_v4(2,game_stage,goalSet);
 	double scoreFor3 = (nplay>=4) ? currentEstimatedScoreForPlayer_v4(3,game_stage,goalSet) : 0;
 	double scoreFor4 = (nplay>=5) ? currentEstimatedScoreForPlayer_v4(4,game_stage,goalSet) : 0;
 	double val = 0;
 	double max = 0;
 	switch(who)
 	{
 	case 0:	
 		val = scoreFor0;
 		max = Math.max(scoreFor1,scoreFor2);
    		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 1:
 		val = scoreFor1;
 		max = Math.max(scoreFor0,scoreFor2);
 		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 2: 
 		val = scoreFor2;
 		max = Math.max(scoreFor0,scoreFor1);
    		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 3:
 		val = scoreFor3;
 		max = Math.max(Math.max(scoreFor0,scoreFor1),scoreFor2);
 		if(nplay>4) { max = Math.max(max,scoreFor4); }  		
 		break;
 	case 4:
 		val = scoreFor4;
 		max = Math.max(Math.max(scoreFor0,scoreFor1),Math.max(scoreFor3,scoreFor2));
 		break;
	default:
		break;
		
 	}
    
     if(print) { System.out.println("Eval for "+who+" is "+ (val-max)); }
     return(val-max);
 }
 public double estimatedStaticEvalPosition_v5(int who,double game_stage,boolean print,ContainerGoalSet goalSet)
 {
  	int nplay = players_in_game;
  	double scoreFor0 = currentEstimatedScoreForPlayer_v5(0,game_stage,goalSet);
 	double scoreFor1 = currentEstimatedScoreForPlayer_v5(1,game_stage,goalSet);
 	double scoreFor2 = currentEstimatedScoreForPlayer_v5(2,game_stage,goalSet);
 	double scoreFor3 = (nplay>=4) ? currentEstimatedScoreForPlayer_v5(3,game_stage,goalSet) : 0;
 	double scoreFor4 = (nplay>=5) ? currentEstimatedScoreForPlayer_v5(4,game_stage,goalSet) : 0;
 	double val = 0;
 	double max = 0;
 	switch(who)
 	{
 	case 0:	
 		val = scoreFor0;
 		max = Math.max(scoreFor1,scoreFor2);
    		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 1:
 		val = scoreFor1;
 		max = Math.max(scoreFor0,scoreFor2);
 		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 2: 
 		val = scoreFor2;
 		max = Math.max(scoreFor0,scoreFor1);
    		if(nplay>3) { max = Math.max(max,scoreFor3); }
 		if(nplay>4) { max = Math.max(max,scoreFor4); }
 		break;
 	case 3:
 		val = scoreFor3;
 		max = Math.max(Math.max(scoreFor0,scoreFor1),scoreFor2);
 		if(nplay>4) { max = Math.max(max,scoreFor4); }  		
 		break;
 	case 4:
 		val = scoreFor4;
 		max = Math.max(Math.max(scoreFor0,scoreFor1),Math.max(scoreFor3,scoreFor2));
 		break;
	default:
		break;
		
 	}
    
     if(print) { System.out.println("Eval for "+who+" is "+ (val-max)); }
     return(val-max);
 } 

 
 //
 // estimate the score after a successful bid. This also works
 // for the auctioneer's score after buying his own goods.
 //
 double scoreAfterSuccessfulBid_v4(int auctioneer,int forplayer,int bidamount,double game_stage,ContainerGoalSet goalSet)
 {	//int hash = Digest();
 	//ContainerBoard copy = cloneBoard();
	playerBoard tobd = playerBoard[forplayer];
	playerBoard frombd = playerBoard[auctioneer];
	int tocash = tobd.cash;
	int fromcash = frombd.cash;
	
 	long code = testBid(auctioneer,forplayer,bidamount);
 	double val = estimatedStaticEvalPosition_v4(forplayer,game_stage,false,goalSet);
 	undoTestBid(code);
 	
 	G.Assert(frombd.cash==fromcash,"from cash matches");
 	G.Assert(tobd.cash==tocash,"to cash matches");
 	//int hash2 = Digest();
 	//sameboard(copy);
 	//G.Assert(hash2==hash,"no change in board");
 	return(val);
 }
 double scoreAfterSuccessfulBid_v5(int auctioneer,int forplayer,int bidamount,double game_stage,ContainerGoalSet goalSet)
 {	//int hash = Digest();
 	//ContainerBoard copy = cloneBoard();
	playerBoard tobd = playerBoard[forplayer];
	playerBoard frombd = playerBoard[auctioneer];
	int tocash = tobd.cash;
	int fromcash = frombd.cash;
	
 	long code = testBid(auctioneer,forplayer,bidamount);
 	double val = estimatedStaticEvalPosition_v5(forplayer,game_stage,false,goalSet);
 	undoTestBid(code);
 	
 	G.Assert(frombd.cash==fromcash,"from cash matches");
 	G.Assert(tobd.cash==tocash,"to cash matches");
 	//int hash2 = Digest();
 	//sameboard(copy);
 	//G.Assert(hash2==hash,"no change in board");
 	return(val);
 }
 // 
 // find a faid bid for the current lot of goods.  A fair bid will
 // be as low as possible, but enough to beat the other players,
 // and not so much that we lose ground.  This is the maximum we
 // can afford to pay.
 //
 int fairBid_v4(int auctioneer,int buyer,int max,boolean print,ContainerGoalSet goalSet,double game_stage)
 {	
	// this is our baseline if we don't bid
	double baseline = estimatedStaticEvalPosition_v4(buyer,game_stage,false,goalSet);
 	int fairBid = -1;
 	for(int i=0;i<=max;i++)
 	{	// for each player including ourselves, estimate the score after a successful bid.
 		// the maximum bid that makes sense is one that improves our position the least.
 		// anyting greater is overpayment.
 		double val = scoreAfterSuccessfulBid_v4(auctioneer,buyer,i,game_stage,goalSet);
 		 if(print) { System.out.println("Bid "+i+" Val "+val); }
 		if(val>baseline) { fairBid = i; } else { break; }
 	}
 	boolean losingBid = (fairBid<0);
 	while(losingBid)
 	{ 
 		 double val = scoreAfterSuccessfulBid_v4(auctioneer,buyer,fairBid,game_stage,goalSet);
		 if(print) { System.out.println("LBid "+fairBid+" Val "+val); }
		 if(val>baseline) { losingBid=false; } else { fairBid--; }
 	}
 	if( print)
 	{	System.out.println("Player "+buyer+" Base "+baseline+" fair "+fairBid +" "+goalSet);
 	}
 	
 	return(fairBid);
 } 
 int fairBid_v5(int auctioneer,int buyer,int max,boolean print,ContainerGoalSet goalSet,double game_stage)
 {	
	// this is our baseline if we don't bid
	double baseline = estimatedStaticEvalPosition_v5(buyer,game_stage,false,goalSet);
 	int fairBid = -1;
 	for(int i=0;i<=max;i++)
 	{	// for each player including ourselves, estimate the score after a successful bid.
 		// the maximum bid that makes sense is one that improves our position the least.
 		// anyting greater is overpayment.
 		double val = scoreAfterSuccessfulBid_v5(auctioneer,buyer,i,game_stage,goalSet);
 		 if(print) { System.out.println("Bid "+i+" Val "+val); }
 		if(val>baseline) { fairBid = i; } else { break; }
 	}
 	boolean losingBid = (fairBid<0);
 	while(losingBid)
 	{ 
 		 double val = scoreAfterSuccessfulBid_v5(auctioneer,buyer,fairBid,game_stage,goalSet);
		 if(print) { System.out.println("LBid "+fairBid+" Val "+val); }
		 if(val>baseline) { losingBid=false; } else { fairBid--; }
 	}
 	if( print)
 	{	System.out.println("Player "+buyer+" Base "+baseline+" fair "+fairBid +" "+goalSet);
 	}
 	
 	return(fairBid);
 }
@Override
public int cellToX(ContainerCell c) {
	throw G.Error("Not implemented");
}
@Override
public int cellToY(ContainerCell c) {
	throw G.Error("Not implemented");
}


}