package actr.model;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A symbol within the system, roughly represented as a string but allowing "=="
 * as a fast test for equality.
 * <p>
 * This class maintains a hash table associating strings to symbols. When a user
 * accesses a symbol using the <tt>Symbol.get()</tt> methods (e.g.,
 * <tt>Symbol.get("hello")</tt> or <tt>Symbol.get(3.5)</tt>), the methods return
 * the symbol from the hash table if it exists, or create and return a new
 * symbol if it does not yet exist. Symbols can be compared for equality
 * efficiently using the "==" operator (rather than performing a string
 * comparison of the symbol names).
 * <p>
 * The class also provides a large set of pervasive symbols for heavily used
 * symbols within the ACT-R system (e.g., the names of buffers and buffer
 * states). The variables are simply shorthand alternatives to the longer forms
 * that use the <tt>Symbol.get()</tt> methods; for example, <tt>Symbol.isa</tt>
 * is equivalent to <tt>Symbol.get("isa")</tt>.
 * 
 * @author Dario Salvucci
 */
public class Symbol {
	private String string;
	private static Map<String, Symbol> hashmap = new HashMap<String, Symbol>();
	private static long unique = 1;
	private static Set<Symbol> pervasives = new HashSet<Symbol>();
	private static DecimalFormat df = new DecimalFormat("#0.####");

	/** Pervasive symbol for the string <tt>"t"</tt>. */
	public static final Symbol t = Symbol.createPervasiveSymbol("t");

	/** Pervasive symbol for the string <tt>"nil"</tt>. */
	public static final Symbol nil = Symbol.createPervasiveSymbol("nil");

	/** Pervasive symbol for the string <tt>"isa"</tt>. */
	public static final Symbol isa = Symbol.createPervasiveSymbol("isa");

	/** Pervasive symbol for the string <tt>"goal"</tt>. */
	public static final Symbol goal = Symbol.createPervasiveSymbol("goal");

	/** Pervasive symbol for the string <tt>"?goal"</tt>. */
	public static final Symbol goalState = Symbol
			.createPervasiveSymbol("?goal");

	/** Pervasive symbol for the string <tt>"retrieval"</tt>. */
	public static final Symbol retrieval = Symbol
			.createPervasiveSymbol("retrieval");

	/** Pervasive symbol for the string <tt>"?retrieval"</tt>. */
	public static final Symbol retrievalState = Symbol
			.createPervasiveSymbol("?retrieval");

	/** Pervasive symbol for the string <tt>"visual-location"</tt>. */
	public static final Symbol visloc = Symbol
			.createPervasiveSymbol("visual-location");

	/** Pervasive symbol for the string <tt>"?visual-location"</tt>. */
	public static final Symbol vislocState = Symbol
			.createPervasiveSymbol("?visual-location");

	/** Pervasive symbol for the string <tt>"visual"</tt>. */
	public static final Symbol visual = Symbol.createPervasiveSymbol("visual");

	/** Pervasive symbol for the string <tt>"?visual"</tt>. */
	public static final Symbol visualState = Symbol
			.createPervasiveSymbol("?visual");

	/** Pervasive symbol for the string <tt>"aural-location"</tt>. */
	public static final Symbol aurloc = Symbol
			.createPervasiveSymbol("aural-location");

	/** Pervasive symbol for the string <tt>"?aural-location"</tt>. */
	public static final Symbol aurlocState = Symbol
			.createPervasiveSymbol("?aural-location");

	/** Pervasive symbol for the string <tt>"aural"</tt>. */
	public static final Symbol aural = Symbol.createPervasiveSymbol("aural");

	/** Pervasive symbol for the string <tt>"?aural"</tt>. */
	public static final Symbol auralState = Symbol
			.createPervasiveSymbol("?aural");

	/** Pervasive symbol for the string <tt>"manual"</tt>. */
	public static final Symbol manual = Symbol.createPervasiveSymbol("manual");

	/** Pervasive symbol for the string <tt>"?manual"</tt>. */
	public static final Symbol manualState = Symbol
			.createPervasiveSymbol("?manual");

	/** Pervasive symbol for the string <tt>"vocal"</tt>. */
	public static final Symbol vocal = Symbol.createPervasiveSymbol("vocal");

	/** Pervasive symbol for the string <tt>"?vocal"</tt>. */
	public static final Symbol vocalState = Symbol
			.createPervasiveSymbol("?vocal");

	/** Pervasive symbol for the string <tt>"imaginal"</tt>. */
	public static final Symbol imaginal = Symbol
			.createPervasiveSymbol("imaginal");

	/** Pervasive symbol for the string <tt>"?imaginal"</tt>. */
	public static final Symbol imaginalState = Symbol
			.createPervasiveSymbol("?imaginal");

	/** Pervasive symbol for the string <tt>"temporal"</tt>. */
	public static final Symbol temporal = Symbol
			.createPervasiveSymbol("temporal");

	/** Pervasive symbol for the string <tt>"?temporal"</tt>. */
	public static final Symbol temporalState = Symbol
			.createPervasiveSymbol("?temporal");

	/** Pervasive symbol for the string <tt>"buffer"</tt>. */
	public static final Symbol buffer = Symbol.createPervasiveSymbol("buffer");

	/** Pervasive symbol for the string <tt>"state"</tt>. */
	public static final Symbol state = Symbol.createPervasiveSymbol("state");

	/** Pervasive symbol for the string <tt>"preparation"</tt>. */
	public static final Symbol preparation = Symbol
			.createPervasiveSymbol("preparation");

	/** Pervasive symbol for the string <tt>"processor"</tt>. */
	public static final Symbol processor = Symbol
			.createPervasiveSymbol("processor");

	/** Pervasive symbol for the string <tt>"execution"</tt>. */
	public static final Symbol execution = Symbol
			.createPervasiveSymbol("execution");

	/** Pervasive symbol for the string <tt>"free"</tt>. */
	public static final Symbol free = Symbol.createPervasiveSymbol("free");

	/** Pervasive symbol for the string <tt>"busy"</tt>. */
	public static final Symbol busy = Symbol.createPervasiveSymbol("busy");

	/** Pervasive symbol for the string <tt>"empty"</tt>. */
	public static final Symbol empty = Symbol.createPervasiveSymbol("empty");

	/** Pervasive symbol for the string <tt>"full"</tt>. */
	public static final Symbol full = Symbol.createPervasiveSymbol("full");

	/** Pervasive symbol for the string <tt>"requested"</tt>. */
	public static final Symbol requested = Symbol
			.createPervasiveSymbol("requested");

	/** Pervasive symbol for the string <tt>"unrequested"</tt>. */
	public static final Symbol unrequested = Symbol
			.createPervasiveSymbol("unrequested");

	/** Pervasive symbol for the string <tt>"error"</tt>. */
	public static final Symbol error = Symbol.createPervasiveSymbol("error");

	/** Pervasive symbol for the string <tt>":recently-retrieved"</tt>. */
	public static final Symbol recentlyRetrieved = Symbol
			.createPervasiveSymbol(":recently-retrieved");

	/** Pervasive symbol for the string <tt>"kind"</tt>. */
	public static final Symbol kind = Symbol.createPervasiveSymbol("kind");

	/** Pervasive symbol for the string <tt>"screen-x"</tt>. */
	public static final Symbol screenx = Symbol
			.createPervasiveSymbol("screen-x");

	/** Pervasive symbol for the string <tt>"screen-y"</tt>. */
	public static final Symbol screeny = Symbol
			.createPervasiveSymbol("screen-y");

	/** Pervasive symbol for the string <tt>"screen-pos"</tt>. */
	public static final Symbol screenpos = Symbol
			.createPervasiveSymbol("screen-pos");

	/** Pervasive symbol for the string <tt>"value"</tt>. */
	public static final Symbol value = Symbol.createPervasiveSymbol("value");

	/** Pervasive symbol for the string <tt>"width"</tt>. */
	public static final Symbol width = Symbol.createPervasiveSymbol("width");

	/** Pervasive symbol for the string <tt>"height"</tt>. */
	public static final Symbol height = Symbol.createPervasiveSymbol("height");

	/** Pervasive symbol for the string <tt>"distance"</tt>. */
	public static final Symbol distance = Symbol
			.createPervasiveSymbol("distance");

	/** Pervasive symbol for the string <tt>"lowest"</tt>. */
	public static final Symbol lowest = Symbol.createPervasiveSymbol("lowest");

	/** Pervasive symbol for the string <tt>"highest"</tt>. */
	public static final Symbol highest = Symbol
			.createPervasiveSymbol("highest");

	/** Pervasive symbol for the string <tt>"current"</tt>. */
	public static final Symbol current = Symbol
			.createPervasiveSymbol("current");

	/** Pervasive symbol for the string <tt>"tone"</tt>. */
	public static final Symbol tone = Symbol.createPervasiveSymbol("tone");

	/** Pervasive symbol for the string <tt>"word"</tt>. */
	public static final Symbol word = Symbol.createPervasiveSymbol("word");

	/** Pervasive symbol for the string <tt>"digit"</tt>. */
	public static final Symbol digit = Symbol.createPervasiveSymbol("digit");

	/** Pervasive symbol for the string <tt>"location"</tt>. */
	public static final Symbol location = Symbol
			.createPervasiveSymbol("location");

	/** Pervasive symbol for the string <tt>"event"</tt>. */
	public static final Symbol event = Symbol.createPervasiveSymbol("event");

	/** Pervasive symbol for the string <tt>"content"</tt>. */
	public static final Symbol content = Symbol
			.createPervasiveSymbol("content");

	/** Pervasive symbol for the string <tt>"hand"</tt>. */
	public static final Symbol hand = Symbol.createPervasiveSymbol("hand");

	/** Pervasive symbol for the string <tt>"finger"</tt>. */
	public static final Symbol finger = Symbol.createPervasiveSymbol("finger");

	/** Pervasive symbol for the string <tt>"left"</tt>. */
	public static final Symbol left = Symbol.createPervasiveSymbol("left");

	/** Pervasive symbol for the string <tt>"right"</tt>. */
	public static final Symbol right = Symbol.createPervasiveSymbol("right");

	/** Pervasive symbol for the string <tt>"where"</tt>. */
	public static final Symbol where = Symbol.createPervasiveSymbol("where");

	/** Pervasive symbol for the string <tt>"keyboard"</tt>. */
	public static final Symbol keyboard = Symbol
			.createPervasiveSymbol("keyboard");

	/** Pervasive symbol for the string <tt>"mouse"</tt>. */
	public static final Symbol mouse = Symbol.createPervasiveSymbol("mouse");

	/** Pervasive symbol for the string <tt>"time"</tt>. */
	public static final Symbol time = Symbol.createPervasiveSymbol("time");

	/** Pervasive symbol for the string <tt>"ticks"</tt>. */
	public static final Symbol ticks = Symbol.createPervasiveSymbol("ticks");

	private Symbol(String string) {
		this.string = string;
	}

	/**
	 * Gets the symbol for the given string.
	 * 
	 * @param s
	 *            the string
	 * @return the symbol for the string, or <tt>Symbol.nil</tt> if the string
	 *         is null
	 */
	public static Symbol get(String s) {
		if (s == null)
			return nil;
		Symbol sym = hashmap.get(s);
		if (sym == null) {
			sym = new Symbol(s);
			hashmap.put(s, sym);
		}
		return sym;
	}

	private static Symbol createPervasiveSymbol(String s) {
		Symbol sym = get(s);
		pervasives.add(sym);
		return sym;
	}

	/**
	 * Gets the symbol for the given integer.
	 * 
	 * @param x
	 *            the integer
	 * @return the associated symbol
	 */
	public static Symbol get(int x) {
		return get(Integer.toString(x));
	}

	/**
	 * Gets the symbol for the given double. To limit the scope of possible
	 * symbols, the double is represented with a maximum of four decimal places.
	 * 
	 * @param x
	 *            the double
	 * @return the associated symbol
	 */
	public static Symbol get(double x) {
		return get(df.format(x));
	}

	/**
	 * Gets the symbol for the given boolean.
	 * 
	 * @param b
	 *            the boolean
	 * @return the associated symbol
	 */
	public static Symbol get(boolean b) {
		if (b)
			return t;
		else
			return nil;
	}

	static Symbol getUnique(String s) {
		if (s == null)
			s = "nil";
		String ustring;
		while (hashmap.get(ustring = uniquify(s)) != null)
			;
		Symbol sym = new Symbol(ustring);
		hashmap.put(ustring, sym);
		return sym;
	}

	private static String uniquify(String s) {
		int pos = s.lastIndexOf('~');
		String base = (pos >= 0) ? s.substring(0, pos) : s;
		return base + "~" + (unique++);
	}

	/**
	 * Gets the symbol's string name.
	 * 
	 * @return the symbol's string
	 */
	public String getString() {
		return string;
	}

	/**
	 * Checks whether the symbol is a variable that begins with <tt>'='</tt>.
	 * 
	 * @return <tt>true</tt> if the symbol represents a variable, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isVariable() {
		return string.length() > 1 && string.charAt(0) == '=';
	}

	/**
	 * Checks whether the symbol represents a number.
	 * 
	 * @return <tt>true</tt> if the symbol represents a number, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isNumber() {
		try {
			toDouble();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks whether the symbol represents a string that begins with a quote (
	 * <tt>'"'</tt>).
	 * 
	 * @return <tt>true</tt> if the symbol represents a string, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isString() {
		return string.length() > 0 && string.charAt(0) == '"';
	}

	/**
	 * Checks whether the symbol represents a buffer state that begins with a
	 * question mark (<tt>'?'</tt>).
	 * 
	 * @return <tt>true</tt> if the symbol represents a buffer state, or
	 *         <tt>false</tt> otherwise
	 */
	public boolean isState() {
		return string.length() > 0 && string.charAt(0) == '?';
	}

	/**
	 * Gets the double value of the symbol's string.
	 * 
	 * @return the double value
	 */
	public double toDouble() {
		return Double.valueOf(string).doubleValue();
	}

	/**
	 * Gets the integer value of the symbol's string.
	 * 
	 * @return the integer value
	 */
	public int toInt() {
		return (int) Math.round(toDouble());
	}

	/**
	 * Gets the boolean value of the symbol's string.
	 * 
	 * @return the boolean value
	 */
	public boolean toBoolean() {
		return (this != nil);
	}

	static void reset() {
		hashmap = new HashMap<String, Symbol>();
		Iterator<Symbol> it = pervasives.iterator();
		while (it.hasNext()) {
			Symbol sym = it.next();
			hashmap.put(sym.string, sym);
		}
		unique = pervasives.size() + 1;
	}

	/**
	 * Gets a string representation of the symbol as its name string.
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return string;
	}
}
