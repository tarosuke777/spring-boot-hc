// https://developer.mozilla.org/ja/docs/Web/API/WebSockets_API/Writing_WebSocket_client_applications

$(function () {
	
	const socket = new WebSocket('ws://localhost:8080/hc-websocket?' + $("#jsiChannelId").val());;

	socket.onmessage = (message) => {
		  showMessage(JSON.parse(message.data));
	};

	function sendName() {
		const msg = {
		  content: $("#jsiContent").val(),
		  channelId: $("#jsiChannelId").val()
		};
		
		socket.send(JSON.stringify(msg));
		$("#jsiContent").val("");
	}

	function showMessage(message) {
		$("#messages").append("<tr><td>"+ message.createdAt + "</td><td>" + message.content + "</td></tr>");
	}
	
	$( "#jsiSend" ).click(() => sendName());
	
});

// https://stackoverflow.com/questions/10503606/scroll-to-bottom-of-div-on-page-load-jquery