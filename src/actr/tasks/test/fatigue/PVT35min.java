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
		
		PVTduration = getModel().getFatigue().getTaskDuration();
		timesOfPVT = getModel().getFatigue().getTaskSchdule();
		
		getModel().getFatigue().setFatigueHour(timesOfPVT.get(sessionNumber));
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
						currentBlock.reactionTimes.add(30000);
						currentBlock.timeOfReactionsFromStart.add(currentBlock.totalBlockTime);
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
						getModel().getFatigue().setFatigueHour(timesOfPVT.get(sessionNumber));
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
		double currentSessionTime = getModel().getTime() - currentSession.startTime;
		double currentBlockTime = getModel().getTime() - currentBlock.startTime;
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime;
			responseTime *= 1000; //Changing the scale to Millisecond
			
			if (response != null) {
				currentSession.numberOfResponses++;
				currentBlock.numberOfResponses++;
				currentSession.responseTotalTime += responseTime;
				currentSession.reactionTimes.add(responseTime);
				currentSession.timeOfReactionsFromStart.add(currentSessionTime);
				currentBlock.reactionTimes.add(responseTime);
				currentBlock.timeOfReactionsFromStart.add(currentBlockTime);
			}

			label.setVisible(false);
			processDisplay();
			
			interStimulusInterval = random.nextDouble() * 8 + 2; // A random
			addUpdate(interStimulusInterval);
			stimulusVisibility = false;
		} else {   // False start situation
			currentSession.reactionTimes.add(1);
			currentSession.timeOfReactionsFromStart.add(currentSessionTime);
			currentBlock.reactionTimes.add(1);
			currentBlock.timeOfReactionsFromStart.add(currentBlockTime);
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
		Values [][][] blocksAlertDistributionProportion = new Values[numberOfSessions][numberOfBlocks][35];
		
		for (int i = 0; i < numberOfSessions; i++) {
			for (int j = 0; j < numberOfBlocks; j++) {
				blocksFalseStarts[i][j] = new Values();
				blocksLapses[i][j] = new Values();
				blocksMeanAlertResponses[i][j] = new Values();
				blocksMedianAlertResponses[i][j] = new Values();
				blocksFalseStartsProportion[i][j] = new Values();
				blocksLapsesProportion[i][j] = new Values();
				for (int k = 0; k < 35; k++) {
					blocksAlertDistributionProportion[i][j][k] = new Values();
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
				getModel().output("False Start in Session   : " + s.getNumberOfFalseStarts());
				getModel().output("Lapses in Session        : " + s.getNumberOfLapses());
				getModel().output("Sleep Attacks in Session : " + s.getNumberOfSleepAttacks());
				getModel().output("total Session time       : " + s.totalSessionTime);
				getModel().output("–––––––––––––––––––––––––––––––––––\n");
				getModel().output("******** Blocks:");
				for (int j = 0; j < s.blocks.size(); j++) {
					Block b = s.blocks.get(j);	
					blocksFalseStarts[i][j].add(b.getNumberOfFalseStarts());
					blocksLapses[i][j].add(b.getNumberOfLapses());
					blocksMeanAlertResponses[i][j].add(b.getMeanAlertReactionTimes());
					blocksMedianAlertResponses[i][j].add(b.getMedianAlertReactionTimes());
					blocksFalseStartsProportion[i][j].add(b.getProportionOfFalseAlert());
					blocksLapsesProportion[i][j].add(b.getProportionOfLapses());
					double blocksAP[] = b.getProportionAlertResponseDistribution();
					for (int k = 0; k < 35; k++) {
						blocksAlertDistributionProportion[i][j][k].add(blocksAP[k]);
					}
					getModel().output("");
					getModel().output("**** Block number : " + j);
					for (int k = 0; k < b.reactionTimes.size(); k++) {
						getModel().outputInLine(df.format(b.reactionTimes.get(k)) + ",");
					}
					getModel().outputInLine("\n");

					getModel().output("False Start in Block   : " + b.getNumberOfFalseStarts());
					getModel().output("Lapses in Block        : " + b.getNumberOfLapses());
					getModel().output("Sleep Attacks in Block   : " + b.getNumberOfSleepAttacks());
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



		for (int i = 0; i < numberOfSessions; i++){
			getModel().output("******* Distribution of Response proportions **********\n");
			getModel().output("    FS  " + " ---------------------------    Alert Responses    --------------------------- "
					+ " Alert Responses "
					+ " ---------------------------    Alert Responses    ---------------------------- " + "L ");
			for (int j = 0; j < numberOfBlocks; j++) {
				double[] AlertResponsesProportion = new double[35];
				for (int k = 0; k < 35; k++)
					AlertResponsesProportion[k] = blocksAlertDistributionProportion[i][j][k].mean();
				
				getModel().output("B" + (j + 1) + "|" + String.format("%.2f", blocksFalseStartsProportion[i][j].mean()) + " "
						+ Utilities.toString(AlertResponsesProportion) + " "
						+ String.format("%.2f", blocksLapsesProportion[i][j].mean()));
			}
			getModel().output("\n-------------------------------------------------------\n");
		}

		// writing the numbers to file
		try {

			File PVT35minDitribution = new File("./test/fatigue/pvt_35min/PVT35minDistribution.csv");
			if (!PVT35minDitribution.exists())
				PVT35minDitribution.createNewFile();
			PrintStream PVTfile = new PrintStream(PVT35minDitribution);
			for (int i = 0; i < numberOfSessions; i++) {
				PVTfile.println("Block#,FA,150,160,170,180,190,200,210,220,230,240,250,260,270,280,290,300,"
						+ "310,320,330,340,350,360,370,380,390,400,410,420,430,440,450,460,470,480,490,Lapses,SA");
				for (int j = 0; j < numberOfBlocks; j++) {
					PVTfile.print("Block" + (j+1) + "," + blocksFalseStartsProportion[i][j].average() + ",");
					for (int k = 0; k < 35; k++)
						PVTfile.print(blocksAlertDistributionProportion[i][j][k].average() + ",");
					PVTfile.print(blocksLapsesProportion[i][j].average() + "\n");
					
				}
				PVTfile.print("\n\n");	
			}
			PVTfile.close();

			// the output file for all the data: RTMedian / % Lapses Mean and SD/ % FalseStarts Mean and SD
			File blockFileTotalPercent = new File("./test/fatigue/pvt_35min/BlockPercent.csv");
			if (!blockFileTotalPercent.exists())
				blockFileTotalPercent.createNewFile();
			PrintStream blockPercent = new PrintStream(blockFileTotalPercent);
			for (int i = 0; i < numberOfSessions; i++){
				blockPercent.println("Block#,Alert RT Medain,% L Mean,% L SD,95% CI, % FS Mean,% FS SD,% FS 95% CI");
				for (int j = 0; j < numberOfBlocks; j++){
					blockPercent.println("Block" + (j+1) 
							+ "," + blocksMedianAlertResponses[i][j].mean()
							+ "," + blocksLapsesProportion[i][j].mean() * 100
							+ "," + blocksLapsesProportion[i][j].stddev() * 100
							+ "," + blocksLapsesProportion[i][j].CI_95percent() * 100
							+ "," + blocksFalseStartsProportion[i][j].mean() * 100 
							+ "," + blocksFalseStartsProportion[i][j].stddev() * 100
							+ "," + blocksFalseStartsProportion[i][j].CI_95percent() * 100
							);
					blockPercent.flush();
				}
				blockPercent.print("\n\n");
			}
			blockPercent.close();


			// Writing the stream of reaction times to a text file for getting CDF
			File blockFile = new File("./test/fatigue/pvt_35min/BlockStream.txt");
			if (!blockFile.exists())
				blockFile.createNewFile();
			PrintStream blockStream = new PrintStream(blockFile);
			for (int i = 0; i < numberOfSessions; i++){
				for (int j = 0; j < numberOfBlocks; j++){
					for (Task taskcast : tasks){
						PVT35min task = (PVT35min) taskcast;
						Values RT = task.sessions.get(i).blocks.elementAt(j).reactionTimes;
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
			
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Result result = new Result();
		// result.add ("PVT", modelTimes, humanTimes);
		return result;
	}
	

}