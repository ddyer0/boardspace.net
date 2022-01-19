#import "bridge_InstallerPackageImpl.h"

@implementation bridge_InstallerPackageImpl

-(NSString*)getPackages{
    return nil;
}

-(NSString*)getOSInfo{
    return nil;
}

-(NSString*)getHostName{
    return nil;
}

-(int)getOrientation{
    return 0;
}
-(double)getScreenDPI{
    return(96.0);
}

-(int)setOrientation:(BOOL)param param1:(BOOL)param1{
    return 0;
}

-(NSString*)getInstaller:(NSString*)param{
    return nil;
}

-(NSString*)getLocalWifiIpAddress{
    return nil;
}
-(void)hardExit {
}

-(BOOL)isSupported{
    return NO;
}

@end
