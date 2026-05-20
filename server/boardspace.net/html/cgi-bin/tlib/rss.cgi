#
# here to register a new player in the database, and then log him in
#


sub show_recent_rss()
{	my ($number) = @_;
	my $db_type = $::db_type;
	my $database= $::php_database;
	my $db_host = $::db_host;
	$number = 0+$number;	# force treatment as an int
	print "<channel>\n";
	print "<title>Boardspace.net forums</title>\n";
	print &obfuscateHML("<link>http://$ENV{'SERVER_NAME'}/BB/</link>\n");
	print "<description>Boardspace.net user forums</description>\n";

	if($database)
	{
	my $dbh = DBI->connect("DBI:${db_type}:${database}:${db_host}",$::db_user,$::db_password);
	if($dbh)
	{

	my $q = "select topic_title,forum_name,phpbb_topics.forum_id,topic_id from phpbb_topics "
		. " left join phpbb_forums on phpbb_forums.forum_id = phpbb_topics.forum_id"
		. " order by topic_time desc limit $number";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $forum = &trans('Forum');
	my $topic = &trans('Topic');
	while($num-->0)
	{ my ($post,$forum,$fid,$tid) = &nextArrayRow($sth);



	print "<item>\n";
	$post = encode_entities($post);
	print "<title>$forum: $post</title>\n";
	print "<description>$forum: $post</description>\n";
	print &obfuscateHTML("<link>http://$ENV{'SERVER_NAME'}/BB/viewtopic.php?t=$tid</link>\n");
	print "</item>\n";

	 }
	 &finishQuery($sth);
	 }
	 print "</channel>\n";
	 &disconnect($dbh);
	 }
}
1
