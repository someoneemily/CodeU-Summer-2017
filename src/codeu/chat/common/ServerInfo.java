package codeu.chat.common;

import java.util.*;
import codeu.chat.util.Time;

public final class ServerInfo{
    public final Time startTime;
    public ServerInfo(){
        this.startTime = Time.now();
    }
    public ServerInfo(Time startTime){
        this.startTime = startTime;
    }
    public long upTime(){
        //todo (emily): Why not format it to seconds here?
        // It'll also help to call it upTimeInSec or upTimeInMS
        return Time.now().inMs()-(startTime).inMs();
    }
}
