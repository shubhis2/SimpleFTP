import java.io.*;
import java.net.*;
import java.util.*;

public class Terminate implements Runnable {
	private MyFtpServer ftpServer;
	private Socket tSocket;

	public Terminate(MyFtpServer ftpServer, Socket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}

	public void run() {
		try {

			InputStreamReader cin = new InputStreamReader(tSocket.getInputStream());
			BufferedReader br = new BufferedReader(cin);

			while (!br.ready()) // wait on client
				Thread.sleep(10);

			String command = br.readLine();
      String delimeter=" ";
      String[] tokens = command.split(delimeter);

			if(tokens[0].equalsIgnoreCase("terminate")) {
					ftpServer.terminate(Integer.parseInt(tokens[1]));
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println(Thread.currentThread().getName() + " Terminated");
	}
}
