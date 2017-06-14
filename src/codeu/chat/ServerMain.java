// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package codeu.chat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;

import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.User;
import codeu.chat.server.NoOpRelay;
import codeu.chat.server.RemoteRelay;
import codeu.chat.server.Server;
import codeu.chat.util.Logger;
import codeu.chat.util.RemoteAddress;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.ClientConnectionSource;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;
import codeu.chat.util.connections.ServerConnectionSource;

final class ServerMain {

  private static final Logger.Log LOG = Logger.newLog(ServerMain.class);
  public static File persistentPath = null;
  public Queue<String> persistentLog = new LinkedList<String>();

  public static void main(String[] args) {

    Logger.enableConsoleOutput();

    try {
      Logger.enableFileOutput("chat_server_log.log");
    } catch (IOException ex) {
      LOG.error(ex, "Failed to set logger to write to file");
    }

    LOG.info("============================= START OF LOG =============================");

    Uuid id = null;
    Secret secret = null;
    int port = -1;
    // This is the directory where it is safe to store data across runs
    // of the server.
    
    RemoteAddress relayAddress = null;
    
    
    try {
      id = Uuid.parse(args[0]);
      secret = Secret.parse(args[1]);
      port = Integer.parseInt(args[2]);
      persistentPath = new File(args[3]);
      relayAddress = args.length > 4 ? RemoteAddress.parse(args[4]) : null;
    } catch (Exception ex) {
      LOG.error(ex, "Failed to read command arguments");
      System.exit(1);
    }

    
    if (!persistentPath.isDirectory()) {
      LOG.error("%s does not exist", persistentPath);
          
      System.exit(1);
    }
    
    
      
   

    try (
        final ConnectionSource serverSource = ServerConnectionSource.forPort(port);
        final ConnectionSource relaySource = relayAddress == null ? null : new ClientConnectionSource(relayAddress.host, relayAddress.port)
    ) {

      LOG.info("Starting server...");
      runServer(id, secret, serverSource, relaySource);

    } catch (IOException ex) {

      LOG.error(ex, "Failed to establish connections");

    }
  }

  private static void runServer(Uuid id,
                                Secret secret,
                                ConnectionSource serverSource,
                                ConnectionSource relaySource) {

    final Relay relay = relaySource == null ?
                        new NoOpRelay() :
                        new RemoteRelay(relaySource);

    final Server server = new Server(id, secret, relay);

    LOG.info("Created server.");
    
    //in the persistentPath, the persistentLog will be stored as
    //persistentLog.txt
    File persistentFile = new File(persistentPath + "//persistentLog.txt");
    try {
		if(!persistentFile.createNewFile()){
			//create the necessary users, conversations and messages
			BufferedReader reader = new BufferedReader(new FileReader(persistentFile));
			
			//reads in each command
			String command = reader.readLine();
			
			//split on space
			String[] splitStr = command.split("\\s+");
			
			switch(splitStr[0]){
				case "U-ADD":
					
					
					
					//put in method in controller
					StringBuilder sb = new StringBuilder();
				    splitStr = Arrays.copyOfRange(splitStr, 1, splitStr.length);
				    
				    for(String s : splitStr){
				    	sb.append(s);
				    }
				    
					InputStream in = new ByteArrayInputStream( sb.toString().getBytes() );
					

						
					server.newUser(in);
									
					
					break;
			
			
			}
			
			
		}
	} catch (IOException e) {
		
		e.printStackTrace();
	}  
    
    
    
    
    
    
    
    
    
    
    
    
    while (true) {

      try {

        LOG.info("Established connection...");
        final Connection connection = serverSource.connect();
        LOG.info("Connection established.");

        server.handleConnection(connection);

      } catch (IOException ex) {
        LOG.error(ex, "Failed to establish connection.");
      }
    }
  }
}
