package MavenDemoClient;

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
	private BufferedReader mSocketReader = null;
	private BufferedWriter mSocketWriter = null;
	private JSONObject mClientInfomation = null;//add mac address as name
	private List<TransferConnection> mTransferConnections = Collections.synchronizedList(new ArrayList<TransferConnection>());
	
	private TransferConnectionCallback mTransferConnectionCallback = new TransferConnectionCallback() {

		@Override
		public void onTransferConnectionConnect(TransferConnection connection, JSONObject data) {
			// TODO Auto-generated method stub
			addTransferConnection(connection);
			if (data != null && data.length() > 0) {
				data.put("action", "connection_started");
				sendMessage(data.toString());
			}
		}

		@Override
		public void onTransferConnectionDisconnect(TransferConnection connection, JSONObject data) {
			// TODO Auto-generated method stub
			removeTransferConnection(connection);
			if (data != null && data.length() > 0) {
				data.put("action", "connection_stopped");
				sendMessage(data.toString());
			}
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
				mSocketReader = new BufferedReader(new InputStreamReader(mInputStream, Charset.forName("UTF-8")));
				mSocketWriter = new BufferedWriter(new OutputStreamWriter(mOutputStream));
				//send client info to fixed request command server
				JSONObject info = new JSONObject();
				//add command
				info.put("command", "information");
				//add client information
				info.put("information", mClientInfomation);
				Log.PrintLog(TAG, "mReceiveRunnable " + info.toString());
				sendMessage(info.toString());
				String inMsg = null;
				String outMsg = null;
				while (mIsRunning) {
					try {
					    while ((inMsg = mSocketReader.readLine()) != null) {
					    	Log.PrintLog(TAG, "Received from  server: " + inMsg);
					    	outMsg = dealCommand(inMsg);
					    	sendMessage(outMsg);
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
		//request a new transfer server
		//{"command":"start_new_transfer_server","server_info":{"new_transfer_server_address":"opendiylib.com","new_transfer_server_port":19909,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":19911}}
		if (resuest != null && resuest.length() > 0) {
			sendMessage(resuest.toString());
		}
	}
	
	public void stopRequestNewTransferConnection(JSONObject resuest) {
		//request a new transfer server
		//{"command":"stop_transfer_server","server_info":{"new_transfer_server_address":"opendiylib.com","new_transfer_server_port":19909,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":19911}}
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
	
	public String getRequestClientInetAddress() {
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
	}
	
	private void initSocketInformation() {
		if (mClientMacAddress == null) {
			mClientMacAddress = getLocalMacAddress();
		}
		//connect to fixed server
		//{"command":"information","information":{"name":"request_tranfer_client","mac_address":"10-7B-44-15-2D-B6","dhcp_address":"192.168.188.150","dhcp_port":19909,"fixed_server_address":"opendiylib.com","fixed_server_port":19910}}
		if (mClientInfomation == null) {
			JSONObject info = new JSONObject();
			info.put("name", "request_tranfer_client");
			info.put("mac_address", mClientMacAddress);
			info.put("dhcp_address", getLocalInetAddress());
			info.put("dhcp_port", getLocalPort());
			info.put("fixed_server_address", MainDemoClient.FIXED_HOST);
			info.put("fixed_server_port", MainDemoClient.FIXED_PORT);
			mClientInfomation = info;
		}
		printClientInfo();
	}
	
	private TransferConnection getTransferConnection(JSONObject object) {
		TransferConnection result = null;
		Iterator<TransferConnection> iterator = mTransferConnections.iterator();
		TransferConnection transferConnection = null;
		while (iterator.hasNext()) {
			transferConnection = (TransferConnection)iterator.next();
			if (object != null && object.equals(transferConnection.getRequestInformation())) {
				result = transferConnection;
				break;
			}
		}
		return result;
	}
	
	private void sendMessage(String outMsg) {
		try {
			if (mSocketWriter != null) {
				mSocketWriter.write(outMsg);
		    	//mSocketWriter.write("\n");
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
					case "start_connect":
						result = parseConnetNewServer(obj);
						break;
					case "status":
						result = parseStatus(obj);
						break;
					default:
						break;
				}
			}
		}
		return result;
	}
	
	private String parseConnetNewServer(JSONObject data) {
		String result = "unknown";
		JSONObject request_client_info = null;
		String server_address = null;
		int server_port = -1;
		String request_client_nat_address = null;
		int request_client_nat_port = -1;
		if (data != null && data.length() > 0) {
			//request client in and tell response client to start connect to transfer server to transfer request
			//{"command":"start_connect","request_client_info":{"request_client_nat_address":"114.82.25.165","request_client_nat_port":50000,"connected_transfer_server_address":"opendiylib.com","connected_transfer_server_port":19911,"bonded_response_server_address":"192.168.188.150","bonded_response_server_port":3389}
			try {
				request_client_info = data.getJSONObject("request_client_info");
			} catch (Exception e) {
				return result;
			}
			try {
				server_address = data.getString("connected_transfer_server_address");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStartNewServer serverObj getString connected_transfer_server_address Exception = " + e.getMessage());
			}
			try {
				server_port = data.getInt("connected_transfer_server_port");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStartNewServer serverObj getInt connected_transfer_server_port Exception = " + e.getMessage());
			}
			try {
				request_client_nat_address = data.getString("request_client_nat_address");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStartNewServer getString request_client_nat_address Exception = " + e.getMessage());
			}
			try {
				request_client_nat_port = data.getInt("request_client_nat_port");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStartNewServer serverObj getInt request_client_nat_port Exception = " + e.getMessage());
			}
			if (server_address != null && server_address.length() > 0 && server_port != -1 && request_client_nat_address != null && request_client_nat_address.length() > 0 && request_client_nat_port != -1) {
				TransferConnection getTransferConnection = getTransferConnection(request_client_info);
				if (getTransferConnection == null) {
					getTransferConnection = new TransferConnection(mExecutorService, TcpClient.this, request_client_info);
					getTransferConnection.setTransferConnectionCallback(mTransferConnectionCallback);
					getTransferConnection.startConnect();
					result = "parseConnetNewServer_" + server_address + ":" + server_port + "_" + request_client_nat_address + ":" + request_client_nat_port + "_ok";
				} else {
					result = "parseConnetNewServer_" + server_address + ":" + server_port + "_" + request_client_nat_port + ":" + request_client_nat_port + "_existed_ok";
				}
			}
		}
		return result;
	}
	
	private String parseStatus(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			try {
				result = "parseStatus_" + mClientInfomation.getString("status") + "_ok";
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus getString status Exception = " + e.getMessage());
			}
		}
		return result;
	}
	
	public interface TransferConnectionCallback {
		void onTransferConnectionConnect(TransferConnection connection, JSONObject data);
		void onTransferConnectionDisconnect(TransferConnection connection, JSONObject data);
	}
}
