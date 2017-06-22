package codeu.chat.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Scanner;

import codeu.chat.server.Controller;
import codeu.chat.server.Server;

public class PersistentLog {
	
	
	//queue where persistent log will be stored
	  public static LinkedList<String> persistentQueue = new LinkedList<String>();
	  
	  //writer to write to file
	  private static PrintWriter persistentDataWriter = null;
	
	//method to read from the file
	public static void read(File persistentFile, Server server){
		try{
			
			//reads from file if it hasn't been created yet
			if(!persistentFile.createNewFile()){
				BufferedReader reader = new BufferedReader(new FileReader(persistentFile));
				
				
					String line;
	    			while ((line = reader.readLine()) != null) {   
	    				String[] command = line.split("\\s+");
	    				
	    				//checks each command and calls appropriate action 
	    				switch(command[0]){
						
							//user should be added
							case "U-ADD":
							
								server.addNewUser(command[1], command[2], command[3]);
								break;
	    			}
				}
			
		}
		}catch (IOException e) {
			
			e.printStackTrace();
		}  

	    
	}	    

	    
	    
	//adds command to queue
	public static void writeQueue(String command){
		persistentQueue.add(command);
	}
	
	//writes the queue to the file
	public static void writeFile(String persistentFile) throws IOException{
		
        
        
  	     persistentDataWriter = new PrintWriter(new FileWriter(persistentFile, true));           	  
		  
		  while (!PersistentLog.persistentQueue.isEmpty()) {
			  String command = PersistentLog.persistentQueue.pop();
			  persistentDataWriter.println(command);
		  }
		  
		  persistentDataWriter.close();
	}

}
