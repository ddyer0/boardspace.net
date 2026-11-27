
binmode STDOUT, ':utf8';

my $string = '\\u5B66\\u5B66\\u5B66\\u5B66\\u5B66\\u5B66\\u5B66';

$string =~ s/\\u(....)/ pack 'U*', hex($1) /eg;

print $string;

print "\nand\n";

my $x = 0x5B66;
my $y = chr($x);
my $z = ord($y);
print "x $x y $y z $z\n";
