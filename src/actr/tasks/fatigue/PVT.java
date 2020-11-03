package actr.tasks.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;

import actr.model.Symbol;
import actr.task.*;
import actr.tasks.fatigue.SessionPVT.Block;

/**
 * The core class for a model of PVT using fatigue mechanism 
 * to be performed by an ACT-R model.
 * <p>
 * 
 * @author Ehsan Khosroshahi
 * 
 */

public class PVT extends Task {
	private double PVTduration = 0;
	public ArrayList<Double> timesOfPVT;

	private final TaskLabel label;
	private double lastTime = 0;
	private final String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	private int sleepAttackIndex = 0; // the variable for handling sleep attacks
	private Random random;

	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private SessionPVT currentSession;
	public final Vector<SessionPVT> sessions = new Vector<>();

	/**
	 * Constructs a new task.
	 */
	public PVT() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		model.output("Staring a new session ========== "  + sessionNumber);
		random = new Random();
		lastTime = 0;
		currentSession = new SessionPVT();
		currentBlock = new Block();
		stimulusVisibility = false;
		currentSession.startTime = 0;
		currentBlock.startTime = 0;

		PVTduration = model.fatigue.getTaskDuration();
		timesOfPVT = model.fatigue.getTaskSchdule();

		model.fatigue.setFatigueStartTime(timesOfPVT.get(sessionNumber));
		model.fatigue.startFatigueSession();


		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);
	}

	@Override
	public void update(double time) {
		currentSession.totalSessionTime = model.getTime() - currentSession.startTime;
		currentBlock.totalBlockTime = model.getTime() - currentBlock.startTime;

		if (currentSession.totalSessionTime <= PVTduration) {
			label.setText(stimulus);
			label.setVisible(true);
			processDisplay();
			stimulusVisibility = true;
			if (model.isVerbose())
				model.output("!!!!! Stimulus !!!!!");

			lastTime = model.getTime(); // when the stimulus has happened

			// Handling the sleep attacks -- adding an event in 30 s to see if
			// the current stimulus is still on
			currentSession.stimulusIndex++;
			addEvent(new actr.model.Event(model.getTime() + 30.0, "task", "update") {
				@Override
				public void action() {
					sleepAttackIndex++;
					if (sleepAttackIndex == currentSession.stimulusIndex && stimulusVisibility) {
						label.setVisible(false);
						processDisplay();
						stimulusVisibility = false;
						
						currentSession.reactionTimes.add(30000);
						currentSession.timeOfReactionsFromStart.add(currentSession.totalSessionTime);
						currentBlock.blockReactionTimes.add(30000);
						currentBlock.blockTimeOfReactionsFromStart.add(currentBlock.totalBlockTime);
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						model.output("Sleep attack at session time  ==> " + (model.getTime() - currentSession.startTime)
								+ " model time :" + model.getTime());
						model.output("Stimulus index in the session ==> " + currentSession.stimulusIndex );

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage(); // reseting the system
						model.declarative.get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
						model.buffers.clear(Symbol.visual); // clearing the buffers after the sleep attack
						model.buffers.clear(Symbol.visloc); // clearing the buffers after the sleep attack
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
			model.output("Stoping session ========== "  + sessionNumber);
			currentSession.blocks.add(currentBlock);
			currentSession.bioMathValue = model.fatigue.getBioMathModelValue(timesOfPVT.get(sessionNumber));
			currentSession.timeAwake = model.fatigue.getTimeAwake(timesOfPVT.get(sessionNumber));
			currentSession.timeOfTheDay = timesOfPVT.get(sessionNumber) % 24;
			sessions.add(currentSession);
			sessionNumber++;
			model.declarative.get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.size()) {
				addEvent(new actr.model.Event(model.getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						model.output("Staring a new session ========== "  + sessionNumber);
						currentSession = new SessionPVT();
						currentBlock = new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = model.getTime();
						currentBlock.startTime = model.getTime();
						model.fatigue.setFatigueStartTime(timesOfPVT.get(sessionNumber));
						model.fatigue.startFatigueSession();

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						model.declarative.get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
						fatigueResetPercentage();
					}
				});
			} else {
				model.stop();
			}
		}
	}

	@Override
	public void eval(Iterator<String> it) {
//		it.next(); // (
		String cmd = it.next();
		switch (cmd) {
			case "fatigue-reset-percentage" -> fatigueResetPercentage();
			case "fatigue-utility-dec-on" -> model.fatigue.setRunWithUtilityDecrement(true);
			case "fatigue-utility-dec-off" -> model.fatigue.setRunWithUtilityDecrement(false);
		}
	}

	// calling percentage reset after any new task presentation (audio or visual)
	void fatigueResetPercentage() {
		model.fatigue.fatigueResetPercentages();
//		if (model.isVerbose())
//			model.output("!!!! Fatigue Percentage Reset !!!!");
	}

	@Override
	public void typeKey(char c) {
		double currentSessionTime = model.getTime() - currentSession.startTime;
		double currentBlockTime = model.getTime() - currentBlock.startTime;
		if (stimulusVisibility) {
			response = c + "";
			responseTime = model.getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (model.isVerbose() && responseTime < 150)
				model.output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (model.getTime() - currentSession.startTime));
			
//			if (responseTime > 5000){ // just for testing
//				model.output(model.getProcedural().getLastProductionFired().toString());
//				model.output("" + responseTime);
//				model.stop();				
//			}
			
			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentSession.timeOfReactionsFromStart.add(currentSessionTime);
				currentBlock.blockReactionTimes.add(responseTime);
				currentBlock.blockTimeOfReactionsFromStart.add(currentBlockTime);
			}

			label.setVisible(false);
			processDisplay();

			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentSession.timeOfReactionsFromStart.add(currentSessionTime);
			currentBlock.blockReactionTimes.add(1);
			currentBlock.blockTimeOfReactionsFromStart.add(currentBlockTime);
			if (model.isVerbose())
				model.output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (model.getTime() - currentSession.startTime));
		}
	}

	//	@Override
	//	public int analysisIterations() {
	// 		return 100;
	//	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		Result result = new Result();
		
		PVT task0 = (PVT) tasks[0]; // getting the numbers of sessions and blocks
		int numberOfSessions = task0.sessions.size();
		int numberOfBlocks = task0.sessions.get(0).blocks.size();

		Values[] ProportionLapses = new Values[numberOfSessions];
		Values[] ProportionFalseStarts = new Values[numberOfSessions];
		Values[] ProportionAlertRT = new Values[numberOfSessions];
		Values[] MedianAlertResponses = new Values[numberOfSessions];
		Values[][] ProportionAlertResponcesDis = new Values[numberOfSessions][35];
		
		Values[] NumberLapses = new Values[numberOfSessions];
		Values[] NumberFalseStarts = new Values[numberOfSessions];
		Values[] NumberAlertRT = new Values[numberOfSessions];
		Values[] NumberSleepAtacks = new Values[numberOfSessions];
		Values[][] NumberAlertResponcesDis = new Values[numberOfSessions][35];
		
		Values[] LSNRapx = new Values[numberOfSessions];
		Values[] ProportionSleepAtacks = new Values[numberOfSessions];
		
		// allocating memory to the vectors
		for (int i = 0; i < numberOfSessions; i++) {
			ProportionLapses[i] = new Values();
			ProportionFalseStarts[i] = new Values();
			ProportionAlertRT[i] = new Values();
			ProportionSleepAtacks[i] = new Values();
			
			NumberLapses[i] = new Values();
			NumberFalseStarts[i] = new Values();
			NumberAlertRT[i] = new Values();
			NumberSleepAtacks[i] = new Values();
			
			MedianAlertResponses[i] = new Values();
			LSNRapx[i] = new Values();
			
			for (int j = 0; j < 35; j++) {
				ProportionAlertResponcesDis[i][j] = new Values();
				NumberAlertResponcesDis[i][j] = new Values();
			}	
		}

		// data for the blocks
		Values [][] blocksProportionLapses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksNumberLapses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksProportionFalseStarts = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksMedianAlertResponses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksLSNRapx = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksProportionSleepAtacks = new Values[numberOfSessions][numberOfBlocks];
		Values [][][] blocksProportionAlertResponcesDis = new Values[numberOfSessions][numberOfBlocks][35];

		for (int i = 0; i < numberOfSessions; i++) {
			for (int j = 0; j < numberOfBlocks; j++) {
				blocksProportionLapses[i][j] = new Values();
				blocksNumberLapses[i][j] = new Values();
				blocksProportionFalseStarts[i][j] = new Values();
				blocksMedianAlertResponses[i][j] = new Values();
				blocksLSNRapx[i][j] = new Values();
				blocksProportionSleepAtacks[i][j] = new Values();
				for (int k = 0; k < 35; k++) {
					blocksProportionAlertResponcesDis[i][j][k] = new Values();
				}
			}
		}


		for (Task taskCast : tasks) {
			PVT task = (PVT) taskCast;
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				
				ProportionLapses[i].add(session.getProportionOfLapses());
				ProportionFalseStarts[i].add(session.getProportionOfFalseStarts());
				ProportionAlertRT[i].add(session.getProportionOfAlertResponses());
				ProportionSleepAtacks[i].add(session.getProportionOfSleepAttacks());
				
				NumberLapses[i].add(session.getNumberOfLapses());
				NumberFalseStarts[i].add(session.getNumberOfFalseStarts());
				NumberAlertRT[i].add(session.getNumberOfAlertResponses());
				NumberSleepAtacks[i].add(session.getNumberOfSleepAttacks());
				
				MedianAlertResponses[i].add(session.getMedianAlertReactionTimes());
				LSNRapx[i].add(session.getLSNR_apx());
				
				double [] proportionAlertDis = session.getProportionAlertResponseDistribution(); 
				for (int j = 0; j < 35; j++) {
					ProportionAlertResponcesDis[i][j].add(proportionAlertDis[j]);
				}

				for (int j = 0; j < session.blocks.size(); j++) {
					Block b = session.blocks.get(j);
					blocksProportionLapses[i][j].add(b.getProportionOfLapses());
					blocksNumberLapses[i][j].add(b.getNumberOfLapses());
					blocksProportionFalseStarts[i][j].add(b.getProportionOfFalseAlert());
					blocksMedianAlertResponses[i][j].add(b.getMedianAlertReactionTimes());
					blocksLSNRapx[i][j].add(b.getLSNR_apx());
					blocksProportionSleepAtacks[i][j].add(b.getProportionOfSleepAttacks());

					double[] blocksAP = b.getProportionAlertResponseDistribution();
					for (int k = 0; k < 35; k++) {
						blocksProportionAlertResponcesDis[i][j][k].add(blocksAP[k]);
					}
				}

			}
		}

		////////////////////////////////////////////////////////////

		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");

		model.output("******* The Reslut Based on the Sessions **********\n");

		model.outputInLine("Time Awake      " + "\t");
		PVT task = (PVT) tasks[0];
		for (int i = 0; i < numberOfSessions; i++) {
			SessionPVT session = task.sessions.get(i);
			model.outputInLine(session.timeAwake + "\t");
		}
		model.outputInLine("\n");
		model.outputInLine("BioMath Value   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			SessionPVT session = task.sessions.get(i);
			model.outputInLine(df2.format(session.bioMathValue) + "\t");
		}
		model.output("\n");

		
		model.outputInLine("Lapses %        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(ProportionLapses[i].mean() * 100) + "\t");
		}
		model.outputInLine("\n");

		model.outputInLine("FalseStarts %   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(ProportionFalseStarts[i].mean() * 100) + "\t");
		}
		model.outputInLine("\n");
		
		model.outputInLine("Alert RT %      " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(ProportionAlertRT[i].mean() * 100) + "\t");
		}
		model.outputInLine("\n");

		model.outputInLine("SleepAttacks %  " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(ProportionSleepAtacks[i].mean() * 100) + "\t");
		}
		model.outputInLine("\n\n");
		
		model.outputInLine("Lapses #        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(NumberLapses[i].mean()) + "\t");
		}
		model.outputInLine("\n");

		model.outputInLine("FalseStarts #   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(NumberFalseStarts[i].mean()) + "\t");
		}
		model.outputInLine("\n");
		
		model.outputInLine("Alert RT #      " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(NumberAlertRT[i].mean()) + "\t");
		}
		model.outputInLine("\n");

		model.outputInLine("SleepAttacks #  " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(NumberSleepAtacks[i].mean()) + "\t");
		}
		model.outputInLine("\n\n");
		
		
		
		model.outputInLine("Median Alert RT " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(MedianAlertResponses[i].mean()) + "\t");
		}
		model.outputInLine("\n");

		model.outputInLine("LSNR_apx        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			model.outputInLine(df2.format(LSNRapx[i].mean()) + "\t");
		}
		model.outputInLine("\n\n");
		
		////////////////////////////////////////////////////////////
		for (int i = 0; i < numberOfSessions; i++){
			model.output("Session " + (i+1) + "\t" + "Block# \tAlert RT       \t% Lapses        \t% False Starts  \t# Lapses");
			model.output("        " +         "\t" + "       \t(Median\tSD   )\t(Mean\tSD\t95CI)\t(Mean\tSD\t95CI)\tMean");
			for (int j = 0; j < numberOfBlocks; j++){
				model.output("\t\tBlock" + (j+1)
						+ "\t(" + df2.format(blocksMedianAlertResponses[i][j].mean())
						+ "\t" + df2.format(blocksMedianAlertResponses[i][j].stddev()) +")"
						+ "\t(" + df2.format(blocksProportionLapses[i][j].mean() * 100)
						+ "\t" + df2.format(blocksProportionLapses[i][j].stddev() * 100)
						+ "\t" + df2.format(blocksProportionLapses[i][j].CI_95percent() * 100) +")"
						+ "\t(" + df2.format(blocksProportionFalseStarts[i][j].mean() * 100) 
						+ "\t" + df2.format(blocksProportionFalseStarts[i][j].stddev() * 100)
						+ "\t" + df2.format(blocksProportionFalseStarts[i][j].CI_95percent() * 100) +")"
						+ "\t" + df2.format(blocksNumberLapses[i][j].mean())
						);
			}
			model.outputInLine("\n\n");
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////		
		// Writing the output to csv files in the specified directory (outputDIR)
		String DIR = model.fatigue.getOutputDIR();

		if (DIR == null)
			return result;
	
		try {
			///////////////////////////////////////////////////////////////////////////////////////////////////					
			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File(DIR + "/"  + "SessionsData.csv");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);

			dataSession.print("Time Awake" + ",");
			task = (PVT) tasks[0];
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				dataSession.print(session.timeAwake + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.print("BioMath" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(task0.sessions.get(i).bioMathValue + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("Lapses %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionLapses[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("FalseStarts %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionFalseStarts[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("Median AlertRT" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(MedianAlertResponses[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("LSNR_apx" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(LSNRapx[i].mean() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("SleepAttacks %" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionSleepAtacks[i].mean() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();


			// writing the Standard Deviations
			dataSession.println("**");
			dataSession.flush();
			
			dataSession.print("Lapses % SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionLapses[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("FalseStarts % SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionFalseStarts[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("Median AlertRT SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(MedianAlertResponses[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("LSNR_apx SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(LSNRapx[i].stddev() + ",");
			}
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("SleepAttacks % SD" + ",");
			for (int i = 0; i < numberOfSessions; i++) {
				dataSession.print(ProportionSleepAtacks[i].stddev() * 100 + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
			
			dataSession.println("\n\n\n\n");
			dataSession.flush();
			
			///////////////////////////////////////////////////////////////////////////////////////////////////					
			// Writing blocks data to the file based on sessions

			dataSession.print("Time Awake" + ",");
			task = (PVT) tasks[0];
			for (int i = 0; i < numberOfSessions; i++) {
				SessionPVT session = task.sessions.get(i);
				dataSession.print(session.timeAwake + ",");
			}
			dataSession.print("\n");
			dataSession.flush();
		
			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print( "B" + (j+1) +" Lapses %" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionLapses[i][j].mean() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) +" FalseStarts %" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionFalseStarts[i][j].mean() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) +" Median AlertRT" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksMedianAlertResponses[i][j].mean() + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " LSNR_apx" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksLSNRapx[i][j].mean() + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " SleepAttacks %" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionSleepAtacks[i][j].mean() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			// writing the Standard Deviations
			
			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " Lapses % SD" + (j+1) + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionLapses[i][j].stddev() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " FalseStarts % SD" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionFalseStarts[i][j].stddev() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " Median AlertRT SD" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksMedianAlertResponses[i][j].stddev() + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " LSNR_apx SD" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksLSNRapx[i][j].stddev() + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			for (int j = 0; j < numberOfBlocks; j++) {
				dataSession.print("B" + (j+1) + " SleepAttacks % SD" + ",");
				for (int i = 0; i < numberOfSessions; i++) {
					dataSession.print(blocksProportionSleepAtacks[i][j].stddev() * 100 + ",");
				}
				dataSession.print("\n");
				dataSession.flush();
			}

			dataSession.print("\n\n");
			dataSession.close();


			///////////////////////////////////////////////////////////////////////////////////////////////////					
			// Writing distribution data to the file based on sessions
			File SessionDisFile = new File(DIR+ "/SessionsDis.csv");
			if (!SessionDisFile.exists())
				SessionDisFile.createNewFile();
			PrintStream SessionDis = new PrintStream(SessionDisFile);
		
			SessionDis.println("Session#,FA,150,160,170,180,190,200,210,220,230,240,250,260,270,280,290,300,"
					+ "310,320,330,340,350,360,370,380,390,400,410,420,430,440,450,460,470,480,490,Lapses,SA");
			for (int i = 0; i < numberOfSessions; i++) {
				SessionDis.print("Session" + (i+1) + "," + ProportionFalseStarts[i].average() + ",");
				for (int k = 0; k < 35; k++)
					SessionDis.print(ProportionAlertResponcesDis[i][k].average() + ",");
				SessionDis.print(ProportionLapses[i].average() + ",");
				SessionDis.print(ProportionSleepAtacks[i].average() + "\n");
				SessionDis.flush();
			}
			SessionDis.print("\n\n");
			SessionDis.flush();

			
			SessionDis.println("Session#,Block#,FA,150,160,170,180,190,200,210,220,230,240,250,260,270,280,290,300,"
					+ "310,320,330,340,350,360,370,380,390,400,410,420,430,440,450,460,470,480,490,Lapses,SA");
			for (int i = 0; i < numberOfSessions; i++) {
				SessionDis.print("Session" +(i+1));
				for (int j = 0; j < numberOfBlocks; j++) {
					SessionDis.print(",Block" + (j+1) + "," + blocksProportionFalseStarts[i][j].average() + ",");
					for (int k = 0; k < 35; k++)
						SessionDis.print(blocksProportionAlertResponcesDis[i][j][k].average() + ",");
					SessionDis.print(blocksProportionLapses[i][j].average() + ",");
					SessionDis.print(blocksProportionSleepAtacks[i][j].average() + "\n");
				}
				SessionDis.print("\n");
				SessionDis.flush();
			}
			SessionDis.close();
			
			///////////////////////////////////////////////////////////////////////////////////////////////////	
			// saving the raw times of lapses
			File rawLapsesFile = new File(DIR + "/SessionsRawLapsesTimes.csv");
			if (!rawLapsesFile.exists())
				rawLapsesFile.createNewFile();
			PrintStream rawLapses = new PrintStream(rawLapsesFile);

			rawLapses.println("session#,TimeInSession,RT");
			
			for (Task taskCast : tasks) {
				PVT taskLapses = (PVT) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					SessionPVT session = taskLapses.sessions.get(i);
					for (int j = 0; j < session.reactionTimes.size(); j++) {
						if (session.reactionTimes.get(j) > 500 && session.reactionTimes.get(j) < 30000){
							rawLapses.println(i + "," + session.timeOfReactionsFromStart.get(j) + "," + session.reactionTimes.get(j));
							rawLapses.flush();
						}		
					}
				}
			}
			rawLapses.close();

			///////////////////////////////////////////////////////////////////////////////////////////////////	
			// the output file for all the data: RTMedian / % Lapses Mean and SD/ % FalseStarts Mean and SD
			File blockFileTotalPercent = new File(DIR + "/SessionsBlockData.csv");
			if (!blockFileTotalPercent.exists())
				blockFileTotalPercent.createNewFile();
			PrintStream blockPercent = new PrintStream(blockFileTotalPercent);
			for (int i = 0; i < numberOfSessions; i++){
				blockPercent.println("Session" + (i+1) + ",Block#,Alert RT Medain,% L Mean,% L SD,95% CI, % FS Mean,% FS SD,% FS 95% CI");
				for (int j = 0; j < numberOfBlocks; j++){
					blockPercent.println(",Block" + (j+1) 
							+ "," + blocksMedianAlertResponses[i][j].mean()
							+ "," + blocksProportionLapses[i][j].mean() * 100
							+ "," + blocksProportionLapses[i][j].stddev() * 100
							+ "," + blocksProportionLapses[i][j].CI_95percent() * 100
							+ "," + blocksProportionFalseStarts[i][j].mean() * 100 
							+ "," + blocksProportionFalseStarts[i][j].stddev() * 100
							+ "," + blocksProportionFalseStarts[i][j].CI_95percent() * 100
							);
					blockPercent.flush();
				}
				blockPercent.print("\n\n");
			}
			blockPercent.close();


		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

}
