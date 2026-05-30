#import <Foundation/Foundation.h>

@interface bridge_NativeServerSocketImpl : NSObject {
}

-(int)write:(int)param param1:(int)param1;
-(int)connect:(NSString*)param param1:(int)param1;
-(int)listen;
-(int)read:(int)param;
-(int)flush:(int)param;
-(int)writeArray:(int)param param1:(NSData*)param1 param2:(int)param2 param3:(int)param3;
-(int)readArray:(int)param param1:(NSData*)param1 param2:(int)param2 param3:(int)param3;
-(int)closeSocket:(int)param;
-(NSString*)getIOExceptionMessage:(int)param;
-(int)bindSocket:(int)param;
-(int)closeOutput:(int)param;
-(int)unBind;
-(int)getOutputHandle:(int)param;
-(int)closeInput:(int)param;
-(int)getInputHandle:(int)param;
-(BOOL)isSupported;
@end
