package viticulture;

import static viticulture.Viticulturemovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * This class doesn't do any graphics or know about anything graphical, 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

/*
 * 
 * 
Interesting special cases, choices and resolutions.  A lot of odd things arise from
the combination of "Mentor", a blue card which lets you take an action from a previous
season, and using that action to play a yellow card. Yellow cards generally assume the
action will be taken in the spring.

 if the "mentor" is used to play the "organizer" (choose a new startup position)
 and row 6 is claimed, it may already have been used by another player to take the
 first player position for next year.  There's no way to resolve this as the player
 is already in position, so we don't allow mentor organizer to choose that position
 unless the grape is present.
 
 Almost any card could be played twice, if the source deck is empty and the deck
 is reshuffled, the same card can be drawn again and played.
 
 if the "innkeeper" (pick 2 discards) is played when no discards are available, it
 can't do anything.  The wording doesn't allow picking nothing.  Likewise, if it
 is played when only one discard is available and no second card?  We allow it.
 
 if "planner" is used to place a worker in the future, it can be retrieved before
 it is ever used by various cards that allow retrieving and reusing workers. I 
 suppose it ought to be legal - things may have changed since the worker was
 placed.
 
 if "organizer" is played twice in the summer, the expected meeple in row 7 may
 not be there.  Also, in a 6p game, if the organizer is played in winter by
 the mentor/orginizer combo, the start marker may be gone and therefore no
 new row is available at all.
 
 if "mentor" is used to place a worker on "train worker", by the time the worker
 is activated, there may not be enough money, or the worker may have already been
 created by some "all workers may train" activity.
 
 in a 6p game, it's definitely possible to completely exhaust the supply of green
 cards (between cards planted and held in player's hands).  Likewise it's unlikely
 but possible to run out of the other card types.
 
 its' possible to gain workers from "guest speaker" and other cards after passing
 out of the year.  They are available when the next year starts.
 
 the mercado structure allows wine orders to be filled immediately.  If the user
 selected the "sell wine" action with the star bonus, and placed a star on the
 wine order bonus, and chooses to fill the wine order immediately, this can leave
 the original action an orphan, with nothing to sell.
 
 soldato might leave a bonus $1 space available, but you can't use the $1
 to pay the soldato.
  
 */
class ViticultureBoard extends RBoard<ViticultureCell> implements BoardProtocol,ViticultureConstants
{	static int REVISION = 158;			// 100 represents the initial version of the game
										// games with no revision information will be 100
										// revision 101, correct the sale price of champagne to 4
										// revision 102, fix the cash distribution for the cafe
										// revision 103, fixes card removal from the automa game, previously they
										// revision 104, merchant pays instead of gains for choice a
										// 	 were removed from all games.  Also fixed the wine critic unique payoff
										// revision 105 fixes politio bug, didn't get to buy a second star on sell wines	
										// and the threshold values using hasWineWithValue
										// revision 106 changes the reshuffle key
										// revision 107 keeps the picked objects in the original order when unpicked
										// revision 108 prevents the professore from retrieving grande workers
										// revision 109 fixes "can build" logic to include structures
										// revision 110 fix "lost continuation" when playing governor as first or two blue cards
										// revision 111 fixes "messenger without continuation" allowed the messenger player to continue playing
										// revision 112 fixes ownership of the gray meeple (should reset between years)
										// revision 113 fixes lost harvest when too many or too few fields were designated to harvest
										// revision 114 producer can retrieve from private structures and yoke
										//              implement uprooting at the yoke, and fix sharecropper and horticulturist discard logic 
										//				implement using fallow fields as homes for structure cards
										// revision 115 fix error when chef bumps a messenger that has not had its action yet.
										//				bug was that the messenger got the action anyway!  Also applicable when
										// 				planner was used to retrieve a messenger.
										// revision 116 allow yoke in any season
										// 				fix oracle at flip cards+structure and sell wine + card bonuses
										// revision 117 fix pay wedding party with only 1 payment
										// revision 118 politico got an extra blue card if played on the play2blue spot
										// revision 119 fixes the other half of the same politico bug, gives you the second card if you don't pay.
										// revision 120 changes randomkey for innkeeper stealing to be more local
										// revision 121 changes the innkeeper to select a particular card color
										// revision 122 fixes the politco (again!) to not offer selling a wine
										// revision 123 fixes early exit from "move 2 stars" after moving only 1 star,
										// revision 124 fix double vp for planter
										// revision 125 fix landscaper "switch" to respect field limits
										// revision 126 transmits charmat wine selection
										//				also pre-checks governor choice
										// revision 127 fixes mentor to offer green OR yellow
										//				also adds "under harvest" logic to the UI
										// 				and changes the digest of "pendingmoves" so edits in govenor victim work correctly
										// revision 128 changes turn order initialization to be random
										// revision 129 only trigger merchant bonus if you're playing on the board
										// revision 130 only pay for fountain if its not your meeple
										// revision 131 give up when "upgradecellar" is used. 
										//              Also make politico trade last and plant last
										// 				Also skip "retrieve grande" option when it is impossible
										// revision 132 fixes the marketer to give $1
										//				fixes manager to not pay soldato
										// revision 133 fixes politico/trade to notice newly acquired funds
										// revision 134 skip "build worker" from guest speaker when not possible
										//				also for uncertified teacher
										// revision 135 misplaced messenger, skip the turn
										// revision 136 allow laborer and jack of all trades to select order of benefits
										// revision 137 allows laborer to choose make wine if harvesting would give him grapes
										// revision 138 fixes mentor to skip player when no winemaking is possible
										// revision 139, don't steal a dollar for soldato when taking a messenger action, adds a stable sort for worker position
										// revision 140 fixes oracle for multiple card draws
										// revision 141 makes it illegal to use done when tavern has selected 1 grape
										// revision 142 fixes a problem where a messenger was declared useless if a soldato was placed after and the player has no money
										// revision 143 re-repairs marketer to only give the dollar
										// revision 144 fix studio giving vp for tour 
										// revision 145 fixes handyman blindness to buildable structure cards
										// revision 146 allows "mafioso twice" for structure cards with action spaces
										// revision 147 consolidates some mafioso actions
										// revision 148 fixes an interaction of inkeeper and soldato
										// revision 149 fixes an interaction of mafioso and soldato
										//   and fixes soldato cost of max 3 if soldatos are on the overflow space
										// revision 150 fixed scholar "both" option with oracle drawing cards
										// revision 151 makes the "sell wines" overlay behave as radio buttons
										// revision 152 tightens up the conditions for showing the "fill wine order" choice
										// revision 153 fixes an omission, considering the workshop discount for craftsman
										// 		also fixes "producer" to only forbid retrieving itself
										// revision 154 adds "fill" to jack of all trades if makewine is a choice
										// revision 155 makes politico not offer to harvest 3 if there are less

//****** this is the point where extensions were added ************

										// revision 156 fixes farmer bug for green card markets and also fixes
										// the "planner" bug, planner actions trigger when you enter the season
										// revision 157 fixes "professor" bug with unlimited workers option
										// revision 158 fixes the interaction between oracle and green card market
public int getMaxRevisionLevel() { return(REVISION); }
	PlayerBoard pbs[] = null;		// player boards
	
	// extended options
	public Bitset<Option>options = new Bitset<Option>();
	
	public void setOption(Option val) { options.set(val); }
	public boolean testOption(Option val) { return(options.test(val)); }
	public void clearOption(Option val) { options.clear(val); }
	
	boolean optionsResolved = false;
	static int MarketSize = 2;
	int reshuffleAt = 0;
	// affected cards for limitPoints:
	// handyman yellow #18
	// swindler yellow #31
	// volunteer crew yellow #37
	// uncertified teacher blue #7
	// motivator blue #25
	// guest speaker blue #38
	//
	public PlayerBoard getPlayerBoard(int n) 
	{ 	PlayerBoard p[] =pbs;
		if(p!=null && n<p.length) { return(p[n]);}
		return(null);
	}
	public PlayerBoard getCurrentPlayerBoard() { return(pbs[whoseTurn]); }
	public Random random = null;
	public ViticultureCell uiCells=null;
	private IntObjHashtable<ViticultureCell>gettableCells = new IntObjHashtable<ViticultureCell>();
	public ViticulturePlay robot = null;
	boolean automa = false;
	ViticultureColor automaColor = null;
	int automaWorkers = 0;
	int automaScore = WINNING_SCORE;
	public boolean turnChangeSamePlayer = false;
	
	public boolean p1(String msg,Object... args)
	{	String m1 = (args!=null && args.length>0) ? G.format(msg,args) : msg;
		if(G.p1(m1) && (robot!=null))
		{	String dir = "g:/share/projects/boardspace-html/htdocs/viticulture/viticulturegames/robot/";
			
			robot.saveCurrentVariation(dir+m1+".sgf");
			return(true);
		}
		return(false);
	}
	public boolean Assert(boolean cond,String msg,Object... args)
	{
		if(!cond)
		{
			p1(msg);
			throw G.Error(msg,args);
		}
		return true;
	}
	/**
	 * overall control flow, when the player hits "Done" one of four things can happen.
	 * 1) the resolution for "done" can specify the next state, for continued actions
	 *    by the same player.  The resolution chain usually returns "nextState" or null
	 * 2) some cards require a special poll of all players, in this case "targetPlayer"
	 *    is the controlling player, while whoseTurn cycles through all the players.
	 * 3) in complex cases, additional actions will be queued using the "continuation"
	 *    mechanism.  The continuation can do anything.
	 * 4) if none of the above, then the next player is selected and starts in "Play" state.
	 *    this might also involve changing season, year, or ending the game.
	 * @author Ddyer
	 *
	 */
	enum Continuation implements Digestable {
		NextPlayer,
		MerchantDraw,
		SelectWakeup,
		Place1Star,
		NextSeason,
		SellWine,
		PlaySecondBlue,
		DiscardWineFor2VP,
		DiscardGrapeFor3VP,
		TradeSecond,
		TakeCard,
		TakeMoreYellow,
		Plant2VinesOnly,
		PlaySecondYellow,
		PlaySecondTrade,
		TrainGuestWorker,
		TakeYellowOrBlue,
		Trade1,
		Fill1Optional,
		Fill1Mandatory,
		Plant1VineOptional,
		Make2WinesOptional,
		Make1WineOptional,
		Harvest1Optional,
		Age1Twice,
		Age2Once,
		FillMercado,
		FinishNewSeason,
		PerformDropActionAfter,
		PoliticoVp,
		PoliticoGreen,
		PoliticoPurple,
		PoliticoYellow,
		PoliticoBlue,
		PoliticoStar,
		PoliticoPlant,
		PoliticoTrade,
		PoliticoStructure,
		MafiosoTwice,
		RestartSeason,			// after taking planner action
		SellWineOptional,
		FlipOptional,
		Flip,
		NextPlayerInMentorCycle,
		PoliticoTradeExtra,
		Harvest2Optional,
		TrainScholarWorker,
		ExecutePlanner,
		// add new continuations to the end to avoid changing the digest, which breaks shuffles
		// likewise for adding new states and new cards
		
		;		
		public long Digest(Random r) {
			return(r.nextLong()*(ordinal()+1));
		}
	}
	
	public void doContinuation(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
	{
		Continuation next = (continuation.size()==0) ? Continuation.NextPlayer : continuation.pop();
		switch(next)
		{
		case TrainScholarWorker:
			{
			// train a worker after the scholar draws cards, doing both
			// the vp has already been deducted.
			ViticultureState nextState = doTrainWorker(pb,1,false,false,replay);  
			if(nextState!=null) { setState(resetState = nextState); }
			}
			break;
		case Flip:
			setState(resetState = ViticultureState.Flip);
			break;
		case FlipOptional:
			setState(resetState = ViticultureState.FlipOptional);
			break;
		case MerchantDraw:
			triggerCard = currentWorker;
			setState(resetState = ViticultureState.TakeCard);
			break;
		case SelectWakeup:
       		{ ViticultureState nexts =  selectWakeup(pb,replay);    		
       		  if(nexts!=null) { setState(resetState = nexts); }
       		  else { doContinuation(pb,replay,m); }
       		}
       		break;
		case MafiosoTwice:
			{
			// no cost for soldato on the second time
			if(revision>=147 
					? !canPlaceWorker(pb,currentWorker,0,currentAction,MoveGenerator.All)
					: !canPlaceWorker(pb,currentWorker,currentAction,MoveGenerator.All)
				) 
				{ //p1("Mafioso can't repeat "+currentAction.rackLocation());
				  doContinuation(pb,replay,m); 
				  break;
				}
			//p1("Mafioso repeat "+currentAction.rackLocation());
			currentWorker = null;
			}
			//$FALL-THROUGH$
		case PerformDropActionAfter:
			{
			// after the innkeeper action
			ViticultureState nextState = performDropActionAfter(pb,currentAction,currentWorker,currentWorker==null,currentReplay,currentMove);
			//p1("innkeeper continuation");
			if(nextState!=null)
				{
				setState(resetState = nextState);
				}
			else { doContinuation(pb,replay,m); }
			}
			break;
		case PoliticoStructure:
			if(pb.cash>0)
			{
			setState(resetState=ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoStructure);
			}
			else
			 {
				 doContinuation(pb,replay,m); 
			 }
			//p1("Politico structure card from flip");
			break;
		case PoliticoVp:
			if((revision<133) || (pb.cash>0))
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoVP);
			}
			else
			 {
				 doContinuation(pb,replay,m); 
			 }
			//p1("politico extra vp");
			break;
		case PoliticoGreen:	// draw extra green
			if(pb.cash>0)
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoGreen);
			}
			else
			 {
				 doContinuation(pb,replay,m); 
			 }
			//p1("politico green");
			break;

		case PoliticoPurple:	// draw extra purple
			if(pb.cash>0)
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoPurple);
			}
			else
			 {
				 doContinuation(pb,replay,m); 
			 }
			//p1("politico purple");
			break;
		case PoliticoStar:
			if(revision>=123)
			{
			 if(pb.cash>0)
			 {	setState(resetState = ViticultureState.ResolveCard);
				resolveCard(ViticultureChip.PoliticoStarExtra);
			 }
			 else
			 {
				 doContinuation(pb,replay,m); 
			 }
			}
			else
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoStar);
			}
			break;
		case PoliticoPlant:
			if(revision>=131)
			{	if(pb.cash>0 && canPlant(pb))
				{
				setState(resetState = ViticultureState.ResolveCard);
				resolveCard(ViticultureChip.PoliticoPlantExtra);
				}
				else { 
					doContinuation(pb,replay,m);
				}
			}
			else
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoPlant );			
			}
			break;
		case PoliticoTradeExtra:
			if(pb.cash>0)
			{
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoTradeExtra);
			}
			else {
				doContinuation(pb,replay,m);
			}
			break;
			
		case PoliticoTrade: // obsolete, new games use PoliticoTradeExtra
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoTrade);			
			break;
		case PoliticoYellow:	// play extra yellow card
			// he might have spent all his money and/or all his cards
			{
			boolean set = false;
			if(pb.nCards(ChipType.YellowCard)>0)
			{	
			if(revision>=122)
			{
				if(pb.cash>0)
				{	set = true;
					setState(resetState = ViticultureState.ResolveCard);
					resolveCard(ViticultureChip.PoliticoYellowExtra);
				}
			}
			else
			{
			set = true;
			setState(resetState = ViticultureState.ResolveCard);
			resolveCard(ViticultureChip.PoliticoYellow);
			}
			//p1("politico yellow");
			}
			if(!set) { doContinuation(pb,replay,m); }
			}
			break;
		case PoliticoBlue:	// play extra blue card			
			// he might have spent all his money and/or all his cards
			{
			boolean set = false;	
			
			if((pb.nCards(ChipType.BlueCard)>0))
			{
				if(revision>=122)
				{
					if(pb.cash>0)
					{	set = true;
						setState(resetState = ViticultureState.ResolveCard);
						resolveCard(ViticultureChip.PoliticoBlueExtra);
					}
				}
				else
				{	set = true;
					setState(resetState = ViticultureState.ResolveCard);
					resolveCard(ViticultureChip.PoliticoBlue);
				}
			//p1("politico blue");
			}
			if(!set) { doContinuation(pb,replay,m); }
			}
			break;
		case Place1Star:
			{
			setState(resetState=pb.placeStarState());
			}
			break;
		case FinishNewSeason:
			{
			ViticultureState nextState = finishNewSeason(pb,replay,m);
			if(nextState!=null) { setState(resetState = nextState); }
			else { doContinuation(pb,replay,m); }
			}
			break;
		case FillMercado:	// fill a special wine order
			triggerCard = ViticultureChip.MercadoCard;
			setState(resetState = ViticultureState.FillMercado);
			break;
		case Harvest1Optional:
			setState(resetState = ViticultureState.Harvest1Optional);	
			break;
			
		case Harvest2Optional:
			setState(resetState = ViticultureState.Harvest2Optional);	
			break;

		case Make1WineOptional:
			//p1("Make wine after harvest fermentation tank");	// from fermentation tank
			triggerCard = ViticultureChip.FermentationTank;
			//p1("use fermentation tank");
			setState(resetState = ViticultureState.Make1WineOptional);			
			break;
		case Make2WinesOptional:
			if(triggerCard==null)
			{	//p1("use wine press");
				triggerCard = ViticultureChip.WinePressCard;
			}
			//p1("Make wine after star");	// from wine press
			setState(resetState = ViticultureState.Make2WinesOptional);			
			break;
		case Fill1Optional:
			if(triggerCard==null)
			{
				triggerCard = ViticultureChip.ShopCard;
				//p1("use shop");
			}
			setState(resetState = ViticultureState.FillWineOptional);
			break;
		case Fill1Mandatory:
			setState(resetState = ViticultureState.FillWineBonus);
			break;
		case Plant1VineOptional:
			setState(resetState = ViticultureState.Plant1VineOptional);
			break;
		case Age1Twice:
			triggerCard = ViticultureChip.CaskCard;
			//p1("use cask");
			setState(resetState = ViticultureState.Age1Twice);
			break;
		case Age2Once:
			triggerCard = ViticultureChip.WineCaveCard;
			setState(resetState = ViticultureState.Age2Once);
			break;
		case Trade1: // trade action after placing star, for trading post card
			//p1("Trade after star");	// from trading post structure
			triggerCard = ViticultureChip.TradingPostCard;
			//p1("use trading post");
			setState(resetState = ViticultureState.Trade1);
			break;
		case RestartSeason:
			{
			// this is used after taking a planner action
			p1("restart season after planner");
			pb = findFirstPlayerInSeason();
			whoseTurn = pb.boardIndex;
	        seasonRow = pbs[whoseTurn].wakeupPosition.row;
	        moveNumber++; //the move is complete in these states
	        setNextPlayState(replay);
			}
			break;
		case NextPlayer:			// default, advance to the next player 
			setNextPlayer(replay);
    		setNextStateAfterDone(replay); 
			break;
		case TakeYellowOrBlue:
			setState(resetState = ViticultureState.TakeYellowOrBlue);
			break;
		case TakeMoreYellow:	// governor picks 3
			if(pendingMoves.size()>0)
				{
				//p1("take more yellow for governor");
				Viticulturemovespec m2 = (Viticulturemovespec)pendingMoves.pop();
				PlayerBoard toPlayer = pbs[m2.to_col-'A'];
				addContinuation(Continuation.TakeMoreYellow);
				whoseTurn = toPlayer.boardIndex;
				resolveCard(ViticultureChip.GovernersChoice);
				setState(resetState = ViticultureState.ResolveCard);
				if(revision>=126)
					{// pre-check the governors choice, it's mandatory
					setState(ViticultureState.Confirm);
					choiceA.selected = true; 
					choiceB.selected = true;
					}
				}
			else
				{ if(revision>=110)		// second blue card went to the current player - oops
					{pb = pbs[targetPlayer];
				     whoseTurn = targetPlayer;
					}
				  targetPlayer = -1;
				  //G.print("Finish TakeMoreYellow");
				  setState(ViticultureState.Confirm);
				  doContinuation(pb,replay,m); 
				}
			break;
		case TrainGuestWorker:
			{
			//p1("train guest cycle");
			resolveCard(ViticultureChip.TrainWorker);
			ViticultureState nextS = null;
			do {
			setState(resetState = ViticultureState.ResolveCard);
			//if(nextS!=null)  { p1("next train");}
			nextS = setNextPlayerInCycle();
			pb = pbs[whoseTurn];
			}
			while (revision>=134 && ((pb.cash<1) || (pb.nWorkers==maxWorkers())) && (nextS!=null));
			
			if(nextS==null) { setState(ViticultureState.Confirm); doContinuation(pb,replay,m); }
			else { setState(resetState=nextS); }
			}
			break;
			
		case DiscardGrapeFor3VP:
			
			if(pb.nGrapes()>=2) 
				{// p1("use tavern");
				  triggerCard = ViticultureChip.Tavern;
				  setState(resetState = ViticultureState.Discard2GrapesFor3VP); 
				}
			else { doContinuation(pb,replay,m); }
			break;
			
		case DiscardWineFor2VP: // tap room
			//p1("use taproom year "+year+" season "+season);
			if(pb.hasWine())
				{ 
				//p1("use taproom discard year ");
				triggerCard = ViticultureChip.TapRoom;
				setState(resetState = ViticultureState.DiscardWineFor2VP); 
				}
			else { doContinuation(pb,replay,m); }
			break;
		case SellWineOptional:
			setState(resetState = ViticultureState.Sell1WineOptional);
			break;
		case SellWine:
			setState(resetState = ViticultureState.Sell1Wine);
			break;
		case Plant2VinesOnly:	// from the cultivator card
			setState(resetState = ViticultureState.Plant2VinesOptional);
			break;
		case TakeCard:
			setState(resetState = ViticultureState.TakeCard);
			break;
		case NextSeason:
			ViticultureState nextState = passToNextSeason(pb,replay,m);
			if(nextState!=null) { setState(resetState = nextState); }
			else { doContinuation(pb,replay,m); }
			break;
		case TradeSecond:
			setState(resetState = ViticultureState.TradeSecond);
			break;
		case PlaySecondYellow:
			// if he has one,
			triggerCard = null;
			if(pb.hasCard(ChipType.YellowCard))
					{
					setState(resetState = ViticultureState.PlaySecondYellow);
					}
				else { doContinuation(pb,replay,m); }
			break;
		case PlaySecondBlue:
			// if he has one,
			triggerCard = null;
			if(pb.hasCard(ChipType.BlueCard))
				{
				setState(resetState = ViticultureState.PlaySecondBlue);
				}
			else { doContinuation(pb,replay,m); }
			break;
		case NextPlayerInMentorCycle:
			// only for mentor !
			Assert(suspendedWhoseTurn>=0,"suspended");
			whoseTurn = suspendedWhoseTurn;
			suspendedWhoseTurn = -1;
			pb = pbs[whoseTurn];
			if(targetPlayer>=0) { setState(resetState = ViticultureState.Make2Draw2); }
			else { doContinuation(pb,replay,m);  }
			break;
		case ExecutePlanner:
			{
			ViticultureState nexts = takePlannerAction(pbs[whoseTurn],replay);
			if(nexts!=null) { setState(resetState=nexts); }
			}
			break;
		default: G.Error("Continuation %s not handled",next);
		}
	}
	class ContinuationStack extends OStack<Continuation> implements Digestable
	{
		public Continuation[] newComponentArray(int sz) { return(new Continuation[sz]); }

		public long Digest(Random r) 
		{
			long v=0;
			for(int lim=size()-1; lim>=0; lim--) { v^=elementAt(lim).Digest(r);		}
			return(v);
		}
	}
	
	public ContinuationStack continuation = new ContinuationStack();
	
	public void addContinuation(Continuation c)
	{
		continuation.push(c);
	}

	
	// card stacks
   	ViticultureCell greenCards = newcell(ViticultureId.GreenCards,ChipType.GreenCard,VineCardStrings);
   	ViticultureCell greenDiscards = newcell(ViticultureId.GreenDiscards,ChipType.GreenCard,null);
   	ViticultureCell yellowCards = newcell(ViticultureId.YellowCards,ChipType.YellowCard,SummerVisitorString);
   	ViticultureCell yellowDiscards = newcell(ViticultureId.YellowDiscards,ChipType.YellowCard,null);
   	ViticultureCell purpleCards = newcell(ViticultureId.PurpleCards,ChipType.PurpleCard,WineOrderString);
   	ViticultureCell purpleDiscards = newcell(ViticultureId.PurpleDiscards,ChipType.PurpleCard,null);
   	ViticultureCell blueCards = newcell(ViticultureId.BlueCards,ChipType.BlueCard,WinterVisitorString);
   	ViticultureCell blueDiscards = newcell(ViticultureId.BlueDiscards,ChipType.BlueCard,null);
   	ViticultureCell structureCards = newcell(ViticultureId.StructureCards,ChipType.StructureCard,StructureCardString);
   	ViticultureCell structureDiscards = newcell(ViticultureId.StructureDiscards,ChipType.StructureCard,null);
   	ViticultureCell automaCards = newcell(ViticultureId.AutomaCards,ChipType.AutomaCard,null);
   	ViticultureCell automaDiscards = newcell(ViticultureId.AutomataDiscards,ChipType.AutomaCard,null);
   	
   	// these don't include the automata cards
   	ViticultureCell cardStacks[] = {
   			greenCards,yellowCards,purpleCards,blueCards,structureCards
   	};
   	// these don't include the automata cards
  	ViticultureCell discardStacks[] = {
   			greenDiscards,yellowDiscards,purpleDiscards,blueDiscards,structureDiscards,
   	};
   	// these don't include the automata cards
  	ViticultureCell cardsAndDiscards[] = {
   			greenDiscards,yellowDiscards,purpleDiscards,blueDiscards,structureDiscards,
   			greenCards,yellowCards,purpleCards,blueCards,structureCards
   	};
  	ViticultureChip triggerCard = null;
   	ViticultureChip cardBeingResolved = null;
   	ViticultureId cardResolution = null;
   	ViticultureId tradeFrom = null;
   	ViticultureId tradeTo = null;
   	ViticultureCell starPlacement = null;	// placed from the player reserve, gets bonus
   	ViticultureCell starDropped = null;		// moved from another place, no bonus
   	ViticultureCell starDropped2 = null;	// when moving 2, the second space dropped. This is for the banquet hall 
   	Viticulturemovespec buildingSelection = null;	// building being built
   	ViticultureCell selectedCard = null;	// card to draw
   	ViticultureCell selectedWakeup = null;	// selected wakup from the organizer card
   	ViticultureChip currentWorker = null;
   	ViticultureCell currentAction = null;
   	Viticulturemovespec currentMove = null;
   	replayMode currentReplay = null;
   	
   	int stateChange = 0;
    CommonMoveStack pendingMoves = new CommonMoveStack();
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    boolean robotBoard = false;
    public int finalYear = 999;
    public void setRobotBoard(boolean v) { robotBoard = v; }
    
	void logGameEvent(String str,String... args)
	{	//if(!robotBoard)
		{String trans = s.get(str,args);	// substitute, but no lookup in the dictionary
		 gameEvents.push(trans);
		}
	}
	void logRawGameEvent(String str)
	{	//if(!robotBoard)
		gameEvents.push(str);
	}

	void logRawGameEvent(String str,String... args)
	{	//if(!robotBoard)
		{String trans = s.subst(str,args);	// substitute, but no lookup in the dictionary
		 gameEvents.push(trans);
		}
	}

   	ViticultureCell[]a4(ViticultureId id,int sea,String tip,int off)
   	{
   		ViticultureCell c[]=new ViticultureCell[4];
   		for(int i=0;i<4;i++)
   		{
   			ViticultureCell cell=newcell(id,'@',i);
   			c[i] = cell;
   			cell.contentType = ChipType.Worker;
   			cell.parentRow = c;
   			cell.season = sea;
   			cell.toolTip = tip;
   			cell.tipOffset = off;
   			
   		}
   		return(c);
   	}
   	private int workerCellSeason(ViticultureCell c) 
   	{ 	return(c.season);
   	}
   	
   	// worker spaces
  	ViticultureCell dollarWorker = newcell(ViticultureId.DollarOrCardWorker,ChipType.Worker,GainDollarOrStructure);

  	ViticultureCell drawGreenWorkers[] = a4(ViticultureId.DrawGreenWorker,0,DrawGreenCard,1);
   	ViticultureCell giveTourWorkers[] = a4(ViticultureId.GiveTourWorker,0,GiveTour,0);
   	ViticultureCell buildStructureWorkers[] = a4(ViticultureId.BuildStructureWorker,0,BuildStructureMode,1);
   	ViticultureCell starPlacementWorkers[] = a4(ViticultureId.StarPlacementWorker,0,PlaceStar,0);
   	
   	ViticultureCell playYellowWorkers[] = a4(ViticultureId.PlayYellowWorker,1,PlayYellow,0);
   	ViticultureCell plantWorkers[] = a4(ViticultureId.PlantWorker,1,PlantVines,1);
   	ViticultureCell tradeWorkers[] = a4(ViticultureId.TradeWorker,1,TradeSomething,0);
   	ViticultureCell flipWorkers[] = a4(ViticultureId.FlipWorker,1,FlipProperty,1);
   	
   	ViticultureCell drawPurpleWorkers[] = a4(ViticultureId.DrawPurpleWorker,2,DrawPurple,0);
   	ViticultureCell harvestWorkers[] = a4(ViticultureId.HarvestWorker,2,HarvestFields,1);
   	ViticultureCell makeWineWorkers[] = a4(ViticultureId.MakeWineWorker,2,MakeWines,0);
   	ViticultureCell buildTourWorkers[] = a4(ViticultureId.BuildTourWorker,2,BuildOrTour,1);
 
   	ViticultureCell playBlueWorkers[] = a4(ViticultureId.PlayBlueWorker,3,PlayBlue,0);
   	ViticultureCell recruitWorkers[] = a4(ViticultureId.RecruitWorker,3,TrainNew,1);
   	ViticultureCell sellWineWorkers[] = a4(ViticultureId.SellWineWorker,3,SellWines,0);
   	ViticultureCell fillWineWorkers[] = a4(ViticultureId.FillWineWorker,3,FillWine,1);
 
   	ViticultureCell mainBoardWorkerPlacements[][] =
   		{	// 4 rows of 4
   				drawGreenWorkers,giveTourWorkers,buildStructureWorkers,starPlacementWorkers,
   				playYellowWorkers,plantWorkers,tradeWorkers,flipWorkers,
   				drawPurpleWorkers,harvestWorkers,makeWineWorkers,buildTourWorkers,
   				playBlueWorkers,recruitWorkers,sellWineWorkers,fillWineWorkers,
   		};
     	
   	
   	CellStack plannerCells = new CellStack();	// filled by yellow "planner" card, in rare circumstances there may be more than 1
   	ChipStack plannerMeeples = new ChipStack();
   	CommonMoveStack plannerMoves = new CommonMoveStack();
   	
   	ViticultureCell roosterTrack[][] = new ViticultureCell[7][4];
   	ViticultureCell starTrack[] = new ViticultureCell[7];
   	
   	ViticultureCell scoringTrack[] = new ViticultureCell[MAX_SCORE-MIN_SCORE+1];	// only used by the gui
   	ViticultureCell residualTrack[] = new ViticultureCell[6];						// only used by the gui
   	
   	ViticultureCell mamaCards = newcell(ViticultureId.MamaCards,ChipType.MamaCard,MamaCardStrings);
   	ViticultureCell papaCards = newcell(ViticultureId.PapaCards,ChipType.PapaCard,PapaCardStrings);
   	ViticultureCell workerCards = newcell(ViticultureId.SpecialWorkerDeck,ChipType.WorkerCard,WorkerCardString);
	
   	ViticultureCell specialWorkerCards = newcell(ViticultureId.SpecialWorkerCards,ChipType.WorkerCard,SpecialWorkerInfo);
	ViticultureChip flashChip = null;		// PART OF THE USER INTERFACE
 	
	ViticultureVariation variation = ViticultureVariation.viticulture;
	public int year = 1;
	public int startingYear = 1;
	public int startingScore = 0;
	public int season = 0;
	public int seasonRow = 0;
	public ViticultureId firstChoice = null;
   	
   	/*
   	 * various cards specify "all players may".  This marks the anchor player who started the cycle.
   	 * there ought to be a state associated with this state, and the state ought to persist until
   	 * the cycle is finished.  
   	 */
	int targetPlayer = -1;				// target / anchor player for a player poll card such as swindler or banker
	int suspendedWhoseTurn = -1;
    private ViticultureState setNextPlayerInCycle()
    {
		whoseTurn = (whoseTurn+1)%players_in_game;
		if(whoseTurn==targetPlayer)
		{	//G.print("Finish target"); 
			targetPlayer = -1;
			return(null);
		}
		else
		{	//G.print("Continue target");
			return(resetState);
		}
    }
    
	// used in the UI
	
	int wineDisplayCount = 0;
	int grapeDisplayCount = 0;
  	ViticultureCell wineDisplay[] = null;			// construction cell for wines
	ViticultureCell grapeDisplay[] = null;		// construction cells for grapes making wines
	ViticultureCell wineSelect[] = null;	// user interface for selling wines
	
	ViticultureCell cardDisplay = newUIcell(ViticultureId.CardDisplay,'@',0);
	ViticultureCell cardDisplay1 = newUIcell(ViticultureId.CardDisplay,'@',1);
	ViticultureCell unusedCardDisplay = newUIcell(ViticultureId.ShowBigChip,'@',2);
	
	ViticultureCell choice0 = newUIcell(ViticultureId.Choice_0,'@',0);
	ViticultureCell choice1 = newUIcell(ViticultureId.Choice_1,'@',0);
	ViticultureCell choiceA = newUIcell(ViticultureId.Choice_A,'@',0);
	ViticultureCell choiceB = newUIcell(ViticultureId.Choice_B,'@',0);
	ViticultureCell choiceC = newUIcell(ViticultureId.Choice_C,'@',0);
	ViticultureCell choiceD = newUIcell(ViticultureId.Choice_D,'@',0);
	
	ViticultureCell choiceAB =  newUIcell(ViticultureId.Choice_AandB,'@',0);
	ViticultureCell choiceAC =  newUIcell(ViticultureId.Choice_AandC,'@',0);
	ViticultureCell choiceBC =  newUIcell(ViticultureId.Choice_BandC,'@',0);
	
	ViticultureCell choice_HarvestFirst = newUIcell(ViticultureId.Choice_HarvestFirst,'@',0);
	ViticultureCell choice_MakeWineFirst = newUIcell(ViticultureId.Choice_MakeWineFirst,'@',0);
	ViticultureCell choice_FillWineFirst = newUIcell(ViticultureId.Choice_FillWineFirst,'@',0);
	
	// for display of resources
  	ViticultureCell yokeCash = newUIcell(ViticultureId.YokeCash,'@',0);
  	ViticultureCell yokeRedGrape = newUIcell(ViticultureId.RedGrape,'@',0);
  	ViticultureCell yokeRedWine = newUIcell(ViticultureId.RedWine,'@',0);
  	ViticultureCell yokeWhiteGrape = newUIcell(ViticultureId.WhiteGrape,'@',0);
  	ViticultureCell yokeWhiteWine = newUIcell(ViticultureId.WhiteWine,'@',0);
  	ViticultureCell yokeRoseWine = newUIcell(ViticultureId.RoseWine,'@',0);
  	ViticultureCell yokeChampaign = newUIcell(ViticultureId.Champaign,'@',0);
	
  	ViticultureCell tradeCards = newUIcell(ViticultureId.Cards,'@',1);
  	ViticultureCell tradeCoins = newUIcell(ViticultureId.Cash,'@',1);
  	ViticultureCell tradeVP = newUIcell(ViticultureId.VP,'@',1);
  	ViticultureCell tradeRedGrape = newUIcell(ViticultureId.RedGrape,'@',1);
  	ViticultureCell tradeWhiteGrape = newUIcell(ViticultureId.WhiteGrape,'@',1);

  	ViticultureCell boardMagnifier = newUIcell(ViticultureId.Magnifier,'@',0);
	ViticultureCell starMagnifier = newUIcell(ViticultureId.Magnifier,'@',1);
	ViticultureCell wakeupMagnifier = newUIcell(ViticultureId.Magnifier,'@',2);
	ViticultureCell magnifiers[] = { boardMagnifier, wakeupMagnifier, starMagnifier};
	

	private ViticultureState board_state = ViticultureState.Puzzle;	
	public ViticultureState resetState = ViticultureState.Puzzle;
	private ViticultureState unresign = null;	// remembers the orignal state when "resign" is hit
	public int robotDepth = 0;
	
	public ViticultureState getState() { return(board_state); }
	
	
	
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(ViticultureState st) 
	{ 	unresign = (st==ViticultureState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

 
	// this is required even though it is meaningless here
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ViticultureChip pickedObject = null;
    public ViticultureChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private IStack pickedSourceIndex = new IStack();
    private CellStack droppedDestStack = new CellStack();
    public ViticultureChip lastDroppedObject = null;	// for image adjustment logic
    private ViticultureCell lastDroppedWorker = null;
    private int lastDroppedWorkerIndex = 0;
    
    private StateStack stateStack = new StateStack();
	private ViticultureCell newcell(ViticultureId loc,ChipType type,String tip)
    {	ViticultureCell c = newcell(loc,'@',0);
    	c.contentType = type;
    	c.toolTip = tip;
    	return(c);
    }
	// factory method to generate a board cell
	private ViticultureCell newcell(ViticultureId loc,char charcode,int row)
	{	ViticultureCell c = new ViticultureCell(loc,charcode,row);
		register(c);
		c.next = allCells;
		allCells = c;
		return(c);
	}
	
	// new cell used by the UI, not part of the maintained board
	public ViticultureCell newUIcell(ViticultureId loc,char col,int ro)
	{	ViticultureCell v = new ViticultureCell(loc,col,ro);
		register(v);
		v.next = uiCells;
		uiCells = v;
		return(v);	
	}
	public void register(ViticultureCell v)
	{	int code = v.uiCode();
		//ViticultureCell oldv = gettableCells.get(code);
		//if(G.debug() && oldv!=null) { 
		//	G.Error("New cell with same id: old %s new %s",oldv,v);
		//}
		gettableCells.put(code, v);
	}
	void unselect()
	{	for(ViticultureCell c = allCells; c!=null; c=c.next) { c.selected = false; }
		unselectUI();
		for(PlayerBoard pb : pbs)
			{ pb.unselect();
			  if(revision>=111) { pb.unselectUI(); } 	// probably ok any time, but this is the revision we fixed it. 
			}
	}
	void unselectUI()
	{
		for(ViticultureCell c = uiCells; c!=null; c=c.next) { c.selected = false; };
	}
	// constructor 
    public ViticultureBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        setColorMap(map, players);
    	cardDisplay.contentType = ChipType.Card;
    	cardDisplay1.contentType = ChipType.Card;

        dollarWorker.parentRow = new ViticultureCell[] { dollarWorker };  

        
        for(int row=0;row<7;row++)
        {
        	for(int col=0;col<4;col++)
        	{
        		roosterTrack[row][col] = newcell(ViticultureId.RoosterTrack,(char)('A'+col),row);
        	}
        	starTrack[row] = newcell(ViticultureId.StarTrack,'@',row);
        }
        for(int i=0;i<scoringTrack.length;i++)
        {
        	scoringTrack[i] = newUIcell(ViticultureId.ScoringTrack,'@',i+MIN_SCORE);
        }
        for(int i=0;i<residualTrack.length;i++)
        {
        	residualTrack[i]= newcell(ViticultureId.ResidualTrack,'@',i);
        }
        wineDisplay = new ViticultureCell[4];	// max of make 4 wines (with politico)
        for(int i=0;i<wineDisplay.length;i++)
        {
        	wineDisplay[i] = newcell(ViticultureId.WineBin,'@',i);
        }
        wineSelect = new ViticultureCell[27];	// 4 types of wine and 2 types of grape
        for(int i=0;i<wineSelect.length;i++)	// wine cave lets you select 2 of any, so need 27!
        {
        	wineSelect[i] = newUIcell(ViticultureId.WineSelection,'@',i);
        }
        grapeDisplay = new ViticultureCell[19];	// theoretical maximim of 18 grapes, plus a blank between reds and whites
        for(int i=0;i<grapeDisplay.length;i++)
        {
        	grapeDisplay[i] = newcell(ViticultureId.StartPlayer,'@',i);
        }
        
		tradeCards.addFixedChip(ViticultureChip.GreyCard);
		tradeCards.addFixedChip(ViticultureChip.GreyCard);

		tradeCoins.addChip(ViticultureChip.Coin_2);
		tradeCoins.addChip(ViticultureChip.Coin_1);
		tradeVP.addChip(ViticultureChip.VictoryPoint_1);
		tradeWhiteGrape.addChip(ViticultureChip.WhiteGrape);
		tradeRedGrape.addFixedChip(ViticultureChip.RedGrape);
		
		yokeCash.addFixedChip(ViticultureChip.Coin_5);
		yokeCash.addFixedChip(ViticultureChip.Coin_2);
		yokeCash.addFixedChip(ViticultureChip.Coin_1);
		yokeRedGrape.addFixedChip(ViticultureChip.RedGrape);
		yokeRedWine.addFixedChip(ViticultureChip.RedWine);
		yokeWhiteGrape.addFixedChip(ViticultureChip.WhiteGrape);
		yokeWhiteWine.addFixedChip(ViticultureChip.WhiteWine);
		yokeRoseWine.addFixedChip(ViticultureChip.RoseWine);
		yokeChampaign.addFixedChip(ViticultureChip.Champagne);
	  	
        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    
    public void startNewYear(replayMode replay)
    {	seasonRow = 0;
    	ViticultureCell bonusMeeple = roosterTrack[6][1];
		bonusMeeple.reInit();
		bonusMeeple.addChip(ViticultureChip.GrayMeeple);
		ViticultureCell grapeMeeple = FirstPlayerMeeplePosition();
		grapeMeeple.reInit();
		grapeMeeple.addChip(ViticultureChip.StartPlayerMarker);
		for(PlayerBoard p : pbs) 
			{ retrieveWorkers(p,replay);	// retrieve workers again, in case they were added by guest speaker
			  p.startNewYear(); 
			}
		
		if(automa) { retrieveAutomaWorkers(); }
		
		checkGameOver(replay); 

		for(PlayerBoard pb : pbs ) 
		{	// statue bonus can't trigger endgame this year.
			pb.activeWakeupPosition = pb.wakeupPosition;
			if(pb.hasStatue())
				{ changeScore(pb,1,replay,StatueBonus,ViticultureChip.Statue,ScoreType.OrangeCard); 
				  // recalculate the winner
				  if(board_state.GameOver()) { findWinner(replay);  }
				}
		}
    }
    //
    // this is where the "grape" marker sits.  It's more than a symbol,
    // it also tells us if the start player for the next round has 
    // already been selected.  This is important for the mentor/organizer combo
    //
	private ViticultureCell FirstPlayerMeeplePosition()
	{
		return(roosterTrack[6][3]);
	}
    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	
		variation = ViticultureVariation.findVariation(gtype);
		Assert(variation!=null,WrongInitError,gtype);
		gametype = gtype;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case viticulture:
			optionsResolved = true;
			break;
		case viticulturep:
			optionsResolved = false;
			break;
			//initBoard();
		}
    	win = new boolean[players];
      	adjustRevision(rev);
		options.clear();
		optionsResolved = false;
    	randomKey = key;
    	players_in_game = players;
    	automa = (players==1);
		setState(ViticultureState.Puzzle);
		choiceA.selected = false;
		choiceB.selected = false;
		reshuffleAt = -1;
    	doInitAfterOptions(variation==ViticultureVariation.viticulture);
    }

	public void doInitAfterOptions(boolean after)
	{
    	random = new Random(randomKey);
    	turnChangeSamePlayer = false;
    	flashChip = null;
    	firstChoice = null;
		for(ViticultureCell c = allCells; c!=null; c=c.next) { c.reInit(); } 
		
    	PlayerBoard oldpb[] = pbs;
    	PlayerBoard newpb[] = new PlayerBoard[players_in_game];
       	int map[] = getColorMap(); 
        
       	for(int i=0;i<newpb.length;i++)
    	{	// reuse the existing player boards where possible. 
    		// this is important for getCell to work
    		if(oldpb!=null && i<oldpb.length && oldpb[i]!=null) 
    			{ newpb[i]=oldpb[i]; 
    			newpb[i].setColor(map[i]); 
    			}
    		else { newpb[i] = new PlayerBoard(this,i,map[i]); }
    		newpb[i].doInit();
    	}
    	pbs = newpb;

    	if(automa)
    	{	ViticultureColor pc = pbs[0].color;
    		ViticultureColor colors[] = ViticultureColor.values();
    		automaColor = colors[(pc.ordinal()+5)%6];
    		automaScore = WINNING_SCORE;
    		automaWorkers = 0;
    	}
    	
	   	for(ViticultureChip ch : ViticultureChip.VineDeck) { greenCards.addChip(ch); }
	   	
 	   	for(ViticultureChip ch : ViticultureChip.SummerDeck) 
 	   	{ if(ch!=ViticultureChip.ImporterCard) { yellowCards.addChip(ch); }
 	   	}
 	   	if(revision>=103)
 	   	{
 	   		if(automa)
 	   			{for(ViticultureChip ch : ViticultureChip.NonAutomaYellowCards103)  { yellowCards.removeChip(ch);}
 	   			}
 	   	}
 	   	else
 	   	{	// these were accidentally always removed
  	   		{for(ViticultureChip ch : ViticultureChip.NonAutomaYellowCards)  { yellowCards.removeChip(ch);}
  	   		}
 	   	}
 	   	
	   	for(ViticultureChip ch : ViticultureChip.OrderDeck) { purpleCards.addChip(ch); }
	   	plannerCells.clear();
	   	plannerMeeples.clear();
	   	plannerMoves.clear();
	   	
	   	cardBeingResolved = null;
	   	triggerCard = null;
	   	stateChange = 0;
	   	cardResolution = null;
	   	tradeFrom = null;
	   	tradeTo = null;
	   	buildingSelection  = null;
	   	pendingMoves.clear();
	   	selectedWakeup = null;
	   	gameEvents.clear();
	   	continuation.clear();
	   	for(ViticultureChip ch : ViticultureChip.WinterDeck) 
	   		{ if(ch!=ViticultureChip.QueenCard) { blueCards.addChip(ch); }	// all except the queen 
	   		}
	   	
	   	if(revision>=103)
	   	{
	   		if(automa)
	   			{for(ViticultureChip ch : ViticultureChip.NonAutomaBlueCards103) { blueCards.removeChip(ch); }
	   			}
	   	}
	   	else
	   	{ // these were accidentally always removed
 	   	for(ViticultureChip ch : ViticultureChip.NonAutomaBlueCards)  { blueCards.removeChip(ch);}
	   	}
	   	
 	   	for(ViticultureChip ch : ViticultureChip.StructureDeck) { structureCards.addChip(ch); }
 	   	if(automa)
 	   	{	
 	   		structureCards.removeChip(ViticultureChip.Academy);
 	   		structureCards.removeChip(ViticultureChip.Fountain);	   		
 	   		
 	   	}
 	   	randomizeHiddenState(random);
 	   	
	   	for(ViticultureChip ch : ViticultureChip.MamasDeck) { mamaCards.addChip(ch); }
	   	mamaCards.shuffle(random);
	   	
	   	for(ViticultureChip ch : ViticultureChip.PapasDeck) { papaCards.addChip(ch); }
	   	papaCards.shuffle(random);
	   	
	   	for(ViticultureChip ch : ViticultureChip.WorkersDeck) { workerCards.addChip(ch); }
	   	workerCards.shuffle(random);
	   	if(automa)
	   	{
	   		workerCards.removeChip(ViticultureChip.SoldatoCard);
	   		workerCards.removeChip(ViticultureChip.ChefCard);
	   		workerCards.removeChip(ViticultureChip.MerchantCard);
	   		workerCards.removeChip(ViticultureChip.InnkeeperCard);
	   		ViticultureChip star = ViticultureChip.getChip(ChipType.Star,automaColor);
	   		for(int i=0;i<starTrack.length;i++)
	   		{
	   			starTrack[i].addChip(star);
	   			if(StarVps[i]==2) { starTrack[i].addChip(star); }
	   		}
	   		for(ViticultureCell row[] : roosterTrack)
	   		{
	   			row[0].addChip(ViticultureChip.Bead);
	   		}
	   	}
	   	if(players_in_game<=2) { workerCards.removeChip(ViticultureChip.InnkeeperCard); }
	   	specialWorkerCards.addChip(workerCards.chipAtIndex(0));
	   	specialWorkerCards.addChip(workerCards.chipAtIndex(1));
	   	
	   	for(ViticultureChip ch : ViticultureChip.AutomaDeck) { automaCards.addChip(ch); }
	   	automaCards.shuffle(random);
	   	
	    reInit(magnifiers);
	    for(ViticultureCell c : magnifiers) { c.addChip(ViticultureChip.Magnifier); } 
	   	
	    whoseTurn = FIRST_PLAYER_INDEX;
	    seasonRow = 0;
	    targetPlayer = -1;		// playing initiating a poll of players
	    suspendedWhoseTurn = -1;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    pickedSourceIndex.clear();
	    stateStack.clear();
	    currentWorker = null;
	    currentAction = null;
	    currentMove = null;
	    currentReplay = null;
	    
	    pickedObject = null;
	    resetState = ViticultureState.Puzzle;
	    lastDroppedObject = null;
	    lastDroppedWorker = null;
	    lastDroppedWorkerIndex = 0;
		unselect();
        animationStack.clear();
        moveNumber = 1;
	   	// deal a mama and a papa to each player, and the starting workers
        // this season and year is where the mamma and papa card draws will be recorded
        if(after && !testOption(Option.DraftPapa))
        {
	   	for(PlayerBoard pb : pbs)
	   	{	pb.mama = mamaCards.removeTop();
	   		pb.papa = papaCards.removeTop();
	   		if(testOption(Option.DrawWithReplacement))
	   		{	replaceCard(mamaCards,pb.mama);
	   			replaceCard(papaCards,pb.papa);	
	   		}
	   		pb.resolveMama();
	   	}}
        startingYear = year = -1;			// year -1 resolve papas year 0 select roosters
	    startNewYear(replayMode.Replay);				
        season = 3;			// start in winter for positioning of the roosters
        seasonRow = 0;
        robotDepth = 0;
        for(PlayerBoard pb : pbs)
        {	pb.setSeason(season);
       		residualTrack[0].addChip(pb.getResidualMarker());
       	 
        }

        // note that firstPlayer is NOT initialized here
    }
    private void setInitialWakeupPositions(int startPosition)
    {	int map[] = AR.intArray(players_in_game);
    	if(revision>=128)
    	{	startPosition = 0;
    		new Random(randomKey).shuffle(map);
     		whoseTurn = map[0];
    	}
 	    for(int i=0;i<players_in_game;i++)
	    {
	    	int idx = (i+startPosition)%players_in_game;
	    	ViticultureCell c = roosterTrack[i][3];
	    	PlayerBoard pb = pbs[map[idx]];
	    	pb.setSeason(3);
	    	pb.wakeupPosition = pb.activeWakeupPosition = c;
	    	c.reInit();
	    	c.addChip(pb.getRooster());
	    }

    }

    /** create a copy of this board */
    public ViticultureBoard cloneBoard() 
	{ ViticultureBoard dup = new ViticultureBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((ViticultureBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ViticultureBoard from_b)
    {
        super.copyFrom(from_b);
        automa = from_b.automa;
        automaColor = from_b.automaColor;
        automaScore = from_b.automaScore;
        automaWorkers = from_b.automaWorkers;
        firstChoice = from_b.firstChoice;
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        pickedSourceIndex.copyFrom(from_b.pickedSourceIndex);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        currentWorker = from_b.currentWorker;
        currentAction = getCell(from_b.currentAction);
        currentMove = Viticulturemovespec.copyFrom(from_b.currentMove);
        currentReplay = from_b.currentReplay;
        for(int i=0;i<pbs.length;i++) { pbs[i].copyFrom(from_b.pbs[i]); }

        triggerCard = from_b.triggerCard;
        cardBeingResolved = from_b.cardBeingResolved;
        stateChange = from_b.stateChange;
        cardResolution = from_b.cardResolution;
        tradeFrom = from_b.tradeFrom;
        tradeTo = from_b.tradeTo;
        
        starPlacement = getCell(from_b.starPlacement);
        starDropped = getCell(from_b.starDropped);
        starDropped2 = getCell(from_b.starDropped2);
        buildingSelection = from_b.buildingSelection;
        pendingMoves.copyFrom(from_b.pendingMoves);
        selectedWakeup = getCell(from_b.selectedWakeup);
        getCell(plannerCells,from_b.plannerCells);
        plannerMeeples.copyFrom(from_b.plannerMeeples);
        continuation.copyFrom(from_b.continuation);
        plannerMoves.copyFrom(from_b.plannerMoves);
        year = from_b.year;
        startingYear = from_b.year;
        season = from_b.season;
	    seasonRow = from_b.seasonRow;
	    targetPlayer = from_b.targetPlayer;
	    suspendedWhoseTurn = from_b.suspendedWhoseTurn;
	   
	    // user interface cells
	    copyFrom(grapeDisplay,from_b.grapeDisplay);
	    copyFrom(wineDisplay,from_b.wineDisplay);
	    yokeCash.copyCurrentCenter(from_b.yokeCash);
	    grapeDisplayCount = from_b.grapeDisplayCount;
	    wineDisplayCount = from_b.wineDisplayCount;
	    lastDroppedWorker = getCell(from_b.lastDroppedWorker);
	    lastDroppedWorkerIndex = from_b.lastDroppedWorkerIndex;

		optionsResolved = from_b.optionsResolved;
		options.copy(from_b.options);

		choiceA.selected = from_b.choiceA.selected;
		choiceB.selected = from_b.choiceB.selected;
		reshuffleAt = from_b.reshuffleAt;
	    sameboard(from_b); 
    }
    public boolean reshuffled()
    {	int rs = reshuffleAt;
    	boolean yes = rs==moveNumber;
    	if(yes)
    	{
    		reshuffleAt = -1;
    	}
    	return yes; 
    }
    

    public void sameboard(BoardProtocol f) { sameboard((ViticultureBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ViticultureBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        Assert(automa==from_b.automa,"automa mismatch");
        Assert(unresign==from_b.unresign,"unresign mismatch");
        Assert(variation==from_b.variation,"variation matches");
        Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        Assert(sameCells(plannerCells,from_b.plannerCells),"plannerCells mismatch");
        Assert(plannerMeeples.sameContents(from_b.plannerMeeples), "plannerMeeples mismatch");
        Assert(resetState==from_b.resetState,"resetstate mismatch");
        Assert(targetPlayer==from_b.targetPlayer,"triggerPlayer mismatch");
        Assert(suspendedWhoseTurn==from_b.suspendedWhoseTurn,"suspendedWhoseTurn mismatch");
        Assert(continuation.sameContents(from_b.continuation),"continuation mismatch");
        Assert(stateChange==from_b.stateChange,"forced state change mismatch");
        for(int i=0;i<pbs.length; i++) { pbs[i].sameBoard(from_b.pbs[i]); }
        Assert(currentWorker==from_b.currentWorker,"currentWorker mismatch");
        Assert(sameCells(currentAction,from_b.currentAction),"currentAction mismatch");
        Assert(choiceA.selected==choiceA.selected,"choiceA different");
        Assert(choiceB.selected==from_b.choiceB.selected,"choiceB different");
		Assert(optionsResolved == from_b.optionsResolved,"optionsResolved mismatch");
		Assert(options.equals(from_b.options),"options mismatch");

        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     * looks for duplicate digests.  
     * (7) digests are also used in live play to detect "parroting" by running two games
     * simultaneously and playing one against the other.
     */
    public long Digest()
    { 
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,pickedSourceIndex);
 		v ^= Digest(r,droppedDestStack);
 		v ^= Digest(r,firstChoice);
		v ^= Digest(r,revision);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
 		v ^= Digest(r, resetState.ordinal());
 		v ^= Digest(r,cardResolution==null?0:cardResolution.ordinal());
		v ^= Digest(r,starPlacement);
 		v ^= Digest(r,starDropped);
 		v ^= Digest(r,starDropped2);
		v ^= Digest(r,targetPlayer);
		v ^= Digest(r,tradeFrom);
		v ^= Digest(r,tradeTo);
		v ^= Digest(r,plannerCells);
		v ^= Digest(r,cardBeingResolved);
		v ^= Digest(r,stateChange);
		v ^= Digest(r,currentWorker);
		v ^= Digest(r,currentAction);
		v ^= Digest(r,optionsResolved);
		v ^= Digest(r,choiceA.selected);
		v ^= Digest(r,choiceB.selected);
		v ^= Digest(r,options);

		if(pendingMoves.size()>0)
		{
			for(int lim = pendingMoves.size()-1; lim>=0; lim--)
			{
				Viticulturemovespec m = (Viticulturemovespec)pendingMoves.elementAt(lim);
				long c = m.source.ordinal()*10000+m.from_col*100+m.from_row*10+m.from_index;
				long d = (m.dest.ordinal()*10000+m.to_col*100+m.to_row*10);
				if(revision>=127) { d *= 10000000; }
				v ^= Digest(r,c);
				v ^= Digest(r,d);
			}
		}
		v ^= continuation.Digest(r);
		v ^= Digest(r,automa);
		for(PlayerBoard pb : pbs) 
		{ v ^= pb.Digest(r); 
		}
		if(revision>=127)
		{
			v ^= Digest(r,suspendedWhoseTurn);
		}
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {	PlayerBoard current = pbs[whoseTurn];
    	if(revision>=105)
    		{
    		 // this is probably always ok, but just to make sure condition on the
    		 // revision number.
    		current.unselect();
    		current.unselectUI();
    		}
    	PlayerBoard pb = findNextPlayer(replay);		
    	currentWorker = null;
    	currentAction = null;
    	currentMove = null;
    	currentReplay = null;
    	pb.flashChip = null;
    	triggerCard = null;
        whoseTurn = pb.boardIndex;
        pb.startingCash = pb.cash;
        pb.startingScore = pb.score;
        turnChangeSamePlayer = (players_in_game>1) && (pb==current);
        seasonRow = pbs[whoseTurn].wakeupPosition.row;
        moveNumber++; //the move is complete in these states
    }
    private PlayerBoard findNextPlayer(replayMode replay)
    {
    	switch (board_state)
        {
        default:
        	throw G.Error("Move not complete in state %s",board_state);
        case Puzzle:
        	return(pbs[whoseTurn]);
        case Play:
        case Confirm:
        case FullPass:
        case Resign:
        	boolean continuous = testOption(Option.ContinuousPlay);    	
        	PlayerBoard next = (continuous&&year>0) ? findNextPlayerAnySeason() : findNextPlayerInSeason();
        	if(next==null)
        	{

        	if(year>0) 
    			{
       			if(continuous)
       			 {
       				 next = findFirstPlayerAnySeason(0);
       				 // we only run out at the end of the year
       				 if(next==null)
       				 {
       				   year++; 
             			   if(year>0)
             			   {	startNewYear(replay);
             			   } 
       				 }
       				next = findFirstPlayerAnySeason(0);
       			 }
       			else 
       			{
    			 next = findFirstPlayerInSeason();
    			 if(next==null)
    			 {
    			 season++;
    			 if(season>3) 
    			 { season = 0;
    			   year++; 
    			   if(year>0)
    			   {	startNewYear(replay);
    			   }
    			 }
    			 next = findFirstPlayerInSeason();
    			 if(automa)
    			 {
    			  placeAutomaInSeason(replay); 
    			 }

    			 }}}
	    		else 
	    		{ year++; if(year>0) { season = 0; } 
	    		  next = findFirstPlayerInSeason();
	    		  if(year>0 && automa)
	 			  {
	 			  placeAutomaInSeason(replay); 
	 			  }
	    		}
        	}
        	Assert(next!=null,"there must be a next player");
         	return(next);
        }
    }
   		
    private void placeAutomaInSeason(replayMode replay)
    {	if(automaCards.topChip()==null) 
    		{ 			
			reshuffle(automaCards,automaDiscards); 
			}   		
    	ViticultureChip card = automaCards.removeTop();
    	automaDiscards.addChip(card);
    	ViticultureChip worker = ViticultureChip.getChip(ChipType.Worker,automaColor);
    	StringTokenizer tok = new StringTokenizer(card.description);
    	while(tok.hasMoreTokens()) {
    		ViticultureId id = ViticultureId.find(tok.nextToken());
    		ViticultureCell c = getCell(id,'@',0);
    		if((automaWorkers < maxWorkers())
    				&& (c.season==season) 
    				&& (c.topChip()==null)) 
    			{ c.addChip(worker);
    			  automaWorkers++;
    			}
    	}
    }
    // this works correctly for the player currently controlling the gray meeple
    public PlayerBoard playerWithColor(ViticultureColor color)
    {	if(color==ViticultureColor.Gray)
    	{	// player with the gray meeple, used the gray meeple as his planner
    		for(PlayerBoard pb : pbs) { if(pb.grayWorker!=null) { return(pb); }}
    	}
    	else {
    		for(PlayerBoard pb : pbs) { if(pb.color==color) { return(pb); }}
    	}
    	throw G.Error("No player has color %s",color);
    }
    	
    private PlayerBoard findNextPlayerAnySeason()
    {	
    	PlayerBoard pb = getCurrentPlayerBoard();
    	// activeWakeupposition is the same as wakeupPosition
    	// except in changing from one year to the next
    	ViticultureCell wake = pb.activeWakeupPosition;
    	return findFirstPlayerAnySeason(wake.row+1);
    }
    private PlayerBoard findFirstPlayerAnySeason(int row)
    {
    	while(row<roosterTrack.length) 
    	{
    	  ViticultureCell cells[] = roosterTrack[row];
    	  for(int i=0;i<cells.length;i++)
    	  {
    		  ViticultureCell potential = cells[i];
    		  ViticultureChip top = potential.topChip();
    		  if((top!=null) && (top.type==ChipType.Rooster))
    		  {	  // ignore players who are pending next year
    			  PlayerBoard p = playerWithColor(top.color);
    			  if(p.activeWakeupPosition==p.wakeupPosition) { return p; }
    		  }
 
    	  }
    	  row++;
    	}    	  
    	return null;
    }
    
    
    private PlayerBoard findNextPlayerInSeason()
    {	
    	char col = (char)('A'+season);	// not the season of the bird
    	int nrows = roosterTrack.length;
    	for(int row = seasonRow+1;row<nrows;row++)		// dont come back to the same cell
    	{
    		ViticultureCell n = getCell(ViticultureId.RoosterTrack,col,row);
    		ViticultureChip top = n.topChip();
    		if(top!=null && (top.type==ChipType.Rooster))	// the grape might be there
    		{
    			return(playerWithColor(top.color));
    		}
    	}
    	return(null);
    }

    private void queuePlannerAction(PlayerBoard pb)
    {
    	for(int i=0;i<plannerCells.size();i++)
    	{	ViticultureCell c = plannerCells.elementAt(i);
    		if(workerCellSeason(c)==season(pb)
    			&& (plannerMoves.elementAt(i).player==pb.boardIndex))
    		{
    			addContinuation(Continuation.ExecutePlanner);
    		}
    	}
    }

    
    private ViticultureState takePlannerAction(PlayerBoard pb,replayMode replay)
    {	//if(plannerCells.size()>1) { //p1("multiple planner actions"); }
    	for(int i=0;i<plannerCells.size();i++)
    	{	ViticultureCell c = plannerCells.elementAt(i);
    		if(workerCellSeason(c)==season(pb)
    				&& (!testOption(Option.ContinuousPlay)
    					|| revision<156
    					|| (plannerMoves.elementAt(i).player==pb.boardIndex))
    				)
    		{	plannerCells.remove(i,true);
    			ViticultureChip meeple = plannerMeeples.remove(i,true);
    			Viticulturemovespec m = (Viticulturemovespec)plannerMoves.remove(i, true);
    			PlayerBoard player = playerWithColor(meeple.color);
    			i--;	// redo the index since we removed one
     			if(canPlaceWorker(player,meeple,c,MoveGenerator.All))
    			{     
     				
     				whoseTurn = player.boardIndex;
     				triggerCard = null;
     				setState(resetState = ViticultureState.Play);
     				//p1("take planner action");
     				if(!testOption(Option.ContinuousPlay))
     						{ addContinuation(Continuation.RestartSeason); 
     						}
     				ViticultureState nextState = performDropAction(player,c,meeple,replay,m,true);	// also a pre-placed piece
    				if(nextState!=null) 
    					{ //p1("planner with continuation");
    					  return(nextState);
    					}
    				else { 
    					//p1("planner without continuation");
    					doContinuation(player,replay,m);
    					return(board_state);
    				}
    			}
    		}
    	}
    	return(null);
    }
    private ViticultureState takeMessengerAction(PlayerBoard pb,replayMode replay)
    {	ViticultureCell messenger = pb.messengerCell;
    	ViticultureState nextState = null;
    	//
    	// under exceedingly rare circumstances, you can play 2 planner cards
    	// and advance seasons twice.  Less uncommon if you are playing with
    	// the card replacement option.  So this assertion, which was intended
    	// for debugging, was always incorrect
    	//Assert((messenger==null)||(workerCellSeason(messenger)>=season(pb)),"bypassed the messenger");
    	if((messenger!=null)&&(workerCellSeason(messenger)<season(pb)))
    	{	pb.messengerCell = null;
    		pb.messengerMove = null;
    		messenger = null;
    		logGameEvent("Bypassed your messenger");
    	}
   	
    	if(messenger!=null && workerCellSeason(messenger)==season(pb))
    	{	
    		pb.messengerCell = null;
        	Viticulturemovespec messengermove = pb.messengerMove;
    		triggerCard = null;
    		ViticultureChip mworker = ViticultureChip.getChip(ChipType.Messenger,pb.color);
    		// soldato cost 0 because it's already there
    		if(canPlaceWorker(pb,mworker,revision>=142 ? 0 : soldatoCost(pb,messenger),messenger,MoveGenerator.All))
    		{
    		//p1("Messenger "+messenger.rackLocation());
    		Viticulturemovespec mm = messengermove;
    		nextState = performDropAction(pb,messenger,mworker,replay,mm,true);
    		if((nextState==null) && (revision>=111))
    			{ // messenger without continuation.  
    			  // This can only happen if you messenger to take purple cards, everything else requires more
    			  // actions by the same player
    			doContinuation(pb,replay,mm);
    			return(board_state); }
    		}
    		else if(revision>=135)
    		{	nextState = ViticultureState.MisplacedMessenger;
    		}
     		//else if(pb.workers.height()>0){ //p1("Messenger "+messenger.rackLocation()+" fail"); }
    	}
    	return(nextState);
    }
    
    private PlayerBoard findFirstPlayerInSeason()
    {	
    	char col = (char)('A'+season);	// not the season of the bird
    	int nrows = roosterTrack.length;
    	for(int row = 0;row<nrows;row++)		// dont come back to the same cell
    	{
    		ViticultureCell n = getCell(ViticultureId.RoosterTrack,col,row);
    		ViticultureChip top = n.topChip();
    		if(top!=null && (top.type==ChipType.Rooster))	// the grape might be there
    		{
    			return(playerWithColor(top.color));
    		}
    	}
    	return(null);
    }
  
    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	boolean v = board_state.doneState();
    	if(v)
    	{
    	switch(board_state)
    	{ 
    	case Plant1AndGive2:
    		return(false); 
    	case Age2Once:
    		{
    			PlayerBoard pb = getCurrentPlayerBoard();
    			if(pb.selectedCells.size()>2) { return(false); }
    			if(!canAgeSelected(pb)) { return(false); }
    		}
    		break;
    	case Discard2GrapesFor3VP:
     		{
    		PlayerBoard pb = getCurrentPlayerBoard();
    		// prior to rev 141, you could always click on done
    		// after 141, can only use the done button when you sacrifice 2
    		if(revision>=141 && pb.selectedCells.size()!=2) { v = false; }
    		}
    		break;
    	case DiscardGrapeAndWine:
    		{
    		// if he has selected only 1, disable the done button
    		PlayerBoard pb = getCurrentPlayerBoard();
    		if(pb.selectedCells.size()>0) { v = false; }
    		if(revision>=141) { v = false; }
    		}
    		break;
    	case DiscardWineFor2VP:
    		if (revision>=151)
    		{
    		PlayerBoard pb = getCurrentPlayerBoard();
    		if(pb.selectedCells.size()>1) { v = false; }
    		break;
    		}
    		// old games fall into the old default clause
			//$FALL-THROUGH$
		default: 
    		if((revision>=127) && underHarvest()) 
    			{ // don't declare underharvest as automatically done states.
    			  // the UI provides an override
    			  return(false); 
    			}
    		switch(board_state.ui)
    		{
    		case ShowHarvestsAndUproots:
    		case ShowPlants:
    		case ShowUproots:
    		{	// all varieties of planting and uprooting, don't allow "done" when
    			// partially specified
        		PlayerBoard pb = getCurrentPlayerBoard();
        		if((pb.nSelectedCards()>0)||(pb.selectedCells.size()>0)||(pendingMoves.size()>0))
        			{
        			v = false;
        			}
        		}
        		break;
    		default: break;
    		}
    		break;
    		}
    	}
    	else if(board_state==ViticultureState.ChooseOptions)
    		{ return allPlayersReady();
    		}

    	return(v);
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }
    private int currentMaxScore()
    {
    	int max = MIN_SCORE;
    	for(PlayerBoard pb : pbs)
    	{
    		int sc = pb.score;
    		if(sc>max) { max = sc;  }
    	}
    	return(max);
    }
    
    private boolean checkGameOver(replayMode replay)
    {
     	int max = currentMaxScore();
    	if(automa 
    			? year==8 
    			: ((max>=WINNING_SCORE) || (robotBoard ? (year>=finalYear) : false)))
    	{	
    		// add the influence area
    		addInfluenceScores(replay);
    		setState(resetState = ViticultureState.Gameover);
   		
    		findWinner(replay);

    		return(true);
    	}
    	return(false);
    }
    
    private void findWinner(replayMode replay)
    {
       	int max = 0;
    	PlayerBoard maxp = null;
 
    	AR.setValue(win,false);		// initialize just to be sure, winner can change
		// re-find the winner
    	for(PlayerBoard pb : pbs)
    	{
    		int sc = pb.tiebreakScore();
    		if(maxp==null || sc>max) { max = sc; maxp = pb; }
    	}
		if(automa) { if(maxp.score<automaScore) { maxp = null; }}
		if(maxp!=null) { win[maxp.boardIndex]= true; } 
		
		if(replay!=replayMode.Replay)
			{for(PlayerBoard pb : pbs)
			{
			G.print("\nScore "+pb+" = "+pb.score+"\n"+pb.scoreString);
			}}
    }
    

    private int countAutomaStars(ViticultureCell starCell)
    {
    	int n = 0;
    	for(int lim=starCell.height()-1; lim>=0; lim--)
    		{
    		if(starCell.chipAtIndex(lim).color==automaColor) { n++; }	
    		}
    	return(n);
    }
    
    private void addInfluenceScores(replayMode replay)
    {
    	for(ViticultureCell star : starTrack)
    	{
    		PlayerBoard bestP = null;
    		int bestCount = 0;
    		int nBest = 0;
    		for(PlayerBoard pb : pbs)
    		{
    			int n = pb.countStars(star);
    			if(n==bestCount) { nBest++; }
    			else if(n>0 && ((bestP==null) || (n>=bestCount)))
    			{	bestP = pb;
    				nBest = 1;
    				bestCount = n;
    			}
    		}
    		if(automa)
    		{
    			int n = countAutomaStars(star);
    			if(n>bestCount)
    				{ 
    				  automaScore += StarVps[star.row]; bestP=null;
    				}
    			else if(n==bestCount) { nBest++; }
    		}
    		if(nBest==1 && bestP!=null) 	// winner for this area
    		{
    			changeScore(bestP,StarVps[star.row],replay,StarInfluence,bestP.getStar(),ScoreType.Star);
    		}
    	}
    }


    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        pickedSourceIndex.clear();
        for(PlayerBoard pb : pbs) { pb.selectedCells.clear(); pb.clearSelectedCards(); }
        stateStack.clear();
        pickedObject = null;
        cardResolution = null;
        tradeFrom = null;
        tradeTo = null;
        starPlacement = null;
        starDropped = null;
        starDropped2 = null;
        firstChoice = null;
        buildingSelection = null;
        selectedWakeup = null;
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private ViticultureCell unDropObject()
    {	ViticultureCell rv = droppedDestStack.pop();
		resetState = stateStack.pop();
    	setState(stateStack.pop());
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ViticultureCell rv = pickedSourceStack.pop();
 		resetState = stateStack.pop();
    	setState(stateStack.pop());
    	if(revision>=107)
    	{	// clean up the picked stack, keeps the digests consistent!
    	   	int ind = pickedSourceIndex.pop();
    	   	if(ind<0) { ind = rv.height()-1; }
    	   	if(!fixedSize(rv)) { rv.insertChipAtIndex(ind,pickedObject); }
    	}
    	else
    	{
    	if(!fixedSize(rv)) { rv.addChip(pickedObject); }
    	}
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(ViticultureCell c)
    {
    	switch(c.rackLocation())
    	{
        case RedGrape:
        case RedWine:
        case WhiteGrape:
        case WhiteWine:
        case RoseWine:
        case Champaign:
        case Cash:
        	if(c.col=='@') { break; }
			//$FALL-THROUGH$
		default:
        	c.addChip(pickedObject); 
    	}
  		switch(c.rackLocation())
		{
		case Cash:
			{
			PlayerBoard pb = pbs[c.col-'A'];
			if(pickedObject==ViticultureChip.Coin_5) { pb.cash += 5; }
			if(pickedObject==ViticultureChip.Coin_2) { pb.cash += 2; }
			if(pickedObject==ViticultureChip.Coin_1) { pb.cash += 1; }
			}
			break;
		default: break;
		}
       droppedDestStack.push(c);
       stateStack.push(board_state);
       stateStack.push(resetState);
       lastDroppedObject = pickedObject;
       pickedObject = null;
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ViticultureCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { ViticultureChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    ViticultureCell getCell(ViticultureId source, char col, int row)
    {	
    	ViticultureCell c = gettableCells.get(ViticultureCell.UICode(source, col, row));
    	Assert(c!=null, "cant find cell %s %s %s",source,col,row);
    	return(c);
    }
    public ViticultureCell getCell(ViticultureId c)
    {
    	return(getCell(c,'@',0));
    }
    public ViticultureCell getCell(ViticultureCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
    
    private boolean fixedSize(ViticultureCell c)
    {
        switch (c.rackLocation())
        {
        default:
        	return(false);
        case RedGrape:
        case RedWine:
        case WhiteGrape:
        case WhiteWine:
        case YokeCash:
        case RoseWine:
        case Champaign:
        	return(c.col=='@');
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private ViticultureChip pickObject(ViticultureCell c,int idx)
    {	boolean fixedSize = fixedSize(c);
    
    	if(fixedSize)
    	{
    		pickedObject = c.topChip();
    	}
    	else
    	{	if(idx<0) 
    			{ idx = c.height()-1; }
    		switch(c.rackLocation())
    		{
    		case Cash:
    			{
    			PlayerBoard pb = pbs[c.col-'A'];
    			loadCoins(pb.cashDisplay,pb.cash);
    			pickedObject = c.removeChipAtIndex(idx);
    			if(pickedObject==ViticultureChip.Coin_5) { pb.cash -= 5; }
    			if(pickedObject==ViticultureChip.Coin_2) { pb.cash -= 2; }
    			if(pickedObject==ViticultureChip.Coin_1) { pb.cash -= 1; }
    			}
    			break;
    		default: 
        		pickedObject = c.removeChipAtIndex(idx);
        		break;
    		}
    	}
 
        lastPicked = pickedObject;
        lastDroppedObject = null;
        
        pickedSourceStack.push(c);
    	pickedSourceIndex.push(idx);
    	stateStack.push(board_state);
    	stateStack.push(resetState);
    	return(pickedObject);
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ViticultureCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(ViticultureCell dest,replayMode replay)
    {
        switch (resetState)
        {
        default:
        	if(resetState.isWinemaking()) { break; }
        	else throw G.Error("Not expecting drop in state %s", board_state);
        case Retrieve1Current:
        case Move1Star:
        	setState( (droppedDestStack.size()==1) ? ViticultureState.Confirm : resetState);
        	break;
        case Move2Star:
        case Retrieve2Workers:
        	setState( (droppedDestStack.size()==2) ? ViticultureState.Confirm : resetState);
        	break;
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case PlaceWorkerFuture:
        	setState(ViticultureState.Confirm);
        	break;
        case Play:
        	if(automa
        			&& (dest.col=='@')
        			&& pbs[whoseTurn].bonusActions.height()>0)
        		{ setState(resetState = ViticultureState.PlayBonus);
        		}
        	else
        	{
			setState(ViticultureState.Confirm);
        	}
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private void prepareWineDisplay(ViticultureState state,PlayerBoard pb)
    {	
    	if(state.isWinemaking())
    	{
    	wineDisplayCount = bottleCount(state);
    	grapeDisplayCount = 0;
		reInit(grapeDisplay);
		reInit(wineDisplay);
   			
		// load the grape display with grapes that know their value
		for(ViticultureCell c : pb.redGrape)
	    	{
	    		if(c.topChip()!=null)
	    		{	
	    			grapeDisplay[grapeDisplayCount].reInit();
	    			grapeDisplay[grapeDisplayCount].addChip(ViticultureChip.RedGrapes[c.row]);
	    			grapeDisplayCount++;
	    		}
	    	}
	     	grapeDisplayCount++;	// add a blank cell between the reds and whites
	     	for(ViticultureCell c : pb.whiteGrape)
	    	{
	    		if(c.topChip()!=null)
	    		{	
	    			grapeDisplay[grapeDisplayCount].reInit();
	    			grapeDisplay[grapeDisplayCount].addChip(ViticultureChip.WhiteGrapes[c.row]);
	    			grapeDisplayCount++;
	    		}
	    	}
    	}    	
    }
    
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Gameover: break;
    	
    	case Give2orVP:		// swindler takes money or VP
    	case Sell1VPfor3:	//banker offers to sell vp
    		if(whoseTurn==targetPlayer) 
    			{ triggerCard = null;
    			  setState(ViticultureState.Play);
    			}
    		break;
    	case ChooseOptions:
    	case Confirm:
    	case PlaySecondYellow:
    	case PlaySecondBlue:
    	case FullPass:
    	case Puzzle:
    	case Play:
    		setNextPlayState(replay);
    	}
      	
    }
	private void setNextPlayState(replayMode replay)
	{
  		
		PlayerBoard pb = getCurrentPlayerBoard();
		
		// after discussion of VI-sven2-ddyer-pangolin-mfeber-idyer-2023-07-09-1902 year 4
		// white had use the planner card to place on harvest2, which was unexpectedly triggered 
		// before the next player played in fall.  This seemed odd at the time, it was decided
		// that the "more natural" interpretation would be to execute the planner action immediately
		// when the planner player enters fall.  This means the planner could in principle be 
		// bumped (by a chef) before it's executed, but also that it's guaranteed to to happen
		// until the player voluntarily enters the season.
		ViticultureState plannerState = (revision>=156)&&testOption(Option.ContinuousPlay)
					? null  
					: takePlannerAction(pb,replay);
		if(plannerState!=null) { setState(plannerState); }
		else {
		ViticultureState messengerState = takeMessengerAction(pb,replay);
		if(messengerState!=null) 
		{ setState(messengerState); 
		}
		else if((variation==ViticultureVariation.viticulturep) && !optionsResolved)
		{
			setState(ViticultureState.ChooseOptions);
		}
		else
		{
		if(!pb.papaResolved)
		{
		if(testOption(Option.DraftPapa))
		{
		setState(ViticultureState.SelectPandM);
		// note that the user interface depends on the number and order of these cards
		drawCards(2,papaCards,pb,replayMode.Replay,null);
		drawCards(2,mamaCards,pb,replayMode.Replay,null);
		}
		else
		{
		setState(ViticultureState.ResolveCard);
		resolveCard(pb.papa);
		}
		}
		else if(year==0)
		{
			setState(ViticultureState.SelectWakeup);
		}
		else if(pb.workers.height()==0) 
		{
			setState(ViticultureState.FullPass );
		}else 
		{	triggerCard = null;
			setState(ViticultureState.Play );
		}}
		}
		resetState = board_state;
	}
    ViticultureCell getStack(ChipType name)
    {
    	for(ViticultureCell stack : cardStacks)
    	{
    		if(stack.contentType==name) { return(stack); }
    	}
    	throw G.Error("not expecting %s", name);
    }
    
    ViticultureCell getDiscards(ViticultureCell from)
    {
    	return(getCell(from.rackLocation().getDiscardId(),'@',0));
    }
    
    private void reshuffle(ViticultureCell from,ViticultureCell discards)
    {	
		long seed = revision>=106 ? randomKey^discards.CardDigest() : randomKey^Digest();
		//G.print("Reshuffle "+discards.rackLocation()+" "+seed+" "+year+":"+season);
		from.copyFrom(discards);
		discards.reInit();
		from.shuffle(new Random(seed));
		logGameEvent(ReshuffleMessage,from.contentType.name());
		reshuffleAt = moveNumber;
    }
    
   
    // redraw n cards, all from a single stack; or if no cards are recorded
    // draw them and record them for replay
    private ViticultureState drawCards(int n,ViticultureCell from,PlayerBoard pb,replayMode replay,Viticulturemovespec formove)
    {	boolean ok = true;	// remains true if we get all the cards requested
    	int originalHeight = from.height();
    	for(int i=0;i<n;i++) { ok &= drawCard(from,pb,replay,formove); }
    	if(ok 
    		&& (currentWorker!=null) 
    		&& (currentWorker.type==ChipType.Oracle)
    		&& (currentWorker.color==pb.color))
    	{	//p1("oracle "+resetState);
    		ViticultureChip cw = currentWorker;
    	    currentWorker = null;				// prevent any other invocations
    		ok &= drawCard(from,pb,replay,formove);	// draw an extra card for the oracle
    		if(ok)	// we got all the cards and one more for the oracle
    		{
    		int h = pb.cards.height();
    		// store the choices so it's unaffected by subsequent activity.  
    		// specifically, if placing 2 stars another card can be captured.
    		pb.oracleCards.reInit();
    		for(int lim =n+1; lim>0; lim--)
    		{
    			pb.oracleCards.addChip(pb.cards.chipAtIndex(h-lim));
    		}
    		triggerCard = cw;

    		if(testOption(Option.DrawWithReplacement)) { replaceCards(from,pb,n+1,originalHeight); }

    		return(ViticultureState.Discard1ForOracle);
    		}
    		// if we didn't get the extra card, skip the oracle. 
    		// this is a real thing for the green deck in 6p games
    	}
    	
    	if(testOption(Option.DrawWithReplacement)) { replaceCards(from,pb,n,originalHeight); }
     	return(null);
    }
    void replaceCards(ViticultureCell from,PlayerBoard pb,int n,int originalHeight)
    {
    	// we take n off the top so there will be no duplicates,
    	// then reinsert them at random points of the deck
    	int h = pb.cards.height();
    	for(int lim =n; lim>0; lim--)
    		{	ViticultureChip ch = pb.cards.chipAtIndex(h-lim);
    			replaceCard(from,ch);
    		}
    	Assert(from.height()==originalHeight,"deck size changed");
    }
    void replaceCard(ViticultureCell to,ViticultureChip card)
    {	long seed = to.CardDigest() * moveNumber;
    	Random r = new Random(seed);
    	to.insertChipAtIndex(r.nextInt(to.height()),card);
    }
    void replaceCards(ViticultureCell from,ViticultureCell to)
    {
    	while(from.height()>0) { replaceCard(to,from.topChip()); }
    }
    //
    // draw a card from a stack and record it for replay. Return true if a card was available
    //
    boolean drawCard(ViticultureCell from,PlayerBoard pb,replayMode replay,Viticulturemovespec formove)
    {	
    	if(from.topChip()==null) 
    	{	reshuffle(from,getDiscards(from));     		
    	}
    	if(from.topChip()!=null)
    	{	ViticultureChip chip = from.removeTop();
    		ViticultureCell dest = pb.cards;
    		dest.addChip(chip);
    		if(formove!=null) { formove.cards = push(formove.cards,chip); }
    		String msg = DrawSomething;
    		if(pb!=pbs[whoseTurn]) { msg = pb.getRooster().colorPlusName()+" "+msg; }
    		logRawGameEvent(msg,chip.type.name());

    		switch(chip.type)
    		{
    		case YellowCard:
    			pb.recordEvent("Take Yellow",chip,ScoreType.ReceiveYellow);
    			break;
    		case BlueCard:
    			pb.recordEvent("Take Blue",chip,ScoreType.ReceiveBlue);   	
    			break;
    		default: break;
    		}
    		
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(from);
    			animationStack.push(dest);
    		}
    		if((chip.type==ChipType.PurpleCard) && pb.hasMercado())
    		{	
    			if(pb.canFillWineOrder(chip))
    			{
    			//p1("fill use mercado");
    			pb.fillableWineOrders.addChip(chip);
    			addContinuation(Continuation.FillMercado);
    			}
    		}
    		return(true);
    	}
    	return(false);
    }
    //
    // draw a chip "on demand", complain if it's not available.
    // this is used to enforce replays despite changes in the
    // random sequence
    //
    void drawCard(ViticultureChip chip,ViticultureCell to,replayMode replay)
    {
    	ViticultureCell from = cardPile(chip);
		if(from.height()==0)
		{
			reshuffle(from,discardPile(chip));
		}
		ViticultureChip top = from.topChip();
		if(top!=chip)
			{ G.print("Not the natural chip "+chip);
			}
    	ViticultureChip ok = from.removeChip(chip);   	
    	Assert(ok==chip,"target chip not found");
    	to.addChip(chip);
    	if(replay!=replayMode.Replay)
		{
			animationStack.push(from);
			animationStack.push(to);
		}
    }
    //
    // change the residual track, respecting the limits of the track
    //
    public void changeResidual(PlayerBoard pb,int amount,replayMode replay)
    {
    	ViticultureChip mark = pb.getResidualMarker();
    	ViticultureCell from = residualTrack[pb.residual];
    	from.removeChip(mark);
    	pb.changeResidual(amount);
        ViticultureCell to = residualTrack[pb.residual];
        to.addChip(mark);
    	if(replay!=replayMode.Replay)
    	{	animationStack.push(from);
    		animationStack.push(to);
    	}
    }

    public void loadCoins(ViticultureCell c,int cash)
    {
    	c.reInit();
    	while(cash>=5) { c.addChip(ViticultureChip.Coin_5); cash-=5; }
        while(cash>=2) { c.addChip(ViticultureChip.Coin_2); cash-=2; }
        while(cash>=1) { c.addChip(ViticultureChip.Coin_1); cash-=1; }
    }
    
    private void changeCash(PlayerBoard pb,int amount,ViticultureCell sink,replayMode replay)
    {	//G.print("Cash "+pb+amount+" = "+(pb.cash+amount));
    	Assert(pb.cash+amount>=0,"cash can't be negative");
    	pb.cash += amount;
    	if(replay!=replayMode.Replay)
    	{
    	ViticultureCell dest = amount>0 ? pb.cashDisplay : sink;
    	ViticultureCell source = amount>0 ? sink : pb.cashDisplay;    	
    	int aa = Math.abs(amount);
    	while(aa>=5) 
    		{ animationStack.push(source); animationStack.push(dest); 
    		  aa -=5; 
    		  dest.addChip(ViticultureChip.Coin_5);
    		}
    	while(aa>=2)
    		{ animationStack.push(source); animationStack.push(dest);
    		  aa -=2;
    		  dest.addChip(ViticultureChip.Coin_2);
    		}
    	while(aa>=1) 
    		{ animationStack.push(source); 
    		  animationStack.push(dest);
    		  aa -=1;
    		  dest.addChip(ViticultureChip.Coin_1);
    		}
    	}
    }
    
    public void changeScore(PlayerBoard pb,int n,replayMode replay,String from,ViticultureChip hint,ScoreType type)
    {	ViticultureCell current = scoringTrack[pb.score-MIN_SCORE];
    	Assert(pb.score+n>=MIN_SCORE, "min score is %s", MIN_SCORE);
    	pb.changeScore(n,from,hint,type);
        if(replay != replayMode.Replay)
        {
        	animationStack.push(current);
        	animationStack.push(scoringTrack[pb.score-MIN_SCORE]);
        }   	
    }
    // cost of a worker to someone - can increase due to academy
    private int costOfWorker(PlayerBoard pb)
    {	int cost = BASE_COST_OF_WORKER;
    	for(PlayerBoard p : pbs)
    	{
    		if((pb!=p) && p.hasAcademy()) { cost++; }
    	}
    	return(cost);
    }
    // cost of a worker to someone - can increase due to academy
    public int costOfWorker(PlayerBoard pb,ViticultureChip worker,ViticultureState state)
    {	int cost = BASE_COST_OF_WORKER;
    	if(worker.type!=ChipType.Worker) { cost++; }  
    	for(PlayerBoard p : pbs)
    	{
    		if((pb!=p) && p.hasAcademy()) { cost++; }
    	}
    	int discount = 0;
    	switch(state)
    	{
    	case TrainWorkerDiscount4: discount++;
    		//$FALL-THROUGH$
    	case TrainWorkerDiscount3: discount++;
			//$FALL-THROUGH$
		case TrainWorkerDiscount2: discount++;
			//$FALL-THROUGH$
		case TrainWorkerDiscount1: discount++;
			//$FALL-THROUGH$
		case TrainWorkerOptional:
		case TrainWorker: break;
    	case TrainWorkerAndUseFree: 
    		discount = 4;
			break;
		case TrainWorkerDiscount1AndUse:
			discount = 1;
    		break;
    	default: G.Error("Not expecting %s",resetState);
    	
    	}
    	return(cost-discount);
    }
 
    private ViticultureState doTrainWorker(PlayerBoard pb,int discount,boolean useNow,boolean optional,replayMode replay)
    {	int cost = costOfWorker(pb)-discount;
		if(DoneState() && (pb.nWorkers>=maxWorkers()) || (pb.cash<cost))  
		{
			// unusual case, if the "Mentor" placed a worker here in the future,
			// by the time we get here there might not be enough money or the
			// worker might have already been created.  Normally we only get
			// here with money and worker count already checked.
			return(null);
		}

    	Assert(pb.nWorkers<maxWorkers(),"too many workers");
    	
    	if(useNow)
    	{
    	switch(discount)
    	{
    	case 1: return ViticultureState.TrainWorkerDiscount1AndUse;
    	case 4: return ViticultureState.TrainWorkerAndUseFree;
    	
    	default: throw G.Error("not expecting use now with discount %s",discount);
    	}}
    	else
    	{
    	switch(discount)
    	{
    	case 0:	return(optional ? ViticultureState.TrainWorkerOptional : ViticultureState.TrainWorker);
    	case 1: return(ViticultureState.TrainWorkerDiscount1);
    	case 2: return(ViticultureState.TrainWorkerDiscount2);
    	case 3: return(ViticultureState.TrainWorkerDiscount3);
    	case 4: return(ViticultureState.TrainWorkerDiscount4);
    	default: throw G.Error("not expecting train with discount %s",discount);
    	}
    	}
    }
    private void finishTraining(PlayerBoard pb,ViticultureChip worker,replayMode replay)
    {	boolean useNow = false;
    	Assert(pb.nWorkers<maxWorkers(),"too many workers");

    	int cost = costOfWorker(pb,worker,resetState);

    	if(worker.type!=ChipType.Worker) 
    		{ 
    		pb.workerTypes.addChip(worker);
    		} 
    	
    	switch(resetState)
    	{
    	case TrainWorkerDiscount1AndUse:
    	case TrainWorkerAndUseFree:
    		useNow = true;
    		break;
    	default: break;
    	}
		ViticultureCell dest = useNow?pb.workers : pb.pendingWorker;
		dest.addChip(worker);
		
		pb.nWorkers++;
		
		for(PlayerBoard p : pbs) 
			{ // even in the base game, there can be more
			  // than one acadamy!
			  if((p!=pb) && p.hasAcademy())
				{
				changeCash(p,1,pb.cashDisplay,replay);
				logRawGameEvent(
						"- $1 "
						+ pb.getRooster().colorPlusName()
						+"  + $1 "
						+ p.getRooster().colorPlusName()
						);
				pb.cash--;
				cost --;
				}
			}
		// rest of the cost to the bank
		changeCash(pb,-cost,yokeCash,replay);
			
		if(replay!=replayMode.Replay)
			{
			animationStack.push(recruitWorkers[0]);
			animationStack.push(dest);
			}
    }
    private ViticultureState giveTour(PlayerBoard pb,int amount,replayMode replay)
    {
    	changeCash(pb,amount,yokeCash,replay);
		if((pb.tastingRoom.topChip()!=null)
			&& (pb.hasWine())				// only if he has wine to taste
			&& (pb.usedTastingRoom<year))
		{
			pb.usedTastingRoom = year;
			changeScore(pb,1,replay,GiveATourMessage,pb.getTastingroom(),ScoreType.Other);
			logGameEvent("+1VP #1",pb.getTastingroom().colorPlusName());
		}
		
		for(PlayerBoard p : pbs)
			{ // rev 130, only pay fountain when someone else gives a tour
			if(p.hasFountain() && ((revision<130) || (pb.boardIndex!=p.boardIndex)))
				{ changeCash(p,1,p.destroyStructureWorker,replay); }		
				}
		
		return(pb.hasGazebo() ? pb.placeStarState() : null);
		
   }
    
    private void paySoldatos(PlayerBoard pb,ViticultureCell dest,replayMode replay)
    {	// note that this logic has to agree with pb.nOpponentSoldatos();
    	// soldatos in the grande position do not collect tolls
    	if(dest.parentRow!=null) 
    		{ for(ViticultureCell c : dest.parentRow) 
    			{ if((revision<148)||(c.row!=GrandeExtraRow))
    					{ paySoldatosCell(pb,c,replay); 
    					}
    			}
    		}
    }
    
    private void paySoldatosCell(PlayerBoard pb,ViticultureCell dest,replayMode replay)
    {
    	for(int lim=dest.height()-1; lim>=0; lim--)
    	{
    		ViticultureChip chip = dest.chipAtIndex(lim);
    		if((chip.type==ChipType.Soldato) && (chip.color!=pb.color))
    		{	PlayerBoard opponent = playerWithColor(chip.color);
    			changeCash(opponent,1,pb.cashDisplay,replay);
    			changeCash(pb,-1,pb.cashDisplay,replayMode.Replay);	// no animation
    			//p1("paying for soldato "+dest.rackLocation());
    		}
    	}
    }
    private boolean canStealACard(PlayerBoard pb,ViticultureCell dest)
    {
    	if(dest.parentRow!=null) { for(ViticultureCell c : dest.parentRow) { if(canStealACardFrom(pb,c))  { return(true); }}}
    	return(false);
    }
    private boolean canStealACardFrom(PlayerBoard pb,ViticultureCell dest)
    {	ViticultureChip top = dest.topChip();
    	if(top!=null)
    	{
    		PlayerBoard victim = playerWithColor(top.color);
    		return ( (victim!=pb) && (victim.hasCard(ChipType.YellowCard)||victim.hasCard(ChipType.BlueCard)));
    	}
    	return(false);
    }
    private boolean aloneInSeason(PlayerBoard pb)
    {	//int nextSeason = (season(pb)+1)%4;
    	for(PlayerBoard p : pbs)
    	{	
    		// this is used by the merchant.  In continuous play there are
    		// more opportunities to be active "alone" in season as other 
    		// players race ahead or the merchant's player races ahead
    		if((p!=pb) && (p.wakeupPosition.col == pb.wakeupPosition.col)) { return(false); }		
    		// this was the old test, checking that all the other guys are in nextseason
    		// if((p!=pb) && ((p.wakeupPosition.col-'A')!=nextSeason)) { return(false); }
    	}
    	return(true);
    }
    private ViticultureState performDropAction(PlayerBoard pb,ViticultureCell dest,ViticultureChip worker,replayMode replay,Viticulturemovespec m,boolean isMessengerAction)
    {	
    	currentWorker = worker;
    	currentAction = dest;
    	currentMove = m;
    	currentReplay = replay;
    	boolean paidforsoldato = false;
    	// this implements the "messenger pay later" ruling, which is not official
    	// otherwise, something will have to prevent a second collection when the
    	// messenger or other advance placement is activated.
    	if((dest.season<=season(pb)) 
    			&& ((revision<132) || (resetState!=ViticultureState.TakeActionPrevious))
    			&& ((revision<139) ||  !isMessengerAction))	// interaction of soldato and messenger
				{ paySoldatos(pb,dest,replay); 
				  paidforsoldato = true;
				}	// pay if this is a current transaction
    	//else {    	p1("messeger pay later"); }
    	if(currentWorker!=null)
    	{
    	switch (currentWorker.type)
    		{
    		case Professore:
    			if(addRetrieveCurrentMoves(pb,null))	// has moves to retrieve
    			{	//p1("professore season "+season);
    				addContinuation(Continuation.PerformDropActionAfter);
    				return( ViticultureState.Retrieve1Current);
    			}
    			break;
    		case Merchant:
    			{
    			if(aloneInSeason(pb) && (revision<129 || dest.onBoard))
    				{
    				//p1("merchant season "+season);
    				addContinuation(Continuation.MerchantDraw);
    				}
    			}
    			break;
    		case Innkeeper:
    			// option to pay $1 to steal a visitor card
    			if(canPlaceInnkeeper(pb,worker,dest,MoveGenerator.All,(revision>=148) && paidforsoldato)
    					&& canStealACard(pb,dest))
    			{	//p1("innkeeper steal "+moveNumber);
       				addContinuation(Continuation.PerformDropActionAfter);
       				return(ViticultureState.StealVisitorCard);
    			}
    			break;
    		case Messenger:
    			if(workerCellSeason(dest)>season(pb))
    				{
    				pb.messengerCell = dest;
    	    		pb.messengerMove = m;
    	    		return(null);		// punt out
    				}
    			break;
    		case Chef:
    			if((dest!=dollarWorker) && (dest.col=='@') && (dest.height()>1) && (dest.row!=GrandeExtraRow))	// bumping someone from an action space
    			{	// note that with soldato and chef, the chef can place in the overflow area, but
    				// can't bump anyone.
    				bumpPlayer(pb,dest,replay);
    			}
    			break;
    		case Mafioso:
    			if(!isBonusRow(dest) && ((revision<149) || (dest.row!=GrandeExtraRow)))
    			{	ViticultureId action = dest.rackLocation();
    				if((action==ViticultureId.PlayerYokeWorker)
    					|| (action==ViticultureId.HarvestWorker))
    				{	// upgrade the mafioso harvest to 2
    					return(ViticultureState.Harvest2Optional);
    				}
    				else if((revision>=147) && (action==ViticultureId.PlantWorker))
    				{	// upgrade mafioso to plant 2 vines
    					return(ViticultureState.Plant2VinesOptional);
    				}
    				else if((revision>=147) && (action==ViticultureId.MakeWineWorker))
    				{	// upgrade mafioso to make 4 wines at once
    					return(ViticultureState.Make4WinesOptional);
    				} 
    				else
    				{ addContinuation(Continuation.MafiosoTwice);
    				}
    			}
    			break;
    		default: break;
    		}
    	}
    	return performDropActionAfter(pb,dest,worker,false,replay,m);
    }
    
    private void bumpPlayer(PlayerBoard pb,ViticultureCell dest,replayMode replay)
    {
    	ViticultureChip bumped = dest.removeChipAtIndex(0);
		PlayerBoard bumpPlayer = playerWithColor(bumped.color);
		Assert(bumpPlayer!=pb,"can't bump yourself");	// this works correctly for the gray meeple
		bumpPlayer.workers.addChip(bumped);
		if((revision>=115) && (bumped.type==ChipType.Messenger))
		{
			bumpPlayer.messengerCell = null;
			bumpPlayer.messengerMove = null;
		}
		//p1("chef bump from "+dest.rackLocation());
		if(replay!=replayMode.Replay)
		{
			animationStack.push(dest);
			animationStack.push(bumpPlayer.workers);
		}
    }
    
    private boolean isBonusRow(ViticultureCell dest)
    {
    	switch(dest.rackLocation())
    	{
    		
    	case RecruitWorker:
    		return(dest.row==TrainWorkerDiscountRow);
    	case PlayBlueWorker:
    		switch(dest.row)
			{
			case PlayBlueDollarRow: return(true);	
			case PlayBlueBonusRow:
				return(true);
			default: return(false);
			}
  		
    	case PlayYellowWorker:
    		switch(dest.row)
    			{
    			case PlayYellowBonusRow:
    			case PlayYellowDollarRow: return(true);	
    			default: return(false);
    			}
    	case DrawGreenWorker:
    		return(dest.row==DrawGreenBonusRow);
    	case DrawPurpleWorker:
    		return(dest.row==DrawPurpleBonusRow);
    	case GiveTourWorker:
    		return(dest.row==GiveTourBonusRow);
    	case MakeWineWorker:
    		return(dest.row==MakeWineBonusRow);
    	case BuildTourWorker:
    		return(dest.row==BuildTourBonusRow); 
    	case BuildStructureWorker:
     		return(dest.row==BuildStructureBonusRow);      		
    	case StarPlacementWorker:
    		return(dest.row==PlaceStarBonusRow);   		
    	case PlantWorker:
    		return(dest.row==Plant2BonusRow);        
    	case PlayerYokeWorker:     	
        case DestroyStructureWorker:
        	return(false);
 
        case PlayerStructureCard:
       	case Field:	
       		// this can occur when a field is used to build a building
       		// consider if the structure card has a bonus.  At present,
       		// all the structure cards that take a worker define the
       		// action as including a bonus.  Revision 146 added "field" 
       		// to this consideration, but could not have existed in earlier
       		// completed games.  Before 146, mafioso on structure card returned
       		// false here, but the continuation didn't play him twice due to 
       		// a matching omission.
     		return(true); 
    	case HarvestWorker:
    		switch(dest.row)
    		{
    		case Harvest2BonusRow:
    		case HarvestDollarBonusRow: return(true);
    		default: return(false);
    		}
    	case TradeWorker:
    		return(dest.row==TradeWorkerBonusRow);  		
    	case FlipWorker:
    		switch(dest.row)
    		{
    		case FlipWorkerCardRow:
    		case FlipWorkerVPRow: return(true);
    		default: return(false);
    		}
        case DollarOrCardWorker: return(false); 
        case SellWineWorker:
        	switch(dest.row)
        	{
        	case SellWineStarRow:
        	case SellWineCardRow:
        		return(true);
			default:
				return(false);
        	}
        case FillWineWorker:
        	return(dest.row==FillWineBonusRow);
     	default: throw G.Error("Not expecting dest %s",dest.rackLocation());
    	}
    }

    private ViticultureState performDropActionAfter(PlayerBoard pb,ViticultureCell dest,ViticultureChip worker,boolean optional,replayMode replay,Viticulturemovespec m)
    { 	ViticultureState nextState = null;
    	boolean isFarmer = (worker!=null) && (worker.type==ChipType.Farmer);
    	switch(dest.rackLocation())
    	{
    	case RecruitWorker:
    		// no impact for politio, pay a dollar to gain a dollar
    		nextState = doTrainWorker(pb,isFarmer || (dest.row==TrainWorkerDiscountRow) ? 1 : 0,false,optional,replay);
    		break;
    	case PlayBlueWorker:
    		if(isFarmer)
    		{
    		nextState = ViticultureState.ResolveCard;
    		resolveCard(ViticultureChip.FarmerDollarOr2Blue);
    		}
    		else
    		{
    		switch(dest.row)
			{
			case PlayBlueDollarRow:	
				nextState = ViticultureState.PlayBlueDollar; 
				break;
			case PlayBlueBonusRow:
				if((revision>=122) && (currentWorker.type==ChipType.Politico))
				{	//p1("politico blue extra");
					nextState = ViticultureState.Play2Blue;
					addContinuation(Continuation.PoliticoBlue);
				}
				else if((currentWorker.type==ChipType.Politico)
						&& (pb.cash>0))
				{	// play 1 then offer to pay a dollar to place another, in either case
					// making 2 or 3 cards in all.
					addContinuation(Continuation.PoliticoBlue);
					nextState = ViticultureState.Play1Blue;
				}
				else
				{
				nextState = ViticultureState.Play2Blue; 
				}
				break;
			default: nextState = optional ? ViticultureState.PlaySecondBlue : ViticultureState.Play1Blue; break;
			}}
			break;
  		
    	case PlayYellowWorker:
    		if(isFarmer)
    		{
        		nextState = ViticultureState.ResolveCard;
        		resolveCard(ViticultureChip.FarmerDollarOr2Yellow);   			
    		}
    		else
    		{
    		switch(dest.row)
    			{
    			case PlayYellowDollarRow:	
    				nextState = ViticultureState.PlayYellowDollar; 
    				break;
    			case PlayYellowBonusRow:
    				if((currentWorker.type==ChipType.Politico) && (revision>=122))
    				{	//p1("Politoco extra yellow");
    					addContinuation(Continuation.PoliticoYellow);
    					nextState = ViticultureState.Play2Yellow;
    				}
    				else if((currentWorker.type==ChipType.Politico) && (pb.cash>0))
    				{
    				// play 1 then offer to pay a dollar to place another, in either case
    				// making 2 or 3 cards in all.
    				addContinuation(Continuation.PoliticoYellow);
    				nextState = ViticultureState.Play1Yellow;
    				}
    				else
    				{
    				nextState = ViticultureState.Play2Yellow;
    				}
    				break;
    			default: nextState = optional ? ViticultureState.PlaySecondYellow : ViticultureState.Play1Yellow; break;
    			}
    		}
    			break;
    	case DrawGreenWorker:
    		{
    		boolean bonus = (dest==drawGreenWorkers[DrawGreenBonusRow]);
    		// the number of cards may be increased later because of oracle
    		if(bonus
    			&& (currentWorker!=null)	// can be null if from the manager card
    			&& (currentWorker.type==ChipType.Politico))
    			{	
    			// pay a dollar to take 3 after seeing the first
    			addContinuation(Continuation.PoliticoGreen);
    			}
       		int ncards = (testOption(Option.GreenMarket) ? MarketSize : 0) + (bonus||isFarmer ? 2 : 1);
       		int startn = pb.cards.height();
       		nextState = drawCards(ncards,greenCards,pb,testOption(Option.GreenMarket)?replayMode.Replay:replay,m);
       		// under extreme circumstances, there may not be enough cards
       		int gotncards = pb.cards.height()-startn;
       		if(testOption(Option.GreenMarket))
    			{
  				if(nextState==null)
   				{
  					pb.oracleCards.reInit();
  	   				for(int i=0;i<gotncards;i++) { pb.oracleCards.addChip(pb.cards.removeTop()); }
  	   				nextState = bonus|(revision>=156&&isFarmer) ? ViticultureState.Select2Of2FromMarket : ViticultureState.Select1Of1FromMarket;
   				}
   				else if(nextState==ViticultureState.Discard1ForOracle)
   				{	if(revision>=158)
   					{
 					pb.oracleCards.reInit();
  	   				for(int i=0;i<gotncards;i++) { pb.oracleCards.addChip(pb.cards.removeTop()); }
   					}
   					nextState = bonus ? ViticultureState.Select2Of3FromMarket : ViticultureState.Select1Of2FromMarket;
   				}
   				else { G.Error("Not expecting nextstate %s",nextState); }
    			}
    		}
    		break;
    	case DrawPurpleWorker:
    		//
    		// purple cards can have an unusual after action - filling them
    		// if the mercado is in effect
    		//
    		if((currentWorker!=null)
    				&& (currentWorker.type==ChipType.Politico)
    				&& (dest==drawPurpleWorkers[DrawPurpleBonusRow])
    				&& ((revision>=133) || (pb.cash>0)))
    			{
    	   		// politico pay a dollar to take 3, after seeing the first two
       			addContinuation(Continuation.PoliticoPurple);
    			nextState = drawCards(2,purpleCards,pb,replay,m);
    			}
    			else 
    			{
    			nextState = drawCards(isFarmer||(dest.row==DrawPurpleBonusRow)?2:1,purpleCards,pb,replay,m);
    			}
    		break;
    	case GiveTourWorker:
    		// no impact for politico
    		nextState = giveTour(pb,isFarmer||(dest.row==GiveTourBonusRow) ? 3 : 2,replay);
    		break;
    	case MakeWineWorker:
    		switch(dest.row) {
    		// politico pay a dollar to make 4 wines
    		case MakeWineBonusRow:	
    			if((currentWorker!=null)
    				&& (currentWorker.type==ChipType.Politico)
    				&& (pb.cash>0))
    			{	
    				nextState = ViticultureState.ResolveCard;
    				resolveCard(ViticultureChip.PoliticoWine);
    			}
    			else { nextState = ViticultureState.Make3Wines; }
    			break;
    		default: 
    			nextState = isFarmer
    							? ViticultureState.Make3Wines 
    							: optional ? ViticultureState.Make2WinesOptional : ViticultureState.Make2Wines;
    			break;
    		}
    		break;
    	case BuildTourWorker:
    		// no impact for politico 
    		nextState = isFarmer|(dest.row==BuildTourBonusRow) 
    			? ViticultureState.BuildTourBonus
    			: ViticultureState.BuildTour;	// no need for an optional, you can always take money
    		break;
    	case BuildStructureWorker:
    		// no impact for politico
     		nextState = isFarmer|(dest.row==BuildStructureBonusRow)  
     			? ViticultureState.BuildStructureBonus
     			: optional ? ViticultureState.BuildStructureOptional : ViticultureState.BuildStructure;
    		break;
    	case StarPlacementWorker:
    		// politico pay a dollar to place a third star
    		if(	(currentWorker!=null)
    				&& (dest.row==PlaceStarBonusRow)
    				&& (currentWorker.type==ChipType.Politico))
    		{	//p1("politico star");
    			addContinuation(Continuation.PoliticoStar);
    			nextState = revision>=123? pb.place2StarState() : pb.placeStarState();
    		}
    		else 
    		{
    		nextState = isFarmer|(dest.row==PlaceStarBonusRow)
    						? pb.place2StarState()
    						: pb.placeStarState();
    		}
    		break;
    	case PlantWorker:
    		
    		if((currentWorker!=null)
    				&& (dest.row==Plant2BonusRow)
    				&& (currentWorker.type==ChipType.Politico))
    		{	// plant 1 then offer to pay 1 to plant 2 more
    			//p1("politico plant");
    			addContinuation(Continuation.PoliticoPlant);
    			nextState = revision>=131 ? ViticultureState.Plant2Vines : ViticultureState.Plant1Vine;
    		}
    		else
    		{
    		nextState = isFarmer|(dest.row==Plant2BonusRow)
    						? ViticultureState.Plant2Vines	// must be able to plant at least one
    						: optional ? ViticultureState.PlantSecondVine : ViticultureState.Plant1Vine;
    		}
    		break;
    	case Field:
    		nextState = resolveStructureCard(pb,dest.chipAtIndex(1),replay,m);
    		break;
    	case PlayerStructureCard:
    		// no impact for politico or farmer
    		nextState = resolveStructureCard(pb,dest.chipAtIndex(0),replay,m);
    		break;
        case DestroyStructureWorker:
        	// no impact for politico or farmer
        	nextState = optional ? ViticultureState.DestroyStructureOptional : ViticultureState.DestroyStructure;
        	break;
    	case PlayerYokeWorker:
    		// no impact for politico or farmer
     		if(revision>=114)
    		{
    			if(canHarvest(pb,MoveGenerator.All))
    			{
    		   		//p1("harvest or uproot");
    				nextState = ViticultureState.HarvestOrUproot;
    			}
    			else
    			{
    				//p1("uproot only");
        			nextState = ViticultureState.Uproot;	
    			}
    		}
    		else
    		{
    			if(canHarvest(pb,MoveGenerator.All)) { nextState = ViticultureState.Harvest1; }
    		}

    		break;
    	case HarvestWorker:
    		// politico pay a dollar to harvest 1 more
    		if(isFarmer)
    		{
        		nextState = ViticultureState.ResolveCard;
        		resolveCard(ViticultureChip.FarmerDollarOrHarvest); 			
    		}
    		else
    		if((currentWorker!=null)
    				&& (dest.row==Harvest2BonusRow)
    				&& (currentWorker.type==ChipType.Politico)
    				&& ((revision<155) || (harvestCount(pb)>=3))
    				)
    		{
    		// offer to harvest 3
    		nextState = ViticultureState.ResolveCard;
    		resolveCard(ViticultureChip.PoliticoHarvest);
    		}
    		else
    		{
    		nextState = (dest.row==Harvest2BonusRow) 
    						? ViticultureState.Harvest2Optional 
    						: (dest.row==HarvestDollarBonusRow)
    							? ViticultureState.Harvest1Dollar 
    							: ViticultureState.Harvest1Optional;
    		}
    		break;
    	case TradeWorker:
    		switch(dest.row)
    		{
    		case TradeWorkerBonusRow:
    			// politico pay a dollar to trade 3
    			if((currentWorker!=null)
        				&& (currentWorker.type==ChipType.Politico))
    			{	if(revision>=131)
    				{
    				//p1("Politico trade");
    				addContinuation(Continuation.PoliticoTradeExtra);
    				nextState = ViticultureState.Trade2;
    				}
    				else {
        				addContinuation(Continuation.PoliticoTrade);
        				nextState = ViticultureState.Trade1;   				
    				}
    			}
    			else { nextState = ViticultureState.Trade2; }
    			break;
    		default:
    			nextState = isFarmer 
    							? ViticultureState.Trade2 
    							: optional
    								? ViticultureState.TradeSecond 
    								: ViticultureState.Trade1;
    			break;
    		}
    		break;
    	case FlipWorker:
    		if(isFarmer)
    		{
        		nextState = ViticultureState.ResolveCard;
        		resolveCard(ViticultureChip.FarmerCardOrVP); 			
    		}
    		else 
    		{switch(dest.row)
    		{
    		case FlipWorkerVPRow:
    			// politico pay a dollar for 1 vp, but only if he actually flips
    			if((currentWorker!=null)
    					&& ((revision>=133) || (pb.cash>0))	// only if he as a dollar now. If he uses all his cash to flip a field, he
    									// wont e able to get the second point.  If he doesn't have a dollar now, 
    									// he can't use the cash from selling to buy the extra bonus
    					&& (currentWorker.type==ChipType.Politico))
    				{
        			addContinuation(Continuation.PoliticoVp);   				
    				}
    			changeScore(pb,1,replay,FlipFieldBonus,ViticultureChip.VictoryPoint_1,ScoreType.Other);
    			break;
    		case FlipWorkerCardRow:
    			if((currentWorker!=null)
    					&& ((revision>=133) || (pb.cash>0))	
    									// only if he as a dollar now. If he uses all his cash to flip a field, he
    									// wont e able to get the second point.  If he doesn't have a dollar now, 
    									// he can't use the cash from selling to buy the extra bonus
    					&& (currentWorker.type==ChipType.Politico))
    			{	//p1("politico flip");
    				addContinuation(Continuation.PoliticoStructure);   
    			}
    			ViticultureState next = drawCards(1,structureCards,pb,replay,m);
    			if(revision>=116) { nextState = next; }	// oracle didn't discard one of the cards!
    			break;
    		default: break;
    		}
     		addContinuation(optional ? Continuation.FlipOptional : Continuation.Flip);
    		}
    		break;
        case DollarOrCardWorker:
        	// no impact for politico
        	nextState = ViticultureState.ResolveCard;
        	resolveCard(ViticultureChip.DefaultSpace);        
        	break;

        case SellWineWorker:
        	if(isFarmer)
        	{
        		nextState = ViticultureState.ResolveCard;
        		resolveCard(ViticultureChip.FarmerCardOrStar); 			    		
        	}
        	else
        	{
        	switch(dest.row)
        	{
        	case SellWineStarRow:
        		addContinuation(Continuation.SellWine);
        		// politico pay a dollar to place 2 stars
        		if((revision>=123)
        			&& (currentWorker!=null)
        			&& (currentWorker.type==ChipType.Politico))
        		{	//if(pb.cash==0) { p1("politico wine star (no cash)"); }
        			//if(pb.stars.height()==0) { p1("politico wine star (no stars)"); }
        			//if((pb.stars.height()>0) && (pb.cash==0)) { p1("politico stars (no cash)"); }
        			addContinuation(Continuation.PoliticoStar);
        			nextState = pb.placeStarState();
        		}
        		else if((revision>=105)
        			&& (currentWorker!=null)
        			&& (currentWorker.type==ChipType.Politico)
        			&& (pb.cash>0))
        			{
        			nextState = ViticultureState.ResolveCard;
        			resolveCard(ViticultureChip.PoliticoStar);
        			}
        		else
        		{
        		nextState = pb.placeStarState();
        		}
        		break;
        	case SellWineCardRow:
        		if((currentWorker!=null)
        				&& (currentWorker.type==ChipType.Politico)
        				&& (pb.cash>0))
        		{
               		// politico pay a dollar to draw 2
        			//p1("politico sell");
           			nextState = ViticultureState.ResolveCard;
        			resolveCard(ViticultureChip.PoliticoStructure);
        			// this drawCards can't return a non-null nextstate
        			drawCards(1,structureCards,pb,replay,m); 
        			if(revision<122) { break; }
        		}
        		else
        		{
        		//if(currentWorker!=null && currentWorker.type==ChipType.Oracle) { p1("Sell wine oracle"); }
        		ViticultureState next = drawCards(1,structureCards,pb,replay,m);
        		if(revision>=116) { nextState = next; }	// could be the oracle
        		}
				//$FALL-THROUGH$
			default:
				addContinuation( optional ? Continuation.SellWineOptional : Continuation.SellWine);
				break;
        	}}
        	break;
        case FillWineWorker:
        	switch(dest.row)
        	{
        	case FillWineBonusRow:
        		// politico pay a dollar for 1 more vp
        		
        		if((currentWorker!=null)
        				&& (currentWorker.type==ChipType.Politico)
        				&& (pb.cash>0))
        		{
        			nextState = ViticultureState.ResolveCard;
        			resolveCard(ViticultureChip.PoliticoVP);
            		addContinuation(Continuation.Fill1Mandatory);

        		}
        		else
        		{
               		nextState = ViticultureState.FillWineBonus;               	 
        		}
        		break;
        	default: nextState = isFarmer 	
        							? ViticultureState.FillWineBonus 
        							: optional 
        								? ViticultureState.FillWineOptional 
        								: ViticultureState.FillWine;
        	}
        	break;
    	default: G.Error("Not expecting dest %s",dest.rackLocation());
    	}
    	return(nextState);
    }
    private int StarVps[] = {
    		1,	// lucca
    		1,	//pisa
    		2,	// firenze
    		2,	// livorno
    		2,	// siena
    		2,	// arezzo
    		1	// grosseto
    };
    
    private ViticultureState performPlaceStarActions(ViticultureCell placement,replayMode replay,Viticulturemovespec m)
    {	PlayerBoard pb = getCurrentPlayerBoard();
    	ViticultureState nextState = null;
    	switch(placement.row)
    		{
    		case 0:	// lucca
    			nextState = drawCards(1,structureCards,pb,replay,m);
    			break;
    		case 1:	//pisa
    			changeCash(pb,2,placement,replay);
    			logRawGameEvent("+ $2");
    			break;
    		case 2: // firenze
    			nextState = drawCards(1,blueCards,pb,replay,m);
    			break;
    		case 3:	// livorno
    			nextState = drawCards(1,yellowCards,pb,replay,m);
    			break;
    		case 4:	// siena
    			changeCash(pb,1,placement,replay);
    			// extra spaces because of the spacing of the $1 icon
    			logRawGameEvent("+    $1");
    			break;
    		case 5: // arezzo
    			nextState = drawCards(1,purpleCards,pb,replay,m);
    			break;
       		case 6:  // grossito
       			nextState = drawCards(1,greenCards,pb,replay,m);
    			break;
    		default: G.Error("Not expecting %s",placement);
    		
    		}
    	return(nextState);
    }
    
    private void ageGrapes(PlayerBoard pb,replayMode replay)
    {	ageGrapeRow(pb.redGrape,replay);
    	ageGrapeRow(pb.whiteGrape,replay);
    	if(pb.hasDistiller())
    	{
    		ageGrapeRow(pb.redGrape,replay);
        	ageGrapeRow(pb.whiteGrape,replay);
    	}
    }
    private void ageGrapeRow(ViticultureCell row[],replayMode replay)
    {
    	for(int lim=row.length-1; lim>=1; lim--)
    	{	ViticultureCell src = row[lim-1];
    		ViticultureCell dest = row[lim];
    		if(src.topChip()!=null && dest.topChip()==null)
    		{
    			dest.addChip(src.removeTop());
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(src);
    				animationStack.push(dest);
    			}
    		}
    	}
    }
    private void retrieveAutomaWorkers()
    {	automaWorkers = 0;
       	for(ViticultureCell row[] : mainBoardWorkerPlacements)
    	{ for(ViticultureCell c : row)
    		{
    		 c.reInit();
    		}
    	}
    }
    private void retrieveWorkers(PlayerBoard pb,replayMode replay)
    {
    	for(ViticultureCell row[] : mainBoardWorkerPlacements)
    	{ for(ViticultureCell c : row)
    		{
    		retrieveWorkersFrom(pb,c,replay);
    		}
    	}
    	retrieveWorkersFrom(pb,dollarWorker,replay);
    	for(ViticultureCell c : pb.workerCells) {  retrieveWorkersFrom(pb,c,replay); }
   
    	pb.workers.removeChip(ViticultureChip.GrayMeeple);
    	if(revision>=139)
    	{	pb.workers.sortWorkers();
    	}
    }
    private boolean ageWines(PlayerBoard pb,replayMode replay,CommonMoveStack all,boolean doit)
    {	boolean some = false;
    	some |= ageWineRow(pb,pb.whiteWine,replay,all,doit,false);
    	some |= ageWineRow(pb,pb.redWine,replay,all,doit,false);
    	some |= ageWineRow(pb,pb.roseWine,replay,all,doit,false);
    	some |= ageWineRow(pb,pb.champagne,replay,all,doit,false);
    	return(some);
    }
    
    private boolean age2Wines(PlayerBoard pb,replayMode replay,CommonMoveStack all,boolean doit)
    {	boolean some = false;
    	some |= ageWineRow(pb,pb.whiteWine,replay,all,doit,true);
    	some |= ageWineRow(pb,pb.redWine,replay,all,doit,true);
    	some |= ageWineRow(pb,pb.roseWine,replay,all,doit,true);
    	some |= ageWineRow(pb,pb.champagne,replay,all,doit,true);
    	return(some);
    }

	// special case for state age2once, we can age two adjacent wines
	// but only if both partners are selected.
    private boolean canAgeSelected(PlayerBoard pb)
    {	CellStack selected = pb.selectedCells;
    	for(int i=0,lim=selected.size();i<lim;i++)
    	{
    	ViticultureCell from = selected.elementAt(i);
    	ViticultureCell to = getCell(from.rackLocation(),from.col,from.row+1);
    	if(to.topChip()!=null && !selected.contains(to)) { return(false); }
      	}
    	return(true);
    }

	private boolean canAgeSomeWine(PlayerBoard pb)
    {
    	return(addAgeWineMoves(pb,null,false));
    }
    private boolean addAgeWineMoves(PlayerBoard pb,CommonMoveStack all,boolean doit)
    {	// the elegant solution would be to use a "visitor" pattern, but the simple
    	// solution for only 2 behaviors is to pass an extra argument and do something
    	// different if it is present.
    	return(ageWines(pb,replayMode.Replay,all,doit));
    }
    private boolean addAge2WineMoves(PlayerBoard pb,CommonMoveStack all,boolean doit)
    {	// the elegant solution would be to use a "visitor" pattern, but the simple
    	// solution for only 2 behaviors is to pass an extra argument and do something
    	// different if it is present.
    	return(age2Wines(pb,replayMode.Replay,all,doit));
    }
    private boolean canAge(int offset,ViticultureCell from,ViticultureCell to,boolean hasMedium, boolean hasLarge)
    {
    	if((to.topChip()==null) && (from.topChip()!=null))
		{	if(!hasLarge && to.row+offset==6) {  } // can't cross boundary
			else if(!hasMedium && to.row+offset==3) { }
			else { return(true); }
		}
    	return(false);
    }
    
    private boolean ageWineRow(PlayerBoard pb,ViticultureCell row[],replayMode replay,CommonMoveStack all,boolean doit,boolean age2)
    {	int offset = 9-row.length;
    	boolean hasLarge = pb.largeCellar.topChip()!=null;
    	boolean hasMedium = pb.mediumCellar.topChip()!=null;
    	boolean some = false;
    	boolean canageprev = false;
    	ViticultureCell prevto = null;
    	for(int lim=row.length+offset-1; lim>offset;lim--)
    	{
    		ViticultureCell to = row[lim-offset];
    		ViticultureCell from = row[lim-offset-1];
    		if(canAge(offset,from,to,hasMedium,hasLarge) || (canageprev && canAge(offset,from,prevto,hasMedium,hasLarge)))
    		{	canageprev = age2;
    			prevto = to;
    			if(doit)
    			{
    			to.addChip(from.removeTop());
				if(replay!=replayMode.Replay)
				{
				animationStack.push(from);
				animationStack.push(to);
				}}
    			else if(all==null) { return(true); }
    			else {
    				// generate moves
    				all.push(new Viticulturemovespec(MOVE_AGEONE,from,0,whoseTurn));
    				some = true;
    			}
    		}
    		else { canageprev = false; prevto = null;}
    	}
    	return(some);
    }
	public ViticultureCell ageWine(PlayerBoard pb,ViticultureCell age,replayMode replay)
	{	
		ViticultureCell row[] = null;
		switch(age.rackLocation())
		{
		case RedWine: 
			row = pb.redWine;
			break;
		case WhiteWine:
			row = pb.whiteWine;
			break;
		case RoseWine:
			row = pb.roseWine;
			break;
		case Champaign:
			row = pb.champagne;
			break;
		default: G.Error("Not expecting %s",age);
		}
		int ind = age.row+1;	
		if(ind<row.length)
		{	
		ViticultureCell dest = row[ind];
		if(canAge(9-row.length,age,dest,pb.hasMediumCellar(),pb.hasBothCellars()))
		{
			dest.addChip(age.removeTop());
			if(replay!=replayMode.Replay)
			{
				animationStack.push(age);
				animationStack.push(dest);
			}
			return(dest);
		}}
		return(null);
	}
	
    private void retrieveWorkersFrom(PlayerBoard pb,ViticultureCell c,replayMode replay)
    {	
		for(int lim = c.height()-1; lim>=0; lim--)
			{
			ViticultureChip ch = c.chipAtIndex(lim);
			if(ch.color==pb.color || ((ch.color==ViticultureColor.Gray) && (pb.grayWorker!=null)))
				{
				pb.workers.addChip(c.removeChipAtIndex(lim));
				if(replay!=replayMode.Replay)
					{
					animationStack.push(c);
					animationStack.push(pb.workers);
					}
				}
			}
    }
    //
    // invoke selection of the next wakeup state.  If we've got the grape,
    // it's automatically 0.  Otherwise, return SelectWakeup state.
    //
    private ViticultureState selectWakeup(PlayerBoard pb,replayMode replay)
    {
    	if(pb.isStartPlayer.topChip()!=null) 
			{ 
    		  changeWakeup(pb,getCell(ViticultureId.RoosterTrack,'A',0),replay);
			  pb.isStartPlayer.reInit();
			  return(null);
			}
  		else { return(ViticultureState.SelectWakeup); }
    }
    public int season(PlayerBoard pb)
    {
    	return testOption(Option.ContinuousPlay) ? pb.season() : season;
    }
    //
    // move the current player's rooster to the next season,
    // and award any applicable bonuses
    //
    private ViticultureState passToNextSeason(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	currentWorker = null;	// so oracle doesn't get extra cards
    	if(season(pb)==3)
      	{	// passing out
      		retrieveWorkers(pb,replay);
      		ageGrapes(pb,replay);
      		ageWines(pb,replay,null,true);
      		changeCash(pb,pb.residual,residualTrack[pb.residual],replay);	// pay residuals
      		if(pb.hasStorehouse()) {  ageWines(pb,replay,null,true); };
      		if(automa ? year<7 : (currentMaxScore()<WINNING_SCORE))
      		{
      		addContinuation(Continuation.SelectWakeup);
      		if(pb.cards.height()>7) { nextState = ViticultureState.DiscardCards; }
      		// card residuals after discarding
      		if(pb.hasSilo())
      			{ ViticultureState ns = drawCards(1,greenCards,pb,replay,m);
      			  Assert(ns==null,"shouldn't be a next state");
      			}	
      			if(pb.hasDock()) 
      			{ ViticultureState ns = drawCards(1,purpleCards,pb,replay,m);
      			  Assert(ns==null,"shouldn't be a next state");
      			}
      		}
      		else { 
      			ViticultureCell dcell = getCell(ViticultureId.RoosterTrack,'A',pb.boardIndex);
      			if(dcell.topChip()!=null)
      			{	// if the position is already occupied, find a vacant slot.
      				// this is purely cosmetic, there never will be a wakeup.
      				int idx = roosterTrack.length-1;
      				while( (dcell = getCell(ViticultureId.RoosterTrack,'A',idx)).topChip()!=null) { idx--; }
      				
      			}
      			changeWakeup(pb,
      				dcell,
      				replay); }
      	}
      	else
      	{
      	if((season(pb)==1) && pb.hasBarn())
      		{	// this is the rare case where a barn provides an extra "discard cards" state.
      			// and nextState is used in the normal continuation too.
      			//p1("pass summer in the barn");
      			triggerCard = ViticultureChip.BarnCard;
        		nextState = ViticultureState.Discard2CardsFor1VP;
        	}	
      	// rather than make this a rare occurrence, always do it this way.
  		addContinuation(Continuation.FinishNewSeason);
      	}
      	return(nextState);
    }

    private ViticultureState finishNewSeason(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	// actually move the rooster
      	ViticultureCell current = pb.wakeupPosition;
      	int oldseason = season(pb);
      	int newseason = oldseason+1;
      	ViticultureCell next = getCell(current.rackLocation(),(char)('A'+newseason),current.row);
      	changeWakeup(pb,next,replay);
      	pb.setSeason(newseason);
    	nextState = takeRoosterBonus(pb,replay,m);
    	
     	if((revision>=156) && testOption(Option.ContinuousPlay))
    	{
    		queuePlannerAction(pb);
    	}
    	
    	// handle the cottage
    	if((oldseason==1) && (pb.cottage.topChip()!=null))
    	{	triggerCard = pb.getCottage();
    		addContinuation(Continuation.TakeYellowOrBlue);
    	}
    	
 

    	return(nextState);
    }
    
    private ViticultureState takeRoosterBonus(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
  		ViticultureCell current = pb.wakeupPosition;
  		int newseason = current.col-'A';
  		// note that these changes can be elgible for the oracle bonus
  		// if the oracle was used to play the organizer
    	switch(current.row)
    	{
    	case 0:	
    		// bonuses in top row (there are none)
    		break;
    	case 1: // bonuses row 1 (labeled 2)
    		switch(newseason)
    		{
    		case 1: 
    			changeCash(pb,1,current,replay);
    			break;
    		default:
    		}
    		break;
    	case 2: // bonuses row 2 (labeled 3)
    		switch(newseason)
    		{
    		case 1: 	
    			nextState = drawCards(1,greenCards,pb,replay,m);
    			break;
    		case 2:
    			nextState = drawCards(1,yellowCards,pb,replay,m);
    			break;
    		default:
    		}
    		break;
    	case 3: // bonuses row 3 (labeled 4)
	    	{
	    	switch(newseason)
	    		{
	    		case 1: 
	    			nextState = drawCards(1,purpleCards,pb,replay,m);
	    			break;
	    		case 2:
	    			nextState = drawCards(1,yellowCards,pb,replay,m);
	    			break;
	    		case 3:
	    			nextState = drawCards(1,structureCards,pb,replay,m);
	    			break;
	    		default: ;
	    		}
	    	}
	    	break;
	   	case 4:	// bonuses row 4 (labeled 5)
    		{
    		switch(newseason)
    			{
    			case 1:
    				triggerCard = pb.getRooster();
    				nextState = ViticultureState.TakeYellowOrBlue;
    				break;
    			case 2:
    				nextState = drawCards(1,blueCards,pb,replay,m);
    				break;
    			case 3:
    				nextState = pb.placeStarState();
    				break;
    			default:;
    			}
    		}
    		break;
    	case 5:
    		// bonuses in row 5 (labeled 6)
    		{
    		switch(newseason)
    			{
    			case 1:
    				changeScore(pb,1,replay,WakeupTrack,ViticultureChip.VictoryPoint_1,ScoreType.Other);	// add 1 vp
    				break;
    			case 2:
    				nextState = drawCards(1,blueCards,pb,replay,m);
    				break;
    			case 3:
    				ageGrapes(pb,replay);
    				break;
    			default:;
    			}	
    		}
    		break;
    	case 6:
    		// bonuses in row 6 (labeled 7) 
    		switch(newseason)
    		{
     		case 1: 	
    			if(current.topChip()==ViticultureChip.GrayMeeple) // the meeple doesn't have to be there!
    				{ // the organizer can take the reuse the row if it's played twice
    				 pb.workers.addChip(current.removeTop());	
    	    		 pb.grayWorker = ViticultureChip.GrayMeeple;	// register owner
    														
    	    		 if(replay!=replayMode.Replay)
    	    		 {	animationStack.push(current);
    	    		 	animationStack.push(pb.workers);
    	    		 }
    				}
    			break;
    		case 2:
    			nextState = ViticultureState.TakeCard;
    			break;
    		case 3:
    			//
    			// here's another anomalie - if the mentor is used to play the organizer,
    			// and row 6 is claimed, it may already have been used by another player
    			// to take the first player position for next year.  There's no way to 
    			// resolve this as the player is already in position.
    			// 
    			Assert(current.topChip()==ViticultureChip.StartPlayerMarker,"the first player marker must be there");
    			
    			pb.isStartPlayer.addChip(current.removeTop());  // the grape has to be there
    			if(replay!=replayMode.Replay)
    			{	animationStack.push(current);
    				animationStack.push(pb.isStartPlayer);
    			}
    			break;
    		default: ;
    		}
  
    		break;
    	default: G.Error("Not expecting %s",current.row);
    	}
    	return(nextState);
    }
    private ViticultureCell discardPile(ViticultureChip ch)
    {	for(ViticultureCell pile : discardStacks)
    		{
    		if(pile.contentType==ch.type) { return(pile); } 
    		}
    	throw G.Error("No discard for %s",ch);
    }
    private ViticultureCell cardPile(ViticultureChip ch)
    {	for(ViticultureCell pile : cardStacks)
		{
    	if(pile.contentType==ch.type) { return(pile); } 
		}
    	throw G.Error("No card stack for %s",ch);
    }

    private ViticultureChip discardCard(ViticultureCell from,int index,replayMode replay,String msg)
    {
    	ViticultureChip ch = from.removeChipAtIndex(index);
    	ViticultureCell dest = discardPile(ch);
    	dest.addChip(ch);
    	logGameEvent(msg,ch.type.toString(),ch.description);
    	if(replay != replayMode.Replay)
    	{ 	animationStack.push(from);
    		animationStack.push(dest);
    	}
    	return ch;
    }
	// remove the top card and adjust the indeces of the 
	// cards that are left.  This is a subtle point, cards
	// are discarded in a particular order which has ramifications
	// for the rest of the game as the decks are reshuffled and
	// also with the blue "inkeeper" can be directly acessible.
	//
	public ViticultureChip discardTopSelectedCard(PlayerBoard pb,replayMode replay,String msg) 
	{	CardPointerStack selectedItems = pb.selectedCards;
		Assert(selectedItems.size()>0,"must be some");
		CardPointer top = selectedItems.pop();
		ViticultureChip removed = discardCard(pb.cards,top.index,replay,msg);
		Assert(removed==top.card,"wrong card, is %s should be %s",removed,top.card);
		for(int i=0;i<pb.selectedCards.size();i++)
		{	CardPointer item = selectedItems.elementAt(i);
			if(item.index>top.index) { item.index--; }
		}
		return removed;
	}

    private int doHarvest(PlayerBoard pb,replayMode replay)
    {	CellStack fields = pb.selectedCells;
    	int n = 0;
    	for(int lim=fields.size()-1; lim>=0; lim--)
    	{	ViticultureCell field = fields.elementAt(lim);
    		ViticultureCell vine = pb.vines[field.row];
    		if((vine.height()>0) && (field.topChip()!=ViticultureChip.Bead))
    		{
    		field.addChip(ViticultureChip.Bead);
    		int harvest[] = pb.harvest(vine,replay);
    		logGameEvent(G.concat("+ ",
    								harvest[0]>0 ? "RedGrape " : "",
    								harvest[1]>0 ? "WhiteGrape" : ""));
    		n++;
    		}
    	}
    	fields.clear();
    	if(n>0 && pb.hasFermentationTank())
    	{
    		addContinuation(Continuation.Make1WineOptional);
    	}
    	return(n);
    }
    public void resolveCard(ViticultureChip card,ViticultureChip trigger)
    {	
    	triggerCard = trigger;
    	resolveCard(card);
    }
    public void resolveCard(ViticultureChip card)
    {	cardBeingResolved = card;
     	unselect();
    }
    // user clicks "done" after deploying a blue card.  Decide what to do next
    private ViticultureState playBlueCard(PlayerBoard pb,int index,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	ViticultureChip card = pb.cards.chipAtIndex(index);
    	Assert(card.type==ChipType.BlueCard,"must be a blue card");
		pb.recordEvent("Play Blue",card,ScoreType.PlayBlue);   	
    	discardCard(pb.cards,index,replay,PlayCardMessage);
    	if(pb.hasInn()) { changeCash(pb,1,pb.destroyStructureWorker,replay); }
    	if(pb.hasTapRoom()) { addContinuation(Continuation.DiscardWineFor2VP); }
    	if(pb.hasTavern()) { addContinuation(Continuation.DiscardGrapeFor3VP); }
    	
    	switch(card.order){
    
    	case 11:	// queen
       	case 16:	// promoter
    	case 17:	// mentor
    	case 19:	// innkeeper
    	case 21:	// politician
    	case 22:	// supervisor
    	case 24:	// reaper
    	case 25:	// motivator
    	case 26:	// bottler
    	case 30:	// designer
    	case 32:	// manager
    	case 33:	// zymologist
    	case 35:	// governor
    	case 36:	// taster
    	case 37:	// caravan
    	case 38:	// guest speaker
    		// direct to confirm
    		nextState = resolveBlue(pb,card,ViticultureId.Choice_A,replay,m);
    		break;
    		
    	case 1:	// merchant
    	case 2:	// crusher
    	case 3:	// judge
    	case 4:	// oenologist
    	case 5: // marketer
    	case 6:	// crush expert
    	case 7:	// uncertified teacher
    	case 8:	// teacher
    	case 9:	// benefactor
    	case 10: //assessor
    	case 12: //harvestor
       	case 13: // professor
     	case 14: // master vintner
    	case 15: // uncertified oenologist
    	case 18: // harvest expert
    	case 28: // exporter
    	case 31: // governess
    	case 34: // noble
    		nextState = ViticultureState.ResolveCard;
    		resolveCard(card,card);
    		break;
    		
    	case 27: // craftsman 
    	case 20: // jack of all trades
    		nextState = ViticultureState.ResolveCard_2of3;
    		resolveCard(card,card);
    		break;
      	case 29: // laborer choose a or b or both
       	case 23: // scholar choose a or b or both
       		// only allow both if he's above minumum score
    		nextState = pb.score>MIN_SCORE ? ViticultureState.ResolveCard_AorBorBoth : ViticultureState.ResolveCard;
    		resolveCard(card,card);
    		break;
    	default: G.Error("Can't resolve %s",card);
    	}
    	return(nextState);
    }
    
    // return true if the player can place a worker on this card, and
    // he has the required resources to use it
    private boolean canPlayOnStructureCard(PlayerBoard pb,ViticultureChip card)
    {
       	switch(card.order)
    	{
    	case 1:	// cask, age 1 wine twice
    	case 3:	// wine cave
    		return(canAgeSomeWine(pb));
    	case 4:	// trading post
    		return(true);
    	case 5: // shop
    		return(canFillWineOrder(pb));
    	case 6: // wine press
    	case 12: // cafe
    		return(pb.hasGrape());
    	case 7: // school
    		return(pb.nWorkers<maxWorkers());
    	case 8: // wine bar
    		return(pb.hasWine());
    	case 10: // ristorante
    		return((pb.hasGrape() && pb.hasWine())); 
    	case 11: // guest house
    		return(pb.nVisitorCards()>=2); 
    	case 22: // label factory
    		return((pb.cash>=3) && (canFillWineOrder(pb)));
    	case 32: // mixer
    		return((canMakeBlushWine(pb) || canMakeChampaign(pb)));
    	default:
    		Assert(!card.needsWorker(),"doesn't need a worker");
    		return(false);
    	}
    }
    /*
     * this is unnecessary, since all the structure cards that place a worker
     * have a "bonus" as part of their action
     */
    /*
    private boolean structureHasBonus(ViticultureChip card)
    {
       	switch(card.order)
    	{
    	case 1:	// cask, age 1 wine twice, purple card bonus
    	case 3:	// wine cave, purple card bonus
    	case 4:	// trading post, place * bonus
    	case 5: // shop, place * bonus
    	case 6: // wine press, place * bonus
    	case 7: // school 1$ bonus
     	case 8: // wine bar, 2vp bonus
    	case 10: // ristorante, $3+ 3vp bonus
     	case 11: // guest house. 2p bonus
    	case 12: // cafe, $3+vp bonus
    	case 22: // label factory. 2vp bonus
    	case 32: // mixer, 1VP bonus
    		return(true);

    	default: G.Error("Not expecting %s",card);
    	}
    	return(false);
    }
    */
    private ViticultureState resolveStructureCard(PlayerBoard pb,ViticultureChip card,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	Assert(canPlayOnStructureCard(pb,card),"should be able to place it");
    	switch(card.order)
    	{
    	case 1:	// cask, age 1 wine twice
    		{
    		//p1("Use cask");
    		addContinuation(Continuation.Age1Twice);
    		nextState = drawCards(1,purpleCards,pb,replay,m);
    		//if(nextState!=null) { //p1("cask draw cards with oracle"); }
     		}
    		break;
    	case 3:	// wine cave
    		{
    		//p1("Use wine cave");
    		addContinuation(Continuation.Age2Once);
    		nextState = drawCards(1,purpleCards,pb,replay,m);
    		//if(nextState!=null) { //p1("cask draw wine cave with oracle"); }
    		}
    		break;
    	case 4:	// trading post
    		//p1("Use trading post");
    		nextState = pb.placeStarState();
    		addContinuation(Continuation.Trade1);
    		break;
    	case 5: // shop
			{
			//p1("Use shop");
			nextState = pb.placeStarState();
			addContinuation(Continuation.Fill1Optional);
			}
			break;
    	case 6: // wine press
    		{
			//p1("Use wine press");
			nextState = pb.placeStarState();
			addContinuation(Continuation.Make2WinesOptional);
			}
			break;
    	case 7: // school
    		{
			//p1("Use school");
			changeCash(pb,1,yokeCash,replay);
			// school's discount is just for the base, academy still applies
			// gets a worker for free and a dollar
			triggerCard = ViticultureChip.SchoolCard;
			//p1("use school");
			nextState = doTrainWorker(pb,BASE_COST_OF_WORKER,true,false,replay);
			}
    		break;
    	case 8: // wine bar
    		{
    		//p1("Use wine bar");
    		triggerCard = ViticultureChip.WineBarCard;
    		nextState = ViticultureState.DiscardWineFor2VP;
    		}
    		break;
    	case 10: // ristorante
    		{ 	//p1("Use ristorante");
    			triggerCard = ViticultureChip.RistoranteCard;
    			nextState = ViticultureState.DiscardGrapeAndWine;
    		}
    		break;
    	case 11: // guest house
    		{
    		// p1("Use guest house");
    		triggerCard = ViticultureChip.GuestHouseCard;	
			nextState = ViticultureState.Discard2CardsFor2VP;
    		}
    		break;
    	case 12: // cafe
    		{
    		//p1("Use cafe year ");
    		triggerCard = ViticultureChip.CafeCard;
    		nextState = ViticultureState.DiscardGrapeFor3And1VP;
    		}
    		break;
    	case 22: // label factory
			{
	    		//p1("Use label factory");
				triggerCard = ViticultureChip.LabelFactoryCard;
				nextState = ViticultureState.FillWineFor2VPMore;
			}
			break;
    	case 32: // mixer
    		{
    		//p1("use mixer");
    		triggerCard = ViticultureChip.MixerCard;
    		nextState = ViticultureState.MakeMixedWinesForVP;
    		}
    		break;
    	/*	
    	case 34: // statue, 1vp per year
    	case 33: // storehouse, age wines every year
    	case 31: // fountain, gain $ when opponents give tour
    	case 30: // penthouse, vp when making wine
    	case 29: // banquet hall, gain bonuses for moving stars
    	case 28: // tavern, discard grapes for vp when playing visitor cards
    	case 27: // tap room, discard wine for vp when playing visitor cards
    	case 26: // inn, $1 when playing visitor cards
    	case 25: // charmat, make champagne cheap
    	case 24: // fermentation tank, make wine after harvest
    	case 23: // harvest machine, always harvest all
    	case 21: // wine parlor, +2 when fill wine order		
    	case 20: // veranda extra vp for fill wine order	
    	case 19: // workshop discount when building
    	case 18: // gazebo, place * after a tour
    	case 17: // academy, opponents pay extra for workers
    	case 16: // barn, discard cards for vp
    	case 15: // studio game 1vp for structure built	
    	case 14: // mercado fill wine orders immediately
    	case 13: // distiller age grapes every year
    	case 9: // patio - get cash when making wines
    	case 2:	// aqueduct, no structures for planting
    		G.Error("Card %s doesn't require a worker",card);
    		break;
    	*/
    	default: G.Error("Not expecting %s",card);
    	}
    	return(nextState);
    }
    private ViticultureState resolveYellow(PlayerBoard pb,ViticultureChip card,ViticultureId resolution,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	triggerCard = card;
    	if(resolution!=ViticultureId.Choice_0)
    	{
    	switch(card.order)
    	{
       	case 1:	// surveyor
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("surveyor a");
       			changeCash(pb,pb.nEmptyFields()*2,yokeCash,replay);
       			break;
       		case Choice_B:
           		//p1("surveyor b");
       			changeScore(pb,pb.nPlantedFields(),replay,Surveyor,card,ScoreType.ScoreYellow);
       			break;
       		default: ;
       		}
    		break;
       	case 2:	// broker
       		switch(resolution)
       		{
       		case Choice_A:
           		//Continuation.p1("broker a");
       			changeCash(pb,-9,yokeCash,replay);
    			changeScore(pb,3,replay,BrokerBuy,card,ScoreType.ScoreYellow);
    			break;
       		case Choice_B:
           		//p1("broker b");
       			changeScore(pb,-2,replay,BrokerSell,card,ScoreType.ScoreYellow);
    			changeCash(pb,6,yokeCash,replay);
       			break;
       		default: ;
       		}
    		break;
    		
       	case 3:	// wine critic
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("wine critic a");
       			nextState = drawCards(2,blueCards,pb,replay,m);
    			break;
       		case Choice_B:
           		//p1("wine critic b");
       			nextState = ViticultureState.DiscardWineFor4VP;
       			break;
    		default: ;
    		}
    		break;
     	
       	case 4:	// blacksmith
       		//p1("blacksmith a");
       		nextState = ViticultureState.BuildAtDiscount2;
       		break;
       		
     	case 5: // contractor, choose 2 of 3
    		switch(resolution)
    		{
    		case Choice_AandB:
       			//p1("contractor aandb");
       			changeScore(pb,1,replay,Contractor,card,ScoreType.ScoreYellow);
       			nextState = ViticultureState.BuildStructureOptional;
     			break;
    		case Choice_AandC:
       			//p1("contractor aandc");
       			changeScore(pb,1,replay,Contractor,card,ScoreType.ScoreYellow);
      			nextState = ViticultureState.Plant1VineOptional;
    			break;
    		case Choice_BandC:
       			//p1("contractor bandc");
       			nextState = ViticultureState.BuildAndPlant;
    			break;
    		case Choice_A:
       			//p1("contractor a");
       			changeScore(pb,1,replay,Contractor,card,ScoreType.ScoreYellow);
     			break;
    		case Choice_B:
       			//p1("contractor b");
       			nextState = ViticultureState.BuildStructureOptional;
    			break;
    		case Choice_C:
       			//p1("contractor c");
       			nextState = ViticultureState.Plant1VineOptional;
    			break;
    		default: break;
    		}
    		break;
      	case 6:	// tour guide
      		switch(resolution)
      		{
      		case Choice_A:
           		//p1("tour guide a");
    			changeCash(pb,4,yokeCash,replay);
      			break;
      		case Choice_B:
           		//p1("tour guide b");
      			nextState = ViticultureState.Harvest1Optional;
      			break;
      		default: ;
      		}
      		break;
       		
       	case 7:	// novice guide
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("novice guide a");
       			changeCash(pb,3,yokeCash,replay);
       			break;
       		case Choice_B:
           		//p1("novice guide b");
       			nextState = ViticultureState.Make2WinesOptional;
       			break;
       		default: ;
       		}
       		break;
    		
       	case 8:	// uncertified broker
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("uncertified broker a");
    			changeScore(pb,-3,replay,UncertifiedBroker,card,ScoreType.ScoreYellow);
    			changeCash(pb,9,yokeCash,replay);
       			break;
       		case Choice_B:
           		//p1("uncertified broker b");
      			changeCash(pb,-6,yokeCash,replay);
    			changeScore(pb,2,replay,UncertifiedBroker,card,ScoreType.ScoreYellow);
    			break;
    		default: ;
       		}
       		break;
	    		
	   	case 9:
	   		switch(resolution)
	   		{
	   		case Choice_A:
	       		//p1("planter a");
	   			changeCash(pb,1,yokeCash,replay);
				nextState = ViticultureState.Plant2VinesOptional;
				break;
	   		case Choice_B:
	       		//p1("planter b");
	   			if(revision<124) { changeScore(pb,2,replay,Planter,card,ScoreType.ScoreYellow); }
				nextState = ViticultureState.Uproot1For2;
				break;
			default: ;
	   		}
    		break;
    		
       	case 10:	// buyer
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("buyer a");
           		resolveCard(ViticultureChip.BuyersBuy,card);
           		nextState = ViticultureState.ResolveCard;
     			break;
       		case Choice_B:
           		//p1("buyer b");
           		resolveCard(ViticultureChip.BuyersDiscard,card);
           		nextState = ViticultureState.ResolveCard;
    			break;
    		default: ;
       		}
       		break;
    		
       	case 11:	// landscaper
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("landscaper a");
       			addContinuation(Continuation.Plant1VineOptional);
       			nextState = drawCards(1,greenCards,pb,replay,m);
    			break;
       		case Choice_B:
           		//p1("landscaper b");
       			Assert(hasSwitchVineMoves(pb),"should have switch vines available");
       			nextState = ViticultureState.SwitchVines;
       			break;
       		default: ;
       		}
       		break;
     		
       	case 12:	// architect
       		switch(resolution)
       		{
       		case Choice_A:
       			//p1("architect a");
    			nextState = ViticultureState.BuildStructureDiscount3;
    			break;
       		case Choice_B:
       			{
       			int n = pb.nStructuresWithValueExactly(4);
           		//p1("architect b "+n);
       			changeScore(pb,n,replay,Architect,card,ScoreType.ScoreYellow);
       			}
       			break;
    		default: ;
       		}
       		break;
       		
     	case 13: // uncertified architect
     		switch(resolution)
     		{
     		case Choice_A:
           		//p1("uncertified architect a");
     			changeScore(pb,-1,replay,UncertifiedArchitect,card,ScoreType.ScoreYellow);
    			nextState = ViticultureState.BuildStructure23Free;
    			break;
     		case Choice_B:
           		//p1("uncertified architect b");
    			changeScore(pb,-2,replay,UncertifiedArchitect,card,ScoreType.ScoreYellow);
    			nextState = ViticultureState.BuildStructureFree;
    			break;
    		default:;
    		}
     		break;
     		
       	case 14:	// patron
       		switch(resolution)
       		{
       		case Choice_A:
           		//p1("patron a");
    			changeCash(pb,4,yokeCash,replay);
       			break;
       		case Choice_B:
           		//p1("patron b");
       			if(drawForOracle(pb,replay,m))
           		{	pb.oracleColors = new ViticultureCell[] {blueCards,purpleCards};
           			reshuffleIfNeeded(pb.oracleColors);
            		nextState = ViticultureState.SelectCardColor;
            		//p1("oracle patron");
           		}
           		else
           		{
    			drawCard(purpleCards,pb,replay,m);
    			drawCard(blueCards,pb,replay,m);
           		}
    			break;
    		default: ;
       		}
    		break;
    		
    	case 15: // auctioneer
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("auctioneer a");
    			nextState = ViticultureState.Discard2CardsFor4;
    			break;
    		case Choice_B:
           		//p1("auctioneer b");
    			nextState = ViticultureState.Discard4CardsFor3;
    			break;
    		default: ;
    		}
    		break;
    		
    	case 16:	// entertainer
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("entertainer a");
     			changeCash(pb,-4,yokeCash,replay);
    			nextState = drawCards(3,blueCards,pb,replay,m);
    			break;
    		case Choice_B:
           		//p1("entertainer b");
    			nextState = ViticultureState.Discard3CardsAnd1WineFor3VP;
    			break;
    		default: ;
    		}
    		break;
    		
    	case 17:	// vendor
    		
       		//p1("vendor a");
    		if(drawForOracle(pb,replay,m))
       		{	pb.oracleColors = new ViticultureCell[] {greenCards,blueCards,purpleCards};
       			reshuffleIfNeeded(pb.oracleColors);
        		nextState = ViticultureState.SelectCardColor;
        		//p1("oracle vendor");
       		}
       		else
       		{
       		drawCard(greenCards,pb,replay,m);
    		drawCard(purpleCards,pb,replay,m);
    		drawCard(blueCards,pb,replay,m);
       		}
    		for(PlayerBoard p : pbs)
    		{
    			if(pb!=p) { drawCard(yellowCards,p,replay,m); }
    		}
    		break;
    		
    	case 18:	// handyman
       		//p1("handyman a");
    		//G.print("Start handyman");
    		targetPlayer = whoseTurn;
    		nextState = resetState = ViticultureState.BuildAtDiscount2forVP;
    		if(revision>=134)
    		{
    			while(!pb.canBuildStructureWithDiscount(2) && (nextState!=null))
    			{	
    				nextState = setNextPlayerInCycle();
    				pb = pbs[whoseTurn];
    			};
    		}
    		break;
       		
       		
    	case 19:	// horticulturist
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("horticulturist a");
        		nextState = ViticultureState.Plant1VineNoStructures;
        		break;
    		case Choice_B:
           		//p1("horticulturist b");
    			nextState = ViticultureState.Uproot2For3;
    			break;
    		default: ;
    		}
    		break;

    	case 20: // peddler
       		//p1("peddler a");
    		nextState = ViticultureState.Discard2CardsForAll;
    		break;
    		
    	case 21: // banker
       		//p1("banker a");
       		changeCash(pb,5,yokeCash,replay);
       		String msg = pb.getRooster().colorPlusName()+" + $5 ";
       		logRawGameEvent(msg);
       		if(players_in_game>1)
       		{
    		nextState = resetState = ViticultureState.Sell1VPfor3;
    		//G.print("Start sell1for3");
    		targetPlayer = whoseTurn;
    		resolveCard(ViticultureChip.BankersGift,card);	// resolve banker
    		whoseTurn = (whoseTurn+1)%players_in_game;
    		pb = pbs[whoseTurn];
    		if(revision>=134)
    		{
    			while((pb.score<=MIN_SCORE) && (nextState!=null))
    			{	
    				nextState = setNextPlayerInCycle();
    				pb = pbs[whoseTurn];
    			}
    			
    		}    		
       		}
    		break;
    		
    	case 22: // overseer
       		//p1("overseer a");
    		nextState = ViticultureState.BuildStructureForBeforePlant;
    		break;

    	case 23: // importer, combine to offer 3 visitor cards
    		//TO DO: importer needs a UI to decide (importer is removed for now)
    		nextState = drawCards(3,blueCards,pb,replay,m);
    		break;
    	case 24:	// sharecropper
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("sharecropper a");
    			nextState = ViticultureState.Plant1VineNoStructures;
    			break;
    		case Choice_B: 
    			//p1("sharecropper b");
           		nextState = ViticultureState.Uproot1For2;
    			break;
    		default: ;
    		}
    		break;
    	case 25:	// grower
    		nextState = ViticultureState.Plant1For2VPVolume;
    		break;
    		
    	case 26:	// negotiator
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("negotiator a");
           		resolveCard(ViticultureChip.NegotiatorDiscardGrape,card);
    			nextState = ViticultureState.ResolveCard;
    			break;
    		case Choice_B:
           		//p1("negotiator b");
          		resolveCard(ViticultureChip.NegotiatorDiscardWine);
    			nextState = ViticultureState.ResolveCard;
     		break;
    		default: ;
    		}
    		break;
    	case 27: // cultivator
       		//p1("cultivator a");
    		nextState = ViticultureState.Plant1VineNoLimit;
    		break;
    	case 28: // homesteader
    		switch(resolution)
    		{
       		case Choice_AandB:
       			//p1("homesteader choice a and b");
       			changeScore(pb,-1,replay,Homesteader,card,ScoreType.ScoreYellow);
    			addContinuation(Continuation.Plant2VinesOnly);
				//$FALL-THROUGH$
       		case Choice_A:
    			//p1("homesteader choice a");
    			nextState = ViticultureState.BuildStructureDiscount3;
    			break;
			case Choice_B:
    			//p1("homesteader choice b");
    			nextState = ViticultureState.Plant2VinesOptional;
    			break;
    		default: break;
    		}
    		break;
    	case 29: // planner 
    		//p1("planner a");
    		// if he has no workers, or there is no future, just pass him out.
    		// the blue card "manager" can result in this being played in the winter!
    		if((season(pb)<3) && (pb.workers.topChip()!=null))
    			{ nextState = ViticultureState.PlaceWorkerFuture; 
    			}
    		break;
    	case 30: // agriculturist
      		//p1("agriculturist a");
      		nextState = ViticultureState.Plant1For2VPDiversity;
    		break;
    		
    	case 31: // swindler
       		//p1("swindler a");
    		nextState = ViticultureState.Give2orVP;
    		//G.print("Start swindler");
    		targetPlayer = whoseTurn;
    		whoseTurn = (whoseTurn+1)%players_in_game;
    		resolveCard(ViticultureChip.Swindled);
    		break;

    	case 32: // producer
       		//p1("producer a");
       		changeCash(pb,-2,yokeCash,replay);
         	nextState = ViticultureState.Retrieve2Workers;
        	break;
    	case 33: // organizer
       		//if((year==6)) { p1("organizer a"); }
   		if(canPickNewRow(pb)) 
    			{ nextState = ViticultureState.PickNewRow; 
           			if(automa)
           			{	// special case for the automa, using the organizer to escape row 7
           				// you have to take position 1 next time "anyway"
           				// the test for "isStartPlayer" is the highly unusual case where
           				// the manager (blue) is used to play organizer (yellow) when the 
           				// grape has already been awarded
           				if((pb.wakeupPosition.row==6) && (pb.isStartPlayer.topChip()==null))
           				{	ViticultureCell current = roosterTrack[6][3];
           					if(current.topChip()!=null)	// better be there!
           					{
           						pb.isStartPlayer.addChip(current.removeTop());
           	    			if(replay!=replayMode.Replay)
           	    			{	animationStack.push(current);
           	    				animationStack.push(pb.isStartPlayer);
           	    			}
           				}
           			
           			}}		
    			}
    		else 
    			{
    			//p1("Organizer can't move starup");
       			nextState = ViticultureState.FullPass; 
       			}
    		break;
    	case 34: // sponsor
    		switch(resolution)
    		{
    		case Choice_A:
    		case Choice_AandB:
           		//p1("sponsor a");
    			nextState = drawCards(2,greenCards,pb,replay,m);
    			break;
    		default: ;
    		}
    		switch(resolution)
    		{
    		case Choice_AandB:
           		//p1("sponsor aandb");
           		changeScore(pb,-1,replay,Sponsor,card,ScoreType.ScoreYellow);
				//$FALL-THROUGH$
			case Choice_B:
	       		//p1("sponsor b");
	       		changeCash(pb,3,yokeCash,replay);
     			break;
	   		default: ;
    		}
    		break;
    	case 35: // artisan
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("artisan a");
    			changeCash(pb,3,yokeCash,replay);
    			break;
    		case Choice_B:
           		//p1("artisan b");
    			nextState = ViticultureState.BuildStructureBonus;
    			break;
    		case Choice_C:
           		//p1("artisan c");
    			nextState = ViticultureState.Plant2VinesOptional;
    			break;
    		default: ;
    		}
    		break;
    		
    	case 36: // stonemason
       		//p1("stonemason a");
       		if(resolution==ViticultureId.Choice_A)
       		{
    		changeCash(pb,-8,yokeCash,replay);
    		nextState = ViticultureState.Build2StructureFree;
       		}
    		break;
    		
    	case 38: // wedding party, pay $2 each gain 1vp		
    		nextState = ViticultureState.PayWeddingParty;
    		break;
    	case 37: // volunteer crew all plant for $2 each
    		targetPlayer = whoseTurn;
    		//p1("Start volunteer crew");
    		nextState = resetState = ViticultureState.Plant1AndGive2;
    		if(revision>=134)
    		{
    			while ((nextState!=null) && !canPlant(pb))
    			{ 	//p1("first player can't");
    				//G.print("Skip "+pb);
    			    nextState = setNextPlayerInCycle();
    				pb = pbs[whoseTurn];
    			}
    		}
    		break;
     	default: G.Error("Can't handle resolve card %s %s",card,resolution);
    	}}
    	return(nextState);
    }
    
    private void doStudio(PlayerBoard pb,replayMode replay)
    {
    	if(pb.hasStudio())
		{	// do this before we build, so it won't count itself
			//p1("use studio");
			changeScore(pb,1,replay,FromStudio,ViticultureChip.Studio,ScoreType.OrangeCard);
		}
    }
    // upgrade a cellar, return the normal cost, or 0 if no upgrade is possible
    // does not consider the workshop discount if applicable
    private int upgradeCellar(PlayerBoard pb,boolean doit, replayMode replay)
    {
    	if(pb.mediumCellar.topChip()==null)
		{	if(doit) 
				{pb.mediumCellar.addChip(pb.getContent(pb.mediumCellar));
				 if(revision>=131) 
				 	{ doStudio(pb,replay); }
				}
			return(pb.mediumCellar.cost);
		}
		else if(pb.largeCellar.topChip()==null)
			{	if(doit) 
					{pb.largeCellar.addChip(pb.getContent(pb.largeCellar));
					 if(revision>=131) 
					 	{ doStudio(pb,replay); }
					}
				return(pb.largeCellar.cost);
    		}
    	return(0);
    }
    private ViticultureState resolveChoice(PlayerBoard pb,ViticultureChip card,ViticultureId resolution,replayMode replay,Viticulturemovespec m)
    {
    	ViticultureState nextState = null;
    	Assert(card.type==ChipType.ChoiceCard,"must be a choice card");

    	switch(card.order)
    	{
    	case 1:	// yoke
    		switch(resolution)
    		{
    		case Choice_A:
           		//p1("yoke a");
    			changeCash(pb,1,yokeCash,replay);
    			break;
    		case Choice_B:
           		//p1("yoke b");
    			nextState = drawCards(1,structureCards,pb,replay,m);
    			break;
    		default: ;
    		}    		
    		break;
    	case 2: // swindler
    		{
    		PlayerBoard anchor = pbs[targetPlayer];
    		switch(resolution)
    		{
    		case Choice_A:
    			{
    			//p1("swindler choice a");
    			boolean nogain = testOption(Option.LimitPoints) && ((anchor.cash-anchor.startingCash)>=6);
    			changeCash(pb,-2,anchor.cashDisplay,replay);
    			if(nogain)
    				{logGameEvent(NoCash);
    				}
    			else 
    				{
        			changeCash(anchor,2,anchor.cashDisplay,replayMode.Replay);
    				String to = pbs[targetPlayer].getRooster().colorPlusName();
    				String msg = G.concat(pb.getRooster().colorPlusName()," : ",to," $2"); 
    				logRawGameEvent(msg);
    				}
    			}
    			break;
    		case Choice_B:
    			{
    			//p1("swindler choice b");
    	   			if(testOption(Option.LimitPoints) && ((anchor.score-anchor.startingScore)>=3))
    	   			{
    	   				logGameEvent(NoVP);
    	   			}
    	   			else
    	   			{
    	   				changeScore(anchor,1,replay,Swindler,card,ScoreType.ScoreYellow);
    	    			String to = pbs[targetPlayer].getRooster().colorPlusName();
    	    			String msg = G.concat(pb.getRooster().colorPlusName()," : ",to," 1VP"); 
    	           		logRawGameEvent(msg);
    	   			}
       			}
    			break;
    		default:
    		}
    		nextState = setNextPlayerInCycle();
    		pb = pbs[whoseTurn];
    		}
    		break;
    	case 3:	// banker
    		if(resolution==ViticultureId.Choice_A)
    		{
    			changeScore(pb,-1,replay,Banker,card,ScoreType.ScoreYellow);
    			changeCash(pb,3,yokeCash,replay);
    		}
    		break;
    	case 4: // buyers bonus gain grape
    		{
    			changeCash(pb,-2,yokeCash,replay);
        		switch(resolution)
        		{
        		case Choice_A:
        			//p1("buyer add red grape");
        			placeGrapeOrWine(pb.redGrape,ViticultureChip.RedGrape,0,replay);
        			break;
        		case Choice_B:
        			//p1("buyer add white grape");
        			placeGrapeOrWine(pb.whiteGrape,ViticultureChip.WhiteGrape,0,replay);
        			break;
        		default:
        		}
    		}
    		break;
    	case 5: // buyers bonus sell grape
    		{
        		switch(resolution)
        		{
        		case Choice_A:
        			//p1("buyer sell red grape");
        			removeTop(pb.redGrape[pb.leastRedGrapeValue()-1],replay);
        			break;
        		case Choice_B:
        			//p1("buyer sell white grape");
        			removeTop(pb.whiteGrape[pb.leastWhiteGrapeValue()-1],replay);
        			break;
        		default:
        		}
        		changeCash(pb,2,yokeCash,replay);
        		changeScore(pb,1,replay,BuyerSellGrape,card,ScoreType.ScoreYellow);

    		}
    		break;
    	case 6: // negotiator sells grape
    		switch(resolution)
    		{
    		case Choice_A:	
    			removeTop(pb.redGrape[pb.leastRedGrapeValue()-1],replay);
    			break;
    		case Choice_B:
       			removeTop(pb.whiteGrape[pb.leastWhiteGrapeValue()-1],replay);
       			break;
    		default: break;
    		}
    		changeResidual(pb,1,replay);
    		break;
    	case 7: // negotiator sell wine
    		{	switch(resolution)
    			{
    			case Choice_A:
    				removeTop(pb.redWine[pb.leastRedWineValue()-1],replay);
    				break;
    			case Choice_B:
    				removeTop(pb.whiteWine[pb.leastWhiteWineValue()-1],replay);
    				break;
    			case Choice_C:
    				removeTop(pb.roseWine[pb.leastRoseWineValue()-4],replay);
    				break;
    			case Choice_D:
    				removeTop(pb.champagne[pb.leastChampaignValue()-7],replay);
    				break;
    			default: break;
    			}
    		}
    		changeResidual(pb,2,replay);
    		break;
    	case 8:	// uncertified teacher, guest worker training, guest speaker training
    		{
    		addContinuation(Continuation.TrainGuestWorker);
    		switch(resolution)
    		{	case Choice_A:
       			if(whoseTurn!=targetPlayer) 
       				{
       				PlayerBoard ap = pbs[targetPlayer];
       				if(testOption(Option.LimitPoints) && ((ap.score-ap.startingScore)>=3))
       				{
       					logGameEvent(NoVP);
       				}
       				else
       				{
       				changeScore(ap,1,replay,UncertifiedTeacher,card,ScoreType.ScoreBlue); 
       				}}
    			nextState = doTrainWorker(pb,3,false,false,replay);
     			break;
    		default:
    			break;
    		}}
    		break;
    	case 9:  // governor: lose a yellow or give a vp
    		if(pb.hasCard(ChipType.YellowCard)) 
    			{ nextState = ViticultureState.GiveYellow; 
  			  	  logRawGameEvent(pb.getRooster().colorPlusName()+" : "+pbs[targetPlayer].getRooster().colorPlusName()+" ScoreYellow");
    			}
    		else 
    			{ changeScore(pbs[targetPlayer],1,replay,Governor,card,ScoreType.ScoreBlue);
    			  logRawGameEvent(pb.getRooster().colorPlusName()+" : "+pbs[targetPlayer].getRooster().colorPlusName()+" 1VP");
    			}
    		break;
    	case 10: // retrieve grande
    		{
    		switch(resolution)
    		{
    		case Choice_A:
    			ViticultureChip grande = pb.getGrandeWorker();
    			boolean found = false;
    			for(ViticultureCell c : pb.workerCells) { found |= retrieveGrande(pb,c,grande,replay); }
    			for(ViticultureCell c = allCells; !found && c!=null; c=c.next)
    			{
    				found |= retrieveGrande(pb,c,grande,replay);
    			}
    			
    			Assert(found,"Didn't find the grande");
    			
    			break;
    		default: break;
    		}
    		do 	{ nextState = setNextPlayerInCycle();
    			  pb = pbs[whoseTurn];  	
    			} while ( (revision>=131)
    					  && (nextState==ViticultureState.ResolveCard) 
    					  && pb.workers.containsChip(pb.getGrandeWorker()));
    		}
    		break;
    	case 11:	// politico vp $1 for 1VP, then make wine
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    		changeCash(pb,-1,yokeCash,replay);
    		changeScore(pb,1,replay,PoliticoBonus,card,ScoreType.Other);
    		}
    		break;
    	case 12:	// politico green $1 for 1 green card
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    		changeCash(pb,-1,yokeCash,replay);
    		drawCards(1,greenCards,pb,replay,m);
    		//p1("politico draw extra green");
    		}
     		break;
    	case 13:	// politico structure $1 for 1 structure card
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    		//p1("politico extra structure card");
    		changeCash(pb,-1,yokeCash,replay);
    		drawCards(1,structureCards,pb,replay,m);
    		}
    		//else { p1("politico extra structure declined"); }
    		if(revision<122) { nextState = ViticultureState.Sell1Wine; }
    		break;
    	case 14:	// politico yellow $1 for 1 yellow card
    		currentWorker = null;	// avoid repeating the choice
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Play2Yellow;
    		//p1("politico play extra yellow");
    		}
    		else
    		{
    		nextState = ViticultureState.Play1Yellow;
    		}
    		break;
    	case 15:	// politico purple $1 for 1 purple card
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    		//p1("politico draw extra purple");
    		changeCash(pb,-1,yokeCash,replay);
    		drawCards(1,purpleCards,pb,replay,m);
    		}
 
    		break;
    	case 16:	// politico blue $1 for 1 blue card
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico play extra blue");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Play2Blue;
    		}
       		else if(revision>=119) { nextState = ViticultureState.Play1Blue; }
       		// this is invoked between card 1 and 2.  he gets 1 more in any case
    		if(revision<118) { drawCards(1,blueCards,pb,replay,m); }
 
    		break;
    	case 17:	// politico make extra wine
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico make extra wine");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Make4WinesOptional;
    		}
       		else
       		{
       			nextState = ViticultureState.Make3Wines;
       		}
       		break;
    	case 18:	// politico place extra star
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico place extra star");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = pb.place2StarState();
    		}
       		else
       		{
       			nextState = pb.placeStarState();
       		}
       		break;
       		
    	case 19:	// politico plant an extra vine
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico plant extra vine");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Plant2VinesOptional;
    		}
       		else
       		{
       			nextState = ViticultureState.PlantSecondVine;
       		}
       		break;
       		
    	case 20:	// politico harvest 3
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico harvest 3");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Harvest3Optional;
    		}
       		else
       		{
       			nextState = ViticultureState.Harvest2Optional;
       		}
       		break;
		
    	case 21:	// politico trade 3
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico trade 3");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Trade2;
    		}
       		else
       		{
       			nextState = ViticultureState.Trade1;
       		}
       		break;
       		
    	case 22:	// farmer dollar "Farmer DollarOr2Blue",
    		//p1("Farmer DollarOr2Blue");
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    			nextState = ViticultureState.Play2Blue;
    		}
    		else {
    			changeCash(pb,1,yokeCash,replay);
    			nextState = ViticultureState.Play1Blue;
    		}
       		break;
    	case 23:	//"Farmer DollarOr2Yellow",	
    		//p1("Farmer DollarOr2Yellow");
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    			nextState = ViticultureState.Play2Yellow;
  			
    		}
    		else {
    			changeCash(pb,1,yokeCash,replay);
    			nextState = ViticultureState.Play1Yellow;
    		}
      		break;
    	case 24:	//"Farmer DollarOrHarvest",	
    		//p1("Farmer DollarOrHarvest");
    		if(cardResolution==ViticultureId.Choice_A) 
    		{	// harvest exactly 2 fields
    			nextState = ViticultureState.Harvest2Optional;
    		}
    		else {
       			changeCash(pb,1,yokeCash,replay); 			
    			nextState = ViticultureState.Harvest1;
    		}
   		
       		break;
    	case 25:	//"Farmer CardOrVP",	//25
    		//p1("Farmer CardOrVP");
    		if(cardResolution==ViticultureId.Choice_A) 
    		{
    			changeScore(pb,1,replay,FarmerBonus,card,ScoreType.Other);
    		}
    		else {
    			drawCards(1,structureCards,pb,replay,m);
    		}
    		nextState = ViticultureState.Flip;
   		
       		break;
    	case 26:	//"Farmer CardOrStar",	/
    		//p1("Farmer CardOrStar");
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
           		addContinuation(Continuation.SellWine);
        		nextState = pb.placeStarState();
    		}
       		else {
        		drawCards(1,structureCards,pb,replay,m); 		
        		nextState = ViticultureState.Sell1Wine;
    		}
    		break;
       	case 27:
       		// politico blue $1 for 1 blue card, revised to occur after the blue cards are played
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico play extra blue");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Play1Blue;
    		}
       		else  { nextState = null; }
 
    		break;
       	case 28:
       		// politico yellow $1 for 1 yellow card, revised to occur after the blue cards are played
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico play extra blue");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = ViticultureState.Play1Yellow;
    		}
       		else  { nextState = null; }
 
    		break;
       	case 29:	
       		// politico place extra star.  Ask after placing the first star, since
       		// that can increase cash
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
       		//p1("politico place extra star");
    		changeCash(pb,-1,yokeCash,replay);
    		nextState = pb.placeStarState();
    		}
       		else
       		{
       			nextState = null;
       		}
       		break;
       	case 30:	
       		// politico trade once more.
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
        		changeCash(pb,-1,yokeCash,replay);
       			nextState = ViticultureState.Trade1;
    		}
       		else
       		{
       			nextState = null;
       		}
       		break;
       	case 31:	
       		currentWorker = null;	// avoid offering the choice again
       		if(cardResolution==ViticultureId.Choice_A) 
    		{
        		changeCash(pb,-1,yokeCash,replay);
       			nextState = ViticultureState.Plant1VineOptional;
    		}
       		else
       		{
       			nextState = null;
       		}
       		break;


    	default: G.Error("Not expecting %s",card);
    	}
    	return(nextState);
    }
    private boolean retrieveGrande(PlayerBoard pb,ViticultureCell c,ViticultureChip grande,replayMode replay)
    {	boolean found = false;
    	if(c.containsChip(grande))
			{	//p1("motivator payoff");
				c.removeChip(grande);
				found = true;
				pb.workers.addChip(grande);
				if(whoseTurn!=targetPlayer) 
					{ 
					  PlayerBoard ap = pbs[targetPlayer];
					  if(testOption(Option.LimitPoints) && ((ap.score-ap.startingScore)>=3))
					  {
						 logGameEvent(NoVP); 
					  }
					  else
					  {
					  changeScore(ap,1,replay,Motivator,ViticultureChip.MotivatorCard,ScoreType.ScoreBlue);
		   			  String to = ap.getRooster().colorPlusName();
	    			  String msg = ">>"+pb.getGrandeWorker().colorPlusName()+"  &  #1 1VP"; 
	           		  logRawGameEvent(msg,to);
					  }
					}// give the motivator 1 point 
				else {
	    			String msg = ">>" +pb.getGrandeWorker().colorPlusName(); 
	    			logRawGameEvent(msg);

				}
				if(replay!=replayMode.Replay)
				{
					animationStack.push(c);
					animationStack.push(pb.workers);
				}
			}
    	return(found);
    }
    private ViticultureState resolveBlue(PlayerBoard pb,ViticultureChip card,ViticultureId resolution,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	triggerCard = card;
    	if(resolution!=ViticultureId.Choice_0)
    	{
    	switch(card.order)
    	{
    	case 1: // merchant
    		switch(resolution)
    		{	
    		case Choice_A:
    			//p1("merchant a");
    			changeCash(pb,revision>=104 ? -3 : 3,yokeCash,replay);	// bug, used to give you money and cards!
    			placeGrapeOrWine(pb.whiteGrape,ViticultureChip.WhiteGrape,0,replay); 
    			placeGrapeOrWine(pb.redGrape,ViticultureChip.RedGrape,0,replay); 
    			break;
    		case Choice_B:
    			//p1("merchant b");
    			nextState = ViticultureState.FillWineBonusOptional;
    			break;
    		default: break;
    		}
    		break;
    		
    		
    	case 2: // crusher
    		switch(resolution)
    		{	
    		case Choice_A:
    			//p1("crusher a");
    			changeCash(pb,3,yokeCash,replay);
    			nextState = drawCards(1,yellowCards,pb,replay,m);
    			break;
    		case Choice_B:
       			//p1("crusher b");
    			addContinuation(Continuation.Make2WinesOptional);
    			nextState = drawCards(1,purpleCards,pb,replay,m);
    			//if(nextState!=null) { //p1("oracl gets cards before crusher"); }
     			break;
    		default: ;
    		}
    		break;
    		

    	case 3: //judge
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("judge a");
    			nextState = drawCards(2,yellowCards,pb,replay,m);
    			break;
    		case Choice_B:
       			//p1("judge b");
    			nextState = ViticultureState.DiscardWineFor3VP;
    			break;
    		default: ;
    		}
    		break;

    	case 4: //oenologist
    		switch(resolution)
    		{	
    		case Choice_A:
    			//p1("oenologist a");
        		ageWines(pb,replay,null,true);
       			ageWines(pb,replay,null,true);
       			break;
    		case Choice_B:
        		//p1("oenologist b");
    			if(upgradeCellar(pb,true,replay)>0)
    				{
    				// fixed cost of 3
    				changeCash(pb,-3 + (pb.hasWorkshop()?1:0),yokeCash,replay);
    				}
    			break;
    		default: ;
    		}
    		break;
    		
       	case 5: // marketer
    		switch(resolution)
    		{	
    		case Choice_A:
        		//p1("marketer a");
        		nextState = drawCards(2,yellowCards,pb,replay,m);
        		if(revision>=132) { changeCash(pb,1,yokeCash,replay); }
        		// in between you get both, nice bug
        		if(revision<143){ changeScore(pb,1,replay,Marketer,card,ScoreType.ScoreBlue); }
    			break;
    		case Choice_B:
    			//p1("marketer b");
    			// avoiding information leakage, we're sometimes allowed to select this even if it's impossible
    			// but if it is impossible, tell the fool something
    			nextState = canFillWineOrder(pb) ? ViticultureState.FillWineBonus : ViticultureState.FillWineOptional;
    			break;
    		default: ;
    		}
    		break;

    	case 6: // crush expert
    		switch(resolution)
    		{	
    		case Choice_A:
    			//p1("crush expert a");
    			changeCash(pb,3,yokeCash,replay);
    			nextState = drawCards(1,purpleCards,pb,replay,m);
    			break;
    		case Choice_B:
    			//p1("crush expert b");
    			nextState = ViticultureState.Make3WinesOptional;
    			break;
    		default: ;
    		}
    		break;

    	case 7:	// uncertified teacher
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("uncertified teacher a");
    			if((pb.nWorkers<maxWorkers()) && (pb.score>MIN_SCORE)) 
    			{	changeScore(pb,-1,replay,UncertifiedTeacher,card,ScoreType.ScoreBlue);
    				nextState = doTrainWorker(pb,BASE_COST_OF_WORKER,false,false,replay);
    			}
    			break;
    		case Choice_B:
    			{
    			//p1("uncertified teacher b");
       			int n=0;
    			for(PlayerBoard p : pbs)
    			{	// the card says 6 so we use MAX_WORKERS not maxWorkers()
    				if((p!=pb) && (p.nWorkers==MAX_WORKERS)) 
    					{ n++; }
    			}
    			if(testOption(Option.LimitPoints) && n>3) { n = 3; logGameEvent(Limit3); }
    			changeScore(pb,n,replay,UncertifiedTeacher,card,ScoreType.ScoreBlue);
    			}
    			break;
    		default: ;
    		}
    		break;

    	case 8: // teacher
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("teacher a");
       			nextState = ViticultureState.Make2WinesOptional;
    			break;
    		case Choice_B:
       			//p1("teacher b");
       			nextState = doTrainWorker(pb,2,false,false,replay);
    			break;
    		default: ;
    		}
    		break;


    	case 9:	// benefactor
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("benefactor a");
    			if(drawForOracle(pb,replay,m))
           		{	pb.oracleColors = new ViticultureCell[] {yellowCards,greenCards};
           			reshuffleIfNeeded(pb.oracleColors);
            		nextState = ViticultureState.SelectCardColor;
            		//p1("Oracle benefactor");
           		}
           		else
           		{
       			drawCard(greenCards,pb,replay,m);
    			drawCard(yellowCards,pb,replay,m);
           		}
    			break;
    		case Choice_B:
       			//p1("benefactor b");
       			nextState = ViticultureState.Discard2CardsFor2VP;
    			break;
    		default: ;
    		}
    		break;
    		
       	case 10: // assessor
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("assessor a");
       			changeCash(pb,pb.cards.height(),yokeCash,replay);
    			break;
    		case Choice_B:
       			//p1("blue 10 b");
       			if(pb.cards.height()>0)
    			{	changeScore(pb,2,replay,Assessor,card,ScoreType.ScoreBlue);
    				while(pb.cards.height()>0)
    				{
    					discardCard(pb.cards,pb.cards.height()-1,replay,DiscardSomething);
    				}
    			}
    			break;
    		default: ;
    		}
    		break;
    		 
    	case 12:	// harvester
    		nextState = ViticultureState.Harvest2Optional;
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("harvester a");
       			changeCash(pb,2,yokeCash,replay);
    			break;
    		case Choice_B:
       			//p1("harvester b");
       			changeScore(pb,1,replay,Harvester,card,ScoreType.ScoreBlue);
    			break;
    		default: ;
    		}
    		break;


    	case 13: // professor
    		// revision 157, the resolution was always set correctly
    		// with unlimited workers, it can be a real choice
    		ViticultureId res = resolution;
    		// before the fix, the resolution was always choice a
    		if((revision<157) && pb.nWorkers==6) { res = ViticultureId.Choice_B; }
    		switch(res)
    		{
    		case Choice_B:
	    		{
	       			//p1("professor a");
	       			changeScore(pb,2,replay,Professor,card,ScoreType.ScoreBlue); 
	    		}
	    		break;
    		case Choice_A:
	    		{
	       			//p1("professor b");
	       			nextState = doTrainWorker(pb,2,false,false,replay);
	    		}
	    		break;
	    	default: G.Error("Not expecting resolution %s",res);
    		}
    		break;
       	case 14:	// master vintner
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("master vintner a");
       			int cost = upgradeCellar(pb,true,replay);	// does not include workshop discount if applicable
    			if(cost>0) { changeCash(pb,-(cost - 2 - (pb.hasWorkshop()?1:0)),yokeCash,replay); }
    			break;
    		case Choice_B:
       			//p1("master vintner b");
    			nextState = canAgeSomeWine(pb) ? ViticultureState.Age1AndFill : ViticultureState.FillWineOptional;
    			break;
    		default: ;
    		}
    		break;
  		
			

    	case 15:	// uncertified oenologist
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("uncertified oenologist a");
       			ageWines(pb,replay,null,true);
    			ageWines(pb,replay,null,true);
    			break;
    		case Choice_B:
       			//p1("uncertified oenologist b");
    			// upgrade for 1 vp 
    			if(upgradeCellar(pb,true,replay)>0)
    				{changeScore(pb,-1,replay,UncertifiedOenologist,card,ScoreType.ScoreBlue);
    				}
     			break;
      		default: ;
    		}
    		break;
    	case 16:	// promoter
    		nextState = ViticultureState.DiscardGrapeOrWine;
    		break;
    	case 17: // mentor, all players make wine
    		nextState = ViticultureState.Make2Draw2;
    		//p1("Start make2draw2");
    		targetPlayer = whoseTurn;
    		break;

    	case 18:	// harvest expert
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("harvest expert a");
    			addContinuation(Continuation.Harvest1Optional);
       			nextState = drawCards(1,greenCards,pb,replay,m);
    			//if(nextState!=null) { //p1("oracle gets cards in harvest expert"); }
    			break;
    		case Choice_B:
       			//p1("harvest expert b");
       			changeCash(pb,-1,yokeCash,replay);
    			if(pb.yoke.topChip()==null)
    			{
    				pb.yoke.addChip(pb.getContent(pb.yoke));
    			}
    			break;
    		default: ;
    		}
    		break;
    	case 19: // innkeeper
   			//p1("innkeeper a");
    		// 
    		// abnormal condition if the innkeeper is played before there are other discards available.
    		// we just punt.  Only the robot would have this problem.
    		//
    		if(hasDiscardMoves(pb)) { nextState = ViticultureState.Pick2Discards; }
    		break;
    	case 20: // jack of all trades
    		switch(resolution)
    		{
    		case Choice_AandB:
       			//p1("jack of all trades aandb");
    			if((revision<136) || (firstChoice!=ViticultureId.Choice_MakeWineFirst))
    			{
    				if(revision<136)
    					{nextState = ViticultureState.HarvestAndMakeWine;
    					}
    				else
    				{
    					// in the presence of the mercado, expect to harvest, make wine then fill
    					nextState = ViticultureState.Harvest1Optional;
    					addContinuation(Continuation.Make2WinesOptional);
    				}
    			}
    			else
    			{	// make wine first
    				nextState = ViticultureState.Make2WinesOptional;
    				addContinuation(Continuation.Harvest1Optional);
    			}
    			break;
    		case Choice_AandC:
       			//p1("jack of all trades aandc");
    			if((revision<136) || (firstChoice!=ViticultureId.Choice_FillWineFirst))
    				{ 
    				if(revision<136)
    					{
    					nextState = ViticultureState.HarvestAndFill;
    					}
    				else {
    					// in the presence of the mercado, expect to harvest, make wine then fill
    					nextState = ViticultureState.Harvest1Optional;
    					addContinuation(Continuation.Fill1Optional);
    					}
    				}
    			else {
    				// fill a wine order first
    				nextState = ViticultureState.FillWineOptional;
    				addContinuation(Continuation.Harvest1Optional);
    			}
    			break;
    		case Choice_BandC:
       			//p1("jack of all trades bandc");
    			if(revision<136 || (firstChoice!=ViticultureId.Choice_FillWineFirst))
    			{
       			nextState = ViticultureState.Make2AndFill;
    			}
    			else {
    				// fill a wine order first
    				nextState = ViticultureState.FillWineOptional;
    				addContinuation(Continuation.Make2WinesOptional);
    			}
    			break;
    		case Choice_A:
       			//p1("jack of all trades a");
       			nextState = ViticultureState.Harvest1Optional;
    			break;
    		case Choice_B:
       			//p1("jack of all trades b");
       			nextState = ViticultureState.Make2WinesOptional;
    			break;
    		case Choice_C:
       			//p1("jack of all trades c");
       			nextState = ViticultureState.FillWineOptional;
    			break;
    		default: break;
    		}
    		break;
    		
    	case 21:	// politician
    		if(pb.score<0)
    		{
       			//p1("politician a");
       			changeCash(pb,6,yokeCash,replay);
    		}
    		else
    		{
       			//p1("politician b");
    			if(drawForOracle(pb,replay,m))
           		{	pb.oracleColors = new ViticultureCell[] {yellowCards,greenCards,purpleCards};
           			reshuffleIfNeeded(pb.oracleColors);
            		nextState = ViticultureState.SelectCardColor;
            		//p1("oracle politician");
           		}
           		else
           		{
       			drawCard(greenCards,pb,replay,m);
    			drawCard(yellowCards,pb,replay,m);
    			drawCard(purpleCards,pb,replay,m);
           		}
    		}
    		flashChip = ViticultureChip.PoliticianCard;
    		break;
    	case 22: // supervisor
   			//p1("supervisor a");
   			nextState = ViticultureState.Make2WinesVP;
    		break;
    	case 23: // scholar
    		if(resolution==null) { p1("Scholar null"); }
    		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("scholar a");
       			nextState = drawCards(2,purpleCards,pb,replay,m);
    			break;
    		case Choice_AandB:
       			//p1("scholar aandb");
    			changeScore(pb,-1,replay,Scholar,card,ScoreType.ScoreBlue);
    			nextState = drawCards(2,purpleCards,pb,replay,m);
    			
    			if((nextState!=null) && (revision>=150))
       			{	//p1("scholar a and b and oracle");
       				addContinuation(Continuation.TrainScholarWorker);
       				break;
       			}
    			//p1("scholar a and b normal");
       			//$FALL-THROUGH$
			case Choice_B:
	   			//p1("scholar b");
	   			nextState = doTrainWorker(pb,1,false,false,replay);   			
    			break;
    		default: ;
    		}
    		break;
    		
       	case 24: // reaper
   			//if(pb.nPlantedFields()==3) 
   			//	{ //p1("reaper a"); 
   			//	}
       		nextState = ViticultureState.Harvest3Optional;
       		break;
      	case 25: // motivator all players retrieve grande
      		resolveCard(ViticultureChip.GrandeMotivation,card);
       		targetPlayer = whoseTurn;
       		nextState = ViticultureState.ResolveCard;
      		//p1("retrieve grande");
    		if((revision>=131) && pb.workers.containsChip(pb.getGrandeWorker()))
    		{
    		//p1("retrieve grande not me");
    		board_state = ViticultureState.Confirm;
    		resetState = ViticultureState.ResolveCard;
    		do 	{ nextState = setNextPlayerInCycle();
    			  pb = pbs[whoseTurn];  		
    			} while ( (nextState==ViticultureState.ResolveCard) 
    					  && pb.workers.containsChip(pb.getGrandeWorker()));
    		}

       		break;
      	case 26: // bottler
   			//p1("bottler a");
   			nextState = ViticultureState.Make3WinesVP;
       		break;
       	case 27: // craftsman
       		//p1("craftsman");
       		switch(resolution)
       		{
       		case Choice_AandB:
       			//p1("craftsman aandb");
       			nextState = drawCards(1,purpleCards,pb,replay,m);
				//$FALL-THROUGH$
			case Choice_B:
	   			//p1("craftsman b");
				{
	   			int cost = upgradeCellar(pb,true,replay);
	   			int discount = revision>=153 && pb.hasWorkshop() ? 1 : 0;
	   			changeCash(pb,-(cost-discount),yokeCash,replay);
				}
       			break;
       		case Choice_AandC:
       			//p1("craftsman aandc");
       			changeScore(pb,1,replay,Craftsman,card,ScoreType.ScoreBlue);
				//$FALL-THROUGH$
       		case Choice_A: 
       			//p1("craftsman a");
       			nextState = drawCards(1,purpleCards,pb,replay,m);
       			break;
       		case Choice_BandC:
       			//p1("craftsman bandc");
       			{
       			int cost = upgradeCellar(pb,true,replay);
	   			int discount = revision>=153 && pb.hasWorkshop() ? 1 : 0;
       			changeCash(pb,-(cost-discount),yokeCash,replay);
       			}
				//$FALL-THROUGH$
			case Choice_C:
	   			//p1("craftsman c");
	   			changeScore(pb,1,replay,Craftsman,card,ScoreType.ScoreBlue);
       			break;
       		default: ;
       		}
       		break;
       	case 28: // exporter
       		switch(resolution)
       		{
       		case Choice_A:
       			//p1("exporter a");
       			nextState = ViticultureState.Make2WinesOptional;
       			break;
       		case Choice_B:
       			//p1("exporter b");
       			nextState = ViticultureState.FillWineOptional;
       			break;
       		case Choice_C:
       			//p1("exporter c");
       			nextState = ViticultureState.DiscardGrapeFor2VP;
       			break;
       		default:;
       		}
       		break;
       	case 29: // laborer
       		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("laborer a");
       			nextState = ViticultureState.Harvest2Optional;
    			break;
    		case Choice_B:
       			//p1("laborer b");
       			nextState = ViticultureState.Make3WinesOptional;
    			break;
    		case Choice_AandB:
       			//p1("laborer aandb");
       			changeScore(pb,-1,replay,Laborer,card,ScoreType.ScoreBlue);
       			if((revision<136) || (firstChoice!=ViticultureId.Choice_MakeWineFirst))
       				{
       				nextState = ViticultureState.Harvest2AndMake3;
       				}
       			else {
       				// make wines first
       				nextState = ViticultureState.Make3WinesOptional;
       				addContinuation(Continuation.Harvest2Optional);      				
       				}
    			break;
    		default: ;
    		}
       		break;
       	case 30: // designer
   			//p1("designer a");
   			nextState = ViticultureState.BuildStructureVP;
       		break;
       	case 31: // governess
       		switch(resolution)
    		{	
    		case Choice_A:
       			//p1("governess a");
       			nextState = doTrainWorker(pb,1,true,false,replay);
    			break;
    		case Choice_B:
       			nextState = ViticultureState.DiscardWineFor2VP;
    			break;
    		default: ;
    		}
       		break;
       	case 32:	// manager, take action from a previous season
       		//p1("manager a");
       		nextState = ViticultureState.TakeActionPrevious;
       		break;
       	case 33: // zymnologist
   			//p1("zymnologist a");
   			nextState = ViticultureState.Make2WinesNoCellar;
       		break;
       	case 34: // noble
       		switch(resolution)
    		{	
    		case Choice_A:
           		//p1("noble a");
    			changeCash(pb,-1,yokeCash,replay);
    			changeResidual(pb,1,replay);
    			break;
    		case Choice_B:
           		//p1("noble b");
    			changeResidual(pb,-2,replay);
    			changeScore(pb,2,replay,Noble,card,ScoreType.ScoreBlue);
    			break;
    		default: ;
    		}
       		break;
       		
       	case 35: // governor take yellow cards
       		//G.print("Start governor");
       		targetPlayer = whoseTurn;
       		nextState = ViticultureState.TakeYellowOrVP;
       		break;

       	case 36: // taster
       		//p1("taster a");
      		nextState = ViticultureState.DiscardWineForCashAndVP;
       		break;
       	case 37: // caravan
       		//p1("caravan a");
       		
       		// assure there are cards, if possible.  In extreme cases, the decks
       		// can be completely empty
       		if(greenCards.topChip()==null) { reshuffle(greenCards,greenDiscards); }
       	 	if(yellowCards.topChip()==null) { reshuffle(yellowCards,yellowDiscards); }
       	 	if(blueCards.topChip()==null) { reshuffle(blueCards,blueDiscards); }
       	    if(purpleCards.topChip()==null) { reshuffle(purpleCards,purpleDiscards); }
       	    if(structureCards.topChip()==null) { reshuffle(structureCards,structureDiscards); }
       		nextState = ViticultureState.Pick2TopCards;
       		break;
     		
       	case 38: // guest speaker all players train
    		p1("card guest speaker 2");

       		resolveCard(ViticultureChip.TrainWorker,card);
       		//p1("Start guest speaker");
 
       		targetPlayer = whoseTurn;
       		nextState = resetState = ViticultureState.ResolveCard;
       		// rev 134 restricts the question to those who have an actual choice.
       		if((revision>=134) && ((pb.cash<1) || (pb.nWorkers==maxWorkers())))
       		{	//p1("first player no train");
       			do {
       				nextState = setNextPlayerInCycle();
       			 	pb = pbs[whoseTurn];
       			}
       			while ( ((pb.cash<1) || (pb.nWorkers==maxWorkers())) && (nextState!=null));
       		}
       		//if(nextState==null) { p1("nobody can train"); }
       		break;
        		
    	case 11:	// queen, is too mean!
    	default: G.Error("Can't handle resolve card %s %s",card,resolution);
    	}}
    	return(nextState);
    }

    private ViticultureState playYellowCard(PlayerBoard pb,int index,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	ViticultureChip card = pb.cards.chipAtIndex(index);
    	Assert(card.type==ChipType.YellowCard,"must be a yellow card");
		pb.recordEvent("Play Yellow",card,ScoreType.PlayYellow);   	
    	discardCard(pb.cards,index,replay,PlayCardMessage);
    	if(pb.hasInn()) { changeCash(pb,1,pb.destroyStructureWorker,replay); }
       	if(pb.hasTapRoom()) { addContinuation(Continuation.DiscardWineFor2VP); }
    	if(pb.hasTavern()) { addContinuation(Continuation.DiscardGrapeFor3VP); }
 
    	switch(card.order)
    	{
    	// these offer no choices
    	case 4:		// blacksmith 
    	case 17:	// vendor
    	case 18:	// handyman
    	case 21:	// banker
    	case 22:	// overseer
    	case 25:	// grower
    	case 27:	// cultivator
    	case 29:	// planner
    	case 30:	// agriculturist
    	case 31:	// swindler
    	case 33:	// organizer
    	case 37:	// volunteer crew
    	case 38:	// wedding party
    		nextState = resolveYellow(pb,card,ViticultureId.Choice_A,replay,m);
    		break;
    	// these require a choice or a resource
    	case 1:		// surveyor
    	case 2:		// broker
    	case 3:		// wine critic
    	case 6:		// tour guide
    	case 7:		// novice guide
    	case 8:		// uncertified broker
    	case 9:		// planter
    	case 10:	// buyer
    	case 11:	// landscaper
    	case 12:	// architect
    	case 13:	// uncertified architect
    	case 14:	// patron
    	case 15:	// auctioneer
    	case 16:	// entertainer
    	case 19:	// horticulturist
    	case 20:	// peddler
    	case 24:	// sharecropper
    	case 26:	// negotiator
    	case 32:	// producer
       	case 35:	// artisan
       	case 36:	// stonemason
    		nextState = ViticultureState.ResolveCard;
    		resolveCard(card,card);
    		break;
    	case 28: // homesteader, build and/or plant
    	case 34:	// sponsor
    		// only let him take both if he is above minimum score
    		nextState = (pb.score>MIN_SCORE) ? ViticultureState.ResolveCard_AorBorBoth : ViticultureState.ResolveCard;
    		resolveCard(card,card);
    		break;
    	case 5: // contractor
    		nextState = ViticultureState.ResolveCard_2of3;
    		resolveCard(card,card);
	    	break;
    	case 23: // importer, coerce 3 cards or draw 3
    		// importer needs a UI to allow giving cards (importer is removed for now)
    		resolveCard(card,card);
    		break;
     	default: G.Error("Can't resolve %s",card);
    	}
    	return(nextState);
    }
    private void removeTop(ViticultureCell from,replayMode replay)
    {
    	from.removeTop();
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(from);
    		animationStack.push(getCell(from.rackLocation(),'@',0));
    	}
    }
    
    // place a wine in the next lower empty slot
    private void placeGrapeOrWine(ViticultureCell to[],ViticultureChip wine,int idx,replayMode replay)
    {
    	idx = Math.min(idx, to.length);
    	boolean placed = false;
    	while(idx>=0)
    	{	ViticultureCell dest = to[idx];
    		if(dest.topChip()==null)
    		{	dest.addChip(wine);
    			placed = true;
    			if(replay!=replayMode.Replay) { 
    				animationStack.push(getCell(dest.rackLocation(),'@',0));
    				animationStack.push(dest);
    			}
    			break;
    		}
    		idx--;
    	}
    	if(!placed && replay!=replayMode.Replay)
    	{
    		logGameEvent("Discard #1",wine.id.shortName);
    	}
    }
    private ViticultureState resolveTrade(PlayerBoard pb,replayMode replay)
    {	ViticultureState nextState = null;
    	switch(tradeFrom)
    	{
    	case Cards: 
    		discardSelectedCards(pb,replay);
    		break;
    	case Cash:
    		changeCash(pb,-3,yokeCash,replay);
    		logRawGameEvent("- $2 $1");
    		break;
    	case VP:
    		changeScore(pb,-1,replay,Trade,ViticultureChip.VictoryPoint_Minus,ScoreType.Other);
    		logRawGameEvent("- 1VP");
    		break;
    	case RedGrape:
    	case RedGrapeDisplay:
    		removeTop(pb.redGrape[pb.leastRedGrapeValue()-1],replay);
    		logRawGameEvent("- RedGrape");
    		break;
    	case WhiteGrape:
    	case WhiteGrapeDisplay:
    		removeTop(pb.whiteGrape[pb.leastWhiteGrapeValue()-1],replay);
    		logRawGameEvent("- WhiteGrape");
    		break;
    	default:	G.Error("Not implemented");
    	}
    	switch(tradeTo)
    	{
       	case Cards: 
       		nextState = ViticultureState.Take2Cards;
       		break;
		case Cash:
			changeCash(pb,3,yokeCash,replay);
			logRawGameEvent("+ $2 $1");
			break;
		case VP:
			changeScore(pb,1,replay,Trade,ViticultureChip.VictoryPoint_1,ScoreType.Other);
			logRawGameEvent("+ 1VP");
			break;
		case RedGrape:
			placeGrapeOrWine(pb.redGrape,ViticultureChip.RedGrape,0,replay);
			logRawGameEvent("+ RedGrape");
			break;
		case WhiteGrape:
			placeGrapeOrWine(pb.whiteGrape,ViticultureChip.WhiteGrape,0,replay);
			logRawGameEvent("+ WhiteGrape");
			break;
		default:	G.Error("Not implemented");
   	
    	}
    	return(nextState);
    }
    private void discardSelectedCards(PlayerBoard pb,replayMode replay)
    {
    	while(pb.nSelectedCards()>0)
    	{	discardTopSelectedCard(pb,replay,DiscardSomething);
    	}
    }
    
    private void doRemove(PlayerBoard pb,Viticulturemovespec m,replayMode replay)
    {
		ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
		ViticultureCell dest = getCell(m.dest,m.to_col,0);
		Assert(dest==pb.workers,"must go to workers");
		ViticultureChip worker = from.removeChipAtIndex(m.from_index);
		Assert(pb.isMyWorker(worker),"wrong worker");
		dest.addChip(worker);
		//
		// you can retrieve a planner placed cell, even if it 
		// hasn't been executed yet.  I suppose you're allowed
		// to think it was a terrible mistake, or things have 
		// changed.
		//
		if((revision>=115) && (worker.type==ChipType.Messenger) && pb.messengerCell!=null)
		{	// oops, got the action anyway, and with no worker on the board!
			pb.messengerCell = null;
			pb.messengerMove = null;
		}
		int ind = plannerCells.indexOf(from);
		if(ind>=0) 
			{ plannerCells.remove(ind,true);
			  plannerMeeples.remove(ind,true);
			  plannerMoves.remove(ind, true);
			}
		
		if(replay!=replayMode.Replay)
		{
			animationStack.push(from);
			animationStack.push(dest);
		}
		
    }
    private void doRetrieval(PlayerBoard pb,CommonMoveStack moves,replayMode replay)
    {	int nmoves = moves.size();
    	Assert(nmoves<=2, "no more than 2");
    	if(nmoves>0)
    	{
    		Viticulturemovespec m1 = (Viticulturemovespec)moves.pop();
    		Viticulturemovespec m2 = null;
    		if(nmoves==2)
    			{ // in case they're from the same cell, take the top one first
    			  // so the numbering is unchanged.
    			  m2 = (Viticulturemovespec)moves.pop();
    			  if(m2.from_index>m1.from_index) { Viticulturemovespec m3 = m1; m1 = m2; m2 = m3; } 
    			}
    		doRemove(pb,m1,replay);
    		if(m2!=null) { doRemove(pb,m2,replay); }
    	}
    }
    private ViticultureState doUproot(PlayerBoard pb,Viticulturemovespec up2,replayMode replay)
    {
    	ViticultureCell victim = getCell(up2.source,up2.from_col,up2.from_row);
		ViticultureChip removed = victim.removeChipAtIndex(up2.from_index);
		ViticultureCell dest = pb.cards;
		ViticultureState nextState = null;
		if(revision>=114)
		{	// put the uprooted card back in hand
			dest = pb.cards;
			switch(resetState)
			{
			case Uproot1For2:
				nextState = ViticultureState.DiscardGreen;
				break;
			case Uproot2For3:
				nextState = ViticultureState.Discard2Green;
				break;
			default: break;
			}
		}
		else
		{	// old and incorrect, just discard the green card
			dest = greenDiscards;	
		}
		logVine(UprootSomething,removed);
		dest.addChip(removed);
		if(replay!=replayMode.Replay)
		{	animationStack.push(victim);
			animationStack.push(dest);
		}
		return(nextState);
    }
    private boolean drawForOracle(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
    {
    	return( revision>=140
    			&& (currentWorker!=null) 
    			&& (currentWorker.type==ChipType.Oracle)
        		&& (currentWorker.color==pb.color));
  
    }
    private void logVine(String call,ViticultureChip chip)
    {
		String msg = chip.description;
		int ind2 = msg.indexOf(')');
		int ind1 = msg.indexOf('(');
		if(ind1>=0 && ind2>=ind2) 
			{ msg = msg.substring(ind1+1,ind2);
			logGameEvent(call,"GreenCard",msg);		
			}
    }
    
    private void reshuffleIfNeeded(ViticultureCell stacks[])
    {
    	for(ViticultureCell from : stacks)
    	{
    		if(from.topChip()==null) 
        	{	reshuffle(from,getDiscards(from));     		
        	}
    	}
    }
    
    private ViticultureState handleConfirm(PlayerBoard pb,replayMode replay,Viticulturemovespec m)
    {	ViticultureState nextState = null;
    	int discount = 0;
    	switch(resetState)
    	{
     	case PickNewRow:	// resolving organizer card
        	changeWakeup(pb,selectedWakeup,replay);    	
        	addContinuation(Continuation.NextSeason);
        	nextState = takeRoosterBonus(pb,replay,m);
        	// also take the bonus for the new space, and pass to the next row
        	break;
    	case PlayBlueDollar:
    		changeCash(pb,1,playBlueWorkers[PlayBlueDollarRow],replay);	// give him the money
			//$FALL-THROUGH$
		case Play1Blue:
		case PlaySecondBlue:
    		nextState = playBlueCard(pb,pb.topSelectedCardIndex(),replay,m);
    		break;
       	case Play2Blue:
       		addContinuation(Continuation.PlaySecondBlue);
      		nextState = playBlueCard(pb,pb.topSelectedCardIndex(),replay,m);
    		break;
       		
    	case PlayYellowDollar:
    		changeCash(pb,1,playYellowWorkers[PlayYellowDollarRow],replay);	// give him the money
			//$FALL-THROUGH$
		case Play1Yellow:
		case PlaySecondYellow:
    		nextState = playYellowCard(pb,pb.topSelectedCardIndex(),replay,m);
    		break;
       	case Play2Yellow:
       		addContinuation(Continuation.PlaySecondYellow);
    		nextState = playYellowCard(pb,pb.topSelectedCardIndex(),replay,m);
    		break;
    		
       	case Trade2:
       		addContinuation(Continuation.TradeSecond);
			//$FALL-THROUGH$
		case Trade1:
       	case TradeSecond:
       		nextState = resolveTrade(pb,replay);
       		break;
       		
       	case Discard2CardsForAll: // peddler
       		discardSelectedCards(pb,replay);
       		if(drawForOracle(pb,replay,m))
       		{	pb.oracleColors = new ViticultureCell[] {yellowCards,greenCards,blueCards,purpleCards,structureCards};
       			reshuffleIfNeeded(pb.oracleColors);
        		nextState = ViticultureState.SelectCardColor;
        		//p1("oracle peddler 2");
       		}
       		else
       		{
      		drawCard(yellowCards,pb,replay,m);
       		drawCard(greenCards,pb,replay,m);
       		drawCard(blueCards,pb,replay,m);
       		drawCard(purpleCards,pb,replay,m);
       		drawCard(structureCards,pb,replay,m);
       		}
       		break;
       	case Discard2CardsFor4: // auctioneer
       		{
       		discardSelectedCards(pb,replay);
       		changeCash(pb,4,yokeCash,replay);
       		}
       		break;
       		
       	case Discard4CardsFor3: // auctioneer
       		{
       		discardSelectedCards(pb,replay);
       		changeScore(pb,3,replay,Auctioneer,ViticultureChip.AuctioneerCard,ScoreType.ScoreYellow);
       		}
       		break;
    	case DiscardCards:
    		{
    		discardSelectedCards(pb,replay);
    		// dispense with new wakeup if the game will be over
       		} 		
    		break;
    	case Discard1ForOracle:
    		pb.oracleCards.reInit();
    		discardSelectedCards(pb,replay);
    		break;
    	case Take2Cards:
    		addContinuation(Continuation.TakeCard);
			//$FALL-THROUGH$
		case TakeCard:
    	case TakeYellowOrBlue:
    	case TakeYellowOrGreen:
    		{
    		CardPointer selectedChip = pb.removeTopSelectedCard();
    		ViticultureCell selectedCard = cardPile(selectedChip.card);
    		nextState = drawCards(1,selectedCard,pb,replay,m);
    		}
    		break;
    	case SelectCardColor:
    		CardPointer selectedChip = pb.removeTopSelectedCard();
    		ViticultureCell selectedCard = cardPile(selectedChip.card);
    		for(ViticultureCell c : pb.oracleColors)
    		{	// draw all the other cards
    			if(c!=selectedCard)
    			{
    				drawCard(c,pb,replay,m);
    			}
    		}
    		// drawCards will trigger the oracle action of sampling
    		nextState = drawCards(1,selectedCard,pb,replay,m);
    		break;
    	case BuildStructureBonus:
    	case BuildTourBonus:
    		discount = 1;
    		//$FALL-THROUGH$
		case BuildStructureDiscount3:
			if(resetState==ViticultureState.BuildStructureDiscount3) { discount=3; }
			//$FALL-THROUGH$
		case BuildAtDiscount2:
			if(resetState==ViticultureState.BuildAtDiscount2) { discount = 2; }
			//$FALL-THROUGH$
		case BuildAtDiscount2forVP:
			if(resetState==ViticultureState.BuildAtDiscount2forVP) { discount = 2; }
			//$FALL-THROUGH$
		case BuildTour:
		case BuildStructure:
		case BuildStructureOptional:
		case BuildAndPlant:
		case BuildStructureForBeforePlant:
		case BuildStructure23Free:
		case Build2StructureFree:
		case BuildStructureFree:
		case BuildStructureVP:			
    		{
    		ViticultureCell from = getCell(buildingSelection.source,buildingSelection.from_col,buildingSelection.from_row);
    		ViticultureCell to = getCell(buildingSelection.dest,buildingSelection.to_col,buildingSelection.to_row);
    		if((revision<144) || (to!=choice0)) { doStudio(pb,replay); }
    		if(pb.hasWorkshop()) { discount++; }
    		if(from.rackLocation()==ViticultureId.Cards)
    			{
    			for(ViticultureCell structure : revision>=114 ? pb.buildStructureCells : pb.structures)
    		 	{ ViticultureChip top = structure.topChip();
    		 	  if((top==null)|| pb.isEmptyField(structure))
    		 		{ to = structure; 
    		 		break; 
    		 		}
    			}

    			}
    		
    		if(to==choice0)
    		{	// giving a tour
    			nextState = giveTour(pb,discount+2,replay);
    		}
    		else
    		{
         	ViticultureChip chip = from.removeChipAtIndex(buildingSelection.from_index);
         	boolean isStructureCard = (chip.type==ChipType.StructureCard);
         	int cost = isStructureCard ? chip.costToBuild() : to.cost;    
         	switch(resetState)
         	{
         	case BuildStructure23Free:
         	case BuildStructureFree:
         	case Build2StructureFree:
         		break;
         	default:
         		changeCash(pb,Math.min(0,- (cost - discount)),yokeCash,replay);
         	}

         	if((resetState==ViticultureState.BuildAtDiscount2)
         			&& (cost>=5))
         		{
         		changeScore(pb,1,replay,Blacksmith,ViticultureChip.BlacksmithCard,ScoreType.ScoreYellow);	// also score a VP
         		}
         	if(isStructureCard)
         		{ changeScore(pb,1,replay,BuildStructureMode,chip,ScoreType.OrangeCard);
         		logGameEvent(BuildMessage,chip.cardName);
    			flashChip = chip;
         		}	// gains a point
           	to.addChip(chip);
 
        	if(replay!=replayMode.Replay)
	        	{
	        	animationStack.push(from);
	        	animationStack.push(to);
	        	}
        	switch(resetState)
        	{
        	case BuildAndPlant:
        		nextState = ViticultureState.Plant1VineOptional;
        		break;
        	case BuildStructureVP:	// from designer
        		if(pb.nStructuresWithValueAtLeast(0)>=6) 
        		{ changeScore(pb,2,replay,Designer,ViticultureChip.DesignerCard,ScoreType.ScoreBlue); 
        		}
        		break;
        	case BuildStructureForBeforePlant:
        		nextState = ViticultureState.PlantVine4ForVP;
        		break;
        	case BuildAtDiscount2forVP:	// handyman
        		if(whoseTurn!=targetPlayer) 
        		{ 	PlayerBoard ap = pbs[targetPlayer];
        			if(testOption(Option.LimitPoints) && ((ap.score-ap.startingScore)>=3))
        			{
        			logGameEvent(NoVP);
        			}
        			else
        			{
        			changeScore(ap,1,replay,Handyman,ViticultureChip.HandymanCard,ScoreType.ScoreYellow); 
        			}
        		}	// give the trigger 1 vp
    			do {
    				nextState = setNextPlayerInCycle();
      				pb = pbs[whoseTurn];
    				} while ((revision>=134) && !pb.canBuildStructureWithDiscount(2) && (nextState!=null));
        		break;
        	case Build2StructureFree:
        		 nextState = ViticultureState.BuildStructureFree; 
        		 break;
        	case BuildStructureOptional:
        	default: break;
        	}}}
    		break;
		case Harvest1Dollar:
			changeCash(pb,1,harvestWorkers[HarvestDollarBonusRow],replay);
			//$FALL-THROUGH$
		case Harvest2:
		case Harvest2Optional:
		case Harvest1:
		case Harvest1Optional:
			doHarvest(pb,replay);
			break;
		case Harvest3Optional:
			if(doHarvest(pb,replay)==3) 
			{ changeScore(pb,2,replay,Reaper,ViticultureChip.ReaperCard,ScoreType.ScoreBlue); 
			}
			break;
		case Harvest2AndMake3:
			doHarvest(pb,replay);
			nextState = ViticultureState.Make3WinesOptional;
			break;
		case HarvestAndFill:
			doHarvest(pb,replay);
			nextState = ViticultureState.FillWineOptional;
			break;
		case HarvestAndMakeWine:
			doHarvest(pb,replay);
			nextState = ViticultureState.Make2Wines;
			break;
		case Plant1For2VPVolume:	// grower
			if((pendingMoves.size()==0) 
				&& (pb.nSelectedCards()>0)
				&& (pb.selectedCells.size()>0)
				&& (pb.nPlantedVines()>=5)
				)
			{	// we succeeded and the number of vines will be 6
				//p1("Plant1For2VPVolume payoff");
				changeScore(pb,2,replay,Grower,ViticultureChip.GrowerCard,ScoreType.ScoreYellow);
			}
			//$FALL-THROUGH$
		case PlantVine4ForVP:
		case Plant1Vine:
		case Uproot2For3:
		case Uproot1For2:
		case Uproot:
		case Plant2Vines:
		case Plant2VinesOptional:
		case Plant1VineOptional:
		case Plant1AndGive2:
		case Plant1VineNoStructures:
		case PlantSecondVine:
		case Plant1VineNoLimit:
		case Plant1For2VPDiversity:
		case HarvestOrUproot:
			{
			if((resetState==ViticultureState.HarvestOrUproot) && (pendingMoves.size()==0))
			{
				doHarvest(pb,replay);
				break;
			}
			if(pendingMoves.size()>0)
			{	// maximum of 2 vines. be sure to uproot the higher numbered one first, so the second index remains valid
				Viticulturemovespec up1 = (Viticulturemovespec)pendingMoves.pop();
				Viticulturemovespec up2 = pendingMoves.size()>0 ? (Viticulturemovespec)pendingMoves.pop() : null;
				if(up2!=null)
					{ if(up2.from_index<up1.from_index) 
						{	Viticulturemovespec x = up2;
							up2 = up1;
							up1 = x;
						}
					doUproot(pb,up2,replay);
					}
				nextState = doUproot(pb,up1,replay);
				
				switch(resetState)
				{
				case Uproot2For3: //horticulturist
					changeScore(pb,3,replay,Horticulturist,ViticultureChip.HorticulturistCard,ScoreType.ScoreYellow);
					break;
				case Uproot1For2: // sharecropper or planter
					changeScore(pb,2,replay,
								(cardBeingResolved == ViticultureChip.SharecropperCard) 
									? Sharecropper
									: Planter ,
								ViticultureChip.SharecropperCard,ScoreType.ScoreYellow);
					break;
				case HarvestOrUproot:
				default: 
					break;
				}

			}
			else
			{
			CardPointer card = pb.removeTopSelectedCard();
			ViticultureCell field = pb.selectedCells.pop();
			ViticultureCell vine = pb.vines[field.row];
			ViticultureChip vineCard = pb.cards.removeChip(card.card);
			flashChip = vineCard;
			vine.addChip(vineCard);
			logVine(PlantSomething,vineCard);
			if(replay!=replayMode.Replay)
				{
				animationStack.push(pb.cards);
				animationStack.push(vine);
				}
			
			// various special cases after planting.
			switch(resetState)
			{
			case Plant2VinesOptional:
				// cycle the same state for all players
				nextState = ViticultureState.Plant1VineOptional;
				break;
			case Plant2Vines:
				if(pb.hasCard(ChipType.GreenCard)) { nextState = ViticultureState.PlantSecondVine; } 
				break;
			case PlantVine4ForVP: // overseer
				if(vineCard.totalVineValue()==4)
				{
					//p1("overseer payoff");
					changeScore(pb,1,replay,Overseer,ViticultureChip.OverseerCard,ScoreType.ScoreYellow);
				}
				break;
			case Plant1For2VPDiversity: // agriculturist
				if(vine.height()>=3)
				{	IStack profile = new IStack();
					// count the different types
					for(int lim=vine.height()-1; lim>=0; lim--)
					{
						profile.pushNew(vine.chipAtIndex(lim).vineProfile());
					}
					if(profile.size()>=3) 
						{ 
						//p1("agriculturist payoff");
						changeScore(pb,2,replay,Grower,ViticultureChip.GrowerCard,ScoreType.ScoreYellow); 	// 2 points for 3 types of grape
						}
				}
				break;
			case Plant1AndGive2:	// cycle through players
				{	if(targetPlayer<0) { p1("bad target"); }
					if(whoseTurn!=targetPlayer) // give the trigger 2$
						{ 	PlayerBoard ap = pbs[targetPlayer];
							if(testOption(Option.LimitPoints) && ((ap.cash-ap.startingCash)>=6))
							{
							logGameEvent(NoCash);	
							}
							else
							{
							changeCash(ap,2,yokeCash,replay); 
							}
						}	
					do {
						nextState = setNextPlayerInCycle();
						pb = pbs[whoseTurn]; }
					while((revision>=134) && !canPlant(pb) && (nextState!=null));
				}
				break;
			default: break;
			}


			if((pb.windmill.topChip()!=null) && (pb.usedWindmill<year))
			{
				pb.usedWindmill = year;
				changeScore(pb,1,replay,Windmill,pb.getWindmill(),ScoreType.Other);
			}
			}}
			break;
		case Retrieve1Current:
		case Retrieve2Workers:
			doRetrieval(pb,pendingMoves,replay);
			break;
		case Flip:
		case FlipOptional:
			{
			ViticultureCell flip = pb.selectedCells.pop();
			ViticultureChip top = flip.topChip();
			switch(top.type)
				{
				case Field: // selling
					changeCash(pb,flip.row+5,yokeCash,replay);
					flip.removeTop();
					flip.addChip(ViticultureChip.SoldFields[flip.row]);
					break;
				case SoldField: // buyback
					changeCash(pb,-(flip.row+5),yokeCash,replay);
					flip.removeTop();
					flip.addChip(ViticultureChip.Fields[flip.row]);
					break;
				default: G.Error("Not expecting %s", top);
				}
			}
			break;
    	case Place2Star:
    		addContinuation(Continuation.Place1Star);
    		if(starPlacement!=null) { nextState = performPlaceStarActions(starPlacement,replay,m); }
    		else if(pb.hasBanquetHall())
    			{ if(starDropped2!=null)
    				{ nextState = performPlaceStarActions(starDropped2,replay,m); };
    			  if(starDropped!=null) 
    			  	{ ViticultureState nextState2 = performPlaceStarActions(starDropped,replay,m);
    			  	  if(nextState==null) { nextState = nextState2; }
    			  	  else { Assert(nextState2==null,"shouldn't be two continuations (due to oracle?)");
    			  	  }
    			  	}
    			}
     		break;

    	case Move1Star:
		case Move2Star:
		case Place1Star:
			// starPlacement is null for move star moves
	  		if(starPlacement!=null) { nextState = performPlaceStarActions(starPlacement,replay,m); }
    		else if(pb.hasBanquetHall())
    			{ if (starDropped2!=null)
    				{ nextState = performPlaceStarActions(starDropped2,replay,m);
    				}
    			  if(starDropped!=null) 
			  		{ ViticultureState nextState2 = performPlaceStarActions(starDropped,replay,m);
			  		if(nextState==null) { nextState = nextState2; }
			  		else { Assert(nextState2==null,"shouldn't be two continuations (due to oracle?)");
			  		}
			  		}
    			}
     		break;
		case DiscardGreen:
			Assert(pb.nSelectedCards()==1,"discarding 1 card");
			discardSelectedCards(pb,replay);
			break;
		case Discard2Green:
			Assert(pb.nSelectedCards()==2,"discarding 2 cards");
			discardSelectedCards(pb,replay);
			break;
		case Discard2CardsFor1VP: // barn structure card
			Assert(pb.nSelectedCards()==2,"discarding 2 cards");
			discardSelectedCards(pb,replay);
			changeScore(pb,1,replay,Barn,ViticultureChip.BarnCard,ScoreType.OrangeCard);
			break;
		case Discard2CardsFor2VP: // benefactor
			Assert(pb.nSelectedCards()==2,"discarding 2 cards");
			discardSelectedCards(pb,replay);
			changeScore(pb,2,replay,Benefactor,ViticultureChip.BenefactorCard,ScoreType.ScoreBlue);
			break;
		case Discard3CardsAnd1WineFor3VP: //entertainer
			{
			//p1("use entertainer");
			Assert(pb.nSelectedCards()==3 && pb.selectedCells.size()==1,"expect selected");
			removeTop(pb.selectedCells.pop(),replay);
			discardSelectedCards(pb,replay);
			changeScore(pb,3,replay,Entertainer,ViticultureChip.EntertainerCard,ScoreType.ScoreBlue);
			}
			break;
       	case Sell1VPfor3: // banker
       		switch(cardResolution)
       		{
       		case Choice_A:
       			changeScore(pb,-1,replay,Banker,ViticultureChip.BankerCard,ScoreType.ScoreYellow);
       			changeCash(pb,3,yokeCash,replay);
       			String msg = pb.getRooster().colorPlusName()+" + $2 $1 - 1VP ";
       			logRawGameEvent(msg);
       			break;
       		default: break;
       		}
       		
       		do
       		{   nextState = setNextPlayerInCycle();	
       			pb = pbs[whoseTurn]; 
       		} while(revision>=134 && pb.score<=MIN_SCORE && nextState!=null) ;
       		pb = pbs[whoseTurn];
       		break;
       	case Give2orVP:
       	case ResolveCard_AorBorBoth:
       	case ResolveCard_AorBorDone:
       	case ResolveCard_2of3:
       	case ResolveCard:
       		if(cardResolution==null) { cardResolution=ViticultureId.Choice_Done; }
    		switch(cardBeingResolved.type)
  		   	{
  		   	case PapaCard:
  		   		Assert(cardBeingResolved==pb.papa, "wrong card %s",cardBeingResolved);
  		   		m.cards = push(pb.mama,pb.papa); 
  		   		// some papas give a card
  		   		pb.resolvePapa(cardResolution==ViticultureId.Choice_A);
  		   		// add the cards from m&p to the replay move
  		   		for(int i=0;i<pb.cards.height(); i++) { m.cards = push(m.cards,pb.cards.chipAtIndex(i)); }
  		   		break;
  		   	case YellowCard:
  		   		Assert(cardBeingResolved.type==ChipType.YellowCard, "wrong kind of card %s",cardBeingResolved);
  		   		nextState = resolveYellow(pb,cardBeingResolved,cardResolution,replay,m);
 		   		break;
  		   	case BlueCard:
  		   		Assert(cardBeingResolved.type==ChipType.BlueCard, "wrong kind of card %s",cardBeingResolved);
  		   		nextState = resolveBlue(pb,cardBeingResolved,cardResolution,replay,m);
 		   		break;
  		   	case ChoiceCard:
  		   		nextState = resolveChoice(pb,cardBeingResolved,cardResolution,replay,m);
  		   		break;
 		   	default: G.Error("Not expecting %s",cardBeingResolved);
  		   	}
		   		unselect();
		   		break;
    		
        case SelectWakeup:	// new year
        	break;
    	
        // various ways to trade wines for cash
        case Age1AndFill:
        	{	ageWine(pb,pb.selectedCells.pop(),replay);       		
        		nextState = ViticultureState.FillWineOptional;
        	}
        	break;
        case Age1Twice:
        	ViticultureCell agedTo = ageWine(pb,pb.selectedCells.pop(),replay);
        	if(agedTo!=null) { ageWine(pb,agedTo,replay); }
        	break;
        case Age2Once:
        	{	ViticultureCell older = pb.selectedCells.pop();
        		ViticultureCell younger = (pb.selectedCells.size()>0) ? pb.selectedCells.pop() : null;
        		if((younger!=null) && (older.row<younger.row))
        		{	// make sure we age the higher number wine first
        			// so it will make room for the young'un to age
        			ViticultureCell c = older;
        			older = younger;
        			younger = c;
        		}
        		ageWine(pb,older,replay);
        		if(younger!=null) { ageWine(pb,younger,replay); }
        	}
        	break;
        case DiscardGrapeOrWine:
        case DiscardWineForCashAndVP:
        case DiscardWineFor3VP:
        case DiscardWineFor2VP:
        case DiscardGrapeAndWine:
        case DiscardWineFor4VP:
        case Discard2GrapesFor3VP:
        case DiscardGrapeFor3And1VP:
        case DiscardGrapeFor2VP:
        case Sell1Wine:	// sell one
        case Sell1WineOptional:
        	{
        	ViticultureCell sold = null;
        	CellStack selectedCells = pb.selectedCells;
 		    int nCellsTargeted = 0;
		    int nCardsTargeted = 0;
		    int nGrapesTargeted = 0;
		    int nWinesTargeted = 0;
  			switch(resetState)
			{
   			default: throw G.Error("Not expeting %s", resetState);
			case DiscardGrapeOrWine:
			case Sell1Wine:
			case Sell1WineOptional:
			case DiscardWineFor2VP:
			case DiscardWineForCashAndVP:
			case DiscardGrapeFor2VP:
			case DiscardWineFor4VP:
			case DiscardGrapeFor3And1VP:
			case DiscardWineFor3VP:
				nCellsTargeted = 1;
				break;
			case Discard2GrapesFor3VP:
				nCellsTargeted = 2;
				nGrapesTargeted = 2;
				break;
			case DiscardGrapeAndWine:
				nGrapesTargeted = 1;
				nWinesTargeted = 1;
				nCellsTargeted = 2;
				break;
			}    
   			if(nGrapesTargeted>0 || nWinesTargeted>0)
   			{	int ngrapes = 0;
   				int nwines = 0;
   				for(int lim=selectedCells.size()-1; lim>=0; lim--) 
   					{ ViticultureCell c = selectedCells.elementAt(lim);
   					  ViticultureChip top = c.topChip();
   					  switch(top.type)
   					  {
   					  case RedGrape:
   					  case WhiteGrape:
   						  ngrapes++;
   						  break;
   					  case WhiteWine:
   					  case RedWine:
   					  case RoseWine:
   					  case Champagne:
   						  nwines++;
   						  break;
   					  default: 
   						  G.Error("Not expecting %1", top.type);
   					  }
   					}
   				Assert(ngrapes==nGrapesTargeted,"expected %s grapes",nGrapesTargeted);
   				Assert(nwines==nWinesTargeted,"expected %s wines",nWinesTargeted);
   			}
   			Assert(nCellsTargeted==selectedCells.size(),"should have selected %s cells",nCellsTargeted);
   			Assert(nCardsTargeted==pb.nSelectedCards(),"should have selected %s cards",nCellsTargeted);
   						
   			discardSelectedCards(pb,replay);
  
   			while(pb.selectedCells.size()>0)
        		{
        		sold = pb.selectedCells.pop();
        		if(sold.topChip()!=null)
        		{
        		sold.removeTop();
	        	if(replay!=replayMode.Replay)
	    		{
	        		ViticultureCell dest = getCell(sold.rackLocation(),'@',0);
	        		animationStack.push(sold);
	        		animationStack.push(dest);
	    		}}}

        	switch(resetState)
        	{
        	case DiscardGrapeAndWine: // Ristorante
        		changeScore(pb,3,replay,RistoranteBonus,ViticultureChip.RistoranteCard,ScoreType.OrangeCard);
        		changeCash(pb,3,pb.destroyStructureWorker,replay);
        		break;
        	case DiscardGrapeFor3And1VP:
        		changeScore(pb,1,replay,CafeBonus,ViticultureChip.CafeCard,ScoreType.OrangeCard);
        		changeCash(pb,(revision>=102)?3:1,pb.destroyStructureWorker,replay);
           		break;
        	case DiscardGrapeOrWine: //promoter
        		changeResidual(pb,1,replay);
        		changeScore(pb,1,replay,Promoter,ViticultureChip.PromoterCard,ScoreType.ScoreBlue);
        		break;
        	case DiscardWineForCashAndVP: // taster
        		{
        		changeCash(pb,4,yokeCash,replay);
        		boolean best = true;
        		// bug fix, was basing on sell wine value verses wine quality
        		// also, overlapping bug, hasWineWithValue was off by 1, so thats the 1+ in the legacy version
	        	int value = revision>=103 
	        						? ViticultureChip.minimumWineValue(sold.rackLocation())+sold.row
	        						: 1+ViticultureChip.wineSaleValue(sold.rackLocation(),revision);
        		for(PlayerBoard p : pbs)
        		{	//p1("best value discard payoff");
        			if(p.hasWineWithValue(value)) { best=false; } 
        		}
        		if(best) { changeScore(pb,2,replay,Taster,ViticultureChip.TasterCard,ScoreType.ScoreBlue); }
        		}
        		break;
        	case DiscardWineFor3VP:	// judge
        		{
	        	int value = ViticultureChip.minimumWineValue(sold.rackLocation())+sold.row;
        		if(value>=4) { changeScore(pb,3,replay,Judge,ViticultureChip.JudgeCard,ScoreType.ScoreBlue); }
        		else { G.Error("shouldn't discard for no benefit"); }
        		}
        		break;
        	case DiscardWineFor2VP: // governess or tap room
        		changeScore(pb,2,replay,triggerCard.cardName,triggerCard,
        				triggerCard==ViticultureChip.GovernessCard ? ScoreType.ScoreBlue : ScoreType.OrangeCard);
        		break;
        	case DiscardWineFor4VP: //wine critic
        		{
	        	int value = ViticultureChip.minimumWineValue(sold.rackLocation())+sold.row;
        		if(value>=7) { changeScore(pb,4,replay,WineCritic,ViticultureChip.WineCriticCard,ScoreType.ScoreBlue); }
        		else { G.Error("Shouldn't discard for no benefit"); }
        		}
        		break;
        	case Sell1Wine:
        	case Sell1WineOptional:
        		{
        		ViticultureId rack = sold.rackLocation();
	        	int value = ViticultureChip.wineSaleValue(rack,revision);
        		changeScore(pb,value,replay,SellWine,ViticultureChip.getChip(rack),ScoreType.WineSale);
        		}
        		break;
        	case Discard2GrapesFor3VP:
        		changeScore(pb,3,replay,SellTavern,ViticultureChip.TavernCard,ScoreType.OrangeCard);
        		break;
        	case DiscardGrapeFor2VP:
        		changeScore(pb,2,replay,SellGrape,ViticultureChip.ExporterCard,ScoreType.OrangeCard);
        		break;
        		
        	default:
        		pb.selectedCells.push(sold);	// if debugging, undo side effect
        		G.Error("Not expecting %s",resetState);
	        }
        	}
        	break;
        case TakeActionPrevious:
        	{
        	ViticultureCell where = pb.selectedCells.pop();
        	nextState = performDropAction(pb,where,null,replay,m,false);	//there is no worker
        	}
        	break;
    	case PlayBonus:
    		if(droppedDestStack.size()==2)
    		{
    		boolean fallThrough =false;
    		ViticultureCell where = droppedDestStack.pop();
    		ViticultureCell dest = droppedDestStack.top();
    		boolean bonus = isBonusRow(where);
    		Assert(bonus,"must be a bonus space");
    		boolean firstBonus = isBonusRow(dest);
    		if(firstBonus)
    		{	// in a few cases, two bonuses are being collected
    			switch(where.rackLocation())
    			{
    			case PlayBlueWorker:
    			case PlayYellowWorker:
    				// the real action is yellow+dollar, the bonus is two cards
    				changeCash(pb,1,yokeCash,replay);
    				break;
    			case SellWineWorker:
    				// the real action is +structureCard, bonus is place star
    				drawCards(1,structureCards,pb,replay,m);
    				break;
       			case FlipWorker:
    				// the real action is +1VP, the bonus is a structure card
       				//p1("check the bonus applied");
    				changeScore(pb,1,replay,AutomaBonus,ViticultureChip.VictoryPoint_1,ScoreType.Other);
    				break;
       			case HarvestWorker:
       				// real action is +1, bonus is a dollar, so we want to give
       				// him a dollar and do the usual action
       				changeCash(pb,1,yokeCash,replay);
       				fallThrough = true;
       				break;
       				
    			default: G.Error("Not expecting localtion %s",where.rackLocation());
    			}
    		}
    		// perform the action as the normal bonus 
    		if(!fallThrough)
    			{
    			nextState = performDropAction(pb,where,dest.topChip(),replay,m,false);
    			break; 
    			}
    		}
			//$FALL-THROUGH$
		case Play:
    		{
    		ViticultureCell where = droppedDestStack.top();
    		nextState = performDropAction(pb,where,where.topChip(),replay,m,false);
    		}
    		break;
    	case SwitchVines:
    		pendingMoves.clear();
    		break;
    	case PlaceWorkerFuture:
    		// placed ahead of time, don't do it now
    		{
    		ViticultureCell dest = droppedDestStack.top();
    		if(dest.height()>1)
    			{ //p1("planner bumps someone"); 
    			Assert(dest.topChip().type==ChipType.Chef,"must be a chef");
    			bumpPlayer(pb,dest,replay);
    			}
    		plannerCells.push(dest);
    		plannerMeeples.push(dest.topChip());
    		plannerMoves.push(m);
    		}
    		break;
    		
    	case FillWineBonus:
		case FillWineBonusOptional:
    	case FillWineFor2VPMore: 		
    	case FillMercado:
    	case FillWineOptional:	// fill wine order from a blue card, always optional
    		stateChange++;		// not filling the wine leaves state unchanged
			//$FALL-THROUGH$
		case FillWine:
    		{
    		if(pb.hasVeranda()) { changeScore(pb,1,replay,VerandaBonus,ViticultureChip.VerandaCard,ScoreType.OrangeCard); }
    		if(pb.hasWineParlor()) { changeCash(pb,2,yokeCash,replay); }
    		ViticultureChip card = pb.removeTopSelectedCard().card;
    		if(replay!=replayMode.Replay) { flashChip = card;	}			// if the viewer notices this card, it will flash up for 2 seconds
    		purpleDiscards.addChip(card);	// put on the discard pile, otherwise it would just vanish
    		pb.cards.removeChip(card);
    		pb.fillWineOrder(card,true,replay);
    		
    		}
    		switch(resetState)
    		{
    		case FillWineFor2VPMore:
				{changeCash(pb,-3,yokeCash,replay);
				 changeScore(pb,2,replay,LabelFactory,ViticultureChip.LabelFactoryCard,ScoreType.OrangeCard);
				}
				break;
    		case FillWineBonus:
			case FillWineBonusOptional:
				changeScore(pb,1,replay,FillWineBonus,ViticultureChip.VictoryPoint_1,ScoreType.WineOrder);
				break;
			default: break;
    		}

    		break;
		case DestroyStructure:
		case DestroyStructureOptional:
			{
			ViticultureChip ch = pb.removeTopSelectedCard().card;
			boolean found = false;
			for(ViticultureCell structure : pb.buildStructureCells)
				{
				int index = structure.height()-1;
				if(index>=0)
				{
				ViticultureChip top = structure.chipAtIndex(index);
				// if a worker is on the spot, dig under it.
				if(top.type.isWorker()) { index--; top=structure.chipAtIndex(index); }
				if(top==ch)
				{	found = true;
					structure.removeChipAtIndex(index);
					if(replay!=replayMode.Replay)
						{
						animationStack.push(structure);
						animationStack.push(structureDiscards);
						}
					break;
					}
				}}
				Assert(found, "didn't find %s",ch);
			}
			break;
		case Pick2Discards:
			{
				for(ViticultureCell stack : discardStacks) 
					{ pb.findAndClaimSelected(stack, replay,false); 
					}
			}
			break;
		case Pick2TopCards:
			{
			for(ViticultureCell stack : cardStacks)
			{	boolean some = pb.findAndClaimSelected(stack,replay,testOption(Option.DrawWithReplacement));					
				if(!some && (stack.topChip()!=null) )
				{ // discard the top card, someone saw it.
				  ViticultureCell discards = getDiscards(stack);
				  discards.addChip(stack.removeTop());
				  if(replay!=replayMode.Replay)
				  {	
					  animationStack.push(stack);
					  animationStack.push(discards);
				  }
				}
			}
			}
			break;
		case GiveYellow:
			{
			ViticultureChip card = pb.removeTopSelectedCard().card;
			pb.cards.removeChip(card);
			ViticultureCell dest = pbs[targetPlayer].cards;
			dest.addChip(card);


			if(replay!=replayMode.Replay)
			{
				animationStack.push(pb.cards);
				animationStack.push(dest);
			}
			}
			break;
		case StealVisitorCard:
			{
    			Viticulturemovespec mm2 = (Viticulturemovespec)pendingMoves.pop();	// should be a select rooster cell
    			PlayerBoard toplayer = pbs[mm2.from_col-'A'];
    			changeCash(toplayer,1,pb.cashDisplay,replay);
    			changeCash(pb,-1,pb.cashDisplay,replayMode.Replay);	// no animation
     			long seed = revision>=120 ? toplayer.cards.CardDigest()^randomKey : Digest();
    			Random r = new Random(seed);
    			//p1("steal card "+moveNumber);
    			ViticultureChip card = toplayer.takeRandomVisitorCard(r,mm2.from_index);
    			pb.cards.addChip(card);
    			if(replay!=replayMode.Replay) 
    				{
    				pb.flashChip = card;
    				toplayer.flashChip = card; 
    				}
    			logGameEvent(InnkeeperSteal,card.type.toString());
    			if(replay!=replayMode.Replay)
    			{
    				animationStack.push(toplayer.cards);
    				animationStack.push(pb.cards);
    			}
    			
			}
			break;
        case PayWeddingParty:
        	{
        	while(pendingMoves.size()>0)
        		{
        			Viticulturemovespec mm2 = (Viticulturemovespec)pendingMoves.pop();	// should be a select roostecell
        			PlayerBoard toplayer = pbs[mm2.from_col-'A'];
        			changeCash(pb,-2,toplayer.cashDisplay,replay);
        			changeCash(toplayer,2,pb.cashDisplay,replay);
        			changeScore(pb,1,replay,"Wedding party",ViticultureChip.WeddingPartyCard,ScoreType.ScoreYellow);
        		}
        	}
        	setState(ViticultureState.Confirm);
   		break;
   		
		case Select2Of2FromMarket:
		case Select2Of3FromMarket:
		case Select1Of1FromMarket:
		case Select1Of2FromMarket:
			{
			int cost = pb.committedCost();
			changeCash(pb,-cost,pb.cashDisplay,replay);
			while(pb.selectedCards.size()>0)
				{
					int ind = pb.removeTopSelectedCard().index;
					ViticultureChip ch = pb.oracleCards.removeChipAtIndex(ind);
					pb.cards.addChip(ch);  		
					if(replay!=replayMode.Replay)
						{
						animationStack.push(cardPile(ch));
						animationStack.push(pb.cards);
						}
				}
			// discard the other cards
			while(pb.oracleCards.height()>0)
			{	ViticultureChip ch = pb.oracleCards.removeTop();
				discardPile(ch).addChip(ch);
				if(replay!=replayMode.Replay) {
					animationStack.push(cardPile(ch));
					animationStack.push(discardPile(ch));
				}
				}
			}
			break;
			
        case TakeYellowOrVP:
    	{	
    		addContinuation(Continuation.TakeMoreYellow);
    	}
    	break;   	
    	case TrainWorker:
    	case TrainWorkerOptional:
    	case TrainWorkerDiscount1:
    	case TrainWorkerDiscount2:
    	case TrainWorkerDiscount3:
    	case TrainWorkerDiscount4:
    	case TrainWorkerDiscount1AndUse:
    	case TrainWorkerAndUseFree:
    		{
    		Viticulturemovespec worker = (Viticulturemovespec)pendingMoves.pop();
    		ViticultureChip chip = ViticultureChip.getChip(ChipType.values()[worker.from_index],pb.color);
    		finishTraining(pb,chip,replay);
    		}
    		break;
    	case FullPass:
    	case MisplacedMessenger:
    		break;
    	case SelectPandM:
    		pb.cards.reInit();			// throw away the preview cards
    		while(pb.selectedCards.size()>0)
    		{
    			ViticultureChip ch = pb.selectedCards.pop().card;
    			switch(ch.type)
    			{
    			default: throw G.Error("Not expecting ",ch);
    			case MamaCard:
    				pb.mama = ch;
    				pb.resolveMama();
    				break;
    			case PapaCard:
    				pb.papa = ch;
    				pb.resolvePapa(choiceA.isSelected());
    				break;
    			}
    		}
    		break;
    	default:
    		if(resetState.isWinemaking())
    		{
    		int allowedCount = bottleCount(resetState);
    		int nmade = pendingMoves.size();
    		Assert(allowedCount>=nmade,"expected bottle count wrong");
    		// wines have already been made
    		switch(resetState)
    		{
    		case Make3WinesVP:
    			int typeMask = 0;
    			//p1("Bottler bonus");
    			for(int lim=nmade-1; lim>=0; lim--)
    			{	Viticulturemovespec mv = (Viticulturemovespec)pendingMoves.elementAt(lim);
    				switch(mv.dest)
    				{
    				case RedWine: typeMask |= 1; break;
    				case WhiteWine: typeMask |= 2; break;
    				case RoseWine: typeMask |= 4; break;
    				case Champaign: typeMask |= 8; break;
    				default: G.Error("Not expecting %s", m.dest);
    				}
    			}
    			int ntypes = G.bitCount(typeMask);
    			changeScore(pb,ntypes,replay,BottlerBonus,ViticultureChip.BottlerCard,ScoreType.ScoreBlue);
    			break;
    		case Make2WinesVP:
    			//p1("supervisor");
    			int nChamp = 0;
    			for(int lim=nmade-1; lim>=0; lim--)
    			{	Viticulturemovespec mv = (Viticulturemovespec)pendingMoves.elementAt(lim);
    			    if (mv.dest == ViticultureId.Champaign) { nChamp++; } 
    			}
    			changeScore(pb,nChamp,replay,SupervisorBonus,ViticultureChip.SupervisorCard,ScoreType.ScoreBlue);
    			break;
    		 

    	   	case MakeMixedWinesForVP:
    			if(nmade>0) 
    			{	// p1("mixer bonus");
    				changeScore(pb,1,replay,MixerBonus,ViticultureChip.MixerCard,ScoreType.OrangeCard);
    			}
    			break;
        	case Make2Draw2:
        		{
        		int wt = whoseTurn;
        		int tp = targetPlayer;
        		
 				do {
 				nextState = setNextPlayerInCycle();
 				pb = pbs[whoseTurn];
 				}
 				while( (revision>=138) && (nextState!=null) && !canMakeWine(pb));

 				if(nmade>0 && (wt!=tp)) 
            	{ 	//p1("make 2 draw 2 payoff");
     				if(revision>=127)
    				{
     				suspendedWhoseTurn = whoseTurn;
     				whoseTurn = tp;
    				addContinuation(Continuation.NextPlayerInMentorCycle); 
        			nextState = ViticultureState.TakeYellowOrGreen;	
        			break;
    				}
    				else
    				{
    				if(drawForOracle(pb,replay,m))
    		       		{	pb.oracleColors = new ViticultureCell[] {greenCards,yellowCards};
    	       				reshuffleIfNeeded(pb.oracleColors);
    	       				nextState = ViticultureState.SelectCardColor;
    		        		//p1("oracle 2-2");
    		       		}
    		       		else
    		       		{
    		       		drawCard(greenCards,pbs[tp],replay,m);
    		       		drawCard(yellowCards,pbs[tp],replay,m);
    				}}
            	}}
    			break;
        	case Make2AndFill:
        		pendingMoves.clear();
        		nextState = ViticultureState.FillWineOptional;
        		break;
        	default: break;
    		}
       		pendingMoves.clear();
    		}
    		else
    		{
    		G.Error("Not expecting confirm after %s",resetState);
    		}
    	}
    	return(nextState);
    }
    private void doDone(replayMode replay,Viticulturemovespec m)
    {	
    	ViticultureState nextState = null;
    	{
		PlayerBoard pb = getCurrentPlayerBoard();
  	
		if(resetState.isWinemaking()) { setState(ViticultureState.Confirm); };
		switch(board_state)
		{
		case ResolveCard_AorBorDone:
		case PlayBonus:
		case MisplacedMessenger:
			 setState(ViticultureState.Confirm);
			 break;
		default: break;
		}; 
    	// handle the now-confirmed side effects
		
		if(revision>=113) 
		{ 	switch(board_state)
			{
			case PayWeddingParty:
				if(revision<117) { break; }	// was broken
			//$FALL-THROUGH$
			case Harvest3Optional:
			case Harvest2AndMake3:
			case Harvest2Optional:
				// harvesting less than the maximum possible and permitted fields
				setState(ViticultureState.Confirm);
				break;
			case Move2Star:
				if(revision>=123)
				{
					setState(ViticultureState.Confirm);
					addContinuation(Continuation.Place1Star);	// this will evaluate to move1star
				}
				break;
			case ChooseOptions:
				optionsResolved = true;
				doInitAfterOptions(true);
				setInitialWakeupPositions(m.player);
				break;
			default: break;
			}
   		}

		switch(board_state)
        {
        case Resign:
        	Assert(players_in_game<=2,"must not be a multiplayer game");
    		addInfluenceScores(replay);
    		int next = pb.boardIndex^1;
        	if(next<win.length) { win[next] = true; }
    		setState(ViticultureState.Gameover);
    		break;
        case FullPass:
        	if(replay!=replayMode.Replay) { unselect(); }
        	nextState = passToNextSeason(getCurrentPlayerBoard(),replay,m);
        	break;
        case Discard2GrapesFor3VP:
        	// if we get here they clicked on done before selecting 2 grapes
        	setState(ViticultureState.Confirm);
        	break;
    	case Confirm:
        	nextState = handleConfirm(pb,replay,m);
           	unselect(); 
           	break;
        case TakeYellowOrVP:
        	{	
        		addContinuation(Continuation.TakeMoreYellow);
        	}
        	break;
       case FillWineOptional:
        	stateChange++;
        	setState(ViticultureState.Confirm);
        	break;

        case Sell1Wine:			// mercado induced
        case ResolveCard:
        case Trade1:
        case Trade2:
        case TradeSecond:
        case Harvest1:
        case HarvestOrUproot:
        case BuildStructure:	// used bonus to force build
        	setState(ViticultureState.Confirm); 	// shouldn't be allowed, but it has happened 
        	break;
        case ChooseOptions:
        	setNextPlayState(replay);
        	nextState = board_state;
        	break;
		default:
        	if(DoneState() || (board_state==ViticultureState.Plant1AndGive2)) 
        	{	// planner and messenger can leave you in an otherwise illegal state
        		if(targetPlayer>=0) 
        		{
        			switch(resetState)
        			{
        			//case PlantSecondVine:	setState(ViticultureState.Confirm); break;
        			case TrainWorkerDiscount3:	// declined to train for guest worker
        				break;
        			case Make2Draw2:			// declined "mentor" to make wines and give cards
         				do {
         				if(nextState!=null) {    p1("make 2 draw 2 skipped"); }
         				nextState = setNextPlayerInCycle();
         				pb = pbs[whoseTurn];
         				if((nextState!=null)&&!pb.hasGrape()) { p1("make2 no grapes"); }
        				} while((revision>=134) && !pb.hasGrape() && (nextState!=null));
        				if(nextState==null) { setState(ViticultureState.Confirm); }
        				break;
        			case BuildAtDiscount2forVP:	// declined to build "handyman"
        				
        				do {
        				nextState = setNextPlayerInCycle();
          				pb = pbs[whoseTurn];
        				} while ((revision>=134) && !pb.canBuildStructureWithDiscount(2) && (nextState!=null));
        				if(nextState==null) { setState(ViticultureState.Confirm); }
        				break;
        			case Plant1AndGive2:		// declined to plant
        				do {
        				nextState = setNextPlayerInCycle();
         				pb = pbs[whoseTurn];
        				} while((revision>=134) && !canPlant(pb) && (nextState!=null));
        				if(nextState==null) { setState(ViticultureState.Confirm); }
        				break;
        			default: G.Error("Expecting a cycle of players in %s", resetState);
        			}
        		}
        		else { 
        			setState(ViticultureState.Confirm); 
          			} 

        		pendingMoves.clear();
        	}
        	else { 	G.Error("Not expecting state %s", board_state); }
        }
    	}
    	{	ViticultureCell d = droppedDestStack.top();
    		ViticultureChip dtop = d!=null ? d.topChip() : null;
    		if(dtop!=null && dtop.type.isWorker())
    		{
    			lastDroppedWorker = d;
    			lastDroppedWorkerIndex = d.height()-1;
    		}
    	}
        acceptPlacement();
        {
        if(nextState!=null) 
        		{ resetState = nextState; 
        		  setState(nextState); }
        else if(!GameOver())
        		{
        		doContinuation(pbs[whoseTurn],replay,m);
        		}
        prepareWineDisplay(board_state,getCurrentPlayerBoard());
        }
    }
    
    public int harvestCount(PlayerBoard pb)
    {
    	CommonMoveStack all = new CommonMoveStack();
    	addHarvestMoves(pb,all,MoveGenerator.All);
    	return(all.size());
    }
	private void changeWakeup(PlayerBoard pb,ViticultureCell rc,replayMode replay)
	{
    	ViticultureCell old = pb.wakeupPosition;
    	rc.insertChipAtIndex(0,old.removeTop());	// watch out for space occupied by the gray meeple or the grape
    	if(automa)
    	{
    		if(rc.topChip()==ViticultureChip.Bead)
    		{	ViticultureCell target = (old.col=='A') ? old : pb.bonusActions;
    			target.addChip(rc.removeTop());
    		}
    	}
    	pb.wakeupPosition = rc;
    	if((rc.col!='A') || (year<=0)) { pb.activeWakeupPosition = rc; }
    	pb.setSeason(rc.col-'A');
       	if(replay!=replayMode.Replay)
    	{
    	animationStack.push(old);
    	animationStack.push(rc);
    	}
	}
	public boolean invalidFieldSwap(PlayerBoard pb)
	{
		return((pendingMoves.size()==2) && (findSwitchMove(pb)==null));
	}
	public boolean validFieldSwap(PlayerBoard pb)
	{
		return((pendingMoves.size()==2) && (findSwitchMove(pb)!=null));
	}

	private void handleSelect(PlayerBoard pb ,Viticulturemovespec m,replayMode replay)
	{	
       int maxSelect = 0;
       ViticultureState state = resetState;
       if(state.isBuilding()) { state = ViticultureState.BuildStructure; }	// consolidate
   	   switch(state)
   	   {
   	   case Uproot1For2:
   	   case StealVisitorCard:
   		   if(!movestackContains(m,pendingMoves)) { pendingMoves.clear(); }	// enforce a single selection
   		   addtoMovestack(m,pendingMoves);
   		   setState(pendingMoves.size()==1 ? ViticultureState.Confirm : resetState);
   		   break;
   	   case SwitchVines:
   		   addtoSingleMovestack(m,pendingMoves,false);
   		   boolean confirm = validFieldSwap(pb);
		   setState(confirm ? ViticultureState.Confirm : resetState);
   		   break;
   	   case Uproot2For3:
   		   addtoMovestack(m,pendingMoves);
		   setState(pendingMoves.size()==2 ? ViticultureState.Confirm : resetState);
		   break;
  	   case PayWeddingParty:
	   		{
	   		addtoMovestack(m,pendingMoves);
	   		int nsel = pendingMoves.size();
	   		boolean overpay =  (nsel*2)>pb.cash;
	   		boolean done =  (nsel==3) || (((nsel+1)*2)>pb.cash);
	   		setState( overpay
	   				? ViticultureState.PayWeddingPartyOverpay 
	   				: done ? ViticultureState.Confirm : resetState);
	   		}
	   		break;
   	   case TakeYellowOrVP:
   		   addtoMovestack(m,pendingMoves);
   		   switch(pendingMoves.size())
   		   {
   		    case 0: case 1: case 2: setState(resetState); break;
   		   	case 3: setState(ViticultureState.Confirm); break;
   		   	default: setState(ViticultureState.TakeYellowOrVPOver);
   		   }
   		   break;
   	   case	BuildStructure:		// all the consolidated build states
   		   pb.unselect();
   		   if(m.source==ViticultureId.Cards)
   		   {
   		   choice0.selected = false;
   		   
   		   pb.selectCardFromHand(m.from_index);
   		   
      	   if(pb.nSelectedCards()==0)
      	   	{  buildingSelection = null;
      		   setState(resetState);
      	   	}
      	   else
      	   	{
      		   buildingSelection =  m.Same_Move_P(buildingSelection)  ? null : m;
      		   setState(buildingSelection==null ? resetState : ViticultureState.Confirm);
      	   	}
   		   }
   		   else
   		   {
   		   // choosing the tour option
   		   buildingSelection = m.Same_Move_P(buildingSelection)  ? null : m;
   		   choice0.selected = buildingSelection!=null;
  		   pb.clearSelectedCards();
  		   
   		   setState(buildingSelection==null ? resetState : ViticultureState.Confirm);
  		   }
   		   break;
   	   case Harvest3Optional:
   		   maxSelect++;
   		//$FALL-THROUGH$
   	   case Harvest2:
   	   case HarvestMoreThan2:
   	   case Harvest2Optional:
   	   case Harvest2AndMake3:
   		   maxSelect++;
			//$FALL-THROUGH$
   	   case Harvest1Dollar:
   	   case Harvest1:
   	   case HarvestOrUproot:
   	   case HarvestMoreThan1:
   	   case HarvestAndMakeWine:
   	   case HarvestAndFill:
   	   case Harvest1Optional:
   	   		{  maxSelect++;
   	   		   int nFields = harvestCount(pb);
   	   		   maxSelect = pb.hasHarvestMachine() ? nFields : Math.min(maxSelect, nFields);
			   CellStack dest = pb.selectedCells;
			   ViticultureCell field = getCell(m.source,m.from_col,m.from_row);
			   ViticultureCell removed = dest.remove(field,false);
			   if(removed==null) 
			   		{ if(maxSelect<=1) { dest.clear(); }
			   		  dest.push(field); 
			   		}
			   int nSelected = dest.size();
			   
			   if(revision>=113)
			   {
			   if(nSelected==maxSelect) { setState(ViticultureState.Confirm); }
			   else if(nSelected<maxSelect) { setState(resetState); }
			   else if(maxSelect==1) { setState(ViticultureState.HarvestMoreThan1); }
			   else if(maxSelect==2) { setState(ViticultureState.HarvestMoreThan2); }
			   else { G.Error("can't find next state"); }
			   }
			   else
			   {   // before rev 113, you could exit with too many fields
				   setState( (nSelected==maxSelect)
						   	   		? ViticultureState.Confirm
						   	   		: resetState);
			   }
  	   		}
   		   break;
   	   case DestroyStructureOptional:
   	   case DestroyStructure:
   	   		{
   		   ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
   		   ViticultureChip ch = from.chipAtIndex(m.from_index);
   		   { boolean removed = pb.selectedCards.remove(m.source,ch,m.from_row)!=null;
   		   	pb.clearSelectedCards();
   		   if(!removed) { pb.selectedCards.push(m.source,ch,m.from_row); }
   		   }
   		   setState((pb.nSelectedCards()==1) ? ViticultureState.Confirm : resetState);
  		   		  
   	   		}
   		   break;
  	   case FillWineBonus:
   	   case FillWineBonusOptional:
   	   case FillWineFor2VPMore:
   	   case FillWine:
   	   case FillMercado:
   	   case FillWineOptional:
   	   case Play1Blue:
   	   case PlaySecondBlue:
   	   case PlayBlueDollar:
   	   case Play2Blue:
   	   case Play1Yellow:
   	   case PlaySecondYellow:
   	   case PlayYellowDollar:
   	   case Play2Yellow:
   	   case GiveYellow:
   	   	{	// selecting a single card to play, out of a group of choices.
   	   		// the card's effect may or may not be achievable
   	   		   ViticultureCell cards = pb.cards;
          	   ViticultureChip ch = cards.chipAtIndex(m.from_index);
          	   if(revision>=127)
          	   {	// this is just a consistency check
          	   switch(state)
          	   {
          	   case FillWineBonus:
           	   case FillWineBonusOptional:
           	   case FillWineFor2VPMore:
           	   case FillWine:
           	   case FillMercado:
           	   case FillWineOptional:
           		   Assert(ch!=null && ch.type==ChipType.PurpleCard,"%s must be a wine order card",ch);
           		   break;
           	   case Play1Blue:
           	   case PlaySecondBlue:
           	   case PlayBlueDollar:
           	   case Play2Blue:
          		   Assert(ch!=null && ch.type==ChipType.BlueCard,"%s must be a blue card",ch);
          		   break;
           	   case Play1Yellow:
           	   case PlaySecondYellow:
           	   case PlayYellowDollar:
           	   case Play2Yellow:
           	   case GiveYellow:
         		   Assert(ch!=null && ch.type==ChipType.YellowCard,"%s must be a yellow card",ch); 
         		   break;
           	   default: G.Error("not expecting state %s",state);
          	   }}
          	   pb.selectCardFromHand(m.from_index);
          	   setState(pb.nSelectedCards()==0 ? resetState : ViticultureState.Confirm);
   	   	}
   	   	break;
   	   case Pick2Discards:
   	   case Pick2TopCards:
   	   case Plant1Vine:
   	   case Plant1VineNoLimit:
   	   case Plant1VineNoStructures:
   	   case Plant2Vines:
   	   case Plant2VinesOptional:
   	   case PlantVine4ForVP:
   	   case Plant1VineOptional:
   	   case Plant1AndGive2:
   	   case Plant1For2VPDiversity: // agriculturist
   	   case Plant1For2VPVolume: // grower
   	   case PlantSecondVine:
   	   case Flip:
   	   case FlipOptional:
   	   	{
   	   	   CellStack dest = pb.selectedCells;
		   switch(m.source)
   		   {
			   case Field:   
   		   case Vine:
   		   	{
   			   ViticultureCell field = getCell(m.source,m.from_col,m.from_row);
   			   ViticultureCell removed = dest.remove(field,false);
   			   dest.clear();	// enforce single selection
   			   if(removed==null) { dest.push(field); }
   		   	}
   		   	break;
   		   case YellowCards:
   		   case BlueCards:
   		   case GreenCards:
   		   case PurpleCards:
   		   case StructureCards:
   		   case YellowDiscards:
   		   case BlueDiscards:
   		   case GreenDiscards:
   		   case PurpleDiscards:
   		   case StructureDiscards:
   		   	{	// picking from places other than the player cards, select cards explicitly
   		   		ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
   		   		ViticultureChip ch = from.chipAtIndex(m.from_index);
   		   		if(pb.selectedCards.remove(m.source,ch,m.from_index)!=null)
   		   		{ }
   		   		else 
   		   		{ pb.selectedCards.push(m.source,ch,m.from_index); 
   		   		}
   		   		}
   		   	break;
			case Cards:
   		   		{	pb.selectCardFromHand(m.from_index);
   		   		}
   		   	break;
   		   default: G.Error("don't expect %s",m.source);
   		   }
			   switch(resetState)
			   {
			   case Flip:
				   setState( (dest.size()>0) ? ViticultureState.Confirm : resetState);
				   break;
			   case Pick2Discards:
				   setState( (!hasDiscardMoves(pb) || (pb.nSelectedCards()==2))
						   ? ViticultureState.Confirm : resetState);
				   break;
			   case Pick2TopCards:
			   		setState((pb.nSelectedCards()==2) ? ViticultureState.Confirm : resetState);
			   		break;
			   default:
				   setState( ((pb.nSelectedCards()>0) && (dest.size()>0)) ? ViticultureState.Confirm : resetState);
			   }
   	   	}
   		   break;
   	   case Sell1VPfor3:
   	   case Give2orVP:
   	   case ResolveCard_AorBorBoth:
   	   case ResolveCard_AorBorDone:
   	   case ResolveCard_2of3:
   	   case ResolveCard:
   	   	{  boolean singleChoice = true;
   	   	   switch(resetState)
   	   	   {
   	   	   default: break;
   	   	   case ResolveCard_2of3:
   	   	   case ResolveCard_AorBorBoth:
   	   	   case ResolveCard_AorBorDone:
   	   		   singleChoice = false;
   	   		   break;
   	   	   }
   		   switch(m.source)
   		   {
    		   case Choice_AandB:
    			   choiceB.selected = true;
    			   //$FALL-THROUGH$
    		   case Choice_A:
    			   if(singleChoice) { choiceD.selected = false; choiceB.selected = false; choiceC.selected = false; }  
    			   choiceA.selected = !choiceA.selected;
    			   break;
    			   
    		   case Choice_AandC:
    			   choiceA.selected = true;
    			   //$FALL-THROUGH$
    		   case Choice_C:	
    			   if(singleChoice) { choiceD.selected = false; choiceB.selected = false; choiceA.selected = false; }  
    			   choiceC.selected = !choiceC.selected;
    			   break;
  
    		   case Choice_BandC:
    			   choiceC.selected = true;
    			   //$FALL-THROUGH$
    		   case Choice_B:	
    			   if(singleChoice) { choiceD.selected = false; choiceA.selected = false; choiceC.selected = false; }  
    			   choiceB.selected = !choiceB.selected;
    			   break;
    		   	
    		   case Choice_D:	
    			   if(singleChoice) { choiceB.selected = false; choiceA.selected = false; choiceC.selected = false; }  
    			   choiceD.selected = !choiceD.selected;
    			   break;
    		   default: ;
   		   }
   		   setActionOrder(m.source);
   		   
   		   if(m.source==ViticultureId.Choice_0)
  		   	{ choice0.selected =  !choice0.selected;
  		   	  cardResolution = choice0.selected ? ViticultureId.Choice_0 : null;
  		   	}
   		   else { choice0.selected = false; }
   		   if(!choice0.selected)
   		   {
   			   cardResolution = null;
   			   if(choiceA.selected) { cardResolution = ViticultureId.Choice_A; }
 			   if(choiceB.selected) { cardResolution = ViticultureId.Choice_B; }
 			   if(choiceC.selected) { cardResolution = ViticultureId.Choice_C; } 
 			   if(choiceD.selected) { cardResolution = ViticultureId.Choice_D; } 
  		   switch(resetState)
   		   {
      		   case ResolveCard_2of3:
      			   if(choiceA.selected && choiceB.selected && choiceC.selected) 
      			   	{ switch(m.source)
      				   {
      				   	case Choice_A: choiceA.selected = false; break;
      				   	case Choice_B: choiceB.selected = false; break;
      				   	case Choice_C: choiceC.selected = false; break;
      				   	default: break;
      				   }
      			   	}
      			   if(choiceA.selected&&choiceB.selected) { cardResolution =  ViticultureId.Choice_AandB; }
      			   else if(choiceA.selected && choiceC.selected) { cardResolution = ViticultureId.Choice_AandC; }
      			   else if(choiceB.selected && choiceC.selected) { cardResolution = ViticultureId.Choice_BandC; }
      			   break;
      		   case ResolveCard_AorBorBoth:
      		   case ResolveCard_AorBorDone:
   		   		if(choiceA.selected&&choiceB.selected) { cardResolution = ViticultureId.Choice_AandB; } 
   		   		break;
      		   case Give2orVP:
      		   case Sell1VPfor3:
      		   case ResolveCard:
      			   break;
   		   default: G.Error("Not expecting state %s", resetState);
    		   }
   		   }
   		   if(choiceA.selected && choiceB.selected && choiceC.selected) { cardResolution = null; }
   		   
   		   if(singleChoice)
   			   { setState(cardResolution==null ? resetState : ViticultureState.Confirm);
   			   }
   		   else {
   			   if(cardResolution==null) { setState(resetState); }
   			   else {
   			   switch(cardResolution)
   			   {
   			   case Choice_0:
   			   case Choice_AandB:
   			   case Choice_AandC:
   			   case Choice_BandC:
   				   setState(ViticultureState.Confirm);
   				   break;
   			   case Choice_A:
   			   case Choice_B:
   			   case Choice_C:
   				   setState(ViticultureState.ResolveCard_AorBorDone);
   				   break;
   			   default: 
   				setState(resetState);
   			   	break;
   			   }}
   		   }
   	   	}
   		   break;
    
   	   case SelectPandM:
		   {
   			   ViticultureChip card = pb.cards.chipAtIndex(m.from_index);
   			   ViticultureId source = m.source;
   			   boolean change = false;
   			   switch(source)
   			   {
   			   case Cards:	source = ViticultureId.MamaCards;
   			   	break;
   			   case Choice_A:	
   				   	change = !choiceA.selected;
   				   	choiceA.selected=true; 
   				   	choiceB.selected=false;
   				   	source = ViticultureId.PapaCards; 
   			   		break;
   			   case Choice_B: 
   				    change = !choiceB.selected;
   				   	choiceA.selected=false; 
   				   	choiceB.selected=true; 
   				   	source = ViticultureId.PapaCards; 
   			   		break;			   
   			   default: break;
   			   }
   			   ViticultureChip removed = pb.selectedCards.remove(source,card,m.from_index);
   			   if(removed==null || change) 
   			   	{ pb.selectedCards.remove(source);	// remove any card with the same source
   			   	  pb.selectedCards.push(source,card,m.from_index);
   			   	}
   			   int haspapa = 0;
   			   int hasmama = 0;
   			   for(int i=0;i<pb.selectedCards.size(); i++)
   			   {
   				   CardPointer cp = pb.selectedCards.elementAt(i);
   				   switch(cp.card.type)
   				   {
   				   case MamaCard:	hasmama++; break;
   				   case PapaCard: 	haspapa++; break;
   				   default: G.Error("Not expecting "+cp.card.type);
   				   }
   			   }
   			   setState(((haspapa==1) && (hasmama==1))
   					   	? ViticultureState.Confirm 
   					   	: resetState);
   		   }
  		   
   		   break;
   	   case Select2Of2FromMarket:
   	   case Select2Of3FromMarket:
   	   case Select1Of1FromMarket:
   	   case Select1Of2FromMarket:
   		   {
   			   ViticultureChip card = pb.oracleCards.chipAtIndex(m.from_index);
   			   ViticultureChip removed = pb.selectedCards.remove(m.source,card,m.from_index);
   			   if(removed==null) { pb.selectedCards.push(m.source,card,m.from_index); }
   			   int finalh = resetState.nToTake();
   			   int committed = pb.committedCost();
   			   int nSelected = pb.selectedCards.size();
   			   setState(
   					   ((committed<=pb.cash) 
   					   	&& ((nSelected==finalh)
   							   || ((nSelected<finalh) && !canSelectMarketCards(pb))))
   					   	? ViticultureState.Confirm 
   					   	: resetState);
   		   }
   		   break;
   	   case Take2Cards:			//two cards, but 1 at a time
   	   case TakeYellowOrBlue:
   	   case TakeYellowOrGreen:
   	   case TakeCard:
   	   case SelectCardColor:
   		   maxSelect++;
   		   selectedCard = getCell(m.source,'@',0);
   		   ViticultureChip card = ViticultureChip.cardBack(m.source);
   		   // this really is selected cards not selectedCardIndex
   		   // we're selecting colors
   		   ViticultureChip removed = pb.selectedCards.remove(m.source,card,0);
   		   if(maxSelect==1) { pb.selectedCards.clear(); }
   		   if(removed==null) { pb.selectedCards.push(m.source,card,0); }
   		   setState(pb.selectedCards.size()==maxSelect ? ViticultureState.Confirm : resetState);
   		   break;
   	   case Trade1:
   	   case Trade2:
   	   case TradeSecond:

   	   	{  
   	   	if(m.source==ViticultureId.CardDisplay)
   	   		{
   	   		ViticultureChip cardsel = pb.cards.chipAtIndex(m.from_index);
   	   		if(pb.selectedCards.contains(pb.cards.rackLocation(),cardsel,m.from_index)) 
   	   			{ pb.selectedCards.remove(pb.cards.rackLocation(),cardsel,m.from_index); }
   			   else {   pb.selectedCards.push(pb.cards.rackLocation(),cardsel,m.from_index); }
   	   		}
   	   	else {
   		   switch(m.to_row)
   		   {
    		   case 0:	// from
   			   tradeFrom = m.source;
   			   //unselectUI();
   			   pb.unselectUI();
   			   pb.cards.selected = false;
   			   getCell(m.source,m.from_col,m.from_row).selected = true;
   			   break;
   		   case 1:	// to
   			   tradeTo = m.source;
   			   unselectUI();
   	    	   getCell(m.source,m.from_col,m.from_row).selected = true;
   	    	   break;
   		   default:	G.Error("Not expecting state %s",board_state);
   		   }}
   		   setState( ((tradeFrom!=null)
   				   		&& (tradeTo!=null)
   				   		&& ((tradeFrom!=ViticultureId.Cards)||(pb.nSelectedCards()==2))) 
   				   			? ViticultureState.Confirm 
   				   			: resetState);
   	   		}
   	   		break;
   	   		
    	   default: 
    		   if(resetState.isWinemaking())
    		   {	

    			   ViticultureCell ho = getCell(m.source,m.from_col,m.from_row);
    			   ho.cost = m.from_index;
    			   break;
    		   }
    		   G.Error("Not expecting state %s",resetState);
   	   }
	}
	private void setActionOrder(ViticultureId choice)
	{	
		 switch(choice)
 		   {
 		   default: break;
 		   case Choice_A:
  		   case Choice_B:
 		   case Choice_C:

 			   if(!(choice_HarvestFirst.selected || choice_MakeWineFirst.selected || choice_FillWineFirst.selected))
 			   	{
 				switch(choice)
 					{
 				default:break;
 				case Choice_A:	
 						choice_HarvestFirst.selected = true;
 						break;
 				case Choice_B:
 						choice_MakeWineFirst.selected = true;
 						break;
 				case Choice_C:  
 						choice_FillWineFirst.selected = true;
 						break;
 					}
 			   	}
 		   		break; 
 		   case Choice_HarvestFirst:
			   choice_HarvestFirst.selected = true;
 			   choice_MakeWineFirst.selected = false;
 			   choice_FillWineFirst.selected = false;
			   break;
 		   case Choice_MakeWineFirst:
			   choice_HarvestFirst.selected = false;
 			   choice_MakeWineFirst.selected = true;
 			   choice_FillWineFirst.selected = false;
 			   break;
 		   case Choice_FillWineFirst:
 			   choice_HarvestFirst.selected = false;
 			   choice_MakeWineFirst.selected = false;
 			   choice_FillWineFirst.selected = true;
 			   break;
 		   }
		 firstChoice = choice_HarvestFirst.selected 
				 		? ViticultureId.Choice_HarvestFirst 
				 		: choice_MakeWineFirst.selected
				 			? ViticultureId.Choice_MakeWineFirst
				 			: choice_FillWineFirst.selected 
				 				? ViticultureId.Choice_FillWineFirst
				 				: null;
	}
	public boolean underHarvest()
	{	PlayerBoard pb = pbs[whoseTurn];
		int nFields = harvestCount(pb);
		int nSelected  = pb.selectedCells.size()+pendingMoves.size();
		int maxSelect = 0;
		switch(resetState)
		{  default: break;
	   	   case Harvest3Optional:
	   		   maxSelect++;
	   		//$FALL-THROUGH$
	   	   case Harvest2:
	   	   case HarvestMoreThan2:
	   	   case Harvest2Optional:
	   	   case Harvest2AndMake3:
	   		   maxSelect++;
				//$FALL-THROUGH$
	   	   case Harvest1Dollar:
	   	   case Harvest1:
	   	   case HarvestOrUproot:
	   	   case HarvestMoreThan1:
	   	   case HarvestAndMakeWine:
	   	   case HarvestAndFill:
	   	   case Harvest1Optional:
	   		   maxSelect++;
	   		   return(nSelected<Math.min(nFields, pb.hasHarvestMachine() ? 3 : maxSelect));
		}
		return(false);
	}
	// return the number of allowed bottles
	private int bottleCount(ViticultureState state)
	{	int nwines = 0;
        switch(state)
        {
        case Make4WinesOptional:
        	nwines++;
			//$FALL-THROUGH$
		case Make3WinesVP:
        case Make3Wines:
        case Make3WinesOptional: nwines++;
			//$FALL-THROUGH$
		case Make2WinesOptional:
        case Make2WinesNoCellar:
        case Make2WinesVP:
        case Make2AndFill:
        case Make2Draw2:
        case MakeMixedWinesForVP:
        case Make2Wines: nwines++;
			//$FALL-THROUGH$
		case Make1WineOptional:
        case Make1Wines: nwines++;
        	break;
        default: G.Error("Not expecting %s",state);
        	
        }
        return(nwines);
	}
	
	boolean addToSelectedCells(CellStack selected,ViticultureCell src,int nCellsTargeted,
			boolean oneOrTwo,boolean grapeAndWine)
	{
			boolean radio = revision>=151;	// treat the selections as radio buttons
			int nGrapes = 0;
			int nWines = 0;
 			ViticultureCell removed = selected.remove(src,false);
			if(nCellsTargeted==1) { selected.clear(); }		// enforce a single selection 
			if(removed==null) { selected.push(src); }
			if(  radio
					&& (removed==null) 
					&& grapeAndWine 
					&& (selected.size()>1))
			{	boolean srcIsWine = src.rackLocation().isWine();
				// make sure only one of each type is selected
				for(int lim = selected.size()-1; lim>=0; lim--)
				{
					ViticultureCell c = selected.elementAt(lim);
					if((c!=src) && c.rackLocation().isWine()==srcIsWine)
					{
						selected.remove(lim);
					}
				}
			}
   			for(int lim=selected.size()-1; lim>=0; lim--)
   			{
   				ViticultureCell c = selected.elementAt(lim);
   				if(c.topChip()==null) { p1("no chip"); }
   				boolean isWine = c.rackLocation().isWine();
   				if(isWine) { nWines++; } else { nGrapes++; }
   			}
   			
			boolean confirm = (((selected.size()==nCellsTargeted)
									|| (oneOrTwo && selected.size()==1))
								&& (!grapeAndWine || ((nGrapes==1) && (nWines==1))));
			return(confirm);
	}
	public boolean allPlayersReady()
	{	if(board_state==ViticultureState.ChooseOptions)
		{
			for(PlayerBoard p : pbs) { if (!p.isReady) { return false; }}
			return true;
		}
		return false;
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	Viticulturemovespec m = (Viticulturemovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        m.cards = null;
        turnChangeSamePlayer = false;
        flashChip = null;
        PlayerBoard pb = getCurrentPlayerBoard();
        //G.print("E "+m+" for "+whoseTurn+" "+resetState); 
        switch (m.op)
        {
        case EPHEMERAL_COMMENCE:
        case MOVE_COMMENCE:
        	options.setMembers(m.from_row);
        	doDone(replay,m);
        	break;
        case EPHEMERAL_READY:
        	if(board_state==ViticultureState.ChooseOptions)
        	{	// ignore strays that arrive late
        		PlayerBoard nn = pbs[m.from_col-'A'];
        		nn.isReady = m.from_row!=0;
        	}
        	break;
        case EPHEMERAL_OPTION:
        	if(board_state==ViticultureState.ChooseOptions)
        	{// ignore strays that arrive late
        	Option op = Option.getOrd(m.from_row);
        	boolean was = testOption(op);
        	PlayerBoard cp = getPlayerBoard(m.from_col-'A');
        	if(m.to_row==0) 
        		{ clearOption(op);
        		  cp.selectedOptions.clear(op);
        		  cp.unSelectedOptions.set(op);
        		}
        		else 
        		{ setOption(op); 
        		  cp.selectedOptions.set(op);
        		  cp.unSelectedOptions.clear(op);
        		}
        	boolean is = testOption(op);
        	if(was!=is)
        		{
        		for(PlayerBoard p : pbs) {  p.isReady = false; }
        		}
        	}
        	break;
        case MOVE_NEXTSEASON:
        	{
        		setState(board_state==ViticultureState.FullPass ? resetState : ViticultureState.FullPass);
        	}
        	break;
        case MOVE_FILLWINE:
        	{
        		ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
        		ViticultureChip card = from.chipAtIndex(m.from_index);
        		Assert(card.type==ChipType.PurpleCard,"must be a wine order");
        		pb.clearSelectedCards();
        		pb.selectedCards.push(m.source,card,m.from_row);
        		setState(ViticultureState.Confirm);
        	}
        	break;
        	

        case MOVE_SELLWINE:       	
        	if(revision<151)
        	{
 			   CellStack dest = pb.selectedCells;
 			   ViticultureCell wine = getCell(m.source,m.from_col,m.from_row);
 			   ViticultureCell removed = dest.remove(wine,false);
 			   if(removed==null) { dest.push(wine); }
 			   setState(dest.size()==1 ? ViticultureState.Confirm : resetState);
 	        	break;
        	}
			//$FALL-THROUGH$
		case MOVE_AGEONE:
        case MOVE_DISCARD:
        	{
        	// load selectedCells and/ot selectedCards for later disposition
        	switch(m.source) 
        	{	case PlayerStructureCard:
        		case Field:
        		case Cards: 
        			{
                	ViticultureCell source = getCell(m.source,m.from_col,m.from_row);
                	@SuppressWarnings("unused")
					ViticultureCell selectedCards = null;	// this is a trap to make sure selectedCards isn't accidentally used
                	// we only need to discard 1 card, so automatically toggle to just one card
                  	boolean discard1 = resetState.chooseSingle()
    						|| ((resetState==ViticultureState.DiscardCards)&&(pb.cards.height()==8)); 

                	ViticultureChip ch = source.chipAtIndex(m.from_index);                	
                	ViticultureChip removed = pb.selectedCards.remove(m.source,ch,m.from_index);
                	if(discard1) { pb.clearSelectedCards();  }			// done discarding
                	if(removed==null) { pb.selectedCards.push(m.source,ch,m.from_index); }
                  	
                	boolean confirm = false;
                	switch(resetState)
                	{
                	case Discard1ForOracle:
                	case DestroyStructure:
                	case DiscardGreen:
                	case DestroyStructureOptional:
                		confirm = (pb.selectedCards.size()==1);
                		break;
                	case Discard3CardsAnd1WineFor3VP:
                		{
                		confirm = (pb.selectedCells.size()==1) && (pb.selectedCards.size()==3);
                		}
                		break;
                	case DiscardCards:
                		confirm = (pb.cards.height()-pb.selectedCards.size()<=7);
                		break;
                	case Discard4CardsFor3:
                		confirm = (pb.selectedCards.size()==4);
                		break;
                	case Discard2CardsForAll:      		
                	case Discard2CardsFor2VP:
                	case Discard2CardsFor1VP:
                	case Discard2Green:
                	case Discard2CardsFor4:
                		confirm = (pb.selectedCards.size()==2);
                		break;
                	default: G.Error("Not expecting %s",resetState);
                	}
                	setState( confirm ? ViticultureState.Confirm : resetState);
                	}
                	break;
        		case WineDisplay:
        		case RedWine:
        		case WhiteWine:
        		case RoseWine:
        		case Champaign:
        		case RedGrape:
        		case WhiteGrape:
        			{
        		    ViticultureCell src = getCell(m.source,m.from_col,m.from_row);  
        		    int nCellsTargeted = 0;
        		    int nCardsTargeted = 0;
        		    boolean grapeAndWine = false;
        		    boolean oneOrTwo = false;
           			switch(resetState)
    				{
           			default: throw G.Error("Not expecting %s", resetState);
           			case Sell1Wine:
           			case DiscardWineFor2VP:
           			case DiscardWineFor4VP:
           			case DiscardWineForCashAndVP:
           			case DiscardWineFor3VP:
           			case Sell1WineOptional:
           				nCellsTargeted = 1;
           				break;
           			case Age2Once:
           				nCellsTargeted = 2;
           				oneOrTwo = true;
           				break;
           			case DiscardGrapeAndWine:
           				nCellsTargeted = 2;
           				grapeAndWine = true;
           				break;
    				case DiscardGrapeOrWine:
    				case DiscardGrapeFor2VP:
    				case DiscardGrapeFor3And1VP:
    				case Age1Twice:
    				case Age1AndFill:
    					nCellsTargeted = 1;
    					break;
    				case Discard3CardsAnd1WineFor3VP: 
    					nCellsTargeted = 1;
    					nCardsTargeted = 3;
    					break;
    				case Discard2GrapesFor3VP:
    					nCellsTargeted = 2;
    					break;
    				}        		    
         			boolean conf = addToSelectedCells(pb.selectedCells,src,nCellsTargeted,oneOrTwo,grapeAndWine)
							&& ((resetState!=ViticultureState.Age2Once) || canAgeSelected(pb))
							&& (pb.selectedCards.size()==nCardsTargeted);

        			
          			setState(conf ? ViticultureState.Confirm : resetState);
        			}
        			break;
        		default: G.Error("Not expecting %s",m.source);
        		}
        	}
        	break;
        	
        case MOVE_BUILDCARD:
        case MOVE_BUILD:
        	{
        	ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
        	pb.clearSelectedCards();
         	
           	buildingSelection = m.Same_Move_P(buildingSelection) ? null : m;
           	pb.unselect();
           	ViticultureChip chip = from.topChip();
           	m.currentWorker = chip;
        	//if(G.debug()) { Assert(chip==null || (chip.type!=null && chip.color!=null),"currentworker bad");}
           	choice0.selected = false; 		// for the give tour option
           	from.selected = buildingSelection!=null;
           	setState(buildingSelection==null ? resetState : ViticultureState.Confirm);
        	}
        	break;
        case MOVE_NEWWAKEUP:
        	{	
        	selectedWakeup = getCell(ViticultureId.RoosterTrack,m.to_col,m.to_row);
        	unselect();
        	selectedWakeup.selected = true;
        	setState(ViticultureState.Confirm);
        	}
        	break;
        case MOVE_SELECTWAKEUP:
        	{
        	changeWakeup(pb,getCell(ViticultureId.RoosterTrack,m.to_col,m.to_row),replay);
          	setState(ViticultureState.Confirm);
        	}
        	break;
        case MOVE_STAR:
        	{
        	ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
        	ViticultureCell to = getCell(m.dest,m.to_col,m.to_row);
        	pickObject(from,m.from_index);
        	dropObject(to);
        	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(from);
        		animationStack.push(to);
        		}
        	starDropped2 = starDropped;
        	starDropped = to;
        	}
        	starPlacement = null;		// star placement is used for bonuses, no bonus when moving stars
        	setState(ViticultureState.Confirm);
        	break;
        	
        case MOVE_PLACE_STAR:
        	{
        	ViticultureCell from = starPlacement!=null 
        							? starPlacement 
        							: getCell(ViticultureId.PlayerStars,pb.colCode,0);
        	ViticultureCell to = getCell(m.dest,m.to_col,m.to_row);
        	ViticultureChip chip = from.removeTop();
        	to.addChip(chip);
        	if(replay!=replayMode.Replay)
        		{	animationStack.push(from);
        			animationStack.push(to);
        		}
        	
        	starPlacement = to;
        	m.currentWorker = chip;
        	if(G.debug()) { Assert(chip==null || (chip.type!=null && chip.color!=null),"currentworker bad");}
        	setState(ViticultureState.Confirm);
        	}
        	break;
        case MOVE_TRAIN:
        	{
        	if(!movestackContains(m,pendingMoves)) { pendingMoves.clear(); } 	// enforce a single selection
        	addtoMovestack(m,pendingMoves);
        	ViticultureChip chip = pb.getWorker();
        	m.currentWorker = chip;
        	if(G.debug()) { Assert(chip==null || (chip.type!=null && chip.color!=null),"currentworker bad");}
        	setState(pendingMoves.size()==1 ? ViticultureState.Confirm : resetState);
        	}
        	break;
 
       case MOVE_SELECT:
        	handleSelect(pb,m,replay);
    	   break;
       case MOVE_DONE:
         	doDone(replay,m);
            break;
       case MOVE_TRADE:
       		{
           		ViticultureCell from = getCell(m.source,m.from_col,0);	// from-index is used only in trading cards, as a card index
           		tradeFrom = m.source;
           		tradeTo = m.dest;
           		if(tradeFrom==ViticultureId.Cards)
           		{
           			pb.clearSelectedCards();      			
           			pb.selectedCards.push(m.source,from.chipAtIndex(m.from_index),m.from_index);
           			pb.selectedCards.push(m.source,from.chipAtIndex(m.from_row),m.from_row);
            	}
           		setState(ViticultureState.Confirm);
       		}
       		break;
       case MOVE_PLACE_WORKER:
       		{
       		ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
       		ViticultureCell to = getCell(m.dest,m.to_col,m.to_row);
       		ViticultureChip chip = pickObject(from,m.from_index);
       		m.currentWorker = chip;
        	if(G.debug()) { Assert(chip==null || (chip.type!=null && chip.color!=null),"currentworker bad");}
       		dropObject(to);
       		if(replay!=replayMode.Replay)
	        	{ animationStack.push(from);
	        	  animationStack.push(to); 
	        	}
	        setNextStateAfterDrop(to,replay);
       		}
    	   break;
        case MOVE_TAKEACTION:
        	{
        		ViticultureCell c = getCell(m.dest,m.to_col,m.to_row);
        		if(pb.selectedCells.contains(c)) { pb.selectedCells.remove(c, true); }
        		else { pb.selectedCells.clear(); pb.selectedCells.push(c); }
        		setState((pb.selectedCells.size()==1) ? ViticultureState.Confirm : resetState);
        	}
        	break;
        case MOVE_DROPB:
        	{
			ViticultureChip po = pickedObject;
			ViticultureCell src = pickedSourceStack.top(); 
			ViticultureCell dest =  getCell(m.dest,m.to_col,m.to_row);
			if((board_state!=ViticultureState.Puzzle)
					&& (revision>=106)
					&& (pickedObject.type!=ChipType.Chef) 
					&& (m.to_row<3)
					&& (dest!=dollarWorker)
					&& (dest.topChip()!=null) ) 
			{
				G.Error("Can't drop! Not Empty ",m+" "+year+":"+season(pb));
			}
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
	            dropObject(dest);
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(dest,replay);
				}
        	}
             break;

        case MOVE_PICK:
 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			ViticultureCell src = getCell(m.source,m.from_col,m.from_row);
 			if(isDest(src))
 				{ unDropObject();
 				}
 			else
 			{
        	// be a temporary p
 			int index = m.from_index;
 			if(board_state==ViticultureState.Retrieve1Current)
 			{	// when retrieving, make sure we retrieve one of our own color.
 				// games before rev 108 had a bug where you could retrieve grande,
 				// and in some cases someone elses grande.  No harm, but it looked strange.
 				// theoretically in rev 108 it can't happen at all.
 				for(int i=0;i<src.height();i++)
 				{
 					ViticultureChip ch = src.chipAtIndex(i);
 					if((revision>=108) ? pb.isMyRegularWorker(ch) : pb.isMyWorker(ch)) 
 						{ index = i; break; 
 						}
 				}
 			}
        	ViticultureChip chip = pickObject(src,index);
        	if(chip!=null && chip.type!=null && chip.color!=null)
        	{
        	m.currentWorker = chip;
        	}
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        	case PlayBonus:
        		triggerCard = null;
        		setState(resetState = ViticultureState.Play);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            ViticultureCell dest = getCell(m.dest,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else
            	{ dropObject(dest);
             	  if(dest.rackLocation()==ViticultureId.StarTrack)
            	  {
            		  starDropped2 = starDropped;
            		  starDropped = dest;
            	  }
          	     setNextStateAfterDrop(dest,replay); 
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            if(year<=0) { setInitialWakeupPositions(m.player); } 
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ViticultureState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay); 
            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?ViticultureState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(resetState = ViticultureState.Puzzle);
 
            break;
           
        case MOVE_MAKEWINE:
        	// TODO: coordinate MOVE_MAKEWINE by the robot with the UI display 
        	ViticultureCell firstWine = getCell(m.source,m.from_col,m.from_row);
        	int totalValue = 0;
        	Assert(firstWine.topChip()!=null,"first wine missing");
        	removeTop(firstWine, replay);
        	totalValue = m.from_row+1;
        	switch(m.dest)	// remove the grapes
        	{
        	case Champaign:	
        		{	
        		ViticultureCell thirdWine = getCell(ViticultureId.WhiteGrape,m.to_col,m.to_row);
            	Assert(thirdWine.topChip()!=null,"third wine missing");
            	totalValue += m.to_row+1;
            	removeTop(thirdWine,replay);
            	// from_index==-1 is a cheap champagne made with the charmat and only 1 red + 1 white
            	ViticultureCell secondWine = (m.from_index==-1) ? null : getCell(ViticultureId.RedGrape,m.from_col,m.from_index);
            	if((secondWine!=null) && (secondWine.topChip()!=null))
            		{
            		totalValue += m.from_index+1; 
            		removeTop(secondWine,replay);
            		}
            	else { Assert(pb.hasCharmat(),"second wine missing"); }
        		if(pb.hasPatio()) { changeCash(pb,2,yokeCash,replay); }
        		}
            	break;
        	case RoseWine:
        		{
            	ViticultureCell secondWine = getCell(ViticultureId.WhiteGrape,m.from_col,m.from_index);
            	Assert(secondWine.topChip()!=null,"second wine missing");
            	totalValue += m.from_index+1; 
            	removeTop(secondWine,replay);
            	if(pb.hasPatio()) { changeCash(pb,2,yokeCash,replay); }
        		}
        		break;
        	case WhiteWine:
        	case RedWine:
        		break;
        		default: G.Error("Not expecting %s", m.dest);
        	}
            
        	// NB: some cards let you place wines without a cellar
        	int maxIndex = ((resetState == ViticultureState.Make2WinesNoCellar) || pb.hasBothCellars()) ? 8 : pb.hasMediumCellar() ? 5 : 2;
            switch(m.dest)
            {
            case Champaign:
            	placeGrapeOrWine(pb.champagne,ViticultureChip.Champagne,Math.min(maxIndex-6, totalValue-7),replay);
            	break;
            case RoseWine:
            	placeGrapeOrWine(pb.roseWine,ViticultureChip.RoseWine,Math.min(maxIndex-3,totalValue-4),replay);
            	break;
            case WhiteWine:
            	placeGrapeOrWine(pb.whiteWine,ViticultureChip.WhiteWine,Math.min(maxIndex,totalValue-1),replay);
            	break;
            case RedWine:
            	placeGrapeOrWine(pb.redWine,ViticultureChip.RedWine,Math.min(maxIndex,totalValue-1),replay);
            	break;
            default: break;
            }  
           	if(totalValue>=7 && pb.hasPenthouse()) 
           	{ changeScore(pb,1,replay,PenthouseBonus,ViticultureChip.PenthouseCard,ScoreType.OrangeCard); 
           	}

            pendingMoves.push(m);
            int nwines = bottleCount(resetState);
 
            setState(pendingMoves.size()==nwines ? ViticultureState.Confirm : resetState);

        	break;
        case MOVE_PLANT:
        	{
        	ViticultureCell from = getCell(m.source,m.from_col,0);
        	ViticultureCell to = getCell(m.dest,m.to_col,m.to_row);
        	ViticultureChip c = from.chipAtIndex(m.from_index);
        	pendingMoves.clear();
        	pb.clearSelectedCards();
        	pb.selectedCards.push(m.source,c,m.from_index);
 
        	pb.selectedCells.clear();
        	pb.selectedCells.push(to);
        	
        	
        	setState(ViticultureState.Confirm);
        	}
        	break;
        	
        	
        case MOVE_RETRIEVE:
        	switch(resetState)
        	{
        	case Retrieve1Current:
        		addtoMovestack(m,pendingMoves);
        		setState((pendingMoves.size()==1) ? ViticultureState.Confirm : resetState);
        		break;
        	case Retrieve2Workers:
        		addtoMovestack(m,pendingMoves);
        		setState((pendingMoves.size()==2) ? ViticultureState.Confirm : resetState);
        		break;
        	default: break;
        	}
        	break;
        case MOVE_UNSELECT:
        	pendingMoves.clear(); 
        	pb.selectedCells.clear();
        	setState(resetState);
        	break;
        case MOVE_UPROOT:
        	{
        	switch(resetState)
        	{
        	case Uproot2For3:
        		addtoMovestack(m,pendingMoves);
        		break;
        	case HarvestOrUproot: 
        	case Uproot:
        	case Uproot1For2:
        		addtoSingleMovestack(m,pendingMoves,true);
        		break;
        	default: G.Error("Not expecting %s",resetState);
        	}
        	int n = pendingMoves.size();
        	int needed = (resetState==ViticultureState.Uproot2For3) ? 2 : 1;
        	setState((n==needed) ? ViticultureState.Confirm : resetState);
        	}
        	break;
        case MOVE_BONUS:
        	ViticultureCell dest = getCell(m.source,m.from_col,m.from_row);
        	ViticultureCell src = pb.bonusActions;
        	if(board_state==ViticultureState.Confirm)
        	{	droppedDestStack.pop();
        		src.addChip(dest.removeTop());
        		if(replay!=replayMode.Replay)
        		{
        			animationStack.push(dest);
        			animationStack.push(src);
        		}
        		setState(resetState = ViticultureState.PlayBonus);
        	}
        	else
        	{	droppedDestStack.push(dest);
        		dest.addChip(src.removeTop());
        		if(replay!=replayMode.Replay)
        		{
        			animationStack.push(src);
        			animationStack.push(dest);
        		}
        		setState(ViticultureState.Confirm);
        	}
        	break;
        case MOVE_SWITCH:
        	{
        	ViticultureCell from = getCell(m.source,m.from_col,m.from_row);
        	ViticultureCell to = getCell(m.dest,m.to_col,m.to_row);
        	ViticultureChip ch1 = from.removeChipAtIndex(m.from_index/100);
        	ViticultureChip ch2 = to.removeChipAtIndex(m.from_index%100);
        	from.addChip(ch2);
        	to.addChip(ch1);
        	setState(ViticultureState.Confirm);
        	}
        	break;
        	
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(ViticultureState.Gameover);
			break;

        default:
        	cantExecute(m);
        }
        
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }
   
    // for switch vines, allow one selection per vine
    private void addtoSingleMovestack(Viticulturemovespec mm,CommonMoveStack stack,boolean removeOthers)
    {
    	boolean removed = false;
    	ViticultureCell dest = getCell(mm.dest,mm.to_col,mm.to_row);
    	for(int i=0;i<stack.size();i++)
    	{
    		Viticulturemovespec target = (Viticulturemovespec)stack.elementAt(i);
    		ViticultureCell tdest = getCell(target.dest,target.to_col,target.to_row);
    		if(tdest==dest) { stack.remove(target, true); removed = target.Same_Move_P(mm); }
    		else if(removeOthers) { stack.remove(target, true); }
     	}
    	if(!removed) {  stack.push(mm); }

    }
    
    private void addtoMovestack(Viticulturemovespec mm,CommonMoveStack stack)
    {
    	boolean removed = false;
    	for(int i=0;i<stack.size();i++)
    	{
    		Viticulturemovespec target = (Viticulturemovespec)stack.elementAt(i);
    		if(target.Same_Move_P(mm)) 
    			{ stack.remove(target, true); removed = true; }
    	}
    	if(!removed) {  stack.push(mm); }

    }
    public boolean movestackContains(Viticulturemovespec mm,CommonMoveStack stack)
    {	if(mm!=null)
    	{for(int lim=stack.size()-1; lim>=0; lim--)
    	{
    	if(mm.Same_Move_P(stack.elementAt(lim))) { return(true); }
    	}}
    	return(false);
    }
    public boolean legalToHit(ViticultureCell c,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	switch(c.rackLocation())
    	{
       	case UnMagnifier:
    	case Magnifier:
    		return(c.topChip()!=null);
    		
    	case SpecialWorkerCards:
    		return(true);
    	case GreenDiscards:
    	case YellowDiscards:
    	case BlueDiscards:
    	case PurpleDiscards:
    	case StructureDiscards:
    		if((board_state==ViticultureState.Puzzle) 
    				&& (pickedObject!=null)
    				&& (pickedObject.type.isCard()))
    		{
    			return(true);
    		}
    		return( (c.topChip()!=null)
    				&& (pickedObject==null) 
    				&& (resetState!=ViticultureState.TakeActionPrevious));
    	default: break;
    	}
    	
    	switch(board_state)
    	{
    	case Puzzle:
    		return((pickedObject!=null) || (c.topChip()!=null));
    	case Retrieve1Current:
    	case Retrieve2Workers:
    		if(pickedObject==null)
    		{
    			return(isDest(c) || (targets.get(c)!=null));
    		}
    		else
    		{
    			return(isSource(c) || (targets.get(c)!=null));
    		}
 		case Confirm:
 			if(resetState==ViticultureState.PlayBonus)
 			{	// if he has played a bonus token, only let him 
 				// pick it back up.
 				return(isDest(c));
 			}
			//$FALL-THROUGH$
		case PlayBonus:
			switch(resetState)
			{
			case Move2Star:
			case Move1Star:
			case Retrieve2Workers:
				return(isDest(c));
			default:
	    		if(resetState.ui==UI.ShowWorkers) { return(false); }
				return(isDest(c) || (targets.get(c)!=null));
			}
 		case TakeActionPrevious:
 			return(targets.get(c)!=null);
 			
    	case Gameover:
    	case Resign:
    		return(false);
    		
    	default:
    		if(board_state.ui==UI.ShowWorkers) { return(false); }
    		if(board_state.ui==UI.ShowCards) { return(false); }
    		return(isSource(c) || targets.get(c)!=null);
    	}
    }


    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Viticulturemovespec m)
    {	
    	robotDepth++;
    	
        // to undo state transistions is to simple put the original state back.
        
        //Assert(m.player == whoseTurn, "whoseturn doesn't agree");
    	//G.print("E "+m);
        if (Execute(m,replayMode.Replay))
        {	/* if this is in effect, saving the variations while debugging doesn't work
        	switch(board_state)
        	{
        	case Confirm:
        	case FullPass:
        		doDone(replayMode.Replay);
        		break;
        	default: break;
        	}
        	*/
        	completeRobotMove();
        }
    }
    private void completeRobotMove()
    {

    }
    


 public Hashtable<ViticultureCell,Viticulturemovespec> getTargets()
 {
	 Hashtable<ViticultureCell,Viticulturemovespec>val = new Hashtable<ViticultureCell,Viticulturemovespec>();
	 switch(resetState)
	 {
	 case Gameover:
	 case Puzzle: break;
	 default:
		 switch(board_state)
		 {
		 case Gameover:
		 case Puzzle: break;
		 default: 
			 CommonMoveStack all = GetListOfMoves(MoveGenerator.All);
			 includeBasedOn(all,val);
		 }}
	 return(val);
 }
 private void addToTargets(Hashtable<ViticultureCell,Viticulturemovespec>val,ViticultureCell c,Viticulturemovespec m)
 {	// chain the moves together
 	m.next = val.get(c);
 	Assert(m.next!=m,"creating a loop");
	val.put(c, m); 	
 }
 private void includeBasedOn(CommonMoveStack all,Hashtable<ViticultureCell,Viticulturemovespec>val)
 {
	 for(int lim=all.size()-1; lim>=0; lim--)
	 {
		 Viticulturemovespec m = (Viticulturemovespec)all.elementAt(lim);
		 switch(m.op) {
		 	case EPHEMERAL_OPTION:
		 	case EPHEMERAL_READY:
		 		addToTargets(val,new ViticultureCell(),m);
		 		break;
		 	case MOVE_MAKEWINE:
		 		switch(m.dest)
		 		{
		 		case WhiteWine:
		 		case RedWine: addToTargets(val,getCell(m.source,m.from_col,m.from_row),m);
		 			break;
		 		case RoseWine:
		 		case Champaign:
		 			// the source isn't actually used, and although there are usually only a few, 
		 			// there can sometimes be a lot
		 			addToTargets(val,new ViticultureCell(),m);
		 			break;
		 		default: G.Error("Not expecting %s", m.dest);
		 		}
		 		
		 		break;
		 	case MOVE_SWITCH:	// note there is special double coding of from_index 
		 		addToTargets(val,getCell(ViticultureId.Field,m.from_col,m.from_row),m);
		 		addToTargets(val,getCell(ViticultureId.Field,m.to_col,m.to_row),m);
		 		break;
		 	case MOVE_UPROOT:
		 		{
		 		Viticulturemovespec m2 = (Viticulturemovespec)m.Copy(null);
		 		addToTargets(val,getCell(m.source, m.from_col,m.from_row),m);	// to_row is used by cards to indicate a second card index
		 		addToTargets(val,getCell(m.dest,m.to_col,m.to_row),m2);
		 		}
		 		break;
		 	case MOVE_TRADE:
		 		ViticultureCell dest = getCell(m.dest,m.to_col,0);
		 		ViticultureCell src = getCell(m.source, m.from_col,0);	// to_row is used by cards to indicate a second card index
		 		addToTargets(val,src,m);
		 		if(src!=dest) { addToTargets(val,dest,m); }
		 		break;
		 	case MOVE_PLANT:
		 		{
		 		Viticulturemovespec m2 = (Viticulturemovespec)m.Copy(null);
		 		addToTargets(val,getCell(m.source, m.from_col,m.from_row),m);	// to_row is used by cards to indicate a second card index
		 		addToTargets(val,getCell(m.dest,m.to_col,m.to_row),m2);
		 		}
		 		break;
		 	case MOVE_TAKEACTION:
		 		switch(m.source)
		 		{
		 		case Cards: 
		 			addToTargets(val,getCell(m.source,m.from_col,m.from_row),m);
		 			break;
		 		case Workers:
		 		case Cash:
		 			addToTargets(val,getCell(m.dest,m.to_col,m.to_row),m);
		 			break;
		 		default: G.Error("Not expecting %s", m.source);
		 		}
		 		
		 		break;
		 	case MOVE_SELECTWAKEUP:
		 	case MOVE_NEWWAKEUP:
		 	case MOVE_SELECT:
		 	case MOVE_DISCARD:
		 	case MOVE_SELLWINE:
		 	case MOVE_FILLWINE:
		 	case MOVE_BUILD:
		 	case MOVE_BUILDCARD:
		 	case MOVE_AGEONE:
		 	case MOVE_TRAIN:
		 		addToTargets(val,getCell(m.source, m.from_col,m.from_row),m);
		 		break;
		 	case MOVE_PLACE_WORKER:
		 		if(board_state==ViticultureState.Confirm) { break; } 
		 	//$FALL-THROUGH$
		 	case MOVE_BONUS:
		 	case MOVE_PLACE_STAR:
		 	case MOVE_STAR:
		 	case MOVE_RETRIEVE:
		 		if(pickedObject==null)
		 		{
		 			addToTargets(val,getCell(m.source,m.from_col,m.from_row), m);
		 		}
		 		else {
		 			addToTargets(val,getCell(m.dest,m.to_col,m.to_row),m);
		 		}
		 		break;
		 	case MOVE_DONE:
		 	default: break;
	}
 }}
 private void placeWorkerMoves(CommonMoveStack all,boolean unrestricted,int season,boolean includePlayerBoard,int action,MoveGenerator generator)
 {
	 PlayerBoard pb = getCurrentPlayerBoard();
	 boolean placedNormal = false;
	 if((pickedObject!=null) && (pickedObject.id==ViticultureId.Workers))
	 {
		 placeWorkerSeasonMoves(pb,all,pickedObject,-1,unrestricted,season,includePlayerBoard,action,generator);
		 
		 if(includePlayerBoard)
		 {
		 if((pb.yoke.topChip()!=null) 
				 && (pb.yokeWorker.topChip()==null) 
				 && (season==1 || season==3 || (revision>=116))
				 && (canHarvest(pb,generator)||(canUproot(pb,generator))) 
				 )
		 	{
			all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,-1,pb.yokeWorker,whoseTurn));
		 	}
		
		 // play a worker on a structure card or unbuild a structure
		 boolean some = false;
		 for(ViticultureCell structure : revision>=114 ? pb.buildStructureCells : pb.structures)
		 {	 ViticultureChip top = structure.topChip();
			 if(top!=null)
			 if(top!=null && (top.type==ChipType.StructureCard))
			 {	some = true;
			 	if(canPlayOnStructureCard(pb,top))
				 {
					 all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,-1,structure,whoseTurn));
				 }
			 }
		 }
		 if(some)
		 {	// if there is a structure, you can destroy it.
			all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,-1,pb.destroyStructureWorker,whoseTurn));
		 }}
	 }
	 else
	 {
	 for(int lim=pb.workers.height()-1; lim>=0; lim--)
	 {	ViticultureChip worker = pb.workers.chipAtIndex(lim);
		if(!placedNormal || (worker.type!=ChipType.Worker))
		{
			placeWorkerSeasonMoves(pb,all,worker,lim,unrestricted,season,includePlayerBoard,action,generator);
		}
		if(includePlayerBoard)
		{
		if((pb.yoke.topChip()!=null) 
				 && (pb.yokeWorker.topChip()==null) 
				 && canHarvest(pb,generator) 
				 && (season==1 || season==3))
		 	{
			all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,lim,pb.yokeWorker,whoseTurn));
		 	}
		
		// play a worker on a structure card or unbuild a structure
		 boolean some = false;
		 for(ViticultureCell structure : pb.structures)
		 {
			 ViticultureChip top = structure.topChip();
			 if(top!=null && (top.type==ChipType.StructureCard))
			 {
				 some = true;
				 if(canPlayOnStructureCard(pb,top))
				 {
					 all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,lim,structure,whoseTurn));
				 }
			 }
		 }
		 if(some)
		 {
			 all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,lim,pb.destroyStructureWorker,whoseTurn));
		 }}

		placedNormal |= worker.type==ChipType.Worker;
	 }
	 
	 }
 }
 private boolean addFlipMoves(PlayerBoard pb,CommonMoveStack all,int surcharge)	// surcharge from soldato
 {
	 boolean some = false;
	 int availableCash = pb.cash-surcharge;
	 if(availableCash<0) { return(false); }	// soldato locks us out
	 
	 for(int lim = pb.fields.length-1; lim>=0; lim--)
	 {
		 ViticultureCell field = pb.fields[lim];
		 ViticultureChip top = field.topChip();
		 if((top!=null)
				&& (top.type == ChipType.Field)
				&& (pb.vines[lim].topChip()==null))
		 {
			 if(all==null) { return(true); }	// can sell this field
			 all.push(new Viticulturemovespec(MOVE_SELECT,field,whoseTurn));
			 some = true;
		 }
		 if(top!=null 
				 && (top.type==ChipType.SoldField)
				 && (availableCash>=5+lim))	// cost 5,6, or 7
		 {
			 if(all==null) { return(true); }	// can buy back this field
			 all.push(new Viticulturemovespec(MOVE_SELECT,field,whoseTurn));	
			 some = true;
		 }
	 }
	 return(some);
 }
 private int soldatoCost(PlayerBoard pb,ViticultureCell where)
 {
	 return ((revision>=132) && (board_state==ViticultureState.TakeActionPrevious)) 
			 	? 0
			 	: pb.nOpponentSoldato(where); 
 }
 /* return true if a worker can be placed.  Season has already been considered 
  * doesn't check that the cell is empty
  */
 private boolean canPlaceWorker(PlayerBoard pb,ViticultureChip worker,ViticultureCell where,MoveGenerator generator)
 {
 	// is there a soldato in play?
 	int soldatoCost = soldatoCost(pb,where);
 	return(canPlaceWorker(pb,worker,soldatoCost,where,generator));
 }
 /* return true if an innkeeper can be placed and use his power, which costs
  * another $1.  This doesn't consider the second half, which is that there
  * is a card available to steal
  * 
  */
 private boolean canPlaceInnkeeper(PlayerBoard pb,ViticultureChip worker,ViticultureCell where,MoveGenerator generator,boolean paidforsoldato)
 {
 	// is there a soldato in play?
 	int soldatoCost = paidforsoldato ? 0 : soldatoCost(pb,where);
 	return(canPlaceWorker(pb,worker,soldatoCost+1,where,generator));
 }

 private boolean canPlaceWorker(PlayerBoard pb,ViticultureChip worker,int soldatoCost,ViticultureCell where,MoveGenerator generator)
 { 	boolean v = false;
 	if(pb.cash<soldatoCost) { return(false); } 	// can't pay
	switch(where.rackLocation())
	{
	case PlayerStructureCard:
		// prior to 146, we didn't consider this and so returned false, as it
		// happened this conspired to do the right thing for the cases where
		// a mafioso was placed on a structure card.  This ought to be superfluous,
		// because we now handle the structure card case correctly, and don't use
		// this continuation at all.
		if(revision>=146) { return(true);	}	// can always place on a structure card
		break;
	case RecruitWorker:
		{
		int discount = ((worker.type==ChipType.Farmer)||(where.row==TrainWorkerDiscountRow)) ? 1 : 0;
		v = pb.nWorkers<maxWorkers() && ((pb.cash+discount)>=(costOfWorker(pb)+soldatoCost));
		}
		break;
	case FillWineWorker:
		v = canFillWineOrder(pb);
		break;
	case DrawGreenWorker:	
		v = greenCards.height()>0;
		break;
	case DrawPurpleWorker:
		v = purpleCards.height()>0;
		break;
	case GiveTourWorker:	// can always do these
	case StarPlacementWorker:
	case TradeWorker:
	case DollarOrCardWorker:
	case BuildTourWorker:
		v = true;
		break;
	case MakeWineWorker:
		v = pb.hasGrape();
		break;
	case FlipWorker:
		v = addFlipMoves(pb,null,soldatoCost);
		break;
	case PlayYellowWorker:
		v = pb.hasCard(ChipType.YellowCard);
		break;
	case PlayBlueWorker:
		v = pb.hasCard(ChipType.BlueCard);
		break;
	case PlantWorker:
		v = canPlant(pb);
		break;
	case SellWineWorker:
		v = pb.hasWine();
		break;
	case PlayerYokeWorker:
		v = canHarvest(pb,generator) || (revision>=114 ? canUproot(pb,generator) : false);
		break;
	case HarvestWorker:
		v = canHarvest(pb,generator);
		break;
				
	case BuildStructureWorker:
		{
		int discount = ((worker.type==ChipType.Farmer)||(where.row==BuildStructureBonusRow))?1:0;
		if(automa && (pb.bonusActions.topChip()!=null)) { discount=1; }	// in automa, you get to use the bonus
		// keep this this way, so it doesn't change with the current revision
		v = addBuildingMoves(pb,null,discount-soldatoCost,10,generator);
		}
		break;
		
/*		
    	
    	PlayYellowWorker("YellowWorker"),
    	PlantWorker("PlantWorker"),
    	TradeWorker("TradeWorker"),
    	FlipWorker("FlipWorker"),
    	
    	("PurpleWorker"),
    	HarvestWorker("HarvestWorker"),
    	MakeWineWorker("MakeWineWorker"),
    	
    	PlayBlueWorker("BlueWorker"),
    	RecruitWorker("RecruitWorker"),
    	SellWineWorker("SellWineWorker"),
    	FillWineWorker("FillWineWorker"),
*/
		
	default: break;
	}
	return(v);
 }
 private void placeWorkerSeasonMoves(PlayerBoard pb,CommonMoveStack all,ViticultureChip worker,int workerIndex,boolean unrestricted,int forseason,boolean includeYoke,int action,MoveGenerator generator)
 {	 // 4 sets of 4 places for each season
	 if(includeYoke)
	 	{ // the public default spot
		 if(pb.cash>=soldatoCost(pb,dollarWorker))
		 {	// if he can pay the soldato, if any
		 all.push(new Viticulturemovespec(MOVE_PLACE_WORKER,ViticultureId.Workers,workerIndex,dollarWorker,whoseTurn));
		 }
	 	}
	 
	 int lastSlot=(players_in_game-1)/2;
	 
	 for(int row = forseason*4,lastRow=row+4; row<lastRow;row++)
	 {
		 placeWorkerInAction(pb,action,lastSlot,worker,workerIndex,all,row,generator,unrestricted);
	 }
	 if(forseason==season(pb))	// if we're placing in the current season, not a special like planner
	 {
	 switch(worker.type)
	 {
	 default: throw G.Error("type %s not handled",worker.type);
	 case Soldato: // no special rules
	 case Politico:
	 case Professore:
	 case Merchant:
	 case Mafioso:
	 case Innkeeper:
	 case Farmer:
	 case Oracle:
	 case Worker:
	 case Chef:
	 case GrandeWorker:
		 break;
	 case Traveler:
	 	{	// travelers can go to any empty slot in a previous season, provided they 
	 		// can normally go there.

		 for(int row = 0,lastRow = forseason*4; row<lastRow;row++)
		 {	 // traveler can use all slots
			 placeWorkerInAction(pb,action,2,worker,workerIndex,all,row,generator,unrestricted);
			// TODO: this assumes that the traveler/soldato combo doesn't allow the traveler to be placed in the overflow 
			// 
		 }
	 	}
	 	break;
	 case Messenger:
		 {	// messenger can go to any empty slot in a subsequent season	
			 for(int row = (season(pb)+1)*4,lastRow = 4*4; row<lastRow;row++)
			 {		// messenger placement is unrestructed, if you can't do the action
				 	// then the time comes, you just lose it.
					placeWorkerInAction(pb,action,lastSlot,worker,workerIndex,all,row,generator,true);
			 }

		 } 
	 }}
 }
	 
public void placeWorkerInAction(PlayerBoard pb,int action,int lastSlot,
		ViticultureChip worker,int workerIndex,CommonMoveStack all,
		int row,MoveGenerator generator,boolean unrestricted)
{	boolean placed = false;
	ViticultureCell place[] = mainBoardWorkerPlacements[row];		
	if(action!=MOVE_TAKEACTION)
	{
	for(int slot = 0;slot<=lastSlot;slot++)
	{   
		ViticultureCell dest = place[slot];
		ViticultureChip top = dest.topChip();
		if( ((top==null) || ((worker.type==ChipType.Chef) && (top.type!=ChipType.Chef) && (playerWithColor(top.color)!=pb))) 
				&& (unrestricted || canPlaceWorker(pb,worker,dest,generator))
				&& (!placed || (generator!=MoveGenerator.Randomizer))
				)
		{
			all.push(new Viticulturemovespec(action,ViticultureId.Workers,workerIndex,dest,whoseTurn));
			placed = true;
		}
	}}
	 
	ViticultureCell overflow = place[3];
	int nSoldato = soldatoCost(pb,overflow);

	 // if nowhere to place the worker, and this is the grande, or there is a soldato, place in the overflow slot
	 if(!placed 
			 && (pb.cash>=nSoldato)
			 && ((nSoldato>0) || (worker.type==ChipType.GrandeWorker))
			 && (unrestricted || ( canPlaceWorker(pb,worker,place[3],generator)
					 			// this implements the restriction that soldato doesn't open up
					 			// the extra slot for active travelers
					 			&&  ((worker.type!=ChipType.Traveler)||(workerCellSeason(overflow)==season(pb)))
					 			)
					 ))
	 	{
		 all.push(new Viticulturemovespec(action,ViticultureId.Workers,workerIndex,overflow,whoseTurn));
	 	}
 }

 public boolean shouldBuild(ViticultureCell building,MoveGenerator generator)
 {
	 switch(generator)
		{
		case Runner:
			switch(building.rackLocation())
			{
			case Trellis:
			case WaterTower:
			case Yoke:
			case LargeCellar:
			case MediumCellar:
				return(false);
			default: 
				return(true);
			}
		case Randomizer:
		case Harvester:
			switch(building.rackLocation())
			{
			case TastingRoom:
			case Windmill:	return(false);
			default: return(true);
			}
		case Robot:
		default: 
			return(true);
		}
 }
 public boolean canBuildStructureWithDiscount(PlayerBoard pb,int n)
 {	if(revision>=109)
 	{
	 return(addBuildingMoves(pb,null,n,100,MoveGenerator.Runner));
 	}
 	else 
 	{
 	 return(pb.canBuildStructureWithDiscount(n));
 	}
 }
 
 public boolean addBuildingMoves(PlayerBoard pb,CommonMoveStack all,int regulardiscount,int maxprice,MoveGenerator generator)
 { 	int discount = regulardiscount + (pb.hasWorkshop() ? 1 : 0);	// extra discount
 	boolean v = addBuildingMovesInternal(pb,all,discount,maxprice,generator);
 	if(!v)
	{
		switch(generator)
		{
		case Harvester:
		case Randomizer:
		case Runner:
			// if the filter chops us down to nothing, allow building moves
			// anyway, so the overall control flow doesn't change. This ought
			// to occur only at the end of long random playouts
			v = addBuildingMovesInternal(pb,all,discount,10,MoveGenerator.Robot);
			break;
		case All:
		case Robot:
		default:
		}
	}
 	return(v);
 }
 
 public boolean addBuildingMovesInternal(PlayerBoard pb,CommonMoveStack all,int discount,int maxprice,MoveGenerator generator)
 {	boolean some=false;
 	for(int lim = pb.buildable.length-1; lim>=0; lim--)
 	{	ViticultureCell c = pb.buildable[lim];
 		if(c.topChip()==null
 				&& (pb.cash >= c.cost-discount)
 				&& (c.cost<=maxprice)
 				&& ((c!=pb.largeCellar) || pb.hasMediumCellar() )	// can't build large before medium
 				&& shouldBuild(c,generator)
 				)
			{	
 				some = true;
				if(all==null) { break; }
				ViticultureCell u = getCell(c.rackLocation().getUnbuilt(),c.col,c.row);
				all.push(new Viticulturemovespec(MOVE_BUILD,u,0,c,whoseTurn));
			}
		}
 	
 	some |= addBuildOrangeMoves(all,pb,discount,maxprice);
 	
 	return(some);
 }
 
 public boolean addBuildOrangeMoves(CommonMoveStack all,PlayerBoard pb,int discount,int maxprice)
 {	boolean some = false;
 	ViticultureCell structure = pb.buildableOrangeSlot();
 	if(structure!=null)
 	{
 	for(int lim=pb.cards.height()-1; lim>=0; lim--)
	 {
		 ViticultureChip chip = pb.cards.chipAtIndex(lim);
		 if((chip.type==ChipType.StructureCard) 
				 && (chip.costToBuild()-discount<=pb.cash)
				 && (chip.costToBuild()<=maxprice)
				 )
			{	some = true;
				if(all==null) { break; }
				all.push(new Viticulturemovespec(MOVE_BUILDCARD,pb.cards,lim,structure,whoseTurn));
			}
		}}
 	return(some);
 }
 int valueOfVines(ViticultureCell field)
 {	int sum = 0;
 	for(int lim=field.height()-1; lim>=0; lim--)
	 {
		 ViticultureChip ch = field.chipAtIndex(lim);
		 if(ch.type==ChipType.GreenCard)
		 {
			 sum += ch.totalVineValue();
		 }
	 }
	 return(sum);
 }
 private boolean addUprootMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {	boolean some = false;
 	ViticultureCell vines[] = pb.vines;
 	ViticultureCell fields[] = pb.fields;
	for(int idx = vines.length-1; idx>=0; idx--)
	{	ViticultureCell vine = vines[idx];
		ViticultureCell field = fields[idx];
		for(int lim = vine.height()-1; lim>=0; lim--)
		{	Viticulturemovespec m = new Viticulturemovespec(MOVE_UPROOT,vine,lim,field,whoseTurn);
			if((generator!=MoveGenerator.All) && movestackContains(m,pendingMoves))
			{
				//G.print("Already there");
			}
			else
			{
			if(all==null) { return(true); }
			all.push(m);
			some = true;
			}
		}
	}
	return(some);
 }
 // add planing moves considering field limits on total value and structure requirements
 // return true if some 
 // all can be null to just test
 public boolean addPlantingMoves(PlayerBoard pb,CommonMoveStack all,
		 							boolean noStructuresThisTime,
		 							boolean noFieldLimits,
		 							ViticultureCell addedStructure)
 {	boolean some=false;
 	ViticultureCell from = pb.cards;
 	boolean noStructures = noStructuresThisTime || pb.hasAqueduct();
 	for(int lim=from.height()-1; lim>=0; lim--)
 	{
 		ViticultureChip vine = from.chipAtIndex(lim);
 		if((vine.type==ChipType.GreenCard)
 				&& (noStructures || pb.hasStructuresForPlanting(vine,addedStructure)))
 		{	
 			for(int fieldIndex = pb.fields.length-1; fieldIndex>=0; fieldIndex--)
 			{	ViticultureCell field = pb.fields[fieldIndex];
 				boolean canPlant = (noFieldLimits && canPlantAtAll(field)) || canPlantValue(vine,field);
 				if(canPlant)
 				{
 					if(all==null) { return(true); }
 					some = true;
 					all.push(new Viticulturemovespec(MOVE_PLANT,pb,lim,fieldIndex,whoseTurn));
 				}
 			}
 		}
 	}
  	return(some);
 	}
 
 public boolean canPlantValue(ViticultureChip vine,ViticultureCell dest)
 {
	 if(canPlantAtAll(dest))
		{
		 ViticultureCell vines = getCell(ViticultureId.Vine,dest.col,dest.row);
		 int vineValue = vine.totalVineValue();
		 boolean val = (valueOfVines(vines)+vineValue)<=(5+dest.row);
		 return(val);	// limits of 5,6,7 for fields 0,1,2
		}
	 return(false);
 }
 // if field can be planted - not sold or covered by a building
 public boolean canPlantAtAll(ViticultureCell field)
 {	
	 ViticultureChip chip = field.chipAtIndex(0);
	 if(chip!=null && (chip.type==ChipType.Field))
	 {
		 for(int i=1;i<field.height();i++)
		 {
			 chip = field.chipAtIndex(i);
			 if(chip!=null && chip.type==ChipType.StructureCard) { return(false); }
		 }
		 return(true);
	 }
	 return(false);
 }
 // can plan with the normal requirements
 public boolean canPlant(PlayerBoard pb)
 {
	 return(addPlantingMoves(pb,null,false,false,null));
 }
 // can plan with the normal requirements
 public boolean canPlantWithAddedStructure(PlayerBoard pb,ViticultureCell structure)
 {
	 return(addPlantingMoves(pb,null,false,false,structure));
 }
 
 // can plant ignoring the structure restrictions
 public boolean canPlantWithoutStructures(PlayerBoard pb)
 {
	 return(addPlantingMoves(pb,null,true,false,null));
 }
 public boolean canPlantWithoutLimits(PlayerBoard pb)
 {
	 return(addPlantingMoves(pb,null,false,true,null));
 }
 public boolean addHarvestMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {	boolean some=false;
 	ViticultureCell vines[] = pb.vines;
 	ViticultureCell fields[] = pb.fields;
 	for(int lim = vines.length-1; lim>=0; lim--)
 	{
 		ViticultureCell vine = vines[lim];
 		ViticultureCell field = fields[lim];
 		if((field.topChip().type == ChipType.Field) 
 				&& (vine.height()>0)
 				&& ((generator==MoveGenerator.All) || !pb.selectedCells.contains(field)))
 		{
 			if(all==null) { return(true); }
 			all.push(new Viticulturemovespec(MOVE_SELECT,field,whoseTurn));
 			some = true;
 		}
 	}
  	return(some);
 	}
 public boolean canUproot(PlayerBoard pb,MoveGenerator m)
 {
	 return(addUprootMoves(pb,null,m));
 }
 public boolean canHarvest(PlayerBoard pb,MoveGenerator m)
 {
	 return(addHarvestMoves(pb,null,m));
 }
 private boolean addCardMoves(CommonMoveStack all,ViticultureCell from,ChipType type)
 {
	 boolean some = false;
	 for(int lim=from.height()-1; lim>=0; lim--)
	 {
		 ViticultureChip ch  = from.chipAtIndex(lim);
		 if(ch.type==type)
		 {
			 if(all==null) { return(true); }
			 all.push(new Viticulturemovespec(MOVE_SELECT,from,lim,from,whoseTurn));
		 }
	 }
	 return(some);
 }
 

 private boolean addDiscardMove
 	(PlayerBoard pb,CommonMoveStack all,ViticultureCell from,ViticultureCell to,int fromtop,boolean filter)
 {
	 if(from.height()>fromtop) 
	 	{ 
	 	  int h = from.height()-fromtop-1;
	 	  ViticultureChip chip = from.chipAtIndex(h);
	 	  if(!filter ||  !pb.selectedCards.contains(from.rackLocation(),chip,h))
	 		  { if(all==null) { return(true); }
	 		    all.push(new Viticulturemovespec(MOVE_SELECT,from,h,to,whoseTurn));
	 		    return(true);
	 		  }
	 	}
	 return(false);
 }
 private boolean addPickTop2Moves(PlayerBoard pb,CommonMoveStack all,ViticultureCell from,MoveGenerator generator)
 {	
	boolean some = false;
	some |= addDiscardMove(pb,all,greenCards,from,0,(generator!=MoveGenerator.All) );
	some |= addDiscardMove(pb,all,yellowCards,from,0,(generator!=MoveGenerator.All));
	some |= addDiscardMove(pb,all,purpleCards,from,0,(generator!=MoveGenerator.All));
	some |= addDiscardMove(pb,all,blueCards,from,0,(generator!=MoveGenerator.All));		
	some |= addDiscardMove(pb,all,structureCards,from,0,(generator!=MoveGenerator.All));
	return(some);
 }
 boolean all_workers = false;
 private boolean addTrainWorkerMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {
	 if(all!=null)
	 {	
		if(pb.cash>=costOfWorker(pb,ViticultureChip.getChip(ChipType.Worker,pb.color),resetState))
		{
		all.push(new Viticulturemovespec(MOVE_TRAIN,ViticultureId.Workers,pb.colCode,ChipType.Worker,whoseTurn));
		}
		for(int i=0,limit=(all_workers||robotBoard ? workerCards.height() : 2);i<limit; i++)
		{
		ChipType special1 = ChipType.find(workerCards.chipAtIndex(i).cardName);
		addTrainWorkerType(pb,all,special1);
		}
	 }
	 return(true);
 }
 private void addTrainWorkerType(PlayerBoard pb,CommonMoveStack all,ChipType special1)
 {
	 ViticultureChip type1 = ViticultureChip.getChip(special1,pb.color);
	 if(!pb.workerTypes.containsChip(type1) && (pb.cash>=costOfWorker(pb,type1,resetState)))
	 {
		all.push(new Viticulturemovespec(MOVE_TRAIN,ViticultureId.Workers,pb.colCode,
					special1,whoseTurn));
	 }
 }
 
 private boolean addUnbuildStructureMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {	
	boolean some = false;
	for(ViticultureCell structure : revision>=114 ? pb.buildStructureCells : pb.structures)
	{	int index = structure.height()-1;
		if(index>=0 && !structure.topChip().type.isWorker())	// not populated by a worker
		{	ViticultureChip struct = structure.chipAtIndex(index);
			if((struct.type==ChipType.Field)&&(index>=1)) { index++; struct = structure.chipAtIndex(index); }
			if(struct.type==ChipType.StructureCard)		// card present, maybe with a worker
				{ some |= addDiscardMove(pb,all,structure,structureDiscards,0,(generator!=MoveGenerator.All) );
				}
		}}
	return(some);
 }
 
 private boolean hasDiscardMoves(PlayerBoard pb)
 {
	 return(addDiscardMoves(pb,null,true));	// filter out already played moves
 }
 private boolean addDiscardMoves(PlayerBoard pb,CommonMoveStack all,boolean filter)
 {	ViticultureCell from = pb.cards;
	 boolean some = false;
	 some |= addDiscardMove(pb,all,greenDiscards,from,0,filter);
	 some |= addDiscardMove(pb,all,yellowDiscards,from,0,filter);
	 some |= addDiscardMove(pb,all,purpleDiscards,from,0,filter);
	 some |= addDiscardMove(pb,all,blueDiscards,from,1,filter);		// the "retrieve" card is a blue card
	 some |= addDiscardMove(pb,all,structureDiscards,from,0,filter);
	 return(some);
 }
 
 // true if the player has at least 1 planted vine
 private boolean hasPlantedVine(PlayerBoard pb)
 {
	 return(pb.nPlantedFields()>0);
 }
 private boolean hasGrape(PlayerBoard pb)
 {
	 return(pb.hasGrape());
 }
 private int nPlantedFields(PlayerBoard pb)
 {
	 return(pb.nPlantedFields());
 }
 private void addChoice(CommonMoveStack all,ViticultureId choice,MoveGenerator generator)
 {	if((generator==MoveGenerator.All)
		 || choice!=cardResolution)
 	{
	 all.push(new Viticulturemovespec(MOVE_SELECT,choice,whoseTurn));
 	}
 }
 private int maxWorkers()
 {
	 return testOption(Option.UnlimitedWorkers) ? 99 : MAX_WORKERS;
 }
 private void addCardResolutionMoves(CommonMoveStack all,ViticultureChip card,MoveGenerator generator)
 {	PlayerBoard pb = getCurrentPlayerBoard();
 	switch(card.type)
		{
		case BlueCard:
			addChoice(all,ViticultureId.Choice_0,generator);
			switch(card.order)
			{
			case 1: // merchant
				if(pb.cash>=3) { addChoice(all,ViticultureId.Choice_A,generator); }
				// avoid information leakage, offer the choice even if not possible 
				if(revision<152 || pb.canPossiblyFillWineOrder()) { addChoice(all,ViticultureId.Choice_B,generator); } 
				break;
			case 3: // judge
				addChoice(all,ViticultureId.Choice_A,generator);
				//p1("use judge");
				if(pb.hasWineWithValue(4)) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 4: // oenologist
				addChoice(all,ViticultureId.Choice_A,generator);
				if(pb.cash>=3) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 5: //marketer
				addChoice(all,ViticultureId.Choice_A,generator);
				// avoid information leakage, offer the choice even if not possible 
				if(revision<152 || pb.canPossiblyFillWineOrder()) { addChoice(all,ViticultureId.Choice_B,generator); } 
				break;
			case 6: //crush expert
				addChoice(all,ViticultureId.Choice_A,generator);
				if(pb.hasGrape()) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 7: // uncertified teacher
				if((pb.nWorkers<maxWorkers())&&(pb.score>MIN_SCORE)) { addChoice(all,ViticultureId.Choice_A,generator); }
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 8: // teacher
				if(pb.hasGrape()) { addChoice(all,ViticultureId.Choice_A,generator); }
				if((pb.nWorkers<maxWorkers()) && pb.cash>=2) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 9: // benefactor
				addChoice(all,ViticultureId.Choice_A,generator);
				if(pb.nVisitorCards()>=2) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 10: // assessor
				addChoice(all,ViticultureId.Choice_A,generator);
				// 1 card remaining, current card is already removed
				if(pb.cards.height()>=1) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 13: // professor
				
				if(pb.cash>=costOfWorker(pb)-2 && (pb.nWorkers<maxWorkers())) { addChoice(all,ViticultureId.Choice_A,generator); }
				// revision 157, make this not an "else" since it cab be a "both"
				if(pb.nWorkers==6) // yes, exactly 6 is what it says on the card
					{ // professor, you can gain 2 vp if you have exactly 6
					  addChoice(all,ViticultureId.Choice_B,generator); 
					}
				break;
			case 14: // master vintner
				{
				int cost = upgradeCellar(pb,false,replayMode.Replay);	// does not consider workshop
				if((pb.cash>=cost-2 - (pb.hasWorkshop()?1:0)) && !pb.hasBothCellars()) 
					{ addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.hasWine()) { addChoice(all,ViticultureId.Choice_B,generator); }
				}
				break;
			case 15: // uncertified oenologist
				if(pb.hasWine()) {  addChoice(all,ViticultureId.Choice_A,generator); }
				if((pb.score>MIN_SCORE)&&!pb.hasBothCellars()) {  addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 16: // promoter
				if(pb.hasWine()||(pb.hasGrape())) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 18: // harvest expert
				addChoice(all,ViticultureId.Choice_A,generator);
				if(pb.cash>=1) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 20:	// jack of all trades
				{
				boolean canHarvest = canHarvest(pb,generator);
				boolean canMakeWine = canHarvest || canMakeWine(pb);
				boolean canFill = (revision>=154 ? canMakeWine : false) || pb.canPossiblyFillWineOrder();
				if(canHarvest) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(canHarvest || canMakeWine) { addChoice(all,ViticultureId.Choice_B,generator); }
				// avoid information leakage
				if(revision<152 || canFill) { addChoice(all,ViticultureId.Choice_C,generator); }
				}
				break;
			case 26:
			case 22: //supervisor
				if(pb.hasGrape()) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 23: // scholar
				addChoice(all,ViticultureId.Choice_A,generator);  
				if((pb.cash>=3) && (pb.nWorkers<maxWorkers()))
				{ addChoice(all,ViticultureId.Choice_B,generator); 
				  if(pb.score>MIN_SCORE) { addChoice(all,ViticultureId.Choice_AandB,generator); }
				}

				break;
			case 27: // craftsman
				addChoice(all,ViticultureId.Choice_A,generator); 
				addChoice(all,ViticultureId.Choice_C,generator); 
				addChoice(all,ViticultureId.Choice_AandC,generator);
				if(!pb.hasBothCellars())
				{	// check the cash reserves
					ViticultureCell c = pb.hasMediumCellar() ? pb.largeCellar : pb.mediumCellar;
		   			int discount = revision>=153 && pb.hasWorkshop() ? 1 : 0;
					if(pb.cash>=c.cost-discount)
					{
					addChoice(all,ViticultureId.Choice_B,generator);
					addChoice(all,ViticultureId.Choice_AandB,generator);
					addChoice(all,ViticultureId.Choice_BandC,generator);
					}
				}
				break;
			case 28: //exporter
				// avoid information leakage
				addChoice(all,ViticultureId.Choice_A,generator); 
				addChoice(all,ViticultureId.Choice_B,generator);	// fill wine order is always presented
				if(pb.hasGrape()) { addChoice(all,ViticultureId.Choice_C,generator); }
				break;
			case 29: // laborer
				boolean harv = canHarvest(getCurrentPlayerBoard(),generator);
				if(harv) { addChoice(all,ViticultureId.Choice_A,generator); } 
				if((revision>=137 && harv) || canMakeWine(pb)) 
					{ addChoice(all,ViticultureId.Choice_B,generator);
					  if(pb.score>MIN_SCORE && harv) { addChoice(all,ViticultureId.Choice_AandB,generator); }
					}
				break;
			case 30: // designer
				if(canBuildStructureWithDiscount(pb,0)) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 31: // governess
				if((pb.nWorkers<maxWorkers()) && (pb.cash>=3)) {  addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.hasWine()) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 34: // noble
				if(pb.cash>=1) { addChoice(all,ViticultureId.Choice_A,generator);  }
				if(pb.residual>=2) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 36: // taster
				if(pb.hasWine()) { addChoice(all,ViticultureId.Choice_A,generator);  }
				break;
				
			case 12: // harvester				
			case 2: // crusher
				addChoice(all,ViticultureId.Choice_B,generator);
				//$FALL-THROUGH$
			case 38: // guest speaker
			case 37: // caravan
			case 35: // governor
			case 33: // zymologist - might make no wines!
			case 32: // manager
			case 25: // motivator
			case 24: // reaper
			case 21: // politician
			case 19: // innkeeper
			case 17: // mentor
				addChoice(all,ViticultureId.Choice_A,generator);
				break;
			case 11:	// queen, too mean
				break;
			default:
				G.Error("Can't handle %s", card);
			}			
			break;
		case ChoiceCard:
			switch(card.order)
			{
			case 11: // politico vp
			case 12: // politico green
			case 13: // politico structure
			case 14: // politico yellow
			case 15: // politico purple
			case 16: // politico blue
			case 17: // politico wine
			case 18: // politico star
			case 19: // politico plant
			case 20: // politico harvest
			case 21: // politico trade
			case 27: // politico blue revised 122
			case 28: // politico yellow revised 122
			case 29: // politico extra star revised 123
			case 30: // politico extra trade revised 131
			case 31: // politico plant extra
				if(pb.cash>0) { addChoice(all,ViticultureId.Choice_A,generator); }
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
				
			case 24:	// farmer, harvesting
			case 22:	// farmer 
			case 23:	// farmer
			case 25:	// farmer
			case 26:	// farmer
			case 1:	// default action choice at the yoke
				addChoice(all,ViticultureId.Choice_A,generator);
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 2: // swindler
				if(pb.cash>=2) { addChoice(all,ViticultureId.Choice_A,generator); };
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 3:	// banker
				if(pb.score>MIN_SCORE) { addChoice(all,ViticultureId.Choice_A,generator); }
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 4: // buyer buys
				addChoice(all,ViticultureId.Choice_0,generator);	// allow him to punt
				if(pb.cash>=2)
					{ addChoice(all,ViticultureId.Choice_A,generator); 
					  addChoice(all,ViticultureId.Choice_B,generator); 
					};
				break;
			case 6: // negotiator sell grapes
			case 5: // buyer sells
				addChoice(all,ViticultureId.Choice_0,generator);	// allow him to punt
				if(pb.hasRedGrape()) { addChoice(all,ViticultureId.Choice_A,generator); };
				if(pb.hasWhiteGrape()) { addChoice(all,ViticultureId.Choice_B,generator); };
				break;
				
			case 7: // negotiator sell wine
				addChoice(all,ViticultureId.Choice_0,generator);	// allow him to punt
				if(pb.hasRedWine()) { addChoice(all,ViticultureId.Choice_A,generator); };
				if(pb.hasWhiteWine()) { addChoice(all,ViticultureId.Choice_B,generator); };
				if(pb.hasRoseWine()) { addChoice(all,ViticultureId.Choice_C,generator); };
				if(pb.hasChampaign()) { addChoice(all,ViticultureId.Choice_D,generator); };
				break;
			case 8: // train worker or not
				if(pb.cash>=1 && pb.nWorkers<maxWorkers()) { addChoice(all,ViticultureId.Choice_A,generator); }
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 9:	// take yellow or pay, no choice
				if(pb.hasCard(ChipType.YellowCard)) { addChoice(all,ViticultureId.Choice_A,generator); }
				else { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 10:
				if(!pb.workers.containsChip(pb.getGrandeWorker()))
					{
					addChoice(all,ViticultureId.Choice_A,generator);
					}
				addChoice(all,ViticultureId.Choice_B,generator);	// do nothing
				break;
				
			default: G.Error("Not expecting %s",card);
			}
			break;
		case YellowCard:
			addChoice(all,ViticultureId.Choice_0,generator);
			switch(card.order)
			{
			case 2:		// broker
				if(pb.cash>=9) { addChoice(all,ViticultureId.Choice_A,generator); };
				if(pb.score >= MIN_SCORE+2) { addChoice(all,ViticultureId.Choice_B,generator); };
				break;
			case 4:		// blacksmith
				if(canBuildStructureWithDiscount(pb,2)) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 38:	// wedding party
			case 37:	// volunteer crew
			case 33:	// organizer
			case 31:	// swindler
			case 23:	// importer
			case 21:	// banker
			case 18:	// handyman
			case 17:	// vendor
				addChoice(all,ViticultureId.Choice_A,generator);
				break;
			case 20:	// peddler
				if(pb.cards.height()>=2) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;

			case 22:	// overseer
				if((canPlant(pb) && canBuildStructureWithDiscount(pb,0))
					|| // forced buy of a trellis
					   ((pb.trellis.topChip()==null) 
						&& (pb.cash>=pb.trellis.cost)
						&& (canPlantWithAddedStructure(pb,pb.trellis)))
					|| // forced buy of a water tower
						((pb.waterTower.topChip()==null)
						&& (pb.cash>=pb.waterTower.cost)
						&& (canPlantWithAddedStructure(pb,pb.waterTower))))
						{
							addChoice(all,ViticultureId.Choice_A,generator); 
					    }
				break;
				
			case 25:	// grower
				if(canPlant(pb)) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 26: 	// negotiator
				if(pb.hasGrape()) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.hasWine()) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 27:	// cultivator
				if(canPlantWithoutLimits(pb)) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
			case 29:	// planner
				if(season(pb)<3) { // no future in the winter
					addChoice(all,ViticultureId.Choice_A,generator);
				}
				break;
			case 30:	// agriculturist
				if(canPlant(pb)) 
				{
					addChoice(all,ViticultureId.Choice_A,generator);
				}
				break;
			case 32:	// producer
				if(pb.cash>=2)
				{
					addChoice(all,ViticultureId.Choice_A,generator);
				}
				break;
				
			case 36:	// stonemason
				if(pb.cash>=8) { addChoice(all,ViticultureId.Choice_A,generator); }
				break;
				
			case 6: 	// tour guide
				addChoice(all,ViticultureId.Choice_A,generator);
				all.push(new Viticulturemovespec(MOVE_SELECT,ViticultureId.Choice_A,whoseTurn));
				if(canHarvest(getCurrentPlayerBoard(),generator))
					{ addChoice(all,ViticultureId.Choice_B,generator);
					}
				break;
			case 7:	// novice guide
				addChoice(all,ViticultureId.Choice_A,generator);
				if(pb.hasGrape())
					{ addChoice(all,ViticultureId.Choice_B,generator);
					}
				break;
			case 8: // uncertified broker
				if(pb.score>=MIN_SCORE+3)
				{
					addChoice(all,ViticultureId.Choice_A,generator);
				}
				if(pb.cash>=6)
				{
					addChoice(all,ViticultureId.Choice_B,generator);
				}
				break;
			case 9: // planter
				if(canPlant(pb))
				{
					addChoice(all,ViticultureId.Choice_A,generator);
				}
				if(hasPlantedVine(pb))
				{
					addChoice(all,ViticultureId.Choice_B,generator); 
				}
				break;
			case 10:  // buyer
				{
				if(pb.cash>=2) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(hasGrape(pb)) { addChoice(all,ViticultureId.Choice_B,generator); }
				}
				break;
			case 11:	// landscaper
				addChoice(all,ViticultureId.Choice_A,generator);
				if(nPlantedFields(pb)>=2 && hasSwitchVineMoves(pb)) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 12:	// architect
				if(canBuildStructureWithDiscount(pb,3)) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.nStructuresWithValueExactly(4)>0) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 13:	// uncertified architect
				if(canBuildStructureWithDiscount(pb,100))	// can build for free, most likely is there anything at all left to build
				{
				if(pb.score>=MIN_SCORE+1) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.score>=MIN_SCORE+2) { addChoice(all,ViticultureId.Choice_B,generator); }
				}
				break;
			case 15:	// auctioneer
				if(pb.cards.height()>=2) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.cards.height()>=4) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 16:	// entertainer
				if(pb.cash>=4) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.hasWine() && pb.nVisitorCards()>=3) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 19:
				if(canPlantWithoutStructures(pb)) { addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.nPlantedVines()>=2) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 24:
				if(canPlantWithoutStructures(pb)) {  addChoice(all,ViticultureId.Choice_A,generator); }
				if(pb.nPlantedVines()>=1) { addChoice(all,ViticultureId.Choice_B,generator); }				
				break;

			case 3: 	// wine critic
				addChoice(all,ViticultureId.Choice_A,generator);
				//p1("use wine critic");
				if(pb.hasWineWithValue(revision>=103 ? 7 : 8)) { addChoice(all,ViticultureId.Choice_B,generator); }
				break;
			case 1:		// surveyor
			case 14:	// patron
				// can do both
				addChoice(all,ViticultureId.Choice_A,generator);
				addChoice(all,ViticultureId.Choice_B,generator);
				break;
			case 5: // contractor
				{
				addChoice(all,ViticultureId.Choice_A,generator);
				boolean canBuild = canBuildStructureWithDiscount(pb,0);
				if(canBuild) 
					{ addChoice(all,ViticultureId.Choice_B,generator);
					  addChoice(all,ViticultureId.Choice_AandB,generator);
					}
				if(canPlant(pb) || (canBuild && (canPlantWithAddedStructure(pb,pb.trellis) || canPlantWithAddedStructure(pb,pb.waterTower))))
					{ addChoice(all,ViticultureId.Choice_C,generator);
					  addChoice(all,ViticultureId.Choice_AandC,generator);
					  if(canBuild) { addChoice(all,ViticultureId.Choice_BandC,generator); }
					};
				}
				;
				break;
			case 28: // homesteader
				{	boolean canBuild = canBuildStructureWithDiscount(pb,3);
					// can build either with the discount available
					boolean canPlant = canPlantWithAddedStructure(pb,pb.trellis)
										|| canPlantWithAddedStructure(pb,pb.waterTower);
					if(canBuild) { addChoice(all,ViticultureId.Choice_A,generator); }
					if(canPlant) { addChoice(all,ViticultureId.Choice_B,generator);
								 if(canBuild && (pb.score>MIN_SCORE)) 
								 	{ addChoice(all,ViticultureId.Choice_AandB,generator); 
								 	}
								}
				}
				break;
			case 34: 
				addChoice(all,ViticultureId.Choice_A,generator);
				addChoice(all,ViticultureId.Choice_B,generator);
				addChoice(all,ViticultureId.Choice_AandB,generator);
				break;
			case 35: // artisan
				addChoice(all,ViticultureId.Choice_A,generator);
				if(canBuildStructureWithDiscount(pb,1))
					{ addChoice(all,ViticultureId.Choice_B,generator);
					}
				if(canPlantWithAddedStructure(pb,null))
					{	addChoice(all,ViticultureId.Choice_C,generator);
					}
				break;
			default:
				// can do both
				G.Error("can't handle %s",card);
			}
			break;
		case PapaCard:
			// can always do both
			addChoice(all,ViticultureId.Choice_A,generator);
			addChoice(all,ViticultureId.Choice_B,generator);
			break;
		case StructureCard:
			addChoice(all,ViticultureId.Choice_A,generator);
			break;
		
			
		default: G.Error("Not expecting %s", cardBeingResolved.type);
		}
 }
 private boolean addPayWeddingPartyMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {	boolean some = false;
	if((generator==MoveGenerator.All) || pb.cash>=2+2*pendingMoves.size())	// $2 for each payment already planned
	{
		for(PlayerBoard other : pbs) 
			{ if(other!=pb)
				{	Viticulturemovespec m = new Viticulturemovespec(MOVE_SELECT,other.roosterDisplay,0,whoseTurn);
					if((generator==MoveGenerator.All) || !movestackContains(m,pendingMoves))
					{
						if(all==null) { return(true); }
						some = true;
						all.push(m);
					}
				}
			}
	}
	return(some);
 }
 //
 // pick a victim in the same location to take a random visitor card
 //
 private boolean addStealVisitorMoves(PlayerBoard pb,CommonMoveStack all,ViticultureCell dest,MoveGenerator generator)
 {	boolean some = false;
 	for(PlayerBoard other : pbs) 
 	{	if((other!=pb)
 			&& (other.hasCard(ChipType.YellowCard) || other.hasCard(ChipType.BlueCard))
 			&& other.hasWorkerInRow(dest.parentRow)
 			)
 		{
 		if(all==null) { return(true); }
 		if(revision>=121)
 		{
 		if(other.hasCard(ChipType.YellowCard)) {  	all.push(new Viticulturemovespec(MOVE_SELECT,other.roosterDisplay,1,whoseTurn)); }
 		if(other.hasCard(ChipType.BlueCard)) {  	all.push(new Viticulturemovespec(MOVE_SELECT,other.roosterDisplay,2,whoseTurn)); }
 		}
 		else
 		{
 		Viticulturemovespec m = new Viticulturemovespec(MOVE_SELECT,other.roosterDisplay,0,whoseTurn);
 		all.push(m);
 		}
 		some = true;
 		}
 	}
 	return(some);
 }
 	

 
 // pick 3 players to give a yellow card or a vp
 // any player will do.
 private boolean addTakeYellowMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
 {	boolean some = false;
 	for(PlayerBoard other : pbs) 
 	{ if(other!=pb)
 		{	
 		Viticulturemovespec m = new Viticulturemovespec(MOVE_SELECT,other.roosterDisplay,0,whoseTurn);
 		// the robot uses the accumulation of moves in pendingmoves to limit it's choices
 		// the UI uses pendingmoves to indicate selection.
 		if((generator==MoveGenerator.All) || !movestackContains(m,pendingMoves) )
 		{
 			if(all==null) { return(true); }
 			some = true;
 			all.push(m);
 		}}
	}
	return(some);
 }
 public boolean canSwap(ViticultureCell from,int fromcard,ViticultureCell to,int tocard)
 {	 if(revision<125) { return(true); }
 
	 int toValue = valueOfVines(to);
	 int fromValue = valueOfVines(from);
	 int fromCardLimit = (5+from.row);
	 int toCardLimit = (5+to.row);
	 ViticultureChip fcard = from.chipAtIndex(fromcard);
	 ViticultureChip tcard = to.chipAtIndex(tocard);
	 int fromCardValue = fcard.totalVineValue();
	 int toCardValue = tcard.totalVineValue();
	 return ((toValue+fromCardValue-toCardValue)<=toCardLimit)
				&& ((fromValue+toCardValue-fromCardValue)<=fromCardLimit);		
 }
 public boolean hasSwitchVineMoves(PlayerBoard pb)
 {
	 return(addSwitchVineMoves(pb,null));
 }
 private boolean addSwitchVineMoves(PlayerBoard pb,CommonMoveStack all)
 {	ViticultureCell vines[] = pb.vines;
 	boolean some = false;
 	//p1("switch vines");
 	for(int fidx = vines.length-1; fidx>=0; fidx--)
 	{	ViticultureCell from = vines[fidx];
 		int fh = from.height();
 		if(fh>0)
 		{
 			for(int tidx=fidx-1; tidx>=0; tidx--)
 			{
 				ViticultureCell to = vines[tidx];
 				int th = to.height();
 				if(th>0)
 				{
 					for(int fromcard = fh-1; fromcard>=0; fromcard--)
 					{
 						for(int tocard = th-1; tocard>=0; tocard--)
 						{	if(canSwap(from,fromcard,to,tocard))
 								{
 								if(all==null) { return(true); }
 								all.push(new Viticulturemovespec(MOVE_SWITCH,from,fromcard,to,tocard,whoseTurn));
 								some = true;
 								}
 						}
 					}
 				}
 			}
 		}
 	}
 	return(some);
 }
 // find a move constructed from two selected vine cards that is a valid swap
 public Viticulturemovespec findSwitchMove(PlayerBoard pb)
 {
	 CommonMoveStack all = new CommonMoveStack();
	 addSwitchVineMoves(pb,all);
	 if(pendingMoves.size()==2)
		{
		Viticulturemovespec m1 = (Viticulturemovespec)pendingMoves.elementAt(0);
		Viticulturemovespec m2 = (Viticulturemovespec)pendingMoves.elementAt(1);
		ViticultureCell from = getCell(m1.source,m1.from_col,m1.from_row);
		int fromcard = m1.from_index;
		ViticultureCell to = getCell(m2.source,m2.from_col,m2.from_row);
		int tocard = m2.from_index;
		
		Viticulturemovespec move1 = new Viticulturemovespec(MOVE_SWITCH,from,fromcard,to,tocard,whoseTurn);
		Viticulturemovespec move2 = new Viticulturemovespec(MOVE_SWITCH,to,tocard,from,fromcard,whoseTurn);

		for(int lim=all.size()-1; lim>=0; lim--)
		{
			Viticulturemovespec match = (Viticulturemovespec)all.elementAt(lim);
			if(match.Same_Move_P(move1)||match.Same_Move_P(move2)) { return(match); }
		}
	}
	 return(null);
 }
 private boolean addFillWineMoves(PlayerBoard pb,CommonMoveStack all)
 {	 
 	 return(addFillWineOrders(pb,all,pb.cards,pb.cards));
 }
 private boolean addFillWineOrders(PlayerBoard pb,CommonMoveStack all,ViticultureCell cards,ViticultureCell filter)
 {
 	 boolean some = false;
	 for(int lim = cards.height()-1; lim>=0; lim--)
	 {	ViticultureChip card = cards.chipAtIndex(lim);
	 	if((card.type==ChipType.PurpleCard) && filter.containsChip(card))
	 	{
	 		if(pb.canFillWineOrder(card))
	 		{
	 			if(all==null) { return(true); }
	 			all.push(new Viticulturemovespec(MOVE_FILLWINE,cards,lim,cards,whoseTurn));
	 		}
	 	}
	 }
	 return(some);
 }
 private boolean addFillMercadoMoves(PlayerBoard pb,CommonMoveStack all)
 {	 return(addFillWineOrders(pb,all,pb.cards,pb.fillableWineOrders));
 }
 
 private boolean canFillWineOrder(PlayerBoard pb)
 {	return(addFillWineMoves(pb,null));
 }
 private boolean addSellGrapeMoves(PlayerBoard pb,CommonMoveStack all,int nOfColor,MoveGenerator generator)
 {	boolean some = addSellGrapeMoves(pb,pb.redGrape,all,nOfColor,generator);
 	some |= addSellGrapeMoves(pb,pb.whiteGrape,all,nOfColor,generator);
 	return(some);
 }
 private boolean addSellGrapeMoves(PlayerBoard pb,ViticultureCell grapes[],CommonMoveStack all,int nOfColor,MoveGenerator generator)
 {	int n = 0;
 	// special for the robot move generator, only generate "select" moves, not "deselect" moves
 	boolean checkOnly = generator!=MoveGenerator.All;
 	for(ViticultureCell grape : grapes)
 	{
 		if((grape.topChip()!=null)
 				&& (checkOnly ? !pb.selectedCells.contains(grape) : true))
 		{
 			if(all==null) { return(true); }
 			all.push(new Viticulturemovespec(MOVE_DISCARD,grape,whoseTurn));
 			n++;
 			if(n>=nOfColor) { break; }
 		}
 	}
 	return(n>0);
 }
 private boolean addSellWineMoves(PlayerBoard pb,CommonMoveStack all,int op,int minvalue)
 {	 boolean some = false;
	 for(ViticultureCell wine[] : pb.wineTypes)
	 {	int minWineValue = ViticultureChip.minimumWineValue(wine[0].rackLocation());
		for(int i=0;i<wine.length;i++)
		 {	if((i+minWineValue)>=minvalue)
			{
			 ViticultureCell c = wine[i];
			 if(c.topChip()!=null) 
			 {
				 if(all==null) { return(true); }
				 all.push(new Viticulturemovespec(op,c,whoseTurn));
				 some = true;
				 break;	// don't bother selling higher value wines
			 }
			}
		 }
	 }
	 return(some);
 }

 private boolean addTradeMoves(PlayerBoard pb, CommonMoveStack all)
 {
	 boolean some = false;
	 if(pb.cash>=3)
	 	{ some |= addTradeMoves(pb,ViticultureId.Cash,all); 
	 	}
	 if(pb.score>MIN_SCORE) { some |= addTradeMoves(pb,ViticultureId.VP,all); }
	 if(pb.cards.height()>=2) { some |= addTradeMoves(pb,ViticultureId.Cards,all); }
	 if(pb.hasRedGrape()) 
	 	{ some |= addTradeMoves(pb,ViticultureId.RedGrape,all);
	 	}
	 if(pb.hasWhiteGrape()) 
	 	{ some |= addTradeMoves(pb,ViticultureId.WhiteGrape,all); 
	 	}
	 return(some);
 }
 
 private boolean addTradeMoves(PlayerBoard pb, ViticultureId fromType, CommonMoveStack all)
 {	
 	if(all==null) { return(true); }
 	if(fromType==ViticultureId.Cards)
 	{
 		for(int cardindex = pb.cards.height()-1; cardindex>=0; cardindex--)
 		{
 			for(int secondCardIndex = cardindex-1; secondCardIndex>=0; secondCardIndex--)
 			{	// can give away any combination of cards
 			 	all.push(new Viticulturemovespec(MOVE_TRADE,fromType,pb.colCode,secondCardIndex,cardindex,ViticultureId.Cards,whoseTurn));				
 			}
 		}
 	}
 	else
 	{
 	all.push(new Viticulturemovespec(MOVE_TRADE,fromType,pb.colCode,0,0,ViticultureId.Cash,whoseTurn));
 	all.push(new Viticulturemovespec(MOVE_TRADE,fromType,pb.colCode,0,0,ViticultureId.RedGrape,whoseTurn));
 	all.push(new Viticulturemovespec(MOVE_TRADE,fromType,pb.colCode,0,0,ViticultureId.WhiteGrape,whoseTurn));
 	all.push(new Viticulturemovespec(MOVE_TRADE,fromType,pb.colCode,0,0,ViticultureId.VP,whoseTurn));
 	} 	
 	return(true);
 }
 
 // make simple white and red
 private boolean addWineMoves(CommonMoveStack all,PlayerBoard pb,ViticultureCell []grapes,ViticultureId wine)
 {	boolean some = false;
 	int minValue = (resetState==ViticultureState.Make2WinesNoCellar) ? 4 : 1;
 			
    for(int grapeValue = minValue-1; grapeValue<grapes.length; grapeValue++)
    {	ViticultureCell grape = grapes[grapeValue];
		if(grape.topChip()!=null)
		{
			if(all==null) { return(true); }
			all.push(new Viticulturemovespec(MOVE_MAKEWINE,wine,grape,0,0,whoseTurn));
			some = true;
		}
	 }
	 return(some);
 }
 
 // add a white to complete the champagne or rose wine
 private boolean addRoseWineMoves(CommonMoveStack all,PlayerBoard pb,ViticultureCell redGrape,ViticultureCell redGrape2)
 {	boolean some = false;
 	ViticultureCell whiteGrapes[] = pb.whiteGrape;
 	for(int whiteIndex = 0; whiteIndex<whiteGrapes.length; whiteIndex++)
 	{
 		ViticultureCell whiteGrape = whiteGrapes[whiteIndex];
 		if(whiteGrape.topChip()!=null) 
 		{	
 			if(redGrape2==null)
 				{ // making rose
 				int grapeValue = redGrape.row+1+whiteGrape.row+1;
 				if(grapeValue>=4)
	 				{
	 				if(all==null) { return(true); }
	 				all.push(new Viticulturemovespec(MOVE_MAKEWINE,ViticultureId.RoseWine,redGrape,whiteGrape.row,0,whoseTurn));
	 				if(pb.hasCharmat() && (grapeValue>=7))
	 				{	// can also make a champagne if the value is high enough
		 				all.push(new Viticulturemovespec(MOVE_MAKEWINE,ViticultureId.Champaign,redGrape,0,whiteGrape.row,whoseTurn));
		 				//p1("can make charmat champagne");
	 				}
	 	 			some = true;
					}
 				}
 			else if(redGrape.row+1+redGrape2.row+1+whiteGrape.row+1>=7)
 				{
 				// making champagne
 				if(all==null) { return(true); }
 				all.push(new Viticulturemovespec(MOVE_MAKEWINE,ViticultureId.Champaign,redGrape,redGrape2.row,whiteGrape.row,whoseTurn)); 
 	 			some = true;
 				}
 		}
 	}
	return(some);
 }
 
 // make rose, requires 1 white and 1 red
 private boolean addRoseWineMoves(CommonMoveStack all,PlayerBoard pb,boolean noCellar)
 {	boolean some = false;
	if(noCellar || pb.hasMediumCellar())
	{
		for(int grapeValue = 1; grapeValue<=pb.redGrape.length; grapeValue++)
		{	ViticultureCell grape = pb.redGrape[grapeValue-1];
			if(grape.topChip()!=null)
				 {	boolean ok = addRoseWineMoves(all,pb,grape,null);
					if(ok && all==null) { return(ok); }
					some |= ok;
				 }
		}
	}
	return(some);
 }
 
 // make rose, requires 1 white and 2 red
 private boolean addChampaignMoves(CommonMoveStack all,PlayerBoard pb,boolean noCellar)
 {	boolean some = false;
 	if(noCellar || pb.hasBothCellars())
 	{
 	for(int grapeValue = 1; grapeValue<=pb.redGrape.length; grapeValue++)
 	{	ViticultureCell grape = pb.redGrape[grapeValue-1];
 		if(grape.topChip()!=null)
 		{	// keep looking for a second red to add
 			for(int grapeValue2 = grapeValue+1; grapeValue2<=pb.redGrape.length; grapeValue2++)
 			{ ViticultureCell grape2 = pb.redGrape[grapeValue2-1];
 			if(grape2.topChip()!=null)
 				{ boolean ok = addRoseWineMoves(all,pb,grape,grape2);
 				if(ok && all==null) { return(ok); }
 				some |= ok;
 				}
			 }
		 }
	 }}
	 return(some);
 }
 private boolean addRetrieveCurrentMoves(PlayerBoard pb,CommonMoveStack all)
 {	boolean some = false;
 	if(pickedObject!=null)
 	{	if(all==null) { return(some); }
 		all.push(new Viticulturemovespec(MOVE_RETRIEVE,pickedSourceStack.top(),pickedSourceIndex.top(),pb.workers,whoseTurn));
 	}
	 for(int index = season(pb)*4,limit = (season(pb)+1)*4; index<limit; index++)
	 {
		 ViticultureCell row[] = mainBoardWorkerPlacements[index];
		 for(ViticultureCell c : row)
		 {
			 for(int lim=c.height()-1; lim>=0; lim--)
			 {
				 ViticultureChip chip = c.chipAtIndex(lim);
				 if((chip.type!=ChipType.Professore)
						&& ((revision>=108) ? pb.isMyRegularWorker(chip) : pb.isMyWorker(chip)))
				 {
					 if(all==null) { return(true); }
					 all.push(new Viticulturemovespec(MOVE_RETRIEVE,c,lim,pb.workers,whoseTurn));
					 some = true;
				 }
			 }
		 }
	 }
	 return(some);
 }
 //
 // the official ruling for the producer is that it can retrieve anything
 // except the worker used to play the card.  Usually that's something on
 // a play yellow, but with "manager" blue card the trigger worker can be
 // a blue card, and no worker at all is on "play yellow"
 private boolean addRetrieveProducerMoves(PlayerBoard pb,CommonMoveStack all)
 {	boolean some = false;
  	for(int lim = mainBoardWorkerPlacements.length-1; lim>=0; lim--)
 	{
 		ViticultureCell place[] = mainBoardWorkerPlacements[lim];
 		if(revision>=153 || place[0].rackLocation()!=ViticultureId.PlayYellowWorker)
 		{
 		for(ViticultureCell from : place)
 			{
 			for(int h = from.height()-1; h>=0; h--)
 			{
 				ViticultureChip worker = from.chipAtIndex(h);
 				if(pb.isMyWorker(worker)
 						&& (revision<153 || from!=lastDroppedWorker || h!=lastDroppedWorkerIndex))
 				{	Viticulturemovespec m = new Viticulturemovespec(MOVE_RETRIEVE,from,h,pb.workers,whoseTurn);
 					if(!movestackContains(m, pendingMoves))
 					{
 					if(all==null) { return(true); }
 					some = true;
 					all.push(m);
 					}
 				}
 			}
 			}
 		}
 		if(revision>=114)
 		{
 		for(ViticultureCell c : pb.workerCells) 
 			{  if(c!=pb.pendingWorker)
 			   {ViticultureChip top = c.topChip();
 			   if(top!=null && pb.isMyWorker(top))
 				   {
 				   Viticulturemovespec m = new Viticulturemovespec(MOVE_RETRIEVE,c,c.height()-1,pb.workers,whoseTurn);
					if(!movestackContains(m, pendingMoves))
					{
					if(all==null) { return(true); }
					some = true;
					all.push(m);
					}
 				   }}
 			}}
 	}
 	if(pickedObject!=null)
 		{	if(all==null) { return(true); }
 			all.push(new Viticulturemovespec(MOVE_RETRIEVE,pickedSourceStack.top(),-1,pb.workers,whoseTurn));
 			some = true;
 		}
 	return(some);
 }
 private boolean canMakeWine(PlayerBoard pb)
 {
	 return(addMakeWineMoves(pb,null,false,false));
 }
 
 private boolean addMakeWineMoves(PlayerBoard pb,CommonMoveStack all,boolean noCellar,boolean blushOnly)
 {	boolean some = false;
	 // reds
	 if(!blushOnly)	// Mixer structure
	 	{
		 some |= addWineMoves(all,pb,pb.redGrape,ViticultureId.RedWine);
		 if(some && all==null) { return(true); }
		 some |= addWineMoves(all,pb,pb.whiteGrape,ViticultureId.WhiteWine);
		 if(some && all==null) { return(true); }
	 	}
	 some |= addRoseWineMoves(all,pb,noCellar);
	 if(some && all==null) { return(true); }
	 some |= addChampaignMoves(all,pb,noCellar);
	 return(some);
 }
 
 private boolean canMakeBlushWine(PlayerBoard pb)
 {	return(addRoseWineMoves(null,pb,false));
 }
 private boolean canMakeChampaign(PlayerBoard pb)
 {
	 return(addChampaignMoves(null,pb,false));
 }
 //
 // organizer pick a new row.  Its usually in the summer, but
 // can be in the fall, if invoked by the manager.  Picking 
 // row 7 can be illegal, and in 6p games, that means nothing
 // is legal.
 //
 private boolean canPickNewRow(PlayerBoard pb)
 {
	 return(addPickNewRowMoves(pb,null));
 }
 private boolean addPickNewRowMoves(PlayerBoard pb,CommonMoveStack all)
 {
	ViticultureCell startMeeple = FirstPlayerMeeplePosition();
	boolean some = false;
	if(startMeeple.topChip()==ViticultureChip.StartPlayerMarker) { startMeeple=null; }	// still available
	for(int row = 0;row<roosterTrack.length-(automa?1:0);row++)
	{	boolean occupied = false;
		ViticultureCell theRow[] = roosterTrack[row];
		for(int col=0;!occupied && col<theRow.length; col++)
		{ ViticultureCell c = theRow[col];
		  ViticultureChip top = c.topChip();
		  occupied |= ((top!=null) && (top.type==ChipType.Rooster));
		  // combination of mentor and organizer, if the star player has already
		  // passed out, it's too late to take over as the start player.
		  // in a 6p game, this may legitimately result in no place to go.
		  occupied |= c==startMeeple;
		}
		if(!occupied) 
		{	some = true;
			if(all==null) { return(true); }
		    all.push(new Viticulturemovespec(MOVE_NEWWAKEUP,getCell(ViticultureId.RoosterTrack,pb.wakeupPosition.col,row),whoseTurn));
		}
	}
	return(some);
 }
 
 public boolean hasBonusMoves(ViticultureCell c)
 {	
 	return(!(isDest(c)&&(c.topChip()!=ViticultureChip.Bead))
 			&& isBonusRow(c));
 }
 
 private boolean addBonusMoves(PlayerBoard pb,CommonMoveStack all,MoveGenerator generator)
{	boolean some = false;
 	if(pb.bonusActions.height()>0)
 	{
	 ViticultureCell dest = droppedDestStack.top();
	 if(dest!=null)
	 {
	 ViticultureCell row[] = dest.parentRow;
	 for(ViticultureCell c : row)
		 { if((c!=dest) && isBonusRow(c))
		 	{
			 if(all==null) { return(true); }
			 some = true;
			 all.push(new Viticulturemovespec(MOVE_BONUS,c,whoseTurn));
		 	}
		 }
 	}}
 	return(some);
 }
 // determine if the player can select more cards from the market
 // normally, this is the number of cards he's supposed to select
 // but if there are fewer cards than expected, he might have to
 // be done early
 private boolean canSelectMarketCards(PlayerBoard pb)
 {
	 ViticultureCell cards = pb.oracleCards;
	 int ncards = cards.height();
	 int nfree = resetState.nFree();
	 for(int h=ncards-1; h>=0; h--)
	 {	boolean skip = false;
	 	skip |= pb.selectedCards.contains(h); // already selected
	 	skip |= (pb.cash <= pb.committedCost()) && (h+nfree<ncards);	// not free
	 	if(!skip) { return true; }
	 }
	 return false;
 }

 CommonMoveStack  GetListOfMoves(MoveGenerator generator)
 {	CommonMoveStack all = new CommonMoveStack();
 	int discount = 0;
 	boolean hasDone = false;
 	PlayerBoard pb = getCurrentPlayerBoard();
  	switch(board_state)
 	{
	case Confirm:
 	case FullPass:
 		hasDone = true;
 		all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));
 		if(generator!=MoveGenerator.All) { return(all); }
 		break;
 	default:
 		if(DoneState() || board_state.isWinemaking())
 		{ 
 			hasDone = true;
 			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));
 		}
 	}
 	if(generator==MoveGenerator.Robot)
 	{	// with the windmill or tasting room, peg for points
 		if((pb.tastingRoom.topChip()!=null)
 			|| pb.windmill.topChip()!=null)
 		{
 			generator = MoveGenerator.Runner;
 		}
 		else 
 		{	// without try for making wines
 			generator = MoveGenerator.Harvester;
 		}
 	}

 	switch(resetState)
	{
 	case PlayBonus:
 		addBonusMoves(pb,all,generator);
 		break;
	case Play:
		if(board_state!=ViticultureState.FullPass)
		{
		placeWorkerMoves(all,false,season(pb), (generator==MoveGenerator.All) || (season(pb)==3) ,MOVE_PLACE_WORKER,generator);
		}
		all.push(new Viticulturemovespec(MOVE_NEXTSEASON,whoseTurn));
		break;
	case Pick2Discards:	// negotiator
		// abnormal condition, if there are no discards available? Ie; the innkeeper was played first or
		// second.  I guess it has to be good enough.
		{
		boolean some = addDiscardMoves(pb,all,generator!=MoveGenerator.All);
		Assert(some,"there should be some in %s",resetState);
		}
		break;
	case Retrieve1Current:
		addRetrieveCurrentMoves(pb,all);
		break;
	case Retrieve2Workers:
		addRetrieveProducerMoves(pb,all);
		break;
	case TakeActionPrevious:
		for(int ss = 0; ss<season(pb); ss++)
		{
		placeWorkerSeasonMoves(pb,all,pb.getGrandeWorker(),-1,false,ss,false,MOVE_TAKEACTION,generator);
		}
		break;
	case PlaceWorkerFuture:
		for(int ss = season(pb)+1; ss<=3; ss++)
		{
		placeWorkerMoves(all,true,ss,false,MOVE_PLACE_WORKER,generator);
		}
		break;
 	case Sell1VPfor3:
 		if(pb.score>MIN_SCORE) { all.push(new Viticulturemovespec(MOVE_SELECT,ViticultureId.Choice_A,whoseTurn)); };
 		all.push(new Viticulturemovespec(MOVE_SELECT,ViticultureId.Choice_B,whoseTurn));
 		break;
 	case PlaySecondBlue:
 	case Play1Blue:
 	case Play2Blue:
 	case PlayBlueDollar:
 		addCardMoves(all,pb.cards,ChipType.BlueCard);
 		break;
 	case PlaySecondYellow:
	case Play1Yellow:
	case Play2Yellow:
	case PlayYellowDollar:
		addCardMoves(all,pb.cards,ChipType.YellowCard);
		break;
	case PlantSecondVine:
	case Plant2VinesOptional:
	case Plant1AndGive2:
	case Plant1VineOptional:
		addPlantingMoves(pb,all,false,false,null);
		break;
			
	case Plant1Vine:
	case Plant2Vines:
		addPlantingMoves(pb,all,false,false,null);
		break;
	case Uproot2For3:
    case Uproot1For2:
    case Uproot:
		addUprootMoves(pb,all,generator);
		break;
	case Plant1VineNoLimit:
		addPlantingMoves(pb,all,false,true,null);
		break;
	case Plant1VineNoStructures:
		addPlantingMoves(pb,all,true,false,null);
		break;
	case Plant1For2VPDiversity:
	case Plant1For2VPVolume:
	case PlantVine4ForVP:	// second half of overseer
		addPlantingMoves(pb,all,false,false,null);
		break;
	case HarvestOrUproot:
		addUprootMoves(pb,all,generator);
		//$FALL-THROUGH$
	case Harvest1:
	case Harvest1Optional:
	case Harvest1Dollar:
	case Harvest2:
	case HarvestAndMakeWine:
	case Harvest2AndMake3:
	case HarvestAndFill:
	case Harvest2Optional:
	case Harvest3Optional:
		boolean some = addHarvestMoves(pb,all,generator);
		if(!some && !hasDone)
			{
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));
			}		
		break;
		
	case Discard3CardsAnd1WineFor3VP:	// 3 visitor cards and 1 wine
		//p1("discard cards and wine");
		if((generator==MoveGenerator.All) || (pb.selectedCells.size()==0)) 
			{ addSellWineMoves(pb,all,MOVE_DISCARD,0); 
			}
		if((generator!=MoveGenerator.All) && (pb.nSelectedCards()==3)) { break; }		
		//$FALL-THROUGH$
	case Discard2CardsFor2VP:
		// visitor cards only
		{
		ViticultureCell cards = pb.cards;
		for(int lim = cards.height()-1; lim>=0; lim--)
			{
			ViticultureChip card = cards.chipAtIndex(lim);
			if((generator==MoveGenerator.All) 
					||  !pb.selectedCards.contains(cards.rackLocation(),card,lim))
			{
			switch(card.type)
			{
			case YellowCard:
			case BlueCard:
				all.push(new Viticulturemovespec(MOVE_DISCARD,cards,lim,whoseTurn));
				break;
			default: break;
			}
			}}
		}
		break;
		
	case Discard1ForOracle:
		{ViticultureCell cards = pb.oracleCards;
		 for(int h=cards.height()-1; h>=0; h--)
			 {	ViticultureChip chip = cards.chipAtIndex(h);
			 	int ind = pb.cards.findChip(chip);
			 	Assert(ind>=0,"card %s disappeared",chip);
			 	all.push(new Viticulturemovespec(MOVE_DISCARD,pb.cards,ind,whoseTurn));
			 }
		 }
		 break;
	case DiscardGreen:
	case Discard2Green:
		{
			
		ViticultureCell cards = pb.cards;
		for(int lim = cards.height()-1; lim>=0; lim--)
			{
			ViticultureChip card = pb.cards.chipAtIndex(lim);
			if(card.type==ChipType.GreenCard)
			{
				all.push(new Viticulturemovespec(MOVE_DISCARD,cards.rackLocation(),lim,cards,whoseTurn));
			}
			}
		}
		break;
		
	case DiscardCards:
	case Discard4CardsFor3:
	case Discard2CardsFor4:
	case Discard2CardsForAll:
	case Discard2CardsFor1VP:
		{
		
		ViticultureCell cards = pb.cards;
		for(int lim = cards.height()-1; lim>=0; lim--)
			{
			ViticultureChip card = pb.cards.chipAtIndex(lim);
			if((generator==MoveGenerator.All) 
					|| !pb.selectedCards.contains(cards.rackLocation(),card,lim))
			{
				all.push(new Viticulturemovespec(MOVE_DISCARD,cards.rackLocation(),lim,cards,whoseTurn));
			}
			}
		}
		break;
	case GiveYellow:
		{ViticultureCell cards = pb.cards;
		 for(int lim = cards.height()-1; lim>=0;lim--)
		 	{
			ViticultureChip card = cards.chipAtIndex(lim);
			if(card.type==ChipType.YellowCard)
				{all.push(new Viticulturemovespec(MOVE_SELECT,cards,lim,whoseTurn));
				}
		 	}
		}
		break;
	case TakeYellowOrBlue:
		{
		all.push(new Viticulturemovespec(MOVE_SELECT,yellowCards.rackLocation(),whoseTurn));
		all.push(new Viticulturemovespec(MOVE_SELECT,blueCards.rackLocation(),whoseTurn));
		}
		break;
	case TakeYellowOrGreen:
		{
		all.push(new Viticulturemovespec(MOVE_SELECT,yellowCards.rackLocation(),whoseTurn));
		all.push(new Viticulturemovespec(MOVE_SELECT,greenCards.rackLocation(),whoseTurn));
		}
		break;
	case Take2Cards:
	case TakeCard:
		{
		for(ViticultureCell c : cardStacks)
			{
			all.push(new Viticulturemovespec(MOVE_SELECT,c.rackLocation(),whoseTurn));
			}
		}
		break;
	case FullPass:
		break;
		
	case BuildTourBonus:
	case BuildStructureBonus:
		discount = 1;
		//$FALL-THROUGH$
	case BuildTour:
	case BuildStructure:
		if((currentWorker!=null)
				&& (currentWorker.type==ChipType.Farmer)
				&& (currentAction!=null)
				&& ((currentAction.rackLocation() == ViticultureId.BuildTourWorker)
						|| (currentAction.rackLocation()==ViticultureId.BuildStructureWorker))
				)
			{ discount = 1; 
			  // with the farmer, you can build with a discount
			  //p1("use farmer to build");
			}
		//$FALL-THROUGH$
	case BuildAndPlant:
	case BuildStructureOptional:
	case BuildStructureVP:		// designer, bonus for 6 structures 
	case BuildStructureForBeforePlant:	// overseer, bonus for planting a 4-value
		switch(resetState)
		{
		case BuildTour:
		case BuildTourBonus:
			all.push(new Viticulturemovespec(MOVE_SELECT,ViticultureId.Choice_0,whoseTurn));
			break;
			default: break;
		}
		addBuildingMoves(pb,all,discount,10,generator);
		if((all.size()==0) && (generator!=MoveGenerator.All))
			{
			// allow the robot to make this mistake
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));
			}
		break;
	case Build2StructureFree:
	case BuildStructureFree:
		addBuildingMoves(pb,all,10,10,generator);
		break;
	case BuildStructure23Free:
		addBuildingMoves(pb,all,3,3,generator)	;// max price of 3
		break;
	case BuildAtDiscount2:
	case BuildAtDiscount2forVP:
		addBuildingMoves(pb,all,2,10,generator)	;
		break;
	case BuildStructureDiscount3:
		addBuildingMoves(pb,all,3,10,generator);
		break;
	case ResolveCard_2of3:
		if((generator==MoveGenerator.All) 
				|| (cardResolution==null)
				|| (cardResolution==ViticultureId.Choice_A)
				|| (cardResolution==ViticultureId.Choice_B)
				|| (cardResolution==ViticultureId.Choice_C))
			{ addCardResolutionMoves(all,cardBeingResolved,generator); 
			}
		break;
	case Give2orVP:
	case ResolveCard:
	case ResolveCard_AorBorBoth:
	case ResolveCard_AorBorDone:
		if((generator==MoveGenerator.All) || (cardResolution==null)) { addCardResolutionMoves(all,cardBeingResolved,generator); }
		break;
	case SelectWakeup:
			for(int i=1;i<roosterTrack.length;i++)
			{	ViticultureCell rooster = roosterTrack[i][0];
				if(rooster.topChip()==(automa ? ViticultureChip.Bead : null))
				{ all.push(new Viticulturemovespec(MOVE_SELECTWAKEUP,rooster,whoseTurn));
				}
			}
		break;
	case Place2Star:
	case Place1Star:
			{	
			if(pb.stars.height()>0)
			{
			for(int i=0;i<starTrack.length;i++)
				{
				all.push(new Viticulturemovespec(MOVE_PLACE_STAR,starTrack[i],whoseTurn));
				}}
			}
			break;
	case Move2Star:
	case Move1Star:
			 {		// no more to place, just move them
				ViticultureChip star = pb.getContent(pb.stars);
				for(ViticultureCell c : starTrack)
				{	int index = c.findChip(star);
					if(index>=0)
					{
						for(ViticultureCell d : starTrack)
						{
							if(d!=c)
							{
								all.push(new Viticulturemovespec(MOVE_STAR,c,index,d,whoseTurn));
							}
						}
					}
				}
				
			}
		break;
	case PickNewRow:
		addPickNewRowMoves(pb,all);
		break;
	case Flip:
	case FlipOptional:
		addFlipMoves(pb,all,0);	// we paid off the soldatos already
		break;
	case Make2WinesNoCellar:
		addMakeWineMoves(pb,all,true,false);
		break;
	case MakeMixedWinesForVP:	// mixer
		
		if(!addMakeWineMoves(pb,all,false,true))
		{
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));	// no more wines to make
		}
		//else { //p1("Used mixer successfully"); }
		
		break;
	case Make1Wines:
	case Make2Wines:
	case Make3Wines:
		if(!addMakeWineMoves(pb,all,false,false))
		{
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));	// no more wines to make
		}
		break;
		
	case Make1WineOptional:
	case Make2WinesOptional:
	case Make3WinesOptional:
	case Make4WinesOptional:
	case Make3WinesVP:
	case Make2WinesVP:
	case Make2AndFill:
	case Make2Draw2:
		addMakeWineMoves(pb,all,false,false);
		break;
	case Trade1:
	case Trade2:
	case TradeSecond:
		addTradeMoves(pb,all);
		if((generator!=MoveGenerator.All)&&(all.size()==0))
		{
			// no cards, no money, no vp. This ought to be pretty rare...
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));	// no more wines to make
		}
		break;
	case Sell1Wine:
	case Sell1WineOptional:
		addSellWineMoves(pb,all,MOVE_SELLWINE,0);
		if(all.size()==0)
		{	// rare case, the wine was sold by card-induced actions. For example
			// mercado can fill a wine order if the star bonus gets a wine card.
			all.push(new Viticulturemovespec(MOVE_DONE,whoseTurn));	// no more wines to make
		}
		break;
	case Age1AndFill:
	case Age1Twice:
		addAgeWineMoves(pb,all,false);
		break;
	case Age2Once:
		addAge2WineMoves(pb,all,false);	// age a single wine
		break;
	case FillMercado:	// just drawn wine order
		addFillMercadoMoves(pb,all);
		break;
	case FillWine:
	case FillWineOptional:
	case FillWineBonus:
	case FillWineFor2VPMore:
	case FillWineBonusOptional:
		addFillWineMoves(pb,all);
		break;
	case StealVisitorCard:
		addStealVisitorMoves(pb,all,currentAction,generator);
		break;
	case TakeYellowOrVP:
		addTakeYellowMoves(pb,all,generator);
		break;
	case PayWeddingPartyOverpay:
	case PayWeddingParty:
		addPayWeddingPartyMoves(pb,all,generator);
		break;
	case SwitchVines:
		addSwitchVineMoves(pb,all);
		break;
	case Discard2GrapesFor3VP:
		//p1("discard 2 grapes");
		addSellGrapeMoves(pb,all,2,generator);
		break;
	case DiscardGrapeFor3And1VP:
	case DiscardGrapeFor2VP:
		//p1("discard grape");
		addSellGrapeMoves(pb,all,1,generator);
		break;
	case DiscardGrapeAndWine:	// ristorante
		{
		ViticultureCell current = pb.selectedCells.top();
		ChipType type =  (generator==MoveGenerator.All || current==null)? ChipType.Any : current.topChip().type;
		switch(type)
		{
		case Any:
			addSellGrapeMoves(pb,all,1,generator);
			//$FALL-THROUGH$
		case RedGrape:
		case WhiteGrape:
			//p1("discard grape and");
			addSellWineMoves(pb,all,MOVE_DISCARD,0);
			break;
		case Champagne:
		case RedWine:
		case WhiteWine:
		case RoseWine:
			addSellGrapeMoves(pb,all,1,generator);
			break;
		default: G.Error("Dont' expect %s",type);
		}
		break;
		}
	case DiscardGrapeOrWine:
		//p1("discard grape or");
		addSellWineMoves(pb,all,MOVE_DISCARD,0);
		addSellGrapeMoves(pb,all,1,generator);
		break;
	case DiscardWineFor4VP:
		//p1("discard wine 4");
		addSellWineMoves(pb,all,MOVE_SELLWINE,7);
		break;
	case DiscardWineFor2VP:
		//p1("discard wine2");
		addSellWineMoves(pb,all,MOVE_SELLWINE,0);
		break;
	case DiscardWineFor3VP:
		//p1("discard wine3");
		addSellWineMoves(pb,all,MOVE_SELLWINE,4);
		break;
	case DiscardWineForCashAndVP:
		//p1("discard wine cash");
		addSellWineMoves(pb,all,MOVE_SELLWINE,0);
		for(int value = 9; value>0; value--)
		{ 	
			int ihave = 0;
			int allhave = 0;
			for(PlayerBoard p : pbs) 
				{ int v = p.nWinesWithValue(value);
				  allhave += v;
				  if(p==pb) { ihave = v; }
				}
			if(allhave > 0)
			{	// if we have a unique high-value bottle, offer to sell it too
				if(allhave == 1 && ihave == 1) { addSellWineMoves(pb,all,MOVE_SELLWINE,value); }
				break;
			}
		}
		break;
	case Pick2TopCards:
		addPickTop2Moves(pb,all,pb.cards,generator);
		break;
	case DestroyStructure:
	case DestroyStructureOptional:
		addUnbuildStructureMoves(pb,all,generator);
		break;
	case TrainWorker:
	case TrainWorkerOptional:
	case TrainWorkerDiscount1:
	case TrainWorkerDiscount2:
	case TrainWorkerDiscount3:
	case TrainWorkerDiscount4:
	case TrainWorkerDiscount1AndUse:
	case TrainWorkerAndUseFree:
		addTrainWorkerMoves(pb,all,generator);
		break;
	case MisplacedMessenger:	// do nothing
		break;
	case SelectCardColor:
		for(ViticultureCell c : pb.oracleColors)
		{
			all.push(new Viticulturemovespec(MOVE_SELECT,c.rackLocation(),whoseTurn));
		}
		break;
	case Select1Of1FromMarket:
	case Select2Of2FromMarket:
	case Select1Of2FromMarket:	// oracle gets one extrafree card choice
	case Select2Of3FromMarket:
		{
		ViticultureCell cards = pb.oracleCards;
		int ncards = cards.height();
		for(int h=ncards-1; h>=0; h--)
			 {	boolean skip = false;
			 	
			 	if(generator!=MoveGenerator.All) 
			 	{	int nfree = resetState.nFree();
			 		skip |= pb.selectedCards.contains(h); // already selected
			 		skip |= (pb.cash <= pb.committedCost()) && (h+nfree<ncards);	// not free
			 	}
			 	if(!skip) { all.push(new Viticulturemovespec(MOVE_SELECT,cards,h,whoseTurn)); }
		 }
		}
		break;
	case SelectPandM:	// draft mama and papa cards
		{
		ViticultureCell cards = pb.cards;
		boolean hasmama = pb.selectedCards.contains(ViticultureId.MamaCards);
		boolean haspapa = pb.selectedCards.contains(ViticultureId.PapaCards);
		
		for(int lim = cards.height()-1; lim>=0; lim--)
			{
			ViticultureChip card = pb.cards.chipAtIndex(lim);
			switch(card.type){
			default: throw G.Error("Not expecting ",card);
			case MamaCard:	
				if(!hasmama || (generator==MoveGenerator.All))
					{ all.push(new Viticulturemovespec(MOVE_SELECT,cards.rackLocation(),lim,cards,whoseTurn));
					
					}
				break;
			case PapaCard:
				if(!haspapa || (generator==MoveGenerator.All))
				{
					all.push(new Viticulturemovespec(MOVE_SELECT,choiceA,lim,whoseTurn));
					all.push(new Viticulturemovespec(MOVE_SELECT,choiceB,lim,whoseTurn));
				}
			}
			
			}
		}
		break;
	case ChooseOptions:
		for(Option op : Option.values())
		{
			all.push(new Viticulturemovespec(EPHEMERAL_OPTION,op,testOption(op),whoseTurn));
		}
		break;
	default:
		G.Error("Not expecting sate %s",resetState);
		break;
	}
 	if(generator!=MoveGenerator.All)
 	{
 		//G.print("All "+all+" "+resetState);
 	}
 	if((generator!=MoveGenerator.All) && all.size()==0)
 	{	p1("no moves generated");
 		G.Error("No moves generated in state %s %s",resetState,generator);
 	}
 	return(all);
 }
public int cellToX(ViticultureCell c) {
	throw G.Error("Not expected");
}
public int cellToY(ViticultureCell c) {
	throw G.Error("Not expected");
}
public int pToX(double pos)
{
	return(G.Left(boardRect)+(int)(pos*G.Width(boardRect)));
}
public int pToY(double pos)
{
	return(G.Top(boardRect)+(int)(pos*G.Height(boardRect)));
}
public int pToS(double sz) { return((int)(sz*G.Width(boardRect))); }

// this is called from robot initialization so the bot moves
// don't know the actual card order
public void randomizeHiddenState(Random random)
{
   	greenCards.shuffle(random);
   	yellowCards.shuffle(random);
   	purpleCards.shuffle(random);
   	blueCards.shuffle(random);
   	structureCards.shuffle(random);
}
StackIterator<ViticultureChip>push(StackIterator<ViticultureChip>from,ViticultureChip c)
	{
	return(from==null ? c : from.push(c));
	}

// for advanced robots
double scoreAsHarvesterMove_01(Viticulturemovespec m)
{	double score = 1.0;
	PlayerBoard pb = pbs[m.player];
	switch(m.op)
	{
	case MOVE_PLACE_WORKER:
		{	ViticultureCell  c = getCell(m.dest,m.to_col,m.to_row);
			if(isBonusRow(c)) { score *= 2; }
			switch(m.dest)
			{
			case StarPlacementWorker:
				if(pb.stars.height()==0) { score = 0; }
				break;
			case DrawGreenWorker: 
				break;
			case GiveTourWorker:
				if(pb.cash>8) { score = 0; }		// not if we're already rich 
				break;
			case BuildStructureWorker:
				
				break;
				
				
			case PlantWorker:
				score += (4-pb.nPlantedVines());	// impetus to plant
				break;
			default: break;
			
			case HarvestWorker:
				
				break;
			case RecruitWorker:
				score *= 2;
				break;
			}
		}
		break;
	default: break;
	}
	return(score);
}
//for advanced robots
double scoreAsRunnerMove_01(Viticulturemovespec m)
{	double score = 1.0;
	PlayerBoard pb = pbs[m.player];
	switch(m.op)
	{
	case MOVE_PLACE_WORKER:
		{
		ViticultureCell  c = getCell(m.dest,m.to_col,m.to_row);
		if(isBonusRow(c)) { score *= 2; }
		switch(m.dest)
		{
		case StarPlacementWorker:
			if(pb.stars.height()==0) { score = 0; }
			break;
		case DrawGreenWorker: 
			break;
		case GiveTourWorker:
			if(pb.cash>8 && (pb.tastingRoom.topChip()==null)) { score = 0; }	// limit cash unless there's a tasting room
			break;
		case BuildStructureWorker:
			
			break;
			
		case PlantWorker:
			if(pb.windmill.topChip()!=null) { score = 2; }		// enhance if you have a windmill
			break;
		default: break;
		
		
		case HarvestWorker:
			
			break;
		case RecruitWorker:
			score *= 2;
			break;

		}
		}
		break;
	default:
	}
	return(score);
}

}


/**
reference replay 3/2/2023 v7.09

summary:
225: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip U!VI-mgahagen-epatterson-lmarkus001-Lgahagen-2020-04-12-2352.sgf lib.ErrorX: must be a blue card
267: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-ddyer-Wilson17-sven2-lfedel-mfeber-idyer-2020-04-19-1907.sgf lib.ErrorX: must be a blue card
284: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-mfeber-ddyer-idyer-sven2-2020-04-12-1916.sgf lib.ErrorX: Not expecting drop in state PayWeddingParty
286: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-mfeber-Runcible-lfedel-idyer-ddyer-sven2-2020-04-11-0218.sgf lib.ErrorX: must be a blue card
298: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-wilson17-ddyer-sven2-2020-04-15-1937.sgf lib.ErrorX: Not expecting state Play

1971 files visited 5 problems

*/

