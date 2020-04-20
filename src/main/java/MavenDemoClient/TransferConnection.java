package MavenDemoClient;

import MavenDemoClient.TcpClient.TransferConnectionCallback;

public class TransferConnection {

	public static final String TAG = TransferConnection.class.getSimpleName() + " : %s\n";
	
	private TransferConnectionCallback mTransferConnectionCallback = null;
	private String mFromAddress = null;
	private int mFromPort = -1;
	private String mToAddress = null;
	private int mToPort = -1;
	
	public TransferConnection(String fromAddress, int fromPort, String toAddress, int toPort) {
		mFromAddress = fromAddress;
		mFromPort = fromPort;
		mToAddress = toAddress;
		mToPort = toPort;
	}
	
	public void setTransferConnectionCallback(TransferConnectionCallback transferConnectionCallback) {
		mTransferConnectionCallback = transferConnectionCallback;
	}
	
	public void startConnection() {
		
	}
	
	public void stopConnection() {
		
	}

	
}
