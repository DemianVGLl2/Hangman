document.addEventListener("DOMContentLoaded", (e) => {
    let socket; // This will hold our WebSocket connection
    let username, password, address, port;

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

        // Connect to WebSocket (not creating a server!)
        socket = new WebSocket(`ws://${address}:${port}`);
        
        

        socket.onopen = function() {
            
            // Send authentication data
            socket.send(`1.${username}.${password}`);
            console.log("Here");
            // Handle server responses
            socket.onmessage = function(event) {
                const [rule, ...data] = event.data.split('.');
                if (rule === "1" && data[0] === "success") {
                    // Store socket in localStorage to access it in game.html
                    localStorage.setItem('hangmanSocket', JSON.stringify({
                        address: address,
                        port: port
                    }));
                    
                    // Redirect to game page
                    window.location.href = "game.html";
                } else {
                    alert("Login failed: " + data.join(' '));
                }
            };
        };
        
        socket.onerror = function(error) {
            console.error("WebSocket error:", error);
            alert("Connection failed. Please check server address and port.");
        };
    }
});