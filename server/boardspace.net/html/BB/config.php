<?php


// phpBB 2.x auto-generated config file
// Do not change anything in this file!

$dbms = 'mysql';

$dbhost = 'localhost';
$dbname = 'phpbb';
$dbuser = 'root';
// the password is kept outside the web tree, in www/../secrets/secrets.php
$secrets = require(dirname(__FILE__) . '/../../secrets/secrets.php');
$dbpasswd = $secrets['phpbb_dbpasswd'];
unset($secrets);

$table_prefix = 'phpbb_';

define('PHPBB_INSTALLED', true);

?>