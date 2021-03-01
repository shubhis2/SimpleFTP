import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PutBg implements Runnable {
	private Client ftpClient;
	private Socket socket;
//	private File file;
	private Path path, serverPath;
	private String[]tokens;
	private int terminateID;

	//Stream
	private InputStreamReader iStream;
	private BufferedReader reader;
	private OutputStream oStream;
	private DataOutputStream dStream;


	public PutBg(Client ftpClient, String hostname, int nPort, String[] tokens, Path serverPath) throws Exception {
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

	/* ????? this ????
		if (!ftpClient.transfer(filePath.resolve(tokens[1]))) {
			System.out.println("error: file already transfering");
			return;
		}
		*****/
		dStream.writeBytes("put " + serverPath.resolve(tokens[1]) + "\n");

			//wait for terminate ID
			try {
				terminateID = Integer.parseInt(reader.readLine());
			} catch(Exception e) {
				System.out.println("Invalid TerminateID");
			}
			System.out.println("TerminateID: " + terminateID);

			//CLIENT side locking
//DS figure this out
			ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);

			if (ftpClient.terminatePUT(serverPath.resolve(tokens[1]), terminateID)) return;

			reader.readLine();

			Thread.sleep(100);

			if (ftpClient.terminatePUT(serverPath.resolve(tokens[1]), terminateID)) return;

			byte[] buffer = new byte[1000];
			try {
				File file = new File(path.resolve(tokens[1]).toString());

				//write long filesize as first 8 bytes
				long fileSize = file.length();
				byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
				dStream.write(fileSizeBytes, 0, 8);

				if (ftpClient.terminatePUT(serverPath.resolve(tokens[1]), terminateID)) return;

				//write file
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count = 0;
				while((count = in.read(buffer)) > 0) {
					if (ftpClient.terminatePUT(serverPath.resolve(tokens[1]), terminateID)) {
						in.close();
						return;
					}
					dStream.write(buffer, 0, count);
				}

				in.close();
			} catch(Exception e){
				System.out.println("transfer error: " + tokens[1]);
			}

			//CLIENT side un-locking
			ftpClient.transferOUT(serverPath.resolve(tokens[1]), terminateID);

	}

	public void run() {
		try {
			put();
			Thread.sleep(100);
			dStream.writeBytes("quit" + "\n");
		} catch (Exception e) {
			System.out.println("Error in PutBg");
		}
	}
}
