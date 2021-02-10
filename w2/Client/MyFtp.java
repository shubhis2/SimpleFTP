/****************************************************
* FTP Client - MyFtp host port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MyFtp {
	private Socket socket;
	private Path path;
    
	public MyFtp(String hostname, int port) throws Exception {
		socket = new Socket();
	    // InetAddress represents both Ipv4 and IPv6 addresses
	    InetAddress inetAddress = InetAddress.getByName(hostname);
	    // InetSocketAddress constructor creates a socketaddress object (IP addr and port#)
	    // and binds it to the specified ip (from getHostAddress) and port
	    SocketAddress socketAddress = new InetSocketAddress(inetAddress.getHostAddress(), port);
	    // connect this client socket to the server socket
			socket.connect(socketAddress);
	    // output some information
	    System.out.println("IP address: "+socket.getInetAddress());
	    System.out.println("Port number: "+socket.getLocalPort());
	    // get the string representing the client's working directory
	    String cwd = System.getProperty("user.dir");
	    // get Path object of current working directory
	    path = Paths.get(cwd);
	    System.out.println("Connected to: " + inetAddress);
	}

  	/*
     * Begin client program to connect to server and issue basic ftp commands
     * invoke: MyFtp hostname portnumber
  	 */
  	public static void main(String[] args) {
  		//num of args
  		if (args.length != 2) {
  			System.out.println("Please enter: myftp hostname port#");
  			System.exit(1);
  		}
		int port = Integer.parseInt(args[1]);
  		try {
        // establish connection with server on port
  			MyFtp ftpClient = new MyFtp(args[0], port);
        // process commands from the client
  			ftpClient.doCommands();
  		} 
  		catch(SocketTimeoutException ste) {
  			System.out.println("Error: host could not be reached");
  		} 
  		catch(ConnectException ce) {
  			System.out.println("Error: no running FTP at remote host");
  		} 
  		catch(Exception h) {
  			System.out.println("Error: program terminated unexpectedly");
  		}
  	}

	public void doCommands() {
		try {
			DataInputStream cin=new DataInputStream(socket.getInputStream());
			DataOutputStream cout=new DataOutputStream(socket.getOutputStream());
			BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//keyboard input
			Scanner input = new Scanner(System.in);
			String command;
			String[]tokens;
			String lineOut;

			do {
				//get input
				System.out.print("MyFtp > ");
    		command = input.nextLine();
    		String delimeter=" ";
    		tokens = command.split(delimeter);

    		if ((tokens[0].equalsIgnoreCase("ls") || tokens[0].equalsIgnoreCase("pwd") || tokens[0].equalsIgnoreCase("quit")) && tokens.length != 1)
      		System.out.println("Invalid arguments");
    		else if ((tokens[0].equalsIgnoreCase("get")||tokens[0].equalsIgnoreCase("put")||tokens[0].equalsIgnoreCase("delete")||tokens[0].equalsIgnoreCase("mkdir")) && tokens.length != 2)
      		System.out.println("Invalid arguments");

        /************************************
        * get fileName
        *************************************/

    		else if (tokens[0].equalsIgnoreCase("get")){
    				System.out.println("inside client get");
    				BufferedReader br1=new BufferedReader(new InputStreamReader(System.in));
    				if(Files.exists(path.resolve(tokens[1])))
    				{
            			System.out.println("File exists....");
            			System.out.println("Overwrite? y/n");
						String option = br1.readLine();
						if(option.equals("n")){
							continue;
						}
						if(option.equals("y")) {
							cout.writeBytes("get " + tokens[1] + "\n");
							//error messages
							if (!(lineOut = br.readLine()).equals("")) {
								System.out.println(lineOut);
								continue;
							}
							//get file size
							long fileSize = Long.parseLong(br.readLine());
							File fl = new File(tokens[1]);
							FileOutputStream f = new FileOutputStream(fl);
							 {
								int count = 0;
								byte[] buffer = new byte[8192];
								long bytesReceived = 0;
								while(bytesReceived < fileSize) {
									count = cin.read(buffer);
									f.write(buffer, 0, count);
									bytesReceived += count;
								}
								System.out.println("File received");
							}
						//br1.close();
						}

						//f.close();
					}
					else {
						System.out.println("in else");
						cout.writeBytes("get " + tokens[1] + "\n");
							//error messages
							if (!(lineOut = br.readLine()).equals("")) {
								System.out.println(lineOut);
								continue;
							}
						//get file size
							long fileSize = Long.parseLong(br.readLine());
							File fl = new File(tokens[1]);
							FileOutputStream f = new FileOutputStream(fl);
							 {
								int count = 0;
								byte[] buffer = new byte[8192];
								long bytesReceived = 0;
								while(bytesReceived < fileSize) {
									count = cin.read(buffer);
									f.write(buffer, 0, count);
									bytesReceived += count;
								}
								System.out.println("File received");
							}

					}

				} 

				/************************************
		     * put fileName
		    *************************************/
				else if(tokens[0].equalsIgnoreCase("put")) {
					System.out.println("inside client put");
					//not a directory or file
					if (Files.notExists(path.resolve(tokens[1]))) {
						System.out.println("put: " + tokens[1] + ": No such file or directory");
					}
					//is a directory
					else if (Files.isDirectory(path.resolve(tokens[1]))) {
						System.out.println("put: " + tokens[1] + ": Is a directory");
					}
					//transfer file
					else {
						cout.writeBytes("put " + tokens[1] + "\n");

						//if(!(lineOut = br.readLine()).equals("")) {
						//	System.out.println(lineOut);
						//	continue;
						//}
						//lineOut = br.readLine();
						//System.out.println(lineOut);
						
						File file = new File(path.resolve(tokens[1]).toString());
						long fileSize1 = file.length();

						//send file size
						cout.writeBytes(fileSize1 + "\n");

						//need to figure
						Thread.sleep(100);

						byte[] buffer1 = new byte[8192];
						try {
							BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
							int count2 = 0;
							while((count2 = in.read(buffer1)) > 0)
								cout.write(buffer1, 0, count2);
							in.close();
						} 
						catch(Exception e){
							System.out.println("transfer error: " + tokens[1]);
						}
					
					}

				} 

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
          * Quit
        *************************************/
        else if (tokens[0].equalsIgnoreCase("quit")) {
	        System.out.println("closing client ...");
				} 
				else {
					System.out.println("unrecognized command '" + tokens[0] + "'");
				}

			} while (!command.equalsIgnoreCase("quit"));
			input.close();
			System.out.println("Client session closing ...");
		} 
		catch(Exception e) {
			System.out.println("Error: disconnected from host");
		}
	}
}