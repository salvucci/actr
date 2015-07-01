package actr.task;

import java.util.Random;

/**
 * The task utilities module that includes helper functions for implementing
 * tasks.
 * 
 * @author Dario Salvucci
 */
public class Utilities {
	/** A random number generator. */
	public static Random random = new Random(System.currentTimeMillis());

	/**
	 * Shuffles (randomizes) the contents of the given array.
	 * 
	 * @param a
	 *            the array
	 */
	public static void shuffle(int a[]) {
		for (int i = 0; i < a.length - 1; i++) {
			int tmp = a[i];
			int index = i + 1 + random.nextInt(a.length - i - 1);
			a[i] = a[index];
			a[index] = tmp;
		}
	}

	/**
	 * Shuffles (randomizes) the contents of the given array.
	 * 
	 * @param a
	 *            the array
	 */
	public static void shuffle(Object a[]) {
		for (int i = 0; i < a.length - 1; i++) {
			Object tmp = a[i];
			int index = i + 1 + random.nextInt(a.length - i - 1);
			a[i] = a[index];
			a[index] = tmp;
		}
	}

	/**
	 * Reverses the contents of the given array.
	 * 
	 * @param a
	 *            the array
	 */
	public static void reverse(int a[]) {
		for (int i = 0; i < 0.5 * a.length; i++) {
			int tmp = a[i];
			a[i] = a[a.length - 1 - i];
			a[a.length - 1 - i] = tmp;
		}
	}

	/**
	 * Gets a tab-separated string representation of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the tab-separated string
	 */
	public static String toString(double a[]) {
		String s = "";
		for (int i = 0; i < a.length; i++)
			s += String.format("%.3f", a[i]) + (i < a.length - 1 ? "\t" : "");
		return s;
	}
}
