package actr.tasks.test.fatigue;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.test.fatigue.SessionPVT.Block;

/**
 * Model of PVT test and Fatigue mechanism 
 * 
 * @author Ehsan Khosroshahi
 * 
 */

public class PVT extends Task {
	private double PVTduration = 0;
	private ArrayList<Double> timesOfPVT;

	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	private int sleepAttackIndex = 0; // the variable for handling sleep attacks
	Random random;

	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private SessionPVT currentSession;
	private Vector<SessionPVT> sessions = new Vector<SessionPVT>();

	public PVT() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	@Override
	public void start() {
		random = new Random();
		lastTime = 0;
		currentSession = new SessionPVT();
		currentBlock = currentSession.new Block();
		stimulusVisibility = false;
		currentSession.startTime = 0;
		currentBlock.startTime = 0;

		PVTduration = getModel().getFatigue().getTaskDuration();
		timesOfPVT = getModel().getFatigue().getTaskSchdule();

		getModel().getFatigue().setFatigueStartTime(timesOfPVT.get(sessionNumber));
		getModel().getFatigue().startFatigueSession();


		interStimulusInterval = random.nextDouble() * 8 + 2; // A random
		addUpdate(interStimulusInterval);
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
						
						currentSession.reactionTimes.add(30000);
						currentSession.timeOfReactionsFromStart.add(currentSession.totalSessionTime);
						currentBlock.blockReactionTimes.add(30000);
						currentBlock.blockTimeOfReactionsFromStart.add(currentBlock.totalBlockTime);
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at session time  ==> " + (getModel().getTime() - currentSession.startTime)
								+ " model time :" + getModel().getTime());
						getModel().output("Stimulus index in the session ==> " + currentSession.stimulusIndex );

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage(); // reseting the system
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
						getModel().getBuffers().clear(Symbol.visual); // clearing the buffers after the sleep attack
						getModel().getBuffers().clear(Symbol.visloc); // clearing the buffers after the sleep attack
					}
					repaint();
				}
			});

			// Handling the 5-min blocks
			// adding a new block
			if (currentBlock.totalBlockTime >= 300 ) {
				currentSession.blocks.add(currentBlock);
				currentBlock = currentSession.new Block();
				currentBlock.startTime = currentSession.startTime + currentSession.blockIndex * 300.0;
				currentSession.blockIndex++;
			}
		}

		// Starting a new Session
		else {
			currentSession.blocks.add(currentBlock);
			currentSession.bioMathValue = getModel().getFatigue().getBioMathModelValueforHour(timesOfPVT.get(sessionNumber));
			currentSession.timeAwake = getModel().getFatigue().getTimeAwake(timesOfPVT.get(sessionNumber));
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.size()) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new SessionPVT();
						currentBlock = currentSession.new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						currentBlock.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueStartTime(timesOfPVT.get(sessionNumber));
						getModel().getFatigue().startFatigueSession();

						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
					}
				});
			} else {
				getModel().stop();
			}
		}
	}

	@Override
	public void eval(Iterator<String> it) {
		it.next(); // (
		String cmd = it.next();
		if (cmd.equals("fatigue-reset-percentage")) {
			fatigueResetPercentage();
		}else if (cmd.equals("fatigue-utility-dec-on")) {
			getModel().getFatigue().setRunWithUtilityDecrement(true);
		}else if (cmd.equals("fatigue-utility-dec-off")) {
			getModel().getFatigue().setRunWithUtilityDecrement(false);
		}
	}

	// calling percentage reset after any new task presentation (audio or visual)
	void fatigueResetPercentage() {
		getModel().getFatigue().fatigueResetPercentages();
//		if (getModel().isVerbose())
//			getModel().output("!!!! Fatigue Percentage Reset !!!!");
	}

	@Override
	public void typeKey(char c) {
		double currentSessionTime = getModel().getTime() - currentSession.startTime;
		double currentBlockTime = getModel().getTime() - currentBlock.startTime;
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (getModel().isVerbose() && responseTime < 150)
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
			
//			if (responseTime > 5000){ // just for testing
//				getModel().output(getModel().getProcedural().getLastProductionFired().toString());
//				getModel().output("" + responseTime);
//				getModel().stop();				
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
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}
	}

	//	@Override
	//	public int analysisIterations() {
	// 		return 100;
	//	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

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

					double blocksAP[] = b.getProportionAlertResponseDistribution();
					for (int k = 0; k < 35; k++) {
						blocksProportionAlertResponcesDis[i][j][k].add(blocksAP[k]);
					}
				}

			}
		}

		////////////////////////////////////////////////////////////

		DecimalFormat df2 = new DecimalFormat("#.00");
		DecimalFormat df3 = new DecimalFormat("#.000");

		getModel().output("******* The Reslut Based on the Sessions **********\n");

		getModel().outputInLine("Time Awake      " + "\t");
		PVT task = (PVT) tasks[0];
		for (int i = 0; i < numberOfSessions; i++) {
			SessionPVT session = task.sessions.get(i);
			getModel().outputInLine(session.timeAwake + "\t");
		}
		getModel().outputInLine("\n");
		getModel().outputInLine("BioMath Value   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			SessionPVT session = task.sessions.get(i);
			getModel().outputInLine(df2.format(session.bioMathValue) + "\t");
		}
		getModel().output("\n");

		
		getModel().outputInLine("Lapses %        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(ProportionLapses[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("FalseStarts %   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(ProportionFalseStarts[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");
		
		getModel().outputInLine("Alert RT %      " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(ProportionAlertRT[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("SleepAttacks %  " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(ProportionSleepAtacks[i].mean() * 100) + "\t");
		}
		getModel().outputInLine("\n\n");
		
		getModel().outputInLine("Lapses #        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(NumberLapses[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("FalseStarts #   " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(NumberFalseStarts[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");
		
		getModel().outputInLine("Alert RT #      " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(NumberAlertRT[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("SleepAttacks #  " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(NumberSleepAtacks[i].mean()) + "\t");
		}
		getModel().outputInLine("\n\n");
		
		
		
		getModel().outputInLine("Median Alert RT " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(MedianAlertResponses[i].mean()) + "\t");
		}
		getModel().outputInLine("\n");

		getModel().outputInLine("LSNR_apx        " + "\t");
		for (int i = 0; i < numberOfSessions; i++) {
			getModel().outputInLine(df2.format(LSNRapx[i].mean()) + "\t");
		}
		getModel().outputInLine("\n\n");
		
		////////////////////////////////////////////////////////////
		for (int i = 0; i < numberOfSessions; i++){
			getModel().output("Session " + (i+1) + "\t" + "Block# \tAlert RT       \t% Lapses        \t% False Starts  \t# Lapses");
			getModel().output("        " +         "\t" + "       \t(Median\tSD   )\t(Mean\tSD\t95CI)\t(Mean\tSD\t95CI)\tMean");
			for (int j = 0; j < numberOfBlocks; j++){
				getModel().output("\t\tBlock" + (j+1)
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
			getModel().outputInLine("\n\n");
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////		
		// Writing the output to csv files in the specified directory (outputDIR)
		Result result = new Result();
		String DIR = getModel().getFatigue().getOutputDIR();

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
