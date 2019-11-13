package actr.model;

import java.util.Iterator;
import java.util.Random;

/**
 * Utility code with various utility variables and methods.
 * 
 * @author Dario Salvucci
 */
public enum Utilities {
	;
	static final Random random = new Random();

	private static long currentID = 0;

	static long getUniqueID() {
		return (++currentID);
	}

	/**
	 * Gets a noise value sampled from the standard ACT-R noise distribution.
	 * The normal approximation used here is derived from the canonical ACT-R
	 * code.
	 * 
	 * @param s
	 *            the s value of the normal distribution
	 * @return a value sampled from this distribution
	 */
	public static double getNoise(double s) {
		if (s == 0)
			return 0;
		// tutorial says logistic distribution, but act-r code has normal
		// approximation
		// normal is used below, derived from act-r code
		double p = Math.max(0.0001, Math.min(random.nextDouble(), 0.9999));
		return s * Math.log((1.0 - p) / p);
	}

	/**
	 * Converts a value from degrees to radians.
	 * 
	 * @param degrees
	 *            the value in degrees
	 * @return the value in radians
	 */
	public static double deg2rad(double degrees) {
		return degrees * (Math.PI / 180.0);
	}

	/**
	 * Converts a value from radians to degrees.
	 * 
	 * @param radians
	 *            the value in radians
	 * @return the value in degrees
	 */
	public static double rad2deg(double radians) {
		return radians * (180.0 / Math.PI);
	}

	/** The viewing distance to the screen, in inches. */
	public static final double viewingDistance = 30; // from Byrne-IJHCS01 // not 15

	/** The value of pixels per inch on the standard display. */
	public static final double pixelsPerInch = 72;

	/**
	 * Converts a value from pixels to visual angle on the standard display.
	 * 
	 * @param pixels
	 *            the value in pixels
	 * @return the visual angle in degrees
	 */
	public static double pixels2angle(double pixels) {
		return rad2deg(Math.atan2(pixels / pixelsPerInch, viewingDistance));
	}

	/**
	 * Converts a value from visual angle to pixels on the standard display.
	 * 
	 * @param angle
	 *            the visual angle in degrees
	 * @return the value in pixels
	 */
	public static double angle2pixels(double angle) {
		return viewingDistance * Math.tan(deg2rad(angle)) * pixelsPerInch;
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	static double evalCompute(Iterator<String> it) throws Exception {
		String test = it.next();
		if (test.equals(")"))
			return -9999;
		if (!test.equals("("))
			return Double.parseDouble(test);

		String operator = it.next();
		double result = evalCompute(it);
		if (result == -9999)
			throw new Exception();
		if (operator.equals("abs"))
			return Math.abs(result);
		double last = evalCompute(it);
		if (last == -9999)
			throw new Exception();
		while (last != -9999) {
			switch (operator) {
				case "+":
					result += last;
					break;
				case "-":
					result -= last;
					break;
				case "*":
					result *= last;
					break;
				case "/":
					result /= last;
					break;
				case "my/":
					result = (last == 0) ? 0 : result / last;
					break;
				case "min":
					result = Math.min(last, result);
					break;
				case "max":
					result = Math.max(last, result);
					break;
				default:
					throw new Exception();
			}
			last = evalCompute(it);
		}
		return result;
	}

	static boolean evalComputeCondition(Iterator<String> it) throws Exception {
		it.next(); // "("
		String operator = it.next();
		double r1 = evalCompute(it);
		double r2 = evalCompute(it);
		// it.next(); // ")"
		switch (operator) {
			case "=":
				return (r1 == r2);
			case "<>":
				return (r1 != r2);
			case "<":
				return (r1 < r2);
			case ">":
				return (r1 > r2);
			case "<=":
				return (r1 <= r2);
			case ">=":
				return (r1 >= r2);
			default:
				throw new Exception();
		}
	}
}
