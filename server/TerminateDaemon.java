import java.net.ServerSocket;

public class TerminateDaemon implements Runnable {
	private MyFtpServer ftpServer;
	private ServerSocket tSocket;

	public TerminateDaemon(MyFtpServer ftpServer, ServerSocket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}

	public void run() {
		System.out.println(Thread.currentThread().getName() + " TerminateDaemon Started");
		while (true) {
			try {
				(new Thread(new TerminateWorker(ftpServer, tSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " TerminateDaemon failed to start TerminateWorker");
			}
		}
	}
}
