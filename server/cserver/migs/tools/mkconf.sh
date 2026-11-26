#!/bin/sh
# mkconf.sh <dir> <port> <wsport> [obfuscation] [sequencenumbers] [sslport]
# writes <dir>/test.conf for a throwaway test server
D=$1; P=$2; W=$3; OB=${4:-0}; SQ=${5:-0}; S=${6:-0}
mkdir -p $D/cache $D/games
cat > $D/test.conf <<EOC
logfile,$D/main.log
chatfile,$D/chat.log
securityfile,$D/security.log
max_logfile_megabytes,10
gamecachedir,$D/cache/
gamecacherate,10
bindip,127.0.0.1
port,$P
loglevel,2
password,xyzzy
lobby-timeout,600
client-timeout,600
websocket,$W
sslport,$S
websocketsslkey,/tmp/mt/key.pem
websocketsslcert,/tmp/mt/cert.pem
statusfile,$D/status.html
player-info-url,"/u?pname=%s"
ngamedirs,1
gamedir0,$D/games/
ngametypes,2
gametype0,Hex
gametype1,Tablut
nroomtypes,2
roomtype0,Nidle
roomtype1,Ggame
players,100
rooms,20
connections,30
connections-per-session,15
connections-per-uid,20
strict,1
strict-score,1
obfuscation,$OB
sequencenumbers,$SQ
use-threads,1
crash-test,0
detatch,0
EOC
