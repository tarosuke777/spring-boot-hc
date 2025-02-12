
// https://ja.legacy.reactjs.org/docs/components-and-props.html

const App = () => {
	
	const socketRef = React.useRef();
	const contentRef = React.useRef(null);
	
	React.useEffect(() => {
		
		const messagesTable = document.getElementById('jsiConversation').getElementsByTagName('tbody')[0];
		const channelId = document.getElementById('jsiChannelId').value;
;
		fetch(`/messages?channelId=${channelId}`, {
		  method: 'GET',
		  headers: {
		    'Accept': 'application/json'
		  }
		})
		.then(response => {
		  if (!response.ok) {
		    throw new Error(`HTTP error! status: ${response.status}`);
		  }
		  return response.json();
		})
		.then(data => {
		  messagesTable.innerHTML = '';

		  data.forEach(message => {
		    const row = messagesTable.insertRow();
		    const createdAtCell = row.insertCell();
		    const contentCell = row.insertCell();

		    createdAtCell.textContent = message.createdAt;
		    contentCell.textContent = message.content;
		  });
		})
		.catch(error => {
		  console.error('Error fetching messages:', error);
		  alert('メッセージの取得に失敗しました。');
		});
		
		const websocket = new WebSocket('ws://localhost:8080/hc-websocket?1');
		socketRef.current = websocket;

		const onMessage = (message) => {
			  showMessage(JSON.parse(message.data));
		};
		
		websocket.addEventListener('message', onMessage);
		
		return () => {
		  websocket.close()
		  websocket.removeEventListener('message', onMessage);
		}
	}, []);
	
	const showMessage = (message) => {
		$("#messages").append("<tr><td>"+ message.createdAt + "</td><td>" + message.content + "</td></tr>");
	}
	
	const sendName = () => {
		const msg = {
		  content: contentRef.current.value,
		  channelId: $("#jsiChannelId").val()
		};
		
		socketRef.current.send(JSON.stringify(msg));
		contentRef.current.value = "";
	}
	
	return (
		<React.Fragment>
			<div className="row mb-3">
			    <div className="col">
			        <input type="text" ref={contentRef} className="form-control" />
			    </div>
				<div className="col">
					<button className="btn btn-dark" onClick={sendName}>Send</button>
				</div>
			</div>
		</React.Fragment>
	)
}

const container = document.getElementById('app');
const root = ReactDOM.createRoot(container);
root.render(<App />);