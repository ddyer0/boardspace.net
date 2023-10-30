#import <Foundation/Foundation.h>

@interface bridge_InstallerPackageImpl : NSObject {
}

-(NSString*)getPackages;
-(NSString*)getOSInfo;
-(NSString*)getHostName;
-(int)getOrientation;
-(int)setOrientation:(BOOL)param param1:(BOOL)param1;
-(NSString*)getInstaller:(NSString*)param;
-(NSString*)getLocalWifiIpAddress;
-(double)getScreenDPI;
-(BOOL)isSupported;
-(void)hardExit;
@end
