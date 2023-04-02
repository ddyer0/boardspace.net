package lib;

import bridge.*;
import java.io.*;
/**
 * this version of network connection uses "MixedPacket" as the 
 * transport mechanism, each mixed packet contains a string
 * intended to be parsed using nextToken() and an optional 
 * binary payload.
 * 
 * @author Ddyer
 *
 */
public class NetPacketConn extends CommonNetConn<MixedPacket> implements Runnable,Config
{
	// for debugging 
	private static int HeaderSize = 12;
    private byte[] outHeaderBuf = new byte[HeaderSize];	// 2 integers worth
    private byte[] inHeaderBuf = new byte[HeaderSize];    
    private byte[] realInBuf;
    private int inputSequence = 0;
    private int outputSequence = 0;
    
	public NetPacketConn(ConnectionManager man,String me)
	{	super(man,me);
	}
	
    public void setBufSize(int n)
    {
        BufferLength = n;
        if((realInBuf==null) || (n != realInBuf.length))
        {
        	realInBuf = new byte[n];
        }
    }

   
    public boolean sendMessage(String m)
    {
    	return sendMessage(new MixedPacket(m));
    }
    public boolean sendMessage(String m,byte[]payload)
    {
    	return(sendMessage(new MixedPacket(m,payload)));
    }
    
    // this outputs a packet for real
    boolean realSendMessage(OutputStream os,MixedPacket packet)
    {
    String message = packet.message;
     try
     {
      byte [] payload = packet.payload;
      
      //
      // new strategy 3/1017, convert the string to utf8
      // so the byte level stream will be all single bytes
      // the old format for multi-byte strings will never occur
      //
      byte[] theBuff = message.getBytes("UTF-8");
      byte[] outBuf = outHeaderBuf;
      int outidx = 0;
      int msgLen = theBuff.length;
      outputSequence++;
      {   int seq = packet.sequence = outputSequence;
          for(int i=0;i<4;i++) { outBuf[outidx++] = (byte)(seq&0xff); seq=seq>>8; }    	  
      }
      
      {
      int len = msgLen;
      for(int i=0;i<4;i++) { outBuf[outidx++] = (byte)(len&0xff); len=len>>8; }
      }
     
      int payLen = (payload!=null) ? payload.length : 0; 
      {
       int plen = payLen;
       for(int i=0;i<4;i++) { outBuf[outidx++] = (byte)(plen&0xff); plen=plen>>8; }
      }
      
      // let TCP do the work, it's reliable (it says here)
      //Plog.log.addLog("tcp write ",outBuf," 0 ",outidx);
      os.write(outBuf,0,outidx);
      if(msgLen>0) 
      	{ 
          //Plog.log.addLog("tcp write msg ",theBuff," 0 ",msgLen);
    	  os.write(theBuff,0,msgLen); 
      	}
      if(payLen>0) 
      	{
    	  //Plog.log.addLog("tcp write payload ",payload," 0 ",payLen);
      	 
    	  os.write(payload,0,payLen);
      	}
      }
     catch (IOException e)
     {	
    	 if (!getExitFlag())
    	 {
    		 String msg = "stream write:("+message+") "+e;
    		 setExitFlag(msg);
    	 }
    	 return (false);
     }
     catch (Throwable e)
     { setExitFlag("Unusual write error:"+e);
       Http.postError(this,"Unusual write error:",e);
       return(false);
     }
     return (true);
    }



    private void readCompletely(InputStream s,byte []buffer,int offset,int len) throws IOException
    {
    	while(len>0)
    	{
    		int n = s.read(buffer,offset,len);
    		if(n>0)
    		{
    		offset += n;
    		len -= n;
    		}
    		else { throw new IOException("unexpected end of data");}
    	}
    }

   void doReadStep(InputStream s)
   {
	   int messageLen=0;
	   int payloadLen=0;
	   int sequence = 0;
	   int idx = HeaderSize;
	   try {
		readCompletely(s,inHeaderBuf,0,HeaderSize);
        while(idx>8) { payloadLen = (payloadLen<<8) | (0xff&inHeaderBuf[--idx]); }
        while(idx>4) { messageLen = (messageLen<<8) | (0xff&inHeaderBuf[--idx]); }
		while(idx>0) { sequence = (sequence<<8) | (0xff&inHeaderBuf[--idx]); }

       	if(sequence!=inputSequence+1)
       	{	// this should never happen.  If it does, either the network
       		// is unreliable, or someone is injecting his own messages.
       		G.print("break in sequence, "+sequence+" follows "+inputSequence);
       	}
   		inputSequence = sequence;
       	byte []inBuf = realInBuf;
       	MixedPacket packet = new MixedPacket();
       	packet.sequence = sequence;
       	if(messageLen<inBuf.length)
       	{
       		readCompletely(s,inBuf,0,messageLen);
       		packet.message = decodeAsUtf8(inBuf,0,messageLen);
       	}
       	else { 
          		byte []messageLoad = new byte[messageLen];
          		readCompletely(s,messageLoad,0,messageLen);
          		packet.message = decodeAsUtf8(messageLoad,0,messageLen);
       	}
       	
       	if(payloadLen>0)
       	{
       		byte []payload = new byte[payloadLen];
       		readCompletely(s,payload,0,payloadLen);
       		packet.payload = payload;
       	}
          	count_reads++;
           sum_reads += payloadLen+messageLen;
           last_read = G.Date();
           if(!discardInput)
           	{String err = inQueue.putItem(packet);
           	 if(err!=null) { setExitFlag(err); }
           	}
       	}
           catch (IOException e)
           {	
               if (!getExitFlag())
               {
                   if (!eofok)
                   {
                       String msg = ": stream read message="+messageLen+" payload = "+payloadLen;
                       setExitFlag(msg+" : "+e.toString());
                   }
               	   setExitFlag(null);
               }
           }
           catch (Throwable e)
           {	String err = "Unusual read error@"+messageLen+"payload = "+payloadLen;
           	setExitFlag(err);
               Http.postError(this,err,e);
           }
   }
   }