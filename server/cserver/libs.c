
#include "libs.h"



void BugMsg
	(char *file,			//@parm the name of the current file, normally __FILE__
	 long line,				//@parm the line number of the current line, normall __LINE__
	 char *msg,				//@parm a context "printf" control string
	 ...)					//@parmvar a variable number of arguments for printf
{
 va_list args;
  va_start(args,msg);
  {char xmsg[BUFFERSIZE];
   
	  {char cxmsg[BUFFERSIZE];
	   Show_Catch_Context(sizeof(cxmsg),cxmsg);
	   lsprintf(sizeof(xmsg),&xmsg[0],
		   "In file %s, at line %ld: %s\ncontext: %s",
		   file,line,msg,cxmsg);
	  }

   vPrintfMsgBoxError("FatalError",xmsg,args);
  }
  va_end(args);
  DO_ESCAPE(3);
}

/* @func
This function always writes exactly NUM characters, and
guarantees a NULL at the end of the copied string.
@rvalue  TRUE | if the destination string was large enough
@rvalue FALSE | the destination string was truncated to fit the length
@normal
<nl>Overview: <l String utilities>
*/
BOOL MyStrncpy
	(char *dst,			//@parm the destination
	const char *src,	//@parm the source string
	long num)			//@parm the number of characters to copy
{
	long i=0;
	if(!(src && dst && (num>0))) 
	{
#ifdef _DEBUG
		BUG3("Invalid args to MyStrncpy src=%x dest=%x n=%d",src,dst,num);
#endif
		return(FALSE); 
	}
	while (i<num) 
		{ BYTE ch=src[i];
		  dst[i]=ch; 
		  if(ch==0) { break; }
		  i++;	//increment late to preserve i if string exactly fits
		}
	if(i<num)
	{	while(i<num) { dst[i++]=(char)0; }		//fill out the rest of the dest with nulls
		return(TRUE);							//dest was long enough
	}
	else
	{	dst[num-1] = (char)0;
		return(FALSE);				//dest was too short
	}
}


static void fmtbug(long space, char *format, char *tmpfmt)
{	char tmpbuf[30];
	MyStrncpy(tmpbuf,format,sizeof(tmpbuf));
	BUG3("lvsprintf: buffer size %d exceeded, format \"%s\" at %s",space,tmpbuf,tmpfmt);
}
int lvsprintf_err
	(BOOL fatal,
	 long space,	//@parm the buffer size
	 char *dest,	//@parm the buffer
	 char *format,	//@parm the format specifier
	 va_list args)	//@parm variable arg list
{	char *start_format = format;
    register char *buffp = dest;
 	long spaceleft = space-1;	//space left
    register char c;
    register char *tp;
    char tempfmt[64];		//temporary format
	char tempbuf[64];		//temporary printing
#ifndef LONGINT
    int longflag;
#endif

#ifndef _DEBUG
	if(space<=4) { BUG("Buffer too small - possibly (sizeof *char)"); }
#endif

    tempfmt[0] = '%';
    while((c = *format++)!=0) {
	if(c=='%') {
	    tp = &tempfmt[1];
#ifndef LONGINT
	    longflag = 0;
#endif
continue_format:
	    switch(c = *format++) {
		case 's':
		    *tp++ = c;
		    *tp = '\0';
			{ char * str = va_arg(args, char *);
			if(str)
			{
			  if(((long)strlen(str))>spaceleft) 
			  { if(fatal) {fmtbug(space,start_format,tempfmt); }
				spaceleft=0;
			  }
			  else
			  {
			  long len = sprintf(buffp, tempfmt, str);
			  {buffp += len;
			   spaceleft -= len;
			  }
			  }
			}
			}
		    break;
		case 'u':
		case 'x':
		case 'o':
		case 'X':
#ifdef UNSIGNEDSPECIAL
		    *tp++ = c;
		    *tp = '\0';
#ifndef LONGINT
		    if(longflag)
			{long len = Sprintf(tempbuf, tempfmt, va_arg(args, unsigned long));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			 else
			 {
			 strcpy(buffp,tempbuf)
			 buffp += len;
			 spaceleft -= len;
			 }
			}
		    else
#endif
			{
			long len = Sprintf(tempbuf, tempfmt, va_arg(args, unsigned));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf)
			buffp += len;
			spaceleft -= len;
			}}
		    break;
#endif
		case 'd':
		case 'c':
		case 'i':
		    *tp++ = c;
		    *tp = '\0';
#ifndef LONGINT
		    if(longflag)
			{long len = sprintf(tempbuf, tempfmt, va_arg(args, long));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt);}
			 spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    else
#endif
			{
			long len = sprintf(tempbuf, tempfmt, va_arg(args, int));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); }
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case 'f':
		case 'e':
		case 'E':
		case 'g':
		case 'G':
		    *tp++ = c;
		    *tp = '\0';
			{long len = sprintf(tempbuf, tempfmt, va_arg(args, double));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case 'p':
		    *tp++ = c;
		    *tp = '\0';
			{long len = sprintf(tempbuf, tempfmt, va_arg(args, pointer));
			if(len>spaceleft) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			  spaceleft=0; 
			}
			else
			{
			strcpy(buffp,tempbuf);
			buffp += len;
			spaceleft -= len;
			}}
		    break;
		case '-':
		case '+':
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		case '.':
		case ' ':
		case '#':
		case 'h':
		    *tp++ = c;
		    goto continue_format;
		case 'l':
#ifndef LONGINT
		    longflag = 1;
		    *tp++ = c;
#endif
		    goto continue_format;
		case '*':
		    tp += sprintf(tp, "%d", va_arg(args, int));
		    goto continue_format;
		case 'n':
		    *va_arg(args, intp) = buffp - dest;
		    break;
		case '%':
		default:
			if(spaceleft<=0) 
			{ if(fatal) { fmtbug(space,start_format,tempfmt); } 
			}
			else
			{
		    *buffp++ = c;
			spaceleft--;
			}
		    break;
	    }
	} 
	else
	{ 
	  if(spaceleft<=0) { if(fatal) {fmtbug(space,start_format,tempfmt); }}
	  *buffp++ = c;
	  spaceleft--;
	}
    }
	if(spaceleft<0) { if(fatal) {fmtbug(space,start_format,tempfmt); }}
    *buffp = '\0';
	if(spaceleft>1) 
	{ buffp[1] = 0; //add an extra 0 for andromeda's escape detection in moonshot
	}
    return buffp - dest;
}

/* @func
This is vsprintf with the addition of a buffer length argument, so you won't crash
or generate unpredictable results if you accidentally exceed the size of the buffer.
<nl>Overview: <l Printing>
*/
int lvsprintf
	(long space,	//@parm the buffer size
	 char *dest,	//@parm the buffer
	 char *format,	//@parm the format specifier
	 va_list args)	//@parm variable arg list
{	return(lvsprintf_err(TRUE,space,dest,format,args));
}

/* @func
This is sprintf with the addition of a buffer length argument, so you won't crash
or generate unpredictable results if you accidentally exceed the size of the buffer.
<nl>Overview: <l Printing>
*/
long lsprintf
	(long size,	//@parm the buffer size
	 char *buf,	//@parm the buffer
	 char *fmt,	//@parm the format string
	 ...)		//@parmvar the arguments
{	va_list lst;
	va_start(lst,fmt);
	return(lvsprintf(size,buf,fmt,lst));
	va_end(fmt);
}
