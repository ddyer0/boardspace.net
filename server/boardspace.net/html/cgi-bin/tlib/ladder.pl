#
# common subroutines to support the rankings ladder
#
# the rankings ladder is based on powers of 2, with 2^n players on level n.
# Level 10 is effectively the end of the line.  "everyone else" is on level 10 which is unsorted.
# This keeps fossils from permanantly blocking up the ladder, and also avoids a computational
# disaster which would ensue at some point, if a level with 2^n players caused millions of records
# to be renumbered.
#
$'max_ladder_level = 9;		
$'entry_ladder_level = 3;

$'max_ladder_level_cached = -1;
sub max_ladder_level()
{	my ($dbh,$table,$game) = @_;
	if($'max_ladder_level_cached>=0) { return($'max_ladder_level_cached); }
	my $q = "select max(ladder_level) from $table where variation=$game";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $val = 1;
	if($nr>=1) { ($val) = &nextArrayRow($sth); if($val<=0) { $val = 1; }}
	&finishQuery($sth);
	if($val>$'max_ladder_level) { $val=$'max_ladder_level; }
	# cache for multiple calls within one run.  Assumes only one variation is mapped per invocation.
	$'max_ladder_level_cached=$val;
	return($val);
} 

sub entry_ladder_level()
{	my ($dbh,$table,$game) = @_;
	my $max = &max_ladder_level($dbh,$table,$game);
	return(($max<=$'entry_ladder_level) ? $'entry_ladder_level : $max);
}
#
# generate a query that will resort a level
#
sub reorder_ladder_query()
{	my ($table,$variation,$lvl) = @_;
	if($lvl > $'max_ladder_level) 
		{ return(""); }	# don't sort overflow levels
	my $lvlsize = (1<<($lvl-1));
	my $q = "set \@ord=0; "
		. "update $table set ladder_level=ladder_level+if(\@ord>=$lvlsize,1,0),"
		. "ladder_order=(\@ord:=\@ord+1)-if(\@ord>$lvlsize,\@ord,0)"
		. " where variation=$variation and ladder_level=$lvl"
		. " order by ladder_order; ";
	return($q);
}
# return 1 so the load is happy
1;

