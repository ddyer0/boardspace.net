#!/usr/bin/perl

sub read_links {
  my $file_name = shift;
  my @links = ();
  open(my $file, '<', $file_name) or die "Can't open $file_name";
  while(<$file>) {
    if (m{ href=" ([^"]+) }x) {
      push @links, $1;
      last if (2 == @links);
    }
  }
  close $file;
  return @links;
}

sub check_links {
  my ($direction, $fwd, $back, $verbose) = @_;
  my %processed;
  my @files = sort(keys(%$fwd));
  foreach $file (@files) {
    until (exists $processed{$file}) {
      $processed{$file} = undef;
      my $fwd_link = $fwd->{$file};
      my $back_link = $back->{$fwd_link};
      if (!defined $back_link) {
        print "$direction $file -> $fwd_link: dead link\n";
        last;
      }
      elsif ($file ne $back_link) {
        print "$direction $file -> $fwd_link: mismatch $fwd_link -> $back_link\n";
      }
      elsif ($verbose) {
        print "$direction $file -> $fwd_link\n";
      }
      $file = $fwd_link;
    }
  }
}

# Main program

my $dir = $ARGV[0] || ".";
chdir $dir or die "Can not cwd to $dir\n";

my %back, %fwd;
while (my $file = <about_*.html>) {
  $file =~ /_notation/ && next;
  $file =~ /_tournaments/ && next;
  ($back{$file}, $fwd{$file}) = read_links($file);
}

check_links("forward", \%fwd, \%back, true);
check_links("back", \%back, \%fwd);
