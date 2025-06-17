
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

my @pastmatches;
my $bye = "bye";

sub forbiddenMatch()
{
  my ($p1, $p2) = @_;
  for my $pair (@pastmatches)
  {
  # players have already played
  if((@$pair[0] eq $p1) && (@$pair[1] eq $p2)) { return 1; }
  if((@$pair[0] eq $p2) && (@$pair[1] eq $p1)) { return 1; }
 
  }
  return 0;
}


# remove an element from an array, and make the array shorter
sub removeElement()
{ my ($ar,$el) = @_;
  my @elements = @$ar;
  my @res;
  for(my $i=0; $i<=$#elements; $i++)
  {
  if($i != $el) { push(@res,@elements[$i]); }
  }
  return @res;
}
#
# generate one of the n-1! possible permutations of pairing
#
sub generatePairsByPermutation()
{
  my($pl,$sequence) = @_;
  my @players = @$pl;
  my $len= $#players + 1;
  my @pairs;

  #print "players ($sequence) $len = @players\n";
  
  while($len > 0)
  {
  my $ind1 = $sequence % $len;
  $sequence = int($sequence/$len);
  my $firstPlayer = @players[$ind1];
  @players = &removeElement(\@players,$ind1);
  $len--;

  my $ind2 = $sequence % $len;
  my $sequence = int($sequence / $len);
  my $secondPlayer = @players[$ind2];
  @players = &removeElement(\@players,$ind2);
  $len--;
  if(&forbiddenMatch($firstPlayer,$secondPlayer))
   {#print "forbidden $firstPlayer $secondPlayer\n"; 
    return ();
   }
  my @pair = ( $firstPlayer, $secondPlayer);
  push(@pairs, \@pair);
  }
  return @pairs;
}

#
# generate pairs from a list already sorted by desirability
#
sub generatePairsByNext()
{
  my($pl) = @_;
  my @players = @$pl;
  my $len= $#players + 1;
  my @pairs;

  #print "players ($sequence) $len = @players\n";
  
  while($len > 0)
  {
   my $firstPlayer = @players[0];
   @players = &removeElement(\@players,0);
   $len--;
   my $success = 0;
   for(my $secondPlayerIndex = 0,; $secondPlayerIndex<$len && !$success; $secondPlayerIndex++)
   {
   my $secondPlayer = @players[$secondPlayerIndex];
   if(! &forbiddenMatch($firstPlayer,$secondPlayer))
   {
     @players = &removeElement(\@players,$secondPlayerIndex);
     $len--;
     my @pair = ( $firstPlayer, $secondPlayer);
     $success = 1;
     push(@pairs, \@pair);
   }
  }
  # failed to find a match for player1, we fail absolutely
  if(!$success) { return ();}
  }
  
  return @pairs;
}


sub fact()
{ my ($n) = @_;
   return (($n==0) ? 1 : $n*&fact($n-1));
}
sub doit()
{ my @p1 = ("dave","irene");
  push(@pastmatches,\@p1);
  my @playerandscore;
  for(my $i=0; $i<16; $i++) { my @pair = ("player$i",$i%4); push(@playerandscore,\@pair); }
  my $p1 = @playerandscore[0];
 
  my @sorted = sort { -(@$a[1] - @$b[1]) } @playerandscore ;
  my @players;
  for my $pl (@sorted)
  {
    print "@$pl[0] @$pl[1]\n";
    push(@players,@$pl[0]);
  }
  print "\np @players\n";
  {
   print "\n";
   my @pairs = &generatePairsByNext(\@players);
   print @pairs;
return;
  }
}

&doit();


