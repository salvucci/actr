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
 * Model of PVT test on time on task
 * 
 * Paper: Functional Equivalence of sleep loss and time on task effects in sustained attention
 * 
 * Bella Z. Vaksler, Glenn Gunzelmann, Air force research lab
 * 
 * @author Ehsan Khosroshahi
 */



public class PVT35min extends Task {
	private double PVTduration = 2100.0;
	private double[] timesOfPVT = {
			24.0 + 10    // setting the time of the PVT at noon in the 2nd day
	};
	
	private TaskLabel label;
	private double lastTime = 0;
	private String stimulus = "\u2588";
	private double interStimulusInterval = 0.0;
	private Boolean stimulusVisibility = false;
	private String response = null;
	private double responseTime = 0;
	private int sleepAttackIndex = 0;// the variable for handling sleep attacks
	Random random;

	int sessionNumber = 0; // starts from 0
	private Block currentBlock;
	private SessionPVT currentSession;
	private Vector<SessionPVT> sessions = new Vector<SessionPVT>();
	
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
		currentSession = new SessionPVT();
		currentBlock = currentSession.new Block();
		stimulusVisibility = false;
		currentSession.startTime = 0;
		currentBlock.startTime = 0;

		getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
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
						currentSession.sleepAttacks++;
						currentBlock.sleepAttacks++;
						// when sleep attack happens we add to the number of responses (NOT DOING IT FOR NOW)
						// currentSession.numberOfResponses++; 
						getModel().output("Sleep attack at session time  ==> " + (getModel().getTime() - currentSession.startTime)
								+ " model time :" + getModel().getTime());
						getModel().output("Stimulus index in the session ==> " + currentSession.stimulusIndex );
						
						interStimulusInterval = random.nextDouble() * 8 + 2; // A random
						addUpdate(interStimulusInterval);
						fatigueResetPercentage(); // reseting the system
						getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"),Symbol.get("wait"));
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
			sessions.add(currentSession);
			sessionNumber++;
			getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("none"));
			// go to the next session or stop the model
			if (sessionNumber < timesOfPVT.length) {
				addEvent(new Event(getModel().getTime() + 60.0, "task", "update") {
					@Override
					public void action() {
						currentSession = new SessionPVT();
						currentBlock = currentSession.new Block();
						stimulusVisibility = false;
						sleepAttackIndex = 0;
						currentSession.startTime = getModel().getTime();
						currentBlock.startTime = getModel().getTime();
						getModel().getFatigue().setFatigueHour(timesOfPVT[sessionNumber]);
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
		}
	}

	// calling percentage reset after any new task presentation (audio or visual)
	void fatigueResetPercentage() {
		getModel().getFatigue().fatigueResetPercentages();
		if (getModel().isVerbose())
			getModel().output("!!!! Fatigue Percentage Reset !!!!");
	}

	@Override
	public void typeKey(char c) {
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentBlock.blockReactionTimes.add(responseTime);
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentBlock.blockReactionTimes.add(1);
			if (getModel().isVerbose())
				getModel().output("False alert happened " + "- Session: " + sessionNumber + " Block:" + (currentSession.blocks.size() + 1)
						+ "   time of session : " + (getModel().getTime() - currentSession.startTime));
		}
	}

	@Override
	public int analysisIterations() {
 		return 100;
	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {
		DecimalFormat df = new DecimalFormat("#.00");
		// getting the numbers of sessions and blocks
		PVT35min t = (PVT35min) tasks[0];
		int numberOfSessions = t.sessions.size();
		int numberOfBlocks = t.sessions.get(0).blocks.size();
		
		Values [][] blocksFalseStarts = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksLapses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksMeanAlertResponses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksMedianAlertResponses = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksFalseStartsProportion = new Values[numberOfSessions][numberOfBlocks];
		Values [][] blocksLapsesProportion = new Values[numberOfSessions][numberOfBlocks];
		Values [][][] blocksAlertProportion = new Values[numberOfSessions][numberOfBlocks][35];
		
		for (int i = 0; i < numberOfSessions; i++) {
			for (int j = 0; j < numberOfBlocks; j++) {
				blocksFalseStarts[i][j] = new Values();
				blocksLapses[i][j] = new Values();
				blocksMeanAlertResponses[i][j] = new Values();
				blocksMedianAlertResponses[i][j] = new Values();
				blocksFalseStartsProportion[i][j] = new Values();
				blocksLapsesProportion[i][j] = new Values();
				for (int k = 0; k < 35; k++) {
					blocksAlertProportion[i][j][k] = new Values();
				}
			}
		}
		
		for (Task taskcast : tasks){
			PVT35min task = (PVT35min) taskcast;
			for (int i = 0; i < task.sessions.size(); i++) {
				getModel().output("******** Session number : " + i);
				SessionPVT s  = task.sessions.get(i);
				for (int j = 0; j < s.reactionTimes.size(); j++) {
					getModel().outputInLine(df.format(s.reactionTimes.get(j)) + ",");
				}
				getModel().outputInLine("\n");
				getModel().output("False Start in Session   : " + s.getNumberOfFalseAlerts());
				getModel().output("Lapses in Session        : " + s.getNumberOfLapses());
				getModel().output("Sleep Attacks in Session : " + s.sleepAttacks);
				getModel().output("total Session time       : " + s.totalSessionTime);
				getModel().output("–––––––––––––––––––––––––––––––––––\n");
				getModel().output("******** Blocks:");
				for (int j = 0; j < s.blocks.size(); j++) {
					Block b = s.blocks.get(j);	
					blocksFalseStarts[i][j].add(b.getNumberOfFalseAlerts());
					blocksLapses[i][j].add(b.getNumberOfLapses());
					blocksMeanAlertResponses[i][j].add(b.getMeanAlertReactionTimes());
					blocksMedianAlertResponses[i][j].add(b.getMedianAlertReactionTimes());
					blocksFalseStartsProportion[i][j].add(b.getProportionOfFalseAlert());
					blocksLapsesProportion[i][j].add(b.getProportionOfLapses());
					double blocksAP[] = b.getProportionAlertResponseDistribution();
					for (int k = 0; k < 35; k++) {
						blocksAlertProportion[i][j][k].add(blocksAP[k]);
					}
					getModel().output("");
					getModel().output("**** Block number : " + j);
					for (int k = 0; k < b.blockReactionTimes.size(); k++) {
						getModel().outputInLine(df.format(b.blockReactionTimes.get(k)) + ",");
					}
					getModel().outputInLine("\n");

					getModel().output("False Start in Block   : " + b.getNumberOfFalseAlerts());
					getModel().output("Lapses in Block        : " + b.getNumberOfLapses());
					getModel().output("Sleep Attacks in Block   : " + b.sleepAttacks);
					getModel().output("total Session time       : " + b.totalBlockTime);
					getModel().output("Alert Proportions        : ");
					for (int k = 0; k < 35; k++)
						getModel().outputInLine(df.format(blocksAP[k]) + " ");
					getModel().outputInLine("\n");
				}
				getModel().output("\n**********************************************\n");
			}
			getModel().output("\n################################################\n");
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
		
		// writing the numbers to file
		try {

			File PVT35min = new File("./test/fatigue/pvt_35min/PVT35min.txt");
			if (!PVT35min.exists())
				PVT35min.createNewFile();
			PrintStream PVTfile = new PrintStream(PVT35min);
			for (int i = 0; i < numberOfSessions; i++) {
				for (int j = 0; j < numberOfBlocks; j++) {
					PVTfile.print(blocksFalseStartsProportion[i][j].average() + "\t");
					for (int k = 0; k < 35; k++)
						PVTfile.print(blocksAlertProportion[i][j][k].average() + "\t");
					PVTfile.print(blocksLapsesProportion[i][j].average() + "\n");	
				}
				PVTfile.print("\n\n");	
			}
			PVTfile.close();
			
			
			// Writing the stream of reaction times to a text file for getting CDF
			File blockFile = new File("./test/fatigue/pvt_35min/BlockStream.txt");
			if (!blockFile.exists())
				blockFile.createNewFile();
			PrintStream blockStream = new PrintStream(blockFile);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					for (Task taskcast : tasks){
						PVT35min task = (PVT35min) taskcast;
						Values RT = task.sessions.get(i).blocks.elementAt(j).blockReactionTimes;
						for (int k = 0; k < RT.size(); k++) {
							blockStream.print((int)(RT.get(k)) + "\t");
						}
					}
					blockStream.flush();
					blockStream.print("\n");
				}
				blockStream.print("\n\n");
			}
			blockStream.close();

			// Writing the block averages
			File blockFileLapsesPercent = new File("./test/fatigue/pvt_35min/BlockLapsesPercent.txt");
			if (!blockFileLapsesPercent.exists())
				blockFileLapsesPercent.createNewFile();
			PrintStream blockLapsesPercent = new PrintStream(blockFileLapsesPercent);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockLapsesPercent.print(blocksLapsesProportion[i][j].average() * 100 + "\n");
					blockLapsesPercent.flush();
				}
				blockLapsesPercent.print("\n\n");
			}
			blockLapsesPercent.close();
			
			File blockFileLapsesPercentSD = new File("./test/fatigue/pvt_35min/BlockLapsesPercentSD.txt");
			if (!blockFileLapsesPercentSD.exists())
				blockFileLapsesPercentSD.createNewFile();
			PrintStream blockLapsesPercentSD = new PrintStream(blockFileLapsesPercentSD);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockLapsesPercentSD.print(blocksLapsesProportion[i][j].stddev() * 100 + "\n");
					blockLapsesPercentSD.flush();
				}
				blockLapsesPercentSD.print("\n\n");
			}
			blockLapsesPercentSD.close();
			
			
			File blockFileMeanRT = new File("./test/fatigue/pvt_35min/BlockMeanRT.txt");
			if (!blockFileMeanRT.exists())
				blockFileMeanRT.createNewFile();
			PrintStream blockMeanRT = new PrintStream(blockFileMeanRT);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockMeanRT.print(blocksMeanAlertResponses[i][j].average() + "\n");
					blockMeanRT.flush();
				}
				blockMeanRT.print("\n\n");
			}
			blockMeanRT.close();
			
			File blockFileFalseStartsPercent = new File("./test/fatigue/pvt_35min/BlockFalseStartsPercent.txt");
			if (!blockFileFalseStartsPercent.exists())
				blockFileFalseStartsPercent.createNewFile();
			PrintStream blockFalseStartsPercent = new PrintStream(blockFileFalseStartsPercent);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockFalseStartsPercent.print(blocksFalseStartsProportion[i][j].average() * 100 + "\n");
					blockFalseStartsPercent.flush();
				}
				blockFalseStartsPercent.print("\n\n");
			}
			blockFalseStartsPercent.close();
			
			File blockFileFalseStartsPercentSD = new File("./test/fatigue/pvt_35min/BlockFalseStartsPercentSD.txt");
			if (!blockFileFalseStartsPercentSD.exists())
				blockFileFalseStartsPercentSD.createNewFile();
			PrintStream blockFalseStartsPercentSD = new PrintStream(blockFileFalseStartsPercentSD);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockFalseStartsPercentSD.print(blocksFalseStartsProportion[i][j].stddev() * 100 + "\n");
					blockFalseStartsPercentSD.flush();
				}
				blockFalseStartsPercentSD.print("\n\n");
			}
			blockFalseStartsPercentSD.close();
			
			// the output file for all the data: RTMedian / % Lapses Mean and SD/ % FalseStarts Mean and SD
			File blockFileTotalPercent = new File("./test/fatigue/pvt_35min/BlockTotalPercent.txt");
			if (!blockFileTotalPercent.exists())
				blockFileTotalPercent.createNewFile();
			PrintStream blockPercent = new PrintStream(blockFileTotalPercent);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					blockPercent.print(j+1 
							+ "\t" + blocksMedianAlertResponses[i][j].mean()
							+ "\t" + blocksLapsesProportion[i][j].mean() * 100
							+ "\t" + blocksLapsesProportion[i][j].stddev() * 100
							+ "\t" + blocksLapsesProportion[i][j].CI_95percent() * 100
							+ "\t" + blocksFalseStartsProportion[i][j].mean() * 100 
							+ "\t" + blocksFalseStartsProportion[i][j].stddev() * 100
							+ "\t" + blocksFalseStartsProportion[i][j].CI_95percent() * 100
							+ "\n");
					blockPercent.flush();
				}
				blockPercent.print("\n\n");
			}
			blockPercent.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}

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
		
		Result result = new Result();
		// result.add ("PVT", modelTimes, humanTimes);
		return result;
	}
	

}