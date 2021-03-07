import java.net.BindException;
import java.net.ServerSocket;

public class Main {
	public static final boolean DEBUG = false;
	private static ServerSocket nSocket, tSocket;


	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("error: Invalid number of arguments");
			return;
		}

		int nPort = 0;
		try {
			nPort = Integer.parseInt(args[0]);
			if (nPort < 1 || nPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid nport number");
			return;
		} catch (Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}

		int tPort = 0;
		try {
			tPort = Integer.parseInt(args[1]);
			if (tPort < 1 || tPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid tport number");
			return;
		} catch(Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}

		if (nPort == tPort) {
			System.out.println("error: nPort and tPort must be port numbers");
			return;
		}

		try {
			nSocket = new ServerSocket(nPort);
			tSocket = new ServerSocket(tPort);
		} catch(BindException be) {
			System.out.println("error: one or more ports are already in use");
			return;
		} catch(Exception e) {
			System.out.println("error: server could not be started");
			return;
		}


		try {
			MyFtpServer ftpServer = new MyFtpServer();

			(new Thread(new NormalDaemon(ftpServer, nSocket))).start();
			(new Thread(new TerminateDaemon(ftpServer, tSocket))).start();
		} catch (Exception e) {
			System.out.println("ftp.server.Main");
			e.printStackTrace(); 
		}
	}
}
