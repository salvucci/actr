package actr.env;

import actr.env.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.StyledEditorKit;

import actr.resources.Resources;

public class Actions {
	private final Core core;
	private final Frame frame;

	final Action newAction;
	final Action openAction;
	final Action closeAction;
	final Action saveAction;
	final Action saveAsAction;
	final Action printAction;
	final Action aboutAction;
	final Action quitAction;
	final Action undoAction;
	final Action redoAction;
	final Action cutAction;
	final Action copyAction;
	final Action pasteAction;
	final Action findAction;
	final Action findNextAction;
	final Action findPreviousAction;
	final Action findHideAction;
	final Action prefsAction;
	final Action runAction;
	final Action runAnalysisAction;
	final Action stopAction;
	final Action resumeAction;
	final Action outputBuffersAction;
	final Action outputWhyNotAction;
	final Action outputDMAction;
	final Action outputPRAction;
	final Action outputVisiconAction;
	Action outputTasksAction;

	Actions(final Core core, final Frame frame) {
		this.core = core;
		this.frame = frame;

		newAction = new AbstractAction("New...", Resources.getIcon("jlfNew16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.newFrame();
			}
		};
		openAction = new AbstractAction("Open...", Resources.getIcon("jlfOpen16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.openFrame();
			}
		};
		closeAction = new AbstractAction("Close") {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.closeFrame(frame);
			}
		};
		saveAction = new AbstractAction("Save", Resources.getIcon("jlfSave16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.save(false);
			}
		};
		saveAsAction = new AbstractAction("Save As...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.save(true);
			}
		};
		printAction = new AbstractAction("Print...", Resources.getIcon("jlfPrint16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.print();
			}
		};
		aboutAction = new AbstractAction("About ACT-R") {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.openAboutDialog();
			}
		};
		quitAction = new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.quit();
			}
		};

		undoAction = new AbstractAction("Undo", Resources.getIcon("jlfUndo16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getDocument().undo();
				update();
			}
		};
		redoAction = new AbstractAction("Redo", Resources.getIcon("jlfRedo16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.getDocument().redo();
				update();
			}
		};

		cutAction = new StyledEditorKit.CutAction();
		cutAction.putValue(Action.NAME, "Cut");
		cutAction.putValue(Action.LARGE_ICON_KEY, Resources.getIcon("jlfCut16.gif"));
		copyAction = new StyledEditorKit.CopyAction();
		copyAction.putValue(Action.NAME, "Copy");
		copyAction.putValue(Action.LARGE_ICON_KEY, Resources.getIcon("jlfCopy16.gif"));
		pasteAction = new StyledEditorKit.PasteAction();
		pasteAction.putValue(Action.NAME, "Paste");
		pasteAction.putValue(Action.LARGE_ICON_KEY, Resources.getIcon("jlfPaste16.gif"));

		findAction = new AbstractAction("Find", Resources.getIcon("jlfFind16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.find();
			}
		};
		findNextAction = new AbstractAction("Find Next", Resources.getIcon("jlfFindAgain16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.findNext();
			}
		};
		findPreviousAction = new AbstractAction("Find Previous", Resources.getIcon("FindPrevious16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.findPrevious();
			}
		};
		findHideAction = new AbstractAction("Find Hide") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.findHide();
			}
		};
		prefsAction = new AbstractAction("Preferences...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.openPreferencesDialog();
			}
		};

		runAction = new AbstractAction("Run", Resources.getIcon("jlfPlay16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.run();
			}
		};
		runAnalysisAction = new AbstractAction("Run Analysis", Resources.getIcon("jlfFastForward16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.runAnalysis();
			}
		};
		stopAction = new AbstractAction("Stop", Resources.getIcon("jlfStop16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.stop();
			}
		};
		resumeAction = new AbstractAction("Resume", Resources.getIcon("jlfStepForward16.gif")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.resume();
			}
		};

		outputBuffersAction = new AbstractAction("Buffers") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.outputBuffers();
			}
		};
		outputWhyNotAction = new AbstractAction("\"Why Not\"") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.outputWhyNot();
			}
		};
		outputDMAction = new AbstractAction("Declarative Memory") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.outputDeclarative();
			}
		};
		outputPRAction = new AbstractAction("Production Rules") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.outputProcedural();
			}
		};
		outputVisiconAction = new AbstractAction("Visual Objects") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.outputVisualObjects();
			}
		};
	}

	Action createOpenRecentAction(final File file) {
		return new AbstractAction(file.getName()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				core.openFrame(file);
			}
		};
	}

	void update() {
		if (frame == null)
			return;

		newAction.setEnabled(true);
		openAction.setEnabled(true);
		frame.getMenus().updateOpenRecent();
		closeAction.setEnabled(true);
		saveAction.setEnabled(frame.getDocument() != null && frame.getDocument().isChanged());
		saveAsAction.setEnabled(frame.getDocument() != null);

		undoAction.setEnabled(
				frame.getDocument() != null && frame.getEditor().hasFocus() && frame.getDocument().canUndo());
		redoAction.setEnabled(
				frame.getDocument() != null && frame.getEditor().hasFocus() && frame.getDocument().canRedo());
		cutAction.setEnabled(frame.getEditor() != null
				// && frame.getEditor().hasFocus()
				&& frame.getEditor().getSelectedText() != null);
		copyAction.setEnabled(frame.getEditor() != null
				// && frame.getEditor().hasFocus()
				&& frame.getEditor().getSelectedText() != null);

		pasteAction.setEnabled(frame.getEditor() != null
				// && frame.getEditor().hasFocus()
				&& (Main.inApplet() || Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this) != null));

		findAction.setEnabled(true);
		findNextAction.setEnabled(frame.isFindNextPossible());
		findPreviousAction.setEnabled(frame.isFindNextPossible());

		boolean runEnabled = !core.isAnyModelRunning();
		runAction.setEnabled(runEnabled); // && frame.isModelFile());
		runAnalysisAction.setEnabled(runEnabled);
		stopAction.setEnabled(core.hasLock(frame));
		resumeAction.setEnabled(runEnabled && frame.getModel() != null && !frame.getModel().isDone());

		boolean outputEnabled = frame.getModel() != null;
		outputBuffersAction.setEnabled(outputEnabled);
		outputWhyNotAction.setEnabled(outputEnabled);
		outputDMAction.setEnabled(outputEnabled);
		outputPRAction.setEnabled(outputEnabled);
		outputVisiconAction.setEnabled(outputEnabled);

		boolean changed = frame.getDocument().isChanged();
		frame.getRootPane().putClientProperty("Window.documentModified", changed);
		String title = frame.getFileName();
		if (changed)
			title = "*" + title;
		frame.setTitle(title);
	}

	Action createAppletFileAction(final String name) {
		return new AbstractAction(name) {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					frame.open(new URL(Main.getApplet().getCodeBase(), "models/" + name));
				} catch (Exception ex) {
				}
			}
		};
	}
}
