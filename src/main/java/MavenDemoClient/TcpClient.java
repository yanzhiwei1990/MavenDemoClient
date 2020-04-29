package MavenDemoClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class TcpClient {

	public static final String TAG = TcpClient.class.getSimpleName() + " : %s\n";
	private static final int MAX_THREAD = 20;
	
	private String mServerAddress = null;
	private int mServerPort = -1;
	private ExecutorService mExecutorService = null;
	private Socket mClientSocket = null;
	private String mClientMacAddress = null;
	private boolean mIsRunning = false;
	private InputStream mInputStream = null;
	private OutputStream mOutputStream = null;
	private BufferedInputStream mSocketReader = null;
	private BufferedOutputStream mSocketWriter = null;
	private JSONObject mClientInfomation = null;//add mac address as name
	private List<TransferConnection> mTransferConnections = Collections.synchronizedList(new ArrayList<TransferConnection>());
	
	private TransferConnectionCallback mTransferConnectionCallback = new TransferConnectionCallback() {

		@Override
		public void onTransferConnectionConnect(TransferConnection connection, JSONObject data) {
			// TODO Auto-generated method stub
			addTransferConnection(connection);
			/*if (data != null && data.length() > 0) {
				data.put("action", "connection_started");
				sendMessage(data.toString());
			}*/
		}

		@Override
		public void onTransferConnectionDisconnect(TransferConnection connection, JSONObject data) {
			// TODO Auto-generated method stub
			removeTransferConnection(connection);
			/*if (data != null && data.length() > 0) {
				data.put("action", "connection_stopped");
				sendMessage(data.toString());
			}*/
		}
	};
	
	private Runnable mReceiveRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.PrintLog(TAG, "start receive");
			try {
				mInputStream = mClientSocket.getInputStream();
			} catch (IOException e) {
				Log.PrintError(TAG, "getInputStream Exception = " + e.getMessage());
			}
			try {
				mOutputStream = mClientSocket.getOutputStream();
			} catch (IOException e) {
				Log.PrintError(TAG, "getOutputStream Exception = " + e.getMessage());
			}
			if (mInputStream != null && mOutputStream != null) {
				//mSocketReader = new BufferedReader(new InputStreamReader(mInputStream, Charset.forName("UTF-8")));
				//mSocketWriter = new BufferedWriter(new OutputStreamWriter(mOutputStream));
				byte[] buffer = new byte[1024 * 1024];
				int length = -1;
				mSocketReader = new BufferedInputStream(mInputStream, buffer.length);
				mSocketWriter = new BufferedOutputStream(mOutputStream, buffer.length);
				//send client info to fixed request command server
				sendClientInfomation();
				String inMsg = null;
				String outMsg = null;
				JSONObject result = null;
				while (mIsRunning) {
					try {
					    while ((length = mSocketReader.read(buffer, 0, buffer.length)) != -1) {
				    		inMsg = new String(buffer, 0, length, Charset.forName("UTF-8")).trim();
					    	Log.PrintLog(TAG, "Received from client: " + inMsg);
					    	outMsg = dealCommand(inMsg);
					    	if (!"no_need_feedback".equals(outMsg)) {
						    	sendMessage(outMsg);
					    	}
					    	Log.PrintLog(TAG, "Received client deal: " + outMsg);
					    }
					    Log.PrintLog(TAG, "client disconnect");
					   
					} catch(Exception e) {
						Log.PrintError(TAG, "receive Exception = " + e.getMessage());
						break;
					}
					break;
				}
			} else {
				Log.PrintError(TAG, "get stream error");
			}
			Log.PrintLog(TAG, "end receive");
			dealClearWork();
		}
	};
	
	public TcpClient(String serverAddress, int serverPort) {
		mServerAddress = serverAddress;
		mServerPort = serverPort;
		mExecutorService = Executors.newFixedThreadPool(MAX_THREAD);
	}
	
	public void connectToServer() {
		try {
			mClientSocket = new Socket(mServerAddress, mServerPort);
			mIsRunning = true;
			initSocketInformation();
			mExecutorService.submit(mReceiveRunnable);
		} catch (IOException e) {
			Log.PrintError(TAG, "connectToServer IOException = " + e.getMessage());
		}
	}
	
	public void disconnectToServer() {
		mIsRunning = false;
		closeSocket();
	}
	
	public void requestNewTransferConnection(JSONObject resuest) {
		if (resuest != null && resuest.length() > 0) {
			sendMessage(resuest.toString());
		}
	}
	
	public void stopRequestNewTransferConnection(JSONObject resuest) {
		if (resuest != null && resuest.length() > 0) {
			sendMessage(resuest.toString());
		}
	}
	
	public InputStream getClientInputStream() {
		return mInputStream;
	}
	
	public OutputStream getClientOutputStream() {
		return mOutputStream;
	}
	
	public JSONObject getClientInformation() {
		return mClientInfomation;
	}
	
	public String getRemoteInetAddress() {
		return mClientSocket.getInetAddress().getHostAddress();
	}
	
	public int getRemotePort() {
		return mClientSocket.getPort();
	}
	
	public String getLocalInetAddress() {
		return mClientSocket.getLocalAddress().getHostAddress();
	}
	
	public int getLocalPort() {
		return mClientSocket.getLocalPort();
	}
	
	public String getLocalMacAddress() {
		String result = null;
		if (mClientMacAddress != null && mClientMacAddress.length() > 0) {
			result = mClientMacAddress;
			return result;
		}
		InetAddress inetAddress = null;
		byte[] macAddress = null;
		NetworkInterface networkInterface = null;
		String localAddress = getLocalInetAddress();
		try {
			inetAddress = InetAddress.getByName(localAddress);
			Log.PrintLog(TAG, "getLocalMacAddress inetAddress = " + inetAddress.getHostAddress());
		} catch (Exception e) {
			Log.PrintError(TAG, "getLocalMacAddress inetAddress Exception = " + e.getMessage());
			return result;
		}
		try {
			networkInterface = NetworkInterface.getByInetAddress(inetAddress);
			Log.PrintLog(TAG, "getLocalMacAddress networkInterface = " + networkInterface.getDisplayName());
		} catch (Exception e) {
			Log.PrintError(TAG, "getLocalMacAddress networkInterface Exception = " + e.getMessage());
			return result;
		}
		try {
			macAddress = networkInterface.getHardwareAddress();
			Log.PrintLog(TAG, "getLocalMacAddress macAddress = " + Arrays.toString(macAddress));
		} catch (Exception e) {
			Log.PrintError(TAG, "getLocalMacAddress macAddress Exception = " + e.getMessage());
			return result;
		}
		if (macAddress != null && macAddress.length > 0) {
			StringBuilder sb = new StringBuilder();
		    for (int i = 0; i < macAddress.length; i++) {
		        if (i != 0) {
		          sb.append("-");
		        }
		        String s = Integer.toHexString(macAddress[i] & 0xFF);
		        sb.append(s.length() == 1 ? 0 + s : s);
		    }
		    if (sb != null && sb.length() > 0) {
		    	result = sb.toString().toUpperCase();
		    }
		}
		Log.PrintLog(TAG, "getLocalMacAddress result = " + result);
	    return result;
	}
	
	/*public String getRequestClientInetAddress() {
		String result = null;
		try {
			result = mClientInfomation.getString("request_client_address");
		} catch (Exception e) {
			//Log.PrintError(TAG, "getRequestClientInetAddress Exception = " + e.getMessage());
		}
		return result;
	}
	
	public int getRequestClientPort() {
		int result = -1;
		try {
			result = mClientInfomation.getInt("request_client_port");
		} catch (Exception e) {
			//Log.PrintError(TAG, "getRequestClientPort Exception = " + e.getMessage());
		}
		return result;
	}*/
	
	private void initSocketInformation() {
		if (mClientMacAddress == null) {
			mClientMacAddress = getLocalMacAddress();
		}
		//connect to fixed server
		/*
		{
			"command":"information",
			"information":
				{
					"name":"response_fixed_request_tranfer_client",
					"mac_address","10-7B-44-15-2D-B6",
					"dhcp_address","192.168.188.150",
					"dhcp_port":5555,
					"fixed_server_address":"opendiylib.com",
					"fixed_server_port":19910,
					"response_fixed_client_nat_address":"",
					"response_fixed_client_nat_port":-1
				}
		}
		*/
		if (mClientInfomation == null) {
			JSONObject info = new JSONObject();
			info.put("name", "response_fixed_request_tranfer_client");
			info.put("mac_address", mClientMacAddress);
			info.put("dhcp_address", getLocalInetAddress());
			info.put("dhcp_port", getLocalPort());
			info.put("fixed_server_address", MainDemoClient.FIXED_HOST);
			info.put("fixed_server_port", MainDemoClient.FIXED_PORT);
			info.put("response_fixed_client_nat_address", "");
			info.put("response_fixed_client_nat_port", -1);
			mClientInfomation = info;
		}
		printClientInfo();
	}
	
	private void sendClientInfomation() {
		JSONObject info = new JSONObject();
		info.put("command", "information");
		info.put("information", mClientInfomation);
		sendMessage(info.toString());
	}
	
	private TransferConnection getTransferConnection(JSONObject object) {
		TransferConnection result = null;
		Iterator<TransferConnection> iterator = mTransferConnections.iterator();
		TransferConnection transferConnection = null;
		while (iterator.hasNext()) {
			transferConnection = (TransferConnection)iterator.next();
			if (object != null && object.equals(transferConnection.getTransferServerInformation())) {
				result = transferConnection;
				break;
			}
		}
		return result;
	}
	
	private void sendMessage(String outMsg) {
		try {
			if (mSocketWriter != null && outMsg != null && outMsg.length() > 0) {
				byte[] send = outMsg.getBytes(Charset.forName("UTF-8"));
				mSocketWriter.write(send, 0, send.length);
		    	mSocketWriter.flush();
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "sendMessage Exception = " + e.getMessage());
		}
	}
	
	private void printClientInfo() {
		if (mClientInfomation != null && mClientInfomation.length() > 0) {
			Log.PrintLog(TAG, "printClientInfo:" + mClientInfomation);
		}
	}
	
	private void dealClearWork() {
		Log.PrintLog(TAG, "closeStream mIsRunning = " + mIsRunning);
		if (mIsRunning) {
			closeSocket();
			closeStream();
			mIsRunning = false;
		} else {
			closeStream();
		}
		mExecutorService.shutdown();
	}
	
	private void closeStream() {
		Log.PrintLog(TAG, "closeStream");
		closeBufferedWriter();
		closeOutputStream();
		closeBufferedReader();
		closeInputStream();
	}
	
	private void closeInputStream() {
		try {
			if (mInputStream != null) {
				mInputStream.close();
				mInputStream = null;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "closeInputStream Exception = " + e.getMessage());
			mInputStream = null;
		}
	}
	
	private void closeBufferedReader() {
		try {
			if (mSocketReader != null) {
				mSocketReader.close();
				mSocketReader = null;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "closeBufferedReader Exception = " + e.getMessage());
			mSocketReader = null;
		}
	}
	
	private void closeOutputStream() {
		try {
			if (mOutputStream != null) {
				mOutputStream.close();
				mOutputStream = null;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "closeOutputStream Exception = " + e.getMessage());
			mOutputStream = null;
		}
	}
	
	private void closeBufferedWriter() {
		try {
			if (mSocketWriter != null) {
				mSocketWriter.close();
				mSocketWriter = null;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "closeBufferedWriter Exception = " + e.getMessage());
			mSocketWriter = null;
		}
	}
	
	private void closeSocket() {
		try {
			if (mClientSocket != null) {
				mClientSocket.close();
				mClientSocket = null;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "closeSocket Exception = " + e.getMessage());
			mClientSocket = null;
		}
	}
	
	private void addTransferConnection(TransferConnection connection) {
		mTransferConnections.add(connection);
	}
	
	private void removeTransferConnection(TransferConnection connection) {
		mTransferConnections.remove(connection);
	}
	
	private String dealCommand(String data) {
		String result = "unknown";
		String command = null;
		JSONObject obj = null;
		if (data != null) {
			try {
				obj = new JSONObject(data);
			} catch (Exception e) {
				Log.PrintError(TAG, "dealCommand new JSONObject Exception = " + e.getMessage());
			}
			if (obj != null && obj.length() > 0) {
				try {
					command = obj.getString("command");
				} catch (Exception e) {
					Log.PrintError(TAG, "dealCommand getString command Exception = " + e.getMessage());
				}
				switch (command) {
					case "start_connect_transfer":
						result = parseConnetToTransferServer(obj);
						break;
					case "status":
						result = parseStatus(obj);
						break;
					case "result":
						result = parseResult(obj);
						break;
					default:
						break;
				}
			}
		}
		return result;
	}
	
	private String parseConnetToTransferServer(JSONObject data) {
		String result = "unknown";
		JSONObject server_info = null;
		String server_address = null;
		int server_port = -1;
		String request_client_nat_address = null;
		int request_client_nat_port = -1;
		if (data != null && data.length() > 0) {
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
				server_info = data.getJSONObject("server_info");
			} catch (Exception e) {
				return result;
			}
			try {
				server_address = server_info.getString("connected_transfer_server_address");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseConnetToTransferServer serverObj getString connected_transfer_server_address Exception = " + e.getMessage());
			}
			try {
				server_port = server_info.getInt("connected_transfer_server_port");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseConnetToTransferServer serverObj getInt connected_transfer_server_port Exception = " + e.getMessage());
			}
			try {
				request_client_nat_address = server_info.getString("request_client_nat_address");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseConnetToTransferServer getString request_client_nat_address Exception = " + e.getMessage());
			}
			try {
				request_client_nat_port = server_info.getInt("request_client_nat_port");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseConnetToTransferServer serverObj getInt request_client_nat_port Exception = " + e.getMessage());
			}
			if (server_address != null && server_address.length() > 0 && server_port != -1 && request_client_nat_address != null && request_client_nat_address.length() > 0 && request_client_nat_port != -1) {
				TransferConnection getTransferConnection = getTransferConnection(server_info);
				if (getTransferConnection == null) {
					getTransferConnection = new TransferConnection(mExecutorService, TcpClient.this, server_info);
					getTransferConnection.setTransferConnectionCallback(mTransferConnectionCallback);
					getTransferConnection.startConnect();
				}
			}
		}
		result = "no_need_feedback";
		return result;
	}
	
	/*private String parseStatus(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			try {
				result = "parseStatus_" + data.getString("status") + "_ok";
				//request a new transfer server
				//{"command":"start_new_transfer_server","server_info":{"new_transfer_server_address":"opendiylib.com","new_transfer_server_port":19909,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":19911}}
				
				if (data.getString("status").equals("parseInformation_" + mClientInfomation.getString("name") + "_" + mClientInfomation.getString("mac_address") + "_ok")) {
					JSONObject resquest = new JSONObject();
					resquest.put("command", "start_new_transfer_server");
					JSONObject server_info = new JSONObject();
					server_info.put("new_transfer_server_address", "0.0.0.0");
					server_info.put("new_transfer_server_port", 19920);
					server_info.put("bonded_response_server_address", getLocalInetAddress());
					server_info.put("bonded_response_server_port", 19920);
					resquest.put("server_info", server_info);
					requestNewTransferConnection(resquest);
				}
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus getString status Exception = " + e.getMessage());
			}
		}
		return result;
	}
	
	private String parseResult(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			try {
				result = "parse_result_ok";
				Log.PrintLog(TAG, "parseResult " + data);
			} catch (Exception e) {
				Log.PrintError(TAG, "parseResult getString status Exception = " + e.getMessage());
			}
		}
		return result;
	}*/
	
	private String parseStatus(JSONObject data) {
		String result = "unknown";
		JSONObject status = null;
		if (data != null && data.length() > 0) {
			try {
				status = data.getJSONObject("status");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus getString status Exception = " + e.getMessage());
				return result;
			}
			try {
				result = "no_need_feedback";
				Log.PrintLog(TAG, "parseStatus " + status);
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus deal status Exception = " + e.getMessage());
			}
		}
		return result;
	}
	
	private String parseResult(JSONObject data) {
		String result = "unknown";
		JSONObject resultJson = null;
		if (data != null && data.length() > 0) {
			/*
			{
				"command":"result",
				"result":
					{
						"status":"connected_to_fixed_server",
						"information":
							{
								"name":"response_fixed_request_tranfer_client",
								"mac_address","10-7B-44-15-2D-B6",
								"dhcp_address","192.168.188.150",
								"dhcp_port":5555,
								"fixed_server_address":"opendiylib.com",
								"fixed_server_port":19910,
								"response_fixed_client_nat_address":"58.246.136.202",
								"response_fixed_client_nat_port":50000
							}
					}
				}
					
			}
			*/
			try {
				resultJson = data.getJSONObject("result");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseResult getString result Exception = " + e.getMessage());
				return result;
			}
			String returnStatus = null;
			try {
				returnStatus = resultJson.getString("status");
			} catch (Exception e) {
				return result;
			}
			switch (returnStatus) {
				case "connected_to_fixed_server":
					JSONObject returnInfo = null;
					try {
						returnInfo = resultJson.getJSONObject("information");
					} catch (Exception e) {
						Log.PrintError(TAG, "parseResult getString information Exception = " + e.getMessage());
					}
					if (hasSameOriginalInformation(returnInfo, mClientInfomation)) {
						String returnResponseFixedClientNatAddress= tryToGetString(returnInfo, "response_fixed_client_nat_address");
						int returnResponseFixedClientNatPort = tryToGetInt(returnInfo, "response_fixed_client_nat_port");
						//update nat address
						mClientInfomation.put("response_fixed_client_nat_address", returnResponseFixedClientNatAddress);
						mClientInfomation.put("response_fixed_client_nat_port", returnResponseFixedClientNatPort);
						testRequestTransferServer();
						Log.PrintLog(TAG, "parseResult connected_to_fixed_server and update client info");
					}
					break;
				case "new_transfer_server_started":
					Log.PrintLog(TAG, "parseResult new_transfer_server_started");
					break;
				case "transfer_server_existed":
					Log.PrintLog(TAG, "parseResult transfer_server_existed");
					break;
				case "transfer_server_stopped":
					Log.PrintLog(TAG, "parseResult transfer_server_stopped");
					break;
				case "transfer_server_not_found":
					Log.PrintLog(TAG, "parseResult transfer_server_not_found");
					break;
				default:
					break;
			}
			try {
				result = "no_need_feedback";
				Log.PrintLog(TAG, "parseResult " + resultJson);
			} catch (Exception e) {
				Log.PrintError(TAG, "parseResult deal result Exception = " + e.getMessage());
			}
		}
		return result;
	}
	
	private boolean hasSameOriginalInformation(JSONObject returnInfo, JSONObject originalInfo) {
		boolean result = false;
		if (returnInfo != null && originalInfo != null && returnInfo.length() > 0 && originalInfo.length() > 0) {
			String returnName = tryToGetString(returnInfo, "name");
			String originalName = tryToGetString(originalInfo, "name");
			result = stringEqual(returnName, originalName);
			
			String returnMacAddress= tryToGetString(returnInfo, "mac_address");
			String originalMacAddress = tryToGetString(originalInfo, "mac_address");
			result = result && stringEqual(returnMacAddress, originalMacAddress);
			
			String returnDhcpAddress= tryToGetString(returnInfo, "dhcp_address");
			String originalDhcpAddress = tryToGetString(originalInfo, "dhcp_address");
			int returnDhcpPort= tryToGetInt(returnInfo, "dhcp_port");
			int originalDhcpPort = tryToGetInt(originalInfo, "dhcp_port");
			result = result && stringEqual(returnDhcpAddress, originalDhcpAddress) && returnDhcpPort == originalDhcpPort;
			
			String returnFixedServerAddress = tryToGetString(returnInfo, "fixed_server_address");
			String originalFixedServerAddress = tryToGetString(originalInfo, "fixed_server_address");
			int returnFixedServerPort= tryToGetInt(returnInfo, "fixed_server_port");
			int originalFixedServerPort = tryToGetInt(originalInfo, "fixed_server_port");
			result = result && stringEqual(returnFixedServerAddress, originalFixedServerAddress) && returnFixedServerPort == originalFixedServerPort;
			
			String returnResponseFixedClientNatAddress= tryToGetString(returnInfo, "response_fixed_client_nat_address");
			//String originalResponseFixedClientNatAddress = tryToGetString(originalInfo, "response_fixed_client_nat_address");
			int returnResponseFixedClientNatPort = tryToGetInt(returnInfo, "response_fixed_client_nat_port");
			//int originalResponseFixedClientNatPort = tryToGetInt(originalInfo, "response_fixed_client_nat_port");*/
			result = result && returnResponseFixedClientNatAddress != null && returnResponseFixedClientNatAddress.length() > 0 && returnResponseFixedClientNatPort != -1; 
		}
		return result;
	}
	
	private String tryToGetString(JSONObject obj, String key) {
		String result = null;
		try {
			if (obj != null && obj.length() > 0) {
				result = obj.getString(key);
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "tryToGetString getString " + key + ", Exception " + e.getMessage());
		}
		return result;
	}
	
	private int tryToGetInt(JSONObject obj, String key) {
		int result = -1;
		try {
			if (obj != null && obj.length() > 0) {
				result = obj.getInt(key);
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "tryToGetInt getInt " + key + ", Exception " + e.getMessage());
		}
		return result;
	}
	
	private boolean stringEqual(String value1, String value2) {
		boolean result = false;
		if (value1 != null && value2 != null && value1.equals(value2)) {
			result = true;
		}
		return result;
	}
	
	private void testRequestTransferServer() {
		requestStartTransferServer("0.0.0.0", 19920, getLocalInetAddress(), 19920);
	}
	
	private void requestStartTransferServer(String transferServerAddress, int transferServerPort, String responseServerAddress, int responseServerPort) {
		/*
		{
			"command":"start_new_transfer_server",
			"server_info":
				{
					"new_transfer_server_address":"0.0.0.0",
					"new_transfer_server_port":19920,
					"bonded_response_server_address","192.168.188.150"
					"bonded_response_server_port":19920
				}
		}
		*/
		JSONObject resquest = new JSONObject();
		resquest.put("command", "start_new_transfer_server");
		JSONObject server_info = new JSONObject();
		server_info.put("new_transfer_server_address", transferServerAddress);
		server_info.put("new_transfer_server_port", transferServerPort);
		server_info.put("bonded_response_server_address", responseServerAddress);
		server_info.put("bonded_response_server_port", responseServerPort);
		resquest.put("server_info", server_info);
		requestNewTransferConnection(resquest);
	}
	
	private void testStopTransferServer() {
		requestStartTransferServer("0.0.0.0", 19920, getLocalInetAddress(), 19920);
	}
	
	private void requestStopTransferServer(String transferServerAddress, int transferServerPort, String responseServerAddress, int responseServerPort) {
		/*
		{
			"command":"stop_transfer_server",
			"server_info":
				{
					"new_transfer_server_address":"0.0.0.0",
					"new_transfer_server_port":19920,
					"bonded_response_server_address","192.168.188.150"
					"bonded_response_server_port":19920
				}
		}
		*/
		JSONObject resquest = new JSONObject();
		resquest.put("command", "stop_transfer_server");
		JSONObject server_info = new JSONObject();
		server_info.put("new_transfer_server_address", transferServerAddress);
		server_info.put("new_transfer_server_port", transferServerPort);
		server_info.put("bonded_response_server_address", responseServerAddress);
		server_info.put("bonded_response_server_port", responseServerPort);
		resquest.put("server_info", server_info);
		requestNewTransferConnection(resquest);
	}
	
	public interface TransferConnectionCallback {
		void onTransferConnectionConnect(TransferConnection connection, JSONObject data);
		void onTransferConnectionDisconnect(TransferConnection connection, JSONObject data);
	}
}
