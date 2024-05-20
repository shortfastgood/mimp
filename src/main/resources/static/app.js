var stompClient = null;

function disableElements(elementIds) {
    elementIds.forEach(function(elementId) {
        document.getElementById(elementId).disabled = true;
    });
}

function enableElements(elementIds) {
    elementIds.forEach(function(elementId) {
        document.getElementById(elementId).disabled = false;
    });
}

function setConnected(connected) {
    if (connected) {
       disableElements(["wsConnect"]);
       enableElements(["wsDisconnect"]);
    } else {
         enableElements(["wsConnect"]);
         disableElements(["wsDisconnect"]);
    }
}

function connect() {
    var socket = new SockJS('/mimp-ws');
    stompClient = Stomp.over(socket);
    stompClient.heartbeat.outgoing = 30000;
    stompClient.heartbeat.incoming = 0;
    stompClient.reconnect_delay = 5000;
    stompClient.connect({}, function(frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe(('/topic/error'), function(message) {
            showAlert(JSON.parse(message.body).message);
        });
        stompClient.subscribe('/topic/mimp', function(message) {
            showMessage(JSON.parse(message.body));
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function showAlert(message) {
    alert(message.detail);
}

function showMessage(message) {

}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelector('form').addEventListener('submit', function(event) {
        event.preventDefault();
    });
    document.getElementById('wsConnect').addEventListener('click', connect);
    document.getElementById('wsDisconnect').addEventListener('click', disconnect);
});