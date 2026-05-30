#import <Foundation/Foundation.h>

@interface udp_UdpListenerImpl : NSObject {
}

-(NSString*)getMessage:(int)param;
-(void)stop;
-(BOOL)sendMessage:(NSString*)param param1:(int)param1;
-(void)runBroadcastReceiver:(int)param param1:(BOOL)param1;
-(BOOL)isSupported;
@end
