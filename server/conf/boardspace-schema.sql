-- MySQL dump 10.14  Distrib 5.5.68-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: boardspace
-- ------------------------------------------------------
-- Server version	5.5.68-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `coincident_ip`
--

DROP TABLE IF EXISTS `coincident_ip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `coincident_ip` (
  `uid1` int(11) NOT NULL DEFAULT '0',
  `uid2` int(11) NOT NULL DEFAULT '0',
  `type` enum('Close','Exact','NoMatch') NOT NULL DEFAULT 'Exact',
  `last_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `count` int(11) NOT NULL DEFAULT '0',
  `last_ip` char(16) DEFAULT NULL,
  KEY `ipindex` (`uid1`,`uid2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `country_info`
--

DROP TABLE IF EXISTS `country_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country_info` (
  `name` char(20) NOT NULL DEFAULT '0',
  `population` int(12) unsigned DEFAULT NULL,
  PRIMARY KEY (`name`),
  UNIQUE KEY `name` (`name`),
  KEY `name_2` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `email`
--

DROP TABLE IF EXISTS `email`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `email` (
  `fromplayer` varchar(10) NOT NULL DEFAULT '',
  `toplayer` varchar(10) NOT NULL DEFAULT '',
  `datesent` tinytext NOT NULL,
  `subject` tinytext,
  `body` text,
  `stamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `toplayer` (`toplayer`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eventlog`
--

DROP TABLE IF EXISTS `eventlog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eventlog` (
  `day_recorded` date NOT NULL DEFAULT '0000-00-00',
  `count` int(11) NOT NULL DEFAULT '0',
  `name` varchar(128) NOT NULL DEFAULT '',
  `alert` tinyint(1) DEFAULT NULL,
  UNIQUE KEY `dateindex` (`day_recorded`,`name`),
  KEY `nameindex` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `facts`
--

DROP TABLE IF EXISTS `facts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `facts` (
  `name` varchar(32) NOT NULL DEFAULT '',
  `value` text,
  UNIQUE KEY `name_2` (`name`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ipinfo`
--

DROP TABLE IF EXISTS `ipinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipinfo` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `status` enum('auto','normal','noguest','confirm','noregister','autobanned','banned','wasbanned') DEFAULT NULL,
  `min` decimal(10,0) DEFAULT '0',
  `max` decimal(10,0) DEFAULT '0',
  `logincount` int(11) DEFAULT '0',
  `badlogincount` int(11) DEFAULT '0',
  `rejectcount` int(11) DEFAULT '0',
  `regcount` int(11) DEFAULT '0',
  `changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `comment` text,
  `short_time` int(11) NOT NULL DEFAULT '0',
  `long_time` int(11) NOT NULL DEFAULT '0',
  UNIQUE KEY `uidindex` (`uid`),
  UNIQUE KEY `miinindex` (`min`,`max`),
  KEY `statusindex` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=344862 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `matchgroup`
--

DROP TABLE IF EXISTS `matchgroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `matchgroup` (
  `uid` int(11) NOT NULL DEFAULT '0',
  `status` enum('active','closed') NOT NULL DEFAULT 'active',
  `sortkey` varchar(100) NOT NULL DEFAULT 'mmm',
  `name` varchar(100) NOT NULL DEFAULT '',
  `comment` text,
  `type` enum('pairs','robin','swiss') NOT NULL DEFAULT 'pairs',
  PRIMARY KEY (`uid`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `matchparticipant`
--

DROP TABLE IF EXISTS `matchparticipant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `matchparticipant` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `matchid` int(11) NOT NULL,
  `player` int(11) DEFAULT NULL,
  `tournament` int(11) NOT NULL,
  `tournament_group` tinytext,
  `outcome` enum('none','cancelled','scheduled','draw','win','loss') DEFAULT NULL,
  `points` tinytext,
  `matchstatus` enum('ready','notready') DEFAULT 'ready',
  `played` datetime DEFAULT NULL,
  `comment` text,
  `scheduled` datetime DEFAULT '0000-00-00 00:00:00',
  `playorder` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`),
  KEY `tournament` (`tournament`)
) ENGINE=InnoDB AUTO_INCREMENT=6673 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `matchrecord`
--

DROP TABLE IF EXISTS `matchrecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `matchrecord` (
  `tournament` int(11) NOT NULL DEFAULT '0',
  `admin` enum('none','cancelled','scheduled','draw','player1','player2','winner') NOT NULL DEFAULT 'none',
  `played` datetime DEFAULT '0000-00-00 00:00:00',
  `comment` text,
  `matchstatus` enum('ready','notready') DEFAULT 'ready',
  `matchid` int(11) NOT NULL AUTO_INCREMENT,
  `tournament_group` tinytext NOT NULL,
  `admin_winner` int(11) DEFAULT NULL,
  `scheduled` datetime DEFAULT '0000-00-00 00:00:00',
  `commentHistory` text,
  `offlinegameuid` int(11) DEFAULT NULL,
  PRIMARY KEY (`matchid`),
  KEY `tourney` (`tournament`)
) ENGINE=InnoDB AUTO_INCREMENT=3675 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `messages` (
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(10) NOT NULL DEFAULT '',
  `message` text,
  KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mp_gamerecord`
--

DROP TABLE IF EXISTS `mp_gamerecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mp_gamerecord` (
  `variation` enum('medina','triad','octiles','frogs','breakingaway','container','lehavre','yspahan','raj','universe','diagonal-blocks','mogul','oneday','euphoria','tammany','qe','viticulture','blackdeath','crosswords','portfolio','imagine','jumbulaya','dayandnight','sprint','crosswordle','dash','honeycomb','manhattan','pendulum','bugspiel') DEFAULT NULL,
  `player1` int(11) NOT NULL DEFAULT '0',
  `player2` int(11) NOT NULL DEFAULT '0',
  `player3` int(11) NOT NULL DEFAULT '0',
  `player4` int(11) NOT NULL DEFAULT '0',
  `player5` int(11) NOT NULL DEFAULT '0',
  `player6` int(11) NOT NULL DEFAULT '0',
  `time1` int(11) DEFAULT NULL,
  `time2` int(11) DEFAULT NULL,
  `time3` int(11) DEFAULT NULL,
  `time4` int(11) DEFAULT NULL,
  `time5` int(11) DEFAULT NULL,
  `time6` int(11) DEFAULT NULL,
  `rank1` int(11) DEFAULT NULL,
  `rank2` int(11) DEFAULT NULL,
  `rank3` int(11) DEFAULT NULL,
  `rank4` int(11) DEFAULT NULL,
  `rank5` int(11) DEFAULT NULL,
  `rank6` int(11) DEFAULT NULL,
  `score1` int(11) DEFAULT NULL,
  `score2` int(11) DEFAULT NULL,
  `score3` int(11) DEFAULT NULL,
  `score5` int(11) DEFAULT NULL,
  `score6` int(11) DEFAULT NULL,
  `score4` int(11) DEFAULT NULL,
  `gamename` char(64) DEFAULT NULL,
  `mode` enum('normal','unranked','master') DEFAULT 'unranked',
  `turnbased` enum('no','yes') NOT NULL DEFAULT 'no',
  `tournament_id` int(11) DEFAULT '0',
  `tournament` enum('no','yes') DEFAULT 'no',
  `gmtdate` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `digest_mid` int(11) DEFAULT NULL,
  `digest_end` int(11) DEFAULT NULL,
  `uniquerow` int(11) NOT NULL AUTO_INCREMENT,
  `player7` int(11) NOT NULL DEFAULT '0',
  `player8` int(11) NOT NULL DEFAULT '0',
  `player9` int(11) NOT NULL DEFAULT '0',
  `player10` int(11) NOT NULL DEFAULT '0',
  `player11` int(11) NOT NULL DEFAULT '0',
  `player12` int(11) NOT NULL DEFAULT '0',
  `time7` int(11) DEFAULT NULL,
  `time8` int(11) DEFAULT NULL,
  `time9` int(11) DEFAULT NULL,
  `time10` int(11) DEFAULT NULL,
  `time11` int(11) DEFAULT NULL,
  `time12` int(11) DEFAULT NULL,
  `rank7` int(11) DEFAULT NULL,
  `rank8` int(11) DEFAULT NULL,
  `rank9` int(11) DEFAULT NULL,
  `rank10` int(11) DEFAULT NULL,
  `rank11` int(11) DEFAULT NULL,
  `rank12` int(11) DEFAULT NULL,
  `score7` int(11) DEFAULT NULL,
  `score8` int(11) DEFAULT NULL,
  `score9` int(11) DEFAULT NULL,
  `score10` int(11) DEFAULT NULL,
  `score11` int(11) DEFAULT NULL,
  `score12` int(11) DEFAULT NULL,
  PRIMARY KEY (`uniquerow`),
  KEY `player4` (`player4`),
  KEY `player5` (`player5`),
  KEY `digest_mid` (`digest_mid`),
  KEY `player6` (`player6`),
  KEY `tournamentindex` (`tournament_id`),
  KEY `digest_end` (`digest_end`),
  KEY `player1` (`player1`),
  KEY `player2` (`player2`),
  KEY `player3` (`player3`),
  KEY `variation_index` (`variation`,`gmtdate`),
  KEY `dateindex` (`gmtdate`)
) ENGINE=InnoDB AUTO_INCREMENT=41061 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `name_change`
--

DROP TABLE IF EXISTS `name_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `name_change` (
  `uid` int(11) NOT NULL DEFAULT '0',
  `oldname` varchar(10) NOT NULL DEFAULT '',
  `newname` varchar(10) NOT NULL DEFAULT '',
  `time` int(11) NOT NULL DEFAULT '0',
  KEY `uid` (`uid`),
  KEY `time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notes`
--

DROP TABLE IF EXISTS `notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notes` (
  `uid` int(11) NOT NULL DEFAULT '0',
  `messageid` int(11) NOT NULL AUTO_INCREMENT,
  `expires` datetime DEFAULT NULL,
  `content` varchar(500) DEFAULT NULL,
  UNIQUE KEY `messageid` (`messageid`)
) ENGINE=InnoDB AUTO_INCREMENT=589 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `offlinegame`
--

DROP TABLE IF EXISTS `offlinegame`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `offlinegame` (
  `owner` int(11) NOT NULL DEFAULT '0',
  `whoseturn` int(11) NOT NULL DEFAULT '0',
  `gameuid` int(11) NOT NULL AUTO_INCREMENT,
  `sequence` int(11) DEFAULT '0',
  `status` enum('setup','active','complete','canceled','suspended') NOT NULL DEFAULT 'setup',
  `marked` enum('delinquent','expired') DEFAULT NULL,
  `offlinegamecol` varchar(45) NOT NULL DEFAULT '0',
  `variation` varchar(30) NOT NULL,
  `playmode` enum('ranked','unranked','tournament') NOT NULL DEFAULT 'ranked',
  `invitedplayers` tinytext,
  `acceptedplayers` tinytext,
  `allowotherplayers` enum('true','false') DEFAULT NULL,
  `body` text,
  `comments` text,
  `created` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `last` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `firstplayer` enum('random','mefirst','youfirst') NOT NULL DEFAULT 'random',
  `speed` enum('day1','day2','day4','day8') NOT NULL DEFAULT 'day2',
  `scored` tinyint(4) NOT NULL DEFAULT '0',
  `chat` text,
  `nag` text,
  `NAGTIME` datetime DEFAULT NULL,
  PRIMARY KEY (`gameuid`),
  KEY `owner` (`owner`),
  KEY `gameuid` (`gameuid`),
  KEY `whoseturn` (`whoseturn`),
  KEY `status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldmatchrecord`
--

DROP TABLE IF EXISTS `oldmatchrecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldmatchrecord` (
  `player1` int(11) NOT NULL DEFAULT '0',
  `player2` int(11) NOT NULL DEFAULT '0',
  `tournament` int(11) NOT NULL DEFAULT '0',
  `outcome1` enum('none','cancelled','scheduled','draw','player1','player2') NOT NULL DEFAULT 'none',
  `admin` enum('none','cancelled','scheduled','draw','player1','player2') NOT NULL DEFAULT 'none',
  `outcome2` enum('none','cancelled','scheduled','draw','player1','player2') NOT NULL DEFAULT 'none',
  `played` datetime DEFAULT '0000-00-00 00:00:00',
  `comment` text,
  `comment1` text,
  `comment2` text,
  `matchstatus` enum('ready','notready') DEFAULT 'ready',
  `matchid` int(11) NOT NULL DEFAULT '0',
  `tournament_group` tinytext NOT NULL,
  `player1_points` tinytext,
  `player2_points` tinytext
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `participant`
--

DROP TABLE IF EXISTS `participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `participant` (
  `tid` int(11) NOT NULL DEFAULT '0',
  `pid` int(11) NOT NULL DEFAULT '0',
  `team` int(11) DEFAULT NULL,
  UNIQUE KEY `tplusu` (`tid`,`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `passwordrecovery`
--

DROP TABLE IF EXISTS `passwordrecovery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `passwordrecovery` (
  `uid` int(11) NOT NULL,
  `recoverytoken` text NOT NULL,
  `recoverydate` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `ipaddress` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `players`
--

DROP TABLE IF EXISTS `players`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `players` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `full_name` varchar(150) DEFAULT NULL,
  `player_name` varchar(10) NOT NULL DEFAULT '',
  `e_mail_bounce_date` datetime DEFAULT '0000-00-00 00:00:00',
  `e_mail_bounce` int(11) NOT NULL DEFAULT '0',
  `e_mail` varchar(64) DEFAULT NULL,
  `pwhash` char(32) DEFAULT NULL,
  `num_logon` int(11) NOT NULL DEFAULT '0',
  `last_logon` int(11) DEFAULT NULL,
  `date_joined` int(11) DEFAULT NULL,
  `country` varchar(30) DEFAULT NULL,
  `latitude` float(10,2) DEFAULT NULL,
  `logitude` float(10,2) DEFAULT NULL,
  `is_robot` enum('y','g') DEFAULT NULL,
  `fixed_rank` enum('y') DEFAULT NULL,
  `city` varchar(20) DEFAULT '',
  `proposal` int(11) DEFAULT NULL,
  `last_ip` varchar(16) DEFAULT NULL,
  `status` enum('unconfirmed','banned','nologin','retired','ok','deleted') NOT NULL DEFAULT 'unconfirmed',
  `locked` enum('y') DEFAULT NULL,
  `is_master` enum('n','y','d') NOT NULL DEFAULT 'n',
  `is_tournament_manager` enum('No','Yes') DEFAULT 'No',
  `is_translator` enum('No','Yes') DEFAULT 'No',
  `is_supervisor` enum('No','Yes') DEFAULT 'No',
  `last_played` int(11) DEFAULT '0',
  `identity` int(11) DEFAULT '0',
  `games_played` int(11) NOT NULL DEFAULT '0',
  `comment` text,
  `message` varchar(128) NOT NULL DEFAULT '',
  `no_email` enum('y') DEFAULT NULL,
  `language` varchar(25) NOT NULL DEFAULT 'english',
  `timezone_offset` int(11) DEFAULT NULL,
  `note_sent` enum('Yes') DEFAULT NULL,
  `discorduid` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `idx1` (`player_name`),
  KEY `proposal` (`proposal`),
  KEY `identityindex` (`identity`),
  KEY `logonindex` (`last_logon`)
) ENGINE=InnoDB AUTO_INCREMENT=57467 DEFAULT CHARSET=latin1 PACK_KEYS=1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `proposals`
--

DROP TABLE IF EXISTS `proposals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `proposals` (
  `ownerid` int(11) DEFAULT '0',
  `created` date DEFAULT NULL,
  `changed` date DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `description` text,
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `status` enum('new','rejected','deleted','accepted','implemented','commented') DEFAULT NULL,
  `comment` text,
  UNIQUE KEY `uid` (`uid`)
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ranking`
--

DROP TABLE IF EXISTS `ranking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ranking` (
  `uid` int(11) DEFAULT NULL,
  `variation` enum('zertz','zertz+11','zertz+24','zertz+xx','tumblingdown','kuba','loa','plateau','yinsh','yinsh-blitz','dvonn','gipf','tamsk','hex','hex-15','hex-19','trax','looptrax','trax-8x8','punct','loap','gobblet','gobbletm','hive','exxit','tablut','dipole','truchet','fanorona','volcano','tzaar','qyshinsu','knockabout','palago','santorini','spangles','che','micropul','medina','yavalath','mutton','cannon','warp6','triad','octiles','frogs','breakingaway','xiangqi','container','arimaa','crossfire','entrapment','lehavre','gounki','quinamid','twixt','yspahan','volo','cookie-disco','raj','universe','pan-kai','diagonal-blocks','diagonal-blocks-duo','phlip','kamisado','khet','syzygy','carnac','gyges','takojudo','mogul','align','rithmomachy','ponte','shogi','oneday','morelli','colorito','euphoria','tammany','majorities','proteus','go','stac','checkers','morris','sixmaking','veletas','modx','lyngk','chess','ultima','magnet','tintas','barca','qe','mancala','blooms','mbrane','viticulture','kulami','pushfight','blackdeath','crosswords','wyps','y','stymie','portfolio','imagine','mijnlieff','jumbulaya','dayandnight','kingscolor','chess960','iro','sprint','havannah','crosswordle','tumbleweed','ordo','meridians','trike','trench','dash','honeycomb','atomic','antidraughts','manhattan','epaminondas','circle','matrx','pendulum','bugspiel') DEFAULT NULL,
  `is_master` enum('No','Yes','Turnbased') DEFAULT 'No',
  `value` int(11) DEFAULT '1500',
  `last_played` int(11) DEFAULT '0',
  `games_started` int(11) DEFAULT '0',
  `games_won` int(11) DEFAULT '0',
  `games_lost` int(11) DEFAULT '0',
  `games_played` int(11) DEFAULT '0',
  `max_rank` int(11) DEFAULT '500',
  `prev_value_2` int(11) DEFAULT NULL,
  `prev_value_1` int(11) DEFAULT NULL,
  `advocate` enum('','apostate','student','player','teacher','evangelist') NOT NULL DEFAULT '',
  `ladder_level` int(11) DEFAULT NULL,
  `ladder_order` int(11) DEFAULT NULL,
  `ladder_mentor` int(11) DEFAULT NULL,
  UNIQUE KEY `rank` (`uid`,`variation`,`is_master`),
  KEY `uid` (`uid`),
  KEY `varindex` (`variation`,`last_played`),
  KEY `ladderindex` (`variation`,`ladder_level`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `single_site_stat`
--

DROP TABLE IF EXISTS `single_site_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `single_site_stat` (
  `month_recorded` date NOT NULL DEFAULT '0000-00-00',
  `stat_name` varchar(100) NOT NULL DEFAULT '',
  `stat_value` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`month_recorded`,`stat_name`),
  KEY `statname` (`stat_name`),
  KEY `statmonth` (`month_recorded`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sp_record`
--

DROP TABLE IF EXISTS `sp_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sp_record` (
  `player1` int(11) DEFAULT NULL,
  `variation` enum('crosswordle_55','crosswordle_66','crosswordle_65') NOT NULL,
  `score1` int(11) NOT NULL,
  `gamename` char(64) NOT NULL,
  `puzzleid` varchar(45) NOT NULL,
  `puzzledate` char(16) NOT NULL,
  `gmtdate` datetime NOT NULL,
  `time1` int(11) NOT NULL,
  `mode` enum('normal','hard') NOT NULL,
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `deviceid` char(16) DEFAULT NULL,
  `nickname` char(16) DEFAULT NULL,
  PRIMARY KEY (`uid`),
  KEY `player1` (`player1`),
  KEY `puzzleid` (`puzzleid`)
) ENGINE=InnoDB AUTO_INCREMENT=665 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `team` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `description` text,
  `tournamentid` int(11) NOT NULL,
  `owner` int(11) DEFAULT NULL,
  PRIMARY KEY (`uid`),
  KEY `tournamentid` (`tournamentid`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `timestamps`
--

DROP TABLE IF EXISTS `timestamps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `timestamps` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `data` varchar(45) NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`uid`,`data`),
  UNIQUE KEY `data_UNIQUE` (`data`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tournament`
--

DROP TABLE IF EXISTS `tournament`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tournament` (
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  `status` enum('signup','active','finished','completed') NOT NULL DEFAULT 'signup',
  `longdescription` text,
  `start` date DEFAULT '0000-00-00',
  `end` date DEFAULT '0000-00-00',
  `variation` varchar(100) NOT NULL DEFAULT '0',
  `format` enum('manual','open-rr','swiss') DEFAULT 'manual',
  `description` varchar(100) NOT NULL DEFAULT '',
  `game_threshold` int(11) NOT NULL DEFAULT '0',
  `teams` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB AUTO_INCREMENT=122 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `translation`
--

DROP TABLE IF EXISTS `translation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `translation` (
  `keystring` varchar(200) NOT NULL DEFAULT '',
  `language` enum('english','french','spanish','german','catala','norwegian','polish','portuguese','dutch','esperanto','russian','swedish','greek','chinese','czech','chinese-traditional','romanian','japanese','italian') NOT NULL DEFAULT 'english',
  `comment` text,
  `context` text,
  `translation` text NOT NULL,
  `translator` int(11) DEFAULT '0',
  `changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `collection` varchar(20) NOT NULL DEFAULT 'web',
  PRIMARY KEY (`language`,`keystring`,`collection`),
  KEY `languageindex` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `variation`
--

DROP TABLE IF EXISTS `variation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `variation` (
  `code` varchar(100) NOT NULL DEFAULT '',
  `familyname` varchar(100) NOT NULL DEFAULT '',
  `max_players` int(11) NOT NULL DEFAULT '2',
  `name` varchar(100) NOT NULL DEFAULT '',
  `directory` varchar(100) DEFAULT '',
  `directory_index` int(11) DEFAULT '0',
  `included` tinyint(1) NOT NULL DEFAULT '0',
  `viewer` varchar(100) NOT NULL DEFAULT '',
  `description` varchar(100) NOT NULL DEFAULT '',
  `subvariants` text,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zertz_gamerecord`
--

DROP TABLE IF EXISTS `zertz_gamerecord`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zertz_gamerecord` (
  `player1` int(11) NOT NULL DEFAULT '0',
  `player2` int(11) NOT NULL DEFAULT '0',
  `variation` enum('zertz','zertz+11','zertz+24','zertz+xx','tumblingdown','kuba','loa','plateau','yinsh','yinsh-blitz','dvonn','gipf','tamsk','hex','hex-15','hex-19','trax','looptrax','trax-8x8','punct','loap','gobblet','gobbletm','hive','exxit','tablut','dipole','truchet','fanorona','volcano','tzaar','qyshinsu','knockabout','palago','santorini','spangles','che','micropul','medina','yavalath','mutton','cannon','warp6','tajii','xiangqi','arimaa','crossfire','entrapment','gounki','quinamid','twixt','volo','cookie-disco','pan-kai','diagonal-blocks-duo','phlip','kamisado','khet','syzygy','carnac','gyges','takojudo','align','rithmomachy','ponte','shogi','morelli','colorito','majorities','proteus','go','stac','checkers','morris','sixmaking','veletas','modx','lyngk','chess','ultima','magnet','tintas','barca','mancala','blooms','mbrane','kulami','pushfight','wyps','y','stymie','mijnlieff','crosswords','dayandnight','kingscolor','chess960','iro','havannah','tumbleweed','ordo','meridians','trike','trench','atomic','antidraughts','epaminondas','circle','matrx') DEFAULT NULL,
  `winner` enum('player1','player2','draw') DEFAULT NULL,
  `time1` int(11) DEFAULT NULL,
  `time2` int(11) DEFAULT NULL,
  `rank1` int(11) DEFAULT NULL,
  `rank2` int(11) NOT NULL,
  `gamename` char(64) DEFAULT NULL,
  `mode` enum('normal','unranked','master') DEFAULT 'unranked',
  `turnbased` enum('no','yes') NOT NULL DEFAULT 'no',
  `tournament_id` int(11) DEFAULT '0',
  `tournament` enum('no','yes') DEFAULT 'no',
  `gmtdate` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `digest_mid` int(11) DEFAULT NULL,
  `digest_end` int(11) DEFAULT NULL,
  `uid` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`uid`),
  KEY `player2` (`player2`),
  KEY `digest_mid` (`digest_mid`),
  KEY `tournamentindex` (`tournament_id`),
  KEY `digest_end` (`digest_end`),
  KEY `player1` (`player1`),
  KEY `variation` (`variation`,`gmtdate`),
  KEY `dateindex` (`gmtdate`)
) ENGINE=InnoDB AUTO_INCREMENT=496446 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-07-30 17:47:18
