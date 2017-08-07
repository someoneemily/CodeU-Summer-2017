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

package codeu.chat.client.commandline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;


import codeu.chat.client.core.Context;
import codeu.chat.client.core.ConversationContext;
import codeu.chat.client.core.MessageContext;
import codeu.chat.client.core.UserContext;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.User;
import codeu.chat.util.Tokenizer;

public final class Chat {

	// PANELS
	//
	// We are going to use a stack of panels to track where in the application
	// we are. The command will always be routed to the panel at the top of the
	// stack. When a command wants to go to another panel, it will add a new
	// panel to the top of the stack. When a command wants to go to the previous
	// panel all it needs to do is pop the top panel.
	private final Stack<Panel> panels = new Stack<>();

	public Chat(Context context) {
		this.panels.push(createRootPanel(context));
	}

	// HANDLE COMMAND
	//
	// Take a single line of input and parse a command from it. If the system
	// is willing to take another command, the function will return true. If
	// the system wants to exit, the function will return false.
	//
	public boolean handleCommand(String line) throws IOException {

		final List<String> args = new ArrayList<>();
		final Tokenizer tokenizer = new Tokenizer(line);
		for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
			args.add(token);
		}
		final String command = args.get(0);
		args.remove(0);
		// Because "exit" and "back" are applicable to every panel, handle
		// those commands here to avoid having to implement them for each
		// panel.

		if ("exit".equals(command)) {
			// The user does not want to process any more commands
			return false;
		}

		// Do not allow the root panel to be removed.
		if ("back".equals(command) && panels.size() > 1) {
			panels.pop();
			return true;
		}

		if (panels.peek().handleCommand(command, args)) {
			// the command was handled
			return true;
		}

		// If we get to here it means that the command was not correctly handled
		// so we should let the user know. Still return true as we want to continue
		// processing future commands.
		System.out.println("ERROR: Unsupported command");
		return true;
	}

	// CREATE ROOT PANEL
	//
	// Create a panel for the root of the application. Root in this context means
	// the first panel and the only panel that should always be at the bottom of
	// the panels stack.
	//
	// The root panel is for commands that require no specific contextual information.
	// This is before a user has signed in. Most commands handled by the root panel
	// will be user selection focused.
	//
	private Panel createRootPanel(final Context context) {

		final Panel panel = new Panel();

		// HELP
		//
		// Add a command to print a list of all commands and their description when
		// the user for "help" while on the root panel.
		//
		panel.register("help", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				System.out.println("ROOT MODE");
				System.out.println("  info");
				System.out.println("    Prints out the version of the server & how long the server has been running");
				System.out.println("  u-list");
				System.out.println("    List all users.");
				System.out.println("  u-add <name>");
				System.out.println("    Add a new user with the given name.");
				System.out.println("  u-sign-in <name>");
				System.out.println("    Sign in as the user with the given name.");
				System.out.println("  exit");
				System.out.println("    Exit the program.");
			}
		});

		// info
		//
		//Add a command to return the amount of time the server has been running
		//when the user enters "info" while on the root panel.
		//
		panel.register("info", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				final ServerInfo info = context.getInfo();
				if (info == null) {
					System.out.format("ERROR: Server not responsive");
				} else {
					System.out.println("Server up at: " + info.startTime);
					System.out.println("Running Duration: " + info.upTime());
					System.out.println("Server version: " + info.version);
				}
			}
		});
		// U-LIST (user list)
		//
		// Add a command to print all users registered on the server when the user
		// enters "u-list" while on the root panel.
		//
		panel.register("u-list", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				for (final UserContext user : context.allUsers()) {
					System.out.format(
							"USER %s (UUID:%s)\n",
							user.user.name,
							user.user.id);
				}
			}
		});

		// U-ADD (add user)
		//
		// Add a command to add and sign-in as a new user when the user enters
		// "u-add" while on the root panel.
		//
		panel.register("u-add", new Panel.Command() {

			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String userName = "";
				while (itr.hasNext()) {
					userName += (itr.next().trim() + " ");
				}
				final String name = userName.trim();
				if (name.length() > 0) {
					if (context.create(name) == null) {
						System.out.println("ERROR: Failed to create new user");
					}
				} else {
					System.out.println("ERROR: Missing <username>");
				}
			}
		});

		// U-SIGN-IN (sign in user)
		//
		// Add a command to sign-in as a user when the user enters "u-sign-in"
		// while on the root panel.
		//
		panel.register("u-sign-in", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String userName = "";
				while (itr.hasNext()) {
					userName += (itr.next().trim() + " ");
				}
				final String name = userName.trim();
				if (name.length() > 0) {
					final UserContext user = findUser(name);
					if (user == null) {
						System.out.format("ERROR: Failed to sign in as '%s'\n", name);
					} else {
						panels.push(createUserPanel(user));
					}
				} else {
					System.out.println("ERROR: Missing <username>");
				}
			}
			// Find the first user with the given name and return a user context
			// for that user. If no user is found, the function will return null.
			private UserContext findUser(String name) {
				for (final UserContext user : context.allUsers()) {
					if (user.user.name.equals(name)) {
						return user;
					}
				}
				return null;
			}
		});

		// Now that the panel has all its commands registered, return the panel
		// so that it can be used.
		return panel;
	}

	private Panel createUserPanel(final UserContext user) {

		final Panel panel = new Panel();

        // HELP
        //
        // Add a command that will print a list of all commands and their
        // descriptions when the user enters "help" while on the user panel.
        //
        panel.register("help", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                System.out.println("USER MODE");
                System.out.println("  u-delete");
                System.out.println("    Deletes the current user.");
                System.out.println("  c-list");
                System.out.println("    List all conversations that the current user can interact with.");
                System.out.println("  c-add <default access control (1=open, 0=private)> \"<title>\"");
                System.out.println("    Add a new conversation with the given title and join it as the current user.");
                System.out.println("  c-delete <title>");
                System.out.println("    Delete a conversation you created.");
                System.out.println("  c-join <title>");
                System.out.println("    Join the conversation as the current user.");
                System.out.println("  info");
                System.out.println("    Display all info for the current user.");
                System.out.println("  back");
                System.out.println("    Go back to ROOT MODE.");
                System.out.println("  exit");
                System.out.println("    Exit the program.");
            }
        });

        // U-DEL (delete current user account)
        //
        // Add a command that will delete the current user
        // "u-delete" while on the user home panel.
        //
        panel.register("u-delete", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                if(user.deleteSelf()) {
                    System.out.format(user.user.name + " was successfully deleted.");
                    panels.pop();
                }
                else
                    System.out.format(user.user.name + " was not deleted.");

            }
        });

        // C-LIST (list conversations)
        //
        // Add a command that will print all conversations when the user enters
        // "c-list" while on the user panel.
        //
        panel.register("c-list", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                for (final ConversationContext conversation : user.conversations()) {
                    System.out.format(
                            "CONVERSATION %s (UUID:%s)\n",
                            conversation.conversation.title,
                            conversation.conversation.id);
                }
            }
        });

        // C-ADD (add conversation)
        //
        // Add a command that will create and join a new conversation when the user
        // enters "c-add" while on the user panel.
        //
        panel.register("c-add", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                Iterator<String> itr = args.iterator();
                String conversationName = "";
                String default_control = "";
                while (itr.hasNext()) {
                    String current = itr.next();
                    if(itr.hasNext()){
                        default_control = current.trim();
                    }else{
                        conversationName += (current.trim() + " ");

                    }
                }
                final String name = conversationName.trim();
                if (name.length() > 0) {
                    final ConversationContext conversation = user.start(name, default_control);
                    if (conversation == null) {
                        System.out.println("ERROR: Failed to create new conversation");
                    } else {
                        panels.push(createConversationPanel(conversation));
                    }
                } else {
                    System.out.println("ERROR: Missing <title> or missing default access control string");
                }
            }
        });

        // C-JOIN (join conversation)
        //
        // Add a command that will joing a conversation when the user enters
        // "c-join" while on the user panel.
        //
        panel.register("c-delete", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                Iterator<String> itr = args.iterator();
                String conversationName = "";
                while (itr.hasNext()) {
                    conversationName += (itr.next().trim() + " ");
                }
                final String name = conversationName.trim();

                if (name.length() > 0) {
                    final ConversationContext conversation = find(name);
                    if (conversation == null) {
                        System.out.format("ERROR: No conversation with name '%s'\n", name);
                    }
                    else if (conversation.checkCreator(user.user.id)){
                        conversation.deleteConversation();
                        System.out.format("You have successfully deleted the " + name + " conversation.\n");
                    }
                    else{
                        System.out.format("Denied Access: not a creator\n");
                    }
                } else {
                    System.out.println("ERROR: Missing <title>");
                }
            }

            // Find the first conversation with the given name and return its context.
            // If no conversation has the given name, this will return null.
            private ConversationContext find(String title) {
                for (final ConversationContext conversation : user.conversations()) {
                    if (title.equals(conversation.conversation.title)) {
                        return conversation;
                    }
                }
                return null;
            }
        });

        // C-JOIN (join conversation)
        //
        // Add a command that will joing a conversation when the user enters
        // "c-join" while on the user panel.
        //
        panel.register("c-join", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                Iterator<String> itr = args.iterator();
                String conversationName = "";
                while (itr.hasNext()) {
                    conversationName += (itr.next().trim() + " ");
                }
                final String name = conversationName.trim();

                if (name.length() > 0) {
                    final ConversationContext conversation = find(name);
                    if (conversation == null) {
                        System.out.format("ERROR: No conversation with name '%s'\n", name);
                    }
                    else if (conversation.checkRemoved(user.user.id) ||
                            (conversation.conversation.default_control == 0 && !conversation.checkMember(user.user.id))){
                    	System.out.format("Not allowed to join");
                    }
                    else {
                        panels.push(createConversationPanel(conversation));
                    }
                } else {
                    System.out.println("ERROR: Missing <title>");
                }
            }

            // Find the first conversation with the given name and return its context.
            // If no conversation has the given name, this will return null.
            private ConversationContext find(String title) {
                for (final ConversationContext conversation : user.conversations()) {
                    if (title.equals(conversation.conversation.title)) {
                        return conversation;
                    }
                }
                return null;
            }
        });

        // INFO
        //
        // Add a command that will print info about the current context when the
        // user enters "info" while on the user panel.
        //
        panel.register("info", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                System.out.println("User Info:");
                System.out.format("  Name : %s\n", user.user.name);
                System.out.format("  Id   : UUID:%s\n", user.user.id);
            }
        });

        // Now that the panel has all its commands registered, return the panel
        // so that it can be used.
        return panel;
    }

	private Panel createConversationPanel(final ConversationContext conversation) {

		final Panel panel = new Panel();

		// HELP
		//
		// Add a command that will print all the commands and their descriptions
		// when the user enters "help" while on the conversation panel.
		//
		panel.register("help", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				System.out.println("USER MODE");
				if(conversation.checkCreator(conversation.user.id)){
					System.out.println("  add-owner <user>");
					System.out.println("    Add the user as an owner of the conversation.");
					System.out.println("  remove-owner <user>");
					System.out.println("    Remove the user as an owner of the conversation (they remain a member).");
					System.out.println("  change-default <default control>");
					System.out.println("    Changes the default access permissions of the current conversation as the creator.");
				}
				if(conversation.checkOwner(conversation.user.id)){
					System.out.println("  add-member <user>");
					System.out.println("    Add the user as a member of the conversation.");
					System.out.println("  remove-member <user>");
					System.out.println("    Remove the user as a member of the conversation.");
				}
				System.out.println("  m-list");
				System.out.println("    List all messages in the current conversation.");
				System.out.println("  m-add <message>");
				System.out.println("    Add a new message to the current conversation as the current user.");
				System.out.println("  info");
				System.out.println("    Display all info about the current conversation.");
				System.out.println("  back");
				System.out.println("    Go back to USER MODE.");
				System.out.println("  exit");
				System.out.println("    Exit the program.");
			}
		});

		// ADD-OWNER
		//
		// Add a user as an owner in the conversation.
		//
		panel.register("add-owner", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String username = "";
				while (itr.hasNext()) {
					username += (" " + itr.next().trim());
				}
				if (username.length() > 0) {
					conversation.addOwner(username);
				} else {
					System.out.println("ERROR: Must enter valid username");
				}

			}
		});

		// REMOVE-OWNER
		//
		// Remove a user as an owner in the conversation.
		// (They remain a member in the conversation.)
		//
		panel.register("remove-owner", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String username = "";
				while (itr.hasNext()) {
					username += (" " + itr.next().trim());
				}
				if (username.length() > 0) {
					conversation.removeOwner(username);
				} else {
					System.out.println("ERROR: Must enter valid username");
				}

			}
		});

		// ADD-MEMBER
		//
		// Add a user as an owner in the conversation.
		//
		panel.register("add-member", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String username = "";
				while (itr.hasNext()) {
					username += (" " + itr.next().trim());
				}
				if (username.length() > 0) {
					conversation.addMember(username);
				} else {
					System.out.println("ERROR: Must enter valid username");
				}

			}
		});

		// REMOVE-MEMBER
		//
		// Remove a user as an owner in the conversation.
		// (They remain a member in the conversation.)
		//
		panel.register("remove-member", new Panel.Command() {
			@Override
			public void invoke(List<String> args) {
				Iterator<String> itr = args.iterator();
				String username = "";
				while (itr.hasNext()) {
					username += (" " + itr.next().trim());
				}
				if (username.length() > 0) {
					conversation.removeMember(username);
				} else {
					System.out.println("ERROR: Must enter valid username");
				}

			}
		});

		// M-LIST (list messages)
        //
        // Add a command to print all messages in the current conversation when the
        // user enters "m-list" while on the conversation panel.
        //
        panel.register("m-list", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
            	if(conversation.checkMember(conversation.user.id)){
            		System.out.println("--- start of conversation ---");
            		for (MessageContext message = conversation.firstMessage();
            				message != null;
            				message = message.next()) {
            			System.out.println();
            			System.out.format("USER : %s\n", message.message.author);
            			System.out.format("SENT : %s\n", message.message.creation);
            			System.out.println();
            			System.out.println(message.message.content);
            			System.out.println();
            		}
            		System.out.println("---  end of conversation  ---");
            	}
            	else{
            		System.out.println("Denied access");
            	}
            }
        });

        // M-ADD (add message)
        //
        // Add a command to add a new message to the current conversation when the
        // user enters "m-add" while on the conversation panel.
        //
        panel.register("m-add", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
            	if(conversation.checkMember(conversation.user.id)){
            		Iterator<String> itr = args.iterator();
            		String message = "";
            		while (itr.hasNext()) {
            			message += (" " + itr.next().trim());
            		}
            		if (message.length() > 0) {
            			conversation.add(message);
            		} else {
            			System.out.println("ERROR: Messages must contain text");
            		}
            	}
            	else{
            		System.out.println("Denied access");
            	}
            }
        });

        // change-default (changes default access permissions)
        //
        // For the creator of the conversation
        // this option allows a change in the default access permissions of the conversation
        //
        panel.register("change-default", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                if(conversation.checkCreator(conversation.user.id)){
                    Iterator<String> itr = args.iterator();
                    String default_control = itr.next().trim();
                    if (default_control.length() > 0) {
                        byte client_default = conversation.conversation.default_control;
                        byte server_default = conversation.getDefault();
                        if(Byte.parseByte(default_control) != server_default){
                            conversation.changeDefault(default_control);
                            System.out.println("The conversation default is now "+(Byte.parseByte(default_control)>=1?"public.":"private."));
                        }else{
                            System.out.println("The conversation default is still "+((server_default>=1)?"public.":"private"));
                        }
                    } else {
                        System.out.println("ERROR: default control byte must be specified");
                    }
                }
                else{
                    System.out.println("Denied access");
                }
            }
        });

        // INFO
        //
        // Add a command to print info about the current conversation when the user
        // enters "info" while on the conversation panel.
        //
        panel.register("info", new Panel.Command() {
            @Override
            public void invoke(List<String> args) {
                System.out.println("Conversation Info:");
                System.out.format("  Title : %s\n", conversation.conversation.title);
                System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
                System.out.format("  Owner : %s\n", conversation.conversation.owner);
                System.out.format("  Default-access: %s\n", conversation.conversation.default_control==0 ? "private": "public");
            }
        });

        // Now that the panel has all its commands registered, return the panel
        // so that it can be used.
        return panel;
    }
}

