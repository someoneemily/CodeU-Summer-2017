package codeu.chat.common;

import java.io.IOException;

import codeu.chat.util.Uuid;

public final class ServerInfo {

	private final static String SERVER_VERSION = "1.0.0";
	
	private final Uuid version;
	public ServerInfo() {
		Uuid parsed;
		try {
			parsed = Uuid.parse(SERVER_VERSION);
		} catch (IOException e) {
			
			System.out.println(e.getMessage());
			parsed = null;
		}
		
		this.version = parsed;
	}
	
	public ServerInfo(Uuid version){
		this.version = version;
	}
}
