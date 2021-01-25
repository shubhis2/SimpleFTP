/****************************************************
* FTP Client - MyFtp host port#
* CSCI 6780 - Distributed Computing - Dr. Ramaswamy
* Authors: Diane Stephens, Shubhi Shrivastava
*****************************************************/
import java.net.*;
import java.io.*;
import java.util.*;


public class MyFtp {
  public static void main(String args[]) throws Exception {
    Socket client = null;
    //String host = args[0];
    int port = Integer.parseInt(args[0]);
    try {
      client=new Socket("localhost",port);
      MyFtp ftpc=new MyFtp();

      while(true) { 
        DataInputStream input=new DataInputStream(client.getInputStream());
        DataOutputStream output=new DataOutputStream(client.getOutputStream());
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));   
        String commandIn;
        commandIn=br.readLine();
        if(commandIn.equals("get"))
        {
          output.writeUTF("GET");
          ftpc.getRemoteFile(client);
        }
        else if(commandIn.equals("put"))
        {
          output.writeUTF("PUT");
          ftpc.putLocalFile(client);
        }
        else
        {
          output.writeUTF("quit");
          System.exit(1);
        }
      }
    }
    catch (IOException e) 
    {
      e.printStackTrace();
    }
    catch (NoSuchElementException ne) 
    {
      System.out.println("Connection closed");
    }
    finally 
    {
      try
      {
        System.out.println("\n* Closing connection *");
        client.close();
      }
      catch (IOException ioEx)
      {
        System.out.println("Unable to disconnect");
        System.exit(1);
      }
    }    
  }

  public void putLocalFile(Socket s) throws Exception
  {        
    DataInputStream cin = new DataInputStream(s.getInputStream());
    DataOutputStream cout=new DataOutputStream(s.getOutputStream());
    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    String filename;
    System.out.print("Enter File Name :");
    filename=br.readLine();

    File f=new File(filename);
    if(!f.exists())
    {
      System.out.println("File not Exists...");
      cout.writeUTF("File not found");
      return;
    }

    cout.writeUTF(filename);

    String servReply=cin.readUTF();
    if(servReply.equals("File exists already"))
    {
      String option;
      System.out.println("File exists already");
      System.out.println("Overwrite? y/n ");
      option=br.readLine();            
      if(option.equals("y"))    
      {
        cout.writeUTF("y");
      }
      else
      {
        cout.writeUTF("n");
        return;
      }
    }

    System.out.println("Sending File ...");
    FileInputStream fin=new FileInputStream(f);
    int ch;
    do
    {
      ch=fin.read();
      cout.writeUTF(String.valueOf(ch));
    }
    while(ch!=-1);
    fin.close();
    System.out.println(cin.readUTF());

  }

  public void getRemoteFile(Socket s) throws Exception
  {
    DataInputStream cin = new DataInputStream(s.getInputStream());
    DataOutputStream cout=new DataOutputStream(s.getOutputStream());
    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));

    String fileName;
    System.out.print("Enter File Name :");
    fileName=br.readLine();
    cout.writeUTF(fileName);
    String servReply=cin.readUTF();

    if(servReply.equals("File Not Found"))
    {
      System.out.println("File not found on Server ...");
      return;
    }
    else if(servReply.equals("READY"))
    {
      System.out.println("Receiving File ...");
      File f=new File(fileName);
      if(f.exists())
      {
        String option;
        System.out.println("File exists already");
        System.out.println("Overwrite? y/n ");
        option=br.readLine();            
        if(option.equals("n"))    
        {
          cout.flush();
          return;    
        }                
      }
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
      System.out.println(cin.readUTF());
    }
  }
}
