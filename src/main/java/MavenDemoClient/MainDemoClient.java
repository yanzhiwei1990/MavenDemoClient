package MavenDemoClient;

import org.json.JSONObject;

public class MainDemoClient {

	public static final String TAG = MainDemoClient.class.getSimpleName() + " : %s\n";
	public static final String FIXED_HOST = "opendiylib.com";
	public static final int FIXED_PORT = 19910;
	
	private TcpClient mTcpClient = null;
	private String mServerAddress = null;
	private int mServerPort = -1;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MainDemoClient mMainDemo = new MainDemoClient(FIXED_HOST, FIXED_PORT);
		mMainDemo.startRun();
	}
	
	public MainDemoClient(String fixedServer, int fixedPort) {
		mServerAddress = fixedServer;
		mServerPort = fixedPort;
	}
	
	private void initShutDownWork() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	Log.PrintLog(TAG, "doShutDownWork");
		    	stopRun();
		    }  
		});
	}
	
	public void startRun() {
		initShutDownWork();
		mTcpClient = new TcpClient(mServerAddress, mServerPort);
		mTcpClient.connectToServer();
	}

	public void stopRun() {
		if (mTcpClient != null) {
			mTcpClient.disconnectToServer();
		}
	}
	
	
	public void requestNewTransfer(JSONObject resuest) {
		//request a new transfer server
		//{"command":"start_new_transfer_server","server_info":{"new_transfer_server_address":"opendiylib.com","new_transfer_server_port":19909,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":19911}}
		if (mTcpClient != null) {
			mTcpClient.requestNewTransferConnection(resuest);
		}
	}
	
	public void stopRequestedTransfer(JSONObject resuest) {
		//request a new transfer server
		//{"command":"stop_transfer_server","server_info":{"new_transfer_server_address":"opendiylib.com","new_transfer_server_port":19909,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":19911}}
		if (mTcpClient != null) {
			mTcpClient.stopRequestNewTransferConnection(resuest);
		}
	}
}
