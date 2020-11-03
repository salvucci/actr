package actr.env;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import actr.model.Model;
import actr.task.Result;
import actr.task.Task;

/**
 * The class that defines a graphical frame (window) for editing and running an
 * ACT-R model. The frame contains a text editor for the model text, a task
 * panel for displaying the task interface, and an output panel for displaying
 * the text output of the simulation.
 * 
 * @author Dario Salvucci
 */
public class Frame extends JFrame {
	private final Frame frame;

	private final Core core;
	private final Actions actions;
	private final Editor editor;
	private final JTextArea outputArea;
	private final Brain brainPanel;
	private final Navigator navigator;
	private final FindPanel editorFind;
	private final FindPanel outputFind;
	private final JSplitPane splitPane;
	private final JSplitPane taskSplitPane;
	private final Menus menus;
	private final Toolbar toolbar;

	private File file;
	private Model model;
	private boolean stop;

	Frame(Core core) {
		this(core, null);
	}

	Frame(final Core core, File file) {
		super("ACT-R");
		frame = this;

		this.core = core;

		actions = new Actions(core, this);

		editor = new Editor(this, core.getPreferences());
		editor.grabFocus();

		JScrollPane editorScroll = new JScrollPane(editor);
		editorScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		editorScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		navigator = new Navigator(this);

		editorFind = new FindPanel(this, true);
		editorFind.setVisible(false);

		JPanel modelSubPanel = new JPanel();
		modelSubPanel.setLayout(new BorderLayout(6, 6));
		modelSubPanel.add(editorScroll, BorderLayout.CENTER);
		modelSubPanel.add(editorFind, BorderLayout.SOUTH);

		JPanel modelPanel = new JPanel();
		modelPanel.setLayout(new BorderLayout(6, 6));
		modelPanel.add(modelSubPanel, BorderLayout.CENTER);
		modelPanel.add(navigator, BorderLayout.SOUTH);

		outputArea = new JTextArea();
		outputArea.setFont(new Font("Courier", Font.PLAIN, 12)); // "Monaco"
		outputArea.setLineWrap(false);
		outputArea.setWrapStyleWord(false);
		outputArea.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				update();
			}

			@Override
			public void focusLost(FocusEvent e) {
				update();
			}
		});

		JScrollPane outputScroll = new JScrollPane(outputArea);
		outputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		outputFind = new FindPanel(this, false);
		outputFind.setVisible(false);

		JPanel outputPanel = new JPanel();
		outputPanel.setLayout(new BorderLayout(6, 6));
		outputPanel.add(outputScroll, BorderLayout.CENTER);
		outputPanel.add(outputFind, BorderLayout.SOUTH);

		taskSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new Task(), outputPanel);
		taskSplitPane.setBorder(BorderFactory.createEmptyBorder());
		taskSplitPane.setOneTouchExpandable(true);
		// taskSplitPane.setOpaque (true);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, modelPanel, taskSplitPane);
		splitPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6)); // 12,12,12,12));
		splitPane.setOneTouchExpandable(true);
		// splitPane.setOpaque(true);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(splitPane, BorderLayout.CENTER);

		brainPanel = new Brain(this);
		brainPanel.setVisible(false);
		getContentPane().add(brainPanel, BorderLayout.EAST);

		toolbar = new Toolbar(this, actions);
		getContentPane().add(toolbar, BorderLayout.NORTH);

		getRootPane().registerKeyboardAction(actions.findHideAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		menus = new Menus(actions, core.getPreferences());
		setJMenuBar(menus);

		if (Main.inApplication())
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				core.closeFrame(frame);
			}

			@Override
			public void windowActivated(WindowEvent e) {
				update();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				update();
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				core.getPreferences().frameWidth = frame.getWidth();
				core.getPreferences().frameHeight = frame.getHeight();
			}
		});

		if (file != null)
			open(file);

		pack();
		setSize(core.getPreferences().frameWidth, core.getPreferences().frameHeight);

		splitPane.setDividerLocation(core.getPreferences().editorPaneSplit);
		taskSplitPane.setDividerLocation(core.getPreferences().taskPaneSplit);
		repaint();

		setVisible(true);

		update();
	}

	Actions getActions() {
		return actions;
	}

	Menus getMenus() {
		return menus;
	}

	Toolbar getToolbar() {
		return toolbar;
	}

	JTextArea getOutputArea() {
		return outputArea;
	}

	/**
	 * Gets the file associated with the frame.
	 * 
	 * @return the file, or <tt>null</tt> if there is none (e.g., the editor has
	 *         not been saved)
	 */
	public File getFile() {
		return file;
	}

	boolean isModelFile() {
		return file != null && file.getName().endsWith(".actr");
	}

	boolean isBatchFile() {
		return file != null && file.getName().endsWith(".batch");
	}

	Editor getEditor() {
		return editor;
	}

	Document getDocument() {
		return (editor != null) ? editor.getModelDocument() : null;
	}

	Model getModel() {
		return model;
	}

	Task getTask() {
		return (model != null) ? model.getTask() : null;
	}

	Navigator getNavigator() {
		return navigator;
	}

	String getShortName() {
		String name = getFileName();
		if (name.contains(".actr"))
			name = name.substring(0, name.indexOf(".actr"));
		return name;
	}

	String getFileName() {
		return file == null ? "Untitled" : file.getName();
	}

	String getFilePath() {
		if (file == null)
			return "Untitled";
		return file.getPath();
	}

	void showTask(Task task) {
		int divider = taskSplitPane.getDividerLocation();
		taskSplitPane.setTopComponent(task);
		taskSplitPane.setDividerLocation(divider);
		repaint();
	}

	void hideTask() {
		// int divider = taskSplitPane.getDividerLocation();
		// taskSplitPane.setTopComponent (new JPanel());
		// taskSplitPane.setDividerLocation (divider);
		// repaint();
	}

	void setDefaultButton(JButton button) {
		getRootPane().setDefaultButton(button);
	}

	/**
	 * Opens the given file in the frame.
	 * 
	 * @param file
	 *            the file
	 * @param suppressStyling
	 *            true to disable coloring and indenting (for batch processing)
	 */
	public void open(File file, boolean suppressStyling) {
		this.file = file;
		if (editor.open(file, suppressStyling))
			getRootPane().putClientProperty("Window.documentFile", file);
		else
			this.file = null;
		editor.grabFocus();
		model = null;
		update();
	}

	/**
	 * Opens the given file in the frame.
	 * 
	 * @param file
	 *            the file
	 */
	public void open(File file) {
		open(file, false);
	}

	/**
	 * Opens the given URL in the frame.
	 * 
	 * @param url
	 *            the URL
	 */
	public void open(URL url) {
		this.file = new File(url.getPath());
		editor.open(url);
		editor.grabFocus();
		model = null;
		update();
	}

	/**
	 * Runs the model currently loaded into the editor.
	 */
	public void run() {
		run(true);
	}

	/**
	 * Runs the model currently loaded into the editor, possibly resetting it
	 * before running.
	 * 
	 * @param reset
	 *            flag indicating whether or not to recompile the model before
	 *            running
	 */
	public void run(final boolean reset) {
		if (isBatchFile()) {
			runBatch(true);
			return;
		}

		final String modelText = editor.getText();
		final String speedup = toolbar.getSpeedup();
		(new SwingWorker<>() {
			@Override
			public Object doInBackground() {
				if (!core.acquireLock(frame))
					return null;
				update();
				if (reset) {
					clearOutput();
					output("> (run)\n");
					editor.clearMarkers();
					model = Model.compile(modelText, frame);
					editor.addMarkers(model.getErrors(), true);
					if (model.hasFatalErrors())
						model = null;
				} else
					output("\n> (resume)\n");
				if (model != null) {
					brainPanel.setVisible(model.bold.isImaging());
					showTask(model.getTask());
					if (!speedup.isEmpty())
						model.setParameter(":real-time", speedup);
					model.run(reset);
					hideTask();
				}
				core.releaseLock(frame);
				update();
				return null;
			}
		}).execute();
	}

	/**
	 * Runs an analysis of the current model. The analysis runs the model the
	 * number of times defined by the task's <tt>analysisIterations()</tt>
	 * method and then runs the task's <tt>analyze()</tt> method on all the
	 * iterations of the task, printing the analysis output to the frame's
	 * output panel.
	 */
	public void runAnalysis() {
		if (isBatchFile()) {
			runBatch(false);
			return;
		}

		final String modelText = editor.getText();
		String iterations = toolbar.getIterations();
		model = Model.compile(modelText, frame);
		Task task = model.getTask();
		final int n = (iterations.isEmpty()) ? task.analysisIterations() : Integer.parseInt(iterations);
		(new SwingWorker<>() {
			@Override
			public Object doInBackground() {
				if (!core.acquireLock(frame))
					return null;
				stop = false;
				update();
				clearOutput();
				output("> (run-analysis " + n + ")\n");
				if (model != null && model.getTask() != null) {
					Task[] tasks = new Task[n];
					for (int i = 0; !stop && i < n; i++) {
						model = Model.compile(modelText, frame);
						brainPanel.setVisible(false);
						showTask(model.getTask());
						model.setParameter(":real-time", "nil");
						model.run();
						model.getTask().finish();
						tasks[i] = model.getTask();
					}
					if (!stop && model != null)
						model.getTask().analyze(tasks, true);
					// model = null;
					hideTask();
				}
				core.releaseLock(frame);
				update();
				return null;
			}
		}).execute();
	}

	void runBatch(final boolean output) {
		(new SwingWorker<>() {
			@Override
			public Object doInBackground() {
				if (file == null)
					return null;
				if (!closing())
					return null;
				if (!core.acquireLock(frame))
					return null;
				stop = false;
				File savedFile = file;
				update();
				clearOutput();
				output("> (run-batch)\n");
				output(Result.headerString());
				String basePath = file.getPath();
				basePath = basePath.substring(0, basePath.lastIndexOf(File.separator)) + File.separator;
				String[] filenames = editor.getText().split("\\s");
				for (int k = 0; !stop && k < filenames.length; k++) {
					String line = filenames[k];
					String modelName = line;
					String[] taskOverrides = {null};
					if (line.contains(":")) {
						modelName = line.substring(0, line.indexOf(':'));
						taskOverrides = line.substring(line.indexOf(':') + 1).split(",");
					}
					open(new File(basePath + modelName), true);
					final String modelText = editor.getText();

					for (String taskOverride : taskOverrides) {
						model = Model.compile(modelText, frame, taskOverride);
						int n = model.getTask().analysisIterations();
						Task[] tasks = new Task[n];
						for (int i = 0; !stop && i < n; i++) {
							model = Model.compile(modelText, frame, taskOverride);
							brainPanel.setVisible(false);
							showTask(model.getTask());
							model.setParameter(":real-time", "nil");
							model.setParameter(":v", "nil");
							model.run();
							model.getTask().finish();
							tasks[i] = model.getTask();
						}
						if (!stop && model != null) {
							Task task = model.getTask();
							Result result = task.analyze(tasks, output);
							output(result.toString());
						}
					}
				}
				model = null;
				hideTask();
				open(savedFile);
				core.releaseLock(frame);
				update();
				return null;
			}
		}).execute();
	}

	/**
	 * Stops the model simulation at the earliest time possible.
	 */
	public void stop() {
		if (core.hasLock(frame)) {
			if (model != null)
				model.stop();
			stop = true;
		}
	}

	/**
	 * Resumes the previous model simulation if possible (equivalent to
	 * <tt>run(false)</tt>).
	 */
	public void resume() {
		if (model != null)
			run(false);
	}

	void save(boolean forceSaveAs) {
		if (Main.inApplet())
			return;

		try {
			if (forceSaveAs || file == null) {
				File newFile = null;
				while (newFile == null) {
					FileDialog fileDialog = new FileDialog(frame, "Save As...", FileDialog.SAVE);
					if (file != null)
						fileDialog.setDirectory(file.getPath());
					else
						fileDialog.setDirectory(core.getPreferences().getMostRecentPath());
					fileDialog.setVisible(true);
					if (fileDialog.getFile() == null)
						return;
					String filename = fileDialog.getDirectory() + fileDialog.getFile();
					if (filename.indexOf('.') != -1)
						filename = filename.substring(0, filename.indexOf('.'));
					filename += ".actr";
					newFile = new File(filename);
					if (newFile.exists()) {
						String[] options = { "Yes", "No", "Cancel" };
						int choice = JOptionPane.showOptionDialog(frame,
								"Overwrite existing \"" + newFile.getName() + "\"?", "File Exists",
								JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
						if (choice == 1)
							newFile = null;
						else if (choice == 2)
							return;
					}
				}
				file = newFile;
			}
			StringReader sr = new StringReader(editor.getText());
			FileWriter outputStream;
			outputStream = new FileWriter(file);
			int c;
			while ((c = sr.read()) != -1)
				outputStream.write(c);
			outputStream.close();
			getDocument().setChanged(false);
			getDocument().noteSave();
			update();
		} catch (IOException exc) {
		}
	}

	void find() {
		if (editor.hasFocus() || editorFind.hasFocus()) {
			editorFind.setVisible(true);
			editorFind.grabFocus();
			navigator.setVisible(false);
			outputFind.setVisible(false);
		} else {
			outputFind.setVisible(true);
			outputFind.grabFocus();
			editorFind.setVisible(false);
			navigator.setVisible(true);
		}
		update();
	}

	void findNext() {
		if (editorFind.isVisible() || editor.hasFocus())
			editorFind.findNext();
		else
			outputFind.findNext();
	}

	void findPrevious() {
		if (editorFind.isVisible() || editor.hasFocus())
			editorFind.findPrevious();
		else
			outputFind.findPrevious();
	}

	void findHide() {
		if (editorFind.isVisible())
			editor.grabFocus();
		if (outputFind.isVisible())
			outputArea.grabFocus();
		editorFind.setVisible(false);
		navigator.setVisible(true);
		outputFind.setVisible(false);
	}

	boolean isFindNextPossible() {
		if (editorFind.isVisible() || editor.hasFocus())
			return editorFind.isFindNextPossible();
		else if (outputFind.isVisible() || outputArea.hasFocus())
			return outputFind.isFindNextPossible();
		else
			return false;
	}

	void print() {
		try {
			editor.getModelDocument().changeFontSize(-3);
			editor.print();
			editor.getModelDocument().changeFontSize(0);
			// Editor printEditor = (Editor) editor.createCopy();
			// printEditor.getModelDocument().changeFontSize (3);
			// printEditor.print();
		} catch (Exception e) {
			output("Print error");
		}
	}

	void refresh() {
		getDocument().resetStyles();
		getDocument().restyle();
		repaint();
	}

	boolean closing() {
		if (model != null && core.hasLock(frame))
			stop();
		if (Main.inApplication() && getDocument().isChanged()) {
			SaveDialog saveDialog = new SaveDialog(frame, getFileName());
			if (saveDialog.cancel)
				return false;
			else if (saveDialog.save)
				save(false);
		}
		return true;
	}

	boolean close() {
		if (!closing())
			return false;
		core.getPreferences().frameWidth = getWidth();
		core.getPreferences().frameHeight = getHeight();
		core.getPreferences().editorPaneSplit = splitPane.getDividerLocation();
		core.getPreferences().taskPaneSplit = taskSplitPane.getDividerLocation();
		core.getPreferences().save();
		return true;
	}

	void update() {
		actions.update();
		repaint();
	}

	/**
	 * Prints the given string to the frame's output panel.
	 * 
	 * @param s
	 *            the string
	 */
	public void output(String s) {
		outputArea.append(s + "\n");
		outputArea.setCaretPosition(outputArea.getDocument().getLength());
	}
	
	/**
	 * Prints the given string to the frame's output panel.
	 * 
	 * @param s
	 *            the string
	 */
	public void outputInLine(String s) {
		outputArea.append(s);
		outputArea.setCaretPosition(outputArea.getDocument().getLength());
	}

	/**
	 * Clears the frame's output panel.
	 */
	public void clearOutput() {
		outputArea.setText("");
	}

	/**
	 * Prints the current contents of the model buffers.
	 */
	public void outputBuffers() {
		output("\n> (buffers)\n");
		if (model != null)
			model.outputBuffers();
	}

	/**
	 * Prints the model's current "why not" trace.
	 */
	public void outputWhyNot() {
		output("\n> (why-not)\n");
		if (model != null)
			model.outputWhyNot();
	}

	/**
	 * Prints the model's current declarative memory.
	 */
	public void outputDeclarative() {
		output("\n> (dm)\n");
		if (model != null)
			model.outputDeclarative();
	}

	/**
	 * Prints the model's current production rules.
	 */
	public void outputProcedural() {
		output("\n> (p)\n");
		if (model != null)
			model.outputProcedural();
	}

	/**
	 * Prints the model's current visual objects.
	 */
	public void outputVisualObjects() {
		output("\n> (visual-objects)\n");
		if (model != null)
			model.outputVisualObjects();
	}

	/**
	 * Prints all implemented task classes.
	 */
	public void outputTasks() {
		output("\n> (all-tasks)\n");
		String[] tasks = Task.allTaskClasses();
		for (String task : tasks) output(task);
	}

	/**
	 * Repaints the simulated brain.
	 */
	public void updateVisuals() {
		brainPanel.repaint();
	}
}
