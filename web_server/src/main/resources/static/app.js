var stompClient = null;

function connect() {
    var socket = new SockJS("/my-websocket");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log("Connected: " + frame);
        stompClient.subscribe("/topic/messages", function (message) {
            console.log(message);
            showMessage(message);
            });
    });
}

function disconnect() {

    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log("Disconnected");
}

function sendClosedMessage() {
    stompClient.send("/app/closed", {},
    JSON.stringify({"content":"Page closed"}));
}

function showMessage(message) {
    document.getElementById("message").innerHTML = message.body;
}

// Automatically connect when the page loads
window.onload = function() {
    connect();
};

window.addEventListener('beforeunload', function (e) {
    console.log("Unsubscribing");
    sendClosedMessage();
    console.log("Disconnecting");
    disconnect(); // Automatically disconnect when the page is about to be closed
});
