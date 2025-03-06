#import "udp_UdpListenerImpl.h"

@implementation udp_UdpListenerImpl

 #include <arpa/inet.h>
 #include <netinet/in.h>
 #include <stdio.h>
 #include <sys/types.h>
 #include <sys/socket.h>
 #include <unistd.h>
 #define BUFLEN 512

NSMutableArray<NSString *>* messages ;
BOOL exitRequest = false;
int sockn = -1;
BOOL filter = false;
char senderId[50];
dispatch_semaphore_t waiting;

// codename1 magic to make the garbage collector happy while we wait
static void _yield() {
    CN1_YIELD_THREAD;
}

static void _resume() {
    CN1_RESUME_THREAD;
}
// get something and/or wait
-(NSString*)getMessage:(int)waitTime
{   NSString *m=nil;
    if(!exitRequest && messages!=nil)
    { 
    if((waitTime>=0) && ([messages count]==0))
    {
    _yield();		// codename1 magic to mark an inactive thread
    dispatch_time_t timeout = (waitTime==0)
    				 ? DISPATCH_TIME_FOREVER
    				 : (DISPATCH_TIME_NOW + (waitTime * (NSEC_PER_SEC/1000)));        
    dispatch_semaphore_wait(waiting, timeout);
    _resume();
    }
     @synchronized (self)
     {
     if([messages count]!=0) { m = messages[0]; [messages removeObjectAtIndex:0];  }
     }
    }
    return(m);
}
// add a message to the queue
-(void)addObject : (NSString *)m
{	@synchronized(self)	
	{
	[messages addObject:m];
	dispatch_semaphore_signal(waiting);
	}
}

-(id)init
{ messages =  [[NSMutableArray alloc] init];
  sprintf(senderId,"S%ld:",arc4random()&0x7fffffffffffffff);
  waiting = dispatch_semaphore_create(0);
  sockn = -1;
  return(self);
}


-(void)stop{
	exitRequest = true;
	if(sockn>=0) { close(sockn); }
	dispatch_semaphore_signal(waiting);
	sockn=-1;
}

-(BOOL)sendMessage:(NSString*)msg param1:(int)port
 {
    BOOL ok = false;
    //unsigned int wifiInterface = if_nametoindex("en0");
    struct sockaddr_in si_other;
    int s, i;
    unsigned int slen=sizeof(si_other);
    char buf[BUFLEN];
    if ((s=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP))<0)
    {[self addObject : @"error:socket creation failed"];
     return(ok); 
    }
  
    static const int kOne = 1;  
    int success1 = setsockopt(s, SOL_SOCKET, SO_BROADCAST, &kOne, sizeof(kOne)) == 0;
    if(!success1) { [self addObject : @"error:set broadcast failed"]; return(ok); }
  
     memset((char *) &si_other, 0, slen);
     si_other.sin_family = AF_INET;
     si_other.sin_len = slen;
     si_other.sin_addr.s_addr = INADDR_BROADCAST;
     si_other.sin_port = htons(port);
  
     sprintf(buf, "%s%s",(filter?senderId:""),[msg UTF8String]);
      if (sendto(s, buf, strlen(buf), 0, (struct sockaddr *)&si_other, slen)<0)
	  {  [self addObject : @"error:sendto failed"];
 	     return(ok); 
	  }
     close(s);
     ok = true;
     return(ok);
}

// bind the receiver and run the listen loop
-(void)runBroadcastReceiver:(int)port param1:(BOOL)fil
{
   struct sockaddr_in si_me;
   int i;
   static const int kOne = 1;
   unsigned int slen=sizeof(si_me);
   filter = fil;
   exitRequest = false;
   if ((sockn=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP))<0)
   { [self addObject:@"error: create socket failed"];
      return;
       
   }
 
    memset((char *) &si_me, 0, sizeof(si_me));
    si_me.sin_family = AF_INET;
    si_me.sin_port = htons(port);
    si_me.sin_addr.s_addr = htonl(INADDR_ANY);
    si_me.sin_len = slen;

    if (bind(sockn, (struct sockaddr *)&si_me, sizeof(si_me))<0)
    {[self addObject:@"error: sock bind failed"];
     return;
    }

    do { [self broadcastRecv] ; } while (!exitRequest); 
 }
 // receive a message
 -(BOOL)broadcastRecv
 {
 if(sockn>=0)
  {	struct sockaddr_in si_other;
    unsigned int slen = sizeof(si_other);
    char buf[BUFLEN];
    _yield();	// codename1 magic to mark an inactive thread
    ssize_t siz = recvfrom(sockn, buf, BUFLEN, 0, (struct sockaddr *)&si_other, &slen);
    _resume();
    if(siz>=0)
     {
      char str[INET_ADDRSTRLEN];
      buf[siz]=(char)0;
      inet_ntop(AF_INET,&si_other.sin_addr,str,INET_ADDRSTRLEN);
      char *idx = buf;
	if(filter && buf[0]=='S')
	{	if(strcmp(buf,senderId)!=0)
		{
		idx = index(buf,':');
		if(idx==nil) { idx = buf; } else { idx++; }
		}
		else { idx = nil; }
	}
	if(idx!=nil)
	{
	NSString *recv = [NSString stringWithFormat:@"%s:%s",str,idx];
	[self addObject: recv];
	return(YES);
	}
    }}
    return(NO);
}

-(BOOL)isSupported
{
    return YES;
}

@end
