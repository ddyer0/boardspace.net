package euphoria;


import static euphoria.EuphoriaMovespec.*;
		
import java.util.*;

import euphoria.EPlayer.PFlag;
import euphoria.EPlayer.TFlag;
import lib.*;
import lib.Random;
import online.game.*;


/**
 * 
 * 
 * 				The main board class
 * 
 *
 * @author ddyer
 *
 */
public class EuphoriaBoard extends EuphoriaBoardConstructor implements EuphoriaConstants
{		static int REVISION = 124;			// revision numbers start at 100
//TODO: rotate market cards when enlarged
//TODO: open artifacts when unseen on touch screens
//TODO: tweak side screen layout for more square screen shape

	boolean LOG_ARTIFACTS = false;

	public class ContinuationStack extends OStack<Continuation>
	{
		public Continuation[] newComponentArray(int n) { return(new Continuation[n]); }
	}
	Variation variation  = Variation.Euphoria;
	boolean GAME_EU_Dumbot_Brius_2014_12_20_2045 = false;
	// revision 101 fixed the payment of bliss+commodity
	// revision 102 changes randomizer from moveNumer()+stepNumber to rollNumber()
	// revision 103 fixed a bug in resolving the dilemma, where it would take 2 when it should offer a choice.
	// revision 104 fixes a bug where you could swap the bottom card until you got one you liked
	// revision 105 fixes counting of artifact pairs
	// revision 106 fixed nakagawa the tribute, who took the worker and gave no star
	// revision 107 fixes the "rolling doubles" bug, no new doubles mid-turn
	// revision 108 fixes matthewthethief, who was getting free stuff instead of paying with knowledge.
	// revision 109 fixed extra knowledge from theater of revelatory propaganda
	// revision 110 adds losing workers due to increasing knowledge
	// revision 111 fixes fetch for marketbasket items on the board.
	// revision 112 fixes esmethefireman, didn't get invoked for icarite stars.
	// revision 113	fixes lost recruitoption for bentheludologist and other screwups when rerolling
	// revision 114 fixed jonathantheartist to activate on contstruction sites
	// revision 115 fixed a bug where market cost of energyx4_card cost only the card.
	// revision 116 fixed influence from BrianTheViticulturist
	// revision 117 fixes jacko the archivist_v2
	// revision 118 fixes the interaction between flartner the luddite and michael the engineer
	// revision 119 fixes the place-new-worker bug associated with chase the miner
	// revision 120 checks for hidden recruits again after all continuations are done.
	// revision 121 fixes esme the fireman benefit when book is used.
	// revision 122 removes knowledge checks other than at rolls.
	// revision 123 fixes recycling of some artifacts - they weren't and so couldn't come up again. 
	//				this also is the demarcation between traditional and ignorance is bliss rules
	// revision 124 adds the "lose morale" payment for doubles
	public int getMaxRevisionLevel() { return(REVISION); }
	
	
	void assert_isV12()
	{
		Assert(!isIIB(),"must not be IIB");
	}
	void assert_isIIB()
	{
		Assert(isIIB(),"must be iib");
	}
	boolean isIIB() { return variation.isIIB(); }
	 
	
	/**
	 * the Continuation class is the heart of the mechanism that lets user interface subroutines
	 * stack and nest in complicated ways.
	 * 
	 * Different situations require different information, but all have at least 
	 * one function to call as the primary place to go.  In some cases this it :return
	 * where the place to go is back.
	 * 
	 * @author ddyer
	 *
	 */
	class Continuation
	{
		Benefit pendingBenefit = null;		// used in "collect a benefit" dialogs.
		RecruitChip activeRecruit = null;	// used in "apply this recruit power" dialogs
		Cost pendingCost = null;			// a cost we haven't paid
		Cost originalPendingCost = null;	// cost before adjustment
		EuphoriaState pendingState = null;	// the board state when starting the continuation
		EuphoriaState exitState = null;		// the board state when exiting the continuation
		private Function pendingContinuation = null;			// the normal continuation, and the "yes" continuation for recruits
		private Function pendingUnpaidContinuation = null;		// the normal continuation, and the "no" continuaatino for recruits
		private int pendingPlayer = -1;							// the current player when starting the continuation
		private int pendingEPlayer = -1;					// the player for setting up EPlayer arguments
		private Function pendingSecondContinuation = null;		// 
		private EuphoriaCell pendingContinuationCell = null;	// cell whose cost we haven't paid
		private EuphoriaChip extraChip = null;
		private int extraInt = 0;
		private Allegiance allegiance;
		EuphoriaCell pendingContinuationCell()
		{
     		Assert(pendingContinuationCell!=null,"contuation cell shouldn't be null");
     		return(pendingContinuationCell);
		}
		void doContinuation()
		{
			G.Error("this should be redefined");
		}
		
		Colors pendingColor = null;
		private ProceedStep proceedStep = ProceedStep.Start;
		public String toString() 
		{
			return( "<continuation "
						+ ((activeRecruit!=null) ? activeRecruit.toString()
								: pendingCost!=null ? pendingCost.toString()
										: (pendingBenefit!=null) ? pendingBenefit.toString() 
												: pendingContinuation)
						+ ">");
		}
		
		// constructor for clone()
		public Continuation()
		{	exitState = pendingState = board_state;
			proceedStep = proceedGameStep;
			pendingPlayer = whoseTurn;
		}
		// constructor for mandatory (eventual) continuation
		private Continuation(EuphoriaCell dest,EuphoriaState newState, Function continuation,EPlayer p)
		{
			this();
			pendingContinuationCell = dest;
			pendingState = newState;
			pendingUnpaidContinuation = pendingContinuation = continuation;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
	    
		// constructor for intermodal function
		private Continuation(Function continuation,Colors pl,Function nextContinuation)
		{
			this();
			Assert(continuation!=Function.DropWorkerAfterBump,"continuation not bump");
			Assert(nextContinuation!=Function.DropWorkerAfterBump,"nextcontinuation not bump");
			pendingColor = pl;
			pendingUnpaidContinuation = pendingContinuation = continuation;
			pendingSecondContinuation = nextContinuation;
			
		}
		
		// constructor for recruit option state with no destination cell
		private Continuation(RecruitChip recruit,Function paidContinuation,Function unpaidContinuation,EPlayer p)
		{	this();
			useRecruit(recruit,"ask");
			activeRecruit = recruit;
			pendingContinuation = paidContinuation;
			pendingUnpaidContinuation = unpaidContinuation;
			pendingState = recruit.optionState();
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
		
		private Continuation(RecruitChip recruit,Function paid,	Function unpaid, Function continuation, EPlayer p) {
			this(recruit,paid,unpaid,p);
			useRecruit(recruit,"ask");
			pendingSecondContinuation = continuation;
		}

		// constructor for recruit option state
		private Continuation(RecruitChip recruit,EuphoriaCell dest,Function paid,Function unpaid,EPlayer p)
		{	this(recruit,paid,unpaid,p);
			pendingContinuationCell = dest;
			Assert(dest!=null,"contuation cell shouldn't be null");
		}
		
		private Continuation(RecruitChip recruit, Function paid, Function unpaid,
					WorkerChip chosen, EPlayer p) 
		{
			this(recruit,paid,unpaid,p);
			extraChip = chosen;
		}
		private Continuation(RecruitChip recruit, Function paid, Function unpaid,
				int extra, EPlayer p) 
		{
		this(recruit,paid,unpaid,p);
		extraInt = extra;
		}
		private Continuation(RecruitChip recruit, Function doit, Function dont,
				Cost penalty, int extraKnowledge, EPlayer p) 
		{
			this(recruit,doit,dont,p);
			pendingCost = penalty;
			extraInt = extraKnowledge;
		}

		// pmai the nurse
		private Continuation(RecruitChip recruit, EuphoriaCell dest, replayMode replay, Function paid,
				Function unpaid, Benefit bene, EPlayer p) 
		{
			this(recruit,dest,paid,unpaid,p);
			pendingBenefit = bene;
			pendingState = recruit.optionState();
		}

		// constructor for mandatory payment
		private Continuation(Cost cost,Function cont,EPlayer p)
		{	this();
			Assert(cont!=Function.DropWorkerAfterBump,"continuation not bump");
			originalPendingCost = pendingCost = cost;
			pendingState = cost.paymentState();
			Assert(pendingState!=null, "state must not be null");
			pendingContinuation = pendingUnpaidContinuation = cont; 
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}

		// constructor for mandatory payment
		private Continuation(Cost cost,EuphoriaCell dest,Function cont,EPlayer p)
		{	this(cost,cont,p);
    		Assert(dest!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = dest;
		}
		// constructor for mandatory payment
		private Continuation(Cost c,EuphoriaCell dst,RecruitChip active,Function cont,EPlayer p)
		{	this(c,dst,cont,p);
			activeRecruit = active;
		}

		// alternate constructor for mandatory payment
		private Continuation(Cost original,Cost cost,EuphoriaCell cell,Function unpaid,Function cont)
		{	this(original,cost,cont);
     		Assert(cell!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = cell;
			pendingContinuation = cont;
			pendingUnpaidContinuation = unpaid;
		}
		// alternate constructor for mandatory payment
		private Continuation(Cost original,Cost cost,Function cont)
		{	this();
			Assert(cont!=Function.DropWorkerAfterBump,"not bump");
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = cost.paymentState();
			Assert(pendingState!=null, "state must not be null");
			pendingContinuation = cont; 
		}

		// constructor for optional payment
		private Continuation(Cost original,Cost cost,Function paid,Function unpaid,EPlayer p)
		{	this();
			Assert(paid!=Function.DropWorkerAfterBump,"not bump");
			Assert(unpaid!=Function.DropWorkerAfterBump,"not bump");
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = cost.paymentState();
			Assert(pendingState!=null, "state must not be null");
			pendingContinuation = paid; 
			pendingUnpaidContinuation = unpaid;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}	
		// constructor for collect benefit
		private Continuation(Benefit bene,EuphoriaCell cell,Function cont,EPlayer p)
		{	this();
			pendingBenefit = bene;
			pendingState = bene.collectionState();
			Assert(pendingState!=null,"state must not be null");
			pendingContinuationCell = cell;
			pendingContinuation = cont;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
		private Continuation(Benefit bene, Function cont,	WorkerChip chosenValue,EPlayer p) 
		{	this();
			pendingBenefit = bene;
			pendingState = bene.collectionState();
			Assert(pendingState!=null,"state must not be null");
			pendingContinuation = cont;
			extraChip = chosenValue;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}

		// alternate constructor for collect benefit
		private Continuation(Benefit bene,Function cont,EPlayer p)
		{	this();
			pendingBenefit = bene;
			pendingState = bene.collectionState();
			Assert(pendingState!=null,"state must not be null");
			pendingContinuation = cont;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
		private Continuation(EuphoriaCell dest, Function func, WorkerChip chosen, Cost penalty,
				int extraKnowledge, EPlayer p) {
			this();
			pendingContinuationCell = dest;
			pendingContinuation = func;
			extraChip = chosen;
			pendingCost = penalty;
			extraInt = extraKnowledge;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
			
		}
		private Continuation(Function return1) {
			this();
			pendingContinuation = pendingUnpaidContinuation = return1;
		}
		private Continuation(EuphoriaState state, Function continuation, EPlayer p) {
			this(continuation);
			pendingState = state;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
		
		private Continuation(Function doit, Cost penalty, int extraKnowledge,EPlayer p) 
		{
			this();
			pendingContinuation = doit;
			pendingCost = penalty;
			extraInt = extraKnowledge;
			if(p!=null) { pendingEPlayer = p.boardIndex; }
		}
		// digest for Continuations
		public long Digest(Random r)
		{	long v = 0;
			v ^= EuphoriaCell.Digest(r,pendingContinuationCell);
			v ^= EuphoriaChip.Digest(r,activeRecruit);
			v ^= r.nextLong()*(pendingState.ordinal()+1);
			v ^= r.nextLong()*(exitState.ordinal()+1);
			v ^= r.nextLong()*(proceedStep.ordinal()+1);
			v ^= r.nextLong()*(pendingPlayer+1);
			v ^= r.nextLong()*(pendingEPlayer+1);
			v ^= r.nextLong()*((pendingContinuation!=null)?(pendingContinuation.ordinal()+1):0);
			v ^= r.nextLong()*((pendingUnpaidContinuation!=null)?(pendingUnpaidContinuation.ordinal()+1):0);
			v ^= r.nextLong()*((pendingCost!=null)?(pendingCost.ordinal()+1):0);
			v ^= r.nextLong()*((originalPendingCost!=null)?(originalPendingCost.ordinal()+1):0);
			v ^= r.nextLong()*((pendingBenefit!=null)?(pendingBenefit.ordinal()+1):0);
			v ^= r.nextLong()*((extraChip!=null) ? extraChip.chipNumber()+1 : 0);
			v ^= r.nextLong()*((allegiance==null)? -1 : allegiance.ordinal());
			v ^= r.nextLong()*extraInt;
			return(v);
		}
		public void sameAs(Continuation other)
		{	Assert(pendingState==other.pendingState,"pending state mismatch");
			Assert(exitState==other.exitState,"exit state mismatch");
			Assert(proceedStep==other.proceedStep,"proceedStep mismatch");
			Assert(pendingPlayer==other.pendingPlayer,"pendingPlayer mismatch");
			Assert(pendingEPlayer==other.pendingEPlayer,"pendingEPlayer mismatch");
	        Assert(pendingBenefit==other.pendingBenefit,"pending benefit mismatch");
	        Assert(activeRecruit==other.activeRecruit,"active recruit matches");
	        Assert(pendingCost==other.pendingCost,"pending cost mismatch");
	        Assert(originalPendingCost==other.originalPendingCost,"original pending cost mismatch");
	        Assert(pendingUnpaidContinuation==other.pendingUnpaidContinuation,"unpaid continuation mismatch");
	        Assert(pendingContinuation==other.pendingContinuation,"continuation mismatch");
	        Assert(sameCells(pendingContinuationCell,other.pendingContinuationCell),"pending cost mismatch");
	        Assert(extraChip==other.extraChip,"extrachip mismatch");
	        Assert(extraInt==other.extraInt, "extraInt mismatch");
	        Assert(allegiance==other.allegiance, "allegiance mismatch");
		}
		/**
		 * note that this is called to clone the continuation stack when a new robot
		 * is being spawned.  All the variables in the continuation had better be
		 * copied and/or relocated in the new board instance, or you will get mysterious
		 * failures only when the game reaches a complex state.  In particular, you
		 * can't use the elegant form new Continuation(){ run() } because that defines
		 * a new class, and clone won't clone the new class type or the final variables
		 * this might be embedded in the elegant continuation
		 * @param b
		 * @return
		 */
		public Continuation clone(EuphoriaBoard b)
		{	Continuation copy = new Continuation();
			copy.activeRecruit = activeRecruit;
			copy.pendingBenefit = pendingBenefit;
			copy.pendingContinuationCell = b.getCell(pendingContinuationCell);
			Assert((copy.pendingContinuationCell!=null) || (pendingContinuationCell==null),"continuation cell ok");
			copy.pendingCost = pendingCost;
			copy.originalPendingCost = originalPendingCost;
			copy.pendingContinuation = pendingContinuation;
			copy.pendingUnpaidContinuation = pendingUnpaidContinuation;
			copy.pendingState = pendingState;
			copy.exitState = exitState;
			copy.proceedStep = proceedStep;
			copy.pendingPlayer = pendingPlayer;
			copy.pendingEPlayer = pendingEPlayer;
			copy.pendingColor = pendingColor;
			copy.pendingSecondContinuation = pendingSecondContinuation;
			copy.extraChip = extraChip;
			copy.extraInt = extraInt;
			copy.allegiance = allegiance;
			sameAs(copy);
			return(copy);
			
			
		}
	}
	void makeContinuation(Benefit bene,Function cont,EPlayer p)
	{	Continuation c = new Continuation(bene,cont,p);
		setContinuation(c);
	}
	
	public void setWhoseTurnTo(int who,String msg)
	{
		if(who!=whoseTurn)
			{ //p1(msg+" from "+whoseTurn+" to "+who); 
			  whoseTurn = who; 
			}
	}

	void makeBrenda(EPlayer p)
	{
		setContinuation(new Continuation(RecruitChip.BrendaTheKnowledgeBringer,Function.DoBrendaTheKnowledgeBringer,Function.Return,p));
		setWhoseTurnTo(p.boardIndex,"BrendaTheKnowledgeBringer");
	}
	void doBrendaTheKnowledgeBringer(EPlayer p,replayMode replay)
	{	//p1("do brenda the knowledge bringer");
		p.payCostOrElse(Cost.Knowledge,replay);
    	setContinuation(new Continuation(Benefit.Artifact,Function.Return,p));  
	}
	
	public ContinuationStack continuationStack = new ContinuationStack();
    //
    // set up a new state to extract a payment and continue when paid
    //
    public void setContinuation(Continuation cc)
    {	// at this point, there should be no unconfirmed elements, so the acceptPlacement ought
    	// to be at least harmless.  If the destination is needed, it is in the continuation.
    	// adding the acceptPlacement() here fixes a problem where a "recruitoption" state is
    	// needed while winding up a move.
    	// if(droppedDestStack.size()>0) { G.print("Clearing stack"); }
		/*
		if((cc.pendingContinuation!=Function.Return)&&(cc.pendingContinuation!=Function.DoBenTheLudologist))
			{for(int lim=continuationStack.size()-1; lim>=0; lim--)
		{
			Continuation cont = continuationStack.elementAt(lim);
			Assert(cont.pendingContinuation!=cc.pendingContinuation,"not the same");
		}}
		*/
     	continuationStack.push(cc);
    	setState(cc.pendingState);
    }
    public void pushContinuation(Continuation cc)
    {
    	Continuation x = continuationStack.pop();
    	continuationStack.push(cc);
    	continuationStack.push(x);
    }

    //
    // see the note in "copyFrom" to explain why there are all these functions.
    //
    private void executeContinuation(Continuation cont,Function cc,int player,replayMode replay)
    {	
    	//G.print("Continuation after "+cont+ " is "+cc+" in state %s",cont.exitState+" step "+cont.proceedStep);
    	setState(cont.exitState);
    	proceedGameStep = cont.proceedStep;
    	setWhoseTurn(player);
    	
     	switch(cc)
     	{
     	case Return:
     		break;
     	case DoXanderTheExcavator:
     		doXanderTheExcavator(cont.pendingContinuationCell,replay);
     		break;
     	case DoStevenTheScholar:
     		// this one place, pendingContinuationCell may be null or not.
     		doStevenTheScholar(cont.pendingContinuationCell,replay,cont.pendingColor,cont.pendingSecondContinuation);
     		break;
     	case DropWorkerAfterBump:
     		dropWorkerAfterBump(cont.pendingContinuationCell(),replay);
     		break;
     	case BumpWorkerJuliaTheThoughtInspector_V2:
     		bumpWorkerJuliaTheThoughtInspector_V2(replay);
     		break;
     	case DoJuliaTheThoughtInspector:
     		doJuliaTheThoughtInspector(cont.pendingContinuationCell(),replay);
     		break;
     	case DoJuliaTheThoughtInspector_V2:
     		doJuliaTheThoughtInspector_V2(replay);
     		break;
     	case DropWorkerMaximeTheAmbassador:
     		dropWorkerMaximeTheAmbassador(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerLeeTheGossip:
     		dropWorkerLeeTheGossip(cont.pendingContinuationCell(),replay);
     		break;
     	case DoLeeTheGossip:
     		doLeeTheGossip(cont.pendingContinuationCell(),replay);
     		break;
     	case DoSheppardTheLobotomistSacrifice:
     		doSheppardTheLobotomistSacrifice(replay);
     		break;
     	case DoPeteTheCannibalBenefit:
     		doPeteTheCannibalBenefit(cont.pendingContinuationCell(),replay);
     		break;
     	case DoSheppardTheLobotomistBenefit:
     		doSheppardTheLobotomistBenefit(cont.pendingContinuationCell(),replay);
     		break;
     	case DoPeteTheCannibalSacrifice:
     		doPeteTheCannibalSacrifice(players[cont.pendingEPlayer],replay);
     		break;
     	case ReRollSheppardTheLobotomist:
     		reRollSheppardTheLobotomist(players[cont.pendingEPlayer],replay);
     		break;
     	case DropWorkerPhilTheSpy:
     		dropWorkerPhilTheSpy(cont.pendingContinuationCell(),replay);
     		break;
     	case DoPhilTheSpy:
     		doPhilTheSpy(cont.pendingContinuationCell(),replay,cont.activeRecruit);
     		break;
     	case DoSheppardTheLobotomistMorale:
     		doSheppardTheLobotomistMorale(cont.pendingContinuationCell(),replay);
     		break;
     	case DoSheppardTheLobotomistGainCard:
     		doSheppardTheLobotomistGainCard(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerJuliaTheThoughtInspector:
     		dropWorkerJuliaTheThoughtInspector(cont.pendingContinuationCell(),replay);
     		break;
     	case DoBradlyTheFuturist:
     		doBradlyTheFuturist(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerNakagawaTheTribute:
    		dropWorkerNakagawaTheTribute(cont.pendingContinuationCell(),replay);
     		break;
     	case DoNakagawaTheTribute:
     		doNakagawaTheTribute(cont.pendingContinuationCell(),replay);
     		break;
     	case DoNakagawaTheTribute_V2:
     		doNakagawaTheTribute_V2(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerAfterMorale:
     		dropWorkerAfterMorale(players[cont.pendingEPlayer],cont.pendingContinuationCell(),replay);
     		break;
      	case DropWorkerKyleTheScavenger:
     		dropWorkerKyleTheScavenger(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerRebeccaThePeddler:
     		dropWorkerRebeccaThePeddler(cont.pendingContinuationCell(),replay);
     		break;
     	case DoChaseTheMinerSacrifice:
     		doChaseTheMinerSacrifice(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerChaseTheMinerSacrifice:
     		dropWorkerChaseTheMinerSacrifice(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerPeteTheCannibal:
     		dropWorkerPeteTheCannibal(cont.pendingContinuationCell(),replay);
     		break;
     	case DoRebeccaThePeddler:
     		doRebeccaThePeddler(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerFlavioTheMerchant:
     		dropWorkerFlavioTheMerchant(cont.pendingContinuationCell(),replay);
     		break;
     	case DoFlavioTheMerchant:
     		doFlavioTheMerchant(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case DoFlavioTheMerchantGainCard:
     		doFlavioTheMerchantGainCard(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerJackoTheArchivist:
     		dropWorkerJackoTheArchivist(cont.pendingContinuationCell(),replay);
     		break;
     	case DoAmandaTheBroker:
     		doAmandaTheBroker(cont.pendingContinuationCell(),replay,cont.activeRecruit,cont.pendingUnpaidContinuation);
     		break;
     	case ReRollBumpedWorker:
     		reRollBumpedWorker(cont.pendingContinuationCell(),replay);
     		break;
     	case DoReitzTheArcheologist:
     		doReitzTheArcheologist(cont.pendingContinuationCell(),replay);
     		break;
     	case DoJackoTheArchivist:
     		doJackoTheArchivist(cont.pendingContinuationCell(),replay);
     		break;
     	case DoJackoTheArchivistStar:
     		doJackoTheArchivistStar(cont.pendingContinuationCell(),replay);
     		break;
     	case DoLauraThePhilanthropist:
     		doLauraThePhilanthropist(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerLauraThePhilanthropist:
     		dropWorkerLauraThePhilanthropist(cont.pendingContinuationCell(),replay);
     		break;
     	case DoDaveTheDemolitionist:
    		doDaveTheDemolitionist(cont.pendingContinuationCell(),cont.pendingContinuationCell.placementBenefit,cont.activeRecruit,replay);
     		break;
     	case DropWorkerCollectBenefitAfterRecruits:
     		dropWorkerCollectBenefitAfterRecruits(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerJonathanTheArtistFarmer:	// buggy version that activates on the farm
     		dropWorkerJonathanTheArtistFarmer(cont.pendingContinuationCell(),replay);
     		break;
     	case DoJonathanTheArtistFarmer:
     		doJonathanTheArtistFarmer(cont.pendingContinuationCell(),replay);
     		break;

     	case DoJonathanTheArtist:
     		doJonathanTheArtist(cont.pendingContinuationCell(),replay);
     		break;
     	case DoYordyTheDemotivator_V2:
     		doYordyTheDemotivator_V2(players[cont.pendingEPlayer],replay);
     		break;
     	case ReRollYordyCheck_V2:
     		reRollYordyCheck_V2(players[cont.pendingEPlayer],replay);
     		break;
     	case DropWorkerScarbyTheHarvester:
    		dropWorkerScarbyTheHarvester(cont.pendingContinuationCell(),replay);
     		break;
     	case ReRoll:
     		reRoll(players[cont.pendingEPlayer],replay);
     		break;
     	case MoraleCheck:
     		doMoraleCheck(players[cont.pendingEPlayer],cont.pendingContinuationCell,replay);	// pendingContinuationCell may be null, it's ok 
     		break;
     	case DoGeekTheOracle:
     		doGeekTheOracle(players[cont.pendingEPlayer],replay,cont.activeRecruit);
     		break;
     	case DoJonathanTheGambler:
     		// this one can have a null continuation
     		doJonathanTheGambler(players[cont.pendingEPlayer],cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case ReRollNormalPayment:
     		reRollNormalPayment(players[cont.pendingEPlayer],replay);
     		break;
     	case DoEsmeTheFireman:
     		doEsmeTheFireman(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case DoEsmeTheFiremanPaid:
     		doEsmeTheFiremanPaid(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case DoGidgitTheHypnotist:
     		doGidgitTheHypnotist(players[cont.pendingEPlayer],replay);
     		break;
      	case ReRollWithoutPayment:
     		reRollWithoutPayment(players[cont.pendingEPlayer],replay);
     		break;
     	case ReRollWithPayment:
     		reRollWithPayment(players[cont.pendingEPlayer],replay);
     		break;
     	case ProceedWithTheGame:
     		proceedWithTheGame(replay);
     		break;
     	case DropWorkerWithoutPayment:
     		dropWorkerWithoutPayment(cont.pendingContinuationCell,replay);
     		break;
     	case DropWorkerBump:
     		dropWorkerBump(cont.pendingContinuationCell,replay);
     		break;
     		
     	case DropWorkerAfterBenefit:
     		dropWorkerAfterBenefit(players[cont.pendingEPlayer],cont.pendingContinuationCell,replay);
     		break;
     	case DoMaximeTheAmbassador:		// lose morale and gain a card
     		doMaximeTheAmbassador(cont.pendingContinuationCell(),replay);
     		break;
     	case DoMaximeTheAmbassadorGainCard:
     		doMaximeTheAmbassadorGainCard(cont.pendingContinuationCell(),replay);
     		break;
     	case DoBrettTheLockPicker:
     		doBrettTheLockPicker(cont.pendingContinuationCell,replay,cont.activeRecruit,cont.pendingUnpaidContinuation);
     		break;
     	case DoBenTheLudologist:
     		doBenTheLudologist(players[cont.pendingEPlayer],cont.pendingContinuationCell,replay);	// with null coninuation is ok
     		break;
     	case DoKyleTheScavenger:
     		doKyleTheScavenger(cont.pendingContinuationCell,replay);
     		break;
     		
     	case DropWorkerOpenMarkets:	
     		dropWorkerOpenMarkets(cont.pendingContinuationCell(),replay);
     		break;
     		
     	// ethical dilemma
     	case JoinTheEstablishment:
     		setState(EuphoriaState.ConfirmJoinTheEstablishment);
     		stepNumber++;
     		doDone(null,replay);
      		break;
    	case FightTheOpressor:
     		setState(EuphoriaState.ConfirmFightTheOpressor);
     		stepNumber++;
     		doDone(null,replay);
     		break;
     		
    	case DoZongTheAstronomer_V2:
    		doZongTheAstronomer_V2(cont.pendingContinuationCell,replay);
    		break;

    	// IIB continuations
    	case DoSamuelTheZapper:
    		// retrieve workers, gain morale
    		doSamuelTheZapper(replay);
    		break;
     	case DoKhaleefTheBruiser:
    		doKhaleefTheBruiser(cont.pendingContinuationCell,replay);
    		break;
     	case BumpWorkerCheckKhaleef:
     		bumpWorkerCheckKhaleef(cont.pendingContinuationCell,replay);
     		break;
    	case DoMilosTheBrainwasher:
    		doMilosTheBrainwasher(players[cont.pendingEPlayer],replay,cont.pendingContinuationCell,(WorkerChip)cont.extraChip);
    		break;
    	case ReRollWorkersAfterJeroen:
    		rerollWorkersAfterJeroen(players[cont.pendingEPlayer],replay,(WorkerChip)cont.extraChip);
    		break;
    	case ReRollWorkersAfterChristine:
    		rerollWorkersAfterChristine(players[cont.pendingEPlayer],replay,(WorkerChip)cont.extraChip);   		
    		break;
    	case ReRollWorkersAfterMilos:
    		reRollWorkersAfterMilos(players[cont.pendingEPlayer],replay,(WorkerChip)cont.extraChip,false);
    		break;
     	case PayForCard:
    		dropWorkerPayCard(players[cont.pendingEPlayer],cont.pendingContinuationCell(),replay);
    		break;
    		
     	case ReRollWorkersAfterKeb:
     		reRollWorkersAfterKeb(players[cont.pendingEPlayer],replay,
     									false,
     									cont.pendingCost,
     									cont.extraInt);
    		break;
     	case ReRollWorkersAfterXyon:
     		reRollWorkersAfterXyon(players[cont.pendingEPlayer],replay,
     									false,
     									cont.pendingCost,
     									cont.extraInt);
    		break;
     	case ReRollWorkersAfterCheck:
     		reRollWorkersAfterCheck(players[cont.pendingEPlayer],replay,
     									cont.pendingCost,
     									cont.extraInt);
    		break;
     	case DoXyonTheBrainSurgeon:
     		doXyonTheBrainSurgeon(players[cont.pendingEPlayer],replay,
     				cont.pendingCost,
						cont.extraInt);
     		break;
     		
    	case DoDustyTheEnforcer:
     		doDustyTheEnforcer(players[cont.pendingEPlayer],cont.pendingContinuationCell,replay,
     				(WorkerChip)cont.extraChip,
     					false,
						cont.pendingCost,
						cont.extraInt);
     		break;
     	case DontDustyTheEnforcer:
     		dontDustyTheEnforcer(players[cont.pendingEPlayer],replay,
     				cont.pendingCost,
						cont.extraInt);
    		break;

     	case DoZaraTheSolipsist:
     		doZaraTheSolipsist(players[cont.pendingPlayer],cont.pendingContinuationCell(),cont.pendingBenefit,replay);
     		break;
     	case DoPmaiTheNurse:
     		doPmaiTheNurse(players[cont.pendingPlayer],cont.pendingContinuationCell(),cont.pendingBenefit,replay);
     		break;
     	case DoLieveTheBriber:
     		doLieveTheBriber(players[cont.pendingPlayer],cont.pendingContinuationCell(),cont.pendingBenefit,replay);  		
     		break;
     	case DropWorkerCollectBenefit:
     		dropWorkerCollectBenefit(players[cont.pendingPlayer],cont.pendingContinuationCell(),cont.pendingBenefit,replay);
     		break;
     	case DoPamhidzai:
      		doPamhidzai(players[cont.pendingPlayer],cont.pendingContinuationCell(),replay);
     		break;
     	case DontPamhidzai:
      		dontPamhidzai(replay);
     		break;
     	case DoBrendaTheKnowledgeBringer:
     		doBrendaTheKnowledgeBringer(players[cont.pendingPlayer],replay);
     		break;
     	case CollectBenefitAfterArtifacts:
     		collectBenefitAfterArtifacts(players[cont.pendingEPlayer],cont.pendingContinuationCell(),replay);
     		break;
     	case DoShaheenaTheDigger:
     		doShaheenaTheDigger(players[cont.pendingEPlayer],replay);
     		break;
     	case DontShaheenaTheDigger:
     		dontShaheenaTheDigger(players[cont.pendingEPlayer],replay);
     		break;
     	case AfterShaheenaTheDigger:
     		afterShaheenaTheDigger(players[cont.pendingEPlayer],replay);
     		break;
     	case DoDarrenTheRepeater:
     		doDarrenTheRepeater(players[cont.pendingEPlayer],replay);
     		break;
     	case ContinueDarrenTheRepeater:
     		continueDarrenTheRepeater(players[cont.pendingEPlayer],replay);
     		break;
     	case DontDarrenTheRepeater:
     		dontDarrenTheRepeater(players[cont.pendingEPlayer],replay);
     		break;
     	case DoJedidiahTheInciter:
     		doJedidiahTheInciter(players[cont.pendingEPlayer],cont.pendingContinuationCell(),replay);
     		break;
     	case DropAfterPedroTheCollector:
     		dropAfterPedroTheCollector(players[cont.pendingEPlayer],cont.pendingContinuationCell(),replay);
     		break;
     		
     	case DoSpirosTheModelCitizen:
     		{	
     		EPlayer p = players[cont.pendingEPlayer];
     		proceedGameStep = ProceedStep.Start;
     		usingDoubles = false;
     		doublesElgible = null;
     		setState(EuphoriaState.Place);
     		p.setTFlag(TFlag.UsedSpirosTheModelCitizen);
     		p.payCostOrElse(Cost.Morale,replay);
     		doMoraleCheck(p,replay,null,Function.Return);
     		}
     		break;
     	case DoLarsTheBallooneer:
     		
     		//p1("use lars the ballooner");
     		usingDoubles = false;
     		doublesElgible = null;
     		lastDroppedWorker = cont.pendingContinuationCell();
     		players[cont.pendingEPlayer].setTFlag(TFlag.UsedLarsTheBallooneer);
     		proceedGameStep = ProceedStep.Start;
     		setState(EuphoriaState.ReUseWorker);
     	    break;
     	    
     	case DontSpirosTheModelCitizen:
     		proceedGameStep = ProceedStep.Step4AfterSpiros;
    		proceedWithTheGame(replay);
     		break;
    		
     	case DontLarsTheBallooneer:
     		proceedGameStep = ProceedStep.Step4AfterLars;
     		proceedWithTheGame(replay);
     		break;
     		
     	case DoTaedTheBrickTrader:
	 		{
	 		EPlayer p = players[cont.pendingEPlayer];
	 		int n = p.taedAuthorityHeight -p.authority.height();
	 		Cost cost = Cost.Clay;
	 		Benefit bene = Benefit.ResourceAndWater;
	 		n = Math.min(n,p.clay.height());
	 		if(n-1<MultiClay.length) { cost = MultiClay[n-1]; bene = MultiResourceAndWater[n-1]; }
	 		p.taedAuthorityHeight -= n;
	 		p.payCostOrElse(cost,replay);
	 		Benefit residual = p.collectBenefit(bene,replay);
	 		// residual will always be a resource or 2 resources
	 		//p1("doing taed "+p.taedAuthorityHeight+" "+p.authority.height()+" "+n);
	 		setContinuation(new Continuation(residual,Function.ProceedWithTheGame,p));
	 		}
	 		break;
     	case DontTaedTheBrickTrader:		
	 		{
	 		EPlayer p = players[cont.pendingEPlayer];
	 		//p1("dont terri");
	 		p.taedAuthorityHeight = p.authority.height();
	 		proceedWithTheGame(replay);
	 		}
	 		break;
     	case DoTerriTheBlissTrader:
     		{
     		EPlayer p = players[cont.pendingEPlayer];
     		p.payCostOrElse(Cost.Bliss,replay);
     		p.terriAuthorityHeight--;
     		//p1("do terri");
     		setContinuation(new Continuation(Benefit.Commodity,Function.ProceedWithTheGame,p));
     		}
     		break;
     		
     	case DontTerriTheBlissTrader:	     		
	 		{
	 		EPlayer p = players[cont.pendingEPlayer];
	 		//p1("dont terri");
	 		p.terriAuthorityHeight = p.authority.height();	// don't ask again until more stars are placed
	 		proceedWithTheGame(replay);
	 		}
	    	break;
	    	
     	case DoGeorgeTheLazyCraftsman:
     		proceedGameStep = ProceedStep.Step2AfterGeorge;
     		proceedWithTheGame(replay);
     		break;
     		
     	case DoJadwigaTheSleepDeprivator:
     		{
     			EPlayer p = players[cont.pendingEPlayer];
     			p.payCostOrElse(Cost.Knowledgex2,replay);
        		p.setTFlag(TFlag.UsedJadwigaTheSleepDeprivator);
        		proceedGameStep = ProceedStep.Start;
        		doublesElgible = null;
        		doublesCount = 0;
        		usingDoubles = false;
         		setState(EuphoriaState.Place);
      		}
     		break;
     	case DontJadwigaTheSleepDeprivator:
     		proceedGameStep = ProceedStep.Step4AfterJadwiga;
     		proceedWithTheGame(replay);
     		break;
     	case DoJuliaTheAcolyte:
    		reRollWorkersAfterJulia(players[cont.pendingEPlayer],replay,(WorkerChip)cont.extraChip,
    				cont.pendingSecondContinuation);   
    		break;
     	case DontJuliaTheAcolyte:
     		reRollWorkersAfterJulia(players[cont.pendingEPlayer],replay,null,cont.pendingSecondContinuation);   
     		break;
     		
     	case DoHajoon:
     		{EPlayer p = players[cont.pendingEPlayer];
     		 p.payCostOrElse(Cost.Gold,replay);
     		 setContinuation(new Continuation(Benefit.FreeArtifact,Function.AfterHajoon,p));
     		 break;
     		}
     	case AfterHajoon:
     		{
     		EPlayer p = players[cont.pendingEPlayer];
      		if(!doMoraleCheck(p,replay,null,Function.DontHajoon)) 
      			{ //p1("hajoon needs morale");
      			break; }
      		//p1("hajoon morale ok");
     		}
			//$FALL-THROUGH$
		case DontHajoon:
     		proceedGameStep = ProceedStep.Step4AfterHajoon;
     		proceedWithTheGame(replay);
     		break;
		case DoHighGeneralBaron:
		{
			EPlayer p = players[cont.pendingEPlayer];
			doHighGeneralBaron(p,replay,cont.extraInt==0?false:true);
			return;
		}
		case DontHighGeneralBaron:
		{
			EPlayer p = players[cont.pendingEPlayer];
			dontHighGeneralBaron(p,replay,cont.extraInt==0?false:true);
			return;
		}
		case DropWorkerPay2:
		{
			EPlayer p = players[cont.pendingEPlayer];
			dropWorkerPay2(p,cont.pendingContinuationCell(),replay);
			return;			
		}
		case FinishChagaTheGamer:
			{
			EPlayer p = players[cont.pendingEPlayer];
			finishChagaTheGamer(p);
			return;			
			}
		
     	default: throw Error("Not expecting continuation %s",cc);
     	}
     }
 
    private void doSamuelTheZapper(replayMode replay)
    {	assert_isIIB();
    	EPlayer p = players[whoseTurn];
    	p.payCostOrElse(Cost.Energy, replay);		// doesn't depend on where placed
    	// record the height of the retrieval stack..  It could be non-empty
    	// if the placement was opening a market or recruiting a new worker or a bump
    	p.samuelTheZapperLevel = p.newWorkers.height();
    	p.setTFlag(TFlag.UsingSamuelTheZapper);
    	setState(EuphoriaState.RetrieveCommodityWorkers);
    }
    void collectResourceNext()
    {
		Continuation cont = new Continuation(Benefit.Resource,Function.Return,null);
		setContinuation(cont);
    }
    private void doUnpaidContinuation(replayMode replay)
    {	do {
    	Continuation cc = continuationStack.pop();
    	Assert(cc.pendingUnpaidContinuation!=null,"unpaid continuation is specified");
    	executeContinuation(cc,cc.pendingUnpaidContinuation,cc.pendingPlayer,replay);
    	} while(board_state==EuphoriaState.ExtendedBenefit);
    }
    private void finishContinuation(replayMode replay)
    {
    	Continuation cont = continuationStack.pop();
    	setState(cont.exitState);
    }
    private void doContinuation(replayMode replay)
    {	
    	do { 
    		Continuation cont = continuationStack.pop();
    		executeContinuation(cont,cont.pendingContinuation,cont.pendingPlayer,replay);
    	}  while(board_state==EuphoriaState.ExtendedBenefit);
    }
	public String getContinuationStack()
	{	String path = "";
		for(int i=0,lim=continuationStack.size(); i<lim; i++)
		{
			path += continuationStack.elementAt(i)+"\n";
		}
		return(path);
	}
	
    Benefit pendingBenefit()
    {
    	Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.pendingBenefit);
    }

    RecruitChip activeRecruit()
    {
    	Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.activeRecruit);  	
    }
    Cost originalPendingCost()
    {
    	Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.originalPendingCost);
    }
    Cost pendingCost()
    {
    	Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.pendingCost);
    }
	public boolean MASTER_SIMULTANEOUS_PLAY = true;
	//
	// if simultaneous play starts out true, it becomes false when the game record is 
	// canonicalized to non-ephemeral opcodes.  After that point, the game behaves as
	// if it had never had simultaneous play.
	//
	public boolean REINIT_SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
	public boolean SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
	public void setSimultaneousPlay(boolean val)
	{
		MASTER_SIMULTANEOUS_PLAY = REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = val; 
	}
	public int activePlayer = -1;
	EPlayer players[] = new EPlayer[MAX_PLAYERS];
	public EPlayer getCurrentPlayer() { return(players[whoseTurn]); }
	public EPlayer getPlayer(int in) { return(in<players.length ? players[in] : null); }
	public void normalizePlayerViews() {  for(EPlayer p : players)
		{ p.hiddenView = p.view = p.pendingView = EPlayer.PlayerView.Normal; }}
	public StringStack finalPath=new StringStack();
	//
	// finalpath is a debugging aid.  The possible paths through the "done" after placing
	// a worker are insane, so we record waypoints and assert that each one should only be
	// reached once.
	//
	public void addToFinalPath(String str)
	{	if(G.debug())
		{
		if(finalPath.contains(str))
		{	String msg = "got to "+str+" a second time";
			Error(msg);
		}
		//G.print("P "+str);
		finalPath.push(str);
		}
	}
	public String getFinalPath()
	{	String path = "";
		for(int i=0,lim=finalPath.size(); i<lim; i++)
		{
			path += finalPath.elementAt(i)+"\n";
		}
		return(path);
	}
	public int CELLSIZE ;
	EuphoriaPlay robot = null;
	private boolean robotBoard = false;
	private EuphoriaCell workerCells[] = null;
	int currentPlayerInTurnOrder = 0;
	WorkerChip bumpedWorker = null;
	WorkerChip bumpingWorker = null;
	boolean openedAMarket = false;		// ephemeral state
	boolean shuffledADeck = false;
	boolean openedAMarketLastTurn = false;
	boolean shuffledADeckLastTurn = false;
	
    boolean revealedNewInformation = false;		// true when the user has revealed new information, so Undo should be inhibited

    WorkerChip selectedDieRoll = null;	// for AmandaTheBroker
    EuphoriaChip activeRecruit = null;
    int stepNumber = 0;
    int rollNumber = 0;
	public void setRobotBoard() 
		{ robotBoard = true; }
	
	public String gameType() { return(variation.name()+" "+revision+" "+players.length+" "+randomKey); }
	
	EPlayer getPlayer(Colors c) 
		{ 
		for(EPlayer p : players) { if(p.color==c) { return(p); }}
		throw Error("No player uses %s",c);
		}
	void recycleArtifact(EuphoriaChip a)
	{	if(LOG_ARTIFACTS) { G.print(moveNumber," recycle ",unusedArtifacts.height()," ",a); }
		usedArtifacts.addChip(a);
	}
	EuphoriaChip getArtifact()
	{	

		if(LOG_ARTIFACTS) { G.print(moveNumber," ea ",unusedArtifacts.height()," ",usedArtifacts.height()," ",usedArtifacts.topChip()); }
		if(unusedArtifacts.height()==0)
		{
			while(usedArtifacts.height()>0) { unusedArtifacts.addChip(usedArtifacts.removeTop()); }
			shuffledADeck = true;
			unusedArtifacts.shuffle(newRandomizer(0x35646353));
		}
		if(unusedArtifacts.height()>0) { return(unusedArtifacts.removeTop()); }
		return(null);	// this should never happen but...
	}

	// bounds of the commodity site, indexed by allegiance
	double commodityBounds[][] = { euphorianGeneratorBounds,subterranAquiferBounds,wasteLanderFarmBounds,icariteCloudMineBounds};
	EuphoriaCell tunnelBenefitMarker[] = { euphorianTunnelMouth, subterranTunnelMouth, wastelanderTunnelMouth, icariteNimbusLoft};
	EuphoriaCell tunnelEnds[] = { euphorianTunnelEnd, subterranTunnelEnd, wastelanderTunnelEnd};
	//markets indexed by Allegiance
	
	EuphoriaCell marketA[] = {markets[Allegiance.Euphorian.ordinal()*2],
							  markets[Allegiance.Subterran.ordinal()*2],
							  markets[Allegiance.Wastelander.ordinal()*2],};
	EuphoriaCell marketB[] = {markets[Allegiance.Euphorian.ordinal()*2+1],
				markets[Allegiance.Subterran.ordinal()*2+1],
			  markets[Allegiance.Wastelander.ordinal()*2+1],
			  };
	
	EuphoriaCell buildMarkets[][] = 
		  { euphorianBuildMarketA,euphorianBuildMarketB,		// build cells in the same order as markets
			subterranBuildMarketA,subterranBuildMarketB,
			wastelanderBuildMarketA,wastelanderBuildMarketB};
	EuphoriaCell useMarkets[] = 
		{
			euphorianMarketA,euphorianMarketB,
			subterranMarketA,subterranMarketB,
			wastelanderMarketA,wastelanderMarketB,
		};
	
	// producer arrays indexed by allegiance
	EuphoriaCell producerArray[][] = 
		{
			euphorianGenerator,
			subterranAquifer,
			wastelanderFarm,
			icariteCloudMine
		};
	// zone coordinates for the dialogs
	double tightMarketZones[][] = { euphorianMarketZone, subterranMarketZone, wastelanderMarketZone};
	double generalMarketZones[][] = { euphorianMarketArea, subterranMarketArea, wastelanderMarketArea};
	EuphoriaCell TunnelEnds[] = { euphorianTunnelEnd, subterranTunnelEnd, wastelanderTunnelEnd}; 

    
    private EuphoriaCell tunnelSteps[][] = {euphorianTunnelSteps,subterranTunnelSteps,wastelanderTunnelSteps,
    		};
	/**
	 * tunnels, per allegiance.  We ignore that there is no icarite tunnel
	 */
    private int tunnelPosition[] = new int[Allegiance.values().length];
  
    public int getTunnelPosition(Allegiance faction)
    {	return(tunnelPosition[faction.ordinal()]);
    }
    public void setTunnelPosition(Allegiance faction,int val)
    {	tunnelPosition[faction.ordinal()] = val;
    }
	
    public void incrementTunnelPosition(Allegiance faction)
    {	int ord = faction.ordinal();
    	// assume all the tunnels are the same length
    	if(tunnelPosition[ord]<(euphorianTunnelSteps.length-1)) { tunnelPosition[ord]++; }
    	openTunnelAtEnd(faction);
    }
    public void openTunnelAtEnd(Allegiance faction)
    {	int ord = faction.ordinal();
    	EuphoriaCell end = TunnelEnds[ord];
    	if(tunnelPosition[ord]==(euphorianTunnelSteps.length-1))
    	{
    		end.placementCost = TunnelAllegiance[ord];
    	}
    	else { end.placementCost = Cost.TunnelOpen; }
    	
    }
    public void decrementTunnelPosition(Allegiance faction)
    {	int ord = faction.ordinal();
    	// assume all the tunnels are the same length
    	if(tunnelPosition[ord]>0) { tunnelPosition[ord]--; }
    }
   
    /**
     * allegiance track for each faction
     */
    private int allegiance[] = new int[Allegiance.values().length];
    public int getAllegianceValue(Allegiance faction)
    {	return(allegiance[faction.ordinal()]);
    }
    public void setAllegianceValue(Allegiance faction,int val)
    {	allegiance[faction.ordinal()]=val;;
    }
    private void awardAllegianceStars(Allegiance faction,replayMode replay)
    {
    	for(EPlayer p : players) { p.awardAllegianceStars(faction,replay); }
    }
    boolean canIncrementAllegiance(Allegiance faction)
    {	int ord = faction.ordinal();
    	return (allegiance[ord] < AllegianceSteps-1);
    }
    // return true if actually incremented
    boolean incrementAllegiance(Allegiance faction,replayMode replay)
    {	int ord = faction.ordinal();
    	if(canIncrementAllegiance(faction)) 
    		{ 
    		allegiance[ord]++;
    		if(allegiance[ord]==(AllegianceSteps-1)) { awardAllegianceStars(faction,replay); }
    		return(true); 
    		}
    	return(false);
    }
    // return true if actually incremented
    boolean decrementAllegiance(Allegiance faction)
    {	int ord = faction.ordinal();
    	if(allegiance[ord]>0) { allegiance[ord]--; return(true); }
    	return(false);
    }
    

   
	EuphoriaState board_state = EuphoriaState.Puzzle;	
	private EuphoriaState unresign = null;			// remembers the orignal state when "resign" is hit
	boolean hasReducedRecruits = false;		// true if we've reduced the recruits at startup
	boolean normalStartSeen = false;			// make sure exactly one "normalstart" get saved in the game record
	WorkerChip doublesElgible = null;			// a die if we placed part of a double
	boolean usingDoubles = false;
	int doublesCount = 0;
	
	enum ProceedStep { Start, Step1, 
		
		Step2,Step2AfterGeorge, 
		Step3, 
		// step4 is where a second worker placement can happen
		Step4,  
		Step4AfterTerri, Step4AfterLars,  Step4AfterJadwiga, Step4AfterSpiros, 
		Step4AfterShaheena, Step4AfterHajoon,
		StepNext,
		Step5};
	
	long steps = 0;
	private ProceedStep proceedGameStep = ProceedStep.Start;				// sub-state of the turn windup.
	private void clearSteps() { steps = 0; }
	private void registerStep(ProceedStep next)
    {	ProceedStep v[] = ProceedStep.values();
    	int idx = 0;
    	int last = v.length;
    	int bit = 1;
    	if(next==ProceedStep.Start) { steps = 0;}
    	while(v[idx]!=next) 
    		{ Assert((steps&bit)!=0,"Step #1 is missing",v[idx]); 
    		  idx++;
    		  bit = bit<<1;
    		}
    	steps |= bit;	// allowed to remain on the same step
    	idx++;
    	bit = bit<<1;
    	
    	while(idx<last) 
    		{ Assert((steps&bit)==0,"reverted to %s after %s",next,v[idx]); 
    		  idx++;
    		  bit = bit<<1;
    		}
    	proceedGameStep = next;
    }
    
	public EuphoriaState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(EuphoriaState st) 
	{ 	unresign = (st==EuphoriaState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw Error("not expected"); };	
	CellStack animationStack = new CellStack();
	StringStack gameEvents = new StringStack();
	
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public EuphoriaChip pickedObject = null;
    public EuphoriaChip lastPicked = null;

    CellStack pickedSourceStack = new CellStack();
    CellStack droppedDestStack = new CellStack();
    EuphoriaCell lastDroppedWorker = null;
    private StateStack droppedStateStack = new StateStack();
    private StateStack pickedStateStack = new StateStack();
    private EuphoriaCell lastUndrop = null;
    private IStack pickedHeightStack = new IStack();
    private ColorsStack reRollPlayers = new ColorsStack();	// players pending reroll
    public boolean canResolveDilemma(EPlayer p) { return(p.canResolveDilemma()); }
    public EuphoriaCell getSource() { return(pickedSourceStack.top()); }
    public EuphoriaCell getDest() { return(droppedDestStack.top()); }
    public boolean isSource(EuphoriaCell c)  {	return(c==pickedSourceStack.top());   }
    public boolean isDest(EuphoriaCell d) { return(d==droppedDestStack.top()); }
    private int nOpenMarkets = 0;
   
    public EuphoriaChip lastDroppedObject = null;	// for image adjustment logic
    private boolean reUsingWorker = false;
    private boolean hasPlacedWorker = false;
    
    // count the workers on one of the producer arrays (for KyleTheScavenger)
    int countWorkers(EuphoriaCell cc[])
    {
    	int n=0;
    	for(EuphoriaCell c : cc) { n += c.height(); }
    	return(n);
    }
    EuphoriaCell []getProducerArray(Allegiance aa)
    {
    	return(producerArray[aa.ordinal()]);
    }
    EuphoriaCell []getBuilderArray(EuphoriaId id)
    {
    	switch(id)
    	{
    	case EuphorianBuildMarketA:	return(euphorianBuildMarketA);
    	case EuphorianBuildMarketB: return(euphorianBuildMarketB);
    	case WastelanderBuildMarketA:	return(wastelanderBuildMarketA);
    	case WastelanderBuildMarketB: return(wastelanderBuildMarketB);
    	case SubterranBuildMarketA:	return(subterranBuildMarketA);
    	case SubterranBuildMarketB: return(subterranBuildMarketB);
    	
    	default: throw Error("not expecting %s",id);
    	}
    }
   // 
    // knowledge on the primary commodities, determine what you get
    //
    private int totalKnowledgeInArray(EuphoriaCell cells[])
    {	int sum=0;
    	for(EuphoriaCell c : cells) {	sum += c.totalKnowledge(); } 
    	return(sum);
    }
    public int totalKnowledgeOnGenerator()
    {
    	return(totalKnowledgeInArray(euphorianGenerator));
    }
    public int totalKnowledgeOnAquifer()
    {	return(totalKnowledgeInArray(subterranAquifer));
    }
    
    public int totalKnowledgeOnFarm()
    {
    	return(totalKnowledgeInArray(wastelanderFarm));
    }
    public int totalKnowledgeOnCloudMine()
    {
    	return(totalKnowledgeInArray(icariteCloudMine));
    }
    
    private boolean hasOpponentWorker(EuphoriaCell a,EPlayer p)
    {
    	EuphoriaCell ar[] = getProducerArray(a.allegiance);
    	Colors targetColor = p.color;
    	for(EuphoriaCell c : ar)
    	{
    		EuphoriaChip ch = c.topChip();
    		if(ch!=null)
    		{
    			if(ch.color!=targetColor) { return(true); }
    		}
    	}
    	return false;
    }
    
    InternationalStrings s = null;
    public EuphoriaBoard(String init,long key,int np,int[]colormap,int rev) // default constructor
    {	s = G.getTranslations();
    	setColorMap(colormap);
        doInit(init,key,np,rev); // do the initialization 
        int n = 0;
        for(EuphoriaCell c = allCells; c!=null; c=c.next) 
        {
        	if(c.rackLocation().isWorkerCell()) { n++; }
        }
        workerCells = new EuphoriaCell[n];
        n=0;
        for(EuphoriaCell c = allCells; c!=null; c=c.next) 
        {
        	if(c.rackLocation().isWorkerCell()) { workerCells[n++]=c; }
        }   

        artifactBazaar[0].initialPlacementCost = Cost.CommodityX2;
        artifactBazaar[1].initialPlacementCost = Cost.Commodity;
        artifactBazaar[2].initialPlacementCost = Cost.Commodity;
        artifactBazaar[3].initialPlacementCost = Cost.Free;
        

    }
    public void doInit(String gtype,long rv)
    {	boolean sp = REINIT_SIMULTANEOUS_PLAY;
    	doInit(gtype,rv,players.length,revision);
    	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = sp;
    }
    public EuphoriaBoard cloneBoard() 
	{ EuphoriaBoard dup = new EuphoriaBoard(gametype,randomKey,players.length,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((EuphoriaBoard)b); }

    public void sameboard(BoardProtocol f) 
    	{ sameboard((EuphoriaBoard)f); }

    private RecruitChip[] getMasterRecruitList()
    {
    	switch(variation)
    	{
    	default: throw Error("Not expecting variation ",variation);
    	case Euphoria:	return RecruitChip.allRecruits;
    	case Euphoria2: return RecruitChip.V2Recruits;
    	case Euphoria3T:
    	case Euphoria3: return RecruitChip.IIBRecruits;
    	}
    }
    public RecruitChip getRandomRecruit(Random r)
    {	RecruitChip masterlist[] = getMasterRecruitList();  
    	int n = Random.nextInt(r,masterlist.length-RecruitChip.FIRST_RECRUIT);
    	return(masterlist[RecruitChip.FIRST_RECRUIT+n]);
    }
    public void getAllRecruits(EuphoriaCell c)
    { 	RecruitChip masterlist[] = getMasterRecruitList();  
    	c.reInit();
    	for(int i=RecruitChip.FIRST_RECRUIT;i<masterlist.length;i++)
    	{	c.addChip(masterlist[i]);
    	}
    }
    static int startingPosition[] = {3,3,3, 2,1, 0,0};
    private int startingTunnelPosition()
    {
    	return(isIIB() ? startingPosition[players_in_game] : 0);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int play,int rev)
    {	clearSteps();
    	adjustRevision(rev);
    	allCells.setDigestChain(new Random(0x6235366));
    	win = new boolean[play];
    	players_in_game = play;
     	Random gameRandom = new Random(key);
    	SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
    	
		Variation v = Variation.find(gtype);
		Assert(v!=null,WrongInitError,gtype);
		variation = v;
		gametype = gtype;
    	//
    	// cells which are for display only are linked from DisplayCells through .next 
    	// this links the end of the displayCells chain with the beginning of the allCells chain
    	// the displayCells part of the chain doesn't participate in the Digest/Clone/SameBoard convention
    	lastDisplayCell.next = allCells;
    	// initialize the board cells, including the display only cells
    	for(EuphoriaCell c=displayCells; c!=null; c=c.next)	
    		{ c.reInit(); 
    		  c.placementCost = c.initialPlacementCost;	
    		  c.placementBenefit = c.initialPlacementBenefit;
    		}
    	
    	// create the market deck and shuffle it.
    	EuphoriaChip marketList[] = variation==Variation.Euphoria3 ? MarketChip.IIBMarkets : MarketChip.V12Markets;
    	for(int i=0;i<marketList.length;i++)
    	{	unusedMarkets.addChip(marketList[i]);
    	}	
    	unusedMarkets.shuffle(gameRandom);
    	
    	
    	// create the recruit deck and shuffle it
    	getAllRecruits(unusedRecruits);
     	unusedRecruits.shuffle(gameRandom);

     	//unusedRecruits.addChip(unusedRecruits.removeChip(RecruitChip.DavaaTheShredder));
     	//unusedRecruits.addChip(unusedRecruits.removeChip(RecruitChip.JeroenTheHoarder));
     	
     	// create the ethical dilemma deck and shuffle it
     	for(int i=1;i<DilemmaChip.allDilemmas.length;i++)
    	{	unusedDilemmas.addChip(DilemmaChip.allDilemmas[i]);
    	}
    	unusedDilemmas.shuffle(gameRandom);
    	
    	// create an artifact deck and shuffle it
     	for(int i=ArtifactChip.FIRST_ARTIFACT;i<ArtifactChip.allArtifacts.length;i++)
    	{	for(int j=0;j<ArtifactChip.nCopies;j++)
    		{
    		unusedArtifacts.addChip(ArtifactChip.allArtifacts[i]);
    		}
    	}
    	unusedArtifacts.shuffle(gameRandom);
       	usedArtifacts.reInit();
       	reInit(	artifactBazaar );
       	if(variation.isIIB())
       	{
       		for(int i=0;i<artifactBazaar.length;i++) { artifactBazaar[i].addChip(unusedArtifacts.removeTop()); }
       	}
       	
       	// add some display chips to the pool
       	adjustResources();
       	
        if(variation.isIIB()) 
        	{ genericSource.addChip(EuphoriaChip.Nocard);
        	  trash.addChip(EuphoriaChip.Trash);
        	}
        
    	// initialize the players
    	players = new EPlayer[play];
    	int rv[] = new int[play];
    	for(int i=0;i<play;i++) { rv[i]=i; }	// these will act as the final tiebreaker
    	gameRandom.shuffle(rv);
    	int map[] = getColorMap();
    	
    	for(int i=0;i<players.length;i++) 
		{ EPlayer p = players[i]=new EPlayer(this,i,Colors.find(map[i]),rv[i]);
		  p.doInit(unusedRecruits,unusedDilemmas);
		  unusedWorkers[i].addChip(WorkerChip.getWorker(p.color,1));
		}
  
    	AR.setValue(allegiance,0);
    	AR.setValue(tunnelPosition,startingTunnelPosition());
    	// place the markets.
    	for(int i=0;i<markets.length;i++)
    	{	EuphoriaCell m = markets[i];
    		m.addChip(unusedMarkets.removeTop());
    		m.addChip(MarketChip.CardBack);		// cover it with the back
    	}
	
      	
    	for(int i=players.length;i<MAX_PLAYERS;i++)
    	{
    		for(Allegiance a : Allegiance.values())
    		{	if(a!=Allegiance.Factionless)
    			{
    			// it doesn't matter what player is supplied as this is setup
    			// and there are no market penalties
    			EuphoriaCell c = getAvailableAuthorityCell(players[0],a);
    			placeAuthorityToken(c,EuphoriaChip.AuthorityBlocker);
    			}
    		}
    	}
		setState(EuphoriaState.Puzzle);

    
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    droppedStateStack.clear();
	    pickedSourceStack.clear();
	    reRollPlayers.clear();
	    pickedStateStack.clear();
	    pickedHeightStack.clear();
	    pickedObject = null;
	    doublesElgible = null;
	    usingDoubles = false;
	    reUsingWorker = false;
	    hasPlacedWorker = false;
	    doublesCount = 0;
	    proceedGameStep = ProceedStep.Start;
	    continuationStack.clear();
	    lastDroppedObject = null;
	    hasReducedRecruits = false;
	    normalStartSeen = false;
		bumpedWorker = null;
		bumpingWorker = null;
		selectedDieRoll = null;
		activeRecruit = null;
		randomKey = key;
		finalPath.clear();
        animationStack.clear();
        gameEvents.clear();
        moveNumber = 1;
        stepNumber = 0;
        rollNumber = 0;
        nOpenMarkets = 0;
        revealedNewInformation = false;
    	lastDroppedWorker = null;
    	currentPlayerInTurnOrder = -1;
         openedAMarket = false;		// ephemeral state
    	 shuffledADeck = false;
    	 openedAMarketLastTurn = false;
    	 shuffledADeckLastTurn = false;
    	 robotBoard = false;
    	 robot = null;
    	 setStatusDisplays();		// start with the displays set up
        // note that firstPlayer is NOT initialized here
    }
    private void adjustSize(EuphoriaCell c,EuphoriaChip ch,int n)
    {
    	int sz = c.height();
    	while(sz<n) { c.addChip(ch); sz++; }
    	while(sz>n) { c.removeTop(); sz--; }
    }
    public void adjustResources()
    {
    	adjustSize(clayPit,EuphoriaChip.Clay,5);
    	adjustSize(quarry,EuphoriaChip.Stone,5);
    	adjustSize(goldMine,EuphoriaChip.Gold,5);
    	adjustSize(	bliss,EuphoriaChip.Bliss,5);
    	adjustSize(farm,EuphoriaChip.Food,5);
    	adjustSize(generator,EuphoriaChip.Energy,5);
    	adjustSize(aquifer,EuphoriaChip.Water,5);
    	adjustSize(trash,EuphoriaChip.Trash,1);
    }
    
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(EuphoriaBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        for(EuphoriaCell c = allCells,d=from_b.allCells; c!=null; c=c.next,d=d.next) 
        	{ Assert(c.sameCell(d),"cell mismatch for %s",c.rackLocation()); }
        Assert(sameCells(usedRecruits,from_b.usedRecruits),"used recruits mismatch");
        Assert(sameCells(unusedRecruits,from_b.unusedRecruits),"unused recruits mismatch");
        Assert(activePlayer==from_b.activePlayer,"activePlayer mismatch");
        Assert(variation==from_b.variation,"variation mismatch");
        Assert(unresign==from_b.unresign,"unresign mismatch");
        Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        Assert(reUsingWorker==from_b.reUsingWorker,"reusing worker mismatch");
        Assert(hasPlacedWorker==from_b.hasPlacedWorker,"hasPlacedWorker worker mismatch");
        Assert(hasReducedRecruits == from_b.hasReducedRecruits,"reduced recruits mismatch");
        Assert(normalStartSeen == from_b.normalStartSeen,"normalStartSeen mismatch");
        Assert(AR.sameArrayContents(allegiance,from_b.allegiance),"allegiance mismatch");
        Assert(AR.sameArrayContents(tunnelPosition,from_b.tunnelPosition),"tunnelPosition mismatch");
        Assert(sameCells(lastDroppedWorker,from_b.lastDroppedWorker),"lastDroppedWorker mismatch");
        for(int i=0;i<players.length;i++) 
        	{  EPlayer.sameBoard(players[i],from_b.players[i]);
        	}
        Assert(pickedObject==from_b.pickedObject,"pickedObject mismatch");
        Assert(sameContents(droppedStateStack,from_b.droppedStateStack),"dropped state mismatch");
        Assert(sameContents(pickedStateStack,from_b.pickedStateStack),"picked state mismatch");
        Assert(sameContents(reRollPlayers,from_b.reRollPlayers),"rerollplayers mismatch");
        Assert(sameContents(pickedHeightStack,from_b.pickedHeightStack),"picked height stack matches %s and %s",pickedHeightStack,from_b.pickedHeightStack);
        Assert(doublesElgible==from_b.doublesElgible,"elgible for doubles mismatch");
        Assert(usingDoubles==from_b.usingDoubles,"using doubles mismatch");
        Assert(doublesCount==from_b.doublesCount,"count for doubles mismatch");
        Assert(proceedGameStep==from_b.proceedGameStep,"proceedGameStep mismatch");
        Assert(bumpedWorker==from_b.bumpedWorker,"bumpedWorkerMismatch");
        Assert(currentPlayerInTurnOrder==from_b.currentPlayerInTurnOrder,"currentPlayerInTurnOrder mismatch");
        Assert(bumpingWorker==from_b.bumpingWorker,"bumpingWorkerMismatch");
        Assert(selectedDieRoll==from_b.selectedDieRoll,"selectedDieRoll mismatch");
        Assert(activeRecruit==from_b.activeRecruit,"activeRecruit mismatch");
        Assert(stepNumber == from_b.stepNumber,"stepnumber matches");
        Assert(nOpenMarkets == from_b.nOpenMarkets,"nOpenMarkets mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        long v1 = Digest();
        long v2 = from_b.Digest();
        Assert(v1==v2,"Digest matches");
 
    }
    

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
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
        long v = super.Digest(r);		// this digests allcells
        if(board_state==EuphoriaState.Puzzle)
        {
        v ^= usedRecruits.Digest(r);
        v ^= unusedMarkets.Digest(r);
        v ^= unusedDilemmas.Digest(r);       
        v ^= unusedRecruits.Digest(r);

        }

        v ^= Digest(r,allegiance);
		v ^= Digest(r,tunnelPosition);
		v ^= r.nextLong()*(hasReducedRecruits?1:2);
        v ^= r.nextLong()*(normalStartSeen?1:2);
        v ^= r.nextLong()*((bumpedWorker==null)?0:(bumpedWorker.Digest()));
        v ^= r.nextLong()*((bumpingWorker==null)?0:(bumpingWorker.Digest()));
        v ^= EuphoriaChip.Digest(r,selectedDieRoll);
        v ^= EuphoriaChip.Digest(r,activeRecruit);
        v ^= Digest(r,lastDroppedWorker);
      
        for(EPlayer p : players) { v ^= p.Digest(r); }
 		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,reUsingWorker);
		v ^= Digest(r,hasPlacedWorker);
		v ^= Digest(r,pickedHeightStack);
		v ^= EuphoriaChip.Digest(r,doublesElgible);
		v ^= Digest(r,usingDoubles);
		v ^= Digest(r,doublesCount);
		v ^= Digest(r,proceedGameStep.ordinal());
		v ^= Digest(r,currentPlayerInTurnOrder);
		v ^= Digest(r,stepNumber);
		v ^= Digest(r,rollNumber);
		v ^= Digest(r,nOpenMarkets);
		
		for(int lim=pickedStateStack.size()-1; lim>=0; lim--)
		{
			v ^= r.nextLong()*(pickedStateStack.elementAt(lim).ordinal()+1);
		}
		for(int lim=reRollPlayers.size()-1; lim>=0; lim--)
		{
			v ^= r.nextLong()*(reRollPlayers.elementAt(lim).ordinal()+1);
		}
 
		for(int lim = continuationStack.size()-1; lim>=0; lim--)
			{
				v ^= r.nextLong()*continuationStack.elementAt(lim).Digest(r);
			}
		
		v ^= r.nextLong()*board_state.ordinal()+whoseTurn;
		return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(EuphoriaBoard from_b)
    {	
        super.copyFrom(from_b);
        board_state = from_b.board_state;
        variation = from_b.variation;
        if(board_state==EuphoriaState.Puzzle)
        	{
            for(EuphoriaCell c = displayCells,d=from_b.displayCells; c!=null; c=c.next,d=d.next)
            	{ c.copyFrom(d); }       	
        	}
        AR.copy(allegiance,from_b.allegiance);
        AR.copy(tunnelPosition,from_b.tunnelPosition);
        copyFrom(usedRecruits,from_b.usedRecruits);
        copyFrom(unusedRecruits,from_b.unusedRecruits);
        copyFrom(unusedMarkets,from_b.unusedMarkets);
        hasReducedRecruits = from_b.hasReducedRecruits;
        normalStartSeen = from_b.normalStartSeen;
        bumpedWorker = from_b.bumpedWorker;
        bumpingWorker = from_b.bumpingWorker;
        selectedDieRoll = from_b.selectedDieRoll;
        activeRecruit = from_b.activeRecruit;
        for(int i=0;i<players.length;i++) { players[i].copyFrom(from_b.players[i]); }
         
        pickedObject = from_b.pickedObject;
        reUsingWorker = from_b.reUsingWorker;
        hasPlacedWorker = from_b.hasPlacedWorker;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        pickedHeightStack.copyFrom(from_b.pickedHeightStack);
        doublesElgible = from_b.doublesElgible;
        usingDoubles = from_b.usingDoubles;;
        doublesCount = from_b.doublesCount;
        proceedGameStep = from_b.proceedGameStep;
        currentPlayerInTurnOrder = from_b.currentPlayerInTurnOrder;
        stepNumber = from_b.stepNumber;
        rollNumber = from_b.rollNumber;
        nOpenMarkets = from_b.nOpenMarkets;
        pickedStateStack.copyFrom(from_b.pickedStateStack);
        reRollPlayers.copyFrom(from_b.reRollPlayers);
        continuationStack.clear();
        lastDroppedWorker = getCell(from_b.lastDroppedWorker);
        for(int i=0,lim=from_b.continuationStack.size(); i<lim; i++)
        {
        	continuationStack.addElement(from_b.continuationStack.elementAt(i).clone(this));
        }
        steps = from_b.steps;
     
        // below here are copied but not digested
        unresign = from_b.unresign;
        REINIT_SIMULTANEOUS_PLAY = from_b.REINIT_SIMULTANEOUS_PLAY;
        SIMULTANEOUS_PLAY = from_b.SIMULTANEOUS_PLAY;
        activePlayer = from_b.activePlayer;
        finalPath.copyFrom(from_b.finalPath);
        droppedStateStack.copyFrom(from_b.droppedStateStack);
        robotBoard = from_b.robotBoard;
       // G.print("T "+from_b.robot+" "+from_b+" > "+this+" "+this.robot);
        robot = from_b.robot;
        lastPicked = null;
        CELLSIZE = from_b.CELLSIZE;
        sameboard(from_b); 
    }
    //
    // generate a new randomizer based on the current move number and the game's random key
    // we use this so we don't have to depend on the long term usage of a single random number stream.
    //
    public Random newRandomizer(long salt)
    {	if(revision>=102)
    	{	rollNumber++;
    	long key = (randomKey*(rollNumber*1000))*salt;
    	//G.print("");
    	//G.print("roll "+rollNumber+" "+key);
    		return(new Random(key));
    	}
    	else {  
    		
    		return(new Random((randomKey*moveNumber()+stepNumber*1000)^salt)); }
    }
    
    // recycle the used artifacts deck back to the unused
    private void reshuffleArtifacts()
    {
    	while(usedArtifacts.height()>0)
    	{
    		unusedArtifacts.addChip(usedArtifacts.removeTop());
    	}
    	if(LOG_ARTIFACTS) { G.print(moveNumber," reshuffle ",unusedArtifacts.height()); };
    	unusedArtifacts.shuffle(newRandomizer(0x6466712));
    }
    
    // set the active values from the visible values
    private void setKnowledgeValues()
    {
		for (int i = 0; i < knowlegeTrack.length; i++) {
			EuphoriaCell c = knowlegeTrack[i];
			for(int idx = c.height()-1; idx>=0; idx--)
			{
				EuphoriaChip ch = c.chipAtIndex(idx);
				EPlayer p = getPlayer(ch.color);
				p.setKnowledge(i+1,replayMode.Replay); 
			}
			
		}
   	
    }
	// set the visible knowledge track marker, val is 1-6 but the slots are 0-5
	private void setKnowlegeDisplay(Colors color, int val) {
		EuphoriaChip ch = EuphoriaChip.getKnowledge(color);
		EuphoriaCell src = getSource();
		for (int i = 0; i < knowlegeTrack.length; i++) {
			EuphoriaCell c = knowlegeTrack[i];
			if (c.findChip(ch) >= 0) {
				c.removeChip(ch);
			}
			if ((i == val - 1) && ((c!=src) || (ch!=pickedObject))) {
				c.addChip(ch);
			}
		}
	}
	
	// set the active values from the visible displays
	private void setAllegianceValues() 
	{
		for(int i=allegianceTrack.length-1; i>=0; i--)
		{	EuphoriaCell c = allegianceTrack[i];
			if(c.height()>0)
			{
			int faction = i / AllegianceSteps;
			int value = i% AllegianceSteps;
			allegiance[faction] = value;
			}
		}
	}

	
	private void setAllegianceDisplay(Allegiance faction, int value) {
		EuphoriaCell src = getSource();
		for (int i = 0, base = faction.ordinal() * AllegianceSteps; i < AllegianceSteps; i++) {
			EuphoriaCell c = allegianceTrack[i + base];
			if (c.topChip() != null) {
				c.removeTop();
			}
			;
			if ((i == value) && ((c!=src) || (EuphoriaChip.AllegianceMarker!=pickedObject)))
			{
				c.addChip(EuphoriaChip.AllegianceMarker);
			}
		}
	}
    // set the active values from the visible values
    private void setMoraleValues()
    {
		for (int i = 0; i < moraleTrack.length; i++) {
			EuphoriaCell c = moraleTrack[i];
			for(int idx = c.height()-1; idx>=0; idx--)
			{
				EuphoriaChip ch = c.chipAtIndex(idx);
				EPlayer p = getPlayer(ch.color);
				p.setMorale(i+1); 
			}
			
		}
   	
    }
	// set the visible morale track marker. val = 1-6 but the array is 0-5
	private void setMoraleDisplay(Colors color, int val) {
		EuphoriaChip ch = EuphoriaChip.getMorale(color);
		EuphoriaCell src = getSource();
		for (int i = 0; i < moraleTrack.length; i++) {
			EuphoriaCell c = moraleTrack[i];
			if (c.findChip(ch) >= 0) {
				c.removeChip(ch);
			}
			if ((i == val - 1) &&  ((c!=src) || (ch!=pickedObject)))
			{
				c.addChip(ch);
			}
		}
	}
	


	// reverse the visual tunnel value to the actively used value
	private void setTunnelValue(Allegiance faction)
	{	if(faction!=Allegiance.Factionless)
		{
		EuphoriaCell row[] = tunnelSteps[faction.ordinal()];
		for(int i=0;i<row.length;i++) { if(row[i].height()>0) { setTunnelPosition(faction,i); }}
		}
	}
	// set the tunnel display from the actively used value
	private void setTunnelDisplay(Allegiance faction, int value) {
		if(faction!=Allegiance.Factionless) {
		EuphoriaCell row[] = tunnelSteps[faction.ordinal()];
		if(row!=null)
		{
		EuphoriaCell src = getSource();
		for (int i = 0; i < row.length; i++) {
			row[i].reInit();
			if ((i == value) && ((row[i]!=src) || (pickedObject!=EuphoriaChip.Miner)))
			{
				row[i].addChip(EuphoriaChip.Miner);
			}
		}}}
	}

    //
    // prepare the display-only cells for use.
    //
    public void setStatusDisplays()
    {
    	for(EPlayer p : players) 
		{ setKnowlegeDisplay(p.color,p.knowledge);
		  setMoraleDisplay(p.color,p.morale);
		}
    	for(Allegiance f : Allegiance.values())
    		{ if(f!=Allegiance.Factionless)
    			{setAllegianceDisplay(f,getAllegianceValue(f));
    			if(f!=Allegiance.Icarite) { setTunnelDisplay(f,getTunnelPosition(f)); }
    			}}
    }
    
    //
    // reverse the visible status displays into the active values.
    // this is called when in puzzle mode and doing things through the UI.
    //
    public void reverseStatusDisplays(EuphoriaCell dest)
    {
    	switch(dest.rackLocation())
    	{
    	default: break;	// we don't need to do anything for the rest
    	
    	case EuphorianTunnel:
    		setTunnelValue(Allegiance.Euphorian);
    		break;
     	case SubterranTunnel:
       		setTunnelValue(Allegiance.Subterran);
    		break;
    	case WastelanderTunnel:
       		setTunnelValue(Allegiance.Wastelander);
    		break;
    	case KnowledgeTrack:
    		setKnowledgeValues();
    		break;
    	case MoraleTrack:
    		setMoraleValues();
    		break;
    	case AllegianceTrack:
    		setAllegianceValues();
    		break;
    	}
    }
    public boolean hasReducedRecruits(int p)
    {
    	return(players[whoseTurn].hasReducedRecruits());
    }
    int activeRecruitsWithFaction(Allegiance faction)
    {	int n = 0;
    	for(EPlayer p : players) 
    	{
    		n += p.countRecruits(p.activeRecruits,faction);
    	}
    	return n;
    }
    
    public void setNormalStarting(replayMode replay)
    {
    	// at this point give the players their dice
    	Assert(!hasReducedRecruits,"not already done");
    	hasReducedRecruits = true;
    	shuffledADeck=true;
    	EPlayer smartest = null;
    	for(EPlayer p : players)
    	{	// give each player 2 workers
    		p.addNewWorker(WorkerChip.getWorker(p.color,1));
    		p.totalWorkers++;
    		if(p.recruitAppliesToMe(RecruitChip.KofiTheHermit))
    		{
    		p.setPFlag(PFlag.StartWithKofiTheHermit);
    		}
    		else
    		{
    		p.addNewWorker(WorkerChip.getWorker(p.color,2));
    		p.totalWorkers++;
    		}
    		initialRerollWorkers(p,replay);
     		if((smartest==null) || (p.totalKnowlege()>smartest.totalKnowlege()))
    		{
    			smartest = p;
    		}
    	}
		setWhoseTurn(smartest.boardIndex);
    	currentPlayerInTurnOrder = whoseTurn;
    	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
    	if(isIIB()||revision>=123)
    	{	int maxr = 0;
    		for(Allegiance a : Allegiance.values()) { maxr = Math.max(maxr,activeRecruitsWithFaction(a)); }
    		for(EPlayer p : players)
    		{
    			RecruitChip recruit = (RecruitChip)p.activeRecruits.topChip();
    			Allegiance faction = recruit!=null ? recruit.allegiance : null;
    			if(faction!=Allegiance.Factionless)
    			{
    				int count = activeRecruitsWithFaction(faction);
    				if(count<maxr) 
    					{ p1("set starting bonus "+p.color);
    					  p.setPFlag(PFlag.ReceiveStartingBonus);
    					}
    			}
    		}
    	}
    	doNormalStart();
    }
    
    boolean readyToStartNormal()
    {
    	for(EPlayer p : players) 
		{ if(!p.hasReducedRecruits()) 
			{ return(false);  }}
    	return(true);
    }
    public boolean mustReduceRecruits(replayMode replay)
    {
    	if(hasReducedRecruits) { return(false); }
    	if(!readyToStartNormal()) { return(true); }
    	setNormalStarting(replay);
    	return(false);
    }
    
	EuphoriaCell authorityCells[][] = 
			{euphorianAuthority,subterranAuthority,wastelanderAuthority,icariteAuthority};

	private boolean hasNonemptyCell(EuphoriaCell c[])
	{
		for(EuphoriaCell r : c) { if(r.topChip()!=null) { return(true); }}
		return(false);
	}
    public EuphoriaCell getAvailableAuthorityCell(EPlayer p,Allegiance a)
    {  	
    	if(a!=Allegiance.Factionless) 
    	{
    	EuphoriaCell row[] = authorityCells[a.ordinal()];
    	
    	if(p.penaltyAppliesToMe(MarketChip.IIB_FieldOfAgorophobia)
    			&& !hasNonemptyCell(row)
    			)
	    	{	logGameEvent(MarketChip.IIB_FieldOfAgorophobia.getExplanation());
	    		//p1("use field of agoraphobia");
	    		return(null); 
	    	}
    	
    	 for(int lim=row.length-1; lim>=0; lim--) 
    	 	{ if (row[lim].topChip()==null) { return(row[lim]); }}
    	}
    	return(null);
    }

    public void placeAuthorityToken(EuphoriaCell c,EuphoriaChip v)
    {	if(v==null) { if(c.topChip()!=null) { c.removeTop(); }}
    	else { c.addChip(v); 
    	}
    }
    public EuphoriaCell getMarketA(Allegiance a)
    {	return(marketA[a.ordinal()]);
    }
    public EuphoriaCell getMarketB(Allegiance a)
    {
    	return(marketB[a.ordinal()]);
    }
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(EPlayer p)
    {	
 
    	revealedNewInformation = openedAMarket;
    	lastDroppedWorker = null;
    	
    	switch (board_state)
        {
        default:
        	throw Error("Move not complete, can't change the current player, state %s",board_state);
        case Puzzle:
        case ConfirmDiscardFactionless:
        case EphemeralConfirmDiscardFactionless:
        case DiscardResources:
        	break;
        case RetrieveCommodityWorkers:
        case RetrieveOrConfirm:
        case Retrieve1OrConfirm:
        case ConfirmRetrieve:
        	p.retrievals++;
        	revealedNewInformation = true;
        	setNextPlayer_internal();
        	break;
        case ConfirmPlace:
        	p.placements++;
        	setNextPlayer_internal();
        	break;
        	
		case EphemeralConfirmRecruits:
        case ConfirmRecruits:
        case ConfirmJoinTheEstablishment:
        case ConfirmFightTheOpressor:
        case ConfirmOneRecruit:
        case ConfirmActivateRecruit:
        case ConfirmPayCost:
        	
        case ConfirmBenefit:
        case PayForOptionalEffect:
        case ConfirmPayForOptionalEffect:
        case PayCost:
        case Resign:
        case RecruitOption:
        case DieSelectOption:
        case ConfirmRecruitOption:
        case NormalStart:
        case PlaceNew:
        case PlaceAnother:
        case DumbKoff:
        	setNextPlayer_internal();
        	break;
        }
    }
    private void setNextPlayer_internal()
    {	for(EuphoriaCell c = displayCells; c!=null; c=c.next) { c.marketPenalty = null; }
    	setNextPlayerTo((whoseTurn+1)%players.length);
    }
    private void setNextPlayerTo(int n)
    {	setWhoseTurn(n);
    	moveNumber++; //the move is complete in these states
        stepNumber = 0;
        currentPlayerInTurnOrder = n;
		for(EuphoriaCell m : markets) { m.ignoredForPlayer = null; }	// lionel the cook
     }
    private void doShaheenaTheDigger(EPlayer p,replayMode replay)
    {	//p1("do shaheena the digger");
    	p.payCostOrElse(Cost.Stone,replay);
    	setContinuation(new Continuation(Benefit.FreeArtifact,Function.AfterShaheenaTheDigger,p));
    }
    private void afterShaheenaTheDigger(EPlayer p,replayMode replay)
    {	
       	setState(EuphoriaState.ConfirmPlace);
    	proceedGameStep = ProceedStep.Step4AfterShaheena;
    	doMoraleCheck(p,replay,null,Function.ProceedWithTheGame);
     	proceedWithTheGame(replay);
    }
    
    private void dontShaheenaTheDigger(EPlayer p,replayMode replay)
    {	//p1("dont shaheena the digger");
    	proceedGameStep = ProceedStep.Step4AfterShaheena;
    	proceedWithTheGame(replay);
    }
    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
    	switch(board_state)
    	{
    	case PayCost:
    		{
    			Cost pend = pendingCost();
    			switch(pend)
    			{
    			case FreeMwicheTheFlusher:
    			case BlissOrFreeMwicheTheFlusherAndCommodity:  				
     			case FreeOrMwicheTheFlusherAndCommodity:
    			case FreeOrEnergyMwicheTheFlusherAndCommodity:
    			case FreeOrFoodMwicheTheFlusherAndCommodity:
    			case FreeOrWaterMwicheTheFlusherAndCommodity:
    			case BlissOrFreeMwicheTheFlusher:
    			case FreeOrWaterMwicheTheFlusher:
    			case FreeOrEnergyMwicheTheFlusher:
    			case FreeOrFoodMwicheTheFlusher:
    				if(!hasPaidSomething()) { return(true); }
    				break;
    			default: break;
    			}
    		}
    		
    		break;
    	default: break;
    	}
    	
    	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selecteive.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }

    


    public boolean WinForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	
    	return(false);
    }
    private int countMarketTokens(EuphoriaChip ch)
    {	int n=0;
    	for(EuphoriaCell c : markets) { if(c.containsChip(ch)) { n++; }}
    	return(n);
    }
    private int countAuthorityTokens(EuphoriaChip ch)
    {	int n=0;
    	for(EuphoriaCell aa[] : authorityCells)
    	{
    	for(EuphoriaCell c : aa) { if(c.containsChip(ch)) { n++; break; }}
    	}
    	return(n);
    }
    private boolean checkGameOver()
    {	EPlayer winner = null;
    	for(EPlayer p : players) 
    		{ if(p.authorityTokensRemaining()==0) 
    			{
    			if(winner==null) 
    				{ winner = p; 
    				}
    			else {
    				if((p.morale>winner.morale)
    					|| (p.knowledge<winner.knowledge)
    					|| (countMarketTokens(p.myAuthority)>countMarketTokens(winner.myAuthority))
    					|| (countAuthorityTokens(p.myAuthority)>countMarketTokens(winner.myAuthority))
    					|| (p.finalRandomizer>winner.finalRandomizer)
    						)
    					{ winner = p;  }
    				else {	// rolloff
    					}
    				}
     			}
    		}
    	if(winner!=null)
    	{
    		win[winner.boardIndex] = true;
    		return(true);
    	}
    	return(false);
    }
    /**
     * return a score for the player in a multiplayer game. 
     */
    public int ScoreForPlayer(int pl)
    {	EPlayer p = players[pl];
    	return( (STARTING_AUTHORITY_TOKENS-p.authorityTokensRemaining())*100000
    			+ p.morale*10000
    			+ (MAX_KNOWLEDGE_TRACK-p.knowledge)*1000
    			+ countMarketTokens(p.myAuthority)*100
    			+ countAuthorityTokens(p.myAuthority)*10
    			+ p.finalRandomizer);
    }
    public int PrettyScoreForPlayer(int pl)
    {	EPlayer p = players[pl];
    	return(STARTING_AUTHORITY_TOKENS-p.authorityTokensRemaining());
    }
    
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	pickedObject = null;
    	reUsingWorker = false;
        droppedDestStack.clear();
        pickedHeightStack.clear();
        pickedSourceStack.clear();
        droppedStateStack.clear();
        pickedStateStack.clear();
    	adjustResources();

      }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private EuphoriaCell unDropObject()
    {	EuphoriaCell rv = droppedDestStack.top();
		EuphoriaId rack = rv.rackLocation();
		if(rv!=null) 
    	{	droppedDestStack.pop();
    	
    		if((board_state==EuphoriaState.PayForLionel)
    			&& (rack==EuphoriaId.Market))
    		{
    			pickedObject = EuphoriaChip.Food;
    			rv.ignoredForPlayer = null;
    		}
    		else
    		{
     		pickedObject = rv.removeTop();
    		
    		if(rv.onBoard && pickedObject.isWorker())
    		{
    			EPlayer p = getPlayer(pickedObject.color);
    	        p.unPlaceWorker(rv);  
    		}}
    		setState(droppedStateStack.pop());
     	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	
    	if(pickedSourceStack.size()>0) 
    		{ int height = pickedHeightStack.pop();
    		  EuphoriaCell src = pickedSourceStack.pop();
    		  if(pickedObject!=null)
    		  { if(!src.rackLocation().infinite) { src.insertChipAtIndex(height,pickedObject); }
    		    if(src.rackLocation==EuphoriaId.UnusedWorkers)
    	    	{
    	    		EPlayer p = getPlayer(pickedObject.color);
    	    		p.totalWorkers--;
    	    	}
    		  else if(src.onBoard && pickedObject.isWorker())
    		  {
    			EPlayer p = getPlayer(pickedObject.color);
    	        p.placeWorker(src);  
    		  }}
    		  else { //Error("Nothing picked"); 
    		  }
    		  setState(pickedStateStack.pop());
    		}
		  pickedObject = null;
    }
    // 
    // drop the floating object.
    //
    private void dropObject(EuphoriaCell c,replayMode replay)
    {
    	EuphoriaId rack = c.rackLocation();

    	switch(rack)
		{
    	case GenericSink:
     	case GenericPool: break;
		case ArtifactDiscards:
			recycleArtifact(pickedObject);   
			break;
		case PlayerActiveRecruits:
			getPlayer(c.color).addActiveRecruit(pickedObject,replay); 
			break;
		case PlayerFood:
		case PlayerWater:
		case PlayerBliss:
		case PlayerEnergy:
			if(getPlayer(c.color).doLottery(1,c)==0) 
				{ 
				  c = trash;
				}
			c.addChip(pickedObject);
			break;
			
		case PlayerGold:
		case PlayerClay:
		case PlayerStone:
			{
			EPlayer p = getPlayer(c.color);
			
			if(p.doForcedAltruism(1,rack.prettyName)==0)
				{
				c = trash;
				}
			c.addChip(pickedObject);  
			}
			break;
		case Market:
			if(pickedObject==EuphoriaChip.Food) { c.ignoredForPlayer = players[whoseTurn].color; }
			else { c.addChip(pickedObject); }
			if((c.height()==1) && pickedObject.isMarket()) 
			{ // rearranging the markets manually
				for(int i=0;i<markets.length;i++)
				{ if(markets[i]==c) 
					{
					useMarkets[i].placementCost =((MarketChip)pickedObject).placementCost;
					}
				}
			}
			break;
		default:  
			c.addChip(pickedObject);  
			break;
		}
        
        if(c.onBoard && pickedObject.isWorker()) 
        	{ 
        	EPlayer p = getPlayer(pickedObject.color);

        	p.placeWorker(c);
        	}
        droppedDestStack.push(c);
        droppedStateStack.push(board_state);
        lastDroppedObject = pickedObject;
        pickedObject = null;
      }

    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { EuphoriaChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }

    // get a local version of the per-player cell
    private EuphoriaCell getCell(Colors c,EuphoriaId r)
    {	return(getPlayer(c).getCell(r));
    }
    // get a local version of a cell on the board
    EuphoriaCell getCell(EuphoriaId r,int idx)
    {	EuphoriaCell c = allCellsById.get(r);
     	if(r.isArray)
    	{	while(c.row!=idx) { c = c.nextInGroup; Assert(c!=null,"Array index %d not found",idx); }
    	}
    	return(c);
    }
    // get the local version of c, which may be local or from another board
    public EuphoriaCell getCell(EuphoriaCell c)
    {
    	return((c==null)
    			? null
    			: c.rackLocation().perPlayer
    				? getCell(c.color,c.rackLocation()) 
    				: getCell(c.rackLocation(),c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(EuphoriaCell c,int h)
    {	pickedSourceStack.push(c);
    	pickedStateStack.push(board_state);
    	if((c==unusedArtifacts)&&(c.height()==0)) { reshuffleArtifacts(); }
    	int pickedHeight = Math.max(0,Math.min(c.chipIndex,h));
    	pickedHeightStack.push(pickedHeight);
    	lastDroppedObject = null;
    	switch(c.rackLocation())
    	{
    	case GenericSink:
    	case GenericPool:
    		pickedObject = c.topChip();
    		break;
    	case UnusedWorkers:
    		EPlayer p = getPlayer(pickedObject.color);
    		p.totalWorkers++;
    		break;
    	default:
         	Assert(c.topChip()!=null,"Nothing to pick from "+c.rackLocation());
         	pickedObject = c.removeChipAtIndex(pickedHeight);    	
    	}
    	
        if(c.onBoard && pickedObject.isWorker())
        	{
        		EPlayer p = getPlayer(pickedObject.color);
        		p.unPlaceWorker(c);
        	}
    }
    
    
    private void doGeekTheOracle(EPlayer p,replayMode replay,RecruitChip active)
    {
			//
  		    // once per turn, if you gained an artifact, gain another and then discard one.
   		    //
    		p.incrementKnowledge(replay);
    		// preserve an old bug
    		if(revision>=124) { p.setTFlag(TFlag.UsedGeekTheOracle); }
    		if(active==RecruitChip.GeekTheOracle)
    		{
    	    p.incrementKnowledge(replay);
    	    p.collectBenefitOrElse(Benefit.Artifact,replay);
    	    logGameEvent(CardPeek); 
    	    Cost residual = p.payCost(Cost.CardForGeek,replay);
   	    	if(residual!=null)
   	    		{
   	    		setContinuation(new Continuation(residual,Function.ProceedWithTheGame,p));
   	    		return;
   	    		}}
    		else if(active==RecruitChip.GeekTheOracle_V2)
    		{	// v2 takes only one knowledge, gives 2 cards
    			p.collectBenefitOrElse(Benefit.Artifactx2,replay);
    			Cost residual = p.payCost(Cost.CardForGeekx2,replay);
    			if(residual!=null)
   	    		{
   	    		setContinuation(new Continuation(residual,Function.ProceedWithTheGame,p));
   	    		return;
   	    		}
    		}
    		else { throw Error("Not expecting recruit %s",active); }
   	    	proceedWithTheGame(replay);
    }

    //
    // do optional end of turn actions, check for doubles, check for end of game, set next player and starting state
    //
    private void proceedWithTheGame(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(continuationStack.size()>0) { return; }
    	//reset whose turn in case it was changed by reroll or other subroutines
    	setWhoseTurn(currentPlayerInTurnOrder);
    	EuphoriaCell lastDropped = lastDroppedWorker;
    	
   		acceptPlacement();
   		
   		
 
    	switch(proceedGameStep)
    	{
    	case Start: registerStep(ProceedStep.Start);
    		if(checkGameOver()) {  setState(EuphoriaState.Gameover); return; }
    		
			//$FALL-THROUGH$
		case Step1:	registerStep(ProceedStep.Step1);
	    	//
	    	// here the game is definitely not over
	    	//
 	    	if(!doMoraleCheck(p,replay,null,Function.ProceedWithTheGame)) 
	    		{ return; }		// adjust morale dialog is needed
 
 	    	if(revision>=110) 
    			{ 
 	    		for(EPlayer pl : players)
 	    		{
 	    		if(!doMoraleCheck(pl,replay,null,Function.ProceedWithTheGame)) 
 	    			{ return; 
 	    			}
 	    		if(revision<122)	// no knowledge checks at turn end stating at 122
 	    		{
    			if(pl.knowledgeCheck(0,replay))
    				{// lost a worker, we don't need to check for DustyTheEnforcer because
    				 // this is only done as a legacy in bugg saved games.
    				pl.loseWorker(replay);
    				setContinuation(new Continuation((EuphoriaCell)null,EuphoriaState.ExtendedBenefit, Function.ProceedWithTheGame,pl));
    				checkStevenTheScholar(pl,replay,false);
    				return;
    				}}
 	    		}}
    		
			//$FALL-THROUGH$
		case Step2:	registerStep(ProceedStep.Step2);	
			if(p.testTFlag(TFlag.TriggerGeorgeTheLazyCraftsman))	// reroll self and collect a resource 
				{
				doTheBumping(lastDroppedWorker);
				p.clearTFlag(TFlag.TriggerGeorgeTheLazyCraftsman);
				setContinuation(new Continuation(Benefit.Resource,Function.DoGeorgeTheLazyCraftsman,p));
				}		
			//$FALL-THROUGH$
		case Step2AfterGeorge: registerStep(ProceedStep.Step2AfterGeorge);
			
    		//
    		// here morale and cards are in balance for sure.
    		//
    		while(reRollPlayers.size()>0)
    		{	// do scheduled reroll after opening markets. Roll them from first in to last in
    			Colors color = reRollPlayers.remove(0,true);
    			EPlayer roller = getPlayer(color);
    			reRollWorkers(roller,replay,null,Function.ProceedWithTheGame);
    			return;
    		}
  	    	for(EPlayer reroll : players)
	    	{	// reroll after normal retrieval or puzzle setup
  	    		if(reroll.newWorkers.height()>0) 
  	    			{ reRollWorkers(reroll,replay,null,Function.ProceedWithTheGame); 
  	    			  return; 
  	    			}
	    	}
			//$FALL-THROUGH$
		case Step3:	registerStep(ProceedStep.Step3);
	    	//
	    	// here no workers need to be rolled
	    	//
   		if(p.testTFlag(TFlag.TriggerMaggieTheOutlaw))
    		{
       			p.clearTFlag(TFlag.TriggerMaggieTheOutlaw);
       			logGameEvent(ExtraWaterOrStone); 
       			setContinuation(new Continuation(Benefit.WaterOrStone,Function.ProceedWithTheGame,null));
       			return;
    		}

   			if(p.testTFlag(TFlag.AddedArtifact))
   			{
    			if(p.recruitAppliesToMe(RecruitChip.GeekTheOracle) 
    					&& !p.testTFlag(TFlag.UsedGeekTheOracle)
    					&& p.canPayX(Cost.Knowledgex2)
    					)
   				{
   				proceedGameStep = ProceedStep.Step4;
   				setContinuation(new Continuation(RecruitChip.GeekTheOracle,Function.DoGeekTheOracle,Function.ProceedWithTheGame,p));
   				return;
   				} 		
    		if(p.recruitAppliesToMe(RecruitChip.GeekTheOracle_V2) 
    			&& !p.testTFlag(TFlag.UsedGeekTheOracle)
    			&& p.canPayX(Cost.Knowledge)
    			)
   				{
   				proceedGameStep = ProceedStep.Step4;
   				setContinuation(new Continuation(RecruitChip.GeekTheOracle_V2,Function.DoGeekTheOracle,Function.ProceedWithTheGame,p));
   				return;
   				}
   			}
			//$FALL-THROUGH$
		case Step4: registerStep(ProceedStep.Step4);
			// handle doubles and chase the miner
			if(!doMoraleCheck(p,replay,null,Function.ProceedWithTheGame)) 
				{ return; }		// adjust morale dialog is needed
		
			p.startNewWorker();

			// various recruits 
			if(p.taedAuthorityHeight>p.authority.height()
					&& p.canPayX(Cost.Clay)
					&& p.recruitAppliesToMe(RecruitChip.TaedTheBrickTrader))
			{	//p1("ask taed the brick trader");
				setContinuation(new Continuation(RecruitChip.TaedTheBrickTrader,
						Function.DoTaedTheBrickTrader,Function.DontTaedTheBrickTrader,p));
				// we'll come back to step4
				return;
			}
			else { p.taedAuthorityHeight = p.authority.height(); }
			
			// various recruits let you play another worker
			if(p.terriAuthorityHeight>p.authority.height()
					&& p.canPay(Cost.Bliss)
					&& p.recruitAppliesToMe(RecruitChip.TerriTheBlissTrader))
			{	
				setContinuation(new Continuation(RecruitChip.TerriTheBlissTrader,Function.DoTerriTheBlissTrader,Function.DontTerriTheBlissTrader,p));
				// we'll come back to step 4
				return;
			}
			else { p.terriAuthorityHeight = p.authority.height(); }
			
			//$FALL-THROUGH$
		case Step4AfterTerri:
			registerStep(ProceedStep.Step4AfterTerri);
			if((lastDropped!=null) 	// this is null when the current move is a retrieve
					&& (lastDropped.topChip()!=null)	// and it is still there
					&& p.testTFlag(TFlag.TriggerLarsTheBallooneer)
					&& !p.testTFlag(TFlag.UsedLarsTheBallooneer)
					&& p.recruitAppliesToMe(RecruitChip.LarsTheBallooneer))
			{ 	//p1("ask lars the ballooneer");
				//
				// don't trigger lars a second time : you only get one chance, then you have
				// to acquire another balloon.  The reason for this is that the latent trigger
				// for lars can interact with other recruits in ways that are difficult to predict,
				// for example if the lars worker was first of a doubles, or one of the other recruits
				// was active that could allow another worker to be placed.
				//
				//p1("lars "+lastDropped.rackLocation()+" "+(lastDropped.topChip()!=null));
				p.clearTFlag(TFlag.TriggerLarsTheBallooneer);
				setContinuation(new Continuation(RecruitChip.LarsTheBallooneer,lastDropped,Function.DoLarsTheBallooneer,Function.DontLarsTheBallooneer,p));
				return;
			}
			//$FALL-THROUGH$
		case Step4AfterLars:
			registerStep(ProceedStep.Step4AfterLars);

			if(((doublesElgible!=null)
					&& (doublesCount>0)
					&& p.hasWorkersInHand()
					&& ((revision<123) || p.testTFlag(TFlag.HasLostMorale) || p.canPayX(Cost.Morale)))
						|| p.testTFlag(TFlag.UsedChaseTheMiner))
					{
					// play another after doubles, or chase the miner allows immediate use
					proceedGameStep = ProceedStep.Start;
					setNextStateAfterDone(replay);  
					return;
					}
		
		if(!p.testTFlag(TFlag.UsedJadwigaTheSleepDeprivator)
				&& p.canPay(Cost.Knowledgex2)
				&& p.hasWorkersInHand()
				&& p.recruitAppliesToMe(RecruitChip.JadwigaTheSleepDeprivator))
			{
			//p1("ask jadwiga");
			setContinuation(new Continuation(RecruitChip.JadwigaTheSleepDeprivator,
					Function.DoJadwigaTheSleepDeprivator,Function.DontJadwigaTheSleepDeprivator,p));
			return;
			}
			//$FALL-THROUGH$
		case Step4AfterJadwiga: registerStep(ProceedStep.Step4AfterJadwiga);
			
			if(!p.testTFlag(TFlag.UsedSpirosTheModelCitizen)
				&& (lastDroppedWorker!=null)	// have played a worker
				&& p.canPay(Cost.Morale)
				&& p.hasWorkersInHand()
				&& p.recruitAppliesToMe(RecruitChip.SpirosTheModelCitizen))
			{	//p1("ask spiros");
	     		setContinuation(new Continuation(RecruitChip.SpirosTheModelCitizen,
	     				Function.DoSpirosTheModelCitizen,Function.DontSpirosTheModelCitizen,p));
	     		return;
				
			}
		
			//$FALL-THROUGH$			
		case Step4AfterSpiros: registerStep(ProceedStep.Step4AfterSpiros);
		
    	if(!p.testTFlag(TFlag.AskedShaheenaTheDigger)
    			&& p.recruitAppliesToMe(RecruitChip.ShaheenaTheDigger)
    			&& p.canPayX(Cost.Stone))
    	{	p.setTFlag(TFlag.AskedShaheenaTheDigger);
     		setContinuation(new Continuation(RecruitChip.ShaheenaTheDigger,Function.DoShaheenaTheDigger,Function.DontShaheenaTheDigger,p));
    		return;
    	}  	
		//$FALL-THROUGH$
		case Step4AfterShaheena: registerStep(ProceedStep.Step4AfterShaheena);

		if(p.canPayX(Cost.Gold)
			&& p.recruitAppliesToMe(RecruitChip.HajoonTheColdTrader))
		{
			setContinuation(new Continuation(RecruitChip.HajoonTheColdTrader,Function.DoHajoon,Function.DontHajoon,p));
			return;
		}
			//$FALL-THROUGH$
		case Step4AfterHajoon: registerStep(ProceedStep.Step4AfterHajoon);
			//$FALL-THROUGH$
		case StepNext: 	
			{
		        boolean dilemma = p.testPFlag(PFlag.RetrievePrisonersDilemma);
		        boolean skipNext = false;
		        if(p.testTFlag(TFlag.TriggerKofiTheHermit))
		        {	// get a new turn.  This can interact with dilemma's prisoner, which we
		        	// define to be that kofi gets his turn first, then the out of turn retrievals
		        	logGameEvent(UseKofiTheHermit);
		        	useRecruit(RecruitChip.KofiTheHermit,"freeplay");
		        	p.clearTFlag(TFlag.TriggerKofiTheHermit);
		        	skipNext = true;
		        	//if(dilemma) { p1("Free turn before dilemma retrieval");}
		        	if(p.testPFlag(PFlag.RetrievePrisonersDilemmaBefore)) 
		        	{
		        		p1("Free turn after dilemma retrieval before");
		        	}
		        	if(p.testPFlag(PFlag.RetrievePrisonersDilemmaAfter))
		        	{
		        		p1("Free turn after dilemma retrieval after");
		        	}
		        	proceedGameStep = ProceedStep.Start;		// reset to the next round
	    			setState(EuphoriaState.Place);
	    			return;
		        }
		        else 
		        {
		        p.clearPFlag(PFlag.RetrievePrisonersDilemmaBefore);
		        if(dilemma)
		        {
		        // when dilemma's prisoner is triggered, do a complicated dance
		        // with the previous and next player, but only if they are elgible
		        // to retrieve a worker
		        int np = players.length;
		        int prev = (whoseTurn+np-1)%np;
		        int next = (whoseTurn+1)%np;
		        EPlayer nextP = players[next];
		        if(nextP.hasWorkersOnBoard()) 
		        	{ nextP.setPFlag(PFlag.RetrievePrisonersDilemmaAfter);
		        	}
		        if(next!=prev) 
		        { EPlayer pp = players[prev];
		          if(pp.hasWorkersOnBoard()) 
		        	  {//p1("dilemma before");
		        	   proceedGameStep = ProceedStep.Start;
		        	   setNextPlayerTo(prev);
		        	   pp.setPFlag(PFlag.RetrievePrisonersDilemmaBefore);
		        	   setState(EuphoriaState.Retrieve1OrConfirm);
		        	   return;
		        	   }
		         }
		         //p1("dilemma no before");
		         p.clearPFlag(PFlag.RetrievePrisonersDilemma);	// we're all set to just do the "after"
		        }
		        
		        if(p.testPFlag(PFlag.RetrievePrisonersDilemmaAfter))
		        {	// got the free retrieve, now take the regular turn
		        	p.clearPFlag(PFlag.RetrievePrisonersDilemmaAfter);
		        	skipNext = true;
		        	//p1("dilemma skip");
		        }}
		        
		        if(!skipNext) 
		        {
		        	setNextPlayer(p);
			        p = players[whoseTurn];	// new p for the new player
		        }
		        
		        if(p.testPFlag(PFlag.RetrievePrisonersDilemma))
		        {	// step forward after step back
		        	p.clearPFlag(PFlag.RetrievePrisonersDilemma);
		        	setNextPlayer(p);
		        	p = players[whoseTurn];
		        }
		        if(p.testPFlag(PFlag.RetrievePrisonersDilemmaAfter))
		        	{	//p1("dilemma after");
		        		proceedGameStep = ProceedStep.Start;
		        		setState(EuphoriaState.Retrieve1OrConfirm);
		        		return;
		        	}

			}
			//$FALL-THROUGH$
		case Step5:		
				registerStep(ProceedStep.StepNext);	// register both steps
				registerStep(ProceedStep.Step5);
				proceedGameStep = ProceedStep.Start;		// reset to the next round
    			setNextStateAfterDone(replay);  
    			
			break;
		default: break;
    	}
    	for(EPlayer pl : players)
    	{
    		Assert(pl.morale>=pl.artifacts.height(),"needed morale check for %s",pl);
    	}
    }
    private boolean moreWorkersAvailable()
    { 	
    	if(revision>=107)
    	{
    	if(revision<123) { doublesCount--;		}	// use one up 
    	if(doublesCount<0) { return(false); }
    	}
    	return(true);
    }
    // set the state to the appropriate running state
    void setRunningState(replayMode replay)
    {	//
    	// no further variations in the program flow from here.
    	//
    	Assert(continuationStack.size()==0,"should be at top level");

    	for(EPlayer pl : players)
    	{	
    		boolean moraleOk = pl.morale>=pl.artifacts.height();
    		boolean gameOn = pl.authority.height()>0; 
    		Assert(moraleOk,"need a morale check for %s",pl.color);
    		Assert(gameOn,"should be gameover here");
     	}

		EPlayer p = players[whoseTurn];
		if(p.testPFlag(PFlag.StartWithKofiTheHermit))
		{
			p.clearPFlag(PFlag.StartWithKofiTheHermit);
			logGameEvent(StartingWithKofi);
		}
		if(!p.testTFlag(TFlag.UsedDarrenTheRepeater)
			&& (lastDroppedWorker != null)
			&& (lastDroppedWorker.rackLocation().canBeBumped)
			&& p.recruitAppliesToMe(RecruitChip.DarrenTheRepeater)
			&& p.canPayX(Cost.Morale)
			&& p.canPay(lastDroppedWorker)
			)
		{	
			
			p.setTFlag(TFlag.UsedDarrenTheRepeater);	// only available the first time
			if(addWorkerPlacementMovesAt(null,p,lastDroppedWorker)	// will otherwise be able to place there
					)
			{	//p1("ask darrentherepeater");
				// this is an approximation to the condition that you have to be able to pay to place on the spot again
				// but it's not exact. Using UsedDarrenTheRepeater loses morale, which might lose a card and a stupid
				// player could lose half of a pair unnecessarily.
				if(p.canPay(lastDroppedWorker))
				{
				setContinuation(new Continuation(RecruitChip.DarrenTheRepeater,Function.DoDarrenTheRepeater,Function.DontDarrenTheRepeater,p));
				return;
				}
				else { p1("Darrent the repeater can't pay "+lastDroppedWorker.placementCost); }
			}
		}
		dontDarrenTheRepeater(p,replay);
    }
    
void doDarrenTheRepeater(EPlayer p,replayMode replay)
{	p.payCostOrElse(Cost.Morale,replay);
	usingDoubles = false;
	doublesElgible = null;
	//if(p.artifacts.height()>p.morale) { p1("do darren the repeater with morale check");}
	if(doMoraleCheck(p,replay,null,Function.ContinueDarrenTheRepeater))
		{	continueDarrenTheRepeater(p,replay); 
		}
}
void continueDarrenTheRepeater(EPlayer p,replayMode replay)
{
	if(p.canPay(lastDroppedWorker))
	{	proceedGameStep = ProceedStep.Start;
		setState(EuphoriaState.RePlace);
	}
	else 
	{ //p1("darren the repeater still can't play");
	  // this is a rare occurrence where invoking darren involves a morale
	  // check and the loss of an artifact, after which the original space
	  // is no longer reachable.   Sometimes this is inevitable (and theoretically
	  // preventable) , but other times it depends on which card was discarded.
	  setState(EuphoriaState.DumbKoff); 
	}	
}

void dontDarrenTheRepeater(EPlayer p,replayMode replay)
{		
	boolean onBoard = p.hasWorkersOnBoard();
	boolean inHand = p.hasWorkersInHand();
	WorkerChip nextDoublesElgible = null;
	usingDoubles = false;

	if(onBoard 
		&& inHand 
		&& (p.workersOnBoard()>=2)
		&& p.penaltyAppliesToMe(MarketChip.IIB_BureauOfRestrictedTourism))
			{
			MarketPenalty mp = MarketChip.IIB_BureauOfRestrictedTourism.marketPenalty;
			logGameEvent(mp.explanation);
			//p1("cant place due to restricted tourism");
			inHand = false;		// can't place
			}
		if(onBoard 
				&& inHand
				&& p.penaltyAppliesToMe(MarketChip.IIB_NaturalFlouridatedSpring)
				&& p.hasBoardWorkerWith6()
				)
		{	MarketPenalty mp = MarketChip.IIB_NaturalFlouridatedSpring.marketPenalty;
			//p1("can't place due toNaturalFlouridatedSpring ");
			logGameEvent(mp.explanation);
			inHand = false;
			
		}
		if(p.testTFlag(TFlag.UsedChaseTheMiner))
			{ 	setState(EuphoriaState.PlaceNew); 
				if(revision>=119)
					{
						nextDoublesElgible = (WorkerChip)p.workers.topChip();
					}else
					{
						doublesElgible = (WorkerChip)p.workers.topChip();
					}
				Assert(p.workers.height()>0,"must be workers");
				p.clearTFlag(TFlag.UsedChaseTheMiner);
			}
		else if((doublesElgible!=null) 
				&& p.workers.containsChip(doublesElgible)
				&& moreWorkersAvailable()
				// in IIB and new euphoria, playing doubles also requires paying morale
				&& ((revision<123) || !variation.isIIB() || p.canPayX(Cost.Morale))
				) 
			{ // the second half of the double could have been lost to a knowledge check,
			  // and the first half may not still be on the board if it was sacrificed.
			//if(p.hasWorkersLike(doublesElgible)>1)
			//	{ throw G.Error("a"); }
			  nextDoublesElgible = doublesElgible;
			  usingDoubles = true;
			  setState(EuphoriaState.PlaceAnother); 
			}
		else if(onBoard && inHand) { setState(EuphoriaState.PlaceOrRetrieve); }
		else if(!onBoard ) { setState(EuphoriaState.Place); }
		else { setState(EuphoriaState.Retrieve); }
		doublesElgible = nextDoublesElgible;
		proceedGameStep = ProceedStep.Start;
		selectedDieRoll = null;
		activeRecruit = null;
		bumpingWorker = null;
		bumpedWorker = null;
		openedAMarketLastTurn = openedAMarket;
		openedAMarket = false;		// ephemeral state
		shuffledADeckLastTurn = shuffledADeck;
		shuffledADeck = false;
		hasPlacedWorker = false;
		//p.startNewTurn(this);	
		finalPath.clear();
		
		switch(board_state)
		{
		default: for(EPlayer pp : players) { pp.startNewTurn(replay); }
				break;
				// continuation of the same turn
		case PlaceAnother:
			if(p.testTFlag(TFlag.HasLostMorale)) { setState(EuphoriaState.Place);}	// we paid, insist on placing
			//$FALL-THROUGH$
		case ReUseWorker:
		case PlaceNew:
		case RePlace:	break;
		}
				
		if(p.testPFlag(PFlag.ReceiveStartingBonus))
		{	//p1("give starting bonus "+p.color);
			p.clearPFlag(PFlag.ReceiveStartingBonus);
			logGameEvent(StartingBonus);
			setContinuation(new Continuation(Benefit.Commodityx2,Function.Return,p));
		}

	}
    
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw Error("Not expecting state %s",board_state);
    	case Gameover: break;
  
    		
       	case ConfirmFightTheOpressor: 		
       		setState(EuphoriaState.ChooseOneRecruit);
       		break;
 
		case EphemeralConfirmRecruits:
			setState(EuphoriaState.NormalStart);
			break;
   			
	   	case NormalStart:
	   		if(!hasReducedRecruits)
	   		{
	   		setNormalStarting(replay);
	   		setRunningState(replay);
	   		}
	   		break;
	   	case ConfirmDiscardFactionless:
	   	case EphemeralConfirmDiscardFactionless:
	   	case ConfirmRecruits:
	   		if(mustReduceRecruits(replay)) 	
   			{ 
   			setRecruitDialogState(players[whoseTurn]);	
   			break;
   			}
	   		setRunningState(replay);
	   		break;
    	case Puzzle:
       		if(mustReduceRecruits(replay)) 	
       			{ 
	   			setRecruitDialogState(players[whoseTurn]);	
	   			break;
       			}
    		// otherwise continue through    		
			//$FALL-THROUGH$
    	case RetrieveCommodityWorkers:
		case RetrieveOrConfirm:
		case Retrieve1OrConfirm:
    	case ConfirmRetrieve:
    	case ConfirmPlace:
    	case PlaceOrRetrieve:
    	case PayForOptionalEffect:
    	case ConfirmPayForOptionalEffect:
    	case PlaceAnother:
    	case PlaceNew:
    	case ConfirmOneRecruit:
    	case ConfirmActivateRecruit:
    	case ConfirmPayCost:
    	case ConfirmBenefit:
    	case PayCost:
    	case ConfirmRecruitOption:
    	case RecruitOption:
    	case DieSelectOption:
    	case ConfirmJoinTheEstablishment:
    	case DumbKoff:
    		setRunningState(replay);
    		break;
    	case DiscardResources:
    		doContinuation(replay);
    		break;
    		 
    	}
    }
    void setRecruitDialogState(EPlayer p)
    {	Assert(activePlayer>=0,"activePlayer set");
    	boolean recruitsReady = (p.activeRecruits.height()>0) && (p.hiddenRecruits.height()>0);
    	int factionLess = p.countRecruits(Allegiance.Factionless);
    	if(SIMULTANEOUS_PLAY)
    	{	
   			boolean ready = true;
   			for(EPlayer pl : players) { ready &= pl.hasReducedRecruits(); }
    		if(ready) { setState(EuphoriaState.NormalStart); }
    		else if(recruitsReady) { setState(EuphoriaState.EphemeralConfirmRecruits); }
    		else if(factionLess>1) 
    			{ setState((p.discardedRecruits.height()>0)
    					? EuphoriaState.EphemeralConfirmDiscardFactionless 
    					: EuphoriaState.EphemeralDiscardFactionless); 
    			}
    		else { setState(EuphoriaState.EphemeralChooseRecruits ); }
    	}
    	else
    	{
    		if(recruitsReady) { setState( EuphoriaState.ConfirmRecruits);}
    		else if (factionLess>1) 
    			{ setState((p.discardedRecruits.height()>0) 
    					? EuphoriaState.ConfirmDiscardFactionless 
    					: EuphoriaState.DiscardFactionless); 
    			}
    		else { setState(EuphoriaState.ChooseRecruits);} 
    	}
    }
 
    private void reRoll(EPlayer p,replayMode replay)
    {	
    	if(p.penaltyAppliesToMe(MarketChip.IIB_DilemmasPrison)
    		&& (p.workersOnBoard()==0))
    		{
    		logGameEvent(MarketChip.IIB_DilemmasPrison.getExplanation());
    		//p1("use prisoners dilemma");
    		p.setPFlag(PFlag.RetrievePrisonersDilemma);		
    		}
    	if(p.recruitAppliesToMe(RecruitChip.PeteTheCannibal)
    			&& (p.newWorkers.height()>0)
    			&& (p.totalWorkers>1))
    	{
    		setContinuation(new Continuation(RecruitChip.PeteTheCannibal,
    				Function.DoPeteTheCannibalSacrifice,Function.ReRollSheppardTheLobotomist,p));
    	}
    	else
    	{
    		reRollSheppardTheLobotomist(p,replay);
    	}
    }
    
    private void doPeteTheCannibalSacrifice(EPlayer p,replayMode replay)
    {	
    	// sacrifice a worker, gain some benefits
    	p.payCostOrElse(Cost.SacrificeRetrievedWorker,replay);
    	p.collectBenefitOrElse(Benefit.Foodx4,replay);
    	logGameEvent(UsingPeteTheCannibal);
    	setContinuation(new Continuation(Benefit.Resource,Function.ReRollSheppardTheLobotomist,p));
    }
    private void reRollSheppardTheLobotomist(EPlayer p,replayMode replay)
    {	
    	if((p.totalWorkers>1)
    			&& (p.newWorkers.height()>0)
    			&& p.recruitAppliesToMe(RecruitChip.SheppardTheLobotomist))
    	{
    		setContinuation(new Continuation(RecruitChip.SheppardTheLobotomist,
    				Function.DoSheppardTheLobotomistSacrifice,Function.ProceedWithTheGame,p));
    	} 
    	else { proceedWithTheGame(replay); }
    }
    private void doSheppardTheLobotomistSacrifice(replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	p.collectBenefitOrElse(Benefit.Blissx4,replay);
    	//p1("use sheppard the lobotomist");
    	p.payCostOrElse(Cost.SacrificeRetrievedWorker,replay);
    	logGameEvent(SheppardTheLobotomistSacrifice);
    	setContinuation(new Continuation(Benefit.Resource,	Function.ProceedWithTheGame,p));
    }


    // pay and gain morale
    private void reRollWithPayment(EPlayer p,replayMode replay)
    {	
    	EuphoriaCell payment = droppedDestStack.top();
    	if(payment.rackLocation()==EuphoriaId.EnergyPool)
    	{
    	// jefferson allows you to pay with energy, and changes the benefit
    	Assert(p.recruitAppliesToMe(RecruitChip.JeffersonTheShockArtist),"should be jefferson");
    	p.gainMorale(replay);
    	p.decrementKnowledge(replay);
    	logGameEvent(JeffersonTheShockArtistEffect,currentPlayerColor(),currentPlayerColor());
    	reRoll(p,replay);
    	}
    	else
    	{
    	if((payment.rackLocation()==EuphoriaId.BlissPool)
    		&& p.recruitAppliesToMe(RecruitChip.GidgitTheHypnotist))
    	{	//acceptPlacement();
    		setContinuation(new Continuation(RecruitChip.GidgitTheHypnotist,Function.DoGidgitTheHypnotist,Function.ReRollNormalPayment,p));
    	}
    	else { reRollNormalPayment(p,replay); }
    	}
    }
    private void doGidgitTheHypnotist(EPlayer p,replayMode replay)
    {
    	p.decrementKnowledge(replay);
    	p.decrementKnowledge(replay);
    	logGameEvent(GidgetTheHypnotistEffect,currentPlayerColor(),currentPlayerColor());
    	reRoll(p,replay);
    }
    private void reRollNormalPayment(EPlayer p,replayMode replay)
    {	
    	if(p.gainMorale(replay))
	    	{
    		// not just clean, it also avoids adding explanations twice
			p.gainMorale(replay);
	    	}
		reRoll(p,replay);
	}
    
    // no payment means lose morale
    private void reRollWithoutPayment(EPlayer p,replayMode replay)
    {	
    	int ntolose = 1;
    	if((p.totalWorkers==2)
    			&& p.recruitAppliesToMe(RecruitChip.YordyTheDemotivator)
    			&& p.canPayX(Cost.Morale)
    			)
    	{	logGameEvent(YordySavesTheDay,currentPlayerColor()); 
    		ntolose = 0;	// yordy fixes everything
    	}
    	else if(p.penaltyAppliesToMe(MarketChip.CafeteriaOfNamelessMeat))
        	{	MarketChip.CafeteriaOfNamelessMeat.logGameEvent(this,NamelessMeat,currentPlayerColor()); 
        		ntolose++;
        	}

    	if(p.recruitAppliesToMe(RecruitChip.KofiTheHermit))
    	{
    		//p1("kofi retrieves for free");
    		p.setTFlag(TFlag.TriggerKofiTheHermit);
    	}
     	if(doLoseMorale(p,ntolose,replay,null,Function.ReRollYordyCheck_V2))
    	{
     		reRollYordyCheck_V2(p,replay);
    	}

    }
    private void reRollWithSamuelTheZapper(EPlayer p,replayMode replay)
    {	
    	if(board_state==EuphoriaState.RetrieveCommodityWorkers)
    	{
    	Assert(p.testTFlag(TFlag.UsingSamuelTheZapper), "should be true");
    	if(p.newWorkers.height()>p.samuelTheZapperLevel)
    		{	// if we ultimately used him, the height of newWorkers will increase
    			p.setTFlag(TFlag.UsedSamuelTheZapper); 
    		}}
    	reRoll(p,replay);
    }

    private void reRollYordyCheck_V2(EPlayer p,replayMode replay)
    {	
    	if(p.recruitAppliesToMe(RecruitChip.YordyTheDemotivator_V2)
    			&& p.canPay(Cost.Energy))
    	{
    		setContinuation(new Continuation(RecruitChip.YordyTheDemotivator_V2,
    				Function.DoYordyTheDemotivator_V2,Function.ReRoll,p));
    		return;
    	}
    	reRoll(p,replay);
    }
    
    private void doYordyTheDemotivator_V2(EPlayer p,replayMode replay)
    {	
    	Assert(selectedDieRoll!=null,"die must be selected");
    	p.payCostOrElse(Cost.Energy,replay); 
    	logGameEvent(YordyTheDemotivatorSelects,selectedDieRoll.shortName());
    	reRollWorkers(p,replay,selectedDieRoll,Function.ReRoll);
    }

    //
    // activate recruits that should now be active
    //
    boolean revealHiddenRecruits(replayMode replay)
    {	boolean some = false;
    	for(EPlayer p : players)
    	{	EuphoriaCell c = p.hiddenRecruits;
    		for(int lim = c.height()-1; lim>=0; lim--)
    		{	RecruitChip newRecruit = (RecruitChip)c.chipAtIndex(lim);
    			if(recruitShouldBeActive(newRecruit))
    			{	c.removeChipAtIndex(lim);
    				p.addActiveRecruit(newRecruit,replay);
    				logGameEvent(ActivateRecruitMessage,p.color.name(),newRecruit.name);
    				if(getAllegianceValue(newRecruit.allegiance)>=(AllegianceSteps-1))
    				{
    					p.addAllegianceStar(replay);
    					some = true;
    				}
    			}
    		}
    	}
    	return(some);
    }
    //
    // award a card to all other players, and do morale checks
    //
    private void finishJackoTheArchivist(EPlayer p,replayMode replay)
    {	
		for(EPlayer op : players)
    	{
    		if(op!=p) 
    			{ // give everyone else a card
    			op.collectBenefitOrElse(Benefit.Artifact,replay);
    			logGameEvent(CardFromJacko,s.get(op.color.name()));
    			// doesn't call doCardsGained since the purpose 
    			// is to check for having jacko
    			doMoraleCheck(op,null,replay);
   			};
    	}  
    }
   
    RecruitChip getRecruit()
    {
    	return (RecruitChip)unusedRecruits.removeTop();
    }
    private void doDone(EuphoriaChip chipin,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	openedMarket = null;
    	marketToOpen = (chipin!=null && chipin.isMarket()) ? chipin : null;
        switch(board_state)
        {
        case Resign:
        	win[(whoseTurn+1)%players.length] = true;
    		setState(EuphoriaState.Gameover);
    		break;
        case ConfirmDiscardFactionless:
        	{
        		Assert(p.discardedRecruits.height()>=1,"should be trashed");
        		p.discardedRecruits.reInit();
        		p.reloadNewRecruits(p.spareRecruits);
        		proceedWithTheGame(replay);
        	}
        	break;
        case EphemeralConfirmRecruits:
   		case ConfirmRecruits:
   			p.discardNewRecruits(true);
   			acceptPlacement();
    		proceedWithTheGame(replay);
    		break;
   		case NormalStart:
   			if(!hasReducedRecruits)
   			{
   			acceptPlacement();
     		proceedWithTheGame(replay);
   			}
			break;
   		case ConfirmActivateRecruit:
   			acceptPlacement();
   			doContinuation(replay);
   			break;
   			
   		case ConfirmOneRecruit:
			p.discardNewRecruits(false);
			p.transferDiscardedRecruits(usedRecruits);
			EuphoriaCell top = droppedDestStack.top();
			if(top.rackLocation()==EuphoriaId.PlayerActiveRecruits)
				{RecruitChip newRecruit = (RecruitChip)top.topChip();
				if(getAllegianceValue(newRecruit.allegiance)>=(AllegianceSteps-1))
				{
				p.addAllegianceStar(replay);
				}
				}
			acceptPlacement();
    		proceedWithTheGame(replay);
			break;
   		case CollectOptionalBenefit:
   			{Benefit pend = pendingBenefit();
   			 switch(pend)
				{
				case WaterOrMorale:			// soulless the plumber
					p.incrementMorale(replay);
					logGameEvent(SoullessThePlumberMorale,currentPlayerColor());
					break;
				case MoraleOrEnergy:
					p.incrementMorale(replay);
					logGameEvent(GaryTheElectricianMorale,currentPlayerColor());
					break;
				case KnowledgeOrBliss:
					p.decrementKnowledge(replay);
					logGameEvent(SarineeTheCloudMinerKnowledge,currentPlayerColor());
					break;
				case KnowledgeOrFood:
					p.decrementKnowledge(replay);
					logGameEvent(ScabyTheHarvesterKnowledge,currentPlayerColor());
					break;
				default: throw Error("Not expecting no benefit for %s",pend);
				}
   			acceptPlacement();
   			doContinuation(replay);
   			}
   			break;
   		case ConfirmBenefit:
   			{
   			Benefit pend = pendingBenefit();
  			p.satisfyBenefit(replay,pend,droppedDestStack);
  			while(marketBasket.height()>0)
  			{
 			// throw away the rejected card
  			EuphoriaChip discard = marketBasket.removeTop();
  			if((discard!=null)&&(discard.isArtifact())) { recycleArtifact(discard); }
 			if(replay!=replayMode.Replay) { animateReturnArtifact(marketBasket); }
   			}
  			if(isIIB() && (pend!=Benefit.FreeArtifact) && (pend!=Benefit.FreeArtifactOrResource))
  			{	// check for needing to pay for a card
  				for(int i=pickedSourceStack.size()-1; i>=0; i--)
  				{
  					EuphoriaCell c = pickedSourceStack.elementAt(i);
  					if(c.rackLocation()==EuphoriaId.ArtifactBazaar)
  						{	// market shouldn't have been refilled yet
			   				Assert(c.topChip()==null,"artifact market already refilled");
  				   			Cost residual = p.payCost(c,replay);
  				   			if(residual==null)
  				   			{	if(p.recruitAppliesToMe(RecruitChip.PamhidzaiTheReader))
  				   				{
  				   				EuphoriaChip topa = p.artifacts.topChip();
  				   				if((topa==ArtifactChip.Book)||(topa==ArtifactChip.Bifocals))
  				   				{
  				   				//p1("use pamhidzai free");
  				   				finishContinuation(replay);
  				   				setContinuation(new Continuation(RecruitChip.PamhidzaiTheReader,c,Function.DoPamhidzai,Function.DontPamhidzai,p));
  				   				setState(EuphoriaState.RecruitOption);
  				   				return;
  				   				}}
  				   				
  				   			}
  				   			else
  				   			{	
  				   				setContinuation(new Continuation(residual,c,Function.PayForCard,p));
  				   				setState(EuphoriaState.PayCost);
  				   				acceptPlacement();
  				   				return;
  				   			}
   				}}
  			}
 			refillArtifactMarket(replay); 
   			acceptPlacement();
   			doContinuation(replay); 			
   			}
   			break;
   		case RecruitOption:			// no recruit action
   		case DieSelectOption:		// AmandaTheBroker no action
   		case PayForOptionalEffect:
   			acceptPlacement();
   			doUnpaidContinuation(replay);
   			break;
   		case ConfirmRecruitOption:
   			acceptPlacement();
   			doContinuation(replay);
   			break;
   		case ConfirmPayForOptionalEffect:
   			doContinuation(replay);
   			acceptPlacement();
   			break;
   			
        case ConfirmUseMwicheOrContinue:
   		case PayCost:
   				p.confirmPayment(originalPendingCost(),pendingCost(),droppedDestStack,replay);
  				acceptPlacement();
   				doUnpaidContinuation(replay);
    			break;
    			
    			
   		case ConfirmPayCost:
   			{	
				p.confirmPayment(originalPendingCost(),pendingCost(),droppedDestStack,replay);
   				acceptPlacement();

  				doContinuation(replay);
   			}
    			break;
  		
   		case ConfirmPlace:
   			{
   			EuphoriaCell dest = getDest();
   			if(dest==null)
   			{
   				Error("should be something placed");
   			}
   			acceptPlacement();		// empty the interaction stack
   			dropWorker(dest,replay);
   			}
   			break;
   		case PlaceAnother:	// hit Done without placing another worker.
   			doublesElgible = null;
   			usingDoubles = false;
			//$FALL-THROUGH$
		case PlaceNew:		// place the new worker, only from ChaseTheMiner
   			acceptPlacement();		
			//$FALL-THROUGH$
		case DumbKoff:
   			proceedWithTheGame(replay);
   			break;
		case PayForBorna:
			p.setTFlag(TFlag.UsedBornaTheStoryteller);
			acceptPlacement();
			setState(EuphoriaState.ExtendedBenefit);
			setContinuation(new Continuation(Benefit.FreeArtifactOrResource,Function.Return,p));
			break;
		case PayForLionel:
			p.setTFlag(TFlag.UsedLionelTheCook);
			logGameEvent(UsedLionelTheCook);
			useRecruit(RecruitChip.LionelTheCook,"pay food");
			//$FALL-THROUGH$
		case DiscardResources:
	   		acceptPlacement();
    		doContinuation(replay);
   			break;
   			
   		// IIB only, retrieve only from commodity spaces
		case ConfirmBump:
			EuphoriaCell dest = getDest();
			Assert(dest!=null,"no dest");
			bumpedWorker = (WorkerChip)dest.topChip();
			bumpingWorker = (WorkerChip)lastDroppedWorker.topChip();
			setState(EuphoriaState.ExtendedBenefit);
			//return;
			//reRollBumpedWorker(dest,replay);
   			//acceptPlacement();	
   			//doContinuation(replay);
   			//finishContinuation(replay);
   			//setState(EuphoriaState.ExtendedBenefit);
  			break;
   			
		case RetrieveCommodityWorkers:
  			reRollWithSamuelTheZapper(p,replay);
   			break;
  		case Retrieve1OrConfirm:	// no rolling needed
  			proceedWithTheGame(replay);
  			break;
 		case RetrieveOrConfirm:
   		case ConfirmRetrieve:
        	{	
        		if(p.testTFlag(TFlag.UsingSamuelTheZapper))
        		{
        			reRollWithSamuelTheZapper(p,replay);
        		}
        		else
        		{
        		if(p.recruitAppliesToMe(RecruitChip.MaggieTheOutlaw))
        		{
        			// check to see if all retrieved workers are from subterra
        			boolean maggieTheOutlawAvailable = true;
        			for(int lim=pickedSourceStack.size()-1; lim>=0; lim--)
        			{
        				EuphoriaCell c = pickedSourceStack.elementAt(lim);
        				maggieTheOutlawAvailable &= (c.allegiance==Allegiance.Subterran);
        			}
        			if(maggieTheOutlawAvailable) { p.setTFlag(TFlag.TriggerMaggieTheOutlaw); }
        		}
        		acceptPlacement();
        		// retrieval cost can be modified
        		if(p.canPay(Cost.BlissOrFoodRetrieval))
        		{	Cost cost = p.alternateCostWithRecruits(null,Cost.BlissOrFoodRetrieval,true);
        			setContinuation(new Continuation(Cost.BlissOrFoodRetrieval,cost,Function.ReRollWithPayment,
        								Function.ReRollWithoutPayment,p));
         		}
        		else 
        		{
        		// can't pay
        		reRollWithoutPayment(p,replay);
        		}}
        	}
        	break;
        case Puzzle:
            acceptPlacement();
            doublesElgible = null;
            proceedGameStep = ProceedStep.Start;
            proceedWithTheGame(replay);
            break;
            
        case ConfirmJoinTheEstablishment:
        	// give the man a star
        	{
         	p.setPFlag(PFlag.HasResolvedDilemma);
        	p.dilemma.addChip(p.getAuthorityToken(replay));
        	if(replay!=replayMode.Replay)
        	{
        		animatePlacedItem(p.authority,p.dilemma);
        	}
        	acceptPlacement();
        	proceedWithTheGame(replay);
        	}
        	break;
        case ConfirmFightTheOpressor:
        	{
           	p.setPFlag(PFlag.HasResolvedDilemma);
           	p.newRecruits[0].reInit();
           	p.newRecruits[1].reInit();
           	p.newRecruits[0].addChip(getRecruit());
           	p.newRecruits[1].addChip(getRecruit());
          	acceptPlacement();
         	setState(EuphoriaState.ChooseOneRecruit);
        	}
        	break;
        case ConfirmUseJackoOrContinue:
        case ConfirmUseJacko:
        	{
         	acceptPlacement();
         	if(revision<117)
         		{ setState(EuphoriaState.ConfirmPayCost);
         	      p.payCostOrElse(Cost.Knowledgex2,replay);
         		}
         	if(!GAME_EU_Dumbot_Brius_2014_12_20_2045)
         		{ // game EU-Dumbot-Brius-2014-12-20-2045 replay was broken by making this doContinuation unconditional
         		  // this was an intermediate bug that slipped through without a revision number
         		doContinuation(replay); 
         		}
        	finishJackoTheArchivist(p,replay);
        	}
        	break;
        case RePlace:	// dumbkoff move by the robot
        	break;
        default:
        	throw Error("Not expecting state %s",board_state);
        }
        if(revealHiddenRecruits(replay))
        {
        	checkGameOver();
        }
    	if(board_state==EuphoriaState.ExtendedBenefit)
    	{	// this happens if StevenTheScholar requires a late benefit dialog, but there are no
    		// other penalties or benefits associated.
         	acceptPlacement();
    		doContinuation(replay);
    	}
        if((revision>=120) && revealHiddenRecruits(replay))
        {	// continuations above can incremement allegiance tracks and should cause
        	// recruits to be revaled.  This was pointed out for game EU-Umbrage-Dumbot-2015-06-12-1527 
        	// at move 37.
        	checkGameOver();
        }
        checkKofiTheHermit(replay,Function.Return);
     }
    
    void doPamhidzai(EPlayer p,EuphoriaCell dest,replayMode replay)
    {
    	// give back the artifact
    	p.returnArtifact(dest,replay);
    	setContinuation(new Continuation(Benefit.Commodityx3,Function.Return,p));  
    	setState(EuphoriaState.CollectBenefit);
    }
    void dontPamhidzai(replayMode replay)
    {
    	refillArtifactMarket(replay);
    	doContinuation(replay);
    }

    // count the bliss or food items in a stack
    int countBlissOrFood(CellStack stack)
	{
    	int blissorfood = 0;
		for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
			{
			EuphoriaCell c = droppedDestStack.elementAt(lim);
			if(c==farm) { blissorfood++; }
			else if(c==bliss) { blissorfood++; }
			
			}
		return(blissorfood);
	}
    
    // true of the stack contains any food items
    boolean containsFood(CellStack stack)
    {
    	for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
		{
		EuphoriaCell c = droppedDestStack.elementAt(lim);
		if(c==farm) { return(true); }		
		}
    	return(false);
    }
    // true of the stack contains any food items
    int containsNFood(CellStack stack)
    {	int n=0;
    	for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
		{
		EuphoriaCell c = droppedDestStack.elementAt(lim);
		if(c==farm) { n++; }		
		}
    	return(n);
    } 
    
    // return true if the currently played items complete the benefit
    // this works cooperatively with the move generator, and the whole
    // process depends on this moving from "collect benefit" to "confirm benefit"
    // at the right moment.
    private boolean boardSatisfiesBenefit(Benefit bene)
    {     
    	EuphoriaCell dest = getDest();
 		if(dest.rackLocation()==EuphoriaId.GenericSink) { return true; }
 		switch(bene)
		{
		case EuphorianAuthority2:
		case WastelanderAuthority2:
		case SubterranAuthority2:
		case EuphorianAuthorityAndInfluenceA:
		case WastelanderAuthorityAndInfluenceA:
		case SubterranAuthorityAndInfluenceA:
		case EuphorianAuthorityAndInfluenceB:
		case WastelanderAuthorityAndInfluenceB:
		case SubterranAuthorityAndInfluenceB:
		
		case CardOrClay:
		case CardOrGold:
		case CardAndGold:
		case CardAndClay:
		case CardAndStone:	// the "and" cases only occur when the ministry of personal secrets is in effect
		case Resource:
		case WaterOrStone:
		case CardOrStone:
		case KnowledgeOrFood:
		case KnowledgeOrBliss:
		case WaterOrEnergy:
		case WaterOrMorale:
		case Commodity:
		case MoraleOrEnergy:
		case Artifactx2for1:
		case MoraleOrKnowledge:
		case Moralex2OrKnowledgex2:
		case IcariteInfluenceAndCardx2:	// need 2 cards, but select and pay one at a time.
		case IcariteAuthorityAndInfluence:	// need a card for an icarite recruit
		case Artifact:
		case FirstArtifact:
		case FreeArtifact:
		case ResourceOrCommodity:
		case StoneOrWater:
		case GoldOrEnergy:
		case ClayOrFood:
		case FreeArtifactOrResource:
			// need only one of some permitted thing
			Assert(droppedDestStack.size()<=1,"too many items placed");
			return (droppedDestStack.size()==1);
			
		case IcariteInfluenceAndResourcex2:
		case ResourceAndCommodity:
		case Resourcex2:
   		case Commodityx2:
 			Assert(droppedDestStack.size()<=2,"too many items placed");
			if(droppedDestStack.size()==2) { return true; }
			break;
   		case Commodityx3:
   		case Resourcex3:
			Assert(droppedDestStack.size()<=3,"too many items placed");
			if(droppedDestStack.size()==3) { return true; }
			break;
  		case Resourcex4:
			Assert(droppedDestStack.size()<=4,"too many items placed");
			if(droppedDestStack.size()==4) { return true; }
			break;
  		case Resourcex5:
			Assert(droppedDestStack.size()<=5,"too many items placed");
			if(droppedDestStack.size()==5) { return true; }
			break;
  		case Resourcex6:
			Assert(droppedDestStack.size()<=6,"too many items placed");
			if(droppedDestStack.size()==6) { return true; }
			break;
  		case Resourcex7:
			Assert(droppedDestStack.size()<=7,"too many items placed");
			if(droppedDestStack.size()==7) { return true; }
			break;
  		case Resourcex8:
			Assert(droppedDestStack.size()<=8,"too many items placed");
			if(droppedDestStack.size()==8) { return true; }
			break;
  		case Resourcex9:
			Assert(droppedDestStack.size()<=9,"too many items placed");
			if(droppedDestStack.size()==9) { return true; }
			break;
		
		case ArtifactOrWaterX2:
		case ArtifactOrFoodX2:
		case ArtifactOrBlissX2:
		case ArtifactOrEnergyX2:
		case ArtifactOrGoldOrEnergyX2:
		case ArtifactOrClayOrFoodX2:
		case ArtifactOrStoneOrWaterX2:
			// take an artifact or a mix of 2 resources and commodities
			Assert(droppedDestStack.size()<=2,"too many items placed");
			if(hasTakenArtifact()) { return(true); }
 			if(droppedDestStack.size()==2) { return(true); }
 			break;
		case ArtifactOrEnergyX3:
		case ArtifactOrWaterX3:
		case ArtifactOrBlissX3:
		case ArtifactOrFoodX3:
		case ArtifactOrGoldOrEnergyX3:
		case ArtifactOrClayOrFoodX3:
		case ArtifactOrStoneOrWaterX3:
			// take an artifact or a mix of 3 resources and commodities
			Assert(droppedDestStack.size()<=3,"too many items placed");
			if(hasTakenArtifact()) { return(true); }
 			if(droppedDestStack.size()==3) { return(true); }
			break;
			
		default: throw Error("Not expecting pending benefit %s",bene);
		}
 		return false;
 }
    private boolean hasPaidArtifactPair(EPlayer p)
    {	return p.hasPaidArtifactPair(droppedDestStack,usedArtifacts);
    }
  
    //
    // board satisfies cost.  The environment will only play moves
    // that legally contrinbute toward the cost, so usually only
    // the number of items on the stack is relevant.  The big exception
    // is artifacts, where either a pair or 3 cards of any type.
    //
    // special side effect, set "optionalSatisfaction" if the user could stop here
    // or continue with an additional payment. This is needed for Mwiche The Flusher
    // who lets you pay 3 water for special treatment when 1 water is also valid.
    //
    private boolean optionalSatisfaction = false;
    private boolean boardSatisfiesCost(EPlayer p,Cost cost)
    {	int nCards=0;
    	int nCommodities=0;
    	optionalSatisfaction = false;
 		switch(cost)
		{
		case BlissOrFree:
 			 optionalSatisfaction = !hasPaidSomething(); 
 			 return true;

 		case IsEuphorianAndCommodity:
 		case IsWastelanderAndCommodity:
 		case IsSubterranAndCommodity:
 		case Energyx4_Card:			// reduced to just a card because we already paid the energy
 		case GoldOrFoodOrBliss:
 		case ClayOrFoodOrBliss:
 		case StoneOrFoodOrBliss:
 		case ClayOrBliss:
 		case StoneOrBliss:
 		case GoldOrBliss:
 		case BlissOrFoodRetrieval:
 		case BlissOrFoodExactly:
 		case BlissOrWater:
 		case BlissOrEnergy:
 		case ResourceOrBlissOrFood:
		case NonBlissCommodity:
		case Stone:
		case Gold:
		case GoldOrFood:
		case ResourceOrBliss:
		case ResourceAndKnowledgeAndMorale:
		case ResourceAndKnowledgeAndMoraleOrArtifact:	// michael the engineer and flartner the luddite
		case GoldOrArtifact:
		case StoneOrArtifact:
		case ClayOrArtifact:
		case Bifocals:
		case Box:
		case Bear:
		case Balloons:
		case Bat:
		case Book:
		case SacrificeAvailableWorker:
		case SacrificeOrClay:
		case SacrificeOrGold:
		case SacrificeOrStone:
		case StoneOrFood:
		case StoneOrBlissOrFood:
		case ClayOrBlissOrFood:
		case ClayOrFood:
		case FoodOrResource:
			return(droppedDestStack.size()==1);
			
		case SacrificeOrGoldOrCommodityX3:
		case SacrificeOrStoneOrCommodityX3:
		case SacrificeOrClayOrCommodityX3:
		case SacrificeOrCommodityX3:
			{
			int na=droppedDestStack.size();
			return ((na==3)
					|| (na==1 && !hasPaidCommodity()));
			}
	
		case ArtifactPair:
			{
			int na = numberOfArtifactsPaid();
			return( (na==2) && hasPaidArtifactPair(p));
			}
			
		case ArtifactJackoTheArchivist_V2:
			if(droppedDestStack.size()==1) 
				{ return(true); }
			// otherwise fall into normal artifactx3 test
			//$FALL-THROUGH$
		case Artifactx3:
		case Morale_Artifactx3_Brian:
			if(p.penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement))
			{
				return(boardSatisfiesCost(p,Cost.Artifactx3Only));
			}
			return(boardSatisfiesCost(p,Cost.ArtifactPair) || (numberOfArtifactsPaid()==3));
			
		case ArtifactX3AndCommodity:
			return(hasPaidCommodity()
					&& 	boardSatisfiesCost(p,Cost.Artifactx3));

		case Artifactx3Only:		// various markets.  Usually take 3 cards or a pair
		case Resourcex3:			// nimbus loft
		case CommodityX3:			// jon the amateur handyman
		case Morale_Resourcex3_Brian:
		case Book_CardAndCommodity:
			return(droppedDestStack.size()==3);
			
		case Artifactx2:
		case Card_ResourceOrBlissOrFood:
		case Card_ResourceOrBliss:
		case Card_Resource:
		case CommodityX2:
		case Card_FoodOrResource:
			
			return(droppedDestStack.size()==2);
			
		case Commodity:
		case Resource:
		case NonBliss:
			// we need 1 of something the player chooses
			return(droppedDestStack.size()==1);
		
		// dilemma costs
		case BatOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Bat)
					|| (droppedDestStack.size()==2));
			
		case BearOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Bear)
					|| (droppedDestStack.size()==2));
			
		case BoxOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Box)
					|| (droppedDestStack.size()==2));
			
		case BalloonsOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Balloons)
					|| (droppedDestStack.size()==2));
			
		case BifocalsOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Bifocals)
					|| (droppedDestStack.size()==2));
			
		case BookOrCardx2:
			return( hasPaidArtifact(p,ArtifactChip.Book)
					|| (droppedDestStack.size()==2));

		case BlissOrFoodx4_ResourceOrBlissOrFood:
			return(droppedDestStack.size()==5);
			
		case Cardx6:	nCards++;	// keep dropping
			//$FALL-THROUGH$
		case Cardx5:	nCards++;	// keep dropping 
			//$FALL-THROUGH$
		case Cardx4:	nCards++;	// keep dropping
			//$FALL-THROUGH$
		case Cardx3:	nCards++;	// keep dropping
			//$FALL-THROUGH$
		case CardForGeekx2:
		case Cardx2:	nCards++;	// keep dropping
			//$FALL-THROUGH$
		case Card:	// last drop
		case Artifact:
		case CardForGeek:
			return(droppedDestStack.size()>nCards);
		case BlissOrFoodPlus1:
		case Morale_BlissOrFoodPlus1_Brian:
			return(droppedDestStack.size()==2);
		case BlissOrFoodx4_Card:
		case BlissOrFoodx4_Resource:
			{
			int blissorfood = countBlissOrFood(droppedDestStack);
			int resource = droppedDestStack.size()-blissorfood;
			return((blissorfood==4) && (resource==1));
			}
		case CommodityOrResourcex4Penalty: nCommodities++;
			//$FALL-THROUGH$
		case CommodityOrResourcex3Penalty: nCommodities++;
			//$FALL-THROUGH$
		case CommodityOrResourcex2Penalty: nCommodities++;
			//$FALL-THROUGH$
		case CommodityOrResourcePenalty: nCommodities++;
			return(droppedDestStack.size()==nCommodities);
		// iib payments
		case Book_Card:
		case Commodity_Artifact:
		case FoodAndCommodity:
		case EnergyAndCommodity:
		case WaterAndCommodity:
		case BlissOrWaterAndCommodity:
		case BlissOrFoodAndCommodity:
		case BlissOrEnergyAndCommodity:
		case NonBlissAndCommodity:
		case Commodity_Book:
		case Commodity_Bear:
		case Commodity_Bifocals:
		case Commodity_Bat:
		case Commodity_Box:
		case Commodity_Balloons:
		case Card_BlissOrFood:
			return(droppedDestStack.size()==2);
			
		case FreeOrWaterMwicheTheFlusher:
			if(boardSatisfiesCost(p,Cost.WaterMwicheTheFlusher)) { return(true); }
			optionalSatisfaction = !hasPaidSomething(); 
			return(false);
		case FreeOrEnergyMwicheTheFlusher:
			if(boardSatisfiesCost(p,Cost.EnergyMwicheTheFlusher)) { return(true); }
			optionalSatisfaction = !hasPaidSomething(); 
			return(false);
		case FreeOrFoodMwicheTheFlusher:
			if(boardSatisfiesCost(p,Cost.FoodMwicheTheFlusher)) { return(true); }
			optionalSatisfaction = !hasPaidSomething(); 
			return(false);
	
		case BlissOrWaterMwicheTheFlusher:
			if(hasPaid(bliss)) { return(true); }
			//$FALL-THROUGH$
		case WaterMwicheTheFlusher:
			{
			//p1("water mwiche the flusher");// 1 or 3 ought to be acceptable
			int sz = droppedDestStack.size();
			optionalSatisfaction = (sz==1);
			return ((sz==1)||(sz==3));
			}
		case BlissOrFreeMwicheTheFlusher:
			optionalSatisfaction = !hasPaidSomething();
			//$FALL-THROUGH$
		case BlissOrFoodMwicheTheFlusher:
			if(hasPaid(bliss)) { return(true); }
			//$FALL-THROUGH$
		case FoodMwicheTheFlusher:
			{
			int sz = droppedDestStack.size();
			if((sz==1) && hasPaid(farm)) { return(true); }
			return (sz==3);
			}
		case BlissOrEnergyMwicheTheFlusher:
			if(hasPaid(bliss)) { return(true); }
			//$FALL-THROUGH$
		case EnergyMwicheTheFlusher:
			{
			int sz = droppedDestStack.size();
			if((sz==1) && hasPaid(generator)) { return(true); }
			return (sz==3);
			}
			
		case WaterMwicheTheFlusherAndCommodity:
		{	// energy+commodity
			// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) && hasPaid(aquifer)) { optionalSatisfaction = true; return(true); }
			return (sz==4);
		}
			
		case FoodMwicheTheFlusherAndCommodity:
		{	// energy+commodity
			// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) && hasPaid(farm)) 
				{ optionalSatisfaction = hasPaid(aquifer); return(true); }
			return (sz==4);
		}
		case BlissOrFoodMwicheTheFlusherAndCommodity:
		{	// bliss+commodity or energy+commodity
			// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) && (hasPaid(bliss)||hasPaid(farm)))
				{ optionalSatisfaction = hasPaid(aquifer); return(true); }
			return (sz==4);
		}
		
		case BlissOrWaterMwicheTheFlusherAndCommodity:
		{	// bliss+commodity or energy+commodity
			// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) && (hasPaid(bliss)||hasPaid(aquifer))) 
					{ optionalSatisfaction = hasPaid(aquifer); return(true); }
			return (sz==4);
		}

		case BlissOrEnergyMwicheTheFlusherAndCommodity:
		{	// bliss+commodity or energy+commodity
			// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) 
					&& (hasPaid(bliss)||hasPaid(generator))) 
						{ optionalSatisfaction = hasPaid(aquifer) ; return(true); }
			return (sz==4);
		}
		case BlissMwicheTheFlusherAndCommodity:
			{	// bliss+commodity
				// 3 water + commodity
			int sz = droppedDestStack.size();
			if((sz==2) && hasPaid(bliss)) 
				{ optionalSatisfaction = hasPaid(aquifer); return(true); }
			return (sz==4);
		}
			
		case EnergyMwicheTheFlusherAndCommodity:
			{	// energy+commodity
				// 3 water + commodity
				int sz = droppedDestStack.size();
				if((sz==2) && hasPaid(generator))
					{ optionalSatisfaction = hasPaid(aquifer); return(true); }
				return (sz==4);
			}
			
		case Energyx3OrBlissx3:
		case Waterx3OrBlissx3:
		case BlissAndNonBlissAndCommodity:
			return(droppedDestStack.size()==3);
			
		case Energyx3OrBlissx3AndCommodity:
		case Waterx3OrBlissx3AndCommodity:
			return(droppedDestStack.size()==4);
			
		case BlissOrFoodx4:
		case ResourceX3AndCommodity:
			return(droppedDestStack.size()==4);	// all or nothing was prepaid
		
		case ClayOrCommodityX3:
		case StoneOrCommodityX3:
		case GoldOrCommodityX3:
			{
			int sz = droppedDestStack.size();
			return ((sz==3) || ((sz==1) && hasPaidResource()));
			}
		case Artifactx3OrArtifactAndBlissx2:
			return ((droppedDestStack.size()==3)
					|| boardSatisfiesCost(p,Cost.ArtifactPair));
		case Artifactx3OrArtifactAndBlissx2AndCommodity:
			{
			int na = numberOfArtifactsPaid();
			int nc = numberCommoditiesPaid();
			if(na==3 || hasPaidArtifactPair(p)) { return (nc==1); }
			else {
				int nb = numberPaid(bliss);
				if((nb>=2) && (nc==3) && (na==1)) { return(true); }
				}
			}
			return false;
		case ArtifactAndBlissx2AndCommodity:
			return ( (droppedDestStack.size()==4)
					&& (numberOfArtifactsPaid()==1)
					&& (numberPaid(bliss)==2)
					&& (numberCommoditiesPaid()==3));

		default: throw Error("Not expecting placement cost %s",cost);
		}
    }
    private boolean couldPayWithoutJacko(EPlayer p)
    {	// guy has placed one artifact on the stack, which could be the
    	// payment for jacko the archivist.  If there are sufficient cards
    	// still available, he could continue placing cards
    	EuphoriaCell ar = p.artifacts;
    	if(ar.height()==0) { return(false); }	// has no more 
    	if(ar.height()>=2) { return(true); }	// can add two more
    	// has one more, if it's a pair and he can use pairs
    	return( (ar.topChip()==droppedDestStack.top().topChip())	// it's a pair
    			&& !p.penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement));
    }
    private void setNextStateAfterDrop(EPlayer p)
    {	switch(board_state)
    	{
    	default: throw Error("not expecting drop in state %s",board_state);
    	case PayForLionel:
    	case PayForBorna:
    		break;
    	case Retrieve1OrConfirm:
    		setState(EuphoriaState.ConfirmRetrieve);
    		break;
     	case PlaceOrRetrieve:
     	case RetrieveCommodityWorkers:
    	case Retrieve:
    	case RetrieveOrConfirm:
    	case Place:
    	case RePlace:
    	case PlaceNew:
    	case PlaceAnother:
    		{
    		EuphoriaCell dest = getDest();
    		EuphoriaId rack = dest.rackLocation();
    		if(rack.perPlayer)
    			{	// destination is the player board, we must be retrieving
    				if(p.testTFlag(TFlag.UsingSamuelTheZapper))
    				{
    					if(!p.hasCommodityWorkers()) { setState(EuphoriaState.ConfirmRetrieve); }
    				}
    				else if(p.hasWorkersOnBoard())
    				{
    					setState(EuphoriaState.RetrieveOrConfirm);
    				}
    				else { setState(EuphoriaState.ConfirmRetrieve); }
    			}
    			else
    			{	// destination is the main board, we must be placing
    			setState(EuphoriaState.ConfirmPlace);
    			}
    		}
    		break;
    	case ReUseWorker:
    		setState(EuphoriaState.ConfirmPlace);
    		break;
    	case BumpOpponent:
    		setState(EuphoriaState.ConfirmBump); 
    		break;
    	case PayForOptionalEffect:
    		// any acceptable payment is enough
    		setState(EuphoriaState.ConfirmPayForOptionalEffect);
    		break;
 
    		
   	case FightTheOpressor:
   	case JoinTheEstablishment:
   	case PayCost:
        		// pay cost through a zoomed view of the board
       	{	Cost cost = pendingCost();
       		if(boardSatisfiesCost(p, cost ))
       		{	
       			if(optionalSatisfaction)
       			{	// using mwiche on the aquifer, everything is water.
       				setState(EuphoriaState.ConfirmUseMwicheOrContinue);
       			}
       			else if((cost==Cost.ArtifactJackoTheArchivist_V2)
       				&& (droppedDestStack.size()==1))
       			{	if(revision<117) { Assert(p.knowledge<=4,"knowledge check"); }
       				setState(couldPayWithoutJacko(p)?EuphoriaState.ConfirmUseJackoOrContinue:EuphoriaState.ConfirmUseJacko);
       			}
       			else 
       			{ setState(EuphoriaState.ConfirmPayCost); 
       			}
       		}
       		}
       		break;
   		case ActivateOneRecruit:
   			setState(EuphoriaState.ConfirmActivateRecruit);
   			break;
       	case ChooseOneRecruit:
       		setState(EuphoriaState.ConfirmOneRecruit);
       		break;
     	case CollectBenefit:
     	case CollectOptionalBenefit:
     		{	
     		Benefit bene = pendingBenefit();
     		if(boardSatisfiesBenefit(bene))
	     		{
	     			setState(EuphoriaState.ConfirmBenefit);
	     		}
 
     		}
    		break;
    	case NormalStart:
     		acceptPlacement();
     		break;
     	case EphemeralConfirmRecruits:
     	case EphemeralChooseRecruits:
     	case EphemeralDiscardFactionless:
     	case ConfirmRecruits:
    	case ConfirmDiscardFactionless:
     		acceptPlacement();
     		/*$FALL-THROUGH$*/
    	case ChooseRecruits:
    	case DiscardFactionless:
    		setRecruitDialogState(p);
    		break;
    	case DiscardResources:
    		break;
    	case Puzzle: acceptPlacement(); 
    		break;
    	}
    }
    void startLionelTheCook()
    {
    	setContinuation(new Continuation(Function.Return));
    	setState(EuphoriaState.PayForLionel);
    }
    void startBornaTheStoryteller()
    {
    	setContinuation(new Continuation(Function.Return));
    	setState(EuphoriaState.PayForBorna);
    }
    void startDiscarding()
    {
    	setContinuation(new Continuation(Function.Return));
    	setState(EuphoriaState.DiscardResources);
    }
    private void setNextStateAfterPick(EPlayer p)
    {	   	
    	switch(board_state)
    	{
    	default: Error("not expecting pick in state %s",board_state);
    		break;
     	case CollectBenefit:
    	case CollectOptionalBenefit:
    		EuphoriaId rack = getSource().rackLocation();
    		if(rack.perPlayer && rack.isResourceCell()) { startDiscarding(); }
    		break;
     	case ConfirmUseMwicheOrContinue:
     	case ConfirmUseJackoOrContinue:
    	case ConfirmUseJacko:
    		setState(EuphoriaState.PayCost);
    		break;

    	case EphemeralDiscardFactionless:
     	case EphemeralChooseRecruits:
     	case NormalStart:
     		break;
     	case ConfirmDiscardFactionless:
    	case ConfirmRecruits:
    	case EphemeralConfirmRecruits:
    		setRecruitDialogState(p);
    		break;
    		
    	case ReUseWorker:
    	case PlaceOrRetrieve:
       	case RePlace:
    	case Place:
    	case PlaceAnother:
    	case PlaceNew:
    	case RetrieveOrConfirm:
    	case Retrieve:
    	case Retrieve1OrConfirm:
    	case RetrieveCommodityWorkers:
    		if(pickedObject==EuphoriaChip.Food) { startLionelTheCook(); }
    		else if(pickedObject.isArtifact()) { startBornaTheStoryteller(); }
    		else if(!pickedObject.isWorker()) { startDiscarding(); }
    		else
    		{// a worker for sure
    		switch(board_state)
    			{	default: break;
    				case ReUseWorker: 
    					reUsingWorker = true;
    					setState(EuphoriaState.Place); 
    					break;
    				case PlaceOrRetrieve:
    					{
    					EuphoriaCell c = getSource();
    		    		if(c.rackLocation().perPlayer) 
    		    			{
    		    			// picked from the player board (it must be a worker)
    		    			setState(EuphoriaState.Place);
    		    			}
    		    		else
    		    			{	// picked from the board (it must be a worker to retrieve)
    		    			setState(EuphoriaState.Retrieve);
    		    			}
    					}
    			}
    		}
    		break;
    		
    	case ChooseRecruits:
    	case DiscardFactionless:
    	case ChooseOneRecruit:
     	case PayForOptionalEffect:
    	case PayCost:
    	case JoinTheEstablishment:
    	case FightTheOpressor:
      	case Puzzle:  
      	case BumpOpponent:
      	case DiscardResources:
    	case ActivateOneRecruit:

    		break;
    	}
    }

    public void reRollWorkers(EPlayer p,replayMode replay,WorkerChip chosenValue,Function continuation)
    {	
    	
    	if((bumpedWorker!=null) 
    		&&(bumpedWorker.color == p.color)
    		&& p.recruitAppliesToMe(RecruitChip.JuliaTheAcolyte))
    	{	Assert(chosenValue==null,"not chosen yet");
    		//p1("ask julia");
    		// when bumped, can choose to keep the same value or take the value of the bumper
    		setContinuation(new Continuation(RecruitChip.JuliaTheAcolyte,
    				Function.DoJuliaTheAcolyte,Function.DontJuliaTheAcolyte,continuation,p));
    		return;
    	}
    	reRollWorkersAfterJulia(p,replay,chosenValue,continuation);
    }
    	
    private void reRollWorkersAfterJulia(EPlayer p,replayMode replay,WorkerChip chosenValue,Function continuation)
    {
    	if(continuation!=null) { setContinuation(new Continuation(EuphoriaState.ExtendedBenefit, continuation,p)); }
    	int nworkers = p.newWorkers.height();
    	if((nworkers>=2)
    			&& p.recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
    	{	logGameExplanation(BonusChristineTheAnarchist,""+(nworkers-1));
    		//p1("use christine the anarchist");
    		useRecruit(RecruitChip.ChristineTheAnarchist,"bonus");
    		setContinuation(new Continuation(MultipleCommodities[nworkers-2],Function.ReRollWorkersAfterChristine,chosenValue,p));
    		return;
    	}
    	rerollWorkersAfterChristine(p,replay,chosenValue);
    }
    
    private void rerollWorkersAfterChristine(EPlayer p,replayMode replay,WorkerChip chosenValue)
    {	
    	if((p.totalResources()>0)
    			&& (p.newWorkers.height()>=3)
    			&& p.recruitAppliesToMe(RecruitChip.JeroenTheHoarder))
    	{	//p1("use jeroen the hoarder "+dest);
    		logGameExplanation("get resource (Jeroen the Hoarder)");
    		useRecruit(RecruitChip.JeroenTheHoarder,"use");
    				
    		setContinuation(new Continuation(Benefit.Resource,Function.ReRollWorkersAfterJeroen,chosenValue,p));
    		return;
    	}
    	rerollWorkersAfterJeroen(p,replay,chosenValue);
    }
    private void rerollWorkersAfterJeroen(EPlayer p,replayMode replay,WorkerChip chosenValue)
    {

    	if(p.recruitAppliesToMe(RecruitChip.MilosTheBrainwasher)
    		&& (p.canPayX(Cost.Water)))
    	{	
    		setContinuation(new Continuation(RecruitChip.MilosTheBrainwasher,Function.DoMilosTheBrainwasher,Function.ReRollWorkersAfterMilos,chosenValue,p));
    		setWhoseTurnTo(p.boardIndex,"MilosTheBrainwasher");
    		return;
    	}
    	reRollWorkersAfterMilos(p,replay,chosenValue,false);
    }
    
   // enter here only at the very first roll after choosing recruits
   public void initialRerollWorkers(EPlayer p,replayMode replay)
    {
    	reRollWorkersAfterMilos(p,replay,null,true);
    }
    private void doMilosTheBrainwasher(EPlayer p,replayMode replay,EuphoriaCell dest,WorkerChip chosenValue)
    {
    	p.payCostOrElse(Cost.Water,replay);		
    	reRollWorkersAfterMilos(p,replay,chosenValue,true);
    }
    
    private void actualReroll(EPlayer p,int numberToRoll,WorkerChip chosenValue,replayMode replay)
    {
    	if(numberToRoll>0)
    	{
    	Random r = newRandomizer(0x72525+p.boardIndex);
    	p.clearTFlag(TFlag.ResultsInDoubles);
    	while(numberToRoll-->0)
    	{	int newvalue = Random.nextInt(r,6)+1;
    		// leave the original workers there in case of HighGeneralBaron
     		WorkerChip newWorker = chosenValue!=null ? chosenValue : (WorkerChip.getWorker(p.color,newvalue));	// the actual reroll
    		p.addWorker(newWorker);
    		chosenValue = null;		// only select one die
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(p.newWorkers);
    			animationStack.push(p.workers);
    		}
    	}}
    }
    
    private void reRollWorkersAfterMilos(EPlayer p,replayMode replay,WorkerChip chosenValue,boolean skipCheck)
    {    
    	int sz = p.newWorkers.height();
    	actualReroll(p,sz,chosenValue,replay);  	
  	
        if((sz>0)
        		&& p.canPayX(Cost.Food)
    			&& p.recruitAppliesToMe(RecruitChip.HighGeneralBaron))
    	 	{
    			setContinuation(new Continuation(RecruitChip.HighGeneralBaron,
    							Function.DoHighGeneralBaron,Function.DontHighGeneralBaron,
    							skipCheck?1:0,p));
    	 		return;
    		}
        dontHighGeneralBaron(p,replay,skipCheck);
        }
    	
    private void doHighGeneralBaron(EPlayer p,replayMode replay,boolean skipCheck)
        {	//p1("use HighGeneralBaron");
        	p.payCostOrElse(Cost.Food,replay);
        	int h = p.newWorkers.height();
        	for(int i=0;i<h;i++) 
        		{ trash.addChip(p.workers.removeTop());
        		  if(replay!=replayMode.Replay) {
        			  animationStack.push(p.workers);
        			  animationStack.push(trash);
        		  }
        		  
        		}
        	actualReroll(p,h,null,replay);
        	dontHighGeneralBaron(p,replay,skipCheck);
        }
      
 
    private void dontHighGeneralBaron(EPlayer p,replayMode replay,boolean skipCheck)
        {	// the rerolled workers are still in newWorkers
        	int numberNew = p.newWorkers.height();
           	int extraKnowledge = 0;	// institute of orwellian optimism increases the effective knowledge of 1 or2 value workers          
           	p.newWorkers.reInit();
        	int rollPenalties = 0;
        	int h = p.workers.height();
        	while(numberNew-- > 0)
        	{
        		WorkerChip newWorker = (WorkerChip)p.workers.chipAtIndex(h-1-numberNew);
        		switch(newWorker.knowledge())
        		{
        		case 1:
        			if(p.penaltyAppliesToMe(MarketChip.DisassembleATeddyBearShop)) { rollPenalties++; }
        			if(p.penaltyAppliesToMe(MarketChip.IIB_InstituteOfOrwellianOptimism)) { extraKnowledge += 2; }
        			break;
        		case 2:
        			if(p.penaltyAppliesToMe(MarketChip.ClinicOfBlindHindsight)) { rollPenalties++; }
        			if(p.penaltyAppliesToMe(MarketChip.IIB_InstituteOfOrwellianOptimism)) { extraKnowledge += 1; }
        			break;
        		case 3:
        			if(p.penaltyAppliesToMe(MarketChip.BemusementPark)) { rollPenalties++; }
        			break;
        		case 4:
        			if(p.penaltyAppliesToMe(MarketChip.FriendlyLocalGameBonfire)) { rollPenalties++; }
        			break;
        		case 5:
        			if(p.penaltyAppliesToMe(MarketChip.StadiumOfGuaranteedHomeRuns)) { rollPenalties++; }
        			break;
        		case 6:
        			if(p.penaltyAppliesToMe(MarketChip.CenterForReducedLiteracy)) { rollPenalties++; }
        			break;
    			default:
    				break;
        		}
        		
        	}
        	
    	Cost penalty = REROLL_PENALTIES[rollPenalties];
    	if(penalty!=null)
    	{
    		logGameEvent(LoseGoods,s.get(penalty.description),p.color.name());
    	}
    	int totalk = p.totalKnowlege();
    	//if(skipCheck && totalk+extraKnowledge>=16)
    	//	{ p1("saved by MilosTheBrainwasher");
    	//	}
    	
      	if(!skipCheck)     		
       	{ 	
       		if ((totalk+extraKnowledge>=14)
						&& p.recruitAppliesToMe(RecruitChip.KebTheInformationTrader))
       			{
       			//p1("use KebTheInformationTrader");
        			if(totalk+extraKnowledge>=16) { G.p1("use keb and lose worker"); }
       			Continuation cont = new Continuation(Function.ReRollWorkersAfterKeb,
       									penalty,extraKnowledge,p );
 				setContinuation(cont);
 				useRecruit(RecruitChip.KebTheInformationTrader,"use");
 				setWhoseTurnTo(p.boardIndex,"UseKebTheInformationTrader");
      			logGameEvent(UseKebTheInformationTrader);
      			setContinuation(new Continuation(Benefit.ResourceOrCommodity,Function.Return,p));
				setWhoseTurnTo(p.boardIndex,"ResourceOrCommodity");
				return;
       			}
       	}
      	
      	reRollWorkersAfterKeb(p,replay,skipCheck,penalty,extraKnowledge);
     }

private void reRollWorkersAfterKeb(EPlayer p,replayMode replay,boolean skipCheck,Cost penalty,int extraKnowledge)
{	
	if(!skipCheck)
	{
	   if(p.knowledgeCheck(extraKnowledge,replay))
  		{ 
			if(p.canPay(Cost.Artifact)
					&& p.recruitAppliesToMe(RecruitChip.XyonTheBrainSurgeon))
			{	//p1("use xyon the brain surgeon");
				setWhoseTurnTo(p.boardIndex,"XyonTheBrainSurgeon");
				setContinuation(new Continuation(RecruitChip.XyonTheBrainSurgeon,Function.DoXyonTheBrainSurgeon,Function.ReRollWorkersAfterXyon,
						extraKnowledge,p));
				setState(EuphoriaState.RecruitOption);
				return;
			}
  		}
	}
	reRollWorkersAfterXyon(p,replay,skipCheck,penalty,extraKnowledge);
	
}
//save the worker
private void doXyonTheBrainSurgeon(EPlayer p,replayMode replay,Cost penalty,int extraKnowledge)
{	logGameEvent(SavedByXyonTheBrainSurgeon);
	p.setTFlag(TFlag.HasLostWorker);	// lie so we won't do it again this turn
	// dest is not included because the dest isn't the cause of morale lost
	//p1("saved by xyon the brain surgeon");
	Assert(penalty==null,"no penalty yet");
	setContinuation(new Continuation(Function.ReRollWorkersAfterCheck,	Cost.Artifact,extraKnowledge,p));
	p.decrementKnowledge(replay);	
	p.decrementKnowledge(replay);	
}

private void reRollWorkersAfterXyon(EPlayer p,replayMode replay,boolean skipCheck,
				Cost penalty,int extraKnowledge)
{	
	if(!skipCheck)
	{
	   if(p.knowledgeCheck(extraKnowledge,replay))
  		{ 
			if(p.hasArtifactOrAlternate(ArtifactChip.Bat)
					&& p.canPayX(Cost.Morale)
					&& p.recruitAppliesToMe(RecruitChip.DustyTheEnforcer))
			{	//p1("use dusty normally");
				setContinuation(new Continuation(RecruitChip.DustyTheEnforcer,Function.DoDustyTheEnforcer,Function.DontDustyTheEnforcer,
						penalty,extraKnowledge,p));
				setWhoseTurnTo(p.boardIndex,"DustyTheEnforcer");
				return;
			}
		   dontDustyTheEnforcer(p,replay,penalty,extraKnowledge);
		   return;
 		}
	}
	reRollWorkersAfterCheck(p,replay,penalty,extraKnowledge);
}
//save the worker
void doDustyTheEnforcer(EPlayer p,EuphoriaCell dest,replayMode replay,WorkerChip chosenValue,boolean skipCheck,Cost penalty,int extraKnowledge)
{	logGameEvent(SavedByDustyTheEnforcer,p.getPlayerColor()+"Morale");
	//p1("saved by dusty the enforcer");
	p.setTFlag(TFlag.HasLostWorker);	// lie so we won't do it again this turn
	// dest is not included because the dest isn't the cause of morale lost
	setContinuation(new Continuation(dest,Function.ReRollWorkersAfterCheck,	chosenValue,penalty,extraKnowledge,p));
	doLoseMorale(p,1,replay,dest,Function.Return);	// lose morale and check artifact size
}
void dontDustyTheEnforcer(EPlayer p,replayMode replay,Cost penalty,int extraKnowledge)
{
	p.loseWorker(replay);
	if(checkStevenTheScholar(p,replay,true))
		{
		if(revision<113) { return; }	// shouldn't exit here, but we did.
		}
	reRollWorkersAfterCheck(p,replay,penalty,extraKnowledge);	
}
private void reRollWorkersAfterCheck(EPlayer p,replayMode replay,Cost penalty,int extraKnowledge)
{	
	// this happens after the knowledge check
	if(p.penaltyAppliesToMe(MarketChip.IIB_ThoughtPoliceOfTheOpenMind)
    			&& p.hasDoubles())
    	{	
    		MarketPenalty mp = MarketChip.IIB_ThoughtPoliceOfTheOpenMind.marketPenalty;
    		p.incrementKnowledge(replay);
    		//b.p1("Gain knowledge due to Thought Police");
    		logGameEvent(mp.explanation);
    	}
    	
	if(revision<113)
       	{
       	if((penalty!=null))
       		{	
       		Cost residual = p.payCost(penalty,replay);
       		if(residual!=null)
       		{
      		setContinuation(new Continuation(residual,residual,Function.Return));
      		setWhoseTurnTo(p.boardIndex,"rerollAfterCheck");
      		return; 	// shouldn't exit here, but we did.
       		}}
       	}
       	
       	
	if(p.testTFlag(TFlag.ResultsInDoubles))
       	{
       		for(EPlayer pl : players)
       		{
       			if((pl!=p)
       					&& pl.canPayX(Cost.Moralex2)
       					&& pl.recruitAppliesToMe(RecruitChip.BenTheLudologist)
       					)
       			{
       				setContinuation(new Continuation(RecruitChip.BenTheLudologist,Function.DoBenTheLudologist,Function.Return,pl));
       				setWhoseTurnTo(p.boardIndex,"BenTheLudologist");
       			}
       		}
       	}
       	
       	if(revision>=113)
       	{
       	if(penalty!=null)
       		{	
       		Cost residual = p.payCost(penalty,replay);
       		if(residual!=null)
       		{
      		setContinuation(	new Continuation(residual,residual,Function.Return));	// dest==null is ok
      		setWhoseTurn(p.boardIndex);							
       		}
       		}}
      	//if(continuation!=null) { doContinuation(replay); }
     } 


    private void doBenTheLudologist(EPlayer p,EuphoriaCell dest,replayMode replay)
    {	// ok to get here multiple times.
    	assert_isV12();
    	Assert(p.recruitAppliesToMe(RecruitChip.BenTheLudologist),"ben only");
		doLoseMorale(p,2,replay,dest,Function.Return);
		logGameEvent(BenTheLudologistEffect,currentPlayerColor());
    	setContinuation( new Continuation(Benefit.Resource,dest,Function.Return,p));
    }
    private void doStevenTheScholar(EuphoriaCell dest,replayMode replay,Colors co,Function continuation)
    {	assert_isV12();
		// ok to get here multiple times
		// steven the scholar applies, give him a card and a resource, check knowledge
    	EPlayer p = getPlayer(co);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(UsingStevenTheScholar,s.get(co.name()));
    	doCardsGained(p,replay,dest,continuation);
    	setContinuation(new Continuation(Benefit.Resource,dest,Function.Return,p));
    }
    
    // bump a worker out of a bumpable cell, give him back to 
    // re-roll and give him back to the owner.  This is the start
    // of the chain for IIB recruits.
    public void bumpWorkerIIB(EuphoriaCell dest,replayMode replay)
    {   	
    	bumpWorkerCheckKhaleef(dest,replay);
    }
    private void bumpWorkerCheckKhaleef(EuphoriaCell dest,replayMode replay)
	 {	assert_isIIB();
	 	addToFinalPath("bumpWorker");
	 	WorkerChip worker = bumpedWorker;
	 	EPlayer otherPlayer = getPlayer(worker.color);
		Assert(dest.rackLocation().canBeBumped,"bumpable position");
		EPlayer p = players[whoseTurn];
		if(p.recruitAppliesToMe(RecruitChip.KhaleefTheBruiser)
				&& (otherPlayer!=p)
				&& (otherPlayer.knowledge>=3))
		{
			//p1("ask Khaleef the bruiser");
			// khaleef the bruiser gives you a commodity and them -1 knowledge
			setContinuation(new Continuation(RecruitChip.KhaleefTheBruiser,dest,Function.DoKhaleefTheBruiser,Function.ReRollBumpedWorker,p));
		}
		else {
			reRollBumpedWorker(dest,replay);
		}
	 }
    
    
    // bump a worker out of a bumpable cell, give him back to 
    // re-roll and give him back to his owner.
    public void bumpWorker(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
		addToFinalPath("bumpWorker");
    	Assert(dest.rackLocation().canBeBumped,"bumpable position");
    	EPlayer otherPlayer = getPlayer(bumpedWorker.color);
    	bumpingWorker = (WorkerChip)dest.topChip();
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(dest);
    		animationStack.push(otherPlayer.newWorkers);
    	}
    	acceptPlacement();
 
    	if(!isIIB()
    		&& otherPlayer.recruitAppliesToMe(RecruitChip.AmandaTheBroker_V2)
	    				&& otherPlayer.canPay(Cost.Bliss)
	    				)
	    		{
	    			setContinuation(new Continuation(RecruitChip.AmandaTheBroker_V2,dest,
	    								Function.DoAmandaTheBroker,Function.BumpWorkerJuliaTheThoughtInspector_V2,otherPlayer));
	    			setWhoseTurnTo(otherPlayer.boardIndex,"AmandaTheBroker_V2");
	    			return;
	    		}
    	
    	if((bumpedWorker.color!=bumpingWorker.color)
    				&& otherPlayer.recruitAppliesToMe(RecruitChip.AmandaTheBroker)
    				&& otherPlayer.canPay(Cost.Bliss)
    				)
    		{
    			setContinuation(new Continuation(RecruitChip.AmandaTheBroker,dest,
    					Function.DoAmandaTheBroker,Function.BumpWorkerJuliaTheThoughtInspector_V2,otherPlayer));
    			setWhoseTurnTo(otherPlayer.boardIndex,"AmandaTheBroker");
    			return;
    		}
	    
    	bumpWorkerJuliaTheThoughtInspector_V2(replay);
    }
    

private void doKhaleefTheBruiser(EuphoriaCell dest,replayMode replay)
{	assert_isIIB();
	WorkerChip worker = bumpedWorker;
	EPlayer otherPlayer = getPlayer(worker.color);
	otherPlayer.decrementKnowledge(replay);
	//p1("use Khaleef the bruiser");
	setContinuation(new Continuation(Benefit.Commodity,Function.Return,null));	// current player gets the resource
	reRollBumpedWorker(dest,replay);
}



  	
// called when we bumped a worker of another color
private void bumpWorkerJuliaTheThoughtInspector_V2(replayMode replay)
{		assert_isV12();
		EPlayer p = players[whoseTurn];
   		if(p.recruitAppliesToMe(RecruitChip.JuliaTheThoughtInspector_V2)
   				&& (bumpedWorker.color != bumpingWorker.color)
   				&& p.canPayX(Cost.Morale))
   			{
   			setContinuation(new Continuation(RecruitChip.JuliaTheThoughtInspector_V2,
										Function.DoJuliaTheThoughtInspector_V2,
										Function.Return,p));
   			return;
   			}

    	reRollWorkers(getPlayer(bumpedWorker.color),replay,null,null);
}

private void doAmandaTheBroker(EuphoriaCell dest,replayMode replay,RecruitChip active,Function continuation)
    {	// give the bumper one of our bliss, and use the selected die roll
		assert_isV12();
		addToFinalPath("doAmandaTheBroker");
		EPlayer p = getPlayer(bumpingWorker.color);
    	EPlayer other = getPlayer(bumpedWorker.color);
    	Assert(selectedDieRoll!=null,"die must be selected");
    	other.payCostOrElse(Cost.Bliss,replay);
     	if(active==RecruitChip.AmandaTheBroker)
    	{
    	// in euphoria2, just pay, the other player doesn't collect
    	p.collectBenefitOrElse(Benefit.Bliss,replay);
    	}
    	logGameEvent(AmandaTheBrokerSelects,selectedDieRoll.shortName());
    	reRollWorkers(other,replay,selectedDieRoll,continuation);
    }
 
	private void reRollBumpedWorker(EuphoriaCell dest,replayMode replay)
    {	EPlayer otherPlayer = getPlayer(bumpedWorker.color);
    	if((players[whoseTurn]==otherPlayer) 
    			&& otherPlayer.penaltyAppliesToMe(MarketChip.IIB_TogetherWeWorkAloneCamp))
    	{
    		//p1("penalty from together we work alone");
    		logGameEvent(TogetherPenalty);
    		otherPlayer.incrementKnowledge(replay);
    	}
    	reRollWorkers(otherPlayer,replay,null,Function.Return);
    }

    /**
     * after we drop a worker and pay for placement, collect the benefits promised
     * 
     * @param dest
     * @param replay
     */
    private void dropWorkerCollectBenefit(EuphoriaCell dest,replayMode replay)
    {	
		EuphoriaId rack = dest.rackLocation();
		EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerCollectBenefit");
		
		if(G.debug())
			{p.checkDropWorker(dest,replay);	// check if this is a known guy for drop worker		
			}
		
		if(!isIIB()) {
		
		Assert(!(rack.canBeBumped && (dest.height()>1)),"should have been bumped");
		if(p.testTFlag(TFlag.UsedBrianTheViticulturist)
				&& p.canPayX(Cost.Knowledge))
			{	p.payCostOrElse(Cost.Knowledge,replay);
				p.clearTFlag(TFlag.UsedBrianTheViticulturist);
			if(revision>=116)
			{	// influence to Wastelander, not Icarite
				if(incrementAllegiance(Allegiance.Wastelander,replay))
				{

				logGameEvent(WastelanderInfluenceFromBrianTheViticulturist);
				}
			}
			else if(incrementAllegiance(Allegiance.Icarite,replay))
					{
					logGameEvent(IcariteInfluenceFromBrianTheViticulturist);
					}			
		}
		switch(rack)
		{
    	case EuphorianBuildMarketA:
    	case EuphorianBuildMarketB:
    	case SubterranBuildMarketA:
    	case SubterranBuildMarketB:
    	case WastelanderBuildMarketA:
    	case WastelanderBuildMarketB:
    		if((revision>=114)
    				&& p.recruitAppliesToMe(RecruitChip.JonathanTheArtist)
    				&& p.canPayX(Cost.Knowledgex2)
    				&& highestMarketKnowledge(rack,dest))
    		{
    			setContinuation(new Continuation(RecruitChip.JonathanTheArtist,dest,
    					Function.DoJonathanTheArtist,Function.DropWorkerCollectBenefitAfterRecruits,p));
    			return;
    		}
    		break;
    		
 
    	case IcariteCloudMine:
				{
					
					if((p.recruitAppliesToMe(RecruitChip.ZongTheAstronomer))
							&& ((p.totalResources()+p.artifacts.height())==0))
					{
						setContinuation(new Continuation(Benefit.WaterOrEnergy,dest,
									Function.DropWorkerCollectBenefitAfterRecruits,p));
						return;
					} 
					else if((p.recruitAppliesToMe(RecruitChip.ZongTheAstronomer_V2))
							&& (p.canPay(Cost.Knowledge)))
					{
						setContinuation(new Continuation(RecruitChip.ZongTheAstronomer_V2,dest,
										Function.DoZongTheAstronomer_V2,
										Function.DropWorkerCollectBenefitAfterRecruits,p));
						return;
					}
				}
				break;
		case EuphorianUseMarket:
		case SubterranUseMarket:
		case WastelanderUseMarket:
		case EuphorianMarketA:
		case EuphorianMarketB:
		case WastelanderMarketA:
		case WastelanderMarketB:
		case SubterranMarketA:
		case SubterranMarketB:
		case IcariteNimbusLoft:
		case IcariteBreezeBar:
		case IcariteWindSalon:
		case IcariteSkyLounge:
		case SubterranTunnelEnd:
		case WastelanderTunnelEnd:
		case EuphorianTunnelEnd:
			// all other bump-able positions
			break;
		case WorkerActivationA:		// LauraThePhilanthropist for tunnels and worker activation
		case WorkerActivationB:
			dropWorkerLauraThePhilanthropist(dest,replay);	
			return;
		case EuphorianTunnelMouth:
		case WastelanderTunnelMouth:
		case SubterranTunnelMouth:
			if(variation.isIIB())
			{
				dropWorkerCollectBenefitAfterRecruits(dest,replay);	
			}
			else
			{
			dropWorkerDaveTheDemolitionist(dest,replay);	// DaveTheDemolitionist only for tunnel mouth
			}
			return;
		default: 
			Assert(!rack.canBeBumped,"all bumpable positions should be considered");
			break;
		}}
 	
    	dropWorkerCollectBenefitAfterRecruits(dest,replay);
    }
    // correct implementation
    private void doJonathanTheArtist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doJohnathanTheArtist");
		p.payCostOrElse(Cost.Knowledgex2,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(JonathanTheArtistEffect,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DropWorkerCollectBenefitAfterRecruits);
    }
    /**
     * zong the astronomer v2 is completely different from v1
     * gets a card, gives bliss to others.
     * @param dest
     * @param replay
     */
    private void doZongTheAstronomer_V2(EuphoriaCell dest,replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	p.payCostOrElse(Cost.Knowledge,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);	// get a card
    	for(EPlayer op : players) { if(op!=p) { op.collectBenefitOrElse(Benefit.Bliss,replay); }}
    	doCardsGained(p,replay,dest,Function.DropWorkerCollectBenefitAfterRecruits);
    }
    
    // reached after all drop worker on tunnel positions
    private void dropWorkerDaveTheDemolitionist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	assert_isV12();
		addToFinalPath("dropWorkerDaveTheDemolitionist");
		if((bumpedWorker!=null)
				&& (bumpedWorker.color!=p.color))
		{	if(p.canPayX(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.DaveTheDemolitionist))
			{
			setContinuation(new Continuation(RecruitChip.DaveTheDemolitionist,dest,
					Function.DoDaveTheDemolitionist,Function.DropWorkerLauraThePhilanthropist,p));
			return;
			}
		else if(p.canPayX(Cost.Knowledge)
				&& p.recruitAppliesToMe(RecruitChip.DaveTheDemolitionist_V2))
			{
			setContinuation(new Continuation(RecruitChip.DaveTheDemolitionist_V2,dest,
					Function.DoDaveTheDemolitionist,Function.DropWorkerLauraThePhilanthropist,p));
			return;
			}
		}
    	dropWorkerLauraThePhilanthropist(dest,replay);
    }

    private void doDaveTheDemolitionist(EuphoriaCell dest,Benefit bene,RecruitChip active,replayMode replay)
    {
		addToFinalPath("doDaveTheDemolitionist");
		EPlayer p = players[whoseTurn];
		Cost cost = (active==RecruitChip.DaveTheDemolitionist) ? Cost.Knowledgex2 : Cost.Knowledge;
		p.payCostOrElse(cost,replay);
		p.collectBenefitOrElse(bene.associatedResource(),replay);
		incrementTunnelPosition(dest.allegiance);	// bump the tunnel position an extra time
		logGameEvent(UseDaveTheDemolitionist,bene.name(),currentPlayerColor());
		dropWorkerLauraThePhilanthropist(dest,replay);	
    }
    
    // reached after drop worker on tunnels and worker activation
    private void dropWorkerLauraThePhilanthropist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	assert_isV12();
		addToFinalPath("dropWorkerLauraThePhilanthropist");
		if((bumpedWorker!=null)
    		&& (bumpedWorker.color!=p.color)
    		&& p.canPay(Cost.Gold)			// uses her own gold
    		&& p.recruitAppliesToMe(RecruitChip.LauraThePhilanthropist))
    	{
    		setContinuation(new Continuation(RecruitChip.LauraThePhilanthropist,dest,
    				Function.DoLauraThePhilanthropist,Function.DropWorkerCollectBenefitAfterRecruits,p));
    		return;
    	}
    	dropWorkerCollectBenefitAfterRecruits(dest,replay);
     }
    
    // give a gold, get 2 cards.
    private void doLauraThePhilanthropist(EuphoriaCell dest,replayMode replay)
    {
		addToFinalPath("doLauraThePhilanthropist");
		EPlayer other = getPlayer(bumpedWorker.color);
    	EPlayer p = players[whoseTurn];
    	p.payCostOrElse(Cost.Gold,replay);
    	other.collectBenefitOrElse(Benefit.Gold,replay);
    	p.collectBenefitOrElse(Benefit.Artifactx2,replay);
    	logGameEvent(LauraThePhilanthropistEffect,other.color.name());
    	doCardsGained(p,replay,dest,Function.DropWorkerCollectBenefitAfterRecruits);
    }

 
    
    // collect benefits after all placment recruits have had their say
    private void dropWorkerCollectBenefitAfterRecruits(EuphoriaCell dest,replayMode replay)
    {	
    	addToFinalPath("dropWorkerCollectBenefitAfterRecruits");
		WorkerChip worker = (WorkerChip)dest.topChip();
 		EPlayer p = getPlayer(worker.color);

		if(p.usedAlternateArtifact!=null)
		{	//p1("collect benefit for wildcard "+p.usedAlternateArtifact.id.name());
			p.usedAlternateArtifact = null;
			setContinuation(new Continuation(Benefit.Commodity,dest,Function.CollectBenefitAfterArtifacts,p));
			return;
		}
		collectBenefitAfterArtifacts(p,dest,replay);
    }
    private void collectBenefitAfterArtifacts(EPlayer p,EuphoriaCell dest,replayMode replay)
    {   addToFinalPath("dropWorkerCollectBenefitAfterArtifacts");
    	Benefit bene = dest.placementBenefit;
    	Benefit originalBenefit = bene;
		Allegiance alleg = dest.allegiance;
		boolean allegianceUpgrade = alleg!=null
										&& (getAllegianceValue(alleg)>=ALLEGIANCE_TIER_2)
										&& p.hasActiveRecruit(alleg);	
		boolean youssef =  p.recruitAppliesToMe(RecruitChip.YoussefTheTunneler);
		boolean mwiche = p.testTFlag(TFlag.UsingMwicheTheFlusher);
		if(youssef || mwiche )
		{	// payoff switches if mwiche uses 3 water
			p.clearTFlag(TFlag.UsingMwicheTheFlusher);
			switch(bene)
			{
			case CardOrGold:
				//(youssef) { p1("use youssef the tunneler"); }
				bene = Benefit.CardAndGold;
				//$FALL-THROUGH$
			case CardAndGold:
				break;
			case CardOrStone:
				bene = Benefit.CardAndStone;
				//$FALL-THROUGH$
			case CardAndStone:
				break;
			case CardOrClay:
				bene = Benefit.CardAndClay;
				//$FALL-THROUGH$
			case CardAndClay:
				break;
			default: if(mwiche) { Error("Not expecting benefit %s",bene); }
				}
			if(mwiche)
				{ logGameEvent(UseMwicheTheFlusher);
				  useRecruit(RecruitChip.MwicheTheFlusher,"collect resources");
				  p.incrementMorale(replay);
				  p.incrementMorale(replay);
				}
			if(youssef && (bene!=originalBenefit)) 
				{ logGameEvent(UseYoussefTheTunneler); 
				  useRecruit(RecruitChip.YoussefTheTunneler,"get both tunnel");
				}
		}
		
		switch(bene)
		{

		case CardOrClay:

		case CardOrGold:
			if((p.artifacts.height()==0) &&
				p.recruitAppliesToMe(RecruitChip.AndrewTheSpelunker))
				{
				// AndrewTheSpelunker upgrades the benefit
				allegianceUpgrade = true;
				logGameEvent(AndrewTheSpelunkerEffect);
				}
				// fall into the full case
			//$FALL-THROUGH$
		case CardOrStone:
			incrementTunnelPosition(alleg);
			
			if(p.recruitAppliesToMe(RecruitChip.TedTheContingencyPlanner)) 
				{ 	// pay bliss instead of normal cost
					useRecruit(RecruitChip.TedTheContingencyPlanner,"didn't gain both");
					if(p.gainMorale(replay)) 
					{ logGameEvent(MoraleFromTedTheConingencyPlanner,p.getPlayerColor()+"Morale");
					}
				}
			if(allegianceUpgrade)
			{
			//
			// upgrade the tunnel benefit if the allegiance track is advanced to the middle tier
			// and we have an active recruit with that allegiance.
			// this can later be downgraded by market RegistryOfPersonalSecrets penalty
			//
			// the way the downgrading works is the "collect" option triggers a choice dialog instead
			// of delivering both benefits, and the various places that normally look for CardOrXX 
			// treat CardAndXX as the same.
			//
			bene = UPGRADED_BENEFIT[alleg.ordinal()];
			}
			if(!allegianceUpgrade
				&& (p.canPay(Cost.Bliss))
				&& p.recruitAppliesToMe(RecruitChip.LieveTheBriber))
			{
				setContinuation(new Continuation(RecruitChip.LieveTheBriber,dest,replay,Function.DoLieveTheBriber,Function.DropWorkerCollectBenefit,bene,p));
				return;
			}
				
			break;
			
		case BlissSelection:
		case PowerSelection:
		case WaterSelection:
		case FoodSelection:
			if(adjustCommodity(p,dest,bene,replay)) { return; }
			break;
			
			
		default: break;
			
		}

		dropWorkerCollectBenefit(p,dest,bene,replay);
    }
    
    // return true if a continuation was added 
    private boolean adjustCommodity(EPlayer p,EuphoriaCell dest,Benefit bene,replayMode replay)
    {
		// set the knowledge so it can be manipulated by recruits.  We depend
		// on there being only one commodity in use, and uniform treatment
		switch(bene)
		{
		default: break;
		case BlissSelection:
			p.commodityKnowledge = totalKnowledgeOnCloudMine();
			break;
			
		case PowerSelection:
			p.commodityKnowledge = totalKnowledgeOnGenerator();
			break;
			
		case WaterSelection:
			p.commodityKnowledge = totalKnowledgeOnAquifer();
			break;
			
		case FoodSelection:
			p.commodityKnowledge = totalKnowledgeOnFarm();
			break;
		}
		
		int zone = knowledgeZone(p.commodityKnowledge);
		boolean askPmai = false;
		boolean askZara = false;
		boolean usePmai = p.recruitAppliesToMe(RecruitChip.PmaiTheNurse);
		boolean useZara = p.recruitAppliesToMe(RecruitChip.ZaraTheSolipsist);
		if(askZara && askPmai) 
		{	p1("Need to figure the interaction");
		}
		if(usePmai)
		{	askPmai = zone!= knowledgeZone(p.commodityKnowledge-1);
		}
		if(useZara)
		{	int knowx2 = dest.topChip().knowledge()*2;
			askZara = zone != knowledgeZone(knowx2);	// 
			if(askPmai)
				{	askZara |= zone != knowledgeZone(knowx2-1);
				}
		}		
		if(askPmai)		
			{	//p1("ask pmaithenurse");
				setContinuation(new Continuation(RecruitChip.PmaiTheNurse,dest,replay,Function.DoPmaiTheNurse,Function.DropWorkerCollectBenefit,bene,p));
				return(true);
			}
		if(askZara)
			{
				//p1("ask zara the solipsist");
				setContinuation(new Continuation(RecruitChip.ZaraTheSolipsist,dest,replay,Function.DoZaraTheSolipsist,
							Function.DropWorkerCollectBenefit,bene,p));		
				return(true);
			}
		return(false);
    }
    
    private int knowledgeZone(int n)
    {	if(n<=4) { return(0); }
    	if(n<=8) { return(1); }
    	return 2;
    }
    private void doZaraTheSolipsist(EPlayer p,EuphoriaCell dest,Benefit bene,replayMode replay)
    {
    	//p1("do zara the soloipsist");
    	p.commodityKnowledge = dest.topChip().knowledge()*2;
    	dropWorkerCollectBenefit(p,dest,bene,replay);
    }
    private void doPmaiTheNurse(EPlayer p,EuphoriaCell dest,Benefit bene,replayMode replay)
    {	//p1("do pmai the nurse");
    	p.commodityKnowledge--;
    	dropWorkerCollectBenefit(p,dest,bene,replay);
    }

    private void dropWorkerCollectBenefit(EPlayer p,EuphoriaCell dest,Benefit bene,replayMode replay)
    {
	Benefit residual = p.collectBenefit(bene,replay);
	if(residual!=null)	// if we can't collect the benefit directly, schedule an interaction
    	{	
		
			setContinuation(new Continuation(residual,dest,Function.DropWorkerAfterBenefit,p));
			if(variation.isIIB() && (dest.rackLocation()==EuphoriaId.IcariteBreezeBar))
       		{	// we'll need to collect 1 artifact twice
       			//p1("collect 2 artifacts "+p.color);
       			setContinuation(new Continuation(Benefit.FirstArtifact,Function.Return,p));
       		}
			return;
    	}
	dropWorkerAfterBenefit(p,dest,replay); 
    }
    private void doLieveTheBriber(EPlayer p,EuphoriaCell dest,Benefit bene,replayMode replay)
    {	//p1("doLieveTheBriber");
    	p.payCostOrElse(Cost.Bliss,replay);
    	bene = UPGRADED_BENEFIT[dest.allegiance.ordinal()];
    	// gets a card for bliss + normal card cost
    	dropWorkerCollectBenefit(p,dest,bene,replay);
    }
    //
    // adjust morale, and trigger a card giveaway if necessary.  
    // Return true if it's fully resolved and the continuation should be called directly
    //
    private boolean doLoseMorale(EPlayer p,int n,replayMode replay,EuphoriaCell dest,Function cont)
    {	
		for(int i=0;i<n;i++) { p.loseMorale(replay); }
		return(doMoraleCheck(p,replay,dest,cont));
    }
    // return false if interaction is needed
    private boolean doMoraleCheck(EPlayer p,replayMode replay,EuphoriaCell dest,Function cont)
    {	boolean val = true;
    	boolean val2 = true;
		if(p.moraleCheck(replay))
		{
			// we need to lose a card or several
			Cost cost = p.moraleCost();
			setContinuation(dest==null 
					? new Continuation(cost,cont,p)
					: new Continuation(cost,dest,cont,p));
			setWhoseTurnTo(p.boardIndex,"morale check");
			val = false;
			
		}
		// do this after morale check, so the resource claim will be first
		if(variation.isIIB()) 
			{ val2 = doCaryTheCarebear(p,replay,dest,cont);
			//p1("morale check "+val+" "+val2);
			}
		return(val && val2);
    }
    // return false if interaction is needed
    boolean doMoraleCheck(EPlayer p,EuphoriaCell dest,replayMode replay)
    {
    	return(doMoraleCheck(p,replay,dest,Function.Return));
    }
        
    private void dropWorkerAfterBenefit(EPlayer p,EuphoriaCell dest,replayMode replay)
    {	
    	acceptPlacement();
		addToFinalPath("dropWorkerAfterBenefit");
		doCardsGained(p,replay,dest,Function.DropWorkerAfterMorale);
    }
    private void dropWorkerAfterMorale(EPlayer p,EuphoriaCell dest,replayMode replay)
    {	// checks for recruit effects after a morale check
    	if(variation.isIIB())
    	{	
    	}
    	else { 
    	addToFinalPath("dropWorkerAfterMorale");
    	
    	setContinuation(new Continuation(dest,EuphoriaState.ExtendedBenefit, Function.DropWorkerJuliaTheThoughtInspector,null)); 
    	switch(dest.rackLocation())
		{
		case WastelanderTunnelMouth:
		case EuphorianTunnelMouth:
		case SubterranTunnelMouth:
			dropWorkerXanderTheExcavator(dest,replay); 
			break;
		case WorkerActivationA:
		case WorkerActivationB:
			dropWorkerChaseTheMinerRoll(dest,replay); 
			break;
			
		case SubterranUseMarket:
		case EuphorianUseMarket:
		case WastelanderUseMarket:
			// we may have placed a star, recheck penalties
			players[whoseTurn].checkMandatoryEquality();
			// fall into the rest of the market code
			//$FALL-THROUGH$
		case SubterranMarketA:
		case SubterranMarketB:
		case EuphorianMarketA:
		case EuphorianMarketB:
		case WastelanderMarketA:
		case WastelanderMarketB:
			dropWorkerBradlyTheFuturist(dest,replay); 
			
			break;
		case IcariteCloudMine:
		    // reached from cloud mine
			
			doJosiahTheHacker(dest,replay,icariteCloudMine);
			{
			if(p.recruitAppliesToMe(RecruitChip.SarineeTheCloudMiner)
					&& (countWorkers(getProducerArray(dest.allegiance))==1)
					)
				{
				Benefit residual = p.collectBenefit(Benefit.KnowledgeOrBliss,replay);
				if(residual!=null)
					{
					setContinuation(new Continuation(residual,dest,Function.Return,p));
					return;
					}
				else { logGameEvent(SarineeTheCloudMinerBliss); }
				}
			}
			// check a chain of recruit effects appropriate for v1 and v2
			// each one interacts if necessary, then triggers the next in the chain
			dropWorkerKyleTheScavenger(dest,replay);
		    break;
		case EuphorianGenerator:
			doJosiahTheHacker(dest,replay,euphorianGenerator);
			{
			if(p.recruitAppliesToMe(RecruitChip.GaryTheElectrician))
			{	int myKnowledge = dest.topChip().knowledge();
				boolean ok = false;
				for(EuphoriaCell c : euphorianGenerator)
				{
					WorkerChip worker = (WorkerChip)c.topChip();
					if((worker!=null) && (c!=dest) && (worker.knowledge()==myKnowledge)) 
					{
						ok=true;
					}
				}
				if(ok)
				{	int morale = p.morale;
					Benefit residual = p.collectBenefit(Benefit.MoraleOrEnergy,replay);
					if(residual!=null)
					{
					setContinuation(new Continuation(residual,dest,
	    					Function.DropWorkerKyleTheScavenger,p));
					return;
					}
					
					logGameEvent((morale<MAX_MORALE_TRACK)
								? GaryTheElectricianMorale
								: GaryTheElectricianEnergy,currentPlayerColor());
				}
			}
			if((p.artifacts.height()==0) && p.recruitAppliesToMe(RecruitChip.KatyTheDietician))
					{
					// get food with the energy
					p.collectBenefitOrElse(Benefit.Food,replay);
					logGameEvent(KatyTheDieticianEffect);
					}
			}
			dropWorkerKyleTheScavenger(dest,replay);
			break;
			
		case WastelanderFarm:
			doJosiahTheHacker(dest,replay,wastelanderFarm);
			dropWorkerIanTheHorticulturist(dest,replay);
			dropWorkerKyleTheScavenger(dest,replay);
			break;
		case SubterranAquifer:
			// this one doesn't need joshthehacker.
			if(!variation.isIIB()) { dropWorkerSoullessThePlumber(dest,replay); }
			break;
	
		case EuphorianBuildMarketA:
    	case EuphorianBuildMarketB:
    	case WastelanderBuildMarketA:
    	case WastelanderBuildMarketB:
    	case SubterranBuildMarketA:
    	case SubterranBuildMarketB:
    			if((getAllegianceValue(Allegiance.Euphorian)<AllegianceSteps-1)
    				&& players[whoseTurn].recruitAppliesToMe(RecruitChip.NakagawaTheTribute_V2))
    				{
    				setContinuation(new Continuation(RecruitChip.NakagawaTheTribute_V2,dest,Function.DoNakagawaTheTribute_V2,Function.Return,null));
    				}
    			break;
		default:	;
		}}
    }

    private WorkerChip highestKnowledge(EuphoriaCell cells[])
    {
	   	WorkerChip highest = null;
			boolean multiple = false;
			for(EuphoriaCell c : cells)
			{	WorkerChip worker = (WorkerChip)c.topChip();
				if(highest==null) { highest = worker; }
				else if(worker!=null) {
					if(worker.knowledge()>highest.knowledge()) { highest = worker; multiple=false; }
					else if(worker.knowledge()==highest.knowledge()) { multiple=true; }
				}
			}
			if(!multiple) { return(highest); }
			return(null);
    }
    private WorkerChip lowestKnowledge(EuphoriaCell cells[])
    {
		WorkerChip lowest = null;
		boolean multiple = false;
		for(EuphoriaCell c : cells)
		{	WorkerChip worker = (WorkerChip)c.topChip();
			if(lowest==null) { lowest = worker; }
			else if(worker!=null) {
				if(worker.knowledge()<lowest.knowledge()) { lowest = worker; multiple=false; }
				else if(worker.knowledge()==lowest.knowledge()) { multiple=true; }
			}
		} 	
		if(!multiple) { return(lowest); }
		return(null); 	
    }
    

    // reached only from the aquifer
    private void dropWorkerSoullessThePlumber(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerSoullessThePlumber");
		if(p.recruitAppliesToMe(RecruitChip.SoullessThePlumber))
    	{	// if alone of has the lowest knowledge
    		WorkerChip lowest = lowestKnowledge(subterranAquifer);
    		if(dest.topChip()==lowest)
    		{	// we have the lowest knowledge
    			Benefit residual = p.collectBenefit(Benefit.WaterOrMorale,replay);
    			if(residual!=null)
    			{
    			setContinuation(new Continuation(residual,dest,Function.DropWorkerKyleTheScavenger,p));
       			return;
    			}
    			else { logGameEvent(SoullessThePlumberWater); }
     		}
    	}
    	dropWorkerKyleTheScavenger(dest,replay);
    }
    // when we use foreign farms, gain morale and make them gain knowledge
    private void dropWorkerIanTheHorticulturist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doIanTheHorticulturist");
		if((p.totalResources()==0)
			&& p.recruitAppliesToMe(RecruitChip.IanTheHorticulturist))
    	{
			p.collectBenefitOrElse(Benefit.Water,replay);
			logGameEvent(IanTheHorticulturistEffect);
    	}
    }
    // when we use foreign farms, gain morale and make them gain knowledge
    private void doJosiahTheHacker(EuphoriaCell dest,replayMode replay,EuphoriaCell[]cells)
    {	assert_isV12() ;
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doJosiahTheHacker");
		if(p.recruitAppliesToMe(RecruitChip.JosiahTheHacker))
    	{
        	int gained =0;
        	if(p.gainMorale(replay))	// we get morale
        	{
        		logGameEvent(MoraleFromJosiaTheHacker,currentPlayerColor());
        	}
        	for(EuphoriaCell c : cells)
        	{
        		WorkerChip chip = (WorkerChip)c.topChip();
        		if((chip!=null) && (chip.color!=p.color))
        			{ int mask = (1<<getPlayer(chip.color).boardIndex);
        			  if((gained&mask)==0)
        			  {
        				  gained |= mask;
        				  if(getPlayer(chip.color).incrementKnowledge(replay))
        				  {
        					  logGameEvent(KnowledgeFromJosiaTheHacker,chip.color.name());
        				  }
        				  
        			  }
        			}
        	}
    	}
    }
    // reached from drop worker on tunnels
    private void dropWorkerXanderTheExcavator(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
		EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerXanderTheExcavator");
		if((bumpedWorker!=null) 
				&& (bumpedWorker.color!=p.color)
				&& (bumpedWorker.knowledge()>bumpingWorker.knowledge())
				&& (p.recruitAppliesToMe(RecruitChip.XanderTheExcavator))
				&& (p.canPayX(Cost.Knowledge))
				)
		{	setContinuation(new Continuation(RecruitChip.XanderTheExcavator,dest,
				Function.DoXanderTheExcavator,Function.DropWorkerChaseTheMinerSacrifice,p));
			return;
		}
		dropWorkerChaseTheMinerSacrifice(dest,replay);
	}
    private void doXanderTheExcavator(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	p.payCostOrElse(Cost.Knowledge,replay);
    	Benefit bene = dest.placementBenefit.associatedResource();
    	p.collectBenefitOrElse(bene,replay);
    	logGameEvent(XanderTheExcavatorBenefit,bene.name(),currentPlayerColor());
    	
    	dropWorkerChaseTheMinerSacrifice(dest,replay);
    }
    // called form worker activation
    private void dropWorkerChaseTheMinerRoll(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerChaseTheMinerRoll");
		if( (p.newWorkers.height()>0)		// we still have a new worker
				&& p.recruitAppliesToMe(RecruitChip.ChaseTheMiner))
    	{
    		p.setTFlag(TFlag.UsedChaseTheMiner); 
    		logGameEvent(ChaseTheMinerRoll);
    	}
    	dropWorkerPeteTheCannibal(dest,replay);
    }

    // reached from worker tunnel placements
    private void dropWorkerChaseTheMinerSacrifice(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerChaseTheMiner");
		if((p.totalWorkers>1)
    		&& p.recruitAppliesToMe(RecruitChip.ChaseTheMiner)
    			)
    	{	setContinuation(new Continuation(RecruitChip.ChaseTheMiner,dest,
    			Function.DoChaseTheMinerSacrifice,Function.DropWorkerPeteTheCannibal,p));
    	}
    	else {  	dropWorkerPeteTheCannibal(dest,replay); }
    }
    private void doChaseTheMinerSacrifice(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
		addToFinalPath("doChaseTheMinerSacrifice");
    	EPlayer p = players[whoseTurn];
     	p.sacrificeWorker(dest,replay);
     	Benefit bene = TUNNEL_BENEFIT_CHASE_THE_MINER[dest.allegiance.ordinal()];
     	logGameEvent(ChaseTheMinerSacrifice,bene.description);
     	p.collectBenefitOrElse(bene,replay);
     }

    // reached from worker activation sites
    private void dropWorkerPeteTheCannibal(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerPeteTheCannibal");
		if(p.hasGainedWorker()
				&& p.canPayX(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.PeteTheCannibal))
		{
			setContinuation(new Continuation(RecruitChip.PeteTheCannibal,dest,
					Function.DoPeteTheCannibalBenefit,Function.DoSheppardTheLobotomistBenefit,p));
		}
		else { doSheppardTheLobotomistBenefit(dest,replay); }

    }
    
    // reached from commodity production sites
    private void dropWorkerKyleTheScavenger(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerKyleTheScavenger");
		if((p.knowledge<=4)
				&& (countWorkers(getProducerArray(dest.allegiance))==1)
				&& p.recruitAppliesToMe(RecruitChip.KyleTheScavenger))
		{	// if we can afford to gain 2 knowledge and have KyleTheScavenger
		setContinuation(new Continuation(RecruitChip.KyleTheScavenger,dest,Function.DoKyleTheScavenger,Function.DropWorkerJonathanTheArtistFarmer,p));
		}
		else 
		{
			dropWorkerJonathanTheArtistFarmer(dest,replay);	// don't need to consider kyle
		}
    }
    private void doKyleTheScavenger(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	addToFinalPath("doKyleTheScavenge");

    	EPlayer p = players[whoseTurn];
    	p.incrementKnowledge(replay);
    	p.incrementKnowledge(replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(KyleTheScavengerEffect);
    	doCardsGained(p,replay,dest,Function.DropWorkerJonathanTheArtistFarmer);
     }
    
    // reached from any commodity site
    private void dropWorkerJonathanTheArtistFarmer(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJonathanTheArtist");
		if((revision<114) 	// mistaken implementation fixed by 114
				&& p.recruitAppliesToMe(RecruitChip.JonathanTheArtist)
    			&& p.canPayX(Cost.Knowledgex2))
    	{
    	WorkerChip highest = highestKnowledge(producerArray[dest.allegiance.ordinal()]);
    	if(highest==dest.topChip())
    		{
    			setContinuation(new Continuation(RecruitChip.JonathanTheArtist,dest,
    					Function.DoJonathanTheArtistFarmer,Function.DropWorkerScarbyTheHarvester,p));
    			return;
    		}
    	}
    	dropWorkerScarbyTheHarvester(dest,replay); 
    }
    // mistaken implementation
    private void doJonathanTheArtistFarmer(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doJohnathanTheArtistFarmer");
		p.payCostOrElse(Cost.Knowledgex2,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(JonathanTheArtistEffect,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DropWorkerScarbyTheHarvester);
     }

    private void doPeteTheCannibalBenefit(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doPeteTheCannibleBenefit");
		p.payCostOrElse(Cost.Knowledgex2,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(PeteTheCannibalNewWorker,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DoSheppardTheLobotomistBenefit);
    }
    
    // reached from worker recruit sites
    private void doSheppardTheLobotomistBenefit(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doShepphardTheLobotomistBenefit");
		if(p.hasGainedWorker()
				&& p.canPayX(Cost.Moralex2)
				&& p.recruitAppliesToMe(RecruitChip.SheppardTheLobotomist))
		{
			setContinuation(new Continuation(RecruitChip.SheppardTheLobotomist,dest,Function.DoSheppardTheLobotomistMorale,Function.Return,p));
		}
    }
    
    // reached from market placements
    private void dropWorkerBradlyTheFuturist(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerBradlyTheFuturist");
		if(p.recruitAppliesToMe(RecruitChip.BradlyTheFuturist)
    			&& p.canPayX(Cost.Knowledgex2))
		{
			// can sacrifice this worker for an extra *
			// nakagawathetribute_v2 is completely different.
			setContinuation(new Continuation(RecruitChip.BradlyTheFuturist,dest,
						Function.DoBradlyTheFuturist,Function.DropWorkerNakagawaTheTribute,p));
    		
		}
    	else { dropWorkerNakagawaTheTribute(dest,replay); }
    }
    private void doBradlyTheFuturist(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doBradlyTheFuturist");
    	p.payCostOrElse(Cost.Knowledgex2,replay);
    	marketBasket.reInit();	// just in case
    	marketBasket.addChip(getArtifact());
    	marketBasket.addChip(getArtifact());
    	logGameEvent(BradleyTheFuturistEffect,currentPlayerColor());
    	if(replay!=replayMode.Replay)
    	{
    		animatePlacedItem(unusedArtifacts,marketBasket);
    		animatePlacedItem(unusedArtifacts,marketBasket);
    	}
    	setContinuation(new Continuation(Benefit.Artifactx2for1,dest,Function.DropWorkerNakagawaTheTribute,p));   			
    }
    boolean recruitAppliesToCurrentPlayer(RecruitChip ch)
    {
    	return(players[whoseTurn].recruitAppliesToMe(ch));
    }
    // reached from market placements
    private void dropWorkerNakagawaTheTribute(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerNakagawaTheTribute");
		// always continue at flavio the merchant		
		if(p.recruitAppliesToMe(RecruitChip.NakagawaTheTribute) 
				&& (p.authorityTokensRemaining()>0)
				&& (p.totalWorkers>1)
				&& canPlaceAuthorityToken(p,dest,replay)
				&& getAvailableAuthorityCell(p,dest.allegiance)!=null)
			{
			// can sacrifice this worker for an extra *
			setContinuation(new Continuation(RecruitChip.NakagawaTheTribute,dest,
						Function.DoNakagawaTheTribute,Function.DropWorkerFlavioTheMerchant,p));
			}
		else { dropWorkerFlavioTheMerchant(dest,replay); }
    }

    private boolean hasHighestKnowledge(EuphoriaCell al[],EuphoriaCell dest)
    {	
    	int know = dest.topChip().knowledge();
    	for(EuphoriaCell c : al)
    	{	if(c!=dest) 
    		{	EuphoriaChip ch = c.topChip();
    			if((ch!=null) && (ch.knowledge()>=know)) { return(false); }
    		}
    	}
    	return(true);
    }
    
    // reached from commodity production sites
    private void dropWorkerScarbyTheHarvester(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerScarbyTheHarvester");
		
		if((dest.rackLocation()==EuphoriaId.WastelanderFarm)
    			&& p.recruitAppliesToMe(RecruitChip.ScarbyTheHarvester)
    			&& hasHighestKnowledge(wastelanderFarm,dest))
    	{	Benefit residual = p.collectBenefit(Benefit.KnowledgeOrFood,replay);
    		if(residual!=null)
    		{
    			setContinuation(new Continuation(residual,Function.Return,p));
    			return;
    		}
    		else { logGameEvent(ScarbyTheHarvesterFood); }
    	}
    }
    
    private void doNakagawaTheTribute(EuphoriaCell dest,replayMode replay)
    {  	// sacrifice the worker, gain an extra authority token.
    	assert_isV12();
    	addToFinalPath("doNakagawaTheTribute");
		EPlayer p = players[whoseTurn];
    	Benefit bene = dest.placementBenefit;
    	Benefit extra = bene.extraStar();
		checkBrettTheLockPicker(dest,replay);
		checkEsmeTheFireman(dest,replay);	// trade card for stuff
    	p.sacrificeWorker(dest,replay);
     	logGameEvent(NakagawaTheTributeEffect);
     	Benefit residual = p.collectBenefit(extra,replay);
     	if(residual!=null)
     	{	if(revision>=106)
     		{
     		throw Error("Nakagawa didn't succeed");
     		}
     	}
	 }
    

    // reached from non-icarite market pacements
    private void dropWorkerFlavioTheMerchant(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerFlavioTheMerchant");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color)
    			&& p.canPayX(Cost.Moralex2)
    			&& p.recruitAppliesToMe(RecruitChip.FlavioTheMerchant) )
			{
    		// can sacrifice this worker for an extra *
    		setContinuation(new Continuation(RecruitChip.FlavioTheMerchant,dest,
					Function.DoFlavioTheMerchant,Function.DropWorkerJackoTheArchivist,p));
			return;
			}
    	 if(p.canPayX(Cost.Knowledge)
    			 && p.recruitAppliesToMe(RecruitChip.FlavioTheMerchant_V2))
    	 {
    	   		// can sacrifice this worker for an extra *
     		setContinuation(new Continuation(RecruitChip.FlavioTheMerchant_V2,dest,
 					Function.DoFlavioTheMerchant,Function.DropWorkerJackoTheArchivist,p));
     		return;
    	 }
 
    	 dropWorkerJackoTheArchivist(dest,replay); 
    }
    
    // pay 2 morale
    private void doFlavioTheMerchant(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	assert_isV12();
    	addToFinalPath("doFlavioTheMerchant");
    	if(active==RecruitChip.FlavioTheMerchant)
    	{
    	if(doLoseMorale(players[whoseTurn],2,replay,dest,Function.DoFlavioTheMerchantGainCard))
    		{ doFlavioTheMerchantGainCard(dest,replay); 
    		  logGameEvent(FlavioTheMerchantEffect,currentPlayerColor());
    		}
    	}
    	else
    	{	EPlayer p = players[whoseTurn];
    		p.payCostOrElse(Cost.Knowledge,replay);
    		doFlavioTheMerchantGainCard(dest,replay); 
    		logGameEvent(FlavioTheMerchantEffect,currentPlayerColor());
    	}
    }
    // gain a card
    private void doFlavioTheMerchantGainCard(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doFlavioTheMerchantCard");
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	doCardsGained(p,replay,dest,Function.DropWorkerJackoTheArchivist);
    }
    
    // jacko lets you place 2 stars for an additional 2 cards.
    // this is v1 - in v2 this power doesn't exist
    private void dropWorkerJackoTheArchivist(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJackoTheArchivist");
    	if(p.recruitAppliesToMe(RecruitChip.JackoTheArchivist)
    			&& p.canPayX(Cost.Artifactx2)
    			&& canPlaceAuthorityToken(p,dest,replay))
    	{	
    		setContinuation(new Continuation(RecruitChip.JackoTheArchivist,dest,
    					Function.DoJackoTheArchivist,Function.Return,p));
     	}
    }
    
    // pay the cost and collect the benefit
    private void doJackoTheArchivist(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doJackoTheArchivist");
		Cost residual = p.payCost(Cost.Artifactx2,replay);
		if(residual!=null)
    	{
    		setContinuation(new Continuation(residual,dest,Function.DoJackoTheArchivistStar,p));
    	}
    	else { doJackoTheArchivistStar(dest,replay); }
    }
    
    private void checkEsmeTheFireman(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	boolean isEsmev1 = false;
    	if((p.artifacts.height()>0)
			&& ((isEsmev1 = p.recruitAppliesToMe(RecruitChip.EsmeTheFireman))
					|| p.recruitAppliesToMe(RecruitChip.EsmeTheFireman_V2)))
		{	RecruitChip recruit = (isEsmev1 ? RecruitChip.EsmeTheFireman : RecruitChip.EsmeTheFireman_V2);
			setContinuation(new Continuation(recruit,dest,Function.DoEsmeTheFireman,Function.Return,p));
		}
    }
    private void doEsmeTheFireman(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	Cost residual = p.payCost(Cost.Artifact,replay);
    	if(residual!=null)
    	{
    		setContinuation(new Continuation(residual,dest,active,Function.DoEsmeTheFiremanPaid,p));
    	}
    	else { doEsmeTheFiremanPaid(dest,active,replay); }
    }
    private void doEsmeTheFiremanPaid(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	boolean paydouble = (active==RecruitChip.EsmeTheFireman_V2) 
    							&& (usedArtifacts.topChip()==ArtifactChip.Book);
    	p.collectBenefitOrElse(paydouble ? Benefit.Energyx2 : Benefit.Energy,replay);
    	marketBasket.addChip(RecruitChip.getMorale(p.color));
    	marketBasket.addChip(RecruitChip.getKnowledge(p.color));
     	Assert(droppedDestStack.size()==0,"empty");
     	Benefit bene = paydouble
     						? ((revision>=121) ? Benefit.Moralex2AndKnowledgex2 : Benefit.Moralex2OrKnowledgex2)
     						:Benefit.MoraleOrKnowledge;
     	Benefit residual = p.collectBenefit(bene,replay);
    	if(residual!=null)
    	{
    		setContinuation(new Continuation(residual,Function.Return,p));
    	} 
    }
    // actually collect the benefit
    private void doJackoTheArchivistStar(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	Benefit bene = dest.placementBenefit;
    	Benefit extra = bene.extraStar();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doJackoTheArchivistStar");
		logGameEvent(JackoTheArchivistEffect);
    	checkEsmeTheFireman(dest,replay);
    	Benefit residual = p.collectBenefit(extra,replay);
    	if(residual!=null)	// if we can't collect the benefit directly, schedule an interaction
    		{	
    		setContinuation(new Continuation(residual,dest,Function.Return,p));
    		}
    }
    /**
     * check morale after cards have been gained, and trigger a card giveaway if necessary
       Also check for JonathanTheGambler and trigger his dialog if appropriate
     * @param replay
     * @param dest
     * @param cont
      */

    private void doCardsGained(EPlayer p,replayMode replay,EuphoriaCell dest,Function cont)
    {	
    	if(cont!=null) 
    		{ setContinuation(new Continuation(dest,EuphoriaState.ExtendedBenefit, cont,p));
    		  setWhoseTurnTo(p.boardIndex,"cardsGained"); 
    		}
     	if(p.testTFlag(TFlag.AddedArtifact)) 
     	{	
     		if(!p.testTFlag(TFlag.AskedJonathanTheGambler))
     		{
     		boolean usedJonathan = 
     				(revision>=124)
     					? p.testTFlag(TFlag.UsedJonathanTheGamblerThisTurn)
     					: p.testTFlag(TFlag.UsedJonathanTheGamblerThisWorker);
     		if(!usedJonathan
     			&& p.recruitAppliesToMe(RecruitChip.JonathanTheGambler_V2))
     		{
     		p.setTFlag(TFlag.AskedJonathanTheGambler);
     		addToFinalPath("Planning DoJonathanTheGambler");
      		// old bug allows to use twice with doubles!
     		setContinuation((dest==null) 
					? new Continuation(RecruitChip.JonathanTheGambler_V2,Function.DoJonathanTheGambler,Function.MoraleCheck,p)
					: new Continuation(RecruitChip.JonathanTheGambler_V2,dest,Function.DoJonathanTheGambler,Function.MoraleCheck,p));
     		}
     		else if(!usedJonathan
     				&& p.recruitAppliesToMe(RecruitChip.JonathanTheGambler)
     				&& p.canPayX(Cost.Knowledge))
    		{	
     		p.setTFlag(TFlag.AskedJonathanTheGambler);
     		addToFinalPath("Planninning DoJonathanTheGambler");
    			setContinuation((dest==null) 
    					? new Continuation(RecruitChip.JonathanTheGambler,Function.DoJonathanTheGambler,Function.MoraleCheck,p)
    					: new Continuation(RecruitChip.JonathanTheGambler,dest,Function.DoJonathanTheGambler,Function.MoraleCheck,p));
    		}
    		else { doMoraleCheck(p,replay,dest,Function.Return);
    		}}
     		else {
     			doMoraleCheck(p,replay,dest,Function.Return);
     		}
    	}
     	
    }
    private boolean doCaryTheCarebear(EPlayer p,replayMode replay,EuphoriaCell dest,Function continuation)
    {	assert_isIIB();
    	int bgain = p.bearsGained;
    	p.bearsGained = 0;
    	// this is deferred to after the morale check, so the resource claim 
    	// will be before the cards discarded due to morale
    	if((bgain>0)
    			&& p.recruitAppliesToMe(RecruitChip.CaryTheCarebear))
				{
    			// if(p.morale<p.artifacts.height()) { p1("use cary the carebear and discard"); }
    			logGameEvent(UseCaryTheCarebear);
    			useRecruit(RecruitChip.CaryTheCarebear,"use");
    			switch(bgain)
    			{
    			case 1:    			
    				p.collectBenefitOrElse(Benefit.Food,replay);
    				//p1("use cary the care bear");
    				setContinuation(new Continuation(Benefit.Commodity,dest,continuation,p));
    				setWhoseTurnTo(p.boardIndex,"CaryTheCarebear");
    				return false;
    			case 2:
    				p.collectBenefitOrElse(Benefit.Foodx2,replay);
    				//p1("use cary the care bear 2");
       				setContinuation(new Continuation(Benefit.Commodityx2,dest,continuation,p));
       				return false;
       			default: Error("shouldn't happen");
				}
				}
    	return true;
    }
    
    private void doJonathanTheGambler(EPlayer p,EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	assert_isV12();
		if(revision>=124) 
		{ p.setTFlag(TFlag.UsedJonathanTheGamblerThisTurn); 
		}
		p.setTFlag(TFlag.UsedJonathanTheGamblerThisWorker);
		if(active==RecruitChip.JonathanTheGambler_V2)
    	{	p.collectBenefitOrElse(Benefit.Artifact,replay);
    		logGameEvent(JonathanGain2);
    		if(p.hasArtifactPair()!=null)
    		{	// no pairs, we get to keep it
    			for(EPlayer op : players)
    			{
    				if(op!=p)
    				{	logGameEvent(JonathanTheGamblerBonus,op.color.toString());
    					op.doArtifact(1,this,replay); 	
    					// check morale for everybody who gets a card
    					doMoraleCheck(op,null,replay);

       				}
    			}
    		}
    	}
    	else if(active==RecruitChip.JonathanTheGambler)
    	{
    	EuphoriaChip top = p.artifacts.removeTop();
    	EuphoriaChip t2 = getArtifact();
    	EuphoriaChip t3 = getArtifact();
    	p.incrementKnowledge(replay);
    	if(replay!=replayMode.Replay)
    		{
    		animateNewArtifact(p.artifacts);
    		animateNewArtifact(p.artifacts);
    		}
    	if(top==t2)
    		{ p.addArtifact(top); 
    		  p.addArtifact(t2);
    		  if(top==t3) 
    		  	{
    			// jackpot, got all 3
    			p.addArtifact(t3); 
    		  	logGameEvent(Gained3Cards);
    		  	}
    		  else { 
    			    recycleArtifact(t3);
    			  	logGameEvent(JonathanGain2);
    			  	if(replay!=replayMode.Replay)
    		  		{
    			  	animateReturnArtifact(p.artifacts);
    		  		}
    		  }
    		  }
    	else if(top==t3)
    		{
    		p.addArtifact(top);
    		p.addArtifact(t3);
    		recycleArtifact(t2);
  		  	logGameEvent(Gained2Cards);
	    		if(replay!=replayMode.Replay)
	    		{
	  		  	  animateReturnArtifact(p.artifacts);
	    		}
    		}
    	else if(t2==t3)
    		{ p.addArtifact(t2);
    		  p.addArtifact(t3); 
    		  recycleArtifact(top);
    		  logGameEvent(Gained2Cards);
    		  if(replay!=replayMode.Replay)
    		  	{ 
    		  	  animateReturnArtifact(p.artifacts);
    		  	}
    		  }
    		  else { 
    			  recycleArtifact(top);
    			  recycleArtifact(t2);
    			  recycleArtifact(t3);
    			  logGameEvent(Gained0Cards);
    			  if(replay!=replayMode.Replay) 
    			  	{  
    			  	  animateReturnArtifact(p.artifacts);
      			  	  animateReturnArtifact(p.artifacts);
    			  	  animateReturnArtifact(p.artifacts);
    			  	}
    		  }
    	}
    	else { throw Error("not expecting %s",active); }
    	doMoraleCheck(p,replay,dest,Function.Return);	// trigger a morale check
    }
    private void doSheppardTheLobotomistMorale(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	if(doLoseMorale(players[whoseTurn],2,replay,dest,Function.DoSheppardTheLobotomistGainCard))
    		{ doSheppardTheLobotomistGainCard(dest,replay); 
    		}
    }
    
    private void doSheppardTheLobotomistGainCard(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(SheppardTheLobotomistRoll,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.Return);
    }
    
    /** julia the thought inspector trades morale for a resource 
     * NOT reached from commodity production sites, reached from all other worker placements
      * */
    private void dropWorkerJuliaTheThoughtInspector(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJuliaTheThoughtInspector");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color))
    	{	if(p.recruitAppliesToMe(RecruitChip.JuliaTheThoughtInspector)
    			&& (bumpedWorker.knowledge()==bumpingWorker.knowledge())
    			&& p.canPayX(Cost.Moralex2))
    		{
    		setContinuation(new Continuation(RecruitChip.JuliaTheThoughtInspector,dest,
    											Function.DoJuliaTheThoughtInspector,
    											Function.DropWorkerRebeccaThePeddler,p));
    		return;
    		}
    	}
    	dropWorkerRebeccaThePeddler(dest,replay); 
    }
    private void doJuliaTheThoughtInspector(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	addToFinalPath("doWorkerJuliaTheThoughtInspector");

    	setContinuation(new Continuation(Benefit.Resource,dest,Function.DropWorkerRebeccaThePeddler,null));

    	logGameEvent(JuliaTheThoughtInspectorEffect,currentPlayerColor());
     	doLoseMorale(players[whoseTurn],2,replay,null,Function.Return);
   	}

   private void doJuliaTheThoughtInspector_V2(replayMode replay)
   {	assert_isV12();
   		addToFinalPath("doWorkerJuliaTheThoughtInspector+v2");
		logGameEvent(JuliaTheThoughtInspectorV2Effect,currentPlayerColor());
		selectedDieRoll = WorkerChip.getWorker(bumpedWorker.color,bumpingWorker.knowledge());
		logGameEvent(JuliaTheThoughtInspectorV2RollEffect,s.get(bumpedWorker.color.name()),""+selectedDieRoll.knowledge());
      	doLoseMorale(players[whoseTurn],1,replay,null,Function.Return);
     	setContinuation(new Continuation(Benefit.Resource,Function.Return,null));
       	reRollWorkers(getPlayer(selectedDieRoll.color),replay,selectedDieRoll,Function.Return);
    }
    
    // reached from all worker placements except commodity production
    private void dropWorkerRebeccaThePeddler(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerRebeccaThePeddler");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color)
    			&& p.canPayX(Cost.Moralex2)
    			&& p.recruitAppliesToMe(RecruitChip.RebeccaThePeddler) )
		{
		// can sacrifice this worker for an extra *
		setContinuation(new Continuation(RecruitChip.RebeccaThePeddler,dest,
					Function.DoRebeccaThePeddler,Function.DropWorkerPhilTheSpy,p));
		}
    	else { dropWorkerPhilTheSpy(dest,replay); }
    	
    }
    void doRebeccaThePeddler(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer other = getPlayer(bumpedWorker.color);
    	EPlayer p = players[whoseTurn];
    	other.incrementKnowledge(replay);
    	setContinuation(new Continuation(Benefit.Resource,dest,Function.DropWorkerPhilTheSpy,p));
    	
    	logGameEvent(RebeccaThePeddlerOtherEffect,other.color.name());
    	logGameEvent(RebeccaThePeddlerEffect,currentPlayerColor());
    	doLoseMorale(p,2,replay,null,Function.Return);
     }
    
    // reached from all worker placements except commodity production
    private void dropWorkerPhilTheSpy(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	boolean recruitIsPhil = false;
    	addToFinalPath("dropWorkerPhilTheSpy");
    	if((bumpedWorker!=null) 
    			&& !getPlayer(bumpedWorker.color).hasActiveRecruit(Allegiance.Subterran)
       			&& ((recruitIsPhil = p.recruitAppliesToMe(RecruitChip.PhilTheSpy)) 
       				|| p.recruitAppliesToMe(RecruitChip.PhilTheSpy_V2))
    			&& p.canPayX(recruitIsPhil?Cost.Knowledgex2:Cost.Knowledge)
    			)
		{	// pay 2 morale and gain a card
    		// v2 is the same except pay 1
		setContinuation(new Continuation( recruitIsPhil?RecruitChip.PhilTheSpy:RecruitChip.PhilTheSpy_V2,
						dest,
						Function.DoPhilTheSpy,	
						Function.DropWorkerLeeTheGossip,p));
		}
		else { 	dropWorkerLeeTheGossip(dest,replay); }
    }
    
    private void doPhilTheSpy(EuphoriaCell dest,replayMode replay,RecruitChip active)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	p.payCostOrElse(active==RecruitChip.PhilTheSpy?Cost.Knowledgex2:Cost.Knowledge,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	logGameEvent(PhilTheSpyEffect,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DropWorkerLeeTheGossip);
     }
    // reached from all worker placements except commodity production
    private void dropWorkerLeeTheGossip(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerLeeTheGossip");
    		if((bumpedWorker!=null) 
    			&& (bumpedWorker.color!=p.color) 
    			&& p.recruitAppliesToMe(RecruitChip.LeeTheGossip) 
    			&& p.canPayX(Cost.Morale))
		{	// pay 2 morale and gain a card
		setContinuation(new Continuation( RecruitChip.LeeTheGossip, dest,Function.DoLeeTheGossip,	Function.DropWorkerMaximeTheAmbassador,p));
		}
		else { 	dropWorkerMaximeTheAmbassador(dest,replay); }
    }
    private void doLeeTheGossip(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer other = getPlayer(bumpedWorker.color);
    	EPlayer p = players[whoseTurn];
    	other.incrementKnowledge(replay);
       	setContinuation(new Continuation(Benefit.Commodity,
       							dest,
       							Function.DropWorkerMaximeTheAmbassador,p));
       	doLoseMorale(p,1,replay,null,Function.Return);
       	logGameEvent(LeeTheGossipOtherEffect,other.color.name());
       	logGameEvent(LeeTheGossipEffect,currentPlayerColor());
    }

    // reached from all worker placements except commodity production
    private void dropWorkerMaximeTheAmbassador(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerMaximeTheAmbassador");
   	
    	if((bumpedWorker!=null) 
    			&& (bumpedWorker.color!=p.color) 
    			&& p.recruitAppliesToMe(RecruitChip.MaximeTheAmbassador) 
    			&& p.canPayX(Cost.Moralex2))
		{	// pay 2 morale and gain a card
		setContinuation(new Continuation( RecruitChip.MaximeTheAmbassador, dest,Function.DoMaximeTheAmbassador,	Function.Return,p));
		}
    }

    // 
    // do "Maxime the Ambassador
    // lose 2 morale, gain 1 card, the other player gains 1 knowlege
    //
    public void doMaximeTheAmbassador(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer other = getPlayer(bumpedWorker.color);
	    other.incrementKnowledge(replay);			// other player gets knowledge
	    logGameEvent(MaximeTheAmbassadorOther,other.color.name());
	    logGameEvent(MaximeTheAmbassadorEffect,currentPlayerColor());
	    
    	if(doLoseMorale(players[whoseTurn],2,replay,dest,Function.DoMaximeTheAmbassadorGainCard))
    	{
    		doMaximeTheAmbassadorGainCard(dest,replay);
    	}
    }
    private void doMaximeTheAmbassadorGainCard(EuphoriaCell dest,replayMode replay)
    {	assert_isV12();
    	EPlayer p = players[whoseTurn];
    	// technically maybe have to do 2 morale checks, one after the decrements,
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	doCardsGained(p,replay,null,null);
    }
    
    // reached after all worker placements
    public void dropWorkerOpenMarkets(EuphoriaCell dest,replayMode replay)
    {	
    	bumpedWorker = null;
    	bumpingWorker = null;
    	selectedDieRoll = null;
    	addToFinalPath("dropWorkerOpenMarkets");
      	for(int i=0;i<markets.length;i++)
    	{
    		if(marketIsReadyToOpen(i)) 
    			{ openMarket(i,replay); 
    			}
    	}
      	
		EPlayer p = getPlayer(whoseTurn);
	
		if(p.recruitAppliesToMe(RecruitChip.SamuelTheZapper)
				&& p.canPay(Cost.Energy)
				&& p.hasCommodityWorkers()
				)
		{	// after the benefit is received, check to use the power
			// note that trying to incorporate this before the open markets check
			// proved to be difficult.
			setContinuation(new Continuation(RecruitChip.SamuelTheZapper,dest,Function.DoSamuelTheZapper,Function.ProceedWithTheGame,p));
		}


      	proceedWithTheGame(replay);
    }
    
    public void dropWorker(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	finalPath.clear();
    	addToFinalPath("dropWorker");
    	lastDroppedWorker = dest;
    	hasPlacedWorker = true;
    	/*if((dest==icariteBreezeBar) && (p.totalCommodities()>4) && (p.nKindsOfCommodity()>2))
			{
				p1("drop on breeze bar loaded");
			}*/

		WorkerChip worker = (WorkerChip)dest.topChip();
		boolean wishfulThinking = p.penaltyAppliesToMe(MarketChip.FountainOfWishfulThinking);
		int nworkers = (p.hasWorkersLike(worker));
		if((nworkers>0) && wishfulThinking)
		{
			MarketChip.FountainOfWishfulThinking.logGameEvent(this);
		}
		{
 		doublesElgible = ((nworkers>0) && !wishfulThinking)
 							? worker : null;		// test for doubles before bumping  
 		if(!usingDoubles) { doublesCount = nworkers;  }
		}
 		acceptPlacement();
    	dropWorkerPay(dest,replay);	// must pay first, so nothing can steal the resources we have to pay for placement.
    	
    }
      	
    private void dropWorkerPay(EuphoriaCell dest,replayMode replay)
    {	
     	//
    	// payCost considers the alternate capabilities of the recruits
    	//

    	addToFinalPath("dropWorkerPay/open market continuation");
       	setContinuation(new Continuation(dest,EuphoriaState.ExtendedBenefit,Function.DropWorkerOpenMarkets,null));
       	dropWorkerPay1(dest,replay);
    }
    private void dropWorkerPayCard(EPlayer p,EuphoriaCell dest,replayMode replay)
    {	addToFinalPath("dropWorkerPay for Artifact "+pendingBenefit());
    	//reload the artifact market
    	// market shouldn't have been refilled yet
    	Assert(dest.topChip()==null,"artifact market already refilled");
    
    	if(p.recruitAppliesToMe(RecruitChip.PamhidzaiTheReader))
		{
    	EuphoriaChip top = p.artifacts.topChip();
    	if((top==ArtifactChip.Book)||(top==ArtifactChip.Bifocals))
    	{
		//p1("use pamhidzai pay");
		finishContinuation(replay);	// finish this continuation, in place of the doContinuation below
		setContinuation(new Continuation(RecruitChip.PamhidzaiTheReader,dest,Function.DoPamhidzai,Function.DontPamhidzai,p));
		setState(EuphoriaState.RecruitOption);
		return;
		}}
    	refillArtifactMarket(replay);
    	doContinuation(replay); 
    }
    private void refillArtifactMarket(replayMode replay)
    {	if(isIIB())
    	{  	
    	for(int left = 0,len = artifactBazaar.length; left<len; left++)
    	{	// start at the left, find an empty slot, fill it by sliding everything left of it
    		if(artifactBazaar[left].topChip()==null)
        	{
	    	for(int lim = left; lim>=1; lim--)
	    	{	EuphoriaCell dest2 = artifactBazaar[lim];
	    		if(dest2.topChip()==null) 
	    		{	EuphoriaCell src = artifactBazaar[lim-1];
	    			Assert(src.topChip()!=null,"Artifacts not filled");
	    			artifactBazaar[lim].addChip(src.removeTop());
	    			if(replay!=replayMode.Replay)
	    			{	animationStack.push(src);
	    				animationStack.push(dest2);
	    			}
	    		}}
	    	// then refill the left slot
	    	EuphoriaCell dest2 = artifactBazaar[0];
	    	dest2.addChip(getArtifact());
	    	if(replay!=replayMode.Replay)
    			{
    				animationStack.push(unusedArtifacts);
    				animationStack.push(dest2);
    			}
    		}
    	}}
    }
    
    	
    private void dropWorkerPay1(EuphoriaCell dest,replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
     	if(usingDoubles && revision>=124 && !p.testTFlag(TFlag.HasLostMorale))
       	{	// theoretically this should never happen any more
     		// if(p.morale<=p.artifacts.height()) { p1("doubles need to lose a card"); }
       		if(!doLoseMorale(p,1,replay,dest,Function.DropWorkerPay2))
       		{	
       			return;
       		}  		
       	}
       	dropWorkerPay2(p,dest,replay);
    }
    private void dropWorkerPay2(EPlayer p,EuphoriaCell dest,replayMode replay)
    {
        	Cost originalcost = dest.placementCost;
       	

    	if((dest!=null) 
    			&& (dest.allegiance==Allegiance.Icarite)
    			&& p.recruitAppliesToMe(RecruitChip.BrianTheViticulturist_V2))
    	{
    		logGameEvent(BrianTheViticulturistMorale,currentPlayerColor());
    	}
    	
    	Cost residual = p.payCost(dest,replay);

    	if(residual!=null)
    	{	Cost cost = residual;
    		// extract the fixed part of the cost, change the cost to just the remainder
    		switch(cost)
    		{
    		case Morale_Artifactx3_Brian:
    			// unusual case for Brian the Viticulturist V2, we have to lose a card before paying 3 cards (or a pair)
    			setContinuation(new Continuation(originalcost,Cost.Artifactx3,dest,Function.DropWorkerWithoutPayment,
        							Function.DropWorkerBump));
    			if(p.canPay(Cost.Smart_Artifact))
    			{	// special case, we have to be sure to discard the right card
    				// discard the card first, then extract the morale penalty which will not interact
    				logGameEvent(BrianTheVitculturistLoseCard);
    				p.payCostOrElse(Cost.Smart_Artifact,replay);
    			}
    			// otherwise, the normal interaction might ensue when we lose morale
    			doLoseMorale(p,1,replay,dest,Function.Return);
    			return;
 
			default:
				break;
    			 	
      		}
    		setContinuation(new Continuation(originalcost,cost,dest,Function.DropWorkerWithoutPayment,
    				Function.DropWorkerBump));
     	}
    	else 
    	{   
     		if(p.recruitAppliesToMe(RecruitChip.KadanTheInfiltrator)
    		   && (p.alternateCostForKadanTheInfiltrator(originalcost)==Cost.Knowledgex2))
			{
    		// we used kadan to make this placement
			logGameEvent(KadanTheInfiltratorEffect,currentPlayerColor());
			}
    	dropWorkerBump(dest,replay); 

    	if(p.testTFlag(TFlag.UsedJackoTheActivist))
    		{	p.clearTFlag(TFlag.UsedJackoTheActivist);
    			finishJackoTheArchivist(players[whoseTurn],replay);
    		}

    	}
    	doMoraleCheck(p,replay,dest,Function.Return);
    }
    private void dropWorkerWithoutPayment(EuphoriaCell dest,replayMode replay)
    {	// this is the unusual case where a payment for placement is optional
    	
    	if(!variation.isIIB())
    	{
    	addToFinalPath("dropWorkerWithoutPayment");
    	EPlayer p = players[whoseTurn];
    	switch(dest.placementCost)
    	{
    	case Water:
    	case Food:
     	case Energy:
     		Assert(p.knowledge<MAX_KNOWLEDGE_TRACK,"should be able to gain knowledge");
    		p.incrementKnowledge(replay);
    		logGameEvent(MatthewTheThiefEffect,dest.placementCost.name(),currentPlayerColor());
    		break;
    	
    	default: throw Error("not expecting %s",dest.placementCost);
    	}
    	}
    	
    	dropWorkerBump(dest,replay);
    }
    

    private void dropWorkerBump(EuphoriaCell dest,replayMode replay)
    {	acceptPlacement();
    	EPlayer p = players[whoseTurn];
    	if(p.getTriggerPedroTheCollector())
			{	//p1("trigger pedro the collector "+lastDroppedWorker.rackLocation());
				logGameEvent(UsePedroTheCollector);
				useRecruit(RecruitChip.PedroTheCollector,"use");
				setContinuation(new Continuation(Benefit.ResourceAndCommodity,dest,Function.DropAfterPedroTheCollector,p));
				return;
			}
    	dropAfterPedroTheCollector(p,dest,replay);
    }
   private void doTheBumping(EuphoriaCell dest)
   {	bumpingWorker = (WorkerChip)dest.topChip();	// this does the right thing even for "self-bump" by GeorgeTheLazyCraftsman
		WorkerChip worker = bumpedWorker = (WorkerChip)dest.removeChipAtIndex(0);
    	EPlayer otherPlayer = getPlayer(worker.color);
    	otherPlayer.unPlaceWorker(dest);
    	otherPlayer.addNewWorker(worker);			// add to the new workers pool, will trigger re-roll and knowledge check.
   }
   private void dropAfterPedroTheCollector(EPlayer p,EuphoriaCell dest,replayMode replay)
   {
    	
    	EuphoriaId rack = dest.rackLocation();
    	addToFinalPath("dropWorkerBump");
    	Assert(dest!=null,"dest shouldn't be null here");
    	
    	setContinuation(new Continuation(dest,EuphoriaState.ExtendedBenefit, Function.DropWorkerAfterBump,null));
    	
    	if(rack.canBeBumped && (dest.height()>1))
    	{	// make the worker space empty if appropriate
    		doTheBumping(dest);

    		if(variation.isIIB()) { bumpWorkerIIB(dest,replay); }
    		else { bumpWorker(dest,replay); }
    	}
    	else 
    	{	// placing without a bump
			if(variation.isIIB())
    		{
    		switch(rack)
    		{
    		case EuphorianGenerator:
    		case WastelanderFarm:
    		case IcariteCloudMine:
    		case SubterranAquifer:
    				if(p.recruitAppliesToMe(RecruitChip.JedidiahTheInciter)
    					&& hasOpponentWorker(dest,p))
    				{	//p1("ask jedidiah "+dest.rackLocation());
    					setContinuation(new Continuation(RecruitChip.JedidiahTheInciter,dest,Function.DoJedidiahTheInciter,Function.Return,p));
    				}
    				break;
    		default: break;
    			
    		}
    		}
    		else
    		{
			switch(rack)
    		{
    		case EuphorianMarketA:
    		case EuphorianMarketB:
    		case SubterranMarketA:
    		case SubterranMarketB:
    		case WastelanderMarketA:
    		case WastelanderMarketB:
    		case EuphorianUseMarket:
    		case SubterranUseMarket:
    		case WastelanderUseMarket:
    			// here we know there's no bump, but don't know if there's a token available
    			checkBrettTheLockPicker(dest,replay);
    			checkEsmeTheFireman(dest,replay);
    			break;
    		case IcariteNimbusLoft:
    		case IcariteWindSalon:
    			if(revision>=112) { checkEsmeTheFireman(dest,replay); }
    			break;
    		case EuphorianTunnelMouth:
    		case WastelanderTunnelMouth:
    		case SubterranTunnelMouth:
    			{
    			if(p.canPayX(Cost.Knowledgex2)
    					&& p.recruitAppliesToMe(RecruitChip.ReitzTheArcheologist))
    					{
    					setContinuation(new Continuation(RecruitChip.ReitzTheArcheologist,dest,
    							Function.DoReitzTheArcheologist,Function.Return,p));
    					
    					return;
    					
    					}
    			}

				break;
			default: break;
    		}
    		}
    	}
    }
    private void doJedidiahTheInciter(EPlayer p,EuphoriaCell dest,replayMode replay)
    {	//p1("use jedidiah the inciter");
    	setState(EuphoriaState.BumpOpponent);
    }
   
   
    private void doNakagawaTheTribute_V2(EuphoriaCell dest,replayMode replay)
    {	// unrelated to the v1 NakagawaTheTribute
    	incrementAllegiance(Allegiance.Euphorian,replay);
    }
    private void checkBrettTheLockPicker(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(bumpedWorker==null)
    	{
    	if(p.canPayX(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.BrettTheLockPicker)
				)
  			{
				setContinuation(new Continuation(RecruitChip.BrettTheLockPicker,dest,
						Function.DoBrettTheLockPicker,Function.Return,p));
			}
    	if(p.canPayX(Cost.Knowledge)
			&& p.recruitAppliesToMe(RecruitChip.BrettTheLockPicker_V2)
			)
  			{
				setContinuation(new Continuation(RecruitChip.BrettTheLockPicker_V2,dest,
						Function.DoBrettTheLockPicker,Function.Return,p));
			}
    	}
    }
    public boolean checkStevenTheScholar(EPlayer p,replayMode replay,boolean reroll)
    {	// called from incremented knowledge, doesn't apply to steventhescholar_v2
    	if(p.recruitAppliesToMe(RecruitChip.StevenTheScholar)
    			|| (reroll && p.recruitAppliesToMe(RecruitChip.StevenTheScholar_V2)))
	  				{	// steven the scholar v2 applies to rerolls but not to knowledge increases,
	  					// but the actual function is identical
	  					setContinuation(new Continuation(Function.DoStevenTheScholar,p.color,Function.Return));
	  					return(true);
	  				}       		
	  	return(false);
    }

    private void doBrettTheLockPicker(EuphoriaCell dest,replayMode replay,RecruitChip active,Function normalContinuation)
    {	EPlayer p = players[whoseTurn];
    	p.incrementKnowledge(replay);
    	// v1 costs 2 knowledge, v2 charges only 1 knowledge
    	if(active==RecruitChip.BrettTheLockPicker) { p.incrementKnowledge(replay); }
    	logGameEvent(BrettTheLockpickerEffect,currentPlayerColor());
    	setContinuation(new Continuation(Benefit.Resource,dest,normalContinuation,p));
    }
    private void doReitzTheArcheologist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doReitzTheArcheologist");
		p.payCostOrElse(Cost.Knowledgex2,replay);
    	p.collectBenefitOrElse(Benefit.Artifact,replay);
    	incrementTunnelPosition(dest.allegiance);
    	logGameEvent(ReitzTheArcheologistEffect,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.Return);
    }
    private boolean highestMarketKnowledge(EuphoriaId rack,EuphoriaCell dest)
    {	int myKnowledge = ((WorkerChip)(dest.topChip())).knowledge(); 
    	for(EuphoriaCell producer : getBuilderArray(rack))
    		{
    		if((producer!=dest) && (producer.height()>0))
    		{
			WorkerChip worker = (WorkerChip)producer.topChip();
			if(worker.knowledge()>=myKnowledge) { return(false); }
    		}}
    	return(true);
    }
    private void dropWorkerAfterBump(EuphoriaCell dest,replayMode replay)
    {	
    	EuphoriaId rack = dest.rackLocation();
		addToFinalPath("dropWorkerAfterBump");
     	Assert(rack.isStackable || (dest.height()==1),"can place worker on %s",dest);
     	Assert(whoseTurn==currentPlayerInTurnOrder,"unexpected turn change");
    	acceptPlacement();

    	dropWorkerCollectBenefit(dest,replay);
    }
    
    //
    // opening markets
    //
    boolean marketIsOpen(int idx) { return(marketIsOpen(markets[idx])); }	// open if we've removed the cover
    boolean marketIsOpen(EuphoriaCell c)   
    { EuphoriaChip chip = c.topChip(); 
      return(chip!=MarketChip.CardBack);  
    }
    // find a currently open market which has the given penalty
    public EuphoriaCell getOpenMarketCell(MarketChip ch)
    {
    	for(EuphoriaCell m : markets)
    	{	MarketChip market = (MarketChip)m.chipAtIndex(0);
    		if(market==ch) 
    			{ return(marketIsOpen(m)?m:null);
    			}
    	}
    	return(null);
    }
    private boolean marketIsReadyToOpen(int idx)
    {	if(!marketIsOpen(idx))
    	{
    	int count = 0;
    	for(EuphoriaCell c : buildMarkets[idx])
    		{
    		if(c.height()>0) { count++; }		// count the occupied cells in the market construction area
    		}
    	if(count>=TOKENS_TO_OPEN_MARKET[players.length])
    		{
    		return(true);
    		}
    	}
    	return(false);
    }
    private EuphoriaChip openedMarket = null;
    private EuphoriaChip marketToOpen = null;
    
    private void openMarket(int idx,replayMode replay)
    {	revealedNewInformation = openedAMarket = true;
		addToFinalPath("openmarket");
		nOpenMarkets++;
		// remove the cap from the market, revealing the market underneath
   		markets[idx].removeTop();
   		if(marketToOpen!=null)
   		{
   			markets[idx].removeTop();
   			markets[idx].addChip(marketToOpen);
   			marketToOpen = null;
   		}
   		openedMarket = markets[idx].topChip();
   		logGameEvent("Market: #1",openedMarket.name);
   		
   		// randomize the market at the time it is actually opened
     	if(variation==Variation.Euphoria3T)
    	{	EuphoriaChip old = markets[idx].removeTop();
    		unusedMarkets.addChip(old);
    		Random r = newRandomizer(1245246);
    		int choice = r.nextInt(unusedMarkets.height());
    		EuphoriaChip add = unusedMarkets.removeChipAtIndex(choice);
    		markets[idx].addChip(add);
    		//G.print("switch market "+old+" to "+add);
    	}
    	// set up the market cell with the new cost
     	MarketChip market = (MarketChip) markets[idx].topChip();
     	useMarkets[idx].placementCost = market.placementCost;		// set the new placement cost
     	
    	// give the workers back to the players
    	for(EuphoriaCell c : buildMarkets[idx]) 
    	{	c.placementCost = Cost.MarketCost;		// this makes the cell unavailable for placement
    		if(c.height()>0) 
    		{	WorkerChip worker = (WorkerChip)c.removeTop();
    			EPlayer p = getPlayer(worker.color);
    			p.unPlaceWorker(c);
    			p.addNewWorker(worker);
    			if(reRollPlayers.pushNew(worker.color))
    			{	EuphoriaChip ch = p.getAuthorityToken(replay);
    				if(ch!=null)
    				{markets[idx].addChip(ch);
    				p.marketStars++;
    				if(replay!=replayMode.Replay) { animatePlacedItem(p.authority,markets[idx]); }
    				}
    				else
    				{	// this is not an error any more. Nakagawathetribute_v2
    					// can increment allegiance, which gives away some stars.
    					//Error("Unexpected - no token available when opening a market");
    				}
    			}
    		}
    	} 
    	for(EPlayer p : players)
    	{	
    		p.newMarketOpened();
    		if(p.recruitAppliesToMe(RecruitChip.RayTheForeman))
    		{	// if we helped, gain morale, if not, lose knowledge
    			if(p.hasMyAuthorityToken(markets[idx]))
    			{
    				p.incrementMorale(replay);
    				p.incrementMorale(replay);
    				logGameEvent(RayTheForemanHelped,currentPlayerColor());
    			}
    			else 
    			{
    			p.decrementKnowledge(replay);
    			p.decrementKnowledge(replay); 
    			logGameEvent(RayTheFormanNoHelp,currentPlayerColor());
    			}
    		}
    	}
    }
    public void setActivePlayer(int n)
    {	activePlayer = n;
    	if(board_state==EuphoriaState.NormalStart)
    	{
    		
    	}
    }
    
    // this is experimental
    void substitutePick(EuphoriaCell to,EuphoriaMovespec m)
    { EuphoriaChip ch = m.chipIn;
    	  if(ch!=null)
    	  {
    		 switch(to.rackLocation())
    		 {
    		 case PlayerActiveRecruits:
    		 case PlayerHiddenRecruits:
    			 unusedArtifacts.removeChip(ch);
				//$FALL-THROUGH$
			case PlayerArtifacts:
    			 if(ch!=pickedObject) 
    			 {
    				 G.print("switching "+pickedObject+" to "+ch);
    				 pickedObject = ch;
    				 break;
    			 }
    			 break;
    			 default: break;
    		 }
    	 }
    	  
    }
    private void doNormalStart()
    {
    	if(!normalStartSeen)
    	{
   		for(EPlayer p : players) { p.transferDiscardedRecruits(usedRecruits); }
   		
   		// remove factionless recruits
   		if(variation==Variation.Euphoria3T)
   		{	// remove only kofi
   			unusedRecruits.removeChip(RecruitChip.KofiTheHermit);
   			while(usedRecruits.height()>0) { unusedRecruits.addChip(usedRecruits.removeTop()); }
   			Random r = newRandomizer(0x63464735);
   			unusedRecruits.shuffle(r);
   		}
   		else
   		{
   		for(int lim=unusedRecruits.height()-1; lim>=0; lim--)
   		{ 	RecruitChip recruit = (RecruitChip)unusedRecruits.chipAtIndex(lim);
   			if(recruit.allegiance==Allegiance.Factionless) 
   				{ unusedRecruits.removeChipAtIndex(lim);
   				}
   		}}
       	normalStartSeen = true;
    	}
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	EuphoriaMovespec m = (EuphoriaMovespec)mm;
    	openedMarket = null;
        if(replay!=replayMode.Replay) { animationStack.clear(); gameEvents.clear(); }
        if(board_state==EuphoriaState.Puzzle)
        {
        	setStatusDisplays();
        }
        //if(!robotBoard) { G.print("A "+m+" for "+whoseTurn+" "+board_state+" #"+moveNumber+" "+Digest()); }
        //G.print("E "+m+" for "+whoseTurn+" "+board_state+" "+Digest()); 
        switch (m.op)
        {
        case MOVE_RECRUIT:
        	{	// for testing purposes, just add a recruit
        		EPlayer p = getPlayer(m.player);
        		RecruitChip newguy = (RecruitChip)m.chipIn;
        		if(newguy==null)
        		{
        		RecruitChip masterlist[] = getMasterRecruitList();  
        		int n = Math.min(m.to_row,masterlist.length-2);
        		Random r = newRandomizer(0x6246264+m.player);
        		for(int i=0;i<n;i++)
        			{
        			newguy = masterlist[r.nextInt(masterlist.length-2)+2];
        			if(!p.activeRecruits.containsChip(newguy))
        				{ p.addActiveRecruit(newguy,replay);
        				logGameEvent("Added recruit #1",newguy.getName());
        				}
        			}
        		}
        		else {
        		unusedRecruits.removeChip(newguy); 
        		p.addActiveRecruit(newguy,replay);
        		logGameEvent("Added recruit #1",newguy.getName());
        		m.chip = newguy;
        		}
        		if(checkGameOver()) {  setState(EuphoriaState.Gameover); }
        	}
        	break;
        case MOVE_MARKET:
        	{
        	EuphoriaCell dest = getCell(m.dest,m.to_row);
        	dest.removeChipAtIndex(0);
        	dest.insertChipAtIndex(0,m.chip);
        	useMarkets[m.to_row].placementCost = ((MarketChip)m.chip).placementCost;
        	}
        	break;
        case MOVE_PEEK:
        	setHasPeeked(m.player,true);
        	break;
		case CONFIRM_DISCARD:
	       	{
	           	EPlayer p = getPlayer(m.from_color);
	        	m.player = p.boardIndex;
	    		p.discardedRecruits.reInit();
	    		p.reloadNewRecruits(p.spareRecruits);
	    		setState(p.countRecruits(Allegiance.Factionless)>1 
	    						? EuphoriaState.DiscardFactionless
	    						: EuphoriaState.ChooseRecruits);
	        	}
    		break;

        case EPHEMERAL_CONFIRM_DISCARD:
        	{
           	EPlayer p = getPlayer(m.from_color);
        	m.player = p.boardIndex;
    		p.discardedRecruits.reInit();
    		p.reloadNewRecruits(p.spareRecruits);
    		setState(p.countRecruits(Allegiance.Factionless)>1 
    						? EuphoriaState.EphemeralDiscardFactionless
    						: EuphoriaState.EphemeralChooseRecruits);
    		break;
        	}
        case EPHEMERAL_CONFIRM_RECRUITS:
 		case EPHEMERAL_CONFIRM_ONE_RECRUIT:
       		{
        	EPlayer p = getPlayer(m.from_color);
        	m.player = p.boardIndex;
  			p.discardNewRecruits(true);
  			moveNumber++; 
   			SIMULTANEOUS_PLAY = true;	// replay of the final NormalStart turns this off
   			setRecruitDialogState(getPlayer(activePlayer));  
       		}
       		break;
        case CONFIRM_RECRUITS:
        case MOVE_DONE:
        	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
        	stepNumber++;
        	doDone(m.chipIn,replay);
        	m.chip = openedMarket;
        	break;
        case NORMALSTART:
        	if(!hasReducedRecruits) { moveNumber--; doDone(null,replay); }
           	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
           	doNormalStart();
           	break;
        case USE_DIE_ROLL:
        	{
        	selectedDieRoll = WorkerChip.getWorker(players[whoseTurn].color,m.source.ordinal()-EuphoriaId.SelectDie1.ordinal()+1);
       		setState(EuphoriaState.ConfirmRecruitOption); 
        	}
        	break;
        case DONT_USE_DIE_ROLL:
        	{
        	selectedDieRoll = null;
        	setState(EuphoriaState.DieSelectOption);
        	}
        	break;
        	
        case USE_SECOND_RECRUIT_OPTION:
        	{
        	Continuation cont = continuationStack.top();
        	WorkerChip top = bumpingWorker;
         	cont.extraChip = WorkerChip.getWorker(bumpedWorker.color,top.knowledge());
        	setState(EuphoriaState.ConfirmRecruitOption);
        	doDone(null,replay);
        	}
        	break;
        	
        case USE_FIRST_RECRUIT_OPTION:
        	{
        	Continuation cont = continuationStack.top();
        	cont.extraChip = bumpedWorker;
        	setState(EuphoriaState.ConfirmRecruitOption);
        	doDone(null,replay);
        	}
        	break;
     	
        case USE_RECRUIT_OPTION:
        	// special cases
        	if(m.chip==RecruitChip.RowenaTheMentor)
        	{	EPlayer p = players[whoseTurn];
        		p.payCostOrElse(Cost.Knowledge,replay);
        		p.setTFlag(TFlag.UsingRowenaTheMentor);
        		//p1("use rowena the mentor");
        		logGameEvent(UseRowena);
        		useRecruit(RecruitChip.RowenaTheMentor,"use");
        	}
        	else if(m.chip==RecruitChip.ChagaTheGamer)
        	{	EPlayer p = players[whoseTurn];
    			useRecruit(RecruitChip.ChagaTheGamer,"use");
        		setContinuation(new Continuation(Function.Return));
        		setState(EuphoriaState.ActivateOneRecruit);

        		if(p.payCost(Cost.Box,replay)!=null)
        		{
        			setContinuation(new Continuation(Cost.Box,Function.FinishChagaTheGamer,p));
        		}
        		finishChagaTheGamer(p);
        	}
        	else {
        	switch(board_state)
        	{
        	case RecruitOption:	
         			setState(EuphoriaState.ConfirmRecruitOption);
        			activeRecruit = m.chip;
           			if(revision>=123) { doDone(null,replay);  }
        			break;
        	case ConfirmRecruitOption: 
        			activeRecruit = null;
        			setState(EuphoriaState.RecruitOption); 
        			break;
        	default: Error("Not expecting "+board_state);
        		break;
         	}}
        	break;
        case FIGHT_THE_OPRESSOR:
        	{
        	EPlayer p = players[m.player];
        	DilemmaChip dilemma = (DilemmaChip)p.dilemma.chipAtIndex(0);
        	Cost cost = dilemma.cost;
        	Cost residual = p.payCost(cost,replay);
        	if(residual==null) 
        		{ 
        		  setState(EuphoriaState.ConfirmFightTheOpressor); 
        		}
        	else 
        		{
        		setContinuation(new Continuation(residual,Function.FightTheOpressor,p));
        		}
        	}
        	break;
        case JOIN_THE_ESTABLISHMENT:
       		{
        	EPlayer p = players[m.player];
        	DilemmaChip dilemma = (DilemmaChip)p.dilemma.chipAtIndex(0);
        	Cost cost = dilemma.cost;
        	Cost residual = p.payCost(cost,replay);
        	if(residual==null) 
        		{ setState(EuphoriaState.ConfirmJoinTheEstablishment); 
        		}
        	else 
        		{ 
        		setContinuation(new Continuation(residual,Function.JoinTheEstablishment,p));
          		}
        	}
       		break;
       		
       	// used by the robot
        case MOVE_ITEM_TO_PLAYER:
	    	{
	           	EuphoriaCell from = getCell(m.source,m.from_row);
	        	EuphoriaCell to = getCell(m.to_color,m.dest);
	        	pickObject(from,m.from_row);
	        	
	        	// handle out of sync replays
	        	// substitutePick(to,m);
	        	
	        	m.chip = pickedObject; 

				EPlayer dp = getPlayer(m.to_color);
	        	setNextStateAfterPick(dp);
	        	dropObject(to,replay);
	        	setNextStateAfterDrop(dp);      
	           	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(from);
        		animationStack.push(to);
        		}

	    	}
	    	break;
       case MOVE_RETRIEVE_WORKER:
        	{
               	EuphoriaCell from = getCell(m.source,m.from_row);
            	EuphoriaCell to = getCell(m.to_color,m.dest);
            	pickObject(from,m.from_row);
				m.chip = pickedObject;
				 
            	switch(from.rackLocation())
            	{
            	case EuphorianBuildMarketA:
            	case EuphorianBuildMarketB:
            	case SubterranBuildMarketA:
            	case SubterranBuildMarketB:
            	case WastelanderBuildMarketA:
            	case WastelanderBuildMarketB:
            		players[whoseTurn].penaltyMoves++;			// normally don't withdraw market chips.
            		break;
            	default: break;
            	}
            	setNextStateAfterPick(players[whoseTurn]);
            	dropObject(to,replay);
            	setNextStateAfterDrop(players[whoseTurn]); 
               	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(from);
        		animationStack.push(to);
        		}

        	}
        	break;
      case MOVE_ITEM:
      case MOVE_MOVE_WORKER:
      	{
    	  EuphoriaCell from = getCell(m.source,m.from_row);
    	  EuphoriaCell to = getCell(m.dest,m.to_row);
    	  pickObject(from,m.from_row);
    	  setNextStateAfterPick(players[whoseTurn]);
    	  dropObject(to,replay);
    	  setNextStateAfterDrop(players[whoseTurn]);
      	  if(replay!=replayMode.Replay)
      		{
      		animationStack.push(from);
      		animationStack.push(to);
      		}
      	}
      	break;
      case MOVE_ITEM_TO_BOARD:
      case MOVE_PLACE_WORKER:
        	{
        	EuphoriaCell from = getCell(m.from_color,m.source);
        	EuphoriaCell to = getCell(m.dest,m.to_row);
        	pickObject(from,m.from_row);
			m.chip = pickedObject; 
			
        	setNextStateAfterPick(players[whoseTurn]);
        	dropObject(to,replay);
        	setNextStateAfterDrop(players[whoseTurn]);
        	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(from);
        		animationStack.push(to);
        		}
        	}
        	break;
        case EPHEMERAL_CHOOSE_RECRUIT:
    		{
    			// only the robot does this, so no animation needed
    			EuphoriaCell from = getCell(m.from_color,m.source);
    			EuphoriaCell to = getCell(m.to_color,m.dest);
    			EPlayer p = getPlayer(m.from_color);
    			// don't use the regular pick/drop code here, because this is used
    			// by the asynchronous robot and we don't want to mess up the active player.
    			EuphoriaChip po = p.ephemeralPickedObject!=null ? p.ephemeralPickedObject : from.removeTop() ;
    			p.ephemeralPickedObject = null;
    			to.addChip(po);
    			m.chip = po; 
    			m.player = p.boardIndex;
    			setRecruitDialogState(p);
    		}
    		break;
        case MOVE_CHOOSE_RECRUIT:
         	{
       		REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
        	// only the robot does this, so no animation needed
         	EuphoriaCell from = getCell(m.from_color,m.source);
        	EuphoriaCell to = getCell(m.to_color,m.dest);
        	// don't use the regular pick/drop code here, because this is used
        	// by the asynchronous robot and we don't want to mess up the active player.
        	pickObject(from,m.from_row);
        	//substitutePick(to,m);
			m.chip = pickedObject; 
			EPlayer dp = getPlayer(m.from_color);
        	setNextStateAfterPick(dp);
        	dropObject(to,replay);
        	setNextStateAfterDrop(dp);
        	}
        	break;
        case MOVE_DROPB:
        	{
        	EuphoriaCell dest = getCell(m.dest,m.to_row);
        	EuphoriaCell src = getSource();
        	if(src==dest) 
        	{ unPickObject(); 
	       	  if((replay!=replayMode.Live) && (lastUndrop!=null))
	    		{
	        	animationStack.push(lastUndrop);
	        	animationStack.push(dest);
	    		}

        	}
        	else { 
        		m.chip = pickedObject;
        		dropObject(dest,replay);
        		setNextStateAfterDrop(players[whoseTurn]);
        		
                if(board_state==EuphoriaState.Puzzle)
                {
                	reverseStatusDisplays(dest);
                }
                if(replay==replayMode.Single)
                {
                	animationStack.push(src);
                	animationStack.push(dest);
                }
        		}
         	}
            break;

        case EPHEMERAL_PICK:
     		{
     		EPlayer pl = getPlayer(m.from_color);
	    	EuphoriaCell src = getCell(m.from_color,m.source);
	    	m.chip = pl.ephemeralPickedObject = src.removeTop();
	    	pl.ephemeralPickedSource = src;
	    	lastUndrop = null;
	    	}
     		break;

        case EPHEMERAL_DROP:
        	{
        	EuphoriaCell dest = getCell(m.to_color,m.dest);
         	EPlayer dp = getPlayer(m.to_color);
        	dp.ephemeralPickedSource = null;
        	dest.addChip(dp.ephemeralPickedObject);
        	dp.ephemeralPickedObject = null;
        	setRecruitDialogState(dp);
        	}
            break;
            
        case MOVE_SACRIFICE:
        	{
          	EuphoriaCell src = getCell(m.from_color,m.source);
          	pickObject(src,m.from_row);
          	dropObject(trash,replay);
          	setNextStateAfterDrop(players[whoseTurn]);
          	//p1("sacrificed a worker");
        	}
          	break;
          	
        case MOVE_PICK:
         	{
        	EuphoriaCell src = getCell(m.from_color,m.source);
            if(isDest(src)) { unDropObject(); lastUndrop = src; }
            else 
            	{
            	lastUndrop = null;
            	pickObject(src,m.from_row);
				m.chip = pickedObject;
	           	setNextStateAfterPick(getPlayer(m.from_color));
            	}
        	}
            break;
        case MOVE_PICKB:
        	{
       		EuphoriaCell src = getCell(m.source,m.from_row);
 			if(isDest(src)) 
 			    { unDropObject(); 
 			      lastUndrop = src;
 			      m.chip = pickedObject;
 			    }
   				else 
   				{	lastUndrop = null;
   					int hgt =  (board_state==EuphoriaState.Puzzle)
   								? src.height()-1
   								: revision>=104 
   							? (((src.row>=0) 
   									|| (revision>=111 && (m.source==EuphoriaId.MarketBasket))) 
   	   								? m.from_row
   	   								: src.height()-1)
   	   						    : ((src.row>=0)
   	   						    		?src.height()
   	   						    		:m.from_row);
   					 pickObject(src,hgt);
  					 m.chip = pickedObject;
   					 setNextStateAfterPick(players[whoseTurn]);
   				}
         	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(board_state==EuphoriaState.EphemeralChooseRecruits) 
        		{ setState(EuphoriaState.ChooseRecruits);
        		  REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
        		}
        	{
        	EuphoriaCell dest = getCell(m.to_color,m.dest);
        	EPlayer dp = getPlayer(m.to_color);
        	EuphoriaCell src = getSource();
            if(src==dest) 
            	{ unPickObject(); 
            		if((replay==replayMode.Single) && (lastUndrop!=null))
            		{
                	animationStack.push(lastUndrop);
                	animationStack.push(dest);
            		}
            	}
            else {
            	m.chip = pickedObject;
            	int h = dest.height();
            	dropObject(dest,replay);
            	int newh = dest.height();
            	setNextStateAfterDrop(dp);
                if(replay==replayMode.Single)
                {
                	animationStack.push(src);
                	animationStack.push(newh==h ? trash : dest );
                }

            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
	        currentPlayerInTurnOrder = whoseTurn;
            acceptPlacement();
            unPickObject();
            refillArtifactMarket(replayMode.Replay);
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            //reverseStatusDisplays(dest);
            openTunnelAtEnd(Allegiance.Euphorian);
            openTunnelAtEnd(Allegiance.Subterran);
            openTunnelAtEnd(Allegiance.Wastelander);
            
            setState(EuphoriaState.Puzzle);
            clearSteps();
            proceedGameStep = ProceedStep.Start;
            continuationStack.clear();
            if(!hasReducedRecruits)
            {
            	if(SIMULTANEOUS_PLAY)
                {
                	setRecruitDialogState(getPlayer(activePlayer));
                }
            	else { setRecruitDialogState(getPlayer(whoseTurn)); }
            }
            else {     proceedWithTheGame(replay); }
            setWhoseTurn(m.player); 
	        currentPlayerInTurnOrder = whoseTurn;
          break;

       case MOVE_RESIGN:
    	    EuphoriaState newstate = (unresign==null)?EuphoriaState.Resign:unresign;
    	    if(pickedObject!=null) { unPickObject(); }
    	   	setState(newstate);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
	        currentPlayerInTurnOrder = whoseTurn;
            setState(EuphoriaState.Puzzle);
            proceedGameStep = ProceedStep.Start;
            continuationStack.clear();

            break;
       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(EuphoriaState.Gameover);
    	   break;
       case MOVE_LOSEMORALE:
      	{  EPlayer p = players[whoseTurn];
	   	   p.setTFlag(TFlag.HasLostMorale);
	   	   doLoseMorale(p,1,replay,null,Function.ProceedWithTheGame);
	   	   break;
      	}
       default:
        	
        	cantExecute(m);
        }

        //if(replay==replayMode.Live)   	{ System.out.println("Ax "+m+" for "+whoseTurn+" "+board_state+" "+Digest());   	}
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        return (true);
    }
 
    private void finishChagaTheGamer(EPlayer p) {
    	// this happens at the beginning, we need to reset to the beginning.
		p.startNewWorker();
	}


	// return true if this is a cell where workers can be placed.
    private boolean canPlaceWorker(EPlayer p,EuphoriaChip ch,EuphoriaCell c)
    {	
    	int cellH = c.height();
    	EuphoriaId rack = c.rackLocation();
    	 if(rack.canBeBumped || (cellH==0))
    	{	// see if the cost can be met

    	if(p.canPay(c))
    		{ 	c.marketPenalty = null;
    		    if(rack.canBeBumped
    		    		&& (cellH>0))
    		    {	boolean workalone = p.penaltyAppliesToMe(MarketChip.IIB_TogetherWeWorkAloneCamp) && (p.knowledge>=6);
    		    	boolean fleet = p.penaltyAppliesToMe(MarketChip.SpaOfFleetingPleasure);
    		    	if(workalone || fleet)
    		    		{	
     		    		WorkerChip worker = (WorkerChip)c.topChip();
	    				if(worker.color==p.color) 
	    				{ c.marketPenalty = workalone ? MarketChip.IIB_TogetherWeWorkAloneCamp : MarketChip.SpaOfFleetingPleasure;
	    				  if(workalone) 
	    				  { //p1("apply workalone "+c.rackLocation()); 
	    				  }
	    				  else 
	    				  {//p1("apply SpaOfFleetingPleasure "+c.rackLocation()); 
	    				  }
	    				  return(false); 
	    				}
	    			}}
    			if((c.allegiance==Allegiance.Icarite) 
    				&& p.penaltyAppliesToMe(MarketChip.ApothecaryOfProductiveDreams))
    			{	c.marketPenalty = MarketChip.ApothecaryOfProductiveDreams;
    				return(false);
    			}
    			if(p.penaltyAppliesToMe(MarketChip.ArenaOfPeacefulConflict))
    			{	EuphoriaCell loc[] = null;
    				switch(rack)
    				{
    				case EuphorianGenerator: loc = euphorianGenerator; break;
    				case IcariteCloudMine: loc = icariteCloudMine; break;
    				case SubterranAquifer: loc = subterranAquifer; break;
    				case WastelanderFarm: loc = wastelanderFarm; break;
    				default: break;
    				}
    				if(loc!=null)
    				{
    					// can't place more than 1 worker
    					if(alreadyHasWorker(p,loc)) 
    						{ c.marketPenalty = MarketChip.ArenaOfPeacefulConflict;
    						  return(false); 
    						}   					
    				}
    			}
    			if(p.penaltyAppliesToMe(MarketChip.PlazaOfImmortalizedHumility))
    			{
    			EuphoriaCell loc = null;
    			switch(rack)
	    			{
	    			case EuphorianMarketA:	loc = EuphorianMarketChipA; break;
	    			case EuphorianMarketB:	loc = EuphorianMarketChipB; break;
	    			case SubterranMarketA:	loc = SubterranMarketChipA; break;
	    			case SubterranMarketB:	loc = SubterranMarketChipB; break;
	    			case WastelanderMarketA:loc = WastelanderMarketChipA; break;
	    			case WastelanderMarketB:loc = WastelanderMarketChipB; break;
	    			default: break;
	    			}
    				if(loc!=null)
    				{	if(!p.hasMyAuthorityToken(loc)) 
	    					{c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
    				}
    			}
    		
    			switch(rack)
    			{
    			case EuphorianBuildMarketA:
    			case EuphorianBuildMarketB:
    			case SubterranBuildMarketA:
    			case SubterranBuildMarketB:
    			case WastelanderBuildMarketA:
    			case WastelanderBuildMarketB:
	    			if(p.penaltyAppliesToMe(MarketChip.LoungeOfOppulentFrugility))
	    			{
	    				for(EuphoriaCell producer : getBuilderArray(rack))
	    				{
	    					if(producer.height()>0)
	    					{
	    						WorkerChip worker = (WorkerChip)producer.topChip();
	    						if(worker.color!=ch.color) 
	    							{ 
	    							c.marketPenalty = MarketChip.LoungeOfOppulentFrugility;
	    							return(false); }	// can't place when opposing workers are present
	    					}
	    				}
	    			}
	    			break;
	    		default: break;
    			}
   			return(true);
    		}
    	}
    	return(false);
    }
    private boolean alreadyHasWorker(EPlayer p,EuphoriaCell []places)
    {
    	for(EuphoriaCell c : places)
    	{
    		WorkerChip worker = (WorkerChip)c.topChip();
    		if((worker!=null) && (worker.color==p.color)) { return(true); }
    	}
    	return(false);
    }
    public boolean canPlace(EPlayer p, EuphoriaChip ch,EuphoriaCell c)
    {
    	if(ch.isWorker())  { return(c.rackLocation().isWorkerCell() && canPlaceWorker(p,ch,c)); }
    	return(true);
    }
    // legal to hit the chip storage area
    public boolean legalToHitPlayer(EuphoriaCell c,Hashtable<EuphoriaCell,EuphoriaMovespec>sources,Hashtable<EuphoriaCell,EuphoriaMovespec>dests)
    {
        switch (board_state)
        {
        default:
        	throw Error("Not expecting state %s", board_state);
        case CollectBenefit:
        case DiscardResources:
        case PayForLionel:
        case PayForBorna:
        case CollectOptionalBenefit:
        	 if(pickedObject!=null)
        	 {
        		 return((c==getSource()) || ((dests!=null) && (dests.get(c)!=null)));
        	 }
        	 else { return((c==getDest()) || ((sources!=null) && (sources.get(c)!=null))); }
        case PayForOptionalEffect:
        case FightTheOpressor:
        case JoinTheEstablishment:
        case ConfirmUseJackoOrContinue:
     	case ConfirmUseMwicheOrContinue:
     	case PayCost:
        	if(pickedObject!=null) { return(c==getSource()); }
        	else { return((sources!=null) && sources.get(c)!=null);}
        case ConfirmRetrieve:
        case ConfirmBenefit:
        case ConfirmPayCost:
        case ConfirmBump:
        	return(getDest()==c);
        case PlaceOrRetrieve:
        case Place:
        case BumpOpponent:
        case ReUseWorker:
        case RePlace:
        case PlaceNew:
        case PlaceAnother:
        	if((pickedObject==null) && (dests!=null) && dests.get(c)!=null) { return(true); }
        	return((c.rackLocation==EuphoriaId.PlayerWorker) && ((pickedObject==null)?(c.topChip()!=null):true));
        case Retrieve:
        case RetrieveCommodityWorkers:
        case Retrieve1OrConfirm:
        case RetrieveOrConfirm:
        	return(((dests!=null) && (dests.get(c)!=null)) 
        			|| (c.rackLocation==EuphoriaId.PlayerNewWorker) && ((pickedObject==null)?(c.topChip()!=null):true));
     	case EphemeralChooseRecruits:
     	case ChooseRecruits:
     	case DiscardFactionless:
     	case EphemeralDiscardFactionless:
		case EphemeralConfirmRecruits:
		case ConfirmDiscardFactionless:
		case EphemeralConfirmDiscardFactionless:
		case ConfirmRecruits:
        case ConfirmOneRecruit:
        case ConfirmActivateRecruit:
        case ChooseOneRecruit:
        case ConfirmFightTheOpressor:
        case ConfirmJoinTheEstablishment:
        case ConfirmPlace:
        case ExtendedBenefit:
        case RecruitOption:
        case DieSelectOption:
        case ConfirmRecruitOption:
        case NormalStart:
        case Resign:
        case ConfirmPayForOptionalEffect:
        case ConfirmUseJacko:
        case DumbKoff:
        case ActivateOneRecruit:
        case Gameover:
        	return(false);	// not used
        case Puzzle:
        	if(pickedObject!=null)
        	{	EuphoriaChip content = c.contentType();
        		boolean isok = ((content==null) || (pickedObject.subtype()==content));
        		return(isok);
        	}
        	else {
            return (c.topChip()!=null);
        	}
        }
    }
    
    //
    // can hit one of the recruit cells on the special per-player gui board
    public boolean canHitRecruit(EuphoriaCell c,EPlayer p,Hashtable<EuphoriaCell,EuphoriaMovespec>sources,Hashtable<EuphoriaCell,EuphoriaMovespec>dests)
    {	if(p.ephemeralPickedObject!=null)
    	{
    		return ((c==p.ephemeralPickedSource) || ((dests!=null) && (dests.get(c)!=null)));
    	}
    	if(pickedObject==null)
    		{
    		return( (getDest()==c) || ((sources!=null) && (sources.get(c)!=null)));
    		}
    		else {    		
    		return((getSource()==c) || ((dests!=null) && (dests.get(c)!=null)));
    		}
    }

    public boolean legalToDropOnBoard(EuphoriaCell c,Hashtable<EuphoriaCell,EuphoriaMovespec>dests)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        default:
        	//throw Error("Not expecting state %s", board_state);
        case ChooseOneRecruit:
        case Place:
        case PlaceAnother:
        case PlaceNew:
        case PlaceOrRetrieve:
        case CollectBenefit:
        case CollectOptionalBenefit:
        case PayForOptionalEffect:
        case JoinTheEstablishment:
        case PayCost:
        case FightTheOpressor:
        case ConfirmUseJackoOrContinue:
     	case ConfirmUseMwicheOrContinue:
     		return((c==getSource()) || (dests.get(c)!=null));
        case Retrieve:
        case RetrieveCommodityWorkers:
        case RetrieveOrConfirm:
        	return(getSource()==c);
		case EphemeralConfirmRecruits:
		case ConfirmRecruits:
        case ConfirmRetrieve:
        case ConfirmPayCost:
     	case EphemeralChooseRecruits:
        case ChooseRecruits: return(false); 
        case Puzzle:
        	if(pickedObject!=null)
        	{	if(c.rackLocation()==EuphoriaId.UnusedWorkers)
        		{
        		return(c.topChip()==pickedObject);
        		}
        		else
        		{
         		return(c.canAddChip(pickedObject,false));
        		}
        	}
            return (true);
        case ConfirmPayForOptionalEffect:
        case ConfirmUseJacko:
        case Resign:
        case ConfirmBenefit:
        case Gameover: return(false);
        }
    }
       
    public boolean legalToHitBoard(EuphoriaCell c,Hashtable<EuphoriaCell,EuphoriaMovespec>sources)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        default:
        	throw Error("Not expecting state %s", board_state);
        case ConfirmOneRecruit:
        case ConfirmActivateRecruit:
        case ChooseOneRecruit:
        case ConfirmPlace:
        case ConfirmRetrieve:
        case ConfirmRecruitOption:
        case ConfirmBenefit:
        case Resign:
        case ConfirmPayForOptionalEffect:
        case ConfirmJoinTheEstablishment:
        case ConfirmFightTheOpressor:
        case PayCost:
        case ConfirmUseJackoOrContinue:
     	case ConfirmUseMwicheOrContinue:
        case ConfirmUseJacko:
        case ConfirmBump:
        case ConfirmPayCost:
        case PayForLionel:
        case PayForBorna:
        case DiscardResources:
        	return(getDest()==c);
        case Place:
        case RePlace:
        case FightTheOpressor:
        case JoinTheEstablishment:
        case Gameover:
        case ExtendedBenefit:
        case PlaceNew:
        case NormalStart:
        case DumbKoff:
        case PlaceAnother:
        	return(false);
        case PlaceOrRetrieve:
        case Retrieve:
        case BumpOpponent:
        case ReUseWorker:
        case Retrieve1OrConfirm:
        case RetrieveOrConfirm:
        case RetrieveCommodityWorkers:
        case PayForOptionalEffect:
        case CollectBenefit:
        case CollectOptionalBenefit:
            	return(sources.get(c)!=null);
       	  
		case EphemeralConfirmRecruits:
        case ConfirmRecruits:
        case ChooseRecruits: 
     	case EphemeralChooseRecruits:
        case EphemeralDiscardFactionless:
        case EphemeralConfirmDiscardFactionless:
        case DiscardFactionless:
        case DieSelectOption:
        case ActivateOneRecruit:
        case RecruitOption:
        	return(false); 
        case Puzzle:
        	if(c.rackLocation()==EuphoriaId.UnusedWorkers)
        	{
        		return((c.height()>0) && (getPlayer(c.row).totalWorkers<MAX_WORKERS));
        	}
        	else
        	{
            return (c.height()>0);
        	}
        }
    }
    double scoreRecruit(EuphoriaCell c)
    {	double val = 0.0;
       	for(int idx = c.height()-1; idx>=0; idx--)
    	{	RecruitChip recruit = (RecruitChip)c.chipAtIndex(idx);
    		Allegiance al = recruit.allegiance;
    		val += 0.001*getAllegianceValue(al);
    		val += 0.001*getTunnelPosition(al);
    	}
       	return(val);
    }
    
    // baseline, just count the tokens left.
    public double scoreEstimate_00(int pl,boolean pr)
    {	EPlayer p = players[pl];
    	return(1.0-p.authority.height()*0.1);
    }
    // add light penalties for wasted moves, wasted cards, and leftover resoruces
    public double scoreEstimate_02(int pl,boolean pr)
    {	EPlayer p = players[pl];
    	int tokens = p.authority.height();
    	double val = 1.0-tokens*0.1;
    	{
    	double ideal_resources = (6-nOpenMarkets)/2.0;
    	double penalty = p.penaltyMoves*0.001;
        double cards = p.cardsLost*0.002;
        int commod = p.totalCommodities();
        int res = p.totalResources();
        double commodities = Math.max(0,commod-3)*0.001;
        double resources = Math.max(0,(res-ideal_resources))*0.002;
        val -= (penalty+cards+commodities+resources);
    	}
    	return(val);
    }
    // score estimate for a player, in range 0-1.0.  Note that this is applied
    // at the bottom of the monte carlo descent, so it scores the overall progress
    // toward victory.  Except for the number of stars placed, the factors are 
    // influences toward what I perceive as generic good play. Ie; minimizing
    // loss of workers, minimizing retrievals, not stockpiling excessive quantities
    // of goods and so on.
    public double scoreEstimate_01(int pl,boolean pr)
    {	EPlayer p = players[pl];
    	double stars = p.authority.height();
    	if(stars==0) { return(1.0); }
    	
    	double val = 0.85-stars*0.1;
    	double market = 0.02*p.marketStars;
    	double retrieve = (((p.placements+1.0)/(p.retrievals+1))-1)*0.015;
    	double lost = (p.workersLost*0.1)/(p.retrievals+1);
    	double penalty = p.penaltyMoves*0.1;
    	double cards = p.artifacts.height()*0.01;
    	double resources = p.totalResources()*0.01;
    	if(pr) { G.print(""+p+" r="+retrieve+" l="+lost+" p="+(-penalty)); }
    	val -= penalty;
    	val += cards;
    	val += market;
    	val += resources;
    	val -= p.cardsLost*0.01;
    	val -= lost;	// penalty for losing workers, scaled by number of retrievals
    	val -= (p.totalCommodities()-10)*0.001;		// discourage collecting a lot of commodities
    	val -= p.knowledge*0.001;
    	val += p.morale*0.001;
    	val += retrieve;	// favor high ratio of placements over retrievals
    	val += scoreRecruit(p.activeRecruits);
    	val += (scoreRecruit(p.hiddenRecruits)*0.5);

    	return(Math.max(0,Math.min(1.0,val)));
    }  
    // score estimate for a player, in range 0-1.0.  Note that this is applied
    // at the bottom of the monte carlo descent, so it scores the overall progress
    // toward victory.  Except for the number of stars placed, the factors are 
    // influences toward what I perceive as generic good play. Ie; minimizing
    // loss of workers, minimizing retrievals, not stockpiling excessive quantities
    // of goods and so on.
    public double scoreEstimate_03(int pl,boolean pr)
    {	EPlayer p = players[pl];
    	double stars = p.authority.height();
    	if(stars==0) { return(1.0); }
    	
    	double val = 0.85-stars*0.1;
    	double market = 0.02*p.marketStars;
    	double idealResources = (6-nOpenMarkets)/2;
    	double resourceWeight = 0.01;
    	double cardWeight = 0.01 + 0.001*nOpenMarkets;		// cards become more valuable
    	
    	double retrieve = (((p.placements+1.0)/(p.retrievals+1))-1)*0.015;
    	double lost = (p.workersLost*0.1)/(p.retrievals+1);
    	double penalty = p.penaltyMoves*0.1;
    	double cards = p.artifacts.height()*cardWeight;
    	double resources = -Math.abs(p.totalResources()-idealResources)*resourceWeight;
    	double commodities = -Math.abs(p.totalCommodities()-2)*0.01;
    	if(pr) { G.print(""+p+" r="+retrieve+" l="+lost+" p="+(-penalty)); }
    	val -= penalty;
    	val += cards;
    	val += market;
    	val += resources;
    	val += commodities;
    	val -= p.cardsLost*0.01;
    	val -= lost;	// penalty for losing workers, scaled by number of retrievals
    	val -= (p.totalCommodities()-10)*0.001;		// discourage collecting a lot of commodities
    	val -= p.knowledge*0.001;
    	val += p.morale*0.001;
    	val += retrieve;	// favor high ratio of placements over retrievals
    	val += scoreRecruit(p.activeRecruits);
    	val += (scoreRecruit(p.hiddenRecruits)*0.5);

    	return(Math.max(0,Math.min(1.0,val)));
    }
    void removeRecruits(EuphoriaCell from,EuphoriaCell s)
    {
    	for(int lim=s.height()-1; lim>=0; lim--)
    	{
    		from.removeChip(s.chipAtIndex(lim));
    	}
    }
    void removeRecruits(EuphoriaCell from,EuphoriaCell s[])
    {
    	for(EuphoriaCell a : s) { removeRecruits(from,a); }
    }
    
    void replaceRecruits(EuphoriaCell from,EuphoriaCell s)
    {
    	if(s.rackLocation()==EuphoriaId.PlayerHiddenRecruits)
    	{	for(int lim=s.height()-1; lim>=0; lim--) 
    		{
    		RecruitChip replacing = (RecruitChip)s.chipAtIndex(lim);
    		if(recruitShouldBeActive(replacing)) 
    			{
    			// unusual, but it can happen.  Leave the original chip
    			// and remove it from the replacement stack
    			from.removeChip(replacing);
    			}
    		else {
    			RecruitChip top= (RecruitChip)from.removeTop();
    			if(	recruitShouldBeActive(top))
    	    			{
    	    			// don't replace a hidden recruit with one that would be activated.
    	    			// we know there's one in the deck, because the actual recruit is there.
    	    			from.insertChipAtIndex(0,top);
    	    			lim++;		// do over
    	    			}
    	    			else
    	    			{
    	    				s.setChipAtIndex(lim,top);
    	    			}
    			}
    		}
    	}
    	else
    	{	// simple replacement except for hidden recruits
    		for(int lim=s.height()-1; lim>=0; lim--) 
    		{
    			s.setChipAtIndex(lim,from.removeTop());
    		}
    	}
    }
    void replaceRecruits(EuphoriaCell from,EuphoriaCell s[])
    {
    	for(EuphoriaCell a : s) { replaceRecruits(from,a); }
    }

    //
    // called for the robot player when starting a new descent.  All the currently hidden state
    // for the non-robot player should be randomized, so the bot doesn't get an unfair peek at 
    // what cards and goals are going to come up in the real game.
    //
    void randomizeHiddenState(Random r,int robotplayer)
    {	
    	EPlayer robo = players[robotplayer];
     	unusedRecruits.reInit();
     	getAllRecruits(unusedRecruits);
     	unusedRecruits.shuffle(r);
     	//
     	// remove the recruits we're using and the one we're not supposed to know about
     	// from the pool represented by unusedRecruits
     	//
     	for(EPlayer p : players)
     	{	
         	p.cardsLost = 0;
         	p.penaltyMoves = 0;
    		if(p==robo)
     		{
     		// remove the robot's recruits from the pool.
     		removeRecruits(unusedRecruits,robo.newRecruits);
     		removeRecruits(unusedRecruits,robo.hiddenRecruits);
     		removeRecruits(unusedRecruits,robo.activeRecruits); 
     		}
     		else // remove other player's recruits unless they're public
     		{	
       			if (hasReducedRecruits) 
       				{ removeRecruits(unusedRecruits,p.activeRecruits); }
     		}
       	}
     	// we're left with a pool of available recruits, shuffled. Replace all the
     	// recruits we're not supposed to know about with a random one from the top 
     	// of the unused deck.
     	for(EPlayer p : players)
     	{	if(p!=robo)
     		{
     		replaceRecruits(unusedRecruits,p.hiddenRecruits);
     		replaceRecruits(unusedRecruits,p.newRecruits);
     		if(!hasReducedRecruits) 
     			{ replaceRecruits(unusedRecruits,p.activeRecruits);
     			// check for state change based on factionless recruits
    			if(p.boardIndex==whoseTurn) { setRecruitDialogState(p); }
      			}
    		}
     	}
     	if(!isIIB())	// in IIB we know what artifacts they have
     	{
     	for(EPlayer p : players)
    	{	p.clearUCTStats();
    		if((p!=robo) && (p!=players[whoseTurn]))		// don't randomize the current player in mid turn
    		{	
    			EuphoriaCell cards = p.artifacts;
    			for(int lim = cards.height()-1; lim>=0; lim--)
    			{	// push the cards we want to randomize back onto the unused pile, but 
    				// preserve the actual stack height.
    				unusedArtifacts.addChip(cards.chipAtIndex(lim));
    			}
    		}
    		// swap out the hidden recruit for one of the unknowns
    		// if we're in the initial stages, swap out the active recruits too
    	}}
       	unusedArtifacts.shuffle(r);
       	if(!isIIB())	// in IIB we know what artifacts they have
       	{for(EPlayer p : players)
    	{	p.clearUCTStats();
    		if((p!=robo) && (p!=players[whoseTurn]))		// don't randomize the current player in mid turn
    		{	
    			EuphoriaCell cards = p.artifacts;
    			for(int lim = cards.height()-1; lim>=0; lim--)
    			{	// change the card to a random one
    				cards.setChipAtIndex(lim,unusedArtifacts.removeTop());
    			}
    		}
    	}}
		{	int nMarkets = unusedMarkets.height();
			for(EuphoriaCell market : markets)
			{ if(!marketIsOpen(market))
				{	// swap in a different market
					int mi = Random.nextInt(r,nMarkets+1);
					if(mi<nMarkets)
					{
					EuphoriaChip ch = market.chipAtIndex(0);
					EuphoriaChip replacement = unusedMarkets.chipAtIndex(mi);
					market.setChipAtIndex(0,replacement);
					unusedMarkets.setChipAtIndex(mi,ch);
					}
				}
			}
     	}
    }
    
   // this is called when the robot detects that it tried to make an illegal move
   // in a replay situation.  This means that the randomization of the game had made
   // a move stored in the UCT move tree illegal.
   void terminateWithExtremePrejudice()
   {
	   setState(EuphoriaState.Gameover);
   }
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(EuphoriaMovespec m)
    {	
    	//G.print("R "+moveNumber+" "+m+" "+board_state);
    	Execute(m,replayMode.Replay);

        switch(board_state)
        {
        case ConfirmPayForOptionalEffect:
        case ConfirmPlace:
        case ConfirmPayCost:
           	m.chip = openedMarket;
			//$FALL-THROUGH$
        case ConfirmBenefit:
        case ConfirmRetrieve:
        case ConfirmRecruitOption:
        case ConfirmRecruits:
		case EphemeralConfirmRecruits:
        case ConfirmActivateRecruit:
        case ConfirmOneRecruit:
        case ConfirmFightTheOpressor:
        case ConfirmJoinTheEstablishment:
         	m.followedByDone = true;
         	stepNumber++; 
         	doDone(m.chipIn,replayMode.Replay);
           	break;
      default:       	
        	break;
        }
    }
    int step=0;

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(EuphoriaMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	throw Error("Not expected");
    }
 
 public boolean ephemeralRecruitMode() { return (board_state.hasRecruitGui()&&board_state.simultaneousTurnsAllowed()); }
 
 /** get all the destination moves for the current picked worker */
 public Hashtable<EuphoriaCell,EuphoriaMovespec> getDests(int who)
 {	Hashtable<EuphoriaCell,EuphoriaMovespec> val = new Hashtable<EuphoriaCell,EuphoriaMovespec>();
 	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==EuphoriaState.Puzzle) { return(val); }
 	EPlayer whoP = getPlayer(who);
 	boolean ephemeralRecruits = ephemeralRecruitMode();
 	EuphoriaChip w = ephemeralRecruits 
 							? whoP.ephemeralPickedObject 
 							: pickedObject;
 	if(w!=null)
 	{ 	
 		EuphoriaCell src = ephemeralRecruits ? whoP.ephemeralPickedSource : getSource();
 		boolean isWorker = w.isWorker() ;
 		if(src.rackLocation().perPlayer || reUsingWorker)
 			{
 			int height = ephemeralRecruits ? 0 : pickedHeightStack.top();
 			EPlayer p = getPlayer((isWorker && reUsingWorker)
 									? ((WorkerChip)pickedObject).color 
 									: src.color); 
 			
			
 		 	if(isWorker && (board_state==EuphoriaState.PayCost)) 
 		 	{	// special case for sacrificing a worker
 		 		val.put(trash,new EuphoriaMovespec(MOVE_SACRIFICE,p,src,pickedHeightStack.top()));
 		 	}
 		 	else
 		 	{
  			addPlacementMoves(all,p,src,w,height);
 			while(all.size()>0)
 				{
 				EuphoriaMovespec m = (EuphoriaMovespec)all.pop();
 				switch(m.op)
 				{
 				case MOVE_CHOOSE_RECRUIT:
 					{
 						EuphoriaCell c = getCell(m.to_color,m.dest);	
 						val.put(c,m);
 					}
 					break;
 				case MOVE_ITEM_TO_BOARD:
 				case MOVE_MOVE_WORKER:
 				case MOVE_ITEM:
 				case MOVE_PLACE_WORKER:
 					{
 						EuphoriaCell c = getCell(m.dest,m.to_row);
 						val.put(c,m);
 					}
 					break;
 				default:
 					throw Error("Not expecting %s",m);
 				}
 			}}}
 		else if(w.isWorker()){
 			// replacement moves
 			EPlayer p = getPlayer(w.color); 
 			val.put(p.newWorkers,new EuphoriaMovespec(src,p.newWorkers,(WorkerChip)w,p.boardIndex));
 		}
 		else {
 			// we were allowed to pick up a non-worker
 			// figure out where it goes
 			addPlacementMoves(all,src,w,players[who]);
 			while(all.size()>0)
				{
				EuphoriaMovespec m = (EuphoriaMovespec)all.pop();
				switch(m.op)
				{
				case MOVE_ITEM:
					val.put(getCell(m.dest,0),m);
					break;
				case MOVE_ITEM_TO_PLAYER:
					{	EuphoriaCell c = getCell(m.to_color,m.dest);
						val.put(c,m);
					}
					break;
				default: throw Error("Not expecting %s",m);
				}
				}
 		}
	}
 	return(val);
 }
 
 private boolean shouldSkipStar(EPlayer p,EuphoriaCell c,WorkerChip worker)
 {	
	 Allegiance allegiance = c.allegiance;
	 EuphoriaCell aa = getAvailableAuthorityCell(p,allegiance);
	 if(aa==null && !canIncrementAllegiance(allegiance)) { return true; }
		else { return !canPlaceWorker(p,worker,c); }
 }
 
 public void addWorkerPlacementMoves(CommonMoveStack all,EPlayer p,EuphoriaCell src,WorkerChip worker,int idx)
 {	boolean placedAquifer = false;
 	boolean placedGenerator = false;
 	boolean placedFarm = false;
 	boolean placedCloud = false;
 	for(int lim=workerCells.length-1; lim>=0; lim--)
	 {	EuphoriaCell c = workerCells[lim];
	 	boolean skip = false;
		if(robotBoard)
		{
		// avoid excessive placements on the commodity sources.
			switch(c.rackLocation())
			{
			case EuphorianUseMarket:
			case WastelanderUseMarket:
			case SubterranUseMarket:
			case EuphorianMarketA:
			case EuphorianMarketB:
			case WastelanderMarketA:
			case WastelanderMarketB:
			case SubterranMarketA:
			case SubterranMarketB:
			case IcariteNimbusLoft:
			case IcariteWindSalon:
				skip = shouldSkipStar(p,c,worker);  // skip if there are no stars to be placed
				break;
			case SubterranAquifer:
				if(placedAquifer || (p.water.height()>=6)) {skip = true; } 
					else { placedAquifer=canPlaceWorker(p,worker,c); skip=!placedAquifer;}
				break;
			case WastelanderFarm:	
				if(placedFarm || (p.food.height()>=6)) { skip=true; } 
					else { placedFarm=canPlaceWorker(p,worker,c); skip=!placedFarm;}
				break;
			case IcariteCloudMine:	
				if(placedCloud || (p.bliss.height()>=6)) { skip= true; } 
					else { placedCloud=canPlaceWorker(p,worker,c); skip=!placedCloud;}
				break;
			case EuphorianGenerator: 
				if(placedGenerator || (p.energy.height()>=6)) { skip=true;  } 
					else { placedGenerator=canPlaceWorker(p,worker,c); skip=!placedGenerator; }
				break;
			default: skip = !canPlaceWorker(p,worker,c);
			break;
				
			}
			}
		else { skip = !canPlaceWorker(p,worker,c); }
		 if(!skip )
		 {	
			 if(src.onBoard)
				 {
				 // this is used by lars the ballooneer, can't stay on the same place
				 if(src!=c) { all.addElement(new EuphoriaMovespec(MOVE_MOVE_WORKER,p,src,c)); }
				 }
			 	else 
			 	{ 
				 all.addElement(new EuphoriaMovespec(p,src,idx,c));
			 	}
 		 }
	 }
 }
 public boolean canPlaceAuthorityToken(EPlayer p,EuphoriaCell dest,replayMode replay)
 {	 switch(dest.placementBenefit)
	 {
	 	case EuphorianAuthority2:
		case WastelanderAuthority2:
		case SubterranAuthority2:	
			return((p.authority.height()>0)
					&& addAuthorityPlacementMoves(null,p,dest.allegiance,null,
							getMarketA(dest.allegiance),getMarketB(dest.allegiance)));
		case EuphorianAuthorityAndInfluenceA:
		case WastelanderAuthorityAndInfluenceA:
		case SubterranAuthorityAndInfluenceA:
		case EuphorianAuthorityAndInfluenceB:
		case WastelanderAuthorityAndInfluenceB:
		case SubterranAuthorityAndInfluenceB:
			if((isIIB()) || (revision>=123))
			{
				EuphoriaChip chip = p.getAuthorityToken(replay);
				if(!dest.containsChip(chip)) { return(true); }
			}
			return(getAvailableAuthorityCell(p,dest.allegiance)!=null);
		default: throw Error("not expecting %s",dest);
	 }
 }
 public boolean addAuthorityPlacementMoves(CommonMoveStack all,EPlayer p,Allegiance a,
		 	EuphoriaCell src,EuphoriaCell marketA,EuphoriaCell marketB)
 {		
	 	EuphoriaCell zone = getAvailableAuthorityCell(p,a);
	 	boolean some = false;
	 	if((marketA!=null) && marketIsOpen(marketA) && !p.hasMyAuthorityToken(marketA))
	 		{	some=true;
	 			if(all!=null) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,marketA)); }
	 			else { return(true); }
	 		}
	 	if((marketB!=null)&& marketIsOpen(marketB) && !p.hasMyAuthorityToken(marketB))
	 		{	some = true;
	 			if(all!=null)
	 			{
	 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,marketB));
	 			}
	 			else { return(true); }
	 		}
	 	if(zone!=null)
	 		{	if(robotBoard)
	 			{// we only need one
	 			some = true;
	 			if(all!=null)
	 				{
	 				all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,zone));
	 				}
	 				else { return(true); }
	 			}
	 			else
	 			{ for(EuphoriaCell c : authorityCells[a.ordinal()])
	 				{
	 				if(c.height()==0) 
	 					{ some = true;
	 					if(all!=null) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,c));}
	 					else { return(true); 
	 					}
	 				}
	 				}
	 			}
	 		}
	 	return(some);
 }
 public void addNormalPlacement(CommonMoveStack all,EPlayer p,EuphoriaCell  src)
 {
	 switch(src.rackLocation())
	 {
	 default: throw Error("Not expecting %s",src);
	 
	 case PlayerGold:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,goldMine));
	 	break;
	 case PlayerStone:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,quarry));
	 	break;
	 case PlayerClay:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,clayPit));
	 	break;
	 case PlayerFood:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,farm));
	 	break;
	 case PlayerWater:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,aquifer));
	 	break;
	 case PlayerEnergy:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,generator));
	 	break;
	 case PlayerBliss:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,bliss));
	 	break;
	 case PlayerArtifacts:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,usedArtifacts));
	 	break;

	 }
 }
 // return true if interaction is needed
 public boolean checkKofiTheHermit(replayMode replay,Function continuation)
 {
	 for(EPlayer p : players)
	 {
		if(p.recruitAppliesToMe(RecruitChip.KofiTheHermit))
		{	boolean trigger = false;
			for(Allegiance a : Allegiance.values())
			{
				trigger |= allegianceIsActive(a);
			}
			if(trigger)
			{	// trash kofi, activate another recruit
				trash.addChip(p.activeRecruits.removeChip(RecruitChip.KofiTheHermit));
				useRecruit(RecruitChip.KofiTheHermit,"discard");
				logGameEvent(DiscardKofiTheHermit);
				if(replay!=replayMode.Replay)
				{
					animationStack.push(p.activeRecruits);
					animationStack.push(trash);
				}
				switch(p.hiddenRecruits.height())
				{
				case 0:	break;
				case 1:
					{
					//p1("Kofi activates recruit");
					RecruitChip newRecruit = (RecruitChip)p.hiddenRecruits.removeTop();
					p.addActiveRecruit(newRecruit,replay);
					logGameEvent(ActivateRecruitMessage,p.color.name(),newRecruit.name);
					}
					break;
				default:	// the hard case, we need to reveal interact to select a recruit
					useRecruit(RecruitChip.KofiTheHermit,"choose");
					setContinuation(new Continuation(continuation));
					//p1("Kofi activates one choose");
					setState(EuphoriaState.ActivateOneRecruit);
					setWhoseTurnTo(p.boardIndex,"kofi's choice");
					return(true);
				}
			}
		}
	 }
	 return false;
 }
 public boolean allegianceIsActive(Allegiance a)
 {	if(getAllegianceValue(a)>=ALLEGIANCE_TIER_3) { return(true); }
 	if(getTunnelPosition(a)>=TUNNEL_REVEAL) { return(true); }
 	return(false); 
 }
 public boolean recruitShouldBeActive(RecruitChip recruit)
 {	Allegiance al = recruit.allegiance;
 	if(allegianceIsActive(al)) { return(true); }
 	return(false);
 }
 public void addPlacementMoves(CommonMoveStack all,EPlayer p,EuphoriaCell  src, EuphoriaChip moving,int idx)
 {		if(moving.isWorker())
 			{
	 		if(board_state==EuphoriaState.RePlace)
	 		{
	 		addWorkerPlacementMovesAt(all,p,lastDroppedWorker,(WorkerChip)moving,pickedHeightStack.top());
	 		}
	 		else
	 		{
	 		addWorkerPlacementMoves(all,p,src,(WorkerChip)moving,idx); 
 			}}
 		else
 		{	boolean forcedAltruism = p.penaltyAppliesToMe(MarketChip.IIB_PalaceOfForcedAltruism);
 			
 			switch(board_state)
 			{
 			case ActivateOneRecruit:
 				addActivateRecruitMoves(all,p);
 				break;
 			case CollectOptionalBenefit:
 			case CollectBenefit:
 			{	Benefit bene = pendingBenefit();
 				if(forcedAltruism)
 				{
 				  addDiscardResourceMoves(all,p,null); 
 				}
 				switch(bene)
 				{
 				case EuphorianAuthority2:
 				case WastelanderAuthority2:
 				case SubterranAuthority2:
 					{
 					Allegiance allegiance = pendingBenefit().placementZone();
 					addAuthorityPlacementMoves(all,p,allegiance,src,getMarketA(allegiance),getMarketB(allegiance));
 					}
 					break;
 				case EuphorianAuthorityAndInfluenceA:
 				case WastelanderAuthorityAndInfluenceA:
 				case SubterranAuthorityAndInfluenceA:
 					{
 					Allegiance allegiance = pendingBenefit().placementZone();
 					addAuthorityPlacementMoves(all,p,allegiance,src,getMarketA(allegiance),null);
 					}
 					break;
 				case EuphorianAuthorityAndInfluenceB:
 				case WastelanderAuthorityAndInfluenceB:
 				case SubterranAuthorityAndInfluenceB:
 					{
 					Allegiance allegiance = pendingBenefit().placementZone();
 					addAuthorityPlacementMoves(all,p,allegiance,src,null,getMarketB(allegiance));
 					}
 					break;

 				default: throw Error("Not expecting benefit %s",bene);
 				}
 			}
 				break;
 			case ChooseOneRecruit:
 				{
 				if(recruitShouldBeActive((RecruitChip)moving)) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.activeRecruits)); }
 				else { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.hiddenRecruits)); }
 				}
 				break;
 			case ConfirmRecruits:
 			case EphemeralConfirmRecruits:
 			case EphemeralChooseRecruits:
 			case ChooseRecruits:
 				if(SIMULTANEOUS_PLAY)
 				{
 					for(EuphoriaCell c : p.newRecruits)
 						{ if(c.topChip()==null) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,c)); }}
 				}
 				RecruitChip recruit = (RecruitChip)moving;
 				boolean factionless = (recruit!=null) && (recruit.allegiance == Allegiance.Factionless);
 				if(!factionless && p.hiddenRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.hiddenRecruits)); }
 				if(p.activeRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.activeRecruits)); }
 				break;
 				
 			case ConfirmDiscardFactionless:
 			case EphemeralConfirmDiscardFactionless:
 				break;
 			case DiscardFactionless:
 			case EphemeralDiscardFactionless:
 				all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.discardedRecruits)); 
 				break;
 			case PayForOptionalEffect:
 			case RecruitOption:
 			case PayCost:
 			case Puzzle:
 			case FightTheOpressor:
 			case ConfirmUseJackoOrContinue:
 			case ConfirmUseMwicheOrContinue:
 			case JoinTheEstablishment:
 				addNormalPlacement(all,p,src);
 			//$FALL-THROUGH$
 			case ConfirmPayCost:
 			case ConfirmUseJacko:
 				break;
 			case PayForLionel:
 				addLionelMoves(all,p);
 				break;
 			case PayForBorna:
 				addBornaMoves(all,p);
 				break;
 			case DiscardResources:
 				addDiscardResourceMoves(all,p,moving);
 				break;
 			default: throw Error("Not expecting state %s",board_state);
 			}
 		}
 }
 // for lionel the cook, moves to ignore a market penalty
 private void addLionelMoves(CommonMoveStack all,EPlayer p)
 {
	 for(EuphoriaCell market : markets)
	 {
		 if(marketIsOpen(market))
		 {
			 if(!p.hasMyAuthorityToken(market))
			 {	//p1("lionel the cook available for "+market.getName());
				 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,market));
			 }
		 }
	 }
 }
 // for rowena the mentor, moves to ignore all market penalties
 private void addRowenaMoves(CommonMoveStack all,EPlayer p)
 {
	 if((all!=null)
			&& p.canUseRowenaTheMentor())
	 {
		all.push(new EuphoriaMovespec(USE_RECRUIT_OPTION,RecruitChip.RowenaTheMentor,p));
	 }
 }
 // for rowena the mentor, moves to ignore all market penalties
 private void addChagaMoves(CommonMoveStack all,EPlayer p)
 {
	 if((all!=null)
			 && p.canUseChagaTheGamer())
	 {
		all.push(new EuphoriaMovespec(USE_RECRUIT_OPTION,RecruitChip.ChagaTheGamer,p));
	 }
 }
 boolean anyMarketPenaltiesApply(EPlayer p)
 {
	 for(EuphoriaCell market : markets)
	 {
		 if(marketIsOpen(market))
		 {
			 if(!p.hasMyAuthorityToken(market))
			 {	return(true);
			 }
		 }
	 }
	 return false;
 
 }
 private void checkUnusualTrades(CommonMoveStack all,EPlayer p)
 {
	 if(p.recruitAppliesToMe(RecruitChip.LionelTheCook)
			 && p.canPayX(Cost.Food)
			 && !p.testTFlag(TFlag.UsedLionelTheCook))
	 {	
		addLionelMoves(all,p); 
	 }
	 if(p.recruitAppliesToMe(RecruitChip.BornaTheStoryteller)
		 && !p.testTFlag(TFlag.UsedBornaTheStoryteller)
		 && (p.canPayX(Cost.Book) || p.canPayX(Cost.Bifocals)))
	 {	//	p1("can use borna");
		 addBornaMoves(all,p);
	 }
 }
 private void addBornaMoves(CommonMoveStack all,EPlayer p)
 {
	 addPayArtifactMoves(all,p,ArtifactChip.Book,ArtifactChip.Bifocals);
 }
 
 private void addWorkerPlacementMoves( CommonMoveStack all, EPlayer p,EuphoriaChip value)
 {
	 EuphoriaCell workers = p.workers;
	 int placedValues = 0;
	 boolean forcedAltruism = p.penaltyAppliesToMe(MarketChip.IIB_PalaceOfForcedAltruism);
	
	 addRowenaMoves(all,p);
	 addChagaMoves(all,p);
	 if(forcedAltruism) 
	 	{//p1("forced altruism worker");
		 addDiscardResourceMoves(all,p,null); 
	 	}
	 for(int lim=workers.height()-1; lim>=0; lim--)
	 {
		 WorkerChip w = (WorkerChip)workers.chipAtIndex(lim);
		 if((value==null)|| (w==value))
		 {
		 int knowledge = w.knowledge();
		 int mask = 1<<knowledge;
		 if((mask&placedValues)==0)
		 {
		 placedValues |= mask;
		 addWorkerPlacementMoves(all,p,workers,w,lim);
		 if(value!=null) 
		 	{ break; }	// only the top move
		 }
		 }
	 }
 }
 public void addLarsTheBallooneerMoves(CommonMoveStack all,EPlayer p,EuphoriaCell c)
 {	 Assert(c.height()==1,"y should be a single worker");
	 addWorkerPlacementMoves(all,p,c,(WorkerChip)c.topChip(),0);
 }
 public void addBumpOpponentMoves(CommonMoveStack all,EPlayer p,EuphoriaCell c)
 {
 	EuphoriaCell ar[] = getProducerArray(c.allegiance);
 	Colors targetColor = p.color;
 	for (EuphoriaCell src : ar)
 	{
 		WorkerChip ch = (WorkerChip)src.topChip();
 		if((ch!=null) && (ch.color!=targetColor))
 		{	EPlayer victim = getPlayer(ch.color);
 			all.addElement(new EuphoriaMovespec(src,victim.newWorkers,ch,p.boardIndex));
 		}
 	}

 }
 public boolean addWorkerPlacementMovesAt( CommonMoveStack all, EPlayer p, EuphoriaCell c)
 {	boolean some = false;
	 EuphoriaCell workers = p.workers;
	 int placedValues = 0;
	 for(int lim=workers.height()-1; lim>=0; lim--)
	 {
		 WorkerChip w = (WorkerChip)workers.chipAtIndex(lim);
		 {
		 int knowledge = w.knowledge();
		 int mask = 1<<knowledge;
		 if((mask&placedValues)==0)
		 {
		 placedValues |= mask;
		 some |= addWorkerPlacementMovesAt(all,p,c,w,lim);
		 }
		 }
	 }
	 return(some);
 }
 
 private boolean addWorkerPlacementMovesAt(CommonMoveStack all, EPlayer p, EuphoriaCell c,WorkerChip w,int index)
 {
	 if(canPlaceWorker(p,w,c)) 
	 {	if(all==null) { return(true); }
		all.addElement(new EuphoriaMovespec(p,p.workers,index,c));
		return(true);
	}
	 return false;
 }
	 
 private void addRecruitShuffleMoves(EPlayer p,Hashtable<EuphoriaCell,EuphoriaMovespec>val)
 {	
 	EuphoriaCell spots[] = {p.activeRecruits,p.hiddenRecruits,
 			p.newRecruits[0],p.newRecruits[1],
 			p.newRecruits[2],p.newRecruits[3],
 				};
 	for(EuphoriaCell src : spots)
 	{	
 		if(src.topChip()!=null)
 		{	for(EuphoriaCell dest : spots)
 			{	if(dest.topChip()==null)
 				{
 				EuphoriaMovespec m = new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,dest);
 				val.put(src,m);
 				}
 			}
 		} 
 	}
	 
 }
 int gui_errors = 0;
 public Hashtable<EuphoriaCell,EuphoriaMovespec>getSources(int who)
 {	Hashtable<EuphoriaCell,EuphoriaMovespec>val = new Hashtable<EuphoriaCell,EuphoriaMovespec>();
 	EPlayer p = players[who];
 	switch(board_state)
 	{
 	default: throw Error("not expecting %s",board_state);
 	case ConfirmPlace:
 	case ConfirmPayCost:
 	case ConfirmPayForOptionalEffect:
 	case ConfirmJoinTheEstablishment:
 	case ConfirmFightTheOpressor:
 	case ConfirmOneRecruit:
 	case ConfirmActivateRecruit:
 	case ConfirmRetrieve:
 	case RecruitOption:
 	case DieSelectOption:
 	case ConfirmRecruitOption:
 	case ConfirmBenefit:
 	case Resign:
 	case NormalStart:
 	case Gameover:
 	case ExtendedBenefit:
 	case ConfirmRecruits:
 	case ConfirmDiscardFactionless:
 	case EphemeralConfirmDiscardFactionless:
 	case ConfirmUseJacko:
 	case DumbKoff:
 	case PayForLionel:
 	case PayForBorna:
 	case Puzzle: break;
 	
 	case EphemeralConfirmRecruits:
	case ChooseRecruits:
 	case EphemeralChooseRecruits:
 		if(SIMULTANEOUS_PLAY)
 		{	
 			addRecruitShuffleMoves(p,val);
 		}
 		// otherwise fall into the usual
		//$FALL-THROUGH$
	case DiscardFactionless:
 	case EphemeralDiscardFactionless:

 	case RetrieveCommodityWorkers:
	case Retrieve:
 	case RetrieveOrConfirm:
 	case Retrieve1OrConfirm:
 	case PlaceOrRetrieve:
 	case Place:
 	case RePlace:
 	case PayForOptionalEffect:
 	case CollectBenefit:
 	case CollectOptionalBenefit:
 	case JoinTheEstablishment:
 	case BumpOpponent:
 	case ReUseWorker:
 	case ConfirmBump:
 	case FightTheOpressor:
 	case PayCost:
 	case PlaceAnother:
 	case ConfirmUseJackoOrContinue:
 	case ConfirmUseMwicheOrContinue:
 	case PlaceNew:
	case ChooseOneRecruit:
 	case ActivateOneRecruit:
 	case DiscardResources:
 		{	try {
 			CommonMoveStack all = GetListOfMoves(p,gui_errors==0);
 			while(all.size()>0)
 			{
 				EuphoriaMovespec m = (EuphoriaMovespec)all.pop();
 				switch(m.op)
 				{
 				case JOIN_THE_ESTABLISHMENT:
 				case FIGHT_THE_OPRESSOR:
 				case MOVE_DONE:
 				case USE_RECRUIT_OPTION:
 				case MOVE_RECRUIT:
 				case MOVE_LOSEMORALE:
 					break;
 				case MOVE_CHOOSE_RECRUIT:
 				case MOVE_ITEM_TO_BOARD:
 				case MOVE_SACRIFICE:
 				case MOVE_PLACE_WORKER:
 					{EuphoriaCell c = getCell(m.from_color,m.source);
 					val.put(c,m);
 					}
 					break;
 				case MOVE_MOVE_WORKER:
 				case MOVE_ITEM:
  				case MOVE_ITEM_TO_PLAYER:
 				case MOVE_RETRIEVE_WORKER:
 					{EuphoriaCell c = getCell(m.source,m.from_row);
 					 val.put(c,m);
 					}
 					break;
 				default: throw Error("not expecting %s",m);
 				}
 			}
 			}
 			catch (ErrorX err)
 				{
 				gui_errors++;
 				throw(err);
 				};
 			
 		}
 	}
 	return(val);
 }
 public void addWorkerRetrievalMoves( CommonMoveStack all, EPlayer p,boolean canRetrieveMarket)
 {	
 	Colors pcolor = p.color;
 	EuphoriaCell boardWorkers[] = p.placedWorkers;
 	boolean commodityOnly = board_state==EuphoriaState.RetrieveCommodityWorkers;	// for Samuel the Zapper
 	for(int lim = boardWorkers.length-1; lim>=0;lim--)
 	{	EuphoriaCell c = boardWorkers[lim];
 		if(c!=null)
 		{
 		 EuphoriaChip w = c.topChip();
 		 if(w instanceof WorkerChip)
 		 {
		 WorkerChip worker = (WorkerChip)c.topChip();
		 Assert(worker.color==pcolor,"Matching color");
		 if(worker.color==pcolor)
			 {	EuphoriaId rack = c.rackLocation();
			 	switch(rack)
				 {
				 case WastelanderBuildMarketA:
				 case WastelanderBuildMarketB:
				 case EuphorianBuildMarketA:
				 case EuphorianBuildMarketB:
				 case SubterranBuildMarketA:
				 case SubterranBuildMarketB:
					 if(!canRetrieveMarket) { break; }
				 //$FALL-THROUGH$
			default:
				 if(!commodityOnly || G.arrayContains(CommodityIds,rack))
				 {
				 all.addElement(new EuphoriaMovespec(c,p.newWorkers,worker,p.boardIndex));
				 }}
			 }}
		 }
	 }
 }
 private void addActivateRecruitMoves(CommonMoveStack all,EPlayer p)
 {
	 EuphoriaCell from = p.hiddenRecruits;
	 { if((from.height()>0)||(pickedObject!=null && pickedObject instanceof RecruitChip))
	 	{
		 all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.activeRecruits)); 
	 }}
 }
 private void addRecruitChoiceMoves(CommonMoveStack all, EPlayer p)
 {
	 for(EuphoriaCell from : p.newRecruits)
	 { if(from.height()>0)
	 	{RecruitChip top = (RecruitChip)from.topChip();
		 if(p.activeRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.activeRecruits)); }
		 if((top.allegiance!=Allegiance.Factionless) 
		 	&& (p.hiddenRecruits.height()==0)) { all.addElement(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.hiddenRecruits)); }
	 }}
 }
 
 // discard factionless recruits
 private void addRecruitDiscardMoves(CommonMoveStack all, EPlayer p)
 {
	 for(EuphoriaCell from : p.newRecruits)
	 { if(from.height()>0)
	 	{
		 RecruitChip recruit = (RecruitChip)from.topChip();
		 if(recruit.allegiance==Allegiance.Factionless) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.discardedRecruits)); }
	 }}
 }
 
 private void addSingleRecruitChoiseMoves(CommonMoveStack all, EPlayer p)
 {
	 for(EuphoriaCell from : p.newRecruits)
	 {	if(from.height()>0)
	 	{
		 if(recruitShouldBeActive((RecruitChip)from.topChip())) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.activeRecruits)); }
		 else { all.addElement(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.hiddenRecruits)); }
	 	}
	 }
 }
 private void addAuthority2Moves(CommonMoveStack all,Allegiance allegiance,EuphoriaCell marketA,EuphoriaCell marketb,EPlayer p)
 {	
 	EuphoriaCell src = p.authority;
 	if(src.height()>0)
 	{	
 		addAuthorityPlacementMoves(all,p,allegiance,src,marketA,marketA);
 		//addPlacementMoves(all,p,src,src.topChip(),0);
 	}
 }
 private void addSubterranGoods(CommonMoveStack all,EPlayer p)
 {
	 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
	 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
 }
 private void addGetArtifactMoves(CommonMoveStack all,Benefit bene, EPlayer p)
 {	 int navailable = 0;
	 if(isIIB()) 
	 {	boolean free = bene==Benefit.FreeArtifact;
		 boolean okSame = !p.penaltyAppliesToMe(MarketChip.IIB_StorageOfInsufficientCapacity);
		 int extraCost = p.penaltyAppliesToMe(MarketChip.IIB_DepartmentOfBribeRegulation) ? 1 : 0;
		 int n = p.totalCommodities()-extraCost;
		 //if(extraCost>0) { p1("pay extra for artifacts "+n); }
		 if(okSame || !p.hasArtifact(artifactBazaar[3].topChip()))
		 {	navailable++;
			 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,artifactBazaar[3],p,p.artifacts));
		 }
		 else { artifactBazaar[3].marketPenalty = MarketChip.IIB_StorageOfInsufficientCapacity;}
		 if(free || (n>0))
		 {	if(okSame|| !p.hasArtifact(artifactBazaar[1].topChip()))
		 		{
			 	navailable++;
			 	all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,artifactBazaar[1],p,p.artifacts));
		 		}
		 		else { artifactBazaar[1].marketPenalty = MarketChip.IIB_StorageOfInsufficientCapacity;}
		 	if(okSame || !p.hasArtifact(artifactBazaar[2].topChip()))
		 		{
		 		navailable++;
		 		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,artifactBazaar[2],p,p.artifacts));	
		 		}
			 	else { artifactBazaar[2].marketPenalty = MarketChip.IIB_StorageOfInsufficientCapacity;}
		 }
		 if(free || (n>1))
		 	{
			 if(okSame || !p.hasArtifact(artifactBazaar[0].topChip()))
			 {
			 navailable++;
			 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,artifactBazaar[0],p,p.artifacts));		
			 }
			 else { artifactBazaar[0].marketPenalty = MarketChip.IIB_StorageOfInsufficientCapacity;}
		 	} 
		 if(navailable==0) 
		 	{ //p1("No artifacts available"); 
		 	  all.addElement(new EuphoriaMovespec(MOVE_MOVE_WORKER,p,genericSource,genericSink)); 		 	 
		 	}
	 }
	 else { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,unusedArtifacts,p,p.artifacts)); }
 }
 private boolean hasCollectedResource()
 {	for(int lim = droppedDestStack.size()-1; lim>=0; lim--)
 	{
	 EuphoriaId type = droppedDestStack.elementAt(lim).rackLocation();
	 switch(type)
	 {
	 case PlayerStone:
	 case PlayerGold:
	 case PlayerClay: return(true);
	 default: break;
	 }
 	}
 	return(false);
 }
 private boolean hasCollectedCommodity()
 {	for(int lim = droppedDestStack.size()-1; lim>=0; lim--)
 	{
	 EuphoriaId type = droppedDestStack.elementAt(lim).rackLocation();
	 switch(type)
	 {
	 case PlayerWater:
	 case PlayerFood:
	 case PlayerEnergy:
	 case PlayerBliss: return(true);
	 default: break;
	 }
 	}
 	return(false);
 }
 private void addGetResourceMoves(CommonMoveStack all,Benefit bene,EPlayer p)
 {	
	switch(bene)
	 {
	 case ResourceAndCommodity:
		 if(!hasCollectedCommodity()) { addGetResourceMoves(all,Benefit.Commodity,p);  }
		 if(!hasCollectedResource()) { addGetResourceMoves(all,Benefit.Resource,p); }
		 break;		 
	case ResourceOrCommodity:
		addGetResourceMoves(all,Benefit.Resource,p);
		//$FALL-THROUGH$
	case Commodity:
	case Commodityx2:
	case Commodityx3:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,farm,p,p.food));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,bliss,p,p.bliss));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,generator,p,p.energy));
		break;
	case WaterOrEnergy:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
		//$FALL-THROUGH$
	case Energy:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,generator,p,p.energy));
		break;
	case Water:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
		break;
	case Bliss:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,bliss,p,p.bliss));
		break;
	case Food:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,farm,p,p.food));
		break;
	case CardOrStone:
	case CardAndStone:		// the AND case only occurs when ministry of personal secrets is in effect
		 addGetArtifactMoves(all,bene,p);
		//$FALL-THROUGH$
	case Stone:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 break;
	 case CardOrClay:
	 case CardAndClay:		// the AND case only occurs when ministry of personal secrets is in effect
		 addGetArtifactMoves(all,bene,p);
		//$FALL-THROUGH$
	case Clay:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 break;
	 case CardOrGold:
	 case CardAndGold:	// the AND case only occurs when ministry of personal secrets is in effect
		 addGetArtifactMoves(all,bene,p);
		//$FALL-THROUGH$
	case Gold:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 break;
	 case Resource:
	 case Resourcex2:
	 case Resourcex3:
	 case Resourcex4:
	 case Resourcex5:
	 case Resourcex6:
	 case Resourcex7:
	 case Resourcex8:
	 case Resourcex9:

		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 break;
		 
	case IcariteInfluenceAndResourcex2:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 break;
		 
	case FreeArtifactOrResource:
		addGetResourceMoves(all,Benefit.Resource,p);
		addGetResourceMoves(all,Benefit.FreeArtifact,p);
		break;
	case IcariteInfluenceAndCardx2:	// this clause only is used in IIB
	case Artifact:					// where taking a card always requires an interaction
	case FirstArtifact:
	case FreeArtifact:
		addGetArtifactMoves(all, bene,p);
		break;
	 default: throw Error("Not expecting benefit %s",bene);
	 }
 }
 private void addPlacementMoves(CommonMoveStack all,EuphoriaCell src,EuphoriaChip ch,EPlayer p)
 {	EuphoriaId rack = src.rackLocation();
	switch(rack)
	 {
	 default: throw Error("Not expecting to place %s",rack);
	 case MoraleTrack: break;
	 case GenericPool:
	 	{
	 		 all.push(new EuphoriaMovespec(MOVE_ITEM,p,src,genericSink));
	 	}
	 	break;
	 case MarketBasket:
		 if(ch.isArtifact())
		 {
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.artifacts));
		 }
		 else { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.marketBasket)); }
		 break;
	 case UsedRecruits:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.activeRecruits));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.hiddenRecruits));
		 break;
	 case StoneQuarry:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.stone));
	 	break;
	 case ClayPit:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.clay));
		 break;
	 case GoldMine:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.gold));
		 break;
	 case FarmPool:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.food));
		 break;
	 case AquiferPool:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.water));
		 break;
	 case EnergyPool:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.energy));
		 break;
	 case BlissPool:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.bliss));
		 break;
	 case ArtifactBazaar:
	 case ArtifactDeck:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.artifacts));
		 break;
		 
	 }
	 
 }

 void addCardToDiscardMoves(CommonMoveStack all,EPlayer p)
 {	int mask=0;
 	EuphoriaCell src = p.artifacts;
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
 		int bit = 1<<ch.id.ordinal();
 		if((mask&bit)==0)
 		{
 			mask |= bit;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
	 
 }
 // prefer moves that use no pairs
 void addUnpairedDiscardMoves(CommonMoveStack all,EPlayer p)
 { 	EuphoriaCell src = p.artifacts;
 	int pairs = p.getArtifactPairMask();
 	boolean some = false;
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
 		int bit = 1<<ch.id.ordinal();
	 		if((bit&pairs)==0)
	 	{	some = true;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 	if(!some) { addCardToDiscardMoves(all,p); }
 }
 // prefer moves that use pairs
 void addPairedDiscardMoves(CommonMoveStack all,EPlayer p)
 { 	EuphoriaCell src = p.artifacts;
 	int pairs = p.getArtifactPairMask();
 	int used = 0;
 	boolean some = false;
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
 		int bit = 1<<ch.id.ordinal();
	 		if(((bit&pairs)!=0) && ((bit&used)==0))
	 	{	some = true;
	 		used |= bit;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 	if(!some) { addCardToDiscardMoves(all,p); }
 }
 // prefer moves that match some existing chip
 void addMatchingDiscardMoves(CommonMoveStack all,EPlayer p,EuphoriaChip match)
 { 	EuphoriaCell src = p.artifacts;
 	boolean some = false;
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
  		if((match==ch) || p.alternateArtifacts.containsChip(ch))
	 	{	some = true;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 	if(!some) { addCardToDiscardMoves(all,p); }
 }
 
 // prefer moves that match some existing chip
 void addPayArtifactMoves(CommonMoveStack all,EPlayer p,EuphoriaChip match,EuphoriaChip match2)
 { 	EuphoriaCell src = p.artifacts;
 	if(numberOfArtifactsPaid()>=3) { return; }
 	int paid = 0;
 	if((pickedObject!=null) && pickedObject.isArtifact())
 	{
 		paid |= ((ArtifactChip)pickedObject).typeMask();
 		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,-1,usedArtifacts));
 	}
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
 		int mask = ch.typeMask();
  		if( ((mask&paid)==0) && ((match==ch) || (match2==ch) || p.alternateArtifacts.containsChip(ch)))
	 	{	paid |= mask;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 }

 void addPayArtifactMoves(CommonMoveStack all,EPlayer p)
 { 	EuphoriaCell src = p.artifacts;
	if(numberOfArtifactsPaid()>=3) { return; }
 	int paid = 0;
 	for(int lim=src.height()-1; lim>=0; lim--)
 	{
 		ArtifactChip ch = (ArtifactChip)src.chipAtIndex(lim);
 		int mask = ch.typeMask();
  		if((mask&paid)==0)
	 	{	paid |= mask;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 }
 void addPay2ArtifactMoves(CommonMoveStack all,EPlayer p,EuphoriaChip match)
 {
	 if(hasPaidArtifact(p,match) || (droppedDestStack.size()==0))
	 {
		 addPayArtifactMoves(all,p);
	 }
	 else { addPayArtifactMoves(all,p,match,null); }
 }
 
 
 // for the robot, playing unpaired moves is preferred
 void addRobotUnpairedDiscardMoves(CommonMoveStack all,EPlayer p)
 {
	 if(robotBoard)
	 { addUnpairedDiscardMoves(all,p);
	 }
	 else
	 {
	 addCardToDiscardMoves(all,p);
	 }
 }
 // for the robot, only consider matching moves if available.
 void addRobotMatchingDiscardMoves(CommonMoveStack all,EPlayer p,ArtifactChip match)
 {
	 if(robotBoard)
	 {
		 addMatchingDiscardMoves(all,p,match);
	 }
	 else { addCardToDiscardMoves(all,p); }
 }
 
 private void addPayGoldMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.gold.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.gold,goldMine)); }
 }
 private void addPayClayMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.clay.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.clay,clayPit)); }

 }
 private void addPayStoneMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.stone.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.stone,quarry)); }
 }
 
 private void addDiscardResourceMoves(CommonMoveStack all,EPlayer p,EuphoriaChip moving)
 {	//p1("add discard resources "+board_state);
 	if(moving!=null) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,getSource(),trash)); }
 	else {
	 if(p.stone.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.stone,trash)); }
	 if(p.gold.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.gold,trash)); }
	 if(p.clay.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.clay,trash)); }
 	}
 }

 private void addPayResourceMoves(CommonMoveStack all,EPlayer p)
 {	 addPayGoldMoves(all,p);
 	 addPayClayMoves(all,p);
 	 addPayStoneMoves(all,p);
 }
 private void addPayWaterMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.water.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.water,aquifer)); }
 }
 private void addPayFoodMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
 }
 private void addPayEnergyMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.energy.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.energy,generator)); }
 }
 private void addPayBlissMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); } 
 }

 private void addPayCommodityMoves(CommonMoveStack all,EPlayer p,boolean withBliss)
 {	addPayWaterMoves(all,p);
 	addPayFoodMoves(all,p);
 	addPayEnergyMoves(all,p);
 	if(withBliss)
	 { addPayBlissMoves(all,p);
	 }
 }
 boolean hasTaken(EuphoriaCell where)
 {	 Assert(!where.rackLocation().perPlayer,"shouldn't be a per-player source");
	 return pickedSourceStack.contains(where);
 }
 private boolean hasTakenNothing() { return pickedSourceStack.size()==0; }
 private boolean hasTakenArtifact() 
 {
	 for(int lim=pickedSourceStack.size()-1; lim>=0; lim--)
	 {
		 EuphoriaCell c = pickedSourceStack.elementAt(lim);
		 switch(c.rackLocation())
		 {
		 case ArtifactDeck:
		 case ArtifactBazaar:
			 return(true);
			default: break;
		 }
	 }
	 return(false);
 }
 
 private boolean hasPaid(EuphoriaCell where)
 {
	return droppedDestStack.contains(where);
 }
 private int numberPaid(EuphoriaCell where)
 {	int n = 0;
	for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
	{
		if(droppedDestStack.elementAt(lim)==where) { n++; }
	}
	return(n);
 }
 private int numberCommoditiesPaid()
 {
	 int n = 0;
	 for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
		{	EuphoriaCell c = droppedDestStack.elementAt(lim);
			if((c==farm)||(c==generator)||(c==aquifer)||(c==bliss)) { n++; }
		}
	 return(n);	
 }
 
 private boolean hasPaidResource()
 {
	 return(hasPaid(goldMine)||hasPaid(clayPit)||hasPaid(quarry));
 }
 private boolean hasPaidCommodity()
 {
	 return (hasPaid(bliss)||hasPaid(farm)||hasPaid(generator)||hasPaid(aquifer));
 }
 
 private boolean hasPaidSomething() { return(droppedDestStack.size()>0); }
 
 //
 // Note: this logic is only valid if at most one card has been played
 // return true if an artifact has been played and it matches mtype,
 // considering the player's list of wildcard "use as any" artifact types
 //
 private boolean hasPaidArtifact(EPlayer p,EuphoriaChip type)
 {	if(numberOfArtifactsPaid()==1)
 	{	ArtifactChip top = (ArtifactChip)usedArtifacts.topChip();
 		if((top==type) || p.alternateArtifacts.containsChip(top)) { return(true); }
 	}
 	return false;
 }
 
 int numberOfArtifactsPaid()
 {	
	 int na = 0;
	 for(int lim=droppedDestStack.size()-1; lim>=0; lim--)
	 	{
		 if(droppedDestStack.elementAt(lim)==usedArtifacts) { na++; }
	 	}
	 return(na);
 }
 
 void addPayCardMoves(CommonMoveStack all,EPlayer p)
 {	 if(numberOfArtifactsPaid()>=3) { return; }
	 if(robotBoard)
	 {
	 EuphoriaCell dest = droppedDestStack.top();
	 if(dest==null)
	 	{
		addPairedDiscardMoves(all,p);	// prefer pairs 
	 	}
	 	else {	// prefer matching
	 		EuphoriaChip top = dest.topChip();
	 		addMatchingDiscardMoves(all,p,top);
	 	}
	 }
	 else
	 {
	 addCardToDiscardMoves(all,p);
	 }
 }
 ArtifactChip artifactCardForCost(Cost cost)
 {
	 switch(cost)
	 {
	 case Commodity_Bat:
	 case Bat: return ArtifactChip.Bat;
	 case Commodity_Box:
	 case Box: return ArtifactChip.Box;
	 case Commodity_Balloons:
	 case Balloons: return ArtifactChip.Balloons;
	 case Commodity_Bifocals:
	 case Bifocals: return ArtifactChip.Bifocals;
	 case Commodity_Book:
	 case Book: return ArtifactChip.Book;
	 case Commodity_Bear:
	 case Bear: return ArtifactChip.Bear;
	 default: 
		 throw Error("Not expecting %s",cost);
	 }
 }
 boolean legalEnergyMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if((nbliss+nfood)>1) { return false; }
	 if((tot==2)&&((nenergy+nwater)>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 
 boolean legalBlissEnergyMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if(nbliss>2) { return false; }
	 if(nenergy>2) { return false; }
	 if(nfood>1) { return false; }
	 
	 if((tot==2)&&((nenergy+nbliss+nwater)>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 boolean legalBlissMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 
	 if(nbliss>2) { return false; }
	 if(nenergy>1) { return false; }
	 if(nfood>1) { return false; }
	 
	 if((tot==2)&&(nbliss>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 
 boolean legalBlissWaterMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if(nbliss>2) { return false; }
	 if(nenergy>1) { return false; }
	 if(nfood>1) { return false; }
	 
	 if((tot==2)&&((nbliss+nwater)>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 
 boolean legalBlissFoodMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if(nbliss>2) { return false; }
	 if(nfood>2) { return false; }
	 if(nenergy>1) { return false; }
	 
	 if((tot==2)&&((nfood+nbliss+nwater)>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 
 boolean legalFoodMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if((nbliss+nenergy)>1) { return false; }
	 if((tot==2)&&((nfood+nwater)>=1)) { return(true); }
	 // at most 1 non-water
	 return ((nbliss+nfood+nenergy)<=1);
 }
 boolean legalWaterMwicheTheFlusherAndCommodity(EuphoriaCell added)
 {	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 
	 if((nbliss+nfood+nenergy)>1) { return false; }	// at most 1 non-water
	 if((tot==2)&&((nwater)==2)) { return(true); }
	 // at most 1 non-water
	 return (true);
 }
 boolean legalEnergyx3OrBlissx3AndCommodity(EuphoriaCell added)
 {
	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 if(tot>4) { return(false); }
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 if(nenergy>0 && nbliss>0 && (nfood+nwater)>0) { return(false); }
	 if(nfood+nwater>1) { return(false); }		// the other 2 commodities
	 if(nenergy>=2 && nbliss>=2) { return(false); }
	 return(true);
	 
 }
 boolean legalWaterx3OrBlissx3AndCommodity(EuphoriaCell added)
 {
	 int tot = droppedDestStack.size() + (added==null ? 0 : 1);
 	 if (tot<=1) { return(true); }	// first can be anything
 	 if(tot>4) { return(false); }
 	 
	 int nbliss = numberPaid(bliss) + (added == bliss ? 1 : 0);
	 int nfood = numberPaid(farm) + (added == farm ? 1 : 0);
	 int nenergy = numberPaid(generator) + (added==generator? 1 : 0);
	 int nwater = numberPaid(aquifer) + (added == aquifer ? 1 : 0);
	 if(nwater>0 && nbliss>0 && (nfood+nenergy)>0) { return false; }
	 if(nfood+nenergy>1) { return(false); }		// the other 2 commodities
	 if(nwater>=2 && nbliss>=2) { return(false); }
	 return(true);
	 
 }
 //
 // the general contract is to add legal moves that contribute to the cost.
 // when enough stuff is added, the state will switch from "pay" to "confirm"
 // mostly this means just adding any stuff that's part of the contract, but
 // some mixed cases require looking at what's been played so far - for example
 // gold+card must add gold moves only if none has been played yet.
 //
 // recruits which allow substitutions are already handled by changing
 // the cost before we get here.
 //
 void addPaymentMoves(CommonMoveStack all,EPlayer p,Cost cost)
 {	if(cost==null) { p1("Board payment missing cost"); }
 	else
 	{
	 switch(cost)
	 {
		
	 default: 
		 throw Error("Not expecting add payment %s",cost);
	 case SacrificeOrCommodityX3:
		 //p1("pay "+cost);	 // tested 3/22
		 addPayCommodityMoves(all,p,true);
		 if(!hasPaidSomething()) { addPaymentMoves(all,p,Cost.SacrificeAvailableWorker); }
		 break;
	 case SacrificeOrGoldOrCommodityX3:
		 //p1("pay "+cost);	// tested 3/21
		 addPayCommodityMoves(all,p,true); 
		 if(hasPaidSomething()) { break; }
		//$FALL-THROUGH$
	 case SacrificeOrGold:
		 // p1("pay "+cost); // tested 3/21	
		 addPayGoldMoves(all,p);
		 addPaymentMoves(all,p,Cost.SacrificeAvailableWorker);
		 break;
		 
	 case SacrificeOrStoneOrCommodityX3:
		 //p1("pay "+cost);	// tested 3/23
		 addPayCommodityMoves(all,p,true); 
		 if(hasPaidSomething()) { break; }
		//$FALL-THROUGH$
	 case SacrificeOrStone:
		 // p1("pay "+cost); //tested 3/21
		 addPayStoneMoves(all,p);
		 addPaymentMoves(all,p,Cost.SacrificeAvailableWorker);
		 break;
		 
	 case SacrificeOrClayOrCommodityX3:
		 //p1("pay "+cost); tested 3/23	
		 addPayCommodityMoves(all,p,true); 
		 if(hasPaidSomething()) { break; }
			//$FALL-THROUGH$
	 case SacrificeOrClay:
		 addPayClayMoves(all,p);
		//$FALL-THROUGH$
	 case SacrificeAvailableWorker:
		 // p1("pay "+cost); tested 3/21	
		 for(int lim=p.workers.height()-1; lim>=0; lim--)
		 {
			 all.push(new EuphoriaMovespec(MOVE_SACRIFICE,p,p.workers,lim));
		 }
		 break;
	 case BlissOrWaterAndCommodity:
		 //p1("pay "+cost);	// tested 3/21
		 if(!hasPaidSomething() || hasPaid(bliss) || hasPaid(aquifer) )
		 	{ addPayCommodityMoves(all,p,true); 
		 	}
		 else 
		 	{ addPayBlissMoves(all,p);
		 	  addPayWaterMoves(all,p); 
		 	}
		break;
	 case BlissOrFree:
		 addPayBlissMoves(all,p);
		 break;
		 
	 case BlissOrFoodAndCommodity:
		 //p1("pay "+cost);	tested 3/21
		 if(!hasPaidSomething() || hasPaid(bliss) || hasPaid(farm) )
		 	{ addPayCommodityMoves(all,p,true); 
		 	}
		 else 
		 	{ addPayBlissMoves(all,p);
		 	  addPayFoodMoves(all,p); 
		 	}
		break;
		
	 case BlissOrEnergyAndCommodity:
		 //p1("pay "+cost); tested 3/21	
		 if(!hasPaidSomething() || hasPaid(bliss) || hasPaid(generator) )
		 	{ addPayCommodityMoves(all,p,true); 
		 	}
		 else 
		 	{ addPayBlissMoves(all,p);
		 	  addPayEnergyMoves(all,p); 
		 	}
		break;
		
	 case Artifactx3Only:
		 //p1("paying artifactx3 only");
		 addPayArtifactMoves(all,p);
		 break;
	 case ArtifactAndBlissx2AndCommodity:
	 	{
	 	 //p1("pay "+cost); // tested 3/21
		 if(!hasPaidArtifact(p,null)) { addPayArtifactMoves(all,p); }
		 int nc = numberCommoditiesPaid();
		 int nb = numberPaid(bliss);
		 if(nc>nb) { addPayBlissMoves(all,p); }	// only bliss will do
		 else { addPayCommodityMoves(all,p,true);}
	 	}
	 	break;
	 case Artifactx3OrArtifactAndBlissx2AndCommodity:
	 	{
	    //p1("pay "+cost);	// tested 3/21
	 	int na = numberOfArtifactsPaid();
	 	int nc = numberCommoditiesPaid();
	 	if(na>=2) 
	 		{ 	
	 			// paid the full artifact cost, we shouldn't be here if the commodity has
	 			// also been paid
	 		if(nc==0) { addPayCommodityMoves(all,p,true); }
	 		if((na<3)&&!hasPaidArtifactPair(p)) {  addPayArtifactMoves(all,p); }
	 		}
	 	else {
	 	int nb = numberPaid(bliss);
	 	if((nc<=1) || (na==0))
	 		{// not committed to bliss+commodities, or no artifacts paid yet
	 		 addPayArtifactMoves(all,p); 
	 		}
	 	if(nc<3)
	 		{if(nc-nb>0) { addPayBlissMoves(all,p); }	// only bliss will do
	 		 else { addPayCommodityMoves(all,p,true);  }
	 		}
	 	}}
	 	
	 	break;
	 case Artifactx3OrArtifactAndBlissx2:
	 	{
	 	//p1("pay "+cost);	// tested 3/21
		 int na = numberOfArtifactsPaid();
		 int nb = numberPaid(bliss);
		 if(na==0) { addPayArtifactMoves(all,p); }
		 if((nb<2) && (na<2)) { addPayBlissMoves(all,p); }
		 if((na>0) && (nb==0))
		 	{ 
			 // nasty case, he's paid a artifact and has one more. 
			 // if he could pay a pair include that as an option
			 if((na>1) 
					 || ((na+p.artifacts.height())>=3)	// can pay 3
					 || ((na==1) 
							 && (p.artifacts.height()==1)
							 && p.artifactsMatch((ArtifactChip)usedArtifacts.topChip(),(ArtifactChip)p.artifacts.topChip())))
			 {				
			 addPayArtifactMoves(all,p); 
			 }}
		 }
	 	break;
	 	
	 case Energyx3OrBlissx3AndCommodity:
		 // p1("pay "+cost); 
		 // tested 3/30.  To get here you have to have the penalty from agency of progressive
		 // backstabbing and the recruit gary the forgetter, and be bumping yourself to from
		 // the worker activation tank
		 if(legalEnergyx3OrBlissx3AndCommodity(generator)) { addPayEnergyMoves(all,p); }
		 if(legalEnergyx3OrBlissx3AndCommodity(farm)) { addPayFoodMoves(all,p); }
		 if(legalEnergyx3OrBlissx3AndCommodity(bliss)) { addPayBlissMoves(all,p); }
		 if(legalEnergyx3OrBlissx3AndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		 break;
	 case Energyx3OrBlissx3:
		 // p1("pay "+cost); tested 3/23	
		 if(hasPaid(bliss)) { addPayBlissMoves(all,p); }
		 else if(hasPaid(generator)) { addPayEnergyMoves(all,p); }
		 else 
		 {	addPayBlissMoves(all,p);
		 	addPayEnergyMoves(all,p);
		 }
		 break;
		 
	 case Waterx3OrBlissx3AndCommodity:
		 // p1("pay "+cost); 
		 // tested 3/30.  To get here you have to have the penalty from agency of progressive
		 // backstabbing and the recruit gary the forgetter, and be bumping yourself to from
		 // the worker activation tank
		 if(legalWaterx3OrBlissx3AndCommodity(generator)) { addPayEnergyMoves(all,p); }
		 if(legalWaterx3OrBlissx3AndCommodity(farm)) { addPayFoodMoves(all,p); }
		 if(legalWaterx3OrBlissx3AndCommodity(bliss)) { addPayBlissMoves(all,p); }
		 if(legalWaterx3OrBlissx3AndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		 break;
		 
	 case Waterx3OrBlissx3:
		 // p1("pay "+cost); // tested 3/23	
		 if(hasPaid(bliss)) { addPayBlissMoves(all,p); }
		 else if(hasPaid(aquifer)) { addPayWaterMoves(all,p); }
		 else 
		 {	addPayBlissMoves(all,p);
		 	addPayWaterMoves(all,p);
		 }
		 break;
	 case Commodity_Artifact:
		 // p1("pay "+cost); tested 3/31
		 if(!hasPaidArtifact(p,null)) { addPayArtifactMoves(all,p); }
		 if(!hasPaidCommodity()) { addPayCommodityMoves(all,p,true); }
		 break;
		 
	 case Book_Card:
		 // p1("pay "+cost); // tested 3/21	
		 // 2 cards, one must be book-compatible
		 addPay2ArtifactMoves(all, p, ArtifactChip.Book);
		 break;
		 
	 case Book_CardAndCommodity:
		 //p1("Pay "+cost); // tested 3/31
		 if(!hasPaidCommodity()) { addPayCommodityMoves(all,p,true); }
		 if(numberOfArtifactsPaid()<2)
			 {	if(hasPaidArtifact(p,ArtifactChip.Book)) 
			 		{ addPayArtifactMoves(all,p,ArtifactChip.Book,null); 
			 		}
			 	else { addPayArtifactMoves(all,p); }
			 }
		break;
	 case Commodity_Box:
	 case Commodity_Balloons:
	 case Commodity_Bifocals:
	 case Commodity_Bat:
	 case Commodity_Bear:
	 case Commodity_Book:
			if(!hasPaidCommodity()) { addPayCommodityMoves(all,p,true); }
			if(!hasPaidArtifact(p,ArtifactChip.Book)) { addPayArtifactMoves(all,p,artifactCardForCost(cost),null); }
	 	break;
	 case Bifocals:
	 case Balloons:
	 case Bear:
	 case Box:
	 case Book:
	 case Bat:
		 addPayArtifactMoves(all,p,artifactCardForCost(cost),null);
		 break;
	 case ResourceX3AndCommodity:
	 	{
	 	//p1("pay "+cost);	// tested 3/31
	 	boolean hp = hasPaidCommodity();
	 	if(!hp) { addPayCommodityMoves(all,p,true); }
	 	if((droppedDestStack.size()<3) || hp) 
	 		{ addPayResourceMoves(all,p); 
	 		}
	 	}
		 break;
		 
	 case NonBlissAndCommodity:
	 case NonBliss:
		 //p1("pay "+cost);	tested 3/21
		 addPayCommodityMoves(all,p,!hasPaid(bliss));
		 break;
		 
	 case ClayOrCommodityX3:
		 //p1("pay "+cost); tested 3/21	
		 if(!hasPaidSomething()) { addPayClayMoves(all,p); }
		 addPayCommodityMoves(all,p,true);
		 break;
	 case ClayOrBlissOrFood:
		 addPayBlissMoves(all,p);
		//$FALL-THROUGH$
	case ClayOrFood:
		 addPayClayMoves(all,p);
		 addPayFoodMoves(all,p);
		 break;
	 case StoneOrBlissOrFood:
		 addPayBlissMoves(all,p);
		//$FALL-THROUGH$
	case StoneOrFood:
		 //("pay "+cost.name()); // tested 4/1
		 addPayStoneMoves(all,p); 
		 addPayFoodMoves(all,p);
		 break;
	 case StoneOrCommodityX3:
		 //p1("pay "+cost);	// tessted 3/21
		 if(!hasPaidSomething()) { addPayStoneMoves(all,p);} 
		 addPayCommodityMoves(all,p,true);
		 break;
	 case GoldOrCommodityX3:
		 // p1("pay "+cost); // tested 3/21	
		 if(!hasPaidSomething()) { addPayGoldMoves(all,p); }	// paid first or not at all
		//$FALL-THROUGH$
	 case Commodity:
	 case CommodityX2:
	 case CommodityX3:
		 addPayCommodityMoves(all,p,true);
		 break;
	 case ResourceOrBliss:
		 addPayResourceMoves(all,p);
		 addPayBlissMoves(all,p);
		 break;
	 case NonBlissCommodity:
		 addPayCommodityMoves(all,p,false);
		 break;
	 case ResourceOrBlissOrFood:
		 addPayResourceMoves(all,p);
		 addPayBlissMoves(all,p);
		 addPayFoodMoves(all,p);
		 break;
	 case EnergyOrBlissOrFoodRetrieval:
		 addPayEnergyMoves(all,p);
		 //$FALL-THROUGH$
	 case BlissOrFoodRetrieval:
	 case BlissOrFoodExactly:
		 addPayBlissMoves(all,p);
		//$FALL-THROUGH$
	 case Food:
		 addPayFoodMoves(all,p);
		 break;
	 case BlissOrFoodPlus1:
	 	{
	 	 // p1("pay "+cost); // tested 3/24	
		 // add a food or a bliss, and one other not bliss
		 boolean hasBliss = hasPaid(bliss);
		 boolean hasFood = hasPaid(farm);
		 if(hasFood || hasBliss)
		 {
			 addPayCommodityMoves(all,p,false);
		 }
		 if(hasPaidSomething() && !(hasFood || hasBliss))
		 {	// if we started with a commodity, only consider food and bliss
			 addPayFoodMoves(all,p);
			 addPayBlissMoves(all,p);
		 }
		 else { addPayCommodityMoves(all,p,!hasBliss); }
	 		}
		 break;
		 
	 case GoldOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 addPayGoldMoves(all,p);
		 break;
		 
	 case StoneOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 addPayStoneMoves(all,p);
		 break;
		 
	 case ClayOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 addPayClayMoves(all,p);
		 break;
		 
	 case CommodityOrResourcex4Penalty:
	 case CommodityOrResourcex3Penalty:
	 case CommodityOrResourcex2Penalty:
	 case CommodityOrResourcePenalty:
		 addPayCommodityMoves(all,p,true);
		 addPayResourceMoves(all,p);
		 break;
		 
	case WaterOrKnowledge:
		addPayWaterMoves(all,p);
		break;
	case EnergyOrKnowledge:
		addPayEnergyMoves(all,p);
		break;
	case FoodOrResource:
		addPayResourceMoves(all,p);
		//$FALL-THROUGH$
	case FoodOrKnowledge:
		addPayFoodMoves(all,p);
		break;
		
	case BlissOrWater:
		addPayWaterMoves(all,p);
		addPayBlissMoves(all,p);
		break;
	case BlissOrEnergy:
		addPayEnergyMoves(all,p);
		addPayBlissMoves(all,p);
		break;
	 case Card:
	 case Artifact:
	 case Cardx2:
	 case Cardx3:
	 case Cardx4:
	 case Cardx5:
	 case Cardx6:
	 case CardForGeek:
	 case CardForGeekx2:
		 addRobotUnpairedDiscardMoves(all,p);
		 break;
 	 case ArtifactJackoTheArchivist_V2:
		 addRobotUnpairedDiscardMoves(all,p);
		 break;
	 case BatOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Bat);
		 break;
	 case BookOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Book);
		 break;
	 case BoxOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Box);
		 break;
	 case BalloonsOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Balloons);
		 break;
	 case BearOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Bear);
		 break;
	 case BifocalsOrCardx2:
		 addRobotMatchingDiscardMoves(all,p,ArtifactChip.Bifocals);
		 break;
		 
	 case Artifactx2:	// jacko the archivist
		 addUnpairedDiscardMoves(all,p);
		 break;
	 case Energyx4_Card:
		 Assert(revision<115,"only if the old bug is present");
		 addRobotUnpairedDiscardMoves(all,p);
		 // fall through
		//$FALL-THROUGH$
	case Energy:
		addPayEnergyMoves(all,p);
		 break;
		 
	 case Morale_Artifactx3_Brian:
	 case Artifactx3:
		 addPayCardMoves(all,p);

		 	break;
		
	 case Card_BlissOrFood:
	 	{
	 	//p1("Pay "+cost); // tested 4/1
	 	if(!hasPaid(usedArtifacts)) { addRobotUnpairedDiscardMoves(all,p); }
	 	if(!hasPaidCommodity()) { addPayBlissMoves(all,p); addPayFoodMoves(all,p);}
	 	}
	 	break;
	 case Card_ResourceOrBlissOrFood:
	 	{
	 	// this involves brian the vituculturist
	 	//p1("pay "+cost); // tested 4/1	
		boolean hasCard = hasPaid(usedArtifacts);
		boolean hasBliss = hasPaid(bliss);
		boolean hasFood = hasPaid(farm);
		boolean hasResource = hasPaidResource();
		if(!hasCard) { addRobotUnpairedDiscardMoves(all,p); }
		if(!(hasResource||hasBliss||hasFood))
			{ 
			addPayBlissMoves(all,p);
			addPayFoodMoves(all,p);
			addPayResourceMoves(all,p);
			}
	 	}
	 	break;
	 case Card_FoodOrResource:
	 	{
	    // this is a subcost of Card_ResourceOrBlissOrFood 
	 	// p1("pay "+cost); // tested 4/1	
	 	boolean hasCard = hasPaid(usedArtifacts);
	 	boolean hasFood = hasPaid(farm);
	 	boolean hasResource = hasPaidResource();
	 	
		if(!hasCard) { addRobotUnpairedDiscardMoves(all,p); }
		if(!(hasResource||hasFood))
			{ 
			addPayFoodMoves(all,p);
			addPayResourceMoves(all,p);
			}
	 	}
	 	break;
		 
	 case Card_ResourceOrBliss:
	 	{
	 	//p1("pay "+cost); tested 4/1	
	 	// this is a subcost of Card_ResourceOrBlissOrFood 
	 	boolean hasCard = hasPaid(usedArtifacts);
	 	boolean hasBliss = hasPaid(bliss);
	 	boolean hasResource = hasPaidResource();
	 	
		if(!hasCard) { addRobotUnpairedDiscardMoves(all,p); }
		if(!(hasResource||hasBliss))
			{ 
			addPayBlissMoves(all,p);
			addPayResourceMoves(all,p);
			}
	 	}
	 	break;
	 case Card_Resource: // card and a resource for a market
	 	{
	 	// josh the negotiator, lab of selective genetics
	 	// p1("pay "+cost); // tested 3/31
		boolean hasCard = hasPaid(usedArtifacts);;
	 	boolean hasResource = hasPaidSomething()&&!hasCard;
	 	if(!hasCard) 
	 		{ addRobotUnpairedDiscardMoves(all,p);
	 		}
	 	if(!hasResource) { addPayResourceMoves(all,p); } 
	 	}
		 break;
		 
	 case BlissOrFoodx4_ResourceOrBlissOrFood:
	 	{	
	 	// at most 1 resource, the rest can be food or bliss.  Brian the viticulturist
	 	//p1("pay "+cost); //tested 3/31	
		int blissorfood = countBlissOrFood(droppedDestStack);
		boolean hasResource = droppedDestStack.size()>blissorfood;
	    if(!hasResource) 
    		{ addPayResourceMoves(all,p);
    		}
	    addPayFoodMoves(all,p);	// brian can use food
	    addPayBlissMoves(all,p);	// josh can use all bliss
	 	}
	 	break;
	 case BlissOrFoodx4_Card: 
	 	{	// brian the viticulturist
	 	//p1("pay "+cost);	// tested 3/31
	 	int blissorfood = countBlissOrFood(droppedDestStack);
	 	boolean hasCard = droppedDestStack.size()>blissorfood;
	 	boolean roomForFood = (blissorfood<4);
	    if(!hasCard) 
	    	{
	    	addRobotUnpairedDiscardMoves(all,p);
	    	}
	    if(roomForFood)
	    {
	    	addPayFoodMoves(all,p);
	    	addPayBlissMoves(all,p);
	 	}}
	 	break;
	 case BlissOrFoodx4:
		 addPayFoodMoves(all,p);
		 addPayBlissMoves(all,p);
		 break;
	 case BlissOrFoodx4_Resource:
	 	{	// brian the viticulturist
		//	 p1("pay "+cost);	// tested 3/31
	 	int blissorfood = countBlissOrFood(droppedDestStack);
	 	boolean hasResource = droppedDestStack.size()>blissorfood;
	 	boolean roomForFood = (blissorfood<4);
	    if(!hasResource) 
	    	{ if(cost==Cost.BlissOrFoodx4_Resource) { addPayResourceMoves(all,p); }
	    		else 
	    		{addRobotUnpairedDiscardMoves(all,p); 
	    		} 
	     	}
	    if(roomForFood)
	    {
	    	addPayFoodMoves(all,p);
	    	addPayBlissMoves(all,p);
	 	}}
	 	break;
	 case ResourceAndKnowledgeAndMoraleOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 // fall into add resoruce moves
		//$FALL-THROUGH$
	 case ResourceAndKnowledgeAndMorale:
	 case Resource:
	 case Morale_Resourcex3_Brian:
	 case Resourcex3:	// pay 3 resources (nimbus loft)
		 addPayResourceMoves(all,p);
		 break;

	 case GoldOrFoodOrBliss:
		 addPayFoodMoves(all,p);
		//$FALL-THROUGH$
	case GoldOrBliss:
		// this can only come up when josh the negotiator is in effect, and bliss is also an option
		// since we only want 1, we don't need to worry about what has already been played
		addPayGoldMoves(all,p);
		addPayBlissMoves(all,p);
		 break;
	 case GoldOrFood:
		 addPayGoldMoves(all,p);
		 addPayFoodMoves(all,p);
		 break;
	 case StoneOrFoodOrBliss:
		 addPayFoodMoves(all,p);
		//$FALL-THROUGH$
	case StoneOrBliss:
		 // this can only come up when josh the negotiator is in effect, and bliss is also an option
		addPayStoneMoves(all,p);
		addPayBlissMoves(all,p);
		break;
		 
	 case ClayOrFoodOrBliss:
		 addPayFoodMoves(all,p);
		//$FALL-THROUGH$
	case ClayOrBliss:
		 // this can only come up when josh the negotiator is in effect, and bliss is also an option
		addPayClayMoves(all,p);
		addPayBlissMoves(all,p);
		break; 	

		
     // iib payments
	case BlissOrFreeMwicheTheFlusher:
		// p1("pay "+cost); // tested 4/3
		if(!hasPaidSomething()) { addPayBlissMoves(all,p); }	
		addPayWaterMoves(all,p); 
		break;
		
	case BlissOrEnergyMwicheTheFlusher:
		if(!hasPaidSomething()) { addPayBlissMoves(all,p); }			
		//$FALL-THROUGH$
	case FreeOrEnergyMwicheTheFlusher:
	case EnergyMwicheTheFlusher:
		 // p1("pay "+cost); // tested 3/21
		if(!hasPaidSomething()) { addPayEnergyMoves(all,p); }
		addPayWaterMoves(all,p); 
		break;
		

	case WaterMwicheTheFlusherAndCommodity:
		 // p1("pay "+cost); // tested 3/21
		if(legalWaterMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalWaterMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalWaterMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalWaterMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
		

	case FoodMwicheTheFlusherAndCommodity:
		// p1("pay "+cost);	 // tested 3/21
		if(legalFoodMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalFoodMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalFoodMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalFoodMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
	
	
	case BlissOrWaterMwicheTheFlusherAndCommodity:
		 //p1("pay "+cost); // tested 4/3	
		if(legalBlissWaterMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalBlissWaterMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalBlissWaterMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalBlissWaterMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
	
		
	case BlissMwicheTheFlusherAndCommodity:
		// p1("pay "+cost); // tested 4/3
		if(legalBlissMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalBlissMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalBlissMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalBlissMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;

		
		//$FALL-THROUGH$
	case BlissOrEnergyMwicheTheFlusherAndCommodity:
		 // p1("pay "+cost); tested 4/3	
		if(legalBlissEnergyMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalBlissEnergyMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalBlissEnergyMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalBlissEnergyMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
		
	case BlissOrFoodMwicheTheFlusherAndCommodity:
		// p1("pay "+cost); // tested 4/3	
		if(legalBlissFoodMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalBlissFoodMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalBlissFoodMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalBlissFoodMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
	
	case EnergyMwicheTheFlusherAndCommodity:
		// p1("pay "+cost);	 // tested 3/21
		if(legalEnergyMwicheTheFlusherAndCommodity(aquifer)) { addPayWaterMoves(all,p); }
		if(legalEnergyMwicheTheFlusherAndCommodity(farm)) { addPayFoodMoves(all,p); }
		if(legalEnergyMwicheTheFlusherAndCommodity(generator)) { addPayEnergyMoves(all,p); }
		if(legalEnergyMwicheTheFlusherAndCommodity(bliss)) { addPayBlissMoves(all,p); }
		break;
	

	case BlissOrFoodMwicheTheFlusher:
		 if(!hasPaidSomething()) { addPayBlissMoves(all,p); }			
		//$FALL-THROUGH$
	case FoodMwicheTheFlusher:
		 	if(!hasPaidSomething()) { addPayFoodMoves(all,p); }
		 	addPayWaterMoves(all,p);
		 	break;
		
	case FreeOrFoodMwicheTheFlusher:
	case BlissOrWaterMwicheTheFlusher:
		if(!hasPaidSomething()) { addPayBlissMoves(all,p); }		
		//$FALL-THROUGH$
	case FreeOrWaterMwicheTheFlusher:
	case WaterMwicheTheFlusher:
		 addPayWaterMoves(all,p);
		 break;
	 case ArtifactX3AndCommodity:
		 addPayCardMoves(all,p);
		 if(!hasPaidCommodity()) { addPayCommodityMoves(all,p,true); }
		 break;

	 }
	if((all.size()==0)&&robotBoard)
	{	//robotBoard = false;
		throw Error("Didn't produce playment moves for %s",cost);
	}}
 }
 
 private void addDilemmaMoves(CommonMoveStack all,EPlayer p)
 {	if(p.canResolveDilemma())
 	{	all.push(new EuphoriaMovespec(FIGHT_THE_OPRESSOR,p));
 		all.push(new EuphoriaMovespec(JOIN_THE_ESTABLISHMENT,p));
 	} 
 }
 public double neededToCompleteScore(EuphoriaId id)
 {
	 int needed = TOKENS_TO_OPEN_MARKET[players.length];
	 EuphoriaCell openers[] = null;
	 switch(id)
	 {
	 case EuphorianBuildMarketA: openers = euphorianBuildMarketA; break;
	 case EuphorianBuildMarketB: openers = euphorianBuildMarketB; break;
	 case SubterranBuildMarketA: openers = subterranBuildMarketA; break;
	 case SubterranBuildMarketB: openers = subterranBuildMarketB; break;
	 case WastelanderBuildMarketA: openers = wastelanderBuildMarketA; break;
	 case WastelanderBuildMarketB: openers = wastelanderBuildMarketB; break;
	 default: throw Error("not expecting %s",id);
	 }
	 for(EuphoriaCell c : openers) { if(c.topChip()!=null) { needed--; }}
	 switch(needed)
	 {
	 case 1:	return(100);
	 case 2:	return(20);
	 default:	return(1);
	 }
 }
 
 
 public double neededToCompleteScore_06(EuphoriaId id)
 {	// less aggressively promote market opening
	 int needed = TOKENS_TO_OPEN_MARKET[players.length];
	 EuphoriaCell openers[] = null;
	 boolean includesLeader = false;
	 switch(id)
	 {
	 case EuphorianBuildMarketA: openers = euphorianBuildMarketA; break;
	 case EuphorianBuildMarketB: openers = euphorianBuildMarketB; break;
	 case SubterranBuildMarketA: openers = subterranBuildMarketA; break;
	 case SubterranBuildMarketB: openers = subterranBuildMarketB; break;
	 case WastelanderBuildMarketA: openers = wastelanderBuildMarketA; break;
	 case WastelanderBuildMarketB: openers = wastelanderBuildMarketB; break;
	 default: throw Error("not expecting %s",id);
	 }
	 for(EuphoriaCell c : openers) 
	 	{ EuphoriaChip top = c.topChip();
	 	  if(top!=null) 
	 	  	{ needed--; 
	 	  	  if(top.color==leadingPlayer.color) { includesLeader = true; }
	 	  	}
	 	}
	 double rr = includesLeader 		// the leader is already on this market
			 	? ((riskRatio < 1.0)	// are we the leader?
			 			? riskRatio		 // we are not the leader, and this would help the leader 
			 			: 1.0/riskRatio) // we are the leader, and we're already here
			 	: 1.0;	// the leader is not represented
	 if(needed==1) { rr = -rr; }	// flag that we can open the market now
	 switch(needed)
	 {
	 case 1:	return(100*rr);
	 case 2:	return(5*rr);
	 default:	return(10*rr);	// encourage earlier placement in 4-6 player games
	 }
 }
 public double scorePotential(EPlayer p)
 {	double val = 1.0-p.authority.height()*0.1;
	val += p.totalCommodities()/60.0;
	val += p.totalResources()/30.0;
	val += p.artifacts.height()/25.0;
	return(Math.min(1.0,Math.max(0.1,val)));
}
public void setRiskRatio(int robo)
{	double my = scorePotential(players[robo]);
	EPlayer rp = robotPlayer = players[robo];
	double max = 0.0;
	leadingPlayer = null;
	for(EPlayer p : players)
		{ if(p!=rp)
			{ double sc = scorePotential(p);
			  if(leadingPlayer==null || sc>max)
				  { max = sc;
				    leadingPlayer = p;
				  }
			}
		}
	riskRatio = my/max;
	workersOnMarket = 0;
	for(EuphoriaCell ca[] : buildMarkets)
	{ for(EuphoriaCell c : ca)
		{
		EuphoriaChip top = c.topChip();
		if(top!=null && (top.color == rp.color)) { workersOnMarket++; }
		}
	}
}
double riskRatio = 1.0;
int workersOnMarket = 0;
EPlayer leadingPlayer = null;
EPlayer robotPlayer = null;
 // baseline
 public double scoreAsMontecarloMove_01(EuphoriaMovespec m)
 {	EPlayer p = players[m.player];
	 switch(m.op)
	 {	
	 case MOVE_PLACE_WORKER:
		 switch(m.dest)
	 	{
		 case WastelanderFarm:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
		 case SubterranAquifer:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case EuphorianGenerator:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case IcariteCloudMine:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.bliss.height()-5)*0.3)));
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore(m.dest);
			 switch(p.totalWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(1*sc);
			 case 3: return(2*sc);
			 case 4: return(4*sc);
			 default: throw Error("not expecting %s",p.totalWorkers);
			 }
		 	}
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.02);
		 default: break;
		 }
		 break;
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
 
 public double scoreAsMontecarloMove_06(EuphoriaMovespec m)
 {	EPlayer p = players[m.player];
	 switch(m.op)
	 {	
	 case MOVE_DONE:
		 switch(board_state)
		 {
		 case PlaceAnother:
			 return(0.02);		// do nothing when you could place another worker
		 default: return(1.0);
		 }
	 case MOVE_PLACE_WORKER:
		 switch(m.dest)
	 	{
		 case WastelanderFarm:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
		 case SubterranAquifer:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case EuphorianGenerator:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case IcariteCloudMine:
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.bliss.height()-5)*0.3)));
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore_06(m.dest);
			 if(sc<0) { return(-sc); }	// we can open the market
			 int availableWorkers = p.totalWorkers-workersOnMarket;
			 switch(availableWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(0.2*sc);
			 case 3: return(2*sc);
			 case 4: return(3*sc);
			 default: throw Error("not expecting %s",availableWorkers);
			 }
		 	}
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.0);	// for random tree, this really is zero
		 default: break;
		 }
		 break;
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
 private double recruitAdvantage(EPlayer p,EuphoriaMovespec m)
 {	Allegiance al = m.dest.allegiance;
 	int myRecruits = p.nActiveRecruits(al);
 	int otherRecruits = 0;
 	int totalKnowledge = newWorkerTotal(m);
 	for(EPlayer op : players)
 	{
 		if(p!=op) { otherRecruits += op.nActiveRecruits(al); }
 	}
 	
 	if(totalKnowledge<=4)
 	{	// advantage to us if we emphasize our own recruits
 		double adv = 1.0 + myRecruits - (otherRecruits/(players.length-1.0));
 		return(Math.max(0,adv));
 	}
 	else
 	{	// opposite situation, no advantage to place there
 		double adv = 1.0 + ((otherRecruits-myRecruits)/(players.length-1.0));
 		return(Math.max(0,adv));
 	}
 }
  private double recruitAdvantage_08(EPlayer p,EuphoriaMovespec m)
 {	Allegiance al = m.dest.allegiance;
 	int myRecruits = p.nActiveRecruits(al);
 	int otherRecruits = 0;
 	int totalKnowledge = newWorkerTotal_08(m);
 	for(EPlayer op : players)
 	{
 		if(p!=op) { otherRecruits += op.nActiveRecruits(al); }
 	}
 	
 	if(totalKnowledge<=4)
 	{	// advantage to us if we emphasize our own recruits
 		double adv = 1.0 + (myRecruits - (otherRecruits/(players.length-1.0)));
 		return(Math.max(0,adv));
 	}
 	else
 	{	// opposite situation, no advantage to place there
 		double adv = 1.0 + ((otherRecruits-myRecruits)/(players.length-1.0));
 		return(Math.max(0,adv));
 	}
 }
 
 public int newWorkerTotal(EuphoriaMovespec m)
 {	int knowledge = 0;
 	EuphoriaCell dest[] = getProducerArray(m.dest.allegiance);
	for(EuphoriaCell d : dest)
	{
		WorkerChip top = (WorkerChip)d.topChip();
		if(top!=null) { knowledge += top.knowledge(); }
	}
	EuphoriaCell src = getCell(m.from_color,m.source);
	WorkerChip worker = (WorkerChip)src.topChip();
	return(knowledge+worker.knowledge());
	 
 }
 
 public int newWorkerTotal_08(EuphoriaMovespec m)
 {	int knowledge = 0;
 	EuphoriaCell dest[] = getProducerArray(m.dest.allegiance);
	for(EuphoriaCell d : dest)
	{
		WorkerChip top = (WorkerChip)d.topChip();
		if(top!=null) { knowledge += top.knowledge(); }
	}
	EuphoriaCell src = getCell(m.from_color,m.source);
	WorkerChip worker = (WorkerChip)src.chipAtIndex(m.from_row);
	return(knowledge+worker.knowledge());
	 
 }
 public double scoreAsMontecarloMove_07(EuphoriaMovespec m)
 {	EPlayer p = players[m.player];
	 switch(m.op)
	 {	
	 case MOVE_DONE:
		 switch(board_state)
		 {
		 case PlaceAnother:
			 return(0.02);		// do nothing when you could place another worker
		 default: return(1.0);
		 }
	 case MOVE_PLACE_WORKER:
		 
		 switch(m.dest)
	 	{
		 // 07 applies commodity limits to tunnel end goods
		 case SubterranTunnelEnd:	// gets food
		 case WastelanderFarm:
		 	{double weight = recruitAdvantage(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.food.height()-5)*0.3)));
		 	}
		 // 07 applies commodity limits to tunnel end goods
		 case EuphorianTunnelEnd:	// gets water
		 case SubterranAquifer:
			 {double weight = recruitAdvantage(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.water.height()-5)*0.3)));
			 }
		 // 07 applies commodity limits to tunnel end goods
		 case WastelanderTunnelEnd:	// gets power
		 case EuphorianGenerator:	
			 {double weight = recruitAdvantage(p,m);
			  return(Math.max(0.0,Math.min(weight,weight-(p.energy.height()-5)*0.3)));
			 }
		 case IcariteCloudMine:
			 {
			 double weight = recruitAdvantage(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.bliss.height()-5)*0.3)));
			 }
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore_06(m.dest);
			 if(sc<0) { return(-sc); }	// we can open the market
			 int availableWorkers = p.totalWorkers-workersOnMarket;
			 switch(availableWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(0.2*sc);
			 case 3: return(2*sc);
			 case 4: return(3*sc);
			 default:throw Error("not expecting %s",availableWorkers);
			 }
		 	}
		 
		 case IcariteSkyLounge:
		 case IcariteBreezeBar:
			 return(10.0);		// 07 incentivizes the icarite exchanges
			 
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.0);	// for random tree, this really is zero
		 default: 
			 if(board_state==EuphoriaState.RetrieveOrConfirm)
			 {
			 return(3.0);	//	boost other choices relative to "done"
			 }
			 break;
		 }
		 break;
	 case MOVE_ITEM_TO_PLAYER:
		 // deprecate taking cards if we are at the card limit
	 	{
		if(m.source == EuphoriaId.ArtifactDeck)
			{	// 07 tries to reduce choice of cards when they will be lost 
				if(p.artifacts.height()>=p.morale)
					{ return(0.01); }
			}
			return(2.0); 

	 	}
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
  public double scoreAsMontecarloMove_08(EuphoriaMovespec m)
 {	EPlayer p = players[m.player];
	 switch(m.op)
	 {	
	 case MOVE_DONE:
		 switch(board_state)
		 {
		 case PlaceAnother:
			 return(0.02);		// do nothing when you could place another worker
		 default: return(1.0);
		 }
	 case MOVE_PLACE_WORKER:
		 
		 switch(m.dest)
	 	{
		 // 07 applies commodity limits to tunnel end goods
		 case SubterranTunnelEnd:	// gets food
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
			 
		 case WastelanderFarm:
		 	{double weight = recruitAdvantage_08(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.food.height()-5)*0.3)));
		 	}
		 // 07 applies commodity limits to tunnel end goods
		 case EuphorianTunnelEnd:	// gets water
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case SubterranAquifer:
			 {double weight = recruitAdvantage_08(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.water.height()-5)*0.3)));
			 }
		 // 07 applies commodity limits to tunnel end goods
		 case WastelanderTunnelEnd:	// gets power
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case EuphorianGenerator:	
			 {double weight = recruitAdvantage_08(p,m);
			  return(Math.max(0.0,Math.min(weight,weight-(p.energy.height()-5)*0.3)));
			 }
		 case IcariteCloudMine:
			 {
			 double weight = recruitAdvantage(p,m);
			 return(Math.max(0.0,Math.min(weight,weight-(p.bliss.height()-5)*0.3)));
			 }
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore_06(m.dest);
			 if(sc<0) { return(-sc); }	// we can open the market
			 int availableWorkers = p.totalWorkers-workersOnMarket;
			 switch(availableWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(0.2*sc);
			 case 3: return(2*sc);
			 case 4: return(3*sc);
			 default: throw Error("not expecting %s",availableWorkers);
			 }
		 	}
		 
		 case IcariteSkyLounge:
		 case IcariteBreezeBar:
			 return(10.0);		// 07 incentivizes the icarite exchanges
			 
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.0);	// for random tree, this really is zero
		 default: 
			 switch(board_state)
			 {
			 case Retrieve: break;
			 case RetrieveOrConfirm: return(3.0);
			 default: return(0);
			 }
		 }
		 break;
	 case MOVE_ITEM_TO_PLAYER:
		 // deprecate taking cards if we are at the card limit
	 	{
	 	double baseline = 1.0;
	 	switch(m.source)
	 	{
	 	default: break;
	 	case ArtifactBazaar:
	 		// in ignorance is bliss, one has to pay for some artifacts
	 		switch(m.from_row) {
	 		case 3: 
	 			baseline = 2.0;
	 			break;
	 		case 1:
	 		case 2:	baseline = 1.0;
	 			break;
	 		case 0:
	 		default: baseline = 0.5;
	 		}
			//$FALL-THROUGH$
		case ArtifactDeck:
	 		if(p.artifacts.height()>=p.morale)
			{ return(0.01*baseline); 
			}
	 		break;
	 	}
	 	return baseline;
	 	}
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
  
 public double scoreAsMontecarloMove_09(EuphoriaMovespec m)
 {	EPlayer p = players[m.player];
	 switch(m.op)
	 {	
	 case MOVE_DONE:
		 switch(board_state)
		 {
		 case PlaceAnother:
			 return(0.02);		// do nothing when you could place another worker
		 default: return(1.0);
		 }
	 case MOVE_PLACE_WORKER:
		 
		
		switch(m.dest)
	 	{
		 case IcariteCloudMine:
		 case WastelanderFarm:
		 case EuphorianGenerator:	
		 case SubterranAquifer:
		 	{double weight = recruitAdvantage_08(p,m);
		 	int commod = p.totalCommodities();
		 	double penalty = (commod-5)*0.3;
		 	return(Math.max(0.0,weight-penalty));
		 	}

		 case SubterranTunnelEnd:	// gets food
		 case WastelanderTunnelEnd:	// gets power
		 case EuphorianTunnelEnd:	// gets water
		 	{
			 int commod = p.totalCommodities();
			 double penalty = (commod-5)*0.3;
			 return(Math.max(0.0,1.0-penalty));
		 	}
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore_06(m.dest);
			 if(sc<0) { return(-sc); }	// we can open the market
			 int availableWorkers = p.totalWorkers-workersOnMarket;
			 switch(availableWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(0.2*sc);
			 case 3: return(2*sc);
			 case 4: return(3*sc);
			 default: throw Error("not expecting %s",availableWorkers);
			 }
		 	}
		 
		 case IcariteSkyLounge:
		 case IcariteBreezeBar:
			 return(10.0);		// 07 incentivizes the icarite exchanges
			 
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
		 	{int commod = p.totalResources();
		 	 if((commod>=3) && (p.artifacts.height()>=p.morale)) { return 0;}
			 return(2.0);
		 	}
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.0);	// for random tree, this really is zero
		 default: 
			 switch(board_state)
			 {
			 case Retrieve: break;
			 case RetrieveOrConfirm: return(3.0);
			 default: return(0);
			 }
		 }
		 break;
	 case MOVE_ITEM_TO_PLAYER:
		 // deprecate taking cards if we are at the card limit
	 	{
	 	double baseline = 1.0;
	 	switch(m.source)
	 	{
	 	default: break;
	 	case WastelanderFarm:
	 	case SubterranAquifer:
	 	case EuphorianGenerator:
	 		baseline += (6-p.totalCommodities())*0.05;
	 		break;
	 	case ArtifactBazaar:
	 		// in ignorance is bliss, one has to pay for some artifacts
	 		switch(m.from_row) {
	 		case 3: 
	 			baseline = 2.0;
	 			break;
	 		case 1:
	 		case 2:	baseline = 1.0;
	 			break;
	 		case 0:
	 		default: baseline = 0.5;
	 		}
			//$FALL-THROUGH$
		case ArtifactDeck:
	 		if(p.artifacts.height()>=p.morale)
			{ return(0.005*baseline); 
			}
	 		break;
	 	}
	 	return baseline;
	 	}
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
  
 public double scoreAsMontecarloMove_03(EuphoriaMovespec m)
 {	 EPlayer p = players[m.player];
 	 double base = 0.0;
	 switch(m.op)
	 {	
	 case MOVE_PLACE_WORKER:
		 if(p.hasActiveRecruit(m.dest.allegiance))
		 {
			 base += 0.1;
		 }
		switch(m.dest)
	 	{
		 case WastelanderFarm:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
		 case SubterranAquifer:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case EuphorianGenerator:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case IcariteCloudMine:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.bliss.height()-5)*0.3)));
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore(m.dest);
			 switch(p.totalWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(1*sc);
			 case 3: return(2*sc);
			 case 4: return(4*sc);
			 default: throw Error("not expecting %s",p.totalWorkers);
			 }
		 	}
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.02);
		 default: break;
		 }
		 break;
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
 //
 // this version adds a bonus/penalty for retrieving from commodity sites 
 // from your allegiance.  It causes change but not in any consistent direction.
 //
 public double scoreAsMontecarloMove_04(EuphoriaMovespec m)
 {	 EPlayer p = players[m.player];
 	 double base = 0.0;
	 switch(m.op)
	 {	
	 case MOVE_PLACE_WORKER:
		 if(p.hasActiveRecruit(m.dest.allegiance))
		 {
			 base += 0.1;
		 }
		switch(m.dest)
	 	{
		 case WastelanderFarm:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
		 case SubterranAquifer:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case EuphorianGenerator:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case IcariteCloudMine:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.bliss.height()-5)*0.3)));
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore(m.dest);
			 switch(p.totalWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(1*sc);
			 case 3: return(2*sc);
			 case 4: return(4*sc);
			 default: throw Error("not expecting %s",p.totalWorkers);
			 }
		 	}
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.02);
			 
		 case WastelanderFarm:
		 case SubterranAquifer:
		 case EuphorianGenerator:
		 case IcariteCloudMine:
		 	{
		 	// add a "stay" bias to slow the allegiance advance of the other players
		 	 Allegiance a = m.source.allegiance;
			 if(allegiance[a.ordinal()] < AllegianceSteps-1)	// can still move
			 {	double bias = 1.0;
			 	for(EPlayer player : players) 
			 		{ int nActive = player.nActiveRecruits(a);
			 		  if(player==p)
			 		  {	int nHidden = player.nHiddenRecruits(a);
			 			while(nActive-- > 0) { bias *= 1.5; }
			 			while(nHidden-- > 0) { bias *= 1.25; }
			 		  }
			 		  else
			 		  {
			 			 while(nActive-- > 0) { bias *=0.75; }
			 		  }
			 		}
			 	return(bias);
			 }
		 	}
		 	break;
		 default: break;
		 }
		 break;
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
 
 public double scoreAsMontecarloMove_05(EuphoriaMovespec m)
 {	 EPlayer p = players[m.player];
 	 double base = 0.0;
	 switch(m.op)
	 {	
	 case MOVE_PLACE_WORKER:
		 if(p.hasActiveRecruit(m.dest.allegiance))
		 {
			 base += 0.1;
		 }
		switch(m.dest)
	 	{
		 case WastelanderFarm:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.food.height()-5)*0.3)));
		 case SubterranAquifer:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
		 case EuphorianGenerator:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.energy.height()-5)*0.3)));
		 case IcariteCloudMine:
			 return(base+Math.max(0.0,Math.min(1.0,1.0-(p.bliss.height()-5)*0.3)));
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
		 	{
			 double sc = neededToCompleteScore(m.dest);
			 switch(p.totalWorkers)
			 {
			 case 0:
			 case 1: return(0.1*sc);
			 case 2: return(1*sc);
			 case 3: return(2*sc);
			 case 4: return(4*sc);
			 default: throw Error("not expecting %s",p.totalWorkers);
			 }
		 	}
		 case EuphorianTunnelMouth:
		 case WastelanderTunnelMouth:
		 case SubterranTunnelMouth:
			 return(10.0);
		 case WorkerActivationA:
		 case WorkerActivationB:
			 return((4-p.totalWorkers)*15);
		 default: break;
	 	} 
	 	break;
	 case MOVE_RETRIEVE_WORKER:
		 switch(m.source)
		 {
		 case EuphorianBuildMarketA:
		 case EuphorianBuildMarketB:
		 case SubterranBuildMarketA:
		 case SubterranBuildMarketB:
		 case WastelanderBuildMarketA:
		 case WastelanderBuildMarketB:
			 return(0.01);
		 default: return(0.25);	// downgrade all the others so they score poorly relative to placement moves
		 }
	 default: break;
	 }
	 return(m.montecarloWeight);
 }
 
 CommonMoveStack  GetListOfMoves()
 {	
	 return(GetListOfMoves(players[whoseTurn],true));
 }
 CommonMoveStack  GetListOfMoves(EPlayer p,boolean err_if_none)
 {	CommonMoveStack all = new CommonMoveStack();
  	switch(board_state)
 	{
 	default:
 		throw Error("Not expecting state %s",board_state);
 	case NormalStart:
 		all.push(new EuphoriaMovespec(NORMALSTART,p));
 		break;
 	case DiscardResources:
 		addDiscardResourceMoves(all,p,null);
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		break;
 	case ActivateOneRecruit:
		addActivateRecruitMoves(all,p);
		break;
	case EphemeralConfirmRecruits:
	case ConfirmRecruits:
 	case ConfirmPlace:
 	case ConfirmPayCost:
 	case ConfirmBenefit:
 	case ConfirmFightTheOpressor:
 	case ConfirmOneRecruit:
 	case ConfirmActivateRecruit:
 	case ConfirmJoinTheEstablishment:
 	case ConfirmPayForOptionalEffect:
 	case ConfirmRecruitOption:
 	case ConfirmRetrieve:
 	case ConfirmUseJacko:
 	case ConfirmBump:
 	case Resign:
 	case DumbKoff:
 	case PayForLionel:
 	case PayForBorna:
 	case ConfirmDiscardFactionless:
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		break;
 	case PayForOptionalEffect:
 	case ConfirmUseJackoOrContinue:
 	case ConfirmUseMwicheOrContinue:
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
		//$FALL-THROUGH$
	case PayCost:
 	case JoinTheEstablishment:
 	case FightTheOpressor:
 		addPaymentMoves(all,p,pendingCost());
 		break;
 	case DieSelectOption:
 		for(EuphoriaId roll : DieRolls)
 		{
 			all.push(new EuphoriaMovespec(USE_DIE_ROLL,roll,p));
 		}
 		break;
 	case RecruitOption:
 		{
 		RecruitChip recruit = activeRecruit();
 		boolean skip = false;
 		if(robotBoard && recruit==RecruitChip.JonathanTheGambler)
 		{
 			if((p.artifacts.height()+1)>p.morale) { skip = true; } 
 		}
 		if(recruit==RecruitChip.JuliaTheAcolyte)
 		{	all.push(new EuphoriaMovespec(USE_FIRST_RECRUIT_OPTION,recruit,p)); 
 			all.push(new EuphoriaMovespec(USE_SECOND_RECRUIT_OPTION,recruit,p)); 
 		}
 		else {
 			Assert(recruit!=null,"should be a recruit");
  	 		if(!skip) { all.push(new EuphoriaMovespec(USE_RECRUIT_OPTION,recruit,p)); }		
 		}
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		}
 		break;
 	case CollectBenefit:
 	case CollectOptionalBenefit:
 	{	Benefit bene = pendingBenefit();
 		if(!hasPaidSomething())
 		{
 		boolean altruism = p.penaltyAppliesToMe(MarketChip.IIB_PalaceOfForcedAltruism);
 		if(altruism) 
 			{ //if(p.totalResources()>=3) { p1("discard altruism "+bene); }
 			  addDiscardResourceMoves(all,p,null);
 			}}
 		
 		Assert(bene!=null,"No benefit - pendingBenefit is null");
 		switch(bene)
 		{
 		case ResourceOrCommodity:
 		case IcariteInfluenceAndResourcex2:
 		case CardAndStone:
 		case CardAndClay:
 		case CardAndGold:	// these occur when registry of personal secrets (no recruit allegiance benefits) is in effect
		case CardOrClay:
		case CardOrGold:
		case Resource:
		case Resourcex2:
		case Resourcex3:
		case Resourcex4:
		case Resourcex5:
		case Resourcex6:
		case Resourcex7:
		case Resourcex8:
		case Resourcex9:

		case CardOrStone:
		case WaterOrEnergy:
		case Commodity:
		case Commodityx2:
		case Commodityx3:
			addGetResourceMoves(all,bene,p);
			break;

		case WaterOrStone:
			addSubterranGoods(all,p);
			break;
 		case EuphorianAuthority2:
 			addAuthority2Moves(all,Allegiance.Euphorian,getMarketA(Allegiance.Euphorian),getMarketB(Allegiance.Euphorian),p);
			break;
 		case WastelanderAuthority2:
 			addAuthority2Moves(all,Allegiance.Wastelander,getMarketA(Allegiance.Wastelander),getMarketB(Allegiance.Wastelander),p);
			break;
 		case SubterranAuthority2:
 			addAuthority2Moves(all,Allegiance.Subterran,getMarketA(Allegiance.Subterran),getMarketB(Allegiance.Subterran),p);
			break;

 		case EuphorianAuthorityAndInfluenceA:
			//p1("Euphorian market option a");
 			addAuthority2Moves(all,Allegiance.Euphorian,getMarketA(Allegiance.Euphorian),null,p);
			break;
 		case WastelanderAuthorityAndInfluenceA:
 			//p1("Wastelander market option a");
 			addAuthority2Moves(all,Allegiance.Wastelander,getMarketA(Allegiance.Wastelander),null,p);
			break;
 		case SubterranAuthorityAndInfluenceA:
 			//p1("Subterran market option a");
 			addAuthority2Moves(all,Allegiance.Subterran,getMarketA(Allegiance.Subterran),null,p);
			break;

 		case EuphorianAuthorityAndInfluenceB:
 			//p1("Euphorian market option b");
			addAuthority2Moves(all,Allegiance.Euphorian,null,getMarketB(Allegiance.Euphorian),p);
			break;
 		case WastelanderAuthorityAndInfluenceB:
 			//p1("Wastelander market option b");
 			addAuthority2Moves(all,Allegiance.Wastelander,null,getMarketB(Allegiance.Wastelander),p);
			break;
 		case SubterranAuthorityAndInfluenceB:
 			//p1("Subterran market option b");
 			addAuthority2Moves(all,Allegiance.Subterran,null,getMarketB(Allegiance.Subterran),p);
			break;

 		case MoraleOrKnowledge:
 		case Moralex2OrKnowledgex2:
 			for(int i=0;i<marketBasket.height();i++)
 			{
 				all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,marketBasket,i,p,p.marketBasket)); 
 			}
 			break;
 		case Artifactx2for1:
 			for(int i=0;i<marketBasket.height();i++)
 			{
 				all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,marketBasket,i,p,p.artifacts)); 
 			}
 			break;
 		case KnowledgeOrBliss:
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,bliss,p,p.bliss));
 			break;
 		case WaterOrMorale:
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
 			break;
 		case MoraleOrEnergy:
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,generator,p,p.energy));
 			break;
  		case KnowledgeOrFood:
 			 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,farm,p,p.food));
 			 break;
  		case IcariteInfluenceAndCardx2:	// this clause only gets used in IIB,
  		case Artifact:					// where taking a card requires an interaction
  		case FirstArtifact:
  		case FreeArtifact:
  		case FreeArtifactOrResource:
  			addGetResourceMoves(all,bene,p);
  			break;	
  		case ArtifactOrWaterX2:	// for joseph the antiquer
  		case ArtifactOrWaterX3:
  			if(hasTakenNothing()) { addGetArtifactMoves(all,bene,p); }
  			addGetResourceMoves(all,Benefit.Water,p);
  			break;
  		case ArtifactOrEnergyX2:
  		case ArtifactOrEnergyX3:
  			if(hasTakenNothing()) { addGetArtifactMoves(all,bene,p); }
  			addGetResourceMoves(all,Benefit.Energy,p);
  			break;
  		case ArtifactOrBlissX2:
  		case ArtifactOrBlissX3:
  			if(hasTakenNothing()) { addGetArtifactMoves(all,bene,p); }
  			addGetResourceMoves(all,Benefit.Bliss,p);
  			break;
  		case ArtifactOrFoodX2:
  		case ArtifactOrFoodX3:
  			if(hasTakenNothing()) { addGetArtifactMoves(all,bene,p); }
  			addGetResourceMoves(all,Benefit.Food,p);
  			break;
  		case IcariteAuthorityAndInfluence:
  			// here the authority and influence have already been awarded
  			// but in IIB a card is owed
  			addGetArtifactMoves(all,bene,p);
  			break;
		case StoneOrWater:
			addGetResourceMoves(all,Benefit.Water,p);
			addGetResourceMoves(all,Benefit.Stone,p);
			break;
		case GoldOrEnergy:
			addGetResourceMoves(all,Benefit.Energy,p);
			addGetResourceMoves(all,Benefit.Gold,p);
			break;
		case ClayOrFood:
			addGetResourceMoves(all,Benefit.Food,p);
			addGetResourceMoves(all,Benefit.Clay,p);
			break;
		case ResourceAndCommodity:
			addGetResourceMoves(all,Benefit.ResourceAndCommodity,p);
			break;
			
		case ArtifactOrGoldOrEnergyX2:
		case ArtifactOrGoldOrEnergyX3:
			if(hasTaken(goldMine))
				{ addGetResourceMoves(all,Benefit.Energy,p);	// must continue with energy for the rest 
				}
			else if(hasTaken(generator)) 
				{ addGetResourceMoves(all,Benefit.Gold,p);  	// can continue with gold or energy
				  addGetResourceMoves(all,Benefit.Energy,p);  
				}
			else {
				addGetResourceMoves(all,Benefit.Energy,p);
				addGetResourceMoves(all,Benefit.Gold,p);
				addGetArtifactMoves(all,bene,p);
			}
			break;

		case ArtifactOrStoneOrWaterX2:
		case ArtifactOrStoneOrWaterX3:
			if(hasTaken(quarry))
				{ addGetResourceMoves(all,Benefit.Water,p);	// must continue with water for the rest 
				}
			else if(hasTaken(aquifer)) 
				{ addGetResourceMoves(all,Benefit.Stone,p);  	// can continue with stsone or water
				  addGetResourceMoves(all,Benefit.Water,p);  
				}
			else {
				addGetResourceMoves(all,Benefit.Water,p);
				addGetResourceMoves(all,Benefit.Stone,p);
				addGetArtifactMoves(all,bene,p);
			}
			break;

		case ArtifactOrClayOrFoodX2:
		case ArtifactOrClayOrFoodX3:
			if(hasTaken(clayPit))
				{ addGetResourceMoves(all,Benefit.Food,p);	// must continue with food for the rest 
				}
			else if(hasTaken(farm)) 
				{ addGetResourceMoves(all,Benefit.Clay,p);  	// can continue with clay or food
				  addGetResourceMoves(all,Benefit.Food,p);  
				}
			else {
				addGetResourceMoves(all,Benefit.Food,p);
				addGetResourceMoves(all,Benefit.Clay,p);
				addGetArtifactMoves(all,bene,p);
			}
			break;

 		default: throw Error("Not expecting pending benefit %s in getListOfMoves()",bene);
 		}
 		}
 		break;
 	case ChooseOneRecruit:
 		addSingleRecruitChoiseMoves(all,p);
 		break;
 	case ChooseRecruits:
 	case EphemeralChooseRecruits:
 		addRecruitChoiceMoves(all,p);
 		break;
 	case DiscardFactionless:
 	case EphemeralDiscardFactionless:
 		addRecruitDiscardMoves(all,p);
 		break;
 	case RetrieveCommodityWorkers:
 	case RetrieveOrConfirm:
 	case Retrieve1OrConfirm:
		checkUnusualTrades(all,p);
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		addWorkerRetrievalMoves(all,p,!robotBoard);
 		break;
  	case Retrieve:
		checkUnusualTrades(all,p);
		addRowenaMoves(all,p);
		addChagaMoves(all,p);
 		addWorkerRetrievalMoves(all,p,!robotBoard);
 		if(all.size()==0)
 		{
 			// this really shouldn't happen, but just in case
 			addWorkerRetrievalMoves(all,p,true);
 		}
 		break;
 	case PlaceOrRetrieve:
 		if(variation==Variation.Euphoria3T)
 		{
 			if((p.activeRecruits.height()<5)
 					&& unusedRecruits.height()>1)
 			{
 				all.push(new EuphoriaMovespec(MOVE_RECRUIT,p));
 			}
 		}
 		checkUnusualTrades(all,p);
 		addWorkerPlacementMoves(all,p,doublesElgible);
 		if(!robotBoard || (all.size()==0)) 
 			{ 
 			addWorkerRetrievalMoves(all,p,!robotBoard);
 			}
 		break;
 	case ReUseWorker:	// lars the repeater, move last worker to anywhere
		checkUnusualTrades(all,p);
 		addLarsTheBallooneerMoves(all,p,lastDroppedWorker);
 		break;
 	case BumpOpponent:	// retrieve for jedidiah the inciter
 		addBumpOpponentMoves(all,p,lastDroppedWorker);
 		break;
 	case RePlace:	// darren the repeater, can only place 1 place
		checkUnusualTrades(all,p);
 		addWorkerPlacementMovesAt(all,p,lastDroppedWorker);
 		if(robotBoard && (all.size()==0))
 		{	// this can happen if the robot spends a food to ignore a market penalty
 			// before placing the worker
 			// p1("dumb robot behavior in re-place");
 			all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		}
 		break;
 	case Place:
 	case PlaceNew:
		checkUnusualTrades(all,p);
 		addWorkerPlacementMoves(all,p,doublesElgible);
 		break;
 	case PlaceAnother:
		checkUnusualTrades(all,p);
		if((robot!=null) && (doublesElgible!=null) && loseMoraleLosesArtifact())
		{	//p1("preemptive lose morale");
			all.push(new EuphoriaMovespec(MOVE_LOSEMORALE,p));
		}
		else
		{
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		addWorkerPlacementMoves(all,p,doublesElgible);
		}

 	}
 	if(board_state.isInitialWorkerState())
 	{
	 	addDilemmaMoves(all,p);
 	}
 	if(err_if_none && (all.size()==0))
 	{
 		//throw G.Error("No moves generated for state %s for player %s",board_state,p);
 		//GetListOfMoves();
 	}
 	if(robotBoard) { Assert(all.size()>0,"x produced no moves for state %s",board_state); }
 	return(all);
 }
 
//
// animation assistants
//
 	void animateTrash(EuphoriaCell c) { animationStack.push(c); animationStack.push(trash); }
	void animateNewFood(EuphoriaCell c) { animationStack.push(farm); animationStack.push(c); }
 	void animateNewWater(EuphoriaCell c) { animationStack.push(aquifer); animationStack.push(c); }
 	void animateNewEnergy(EuphoriaCell c) { animationStack.push(generator); animationStack.push(c); }
 	void animateNewWorkerA(EuphoriaCell c) { animationStack.push(workerActivationA); animationStack.push(c); }
 	void animateNewWorkerB(EuphoriaCell c) { animationStack.push(workerActivationB); animationStack.push(c); }
 	void animateNewBliss(EuphoriaCell c) { animationStack.push(bliss); animationStack.push(c); }
	void animateNewGold(EuphoriaCell c) { animationStack.push(goldMine); animationStack.push(c); }
	void animateNewStone(EuphoriaCell c) { animationStack.push(quarry); animationStack.push(c); }
	void animateNewClay(EuphoriaCell c) { animationStack.push(clayPit); animationStack.push(c); }
	void animateNewArtifact(EuphoriaCell c) { animationStack.push(unusedArtifacts); animationStack.push(c); }
	
	void animateReturnArtifact(EuphoriaCell c) { animationStack.push(c); animationStack.push(usedArtifacts); }
	void animateReturnFood(EuphoriaCell c) { animationStack.push(c); animationStack.push(farm); }
	void animateReturnBliss(EuphoriaCell c) { animationStack.push(c); animationStack.push(bliss); }
	void animateReturnStone(EuphoriaCell c) { animationStack.push(c); animationStack.push(quarry); }
	void animateReturnGold(EuphoriaCell c) { animationStack.push(c); animationStack.push(goldMine); }
	void animateReturnClay(EuphoriaCell c) { animationStack.push(c); animationStack.push(clayPit); }
	void animateReturnWater(EuphoriaCell c) { animationStack.push(c); animationStack.push(aquifer); }
	void animateReturnEnergy(EuphoriaCell c) { animationStack.push(c); animationStack.push(generator); }
	void animateSacrificeWorker(EuphoriaCell c,WorkerChip m)
		{ EuphoriaCell dest = unusedWorkers[getPlayer(m.color).boardIndex];
		  animationStack.push(c);
		  dest.copyCurrentCenter(isIIB() ? trash : workerActivationB);
		  dest.removeTop();
		  dest.addChip(m);
		  animationStack.push(dest);
		}
	void animatePlacedItem(EuphoriaCell c,EuphoriaCell d) { animationStack.push(c); animationStack.push(d); }
	void logGameEvent(String str,String... args)
	{	if(!robotBoard)
		{String trans = s.get(str,args);
		 gameEvents.push(trans);
		}
	
	}
	void logGameExplanation(String str,String...args)
	{
		if(!robotBoard)
		{String trans = s.get(str,args);
		 gameEvents.push("E: "+trans);
		}
	}
	String currentPlayerColor()
	{
		return(playerColor(whoseTurn));
	}
	String playerColor(int n)
	{
		return(players[n].getPlayerColor());
	}
	
	public boolean revealedNewInformation()
	{
		return(revealedNewInformation);
	}
	
	public boolean hasPeeked(int ind) { return(players[ind].hasPeeked()); } 
	
	public void setHasPeeked(int ind,boolean v)
	{
		EPlayer p = players[ind];
		p.setHasPeeked(v);
	}
	public boolean p1(String msg)
	{
		if(G.p1(msg) && (robot!=null))
		{	String dir = "g:/share/projects/boardspace-html/htdocs/euphoria/euphoriagames/robot/";
			robot.saveCurrentVariation(dir+msg+".sgf");
			return(true);
		}
		return(false);
	}
	public ErrorX Error(String msg,Object...args)
	{	if(args.length>0) { msg = G.format(msg,args); }
		p1(msg);
		return G.Error(msg);
	}
	public boolean Assert(boolean condition,String msg,Object...args)
	{	if(!condition)
		{	if(args.length>0) { msg = G.format(msg,args); }
			p1(msg);
			G.Assert(condition,msg);
		}
		return(true);
	}
	public void useRecruit(RecruitChip ch,String context)
	{	switch(ch.chipNumber()) 
		{
		// recruits before iib
		case 101:
		case 102:
		case 106:
		case 107:
		case 108:
		case 109:
		case 110:
		case 112:
		case 117:
		case 118:
		case 120:
		case 121:
		case 122:
		case 127:	
		case 129:
			
		case 130:
		case 131:
		case 132:
		case 133:
		case 135:
		case 136:
		case 139:
		case 143:
		case 145:
		case 146:
		case 147:
		case 201:
		case 203:
		case 204:
		case 205:
		case 207:
		case 208:
		case 209:
		case 210:
		case 211:
		case 213:
		case 214:
		case 215:
			break;
		default: p1("use "+ch.name+"#"+ch.chipNumber()+" "+context);
			break;
		case 221: // lieve the briber, pay bliss to get both resources from tunnels
		case 222: // ahmed the artifact dealer, discount on artifact purchase
		case 223: // amina the bliss bringer, better treatment with knowledge check
		case 224: // teri the bliss trader, trade bliss for commodity when you gain star
		case 225: // ted the contingency planner, use bliss at tunnels and maybe gain morale
		case 226: // gary the forgetter, use bliss when training workers and lose extra knowledge
		case 227: // bok the gamemaster, limit knowledge to 4
		case 228: // keb the information trader, gain something before knowledge check
		case 229: // gain extra knowledge and card
			
		case 230: // mosi the patron, use 2 bliss in place of 2 artifacts in artifact market
		case 231: // jadwiga the sleep deprivator, pay 2 knowledge to take another action
		case 232: // zara the solipsist, double knowledge on commodity areas
		case 233: // jon the amateur handyman, use commodities instead of construction cost
		case 234: // doug the builder, sacrifice worker at construction site
		case 235: // cary thye care bear, free stuff when you get a bear
		case 236: // ekaternia the cheater, use box as wildcard
		case 237: // miroslav the con artist, use bear as wildcard
		case 238: // steve the double agent, get * for other factions
		case 239: // pmai the nurse, decrement knowledge for commodity placement
			
		case 240: // chaga the gamer, activate another recruit
		case 241: // ha-joon the gold trader, trade gold for free artifact
		case 242: // rowena the mentor, gain knowlede to ignore market penalties
		case 243: // frazer the motivator, take any commodity in place of 1 energy
		case 244: // samuel the zapper, pay energy to retrieve from commodity areas
		case 245: // lars the ballooneer, use a worker again
		case 246: // xyon the brain surgeon, rescue workers for artifact
		case 247: // julia the acolyte, control knowledge when being bumped
		case 248: // taed the brick trader, trade brick for resource and water
		case 249: // lionel the cook, ignore 1 market penalty
			
		case 250: // alexander the heister, use balloons anywhere
		case 251: // george the lazy craftsman, bump worker back and gain commodity
		case 252: // gwen the minerologist, get a resource instead from commodity area
		case 253: // jose the persuader, use bat as wildcard
		case 254: // jedidiah the inciter, bump a worker from commodity area
		case 255: // darren the repeater, bump the previous guy]
		case 256: // high general baron, allows 1 time reroll
		case 257: // joseph the antiquer, get artifact instead of commodity
		case 258: // milos the brainwasher, pay water to skip knowledge check
		case 259: // khaleef the bruiser, gain commodity when bumping other worker
			
		case 260: // pedro the collector, gain resource and commodity when paying 3 artifacts
		case 261: // shaheena the digger, pay stone for artifact
		case 262: // dusty the enforcer, saves a worker with a bat and morale\
		case 263: // mwiche the flusher, use water at tunnels
		case 264: // makato the forger, use bifocals as wildcard
		case 265: // albert the founder, gain an extra worker and stone
		case 266: // pamhidzi the reader, gain 3 commodity instead of book or bifocals
		case 267: // borna the storyteller, trade book or bifocals for resoruce or free card
		case 268: // javier the underground librarian, use book as wildcard
		case 269: // christine the anarchist, gain commodity when retrieving
			
		case 270: // kofi the hermit, uses one worker, gets discarded
		case 271: // jeroen the hoarder, gain resource when you retrieve 3 or 4
		case 272: // spiros the model citizen, pay morale to place another worker
		case 273: // davaa the shredder, use tunnels for free
		case 274: // youssef the tunneler, get both tunnel benefits
		}
	}

	public boolean loseMoraleLosesArtifact() {
		EPlayer p = getCurrentPlayer();
		return (!p.testTFlag(TFlag.HasLostMorale) && (p.morale<=p.artifacts.height()));
	}
}
