package udp;

/**
This package implements a simple UDP beacon, where servers can advertise services and clients
can identify themselves and and servers they might connect to.

There's no background "drumbeat".  Each participant announces themselves a few times, then
goes silent and waits for other participants to show themselves.   Normally, a proto-server
will respond when a suitable client announces itself, so then they will both know about each
other.

On windows this is all fairly straightforward, but not expected to be used.
On IOS, only the "client" mode works because IOS restricts broadcast mode UDP
..that's OK for now, because we only expect android devices to be servers.
On Android "table size" devices will advertise themselves as servers, and other
mobile devices will connect as clients.

*/
