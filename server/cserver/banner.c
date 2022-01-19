#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/timeb.h>
//
// this is a simple hack designed to trick browsers into caching a unique number
// think of it as a "sealth cookie"
//
int main(int argc, char **argv)
{ 
  struct timeb now;
  ftime(&now);
  fprintf(stdout,"Content-Type: text/plain\n");
  fprintf(stdout,"Last-Modified: Sat, 24 Jan 1970 03:33:21 GMT\n");
  fprintf(stdout,"\n");
  fprintf(stdout,"%d%'0'3d%'0'4d\n",now.time,now.millitm,getpid()%10000);
  return(0);
}
