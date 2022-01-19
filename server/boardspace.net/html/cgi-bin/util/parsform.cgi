############################################################################
#                                                                          #
# parse_form()                      Version 1.5                            #
# Written by Matthew Wright         mattw@worldwidemart.com                #
# Created 9/30/96                   Last Modified 3/28/97                  #
#                                                                          #
# Copyright 1997 Craig Patchett & Matthew Wright.  All Rights Reserved.    #
# This subroutine is part of The CGI/Perl Cookbook from John Wiley & Sons. #
# License to use this program or install it on a server (in original or    #
# modified form) is granted only to those who have purchased a copy of The #
# CGI/Perl Cookbook. (This notice must remain as part of the source code.) #
#                                                                          #
# Function:      Takes form field data from a POST or GET request and      #
#                converts it to name/value pairs in the %FORM array or,    #
#                if a corresponding entry in the %CONFIG array is defined, #
#                in %CONFIG.                                               #
#                                                                          #
# Usage:         &parse_form;                                              #
#                                                                          #
# Variables:     None                                                      #
#                                                                          #
# Returns:       0 if invalid request method                               #
#                1 if successful                                           #
#                                                                          #
# Uses Globals:  Sets %CONFIG with name/value pairs if corresponding entry #
#                  in %CONFIG is defined                                   #
#                Otherwise sets entries in %FORM                           # 
#                $Error_Message for descriptive error messages             #
#                                                                          #
# Files Created: None                                                      #
#                                                                          #
############################################################################


sub parse_form {
    local($name, $value, $pair, $buffer, @pairs);
    
    # Check for request method and handle appropriately
    
    if ($ENV{'REQUEST_METHOD'} eq 'GET') {
        @pairs = split(/&/, $ENV{'QUERY_STRING'});
    }
    elsif ($ENV{'REQUEST_METHOD'} eq 'POST') {
        read(STDIN, $buffer, $ENV{'CONTENT_LENGTH'});
        @pairs = split(/&/, $buffer);
    }
    else {
        $Error_Message = "Bad request method ($ENV{'REQUEST_METHOD'}).  Use POST or GET";
        return(0);
    }

    # Convert the data to its original format
    
    foreach $pair (@pairs) {
        ($name, $value) = split(/=/, $pair);

        $name =~ tr/+/ /;
        $name =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
        $name =~ s/\n//g;
        $value =~ tr/+/ /;
        $value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
        $value =~ s/\n//g;

        # If they try to include server side includes, erase them, so they
        # arent a security risk if the HTML gets returned.  Another
        # security hole plugged up.
        
		  $value =~ s/[;<>*`|&$#]//g;

        # Store name/value pair in %CONFIG if the corresponding entry is
        # defined
        
        if ($CONFIG{$name} && $CONFIG_INIT{$name}) {
            $CONFIG{$name} .= ",$value";
        }
        elsif (defined($CONFIG{$name})) {
            $CONFIG{$name} = $CONFIG_INIT{$name} = $value;
        }
        
        # Otherwise store in %FORM
        
        elsif ($FORM{$name}) {
            $FORM{$name} .= ",$value";
        }
        else {
            $FORM{$name} = $value;
        }
    }
    return(1);
}

1;
