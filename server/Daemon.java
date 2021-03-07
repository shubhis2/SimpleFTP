import java.net.ServerSocket;

public class Daemon implements Runnable {
	private MyFtpServer ftpServer;
	private ServerSocket nSocket;

	public Daemon(MyFtpServer ftpServer, ServerSocket nSocket) {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
	}

	public void run() {
		while (true) {
			try {
				(new Thread(new CommandS(ftpServer, nSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}
}
