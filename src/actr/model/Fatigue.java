package actr.model;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Implementation of the ACT-R fatigue model (Gunzelmann et al.)
 * 
 * @author ehsanebk
 * @author salvucci
 */
public class Fatigue extends Module {
	private Model model;

	boolean fatigueEnabled = false; 	// Turns fatigue module off / on
	boolean runWithMicrolapses = true; 
	double fatigueStimulus = 1.0;  		/* Signals a reset to fp based on specified scalar.  
										fp <- stimulus.  Resets decrements */
	double fatigueFPDec = 0.01440861; 	// Decrease in fp after each microlapse
	double fatigueFPOriginal = 0;
	double fatigueFP = 1.0;  			// "fatigue parameter" value scales utility calculations
	double fatigueUT = 0;  				//Utility threshold (from utilty module)
	double fatigueDAT = 0;

	double fatigueFDDec = 0.0;
	double fatigueFD = 0;

	double fatigueFPBMC = 0; 			//Coefficient relating biomath value to fp
	double fatigueFPMC = 0; 			// Coefficient relating minute within session to fp
	double fatigueUTBMC = 0; 			//Coefficient relating biomath value to ut
	double fatigueUTMC = 0;  			//oefficient relating minute within session to ut
	double fatigueUT0 = 2.0643395401332;//Constant ut offset
	double fatigueFDBMC = -0.02681;  	//Coefficient relating biomath value to fd
	double fatigueFDC = 0.95743;  		// Constant fd offset
	double fatigueFPPercent = 1; 		// The current percentage of fp after microlapses
	double fatigueFDPercent = 1;
	double fatigueHour = 0;   			/* Initiates a new session by providing the number of hours 
										since the beginning of the sleep schedule */
	double startTimeSC = 0;  			/* Records the time in second at the start of a session, 
									    so proper within-session offsets can be calculated */

	double fatigueP0 = 3.841432;  // the initial values of bioMath model
	double fatigueU0 = 38.509212; // the initial values of bioMath model
	double fatigueK0 = 0.019455;  // the initial values of bioMath model

	private ArrayList<Double> wake = new ArrayList<Double>();
	private ArrayList<Double> asleep = new ArrayList<Double>();
	private ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
	private TreeMap<Double, Double> pvalues = new TreeMap<Double, Double>();



	Fatigue(Model model) {
		this.model = model;
		// reset_fatigue_module();
	}

	void addWake(double time) {
		wake.add(time);
	}

	void addSleep(double time) {
		asleep.add(time);
	}

	void setSleepSchedule() {
		if (!wake.isEmpty() && !asleep.isEmpty())  // check if the the model has setup the set-schedule function
		{
			// values : p , u , k , times
			values = BioMathModel.sleepsched(wake, asleep, fatigueP0 , fatigueU0, fatigueK0);
			// for (int i = 0; i < values.size(); i++) {
			// System.out.print(Utilities.toString(values.get(i)));
			// System.out.println();
			// }

			// given a schedule of sleep/wake hours in the form of '((awake1 sleep1)
			// (awake2 sleep2)....)
			// returns a hashtable with all performance values where keys are the
			// time
			for (int i = 0; i < values.size(); i++)
				pvalues.put(values.get(i).get(3), values.get(i).get(0));

			//System.out.println(pvalues.ceilingEntry(70.00).getValue());
			//System.out.println(Pvalues);
		}
	}

	public double computeBioMathValueForHour(){
		if (pvalues.isEmpty())
			return 0;
		else
			return pvalues.ceilingEntry(fatigueHour).getValue();
	}

	// Anytime there is a microlapse, the fp-percent and fd-percent are decremented
	void decrementFPFD() {
		fatigueFPPercent = Math.max(.000001, fatigueFPPercent - fatigueFPDec);
		fatigueFDPercent = Math.max(.000001, fatigueFDPercent - fatigueFDDec);
	}

	// Calculate the number of minutes that have passed
	double mpTime() {
		return ((int) ((model.getTime() - startTimeSC) / 60));
	}

	@Override
	void update() {
		if (fatigueEnabled) {
			fatigueFP = fatigueFPPercent * (1 - fatigueFPBMC * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), fatigueFPMC);
			fatigueUT = fatigueUT0 * (1 - fatigueUTBMC * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), fatigueUTMC);
		}
	}

	// Initiates a new session by providing the number of hours since the beginning of the sleep schedule 
	public void setFatigueHour(double hour) {
		fatigueHour = hour;
	}

	// When ever there is a new session this function should be called so the startTimeSc is set
	public void startFatigueSession() {  
		startTimeSC = model.getTime();
	}

	// This method is called just after any new task presentation (audio or visual) 
	public void fatigueResetPercentages(){
		fatigueFPPercent = fatigueStimulus;
		fatigueFDPercent = fatigueStimulus;
	}


	public void setFatigueFP(double fp) {
		fatigueFP = fp;
	}

	public double getFatigueFPOriginal() {
		return fatigueFPOriginal;
	}

	public void resetFatigueModule() {

		fatigueFP = fatigueStimulus * fatigueFPOriginal;
		fatigueUT = model.getProcedural().utilityThreshold;

		// fatigue_pending = nil;
		// fatigue_last_one_empty = nil;
		//
		// fatigue_fp_percent = 1;
		// fatigue_fp = 1;
		// fatigue_fp_dec = 0.01440861;
		// fatigue_fd_percent = 1;
		// fatigue_fd = 0;
		// fatigue_stimulus =1;
		// fatigue_fpbmc = 0;
		// fatigue_fpmc = 0;
		// fatigue_utbmc = 0;
		// fatigue_utmc = 0;
		// fatigue_ut0 = 2.0643395401332;
		// fatigue_fdbmc =-0.02681;
		// fatigue_fdc =0.95743;
		// fatigue_hour =0;
		// fatigue_start_time = 0;
		// fatigue_ut= (car (no-output (sgp :ut))))
		// fatigue_dat =(car (no-output (sgp :dat))));
	}
}
