package rpc;

import java.util.StringTokenizer;

import lib.Base64;
import lib.G;
import lib.Plog;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.StringStack;
import online.game.PlayConstants;
import online.game.commonCanvas;
/**
 * this is the bridge class that connects a game window with a remote copy.
 * 
 * the general scheme is that one of these exists for each remote viewer,
 * "client" is the main window for the actual game in progress.
 * we prime the pump by sending a complete game state, then send individual
 * moves as updates.  We can receive moves from the remote client, which
 * are stuffed into the message queue for the client just as they would from
 * a networked player.
 * 
 * Some care needs to be taken to interact with the client synchronously. We
 * don't call game functions like "serverRecordString" directly, but rather
 * enter a request on the game's message queue, which will interact with us
 * via a callback.
 * 
 * In theory, everything always stays in synch, just as it does during networked games.
 * 
 * @author ddyer
 *
 */
public class RpcRemoteServer implements RpcInterface,SimpleObserver,PlayConstants
{
	commonCanvas client;					// nominally a game window
	String description = "unnamed remote";
	boolean active = false;
	long lastClientDigest = 0;
	String completeState = null;
	boolean needComplete = true;
	int forPlayer = -1;			// the player we're viewing for, or -1 for a general spectator
	Plog plog = new Plog(25);
	public void shutDown()
	{	setRpcIsActive(false);
		if(client!=null) 
			{ client.shutDown(); 
			}
	}
	public RpcRemoteServer(String des,commonCanvas forWindow,int forP)
	{	description = des;
		client = forWindow;
		forPlayer = forP;
	}

	// return true if there is new information to push out to the remote client
	public boolean needsRecapture() {
		return (messages.size()>0 || completeState!=null);
	}

	public ServiceType serviceType() {
		return forPlayer<0 ? ServiceType.RemoteScreen : ServiceType.SideScreen;
	}

	// transmit the state or an update to the remote client.
	public String captureState() {
		String cs = completeState;
		completeState = null;
		if(cs!=null) 
			{ 
			messages.clear(); 
			lastClientDigest = client.Digest();
			plog.appendNewLog("Complete ");
			plog.appendLog(cs);
			plog.finishEvent();
			return(Keyword.Complete +" "+cs);
			}
		else {
			// update a short list of commands, presumably moves
			// we base64 encode the moves so the contents are protected
			StringStack m = messages;
			messages = new StringStack();
			int lim = m.size();
			if(lim>0)
			{
			StringBuilder b = new StringBuilder();
			b.append(Keyword.Update);
			b.append(' ');
			for(int i=0; i<lim; i++)
			{	String msg = m.elementAt(i);
				b.append(Base64.encodeSimple(msg));
				b.append(' ');
			}
			String msg = b.toString();
			plog.addLog(msg);
			return(msg);
			}
			else {
			plog.addLog("sending noop");
			return(Keyword.None.name());
			}
		}
	}

	// incoming requests from a remote viewer
	public void execute(String msg) {
		StringTokenizer tok = new StringTokenizer(msg);
		plog.addLog(msg);
		while(tok.hasMoreTokens())
		{
			String cmd = tok.nextToken();
			Keyword op = Keyword.valueOf(cmd);
			switch(op)
			{
			default: throw G.Error("Not expecting %s\n%s", op,plog.finishLog());
			case Complete:
				// needs a complete update
				client.deferMessage(op,this,null, forPlayer);
				break;
			case Update:
				// making a standard game move.  
				String dat = Base64.decodeString(tok.nextToken());
				client.deferMessage(dat,forPlayer);
				client.notifySiblings(this, dat);
				break;
			case Digest:
				// declaring the current digest
				lastClientDigest = G.LongToken(tok);
				break;

			}	
		}
		
	}

	public String getName() {
		return description;
	}
	public void setName(String c) 
	{ 	if(!description.equals(c))
		{ description = c; 
		  observers.setChanged();
		}
	}
	
	public boolean rpcIsActive() {
		
		return active;	// always available
	}
	public void setRpcIsActive(boolean v)
	{	boolean wasActive = active;
		active = v;
		if(v) 
			{
			if(!wasActive)
			{
			client.addObserver(this); 
			// this primes the pump with a complete state for the client
			plog.verbose = false;		
			plog.addLog("Start, request complete");
			
			client.deferMessage(Keyword.Complete, this,null,forPlayer);
			}}
		else if(wasActive)
			{ client.removeObserver(this); 
			}
	}
	
	public void updateProgress()
	{ 
		if(client!=null) { client.touchPlayer(forPlayer); }
	}

	@SuppressWarnings("deprecation")
	public String captureInitialization() 
	{ return(client.getClass().getName()+" "+forPlayer);	
	}

	
	// the observers will be the transmitter to the remote screen
	SimpleObservable observers = new SimpleObservable();
	public void removeObserver(SimpleObserver b) {
		observers.removeObserver(b);
	}

	public void addObserver(SimpleObserver o) {
		observers.addObserver(o);
	}

	StringStack messages = new StringStack();
	
	public void update(SimpleObservable o, Object eventType, Object arg) {
		plog.appendNewLog("update ");
		if(eventType == this) {}		// ignore messages we sent
		else if(eventType==Keyword.Complete)
			{
			 completeState = (String)arg;
			 plog.appendLog(eventType.toString());
			 plog.appendLog(" ");
			 plog.appendLog(completeState);

			 observers.setChanged(true);
			}
			else if((eventType==null) ||(eventType instanceof RpcRemoteServer))
		{
			if(arg instanceof String)
			{
		String update = (String)arg;
		messages.push(update);
		observers.setChanged(true);
					plog.appendLog(update);
			}
		}
			else {
				G.Error("Unexpected update event type %s\n%s", eventType,plog.finishLog());
	}
		plog.finishEvent();
		}
	
	
}
