package lib;

import online.common.OnlineConstants;

public class NewsReader extends Thread implements Runnable, OnlineConstants
{
    ChatInterface myChat = null;
    String postMessage = null;
    String postFile = null;
    public String postMessageHost = null;
    public NewsReader(ChatInterface chat, String file, String message)
    {	super("NewsReader");
        postFile = file;
        myChat = chat;
        postMessage = message;
    }

    public void run()
    {
    	try {
    	if(postFile!=null) { myChat.PostNews(postFile); }

        if (postMessage != null)
        {
            myChat.postMessage(ChatInterface.GAMECHANNEL, KEYWORD_CHAT, postMessage);
        }
        if(postMessageHost!=null)
        	{	myChat.postHostMessages(postMessageHost);
        	}
        
    	}
    	catch (Throwable err)
    	{	Http.postError(this, "news reader for "+postFile, err);
    	}
    	}
}
