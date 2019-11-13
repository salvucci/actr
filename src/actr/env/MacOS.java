//package actr.env;
//
//import java.io.File;
//
//import javax.swing.SwingUtilities;
//
//import com.apple.eawt.AboutHandler;
//import com.apple.eawt.AppEvent.AboutEvent;
//import com.apple.eawt.AppEvent.OpenFilesEvent;
//import com.apple.eawt.AppEvent.PreferencesEvent;
//import com.apple.eawt.AppEvent.QuitEvent;
//import com.apple.eawt.Application;
//import com.apple.eawt.OpenFilesHandler;
//import com.apple.eawt.PreferencesHandler;
//import com.apple.eawt.QuitHandler;
//import com.apple.eawt.QuitResponse;
//import com.apple.eawt.QuitStrategy;
//
//class MacOS {
//
//	static void start() {
//		final Application app = Application.getApplication();
//
//		System.setProperty("apple.laf.useScreenMenuBar", "true");
//		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "ACT-R");
//
//		app.setOpenFileHandler(new OpenFilesHandler() {
//			@Override
//			public void openFiles(OpenFilesEvent e) {
//				final String filename = e.getSearchTerm();
//				if (Main.core != null)
//					Main.core.openFrame(new File(filename));
//				else
//					Core.fileToOpen = filename;
//			}
//		});
//
//		app.setAboutHandler(new AboutHandler() {
//			@Override
//			public void handleAbout(AboutEvent e) {
//				if (Main.core != null)
//					Main.core.openAboutDialog();
//			}
//		});
//
//		app.setPreferencesHandler(new PreferencesHandler() {
//			@Override
//			public void handlePreferences(PreferencesEvent e) {
//				if (Main.core != null)
//					Main.core.openPreferencesDialog();
//			}
//		});
//
//		app.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
//		app.setQuitHandler(new QuitHandler() {
//			@Override
//			public void handleQuitRequestWith(QuitEvent e, QuitResponse r) {
//				if (Main.core == null || Main.core.quit())
//					r.performQuit();
//				else
//					r.cancelQuit();
//			}
//		});
//
//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				Main.core = new Core();
//				Main.core.startup();
//			}
//		});
//	}
//}
