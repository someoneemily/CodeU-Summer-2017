package codeu.chat.util;

import java.io.IOException;

public final class Tokenizer {
	
	private StringBuilder token;
	private String source;
	private int at;

	public Tokenizer(String source) {
		this.token = new StringBuilder();
		this.source = source;
		this.at = 0;
	}
		
	// NEXT
    //
    // Retrieves next string to read as a token.
    //
	public String next() throws IOException {
		// skip leading whitespaces
		while (remaining() > 0 && Character.isWhitespace(peek())) {
			// ignore result if whitespace
			read();
		}
		if (remaining() <= 0) {
			return null;
		} else if (peek() == '"') {
			return readWithQuotes(); // read a token surrounded by quotes
		} else {
			return readWithNoQuotes(); // read a token not surrounded by quotes
		}
	}
	
	// REMAINING
    //
    // Returns remaining number of characters in source string
    // that have not been read yet while parsing tokens.
	//
	private int remaining() {
		return source.length() - at;
	}
	
	// PEEK
    //
    // Return character at current position in source string.
    //
	private char peek() throws IOException {
		if (at < source.length()) {
			return source.charAt(at);
		} else {
			throw new IndexOutOfBoundsException("This is outside of the source string.");
		}
	}	
	
	// READ
    //
    // Read the character at current position in source string
    // and advance to the next position.
	//
	private char read() throws IOException {
		final char c = peek();
		at += 1;
		return c;
	}	
	
	// READWITHNOQUOTES
    //
    // Read source string without quotation marks as token.
    //
	private String readWithNoQuotes() throws IOException {
		token.setLength(0); // clear the token
		while (remaining() > 0 && !Character.isWhitespace(peek())) {
			token.append(read());
		}
		return token.toString();
	}
	
	// READWITHQUOTES
    //
    // Read source string with quotation marks as token.
    //
	private String readWithQuotes() throws IOException {
		token.setLength(0); // clear the token
		if (read() != '"') {
			throw new IOException("Strings must start with opening quotes!");
		}
		while(peek() != '"') {
			token.append(read());
		}
		read(); // read closing quote that allows us to exit the loop
		return token.toString();
	}
}