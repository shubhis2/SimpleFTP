/****************************************************
* FTP Server - MyFtpServer port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/

import java.net.*;
import java.io.*;
import java.util.*;

public class MyFtpServer extends Thread
{
  static ServerSocket serverSocket;
  public static void main(String args[]) throws Exception
  {
    int port = Integer.parseInt(args[0]);
    serverSocket=new ServerSocket(port);

    System.out.println("Server socket running ...");
    // raise exeption if block on input takes longer than 1k seconds (`16 minutes`)
    // serverSocket.setSoTimeout(1000000);
    System.out.println("Waiting for client on port " + serverSocket.getLocalPort());
    // accept client socket connection
    Socket s = serverSocket.accept();
    System.out.println("FTP Client connected to " + s.getRemoteSocketAddress());

    MyFtpServer ftps=new MyFtpServer();
    DataInputStream input;

    while(true)
    {
      try
      {
        input=new DataInputStream(s.getInputStream());
        System.out.println("Waiting for Command ...");
        
        //read commands from client - get/put/quit
        String commandIn=input.readUTF();
        if(commandIn.equals("GET"))
        {
          System.out.println("GET Command Received ...");
          ftps.sendFile(s);
        }
        else if(commandIn.equals("PUT"))
        {
          System.out.println("PUT Command Receiced ...");                
          ftps.receiveFile(s);
        }
        else if(commandIn.equals("quit"))
        {
          System.out.println("Quit Command Received ...");
          System.exit(1);
        }
      }
      catch(IOException i)
      {
        System.out.println(i);
      }
      catch(Exception e){

      }
    }
  }


  public void sendFile(Socket s) throws Exception
  {          
    DataInputStream cin=new DataInputStream(s.getInputStream());
    DataOutputStream cout=new DataOutputStream(s.getOutputStream()); 
    String filename=cin.readUTF();
    File f=new File(filename);
    if(!f.exists())
    {
      cout.writeUTF("File Not Found");
      return;
    }
    else
    {
      cout.writeUTF("READY");
      FileInputStream fin=new FileInputStream(f);
      int ch;
      do
      {
        ch=fin.read();
        cout.writeUTF(String.valueOf(ch));
      }
      while(ch!=-1);    
      fin.close();    
      cout.writeUTF("File Receive Successfully");                            
    }
  }

  public void receiveFile(Socket s) throws Exception
  {
    DataInputStream cin=new DataInputStream(s.getInputStream());
    DataOutputStream cout=new DataOutputStream(s.getOutputStream());
    String filename=cin.readUTF();
    if(filename.equals("File not found"))
    {
      return;
    }
    File f=new File(filename);
    String option;

    if(f.exists())
    {
      cout.writeUTF("File exists already");
      option=cin.readUTF();
    }
    else
    {
      cout.writeUTF("sendFile");
      option="y";
    }

    //if file exists and we want to overwrite it
    if(option.equals("y"))
    {
      FileOutputStream fout=new FileOutputStream(f);
      int ch;
      String temp;
      do
      {
        temp=cin.readUTF();
        ch=Integer.parseInt(temp);
        if(ch!=-1)
        {
          fout.write(ch);                    
        }
      }while(ch!=-1);
      fout.close();
      cout.writeUTF("File Sent Successfully");
    }
    else
    {
      return;
    }
  }
}
