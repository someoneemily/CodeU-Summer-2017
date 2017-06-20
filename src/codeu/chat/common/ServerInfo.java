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
	public ServerInfo(Uuid version, Time startTime){
		this.version = version;
		this.startTime = startTime;
	}
    public String upTime(){
		long seconds = (Time.now().inMs()-(startTime).inMs())/1000;
		long hours = seconds/3600;
		long min = seconds%3600/60;
		long sec = seconds%3600%60;
		return String.format("%02d:%02d:%02d", hours, min, sec);
    }
}
