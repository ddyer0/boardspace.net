
function isTouchEnabled() {
	    return ( 'ontouchstart' in window ) || 
		( navigator.maxTouchPoints > 0 ) || 
		( navigator.msMaxTouchPoints > 0 );
};

function touchTest()
{  let mouse = matchMedia('(pointer:fine)').matches;
   if( !mouse && isTouchEnabled())
{ alert("touch interfaces are not supported yet,\nplease use the boardspace.net app");
}


}
function getOS() {
  const userAgent = window.navigator.userAgent,
      platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
      macosPlatforms = ['macOS', 'Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
      windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'],
      iosPlatforms = ['iPhone', 'iPad', 'iPod'];
  let os = null;

  if (macosPlatforms.indexOf(platform) !== -1) {
    os = 'Mac OS';
  } else if (iosPlatforms.indexOf(platform) !== -1) {
    os = 'iOS';
  } else if (windowsPlatforms.indexOf(platform) !== -1) {
    os = 'Windows';
  } else if (/Android/.test(userAgent)) {
    os = 'Android';
  } else if (/Linux/.test(platform)) {
    os = 'Linux';
  }

  return os;
}

function alertUnsupportedOS()
{
  var os = getOS();
  if(! ((os == 'Windows') || (os == 'Linux') || (os == 'Max OS')))
  {
   alert("Not supported on " + os + "\nDon't expect this to work");
  }
}

let sockets = [];
let nsockets = 0;  
let clips = [];
let cheerpjparent = document.body;

function redirectifHttps()
{
if(window.location.protocol == "https:")
{ var newloc = "http://" + window.location.hostname + window.location.pathname;
  window.location = newloc;
}
}

function createDisplay(width,height,parent)
{
   cheerpjparent = parent;
   cheerpjCreateDisplay(width,height,parent);
}

async function initNatives()
{   
    await cheerpjInit( 
    { 	clipboardMode: "system" ,
	natives: {

	Java_bridge_Cheerpj_playSound(lib,_this,cl)
	{
 	 var clip = clips[cl];
 	 if(!clip) { 
		clip = new Audio(cl);
		clips[cl] = clip;
		//alert("new clip "+cl);
		}
	 //alert("play "+cl);
 	 clip.play();
	},
	Java_bridge_Cheerpj_getWidth(lib,_this) { 
	     return cheerpjparent.clientWidth; 
         }
	,Java_bridge_Cheerpj_getHeight(lib,_this) {
	     return cheerpjparent.clientHeight; 
	 },
         async Java_bridge_WebSocket_read(lib,_this,sock) {
              let socket = sockets[sock];
	      const m = socket.message;
	      socket.message = null;
	      return m;
	    },
         async Java_bridge_WebSocket_send(lib,_this,sock,message) {
              let socket = sockets[sock];
	      socket.send(message);
	    },
	Java_bridge_WebSocket_isConnected(lib,_this,sock)
	{ let socket = sockets[sock];
	  return socket.connok;
	},
	 Java_bridge_WebSocket_connect(lib,_this,host,socket)
	    {  let target = "ws://" + host + ":" + socket +"/gameserver";
	       console.log("make socket "+target );
               let sock = new WebSocket(target);
               let n = nsockets++;

               sock.message =null;
               sock.myIndex = n;
	       sock.connok = false;
               sockets[n] = sock;

	       sock.addEventListener("open", (event) => {
		  console.log("connected");
                  sock.connok = true;
		});

	       sock.addEventListener("message", (event) => {
		   sock.message = sock.message==null ? event.data : sock.message + event.data;
	       });
                 return n;
	    }
         }
    });
}






