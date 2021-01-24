/****************************************************
* FTP Server - MyFtpServer port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/

import java.net.*;
import java.io.*;
import java.util.*;

public class MyFtpServer extends Thread {

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
