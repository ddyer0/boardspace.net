package net.boardspace.migs;

/** a user registered by the login scripts (C struct registeredUser) */
final class RegisteredUser
{
    int ip;
    int regkey;
    long timestamp;
    String clientName = "";
    int clientUid;
    RegisteredUser next;
}
