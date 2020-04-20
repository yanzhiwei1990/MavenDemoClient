package MavenDemoClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TcpClientManager {

	public static final String TAG = TcpClientManager.class.getSimpleName() + " : %s\n";
	
	private String mRemoteServerAddress = null;
	private int mRemoteServerPort = -1;
	private List<TcpClient> mTransferServers = Collections.synchronizedList(new ArrayList<TcpClient>());
	
	public TcpClientManager(String fixedServer, int fixedPort) {
		mRemoteServerAddress = fixedServer;
		mRemoteServerPort = fixedPort;
	}
	
	public void startRun() {
		
	}

	public void stopRun() {
		
	}
	
	public void addTcpClient() {
		
	}
	
	public void removeTcpClient() {
		
	}
}
