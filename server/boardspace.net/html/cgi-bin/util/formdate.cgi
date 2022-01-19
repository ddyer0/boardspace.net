############################################################################
#                                                                          #
# format_date()                     Version 1.5                            #
# Written by Craig Patchett         craig@patchett.com                     #
# Created 8/15/96                   Last Modified 5/31/97                  #
#                                                                          #
# Copyright 1997 Craig Patchett & Matthew Wright.  All Rights Reserved.    #
# This subroutine is part of The CGI/Perl Cookbook from John Wiley & Sons. #
# License to use this program or install it on a server (in original or    #
# modified form) is granted only to those who have purchased a copy of The #
# CGI/Perl Cookbook. (This notice must remain as part of the source code.) #
#                                                                          #
# Function:      Formats the date and time according to a format string.   #
#                                                                          #
# Usage:         &format_date($date[, $format, $gmt]);                     #
#                                                                          #
# Variables:     $date --       Date to format in time() format            #
#                               Example: 839120118                         #
#                $format --     Format specification string. The following #
#                               variables can be used within the string:   #
#                $gmt --        Set to non-null if GMT should be used      #
#                                                                          #
#                               <h> hour without padding (i.e. 6, 12)      #
#                               <0h> hour with padding (i.e. 06, 12)       #
#                               <mh> military hour w/ padding (i.e. 06,23) #
#                               <n> minutes without padding (i.e. 6, 35)   #
#                               <0n> minutes with padding (i.e. 06, 35)    #
#                               <s> seconds without padding (i.e. 6, 35)   #
#                               <0s> seconds with padding (i.e. 06, 35)    #
#                               <ap> am/pm according to hour               #
#                               <ap.> a.m./p.m. according to hour          #
#                               <time> time w/o seconds (i.e. 6:12 p.m.)   #
#                               <timesec> time w/secs (i.e. 6:12:14 p.m.)  #  
#                               <mtime> military time w/o secs (i.e. 18:12)#  
#                               <mtimesec> military time w/secs            #
#                                         (i.e. 18:12:14)                  #
#                               <m> month without padding (i.e. 6, 12)     #  
#                               <0m> month with padding (i.e. 06,12)       #
#                               <mon> 3-character month name (i.e. Jan)    #
#                               <month> full month name (i.e. January)     #
#                               <d> day of month w/o padding (i.e. 6, 23)  #
#                               <0d> day of month w/padding (i.e. 06, 23)  #
#                               <df> day of month with suffix (i.e. 23rd)  #
#                               <wd> weekday (0 - 7)                       #
#                               <wday> 3-character weekday name (i.e. Fri) #
#                               <weekday> full weekday name (i.e. Friday)  #
#                               <yr> 2-digit year (i.e. 96)                #
#                               <year> 4-digit year (i.e. 1996)            #
#                               <yd> day of year w/o padding (i.e. 6, 300) #
#                               <0yd> day of year w/padding (i.e. 006, 300)#
#                               <ydf> day of year with suffix (i.e. 300th) #
#                               <dst> dst if daylight savings time or blank#
#                               <dstnot> 'not ' or blank                   #
#                                                                          #
#                               In addition, writing a variable name in    #
#                               uppercase will result in any letters in    #
#                               the value of the variable to appear in     #
#                               uppercase.                                 #
#                               i.e. <WEEKDAY> will result in MONDAY       #
#                                                                          #
#                               Any text in $format that does not match a  #
#                               variable name will be left alone.          #
#                                                                          #
#                               Note: If <m> or <0m> appear after a : they #
#                               be interpreted as minutes, not month.      #
#                                                                          #
# Returns:       $format with all variables replaced w/appropriate values  #
#                If $format is omitted then an associative array will be   #
#                   returned with the lowercase variables (no brackets) as # 
#                   the keys and their corresponding values as the values. #  
#                A null string or array if $date is null or zero.          #
#                No other error checking is performed                      #
#                                                                          #
# Uses Globals:  @DAYS   -- Array containing full names of weekdays        #
#                           (Sunday first)                                 #
#                @MONTHS -- Array containing full names of months (January #
#                           first)                                         #
#                                                                          #
# Files Created: None                                                      #
#                                                                          #
############################################################################


sub format_date {
    
    # Get the arguments
    
    local($date, $format, $gmt) = @_;
    local(@suffix) = ('th', 'st', 'nd', 'rd');
    local(%date_vars, $result, $var, $upper, $last_digit, $suffix);
    local($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst);
    
    # Create default arrays if necessary
    
    if (!@DAYS) {
		@DAYS = ('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday',  
		         'Friday', 'Saturday');
    }
    if (!@MONTHS) {
		@MONTHS = ('January', 'February', 'March', 'April', 'May', 'June',  
		           'July', 'August', 'September', 'October', 'November',
		           'December');
    }

    # Convert the date into local time or GMT if a date was given
    
    if (!$date) { return ($format ? '' : %date_vars) }
    if ($gmt) {
        ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = gmtime($date);
    }
    else {
        ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime($date);
    }
    
    # Calculate all our variables
    
    $date_vars{'mh'} = sprintf("%02d", $hour);
    $date_vars{'mtime'} = sprintf("%02d:%02d", $hour, $min);
    $date_vars{'mtimesec'} = sprintf("%02d:%02d:%02d", $hour, $min, $sec);
    $date_vars{'ap'} = ($hour > 11) ? 'pm' : 'am';
    $date_vars{'ap.'} = ($hour > 11) ? 'p.m.' : 'a.m.';
    if ($hour > 12) { $hour -= 12 }
    elsif ($hour == 0) { $hour = 12 }
    $date_vars{'h'} = $hour;
    $date_vars{'0h'} = sprintf("%02d", $hour);
    $date_vars{'n'} = $min;
    $date_vars{'0n'} = sprintf("%02d", $min);
    $date_vars{'s'} = $sec;
    $date_vars{'time'} = sprintf("%d:%02d %s", $hour, $min, $date_vars{'ap.'});
    $date_vars{'timesec'} = sprintf("%d:%02d:%02d %s", $hour, $min, $sec, $date_vars{'ap.'});
    $date_vars{'0s'} = sprintf("%02d", $sec);
    $date_vars{'m'} = $mon + 1;
    $date_vars{'0m'} = sprintf("%02d", $mon + 1);
    $date_vars{'mon'} = substr($MONTHS[$mon], 0, 3);
    $date_vars{'month'} = $MONTHS[$mon];
    $date_vars{'d'} = $mday;
    $date_vars{'0d'} = sprintf("%02d", $mday);
    $last_digit = substr($mday, -1);
    if ((($mday < 11) || ($mday > 20)) && $last_digit < 4) {
        $date_vars{'df'} = $mday . $suffix[$last_digit];
    }
    else { $date_vars{'df'} = $mday . 'th' }
    $date_vars{'wd'} = $wday;
    $date_vars{'wday'} = substr($DAYS[$wday], 0, 3);
    $date_vars{'weekday'} = $DAYS[$wday];
    $year = sprintf("%02d", $year);
    $date_vars{'yr'} = $year % 100;
    $date_vars{'year'} = 1900 + $year;
    $date_vars{'yd'} = $yday;
    $date_vars{'0yd'} = sprintf("%03d", $yday);
    $last_digit = substr($yday, -1);
    if ((($yday % 100 < 12) || ($yday % 100 > 20)) && $last_digit < 4) {
        $date_vars{'ydf'} = $yday . $suffix[$last_digit];
    }
    else { $date_vars{'ydf'} = $yday . 'th' }
    $date_vars{'dst'} = $isdst ? 'dst' : '';
    $date_vars{'dstnot'} = $isdst ? '' : 'not ';

    # Scan the format string for variables, replacing them as we go
    
    while ($format =~ /<([^>]+)>/) {
    
        # Check if the variable name is in uppercase
        
        $_ = $1;
        $upper = tr/A-Z/a-z/;
        
        # Make an effort to correct m where n was intended

        if (substr($_, -1) eq 'm') {
            if ($` eq ':') { substr($_, -1) = 'n' };
        }

        # If the variable name is in uppercase convert the value to uppercase
        
        if ($upper) { 
            $var = $date_vars{$_};
            $var =~ tr/a-z/A-Z/;
            $result .= $` . $var;
        }
        else { $result .= $` . $date_vars{$_} }
        
        # Change format to the rest of the string after the match
        
        $format = $';
    }

    # Set $result according to whether or not any matches were found
    
    $result .= $format;
    
    # Return the result or the variable array depending how the routine was called
    
    if ($result) { $result }
    else { %date_vars }
}

1;
