
let socket = null;
let message = null;

socket.addEventListener("open", (event) => {
  console.log("connected");
});
socket.addEventListener("message", (event) => {
  console.log("recv", event.data);
  message = event.data;
});

await cheerpjInit({
  natives: {
    async Java_com_web_WebSocket_read(lib) {
      const m = message;
      message = null;
      return m;
    },
    async Java_com_web_WebSocket_send(lib, message) {
      socket.send(message);
    },
    async connect(host,socket)
    {
	socket = new WebSocket("ws://" . host . ":" . socket);
     }
  },
});
