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

public class Worker implements Runnable {
	private FtpClient ftpClient;
	private String hostname;
	private int nPort;
	private Socket socket;
	private Path path, serverPath;
	private String[] tokens;
	private int terminateID;

	private InputStreamReader iStream;
	private BufferedReader reader;
	private DataInputStream byteStream;
	private OutputStream oStream;
	private DataOutputStream dStream;

	public Worker(FtpClient ftpClient, String hostname, int nPort) throws Exception {
		this.ftpClient = ftpClient;
		this.hostname = hostname;
		this.nPort = nPort;

		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), nPort), 1000);

		initiateStream();

		path = Paths.get(System.getProperty("user.dir"));
		System.out.println("Connected to: " + ip);
	}

	public void initiateStream() {
		try {

			iStream = new InputStreamReader(socket.getInputStream());
			reader = new BufferedReader(iStream);

			byteStream = new DataInputStream(socket.getInputStream());

			oStream = socket.getOutputStream();
			dStream = new DataOutputStream(oStream);

	/*		System.out.print("MyFtp > ");
			String cmdLine = input.nextLine();
			String delimeter=" ";
			tokens = cmdLine.split(delimeter);
			// check # args supplied
			if (tokens.length > 2 && !tokens[3].equals("&"))
					System.out.println("Incorrect number of arguments");
			else if ((tokens[0].equalsIgnoreCase("ls") || tokens[0].equalsIgnoreCase("pwd") || tokens[0].equalsIgnoreCase("quit")) && tokens.length != 1)
					System.out.println("Incorrect number of arguments");
			else if ((tokens[0].equalsIgnoreCase("get")||tokens[0].equalsIgnoreCase("put")||tokens[0].equalsIgnoreCase("delete")||tokens[0].equalsIgnoreCase("mkdir")) && tokens.length != 2)
					System.out.println("Incorrect number of arguments");
*/

			dStream.writeBytes("pwd" + "\n");

			String get_line;
			if (!(get_line = reader.readLine()).equals("")) {
				serverPath = Paths.get(get_line);
			}

		} catch (Exception e) {
			System.out.println("communication error\n");
		}
	}

	public void get() throws Exception {

		if (tokens.length > 2) {
			if (tokens[2].equals("&")) {
			/*		tokens.set(1, tokens.get(1).substring(0, tokens.get(1).length()-1).trim());

			List<String> tempList = new ArrayList<String>(tokens);
			*/
			Path tempPath = Paths.get(serverPath.toString());
			Path tempPathClient = Paths.get(path.toString());

			(new Thread(new GetWorker(ftpClient, hostname, nPort, tokens, tempPath, tempPathClient))).start();

			Thread.sleep(50);

			return;
		}
	}

		if (!ftpClient.transfer(serverPath.resolve(tokens[1]))) {
			System.out.println("file busy");
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

		ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);


		byte[] fileSizeBuffer = new byte[8];
		byteStream.read(fileSizeBuffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
		DataInputStream dis = new DataInputStream(bais);
		long fileSize = dis.readLong();

		FileOutputStream f = new FileOutputStream(new File(tokens[1]));
		int count = 0;
		byte[] buffer = new byte[1000];
		long bytesReceived = 0;
		while(bytesReceived < fileSize) {
			count = byteStream.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		}
		f.close();

		ftpClient.transferOUT(serverPath.resolve(tokens[1]), terminateID);
	}

	public void put() throws Exception {
		if (tokens.length > 2) {
			if (tokens[2].equals("&")) {
	/*		tokens.set(1, tokens.get(1).substring(0, tokens.get(1).length()-1).trim());

			List<String> tempList = new ArrayList<String>(tokens);
		*/
				Path tempPath = Paths.get(serverPath.toString());

				(new Thread(new PutWorker(ftpClient, hostname, nPort, tokens, tempPath))).start();

				Thread.sleep(50);

				return;
			}
		}

		if (!ftpClient.transfer(serverPath.resolve(tokens[1]))) {
			System.out.println("error: file already transfering");
			return;
		}

		if (Files.notExists(path.resolve(tokens[1]))) {
			System.out.println("put: " + tokens[1] + ": No such file or directory");
		}

		else if (Files.isDirectory(path.resolve(tokens[1]))) {
			System.out.println("put: " + tokens[1] + ": Is a directory");
		}

		else {
			dStream.writeBytes("put " + serverPath.resolve(tokens[1]) + "\n");

			try {
				terminateID = Integer.parseInt(reader.readLine());
			} catch(Exception e) {
				System.out.println("Invalid TerminateID");
			}

			ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);

			reader.readLine();

			Thread.sleep(100);

			byte[] buffer = new byte[1000];
			try {
				File file = new File(path.resolve(tokens[1]).toString());

				long fileSize = file.length();
				byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
				dStream.write(fileSizeBytes, 0, 8);

				Thread.sleep(100);

				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count = 0;
				while((count = in.read(buffer)) > 0)
					dStream.write(buffer, 0, count);

				in.close();
			} catch(Exception e){
				System.out.println("transfer error: " + tokens[1]);
			}

			ftpClient.transferOUT(serverPath.resolve(tokens[1]), terminateID);
		}
	}

	public void delete() throws Exception {

		dStream.writeBytes("delete " + tokens[1] + "\n");

		String delete_line;
		while (!(delete_line = reader.readLine()).equals(""))
		    System.out.println(delete_line);
	}

	public void ls() throws Exception {

		dStream.writeBytes("ls" + "\n");

		String ls_line;
		while (!(ls_line = reader.readLine()).equals(""))
		    System.out.println(ls_line);
	}

	public void cd() throws Exception {

		if (tokens.length == 1)
			dStream.writeBytes("cd" + "\n");
		else
			dStream.writeBytes("cd " + tokens[1] + "\n");

		String cd_line;
		if (!(cd_line = reader.readLine()).equals(""))
			System.out.println(cd_line);

		dStream.writeBytes("pwd" + "\n");

		String get_line;
		if (!(get_line = reader.readLine()).equals(""))
			serverPath = Paths.get(get_line);
	}

	public void mkdir() throws Exception {

		dStream.writeBytes("mkdir " + tokens[1] + "\n");

		String mkdir_line;
		if (!(mkdir_line = reader.readLine()).equals(""))
			System.out.println(mkdir_line);
	}

	public void pwd() throws Exception {
		dStream.writeBytes("pwd" + "\n");

		System.out.println(reader.readLine());
	}

	public void quit() throws Exception {

		if (!ftpClient.quit()) {
			System.out.println("error: Transfers in progress");
			return;
		}

		dStream.writeBytes("quit" + "\n");
	}

	public void terminate() throws Exception {

		try {
			int terminateID = Integer.parseInt(tokens[1]);
			if (!ftpClient.terminateADD(terminateID))
				System.out.println("Invalid TerminateID");
			else
				(new Thread(new TerminateWorker(hostname, Main.tPort, terminateID))).start();
		} catch (Exception e) {
			System.out.println("Invalid TerminateID");
		}
	}

	public void input() {
		try {
			//keyboard input
			Scanner input = new Scanner(System.in);
			String cmdLine;
	//		String[]tokens;
			String lineOut;
	/*		Scanner input = new Scanner(System.in);
			String command;
*/
			do {
				System.out.print("MyFtp >> ");
				cmdLine = input.nextLine();
				String delimeter=" ";
				tokens = cmdLine.split(delimeter);
				// check # args supplied
				if (tokens.length > 2 && !tokens[3].equals("&"))
						System.out.println("Incorrect number of arguments");
				else if ((tokens[0].equalsIgnoreCase("ls") || tokens[0].equalsIgnoreCase("pwd") || tokens[0].equalsIgnoreCase("quit")) && tokens.length != 1)
						System.out.println("Incorrect number of arguments");
				else if ((tokens[0].equalsIgnoreCase("get")||tokens[0].equalsIgnoreCase("put")||tokens[0].equalsIgnoreCase("delete")||tokens[0].equalsIgnoreCase("mkdir")) && tokens.length != 2)
						System.out.println("Incorrect number of arguments");

	/*			command = input.nextLine();
				command = command.trim();

				tokens = new ArrayList<String>();
				Scanner tokenize = new Scanner(command);
				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				System.out.println(tokens);
*/
				if (tokens.length < 1)
					continue;

				switch(tokens[0]) {
					case "get": 		get(); 			break;
					case "put": 		put(); 			break;
					case "delete": 		delete(); 		break;
					case "ls": 			ls(); 			break;
					case "cd": 			cd(); 			break;
					case "mkdir": 		mkdir(); 		break;
					case "pwd": 		pwd(); 			break;
					case "quit": 		quit(); 		break;
					case "terminate":	terminate();	break;
					default:
						System.out.println("unrecognized command");
				}
		} while (!cmdLine.equalsIgnoreCase("quit"));
			input.close();
			System.out.println("Bye bye ...");

		} catch (Exception e) {
			System.out.println("error: disconnected from host");
			e.printStackTrace();
		}
	}

	public void run() {
		input();
	}
}
