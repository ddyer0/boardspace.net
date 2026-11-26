package net.boardspace.migs;

/** state of a restartable game (C struct GameBuffer) */
final class GameBuffer
{
    static final int GAMEBUFMAGIC = 0xfafe9193;

    int magic = GAMEBUFMAGIC;
    String idString = "";			// UIDSIZE
    long startTime;					// days since 1970
    byte[] gamePtr;					// the game text, NUL terminated, like the C buffer
    int gamePtrSize;				// == gamePtr.length, or 0 when gamePtr is null
    GameBuffer next, prev;
    int refCount;
    int hashCode;					// unsigned in C
    int uid;
    int gamePtrOffset;
    GameBuffer next_dirty_gamebuffer;
    Session ownerSession;
    boolean hashed;
    boolean preserved;
    boolean dirty;
}
