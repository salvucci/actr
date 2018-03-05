package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;

import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;


/**
 * Model of PVT test on time on task
 * 
 * Paper: Functional Equivalence of sleep loss and time on task effects in sustained attention
 * 
 * Bella Z. Vaksler, Glenn Gunzelmann, Air force research lab
 * 
 * @author Ehsan Khosroshahi
 */

public class PVT35min extends Task {
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	// the following variable are for handling sleep attacks
	private int sleepAttackIndex = 0;
	private double PVTduration = 2100.0;
	Random random;

	private double[] timesOfPVT = {
			24.0 + 12   // setting the time of the PVT at noon in the 2nd day
	};

	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private Session currentSession;
	private Vector<Session> sessions = new Vector<Session>();

	//	private PrintStream b1Stream;
	//	private PrintStream b2Stream;
	//	private PrintStream b3Stream;
	//	private PrintStream b4Stream;
	//	private PrintStream b5Stream;
	//	private PrintStream b6Stream;
	//	private PrintStream b7Stream;
	//	private PrintStream uutStream;

	// 5-min blocks
	class Block {
		Values reactionTimes = new Values();
		double startTime;
		double totalBlockTime;
		int falseAlert = 0;
		int alertResponse[] = new int[35]; // Alert responses (150-500ms, 10ms
		// intervals )
		int lapses = 0;
		int numberOfResponses = 0;
		int sleepAttacks = 0;
		public int getFalseAlertProportion() {
			return falseAlert/ reactionTimes.size();
		}
		public int getLapsesProportion() {
			return lapses / reactionTimes.size();
		}
	}

	class Session {
		Vector<Block> blocks = new Vector<Block>();
		int blockIndex = 0;
		Values reactionTimes = new Values();
		double startTime = 0;
		int falseStarts = 0;
		int alertRosponses = 0;
		// Alert responses (150-500 ms,10 ms intervals )
		int alertResponse[] = new int[35]; 
		double totalSessionTime = 0;
		int lapses = 0;
		int sleepAttacks = 0;
		int stimulusIndex = 0;
		int numberOfResponses = 0; // number of responses, this can be diff from the
		// stimulusIndex because of false resonces
		double responseTotalTime = 0;
	}

	public PVT35min() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		random = new Random();
		lastTime = 0;
		currentSession = new Session();
		currentBlock = new Block();
		stimulusVisibility = false;
		currentSession.startTime = 0;
		currentBlock.startTime = 0;

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
		getModel().getFatigue().startFatigueSession();

		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);

		//		try {
		//			File block1file = new File("./test/fatigue/pvt_35min/Block1.txt");
		//			if (!block1file.exists())
		//				block1file.createNewFile();
		//			b1Stream = new PrintStream(block1file);
		//
		//			File block2file = new File("./test/fatigue/pvt_35min/Block2.txt");
		//			if (!block2file.exists())
		//				block2file.createNewFile();
		//			b2Stream = new PrintStream(block2file);
		//
		//			File block3file = new File("./test/fatigue/pvt_35min/Block3.txt");
		//			if (!block3file.exists())
		//				block3file.createNewFile();
		//			b3Stream = new PrintStream(block3file);
		//
		//			File block4file = new File("./test/fatigue/pvt_35min/Block4.txt");
		//			if (!block4file.exists())
		//				block4file.createNewFile();
		//			b4Stream = new PrintStream(block4file);
		//
		//			File block5file = new File("./test/fatigue/pvt_35min/Block5.txt");
		//			if (!block5file.exists())
		//				block5file.createNewFile();
		//			b5Stream = new PrintStream(block5file);
		//
		//			File block6file = new File("./test/fatigue/pvt_35min/Block6.txt");
		//			if (!block6file.exists())
		//				block6file.createNewFile();
		//			b6Stream = new PrintStream(block6file);
		//
		//			File block7file = new File("./test/fatigue/pvt_35min/Block7.txt");
		//			if (!block7file.exists())
		//				block7file.createNewFile();
		//			b7Stream = new PrintStream(block7file);
		//
		//			File uut = new File("./test/fatigue/pvt_35min/UUT.txt");
		//			if (!uut.exists())
		//				uut.createNewFile();
		//			uutStream = new PrintStream(uut);
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}

		// this was for when you want to reset the numbers at each session
		// getModel().getFatigue().resetFatigueModule();
	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = getModel().getTime() - currentSession.startTime;
		currentBlock.totalBlockTime = getModel().getTime() - currentBlock.startTime;
		
		if (currentSession.totalSessionTime <= PVTduration) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			if (getModel().isVerbose())
				getModel().output("!!!!! Stimulus !!!!!");

			lastTime = getModel().getTime(); // when the stimulus has happened

			// Handling the sleep attacks -- adding an event in 30 s to see if
			// the current stimulus is still on
			currentSession.stimulusIndex++;
			addEvent(new Event(getModel().getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility == true) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						currentSession.sleepAttacks++;
						currentBlock.sleepAttacks++;
						// when sleep attack happens we add to the number of responses
						currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at time ==>" + (getModel().getTime() - currentSession.startTime)
								+ "model time :" + getModel().getTime());
						getModel().output(currentSession.stimulusIndex + " " + sleepAttackIndex);
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
					repaint();

				}
			});

			// Handling the 5-min blocks
			// adding a new block
			if (currentBlock.totalBlockTime >= 300 ) {
				currentSession.blocks.add(currentBlock);
				currentBlock = new Block();
				currentBlock.startTime = currentSession.startTime + currentSession.blockIndex * 300.0;
				currentSession.blockIndex++;
			}
		}

		// Starting a new Session
		else {
			currentSession.blocks.add(currentBlock);
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new Session();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
						getModel().getFatigue().startFatigueSession();
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
				});

			} else {
				//				b1Stream.close();
				//				b2Stream.close();
				//				b3Stream.close();
				//				b4Stream.close();
				//				b5Stream.close();
				//				b6Stream.close();
				//				b7Stream.close();
				//				uutStream.close();
				getModel().stop();
			}
		}
	}

	@Override
	public void typeKey(char c) {

		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;

			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentBlock.reactionTimes.add(responseTime);
			}

			//			if (currentSession.blocks.size() == 0)
			//				b1Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 1)
			//				b2Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 2)
			//				b3Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 3)
			//				b4Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 4)
			//				b5Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 5)
			//				b6Stream.println((int) (responseTime * 1000));
			//			if (currentSession.blocks.size() == 6)
			//				b7Stream.println((int) (responseTime * 1000));

			//			if (iteration == 1 && getModel().getProcedural().getFatigueUtility() < 4
			//					&& getModel().getProcedural().getFatigueUtilityThreshold() < 4) {
			//				uutStream.print((int) getModel().getTime() + "\t");
			//				uutStream.print((getModel().getProcedural().getFatigueUtility()) + "\t");
			//				uutStream.print((getModel().getProcedural().getFatigueUtilityThreshold()) + "\n");
			//				uutStream.flush();
			//			}

			label.setVisible(false);
			processDisplay();

			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;

			if (responseTime < .150){
				currentBlock.falseAlert++;
				currentSession.falseStarts++;
			}
			else if (responseTime > .150 && responseTime <= .500){
				// making the array for alert reaction times
				currentBlock.alertResponse[(int) ((responseTime - .150) * 100)]++;
				currentSession.alertResponse[(int) ((responseTime - .150) * 100)]++;
			}
			else if (responseTime > .500){
				currentBlock.lapses++;
				currentSession.lapses++;
			}

			// setting up the state to wait
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("wait"));

		} else {
			//			if (currentSession.blocks.size() == 0)
			//				b1Stream.println(0);
			//			if (currentSession.blocks.size() == 1)
			//				b2Stream.println(0);
			//			if (currentSession.blocks.size() == 2)
			//				b3Stream.println(0);
			//			if (currentSession.blocks.size() == 3)
			//				b4Stream.println(0);
			//			if (currentSession.blocks.size() == 4)
			//				b5Stream.println(0);
			//			if (currentSession.blocks.size() == 5)
			//				b6Stream.println(0);
			//			if (currentSession.blocks.size() == 6)
			//				b7Stream.println(0);
			currentSession.reactionTimes.add(0);
			currentBlock.reactionTimes.add(0);
			currentBlock.numberOfResponses++;
			currentBlock.falseAlert++;
			currentSession.numberOfResponses++;
			currentSession.falseStarts++;

			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}

	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		DecimalFormat df = new DecimalFormat("#.00");

		for (Task taskcast : tasks){
			PVT35min task = (PVT35min) taskcast;

			for (int i = 0; i < task.sessions.size(); i++) {

				getModel().output("******** Session number : " + i);
				Session s  = task.sessions.get(i);
				for (int j = 0; j < s.reactionTimes.size(); j++) {
					getModel().outputInLine(df.format(s.reactionTimes.get(j)) + ",");
				}
				getModel().outputInLine("\n");

				getModel().output("False Start in Session: " + s.falseStarts);
				getModel().output("Lapses in Session     : " + s.lapses);
				getModel().output("total Session time    : " + s.totalSessionTime);

				getModel().output("");
				getModel().output("*** Blocks:");
				for (int j = 0; j < s.blocks.size(); j++) {
					Block b = s.blocks.get(j);
					getModel().output("");
					getModel().output("* Block number : " + j);
					for (int k = 0; k < b.reactionTimes.size(); k++) {
						getModel().outputInLine(df.format(b.reactionTimes.get(k)) + ",");
					}
					getModel().outputInLine("\n");

					getModel().output("False Start in Block  : " + b.falseAlert);
					getModel().output("Lapses in Block       : " + b.lapses);
					getModel().output("Total Block time      : " + b.totalBlockTime);
					
				}
			}

		}


		//		getModel().output("******* Proportion of Responses **********\n");
		//		getModel().output("    FS  " + " ---------------------------    Alert Responses    --------------------------- "
		//				+ " Alert Responses "
		//				+ " ---------------------------    Alert Responses    ---------------------------- " + "L ");
		//		for (int i = 0; i < 7; i++) {
		//			double[] AlertResponsesProportion = new double[35];
		//			for (int j = 0; j < 35; j++)
		//				AlertResponsesProportion[j] = AlertResponses[i][j] / Responses[i];
		//			getModel().output("B" + (i + 1) + "|" + String.format("%.2f", FalseStarts[i] / Responses[i]) + " "
		//					+ Utilities.toString(AlertResponsesProportion) + " "
		//					+ String.format("%.2f", Lapses[i] / Responses[i]));
		//
		//			getModel().output("\n-------------------------------------------------------\n");
		//		}
		//
		//		// writing the numbers to file
		//		try {
		//
		//			File PVT35min = new File("./test/fatigue/pvt_35min/PVT35min.txt");
		//			if (!PVT35min.exists())
		//				PVT35min.createNewFile();
		//			PrintStream PVTfile = new PrintStream(PVT35min);
		//
		//			for (int i = 0; i < 7; i++) {
		//				PVTfile.print(FalseStarts[i] / Responses[i] + "\t");
		//				for (int j = 0; j < 35; j++)
		//					PVTfile.print(AlertResponses[i][j] / Responses[i] + "\t");
		//				PVTfile.print(Lapses[i] / Responses[i] + "\n");
		//			}
		//
		//			PVTfile.close();
		//
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		//
		//		for (int i = 0; i < runIterations; i++) {
		//			double responses = 0, responseTime = 0;
		//			responses = task.sessions.elementAt(i).responses;
		//			responseTime = task.sessions.elementAt(i).responseTotalTime;
		//			modelTimes[i] = (responses == 0) ? 0 : (responseTime / responses);
		//		}
		//
		//		if (output) {
		//			getModel().output("\n=========              Mean Reaction Times            ===========\n");
		//
		//			for (int i = 0; i < runIterations; i++) {
		//				getModel().output("Session " + i + "==>" + modelTimes[i]);
		//			}
		//
		//		}
		//
		Result result = new Result();
		// result.add ("PVT", modelTimes, humanTimes);
		return result;
	}

}
