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
package lib;

public class NewsReader extends Thread implements Runnable
{
    ChatInterface myChat = null;
    String postMessage = null;
    String postFile = null;
    public boolean finished = false;
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
            myChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT, postMessage);
        }
        if(postMessageHost!=null)
        	{	myChat.postHostMessages(postMessageHost);
        	}
        
    }
    catch (Throwable err)
    {	Http.postError(this, "news reader for "+postFile, err);
    }
    	finally { finished = true; }
    }
}
