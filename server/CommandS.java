import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.*;

public class CommandS implements Runnable {
	private MyFtpServer ftpServer;
	private Socket nSocket;
	private Path path;
	private String[] tokens;


	InputStreamReader iStream;
	BufferedReader br;
	DataInputStream cin;
	OutputStream oStream;
	DataOutputStream cout;


	public CommandS(MyFtpServer ftpServer, Socket nSocket) throws Exception {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
		path = Paths.get(System.getProperty("user.dir"));

		iStream = new InputStreamReader(nSocket.getInputStream());
		br = new BufferedReader(iStream);
		cin = new DataInputStream(nSocket.getInputStream());
		oStream = nSocket.getOutputStream();
		cout = new DataOutputStream(oStream);
	}

	public void quit() throws Exception {
		nSocket.close();
		throw new Exception();
	}

	public void run() {
		System.out.println(" Ready for commands ...");
		while (true) {
			try {
				//check every 10 ms for input
				while (!br.ready())
					Thread.sleep(10);

				//capture and parse input
				String command = br.readLine();
				String delimeter=" ";
				String[] tokens = command.split(delimeter);
				System.out.println("command: " + command);

				/*************************************
				* execute commands received from the Client
				 ***************************************/
				 if(tokens[0].equals("get")) {
						// file to get is in tokens[1]
							 if (Files.notExists(path.resolve(tokens[1]))) {
					 			cout.writeBytes("get: " + path.resolve(tokens[1]).getFileName() + ": No such file or directory" + "\n");
					 			return;
					 		}
					 		if (Files.isDirectory(path.resolve(tokens[1]))) {
					 			cout.writeBytes("get: " + path.resolve(tokens[1]).getFileName() + ": Is a directory" + "\n");
					 			return;
					 		}

					 		int lockID = ftpServer.getIN(path.resolve(tokens[1]));
					 		System.out.println(lockID);
					 		if (lockID == -1) {
					 			cout.writeBytes("get: " + path.resolve(tokens[1]).getFileName() + ": No such file or directory" + "\n");
					 			return;
					 		}
					 		cout.writeBytes("\n");
					 		//send id for terminate
					 		cout.writeBytes(lockID + "\n");

					 		Thread.sleep(100);
					 		if (ftpServer.terminateGET(path.resolve(tokens[1]), lockID)) {
					 			quit();
					 			return;
					 		}
					 		byte[] buffer = new byte[1000];
					 		try {
					 			File file = new File(path.resolve(tokens[1]).toString());
					 			//write long filesize as first 8 bytes
					 			long fileSize = file.length();
					 			byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(fileSize).array();
					 			cout.write(fileSizeBytes, 0, 8);
					 			if (ftpServer.terminateGET(path.resolve(tokens[1]), lockID)) {
					 				quit();
					 				return;
					 			}
					 			//write file
					 			BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
					 			int count = 0;
					 			while((count = in.read(buffer)) > 0) {
					 					if (ftpServer.terminateGET(path.resolve(tokens[1]), lockID)) {
					 						in.close();
					 						quit();
					 						return;
					 					}
					 					cout.write(buffer, 0, count);
					 			 }
					 			 in.close();
					 		} catch(Exception e) {
					 			System.out.println("transfer error: " + tokens[1]);
					 		}
					 		ftpServer.getOUT(path.resolve(tokens[1]), lockID);

					  /*******************************************
					  * put fileName
					  *******************************************/
						} else if (tokens[0].equals("put")) {
							int lockID = ftpServer.putIN_ID(path.resolve(tokens[1]));
							System.out.println(lockID);
							cout.writeBytes(lockID + "\n");

							while (!ftpServer.putIN(path.resolve(tokens[1]), lockID))
								Thread.sleep(10);
							if (ftpServer.terminatePUT(path.resolve(tokens[1]), lockID)) {
								quit();
								return;
							}
							cout.writeBytes("\n");
							if (ftpServer.terminatePUT(path.resolve(tokens[1]), lockID)) {
								quit();
								return;
							}
							byte[] fileSizeBuffer = new byte[8];
							cin.read(fileSizeBuffer);
							ByteArrayInputStream bais = new ByteArrayInputStream(fileSizeBuffer);
							DataInputStream dis = new DataInputStream(bais);
							long fileSize = dis.readLong();

							if (ftpServer.terminatePUT(path.resolve(tokens[1]), lockID)) {
								quit();
								return;
							}

							FileOutputStream f = new FileOutputStream(new File(tokens[1]).toString());
							int count = 0;
							byte[] buffer = new byte[1000];
							long bytesReceived = 0;
							while(bytesReceived < fileSize) {
								if (ftpServer.terminatePUT(path.resolve(tokens[1]), lockID)) {
									f.close();
									quit();
									return;
								}
								count = cin.read(buffer);
								f.write(buffer, 0, count);
								bytesReceived += count;
							}
							f.close();
							ftpServer.putOUT(path.resolve(tokens[1]), lockID);

						 /*******************************************
						 * delete fileName
						 *******************************************/
						 } else if (tokens[0].equals("delete")) {
							 if (!ftpServer.delete(path.resolve(tokens[1]))) {
								 cout.writeBytes("cannot delete " + tokens[1] + "\n");
								 cout.writeBytes("\n");
							 }
							 try {
									boolean confirm = Files.deleteIfExists(path.resolve(tokens[1]));
									if (!confirm) {
											 cout.writeBytes("delete: cannot remove '" + tokens[1] + "': No such file" + "\n");
											 cout.writeBytes("\n");
									 } else
											 cout.writeBytes("\n");
								} catch(DirectoryNotEmptyException enee) {
											 cout.writeBytes("delete: failed to remove `" + tokens[1] + "': Directory not empty" + "\n");
											 cout.writeBytes("\n");
							 	} catch(Exception e) {
											 cout.writeBytes("delete: failed to remove `" + tokens[1] + "'" + "\n");
											 cout.writeBytes("\n");
								}

							/*******************************************
							 * ls
							 *******************************************/
						 } else if (tokens[0].equals("ls")) {
									 try {
											 DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
											 for (Path entry: dirStream)
													 cout.writeBytes(entry.getFileName() + "\n");
											 cout.writeBytes("\n");
									 } catch(Exception e) {
											 cout.writeBytes("ls: failed to retrieve contents" + "\n");
											 cout.writeBytes("\n");
						 }
					 	 /*******************************************
						 * cd directoryname - set path to new location
						 *******************************************/
						 } else if (tokens[0].equals("cd")) {
							 try {
									 if (tokens.length == 1) { // current path
											 path = Paths.get(System.getProperty("user.dir"));
											 cout.writeBytes("\n");
									 }
									 else if (tokens[1].equals("..")) { // parent directory
											 if (path.getParent() != null)
													 path = path.getParent();
											 cout.writeBytes("\n");
										}
											 //cd somedirectory
										else {
													//not a directory or file
													if (Files.notExists(path.resolve(tokens[1]))) {
														 cout.writeBytes("cd: " + tokens[1] + ": No such file or directory" + "\n");
													}
													//is a directory
													else if (Files.isDirectory(path.resolve(tokens[1]))) {
														 path = path.resolve(tokens[1]);
														 cout.writeBytes("\n");
													}
													//is a file
													else {
															cout.writeBytes("cd: " + tokens[1] + ": Not a directory" + "\n");
													}
											}
									} catch (Exception e) {
											 cout.writeBytes("cd: " + tokens[1] + ": Error" + "\n");
									}
							 /*******************************************
								* mkdir directoryName
							 *******************************************/
						 } else if (tokens[0].equals("mkdir")) {
							 		try {
								 			Files.createDirectory(path.resolve(tokens[1]));
								 			cout.writeBytes("\n");
							 		} catch(FileAlreadyExistsException falee) {
								 			cout.writeBytes("mkdir: cannot create directory `" + tokens[1] + "': File or folder exists" + "\n");
							 		} catch(Exception e) {
								 			cout.writeBytes("mkdir: cannot create directory `" + tokens[1] + "': Permission denied" + "\n");
							 		}

							 /*******************************************
								 * pwd
							 *******************************************/
						 	} else if (tokens[0].equals("pwd")) {
									cout.writeBytes(path + "\n");

						 	} else if (tokens[0].equals("quit")) {
								 //close socket
							 	 nSocket.close();
						 	} else {
									break;
						 	}

			}catch (Exception e) {
			 	System.out.println(e);
			}
		} // end while
	} // end thread run
}
