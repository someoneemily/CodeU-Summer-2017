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

package codeu.chat.client.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

final class Controller implements BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final ConnectionSource source;

  public Controller(ConnectionSource source) {
    this.source = source;
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {

    Message response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), author);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      Serializers.STRING.write(connection.out(), body);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_MESSAGE_RESPONSE) {
        response = Serializers.nullable(Message.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public User newUser(String name) {

    User response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      LOG.info("newUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_RESPONSE) {
        response = Serializers.nullable(User.SERIALIZER).read(connection.in());
        LOG.info("newUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner, String default_control)  {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);
      Serializers.STRING.write(connection.out(), default_control);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.nullable(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public void changeDefault(Uuid conversation_id, String default_control){

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.CHANGE_DEFAULT_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversation_id);
      Serializers.STRING.write(connection.out(), default_control);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.CHANGE_DEFAULT_RESPONSE) {
        LOG.info("change-default: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

  }
  @Override
  public void deleteConversation(Uuid conversation_id){

      try (final Connection connection = source.connect()) {
          Serializers.INTEGER.write(connection.out(), NetworkCode.DELETE_CONVERSATION_REQUEST);
          Uuid.SERIALIZER.write(connection.out(), conversation_id);

          if (Serializers.INTEGER.read(connection.in()) == NetworkCode.DELETE_CONVERSATION_RESPONSE) {
              LOG.info("Deleted conversation. Response completed.");
          } else {
              LOG.error("Response from server failed.");
          }
      } catch (Exception ex) {
          System.out.println("ERROR: Exception during call on server. Check log for details.");
          LOG.error(ex, "Exception during call on server.");
      }

  }
  
  public boolean checkAccess(Uuid user_id, Uuid conversation_id, String toCheck){
	  
	  try(final Connection connection = source.connect()){
		  //toCheck contains what access is being checked
		  if(toCheck.equals("Member")){
			  Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_MEMBER_REQUEST);
		  }
		  else if(toCheck.equals("Creator")){
			  Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_CREATOR_REQUEST);
		  }
		  else if(toCheck.equals("Owner")){
			  Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_OWNER_REQUEST);
		  }
		  else if(toCheck.equals("Removed")){
			  Serializers.INTEGER.write(connection.out(), NetworkCode.CHECK_REMOVED_REQUEST);
		  }
		  
		  Uuid.SERIALIZER.write(connection.out(), user_id);
		  Uuid.SERIALIZER.write(connection.out(), conversation_id);
		  
		  //Has retrieved user status
		  if(Serializers.INTEGER.read(connection.in()) == NetworkCode.RETRIEVE_USER_STATUS){
			  LOG.info("checking status of user: Response completed.");
			  return Serializers.BOOLEAN.read(connection.in());
			  
		  } else{
			  LOG.error("Response from server failed.");
		  }
		  
	  } catch(Exception ex){
		  System.out.println("ERROR: Exception durring call on server. Check log for details.");
		  LOG.error(ex, "Exception during call on server.");
	  }
	  return false;
  }

  @Override
  public byte getDefault(Uuid conversation_id){

    byte response = 0;
    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.RETRIEVE_DEFAULT_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversation_id);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.RETRIEVE_DEFAULT_RESPONSE) {
        LOG.info("getting conversation default: Response completed.");
        response = Byte.parseByte(Serializers.STRING.read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;

  }
}
