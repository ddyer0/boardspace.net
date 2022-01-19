package nuphoria;


import static nuphoria.EuphoriaMovespec.*;

import java.util.*;

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
{	static int REVISION = 122;			// revision numbers start at 100


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
	
	public int getMaxRevisionLevel() { return(REVISION); }
	
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
		private int pendingPlayer = 0;							// the current player when starting the continuation
		private int pendingUnpaidPlayer = 0;					// the current player when taking the "unpaid" continuation
		private Function pendingSecondContinuation = null;		// 
		private EuphoriaCell pendingContinuationCell = null;	// cell whose cost we haven't paid
		EuphoriaCell pendingContinuationCell()
		{
     		G.Assert(pendingContinuationCell!=null,"contuation cell shouldn't be null");
     		return(pendingContinuationCell);
		}
		Colors pendingColor = null;
		private int proceedStep = 0;
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
		{	exitState = board_state;
			proceedStep = proceedGameStep;
			pendingPlayer = pendingUnpaidPlayer = whoseTurn;
		}
		// constructor for mandatory (eventual) continuation
		private Continuation(EuphoriaCell dest,Function continuation)
		{
			this();
			pendingContinuationCell = dest;
			pendingState = EuphoriaState.ExtendedBenefit;;
			pendingUnpaidContinuation = pendingContinuation = continuation;
		}

		// constructor for intermodal function
		private Continuation(Function continuation,Colors pl,Function nextContinuation)
		{
			this();
			G.Assert(continuation!=Function.DropWorkerAfterBump,"not bump");
			G.Assert(nextContinuation!=Function.DropWorkerAfterBump,"not bump");
			pendingState = EuphoriaState.ExtendedBenefit;
			pendingColor = pl;
			pendingUnpaidContinuation = pendingContinuation = continuation;
			pendingSecondContinuation = nextContinuation;
			
		}
		// constructor for recruit option state
		private Continuation(RecruitChip recruit,Function paidContinuation,Function unpaidContinuation)
		{	this();
			G.Assert(unpaidContinuation!=Function.DropWorkerAfterBump,"not bump");
			G.Assert(paidContinuation!=Function.DropWorkerAfterBump,"not bump");
			activeRecruit = recruit;
			pendingContinuation = paidContinuation;
			pendingUnpaidContinuation = unpaidContinuation;
			pendingState = recruit.optionState();
		}
		// constructor for recruit option state
		private Continuation(RecruitChip recruit,Function paidContinuation,int pl,Function unpaidContinuation)
		{	this();
			G.Assert(unpaidContinuation!=Function.DropWorkerAfterBump,"not bump");
			G.Assert(paidContinuation!=Function.DropWorkerAfterBump,"not bump");
			pendingPlayer = pl;
			activeRecruit = recruit;
			pendingContinuation = paidContinuation;
			pendingUnpaidContinuation = unpaidContinuation;
			pendingState = recruit.optionState();	
		}
		// constructor for recruit option state
		private Continuation(RecruitChip recruit,EuphoriaCell dest,Function paidContinuation,Function unpaidContinuation)
		{	this();
			activeRecruit = recruit;
			pendingContinuation = paidContinuation;
			pendingUnpaidContinuation = unpaidContinuation;
			pendingState = recruit.optionState();	
     		G.Assert(dest!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = dest;
		}
		// constructor for recruit option state
		private Continuation(RecruitChip recruit,EuphoriaCell dest,Function paidContinuation,int pl,Function unpaidContinuation)
		{	this();
			activeRecruit = recruit;
			pendingPlayer = pl;
			pendingContinuation = paidContinuation;
			pendingUnpaidContinuation = unpaidContinuation;
			pendingState = recruit.optionState();	
     		G.Assert(dest!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = dest;
		}
		// constructor for mandatory payment
		private Continuation(Cost cost,EuphoriaState state,Function cont)
		{	this();
			G.Assert(cont!=Function.DropWorkerAfterBump,"not bump");
			originalPendingCost = pendingCost = cost;
			pendingState = state;
			pendingContinuation = cont; 
		}

		// constructor for mandatory payment
		private Continuation(Cost cost,EuphoriaState state,EuphoriaCell dest,Function cont)
		{	this();
			originalPendingCost = pendingCost = cost;
			pendingState = state;
     		G.Assert(dest!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = dest;
			pendingContinuation = cont; 
		}
		// constructor for mandatory payment
		private Continuation(Cost cost,EuphoriaState state,EuphoriaCell dest,RecruitChip active,Function cont)
		{	this();
			originalPendingCost = pendingCost = cost;
			activeRecruit = active;
			pendingState = state;
     		G.Assert(dest!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = dest;
			pendingContinuation = cont; 
		}

		
		// alternate constructor for mandatory payment
		private Continuation(Cost original,Cost cost,EuphoriaState state,EuphoriaCell cell,Function cont)
		{	this();
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = state;
     		G.Assert(cell!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = cell;
			pendingContinuation = cont; 
		}
		// alternate constructor for mandatory payment
		private Continuation(Cost original,Cost cost,EuphoriaState state,EuphoriaCell cell,Function unpaid,Function cont)
		{	this();
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = state;
     		G.Assert(cell!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = cell;
			pendingContinuation = cont; 
			pendingUnpaidContinuation = unpaid;
		}
		// alternate constructor for mandatory payment
		private Continuation(Cost original,Cost cost,EuphoriaState state,Function cont)
		{	this();
			G.Assert(cont!=Function.DropWorkerAfterBump,"not bump");
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = state;
			pendingContinuation = cont; 
		}
		// constructor for optional payment
		private Continuation(Cost original,Cost cost,EuphoriaState state,Function paid,Function unpaid)
		{	this();
			G.Assert(paid!=Function.DropWorkerAfterBump,"not bump");
			G.Assert(unpaid!=Function.DropWorkerAfterBump,"not bump");
			originalPendingCost = original;
			pendingCost = cost;
			pendingState = state;
			pendingContinuation = paid; 
			pendingUnpaidContinuation = unpaid;
		}	
		// constructor for collect benefit
		private Continuation(Benefit bene,EuphoriaState state,EuphoriaCell cell,Function cont)
		{	this();
			pendingBenefit = bene;
			pendingState = state;
     		G.Assert(cell!=null,"contuation cell shouldn't be null");
			pendingContinuationCell = cell;
			pendingContinuation = cont;
		}
		// alternate constructor for collect benefit
		private Continuation(Benefit bene,EuphoriaState state,Function cont)
		{	this();
			G.Assert(state!=null,"state must not be null");
			pendingBenefit = bene;
			pendingState = state;
			pendingContinuation = cont;
		}
		// digest for Continuations
		public long Digest(Random r)
		{	long v = 0;
			v ^= EuphoriaCell.Digest(r,pendingContinuationCell);
			v ^= EuphoriaChip.Digest(r,activeRecruit);
			v ^= r.nextLong()*(pendingState.ordinal()+1);
			v ^= r.nextLong()*(exitState.ordinal()+1);
			v ^= r.nextLong()*(proceedStep+1);
			v ^= r.nextLong()*(pendingPlayer+1);
			v ^= r.nextLong()*(pendingUnpaidPlayer+1);
			v ^= r.nextLong()*((pendingContinuation!=null)?(pendingContinuation.ordinal()+1):0);
			v ^= r.nextLong()*((pendingUnpaidContinuation!=null)?(pendingUnpaidContinuation.ordinal()+1):0);
			v ^= r.nextLong()*((pendingCost!=null)?(pendingCost.ordinal()+1):0);
			v ^= r.nextLong()*((originalPendingCost!=null)?(originalPendingCost.ordinal()+1):0);
			v ^= r.nextLong()*((pendingBenefit!=null)?(pendingBenefit.ordinal()+1):0);
			return(v);
		}
		public void sameAs(Continuation other)
		{	G.Assert(pendingState==other.pendingState,"pending state mismatch");
			G.Assert(exitState==other.exitState,"exit state mismatch");
			G.Assert(proceedStep==other.proceedStep,"proceedStep mismatch");
			G.Assert(pendingPlayer==other.pendingPlayer,"pendingPlayer mismatch");
			G.Assert(pendingUnpaidPlayer==other.pendingUnpaidPlayer,"pendingUnpaidPlayer mismatch");
	        G.Assert(pendingBenefit==other.pendingBenefit,"pending benefit mismatch");
	        G.Assert(activeRecruit==other.activeRecruit,"active recruit matches");
	        G.Assert(pendingCost==other.pendingCost,"pending cost mismatch");
	        G.Assert(originalPendingCost==other.originalPendingCost,"original pending cost mismatch");
	        G.Assert(pendingUnpaidContinuation==other.pendingUnpaidContinuation,"unpaid continuation mismatch");
	        G.Assert(pendingContinuation==other.pendingContinuation,"continuation mismatch");
	        G.Assert(sameCells(pendingContinuationCell,other.pendingContinuationCell),"pending cost mismatch");
	
		}
		public Continuation clone(EuphoriaBoard b)
		{	Continuation copy = new Continuation();
			copy.activeRecruit = activeRecruit;
			copy.pendingBenefit = pendingBenefit;
			copy.pendingContinuationCell = b.getCell(pendingContinuationCell);
			copy.pendingCost = pendingCost;
			copy.originalPendingCost = originalPendingCost;
			copy.pendingContinuation = pendingContinuation;
			copy.pendingUnpaidContinuation = pendingUnpaidContinuation;
			copy.pendingState = pendingState;
			copy.exitState = exitState;
			copy.proceedStep = proceedStep;
			copy.pendingPlayer = pendingPlayer;
			copy.pendingUnpaidPlayer = pendingUnpaidPlayer;
			copy.pendingColor = pendingColor;
			copy.pendingSecondContinuation = pendingSecondContinuation;
			sameAs(copy);
			return(copy);
			
			
		}
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
    	acceptPlacement();
		if(cc.activeRecruit!=null)
			{G.Assert(!players[whoseTurn].penaltyAppliesToMe(MarketChip.AcademyOfMandatoryEquality),
				"shouldnt use recruits");
			}
		if((cc.pendingContinuation!=Function.Return)&&(cc.pendingContinuation!=Function.DoBenTheLudologist))
			{for(int lim=continuationStack.size()-1; lim>=0; lim--)
		{
			Continuation cont = continuationStack.elementAt(lim);
			G.Assert(cont.pendingContinuation!=cc.pendingContinuation,"not the same");
		}}
    	continuationStack.push(cc);
    	setState(cc.pendingState);
    }
    // set the continuation and switch players
    public void setContinuation(Continuation cc,int pl)
    {	setWhoseTurn(pl);
    	setContinuation(cc);
    }
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
     		bumpWorkerJuliaTheThoughtInspector_V2(cont.pendingContinuationCell(),replay);
     		break;
     	case DoJuliaTheThoughtInspector:
     		doJuliaTheThoughtInspector(replay);
     		break;
     	case DoJuliaTheThoughtInspector_V2:
     		doJuliaTheThoughtInspector_V2(replay);
     		break;
     	case DropWorkerMaximeTheAmbassador:
     		dropWorkerMaximeTheAmbassador(replay);
     		break;
     	case DropWorkerLeeTheGossip:
     		dropWorkerLeeTheGossip(replay);
     		break;
     	case DoLeeTheGossip:
     		doLeeTheGossip(replay);
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
     		doPeteTheCannibalSacrifice(replay);
     		break;
     	case ReRollSheppardTheLobotomist:
     		reRollSheppardTheLobotomist(replay);
     		break;
     	case DropWorkerPhilTheSpy:
     		dropWorkerPhilTheSpy(replay);
     		break;
     	case DoPhilTheSpy:
     		doPhilTheSpy(replay,cont.activeRecruit);
     		break;
     	case DoSheppardTheLobotomistMorale:
     		doSheppardTheLobotomistMorale(cont.pendingContinuationCell(),replay);
     		break;
     	case DoSheppardTheLobotomistGainCard:
     		doSheppardTheLobotomistGainCard(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerJuliaTheThoughtInspector:
     		dropWorkerJuliaTheThoughtInspector(replay);
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
     		dropWorkerAfterMorale(cont.pendingContinuationCell(),replay);
     		break;
      	case DropWorkerKyleTheScavenger:
     		dropWorkerKyleTheScavenger(cont.pendingContinuationCell(),replay);
     		break;
     	case DropWorkerRebeccaThePeddler:
     		dropWorkerRebeccaThePeddler(replay);
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
     		doRebeccaThePeddler(replay);
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
     		doYordyTheDemotivator_V2(replay);
     		break;
     	case ReRollYordyCheck_V2:
     		reRollYordyCheck_V2(replay);
     		break;
     	case DropWorkerScarbyTheHarvester:
    		dropWorkerScarbyTheHarvester(cont.pendingContinuationCell(),replay);
     		break;
     	case ReRoll:
     		reRoll(replay);
     		break;
     	case MoraleCheck:
     		doMoraleCheck(players[whoseTurn],cont.pendingContinuationCell,replay);	// pendingContinuationCell may be null, it's ok 
     		break;
     	case DoGeekTheOracle:
     		doGeekTheOracle(replay,cont.activeRecruit);
     		break;
     	case DoJonathanTheGambler:
     		// this one can have a null continuation
     		doJonathanTheGambler(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case ReRollNormalPayment:
     		reRollNormalPayment(replay);
     		break;
     	case DoEsmeTheFireman:
     		doEsmeTheFireman(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case DoEsmeTheFiremanPaid:
     		doEsmeTheFiremanPaid(cont.pendingContinuationCell,cont.activeRecruit,replay);
     		break;
     	case DoGidgitTheHypnotist:
     		doGidgitTheHypnotist(replay);
     		break;
      	case ReRollWithoutPayment:
     		reRollWithoutPayment(replay);
     		break;
     	case ReRollWithPayment:
     		reRollWithPayment(replay);
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
     		dropWorkerAfterBenefit(cont.pendingContinuationCell,replay);
     		break;
     	case DoMaximeTheAmbassador:		// lose morale and gain a card
     		doMaximeTheAmbassador(replay);
     		break;
     	case DoMaximeTheAmbassadorGainCard:
     		doMaximeTheAmbassadorGainCard(replay);
     		break;
     	case DoBrettTheLockPicker:
     		doBrettTheLockPicker(cont.pendingContinuationCell,replay,cont.activeRecruit,cont.pendingUnpaidContinuation);
     		break;
     	case DoBenTheLudologist:
     		doBenTheLudologist(cont.pendingContinuationCell,replay);
     		break;
     	case DoKyleTheScavenger:
     		doKyleTheScavenger(cont.pendingContinuationCell,replay);
     		break;
     		
     	case DropWorkerOpenMarkets:	
     		dropWorkerOpenMarkets(replay);
     		break;
     		
     	// ethical dilemma
     	case JoinTheEstablishment:
     		setState(EuphoriaState.ConfirmJoinTheEstablishment);
     		stepNumber++;
     		doDone(replay);
     		break;
    	case FightTheOpressor:
     		setState(EuphoriaState.ConfirmFightTheOpressor);
     		stepNumber++;
     		doDone(replay);
     		break;
     		
    	case DoZongTheAstronomer_V2:
    		doZongTheAstronomer_V2(cont.pendingContinuationCell,replay);
    		break;

    	default: throw G.Error("Not expecting continuation %s",cc);
     	}
     }

    private void doUnpaidContinuation(replayMode replay)
    {	do {
    	Continuation cc = continuationStack.pop();
    	G.Assert(cc.pendingUnpaidContinuation!=null,"unpaid continuation is specified");
    	executeContinuation(cc,cc.pendingUnpaidContinuation,cc.pendingUnpaidPlayer,replay);
    	} while(board_state==EuphoriaState.ExtendedBenefit);
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
    	G.Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.pendingBenefit);
    }
    boolean hasPendingBenefit()
    {
    	return((continuationStack.size()>0) && (continuationStack.top().pendingBenefit!=null));
    }
    public RecruitChip activeRecruit()
    {
    	G.Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.activeRecruit);  	
    }
    Cost originalPendingCost()
    {
    	G.Assert(continuationStack.size()>0,"no continuation pending");
    	Continuation cc = continuationStack.top();
    	return(cc.originalPendingCost);
    }
    Cost pendingCost()
    {
    	G.Assert(continuationStack.size()>0,"no continuation pending");
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
		G.Assert(!finalPath.contains(str),"got to "+str+" a second time");
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
		throw G.Error("No player uses %s",c);
		}
	
	EuphoriaChip getArtifact()
	{
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

	/**
	 * tunnels, per allegiance.  We ignore that there is no icarite tunnel
	 */
    private int tunnelPosition[] = new int[Allegiance.values().length];
    
    private EuphoriaCell tunnelSteps[][] = {euphorianTunnelSteps,subterranTunnelSteps,wastelanderTunnelSteps};
    
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
    // return true if actually incremented
    public boolean incrementAllegiance(Allegiance faction,replayMode replay)
    {	int ord = faction.ordinal();
    	if(allegiance[ord] < AllegianceSteps-1) 
    		{ 
    		allegiance[ord]++;
    		if(allegiance[ord]==(AllegianceSteps-1)) { awardAllegianceStars(faction,replay); }
    		return(true); 
    		}
    	return(false);
    }
    // return true if actually incremented
    public boolean decrementAllegiance(Allegiance faction)
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
	boolean maggieTheOutlawAvailable = false;		// true if maggie should be activated
	boolean usedChaseTheMiner = false;				// true if we use the top die immediately
    private int proceedGameStep = 0;				// sub-state of the turn windup.

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

// this is required even though it is meaningless for Hex, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();
	StringStack gameEvents = new StringStack();
	
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public EuphoriaChip pickedObject = null;
    public EuphoriaChip lastPicked = null;

    CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
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
     
   
    public EuphoriaChip lastDroppedObject = null;	// for image adjustment logic

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
    	
    	default: throw G.Error("not expecting %s",id);
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

    public RecruitChip getRandomRecruit(Random r)
    {	RecruitChip masterlist[] = (variation==Variation.Euphoria) 
    				? RecruitChip.allRecruits
    				: RecruitChip.V2Recruits;  
    	int n = Random.nextInt(r,masterlist.length-RecruitChip.FIRST_RECRUIT);
    	return(masterlist[RecruitChip.FIRST_RECRUIT+n]);
    }
    public void getAllRecruits(EuphoriaCell c)
    { 	RecruitChip masterlist[] = (variation==Variation.Euphoria) 
    				? RecruitChip.allRecruits
    				: RecruitChip.V2Recruits;  
    	c.reInit();
    	for(int i=RecruitChip.FIRST_RECRUIT;i<masterlist.length;i++)
    	{	c.addChip(masterlist[i]);
    	}
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int play,int rev)
    {	
    	adjustRevision(rev);
    	allCells.setDigestChain(new Random(0x6235366));
    	win = new boolean[play];
    	players_in_game = play;
     	Random gameRandom = new Random(key);
    	SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
    	
		Variation v = Variation.find(gtype);
		G.Assert(v!=null,WrongInitError,gtype);
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
    	for(int i=1;i<MarketChip.allMarkets.length;i++)
    	{	unusedMarkets.addChip(MarketChip.allMarkets[i]);
    	}	
    	unusedMarkets.shuffle(gameRandom);
    	
  	
    	// create the recruit deck and shuffle it
    	getAllRecruits(unusedRecruits);
     	unusedRecruits.shuffle(gameRandom);
 	
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
       	
       	// add some display chips to the pool
       	for(int i=0;i<5;i++) 
       	{	clayPit.addChip(EuphoriaChip.Clay);
       		quarry.addChip(EuphoriaChip.Stone);
       		goldMine.addChip(EuphoriaChip.Gold);
       		bliss.addChip(EuphoriaChip.Bliss);
       		farm.addChip(EuphoriaChip.Food);
       		generator.addChip(EuphoriaChip.Energy);
       		aquifer.addChip(EuphoriaChip.Water);
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
    	AR.setValue(tunnelPosition,0);
    	// place the markets.
    	for(int i=0;i<markets.length;i++)
    	{	EuphoriaCell m = markets[i];
    		m.addChip(unusedMarkets.removeTop());
    		m.addChip(MarketChip.CardBack);		// cover it with the back
    	}
	
      	
    	for(int i=players.length;i<MAX_PLAYERS;i++)
    	{
    		for(Allegiance a : Allegiance.values())
    		{
    			EuphoriaCell c = getAvailableAuthorityCell(a);
    			placeAuthorityToken(c,EuphoriaChip.AuthorityBlocker);
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
	    doublesCount = 0;
	    maggieTheOutlawAvailable = false;
	    usedChaseTheMiner = false;
	    proceedGameStep = 0;
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
        revealedNewInformation = false;
        currentPlayerInTurnOrder = -1;
         openedAMarket = false;		// ephemeral state
    	 shuffledADeck = false;
    	 openedAMarketLastTurn = false;
    	 shuffledADeckLastTurn = false;
    	 setStatusDisplays();		// start with the displays set up
        // note that firstPlayer is NOT initialized here
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
        	{ G.Assert(c.sameCell(d),"cell mismatch for %s",c.rackLocation()); }
        G.Assert(sameCells(usedRecruits,from_b.usedRecruits),"used recruits mismatch");
        G.Assert(sameCells(unusedRecruits,from_b.unusedRecruits),"unused recruits mismatch");
        G.Assert(activePlayer==from_b.activePlayer,"activePlayer mismatch");
        G.Assert(variation==from_b.variation,"variation mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(hasReducedRecruits == from_b.hasReducedRecruits,"reduced recruits mismatch");
        G.Assert(normalStartSeen == from_b.normalStartSeen,"normalStartSeen mismatch");
        G.Assert(AR.sameArrayContents(allegiance,from_b.allegiance),"allegiance mismatch");
        G.Assert(AR.sameArrayContents(tunnelPosition,from_b.tunnelPosition),"tunnelPosition mismatch");
        for(int i=0;i<players.length;i++) 
        	{  EPlayer.sameBoard(players[i],from_b.players[i]);
        	}
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSource mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDest mismatch");
        G.Assert(sameContents(droppedStateStack,from_b.droppedStateStack),"dropped state mismatch");
        G.Assert(sameContents(pickedStateStack,from_b.pickedStateStack),"picked state mismatch");
        G.Assert(sameContents(reRollPlayers,from_b.reRollPlayers),"rerollplayers mismatch");
        G.Assert(sameContents(pickedHeightStack,from_b.pickedHeightStack),"picked height stack matches %s and %s",pickedHeightStack,from_b.pickedHeightStack);
        G.Assert(doublesElgible==from_b.doublesElgible,"elgible for doubles mismatch");
        G.Assert(usingDoubles==from_b.usingDoubles,"using doubles mismatch");
        G.Assert(doublesCount==from_b.doublesCount,"count for doubles mismatch");
        G.Assert(maggieTheOutlawAvailable==from_b.maggieTheOutlawAvailable,"maggieTheOutlawAvailable mismatch");
        G.Assert(usedChaseTheMiner==from_b.usedChaseTheMiner,"usedChaseTheMiner mismatch");
        G.Assert(proceedGameStep==from_b.proceedGameStep,"proceedGameStep mismatch");
        G.Assert(bumpedWorker==from_b.bumpedWorker,"bumpedWorkerMismatch");
        G.Assert(currentPlayerInTurnOrder==from_b.currentPlayerInTurnOrder,"currentPlayerInTurnOrder mismatch");
        G.Assert(bumpingWorker==from_b.bumpingWorker,"bumpingWorkerMismatch");
        G.Assert(selectedDieRoll==from_b.selectedDieRoll,"selectedDieRoll mismatch");
        G.Assert(activeRecruit==from_b.activeRecruit,"activeRecruit mismatch");
        G.Assert(stepNumber == from_b.stepNumber,"stepnumber matches");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        long v1 = Digest();
        long v2 = from_b.Digest();
        G.Assert(v1==v2,"Digest matches");
 
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
        long v = super.Digest();		// this digests allcells
        if(board_state==EuphoriaState.Puzzle)
        {
        v ^= usedRecruits.Digest();
        v ^= unusedMarkets.Digest();
        v ^= unusedDilemmas.Digest();       
        v ^= unusedRecruits.Digest();

        }

        v ^= Digest(r,allegiance);
		v ^= Digest(r,tunnelPosition);
		v ^= r.nextLong()*(hasReducedRecruits?1:2);
        v ^= r.nextLong()*(normalStartSeen?1:2);
        v ^= r.nextLong()*((bumpedWorker==null)?0:(bumpedWorker.Digest()));
        v ^= r.nextLong()*((bumpingWorker==null)?0:(bumpingWorker.Digest()));
        v ^= EuphoriaChip.Digest(r,selectedDieRoll);
        v ^= EuphoriaChip.Digest(r,activeRecruit);
        
      
        for(EPlayer p : players) { v ^= p.Digest(r); }
 		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,pickedHeightStack);
		v ^= EuphoriaChip.Digest(r,doublesElgible);
		v ^= Digest(r,usingDoubles);
		v ^= Digest(r,doublesCount);
		v ^= Digest(r,maggieTheOutlawAvailable);
		v ^= Digest(r,usedChaseTheMiner);
		v ^= Digest(r,proceedGameStep);
		v ^= Digest(r,currentPlayerInTurnOrder);
		v ^= Digest(r,stepNumber);
		v ^= Digest(r,rollNumber);
		
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
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        pickedHeightStack.copyFrom(from_b.pickedHeightStack);
        doublesElgible = from_b.doublesElgible;
        usingDoubles = from_b.usingDoubles;;
        doublesCount = from_b.doublesCount;
        maggieTheOutlawAvailable = from_b.maggieTheOutlawAvailable;
        usedChaseTheMiner = from_b.usedChaseTheMiner;
        proceedGameStep = from_b.proceedGameStep;
        currentPlayerInTurnOrder = from_b.currentPlayerInTurnOrder;
        stepNumber = from_b.stepNumber;
        rollNumber = from_b.rollNumber;

        pickedStateStack.copyFrom(from_b.pickedStateStack);
        reRollPlayers.copyFrom(from_b.reRollPlayers);
        continuationStack.clear();
        for(int i=0,lim=from_b.continuationStack.size(); i<lim; i++)
        {
        	continuationStack.addElement(from_b.continuationStack.elementAt(i).clone(this));
        }
     
        // below here are copied but not digested
        unresign = from_b.unresign;
        REINIT_SIMULTANEOUS_PLAY = from_b.REINIT_SIMULTANEOUS_PLAY;
        SIMULTANEOUS_PLAY = from_b.SIMULTANEOUS_PLAY;
        activePlayer = from_b.activePlayer;
        finalPath.copyFrom(from_b.finalPath);
        droppedStateStack.copyFrom(from_b.droppedStateStack);
        robotBoard = from_b.robotBoard;
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
    		return(new Random((randomKey*(rollNumber*1000))*salt));
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
				p.setKnowledge(i+1); 
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
	{
		EuphoriaCell row[] = tunnelSteps[faction.ordinal()];
		for(int i=0;i<row.length;i++) { if(row[i].height()>0) { setTunnelPosition(faction,i); }}
	}
	// set the tunnel display from the actively used value
	private void setTunnelDisplay(Allegiance faction, int value) {
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
		}}
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
    		{ setAllegianceDisplay(f,getAllegianceValue(f));
    		  if(f!=Allegiance.Icarite) { setTunnelDisplay(f,getTunnelPosition(f)); }
    		}
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
    public void setNormalStarting(replayMode replay)
    {
    	// at this point give the players their dice
    	G.Assert(!hasReducedRecruits,"not already done");
    	hasReducedRecruits = true;
    	shuffledADeck=true;
    	EPlayer smartest = null;
    	for(EPlayer p : players)
    	{	// give each player 2 workers
    		p.addNewWorker(WorkerChip.getWorker(p.color,1));
    		p.addNewWorker(WorkerChip.getWorker(p.color,2));
    		p.totalWorkers += 2;
    		reRollWorkers(p,replay,null,null,null);
     		if((smartest==null) || (p.totalKnowlege()>smartest.totalKnowlege()))
    		{
    			smartest = p;
    		}
    	}
		setWhoseTurn(smartest.boardIndex);
    	currentPlayerInTurnOrder = whoseTurn;
    	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
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
    
	EuphoriaCell authorityCells[][] = {euphorianAuthority,subterranAuthority,wastelanderAuthority,icariteAuthority};

    public EuphoriaCell getAvailableAuthorityCell(Allegiance a)
    {  	EuphoriaCell row[] = authorityCells[a.ordinal()];
    	for(int lim=row.length-1; lim>=0; lim--) { if (row[lim].topChip()==null) { return(row[lim]); }}
    	return(null);
    }

    public void placeAuthorityToken(EuphoriaCell c,EuphoriaChip v)
    {	if(v==null) { if(c.topChip()!=null) { c.removeTop(); }}
    	else { c.addChip(v); 
    		   if(v.color!=null) { getPlayer(v.color).checkForMandatoryEquality(c); }
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
    public void setNextPlayer()
    {	revealedNewInformation = openedAMarket;
    	switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player, state %s",board_state);
        case Puzzle:
            break;
 
        case RetrieveOrConfirm:
        case ConfirmRetrieve:
        	players[whoseTurn].retrievals++;
        	revealedNewInformation = true;
        	setNextPlayer_internal();
        	break;
        case ConfirmPlace:
        	players[whoseTurn].placements++;
        	setNextPlayer_internal();
        	break;
		case EphemeralConfirmRecruits:
        case ConfirmRecruits:
        case ConfirmJoinTheEstablishment:
        case ConfirmFightTheOpressor:
        case ConfirmOneRecruit:
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
        	setNextPlayer_internal();
        	break;
        }
    }
    private void setNextPlayer_internal()
    {
        moveNumber++; //the move is complete in these states
        stepNumber = 0;
        setWhoseTurn((whoseTurn+1)%players.length);
        currentPlayerInTurnOrder = whoseTurn;
     }
    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	return(board_state.doneState());
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
        droppedDestStack.clear();
        pickedHeightStack.clear();
        pickedSourceStack.clear();
        droppedStateStack.clear();
        pickedStateStack.clear();
      }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private EuphoriaCell unDropObject()
    {	EuphoriaCell rv = droppedDestStack.top();
    	if(rv!=null) 
    	{	pickedObject = rv.rackLocation().infinite ? rv.topChip() : rv.removeTop();
    		droppedDestStack.pop();
    		if(rv.onBoard && pickedObject.isWorker())
    		{
    			EPlayer p = getPlayer(pickedObject.color);
    	        p.unPlaceWorker(rv);  
    		}
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
    		  else { //G.Error("Nothing picked"); 
    		  }
    		  setState(pickedStateStack.pop());
    		}
		  pickedObject = null;
    }
    // 
    // drop the floating object.
    //
    private void dropObject(EuphoriaCell c)
    {
    	EuphoriaId rack = c.rackLocation();
        if(!rack.infinite) 
        	{  
        	   if(rack==EuphoriaId.PlayerActiveRecruits) 
        	   	{ getPlayer(c.color).addActiveRecruit(pickedObject); 
        	   	}
        	   else 
        	   	{ c.addChip(pickedObject); 
        	   	}
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
    	{	while(c.row!=idx) { c = c.nextInGroup; G.Assert(c!=null,"Array index %d not found",idx); }
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
     	G.Assert(c.topChip()!=null,"Nothing to pick");
    	lastDroppedObject = null;
    	pickedObject = c.rackLocation().infinite? c.chipAtIndex(pickedHeight) : c.removeChipAtIndex(pickedHeight);
    	
     	if(c.rackLocation==EuphoriaId.UnusedWorkers)
    	{
    		EPlayer p = getPlayer(pickedObject.color);
    		p.totalWorkers++;
    	}
     	else if(c.onBoard && pickedObject.isWorker())
    	{
    		EPlayer p = getPlayer(pickedObject.color);
    		p.unPlaceWorker(c);
    	}
 
    }
    
    private void doGeekTheOracle(replayMode replay,RecruitChip active)
    {
			//
  		    // once per turn, if you gained an artifact, gain another and then discard one.
   		    //
    		EPlayer p = players[whoseTurn];
    		p.incrementKnowledge(replay);
    		if(active==RecruitChip.GeekTheOracle)
    		{
    	    p.incrementKnowledge(replay);
    	    G.Assert(p.collectBenefit(Benefit.Artifact,replay),"extra card failed");
    	    logGameEvent(CardPeek); 
   	    	if(!p.payCost(Cost.CardForGeek,replay))
   	    		{
   	    		setContinuation(new Continuation(Cost.CardForGeek,Cost.CardForGeek.paymentState(),Function.ProceedWithTheGame));
   	    		return;
   	    		}}
    		else if(active==RecruitChip.GeekTheOracle_V2)
    		{	// v2 takes only one knowledge, gives 2 cards
    			G.Assert(p.collectBenefit(Benefit.Artifactx2,replay),"extra cards failed");
    			if(!p.payCost(Cost.CardForGeekx2,replay))
   	    		{
   	    		setContinuation(new Continuation(Cost.CardForGeekx2,Cost.CardForGeekx2.paymentState(),Function.ProceedWithTheGame));
   	    		return;
   	    		}
    		}
    		else { throw G.Error("Not expecting recruit %s",active); }
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
   		acceptPlacement();
    	switch(proceedGameStep)
    	{
    	case 0: proceedGameStep = 0;
    		if(checkGameOver()) {  setState(EuphoriaState.Gameover); return; }
    		
			//$FALL-THROUGH$
		case 1:	proceedGameStep = 1;
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
    			if(pl.knowledgeCheck(replay))
    				{// lost a worker
    				setContinuation(new Continuation(null,Function.ProceedWithTheGame));
    				checkStevenTheScholar(pl,replay,false);
    				return;
    				}}
 	    		}}
    		
			//$FALL-THROUGH$
		case 2:	proceedGameStep = 2;
    		//
    		// here morale and cards are in balance for sure.
    		//
    		while(reRollPlayers.size()>0)
    		{	// do scheduled reroll after opening markets. Roll them from first in to last in
    			Colors color = reRollPlayers.remove(0,true);
    			EPlayer roller = getPlayer(color);
    			reRollWorkers(roller,replay,null,null,Function.ProceedWithTheGame);
    			return;
    		}
  	    	for(EPlayer reroll : players)
	    	{	// reroll after normal retrieval or puzzle setup
  	    		if(reroll.newWorkers.height()>0) 
  	    			{ reRollWorkers(reroll,replay,null,null,Function.ProceedWithTheGame); 
  	    			  return; 
  	    			}
	    	}
			//$FALL-THROUGH$
		case 3:	proceedGameStep = 3;
	    	//
	    	// here no workers need to be rolled
	    	//
   		if(maggieTheOutlawAvailable)
    		{
       			maggieTheOutlawAvailable = false;	// so we don't do it twice
       			logGameEvent(ExtraWaterOrStone); 
       			setContinuation(new Continuation(Benefit.WaterOrStone,Benefit.WaterOrStone.collectionState(),Function.ProceedWithTheGame));
       			return;
    		}

   			if(p.hasAddedArtifact())
   			{
    			if(p.recruitAppliesToMe(RecruitChip.GeekTheOracle) 
    					&& p.canPay(Cost.Knowledgex2)
    					)
   				{
   				proceedGameStep = 4;
   				setContinuation(new Continuation(RecruitChip.GeekTheOracle,Function.DoGeekTheOracle,Function.ProceedWithTheGame));
   				return;
   				} 		
    		if(p.recruitAppliesToMe(RecruitChip.GeekTheOracle_V2) 
    			&& p.canPay(Cost.Knowledge)
    			)
   				{
   				proceedGameStep = 4;
   				setContinuation(new Continuation(RecruitChip.GeekTheOracle_V2,Function.DoGeekTheOracle,Function.ProceedWithTheGame));
   				return;
   				}
   			}
			//$FALL-THROUGH$
		case 4: proceedGameStep = 4;
    	
    			if( ((doublesElgible==null) || (doublesCount<=0))
    					&& !usedChaseTheMiner
    					//&& (board_state!=EuphoriaState.NormalStart)
    					)
    				{ setNextPlayer(); } 
    			
			//$FALL-THROUGH$
		case 5:	proceedGameStep = 0;		// reset to the next round
    			setNextStateAfterDone(replay);  
    			
			break;
		default: break;
    	}
    	for(EPlayer pl : players)
    	{
    		G.Assert(pl.morale>=pl.artifacts.height(),"needed morale check for %s",pl);
    	}
    }
    private boolean moreWorkersAvailable()
    { 	
    	if(revision>=107)
    	{
    	doublesCount--;			// use one up
    	if(doublesCount<0) { return(false); }
    	}
    	return(true);
    }
    // set the state to the appropriate running state
    void setRunningState()
    {	//
    	// no further variations in the program flow from here.
    	//
    	G.Assert(continuationStack.size()==0,"should be at top level");

    	for(EPlayer pl : players)
    	{	

    		G.Assert(pl.morale>=pl.artifacts.height(),"needed morale check for %s",pl);
    		G.Assert(pl.authority.height()>0,"should be gameover here");
    	}

		EPlayer p = players[whoseTurn];
		boolean onBoard = p.hasWorkersOnBoard();
		boolean inHand = p.hasWorkersInHand();
		WorkerChip nextDoublesElgible = null;
		usingDoubles = false;
		if(usedChaseTheMiner) 
			{ 	setState(EuphoriaState.PlaceNew); 
				if(revision>=119)
					{
						nextDoublesElgible = (WorkerChip)p.workers.topChip();
					}else
					{
						doublesElgible = (WorkerChip)p.workers.topChip();
					}
				G.Assert(p.workers.height()>0,"must be workers");
				usedChaseTheMiner = false;
			}
		else if((doublesElgible!=null) 
				&& p.workers.containsChip(doublesElgible)
				&& moreWorkersAvailable()
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
		maggieTheOutlawAvailable = false;
		usedChaseTheMiner = false;
		proceedGameStep = 0;
		selectedDieRoll = null;
		activeRecruit = null;
		bumpingWorker = null;
		bumpedWorker = null;
		openedAMarketLastTurn = openedAMarket;
		openedAMarket = false;		// ephemeral state
		shuffledADeckLastTurn = shuffledADeck;
		shuffledADeck = false;
		//p.startNewTurn(this);	
		finalPath.clear();
		for(EPlayer pp : players) { pp.startNewTurn(); }
	}
    
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
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
	   		setRunningState();
	   		}
	   		break;
	   	case ConfirmRecruits:
	   		if(mustReduceRecruits(replay)) 	
   			{ 
   			setRecruitDialogState(players[whoseTurn]);	
   			break;
   			}
	   		setRunningState();
	   		break;
    	case Puzzle:
       		if(mustReduceRecruits(replay)) 	
       			{ 
	   			setRecruitDialogState(players[whoseTurn]);	
	   			break;
       			}
    		// otherwise continue through    		
			//$FALL-THROUGH$
		case RetrieveOrConfirm:
    	case ConfirmRetrieve:
    	case ConfirmPlace:
    	case PlaceOrRetrieve:
    	case PayForOptionalEffect:
    	case ConfirmPayForOptionalEffect:
    	case PlaceAnother:
    	case PlaceNew:
    	case ConfirmOneRecruit:
    	case ConfirmPayCost:
    	case ConfirmBenefit:
    	case PayCost:
    	case ConfirmRecruitOption:
    	case RecruitOption:
    	case DieSelectOption:
    	case ConfirmJoinTheEstablishment:
    		setRunningState();
    		break;
    	}
    }
    void setRecruitDialogState(EPlayer p)
    {	G.Assert(activePlayer>=0,"activePlayer set");
    	boolean recruitsReady = (p.activeRecruits.height()>0) && (p.hiddenRecruits.height()>0);
    	
    	if(SIMULTANEOUS_PLAY)
    	{	
    		if(p.hasReducedRecruits()) { setState(EuphoriaState.NormalStart); }
    		else if(recruitsReady) { setState(EuphoriaState.EphemeralConfirmRecruits); }
    		else { setState(EuphoriaState.EphemeralChooseRecruits ); }
    	}
    	else
    	{
    		if(recruitsReady) { setState( EuphoriaState.ConfirmRecruits);}
    		else { setState(EuphoriaState.ChooseRecruits);} 
    	}
    }

    private void reRoll(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(p.recruitAppliesToMe(RecruitChip.PeteTheCannibal)
    			&& (p.newWorkers.height()>0)
    			&& (p.totalWorkers>1))
    	{
    		setContinuation(new Continuation(RecruitChip.PeteTheCannibal,
    				Function.DoPeteTheCannibalSacrifice,Function.ReRollSheppardTheLobotomist));
    	}
    	else
    	{
    		reRollSheppardTheLobotomist(replay);
    	}
    }
    
    private void doPeteTheCannibalSacrifice(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	// sacrifice a worker, gain some benefits
    	G.Assert(p.payCost(Cost.SacrificeWorker,replay),"paycost must succeed");
    	G.Assert(p.collectBenefit(Benefit.Foodx4,replay),"collect bene must succeed");
    	logGameEvent(UsingPeteTheCannibal);
    	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.ReRollSheppardTheLobotomist));
    }
    private void reRollSheppardTheLobotomist(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if((p.totalWorkers>1)
    			&& (p.newWorkers.height()>0)
    			&& p.recruitAppliesToMe(RecruitChip.SheppardTheLobotomist))
    	{
    		setContinuation(new Continuation(RecruitChip.SheppardTheLobotomist,
    				Function.DoSheppardTheLobotomistSacrifice,Function.ProceedWithTheGame));
    	} 
    	else { proceedWithTheGame(replay); }
    }
    private void doSheppardTheLobotomistSacrifice(replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	p.collectBenefit(Benefit.Blissx4,replay);
    	G.Assert(p.payCost(Cost.SacrificeWorker,replay),"paycost should succeed");
    	logGameEvent(SheppardTheLobotomistSacrifice);
    	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),
    			Function.ProceedWithTheGame));
    }


    // pay and gain morale
    private void reRollWithPayment(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	EuphoriaCell payment = droppedDestStack.top();
    	if(payment.rackLocation()==EuphoriaId.EnergyPool)
    	{
    	// jefferson allows you to pay with energy, and changes the benefit
    	G.Assert(p.recruitAppliesToMe(RecruitChip.JeffersonTheShockArtist),"should be jefferson");
    	p.gainMorale();
    	p.decrementKnowledge();
    	logGameEvent(JeffersonTheShockArtistEffect,currentPlayerColor(),currentPlayerColor());
    	reRoll(replay);
    	}
    	else
    	{
    	if((payment.rackLocation()==EuphoriaId.BlissPool)
    		&& p.recruitAppliesToMe(RecruitChip.GidgitTheHypnotist))
    	{	//acceptPlacement();
    		setContinuation(new Continuation(RecruitChip.GidgitTheHypnotist,Function.DoGidgitTheHypnotist,Function.ReRollNormalPayment));
    	}
    	else { reRollNormalPayment(replay); }
    	}
    }
    private void doGidgitTheHypnotist(replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	p.decrementKnowledge();
    	p.decrementKnowledge();
    	logGameEvent(GidgetTheHypnotistEffect,currentPlayerColor(),currentPlayerColor());
    	reRoll(replay);
    }
    private void reRollNormalPayment(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	p.gainMorale();
		p.gainMorale();
		reRoll(replay);
	}
    
    // no payment mean lose morale
    private void reRollWithoutPayment(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	int ntolose = 1;
    	if((p.totalWorkers==2)
    			&& p.recruitAppliesToMe(RecruitChip.YordyTheDemotivator)
    			&& p.canPay(Cost.Morale)
    			)
    	{	logGameEvent(YordySavesTheDay,currentPlayerColor()); 
    		ntolose = 0;	// yordy fixes everything
    	}
    	else if(p.penaltyAppliesToMe(MarketChip.CafeteriaOfNamelessMeat))
        	{	MarketChip.CafeteriaOfNamelessMeat.logGameEvent(this,NamelessMeat,currentPlayerColor()); 
        		ntolose++;
        	}

    	  	
     	if(doLoseMorale(p,ntolose,replay,null,Function.ReRollYordyCheck_V2))
    	{
     		reRollYordyCheck_V2(replay);
    	}

    }

    private void reRollYordyCheck_V2(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(p.recruitAppliesToMe(RecruitChip.YordyTheDemotivator_V2)
    			&& p.canPay(Cost.Energy))
    	{
    		setContinuation(new Continuation(RecruitChip.YordyTheDemotivator_V2,
    				Function.DoYordyTheDemotivator_V2,Function.ReRoll));
    		return;
    	}
    	reRoll(replay);
    }
    
    private void doYordyTheDemotivator_V2(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	G.Assert(selectedDieRoll!=null,"die must be selected");
    	G.Assert(p.payCost(Cost.Energy,replay),"payment must succeed"); 
    	logGameEvent(YordyTheDemotivatorSelects,selectedDieRoll.shortName());
    	reRollWorkers(p,replay,null,selectedDieRoll,Function.ReRoll);
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
    				p.addActiveRecruit(newRecruit);
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
    			op.collectBenefit(Benefit.Artifact,replay);
    			logGameEvent(CardFromJacko,s.get(op.color.name()));
    			// doesn't call doCardsGained since the purpose 
    			// is to check for having jacko
    			doMoraleCheck(op,null,replay);
   			};
    	}  
    }
 
    private void doDone(replayMode replay)
    {	
        switch(board_state)
        {
        case Resign:
        	win[(whoseTurn+1)%players.length] = true;
    		setState(EuphoriaState.Gameover);
    		break;
        case EphemeralConfirmRecruits:
   		case ConfirmRecruits:
   			players[whoseTurn].discardNewRecruits(true);
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
   		case ConfirmOneRecruit:
			players[whoseTurn].discardNewRecruits(false);
			players[whoseTurn].transferDiscardedRecruits(usedRecruits);
			EuphoriaCell top = droppedDestStack.top();
			if(top.rackLocation()==EuphoriaId.PlayerActiveRecruits)
				{RecruitChip newRecruit = (RecruitChip)top.topChip();
				if(getAllegianceValue(newRecruit.allegiance)>=(AllegianceSteps-1))
				{
				players[whoseTurn].addAllegianceStar(replay);
				}
			}
			acceptPlacement();
    		proceedWithTheGame(replay);
			break;
   		case CollectOptionalBenefit:
   			{Benefit pend = pendingBenefit();
   			EPlayer p = players[whoseTurn];
   			switch(pend)
				{
				case WaterOrMorale:			// soulless the plumber
					p.incrementMorale();
					logGameEvent(SoullessThePlumberMorale,currentPlayerColor());
					break;
				case MoraleOrEnergy:
					p.incrementMorale();
					logGameEvent(GaryTheElectricianMorale,currentPlayerColor());
					break;
				case KnowledgeOrBliss:
					p.decrementKnowledge();
					logGameEvent(SarineeTheCloudMinerKnowledge,currentPlayerColor());
					break;
				case KnowledgeOrFood:
					p.decrementKnowledge();
					logGameEvent(ScabyTheHarvesterKnowledge,currentPlayerColor());
					break;
				default: throw G.Error("Not expecting no benefit for %s",pend);
				}
   			acceptPlacement();
   			doContinuation(replay);
   			}
   			break;
   		case ConfirmBenefit:
   			{
   			Benefit pend = pendingBenefit();
   			EPlayer p = players[whoseTurn];
  			p.satisfyBenefit(replay,pend,droppedDestStack);
  			while(marketBasket.height()>0)
  			{
 			// throw away the rejected card
  			EuphoriaChip discard = marketBasket.removeTop();
  			if((discard!=null)&&(discard.isArtifact())) { addArtifact(discard); }
 			if(replay!=replayMode.Replay) { animateReturnArtifact(marketBasket); }
   			}
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
   			break;
  
   		case PayCost:
   				players[whoseTurn].confirmPayment(originalPendingCost(),pendingCost(),droppedDestStack,replay);
  				acceptPlacement();
   				doUnpaidContinuation(replay);
    			break;
   		case ConfirmPayCost:
				players[whoseTurn].confirmPayment(originalPendingCost(),pendingCost(),droppedDestStack,replay);
   				acceptPlacement();
  				doContinuation(replay);
    			break;
  		
   		case ConfirmPlace:
   			{
   			EuphoriaCell dest = getDest();
   			G.Assert(dest!=null,"should be something placed");
   			acceptPlacement();		// empty the interaction stack
   			dropWorker(dest,replay);
   			}
   			break;
   		case PlaceAnother:	// hit Done without placing another worker.
   			doublesElgible = null;
			//$FALL-THROUGH$
		case PlaceNew:		// place the new worker, only from ChaseTheMiner
   			acceptPlacement();		
   			proceedWithTheGame(replay);
   			break;
   		case RetrieveOrConfirm:
   		case ConfirmRetrieve:
        	{	EPlayer p = players[whoseTurn];
        		if(p.recruitAppliesToMe(RecruitChip.MaggieTheOutlaw))
        		{
        			// check to see if all retrieved workers are from subterra
        			maggieTheOutlawAvailable = true;
        			for(int lim=pickedSourceStack.size()-1; lim>=0; lim--)
        			{
        				EuphoriaCell c = pickedSourceStack.elementAt(lim);
        				maggieTheOutlawAvailable &= (c.allegiance==Allegiance.Subterran);
        			}
        		}
        		acceptPlacement();
        		if(p.canPay(Cost.BlissOrFood))
        		{	Cost cost = p.alternateCostWithRecruits(Cost.BlissOrFood);
        			setContinuation(new Continuation(Cost.BlissOrFood,cost,cost.paymentState(),
        								Function.ReRollWithPayment,Function.ReRollWithoutPayment));
         		}
        		else 
        		{
        		// can't pay
        		reRollWithoutPayment(replay);
        		}
        	}
        	break;
        case Puzzle:
            acceptPlacement();
            doublesElgible = null;
            usedChaseTheMiner = false;
            proceedGameStep = 0;
            proceedWithTheGame(replay);
            break;
            
        case ConfirmJoinTheEstablishment:
        	// give the man a star
        	{
        	EPlayer p = players[whoseTurn];
        	p.dilemmaResolved = true;
        	p.dilemma.addChip(p.getAuthorityToken());
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
           	EPlayer p = players[whoseTurn];
           	p.dilemmaResolved = true;
           	p.newRecruits[0].reInit();
           	p.newRecruits[1].reInit();
           	p.newRecruits[0].addChip(unusedRecruits.removeTop());
           	p.newRecruits[1].addChip(unusedRecruits.removeTop());
          	acceptPlacement();
         	setState(EuphoriaState.ChooseOneRecruit);
        	}
        	break;
        	
        case ConfirmUseJackoOrContinue:
        case ConfirmUseJacko:
        	{
        	EPlayer p = players[whoseTurn];
         	acceptPlacement();
         	if(revision<117)
         		{ setState(EuphoriaState.ConfirmPayCost);
         	      G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment must succeed");
         		}
         	if(!GAME_EU_Dumbot_Brius_2014_12_20_2045)
         		{ // game EU-Dumbot-Brius-2014-12-20-2045 replay was broken by making this doContinuation unconditional
         		  // this was an intermediate bug that slipped through without a revision number
         		doContinuation(replay); 
         		}
        	finishJackoTheArchivist(p,replay);
        	}
        	break;
        default:
        	throw G.Error("Not expecting state %s",board_state);
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
    // board satisfies cost.  There always only one type of item in question,
    // and the height of the stack is usually the only question.
    private boolean boardSatisfiesCost(Cost cost)
    {	int nCards=0;
    	int nCommodities=0;
 		switch(cost)
		{
 		case Energyx4_Card:			// reduced to just a card because we already paid the energy
 		case GoldOrFoodOrBliss:
 		case ClayOrFoodOrBliss:
 		case StoneOrFoodOrBliss:
 		case ClayOrBliss:
 		case StoneOrBliss:
 		case GoldOrBliss:
 		case BlissOrFood:
 		case ResourceOrBlissOrFood:
		case NonBlissCommodity:
		case ConstructionSiteStone:
		case ConstructionSiteGold:
		case Gold:
		case ConstructionSiteClay:
		case ResourceOrBliss:
		case ResourceAndKnowledgeAndMorale:
		case ResourceAndKnowledgeAndMoraleOrArtifact:	// michael the engineer and flartner the luddite
		case GoldOrArtifact:
		case StoneOrArtifact:
		case ClayOrArtifact:
			return(droppedDestStack.size()==1);
		case ArtifactPair:
			{
			int h = usedArtifacts.height();
			return( (droppedDestStack.size()==2) && (usedArtifacts.chipAtIndex(h-1)==usedArtifacts.chipAtIndex(h-2)));
			}
			
		case ArtifactJackoTheArchivist_V2:
			if(droppedDestStack.size()==1) 
				{ return(true); }
			// otherwise fall into normal artifactx3 test
			//$FALL-THROUGH$
		case Artifactx3:
		case Morale_Artifactx3:
		case Mostly_Artifactx3:
			if(players[whoseTurn].penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement))
			{
				return(boardSatisfiesCost(Cost.Artifactx3Only));
			}
			return(boardSatisfiesCost(Cost.ArtifactPair) || boardSatisfiesCost(Cost.Artifactx3Only));
			
			
		case Artifactx3Only:		// various markets.  Usually take 3 cards or a pair
		case Resourcex3:			// nimbus loft
		case Morale_Resourcex3:
		case Mostly_Resourcex3:
			return(droppedDestStack.size()==3);
			
		case Artifactx2:
		case Card_ResourceOrBlissOrFood:
		case Card_ResourceOrBliss:
		case Card_Resource:
			return(droppedDestStack.size()==2);
			
		case Commodity:
		case Resource:
			// we need 1 of something the player chooses
			return(droppedDestStack.size()==1);
		
		// dilemma costs
		case BatOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Bat))
					|| (droppedDestStack.size()==2));
			
		case BearOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Bear))
					|| (droppedDestStack.size()==2));
			
		case BoxOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Box))
					|| (droppedDestStack.size()==2));
			
		case BalloonsOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Balloons))
					|| (droppedDestStack.size()==2));
			
		case BifocalsOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Bifocals))
					|| (droppedDestStack.size()==2));
			
		case BookOrCardx2:
			return( ((droppedDestStack.size()==1) && (usedArtifacts.topChip()==ArtifactChip.Book))
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
		case Morale_BlissOrFoodPlus1:
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
		default: throw G.Error("Not expecting placement cost %s",cost);
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
    	default: throw G.Error("not expecting drop in state %s",board_state);
     	case PlaceOrRetrieve:
    	case Retrieve:
    	case RetrieveOrConfirm:
    	case Place:
    	case PlaceNew:
    	case PlaceAnother:
    		{
    		EuphoriaCell dest = getDest();
    		EuphoriaId rack = dest.rackLocation();
    		if(rack.perPlayer)
    			{	// destination is the player board, we must be retrieving
    				if(p.hasWorkersOnBoard())
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
    	case PayForOptionalEffect:
    		// any acceptable payment is enough
    		setState(EuphoriaState.ConfirmPayForOptionalEffect);
    		break;
 
    		
   	case FightTheOpressor:
   	case JoinTheEstablishment:
   	case PayCost:
        		// pay cost through a zoomed view of the board
       	{	Cost cost = pendingCost();
       		if(boardSatisfiesCost( pendingCost()))
       		{	if((cost==Cost.ArtifactJackoTheArchivist_V2)
       				&& (droppedDestStack.size()==1))
       			{	if(revision<117) { G.Assert(p.knowledge<=4,"knowledge check"); }
       				setState(couldPayWithoutJacko(p)?EuphoriaState.ConfirmUseJackoOrContinue:EuphoriaState.ConfirmUseJacko);
       			}
       			else 
       			{ setState(EuphoriaState.ConfirmPayCost); 
       			}
       		}
       		}
       		break;
       	case ChooseOneRecruit:
       		setState(EuphoriaState.ConfirmOneRecruit);
       		break;
     	case CollectBenefit:
     	case CollectOptionalBenefit:
     	{	Benefit bene = pendingBenefit();
    		switch(bene)
    		{
    		case EuphorianAuthority2:
    		case WastelanderAuthority2:
    		case SubterranAuthority2:
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
    			// need only one of some permitted thing
    			setState(EuphoriaState.ConfirmBenefit);
    			break;
    		case IcariteInfluenceAndResourcex2:

    			if(droppedDestStack.size()==2) { setState(EuphoriaState.ConfirmBenefit); }
    			break;

    		default: throw G.Error("Not expecting pending benefit %s",bene);
    		}
     	}
    		break;
    	case NormalStart:
     		acceptPlacement();
     		break;
     	case EphemeralConfirmRecruits:
     	case EphemeralChooseRecruits:
     	case ConfirmRecruits:
     		acceptPlacement();
     		/*$FALL-THROUGH$*/
    	case ChooseRecruits:
    		setRecruitDialogState(p);
    		break;
    	case Puzzle: acceptPlacement(); 
    		break;
    	}
    }
    
    private void setNextStateAfterPick(EPlayer p)
    {	switch(board_state)
    	{
    	default: G.Error("not expecting pick in state %s",board_state);
    		break;

    	case ConfirmUseJackoOrContinue:
    	case ConfirmUseJacko:
    		setState(EuphoriaState.PayCost);
    		break;

     	case EphemeralChooseRecruits:
     	case NormalStart:
     		break;
    	case ConfirmRecruits:
    	case EphemeralConfirmRecruits:
    		setRecruitDialogState(p);
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
    		break;
    	case Place:
    	case PlaceAnother:
    	case PlaceNew:
    	case Retrieve:
    	case ChooseRecruits:
    	case ChooseOneRecruit:
     	case CollectBenefit:
    	case CollectOptionalBenefit:
    	case RetrieveOrConfirm:
     	case PayForOptionalEffect:
    	case PayCost:
    	case JoinTheEstablishment:
    	case FightTheOpressor:
      	case Puzzle:  
    		break;
    	}
    }

    // return true if no further interaction is needed.
    public void reRollWorkers(EPlayer p,replayMode replay,EuphoriaCell dest,WorkerChip chosenValue,Function continuation)
    {	
    	EuphoriaCell workers = p.newWorkers;
    	if(continuation!=null) { setContinuation(new Continuation(dest,continuation)); }

    	if(workers.height()>0)
    	{
    	Random r = newRandomizer(0x72525+p.boardIndex);
    	int rollPenalties = 0;
    	p.resultsInDoubles = false;
    	while(workers.height()>0)
    	{	int newvalue = Random.nextInt(r,6)+1;
    		workers.removeTop();
    		WorkerChip newWorker = chosenValue!=null ? chosenValue : (WorkerChip.getWorker(p.color,newvalue));	// the actual reroll
    		p.addWorker(newWorker);
    		chosenValue = null;		// only select one die
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(p.newWorkers);
    			animationStack.push(p.workers);
    		}
    		switch(newWorker.knowledge())
    		{
    		case 1:
    			if(p.penaltyAppliesToMe(MarketChip.DisassembleATeddyBearShop)) { rollPenalties++; }
    			break;
    		case 2:
    			if(p.penaltyAppliesToMe(MarketChip.ClinicOfBlindHindsight)) { rollPenalties++; }
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
    	
    	
      	if(p.knowledgeCheck(replay))
      		{ if(checkStevenTheScholar(p,replay,true))
      			{
      			if(revision<113) { return; }	// shouldn't exit here, but we did.
      			}
      		}

      	if(revision<113)
       	{
       	if((penalty!=null) && !p.payCost(penalty,replay))
       		{	
      		setContinuation(dest==null
      							?new Continuation(penalty,penalty,penalty.paymentState(),Function.Return)
      							:new Continuation(penalty,penalty,penalty.paymentState(),dest,Function.Return),
      							p.boardIndex							
      					);
      		if(revision<113) { return; }	// shouldn't exit here, but we did.
       		}
       	}
       	
       	
       	if(p.resultsInDoubles)
       	{
       		for(EPlayer pl : players)
       		{
       			if((pl!=p)
       					&& pl.canPay(Cost.Moralex2)
       					&& pl.recruitAppliesToMe(RecruitChip.BenTheLudologist)
       					)
       			{
       				setContinuation((dest==null)
       							? new Continuation(RecruitChip.BenTheLudologist,Function.DoBenTheLudologist,pl.boardIndex,Function.Return)
       							: new Continuation(RecruitChip.BenTheLudologist,dest,Function.DoBenTheLudologist,pl.boardIndex,Function.Return),
       								pl.boardIndex);
       			}
       		}
       	}
       	
       	if(revision>=113)
       	{
       	if((penalty!=null) && !p.payCost(penalty,replay))
       		{	
      		setContinuation(dest==null
      							?new Continuation(penalty,penalty,penalty.paymentState(),Function.Return)
      							:new Continuation(penalty,penalty,penalty.paymentState(),dest,Function.Return),
      							p.boardIndex							
      					);
      		if(revision<113) { return; }	// shouldn't exit here, but we did.
       		}
       	}

 
    	}
    	//if(continuation!=null) { doContinuation(replay); }
     }
    private void doBenTheLudologist(EuphoriaCell dest,replayMode replay)
    {	// ok to get here multiple times.
    	EPlayer p = players[whoseTurn];
    	G.Assert(p.recruitAppliesToMe(RecruitChip.BenTheLudologist),"ben only");
		doLoseMorale(p,2,replay,dest,Function.Return);
		logGameEvent(BenTheLudologistEffect,currentPlayerColor());
    	setContinuation((dest==null)
    					? new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.Return)
    					: new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),dest,Function.Return));
    }
    private void doStevenTheScholar(EuphoriaCell dest,replayMode replay,Colors co,Function continuation)
    {
		// ok to get here multiple times
		// steven the scholar applies, give him a card and a resource, check knowledge
    	EPlayer p = getPlayer(co);
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"collect must succeed");
    	logGameEvent(UsingStevenTheScholar,s.get(co.name()));
    	doCardsGained(p,replay,dest,continuation);
    	setContinuation((dest==null)
    						? new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.Return)
    						: new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),dest,Function.Return));
    }
    // bump a worker out of a bumpable cell, give him back to 
    // re-roll and give him back to his owner.
    public void bumpWorker(EuphoriaCell dest,replayMode replay)
    {
		addToFinalPath("bumpWorker");
    	G.Assert(dest.rackLocation().canBeBumped,"bumpable position");
    	WorkerChip worker = (WorkerChip)dest.removeChipAtIndex(0);
    	EPlayer otherPlayer = getPlayer(worker.color);
    	otherPlayer.unPlaceWorker(dest);
    	otherPlayer.addNewWorker(worker);			// add to the new workers pool, will trigger re-roll and knowledge check.
    	bumpedWorker = worker;		// remember for MaximeTheAmbassador
    	bumpingWorker = (WorkerChip)dest.topChip();
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(dest);
    		animationStack.push(otherPlayer.newWorkers);
    	}
    	acceptPlacement();
 
    	if(otherPlayer.recruitAppliesToMe(RecruitChip.AmandaTheBroker_V2)
	    				&& otherPlayer.canPay(Cost.Bliss)
	    				)
	    		{
	    			setContinuation(new Continuation(RecruitChip.AmandaTheBroker_V2,dest,Function.DoAmandaTheBroker,Function.BumpWorkerJuliaTheThoughtInspector_V2),
	    					otherPlayer.boardIndex
	    					);
	    			return;
	    		}
    	
    	if((bumpedWorker.color!=bumpingWorker.color)
    				&& otherPlayer.recruitAppliesToMe(RecruitChip.AmandaTheBroker)
    				&& otherPlayer.canPay(Cost.Bliss)
    				)
    		{
    			setContinuation(new Continuation(RecruitChip.AmandaTheBroker,dest,Function.DoAmandaTheBroker,Function.BumpWorkerJuliaTheThoughtInspector_V2),
    					otherPlayer.boardIndex
    					);
    			return;
    		}
	    
    	bumpWorkerJuliaTheThoughtInspector_V2(dest,replay);
    }
    	
// called when we bumped a worker of another color
private void bumpWorkerJuliaTheThoughtInspector_V2(EuphoriaCell dest,replayMode replay)
{
		EPlayer p = players[whoseTurn];
   		if(p.recruitAppliesToMe(RecruitChip.JuliaTheThoughtInspector_V2)
   				&& (bumpedWorker.color != bumpingWorker.color)
   				&& p.canPay(Cost.Morale))
   			{
   			setContinuation(new Continuation(RecruitChip.JuliaTheThoughtInspector_V2,
										Function.DoJuliaTheThoughtInspector_V2,
										Function.Return));
   			return;
   			}

    	reRollWorkers(getPlayer(bumpedWorker.color),replay,dest,null,null);
}

private void doAmandaTheBroker(EuphoriaCell dest,replayMode replay,RecruitChip active,Function continuation)
    {	// give the bumper one of our bliss, and use the selected die roll
		addToFinalPath("doAmandaTheBroker");
		EPlayer p = getPlayer(bumpingWorker.color);
    	EPlayer other = getPlayer(bumpedWorker.color);
    	G.Assert(selectedDieRoll!=null,"die must be selected");
    	G.Assert(other.payCost(Cost.Bliss,replay),"payment must succeed");
     	if(active==RecruitChip.AmandaTheBroker)
    	{
    	// in euphoria2, just pay, the other player doesn't collect
    	G.Assert(p.collectBenefit(Benefit.Bliss,replay),"collection must succeed");
    	}
    	logGameEvent(AmandaTheBrokerSelects,selectedDieRoll.shortName());
    	reRollWorkers(other,replay,dest,selectedDieRoll,continuation);
    }
    // return true if no further interaction is needed
    private void reRollBumpedWorker(EuphoriaCell dest,replayMode replay)
    {	EPlayer otherPlayer = getPlayer(bumpedWorker.color);
    	reRollWorkers(otherPlayer,replay,dest,null,Function.Return);

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
		G.Assert(!(rack.canBeBumped && (dest.height()>1)),"should have been bumped");
		if(p.hasUsedBrianTheViticulturist() && p.canPay(Cost.Knowledge))
		{	p.payCost(Cost.Knowledge,replay);
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
    				&& p.canPay(Cost.Knowledgex2)
    				&& highestMarketKnowledge(rack,dest))
    		{
    			setContinuation(new Continuation(RecruitChip.JonathanTheArtist,dest,
    					Function.DoJonathanTheArtist,Function.DropWorkerCollectBenefitAfterRecruits));
    			return;
    		}
    		break;
		case IcariteCloudMine:
				{
					if((p.recruitAppliesToMe(RecruitChip.ZongTheAstronomer))
							&& ((p.totalResources()+p.artifacts.height())==0))
					{
						setContinuation(new Continuation(Benefit.WaterOrEnergy,Benefit.WaterOrEnergy.collectionState(),
									dest,Function.DropWorkerCollectBenefitAfterRecruits));
						return;
					}
					if((p.recruitAppliesToMe(RecruitChip.ZongTheAstronomer_V2))
							&& (p.canPay(Cost.Knowledge)))
					{
						setContinuation(new Continuation(RecruitChip.ZongTheAstronomer_V2,dest,
										Function.DoZongTheAstronomer_V2,
										Function.DropWorkerCollectBenefitAfterRecruits));
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
			dropWorkerDaveTheDemolitionist(dest,replay);	// DaveTheDemolitionist only for tunnel mouth
			return;
		default: 
			G.Assert(!rack.canBeBumped,"all bumpable positions should be considered");
			break;
		}
 	
    	dropWorkerCollectBenefitAfterRecruits(dest,replay);
    }
    // correct implementation
    private void doJonathanTheArtist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doJohnathanTheArtist");
		G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment must succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"benefit must succeed");
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
    	G.Assert(p.payCost(Cost.Knowledge,replay),"payment must succeed");
    	p.collectBenefit(Benefit.Artifact,replay);	// get a card
    	for(EPlayer op : players) { if(op!=p) { op.collectBenefit(Benefit.Bliss,replay); }}
    	doCardsGained(p,replay,dest,Function.DropWorkerCollectBenefitAfterRecruits);
    }
    
    // reached after all drop worker on tunnel positions
    private void dropWorkerDaveTheDemolitionist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerDaveTheDemolitionist");
		if((bumpedWorker!=null)
				&& (bumpedWorker.color!=p.color))
		{	if(p.canPay(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.DaveTheDemolitionist))
			{
			setContinuation(new Continuation(RecruitChip.DaveTheDemolitionist,dest,
					Function.DoDaveTheDemolitionist,Function.DropWorkerLauraThePhilanthropist));
			return;
			}
		else if(p.canPay(Cost.Knowledge)
				&& p.recruitAppliesToMe(RecruitChip.DaveTheDemolitionist_V2))
			{
			setContinuation(new Continuation(RecruitChip.DaveTheDemolitionist_V2,dest,
					Function.DoDaveTheDemolitionist,Function.DropWorkerLauraThePhilanthropist));
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
		G.Assert(p.payCost(cost,replay),"paycost should succeed");
		G.Assert(p.collectBenefit(bene.associatedResource(),replay),"benefit must succeed");
		incrementTunnelPosition(dest.allegiance);	// bump the tunnel position an extra time
		logGameEvent(UseDaveTheDemolitionist,bene.name(),currentPlayerColor());
		dropWorkerLauraThePhilanthropist(dest,replay);	
    }
    
    // reached after drop worker on tunnels and worker activation
    private void dropWorkerLauraThePhilanthropist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerLauraThePhilanthropist");
		if((bumpedWorker!=null)
    		&& (bumpedWorker.color!=p.color)
    		&& p.canPay(Cost.Gold)			// uses her own gold
    		&& p.recruitAppliesToMe(RecruitChip.LauraThePhilanthropist))
    	{
    		setContinuation(new Continuation(RecruitChip.LauraThePhilanthropist,dest,
    				Function.DoLauraThePhilanthropist,Function.DropWorkerCollectBenefitAfterRecruits));
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
    	G.Assert(p.payCost(Cost.Gold,replay),"payment must succeed");
    	G.Assert(other.collectBenefit(Benefit.Gold,replay),"collection must succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifactx2,replay),"collection must succeed");
    	logGameEvent(LauraThePhilanthropistEffect,other.color.name());
    	doCardsGained(p,replay,dest,Function.DropWorkerCollectBenefitAfterRecruits);
    }

    
    // collect benefits after all placment recruits have had their say
    private void dropWorkerCollectBenefitAfterRecruits(EuphoriaCell dest,replayMode replay)
    {	

    	Benefit bene = dest.placementBenefit;
    	addToFinalPath("dropWorkerCollectBenefitAfterRecruits");

		WorkerChip worker = (WorkerChip)dest.topChip();
		EPlayer p = getPlayer(worker.color);
		Allegiance alleg = dest.allegiance;
		boolean allegianceUpgrade = alleg!=null && (getAllegianceValue(alleg)>=ALLEGIANCE_TIER_2) && p.hasActiveRecruit(alleg);
		boolean knowledgeCheckNeeded = false;
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
			break;
		default: break;
			
		}
    	if(!p.collectBenefit(bene,replay))	// if we can't collect the benefit directly, schedule an interaction
    	{	setContinuation(new Continuation(bene,bene.collectionState(),dest,Function.DropWorkerAfterBenefit));
    		if(knowledgeCheckNeeded) 
    			{ // this wil happen BEFORE the resource distribution
    				doCardsGained(p,replay,dest,Function.Return); 
    			}
    		return;
     	}
    	
    	if(knowledgeCheckNeeded)
    	{
    		doCardsGained(p,replay,dest,Function.DropWorkerAfterBenefit); 
    	}
    	else { dropWorkerAfterBenefit(dest,replay); }
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
    {
		if(p.moraleCheck(replay))
		{
			// we need to lose a card or several
			Cost cost = p.moraleCost();
			setContinuation(dest==null 
					? new Continuation(cost,cost.paymentState(),cont)
					: new Continuation(cost,cost.paymentState(),dest,cont),
					p.boardIndex);
			return(false);
			
		}
		G.Assert(p.morale>=p.artifacts.height(),"should be in balance");
		return(true);
    }
    // return false if interaction is needed
    private boolean doMoraleCheck(EPlayer p,EuphoriaCell dest,replayMode replay)
    {
    	return(doMoraleCheck(p,replay,dest,Function.Return));
    }
        
    private void dropWorkerAfterBenefit(EuphoriaCell dest,replayMode replay)
    {	
    	acceptPlacement();
		addToFinalPath("dropWorkerAfterBenefit");
		doCardsGained(players[whoseTurn],replay,dest,Function.DropWorkerAfterMorale);
    }
    private void dropWorkerAfterMorale(EuphoriaCell dest,replayMode replay)
    {	addToFinalPath("dropWorkerAfterMorale");
    	setContinuation(new Continuation(dest,Function.DropWorkerJuliaTheThoughtInspector));
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
			EPlayer p = players[whoseTurn];
			if(p.recruitAppliesToMe(RecruitChip.SarineeTheCloudMiner)
					&& (countWorkers(getProducerArray(dest.allegiance))==1)
					)
				{
				if(!p.collectBenefit(Benefit.KnowledgeOrBliss,replay))
					{
					setContinuation(new Continuation(Benefit.KnowledgeOrBliss,Benefit.KnowledgeOrBliss.collectionState(),
							dest,Function.Return));
					return;
					}
				else { logGameEvent(SarineeTheCloudMinerBliss); }
				}
			}
			
			dropWorkerKyleTheScavenger(dest,replay);
		    break;
		case EuphorianGenerator:
			doJosiahTheHacker(dest,replay,euphorianGenerator);
			{EPlayer p = players[whoseTurn];
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
					if(!p.collectBenefit(Benefit.MoraleOrEnergy,replay))
					{
					setContinuation(new Continuation(Benefit.MoraleOrEnergy,Benefit.MoraleOrEnergy.collectionState(),
	    					dest,Function.DropWorkerKyleTheScavenger));
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
					p.collectBenefit(Benefit.Food,replay);
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
			dropWorkerSoullessThePlumber(dest,replay);
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
    				setContinuation(new Continuation(RecruitChip.NakagawaTheTribute_V2,dest,Function.DoNakagawaTheTribute_V2,Function.Return));
    				}
    			break;
		default:	;
		}
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
    			if(!p.collectBenefit(Benefit.WaterOrMorale,replay))
    			{
    			setContinuation(new Continuation(Benefit.WaterOrMorale,Benefit.WaterOrMorale.collectionState(),
    					dest,
    					Function.DropWorkerKyleTheScavenger));
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
			G.Assert(p.collectBenefit(Benefit.Water,replay),"collection must succeed");
			logGameEvent(IanTheHorticulturistEffect);
    	}
    }
    // when we use foreign farms, gain morale and make them gain knowledge
    private void doJosiahTheHacker(EuphoriaCell dest,replayMode replay,EuphoriaCell[]cells)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doJosiahTheHacker");
		if(p.recruitAppliesToMe(RecruitChip.JosiahTheHacker))
    	{
        	int gained =0;
        	if(p.incrementMorale())	// we get morale
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
    {
		EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerXanderTheExcavator");
		if((bumpedWorker!=null) 
				&& (bumpedWorker.color!=p.color)
				&& (bumpedWorker.knowledge()>bumpingWorker.knowledge())
				&& (p.recruitAppliesToMe(RecruitChip.XanderTheExcavator))
				&& (p.canPay(Cost.Knowledge))
				)
		{	setContinuation(new Continuation(RecruitChip.XanderTheExcavator,dest,
				Function.DoXanderTheExcavator,Function.DropWorkerChaseTheMinerSacrifice));
			return;
		}
		dropWorkerChaseTheMinerSacrifice(dest,replay);
	}
    private void doXanderTheExcavator(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	G.Assert(p.payCost(Cost.Knowledge,replay),"payment must succeed");
    	Benefit bene = dest.placementBenefit.associatedResource();
    	G.Assert(p.collectBenefit(bene,replay),"benefit must succeed");
    	logGameEvent(XanderTheExcavatorBenefit,bene.name(),currentPlayerColor());
    	
    	dropWorkerChaseTheMinerSacrifice(dest,replay);
    }
    // called form worker activation
    private void dropWorkerChaseTheMinerRoll(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerChaseTheMinerRoll");
		if( (p.newWorkers.height()>0)		// we still have a new worker
				&& p.recruitAppliesToMe(RecruitChip.ChaseTheMiner))
    	{
    		usedChaseTheMiner = true;    
    		logGameEvent(ChaseTheMinerRoll);
    	}
    	dropWorkerPeteTheCannibal(dest,replay);
    }

    // reached from worker tunnel placements
    private void dropWorkerChaseTheMinerSacrifice(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerChaseTheMiner");
		if((p.totalWorkers>1)
    		&& p.recruitAppliesToMe(RecruitChip.ChaseTheMiner)
    			)
    	{	setContinuation(new Continuation(RecruitChip.ChaseTheMiner,dest,
    			Function.DoChaseTheMinerSacrifice,Function.DropWorkerPeteTheCannibal));
    	}
    	else {  	dropWorkerPeteTheCannibal(dest,replay); }
    }
    private void doChaseTheMinerSacrifice(EuphoriaCell dest,replayMode replay)
    {
		addToFinalPath("doChaseTheMinerSacrifice");
    	EPlayer p = players[whoseTurn];
     	p.sacrificeWorker(dest,replay);
     	Benefit bene = TUNNEL_BENEFIT_CHASE_THE_MINER[dest.allegiance.ordinal()];
     	logGameEvent(ChaseTheMinerSacrifice,bene.description);
     	p.collectBenefit(bene,replay);
     }

    // reached from worker activation sites
    private void dropWorkerPeteTheCannibal(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerPeteTheCannibal");
		if(p.hasGainedWorker()
				&& p.canPay(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.PeteTheCannibal))
		{
			setContinuation(new Continuation(RecruitChip.PeteTheCannibal,dest,
					Function.DoPeteTheCannibalBenefit,Function.DoSheppardTheLobotomistBenefit));
		}
		else { doSheppardTheLobotomistBenefit(dest,replay); }

    }
    
    // reached from commodity production sites
    private void dropWorkerKyleTheScavenger(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerKyleTheScavenger");
		if((p.knowledge<=4)
				&& (countWorkers(getProducerArray(dest.allegiance))==1)
				&& p.recruitAppliesToMe(RecruitChip.KyleTheScavenger))
		{	// if we can afford to gain 2 knowledge and have KyleTheScavenger
		setContinuation(new Continuation(RecruitChip.KyleTheScavenger,dest,Function.DoKyleTheScavenger,Function.DropWorkerJonathanTheArtistFarmer));
		}
		else 
		{
			dropWorkerJonathanTheArtistFarmer(dest,replay);	// don't need to consider kyle
		}
    }
    private void doKyleTheScavenger(EuphoriaCell dest,replayMode replay)
    {	addToFinalPath("doKyleTheScavenge");

    	EPlayer p = players[whoseTurn];
    	p.incrementKnowledge(replay);
    	p.incrementKnowledge(replay);
    	p.collectBenefit(Benefit.Artifact,replay);
    	logGameEvent(KyleTheScavengerEffect);
    	doCardsGained(p,replay,dest,Function.DropWorkerJonathanTheArtistFarmer);
     }
    
    // reached from any commodity site
    private void dropWorkerJonathanTheArtistFarmer(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJonathanTheArtist");
		if((revision<114) 	// mistaken implementation fixed by 114
				&& p.recruitAppliesToMe(RecruitChip.JonathanTheArtist)
    			&& p.canPay(Cost.Knowledgex2))
    	{
    	WorkerChip highest = highestKnowledge(producerArray[dest.allegiance.ordinal()]);
    	if(highest==dest.topChip())
    		{
    			setContinuation(new Continuation(RecruitChip.JonathanTheArtist,dest,
    					Function.DoJonathanTheArtistFarmer,Function.DropWorkerScarbyTheHarvester));
    			return;
    		}
    	}
    	dropWorkerScarbyTheHarvester(dest,replay); 
    }
    // mistaken implementation
    private void doJonathanTheArtistFarmer(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doJohnathanTheArtistFarmer");
		G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment must succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"benefit must succeed");
    	logGameEvent(JonathanTheArtistEffect,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DropWorkerScarbyTheHarvester);
     }

    private void doPeteTheCannibalBenefit(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doPeteTheCannibleBenefit");
		G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment must succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"benefit must succeed");
    	logGameEvent(PeteTheCannibalNewWorker,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.DoSheppardTheLobotomistBenefit);
    }
    
    // reached from worker recruit sites
    private void doSheppardTheLobotomistBenefit(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doShepphardTheLobotomistBenefit");
		if(p.hasGainedWorker()
				&& p.canPay(Cost.Moralex2)
				&& p.recruitAppliesToMe(RecruitChip.SheppardTheLobotomist))
		{
			setContinuation(new Continuation(RecruitChip.SheppardTheLobotomist,dest,Function.DoSheppardTheLobotomistMorale,Function.Return));
		}
    }
    
    // reached from market placements
    private void dropWorkerBradlyTheFuturist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerBradlyTheFuturist");
		if(p.recruitAppliesToMe(RecruitChip.BradlyTheFuturist)
    			&& p.canPay(Cost.Knowledgex2))
		{
			// can sacrifice this worker for an extra *
			// nakagawathetribute_v2 is completely different.
			setContinuation(new Continuation(RecruitChip.BradlyTheFuturist,dest,
						Function.DoBradlyTheFuturist,Function.DropWorkerNakagawaTheTribute));
    		
		}
    	else { dropWorkerNakagawaTheTribute(dest,replay); }
    }
    private void doBradlyTheFuturist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doBradlyTheFuturist");
    	G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment must succeed");
    	marketBasket.reInit();	// just in case
    	marketBasket.addChip(getArtifact());
    	marketBasket.addChip(getArtifact());
    	logGameEvent(BradleyTheFuturistEffect,currentPlayerColor());
    	if(replay!=replayMode.Replay)
    	{
    		animatePlacedItem(unusedArtifacts,marketBasket);
    		animatePlacedItem(unusedArtifacts,marketBasket);
    	}
    	setContinuation(new Continuation(Benefit.Artifactx2for1,Benefit.Artifactx2for1.collectionState(),dest,
    						Function.DropWorkerNakagawaTheTribute));   			
    }
    boolean recruitAppliesToCurrentPlayer(RecruitChip ch)
    {
    	return(players[whoseTurn].recruitAppliesToMe(ch));
    }
    // reached from market placements
    private void dropWorkerNakagawaTheTribute(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerNakagawaTheTribute");
		// always continue at flavio the merchant		
		if(p.recruitAppliesToMe(RecruitChip.NakagawaTheTribute) 
				&& (p.authorityTokensRemaining()>0)
				&& (p.totalWorkers>1)
				&& canPlaceAuthorityToken(p,dest)
				&& getAvailableAuthorityCell(dest.allegiance)!=null)
			{
			// can sacrifice this worker for an extra *
			setContinuation(new Continuation(RecruitChip.NakagawaTheTribute,dest,
						Function.DoNakagawaTheTribute,Function.DropWorkerFlavioTheMerchant));
			}
		else { dropWorkerFlavioTheMerchant(dest,replay); }
    }

    private boolean hasHighestKnowledge(EuphoriaCell al[],EuphoriaCell dest)
    {	int know = dest.topChip().knowledge();
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
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerScarbyTheHarvester");
		
		if((dest.rackLocation()==EuphoriaId.WastelanderFarm)
    			&& p.recruitAppliesToMe(RecruitChip.ScarbyTheHarvester)
    			&& hasHighestKnowledge(wastelanderFarm,dest))
    	{
    		if(!p.collectBenefit(Benefit.KnowledgeOrFood,replay))
    		{
    			setContinuation(new Continuation(Benefit.KnowledgeOrFood,Benefit.KnowledgeOrFood.collectionState(),
    					Function.Return));
    			return;
    		}
    		else { logGameEvent(ScarbyTheHarvesterFood); }
    	}
    }
    
    private void doNakagawaTheTribute(EuphoriaCell dest,replayMode replay)
    {  	// sacrifice the worker, gain an extra authority token.
		addToFinalPath("doNakagawaTheTribute");
		EPlayer p = players[whoseTurn];
    	Benefit bene = dest.placementBenefit;
    	Benefit extra = bene.extraStar();
		checkBrettTheLockPicker(dest,replay);
		checkEsmeTheFireman(dest,replay);	// trade card for stuff
    	p.sacrificeWorker(dest,replay);
     	logGameEvent(NakagawaTheTributeEffect);
     	if(!p.collectBenefit(extra,replay))
     	{	if(revision>=106)
     		{
     		throw G.Error("Nakagawa didn't succeed");
     		}
     	}
	 }
    

    // reached from non-icarite market pacements
    private void dropWorkerFlavioTheMerchant(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerFlavioTheMerchant");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color)
    			&& p.canPay(Cost.Moralex2)
    			&& p.recruitAppliesToMe(RecruitChip.FlavioTheMerchant) )
			{
    		// can sacrifice this worker for an extra *
    		setContinuation(new Continuation(RecruitChip.FlavioTheMerchant,dest,
					Function.DoFlavioTheMerchant,Function.DropWorkerJackoTheArchivist));
			return;
			}
    	 if(p.canPay(Cost.Knowledge)
    			 && p.recruitAppliesToMe(RecruitChip.FlavioTheMerchant_V2))
    	 {
    	   		// can sacrifice this worker for an extra *
     		setContinuation(new Continuation(RecruitChip.FlavioTheMerchant_V2,dest,
 					Function.DoFlavioTheMerchant,Function.DropWorkerJackoTheArchivist));
     		return;
    	 }
 
    	 dropWorkerJackoTheArchivist(dest,replay); 
    }
    
    // pay 2 morale
    private void doFlavioTheMerchant(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	addToFinalPath("doFlavioTheMerchant");
    	if(active==RecruitChip.FlavioTheMerchant)
    	{
    	if(doLoseMorale(players[whoseTurn],2,replay,dest,Function.DoFlavioTheMerchantGainCard))
    		{ doFlavioTheMerchantGainCard(dest,replay); 
    		  logGameEvent(FlavioTheMerchantEffect,currentPlayerColor());
    		}
    	}
    	else
    	{	EPlayer p = players[whoseTurn];
    		G.Assert(p.payCost(Cost.Knowledge,replay),"payment must succeed");
    		doFlavioTheMerchantGainCard(dest,replay); 
    		logGameEvent(FlavioTheMerchantEffect,currentPlayerColor());
    	}
    }
    // gain a card
    private void doFlavioTheMerchantGainCard(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doFlavioTheMerchantCard");
    	p.collectBenefit(Benefit.Artifact,replay);
    	doCardsGained(p,replay,dest,Function.DropWorkerJackoTheArchivist);
    }
    
    // jacko lets you place 2 stars for an additional 2 cards.
    // this is v1 - in v2 this power doesn't exist
    private void dropWorkerJackoTheArchivist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJackoTheArchivist");
    	if(p.recruitAppliesToMe(RecruitChip.JackoTheArchivist)
    			&& p.canPay(Cost.Artifactx2)
    			&& canPlaceAuthorityToken(p,dest))
    	{	
    		setContinuation(new Continuation(RecruitChip.JackoTheArchivist,dest,
    					Function.DoJackoTheArchivist,Function.Return));
     	}
    }
    
    // pay the cost and collect the benefit
    private void doJackoTheArchivist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doJackoTheArchivist");
		
		if(!p.payCost(Cost.Artifactx2,replay))
    	{
    		setContinuation(new Continuation(Cost.Artifactx2,Cost.Artifactx2.paymentState(),dest,Function.DoJackoTheArchivistStar));
    	}
    	else { doJackoTheArchivistStar(dest,replay); }
    }
    
    private void checkEsmeTheFireman(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	boolean isEsmev1 = false;
    	if((p.artifacts.height()>0)
			&& ((isEsmev1 = p.recruitAppliesToMe(RecruitChip.EsmeTheFireman))
					|| p.recruitAppliesToMe(RecruitChip.EsmeTheFireman_V2)))
		{	RecruitChip recruit = (isEsmev1 ? RecruitChip.EsmeTheFireman : RecruitChip.EsmeTheFireman_V2);
			setContinuation(new Continuation(recruit,dest,Function.DoEsmeTheFireman,Function.Return));
		}
    }
    private void doEsmeTheFireman(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(!p.payCost(Cost.Artifact,replay))
    	{
    		setContinuation(new Continuation(Cost.Artifact,Cost.Artifact.paymentState(),dest,active,Function.DoEsmeTheFiremanPaid));
    	}
    	else { doEsmeTheFiremanPaid(dest,active,replay); }
    }
    private void doEsmeTheFiremanPaid(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	boolean paydouble = (active==RecruitChip.EsmeTheFireman_V2) 
    							&& (usedArtifacts.topChip()==ArtifactChip.Book);
    	p.collectBenefit(paydouble ? Benefit.Energyx2 : Benefit.Energy,replay);
    	marketBasket.addChip(RecruitChip.getMorale(p.color));
    	marketBasket.addChip(RecruitChip.getKnowledge(p.color));
     	G.Assert(droppedDestStack.size()==0,"empty");
     	Benefit bene = paydouble
     						? ((revision>=121) ? Benefit.Moralex2AndKnowledgex2 : Benefit.Moralex2OrKnowledgex2)
     						:Benefit.MoraleOrKnowledge;
    	if(!p.collectBenefit(bene,replay))
    	{
    		setContinuation(new Continuation(bene,bene.collectionState(),Function.Return));
    	} 
    }
    // actually collect the benefit
    private void doJackoTheArchivistStar(EuphoriaCell dest,replayMode replay)
    {	Benefit bene = dest.placementBenefit;
    	Benefit extra = bene.extraStar();
    	EPlayer p = players[whoseTurn];
		addToFinalPath("doJackoTheArchivistStar");
		logGameEvent(JackoTheArchivistEffect);
    	checkEsmeTheFireman(dest,replay);
    	if(!p.collectBenefit(extra,replay))	// if we can't collect the benefit directly, schedule an interaction
    		{	
    		setContinuation(new Continuation(bene,bene.collectionState(),dest,Function.Return));
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
    {	if(cont!=null) { setContinuation(new Continuation(dest,cont),p.boardIndex); }
     	if(p.hasAddedArtifact())
     	{

     		if(!p.hasUsedJonathanTheGambler
     			&& p.recruitAppliesToMe(RecruitChip.JonathanTheGambler_V2))
     		{
      		addToFinalPath("Planning DoJonathanTheGambler");
          	p.hasUsedJonathanTheGambler = true;
    		setContinuation((dest==null) 
					? new Continuation(RecruitChip.JonathanTheGambler_V2,Function.DoJonathanTheGambler,Function.MoraleCheck)
					: new Continuation(RecruitChip.JonathanTheGambler_V2,dest,Function.DoJonathanTheGambler,Function.MoraleCheck));
     		}
     		else if(!p.hasUsedJonathanTheGambler
     				&& p.recruitAppliesToMe(RecruitChip.JonathanTheGambler)
     		
     			&& p.canPay(Cost.Knowledge))
    		{	
              	p.hasUsedJonathanTheGambler = true;
         		addToFinalPath("Planninning DoJonathanTheGambler");
    			setContinuation((dest==null) 
    					? new Continuation(RecruitChip.JonathanTheGambler,Function.DoJonathanTheGambler,Function.MoraleCheck)
    					: new Continuation(RecruitChip.JonathanTheGambler,dest,Function.DoJonathanTheGambler,Function.MoraleCheck));
    		}
    		else { doMoraleCheck(p,replay,dest,Function.Return);
    		}
    	}
     	
    }

    private void doJonathanTheGambler(EuphoriaCell dest,RecruitChip active,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(active==RecruitChip.JonathanTheGambler_V2)
    	{	p.collectBenefit(Benefit.Artifact,replay);
    		logGameEvent(JonathanGain2);
    		if(p.hasArtifactPair()!=null)
    		{	// no pairs, we get to keep it
    			for(EPlayer op : players)
    			{
    				if(op!=p)
    				{
    					op.addArtifact(getArtifact());
    					logGameEvent(JonathanTheGamblerBonus,op.color.toString());
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
    			    usedArtifacts.addChip(t3);
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
    		usedArtifacts.addChip(t2);
  		  	logGameEvent(Gained2Cards);
	    		if(replay!=replayMode.Replay)
	    		{
	  		  	  animateReturnArtifact(p.artifacts);
	    		}
    		}
    	else if(t2==t3)
    		{ p.addArtifact(t2);
    		  p.addArtifact(t3); 
    		  usedArtifacts.addChip(top);
    		  logGameEvent(Gained2Cards);
    		  if(replay!=replayMode.Replay)
    		  	{ 
    		  	  animateReturnArtifact(p.artifacts);
    		  	}
    		  }
    		  else { 
    			  usedArtifacts.addChip(top);
    			  usedArtifacts.addChip(t2);
    			  usedArtifacts.addChip(t3);
    			  logGameEvent(Gained0Cards);
    			  if(replay!=replayMode.Replay) 
    			  	{  
    			  	  animateReturnArtifact(p.artifacts);
      			  	  animateReturnArtifact(p.artifacts);
    			  	  animateReturnArtifact(p.artifacts);
    			  	}
    		  }
    	}
    	else { throw G.Error("not expecting %s",active); }
    	doMoraleCheck(p,replay,dest,Function.Return);	// trigger a morale check
    }
    private void doSheppardTheLobotomistMorale(EuphoriaCell dest,replayMode replay)
    {	
    	if(doLoseMorale(players[whoseTurn],2,replay,dest,Function.DoSheppardTheLobotomistGainCard))
    		{ doSheppardTheLobotomistGainCard(dest,replay); 
    		}
    }
    
    private void doSheppardTheLobotomistGainCard(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	p.collectBenefit(Benefit.Artifact,replay);
    	logGameEvent(SheppardTheLobotomistRoll,currentPlayerColor());
    	doCardsGained(p,replay,dest,Function.Return);
    }
    
    /** julia the thought inspector trades morale for a resource 
     * NOT reached from commodity production sites, reached from all other worker placements
      * */
    private void dropWorkerJuliaTheThoughtInspector(replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("dropWorkerJuliaTheThoughtInspector");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color))
    	{	if(p.recruitAppliesToMe(RecruitChip.JuliaTheThoughtInspector)
    			&& (bumpedWorker.knowledge()==bumpingWorker.knowledge())
    			&& p.canPay(Cost.Moralex2))
    		{
    		setContinuation(new Continuation(RecruitChip.JuliaTheThoughtInspector,
    											Function.DoJuliaTheThoughtInspector,
    											Function.DropWorkerRebeccaThePeddler));
    		return;
    		}
    	}
    	dropWorkerRebeccaThePeddler(replay); 
    }
    private void doJuliaTheThoughtInspector(replayMode replay)
    {	addToFinalPath("doWorkerJuliaTheThoughtInspector");

    	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.DropWorkerRebeccaThePeddler));

    	logGameEvent(JuliaTheThoughtInspectorEffect,currentPlayerColor());
     	doLoseMorale(players[whoseTurn],2,replay,null,Function.Return);
   	}

   private void doJuliaTheThoughtInspector_V2(replayMode replay)
   {	addToFinalPath("doWorkerJuliaTheThoughtInspector+v2");
		logGameEvent(JuliaTheThoughtInspectorV2Effect,currentPlayerColor());
		selectedDieRoll = WorkerChip.getWorker(bumpedWorker.color,bumpingWorker.knowledge());
		logGameEvent(JuliaTheThoughtInspectorV2RollEffect,s.get(bumpedWorker.color.name()),""+selectedDieRoll.knowledge());
      	doLoseMorale(players[whoseTurn],1,replay,null,Function.Return);
     	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.Return));
       	reRollWorkers(getPlayer(selectedDieRoll.color),replay,null,selectedDieRoll,Function.Return);
    }
    
    // reached from all worker placements except commodity production
    private void dropWorkerRebeccaThePeddler(replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerRebeccaThePeddler");
    	if((bumpedWorker!=null)
    			&& (bumpedWorker.color!=p.color)
    			&& p.canPay(Cost.Moralex2)
    			&& p.recruitAppliesToMe(RecruitChip.RebeccaThePeddler) )
		{
		// can sacrifice this worker for an extra *
		setContinuation(new Continuation(RecruitChip.RebeccaThePeddler,
					Function.DoRebeccaThePeddler,Function.DropWorkerPhilTheSpy));
		}
    	else { dropWorkerPhilTheSpy(replay); }
    	
    }
    void doRebeccaThePeddler(replayMode replay)
    {	EPlayer other = getPlayer(bumpedWorker.color);
    	other.incrementKnowledge(replay);
    	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),Function.DropWorkerPhilTheSpy));
    	
    	logGameEvent(RebeccaThePeddlerOtherEffect,other.color.name());
    	logGameEvent(RebeccaThePeddlerEffect,currentPlayerColor());
    	doLoseMorale(players[whoseTurn],2,replay,null,Function.Return);
     }
    
    // reached from all worker placements except commodity production
    private void dropWorkerPhilTheSpy(replayMode replay)
    {
    	EPlayer p = players[whoseTurn];
    	boolean recruitIsPhil = false;
    	addToFinalPath("dropWorkerPhilTheSpy");
    	if((bumpedWorker!=null) 
    			&& !getPlayer(bumpedWorker.color).hasActiveRecruit(Allegiance.Subterran)
       			&& ((recruitIsPhil = p.recruitAppliesToMe(RecruitChip.PhilTheSpy)) 
       				|| p.recruitAppliesToMe(RecruitChip.PhilTheSpy_V2))
    			&& p.canPay(recruitIsPhil?Cost.Knowledgex2:Cost.Knowledge)
    			)
		{	// pay 2 morale and gain a card
    		// v2 is the same except pay 1
		setContinuation(new Continuation( recruitIsPhil?RecruitChip.PhilTheSpy:RecruitChip.PhilTheSpy_V2,
						Function.DoPhilTheSpy,	
						Function.DropWorkerLeeTheGossip));
		}
		else { 	dropWorkerLeeTheGossip(replay); }
    }
    
    private void doPhilTheSpy(replayMode replay,RecruitChip active)
    {	EPlayer p = players[whoseTurn];
    	G.Assert(p.payCost(active==RecruitChip.PhilTheSpy?Cost.Knowledgex2:Cost.Knowledge,replay),"payment must succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"benefit must succeed");
    	logGameEvent(PhilTheSpyEffect,currentPlayerColor());
    	doCardsGained(p,replay,null,Function.DropWorkerLeeTheGossip);
     }
    // reached from all worker placements except commodity production
    private void dropWorkerLeeTheGossip(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerLeeTheGossip");
    		if((bumpedWorker!=null) 
    			&& (bumpedWorker.color!=p.color) 
    			&& p.recruitAppliesToMe(RecruitChip.LeeTheGossip) 
    			&& p.canPay(Cost.Morale))
		{	// pay 2 morale and gain a card
		setContinuation(new Continuation( RecruitChip.LeeTheGossip, Function.DoLeeTheGossip,	Function.DropWorkerMaximeTheAmbassador));
		}
		else { 	dropWorkerMaximeTheAmbassador(replay); }
    }
    private void doLeeTheGossip(replayMode replay)
    {	EPlayer other = getPlayer(bumpedWorker.color);
    	other.incrementKnowledge(replay);
       	setContinuation(new Continuation(Benefit.Commodity,Benefit.Commodity.collectionState(),Function.DropWorkerMaximeTheAmbassador));
       	doLoseMorale(players[whoseTurn],1,replay,null,Function.Return);
       	logGameEvent(LeeTheGossipOtherEffect,other.color.name());
       	logGameEvent(LeeTheGossipEffect,currentPlayerColor());
    }

    // reached from all worker placements except commodity production
    private void dropWorkerMaximeTheAmbassador(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	addToFinalPath("dropWorkerMaximeTheAmbassador");
   	
    	if((bumpedWorker!=null) 
    			&& (bumpedWorker.color!=p.color) 
    			&& p.recruitAppliesToMe(RecruitChip.MaximeTheAmbassador) 
    			&& p.canPay(Cost.Moralex2))
		{	// pay 2 morale and gain a card
		setContinuation(new Continuation( RecruitChip.MaximeTheAmbassador, Function.DoMaximeTheAmbassador,	Function.Return));
		}
    }

    // 
    // do "Maxime the Ambassador
    // lose 2 morale, gain 1 card, the other player gains 1 knowlege
    //
    public void doMaximeTheAmbassador(replayMode replay)
    {	EPlayer other = getPlayer(bumpedWorker.color);
	    other.incrementKnowledge(replay);			// other player gets knowledge
	    logGameEvent(MaximeTheAmbassadorOther,other.color.name());
	    logGameEvent(MaximeTheAmbassadorEffect,currentPlayerColor());
	    
    	if(doLoseMorale(players[whoseTurn],2,replay,null,Function.DoMaximeTheAmbassadorGainCard))
    	{
    		doMaximeTheAmbassadorGainCard(replay);
    	}
    }
    private void doMaximeTheAmbassadorGainCard(replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	// technically maybe have to do 2 morale checks, one after the decrements,
    	p.collectBenefit(Benefit.Artifact,replay);
    	doCardsGained(p,replay,null,null);
    }
    
    // reached after all worker placements
    public void dropWorkerOpenMarkets(replayMode replay)
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
      	proceedWithTheGame(replay);
    }
    
    public void dropWorker(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	finalPath.clear();
    	addToFinalPath("dropWorker");
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

    Cost adjustCostForDestAndRecruits(EPlayer p,EuphoriaCell dest,boolean placed,boolean uncond)
    {	EuphoriaId rack = dest.rackLocation();
    	Cost cost = dest.placementCost;
    	if((dest.height()>(placed ? 1 : 0)) 
    			&&  (uncond || (dest.chipAtIndex(0).color!=p.color)))
    	{
    		if(p.recruitAppliesToMe(RecruitChip.MatthewTheThief))
	    	{ 	
	    		switch(rack)
	    		{
	    		case EuphorianTunnelMouth:
	    			cost = Cost.EnergyForMatthewTheThief;
	    			break;
	    		case WastelanderTunnelMouth:
	    			cost = Cost.FoodForMatthewTheThief;
	    			break;
	    		case SubterranTunnelMouth:
	    			cost = Cost.WaterForMatthewTheThief;
	    			break;
	    		default: break;
	    		}
	    	}
    	}
     	if((rack==EuphoriaId.WastelanderUseMarket)
     			&& ((revision>117) || p.canPay(Cost.Knowledgex2))
     			&& p.recruitAppliesToMe(RecruitChip.JackoTheArchivist_V2))
     		{
     		cost = Cost.ArtifactJackoTheArchivist_V2;
     		}
    	return(cost);
    }
   
       	
    private void dropWorkerPay(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
       	Cost originalcost = dest.placementCost;
       	Cost adjustedCost = adjustCostForDestAndRecruits(p,dest,true,revision<108);
     	//
    	// payCost considers the alternate capabilities of the recruits
    	//
    	G.Assert(p.canPay(adjustedCost),"must be able");

    	addToFinalPath("dropWorkerPay/open market continuation");

    	setContinuation(new Continuation(null,Function.DropWorkerOpenMarkets));
    	
    	switch(adjustedCost)
    	{
    	case Mostly_Free:
    	case Mostly_Artifactx3:
    	case Mostly_Resourcex3:
    	case Mostly_Bliss_Commodity:
    		if(p.recruitAppliesToMe(RecruitChip.BrianTheViticulturist_V2))
    			{ logGameEvent(BrianTheViticulturistMorale,currentPlayerColor());}
    		break;
    	default: break;
    	}
    	if(!p.payCost(adjustedCost,replay))
    	{	Cost cost = p.alternateCostWithRecruits(adjustedCost);
    		// extract the fixed part of the cost, change the cost to just the remainder
    		switch(cost)
    		{
    		case Morale_Artifactx3:
    			// unusual case for Brian the Viticulturist V2, we have to lose a card before paying 3 cards (or a pair)
    			setContinuation(new Continuation(originalcost,Cost.Artifactx3,Cost.Artifactx3.paymentState(),dest,
        							Function.DropWorkerWithoutPayment,Function.DropWorkerBump));
    			if(p.canPay(Cost.Smart_Artifact))
    			{	// special case, we have to be sure to discard the right card
    				// discard the card first, then extract the morale penalty which will not interact
    				logGameEvent(BrianTheVitculturistLoseCard);
    				G.Assert(p.payCost(Cost.Smart_Artifact,replay),"must succeed");
    			}
    			// otherwise, the normal interaction might ensue when we lose morale
    			doLoseMorale(p,1,replay,dest,Function.Return);
    			return;
    		case Bliss_Commodity:
    		case Mostly_Bliss_Commodity:
    				G.Assert(p.payCost(Cost.Bliss,replay),"payment must succeed");
    				cost = Cost.NonBlissCommodity;
    				break;
    		case Blissx4_ResourceOrBliss:
	    			G.Assert(p.payCost(Cost.Blissx4,replay),"payment must succeed");
	    			cost = Cost.ResourceOrBliss;
	    			break;
    		case Blissx4_Resource:	
    				G.Assert(p.payCost(Cost.Blissx4,replay),"payment must succeed");
    				cost = Cost.Resource;
    				break;
    		case Energyx4_StoneOrBliss:
    				G.Assert(p.payCost(Cost.Energyx4,replay),"payment must succeed");
    		 		cost = Cost.StoneOrBliss;
    		 		break;
    		case Foodx4_Card:
					G.Assert(p.payCost(Cost.Foodx4,replay),"payment must succeed");
					cost = Cost.Artifact;
					break;
    		case Energyx4_Card:
    				if(revision>=115)
    					{// the absence of this case reduced the cost to just the card
    					 G.Assert(p.payCost(Cost.Energyx4,replay),"payment must succeed");
    					 cost = Cost.Artifact;
    					}
    				break;
    		case Blissx4_Card:
					G.Assert(p.payCost(Cost.Blissx4,replay),"payment must succeed");
					cost = Cost.Artifact;
					break;
    		 case Waterx4_ClayOrBliss:
    				G.Assert(p.payCost(Cost.Waterx4,replay),"payment must succeed");
    		 		cost = Cost.ClayOrBliss;
    		 		break;
    		 case Waterx4_Card:
    			 	G.Assert(p.payCost(Cost.Waterx4,replay),"payment must succeed");
    			 	cost = Cost.Artifact;
    			 	break;
    		 case Waterx4_GoldOrBliss:
    				G.Assert(p.payCost(Cost.Waterx4,replay),"payment must succeed");
    		 		cost = Cost.GoldOrBliss;
    		 		break;
    		 case Energyx4_ClayOrBliss:
	 				G.Assert(p.payCost(Cost.Energyx4,replay),"payment must succeed");
			 		cost = Cost.ClayOrBliss;
			 		break;
	   			 	
    		 case Foodx4_StoneOrBliss:
	 				G.Assert(p.payCost(Cost.Foodx4,replay),"payment must succeed");
			 		cost = Cost.StoneOrBliss;
			 		break;
	   			 	
			 		// interactions between JoshTheNegotiator and Brian the Viticulturist
      		 case Energyx4_StoneOrBlissOrFood:
    			 	G.Assert(p.payCost(Cost.Energyx4,replay),"payment must succeed");
    			 	cost = Cost.StoneOrFoodOrBliss;
    			 	break;
    		 case Waterx4_ClayOrBlissOrFood:
    			 	G.Assert(p.payCost(Cost.Waterx4,replay),"payment must succeed");
    			 	cost = Cost.ClayOrFoodOrBliss;
    			 	break;
    		 case Waterx4_GoldOrBlissOrFood:
    			 	G.Assert(p.payCost(Cost.Waterx4,replay),"payment must succeed");
    			 	cost = Cost.GoldOrFoodOrBliss;
    			 	break;
    		 case Energyx4_ClayOrBlissOrFood:
    			 	G.Assert(p.payCost(Cost.Energyx4,replay),"payment must succeed");
    			 	cost = Cost.ClayOrFoodOrBliss;
    			 	break;
    		 case Foodx4_StoneOrBlissOrFood:
    			 	G.Assert(p.payCost(Cost.Foodx4,replay),"payment must succeed");
    			 	cost = Cost.StoneOrFoodOrBliss;
    			 	break;
    		 case Commodity_Balloons:
    			 	G.Assert(p.payCost(Cost.Balloons,replay),"payment must succeed");
    			 	cost = Cost.Commodity;
    			 	break;
    		 case Commodity_Bat:
 			 	G.Assert(p.payCost(Cost.Bat,replay),"payment must succeed");
 			 	cost = Cost.Commodity;
 			 	break;
    		 case Commodity_Book:
 			 	G.Assert(p.payCost(Cost.Book,replay),"payment must succeed");
 			 	cost = Cost.Commodity;
 			 	break;
    		 case Commodity_Bear:
 			 	G.Assert(p.payCost(Cost.Bear,replay),"payment must succeed");
 			 	cost = Cost.Commodity;
 			 	break;
    		 case Commodity_Bifocals:
 			 	G.Assert(p.payCost(Cost.Bifocals,replay),"payment must succeed");
 			 	cost = Cost.Commodity;
 			 	break;
    		 case Commodity_Box:
 			 	G.Assert(p.payCost(Cost.Box,replay),"payment must succeed");
 			 	cost = Cost.Commodity;
 			 	break;
			default:
				break;
    			 	
      		}
    		setContinuation(new Continuation(originalcost,cost,cost.paymentState(),dest,
    				Function.DropWorkerWithoutPayment,Function.DropWorkerBump));
     	}
    	else 
    	{   
     		if(p.recruitAppliesToMe(RecruitChip.KadanTheInfiltrator)
    		   && (p.alternateCostForKadanTheInfiltrator(adjustedCost)==Cost.Knowledgex2))
			{
    		// we used kadan to make this placement
			logGameEvent(KadanTheInfiltratorEffect,currentPlayerColor());
			}
    	dropWorkerBump(dest,replay); 

    	if(adjustedCost==Cost.ArtifactJackoTheArchivist_V2)
    		{
    			finishJackoTheArchivist(players[whoseTurn],replay);
    		}

    	}
    	doMoraleCheck(p,replay,dest,Function.Return);
    }
    private void dropWorkerWithoutPayment(EuphoriaCell dest,replayMode replay)
    {	// this is the unusual case where a payment for placement is optional
    	addToFinalPath("dropWorkerWithoutPayment");
    	EPlayer p = players[whoseTurn];
    	switch(dest.placementCost)
    	{
    	case Water:
    	case Food:
     	case Energy:
     		G.Assert(p.knowledge<MAX_KNOWLEDGE_TRACK,"should be able to gain knowledge");
    		p.incrementKnowledge(replay);
    
    		logGameEvent(MatthewTheThiefEffect,dest.placementCost.name(),currentPlayerColor());
    		break;
    	default: throw G.Error("not expecting %s",dest.placementCost);
    	}
    	
    	
    	dropWorkerBump(dest,replay);
    }
    

    private void dropWorkerBump(EuphoriaCell dest,replayMode replay)
    {	EuphoriaId rack = dest.rackLocation();
    	addToFinalPath("dropWorkerBump");
    	G.Assert(dest!=null,"dest shouldn't be null here");
    	
    	setContinuation(new Continuation(dest,Function.DropWorkerAfterBump));
    	
    	if(rack.canBeBumped && (dest.height()>1))
    	{	// make the worker space empty if appropriate
    		bumpWorker(dest,replay);
    	}
    	else 
    	{	// placing without a bump
			EPlayer p = players[whoseTurn];
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
    			if(p.canPay(Cost.Knowledgex2)
    					&& p.recruitAppliesToMe(RecruitChip.ReitzTheArcheologist))
    					{
    					setContinuation(new Continuation(RecruitChip.ReitzTheArcheologist,dest,
    							Function.DoReitzTheArcheologist,Function.Return));
    					
    					return;
    					
    					}
    			}

				break;
			default: break;
    		}    	
    	}
    }
    private void doNakagawaTheTribute_V2(EuphoriaCell dest,replayMode replay)
    {	// unrelated to the v1 NakagawaTheTribute
    	incrementAllegiance(Allegiance.Euphorian,replay);
    }
    private void checkBrettTheLockPicker(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
    	if(bumpedWorker==null)
    	{
    	if(p.canPay(Cost.Knowledgex2)
				&& p.recruitAppliesToMe(RecruitChip.BrettTheLockPicker)
				)
  			{
				setContinuation(new Continuation(RecruitChip.BrettTheLockPicker,dest,
						Function.DoBrettTheLockPicker,Function.Return));
			}
    	if(p.canPay(Cost.Knowledge)
			&& p.recruitAppliesToMe(RecruitChip.BrettTheLockPicker_V2)
			)
  			{
				setContinuation(new Continuation(RecruitChip.BrettTheLockPicker_V2,dest,
						Function.DoBrettTheLockPicker,Function.Return));
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
    	setContinuation(new Continuation(Benefit.Resource,Benefit.Resource.collectionState(),dest,normalContinuation));
    }
    private void doReitzTheArcheologist(EuphoriaCell dest,replayMode replay)
    {	EPlayer p = players[whoseTurn];
		addToFinalPath("doReitzTheArcheologist");
		G.Assert(p.payCost(Cost.Knowledgex2,replay),"payment has to succeed");
    	G.Assert(p.collectBenefit(Benefit.Artifact,replay),"collect has to succeed");
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
    public void dropWorkerAfterBump(EuphoriaCell dest,replayMode replay)
    {	
    	EuphoriaId rack = dest.rackLocation();
		addToFinalPath("dropWorkerAfterBump");
     	G.Assert(rack.isStackable || (dest.height()==1),"can place worker on %s",dest);
     	G.Assert(whoseTurn==currentPlayerInTurnOrder,"unexpected turn change");
    	acceptPlacement();

    	dropWorkerCollectBenefit(dest,replay);
    }
    
    //
    // opening markets
    //
    boolean marketIsOpen(int idx) { return(marketIsOpen(markets[idx])); }	// open if we've removed the cover
    boolean marketIsOpen(EuphoriaCell c)   { return(c.topChip().isAuthorityMarker());   }
    
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
    private void openMarket(int idx,replayMode replay)
    {	revealedNewInformation = openedAMarket = true;
		addToFinalPath("openmarket");
		// remove the cap from the market, revealing the market underneath
    	markets[idx].removeTop();
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
    			{	EuphoriaChip ch = p.getAuthorityToken();
    				if(ch!=null)
    				{markets[idx].addChip(ch);
    				p.marketStars++;
    				if(replay!=replayMode.Replay) { animatePlacedItem(p.authority,markets[idx]); }
    				}
    				else
    				{	// this is not an error any more. Nakagawathetribute_v2
    					// can increment allegiance, which gives away some stars.
    					//G.Error("Unexpected - no token available when opening a market");
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
    				p.incrementMorale();
    				p.incrementMorale();
    				logGameEvent(RayTheForemanHelped,currentPlayerColor());
    			}
    			else 
    			{
    			p.decrementKnowledge();
    			p.decrementKnowledge(); 
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
    public boolean Execute(commonMove mm,replayMode replay)
    {	EuphoriaMovespec m = (EuphoriaMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); gameEvents.clear(); }
        if(board_state==EuphoriaState.Puzzle)
        {
        	setStatusDisplays();
        }
        //if(!robotBoard) { G.print("A "+m+" for "+whoseTurn+" "+board_state+" #"+moveNumber+" "+Digest()); }
       // G.print("E "+m+" for "+whoseTurn+" "+board_state+" "+Digest()); 
        switch (m.op)
        {
        case MOVE_PEEK:
        	setHasPeeked(m.player,true);
        	break;
        case EPHEMERAL_CONFIRM_RECRUITS:
		case EPHEMERAL_CONFIRM_ONE_RECRUIT:
       		{
        	EPlayer p = getPlayer(m.from_color);
        	m.player = p.boardIndex;
  			p.discardNewRecruits(true);
  			moveNumber++; 
   			SIMULTANEOUS_PLAY = true;	// replay of the final NormalStart turns this off
   			boolean ready = true;
   			for(EPlayer pl : players) { ready &= pl.hasReducedRecruits(); }
   			if(ready) { setState(EuphoriaState.NormalStart); }
   			else { setState(EuphoriaState.EphemeralChooseRecruits); }   
       		}
       		break;
        case CONFIRM_RECRUITS:
        case MOVE_DONE:
        	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
        	stepNumber++;
        	doDone(replay);
        	break;
        case NORMALSTART:
        	if(!hasReducedRecruits) { moveNumber--; doDone(replay); }
           	REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = false;
       		for(EPlayer p : players) { p.transferDiscardedRecruits(usedRecruits); }
           	normalStartSeen = true;
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
        case USE_RECRUIT_OPTION:
        	switch(board_state)
        	{
        	case RecruitOption:	setState(EuphoriaState.ConfirmRecruitOption);
        			activeRecruit = m.chip;
        			break;
        	case ConfirmRecruitOption: 
        			activeRecruit = null;
        			setState(EuphoriaState.RecruitOption); break;
        	default: throw G.Error("Not expecting state %s",board_state);
        	}
        	break;
        case FIGHT_THE_OPRESSOR:
        	{
        	EPlayer p = players[m.player];
        	DilemmaChip dilemma = (DilemmaChip)p.dilemma.chipAtIndex(0);
        	Cost cost = dilemma.cost;
        	if(p.payCost(cost,replay)) 
        		{ 
        		  setState(EuphoriaState.ConfirmFightTheOpressor); 
        		}
        	else 
        		{
        		setContinuation(new Continuation(cost,EuphoriaState.FightTheOpressor,Function.FightTheOpressor));
        		}
        	}
        	break;
        case JOIN_THE_ESTABLISHMENT:
       		{
        	EPlayer p = players[m.player];
        	DilemmaChip dilemma = (DilemmaChip)p.dilemma.chipAtIndex(0);
        	Cost cost = dilemma.cost;
        	if(p.payCost(cost,replay)) 
        		{ setState(EuphoriaState.ConfirmJoinTheEstablishment); 
        		}
        	else 
        		{ 
        		setContinuation(new Continuation(cost,EuphoriaState.JoinTheEstablishment,Function.JoinTheEstablishment));
          		}
        	}
       		break;
       		
       	// used by the robot
        case MOVE_ITEM_TO_PLAYER:
	    	{
	           	EuphoriaCell from = getCell(m.source,m.from_row);
	        	EuphoriaCell to = getCell(m.to_color,m.dest);
	        	pickObject(from,m.from_row);
	        	if((m.chip!=null)&&(m.chip!=pickedObject))
	        			{
	        			// this is important for the robot, when replaying moves from the leading part of the 
	        			// move tree, the deck has been re-randomized, so the cards might not be the same.
	        			// BUT for the move tree to be sensible, the picks have to be the same
	        			if(!robotBoard) { G.print("Dissonant pick - expected "+m.chip+" but got "+pickedObject); }
	        			//
	        			// it also potentially comes into play replaying games when the software has been changed
	        			//
	        			pickedObject = m.chip;
	        			}
				m.chip = pickedObject; 
				EPlayer dp = getPlayer(m.to_color);
	        	setNextStateAfterPick(dp);
	        	dropObject(to);
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
            	dropObject(to);
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
			if(pickedObject.isAuthorityMarker()) { players[whoseTurn].clearCostCache(); }
        	setNextStateAfterPick(players[whoseTurn]);
        	dropObject(to);
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
			m.chip = pickedObject; 
			EPlayer dp = getPlayer(m.from_color);
        	setNextStateAfterPick(dp);
        	dropObject(to);
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
        		dropObject(dest);
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
        	setNextStateAfterDrop(dp);
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
 			if(isDest(src)) { unDropObject(); lastUndrop = src; }
   				else 
   				{	lastUndrop = null;
   					int hgt =  revision>=104 
   							? (((board_state==EuphoriaState.Puzzle) || (src.row>=0) || (revision>=111 && (m.source==EuphoriaId.MarketBasket))) 
   	   								? m.from_row
   	   								: src.height()-1)
   	   						    : (((board_state==EuphoriaState.Puzzle) || (src.row>=0))
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
            	dropObject(dest);
            	setNextStateAfterDrop(dp);
                if(replay==replayMode.Single)
                {
                	animationStack.push(src);
                	animationStack.push(dest);
                }

            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
	        currentPlayerInTurnOrder = whoseTurn;
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            //reverseStatusDisplays(dest);
            openTunnelAtEnd(Allegiance.Euphorian);
            openTunnelAtEnd(Allegiance.Subterran);
            openTunnelAtEnd(Allegiance.Wastelander);
            
            setState(EuphoriaState.Puzzle);
            proceedGameStep = 0;
            continuationStack.clear();
            if(SIMULTANEOUS_PLAY)
            {
            	setRecruitDialogState(getPlayer(activePlayer));
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
            proceedGameStep = 0;
            continuationStack.clear();

            break;
       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(EuphoriaState.Gameover);
    	   break;
        default:
        	
        	cantExecute(m);
        }

        //if(replay==replayMode.Live)   	{ System.out.println("Ax "+m+" for "+whoseTurn+" "+board_state+" "+Digest());   	}
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        return (true);
    }
 
    // return true if this is a cell where workers can be placed.
    private boolean canPlaceWorker(EPlayer p,EuphoriaChip ch,EuphoriaCell c)
    {	
    	int cellH = c.height();
    	EuphoriaId rack = c.rackLocation();
    	 if(rack.canBeBumped || (cellH==0))
    	{	// see if the cost can be met
    	Cost cost = adjustCostForDestAndRecruits(p,c,false,false);

    	if(p.canPay(cost))
    		{ 	c.marketPenalty = null;
    		    if(rack.canBeBumped
    		    		&& (cellH>0)
    		    		&& p.penaltyAppliesToMe(MarketChip.SpaOfFleetingPleasure))
    			{	WorkerChip worker = (WorkerChip)c.topChip();
    				if(worker.color==p.color) 
    				{ c.marketPenalty = MarketChip.SpaOfFleetingPleasure;
    				  return(false); 
    				}
    			}
    			if((c.allegiance==Allegiance.Icarite) 
    				&& p.penaltyAppliesToMe(MarketChip.ApothecaryOfProductiveDreams))
    			{	c.marketPenalty = MarketChip.ApothecaryOfProductiveDreams;
    				return(false);
    			}
    			if(p.penaltyAppliesToMe(MarketChip.ArenaOfPeacefulConflict))
    			{
    				switch(rack)
    				{
    				case EuphorianGenerator:
    					// can't place more than 1 worker
    					if(alreadyHasWorker(p,euphorianGenerator)) 
    						{ c.marketPenalty = MarketChip.ArenaOfPeacefulConflict;
    						  return(false); 
    						}
    					break;
    				case IcariteCloudMine:
    					// can't place more than 1 worker
    					if(alreadyHasWorker(p,icariteCloudMine))
    						{ c.marketPenalty = MarketChip.ArenaOfPeacefulConflict;
    						  return(false); 
    						}
    					break;
    				case SubterranAquifer:
    					// can't place more than 1 worker
    					if(alreadyHasWorker(p,subterranAquifer)) 
    						{c.marketPenalty = MarketChip.ArenaOfPeacefulConflict;
    						return(false); 
    						}
    					break;
    				case WastelanderFarm:
    					// can't place more than 1 worker
    					if(alreadyHasWorker(p,wastelanderFarm)) 
    						{ c.marketPenalty = MarketChip.ArenaOfPeacefulConflict;
    						  return(false); 
    						}
    					break;
    					
    				default: break;
    				}
    			}
    			if(p.penaltyAppliesToMe(MarketChip.PlazaOfImmortalizedHumility))
    			{
    			switch(rack)
	    			{
	    			case EuphorianMarketA:
	    				if(!p.hasAuthorityOnMarket(EuphorianMarketChipA)) 
	    					{ c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					  return(false); 
	    					}
	    				break;
	    				
	    			case EuphorianMarketB:
	    				if(!p.hasAuthorityOnMarket(EuphorianMarketChipB)) 
	    					{ c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
	    				break;
	    				
	    			case SubterranMarketA:
	    				if(!p.hasAuthorityOnMarket(SubterranMarketChipA)) 
	    					{ c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
	    				break;
	    				
	    			case SubterranMarketB:
	    				if(!p.hasAuthorityOnMarket(SubterranMarketChipB)) 
	    					{ c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
	    				break;
	    				
	    			case WastelanderMarketA:
	    				if(!p.hasAuthorityOnMarket(WastelanderMarketChipA)) 
	    					{ c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
	    				break;
	    				
	    			case WastelanderMarketB:
	    				if(!p.hasAuthorityOnMarket(WastelanderMarketChipB)) 
	    					{c.marketPenalty = MarketChip.PlazaOfImmortalizedHumility;
	    					return(false); 
	    					}
	    				break;
    				
   				default: break;
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
        	throw G.Error("Not expecting state %s", board_state);
        case CollectBenefit:
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
        case PayCost:
        	if(pickedObject!=null) { return(c==getSource()); }
        	else { return((sources!=null) && sources.get(c)!=null);}
        case ConfirmRetrieve:
        case ConfirmBenefit:
        case ConfirmPayCost:
        	return(getDest()==c);
        case PlaceOrRetrieve:
        case Place:
        case PlaceNew:
        case PlaceAnother:
        	return((c.rackLocation==EuphoriaId.PlayerWorker) && ((pickedObject==null)?(c.topChip()!=null):true));
        case Retrieve:
        case RetrieveOrConfirm:
        	return((c.rackLocation==EuphoriaId.PlayerNewWorker) && ((pickedObject==null)?(c.topChip()!=null):true));
     	case EphemeralChooseRecruits:
     	case ChooseRecruits:
		case EphemeralConfirmRecruits:
		case ConfirmRecruits:
        case ConfirmOneRecruit:
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
        	throw G.Error("Not expecting state %s", board_state);
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
        	return((c==getSource()) || (dests.get(c)!=null));
        case Retrieve:
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
        	throw G.Error("Not expecting state %s", board_state);
        case ConfirmOneRecruit:
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
        case ConfirmUseJacko:
        case ConfirmPayCost:
        	return(getDest()==c);
        case Place:
        case FightTheOpressor:
        case JoinTheEstablishment:
        case Gameover:
        case ExtendedBenefit:
        case PlaceNew:
        case NormalStart:
        case PlaceAnother:
        	return(false);
        case PlaceOrRetrieve:
        case Retrieve:
        case RetrieveOrConfirm:
        case PayForOptionalEffect:
        case CollectBenefit:
        case CollectOptionalBenefit:
        	return(sources.get(c)!=null);

		case EphemeralConfirmRecruits:
        case ConfirmRecruits:
        case ChooseRecruits: 
     	case EphemeralChooseRecruits:
        case DieSelectOption:
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
    // add light penalties for wasted moves, wasted cards
    public double scoreEstimate_02(int pl,boolean pr)
    {	EPlayer p = players[pl];
    	int tokens = p.authority.height();
    	double val = 1.0-tokens*0.1;
    	if(tokens>0)
    	{
    	double penalty = p.penaltyMoves*0.001;
        double cards = p.cardsLost*0.001;
        val -= (penalty+cards);
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
      			}
     		}
     	}
 
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
    	}
       	unusedArtifacts.shuffle(r);
    	for(EPlayer p : players)
    	{	p.clearUCTStats();
    		if((p!=robo) && (p!=players[whoseTurn]))		// don't randomize the current player in mid turn
    		{	
    			EuphoriaCell cards = p.artifacts;
    			for(int lim = cards.height()-1; lim>=0; lim--)
    			{	// change the card to a random one
    				cards.setChipAtIndex(lim,unusedArtifacts.removeTop());
    			}
    		}
    	}
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
    	Execute(m,replayMode.Replay);
    	//G.print("R "+moveNumber+" "+bs+" "+m+" "+board_state+" "+rstep);

        switch(board_state)
        {
		case EphemeralConfirmRecruits:
        case ConfirmRecruitOption:
        case ConfirmRecruits:
        case ConfirmRetrieve:
        case ConfirmPayForOptionalEffect:
        case ConfirmPlace:
        case ConfirmPayCost:
        case ConfirmBenefit:
        case ConfirmFightTheOpressor:
        case ConfirmJoinTheEstablishment:
        case ConfirmOneRecruit:
        	m.followedByDone = true;
        	stepNumber++; 
        	doDone(replayMode.Replay);
        	break;
        default: break;
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
    	throw G.Error("Not expected");
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
 		if(src.rackLocation().perPlayer)
 			{
 			int height = ephemeralRecruits ? 0 : pickedHeightStack.top();
 			EPlayer p = getPlayer(src.color); 
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
 				case MOVE_PLACE_WORKER:
 					{
 						EuphoriaCell c = getCell(m.dest,m.to_row);
 						val.put(c,m);
 					}
 					break;
 				default:
 					throw G.Error("Not expecting %s",m);
 				}
 			}}
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
				case MOVE_ITEM_TO_PLAYER:
					{	EuphoriaCell c = getCell(m.to_color,m.dest);
						val.put(c,m);
					}
					break;
				default: throw G.Error("Not expecting %s",m);
				}
				}
 		}
	}
 	return(val);
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
			case SubterranAquifer:
				if(placedAquifer || (p.water.height()>=10)) {skip = true; } else { placedAquifer=canPlaceWorker(p,worker,c); skip=!placedAquifer;}
				break;
			case WastelanderFarm:	
				if(placedFarm || (p.food.height()>=10)) { skip=true; } else { placedFarm=canPlaceWorker(p,worker,c); skip=!placedFarm;}
				break;
			case IcariteCloudMine:	
				if(placedCloud || (p.bliss.height()>=10)) { skip= true; } else { placedCloud=canPlaceWorker(p,worker,c); skip=!placedCloud;}
				break;
			case EuphorianGenerator: 
				if(placedGenerator || (p.energy.height()>=10)) { skip=true;  } else { placedGenerator=canPlaceWorker(p,worker,c); skip=!placedGenerator; }
				break;
			default: skip = !canPlaceWorker(p,worker,c);
			break;
				
			}
			}
		else { skip = !canPlaceWorker(p,worker,c); }
		 if(!skip )
		 {	
			 all.addElement(new EuphoriaMovespec(p,src,idx,c));
 		 }
	 }
 }
 public boolean canPlaceAuthorityToken(EPlayer p,EuphoriaCell dest)
 {	 switch(dest.placementBenefit)
	 {
	 	case EuphorianAuthority2:
		case WastelanderAuthority2:
		case SubterranAuthority2:	
			return((p.authority.height()>0) && addAuthorityPlacementMoves(null,p,dest.allegiance,null,p.myAuthority));
		case EuphorianAuthorityAndInfluence:
		case WastelanderAuthorityAndInfluence:
		case SubterranAuthorityAndInfluence:
			return(getAvailableAuthorityCell(dest.allegiance)!=null);
		default: throw G.Error("not expecting %s",dest);
	 }
 }
 public boolean addAuthorityPlacementMoves(CommonMoveStack all,EPlayer p,Allegiance a,EuphoriaCell src,EuphoriaChip token)
 {		
	 	EuphoriaCell marketA = getMarketA(a);
	 	EuphoriaCell marketB = getMarketB(a);
	 	EuphoriaCell zone = getAvailableAuthorityCell(a);
	 	boolean some = false;
	 	if(marketIsOpen(marketA) && !p.hasAuthorityOnMarket(marketA))
	 		{	some=true;
	 			if(all!=null) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,marketA)); }
	 			else { return(true); }
	 		}
	 	if(marketIsOpen(marketB) && !p.hasAuthorityOnMarket(marketB))
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
	 default: throw G.Error("Not expecting %s",src);
	 
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
 {		if(moving.isWorker()) { addWorkerPlacementMoves(all,p,src,(WorkerChip)moving,idx); }
 		else
 		{	switch(board_state)
 			{
 			case CollectOptionalBenefit:
 			case CollectBenefit:
 			{	Benefit bene = pendingBenefit();
 				switch(bene)
 				{
 				case EuphorianAuthority2:
 				case WastelanderAuthority2:
 				case SubterranAuthority2:
 					addAuthorityPlacementMoves(all,p,pendingBenefit().placementZone(),src,moving);
 					break;
 				default: throw G.Error("Not expecting benefit %s",bene);
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
 				if(p.hiddenRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.hiddenRecruits)); }
 				if(p.activeRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,src,p.activeRecruits)); }
 				break;
 				
 			case PayForOptionalEffect:
 			case RecruitOption:
 			case PayCost:
 			case Puzzle:
 			case FightTheOpressor:
 			case ConfirmUseJackoOrContinue:
 			case JoinTheEstablishment:
 				addNormalPlacement(all,p,src);
 			//$FALL-THROUGH$
 			case ConfirmPayCost:
 			case ConfirmUseJacko:
 				break;
 			default: throw G.Error("Not expecting state %s",board_state);
 			}
 		}
 }
 public void addWorkerPlacementMoves( CommonMoveStack all, EPlayer p,EuphoriaChip value)
 {
	 EuphoriaCell workers = p.workers;
	 int placedValues = 0;
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
 	default: throw G.Error("not expecting %s",board_state);
 	
 	case ConfirmPlace:
 	case ConfirmPayCost:
 	case ConfirmPayForOptionalEffect:
 	case ConfirmJoinTheEstablishment:
 	case ConfirmFightTheOpressor:
 	case ConfirmOneRecruit:
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
 	case ConfirmUseJacko:
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
	case Retrieve:
 	case RetrieveOrConfirm:
 	case PlaceOrRetrieve:
 	case Place:
 	case PayForOptionalEffect:
 	case CollectBenefit:
 	case CollectOptionalBenefit:
 	case JoinTheEstablishment:
 	case FightTheOpressor:
 	case PayCost:
 	case PlaceAnother:
 	case ConfirmUseJackoOrContinue:
 	case PlaceNew:
	case ChooseOneRecruit:
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
 					break;
 				case MOVE_CHOOSE_RECRUIT:
 				case MOVE_ITEM_TO_BOARD:
 				case MOVE_PLACE_WORKER:
 					{EuphoriaCell c = getCell(m.from_color,m.source);
 					val.put(c,m);
 					}
 					break;
 				case MOVE_ITEM_TO_PLAYER:
 				case MOVE_RETRIEVE_WORKER:
 					{EuphoriaCell c = getCell(m.source,m.from_row);
 					 val.put(c,m);
 					}
 					break;
 				default: throw G.Error("not expecting %s",m);
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
 	for(int lim = boardWorkers.length-1; lim>=0;lim--)
 	{	EuphoriaCell c = boardWorkers[lim];
 		if(c!=null)
 		{
		 WorkerChip worker = (WorkerChip)c.topChip();
		 G.Assert(worker.color==pcolor,"Matching color");
		 if(worker.color==pcolor)
			 {	switch(c.rackLocation())
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
				 all.addElement(new EuphoriaMovespec(c,p.newWorkers,worker,p.boardIndex));
				 }
			 }
		 }
	 }
 }
 private void addRecruitChoiceMoves(CommonMoveStack all, EPlayer p)
 {
	 for(EuphoriaCell from : p.newRecruits)
	 { if(from.height()>0)
	 	{
		 if(p.activeRecruits.height()==0) { all.push(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.activeRecruits)); }
		 if(p.hiddenRecruits.height()==0) { all.addElement(new EuphoriaMovespec(MOVE_CHOOSE_RECRUIT,p,from,p.hiddenRecruits)); }
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
 private void addAuthority2Moves(CommonMoveStack all,EPlayer p)
 {	
 	EuphoriaCell src = p.authority;
 	if(src.height()>0)
 	{
 		addPlacementMoves(all,p,src,src.topChip(),0);
 	}

 }
 private void addSubterranGoods(CommonMoveStack all,EPlayer p)
 {
	 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
	 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
 }
 private void addResourceMoves(CommonMoveStack all,Benefit bene,EPlayer p)
 {	
	switch(bene)
	 {
	case Commodity:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,farm,p,p.food));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,bliss,p,p.bliss));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,generator,p,p.energy));
		break;
	case WaterOrEnergy:
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,aquifer,p,p.water));
		all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,generator,p,p.energy));
		break;
	 case CardOrStone:
	 case CardAndStone:		// the AND case only occurs when ministry of personal secrets is in effect
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,unusedArtifacts,p,p.artifacts));
		 break;
	 case CardOrClay:
	 case CardAndClay:		// the AND case only occurs when ministry of personal secrets is in effect
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,unusedArtifacts,p,p.artifacts));
		 break;
	 case CardOrGold:
	 case CardAndGold:	// the AND case only occurs when ministry of personal secrets is in effect
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,unusedArtifacts,p,p.artifacts));
		 break;
	 case Resource:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 break;
		 
	case IcariteInfluenceAndResourcex2:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,goldMine,p,p.gold));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,clayPit,p,p.clay));
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,quarry,p,p.stone));
		 break;
	 default: throw G.Error("Not expecting benefit %s",bene);
	 }
 }
 private void addPlacementMoves(CommonMoveStack all,EuphoriaCell src,EuphoriaChip ch,EPlayer p)
 {	EuphoriaId rack = src.rackLocation();
	switch(rack)
	 {
	 default: throw G.Error("Not expecting to place %s",rack);
	 case MoraleTrack: break;
	 
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
	 
	 case ArtifactDeck:
		 all.push(new EuphoriaMovespec(MOVE_ITEM_TO_PLAYER,src,p,p.artifacts));
		 break;
		 
	 }
	 
 }
 
 // pay food or bliss to retrieve workers
 void addFoodOrBlissMoves(CommonMoveStack all,EPlayer p)
 {
	 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
	 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
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
  		if(match==ch)
	 	{	some = true;
 			all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,src,lim,usedArtifacts));
 		}
 	}
 	if(!some) { addCardToDiscardMoves(all,p); }
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
 void addResourceMoves(CommonMoveStack all,EPlayer p)
 {	 if(p.gold.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.gold,goldMine)); }
	 if(p.clay.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.clay,clayPit)); }
	 if(p.stone.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.stone,quarry)); }
 }
 void addCommodityMoves(CommonMoveStack all,EPlayer p,boolean withBliss)
 {
	 if(p.water.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.water,aquifer)); }
	 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
	 if(p.energy.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.energy,generator)); }
	 if(withBliss)
	 {
		 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); } 
	 }
 }
 void addPaymentMoves(CommonMoveStack all,EPlayer p,Cost cost)
 {	
	switch(cost)
	 {
	 default: throw G.Error("Not expecting %s",cost);
	 case Commodity:
		 addCommodityMoves(all,p,true);
		 break;
	 case ResourceOrBliss:
		 addResourceMoves(all,p);
		 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		 break;
	 case NonBlissCommodity:
		 addCommodityMoves(all,p,false);
		 break;
	 case ResourceOrBlissOrFood:
		 addResourceMoves(all,p);
		 addFoodOrBlissMoves(all,p);
		 break;
	 case EnergyOrBlissOrFood:
		 if(p.energy.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.energy,generator)); }
		 // fall into regular blissorfood
		//$FALL-THROUGH$
	case BlissOrFood:
		 addFoodOrBlissMoves(all,p);
		 break;
	 case Morale_BlissOrFoodPlus1:
	 case BlissOrFoodPlus1:
		 // add a food or a bliss, and one other not bliss
		 if((droppedDestStack.size()==1) && !(droppedDestStack.contains(farm) || droppedDestStack.contains(bliss)))
		 {	// if we started with a commodity, only consider food and bliss
			 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
			 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		 }
		 else { addCommodityMoves(all,p,!droppedDestStack.contains(bliss)); }
		 break;
		 
	 case GoldOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 if(p.gold.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.gold,goldMine)); }
		 break;
		 
	 case StoneOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 if(p.stone.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.stone,quarry)); }
		 break;
		 
	 case ClayOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 if(p.clay.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.clay,clayPit)); }
		 break;
		 
	 case CommodityOrResourcex4Penalty:
	 case CommodityOrResourcex3Penalty:
	 case CommodityOrResourcex2Penalty:
	 case CommodityOrResourcePenalty:
		 addCommodityMoves(all,p,true);
		 addResourceMoves(all,p);
		 break;
		 
	case WaterOrKnowledge:
		if(p.water.height()>0)	// Brian can use food for bliss
	 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.water,aquifer)); 
	 	}
		break;
	case EnergyOrKnowledge:
		if(p.energy.height()>0)	// Brian can use food for bliss
	 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.energy,generator)); 
	 	}
	
		break;
	case FoodOrKnowledge:
		if(p.food.height()>0)	// Brian can use food for bliss
	 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); 
	 	}
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
		 G.Assert(revision<115,"only if the old bug is present");
		 addRobotUnpairedDiscardMoves(all,p);
		 // fall through
		//$FALL-THROUGH$
	case Energy:
		 if(p.energy.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.energy,generator)); }
		 break;
		 
	 case Morale_Artifactx3:
	 case Artifactx3:
	 case Mostly_Artifactx3:
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
		 	break;
		 
	 case Card_ResourceOrBlissOrFood:
	 	{
		boolean hasCard = false;
		boolean hasResource = false;
		if(droppedDestStack.size()>0)
		 {	EuphoriaCell d = droppedDestStack.top();
		 	if(d==usedArtifacts) { hasCard = true; } else { hasResource=true; }
		 }
		if(!hasResource && (p.bliss.height()>0)) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		if(!hasResource && (p.food.height()>0)) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,bliss)); }
		if(!hasResource) { addResourceMoves(all,p); }
		if(!hasCard) 
			{ 
			addRobotUnpairedDiscardMoves(all,p);
			}
	 	}
	 	break;
	 case Card_ResourceOrBliss:
	 	{
		boolean hasCard = false;
		boolean hasResource = false;
		if(droppedDestStack.size()>0)
		 {	EuphoriaCell d = droppedDestStack.top();
		 	if(d==usedArtifacts) { hasCard = true; } else { hasResource=true; }
		 }
		if(!hasResource && (p.bliss.height()>0)) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		if(!hasResource) { addResourceMoves(all,p); }
		if(!hasCard) 
			{ addRobotUnpairedDiscardMoves(all,p); 
			}
	 	}
	 	break;
	 case Card_Resource: // card and a resource for a market
	 	{
	 	boolean hasResource = false;
		boolean hasCard = false;
		if(droppedDestStack.size()>0)
		 {	EuphoriaCell d = droppedDestStack.top();
		 	if(d==usedArtifacts) { hasCard = true; } else { hasResource=true; }
		 }
		 if(!hasCard) 
		 	{ addRobotUnpairedDiscardMoves(all,p);
		 	}
		 if(!hasResource) { addResourceMoves(all,p); }
	 	}
		 break;
		 
	 case BlissOrFoodx4_ResourceOrBlissOrFood:
	 	{	// at most 1 resource, the rest can be food or bliss
		int blissorfood = countBlissOrFood(droppedDestStack);
		boolean hasResource = droppedDestStack.size()>blissorfood;
	    if(!hasResource) 
    		{ addResourceMoves(all,p);
    		}
		if(p.food.height()>0)	// Brian can use food for bliss
	 		{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); 
	 		} 
		if(p.bliss.height()>0)	// josh can use all bliss
	 		{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); 
	 		} 
	 	}
	 	break;
	 case BlissOrFoodx4_Card: 
	 	{
	 	int blissorfood = countBlissOrFood(droppedDestStack);
	 	boolean hasCard = droppedDestStack.size()>blissorfood;
	 	boolean roomForFood = (blissorfood<4);
	    if(!hasCard) 
	    	{
	    	addRobotUnpairedDiscardMoves(all,p);
	    	}
	    if(roomForFood)
	    {
		if(p.food.height()>0)	// Brian can use food for bliss
		 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); 
		 	} 
		if(p.bliss.height()>0)	// josh can use all bliss
		 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); 
		 	}
	 	}}
	 	break;
	 case BlissOrFoodx4_Resource:
	 	{
	 	int blissorfood = countBlissOrFood(droppedDestStack);
	 	boolean hasResource = droppedDestStack.size()>blissorfood;
	 	boolean roomForFood = (blissorfood<4);
	    if(!hasResource) 
	    	{ if(cost==Cost.BlissOrFoodx4_Resource) { addResourceMoves(all,p); }
	    		else 
	    		{addRobotUnpairedDiscardMoves(all,p); 
	    		} 
	     	}
	    if(roomForFood)
	    {
		if(p.food.height()>0)	// Brian can use food for bliss
		 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); 
		 	} 
		if(p.bliss.height()>0)	// josh can use all bliss
		 	{ all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); 
		 	}
	 	}}
	 	break;
	 case ResourceAndKnowledgeAndMoraleOrArtifact:
		 addRobotUnpairedDiscardMoves(all,p);
		 // fall into add resoruce moves
		//$FALL-THROUGH$
	case ResourceAndKnowledgeAndMorale:
	 case Resource:
	 case Morale_Resourcex3:
	 case Resourcex3:	// pay 3 resources (nimbus loft)
	 case Mostly_Resourcex3:
		 addResourceMoves(all,p);
		 break;

	 case GoldOrFoodOrBliss:
		 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
		 // fall into GoldOrBliss
		//$FALL-THROUGH$
	case GoldOrBliss:
		 // this can only come up when josh the negotiator is in effect, and bliss is also an option
		 if(p.gold.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.gold,goldMine)); }
		 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		 break;
		 
	 case StoneOrFoodOrBliss:
		 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
		 // fall into stoneorbliss
		//$FALL-THROUGH$
	case StoneOrBliss:
		 // this can only come up when josh the negotiator is in effect, and bliss is also an option
		 if(p.stone.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.stone,quarry)); }
		 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		 break;
		 
	 case ClayOrFoodOrBliss:
		 if(p.food.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.food,farm)); }
		 // fall into clayorbliss
		//$FALL-THROUGH$
	case ClayOrBliss:
		 // this can only come up when josh the negotiator is in effect, and bliss is also an option
		 if(p.clay.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.clay,quarry)); }
		 if(p.bliss.height()>0) { all.push(new EuphoriaMovespec(MOVE_ITEM_TO_BOARD,p,p.bliss,bliss)); }
		 break; 	
		 

	 }
	if((all.size()==0)&&robotBoard)
	{	robotBoard = false;
		throw G.Error("Didn't produce playment moves for %s",cost);
	}
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
	 default: throw G.Error("not expecting %s",id);
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
	 default: throw G.Error("not expecting %s",id);
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
			 default: throw G.Error("not expecting %s",p.totalWorkers);
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
			 default: throw G.Error("not expecting %s",availableWorkers);
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
			 default:throw G.Error("not expecting %s",availableWorkers);
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
			 return(Math.max(0.0,Math.min(1.0,1.0-(p.water.height()-5)*0.3)));
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
			 default: throw G.Error("not expecting %s",availableWorkers);
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
			 default: throw G.Error("not expecting %s",p.totalWorkers);
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
			 default: throw G.Error("not expecting %s",p.totalWorkers);
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
			 default: throw G.Error("not expecting %s",p.totalWorkers);
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
 		throw G.Error("Not expecting state %s",board_state);
 	case NormalStart:
 		all.push(new EuphoriaMovespec(NORMALSTART,p));
 		break;

	case EphemeralConfirmRecruits:
	case ConfirmRecruits:
 	case ConfirmPlace:
 	case ConfirmPayCost:
 	case ConfirmBenefit:
 	case ConfirmFightTheOpressor:
 	case ConfirmOneRecruit:
 	case ConfirmJoinTheEstablishment:
 	case ConfirmPayForOptionalEffect:
 	case ConfirmRecruitOption:
 	case ConfirmRetrieve:
 	case ConfirmUseJacko:
 	case Resign:
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		break;
 	case PayForOptionalEffect:
 	case ConfirmUseJackoOrContinue:
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
 		if(!skip) { all.push(new EuphoriaMovespec(USE_RECRUIT_OPTION,recruit,p)); }
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		}
 		break;
 	case CollectBenefit:
 	case CollectOptionalBenefit:
 	{	Benefit bene = pendingBenefit();
 		switch(bene)
 		{
 		case IcariteInfluenceAndResourcex2:
 		case CardAndStone:
 		case CardAndClay:
 		case CardAndGold:	// these occur when registry of personal secrets (no recruit allegiance benefits) is in effect
		case CardOrClay:
		case CardOrGold:
		case Resource:
		case CardOrStone:
		case WaterOrEnergy:
		case Commodity:
			addResourceMoves(all,bene,p);
			break;
		case WaterOrStone:
			addSubterranGoods(all,p);
			break;
 		case EuphorianAuthority2:
 		case WastelanderAuthority2:
 		case SubterranAuthority2:
 			addAuthority2Moves(all,p);
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
 		default: throw G.Error("Not expecting pending benefit %s",bene);
 		}}
 		break;
 	case ChooseOneRecruit:
 		addSingleRecruitChoiseMoves(all,p);
 		break;
 	case ChooseRecruits:
 	case EphemeralChooseRecruits:
 		addRecruitChoiceMoves(all,p);
 		break;
 	case RetrieveOrConfirm:
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		addWorkerRetrievalMoves(all,p,!robotBoard);
 		break;
  	case Retrieve:
 		addWorkerRetrievalMoves(all,p,!robotBoard);
 		if(all.size()==0)
 		{
 			// this really shouldn't happen, but just in case
 			addWorkerRetrievalMoves(all,p,true);
 		}
 		break;
 	case PlaceOrRetrieve:
 		addWorkerPlacementMoves(all,p,doublesElgible);
 		if(!robotBoard || (all.size()==0)) 
 			{ 
 			addWorkerRetrievalMoves(all,p,!robotBoard);
 			}
 		break;
 	case Place:
 	case PlaceNew:
 		addWorkerPlacementMoves(all,p,doublesElgible);
 		break;
 	case PlaceAnother:
 		all.push(new EuphoriaMovespec(MOVE_DONE,p));
 		addWorkerPlacementMoves(all,p,doublesElgible);
 		break;

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
 	return(all);
 }
 
//
// animation assistants
//
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
		  dest.copyCurrentCenter(workerActivationB);
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

}
