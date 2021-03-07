import java.net.ServerSocket;

public class TDaemon implements Runnable {
	private MyFtpServer ftpServer;
	private ServerSocket tSocket;

	public TDaemon(MyFtpServer ftpServer, ServerSocket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}

	public void run() {
		while (true) {
			try {
				(new Thread(new Terminate(ftpServer, tSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
}
