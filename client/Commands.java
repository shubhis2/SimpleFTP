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

public class Commands implements Runnable {
	private FtpClient ftpClient;
	private String hostname;
	private int nPort;
	private Socket socket;
	private Path path, serverPath;
	private String[] tokens;
	private int terminateID;

	private InputStreamReader iStream;
	private BufferedReader br;
	private DataInputStream cin;
	private OutputStream oStream;
	private DataOutputStream cout;

	public Commands(FtpClient ftpClient, String hostname, int nPort) throws Exception {
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
			br = new BufferedReader(iStream);

			cin = new DataInputStream(socket.getInputStream());

			oStream = socket.getOutputStream();
			cout = new DataOutputStream(oStream);

			cout.writeBytes("pwd" + "\n");

			String get_line;
			if (!(get_line = br.readLine()).equals("")) {
				serverPath = Paths.get(get_line);
			}

		} catch (Exception e) {
			System.out.println("communication error\n");
		}
	}

	public void run() {
		try {
			//keyboard input
			Scanner input = new Scanner(System.in);
			String cmdLine;
			String lineOut;

			do {
				System.out.print("MyFtp >> ");
				cmdLine = input.nextLine();
				String delimeter=" ";
				tokens = cmdLine.split(delimeter);
				// check # args supplied
				if (tokens.length > 2 && !tokens[2].equals("&"))
						System.out.println("Incorrect number of arguments");
				else if ((tokens[0].equalsIgnoreCase("ls") || tokens[0].equalsIgnoreCase("pwd") || tokens[0].equalsIgnoreCase("quit")) && tokens.length != 1)
						System.out.println("Incorrect number of arguments");
		//		else if ((tokens[0].equalsIgnoreCase("get")||tokens[0].equalsIgnoreCase("put")||tokens[0].equalsIgnoreCase("delete")||tokens[0].equalsIgnoreCase("mkdir")) && tokens.length != 2)
			//			System.out.println("Incorrect number of arguments");
				else if ((tokens[0].equalsIgnoreCase("delete")||tokens[0].equalsIgnoreCase("mkdir")) && tokens.length != 2)
						System.out.println("Incorrect number of arguments");

				if (tokens.length < 1)
					continue;
				/****************************************************
				* terminate - launch new terminate thread
				*****************************************************/
				if (tokens[0].equalsIgnoreCase("terminate")) {
 							System.out.println("Terminate!!!");
							int terminateID = Integer.parseInt(tokens[1]);
							if (!ftpClient.terminateADD(terminateID))
									System.out.println("Invalid TerminateID");
							else
									(new Thread(new Terminator(hostname, Client.tPort, terminateID))).start();
				}

				/************************************
 				* get fileName
				*************************************/

				else if (tokens[0].equalsIgnoreCase("get")){
					if (tokens.length > 2) {
						if (tokens[2].equals("&")) {
								System.out.println("Detected & on get\n");
								Path filePath = Paths.get(serverPath.toString());
								Path filePathClient = Paths.get(path.toString());

								(new Thread(new GetBg(ftpClient, hostname, nPort, tokens, filePath, filePathClient))).start();

								Thread.sleep(100);
						}
					}
						// send the command line to server socket
						else {
								cout.writeBytes("get " + serverPath.resolve(tokens[1]) + "\n");
								String get_line;
								// if error msg sent output and continue
								if (!(get_line = br.readLine()).equals("")) {
										System.out.println(get_line);
										continue;
								}
								try {
										terminateID = Integer.parseInt(br.readLine());
								} catch(Exception e) {
										System.out.println("Invalid TerminateID");
								}
								System.out.println("Terminate ID: " + terminateID);
								ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);

								//	long fileSize = Long.parseLong(br.readLine());
								byte[] fileSizeBuffer = new byte[8];
								cin.read(fileSizeBuffer);
								ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
								DataInputStream dis = new DataInputStream(bais);
								long fileSize = dis.readLong();

								FileOutputStream f = new FileOutputStream(new File(tokens[1]));
								int count = 0;
								byte[] buffer = new byte[1000];
								long bytesReceived = 0;
								while(bytesReceived < fileSize) {
										count = cin.read(buffer);
										f.write(buffer, 0, count);
										bytesReceived += count;
								}
								f.close();
								ftpClient.transferOUT(serverPath.resolve(tokens[1]), terminateID);
						}
				} // get

				/************************************
				 * put fileName
				*************************************/
				else if(tokens[0].equalsIgnoreCase("put")) {
					if (Files.notExists(path.resolve(tokens[1]))) {
							System.out.println("put: " + tokens[1] + " : No such file or directory");
					}
					else if (Files.isDirectory(path.resolve(tokens[1]))) {
							System.out.println("put: " + tokens[1] + ": Is a directory");
					}
					System.out.println("tokens length: " + tokens.length);
					if (tokens.length > 2) {
						if (tokens[2].equals("&")) {
							System.out.println("Detected & on put\n");

							Path filePath = Paths.get(serverPath.toString());

			//			long fileSize1 = file.length();  // ??? in thread ???
							(new Thread(new PutBg(ftpClient, hostname, nPort, tokens, filePath))).start();

							Thread.sleep(100);
						}
					}
					cout.writeBytes("put " + serverPath.resolve(tokens[1]) + "\n"); // send command line
					try {
							terminateID = Integer.parseInt(br.readLine());
					} catch(Exception e) {
							System.out.println("Invalid TerminateID");
					}
					System.out.println("Terminate ID: " + terminateID);
					ftpClient.transferIN(serverPath.resolve(tokens[1]), terminateID);
					// ready to write
					br.readLine();
					Thread.sleep(100);

					File file = new File(path.resolve(tokens[1]).toString());
					long fileSize1 = file.length();
					byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize1).array();
					cout.write(fileSizeBytes, 0, 8);

					Thread.sleep(100);
					/*
						cout.writeBytes(fileSize1 + "\n"); // send #bytes in file
						Thread.sleep(100); // pause before writing file
					*/
					byte[] buffer1 = new byte[1000];
					try {
							BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
							int count2 = 0;
							while((count2 = in.read(buffer1)) > 0)
								cout.write(buffer1, 0, count2);
								in.close();
					} catch(Exception e){
								System.out.println("transfer error: " + tokens[1]);
					}
				} // put

				/************************************
				* ls
				*************************************/
				else if (tokens[0].equalsIgnoreCase("ls")) {
					cout.writeBytes("ls" + "\n");
					while (!(lineOut = br.readLine()).equals(""))
							System.out.println(lineOut);
				}
				/************************************
				* delete fileName
				*************************************/
				else if(tokens[0].equalsIgnoreCase("delete")) {
					cout.writeBytes("delete " + tokens[1] + "\n");
					while (!(lineOut = br.readLine()).equals(""))
							System.out.println(lineOut);

				}
				/************************************
					* cd directoryname
				*************************************/
				else if (tokens[0].equalsIgnoreCase("cd")) {
					if (tokens.length == 1) //allow "cd" goes back to home directory
							cout.writeBytes("cd" + "\n");
					else
							cout.writeBytes("cd " + tokens[1] + "\n");
					if (!(lineOut = br.readLine()).equals(""))
							System.out.println(lineOut);
				}
				/************************************
				* mkdir directoryname
				*************************************/
				else if(tokens[0].equalsIgnoreCase("mkdir")) {
					cout.writeBytes("mkdir " + tokens[1] + "\n");
					if (!(lineOut = br.readLine()).equals(""))
							System.out.println(lineOut);
				}
				/************************************
				 * pwd
				*************************************/
				else if (tokens[0].equalsIgnoreCase("pwd")) {
					cout.writeBytes("pwd" + "\n");
					System.out.println(br.readLine());
				}
				/************************************
				* unrecognized command
				*************************************/
				else {
					System.out.println("unrecognized command '" + tokens[0] + "'");
				}

		} while (!cmdLine.equalsIgnoreCase("quit"));
			input.close();
			System.out.println("Bye bye ...");

		} catch (Exception e) {
			System.out.println("error: disconnected from host");
			e.printStackTrace();
		}
	}
}
