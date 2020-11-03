package actr.env;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

class Navigator extends JPanel {
	private final JComboBox<Marker> commandCB;
	private final JComboBox<Marker> productionCB;
	private final JComboBox<Marker> chunkCB;
	private boolean disableAction;

	Navigator(final Frame frame) {
		super();

		disableAction = false;

		commandCB = new JComboBox<>();
		commandCB.putClientProperty("JComponent.sizeVariant", "small");
		commandCB.setAutoscrolls(true);
		commandCB.addActionListener(e -> {
			if (!disableAction)
				goToSelected(frame, (Marker) commandCB.getSelectedItem());
		});

		productionCB = new JComboBox<>();
		productionCB.putClientProperty("JComponent.sizeVariant", "small");
		productionCB.setAutoscrolls(true);
		productionCB.addActionListener(e -> {
			if (!disableAction)
				goToSelected(frame, (Marker) productionCB.getSelectedItem());
		});

		chunkCB = new JComboBox<>();
		chunkCB.putClientProperty("JComponent.sizeVariant", "small");
		chunkCB.setAutoscrolls(true);
		chunkCB.addActionListener(e -> {
			if (!disableAction)
				goToSelected(frame, (Marker) chunkCB.getSelectedItem());
		});

		setLayout(new GridLayout(0, 3));
		add(commandCB);
		add(productionCB);
		add(chunkCB);

		setMinimumSize(new Dimension(1, 1));
		setBorder(BorderFactory.createLineBorder(Color.gray));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray),
				BorderFactory.createEmptyBorder(3, 9, 3, 3)));
	}

	void update(Document document) {
		disableAction = true;

		commandCB.removeAllItems();
		List<Marker> v = document.getCommandMarkers();
		for (Marker marker : v) {
			if (!marker.getText().startsWith("(p ") && !marker.getText().startsWith("(spp "))
				commandCB.addItem(marker);
		}

		productionCB.removeAllItems();
		v = document.getProductionMarkers();
		for (Marker value : v) productionCB.addItem(value);

		chunkCB.removeAllItems();
		v = document.getChunkMarkers();
		for (Marker marker : v) chunkCB.addItem(marker);

		disableAction = false;
	}

	private static void goToSelected(Frame frame, Marker marker) {
		if (marker == null)
			return;
		int pos = frame.getDocument().findPreviousParen('(', marker.getOffset());
		Editor editor = frame.getEditor();
		editor.setCaretPosition(pos);
		editor.scrollToVisible(pos);
		editor.grabFocus();
	}
}
