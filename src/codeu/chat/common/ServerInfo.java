package codeu.chat.common;

import java.io.IOException;
import java.util.*;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class ServerInfo{
    public final Time startTime;
	private final static String SERVER_VERSION = "1.0.0";
	public final Uuid version;

	public ServerInfo(){
		Uuid parsed;
		try {
			parsed = Uuid.parse(SERVER_VERSION);
		} catch (IOException e) {

			System.out.println(e.getMessage());
			parsed = null;
		}
		this.version = parsed;
        this.startTime = Time.now();
		
	}
	
	/*
    public ServerInfo(Time startTime){
        this.startTime = startTime;
    }
	public ServerInfo(Uuid version){
		this.version = version;
	}
	*/
	public ServerInfo(Uuid version, Time startTime){
		this.version = version;
		this.startTime = startTime;
	}
    public long upTime(){
        //todo (emily): Why not format it to seconds here?
        // It'll also help to call it upTimeInSec or upTimeInMS
        return Time.now().inMs()-(startTime).inMs();
    }
}
