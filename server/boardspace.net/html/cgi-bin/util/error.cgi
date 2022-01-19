############################################################################
#                                                                          #
# error()                           Version 1.0                            #
# Written by Craig Patchett         mattw@worldwidemart.com                #
# Created 9/9/96                    Last Modified 3/23/97                  #
#                                                                          #
# Copyright 1997 Craig Patchett & Matthew Wright.  All Rights Reserved.    #
# This subroutine is part of The CGI/Perl Cookbook from John Wiley & Sons. #
# License to use this program or install it on a server (in original or    #
# modified form) is granted only to those who have purchased a copy of The #
# CGI/Perl Cookbook. (This notice must remain as part of the source code.) #
#                                                                          #
# Function:      Prints an HTML error message page to STDOUT               #
#                                                                          #
# Usage:         &error([$error_msg, $error_title, $error_name, $no_mime]);#
#                                                                          #
# Variables:     $error_msg --   Error message                             #
#                                Defaults to 'Sorry, an unexpected error   #
#                                occurred.'                                #
#                $error_title -- Title for HTML page                       #
#                                Defaults to 'UNEXPECTED ERROR'            #
#                $error_name --  Error name                                #
#                                Defaults to $page_title                   #
#                $no_mime --     Flag indicating that MIME header is not   #
#                                needed                                    #
#                                Defaults to 0 (print MIME header)         #
#                                                                          #
# Returns:       Does not return. Calling this subroutine displays the     #
#                page and exits the program.                               #
#                                                                          #
# Uses Globals:  $VAR{} --       Used to pass variables to parse_template()#
#                $ERROR_PAGE --  Full path of template file to use for     #
#                                error page. It can include the following  #
#                                template variables, which will be         #
#                                replaced with the corresponding text      #
#                                before the page is displayed:             #
#                                                                          #
#                                <<ERROR_TITLE>> Title for page            #
#                                <<ERROR_NAME>>  Name of error             #
#                                <<ERROR_MSG>>   Error message             #
#                                                                          #
#                                Example: '/home/user/error/error.html'    #
#                                                                          #
# Files Created: None, but an HTML error page is printed to STDOUT         #
#                                                                          #
############################################################################


sub error {

    # Get the arguments, set defaults if arguments missing
    
    ($VAR{'ERROR_MSG'}, $VAR{'ERROR_TITLE'}, $VAR{'ERROR_NAME'}, $NO_MIME) = @_;
    if (!$VAR{'ERROR_MSG'}) { 
        $VAR{'ERROR_MSG'} = 'Sorry, an unexpected error occurred.';
    }
    if (!$VAR{'ERROR_TITLE'}) { 
       $VAR{'ERROR_TITLE'} = 'UNEXPECTED ERROR';
    }
    if (!$VAR{'ERROR_NAME'}) { 
       $VAR{'ERROR_NAME'} = $VAR{'ERROR_TITLE'};
    }
    
    # Check to make sure the error page template file exists
    
    if ($status = open(TEMPLATE, $ERROR_PAGE)) {
        close(TEMPLATE);
        
        # Print the MIME header, parse the file, and exit
    
        print "Content-type: text/html\n\n" unless $NO_MIME;
        &parse_template($ERROR_PAGE, *STDOUT);
        exit;
    }
    else {
        
        # The error page template file couldn't be found
        
        print "Content-type: text/html\n";
        print "Status: 404 Not Found\n\n";
        print "The error page template file ('$ERROR_PAGE') could not be opened ($status).\n";
        exit;
    }
}

1;
