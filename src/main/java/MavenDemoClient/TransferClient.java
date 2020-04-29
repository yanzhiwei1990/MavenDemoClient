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
				//send response request/response client to transfer server
				sendClientInfomation();
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
					    		if (!"no_need_feedback".equals(outMsg)) {
					    			Log.PrintLog(TAG, "Received from inMsg = " + inMsg + ", outMsg = " + outMsg);
							    	sendMessage(outMsg);
					    		}
					    	} else {
					    		outMsg = "unknown";
					    	}
					    	Log.PrintLog(TAG, "length = " + length + ", mClientInfomation = " + mClientInfomation + ",outMsg = " + outMsg);
					    	if ("unknown".equals(outMsg)) {
					    		if (ROLE_REQUEST.equals(mClientRole) && mTransferConnection.getToTransferClient() == null) {
					    			mTransferConnection.startConnetToResponseServer();
					    			int count = 30;
					    			while (!mTransferConnection.getToTransferClient().isRunning()) {
					    				delayMs(1000);
					    				count--;
					    				if (count < 0) {
					    					Log.PrintLog(TAG, "wait response server 30s time out");
					    					break;
					    				}
										
									}
					    			if (count < 0) {
					    				Log.PrintLog(TAG, "stop request client as time out");
					    				break;
					    			}
					    		}
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
	
	public TransferClient(ExecutorService executor, TransferConnection transferConnection, JSONObject transferServerInformation) {
		mServerAddress = transferConnection.getTransferServerAddress();
		mServerPort = transferConnection.getTransferServerPort();
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
	
	public boolean isRunning() {
		return isRunning;
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
		/*
		{
				"command":"information",
				"information":
					{
						"client_info":
							{
								"name":"response_fixed_request_tranfer_client",
								"mac_address","10-7B-44-15-2D-B6",
								"client_role","request",
								"request_client_nat_address","58.246.136.202",
								"request_client_nat_port":5555,
								"dhcp_address","192.168.188.150",
								"dhcp_port":5555,
								"fixed_server_address":"opendiylib.com",
								"fixed_server_port":19910,
								"connected_transfer_server_address":"www.opendiylib.com",
								"connected_transfer_server_port":19920,
								"connected_server_address":"www.opendiylib.com",
								"connected_server_port":19920
							},
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
			} 
		*/
		if (mClientInfomation == null) {
			JSONObject info = new JSONObject();
			if (ROLE_REQUEST.equals(mClientRole)) {
				info.put("name", "response_request_client");
				info.put("mac_address", mClientMacAddress);
				info.put("client_role", ROLE_REQUEST);
				info.put("request_client_nat_address", mTransferConnection.getOrinalRequestNatAddress());
				info.put("request_client_nat_port", mTransferConnection.getOrinalRequestNatPort());
				info.put("dhcp_address", getLocalInetAddress());
				info.put("dhcp_port", getLocalPort());
				info.put("connected_transfer_server_address", mTransferConnection.getTransferServerAddress());
				info.put("connected_transfer_server_port", mTransferConnection.getTransferServerPort());
				//transfer server
				info.put("connected_server_address", MainDemoClient.FIXED_HOST);
				info.put("connected_server_port", getRemotePort());
				info.put("bonded_response_server_address",mTransferConnection.getResponseServerAddress());
				info.put("bonded_response_server_port", mTransferConnection.getResponseServerPort());
			} else {
				info.put("name", "response_response_client");
				info.put("mac_address", mClientMacAddress);
				info.put("client_role", ROLE_REPONSE);
				info.put("request_client_nat_address", mTransferConnection.getOrinalRequestNatAddress());
				info.put("request_client_nat_port", mTransferConnection.getOrinalRequestNatPort());
				info.put("dhcp_address", getLocalInetAddress());
				info.put("dhcp_port", getLocalPort());
				info.put("connected_transfer_server_address", MainDemoClient.FIXED_HOST);
				info.put("connected_transfer_server_port", getRemotePort());
				//local server
				info.put("connected_server_address", getRemoteInetAddress());
				info.put("connected_server_port", getRemotePort());
				info.put("bonded_response_server_address",mTransferConnection.getResponseServerAddress());
				info.put("bonded_response_server_port", mTransferConnection.getResponseServerPort());
			}
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
					case "result":
						result = parseResult(obj);
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
	
	private String parseStatus(JSONObject data) {
		String result = "unknown";
		if (data != null && data.length() > 0) {
			try {
				result = "parseStatus_" + mClientInfomation.getString("status") + "_ok";
				String checkClient = "parseInformation_" + mClientInfomation.getString("name") + "_" + mClientInfomation.getString("mac_address") + "_ok";
			} catch (Exception e) {
				Log.PrintError(TAG, "parseStatus getString status Exception = " + e.getMessage());
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
						"status":"connected_to_transfer_server",
						"information":
							{
								"name":"response_request_client",
								"mac_address":"10-7B-44-15-2D-B6",
								"client_role":"request",
								"request_client_nat_address","58.246.136.202",
								"request_client_nat_port":5555,
								"dhcp_address","192.168.188.150",
								"dhcp_port":5555,
								"connected_transfer_server_address":"opendiylib.com",
								"connected_transfer_server_port":19920,
								"connected_server_address":"opendiylib.com",
								"connected_server_port":19920,
								"bonded_response_server_address","192.168.188.150",
								"bonded_response_server_port":19920
								"nat_address","58.246.136.202",
								"nat_port":55555
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
				case "connected_to_transfer_server":
					JSONObject returnInfo = null;
					try {
						returnInfo = resultJson.getJSONObject("information");
					} catch (Exception e) {
						Log.PrintError(TAG, "parseResult getString information Exception = " + e.getMessage());
					}
					if (returnInfo != null && returnInfo.length() > 0) {
						String natAddress= tryToGetString(returnInfo, "nat_address");
						int natPort = tryToGetInt(returnInfo, "nat_port");
						//update nat address
						mClientInfomation.put("nat_address", natAddress);
						mClientInfomation.put("nat_port", natPort);
						Log.PrintLog(TAG, "parseResult connected_to_fixed_server and update client info");
					}
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
}