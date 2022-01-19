if (! $?SSH_TTY && ! $?SSH_CLIENT ) then
echo "Telnet connections are not allowed.  Please reconnect with SSH"
logout
endif
setenv EDITOR emacs
setenv QMAIL /home/vpopmail/domains/real-me.net
setenv MAILLOG /var/log/send
#set path = ($path /usr/sbin /usr/jdk1.2.2/bin/)
alias dir ls -al
alias procs "ps -lu boardspa"
alias toplog "top -i -b -d 30 > top.log &"
