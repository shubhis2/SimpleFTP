/****************************************************
* FTP Server - MyFtpServer port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MyFtpServer implements Runnable {
  private Socket socket;
  private Path path;

  // constructor
  public MyFtpServer(Socket serverSocket)
  {
    // start server and wait for a client Connection
    this.socket = serverSocket;
    // get the string representing the client's working directory
    String cwd = System.getProperty("user.dir");
    // get Path object of current working directory
    path = Paths.get(cwd);
  }

  public void run()
  {
    try
    {
      /*
        Receive command on br
        Send error messages on cout
        Send files on cout
        Receive files on cin
      */
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//Use DataInputStream for platform independent formatting of data
			DataInputStream cin = new DataInputStream(socket.getInputStream());
			DataOutputStream cout = new DataOutputStream(socket.getOutputStream());

      while (true) {
        try {
          //capture and parse input
          String cmdLine = br.readLine();
          String delimeter=" ";
          String[] tokens = cmdLine.split(delimeter);

          /*************************************
          * execute commands received from the Client
          ***************************************/
          if(tokens[0].equals("get")) {
            // file to get is in tokens[1]
            //not a directory or file

            if (Files.notExists(path.resolve(tokens[1]))) {
              cout.writeBytes("get: " + tokens[1] + ": No such file or directory in server" + "\n");
            }
            //is a directory
            else if (Files.isDirectory(path.resolve(tokens[1]))) {
              cout.writeBytes("get: " + tokens[1] + ": Is a directory" + "\n");
            }
            // send file
            else {
              cout.writeBytes("\n"); // empty error msg
              File file = new File(path.resolve(tokens[1]).toString());
              long fileSize = file.length(); // length returns file size in bytes
              //send file size
              cout.writeBytes(fileSize + "\n");
              Thread.sleep(100); // pause before writing file

              byte[] buffer = new byte[8192];
              try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                int count = 0;
                while((count = in.read(buffer)) > 0)
                  cout.write(buffer, 0, count);

                in.close();
              }
              catch(Exception e) {
                System.out.println("error in file transfer" + tokens[1]);
              }
            } // end "get"
          }

          /*******************************************
            * put fileName
          *******************************************/
          else if (tokens[0].equals("put")) {
            long fileSize = Long.parseLong(br.readLine());
            File fle = new File(path.resolve(tokens[1]).toString());
            FileOutputStream f = new FileOutputStream(fle);
            //if(fle.exists()){
              //System.out.println("File already exists");
              //cout.writeBytes("File already exists"+ "\n");
            //}
            //else{
            int count = 0;
            byte[] buffer = new byte[8192];
            long bytesReceived = 0;
            while(bytesReceived < fileSize) {
              count = cin.read(buffer);
              f.write(buffer, 0, count);
              bytesReceived += count;
            }
            f.close();

          }

          /*******************************************
            * delete fileName
          *******************************************/
          else if (tokens[0].equals("delete")) {
            try {
              boolean confirm = Files.deleteIfExists(path.resolve(tokens[1]));
              if (!confirm) {
                cout.writeBytes("delete: cannot remove '" + tokens[1] + "': No such file" + "\n");
                cout.writeBytes("\n");
              }
              else
                cout.writeBytes("\n");
            }
            catch(DirectoryNotEmptyException enee) {
              cout.writeBytes("delete: failed to remove `" + tokens[1] + "': Directory not empty" + "\n");
              cout.writeBytes("\n");
            }
            catch(Exception e) {
              cout.writeBytes("delete: failed to remove `" + tokens[1] + "'" + "\n");
              cout.writeBytes("\n");
            }
          }

          /*******************************************
            * ls
          *******************************************/
          else if (tokens[0].equals("ls")) {
            try {
              DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
              for (Path entry: dirStream)
                cout.writeBytes(entry.getFileName() + "\n");
                cout.writeBytes("\n");
            }
            catch(Exception e) {
              cout.writeBytes("ls: failed to retrive contents" + "\n");
              cout.writeBytes("\n");
            }
          }

          /*******************************************
            * cd directoryname - set path to new location
          *******************************************/
          else if (tokens[0].equals("cd")) {
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
            }
            catch (Exception e) {
              cout.writeBytes("cd: " + tokens[1] + ": Error" + "\n");
            }
          }

          /*******************************************
            * mkdir directoryName
          *******************************************/
          else if (tokens[0].equals("mkdir")) {
            try {
              Files.createDirectory(path.resolve(tokens[1]));
              cout.writeBytes("\n");
            }
            catch(FileAlreadyExistsException falee) {
              cout.writeBytes("mkdir: cannot create directory `" + tokens[1] + "': File or folder exists" + "\n");
            }
            catch(Exception e) {
              cout.writeBytes("mkdir: cannot create directory `" + tokens[1] + "': Permission denied" + "\n");
            }
          }

          /*******************************************
            * pwd
          *******************************************/
          else if (tokens[0].equals("pwd")) {
            cout.writeBytes(path + "\n");
          }
          else if (tokens[0].equals("quit")) {
            //close socket
            socket.close();
          }
          else {
            break;
          }
        }
        catch (Exception e) {
          break;
        }
      } // end while
    }
    catch (Exception e) {
        System.out.println(e);
    }
  } // end run thread

  public static void main(String args[])
  {
    int port = 0;
    if(args.length != 1) {
      System.out.println("must supply port number");
      System.exit(1);
    }
    // port number supplied on command line ie: MyFtpServer 9999
    port = Integer.parseInt(args[0]);
    try {
      ServerSocket serverSocket = new ServerSocket(port);
      System.out.println("Server started");
      System.out.println("Waiting for a client ...");
      while (true) {
        (new Thread(new MyFtpServer(serverSocket.accept()))).start();
      }
    }
    catch(BindException be) {
        System.out.println(be);
    }
    catch(Exception e) {
        System.out.println(e);
    }
  } // end main
}
