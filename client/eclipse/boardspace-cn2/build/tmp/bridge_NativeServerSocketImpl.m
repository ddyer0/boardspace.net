#import "bridge_NativeServerSocketImpl.h"

@implementation bridge_NativeServerSocketImpl

-(int)write:(int)param param1:(int)param1{
    return 0;
}

-(int)connect:(NSString*)param param1:(int)param1{
    return 0;
}

-(int)listen{
    return 0;
}

-(int)read:(int)param{
    return 0;
}

-(int)flush:(int)param{
    return 0;
}

-(int)writeArray:(int)param param1:(NSData*)param1 param2:(int)param2 param3:(int)param3{
    return 0;
}

-(int)readArray:(int)param param1:(NSData*)param1 param2:(int)param2 param3:(int)param3{
    return 0;
}

-(int)closeSocket:(int)param{
    return 0;
}

-(NSString*)getIOExceptionMessage:(int)param{
    return nil;
}

-(int)bindSocket:(int)param{
    return 0;
}

-(int)closeOutput:(int)param{
    return 0;
}

-(int)unBind{
    return 0;
}

-(int)getOutputHandle:(int)param{
    return 0;
}

-(int)closeInput:(int)param{
    return 0;
}

-(int)getInputHandle:(int)param{
    return 0;
}

-(BOOL)isSupported{
    return NO;
}

@end
