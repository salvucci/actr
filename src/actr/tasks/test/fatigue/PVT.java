package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import actr.model.Symbol;
import actr.task.*;

public class PVT extends Task {
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;

	private int trial;
	private int iteration;
	// private final int numberOfBlocks = 7;
	private final int runIterations = 150;
	private String response = null;
	private double responseTime = 0;

	private Block currentBlock;
	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

	private PrintStream b1Stream;
	private PrintStream b2Stream;
	private PrintStream b3Stream;
	private PrintStream b4Stream;
	private PrintStream b5Stream;
	private PrintStream b6Stream;
	private PrintStream b7Stream;
	private PrintStream uutStream;

	class Block {

		double startTime;
		double totalBlockTime;
		int FalseAlert = 0;
		int alertResponse[] = new int[35]; // Alert responses (150-500ms, 10ms
											// intervals )
		int lapses = 0;
		int Bresponces = 0;
	}

	class Session {
		Vector<Block> blocks = new Vector<Block>();
		int responses = 0;
		double responseTotalTime = 0;
	}

	public PVT() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		iteration = 1;
		trial = 0;
		lastTime = -10;
		response = null;
		responseTime = 0;
		currentSession = new Session();
		currentBlock = new Block();
		stimulusVisibility = false;

		currentBlock.startTime = 0;
		addUpdate(1.0);

		try {
			File block1file = new File("./PVTmodel/Block1.txt");
			if (!block1file.exists())
				block1file.createNewFile();
			b1Stream = new PrintStream(block1file);

			File block2file = new File("./PVTmodel/Block2.txt");
			if (!block2file.exists())
				block2file.createNewFile();
			b2Stream = new PrintStream(block2file);

			File block3file = new File("./PVTmodel/Block3.txt");
			if (!block3file.exists())
				block3file.createNewFile();
			b3Stream = new PrintStream(block3file);

			File block4file = new File("./PVTmodel/Block4.txt");
			if (!block4file.exists())
				block4file.createNewFile();
			b4Stream = new PrintStream(block4file);

			File block5file = new File("./PVTmodel/Block5.txt");
			if (!block5file.exists())
				block5file.createNewFile();
			b5Stream = new PrintStream(block5file);

			File block6file = new File("./PVTmodel/Block6.txt");
			if (!block6file.exists())
				block6file.createNewFile();
			b6Stream = new PrintStream(block6file);

			File block7file = new File("./PVTmodel/Block7.txt");
			if (!block7file.exists())
				block7file.createNewFile();
			b7Stream = new PrintStream(block7file);

			File uut = new File("./PVTmodel/UUT.txt");
			if (!uut.exists())
				uut.createNewFile();
			uutStream = new PrintStream(uut);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// this was for when you want to reset the numbers at each session
		// getModel().getFatigue().resetFatigueModule();
	}

	@Override
	public void update(double time) {
		if (iteration <= runIterations) {

			// a session
			if (trial < 340) {
				currentBlock.totalBlockTime = getModel().getTime() - currentBlock.startTime;
				// adding a new block
				if (currentBlock.totalBlockTime > 300 && currentSession.blocks.size() < 6) {
					currentSession.blocks.add(currentBlock);
					currentBlock = new Block();
					currentBlock.startTime = getModel().getTime();
					trial++;
					addUpdate(0.5);

				}

				else {
					label.setText(stimulus);
					label.setVisible(true);
					stimulusVisibility = true;
					processDisplay();
					trial++;
					lastTime = getModel().getTime();
					// setting up the state to wait
					getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),
							Symbol.get("stimulus"));

				}
			}
			// Starting a new Session
			else {
				currentSession.blocks.add(currentBlock);

				// System.out.println(" Block size ==>" +
				// currentSession.blocks.size());
				// System.out.println("Session # ==> " + iteration);
				// System.out.println("Responses ==> " +
				// currentSession.responses);
				// System.out.println("Responces Total Time ==> " +
				// currentSession.responseTotalTime);
				// System.out.println("---------------------------------------");
				// for (int i = 0; i < currentSession.blocks.size(); i++) {
				// System.out.println(" Block # ==>" + (i+1));
				// System.out.println(" Block Start Time ==>" +
				// currentSession.blocks.elementAt(i).startTime);
				// System.out.println(" TotalBlockTime ==>" +
				// currentSession.blocks.elementAt(i).totalBlockTime);
				// System.out.println(" Block Responces ==> " +
				// currentSession.blocks.elementAt(i).Bresponces);
				// System.out.println(" False Alerts ==> " +
				// currentSession.blocks.elementAt(i).FalseAlert);
				// System.out.println(" Lapses ==> " +
				// currentSession.blocks.elementAt(i).lapses);
				// System.out.println("
				// ............................................");
				//
				// }
				sessions.add(currentSession);
				currentSession = new Session();

				currentBlock = new Block();
				// System.out.println(getModel().getTime());
				currentBlock.startTime = getModel().getTime();
				trial = 0;
				iteration++;
				getModel().getFatigue().startNewTask();
				addUpdate(0); // between iteration time
			}
			// when the number of iterations exceeds the the runIteration, it's
			// the end of modeling
		} else {
			b1Stream.close();
			b2Stream.close();
			b3Stream.close();
			b4Stream.close();
			b5Stream.close();
			b6Stream.close();
			b7Stream.close();
			uutStream.close();

			getModel().stop();
		}
	}

	@Override
	public void typeKey(char c) {

		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			if (response != null) {
				currentSession.responses++;
				currentSession.responseTotalTime += responseTime;
				currentBlock.Bresponces++;
			}

			if (currentSession.blocks.size() == 0)
				b1Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 1)
				b2Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 2)
				b3Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 3)
				b4Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 4)
				b5Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 5)
				b6Stream.println((int) (responseTime * 1000));
			if (currentSession.blocks.size() == 6)
				b7Stream.println((int) (responseTime * 1000));

			if (iteration == 1 && getModel().getProcedural().getFatigueUtility() < 4
					&& getModel().getProcedural().getFatigueUtilityThreshold() < 4) {
				uutStream.print((int) getModel().getTime() + "\t");
				uutStream.print((getModel().getProcedural().getFatigueUtility()) + "\t");
				uutStream.print((getModel().getProcedural().getFatigueUtilityThreshold()) + "\n");
				uutStream.flush();
			}

			label.setVisible(false);
			processDisplay();

			Random random = new Random();
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;

			if (responseTime < .150)
				currentBlock.FalseAlert++;
			else if (responseTime > .150 && responseTime <= .500)
				currentBlock.alertResponse[(int) ((responseTime - .150) * 100)]++; // making
																					// the
																					// array
																					// for
																					// response
																					// time
			else if (responseTime > .500)
				currentBlock.lapses++;
			else if (responseTime >= 30.0)
				getModel().output("The Responce Time Was Over 30 Second: " + responseTime);

			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));

		} else {
			if (currentSession.blocks.size() == 0)
				b1Stream.println(0);
			if (currentSession.blocks.size() == 1)
				b2Stream.println(0);
			if (currentSession.blocks.size() == 2)
				b3Stream.println(0);
			if (currentSession.blocks.size() == 3)
				b4Stream.println(0);
			if (currentSession.blocks.size() == 4)
				b5Stream.println(0);
			if (currentSession.blocks.size() == 5)
				b6Stream.println(0);
			if (currentSession.blocks.size() == 6)
				b7Stream.println(0);
			currentBlock.FalseAlert++;
			currentSession.responses++;
			getModel().output(
					"False alert happened " + "- Trial: " + iteration + " Block:" + (currentSession.blocks.size() + 1)
							+ "   time of block : " + (getModel().getTime() - currentBlock.startTime));
		}

	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		double[] modelTimes = new double[runIterations];
		PVT task = (PVT) tasks[0];

		// int allResponses = 0;
		double Responses[] = new double[7];
		double FalseStarts[] = new double[7];
		double AlertResponses[][] = new double[7][35];
		double Lapses[] = new double[7];

		for (int i = 0; i < runIterations; i++) {
			for (int j = 0; j < task.sessions.elementAt(i).blocks.size(); j++) {

				Responses[j] += task.sessions.elementAt(i).blocks.elementAt(j).Bresponces;
				FalseStarts[j] += task.sessions.elementAt(i).blocks.elementAt(j).FalseAlert;
				for (int k = 0; k < 35; k++)
					AlertResponses[j][k] += task.sessions.elementAt(i).blocks.elementAt(j).alertResponse[k];
				Lapses[j] += task.sessions.elementAt(i).blocks.elementAt(j).lapses;
			}
			// allResponses += task.sessions.elementAt(i).responses;
		}

		getModel().output("******* Proportion of Responses **********\n");
		getModel().output("    FS  " + " ---------------------------    Alert Responses    --------------------------- "
				+ " Alert Responses "
				+ " ---------------------------    Alert Responses    ---------------------------- " + "L ");
		for (int i = 0; i < 7; i++) {
			double[] AlertResponsesProportion = new double[35];
			for (int j = 0; j < 35; j++)
				AlertResponsesProportion[j] = AlertResponses[i][j] / Responses[i];
			getModel().output("B" + (i + 1) + "|" + String.format("%.2f", FalseStarts[i] / Responses[i]) + " "
					+ Utilities.toString(AlertResponsesProportion) + " "
					+ String.format("%.2f", Lapses[i] / Responses[i]));

			getModel().output("\n-------------------------------------------------------\n");
		}

		// writing the numbers to file
		try {

			File PVT35min = new File("./PVTmodel/PVT35min.txt");
			if (!PVT35min.exists())
				PVT35min.createNewFile();
			PrintStream PVTfile = new PrintStream(PVT35min);

			for (int i = 0; i < 7; i++) {
				PVTfile.print(FalseStarts[i] / Responses[i] + "\t");
				for (int j = 0; j < 35; j++)
					PVTfile.print(AlertResponses[i][j] / Responses[i] + "\t");
				PVTfile.print(Lapses[i] / Responses[i] + "\n");
			}

			PVTfile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < runIterations; i++) {
			double responses = 0, responseTime = 0;
			responses = task.sessions.elementAt(i).responses;
			responseTime = task.sessions.elementAt(i).responseTotalTime;
			modelTimes[i] = (responses == 0) ? 0 : (responseTime / responses);
		}

		if (output) {
			getModel().output("\n=========              Mean Reaction Times            ===========\n");

			for (int i = 0; i < runIterations; i++) {
				getModel().output("Session " + i + "==>" + modelTimes[i]);
			}

		}

		Result result = new Result();
		// result.add ("PVT", modelTimes, humanTimes);
		return result;
	}

}
