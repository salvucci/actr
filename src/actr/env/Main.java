package actr.env;

import java.awt.BorderLayout;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * The class that defines <tt>main()</tt>, determines the operating system
 * platform, and opens the application.
 * 
 * @author Dario Salvucci
 */
public class Main extends JApplet {
	/** The application's version string. */
	private static final String VERSION = "1.3";

	/** The single core class used in the application. */
	public static Core core = null;

	/** The applet class when run as an applet. */
	private static JApplet applet = null;

	/**
	 * Checks whether the application is running on the Macintosh platform.
	 * 
	 * @return <tt>true</tt> if the system is running on the Macintosh platform,
	 *         or <tt>false</tt> otherwise
	 */
	public static boolean onMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}

	/**
	 * Checks whether the application is running on the Windows platform.
	 * 
	 * @return <tt>true</tt> if the system is running on the Windows platform,
	 *         or <tt>false</tt> otherwise
	 */
	public static boolean onWin() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	/**
	 * Checks whether the application is running on a *NIX platform.
	 * 
	 * @return <tt>true</tt> if the system is running on a *NIX platform, or
	 *         <tt>false</tt> otherwise
	 */
	public static boolean onNix() {
		return System.getProperty("os.name").toLowerCase().contains("nix")
				|| System.getProperty("os.name").toLowerCase().contains("nux");
	}

	/**
	 * Returns the current applet, or none if running as an application.
	 * 
	 * @return the applet if the system is running as an applet, or
	 *         <tt>null</tt> otherwise
	 */
	public static JApplet getApplet() {
		return applet;
	}

	/**
	 * Checks whether the system is running as an applet.
	 * 
	 * @return <tt>true</tt> if the system is running as an applet, or
	 *         <tt>false</tt> otherwise
	 */
	public static boolean inApplet() {
		return applet != null;
	}

	/**
	 * Checks whether the system is running as an application.
	 * 
	 * @return <tt>true</tt> if the system is running as an application, or
	 *         <tt>false</tt> otherwise
	 */
	public static boolean inApplication() {
		return applet == null;
	}

	/**
	 * Gets the version string, including main version number and revision
	 * number.
	 * 
	 * @return the version string
	 */
	public static String getVersion() {
		if (inApplication()) {
			try {
				URL url = Main.class.getResource("Version.txt");
				InputStream in = url.openStream();
				StringWriter sw = new StringWriter();
				int c;
				while ((c = in.read()) != -1)
					sw.write(c);
				in.close();
				return VERSION + " r" + sw.toString();
			} catch (Exception e) {
				e.printStackTrace();
				return VERSION;
			}
		} else
			return VERSION;
	}

	/**
	 * The main method called on startup of the application.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
//		if (onMac())
//			MacOS.start();
//		else {
			SwingUtilities.invokeLater(() -> {
				core = new Core();
				core.startup();
			});
//		}
	}

	/**
	 * The init method called on startup of the applet.
	 */
	@Override
	public void init() {
		applet = this;
		JButton button = new JButton("Launch ACT-R");
		button.addActionListener(arg0 -> {
			core = new Core();
			core.newFrame();
		});
		setLayout(new BorderLayout());
		add(button, BorderLayout.CENTER);
	}
}
