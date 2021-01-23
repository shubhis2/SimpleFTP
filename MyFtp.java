/****************************************************
* FTP Client - MyFtp host port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MyFtp {
  public static void main(String[] args) {

    Socket client = null;
    String host = args[0];
    int port = Integer.parseInt(args[1]);
    try {
      client = new Socket(host, port);

      // Set up I/O streams for server communication
      DataInputStream input = new DataInputStream(client.getInputStream());
      DataOutputStream output = new DataOutputStream(client.getOutputStream());

      // keyboard entry
      Scanner userEntry = new Scanner(System.in);
      // read prompt from server
      System.out.println("after Scanner: " + input.readUTF());

      String commandIn;
      int i;
      do {
        System.out.print("\nClient reads commands (int) to send to server:\n");
        //commandIn = userEntry.nextLine();
        i = userEntry.nextInt();
        //send command to server
  //      output.writeUTF(commandIn);
        output.writeInt(i);

        System.out.println("Back from server: " + input.readUTF());
//      } while (commandIn != null);
      } while (i != 0);
    } catch (IOException e) {
      e.printStackTrace();
    }
    catch (NoSuchElementException ne) {
      System.out.println("Connection closed");
    }
    finally {
      try {
        System.out.println("\n* Closing connection *");
        client.close();
      } catch (IOException ioEx) {
        System.out.println("Unable to disconnect");
        System.exit(1);
      }
    }
  }
}
