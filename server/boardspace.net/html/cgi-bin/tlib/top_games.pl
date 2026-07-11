# top_games_table.pl
#
# Pulls per-game sample icons out of Icons.jar (a zip archive) instead of
# scanning a directory of loose image files. Uses Archive::Zip so the jar
# is opened and its central directory read exactly once per process, with
# every subsequent lookup served from that same in-memory object.
#
# Images are inlined directly into the HTML as base64 data URIs, so there
# is no companion streaming script and no second HTTP request per image.
#
# Entry naming convention inside the jar: icons/${gamename}-xx.jpg
# (e.g. icons/havannah-sample1.jpg, icons/havannah-sample2.jpg, ...)
# "xx" can be any suffix - sample1/sample2/sample3/etc - since matching
# is done on the ${gamename}- prefix and .jpg suffix only.

#
# note that this icons file is also used by the offline app to present images for the games that
# can be played, and the appdata/icons.res file ought to contain the same images for the mobile apps
#
use Archive::Zip qw(:ERROR_CODES);
use MIME::Base64;

# opened once per process; every game's icon is pulled from this same object
my $icons_zip;

sub open_icons_jar
{ my ($jarpath) = @_;
  return $icons_zip if $icons_zip;
  my $zip = Archive::Zip->new();
  unless ($zip->read($jarpath) == AZ_OK)
    { print "<!-- could not read icons jar: $jarpath -->\n";
      return undef;
    }
  return $icons_zip = $zip;
}
#
# some game codes don't match the icon naming convention used inside
# icons.jar - map those known exceptions to the name actually used there.
# mostly these are historical artifacts, we'll avoid adding to the exceptions list
# in the future
#
my %icon_name_exceptions =
( 'checkers'      => 'checkerboard',
  'go'            => 'goban',
  'tumbleweed' => 'tweed',
  'tumblingdown' => 'tumble',
  'cookie-disco'  => 'cookie',
  'honeycomb' => 'honey',
);

sub random_icon_member
{ my ($zip,$game) = @_;
  my $iconname = $icon_name_exceptions{$game} || $game;
  my $qgame = quotemeta($iconname);
  my $key = "(?i)^icons/${qgame}-[^/]+\\.jpg\$";
  my @members = $zip->membersMatching($key);

  if($#members<0) { print "no match for ($qgame) with ($key)<p>"; }
#  print "$game -> @members<p>";
  return undef unless @members;
  return $members[int(rand(scalar @members))];
}

sub top_games_table
{ my ($dbh,$number,$lan,$myname,@games) = @_;
  if($dbh)
  {
  my $language = &select_language($lan);
  my $idx = 0;
  my $months = $::retire_months;
  my $sunset = time()-60*60*24*30*$months;
  my $t1 = &trans('Games at Boardspace.net');
  my $ncols = $::top_players_columns;
  my $www_root = $ENV{'DOCUMENT_ROOT'};
  my $jarpath = &dircat($www_root,"/java/jws/Icons.jar");
  my $zip = &open_icons_jar($jarpath);

  print "<b>$t1</b>";
  print "<table border=1 caption='$t1'><tr>";
  foreach my $game (@games)
  {
  my $member = $zip ? &random_icon_member($zip,$game) : undef;


  my $imtag = "";
  if($member)
    { my $bytes = $member->contents();
      my $b64 = MIME::Base64::encode_base64($bytes,'');
      $imtag = "<img width=120 src='data:image/jpeg;base64,$b64'>";
    }
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
  print "<b><center><a target=_new href='$vlink'>$vtr</a></center></b>";
  if($imtag)
    { print "<a target=_new href='$vlink'>$imtag</a>"; }
  print "</td>\n";
  $idx++;
  }
  print "</tr>";
  print "</table>\n";
  }
}


1
