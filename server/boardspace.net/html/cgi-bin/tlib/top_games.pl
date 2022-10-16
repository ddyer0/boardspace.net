sub randomimage()
{	my @list = @_;
	my @start = @list;
	my $len = $#list;
	my $possible = 0;
	if($len>0)
	{
	my $n = rand($len);
	do
	{
	 my $im = pop @list;
	 my ($name,$dir,$type) = &filename_split($im);
	 if(lc($type) eq ".jpg")
	 {	$n--;
		if($n<0) { return("$name$type"); }
		$possible++;
	 }
	 if(($possible>0)&& ($#list == 0)) { @list = @start; }
	} while($#list>0);
	}
	return("no-image.jpg");
}

sub top_games_table
{ my ($dbh,$number,$lan,$myname,@games) = @_;
  if($dbh)
  {
  my $language = &select_language($lan);
  my $idx = 0;
  my $months = $'retire_months;
  my $sunset = time()-60*60*24*30*$months; 
  my $t1 = &trans('Games at Boardspace.net');
  my $ncols = $'top_players_columns;
  my $imagedir = &dircat(&dircat($www_root,"images"),"samples");
  print "<b>$t1</b>";
  print "<table border=1 caption='$t1'><tr>";
  foreach my $game (@games)
  {
  my $imdir = &dircat("/images/samples/",
				&dircat($game,&randomimage(&list_dir(&dircat($imagedir,$game)))));
  print @dir;
  my $fulllist = &trans('rankings for #1',$game);
  my $qvar = $dbh->quote($game);
  my $vlink = "about_${game}.html";
  my $jname = &gamename_to_gamecode($dbh,$game);
  my $variation = &gamecode_to_gamename($dbh,$jname);
  my $vtr = &trans("${variation}-pretty-name");
  if((($idx>0)&&(($idx%$ncols)==0))) 
	{ print "</tr><tr>"; 
	}
  print "<td valign=top>";
  print "<b><center><a href='$vlink'>$vtr</a></center></b>";
  print "<a href='$vlink'><img width=120 src='$imdir'></a>";
  print "</td>\n";
  $idx++;
  }
  print "</tr>";
  print "</table>\n";
  }
}

1
