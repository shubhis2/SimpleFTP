/****************************************************
* FTP Server - MyFtp port# port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;

public class Server {
	private static ServerSocket nSocket, tSocket;

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("must supply port number");
		 	System.exit(1);
		}

		int nPort = 0, tPort = 0;
		nPort = Integer.parseInt(args[0]);
		tPort = Integer.parseInt(args[1]);
		try {
			nSocket = new ServerSocket(nPort);
			tSocket = new ServerSocket(tPort);
		} catch(BindException be) {
			   System.out.println(be);
		} catch(Exception e) {
			System.out.println(e);
		}


		try {
			MyFtpServer ftpServer = new MyFtpServer();

			(new Thread(new Daemon(ftpServer, nSocket))).start();
			(new Thread(new TDaemon(ftpServer, tSocket))).start();
		} catch (Exception e) {
			System.out.println("ftp.server.Main");
			e.printStackTrace();
		}
	}
}
