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

import bridge.*;
import java.io.*;


/**
 * transition to UTF-8 (March 2017)
 * 
 * It was noticed that some game records saved to the server
 * have nontrivial unicode representations, which can make 
 * them unreadable by the game browser.  The solution is to
 * switch the numeric encoding of non-ascii characters from 
 * our previous ad-hoc format to UTF-8.  This is "mostly" 
 * transparent, because after conversion to UTF-8 we encode
 * the nonstandard characters as before, using \nnn or \#hhhh
 * 
 * The exception to "mostly" is the other place where unicode
 * usually flows to the server and back, which is chat text.
 * Switching to the new scheme in a trivial way makes unicode 
 * chat mutually incomprehensible between new and old clients
 * which is unseemly.
 * 
 * So, to maintain appearances, we do a 4 stage release.  In 
 * stage 1, only the game records are made utf8, but the client
 * is made aware of the new encoding scheme.
 * Version 2.12 includes stage 1.
 *    
 * stage 2, after all pre-utf-8 clients are extinct, we actually 
 * start generating UTF-8 all the time.  This corresponds to 
 * version 2.82 in October 2018
 * 
 * stage 3, after stage 1 clients are extinct we can remove the
 * dual decoding capability.
 * stage 4, after all stage 2 clients are extinct, is to switch 
 * to using \nnn all the time since it is more efficient.
 * 
 * The key to differentiating which encoding is in use without
 * breaking anything is that bytes codes > 128 are encoded as
 * \#xxxx even though they could be encoded as \nnn
 * 
 */
/** this class handles the network interaction for one frame.

 Essentially there are three states, active, connecting, and waiting.

 In active state, there is data on the way in, and we are either reading
 it or blocked waiting.

 In connecting state, we are trying to establish a connection and set up
 streams.  If we fail, we set the connectionFailed flag. If we succeed, we
 enter active state.

 In waiting state, we've just started and we wait to be interrupted and
 told to connect.

*/

public class NetConn extends CommonNetConn<String> implements Runnable,Config
{
    /** public utility to request a URL and take care of header gubbush.  Returns
    a DataInputStream positioned to the start of the user data, or null if there
    was an explicit "not ok".  Throws IOExceptions, which the caller must be catching
    anyway, since he's about to read from the value we return
    */
	ConnectionManager myManager=null;
 	private int BufferLength = 4096;		// this number has to be smaller than BUFFSIZE acceptable to the server
    final int XmitQuantum = 120;
    public int deficit = 0;
    public boolean checksum = true;
    public static final int MAX_READ_ERRORS = 10;		// limit on the number of reports we send.
    public int sum_reads = 0;
    public long last_read = 0;
    private byte[] realOutBuf;
    private byte[] realInBuf;
    private byte[] csBuf = new byte[9];
    public boolean readSingle = false;
	public static final String FAILED_NOT_UNDERSTOOD = "999";
	public static final String ECHO_PROXY_DATA = "364";
	public static final String SEND_PROXY_DATA = "363 ";
	public static final String ECHO_PROXY_OP = "362";
	public static final String SEND_PROXY_OP = "361 ";
	public static final String SEND_MULTIPLE_COMMAND = "338";		// combine multiple commands
	public static final String SEND_MULTIPLE = SEND_MULTIPLE_COMMAND + " ";
	public static final String ECHO_APPEND_GAME = "337";	// unexpectedly there were problems
	public static final String SEND_APPEND_GAME = "336 ";	// append a partial game
	public static final String ECHO_ROOMTYPE = "335";
	public static final String FAILED_SET_ROOMTYPE = "334";
	public static final String SEND_SET_ROOMTYPE = "334 ";
	public static final String ECHO_RESERVE = "333";
	public static final String FAILED_RESERVE = "332";
	public static final String SEND_ASK_RESERVE = "332 ";
	public static final String SEND_LOBBY_INFO = "330 ";
	public static final String ECHO_STATE = "329";		// receive notice of possible follower fraud
	public static final String SEND_STATE = "328 ";	// send digest for follower fraud detection
	public static final String SEND_NOTE = "326 ";
	public static final String ECHO_REMOVE_GAME = "325";	// echo from 324
	public static final String SEND_REMOVE_GAME = "324 ";	// forget a game
	public static final String ECHO_SEND_GAME = "323";		// echo from 322
	public static final String SEND_GAME_RECORD = "322 ";	// send a complete game
	public static final String ECHO_QUERY_GAME = "319";	// reply
	public static final String SEND_QUERY_GAME = "318 ";	// ask if a game exists
	public static final String SEND_GAME_SGF = "316 ";
	public static final String SEND_REGISTER_PLAYER = "314 ";
	public static final String ECHO_GROUP = "213";
	public static final String ECHO_GROUP_SELF = "211";
	public static final String SEND_AS_ROBOT_ECHO = "312 "+ECHO_GROUP+" ";
	public static final String SEND_AS_ROBOT = "312 ";
	public static final String SEND_AS_ROBOT_CMD = "312";
	public static final String ECHO_DETAIL_END = "309";
	public static final String SEND_LOG_REQUEST = "308 ";
	public static final String ECHO_DETAIL = "307";
	public static final String FAILED_ASK_DETAIL = "306";
	public static final String SEND_ASK_DETAIL =  "306 ";
	public static final String ECHO_SUMMARY = "305";
	public static final String SEND_ASK_SUMMARY = "304 ";
	public static final String ECHO_PING = "303";
	public static final String SEND_PING = "302 ";
	public static final String SEND_MESSAGE_TO = "230 ";
	public static final String ECHO_PLAYER_QUIT = "223";
	public static final String ECHO_I_QUIT = "221";
	public static final String SEND_REQUEST_EXIT_COMMAND = "220";
	public static final String SEND_REQUEST_EXIT = SEND_REQUEST_EXIT_COMMAND+" ";

	// 208 used to be score by name, used by the cgi scripts, now obsolete
	public static final String SEND_GROUP_COMMAND = "210";
	public static final String SEND_GROUP = SEND_GROUP_COMMAND + " ";
	public static final String ECHO_MYNAME = "205";
	//    static final String SEND_COLOR = "206 ";		// no longer used
	//    static final String ECHO_COLOR = "207";
	public static final String SEND_MYNAME = "204 ";		// used for guests to set their "guest name"
	public static final String ECHO_INTRO = "203";
	public static final String ECHO_INTRO_SELF = "201";
	public static final String SEND_INTRO = "200 ";
	public static final String SEND_REQUEST_LOCK = "342 ";
	public static final String ECHO_REQUEST_LOCK = "342";
	
	// these are the new versions, which don't attempt to filter out the time
	// information (which is done to protect old client programs)
	public static final String ECHO_ACTIVE_GAME = "347";		// retrieved current game record
	public static final String SEND_FETCH_ACTIVE_GAME = "346 ";	// ask for the current game record
	public static final String ECHO_FETCHED_GAME = "345";	// retrieved game record
	public static final String SEND_FETCH_GAME = "344 ";	// ask for a game record

	//
	// encryption, which must exactly mirror the implementation in the server.
	//
    public int m_w_in=0;
    public int m_z_in=0;
    public int m_w_out=0;
    public int m_z_out=0;
    public int rng_in_chars = 0;
    public int rng_out_chars = 0;
    public boolean use_rng_in=false;
    public boolean use_rng_out=false;

    void initObf(int r1,int r2,int r3,int r4,int uid)
    {   // enable it.  These initialization strings have to exactly match the ones 
		// constructed by the server for itself.
 		String inMsg = ipString(r1+1,r2+2,r3+3,r4+4,uid+2);
		String outMsg = ipString(r1+3,r2+6,r3+9,r4+12,uid+1);

    	m_w_in = m_w_out = 0;
    	m_z_in = m_z_out = 0;
    	rng_in_chars = 0;
    	rng_out_chars = 0;
    	for(int i=0,lim=inMsg.length(); i<lim; i++) { m_w_out = m_w_out*13+inMsg.charAt(i); }
    	for(int i=0,lim=inMsg.length(); i<lim; i++) { m_z_out = m_z_out*31+ inMsg.charAt(i); }
    	for(int i=0,lim=outMsg.length(); i<lim; i++) { m_w_in = m_w_in*17+outMsg.charAt(i); }
    	for(int i=0,lim=outMsg.length(); i<lim; i++) { m_z_in = m_z_in*23+ outMsg.charAt(i); }
    	use_rng_in = true;
    	use_rng_out = true;
    }
    
    // obfuscate an input message.
    private void obinmessage(byte[]data,int start,int len)
    {	
    	if(use_rng_in)
		{
		int m_w = m_w_in;
		int m_z = m_z_in;
		for(int i=0;i<len;i++)
		{	int ch = data[start+i];
			if(ch>' ')
			{
		    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
		    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
		    int val = 0x3f &  m_w;  /* 7-bit result */
		    int nv = ((ch-' '-1)+(127-' ')-val)%(127-' ')+' '+1;
    		//System.out.println("i "+ch+" "+val+" "+nv);
		    rng_in_chars++;
		    G.Assert(nv>' ' && (nv<=127),"enc in in range");
		    data[start+i] = (byte)nv;

			}
		}			
		m_w_in = m_w;
		m_z_in = m_z;
		}
    }
    // obfuscate an output message
    private void oboutmessage(byte[]data,int start,int len)
    {	
    		if(use_rng_out)
   		{
   		int m_w = m_w_out;
    	int m_z = m_z_out;
    	for(int i=0;i<len;i++)
    	{	int ch = data[start+i];
    		if(ch>' ')
    				{
    			    m_z = 36969 * (m_z & 65535) + (m_z >> 16);
    			    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
    			    int val = 0x3f &  m_w;  /* 7-bit result */
    			    int nv = (((ch-' '-1)+val)%(127-' ')+' '+1);
    			    //System.out.println("o "+ch+" "+val+" "+nv);
    			    rng_out_chars++;
    			    G.Assert(nv>' ' && (nv<=127),"enc out in range");
    			    data[start+i] = (byte)nv;
    				}
    			}
    			m_w_out = m_w;
    			m_z_out = m_z;
    		}
    	
    }
    

	public NetConn(ConnectionManager man,String me)
	{	super(man,me);
	}
    public void setBufSize(int n)
    {
        BufferLength = n;
        if((realOutBuf==null) || (n != realOutBuf.length))
        {
        	realOutBuf = new byte[n];
	    }
        if((realInBuf==null) || (n != realInBuf.length))
        {
        	realInBuf = new byte[n];
        }
    }

    //private Vector pings = new Vector();
    public void addPing(long time)
    {
         /* pings.addElement(new Integer(time));*/
    }



    public void logError(String message, Throwable err)
    {	if(myManager!=null) { myManager.na.getLock(); }
        String str = NetConn.SEND_LOG_REQUEST + Http.getErrorMessage(message, err);
        int part = 2;
        int msg = 1;		// count the messages generated outside the usual context

        while (str.length() > (BufferLength / 2))
        {	if(myManager!=null) 
        		{ myManager.missingItems++; }
        	msg++;
            sendMessage(str.substring(0, BufferLength / 2));
            str = NetConn.SEND_LOG_REQUEST+"(part " + part++ + ") " +
                str.substring(BufferLength / 2);
        }

        sendMessage(str);
        if(myManager!=null) 
        	{
        	myManager.count(msg);
        	myManager.na.Unlock();
        	}
    }

    private int checksumString(byte[] buf, int from, int len)
    {
        int sum = 0;
        int last = from + len;

        for (int f = from, i = 0; i < last; i++, f++)
        {
            int ch = 0xff & (buf[f]);

            if ((ch == 0) || (ch == '\r') || (ch == '\n'))
            {
                break;
            }

            sum += (ch ^ i);
        }
        
        return (sum & 0xffff);
    }
     
	/** this is the hash checksum used by the server, very similar to G.hashChecksum, but it 
	 * must deal with out of range characters in exactly the same way as the message encoder
	 * does.  In particular, embedded unicode characters are encoded and expanded, causing
	 * excess characters in the virtual stream
	 * 
	 *  */
    public int serverHashChecksum(String str,int n)
    {   int hash = 5381;
    	int c;
     	for (int i=0; i<n;i++)
    	{
    		c = str.charAt(i);
    		if(c<=0 ||c>=128)
    		{	// this is not expected to be used, because the server string
    			// ought to already be pure ascii
    			return slowServerHashChecksum(str,n);
    		}
    		hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
    	}
      	serverHashChecksumOffset = n;
    	return hash;
    }
    public int  serverHashChecksumOffset = 0;
    //
    // the server stores UTF8 versions of the game record
    // if we encounter UTF8-likely characters, we have to convert 
    // just to be sure.
    public int slowServerHashChecksum(String str,int n)
    {
        try
        {
        byte[] theBuff = str.substring(0,n).getBytes("UTF-8");
    	int hash = 5381;
    	int len = theBuff.length;
    	serverHashChecksumOffset = len;
    	for(int idx=0;idx<len;idx++)
    	{
    		int ch = 0xff & theBuff[idx];
    		hash = ((hash << 5) + hash) + ch; /* hash * 33 + c */
    	}
    	return hash;
        }
        catch (UnsupportedEncodingException e) 
        	{
        	throw G.Error("Unexpected error ",e);
        	}
        
    }

    private int writeErr=0;
    boolean realSendMessage(OutputStream os,String theBuffStr)
    {
            
     try
     {
      boolean split = false;
      //
      // new strategy 3/2017, convert the string to utf8
      // so the byte level stream will be all single bytes
      // the old format for multi-byte strings will never occur
      //
      byte[] theBuff = theBuffStr.getBytes("UTF-8");
      byte[] outBuf = realOutBuf;
      int inBufLength = theBuff.length;
      int outBufLength = (checksum & !split) ? 9 : 0;
      int inbufi = 0;
      int maxlen = outBuf.length - 20; //-20 is slop to make sure we don't overrun
      {
      for (inbufi = 0; 
      	  (inbufi < inBufLength) && (outBufLength < maxlen);
           inbufi++)
   		 {	//short rather than byte so we get positive ints for > 127
 				int ch = 0xff & theBuff[inbufi];
				/* a simple encoding for non printable ascii characters */
				
				if(ch>=127)
				{
				// encode >127 as \#xxxx, which will be detected
				// as utf8 because it should have been \nnn
				outBuf[outBufLength++] = (byte)'\\';
   			    outBuf[outBufLength++] = (byte)'#';
   			    String f = Integer.toHexString(ch);
   			    int flen=f.length();
   			    for(int i=flen;i<4;i++) { outBuf[outBufLength++]=(byte)'0'; }
   			    for(int i=0;i<flen;i++) { outBuf[outBufLength++]=(byte)f.charAt(i); }
				}
				else if(ch<32) 
					{ int hundreds = ch/100;
				      int tens = (ch%100)/10;
					  int ones = ch%10;
						//System.out.println("ch " + ch );
					  outBuf[outBufLength++]=(byte)'\\';
					  outBuf[outBufLength++]=(byte)('0'+hundreds);
					  outBuf[outBufLength++]=(byte)('0'+tens);
					  outBuf[outBufLength++]=(byte)('0'+ones);
					}
				else 
					{ if(ch=='\\') { outBuf[outBufLength++]=(byte)'\\'; }
						outBuf[outBufLength++] = (byte)ch;
					}

    	   }
 
             if (inbufi < inBufLength)
            {
                String msg = "outgoing message too long, limit is " + maxlen +
                    " length is " + inBufLength + " message starts " +
                    theBuffStr.substring(0, Math.min(inBufLength, 30));
                logError(msg, null);

                return (false);
            }

            outBuf[outBufLength++] = ' ';
            outBuf[outBufLength++] = 0x0d;
            outBuf[outBufLength++] = 0x0a;
            //System.out.print("Sending ");
            //System.out.write(outBuf,0,outBufLength-3);
            //System.out.println();
            //
            //hack to generate data breaks artificailly
            //if(outBufLength>10)
            //	{os.write(outBuf,0,10);
            //	os.flush();
            //try { sleep(1000); } catch(InterruptedException e) {};
            //	os.write(outBuf,10,outBufLength-10);
            //	}else
            {
                    if (checksum)
                    {	int frm = split ? 0 : 9;
                    	int to = outBufLength - (2 + (split ? 0 : 9));
                    	
                    	oboutmessage(outBuf,frm,to);
                    	
                        int sum = checksumString(outBuf, frm,to);
                        
                        int sum1 = (sum >> 12) & 0xf;
                        int sum2 = (sum >> 8) & 0xf;
                        int sum3 = (sum >> 4) & 0xf;
                        int sum4 = sum & 0xf;
                        byte[] sbuf = split ? csBuf : outBuf;
                        sbuf[0] = (byte) '5';
                        sbuf[1] = (byte) '0';
                        sbuf[2] = (byte) '0';
                        sbuf[3] = (byte) ' ';
                        sbuf[4] = (byte) ('A' + sum1);
                        sbuf[5] = (byte) ('A' + sum2);
                        sbuf[6] = (byte) ('A' + sum3);
                        sbuf[7] = (byte) ('A' + sum4);
                        sbuf[8] = (byte) (' ');
                        
                    }
                    else { oboutmessage(outBuf,0,outBufLength-2); }
                    for (int i = 0; i < outBufLength; i += XmitQuantum)
                    {
                        int rem = outBufLength - i;
                        int packet = Math.min(XmitQuantum, rem);

                        if (split)
                        {
                            count_writes++;
                            sum_writes += 9;
                            os.write(csBuf, 0, 9);
                        }
                        else
                        {
                            //os.write(outBuf,i,12); i+=12; packet-=12;
                        }

                        count_writes++;
                        sum_writes += (packet-i);
                        last_write = G.Date();
                        os.write(outBuf, i, packet);
                        if(writeErr!=0) { writeErr=0; throw new IOException(); }

                        //for(int j=0;j<packet;j++) { outBuf[i+j]='x'; }
                    }
                }
                    //System.out.println("w :"+theBuff);
           }
      }
            catch (IOException e)
            {	
                if (!getExitFlag())
                {
                    String msg = //outQueue.stateSummary(": stream write." +
                            //inQueue.stateSummary("")+
                            "stream write: "+e;

                    //e.printStackTrace();
                    //System.out.println("M: " + e.getMessage());
                     setExitFlag(msg);

                    //theRoot.postError(this,msg,e,pings);
                }

                return (false);
            }
 		   catch (ThreadDeath err) { throw err;}
           catch (Throwable e)
           {
        	Plog.log.addLog("Unusual write error ",e," ",os," ",theBuffStr);
        	System.out.println(Plog.log.getLog());
        	setExitFlag("Unusual write error:"+e);
            Http.postError(this,"Unusual write error:",e);
            return(false);
           }
        return (true);
    }

/*
    public void show(byte buf[],int off,int len)
    {
    	StringBuilder b = new StringBuilder(" o : ");
    	for(int i=0;i<len;i++)
    	{
    		int ch = buf[off+i];
    		if((ch>='A' && ch<='Z') || (ch>='a' && ch<='z')) { b.append((char)ch); }
    		else { b.append(""+ch); }
    		b.append(" ");
    	}
    	G.print(b.toString());
    }
  */ 
    int byteBufSize = 1024;
    byte []byteBuf = new byte[byteBufSize];
    int byteBufIndex=0;
    int byteBufLen = 0;
 
    void doReadStep(InputStream s)
    {
	   byte []inBuf = realInBuf;
	   int inBufLength = 0;
	   try
	   {
               boolean doneReading = false;

               while (!doneReading)
               {	if(delays>0) { G.doDelay(Random.nextInt(R,delays)); }
                   /* read blocks waiting for data */
               	byte newchar;
               	if(byteBufIndex>=byteBufLen)
               	{	while((byteBufIndex>=byteBufLen) && (byteBufLen>=0))
               		{ byteBufLen = s.read(byteBuf,0,byteBufSize);
               		  byteBufIndex = 0;
               		}
               		if (byteBufLen < 0)
                       { //-1 is eof, 0 should be impossible
                           if (!eofok)
                           {
                               setExitFlag(myName + " read EOF: ");
                           }
                           else
                           {
                               setExitFlag(null);
                           }
                           doneReading = true;
                           newchar = 0;
                       }
               		else { newchar = byteBuf[byteBufIndex++];  } 
               	}
               	else { // continue with the last read buffer
               		newchar = byteBuf[byteBufIndex++];
               	}
               	if(!doneReading)
               	{	inBuf[inBufLength++] = newchar;
                       if (newchar == 0x0a)
                       {	inBuf[inBufLength]=0;
                           doneReading = true;
                       } 
                   }
               }
           }
           catch (IOException e)
           {	
               if (!getExitFlag())
               {
                   if (!eofok)
                   {
                       String msg = 
                       	//inQueue.stateSummary(
                               ": stream read n=" + inBufLength + " "+e;
                       	//outQueue.stateSummary("") + "\n" + e;

                       //e.printStackTrace();
                       //System.out.println("M: " + e.getMessage());
                       //setExitFlag(msg);
                       setExitFlag(msg);

                       //theRoot.postError(this,msg,e,pings);
                   }
                   else
                   {
                       setExitFlag(null);
                   }
               }

           }
           catch (Throwable e)
           {	setExitFlag("Unusual read error@"+inBufLength+e);
               Http.postError(this,"Unusual read error@"+inBufLength,e);
           }

           if (inBufLength > 3)
           { //jdk1.1 allows this, but jdk1.02 lacks the constructor

               //putInputItem(new String(inBuf,0,inBufLength)); 
           	boolean cserr = false;
               int i = 0;
               String str = null;
               if ((inBuf[i] == '5') &&
               		(inBuf[i + 1] == (byte) '0') &&
                       (inBuf[i + 2] == (byte) '1') &&
                       (inBuf[i + 3] == (byte) ' '))
               {
                   int asum = checksumString(inBuf, i + 9,
                           inBufLength - (i + 9));
                   int xsum1 = inBuf[i + 4] - (byte) 'A';
                   int xsum2 = inBuf[i + 5] - (byte) 'A';
                   int xsum3 = inBuf[i + 6] - (byte) 'A';
                   int xsum4 = inBuf[i + 7] - (byte) 'A';
                   int xsum = ((xsum1 << 12) + (xsum2 << 8) +
                       (xsum3 << 4) + xsum4);
                   cserr = (xsum != asum);
                   i = i + 8; //leave the leading space
                   obinmessage(inBuf,i,inBufLength-i);
               }
               else
               {
                   i = 0;
                   obinmessage(inBuf,i,inBufLength-i);
                   cserr = false; //ordinary unchecksummed string
               }

               int ii = i;

              
               str = decodeAsUtf8(inBuf,ii,inBufLength);
               count_reads++;
               sum_reads += str.length();
               last_read = G.Date();
               if(!cserr) 
               	{ if(!discardInput) 
               		{
               		  String err = inQueue.putItem(str);
               		  boolean isError = (err != null);

                         if (isError)
                         {
                           setExitFlag(err);
                           Http.postError(this, err, null);
                         }
               		}
               	  if(readSingle) 
               	  	{ synchronized (this) 
               		  { while(readSingle && (getMyInputStream()!=null) && !exitFlag) 
               		  	{  try {  wait(); } catch(InterruptedException e) {} }
               		  	} 
               	  	}
                	}
               //System.out.println("in:" + str.length()+" " +str);

               if (cserr && (read_errors++<MAX_READ_ERRORS))
               {
                   StringBuffer abuf = new StringBuffer();

                   for (int j = 0; j < ii; j++)
                   {
                       abuf.append((char) inBuf[j]);
                   }

                   String astr = abuf.toString();
                   String bstr = (str.length()>100) ? str.substring(0,99)+"..." : str;
                   logError("checksum error:" + astr + ": "+bstr,null);
               }
   }}
   
}
