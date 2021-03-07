import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TerminateWorker implements Runnable {
	private MyFtpServer ftpServer;
	private Socket tSocket;

	public TerminateWorker(MyFtpServer ftpServer, Socket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}

	public void run() {
		System.out.println(Thread.currentThread().getName() + " TerminateWorker Started");
		try {

			InputStreamReader iStream = new InputStreamReader(tSocket.getInputStream());
			BufferedReader reader = new BufferedReader(iStream);

			while (!reader.ready())
				Thread.sleep(10);

			List<String> tokens = new ArrayList<String>();
			String command = reader.readLine();
			Scanner tokenize = new Scanner(command);
			if (tokenize.hasNext())
			    tokens.add(tokenize.next());

			if (tokenize.hasNext())
				tokens.add(command.substring(tokens.get(0).length()).trim());
			tokenize.close();
			if (Main.DEBUG) System.out.println(tokens.toString());

			switch(tokens.get(0)) {
				case "terminate":
					ftpServer.terminate(Integer.parseInt(tokens.get(1)));
					System.out.println("Terminate Interrupt=" + tokens.get(1));
					break;
				default:
					if (Main.DEBUG) System.out.println("TerminateWorker invalid command");
			}
		} catch (Exception e) {
			if (Main.DEBUG) e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + " TerminateWorker Exited");
	}
}
