package MavenDemoClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class TcpClient {

	public static final String TAG = TcpClient.class.getSimpleName() + " : %s\n";
	private static final int MAX_THREAD = 20;
	
	private String mFromAddress = null;
	private int mFromPort = -1;
	private String mToAddress = null;
	private int mToPort = -1;
	private ExecutorService mExecutorService = null;
	private Socket mClientSocket = null;
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
	
	public TcpClient(String fromAddress, int fromPort, String toAddress, int toPort) {
		mFromAddress = fromAddress;
		mFromPort = fromPort;
		mToAddress = toAddress;
		mToPort = toPort;
		mExecutorService = Executors.newFixedThreadPool(MAX_THREAD);
	}
	
	public void connectToServer() {
		try {
			mClientSocket = new Socket(mFromAddress, mFromPort);
			mIsRunning = true;
			printAddress();
			mExecutorService.submit(mReceiveRunnable);
		} catch (IOException e) {
			Log.PrintError(TAG, "connectToServer IOException = " + e.getMessage());
		}
	}
	
	public void disconnectToServer() {
		mIsRunning = false;
		closeSocket();
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
	
	private void sendMessage(String outMsg) {
		try {
			if (mSocketWriter != null) {
				mSocketWriter.write(outMsg);
		    	mSocketWriter.write("\n");
		    	mSocketWriter.flush();
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "sendMessage Exception = " + e.getMessage());
		}
	}
	
	private void printAddress() {
		if (mClientSocket != null) {
			Log.PrintLog(TAG, "server:" + getLocalInetAddress() + ":" + getLocalPort() +
					", client:" + getRemoteInetAddress() + ":" + getRemotePort());
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
					case "information":
						result = parseInformation(obj);
						break;
					case "connectnewserver":
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
	
	private String parseInformation(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			mClientInfomation = data.getJSONObject("information");
			try {
				result = "parseInformation_" + mClientInfomation.getString("name") + "_ok";
			} catch (Exception e) {
				Log.PrintError(TAG, "parseInformation getString name Exception = " + e.getMessage());
			}
		}
		return result;
	}
	
	private String parseConnetNewServer(JSONObject data) {
		String result = "unknown";
		JSONObject serverObj = null;
		String address = null;
		int port = -1;
		if (data != null && data.length() > 0) {
			try {
				serverObj = data.getJSONObject("server_info");
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStartNewServer getString server_info Exception = " + e.getMessage());
			}
			if (serverObj != null && serverObj.length() > 0) {
				try {
					address = serverObj.getString("address");
				} catch (Exception e) {
					Log.PrintError(TAG, "parseStartNewServer getString address Exception = " + e.getMessage());
				}
				try {
					port = serverObj.getInt("port");
				} catch (Exception e) {
					Log.PrintError(TAG, "parseStartNewServer getString port Exception = " + e.getMessage());
				}
				if (address != null && address.length() > 0 && port != -1) {
					result = "parseConnetNewServer_" + address + ":" + port + "_ok";
					TransferConnection transferConnection = new TransferConnection(address, port, address, port);
					transferConnection.setTransferConnectionCallback(mTransferConnectionCallback);
					transferConnection.startConnection();
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
