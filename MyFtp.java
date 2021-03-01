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
	public static int nport, tport;
	public static String hostname;

// initialize
/*  moved this to commander thread
	public MyFtp() throws Exception {

			socket = new Socket();
	    // InetAddress represents both Ipv4 and IPv6 addresses
			InetAddress inetAddress = InetAddress.getByName(hostname);
	    // InetSocketAddress constructor creates a socketaddress object (IP addr and port#)
	    // binds it to the specified ip (from getHostAddress) and port
	    SocketAddress socketAddress = new InetSocketAddress(inetAddress.getHostAddress(), n_port);
	    // connect this client socket to the server socket
			socket.connect(socketAddress);
			System.out.println("Connected to: " + inetAddress);
	    // get the string representing the client's working directory
	    String cwd = System.getProperty("user.dir");
	    // get Path object of current working directory
	    path = Paths.get(cwd);

	}
*/
  	/*
     * Begin client program to connect to server and pass commands to server
     * invoke: MyFtp hostname portnumber
  	 */
  	public static void main(String[] args) {

			 	nport = 1998; tport = 1999; // initialize to something
	//		int nport = 1000, tport = 1000;
			if (args.length != 3) {
  				System.out.println("Please enter: myftp hostname port# port#");
  				System.exit(1);
  		}
			try {
					nport = Integer.parseInt(args[1]);
					tport = Integer.parseInt(args[2]);
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
  			Client client = new Client();
        // create thread Commander to parse commands and send to server
				(new Thread(new Commander(client, hostname, nport))).start();
  		}
  		catch(SocketTimeoutException ste) {
  			System.out.println("Connection Timeout");
  		}
  		catch(ConnectException ce) {
  			System.out.println("Not able to connect to server");
  		}
  		catch(Exception h) {
  			System.out.println("Program unexpected quit");
  		}
  	}
		/*
			Send commands to server via cout
			Send files on cout.  Receive files sent on cin
			Receive possible error messages on br
		*/

	public void doCommands() {
		try {
			DataInputStream cin=new DataInputStream(socket.getInputStream());
			DataOutputStream cout=new DataOutputStream(socket.getOutputStream());
			BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//keyboard input
			Scanner input = new Scanner(System.in);
			String cmdLine;
			String[]tokens;
			String lineOut;

			do {
				System.out.print("MyFtp > ");
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

				/****************************************************
				* if & - get and put run in new Thread
				*****************************************************/
				else if (tokens[0].equalsIgnoreCase("get") || tokens[0].equalsIgnoreCase("put") && tokens[3].equals("&")) {
					System.out.println("Launching doCommands_bg thread !!!");
					Runnable r1 = new doCommands_bg(hostname, nport);
					new Thread(r1).start();

					/****************************************************
					* terminate - launch new terminate thread
					*****************************************************/
				} else if (tokens[0].equalsIgnoreCase("terminate")) {
						System.out.println("Launching thread Terminate!!!");
						Runnable t1 = new Terminate(hostname, tport);
						new Thread(t1).start();
				}

        /************************************
        * get fileName
        *************************************/

    		else if (tokens[0].equalsIgnoreCase("get")){
							// send the command line to server socket
							cout.writeBytes("get " + tokens[1] + "\n");
							String get_line;
							// if error msg sent output and continue
							if (!(get_line = br.readLine()).equals("")) {
									System.out.println(get_line);
									continue;
							}
							long fileSize = Long.parseLong(br.readLine());
							FileOutputStream f = new FileOutputStream(new File(tokens[1]));
							int count = 0;
							byte[] buffer = new byte[8192];
							long bytesReceived = 0;
							while(bytesReceived < fileSize) {
										count = cin.read(buffer);
										f.write(buffer, 0, count);
										bytesReceived += count;
							}
							f.close();
				} // get

				/************************************
		     * put fileName
		    *************************************/
				else if(tokens[0].equalsIgnoreCase("put")) {
					//not a directory or file
					if (Files.notExists(path.resolve(tokens[1]))) {
						System.out.println("put: " + tokens[1] + " : No such file or directory");
					}
					//is a directory
					else if (Files.isDirectory(path.resolve(tokens[1]))) {
						System.out.println("put: " + tokens[1] + ": Is a directory");
					}

					else { // send file
							cout.writeBytes("put " + tokens[1] + "\n"); // send command line

							File file = new File(path.resolve(tokens[1]).toString());
							long fileSize1 = file.length();

							cout.writeBytes(fileSize1 + "\n"); // send #bytes in file
							Thread.sleep(100); // pause before writing file

							byte[] buffer1 = new byte[8192];
							try {
										BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
										int count2 = 0;
										while((count2 = in.read(buffer1)) > 0)
													cout.write(buffer1, 0, count2);
										in.close();
							} catch(Exception e){
										System.out.println("transfer error: " + tokens[1]);
							}
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
          * Quit
        *************************************/
        else if (tokens[0].equalsIgnoreCase("quit")) {
	        System.out.println("Closing client session ...");
				}
				else {
					System.out.println("unrecognized command '" + tokens[0] + "'");
				}

			} while (!cmdLine.equalsIgnoreCase("quit"));
			input.close();
			System.out.println("Bye bye ...");
		}
		catch(Exception e) {
			System.out.println("Disconnected session ...");
		}
	}
}

// Here we can extends any other class
class Terminate implements Runnable {

	   public Terminate(String host, int tport) throws Exception {
			 int t_port = tport;
			 Socket t_socket = new Socket();
 	    // InetAddress represents both Ipv4 and IPv6 addresses
 			InetAddress t_inetAddress = InetAddress.getByName(host);
 	    // InetSocketAddress constructor creates a socketaddress object (IP addr and port#)
 	    // binds it to the specified ip (from getHostAddress) and port

 	    SocketAddress t_socketAddress = new InetSocketAddress(t_inetAddress.getHostAddress(), t_port);
 	    // connect this client socket to the server socket
 			t_socket.connect(t_socketAddress);
 			System.out.println("Connected to: " + t_inetAddress);
			DataInputStream t_cin=new DataInputStream(t_socket.getInputStream());
			DataOutputStream t_cout=new DataOutputStream(t_socket.getOutputStream());

	   }

	   public void run() {
			 System.out.println("Run method of Terminate thread");
	 	}
}

// Here we can extends any other class
class doCommands_bg implements Runnable {

	   public doCommands_bg(String host, int nport) throws Exception {
			 int n_port = nport;
			 Socket n_socket = new Socket();
 	    // InetAddress represents both Ipv4 and IPv6 addresses
 			InetAddress n_inetAddress = InetAddress.getByName(host);
 	    // InetSocketAddress constructor creates a socketaddress object (IP addr and port#)
 	    // binds it to the specified ip (from getHostAddress) and port

 	    SocketAddress n_socketAddress = new InetSocketAddress(n_inetAddress.getHostAddress(), n_port);
 	    // connect this client socket to the server socket
 			n_socket.connect(n_socketAddress);
 			System.out.println("Connected to: " + n_inetAddress);
			DataInputStream n_cin=new DataInputStream(n_socket.getInputStream());
			DataOutputStream n_cout=new DataOutputStream(n_socket.getOutputStream());

	   }

	   public void run() {
			 System.out.println("Run method of doCommands_bg thread");
	 	}
}
