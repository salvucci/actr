package actr.env;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;

class Preferences {
	int frameWidth, frameHeight, editorPaneSplit, taskPaneSplit;
	String font;
	int fontSize;
	boolean autoHilite, autoIndent;
	Color commandColor, parameterColor, productionColor, chunkColor, bufferColor, commentColor;
	int indentSpaces;
	Vector<String> recentFiles = new Vector<String>();

	private static final String prefsFilePath = ((Main.inApplet()) ? null
			: (Main.onMac()) ? System.getProperty("user.home") + "/Library/Preferences/actr.txt"
					: ((Main.onNix()) ? System.getProperty("user.home") + "/.actr"
							: System.getProperty("user.home") + File.separator + "actrprefs.txt"));

	private final int maxRecentFiles = 5;

	private Preferences() {
		setDefaults();
	}

	void setDefaults() {
		frameWidth = 1200;
		frameHeight = 700;
		editorPaneSplit = 500;
		taskPaneSplit = 320;
		font = "Verdana";
		fontSize = 12;
		autoHilite = true;
		autoIndent = true;
		commandColor = new Color(127, 0, 85);
		parameterColor = new Color(63, 127, 95);
		productionColor = new Color(0, 0, 192);
		chunkColor = new Color(0, 0, 192);
		bufferColor = new Color(120, 100, 60);
		commentColor = Color.gray;
		indentSpaces = 4;
	}

	static Preferences load(Core core) {
		if (Main.inApplet())
			return new Preferences();

		try {
			Preferences prefs = new Preferences();
			File file = new File(prefsFilePath);
			FileReader inputStream = null;
			StringWriter sw = new StringWriter();
			inputStream = new FileReader(file);
			int c;
			while ((c = inputStream.read()) != -1)
				sw.write(c);
			inputStream.close();
			String lines[] = sw.toString().split("\n");
			for (int i = 0; i < lines.length; i++) {
				String pair[] = lines[i].split("\\$");
				String var = pair[0];
				String val = pair[1];

				if (var.equals("frameWidth"))
					prefs.frameWidth = Integer.valueOf(val);
				else if (var.equals("frameHeight"))
					prefs.frameHeight = Integer.valueOf(val);
				else if (var.equals("editorPaneSplit"))
					prefs.editorPaneSplit = Integer.valueOf(val);
				else if (var.equals("taskPaneSplit"))
					prefs.taskPaneSplit = Integer.valueOf(val);
				else if (var.equals("font"))
					prefs.font = val;
				else if (var.equals("fontSize"))
					prefs.fontSize = Integer.valueOf(val);
				else if (var.equals("autoHilite"))
					prefs.autoHilite = val.equals("true");
				else if (var.equals("autoTab"))
					prefs.autoIndent = val.equals("true");
				else if (var.equals("commandColor"))
					prefs.commandColor = Color.decode(val);
				else if (var.equals("parameterColor"))
					prefs.parameterColor = Color.decode(val);
				else if (var.equals("productionColor"))
					prefs.productionColor = Color.decode(val);
				else if (var.equals("chunkColor"))
					prefs.chunkColor = Color.decode(val);
				else if (var.equals("bufferColor"))
					prefs.bufferColor = Color.decode(val);
				else if (var.equals("commentColor"))
					prefs.commentColor = Color.decode(val);
				else if (var.equals("tabSpaces"))
					prefs.indentSpaces = Integer.valueOf(val);
				else if (var.equals("recentFiles")) {
					String paths[] = val.split(",");
					for (int j = 0; j < paths.length; j++)
						prefs.recentFiles.add(paths[j]);
				}

			}
			return prefs;
		} catch (Exception e) {
			Preferences prefs = new Preferences();
			prefs.save();
			return prefs;
		}
	}

	String toHex(Color color) {
		String s = Integer.toHexString(color.getRGB());
		s = "#" + s.substring(s.length() - 6, s.length());
		return s;
	}

	void save() {
		if (Main.inApplet())
			return;

		try {
			File file = new File(prefsFilePath);
			PrintWriter pw = new PrintWriter(new FileWriter(file));

			pw.println("frameWidth$" + frameWidth);
			pw.println("frameHeight$" + frameHeight);
			pw.println("editorPaneSplit$" + editorPaneSplit);
			pw.println("taskPaneSplit$" + taskPaneSplit);
			pw.println("font$" + font);
			pw.println("fontSize$" + fontSize);
			pw.println("autoHilite$" + (autoHilite ? "true" : "false"));
			pw.println("autoTab$" + (autoHilite ? "true" : "false"));
			pw.println("commandColor$" + toHex(commandColor));
			pw.println("parameterColor$" + toHex(parameterColor));
			pw.println("productionColor$" + toHex(productionColor));
			pw.println("chunkColor$" + toHex(chunkColor));
			pw.println("bufferColor$" + toHex(bufferColor));
			pw.println("commentColor$" + toHex(commentColor));
			pw.println("tabSpaces$" + indentSpaces);
			pw.print("recentFiles$");
			if (recentFiles.size() > 0)
				pw.print(recentFiles.elementAt(0));
			for (int j = 1; j < recentFiles.size(); j++)
				pw.print("," + recentFiles.elementAt(j));

			pw.close();
		} catch (Exception e) {
		}
	}

	String getMostRecentPath() {
		if (recentFiles.size() > 0)
			return recentFiles.elementAt(0);
		else
			return null;
	}

	void addRecentFile(String fileName) {
		recentFiles.remove(fileName);
		recentFiles.insertElementAt(fileName, 0);
		if (recentFiles.size() > maxRecentFiles)
			recentFiles.removeElementAt(recentFiles.size() - 1);
	}
}
