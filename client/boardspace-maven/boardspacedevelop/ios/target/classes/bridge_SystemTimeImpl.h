#import <Foundation/Foundation.h>

@interface bridge_SystemTimeImpl : NSObject {
}

-(long long)currentNanoTime;
-(BOOL)isSupported;
@end
