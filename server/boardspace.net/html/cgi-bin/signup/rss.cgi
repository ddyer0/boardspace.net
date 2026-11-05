#!/usr/bin/perl

package sign;

use CGI::Carp qw(fatalsToBrowser);
use CGI qw(:standard);
use strict vars;
use HTML::Entities;


sub doit()
{
    my $remove = param('remove');
    my $add = param('add');
    my $name = encode_entities(param('name'));
    my $password = encode_entities(param('password'));
    my $comment = encode_entities(param('comment'));
    my $result = "";
    my $total = 0;
    print "Content-type: text/html\n\n";
    print "<h2>LAX ultimate signup sheet for </h2>\n";
    print "<form action=$ENV{'SCRIPT_NAME'} method=post>\n";
    #print "<form action=/cgi-bin/util/show-form.pl method=post>\n";
    print "<table><tr>";
    print "<td>Name:</td>";
    print "<td><input type=text name=name value='$name'></td>";
    print "<td>this will appear in the list</td>";
    print "</tr><tr>\n";
    print "<td>Password:</td>";
    print "<td><input type=text name=password value='$password'></td>";
    print "<td>this can be anything you want</td>";
    print "</tr><tr>";
    print "<td>comment:</td>";
    print "<td colspan=2><input type=text size=80 name=comment></td>";
    print "</tr><td><input type=submit name=add value='Add me'></td>";
    print "<td><input type=submit name='remove' value='Remove Me'></td>";
    print "</tr></table><p>";
    print "Who's in so far: <br>";
    print "<table border=1>";
    print "<tr><td align=center>Who</td><td align=left>&nbsp;&nbsp;comments</td></tr>\n";
open (INFILE ,"<data.txt");
while (<INFILE>)
{
 my @data = split(/</,"$_"); # @data is an array containing the fields in one line of data.txt.   
 my $dname = $data[0];
 my $dpassword = $data[1];
 my $dcomment = $data[2];
 if($#data>1)
 {
 if($remove && (lc($dname) eq lc($name)) && (lc($password) eq lc($dpassword)))
     { $dname=''; 
     }
 elsif ($add && $name && (lc($name) eq lc($dname)) && (lc($password) eq lc($dpassword)))
     {  $dname = $name;
        $dpassword = $password;
        $dcomment = $comment;
        $name='';
     }
 elsif($add && $name && (lc($name) lt lc($dname)))
     { # add a new signee in alphabetical order
       $result = $result . "$name<$password<$comment\n"; 
       print "<tr><td>$name</td><td>$comment</td></tr>\n"; 
       $total++;
       $name = '';
     }
 if($dname) 
     { $result = $result . "$dname<$dpassword<$dcomment\n"; 
       print "<tr><td>$dname</td><td>$dcomment</td></tr>\n"; 
       $total++;
     }
 }

}
close INFILE;

     if($add && $name) # finally, add new entry to the end of the list
     { $result = $result . "$name<$password<$comment\n"; 
       print "<tr><td>$name</td><td>$comment</td></tr>\n"; 
       $total++;
     }
print "<tr><td>Total is $total</td></tr>\n";
    print "</table>\n";

if($add || $remove)
{   open (OUTFILE ,">data.new");
    print OUTFILE $result;
    close OUTFILE;
    unlink("data.old");
    rename("data.txt","data.old");
    rename("data.new","data.txt");
}


}


&doit();

