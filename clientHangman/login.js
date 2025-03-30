document.addEventListener("DOMContentLoaded", (e) => {
    let username, password, address, port;
    const PROXY_ADDRESS = "localhost"; // Dirección del servidor proxy
    const PROXY_PORT = "8080";         // Puerto del servidor proxy

    // DOM elements
    const usernameInputElement = document.querySelector(".username");
    const passwordInputElement = document.querySelector(".password");
    const submitButtonElement = document.querySelector(".submit");
    const serverModal = document.querySelector(".server-modal");
    const serverAddressInput = document.querySelector(".server-address");
    const serverPortInput = document.querySelector(".server-port");
    const applyServerBtn = document.querySelector(".apply-server");
    const cancelServerBtn = document.querySelector(".cancel-server");
    
    // Create and add server connection button
    const serverConnectionBtn = document.createElement("button");
    serverConnectionBtn.className = "server-connection-btn";
    serverConnectionBtn.textContent = "Configure Server Connection";
    document.querySelector(".loginInput").appendChild(serverConnectionBtn);

    // Event listeners
    serverConnectionBtn.addEventListener("click", showServerModal);
    applyServerBtn.addEventListener("click", applyToServer);
    cancelServerBtn.addEventListener("click", hideServerModal);
    usernameInputElement.addEventListener("input", updateUsernameValue);
    passwordInputElement.addEventListener("input", updatePasswordValue);
    submitButtonElement.addEventListener("click", submit);

    function updateUsernameValue(e) {
        username = e.target.value;
    }

    function updatePasswordValue(e) {
        password = e.target.value;
    }

    function showServerModal() {
        serverModal.style.display = "flex";
    }
    
    function hideServerModal() {
        serverModal.style.display = "none";
    }
    
    function applyToServer() {
        address = serverAddressInput.value;
        port = serverPortInput.value;
        
        if (!address || !port) {
            alert("Please enter both server address and port");
            return;
        }
        
        hideServerModal();
    }
    
    function submit() {
        if (!username || !password) {
            alert("Please insert your username and password");
            return;
        }
        
        if (!address || !port) {
            alert("Please configure your server connection");
            return;
        }
    
        // Almacenar los datos del servidor real para uso posterior
        localStorage.setItem('serverConfig', JSON.stringify({
            address: address,
            port: port
        }));
    
        // Usar el proxy WebSocket en lugar del servidor directo
        const socket = new WebSocket(`ws://${PROXY_ADDRESS}:${PROXY_PORT}`);
        
        socket.onopen = function() {
            console.log("Connected to proxy server");
            
            // Enviar mensaje de configuración
            const serverInfo = {
                type: "connection",
                serverAddress: address,
                serverPort: port
            };
            socket.send(JSON.stringify(serverInfo));
            
            // Luego enviar datos de autenticación
            setTimeout(() => {
                socket.send(`1.${username}.${password}`);
                console.log("Sent authentication data");
            }, 500);
            
            // Manejar respuestas del servidor
            socket.onmessage = function(event) {
                console.log("Response received:", event.data);
                const [rule, ...data] = event.data.split('.');
                if (rule === "1" && data[0] === "success") {
                    localStorage.setItem('hangmanSocket', JSON.stringify({
                        address: PROXY_ADDRESS,
                        port: PROXY_PORT
                    }));
                    // Redirige a game.html
                    window.location.href = "game.html";
                } else {
                    alert("Login failed: " + data.join(' '));
                }
            };
        };
        
        socket.onerror = function(error) {
            console.error("WebSocket error:", error);
            alert("Connection failed. Please make sure the proxy server is running.");
        };
        
        socket.onclose = function() {
            console.log("Connection closed");
        };
    }
});