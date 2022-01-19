extern fileBuffer mainLog;
extern fileBuffer chatLog; 
extern fileBuffer securityLog;	
extern int statusThreadRunning;
extern int killThreads;

void MyStrncpy(char *dest,const char *src,size_t destsize);
int lsprintf
	(size_t size,
	 char *buf,
	 char *fmt,
	 ...);
int lvsprintf
	(size_t space,
	 char *dest,
	 char *format,
	 va_list args);
typedef int *intp;
void logEntry(fileBuffer *B,char *str,...);
char *timestamp();
#if WIN32
void usleep(int n);
#endif

#if WIN32
void Start_Winsock();
#endif

void ExitWith(int val);
void error(char *inStr, int err);
extern int totalAllocations;
extern int allocations;
extern size_t allocatedSize;
void *ALLOC(size_t size);
void FREE(void *obj,size_t size);
void CHECK(void *obj,size_t size);
void MEMCPY(void *dest,void *src,size_t siz);
void MEMMOVE(void *dest,void *src,size_t siz);
