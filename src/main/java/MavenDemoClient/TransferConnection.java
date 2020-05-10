package MavenDemoClient;

import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

import MavenDemoClient.TcpClient.TransferConnectionCallback;

public class TransferConnection {

	public static final String TAG = TransferConnection.class.getSimpleName() + " : %s\n";
	
	private TransferConnectionCallback mTransferConnectionCallback = null;
	private TcpClient mTcpClient = null;
	private JSONObject mTransferServerInformation = null;
	private TransferClient mFromTransferClient = null;
	private TransferClient mToTransferClient = null;
	private ExecutorService mExecutorService = null;
	public TransferClient mRequestTransferClient = null;
	public TransferClient mResponseTransferClient = null;
	
	public TransferConnection(ExecutorService executor, TcpClient client, JSONObject object) {
		mTcpClient = client;
		mTransferServerInformation = object;
		mExecutorService = executor;
	}
	
	public void setTransferConnectionCallback(TransferConnectionCallback transferConnectionCallback) {
		mTransferConnectionCallback = transferConnectionCallback;
	}
	
	public void startConnect() {
		if (mFromTransferClient == null) {
			mFromTransferClient = new TransferClient(mExecutorService, this, mTransferServerInformation);
			mFromTransferClient.setClientRole(TransferClient.ROLE_REQUEST);
		}
		mFromTransferClient.connectToServer();
	}
	
	public void startConnetToResponseServer() {
		if (mToTransferClient == null) {
			mToTransferClient = new TransferClient(mExecutorService, this, mTransferServerInformation);
			mToTransferClient.setClientRole(TransferClient.ROLE_REPONSE);
		}
		mToTransferClient.connectToServer();
	}
	
	public void stopConnect() {
		if (mFromTransferClient != null) {
			mFromTransferClient.disconnectToServer();
		}
		if (mToTransferClient != null) {
			mToTransferClient.disconnectToServer();
		}
	}

	public TransferClient getFromTransferClient() {
		return mFromTransferClient;
	}
	
	public TransferClient getToTransferClient() {
		return mToTransferClient;
	}
	
	public JSONObject getTransferServerInformation() {
		return mTransferServerInformation;
	}
	
	public String getOrinalRequestNatAddress() {
		String result = null;
		//request client in and tell response client to start connect to transfer server to transfer request
		/*
		{
				"command":"start_connect_transfer",
				"server_info":
					{
						"connected_transfer_server_address":"www.opendiylib.com",
						"connected_transfer_server_port":19920,
						"request_client_nat_address":"58.246.136.202",
						"request_client_nat_port":50000,
						"bonded_response_server_address","192.168.188.150"
						"bonded_response_server_port":19920
					}
			} 
		*/
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getString("request_client_nat_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getOrinalRequestNatAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getOrinalRequestNatPort() {
		int result = -1;
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getInt("request_client_nat_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getOrinalRequestNatPort Exception = " + e.getMessage());
		}
		return result;
	}
	
	public String getTransferServerAddress() {
		String result = null;
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getString("connected_transfer_server_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getTransferServerAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getTransferServerPort() {
		int result = -1;
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getInt("connected_transfer_server_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getTransferServerPort Exception = " + e.getMessage());
		}
		return result;
	}
	
	public String getResponseServerAddress() {
		String result = null;
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getString("bonded_response_server_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getResponseServerAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getResponseServerPort() {
		int result = -1;
		try {
			if (mTransferServerInformation != null) {
				result = mTransferServerInformation.getInt("bonded_response_server_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getResponseServerPort Exception = " + e.getMessage());
		}
		return result;
	}
}
