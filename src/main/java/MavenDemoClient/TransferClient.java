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

public class TransferClient {

	public static final String TAG = TransferClient.class.getSimpleName() + " : %s\n";
	public static final String ROLE_REQUEST = "request";
	public static final String ROLE_REPONSE = "response";
	
	private String mServerAddress = null;
	private int mServerPort = -1;
	private ExecutorService mExecutorService = null;
	private TransferConnection mTransferConnection = null;
	private Socket mClientSocket = null;
	private boolean mIsRunning = false;
	private InputStream mInputStream = null;
	private OutputStream mOutputStream = null;
	private BufferedInputStream mSocketReader = null;
	private BufferedOutputStream mSocketWriter = null;
	private JSONObject mClientInfomation = null;//add mac address as name
	private String mClientMacAddress = null;
	private boolean isRunning = false;
	private boolean mRecognised = false;
	private String mClientRole = null;
	
	private Runnable mStartListener = new Runnable() {

		public void run() {
			Log.PrintLog(TAG, "startListener running");
			try {
				mInputStream = mClientSocket.getInputStream();
			} catch (IOException e) {
				Log.PrintError(TAG, "accept getInputStream Exception = " + e.getMessage());
			}
			try {
				mOutputStream = mClientSocket.getOutputStream();
			} catch (IOException e) {
				Log.PrintError(TAG, "accept getOutputStream Exception = " + e.getMessage());
			}
			if (mInputStream != null && mOutputStream != null) {
				String inMsg = null;
				String outMsg = null;
				byte[] buffer = new byte[1024 * 1024];
				int length = -1;
				mSocketReader = new BufferedInputStream(mInputStream, buffer.length);
				mSocketWriter = new BufferedOutputStream(mOutputStream, buffer.length);
				if (ROLE_REQUEST.equals(mClientRole)) {
					//send response request client to transfer server
					JSONObject info = new JSONObject();
					//add command
					info.put("command", "information");
					//add client information
					info.put("information", mClientInfomation);
					sendMessage(info.toString());
				}
				while (isRunning) {
					try {
					    while ((length = mSocketReader.read(buffer, 0, buffer.length)) != -1) {
					    	if (length <= 1024) {
					    		try {
					    			inMsg = new String(buffer, 0, length, Charset.forName("UTF-8")).trim();
								} catch (Exception e) {
									inMsg = null;
									Log.PrintError(TAG, "parse first 256 bytes error");
								}
					    		outMsg = dealCommand(inMsg);
					    		if (!"unknown".equals(outMsg)) {
					    			Log.PrintLog(TAG, "Received from inMsg = " + inMsg + ", outMsg = " + outMsg);
							    	sendMessage(outMsg);
					    		}
					    	} else {
					    		outMsg = "unknown";
					    	}
					    	Log.PrintLog(TAG, "length = " + length + ", mClientInfomation = " + mClientInfomation + ",outMsg = " + outMsg);
					    	if ("unknown".equals(outMsg)) {
					    		if (mTransferConnection.getFromTransferClient() != null && mTransferConnection.getToTransferClient() != null) {
					    			switch (mClientRole) {
						    			case ROLE_REQUEST:
						    				mTransferConnection.getToTransferClient().transferBuffer(buffer, 0, length);
						    				break;
						    			case ROLE_REPONSE:
						    				mTransferConnection.getFromTransferClient().transferBuffer(buffer, 0, length);
						    				break;
					    			}
					    		}
					    	}
					    }
					    Log.PrintLog(TAG, "startListener disconnect");
					} catch(Exception e) {
						Log.PrintError(TAG, "accept Exception = " + e.getMessage());
						e.printStackTrace();
						break;
					}
					break;
				}
			} else {
				Log.PrintError(TAG, "accept get stream error");
			}
			Log.PrintLog(TAG, "stop accept");
			dealClearWork();
		}
	};
	
	public TransferClient(ExecutorService executor, TransferConnection transferConnection, String serverAddress, int serverPort) {
		mServerAddress = serverAddress;
		mServerPort = serverPort;
		mExecutorService = executor;
		mTransferConnection = transferConnection;
	}
	
	public void connectToServer() {
		try {
			mClientSocket = new Socket(mServerAddress, mServerPort);
			mIsRunning = true;
			initSocketInformation();
			mExecutorService.submit(mStartListener);
		} catch (IOException e) {
			Log.PrintError(TAG, "connectToServer IOException = " + e.getMessage());
		}
	}
	
	public void disconnectToServer() {
		mIsRunning = false;
		closeSocket();
	}
	
	public void setClientRole(String role) {
		mClientRole = role;
	}
	
	public String getClientRole() {
		return mClientRole;
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
	
	private void initSocketInformation() {
		if (mClientMacAddress == null) {
			mClientMacAddress = getLocalMacAddress();
		}
		//connect to transfer server and report related infomation
		//{"command":"information","information":{"name":"response_tranfer_client","mac_address":"10-7B-44-15-2D-B6","dhcp_address":"192.168.188.150","dhcp_port":50001,"request_client_nat_address":"114.82.25.165","request_client_nat_port":50000,"connected_transfer_server_address":"opendiylib.com","connected_transfer_server_port":19911}}
		if (mClientInfomation == null) {
			JSONObject info = new JSONObject();
			if (ROLE_REQUEST.equals(mClientRole)) {
				info.put("name", "response_request_client");
				info.put("mac_address", mClientMacAddress);
				info.put("request_client_nat_address", mTransferConnection.getRequestNatAddress());
				info.put("request_client_nat_port", mTransferConnection.getRequestNatPort());
				info.put("dhcp_address", getLocalInetAddress());
				info.put("dhcp_port", getLocalPort());
				info.put("connected_transfer_server_address", MainDemoClient.FIXED_HOST);
				info.put("connected_transfer_server_port", getRemotePort());
			} else {
				info.put("name", "response_response_client");
				info.put("mac_address", mClientMacAddress);
				info.put("request_client_nat_address", mTransferConnection.getRequestNatAddress());
				info.put("request_client_nat_port", mTransferConnection.getRequestNatPort());
				info.put("dhcp_address", getLocalInetAddress());
				info.put("dhcp_port", getLocalPort());
				info.put("connected_transfer_server_address", MainDemoClient.FIXED_HOST);
				info.put("connected_transfer_server_port", getRemotePort());
			}
			mClientInfomation = info;
		}
		printClientInfo();
	}
	
	private void sendMessage(String outMsg) {
		try {
			if (mSocketWriter != null && outMsg != null && outMsg.length() > 0) {
				byte[] send = (outMsg/* + "\n"*/).getBytes(Charset.forName("UTF-8"));
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
	
	private void delayMs(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
			// TODO: handle exception
			Log.PrintError(TAG, "delayMs = " + e.getMessage());
		}
	}
	
	private boolean transferBuffer(byte[] buffer, int start, int end) {
		//Log.PrintLog(TAG, "transferBuffer " + mClientInfomation + ", end = " + end);
		boolean result = false;
		try {
			if (mSocketWriter != null) {
				mSocketWriter.write(buffer, start, end);
				mSocketWriter.flush();
				result = true;
			}
		} catch (Exception e) {
			Log.PrintError(TAG, "transferBuffer Exception = " + e.getMessage());
		}
		return result;
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
	
	private String dealCommand(String data) {
		String result = "unknown";
		String command = null;
		JSONObject obj = null;
		if (data != null) {
			try {
				obj = new JSONObject(data);
			} catch (Exception e) {
				//Log.PrintError(TAG, "dealCommand new JSONObject Exception = " + e.getMessage());
			}
			//connect to transfer server and report related infomation
			//{"command":"information","information":{"name":"response_tranfer_client","mac_address":"10-7B-44-15-2D-B6","dhcp_address":"192.168.188.150","dhcp_port":50001,"request_client_nat_address":"114.82.25.165","request_client_nat_port":50000,"connected_transfer_server_address":"opendiylib.com","connected_transfer_server_port":19911}}

			if (obj != null && obj.length() > 0) {
				try {
					command = obj.getString("command");
				} catch (Exception e) {
					//Log.PrintError(TAG, "dealCommand getString command Exception = " + e.getMessage());
				}
				switch (command) {
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
	
	private String parseStatus(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			try {
				result = "parseStatus_" + mClientInfomation.getString("status") + "_ok";
				String checkClient = "parseInformation_" + mClientInfomation.getString("name") + "_" + mClientInfomation.getString("mac_address") + "_ok";
				if (mClientInfomation.getString("status").equals(checkClient)) {
					mTransferConnection.startConnetToResponseServer();
				}
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus getString status Exception = " + e.getMessage());
			}
		}
		return result;
	}
}