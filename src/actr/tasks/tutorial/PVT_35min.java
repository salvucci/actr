package actr.tasks.tutorial;

import java.text.DecimalFormat;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;



import actr.model.Chunk;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.tutorial.DualChoicePRP.Trial;

public class PVT_35min extends Task {

	TaskLabel label;
	double lastTime = 0;
	String stimulus = "\u2588";
	double interStimulusInterval = 0.0;

	Boolean stimulusVisibility = false;

	int trail ;
	int iteration ;
	final int numberOfBlocks =7 ;
	final int runIterations = 150; 
	String response = null; 
	double responseTime = 0;
	
	
	
	Block currentBlock;
	Session currentSession;
	Vector<Session> sessions = new Vector<Session>();
	
	PrintStream fileB1;
	PrintStream fileB2;
	PrintStream fileB3;
	PrintStream fileB4;
	PrintStream fileB5;
	PrintStream fileB6;
	PrintStream fileB7;
	PrintStream utility_utilityThreshold;
	
	class Block {
		
		double startTime ;
		double totalBlockTime ;
		int FalseAlert = 0;
		int alertResponse[] = new int[35]; // Alert responses (150-500ms, 10ms
											// intervals )
		int lapses = 0;
		int Bresponces = 0; 
	}
	
	class Session{
		Vector<Block> blocks = new Vector<Block>();
		int responses = 0;
		double responseTotalTime = 0;
	}
	
	public PVT_35min() {
		super();
		label = new TaskLabel("", 200, 150, 40, 20);
		add(label);
		label.setVisible(false);
	}

	public void start() {
		iteration = 1;
		trail =0;
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
			fileB1 = new PrintStream(block1file);

			File block2file = new File("./PVTmodel/Block2.txt");
			if (!block2file.exists())
				block2file.createNewFile();
			fileB2 = new PrintStream(block2file);

			File block3file = new File("./PVTmodel/Block3.txt");
			if (!block3file.exists())
				block3file.createNewFile();
			fileB3 = new PrintStream(block3file);

			File block4file = new File("./PVTmodel/Block4.txt");
			if (!block4file.exists())
				block4file.createNewFile();
			fileB4 = new PrintStream(block4file);

			File block5file = new File("./PVTmodel/Block5.txt");
			if (!block5file.exists())
				block5file.createNewFile();
			fileB5 = new PrintStream(block5file);

			File block6file = new File("./PVTmodel/Block6.txt");
			if (!block6file.exists())
				block6file.createNewFile();
			fileB6 = new PrintStream(block6file);

			File block7file = new File("./PVTmodel/Block7.txt");
			if (!block7file.exists())
				block7file.createNewFile();
			fileB7 = new PrintStream(block7file);
			
			File U_UT = new File("./PVTmodel/utility_utilityThreshold.txt");
			if (!U_UT.exists())
				U_UT.createNewFile();
			utility_utilityThreshold = new PrintStream(U_UT);		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// this was for when you want to reset the numbers at each session 
		//getModel().getFatigue().reset_fatigue_module();
	}

	public void update(double time) {
		if (iteration <= runIterations){
			
			// a session 
			if (trail<340) {
				currentBlock.totalBlockTime = getModel().getTime() - currentBlock.startTime;
				// adding a new block 
				if (currentBlock.totalBlockTime > 300 && currentSession.blocks.size()<6){
					currentSession.blocks.add(currentBlock);
					currentBlock = new Block();
					currentBlock.startTime = getModel().getTime()  ;
					trail++;
					addUpdate(0.5);
					
				}
				
				else {
					label.setText(stimulus);
					label.setVisible(true);
					stimulusVisibility = true;
					processDisplay();
					trail++;
					lastTime = getModel().getTime();
					// setting up the state to wait 
					getModel().getDeclarative().get(Symbol.get("goal")).set(Symbol.get("state"), Symbol.get("stimulus"));
					
				}
			}
			// Starting a new Session
			else {
				currentSession.blocks.add(currentBlock);
				
//				System.out.println("	Block size ==>" + currentSession.blocks.size());
//				System.out.println("Session #  ==> " + iteration);
//				System.out.println("Responses ==> " + currentSession.responses);
//				System.out.println("Responces Total Time  ==> " + currentSession.responseTotalTime);
//				System.out.println("---------------------------------------");
//				for (int i = 0; i < currentSession.blocks.size(); i++) {
//					System.out.println("	Block # ==>" + (i+1));
//					System.out.println("	Block Start Time ==>" + currentSession.blocks.elementAt(i).startTime);
//					System.out.println("	TotalBlockTime ==>" + currentSession.blocks.elementAt(i).totalBlockTime);
//					System.out.println("	Block Responces  ==> " + currentSession.blocks.elementAt(i).Bresponces);
//					System.out.println("	False Alerts  ==> " + currentSession.blocks.elementAt(i).FalseAlert);
//					System.out.println("	Lapses ==> " + currentSession.blocks.elementAt(i).lapses);
//					System.out.println("	............................................");
//					
//				}
				sessions.add(currentSession);
				currentSession = new Session(); 
				
				currentBlock = new Block();
				//System.out.println(getModel().getTime());
				currentBlock.startTime = getModel().getTime();
				trail = 0;
				iteration++;
				getModel().getFatigue().start_new_task();
				addUpdate(0); // between iteration time
			}
		// when the number of iterations exceeds the the runIteration, it's the end of modeling	
		}else{
			fileB1.close();
			fileB2.close();
			fileB3.close();
			fileB4.close();
			fileB5.close();
			fileB6.close();
			fileB7.close();
			utility_utilityThreshold.close();
			
			getModel().stop();
			}
	}

	public void typeKey(char c) {
		
		if (stimulusVisibility == true) {
			response = c + "";
			responseTime = getModel().getTime() - lastTime  ;
			if (response != null) 
			{
				currentSession.responses++;
				currentSession.responseTotalTime += responseTime;
				currentBlock.Bresponces++;
			}

			if (currentSession.blocks.size()==0)
				fileB1.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==1)
				fileB2.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==2)
				fileB3.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==3)
				fileB4.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==4)
				fileB5.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==5)
				fileB6.println((int)(responseTime*1000));
			if (currentSession.blocks.size()==6)
				fileB7.println((int)(responseTime*1000));
			
			if (iteration == 1 
					&& getModel().getProcedural().getFatigueUtility()<4 
					&& getModel().getProcedural().getFatigueUtilityThreshold()<4){
				utility_utilityThreshold.print((int)getModel().getTime()+ "\t");
				utility_utilityThreshold.print((getModel().getProcedural().getFatigueUtility())+ "\t");
				utility_utilityThreshold.print((getModel().getProcedural().getFatigueUtilityThreshold())+ "\n");
				utility_utilityThreshold.flush();
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
			if (currentSession.blocks.size()==0)
				fileB1.println(0);
			if (currentSession.blocks.size()==1)
				fileB2.println(0);
			if (currentSession.blocks.size()==2)
				fileB3.println(0);
			if (currentSession.blocks.size()==3)
				fileB4.println(0);
			if (currentSession.blocks.size()==4)
				fileB5.println(0);
			if (currentSession.blocks.size()==5)
				fileB6.println(0);
			if (currentSession.blocks.size()==6)
				fileB7.println(0);
			currentBlock.FalseAlert++;
			currentSession.responses++;
			getModel().output("False alert happened " 
					+"- Trial: " + iteration+ " Block:"+(currentSession.blocks.size()+1)
					+"   time of block : "+  (getModel().getTime() - currentBlock.startTime));
		}
		
	}

	public Result analyze(Task[] tasks, boolean output) {
		double[] modelTimes = new double[runIterations];
		PVT_35min task = (PVT_35min) tasks[0];

		int allResponces = 0;
		double Responses[] = new double[7];
		double FalseStarts[] = new double [7];
		double AlertResponses[][] = new double[7][35];
		double Lapses [] = new double [7];
		

		for (int i = 0; i < runIterations; i++) {
			for (int j = 0; j < task.sessions.elementAt(i).blocks.size(); j++) {
				
				Responses[j] += task.sessions.elementAt(i).blocks.elementAt(j).Bresponces;
				FalseStarts[j] += task.sessions.elementAt(i).blocks.elementAt(j).FalseAlert;
				for (int k = 0; k < 35; k++)
					AlertResponses[j][k] += task.sessions.elementAt(i).blocks.elementAt(j).alertResponse[k];
				Lapses[j] += task.sessions.elementAt(i).blocks.elementAt(j).lapses;
			}
			allResponces += task.sessions.elementAt(i).responses;
			
		}

		getModel().output("******* Proportion of Responses **********\n");
		getModel()
				.output("    FS  "
						+ " ---------------------------    Alert Responses    --------------------------- "
						+ " Alert Responses "
						+ " ---------------------------    Alert Responses    ---------------------------- "
						+ "L ");
		for (int i = 0; i < 7; i++) {
			double[] AlertResponsesProportion = new double[35];
			for (int j = 0; j < 35; j++)
				AlertResponsesProportion[j] = AlertResponses[i][j] / Responses[i];
			getModel().output("B"+(i+1)+"|"+
					String.format("%.2f", FalseStarts[i] / Responses[i]) + " "
							+ Utilities.toString(AlertResponsesProportion) + " "
							+ String.format("%.2f", Lapses[i] / Responses[i]) );

			getModel().output("\n-------------------------------------------------------\n");
		}
		
		// writing the numbers to file
		try {

			File PVT35min = new File("./PVTmodel/PVT35min.txt");
			if (!PVT35min.exists())
				PVT35min.createNewFile();
			PrintStream PVTfile = new PrintStream(PVT35min);
			
			for (int i = 0; i < 7; i++) {
				PVTfile.print( FalseStarts[i] /  Responses[i]+ "\t" );
				for (int j = 0; j < 35; j++)
					PVTfile.print(AlertResponses[i][j] /  Responses[i]+ "\t" );
				PVTfile.print(Lapses[i] /  Responses[i]+ "\n" );
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
			getModel()
					.output("\n=========              Mean Reaction Times            ===========\n");
		
			for (int i = 0; i < runIterations ; i++) {
				getModel().output("Session " +i + "==>"+ modelTimes[i]);
			}
			
		}

		

		Result result = new Result();
		// result.add ("PVT", modelTimes, humanTimes);
		return result;
	}

}
