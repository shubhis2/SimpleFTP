import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NormalWorker implements Runnable {
	private MyFtpServer ftpServer;
	private Socket nSocket;
	private Path path;
	private List<String> tokens;


	InputStreamReader iStream;
	BufferedReader reader;

	DataInputStream byteStream;

	OutputStream oStream;
	DataOutputStream dStream;


	public NormalWorker(MyFtpServer ftpServer, Socket nSocket) throws Exception {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
		path = Paths.get(System.getProperty("user.dir"));

		iStream = new InputStreamReader(nSocket.getInputStream());
		reader = new BufferedReader(iStream);
		byteStream = new DataInputStream(nSocket.getInputStream());
		oStream = nSocket.getOutputStream();
		dStream = new DataOutputStream(oStream);
	}

	public void get() throws Exception {

		if (Files.notExists(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": No such file or directory" + "\n");
			return;
		}

		if (Files.isDirectory(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": Is a directory" + "\n");
			return;
		}

		int lockID = ftpServer.getIN(path.resolve(tokens.get(1)));
		if (Main.DEBUG) System.out.println(lockID);
		if (lockID == -1) {
			dStream.writeBytes("get: " + path.resolve(tokens.get(1)).getFileName() + ": No such file or directory" + "\n");
			return;
		}

		dStream.writeBytes("\n");

		dStream.writeBytes(lockID + "\n");

		Thread.sleep(100);

		if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}

		byte[] buffer = new byte[1000];
		try {
			File file = new File(path.resolve(tokens.get(1)).toString());

			long fileSize = file.length();
			byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
			dStream.write(fileSizeBytes, 0, 8);

			if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
				quit();
				return;
			}

			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
			int count = 0;
			while((count = in.read(buffer)) > 0) {
				if (ftpServer.terminateGET(path.resolve(tokens.get(1)), lockID)) {
					in.close();
					quit();
					return;
				}
				dStream.write(buffer, 0, count);
			}

			in.close();
		} catch(Exception e) {
			if (Main.DEBUG) System.out.println("transfer error: " + tokens.get(1));
		}

		ftpServer.getOUT(path.resolve(tokens.get(1)), lockID);
	}

	public void put() throws Exception {

		int lockID = ftpServer.putIN_ID(path.resolve(tokens.get(1)));
		if (Main.DEBUG) System.out.println(lockID);

		dStream.writeBytes(lockID + "\n");

		while (!ftpServer.putIN(path.resolve(tokens.get(1)), lockID))
			Thread.sleep(10);

		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}

		dStream.writeBytes("\n");

		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}

		byte[] fileSizeBuffer = new byte[8];
		byteStream.read(fileSizeBuffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
		DataInputStream dis = new DataInputStream(bais);
		long fileSize = dis.readLong();

		if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
			quit();
			return;
		}

		FileOutputStream f = new FileOutputStream(new File(tokens.get(1)).toString());
		int count = 0;
		byte[] buffer = new byte[1000];
		long bytesReceived = 0;
		while(bytesReceived < fileSize) {
			if (ftpServer.terminatePUT(path.resolve(tokens.get(1)), lockID)) {
				f.close();
				quit();
				return;
			}
			count = byteStream.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		}
		f.close();

		ftpServer.putOUT(path.resolve(tokens.get(1)), lockID);
	}

	public void delete() throws Exception {
		if (!ftpServer.delete(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("delete: cannot remove '" + tokens.get(1) + "': The file is locked" + "\n");
			dStream.writeBytes("\n");
			return;
		}

		try {
			boolean confirm = Files.deleteIfExists(path.resolve(tokens.get(1)));
			if (!confirm) {
				dStream.writeBytes("delete: cannot remove '" + tokens.get(1) + "': No such file" + "\n");
				dStream.writeBytes("\n");
			} else
				dStream.writeBytes("\n");
		} catch(DirectoryNotEmptyException enee) {
			dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "': Directory not empty" + "\n");
			dStream.writeBytes("\n");
		} catch(Exception e) {
			dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "'" + "\n");
			dStream.writeBytes("\n");
		}
	}

	public void ls() throws Exception {
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
			for (Path entry: dirStream)
				dStream.writeBytes(entry.getFileName() + "\n");
			dStream.writeBytes("\n");
		} catch(Exception e) {
			dStream.writeBytes("ls: failed to retrive contents" + "\n");
			dStream.writeBytes("\n");
		}
	}

	public void cd() throws Exception {
		try {
			if (tokens.size() == 1) { // cd
				path = Paths.get(System.getProperty("user.dir"));
				dStream.writeBytes("\n");
			}
			else if (tokens.get(1).equals("..")) { // cd ..
				if (path.getParent() != null)
					path = path.getParent();

				dStream.writeBytes("\n");
			}
			else {

				if (Files.notExists(path.resolve(tokens.get(1)))) {
					dStream.writeBytes("cd: " + tokens.get(1) + ": No such file or directory" + "\n");
				}

				else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
					path = path.resolve(tokens.get(1));
					dStream.writeBytes("\n");
				}

				else {
					dStream.writeBytes("cd: " + tokens.get(1) + ": Not a directory" + "\n");
				}
			}
		} catch (Exception e) {
			dStream.writeBytes("cd: " + tokens.get(1) + ": Error" + "\n");
		}
	}

	public void mkdir() throws Exception {
		try {
			Files.createDirectory(path.resolve(tokens.get(1)));
			dStream.writeBytes("\n");
		} catch(FileAlreadyExistsException falee) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': File or folder exists" + "\n");
		} catch(Exception e) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': Permission denied" + "\n");
		}
	}

	public void pwd() throws Exception {

		dStream.writeBytes(path + "\n");
	}

	public void quit() throws Exception {

		nSocket.close();
		throw new Exception();
	}

	public void run() {
		System.out.println(Thread.currentThread().getName() + " NormalWorker Started");
		exitThread:
		while (true) {
			try {

				while (!reader.ready())  // check for input
					Thread.sleep(10);

				tokens = new ArrayList<String>();
				String command = reader.readLine();
				Scanner tokenize = new Scanner(command);

				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				if (Main.DEBUG) System.out.println(tokens.toString());

				switch(tokens.get(0)) {
					case "get": 	get();		break;
					case "put": 	put();		break;
					case "delete": 	delete();	break;
					case "ls": 		ls();		break;
					case "cd": 		cd();		break;
					case "mkdir": 	mkdir();	break;
					case "pwd": 	pwd();		break;
					case "quit": 	quit();		break exitThread;
					default:
						System.out.println("invalid command");
				}
			} catch (Exception e) {
				break exitThread;
			}
		}
		System.out.println(Thread.currentThread().getName() + " NormalWorker Exited");
	}
}
