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

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.InterestInfo;
import codeu.chat.common.User;
import codeu.chat.util.Uuid;

public final class UserContext {

  public final User user;
  private final BasicView view;
  private final BasicController controller;

  public UserContext(User user, BasicView view, BasicController controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

  public ConversationContext start(String name) {
    final ConversationHeader conversation = controller.newConversation(name, user.id);
    return conversation == null ?
        null :
        new ConversationContext(user, conversation, view, controller);
  }

  public Iterable<UserContext> users() {

	    // Use all the ids to get all the users and convert them to
	    // User Contexts.
	    final Collection<UserContext> all = new ArrayList<>();
	    for (final User user : view.getUsers()) {
	      all.add(new UserContext(user, view, controller));
	    }

	    return all;
	  }
  
  public Iterable<ConversationContext> conversations() {

    // Use all the ids to get all the conversations and convert them to
    // Conversation Contexts.
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }

    return all;
  }

  public InterestInfo getInterestConversation(Uuid c_id, Uuid u_id){
    return view.getInterestConversation(c_id, u_id);
  }

  public InterestInfo getInterestMessage(Uuid c_id, Uuid m_id, Uuid u_id){
    return view.getInterestMessage(c_id, m_id, u_id);
  }
  
  public void addUserInterest(String name) {
	  controller.newUserInterest(name, user.id);
  }
  
  public void addConversationInterest(String title) {
	  controller.newConversationInterest(title, user.id);
  }
  
  public void removeUserInterest(String name) {
	  controller.removeUserInterest(name, user.id);
  }
  
  public void removeConversationInterest(String title) {
	  controller.removeConversationInterest(title, user.id);
  }

}
