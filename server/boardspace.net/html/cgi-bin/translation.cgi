#!/usr/bin/perl
#
# accept self signups and other data for a scheduled tournament.  
# administer the tournament system.  The same script serves both
# for administration and ordinary users, so some care is taken to
# present the right options depending on if we're the administrator
# or not.  
#
# this is work-in-progress linked to the boardspace database
# there are unique "tournament" records for each tournament,
# "participant" records for players who intend to play
# "match" records for two player matches scheduled to take place
# "group" records for groups of matches that go together
# 
# 
# minimal administrative functions if you specify ?admin=password
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;
use Crypt::Tea;
use Encode;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";



sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}

#
# like &select_language_menu, but work from the translations database
#
sub select_db_language_menu()
{	my ($dbh,$def,$auto) = @_;
	my $key;
	my $issel = ($def eq '') ? "selected" : "";
	my $ac = $auto ? " onchange=this.form.submit()" : "";
	my @choices = sort(&get_enum_choices($dbh,'translation','language'));
	print "<select name='language' $ac>\n";
	print "<OPTION VALUE='' $issel>"
	. &trans("Select Language")
	. "</OPTION>\n";
	foreach my $val (@choices) 
	{	$issel = lc($val) eq lc($def) ? "selected" : "";
		my $tval = &trans($val);
		print "<option value=$val $issel>$tval</option>\n";
	}
	print "</select>\n";
}

# return only acceptable languages. 
sub select_db_language()
{ my ($dbh,$languageName) = @_;

  # supply language from cookies
  my @choices = sort(&get_enum_choices($dbh,'translation','language'));

  foreach my $val (@choices)
  {	if($val eq $languageName) { return($languageName); }
  }
  return(&select_language($languageName));
}

sub print_header 
{	&standard_header();

	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>Translation Manager</TITLE>
<center><H2>Translation Manager</h2>
<br>use this page to assign translations. 
</center>
<p>
Head_end
}

#
# this logon requires the correct password and that the user is designated a translator
#
sub logon 
{
	my ($dbh,$pname,$passwd) = @_;
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $bannercookie = &bannerCookie();
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $pw = "";
	my $q = "SELECT uid FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok' AND is_translator='Yes' ";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
        my $val=0;
	if($n == 1)
      { 
      if((&allow_ip_access($dbh,$myaddr)>=0)
		 && (&allow_ip_access($dbh,$bannercookie)>=0))
		 {
		($val) = &nextArrayRow($sth);
		}
	}
	else 
	{
	&note_failed_login($dbh,$myaddr,"IP: Translation as $pname");
    &note_failed_login($dbh,$bannercookie,"CK: Translation as $pname");
    
	&countEvent("failed translation",10,100);
	}
	&finishQuery($sth);
	return $val;
}
sub savechanges()
{	my ($dbh,$fromname,$passwd) = @_;
	my $nr = &param('numberofrows');
	my $uid = &logon($dbh,$fromname,$passwd);
	my $tlan = param('tlanguage');
	my $ttype = param('ttype');
	if($uid>0)
	{
	while($nr-- > 0)
	{
	my $key = &utfEncode(decode_entities(&param("row${nr}key")));
	my $collection = &utfEncode(decode_entities(&param("row${nr}collection")));
	my $com = &utfEncode(decode_entities(&param("row${nr}com")));
	my $tran = utfEncode(decode_entities(&param("row${nr}trans")));
	my $del = &param("row${nr}delete");
	my $qkey = $dbh->quote($key);
	my $qcol = $dbh->quote($collection);
	my $qcom = $com ? $dbh->quote($com) : "null";
	my $qtrans = $tran ? $dbh->quote($tran) : "null";
	my $qtlan = $dbh->quote($tlan);
	#print "C $com ($tlan $key)<p>";

	if($key && $tlan)
	{
	if($del)
	{
	 my $lkey = ($tlan eq 'english') ? "" : " AND language=$qtlan";
	 my $q = "delete from translation where keystring=$qkey and collection=$qcol $lkey";
	# print "Q: $q<p>";
	 &commandQuery($dbh,$q);
	}
	elsif($tran)
	{
	my $getold = "select translation,collection,comment from translation "
				. " where language=$qtlan and keystring=$qkey and collection=$qcol ";
	#print "Q: $getold<p>";
	my $sth = &query($dbh,$getold);
	my $num = &numRows($sth);
	my ($oldtrans,$oldcoll,$oldcomm) = &nextArrayRow($sth);

	if($ttype eq 'matchmiss')
	{
	if(!($tran eq $oldtrans))
	{
	my $q = "replace into translation set translation=$qtrans,keystring=$qkey,language=$qtlan,translator=$uid,collection=$qcol";
	#print "q0:: $q\n<p>";
	 &commandQuery($dbh,$q);
	 }
	}
	else
	{ # if english, the original table exists and has other data.  If not,
	  # it may not exist and doesn't contain other data
	  if(!($tran eq $oldtrans))
	  {
		my $q = ($tlan eq 'english') 
				? "update translation set translation=$qtrans,translator=$uid where language=$qtlan and keystring=$qkey and collection=$qcol"
				: "replace into translation set language=$qtlan,keystring=$qkey,translator=$uid,translation=$qtrans,collection=$qcol";
			#print "q:: $q\n<p>";
			&commandQuery($dbh,$q);
	  }
	    
	}
	if($com && !($com eq $oldcomm))
	{
	my $q2 = "update translation set comment=$qcom,translator=$uid where language='english' and keystring=$qkey";
	#if($com) { print "Q: $q2<p>"; }
	#print "q2:: $q2\n<p>";
	&commandQuery($dbh,$q2);
	}

	}
	
	}
	}
	}
	else
	{ print "<b>Username and Password not valid for translation</b><p>";
	}
}


sub show_translations_form()
{
	my ($dbh,$fromname,$passwd,$default,$group,$days) = @_;
	my $ttype = &param('ttype');
	my $rawmatchstr = &param('matchstr');
	my $matchstr = &utfEncode(decode_entities($rawmatchstr));
	$matchstr =~ s/\\/\\\\/g;
	$matchstr =~ s/%/\\%/g;
	$matchstr =~ s/_/\\_/g;
	my $tlan = &param('language');
	#print "mat $ttype $matchstr as $tlan<p>";
	if( (length($matchstr)>=1) || ($ttype eq 'matchmiss') || $group || ($days<300))
	{
	my $qmatch = $dbh->quote("%$matchstr%");
#print "m: $rawmatchstr ($matchstr) $qmatch<br>";
	my $lan = $tlan;
	my $qlan = $dbh->quote($lan);
	my $matchtab = ($ttype eq 'matchtrans') ? "translation" : "master";
	my $tclause = 
		($ttype eq 'matchmiss') 
		 ? " and (translation.translation is null)"
		 : "";
	my $qgroup = $dbh->quote($group);
	my $qdays = encode_entities($days);
	my $gclause = ($group eq '') ? "" :  " and (master.collection=$qgroup)";
	my $d0clause = ($lan eq '') ? "" : " OR (master.changed>date_sub(now(),interval $days day)) ";
	my $dclause = ($days>0) ? " and ((translation.changed>date_sub(now(),interval $days day)) $d0clause ) " : "";
	my $oclause = ($ttype eq 'matchenglish')
					? " OR (master.keystring like $qmatch)"
					: "";
	my $lclause = ($lan eq '') ? "" : " and (translation.language=$qlan) ";
	my $q = "select master.collection,master.keystring,master.translation,translation.translation,master.context,master.comment,translation.language,players.player_name"
			. " from translation as master"
			. " left join translation "
			. " left join players on players.uid = translation.translator "
			. " on (master.keystring = translation.keystring) $lclause and (master.collection=translation.collection)"
			. " where (master.language='english')  and (${matchtab}.translation like $qmatch $oclause)$tclause$gclause$dclause"
			. " order by ${matchtab}.translation";
	#print "Q: $q<p>";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr>0)
	{
	print "<form action=$ENV{'SCRIPT_NAME'} method=post>\n";
	print "<table border=1><tr>";
	print "<td>Key</td><td>English</td><td>Group</td>";
	print "</tr>";
	print "<input type=hidden name=numberofrows value=$nr>\n";
	print "<input type=hidden name=tlanguage value=$qlan>\n";
	print "<input type=hidden name=language value=$qlan>\n";
	print "<input type=hidden name=selectedgroup value=$qgroup>\n";
	print "<input type=hidden name=daystosee value='$qdays'>\n";
	my $qfrom=$dbh->quote($fromname);
	my $qpass=$dbh->quote($passwd);
	my $qmatch=encode_entities($matchstr);
	my $qttype = $dbh->quote($ttype);
	print "<input type=hidden name=ttype value=$qttype>\n";
	print "<input type=hidden name=matchstr value='$qmatch'>\n";
	my $color1 = "#xddddff";
	my $color2 = "#xddffdd";
	while($nr-- > 0)
	{
	my ($collection,$key,$master,$trans,$cxt,$comment,$tlan,$translator) = &nextArrayRow($sth);
	my $kk = ($key eq $master) ? "--" : $key;
	my $ecollection = encode_entities(&utfDecode($collection));
	my $ekey = encode_entities(&utfDecode($key));
	my $etrans = encode_entities(&utfDecode($trans));
	my $ecom = encode_entities(&utfDecode($comment));
	my $emas = encode_entities(&utfDecode($master));
	my $temp = $color1;
	 $color1 = $color2;
	 $color2 = $temp;
	print "<input type=hidden name=row${nr}key value='$ekey'>\n";
	print "<input type=hidden name=row${nr}collection value='$ecollection'>\n";
	print "<tr bgcolor=$color1><td>$kk</td><td>$emas</td>";
	print "<td>$ecollection $cxt</td>";
	print "</tr><tr  bgcolor=$color1>\n";
	print "<td><input type=checkbox name='row${nr}delete' >XX $tlan $translator </td>";
	if(length($emas)<50)
	{
	print "<td><pre><input type=text name=row${nr}trans value='$etrans'  size=80></pre></td>\n";
	}else
	{ print "<td><pre><textarea cols=80 rows=3 name=row${nr}trans>$etrans</textarea></pre></td>\n";
	}
	print "<td><input type=text name=row${nr}com value='$ecom'  size=40></td>\n";
	print "</tr>\n";

	}

	print "</table>";

	print "<table>";
    print "<tr><td>Player: ";
    print "<input type=text name=fromname value='$fromname' size=12>\n";
    print "</td><td>Your Password: ";
    print "<input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25>\n";
    print "</td>";
    print "<td>name and password required to make changes</td>";
    print "</tr>";
	print "</table>";
	print "<input type=hidden name=operation value='makechanges'>";
	print "<input type=submit value='save changes'>";
	print "</form>";
	}
	&finishQuery($sth);
	}
}
sub select_group_menu()
{	my ($dbh,$default) = @_;
	my $q = "select distinct collection from translation order by collection";
	my $sth = &query($dbh,$q);
	
	#push these groups first
	my $nr = &numRows($sth);
	{
	my $def = ($default eq '') ? "selected" : "";
	print "<option value='' $def>All groups</option>\n";
	}
	{
	my $def = ($default eq 'web') ? "selected" : "";
	print "<option value='web' $def>Web Pages</option>\n";
	}
	{
	my $def = ($default eq 'lobby') ? "selected" : "";
	print "<option value='lobby' $def>The Lobby</option>\n";
	}
	{
	my $def = ($default eq 'game') ? "selected" : "";
	print "<option value='game' $def>Common to Games</option>\n";
	}
		
	while($nr-->0)
	{	my ($group) = &nextArrayRow($sth);
		if(($group eq '') || ($group eq 'web') || ($group eq 'lobby') || ($group eq 'game')) {}
		else
		{
		my $def = ($group eq $default) ? "selected" : "";
		my $egroup = &encode_entities($group);
		my $qgroup = $dbh->quote($group);
		print "<option value=$qgroup $def>$egroup</option>\n";
		}
	}
	&finishQuery($sth);
}
#
# show the "add me" form
#
sub show_input_form
{ my ($dbh,$fromname,$passwd,$default,$selectedgroup,$days) = @_;
  $fromname = encode_entities($fromname);
  my $mat = &param('matchstr');
  my $ttype = &param('ttype');
  my $sel1 = ($ttype eq 'matchenglish') ? "selected" : "";
  my $sel2 = ($ttype eq 'matchtrans') ? "selected" : "";
  my $sel3 = ($ttype eq 'matchmiss') ? "selected" : "";
  print "<hr>";
  print "<form action=$ENV{SCRIPT_NAME} method=POST>";
  print "<table>";

  print "<tr>";
  
  print "<td>Translate ";
  &select_db_language_menu($dbh,$default,0);
  print "</td>";

  print "<td>Match <input name=matchstr type=text value='$mat'></td>";

  print "<td>";
  print "<select name=ttype>";
  print "<option value='matchenglish' $sel1>in the English</option>";
  print "<option value='matchmiss' $sel3>in English with no translation</option>";
  print "<option value='matchtrans' $sel2>in the translation</option>";
  print "</select>";
  print "</td>";


  print "<td>";
  print "select only group ";
  print "<select name=selectedgroup>\n";
  &select_group_menu($dbh,$selectedgroup);
  print "</select>";
  print "</td>\n";

  print "<td>";
  my $qdays = encode_entities("$days");
  print "Show only changed in the last ";
  print "<input type=text name=daystosee value='$qdays' size=4> days";
  print "</td></tr>";

  print "</table>\n";
  print "<input type=hidden name=operation value=''>";
  print "<input type=submit name=doit value='Show Translations'>\n";
  print "</form>\n";
}



sub show_admin_panel()
{	my ($dbh,$admin,$default,$op) = @_;
	print "<hr><br>Administrative Operations<br>";
	print "<form name=admin_panel action=$ENV{'SCRIPT_NAME'} method=post><table>\n";
	print "<input type=hidden name=admin value=$admin>\n";
	print "<tr><td>Translations for </td>";
	print "<td>";
	&select_db_language_menu($dbh,$default,0);
	print "</td></tr>";

	print "<tr><td>\n";
	print "<select name=operation>";
	print "<option value='import' selected>Import Text To Database</option>\n";
	print "</select>";
	print "</td></tr>\n";
	print "</table><input type=submit name=doit value='Administrate'></form>\n";
}

sub print_option()
{	my ($value,$default,$pretty) = @_;
	my $def = ($value eq $default) ? "selected" : "";
	print "<option value='$value' $def>$pretty</option>\n";
}


sub do_import()
{	my ($dbh,$lan,$uid) = @_;
	my $qlan = $dbh->quote($lan);
	#my $sth = &commandQuery($dbh,"delete from translation where language=$qlan");
	print "importing $lan by $uid<br>";
	my %tr = &readtrans($lan);
	foreach my $key (keys(%tr))
	{
	my $qkey = $dbh->quote($key);
	my $qtrans = $dbh->quote($tr{$key});
	&commandQuery($dbh,"replace into translation set keystring=$qkey,language=$qlan,translation=$qtrans");
	#print "key $key = $tr{$key}<br>\n";
	}
	if(!($lan eq 'english'))
	{	&commandQuery($dbh,"delete from translation where keystring=translation and language=$qlan");
	}
}

&init();

print header;

#__d( "translation...");

sub do_translation()
{
  my $fromname = param('fromname');
  	 $fromname = &despace(substr($fromname,0,25));
  my $passwd = param('passwd');
  	 $passwd = &despace(substr($passwd,0,15));
  my $admin = param('admin');	# the value of "admin=xx" is the admin password
								# which is taken from include.pl.  All the local
								# functions are passed either this password or null
  my $dbh = &connect();
  if($dbh)
  {
  &readtrans_db($dbh);
  my $uid = 0;
  my $operation = &param('operation');
  my $language = &param('language');
  my $default = &select_db_language($dbh,$language);
  my $selectedgroup = &param('selectedgroup');
  my $daystosee = &param('daystosee');
  if($daystosee<=0) { $daystosee = 9999; }
  &print_header(); 
  if($admin && (!$'translation_password || !($admin eq $'translation_password)) ) 
	{ $admin = ''; 
	  print "<b>Incorrect admin password<b><p>\n";
	}
	if($admin && ($operation eq 'import'))
		{	&do_import($dbh,$language);
		}
	elsif(($operation eq '') or ($operation eq 'makechanges'))
	{
	if($operation eq 'makechanges') { &savechanges($dbh,$fromname,$passwd); }
	&show_translations_form($dbh,$fromname,$passwd,$default,$selectedgroup,$daystosee);
	&show_input_form($dbh,$fromname,$passwd,$default,$selectedgroup,$daystosee);
	}
	else
	{	print "Operation $operation not implemented<br>\n";
	}
	  if($admin)
	  {	&show_admin_panel($dbh,$admin,$default,$operation);
	  }
    }
  &standard_footer();
}

do_translation();

