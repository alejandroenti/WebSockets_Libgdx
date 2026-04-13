const { createServer } = require('http');
const { WebSocketServer } = require('ws');
 
const server = createServer();
const wss = new WebSocketServer({ server });
 
wss.on('connection', function connection(ws) {
  console.log("Nova connexió.");
  ws.send('something');
 
  ws.on('error', console.error);
 
  ws.on('message', function message(data) {
    console.log('received: %s', data);
  });
 
  ws.on('close', function close() {
    console.log("Tancant connexió.")
  })
 
});
 
server.listen( 8888, function() {
  console.log("listening...");
});