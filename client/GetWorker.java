import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;

public class GetWorker implements Runnable {
	private FtpClient ftpClient;
	private Socket socket;
	private Path serverPath, path;
	private String[] tokens;
	private int terminateID;

	private InputStreamReader iStream;
	private BufferedReader reader;
	private DataInputStream byteStream;
	private OutputStream oStream;
	private DataOutputStream dStream;

	public GetWorker(FtpClient ftpClient, String hostname, int nPort, String[] tokens, Path serverPath, Path path) throws Exception {
		this.ftpClient = ftpClient;
		this.tokens = tokens;
		this.serverPath = serverPath;
		this.path = path;

		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), nPort), 1000);

		iStream = new InputStreamReader(socket.getInputStream());
		reader = new BufferedReader(iStream);
		byteStream = new DataInputStream(socket.getInputStream());
		oStream = socket.getOutputStream();
		dStream = new DataOutputStream(oStream);
	}

	public void get() throws Exception {
		if (!ftpClient.transfer(serverPath.resolve(tokens[1]))) {
			System.out.println("error: file already transfering");
			return;
		}

		dStream.writeBytes("get " + serverPath.resolve(tokens[1]) + "\n");

		String get_line;
		if (!(get_line = reader.readLine()).equals("")) {
			System.out.println(get_line);
			return;
		}

		try {
			terminateID = Integer.parseInt(reader.readLine());
		} catch(Exception e) {
			System.out.println("Invalid TerminateID");
		}
		System.out.println("TerminateID: " + terminateID);

		ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);

		if (ftpClient.terminateGET(path.resolve(tokens[1]), serverPath.resolve(tokens[1]), terminateID)) return;

		byte[] fileSizeBuffer = new byte[8];
		byteStream.read(fileSizeBuffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
		DataInputStream dis = new DataInputStream(bais);
		long fileSize = dis.readLong();

		if (ftpClient.terminateGET(path.resolve(tokens[1]), serverPath.resolve(tokens[1]), terminateID)) return;

		FileOutputStream f = new FileOutputStream(new File(tokens[1]));
		int count = 0;
		byte[] buffer = new byte[8192];
		long bytesReceived = 0;
		while(bytesReceived < fileSize) {
			if (ftpClient.terminateGET(path.resolve(tokens[1]), serverPath.resolve(tokens[1]), terminateID)) {
				f.close();
				return;
			}
			count = byteStream.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		}
		f.close();

		//CLIENT side un-locking
		ftpClient.transferOUT(serverPath.resolve(tokens[1]), terminateID);
	}

	public void run() {
		try {
			get();
			Thread.sleep(100);
			dStream.writeBytes("quit" + "\n");
		} catch (Exception e) {
			System.out.println("GetWorker error");
		}
	}
}
