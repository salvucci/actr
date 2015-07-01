package actr.task;

/**
 * The statistics module that includes helper functions for statistical analysis
 * of task performance.
 * 
 * @author Dario Salvucci
 */
public class Statistics {
	/**
	 * Flattens the given two-dimensional array into a one-dimensional array.
	 * 
	 * @param a
	 *            the two-dimensional array
	 * @return the one-dimensional array
	 */
	public static double[] flatten(double a[][]) {
		int maxi = a.length;
		int maxj = a[0].length;
		double b[] = new double[maxi * maxj];
		for (int i = 0; i < maxi; i++)
			for (int j = 0; j < maxj; j++)
				b[i * maxj + j] = a[i][j];
		return b;
	}

	/**
	 * Computes the max of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the max value
	 */
	public static double max(double a[]) {
		if (a.length == 0)
			return 0;
		double max = 0;
		for (int i = 0; i < a.length; i++)
			if (a[i] > max)
				max = a[i];
		return max;
	}

	/**
	 * Computes the mean (average) of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the mean value
	 */
	public static double mean(double a[]) {
		if (a.length == 0)
			return 0;
		double sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += a[i];
		return sum / a.length;
	}

	/**
	 * Computes the mean (average) of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the mean value
	 */
	public static double average(double a[]) {
		return mean(a);
	}

	/**
	 * Computes the standard deviation of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the standard deviation
	 */
	public static double stddev(double a[]) {
		if (a.length == 0)
			return 0;
		double ma = mean(a);
		double sum = 0;
		for (int i = 0; i < a.length; i++)
			sum += Math.pow(a[i] - ma, 2);
		return Math.sqrt(sum / (a.length - 1));
	}

	/**
	 * Computes the standard error of the given array.
	 * 
	 * @param a
	 *            the array
	 * @return the standard error
	 */
	public static double stderr(double a[]) {
		return Math.sqrt(stddev(a));
	}

	/**
	 * Computes the size of the confidence interval (+/-) around the mean.
	 * 
	 * @param a
	 *            the array
	 * @return the size of the confidence interval
	 */
	public static double confidence(double a[]) {
		return 1.96 * stderr(a);
	}

	/**
	 * Computes the correlation of two arrays.
	 * 
	 * @param a1
	 *            the first array
	 * @param a2
	 *            the second array
	 * @return the correlation
	 */
	public static double correlation(double a1[], double a2[]) {
		if (a1.length <= 1 || (a1.length != a2.length))
			return 0;
		double ma = mean(a1), mb = mean(a2);
		double sda = stddev(a1), sdb = stddev(a2);
		double sum = 0;
		for (int i = 0; i < a1.length; i++)
			sum += (a1[i] - ma) * (a2[i] - mb);
		return sum / ((a1.length - 1) * sda * sdb);
	}

	/**
	 * Computes the correlation of two two-dimensional arrays (by flattening
	 * them and then computing the correlation on the flattened arrays).
	 * 
	 * @param a1
	 *            the first array
	 * @param a2
	 *            the second array
	 * @return the correlation
	 */
	public static double correlation(double a1[][], double a2[][]) {
		return correlation(flatten(a1), flatten(a2));
	}

	/**
	 * Computes the root-mean-squared-error (RMSE) of two arrays.
	 * 
	 * @param a1
	 *            the first array
	 * @param a2
	 *            the second array
	 * @return the RMSE
	 */
	public static double rmse(double a1[], double a2[]) {
		if (a1.length <= 1 || (a1.length != a2.length))
			return 0;
		double sum = 0;
		for (int i = 0; i < a1.length; i++)
			sum += (a1[i] - a2[i]) * (a1[i] - a2[i]);
		return Math.sqrt(sum / a1.length);
	}

	/**
	 * Computes the RMSE of two two-dimensional arrays (by flattening them and
	 * then computing the RMSE on the flattened arrays).
	 * 
	 * @param a1
	 *            the first array
	 * @param a2
	 *            the second array
	 * @return the RMSE
	 */
	public static double rmse(double a1[][], double a2[][]) {
		return rmse(flatten(a1), flatten(a2));
	}

	/**
	 * Computes a normalized error of two arrays as the proportion of the RMSE
	 * to the mean.
	 * 
	 * @param model
	 *            the model data
	 * @param human
	 *            the human data
	 * @return the normalized error
	 */
	public static double error(double model[], double human[]) {
		double rmse = rmse(model, human);
		// double mean = mean(human);
		// return Math.abs (rmse / mean);
		double max = max(human);
		return Math.abs(rmse / max);
	}

	/**
	 * Computes a normalized error of two 2d arrays as the proportion of the
	 * RMSE to the mean.
	 * 
	 * @param model
	 *            the model data
	 * @param human
	 *            the human data
	 * @return the normalized error
	 */
	public static double error(double model[][], double human[][]) {
		return error(flatten(model), flatten(human));
	}
}
