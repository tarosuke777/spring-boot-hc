const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/gs-guide-websocket'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/message', (message) => {
        showMessage(JSON.parse(message.body));
    });
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    if (connected) {
        $("#jsiConversation").show();
    }
    else {
        $("#jsiConversation").hide();
    }
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.publish({
        destination: "/app/message",
        body: JSON.stringify({'content': $("#jsiContent").val(), 'channelId':$("#jsiChannelId").val()})
    });
	$("#jsiContent").val("");
}

function showMessage(message) {
	$("#messages").append("<tr><td>"+ message.createdAt + "</td><td>" + message.content + "</td></tr>");
}

$(function () {
	connect();
    $( "#jsiSend" ).click(() => sendName());
});