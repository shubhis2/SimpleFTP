/****************************************************
* FTP Client - MyFtp host port# port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Client {
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
			// instantiate client address space
			FtpClient ftpClient = new FtpClient();
      // create thread Commander to parse commands and send to ser
			(new Thread(new Commands(ftpClient, hostname, nPort))).start();
		} catch(SocketTimeoutException ste) {
	  			System.out.println("Connection Timeout");
	  }	catch(ConnectException ce) {
	  			System.out.println("Not able to connect to server");
	 	}	catch(Exception h) {
	  			System.out.println("Program unexpected quit");
	  }
	}
}
