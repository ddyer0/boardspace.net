
#define BS 512



/*
[**************************************************\
The QuerySZValue function retrieves the data for a
specified value name associated with an open registry
key.
----- Parameters -----
hKey

Identifies a currently open key or any of the
following predefined reserved handle values: 
	HKEY_CLASSES_ROOT
	HKEY_CURRENT_USER
	HKEY_LOCAL_MACHINE
	HKEY_USERS

lpszKeyName

Points to a null-terminated string containing the name
of the key to be queried. 

lpszValueName

Points to a null-terminated string containing the name
of the value to be queried. 

lpbData

Points to a buffer that receives the value's data.
This parameter can be NULL if the data is not
required. 

lpcbData

Points to a variable that specifies the size, in
bytes, of the buffer pointed to by the lpbData
parameter. When the function returns, this variable
contains the size of the data copied to lpbData. 
If the buffer specified by lpbData parameter is not
large enough to hold the data, the function returns
the value ERROR_MORE_DATA, and stores the required
buffer size, in bytes, into the variable pointed to by
lpcbData. 

If lpbData is NULL, and lpcbData is non-NULL, the
function returns ERROR_SUCCESS, and stores the size of
the data, in bytes, in the variable pointed to by
lpcbData. This lets an application determine the best
way to allocate a buffer for the value key's data. 

The lpcbData parameter can be NULL only if lpbData is
NULL. 

----- Return Value -----
If the function succeeds, the return value is
ERROR_SUCCESS.

If the function fails, the return value is an error
value.

----- Remarks -----
The key identified by hKey must have been opened with
KEY_QUERY_VALUE access. To open the key, use the
RegCreateKeyEx or RegOpenKeyEx function. 

----- See Also -----
RegQueryValueEx
\**************************************************]*/
static LONG QuerySZValue (
    HKEY		hKey,	// handle of key to query 
    LPCTSTR		lpszKeyName,	// address of name of the key to query
	LPCTSTR		lpszValueName,	// address of the name of value to query
    LPTSTR		lpbData,	// address of data buffer 
    LPDWORD		lpcbData 	// address of data buffer size 
   )	
{
	HKEY hValueKey;
	LONG lRet;

	if((lRet = RegOpenKeyEx(hKey,lpszKeyName,0,KEY_QUERY_VALUE,&hValueKey)) != ERROR_SUCCESS)
		return(lRet);
	lRet = RegQueryValueEx(hValueKey,lpszValueName,NULL,NULL,lpbData,lpcbData);
	(void)RegCloseKey(hValueKey);
	return(lRet);
}

unsigned long machine_key(unsigned long seed,char **keyset)
{	char Result[BS];
	unsigned long key = seed;
	char **ka=keyset;
	while(*ka!=0)
	{ char Name[BS];
	  strcpy(Name,*ka);
	  {char *p=strrchr(&Name[0],'\\');
	   	long sz=sizeof(Result);
		*p++=0;
		Result[0]=(char)0;
		{long val=QuerySZValue(HKEY_LOCAL_MACHINE,&Name[0],p,&Result[0],&sz);
	    if((val==0) && (Result[0]!=(char)0))
		{	size_t i,len=strlen(Result);
#if VERBOSE
		printf("OK Key: %s\n",*ka);
#endif
			for(i=0;i<len;i++) { key += Result[i]; key=key*3; } 
		}
	  }}
	  ka++;
	}
	return(key);
}
unsigned long final_key(unsigned long machine_key)
{	int i=0;
	for(i=1;i<20;i++) 
	{ unsigned int iv= machine_key^((machine_key<<i)|(machine_key>>(32-i)));
	  machine_key += iv;
	  machine_key = (machine_key>>3)|(machine_key<<29);
	}
	return(machine_key);
}
