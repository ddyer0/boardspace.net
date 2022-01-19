#include <stdio.h>
#include <fcntl.h>
#include <errno.h>

int main(int argc, char **argv)
{
  //  printf("FS_SETSIZE is %d (%d)\n",FD_SETSIZE,FD_SETSIZE*__NFDBITS);
  printf("EOUWOULDBLOCK=%d\n",EWOULDBLOCK); 
  printf("EAGAIN=%d\n",EAGAIN); 
  printf("Argc %d\n",argc);
  if(argc>1)
    {
    long n=0;
    long k=0;
    printf("Argv[1] %s\n",argv[1]);
    sscanf(argv[1],"%d",&n);
    k=n;
    if(argc>2)
      {
      printf("Argv[2] %s\n",argv[2]);
      sscanf(argv[2],"%d",&k);
      }
  while(n<=k)
    {   printf("n = %d\n",n);
	 printf("error %d: %s\n",n,strerror(n));
	 n++;
     }
    }
  exit(0);

}
