
// https://ja.legacy.reactjs.org/docs/components-and-props.html

const App = () => {
	
	const socketRef = React.useRef();
	const contentRef = React.useRef(null);
	
	React.useEffect(() => {
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