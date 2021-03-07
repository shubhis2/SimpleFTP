import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
	public static int nPort, tPort;
	public static String hostname;

	public static void main(String[] args) {
		nPort = 1998; tPort = 1999; // initialize to something
		if (args.length != 3) {
			System.out.println("Please enter: myftp hostname port# port#");
			System.exit(1);
		}
		try {
				nPort = Integer.parseInt(args[1]);
				tPort = Integer.parseInt(args[2]);
		} catch(Exception e) {
				System.out.println("Invalid port number");
				System.exit(1);
		}
		try {
				InetAddress.getByName(args[0]);
		} catch(Exception e) {
				System.out.println("Invalid hostname");
				System.exit(1);
		}

		try {

			FtpClient ftpClient = new FtpClient();

			(new Thread(new Worker(ftpClient, hostname, nPort))).start();
		} catch (SocketTimeoutException ste) {
			System.out.println("error: host could not be reached");
		} catch (ConnectException ce) {
			System.out.println("error: no running FTP at remote host");
		} catch (Exception e) {
			System.out.println("error: program quit unexpectedly");
		}
	}
}
