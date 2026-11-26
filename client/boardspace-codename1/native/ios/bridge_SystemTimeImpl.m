#import "bridge_SystemTimeImpl.h"
#include <mach/mach_time.h>

@implementation bridge_SystemTimeImpl

-(long long)currentNanoTime{
	uint64_t mach_time = mach_absolute_time();
 	static mach_timebase_info_data_t _clock_timebase;
	if(_clock_timebase.denom == 0 ) 
		{
        	mach_timebase_info(&_clock_timebase); // Initialize timebase_info
    		}    	
    	double nanos = (mach_time * _clock_timebase.numer) / _clock_timebase.denom;
        return((long long)nanos);
}

// MyClass.m (native iOS implementation)
-(BOOL)isRunningOnMac {
    if (@available(iOS 14.0, *)) {
        return [[NSProcessInfo processInfo] isiOSAppOnMac];
    }
    return NO;
}

-(BOOL)isSupported{
    return YES;
}
@end
