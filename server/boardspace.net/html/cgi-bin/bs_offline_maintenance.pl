
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use HTML::Entities;
use URI::Escape;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";

sub init {
	$| = 1;				# force writes
}


var $::testdel = 0;
sub markdelinquent()
{
	my ($dbh,$status,$speed,$oldmark,$newmark,$days) = @_;
	my $qstatus = $dbh->quote($status);
	my $qspeed= $speed ? " and speed = " . $dbh->quote($speed) : "";
	my $mark = $oldmark ? " marked = " . $dbh->quote($oldmark) : " marked is null ";
	my $qnewmark = $dbh->quote($newmark);
	my $sel = $::testdel
			? "select gameuid,variation,status from offlinegame "
			: $newmark 
				? "update offlinegame set marked = $qnewmark "
				: "delete from offlinegame "
					;
	my $q = "$sel where $mark $qspeed and status=$qstatus and DATE_ADD(last,INTERVAL $days DAY)<utc_timestamp()"; 
	my $sth = &commandQuery($dbh,$q);
	my $nr = &numRows($sth);
	if ($nr>0)
		{
		return "$nr games marked $newmark stat=$status days=$days\n";
		}
	#print "rows $nr $q\n";
	return "";
}

sub changemarks()
{
	my ($dbh) = @_;
	my $msg = "";
	# setup games disappear after 7 days
	&markdelinquent($dbh,'setup','','','delinquent',7);

	# active games disappear on a schedule related to their play speed
	$msg .= &markdelinquent($dbh,'active','day1','','delinquent',3);
	$msg .= &markdelinquent($dbh,'active','day2','','delinquent',5);
	$msg .= &markdelinquent($dbh,'active','day4','','delinquent',8);
	$msg .= &markdelinquent($dbh,'active','day8','','delinquent',16);

	# complete games and damaged games disappear after 2 weeks
	$msg .= &markdelinquent($dbh,'complete','','','expired',14);
	$msg .= &markdelinquent($dbh,'suspended','','','expired',14);
	
	# delibquent games become expired after 3 weeks
	$msg .= &markdelinquent($dbh,'','','delinquent','expired',21);

	# expired games are deleted after a month
	$msg .= &markdelinquent($dbh,'','','expired','',30);

	return $msg;
}


&init();
print header;
# print start_html('Logon');
if( param() ) 
{	 &logForm("offlineops");
	my $tagname = &param('tagname');
	my $dbh = &connect();
	if($dbh)
	{
	if('markobsolete' eq $tagname) { print &changemarks($dbh); }
	}
}

1
