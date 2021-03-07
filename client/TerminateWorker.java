/****************************************************
* FTP Client - MyFtp host port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TerminateWorker implements Runnable {
	private Socket socket;
	private OutputStream oStream;
	private DataOutputStream dStream;
	private int terminateID;

	public TerminateWorker(String hostname, int tPort, int terminateID) throws Exception {
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
			System.out.println("TerminateWorker");
		}
	}
}
