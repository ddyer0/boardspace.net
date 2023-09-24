/**
 * Native Logs Reader
 * Written in 2018 by Francesco Galgani, https://www.informatica-libera.net/
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
#import "net_informaticalibera_cn1_nativelogreader_NativeLogsReaderImpl.h"

@implementation net_informaticalibera_cn1_nativelogreader_NativeLogsReaderImpl

// Useful information:
// Log iOS Errors to File
// https://www.progressconcepts.com/blog/log-ios-errors-file/

-(NSString*)getFilePath{
    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,NSUserDomainMask, YES);
    NSString* documentsDirectory = [paths objectAtIndex:0];
    NSString* fileName = @"device.log";
    NSString* logFilePath = [documentsDirectory stringByAppendingPathComponent:fileName];
    return logFilePath;
}

-(NSString*)readLog{
    NSString* logFilePath = [self getFilePath];
    NSString* content = [NSString stringWithContentsOfFile:logFilePath encoding:NSUTF8StringEncoding error:nil];
    return content;
}

-(void)clearAndRestartLog{
    NSError *error = nil;
    NSString* logFilePath = [self getFilePath];
    [[NSFileManager defaultManager] removeItemAtPath:logFilePath error:&error];

    // wrap the code to check if the debugger was attached, and only write to the file when it wasn’t
    if (!isatty(STDERR_FILENO)) { 
        freopen([logFilePath cStringUsingEncoding:NSUTF8StringEncoding],"a+",stdout);
        freopen([logFilePath cStringUsingEncoding:NSUTF8StringEncoding],"a+",stderr);
    }
}

-(BOOL)isSupported{
    return YES;
}

@end
