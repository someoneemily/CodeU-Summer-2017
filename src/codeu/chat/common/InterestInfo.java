package codeu.chat.common;

import java.io.*;
import java.util.*;

public class InterestInfo {

    public final ConversationHeader conversation;
    public final Message message;
    public final User user;

    public InterestInfo (ConversationHeader conversation, User user){

        this.conversation = conversation;
        this.message = null;
        this.user = user;
    }

    public InterestInfo (ConversationHeader conversation, Message message, User user){

        this.conversation = conversation;
        this.message = message;
        this.user = user;

    }

}