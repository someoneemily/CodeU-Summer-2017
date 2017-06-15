package codeu.chat.util;

import java.io.IOException;

public final class Tokenizer {
	
	private StringBuilder token;
	private String source;
	private int at;
	//todo(Weizhen): What is this costructor doing?
	public Tokenizer(String source) {
	}
	//todo(Weizhen): please a comment describing the method's purpose
	public String next() throws IOException {
		// skip leading whitespaces
		while (remaining() > 0 && Character.isWhitespace(peek())) {
			// ignore result if whitespace
			read();
		}
		if (remaining() <= 0) {
			return null;
		} else if (peek() == '"') {
			readWithQuotes(); // read a token surrounded by quotes
		} else {
			readWithNoQuotes(); // read a token not surrounded by quotes
		}
		
		return null;
		
	}
	//todo(Weizhen): please a comment describing the method's purpose
	private int remaining() {
		return source.length() - at;
	}
	//todo(Weizhen): please a comment describing the method's purpose
	private char peek() throws IOException {
		if (at < source.length()) {
			return source.charAt(at);
		} else {
			throw new IOException();
			//todo(Weizhen): Please add content to the exeption
		}
	}
	//todo(Weizhen): please a comment describing the method's purpose
	private char read() throws IOException {
		final char c = peek();
		at += 1;
		return c;
	}
	//todo(Weizhen): please a comment describing the method's purpose
	private String readWithNoQuotes() throws IOException {
		token.setLength(0); // clear the token
		while (remaining() > 0 && !Character.isWhitespace(peek())) {
			token.append(read());
		}
		return token.toString();
	}
	//todo(Weizhen): please a comment describing the method's purpose

	private String readWithQuotes() throws IOException {
		token.setLength(0); // clear the token
		if (read() != '"') {
			token.append(read());
		}
		//todo(Weizhen): Some things are missing here - please look at the bottom of page 9
		read(); // read closing quote that allows us to exit the loop
		return token.toString();
	}
}


