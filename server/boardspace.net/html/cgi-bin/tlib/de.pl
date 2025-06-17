
sub validname()
{	my ($str) = @_;
  my $valid = "abcdefghijklmnopqrstuvwxyz0123456789";
  my $i=0;
  my $len = length($str);
  while ($i<$len)
	{ my $ch = lc(substr($str,$i,1));
	  my $ind = index($valid,$ch);
	  if($ind<0) {print "no $i $ch\n"; return(0); }
		$i++;
	}
	return(1);
}

print &validname("foo");
print "\n";
print &validname("foo&me");
print "\n";
print &validname("for!");
print "\n";