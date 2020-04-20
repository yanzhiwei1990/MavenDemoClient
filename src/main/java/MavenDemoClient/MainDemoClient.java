package MavenDemoClient;

public class MainDemoClient {

	public static final String TAG = MainDemoClient.class.getSimpleName() + " : %s\n";
	public static final String FIXED_HOST = "opendiylib.com";
	public static final int FIXED_PORT = 19910;
	
	private TcpClientManager mTcpClientManager = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MainDemoClient mMainDemo = new MainDemoClient();
		mMainDemo.startRun();
	}
	
	private void startRun() {
		initShutDownWork();
		mTcpClientManager = new TcpClientManager(FIXED_HOST, FIXED_PORT);
		mTcpClientManager.startRun();
	}
	
	private void stopRun() {
		mTcpClientManager.stopRun();
	}
	
	private void initShutDownWork() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	Log.PrintLog(TAG, "doShutDownWork");
		    	stopRun();
		    }  
		});
	}
}
