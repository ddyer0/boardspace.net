package rpc;

public interface RpcConstants {

	enum Protocol { r1, r2 ; }
	
	enum ServiceType { Dispatch, RemoteScreen, SideScreen ; }
	
	public Protocol SelectedProtocol = Protocol.r1;
	public int waitTime = 1000;						// 1 second
	public int pingTime = 11*1000;					// 11 seconds
	public int pingTimeout = 34*1000;				// 34 seconds
	
	enum Command
	{
		SessionID,
		Connect,
		Echo,				// just a return status
		Say,				// just info or echo return
		UpdateAvailable,	// push notification from the server
		UpdateRequired,		// service has changed, full update required
		GetGameState,		// retrieve the state of the interface		
		SwitchTypes,		// change types, provide initialization for new type
		SetGameState,		// response to getGameState
		Execute,			// process a command from the client to server
		// and otherwise
		_Undefined_;		// 
		public static Command find(String e) 
		{	return( valueOf(e));			
		}
	}


	public static String NothingPlaying = "Nothing playing now";
	public static String Shutdown = "Server shut down";
	public static String ActiveConnection = "(connected)";
	public String RpcStrings[] = 
		{	NothingPlaying,
			Shutdown,
			ActiveConnection,
		};
}
