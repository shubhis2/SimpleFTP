/****************************************************
* FTP Server - MyFtpServer port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/

import java.net.*;
import java.io.*;
import java.util.*;

public class MyFtpServer extends Thread {
  private ServerSocket serverSocket = null;
  private Socket socket = null;
  private DataInputStream input = null;
  private DataOutputStream output =  null;

  // constructor
  public MyFtpServer(int port)
  {
    // start server and wait for a client Connection
    try
    {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started");
//  while (true) // wait for a client connection - DS
        System.out.println("Waiting for a client ...");

        socket = serverSocket.accept();
        System.out.println("Client accepted");
//  run(){ // DS
        // will start thread here
        input = new DataInputStream(
          new BufferedInputStream(socket.getInputStream()));
        output = new DataOutputStream(
          new BufferedOutputStream(socket.getOutputStream()));

        String line = "";
        // read commands from client until "quit" is sent
        while (!line.equals("quit"))
        {
            try
            {
              line = input.readUTF();
              System.out.println(line);
            }
            catch(IOException i)
            {
              System.out.println(i);
            }
        }
//  } // DS end thread
        System.out.println("Closing connection");
        socket.close();
        input.close();
    }
    catch (IOException i)
    {
        System.out.println(i);
    }
  }
  public static void main(String args[])
  {
      // port number supplied on command line ie: MyFtpServer 9999
      int port = Integer.parseInt(args[0]);
      // return???
      MyFtpServer myFtpServer = new MyFtpServer(port);
      myFtpServer.start();
  }
}
  /*
    }
  }

  static ServerSocket serverSocket;

  public MyFtpServer(int port) throws IOException {
    // create server socket on port input on command line
    serverSocket = new ServerSocket(port);
    // raise exeption if block on input takes longer than 1k seconds (`16 minutes`)
    serverSocket.setSoTimeout(1000000);
  }

  public static void main(String[] args) {
    // port number supplied on command line ie: MyFtpServer 9999
    int port = Integer.parseInt(args[0]);

    try {
      Thread t = new MyFtpServer(port);
      t.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
      System.out.println("Server socket running ...");

      while (true) {
        try {
          System.out.println("Waiting for client on port " + serverSocket.getLocalPort());
          // accept client socket connection
          Socket socket = serverSocket.accept();

          System.out.println("Just connected to " + socket.getRemoteSocketAddress());

          DataInputStream input = new DataInputStream(socket.getInputStream());
          DataOutputStream output = new DataOutputStream(socket.getOutputStream());

          output.writeUTF("1 myftpserver>>  ");

         String command = input.readUTF();

          do {
              output.writeUTF("2 myftpserver>> >> " + command);

              command = input.readUTF();

      } while (!command.equals("quit"));

          output.writeUTF("The End");
          socket.close();

        } catch(SocketTimeoutException s) {
          System.out.println("Socket timed out");
          break;
        } catch (IOException e) {
          e.printStackTrace();
          break;
        }
      } // end while
  } // end run
}
*/
