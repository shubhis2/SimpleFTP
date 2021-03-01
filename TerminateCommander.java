import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TerminateCommander implements Runnable {
	private Socket socket;
	private OutputStream oStream;
	private DataOutputStream dStream;
	private int terminateID;

	public TerminateCommander(String hostname, int tPort, int terminateID) throws Exception {
		this.terminateID = terminateID;

		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), tPort), 1000);

		oStream = socket.getOutputStream();
		dStream = new DataOutputStream(oStream);
	}

	public void run() {
		try {
			dStream.writeBytes("terminate " + terminateID + "\n");
		} catch (IOException e) {
			System.out.println("terminate " + terminateID + " error\n");
		}
	}
}
