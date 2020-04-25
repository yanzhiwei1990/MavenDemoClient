package MavenDemoClient;

import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

import MavenDemoClient.TcpClient.TransferConnectionCallback;

public class TransferConnection {

	public static final String TAG = TransferConnection.class.getSimpleName() + " : %s\n";
	
	private TransferConnectionCallback mTransferConnectionCallback = null;
	private TcpClient mTcpClient = null;
	private JSONObject mRequestInformation = null;
	private TransferClient mFromTransferClient = null;
	private TransferClient mToTransferClient = null;
	private ExecutorService mExecutorService = null;
	
	public TransferConnection(ExecutorService executor, TcpClient client, JSONObject object) {
		mTcpClient = client;
		mRequestInformation = object;
		mExecutorService = executor;
	}
	
	public void setTransferConnectionCallback(TransferConnectionCallback transferConnectionCallback) {
		mTransferConnectionCallback = transferConnectionCallback;
	}
	
	public void startConnect() {
		if (mFromTransferClient == null) {
			mFromTransferClient = new TransferClient(mExecutorService, this, getFromAddress(), getFromPort());
			mFromTransferClient.setClientRole(TransferClient.ROLE_REQUEST);
		}
		mFromTransferClient.connectToServer();
	}
	
	public void startConnetToResponseServer() {
		if (mToTransferClient == null) {
			mToTransferClient = new TransferClient(mExecutorService, this, getToAddress(), getToPort());
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
	
	public JSONObject getRequestInformation() {
		return mRequestInformation;
	}
	
	public String getRequestNatAddress() {
		String result = null;
		//request client in and tell response client to start connect to transfer server to transfer request
		//{"command":"start_connect","request_client_info":{"request_client_nat_address":"114.82.25.165","request_client_nat_port":50000,"connected_transfer_server_address":"opendiylib.com","connected_transfer_server_port":19911,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":3389}
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getString("request_client_nat_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getRequestNatAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getRequestNatPort() {
		int result = -1;
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getInt("request_client_nat_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getRequestNatPort Exception = " + e.getMessage());
		}
		return result;
	}
	
	public String getFromAddress() {
		String result = null;
		//request client in and tell response client to start connect to transfer server to transfer request
		//{"command":"start_connect","request_client_info":{"request_client_nat_address":"114.82.25.165","request_client_nat_port":50000,"connected_transfer_server_address":"opendiylib.com","connected_transfer_server_port":19911,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":3389}
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getString("connected_transfer_server_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getFromAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getFromPort() {
		int result = -1;
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getInt("connected_transfer_server_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getFromPort Exception = " + e.getMessage());
		}
		return result;
	}
	
	public String getToAddress() {
		String result = null;
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getString("bonded_response_server_address");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getToAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getToPort() {
		int result = -1;
		try {
			if (mRequestInformation != null) {
				result = mRequestInformation.getInt("bonded_response_server_port");
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "getToPort Exception = " + e.getMessage());
		}
		return result;
	}
}
