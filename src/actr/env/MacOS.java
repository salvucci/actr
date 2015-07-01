package actr.env;

import java.io.File;

import javax.swing.SwingUtilities;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

@SuppressWarnings({ "deprecation" })
class MacOS extends Application {
	static void start() {
		final Application app = new MacOS();

		app.setEnabledPreferencesMenu(true);
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		ApplicationListener adapter = new ApplicationListener() {

			@Override
			public void handleOpenFile(ApplicationEvent e) {
				final String filename = e.getFilename();
				if (Main.core != null)
					Main.core.openFrame(new File(filename));
				else
					Core.fileToOpen = filename;
			}

			@Override
			public void handleQuit(ApplicationEvent e) {
				if (Main.core != null)
					e.setHandled(Main.core.quit());
				else
					e.setHandled(true);
			}

			@Override
			public void handleAbout(ApplicationEvent e) {
				if (Main.core != null)
					Main.core.openAboutDialog();
				e.setHandled(true);
			}

			@Override
			public void handlePreferences(ApplicationEvent e) {
				if (Main.core != null)
					Main.core.openPreferencesDialog();
				e.setHandled(true);
			}

			@Override
			public void handleOpenApplication(ApplicationEvent e) {
			}

			@Override
			public void handlePrintFile(ApplicationEvent e) {
			}

			@Override
			public void handleReOpenApplication(ApplicationEvent e) {
			}

		};
		app.addApplicationListener(adapter);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Main.core = new Core();
				Main.core.startup();
			}
		});
	}
}
