package net.boardspace.migs;

/** a banned user or ip (C struct bannedUser) */
final class BannedUser
{
    int banned_ip;
    long startTime;
    String cookie = "";				// BANSIZE
    char serverBancode;
    String userName = "";
    int userUid;
    int attempts;
    int eventid;
}
