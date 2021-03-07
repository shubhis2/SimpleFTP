/****************************************************
* FTP Client - MyFtp host port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.nio.ByteBuffer;

public class PutWorker implements Runnable {
	private FtpClient ftpClient;
	private Socket socket;
	private Path path, serverPath;
	private List<String> tokens;
	private int terminateID;


	private InputStreamReader iStream;
	private BufferedReader reader;
	private OutputStream oStream;
	private DataOutputStream dStream;


	public PutWorker(FtpClient ftpClient, String hostname, int nPort, List<String> tokens, Path serverPath) throws Exception {
		this.ftpClient = ftpClient;
		this.tokens = tokens;
		this.serverPath = serverPath;

		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), nPort), 1000);

		iStream = new InputStreamReader(socket.getInputStream());
		reader = new BufferedReader(iStream);
		oStream = socket.getOutputStream();
		dStream = new DataOutputStream(oStream);

		path = Paths.get(System.getProperty("user.dir"));
	}

	public void put() throws Exception {

		if (!ftpClient.transfer(serverPath.resolve(tokens.get(1)))) {
			System.out.println("error: file already transfering");
			return;
		}

		if (Files.notExists(path.resolve(tokens.get(1)))) {
			System.out.println("put: " + tokens.get(1) + ": No such file or directory");
		}

		else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
			System.out.println("put: " + tokens.get(1) + ": Is a directory");
		}

		else {

			dStream.writeBytes("put " + serverPath.resolve(tokens.get(1)) + "\n");

			try {
				terminateID = Integer.parseInt(reader.readLine());
			} catch(Exception e) {
				System.out.println("Invalid TerminateID");
			}
			System.out.println("TerminateID: " + terminateID);

			ftpClient.transferIN(serverPath.resolve(tokens.get(1)), terminateID);

			if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;

			reader.readLine();
			Thread.sleep(100);

			if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;

			byte[] buffer = new byte[1000];
			try {
				File file = new File(path.resolve(tokens.get(1)).toString());

				long fileSize = file.length();
				byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
				dStream.write(fileSizeBytes, 0, 8);

				if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) return;

				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count = 0;
				while((count = in.read(buffer)) > 0) {
					if (ftpClient.terminatePUT(serverPath.resolve(tokens.get(1)), terminateID)) {
						in.close();
						return;
					}
					dStream.write(buffer, 0, count);
				}

				in.close();
			} catch(Exception e){
				System.out.println("transfer error: " + tokens.get(1));
			}

			ftpClient.transferOUT(serverPath.resolve(tokens.get(1)), terminateID);
		}
	}

	public void run() {
		try {
			put();
			Thread.sleep(100);
			dStream.writeBytes("quit" + "\n");
		} catch (Exception e) {
			System.out.println("PutWorker error");
		}
	}
}
