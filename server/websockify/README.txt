
This is the proof of concept websocket server.  It runs
on the unmodified live server and bridges websockets.

the basic configuration is to run 

	websockify hostname:listensocket hostname:targetsocket

listensocket is a random available socket (open it in the firewall)
targetsocket is the customary server port for the test server. 

for testing purposes, you can use echoserver as the target, or netcat

in principle, whatever service you normally provide on targetsocket
can run unchanged. 


echo.c is a simple echo server cribbed from a random internet page.

cheerpj.js contains the initialization for cheerpj, including the tricky
	 native interfaces that have to match the java code exactly.
	 it's normally deployed as /js/cheerpj.js

applettag.cgi provides the launchers for offline reviewing (and a
	collection of other services.).  It's invoked by each of the
	xx-viewer.shtml files

apple
so summary: the server runs unmodified
	    websockify runs as a proxy between 

webchat.html is a simple test that connects to websockify and pings
a few lines, expecting them to be echoed.  to run this test, set up
websockify to listen on port 12346 and connect to echoserver on any
port you wish.

to set this up as a service, (advice from https://abhinand05.medium.com/run-any-executable-as-systemd-service-in-linux-21298674f66f)

copy websockify and websockify-service-start to /usr/sbin
copy websockify.service to /etc/systemd/system/
systemctl daemon-reload
systemctl enable websockify.service
systemctl start websockify.service

