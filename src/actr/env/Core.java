package actr.env;

import java.awt.FileDialog;
import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;

/**
 * The core class for managing the entire application. This class keeps track of
 * all open frames as well as application-wide preferences.
 * 
 * @author Dario Salvucci
 */
public class Core {
	private final List<Frame> frames;
	private final Preferences prefs;
	private final PrefDialog prefDialog;
	private final AtomicReference<Frame> runLock = new AtomicReference<>();
	private JFrame invisibleFrame;

	static final String fileToOpen = null;
	static final String starterClass = "actr.tasks.Starter";

	Core() {
		frames = new Vector<>();
		prefs = Preferences.load(this);
		prefDialog = new PrefDialog(this);
	}

	void startup() {
		try {
			Starter starter = (Starter) (Class.forName(starterClass).getConstructor().newInstance());
			starter.startup(this);
		} catch (Exception e) {
			newFrame();
		}
	}

	Preferences getPreferences() {
		return prefs;
	}

	PrefDialog getPrefDialog() {
		return prefDialog;
	}

	/**
	 * Gets the current number of open frames.
	 * 
	 * @return the number of open frames
	 */
	public int getFrameCount() {
		return frames.size();
	}

	/**
	 * Gets the frame associated with the given file.
	 * 
	 * @param file
	 *            the file
	 * @return the associated frame, or <tt>null</tt> if not present
	 */
	public Frame getFrame(File file) {
		for (Frame frame : frames)
			if (file.equals(frame.getFile()))
				return frame;
		return null;
	}

	/**
	 * Gets the currently active frame.
	 * 
	 * @return the current frame, or <tt>null</tt> if no frames exist
	 */
	public Frame currentFrame() {
		int n = frames.size();
		for (Frame frame : frames)
			if (frame.isActive())
				return frame;
		return null;
	}

	/**
	 * Creates a new frame with a new (untitled) file.
	 * 
	 * @return a new frame
	 */
	public Frame newFrame() {
		return newFrame(null);
	}

	/**
	 * Creates a new frame and loads the given file into the frame.
	 * 
	 * @param file
	 *            the file
	 * @return a new frame
	 */
	public Frame newFrame(File file) {
		Frame frame = new Frame(this, file);
		frames.add(frame);
		if (invisibleFrame != null && invisibleFrame.isVisible())
			invisibleFrame.setVisible(false);
		return frame;
	}

	/**
	 * Presents an Open File dialog box to the user and, if a file is selected,
	 * opens the file.
	 */
	public void openFrame() {
		final Frame f = currentFrame();
		FileDialog fileDialog = new FileDialog(f, "Open...", FileDialog.LOAD);
		fileDialog.setDirectory(prefs.getMostRecentPath());
		fileDialog.setVisible(true);
		if (fileDialog.getFile() == null)
			return;
		String filename = fileDialog.getDirectory() + fileDialog.getFile();
		openFrame(new File(filename));
	}

	/**
	 * Opens the given file. If the current frame has an unchanged blank
	 * document, the file is loaded into the current frame; otherwise, the file
	 * is loaded into a new frame.
	 * 
	 * @param file
	 *            the file
	 */
	public void openFrame(File file) {
		prefs.addRecentFile(file.getPath());
		Frame frame = getFrame(file);
		if (frame != null)
			frame.toFront();
		else {
			frame = currentFrame();
			if (frame != null && frame.getFile() == null && !frame.getDocument().isChanged())
				frame.open(file);
			else
				newFrame(file);
		}
	}

	void refreshEditors() {
		for (Frame frame : frames) frame.refresh();
	}

	/**
	 * Closes the given frame, asking to save changes if necessary.
	 * 
	 * @param frame
	 *            the frame
	 */
	public void closeFrame(Frame frame) {
		if (frame == null)
			return;
		if (!frame.close())
			return;
		frames.remove(frame);
		frame.setVisible(false);
		if (frames.size() == 0) {
			if (Main.onMac()) {
				if (invisibleFrame == null)
					invisibleFrame = new InvisibleFrame(this);
				invisibleFrame.setVisible(true);
			} else
				quit();
		}
	}

	boolean acquireLock(Frame frame) {
		return runLock.compareAndSet(null, frame);
	}

	boolean releaseLock(Frame frame) {
		return runLock.compareAndSet(frame, null);
	}

	boolean hasLock(Frame frame) {
		return (runLock.getOpaque() == frame);
	}

	boolean isAnyModelRunning() {
		return (runLock.getOpaque() != null);
	}

	void openPreferencesDialog() {
		prefDialog.setVisible(true);
	}

	void openAboutDialog() {
		new AboutDialog(this);
	}

	/**
	 * Closes all frames, except when a frame is unsaved and the user cancels
	 * the save operation.
	 * 
	 * @return <tt>true</tt> if all frames were closed, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean closeAllFrames() {
		for (Frame frame : frames)
			if (!frame.close())
				return false;
		return true;
	}

	/**
	 * Closes all frames and exits the application, except when a frame is
	 * unsaved and the user cancels the save operation.
	 * 
	 * @return <tt>true</tt> if the quit is successful, or <tt>false</tt>
	 *         otherwise
	 */
	public boolean quit() {
		if (closeAllFrames())
			System.exit(0);
		return false;
	}

	private static class InvisibleFrame extends JFrame {
		InvisibleFrame(Core core) {
			super();
			setUndecorated(true);
			Menus menus = new Menus(new Actions(core, null), core.getPreferences(), true);
			setJMenuBar(menus);
			pack();
			setSize(1, 1);
			setLocation(0, 0);
			setVisible(false);
		}
	}
}
