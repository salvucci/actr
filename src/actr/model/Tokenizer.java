package actr.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

/**
 * A tokenizer that breaks an ACT-R model file into relevant tokens for the
 * parser.
 * 
 * @author Dario Salvucci
 */
class Tokenizer {
	private final Reader reader;
	private int c = 0;
	private int offset = 0;
	private int line = 1;
	private int lastOffset = 0, lastLine = 1;
	private String token = "";
	private final List<String> putbacks = new ArrayList<>();
	private final Map<String, String> variables = new HashMap<>();

	boolean caseSensitive = false;

	Tokenizer(File file) throws FileNotFoundException {
		reader = new FileReader(file);
		readChar();
		while (c != -1 && Character.isWhitespace(c))
			readChar();
		next();
	}

	Tokenizer(URL url) throws IOException {
		reader = new BufferedReader(new InputStreamReader(url.openStream()));
		readChar();
		while (c != -1 && Character.isWhitespace(c))
			readChar();
		next();
	}

	Tokenizer(String s) {
		reader = new StringReader(s);
		readChar();
		while (c != -1 && Character.isWhitespace(c))
			readChar();
		next();
	}

	boolean hasMoreTokens() {
		return (c != -1) || !putbacks.isEmpty();
	}

	final String token() {
		return token;
	}

	boolean isLetterToken() {
		return !token.isEmpty() && Character.isLetter(token.charAt(0));
	}

	int getLine() {
		return line;
	}

	int getOffset() {
		return offset;
	}

	int getLastLine() {
		return lastLine;
	}

	int getLastOffset() {
		return lastOffset;
	}

	void readChar() {
		try {
			c = reader.read();
		} catch (IOException exc) {
			System.err.println("IOException: " + exc.getMessage());
		}
		offset++;
		if (c == '\n' || c == '\r')
			line++;
	}

	static boolean isSpecial(int c2) {
		return c2 == '(' || c2 == ')';
	}

	void next() {
		if (!hasMoreTokens()) {
			token = "";
			return;
		}

		lastOffset = offset;
		lastLine = line;

		if (!putbacks.isEmpty()) {
			token = putbacks.remove(0);
			return;
		}

		StringWriter sr = new StringWriter();

		while (c != -1 && (c == ';' || c == '#')) {
			if (c == ';') {
				while (c != -1 && c != '\n' && c != '\r')
					readChar();
			} else {
				readChar(); // '#'
				if (c != -1)
					readChar(); // '|'
				while (c != -1 && c != '|')
					readChar();
				if (c != -1)
					readChar(); // '|'
				if (c != -1)
					readChar(); // '#'
			}
			while (c != -1 && Character.isWhitespace(c))
				readChar();
		}

		if (isSpecial(c)) {
			sr.write(c);
			readChar();
		} else if (c == '"') {
			sr.write(c);
			readChar();
			while (c != -1 && c != '"') {
				sr.write(c);
				readChar();
			}
			sr.write(c);
			readChar();
		} else {
			while (c != -1 && !Character.isWhitespace(c) && !isSpecial(c)) {
				sr.write(c);
				readChar();
			}
		}
		while (c != -1 && Character.isWhitespace(c))
			readChar();

		token = sr.toString();

		if (!caseSensitive && !token.startsWith("\""))
			token = token.toLowerCase();

		String value = variables.get(token);
		if (value != null)
			token = value;

		// System.out.println ("-" + token + "-");
	}

	void pushBack(String old) {
		putbacks.add(token);
		token = old;
	}

	void addVariable(String variable, String value) {
		variables.put(variable, value);
	}
}