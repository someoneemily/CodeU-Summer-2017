package codeu.chat.util;

import java.io.IOException;

public final class Tokenizer {
	
	private StringBuilder token;
	private String source;
	private int at;
	
	public Tokenizer(String source) {
		
		
		
	}
	
	public String next() throws IOException {
		return null;
		
	}
	
	private int remaining() {
		return source.length() - at;
	}
	
	private char peek() throws IOException {
		if (at < source.length()) {
			return source.charAt(at);
		} else {
			throw new IOException();
		}
	}
	
	private char read() throws IOException {
		final char c = peek();
		at += 1;
		return c;
	}
}


