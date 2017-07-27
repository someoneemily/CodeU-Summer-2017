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


package codeu.chat.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.LinearUuidGenerator;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.User;
import codeu.chat.util.*;
import codeu.chat.util.connections.Connection;

public final class Server {

  private static final ServerInfo info = new ServerInfo();
  
  private interface Command {
    void onMessage(InputStream in, OutputStream out) throws IOException;
  }

  
  private static final Logger.Log LOG = Logger.newLog(Server.class);

  private static final int RELAY_REFRESH_MS = 5000;  // 5 seconds

private static final long LOG_REFRESH_MS = 20000;

  private final Timeline timeline = new Timeline();

  private final Map<Integer, Command> commands = new HashMap<>();

  private final Uuid id;
  private final Secret secret;

  private final Model model = new Model();
  private final View view = new View(model);
  private final Controller controller;

  private final Relay relay;
  private Uuid lastSeen = Uuid.NULL;

  private String persistentFile;

  public Server(final Uuid id, final Secret secret, final Relay relay, final String persistentFile) {

    this.id = id;
    this.secret = secret;
    this.controller = new Controller(id, model);
    this.relay = relay;
    
    //store persistent file
    this.persistentFile = persistentFile;

    //Info - A client wants information about the server - uptime and version
    this.commands.put(NetworkCode.SERVER_INFO_REQUEST, new Command(){
    	public void onMessage(InputStream in, OutputStream out) throws IOException {
    		Serializers.INTEGER.write(out, NetworkCode.SERVER_INFO_RESPONSE);
    		Uuid.SERIALIZER.write(out, info.version);
    		Time.SERIALIZER.write(out, info.startTime);
        }
    });


    // New Message - A client wants to add a new message to the back end.
    this.commands.put(NetworkCode.NEW_MESSAGE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid author = Uuid.SERIALIZER.read(in);
        final Uuid conversation = Uuid.SERIALIZER.read(in);
        final String content = Serializers.STRING.read(in);

        final Message message = controller.newMessage(author, conversation, content);
        
        //if message does not exist
        //create new message
        if (message != null) {
         	String messageAddCommand = "M-ADD " + 
         			message.id.toString() + " " + 
         			author.toString() + " " +
         			conversation.toString() + " " +
         			message.creation.inMs() + " " +
         			content;     

            //add command to queue
            PersistentLog.writeQueue(messageAddCommand);
         } else {
        	 
        	LOG.info("unable to create message " + content);
        }
        
        Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
        Serializers.nullable(Message.SERIALIZER).write(out, message);

        timeline.scheduleNow(createSendToRelayEvent(
            author,
            conversation,
            message.id));
        
      }
    });

    // New User - A client wants to add a new user to the back end.
    this.commands.put(NetworkCode.NEW_USER_REQUEST,  new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);
        final User user = controller.newUser(name);
        
        //if a user can be created
        //then we will add the command to the queue
        if(user != null){
        	String userAddCommand = "U-ADD "
                 + user.id.toString() + " "
                 + user.creation.inMs() + " "
                 + user.name;
            

            //add command to queue
            PersistentLog.writeQueue(userAddCommand);
            
        }
        else{
        	
        	LOG.info("unable to create user " + name);
        }
        
        
        

        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
        Serializers.nullable(User.SERIALIZER).write(out, user);
      }
    });

    // New Conversation - A client wants to add a new conversation to the back end.
    this.commands.put(NetworkCode.NEW_CONVERSATION_REQUEST,  new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String title = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        final String default_control = Serializers.STRING.read(in);
        final ConversationHeader conversation = controller.newConversation(title, owner, default_control);

        if(conversation != null){
          String conversationAddCommand = "C-ADD "
                  + conversation.id.toString() + " "
                  + conversation.owner.toString() + " "
                  + conversation.creation.inMs() + " "
                  + conversation.title;

          //add command to queue
          PersistentLog.writeQueue(conversationAddCommand);

        }
        else{

          LOG.info("unable to create conversation " + title);
        }

        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
        Serializers.nullable(ConversationHeader.SERIALIZER).write(out, conversation);
      }
    });

    // Get Users - A client wants to get all the users from the back end.
    this.commands.put(NetworkCode.GET_USERS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<User> users = view.getUsers();

        Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
        Serializers.collection(User.SERIALIZER).write(out, users);
      }
    });

    // Get Conversations - A client wants to get all the conversations from the back end.
    this.commands.put(NetworkCode.GET_ALL_CONVERSATIONS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<ConversationHeader> conversations = view.getConversations();

        Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
        Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
      }
    });

    // Get Conversations By Id - A client wants to get a subset of the converations from
    //                           the back end. Normally this will be done after calling
    //                           Get Conversations to get all the headers and now the client
    //                           wants to get a subset of the payloads.
    this.commands.put(NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
        Serializers.collection(ConversationPayload.SERIALIZER).write(out, conversations);
      }
    });

    // Get Messages By Id - A client wants to get a subset of the messages from the back end.
    this.commands.put(NetworkCode.GET_MESSAGES_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<Message> messages = view.getMessages(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
        Serializers.collection(Message.SERIALIZER).write(out, messages);
      }
    });
    
    this.timeline.scheduleNow(new Runnable() {
        @Override
        public void run() {
          try {

        	  LOG.info("Reading update from relay...");

              for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
                onBundle(bundle);
                lastSeen = bundle.id();
              }
            
            try{
            	
            	// if queue has 5 or more commands, write to file
          	  if (PersistentLog.persistentQueue.size() >= 5) {
          		  LOG.info("Writing commands to transaction log.");
          	  
          		  
          		  PersistentLog.writeFile(persistentFile);
          		  
            }
            }
            catch(Exception e){
          	  LOG.error(e, "Unable to read from file");
            }
            
            
            

          } catch (Exception ex) {

            LOG.error(ex, "Failed to read update from relay.");

          }

          timeline.scheduleIn(RELAY_REFRESH_MS, this);
        }
      });
    

    this.timeline.scheduleIn(LOG_REFRESH_MS, new Runnable() {
        @Override
        public void run() {
          try {
          	  
            //writes to file every twenty seconds regardless of size of queue 
        	  
        	LOG.info("Writing commands to transaction log.");
          	PersistentLog.writeFile(persistentFile);
           
            
          } catch (Exception ex) {

        	LOG.error(ex, "Unable to export to transaction log file.");

          }

          timeline.scheduleIn(LOG_REFRESH_MS, this);
        }
      });
  }
  

//adds new user at the start
public void addNewUser(String id, String time, String name){
	  
	  //converts strings to necessary objects
	  Uuid userid;
	try {
		userid = Uuid.parse(id);
		Time usercreation = Time.fromMs(Long.parseLong(time));
		  
		//adds user
		controller.newUser(userid, name, usercreation);
	} catch (IOException e) {
		LOG.info("Could not read in users from persistent log");
		e.printStackTrace();
	}	  
	  
	  
}

//adds new conversation at the start
  public void addNewConversation(String c_id, String c_owner, String creation, String title, String default_control){

    //converts strings to necessary objects
    Uuid id;
    Uuid owner;
    try {
      id = Uuid.parse(c_id);
      owner = Uuid.parse(c_owner);
      Time creationTime = Time.fromMs(Long.parseLong(creation));

      //adds conversation
      controller.newConversation(id, title, owner, creationTime, default_control);
    } catch (IOException e) {

      LOG.info("Could not read in conversation from persistent log");
      e.printStackTrace();

    }
  }
    
    //adds new message to conversation
    public void addNewMessage(String messageIdString, String userIdString, String convoIdString, String timeString, String content){
    	
    	LOG.info("import message (messageId=%s userId=%s convoId=%s time=%s message=%s)",
    	          messageIdString,
    	          userIdString,
    	          convoIdString,
    	          timeString,
    	          content);
    	
    	//converts strings
    	Uuid messageId;
    	Uuid userId;
    	Uuid convoId;

        try {
          messageId = Uuid.parse(messageIdString);
          userId = Uuid.parse(userIdString);
          convoId = Uuid.parse(convoIdString);
          Time time = Time.fromMs(Long.parseLong(timeString));

          //adds message
          controller.newMessage(messageId, userId, convoId, content, time);
          
        } catch (IOException e) {

          LOG.info("Could not read in conversation from persistent log");
          e.printStackTrace();

        } 
  }


  public void handleConnection(final Connection connection) {
    timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Handling connection...");

          final int type = Serializers.INTEGER.read(connection.in());
          final Command command = commands.get(type);

          if (command == null) {
            // The message type cannot be handled so return a dummy message.
            Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
            LOG.info("Connection rejected");
          } else {
            command.onMessage(connection.in(), connection.out());
            LOG.info("Connection accepted");
          }

        } catch (Exception ex) {

          LOG.error(ex, "Exception while handling connection.");

        }

        try {
          connection.close();
        } catch (Exception ex) {
          LOG.error(ex, "Exception while closing connection.");
        }
      }
    });
  }

  private void onBundle(Relay.Bundle bundle) {

    final Relay.Bundle.Component relayUser = bundle.user();
    final Relay.Bundle.Component relayConversation = bundle.conversation();
    final Relay.Bundle.Component relayMessage = bundle.user();

    User user = model.userById().first(relayUser.id());

    if (user == null) {
      user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
    }

    ConversationHeader conversation = model.conversationById().first(relayConversation.id());

    if (conversation == null) {

      // As the relay does not tell us who made the conversation - the first person who
      // has a message in the conversation will get ownership over this server's copy
      // of the conversation.
      conversation = controller.newConversation(relayConversation.id(),
                                                relayConversation.text(),
                                                user.id,
                                                relayConversation.time(), "1");
      // setting default control to 1 (member) for all conversation from bundle : TODO change if necessary
    }

    Message message = model.messageById().first(relayMessage.id());

    if (message == null) {
      message = controller.newMessage(relayMessage.id(),
                                      user.id,
                                      conversation.id,
                                      relayMessage.text(),
                                      relayMessage.time());
    }
  }

  private Runnable createSendToRelayEvent(final Uuid userId,
                                          final Uuid conversationId,
                                          final Uuid messageId) {
    return new Runnable() {
      @Override
      public void run() {
        final User user = view.findUser(userId);
        final ConversationHeader conversation = view.findConversation(conversationId);
        final Message message = view.findMessage(messageId);
        relay.write(id,
                    secret,
                    relay.pack(user.id, user.name, user.creation),
                    relay.pack(conversation.id, conversation.title, conversation.creation),
                    relay.pack(message.id, message.content, message.creation));
      }
    };
  }
}
