
// https://ja.legacy.reactjs.org/docs/components-and-props.html

const App = () => {
	
	const socketRef = React.useRef();
	const contentRef = React.useRef(null);
	const channelIdRef = React.useRef();

	React.useEffect(() => {

		const messagesTable = document.getElementById('jsiConversation').getElementsByTagName('tbody')[0];

		const urlParams = new URLSearchParams(window.location.search);
		let channelId = urlParams.get('channelId');
		
		if (channelId === null) {
		  channelId = 1;
		}
		
		channelIdRef.current = channelId;

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
			showMessage(message);
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
		
		const messagesTable = document.getElementById('jsiConversation').getElementsByTagName('tbody')[0];
		const row = messagesTable.insertRow();
		const createdAtCell = row.insertCell();
		const contentCell = row.insertCell();

		createdAtCell.textContent = message.createdAt;
		contentCell.textContent = message.content;
	}
	
	const sendName = () => {
		const msg = {
		  content: contentRef.current.value,
		  channelId: channelIdRef.current
		};
		
		socketRef.current.send(JSON.stringify(msg));
		contentRef.current.value = "";
	}
	
	return (
		<React.Fragment>
			<div className="container">
				<div className="row">
				    <div className="col">
				        <table id="jsiConversation" className="table table-striped" >
				            <thead>
				            <tr>
				                <th>Time</th>
								<th>Message</th>
				            </tr>
				            </thead>
				            <tbody id="messages">
				            </tbody>
				        </table>
				    </div>
				</div>
				<hr/>
				<div className="row mb-3">
				    <div className="col">
				        <input type="text" ref={contentRef} className="form-control" />
				    </div>
					<div className="col">
						<button className="btn btn-dark" onClick={sendName}>Send</button>
					</div>
				</div>
			</div>
		</React.Fragment>
	)
}

const container = document.getElementById('app');
const root = ReactDOM.createRoot(container);
root.render(<App />);