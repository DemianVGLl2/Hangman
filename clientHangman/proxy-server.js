// proxy-server.js
const WebSocket = require('ws');
const net = require('net');

// Configuración del servidor WebSocket
const wss = new WebSocket.Server({ port: 8080 });
console.log('Servidor proxy WebSocket iniciado en puerto 8080');

wss.on('connection', function connection(ws) {
  console.log('Cliente WebSocket conectado');
  
  let tcpClient = null;
  let serverConfig = null;
  
  // Escuchar mensajes del cliente web
  ws.on('message', function incoming(message) {
    console.log('Mensaje recibido:', message.toString());
    
    // Comprobar si es un mensaje de configuración
    try {
      const jsonMessage = JSON.parse(message);
      if (jsonMessage.type === "connection") {
        serverConfig = {
          address: jsonMessage.serverAddress || '172.17.69.134',
          port: parseInt(jsonMessage.serverPort) || 5000
        };
        
        // Iniciar conexión TCP al servidor C
        tcpClient = new net.Socket();
        
        tcpClient.connect(serverConfig.port, serverConfig.address, function() {
          console.log(`Conectado al servidor C en ${serverConfig.address}:${serverConfig.port}`);
        });
        
        // Configurar eventos para la conexión TCP
        tcpClient.on('data', function(data) {
          console.log('Respuesta del servidor C:', data.toString());
          ws.send(data.toString());
        });
        
        tcpClient.on('close', function() {
          console.log('Conexión TCP cerrada');
          // No cerramos el WebSocket aquí para permitir reconexiones
        });
        
        tcpClient.on('error', function(err) {
          console.log('Error en conexión TCP:', err);
          ws.send(`error.${err.message}`);
        });
        
        return; // No reenviar mensaje de configuración al servidor C
      }
    } catch (e) {
      // No es un mensaje JSON, asumir que es un mensaje para el servidor C
    }
    
    // Reenviar mensaje al servidor C
    if (tcpClient && tcpClient.writable) {
      tcpClient.write(message);
    } else {
      console.error('No hay conexión TCP activa');
      ws.send('error.No connection to server');
    }
  });
  
  // Manejar desconexión del WebSocket
  ws.on('close', function() {
    console.log('WebSocket desconectado');
    if (tcpClient) {
      tcpClient.destroy();
    }
  });
});