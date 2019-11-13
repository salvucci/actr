package actr.model;

public class ParseError {
	private final String text;
	private final int offset;
	private final int line;
	private final boolean fatal;

	ParseError(String text, int offset, int line, boolean fatal) {
		this.text = text;
		this.offset = offset;
		this.line = line;
		this.fatal = fatal;
	}

	public boolean isFatal() {
		return fatal;
	}

	public String getText() {
		return text;
	}

	public int getOffset() {
		return offset;
	}

	public int getLine() {
		return line;
	}
}
