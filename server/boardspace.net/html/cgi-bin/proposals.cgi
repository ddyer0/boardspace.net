#!/usr/bin/perl
#
# gs_createproposal.cgi
#
# creation and editing of proposals 3/2003
#
#

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Debug;
use strict;
use URI::Escape;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";

sub init {
	$| = 1;				# force writes
	__dStart( "$::debug_log", $ENV{'SCRIPT_NAME'} );
}

# print a row of a table, one cell per arg
sub printRow()
{ my @args = @_;
	print "<tr>";
	while($#args>=0)
		{ my $a = shift(@args);
		  print "<td align=left>$a</td>\n";
		}
	print "</tr>\n";
}

sub print_header
{	my ($op)=@_;

	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$op</TITLE>
Head_end

&standard_header();

print "<h1>$op</h1>\n";

}

# print links to variations on the summary
sub print_top_links()
{		my ($dead) = @_;
		my $super = &param('super');
		my $suplink = $super ? "&super=$super" : "";
		my $months = &param('months');
		my $thisurl = $ENV{'SCRIPT_NAME'} ;
	if($months) { $months="&months=$months"; };
	if($dead!=0) { print "<a href='$thisurl?dead=0$suplink$months'>View all supported proposals</a>&nbsp;&nbsp;\n"; }
	if($dead!=1) { print "<a href='$thisurl?dead=1$suplink$months'>View proposals with no support</a>&nbsp;&nbsp;\n";}
	if($dead!=2) {print "<a href='$thisurl?dead=2$suplink$months'>View implemented proposals</a>&nbsp;&nbsp;\n";}
}

# check login with pname and password, return UID or -1 and error reason
sub logon 
{
	my ($dbh,$pname,$passwd) = @_;
	my $uid = -1;
	my $reason="Username $pname and Password do not match";
	my $myaddr = $ENV{'REMOTE_ADDR'};
	{
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $sunset = &player_retired_time();
	my $q = "SELECT uid,games_played,locked,last_played FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok'";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my $minor=0;	# minor infraction
	if(($n==1) && &allow_ip_login($dbh,$myaddr))
	{
		my ($nuid,$played,$fixed,$last) = &nextArrayRow($sth);
		if(($played)<=10) 
			{$reason = "You haven't played enough games"; 
			 $minor=1;
			}
		elsif ((lc($fixed) eq 'y')) 
			{ $reason = "No, you can't make proposals this way"; 
			}
       elsif ($last < $sunset) 
      { $reason = "Sorry, retired players can't vote.  Play!"; 
      	$minor = 1;
      }
		else 
			{ #fully acceptable
			$uid = $nuid; 
			$reason = "";
			}
	}
	&finishQuery($sth);
	
	if(($n==1) && ($uid<0) && !$minor)
		{
		&note_failed_login($dbh,$myaddr,"createproposal as $pname");	
		}
	}
	return($uid,$reason)
}



%'allstatus = 
	( "new"=>"new",
		"accepted"=>"accepted",
	  "implemented"=>"implemented",
	  "commented"=>"commented",
	  "rejected"=>"rejected"
	);


sub print_select_menu()
{	my ($name,$def,%status_codes) = @_;
	my $key;
	my @keylist = keys(%status_codes);
	print "<select name='$name' >\n";
	foreach $key (@keylist) 
	{	my $val = $status_codes{$key};
		my $issel = (lc($val) eq lc($def)) ? "selected" : "";
		print "<option value='$val' $issel>$val</option>\n";
	}
	print "</select>\n";
}


# --------------------------------------------
sub do_gs_edit()
{
&init();

print header;
	my $dbh = &connect();
	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&readtrans_db($dbh);
	my $myaddr = $ENV{'REMOTE_ADDR'};

__d("$ENV{'REQUEST_URI'} $myaddr");

	my $thisurl = $ENV{'SCRIPT_NAME'} ;
	my $detail = param('view');
	my $fromname = param('fromname');
	my $passwd = param('passwd');
	my $subject = param('subject');
	my $message = param('message');
	my $approve = param('approve');
	my $deliver = param('deliver');
	my $body = param('body');
	my $cbody = param('cbody');
	my $date = param('date');
	my $escaped = param('escaped');
	my $cescaped = param('cescaped');
	my $delete = &param('delete');
	my $super = &param('super');
	my $spass = &param('spass');
  my $months = &param('months');
	my $status = &param('status');
	my $suplink = $super ? "&super=$super" : "";
	
	my $created = &date_string(time());
	my $ownerid = 0;
	
	if($date eq '') { $date = $created; }
	if($escaped) 
	{ 
	 $body = uri_unescape($body);
	 $subject = uri_unescape($subject); 
	}
	if($cescaped)
	{
	 $cbody = uri_unescape($cbody);
	}	
	&print_header($detail ? "Edit a Proposal for BoardSpace.net" : "Create a Proposal for BoardSpace.net");


	my ($uid,$reason) = ($detail || $approve || $deliver || $delete) ? &logon($dbh,$fromname,$passwd) : 0;
	if($detail && ($uid>0))
  	{	# if logon is ok, check ownership
   		my $qd = $dbh->quote($detail);
  		my $q = "SELECT ownerid,title,description,created,comment,status from proposals where uid=$qd";
  		my $sth = &query($dbh,$q);
  		my $n = &numRows($sth);
  		if($n>0)
  			{	my ($owner,$title,$description,$cre,$cb,$sta) = &nextArrayRow($sth);

  				if( $super ? ($::proposal_password && ($spass eq $::proposal_password)) : ($owner == $uid) )
  				{	# copy data from proposal to input parameters
  					if( ! ($approve || $deliver || $delete))
  						{$subject = $title;
  						 $body = $description;
  						 $status = $sta;
  						 $cbody = $cb;
  						}
  					# alwasy use creation date
  					$created = $cre;
  					$ownerid = $owner;
  				}
  				else
  				{	$uid = -1;
    				$reason = "You can only edit proposals that you own";
    				if($super) { &note_failed_login($dbh,$myaddr,"createproposal as supervisor $super"); }
  				}
 		
  	}
  	&finishQuery($sth);	
  }

  if($uid<0)
  		{ #tell him the bad news.
  			print "<p><b>$reason</b><p>"; 
   			&print_top_links(-1);

  		  $approve = 0;
  		  $deliver = 0;
  		  $delete = 0;
  		}
		else
  		{
			# authentication successful. 
			if($uid == 0) { $delete = 0; $deliver=0; $approve= 0; }
 			if ($delete)
  		{
  			if($detail)
  			{
  			my $quid = $dbh->quote($detail);
  			&commandQuery($dbh,"delete from proposals where uid=$quid");
  			&commandQuery($dbh,"update players set proposal=0 where proposal=$quid");
  			print "<p>Proposal deleted<p>";
  			}
 			&print_top_links(-1);
  		}
			elsif ($deliver)
 	 		{ # finish the creation or editing of a proposal
  			my $qsubj = $dbh->quote($subject);
  			my $qfrom = $dbh->quote($fromname);
  			my $qdate = $dbh->quote($date);
  			my $qbody = $dbh->quote($body);
				my $qcbody = $dbh->quote($cbody);
				my $qstatus = $dbh->quote($status ? $status : 'new');
  			my $qid = $dbh->quote($ownerid ? $ownerid : $uid);
			my $qcreated = $dbh->quote($created);
  			my $created = $detail ? ",created=$qcreated" : ",created=current_date()";
  			my $setu = $detail ? " uid=$detail," : "";
 			my $query = "REPLACE INTO proposals set $setu ownerid=$qid$created,title=$qsubj,description=$qbody,changed=current_date,comment=$qcbody,status=$qstatus";
   			#print "Q: $query<br>";
  			&commandQuery($dbh,$query);
	  		if(!$detail)
	  			{
				my $sth = &query($dbh,"SELECT last_insert_id() from proposals LIMIT 1");
	  			($detail) = &nextArrayRow($sth);
	    			&finishQuery($sth);
				}

 		 		my $quid=$dbh->quote($detail);
  			if(!($super && $spass) ) 
  				{ &commandQuery($dbh,"update players set proposal=$quid WHERE uid=$qid"); 
  				}
				# determine the one we just created, change the user to vote for it
  			print "<p>Proposal entered<p>";
 				&print_top_links(-1);

  		}
			else 
  			{ # present for editing or approval
  			
 				my $button = $approve ? "Ok, Make this my Proposal" : "Preview the Proposal";
 
				my $bname =  $approve ? "deliver" : "approve";

			 	&print_top_links(-1);
  			print "<p><form action=$ENV{'REQUEST_URI'} method=POST>\n";
  			print "<input type=hidden name=months value='$months'>\n";
				print "<table>";
	
				#
				# proposal owner
				#
				print "<tr><td><b>From:</b></td><td align=left>" ;

			  if($fromname)
 				 	{ print "$fromname</td></tr>";
  					print "<input type=hidden name=fromname value='$fromname'>\n";
 						print "<input type=hidden name=passwd value='$passwd'>\n";
 						if($super && $spass)
 				 		{
						print "<input type=hidden name=super value=$super>\n";
						print "<input type=hidden name=spass value=$spass>\n";
 				 		}
  				}
				else
  				{
					print "<input type=text name=fromname value='$fromname' size=12></td></tr>\n";
					print "<tr><td><b>your player password:</b></td><td>"
							. "<input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25></td></tr>\n";
 					print "<tr><td colspan=2>(you must have played more than 10 games to create new proposals)</td></tr>\n"; 
  				}  
	

				#
				# proposal status, editable if we're the supervisor
				#
				if($status)
					{
					if(!$approve && $super && $spass)
						{
						print "<tr><td><b>Status:</b></td><td>";
						if($status eq '') { $status='new'; }
						&print_select_menu("status",$status,%'allstatus);
						print "</td></tr>\n";
						}
						else
						{
							 print "<tr><td><b>Status:</b></td><td>$status</td></tr>\n";
							 print "<input type=hidden name=status value=$status>\n";
						}
					}
					
				#
				# proposal short description
				#
				print "<tr><td><b>Description:</b></td><td align=left>";
				if(!$approve )
					{	print "<input type=text name=subject value=\"$subject\" size=80>\n";
					}
					else
					{	print $subject;
		my $cs = uri_escape($subject);
					print "<input type=hidden name=subject value=\"$cs\">\n";
					}
				print "</td></tr>\n";
				
				#
				# proposal main description
				#
				print "<tr><td colspan=2><p><pre width=80>";
				if(!$approve)
				{
				 print "<textarea name=body  cols=80 rows=20>$body</textarea>\n";
				}
				else
				{ my $b = uri_escape($body);
				print "<input type=hidden name=escaped value=1>\n";
				print "<input type=hidden name=body value=\"$b\">\n";
				print $body;
				}
				print "</pre>\n";
				
				#
				# proposal official comments
				#
				if(!$approve && $super && $spass)
				{

						print "<tr><td colspan=2><p><pre width=80>";
						print "<br><b>Official Comments:</b><br>";
						print "<textarea name=cbody cols=80 rows=10>$cbody</textarea>\n";
						print "<input type=hidden name=body value=\"$b\">\n";				
						print "</pre></td></tr>\n";
				}
				else
  				{
				  	if($cbody)
						{
						my $c = uri_escape($cbody);
						print "<tr><td colspan=2><p><pre width=80>";
						print "<b>Official Comments:</b><br>";
						print "<input type=hidden name=cbody value=\"$c\">\n";
						print "$cbody\n";
						print "<input type=hidden name=cescaped value=1>\n";
						print "</pre></td></tr>\n";
						}
  				}
	  				
 
	 	 		print "</table>\n";
				print "<input type=hidden name=view value=$detail>\n";
				print "<input type=submit name=$bname value='$button'>\n";
				print "</form>\n";

  			} # end of edit/create/approve
  			


 		} # end of authentication successful
  	

  &disconnect($dbh);
  &standard_footer();
	}
}
my @approved_orders = ("pcount","uid","oid","description","created","changed","status");
sub approveOrder()
{	my ($target) = @_;
	foreach my $or (@approved_orders)
	{	if($or eq $target) { return($or); }
	}
	return($approved_orders[0]);
}
sub newlink()
{	my ($newsort,$text) = @_;
	my $order = &approveOrder(&param('order'));
	my $direction = &param('direction');
	my $view = &param('view');
	my $dead = &param('dead');
	my $super = &param('super');
	my $thisurl = $ENV{'SCRIPT_NAME'} ;
	my $months = &param('months');

	&bless_parameter_length($dead,2);
	&bless_parameter_length($order,10);
	&bless_parameter_length($view,10);
	&bless_parameter_length($months,10);

	if($months) { $months="&months=$months"; }
	if($super) { $super = "&super=$super"; }
	if(!($direction eq 'asc')) { $direction = "desc"; }

	my $newdir = (lc($newsort) eq lc($order)) 
			? (($direction eq "desc") ? "asc" : "desc") 
			: $direction;
			
	return("<a href='$thisurl?direction=$newdir&order=$newsort&dead=$dead&view=$view$super$months'>$text</a>");
}
sub do_gs_show()
{	print header;
	&print_header("Boardspace.net proposals");
		print<<Head_end;
<p>These proposals can be anything you would like to see changed, added, removed or
(in general) done on BoardSpace.net or anywhere in the worldwide empire of BoardSpace.
You can create as many proposals as you like.  However, you can only <i>support</i> one proposal at a time, so choose wisely.  You should think of your
support as the answer to the question <i>if you could have just one wish...</i> 
<p>
Head_end
	my $fromname = &param('fromname');
	 $fromname = &despace(substr($fromname,0,25));
	my $passwd = param('passwd');
	 $passwd = &despace(substr($passwd,0,15));

	my $detail = &param('view');
	my $addme = &param('addme');
	my $dead = &param('dead');
	my $months = &param('months');
	my $dbh = &connect();
	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&readtrans_db($dbh);
	my $implemented = ($dead==2);
	my $super = &param('super');
	my $suplink = $super ? "&super=$super" : "";
	my $thisurl = $ENV{'SCRIPT_NAME'} ;
	my $order = &approveOrder(&param('order'));
	my $direction = &param('direction');
	my $xorder = ($order eq 'uid') ? "" : ",uid desc";
	if(!($direction eq 'asc')) { $direction = "desc"; }

	

	if($addme)
  	{	
  		my ($uid,$reason) = &logon($dbh,$fromname,$passwd);
  		if($uid<=0)
  		{	print "<b>$reason</b><br>";
  		}
  		else
  		{	my $qdetail = $dbh->quote($detail);
  			&commandQuery($dbh,"UPDATE players set proposal=$qdetail where uid=$uid");
  			$detail=0;
  		}
  	}  	
  	
  my $have = $detail ? "" 
  		: ($implemented ? "" : $dead ? "having count(players.uid)=0" : "having count(players.uid)>0");
  &print_top_links($detail ? -1 : $dead);
	print "<p>";

	my $sunset = &player_retired_time();

	#
	#note in supported proposals, the supporters must be in good standing
	#in unsupported proposals, the owner must be in good standing
	#
	my $pt = $dead ? "moreplayers" : "players" ;
	my $obsolete = $dead 
									?" AND ((proposals.status='accepted') OR (UNIX_TIMESTAMP(proposals.created)>$sunset))"
									: "";
	my $pcondsimple = "((players.status='ok') AND (players.last_played>$sunset))" ;
	my $pcond = "(($pt.status='ok') AND ($pt.last_played>$sunset))" ;
	my $qdetail = $dbh->quote($detail);
	my $where = $detail 
		? "WHERE proposals.uid=$qdetail "
	  : $implemented 
	  		? "WHERE proposals.status='implemented'"
	  		: "WHERE ($pcond AND (proposals.status!='implemented') $obsolete)";
	# get a list of proposals and count of supporters
	my $q = "select proposals.ownerid as oid,created,changed,proposals.uid,proposals.description,"
					. " count(players.uid) as pcount,"
					. " moreplayers.player_name,title,moreplayers.e_mail,proposals.status,proposals.comment"
					. " from proposals"
					. " left join players on (players.proposal=proposals.uid and players.last_played>$sunset)"
					. " left join players as moreplayers on moreplayers.uid=proposals.ownerid "
					. " $where group by proposals.uid,players.proposal"
					. " $have"
					. " order by $order $direction $xorder"
					;
	#print "q: $q<br>";
	my $description = "";
	my $comment = "";

  {
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	print "<table borders=1 >\n";
	my $idlink = &newlink("uid","<b>id</b>&nbsp;&nbsp;");
	my $ownlink = &newlink("oid","<b>Owner</b>&nbsp;&nbsp;");
	my $slink = &newlink("pcount","<b># supporters</b>&nbsp;&nbsp;");
	my $deslink = &newlink("description","<b>Short description of the proposal</b>&nbsp;&nbsp;");
	my $crelink = &newlink("created","<b>Created</b>&nbsp;&nbsp;");
	my $changelink = &newlink("changed","<b>Changed</b>&nbsp;&nbsp;");
	my $statlink = &newlink("status","<b>Status&nbsp;&nbsp;</b>");
	&printRow("$idlink","$ownlink  ","$slink","$deslink","$crelink","$changelink","$statlink");
	while ($n>0)
  	{
  		$n--;
  		my $mmonths = $months ? "&months=$months" : "";
  		my ($ownerid,$created,$changed,$uid,$des,$count,$pname,$title,$email,$stat,$comm) = &nextArrayRow($sth);
  		my $xtitle = $title ? $title : "--";
  		&printRow($uid,"<a href='/cgi-bin/edit.cgi?pname=$pname'>$pname</a>",$count,
  			$detail? $title : "<a href=$thisurl?view=$uid$suplink$mmonths>$xtitle</a>&nbsp;&nbsp;",
  			"&nbsp;$created&nbsp;&nbsp;",
  			($created eq $changed)?"":$changed,
  			($stat eq 'new') ? "" : $stat);
  		$description = $des;
  		$comment = $comm;
  	}
  print "</table>\n";
  &finishQuery($sth);
  }
  
  if($detail)
  	{	print "<p><pre width=80>$description</pre>\n";
			if($comment)
  		{	print "<b>Official Comment:</b><br>\n";
  			print "<pre width=80>$comment</pre>\n";
  		}
 		  print "<p><hr><form action='$thisurl' method=POST>\n";
 		  print "<input type=hidden name=months value='$months'>\n";
 		  print "<table>";
 		  if($super)
  		{
			print "<tr><td>Supervisor Nickname: </td><td>$super</td></tr>	";
			print "<input type=hidden name=super value=$super>\n";
			print "<input type=hidden name=fromname value=$super>\n";
  		}
  		else
  		{
			print "<tr><td>Player Nickname: </td><td><input type=text name=fromname value='' size=12></td></tr>	";
  		}
  		print "<tr><td>your player password:</td><td><input type=password name=passwd value='' SIZE=20 MAXLENGTH=25></td></tr>";
  		if($super) 
  			{ print "<tr><td>supervisor password:</td><td><input type=password name=spass value='' SIZE=20 MAXLENGTH=25></td></tr>"; 
  			}
  		print "</table>";
  		print "<input type=hidden name=view value=$detail>\n";
  		my $qd = $dbh->quote($detail);
 
  		{
  		my $sth = &query($dbh,"select player_name from players where $pcondsimple and proposal=$qd order by player_name");
  		my $n = &numRows($sth);
  		if($n>0)
  		{
  		my $ss = ($n>1) ? "s" : "";
  		print "<p>$n Supporter$ss of this proposal: ";
			while($n>0)
  			{	$n--;
  				my ($pid) = &nextArrayRow($sth);
  				print " $pid";
  				if ($n>0) { print ","; }
  			}  			
  		}
  		&finishQuery($sth);
  		}
  		
  		print "<p><input type=submit name='addme' value='Add My Support'>\n";
  		print "&nbsp;<input type=submit name='edit' value='Edit this proposal'>\n";
  		print "&nbsp;<input type=submit name='delete' value='Delete this proposal'>\n";
  		print "</form>\n";
		}
		else
  	{	print "<p><a href='$thisurl?edit=-1$suplink'>Start a new Proposal</a>\n";
  	}
   
  &standard_footer();
    }
  &disconnect($dbh);

}

if(param('edit') || param('approve') || param('deliver') || param('delete')) 
	{ do_gs_edit(); 
	}
	else 
	{ do_gs_show(); 
	}
