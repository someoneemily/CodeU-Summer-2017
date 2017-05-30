package codeu.chat.util;

import java.io.IOException;

public final class Tokenizer {
	
	private StringBuilder token;
	private String source;
	private int at;
	
	public Tokenizer(String source) {
	}
	
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
	
	private String readWithNoQuotes() throws IOException {
		token.setLength(0); // clear the token
		while (remaining() > 0 && !Character.isWhitespace(peek())) {
			token.append(read());
		}
		return token.toString();
	}
	
	private String readWithQuotes() throws IOException {
		token.setLength(0);; // clear the token
		if (read() != '"') {
			token.append(read());
		}
		read(); // read closing quote that allows us to exit the loop
		return token.toString();
	}
}


