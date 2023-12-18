

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

let sockets = [];
let nsockets = 0;  

async function initNatives()
{   
    await cheerpjInit( 
    { 	clipboardMode: "system" ,
	natives: {

	Java_bridge_Cheerpj_getWidth(lib,_this) { 
	     return document.body.clientWidth; 
         }
	,Java_bridge_Cheerpj_getHeight(lib,_this) {
	     return document.body.clientHeight; 
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
	    {  let target = "wss://" + host + ":" + socket +"/gameserver";
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






