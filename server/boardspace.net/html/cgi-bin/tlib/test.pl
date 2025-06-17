use DBI;

sub doit()
{
        my $dbh = DBI->connect("DBI:mysql:boardspace:localhost mysql_multi_statements=1",'root','greatpumpkin');
        if($dbh)
        {
            my $q = "select * from players where uid='1'";
        print "ok\n"
        }
else {
 print "fail\n";
}
}
&doit();

