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

	private boolean fatigueEnabled = false; 	// Turns fatigue module off / on
	private boolean runWithMicrolapses = true; 
	
	private double fatigueFPDec = 1; 	// Decrease in fp after each microlapse (NEW VALUE FOR THE MULTIPICATION)
	private double fatigueFPDecC = 1; 	// FPDec constant

	private double fatigueFPOriginal = 0;
	private double fatigueFP = 1.0;  			// "fatigue parameter" value scales utility calculations
	private double fatigueUT = 0;  				//Utility threshold (from utilty module)
	private double fatigueDAT = 0;

	private double fatigueFDDec = 0.0;
	private double fatigueFD = 0;				// ??

	private double fatigueFPBMC = 0; 			//Coefficient relating biomath value to fp
	private double fatigueFPMC = 0; 			// Coefficient relating minute within session to fp
	private double fatigueUTBMC = 0; 			//Coefficient relating biomath value to ut
	private double fatigueUTMC = 0;  			//oefficient relating minute within session to ut
	private double fatigueUT0 = 2.0643395401332;//Constant ut offset
	private double fatigueFDBMC = -0.02681;  	//Coefficient relating biomath value to fd
	private double fatigueFDC = 0.95743;  		// Constant fd offset
	private double fatigueHour = 0;   			/* Initiates a new session by providing the number of hours 
													since the beginning of the sleep schedule */
	private int numberOfConsecutiveMicroLapses = 1;
	double startTimeSC = 0;  					/* Records the time in second at the start of a session, 
									    			so proper within-session offsets can be calculated */

	double fatigueP0 = 3.841432;  				// the initial values of bioMath model
	double fatigueU0 = 38.509212; 				// the initial values of bioMath model
	double fatigueK0 = 0.019455;  				// the initial values of bioMath model

	private ArrayList<Double> wake = new ArrayList<Double>();
	private ArrayList<Double> asleep = new ArrayList<Double>();
	private ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
	private TreeMap<Double, Double> pvalues = new TreeMap<Double, Double>();


	private ArrayList<Double> TaskSchedule = new ArrayList<Double>();
	private double TaskDuration = 0 ;
	private String outputDIR = null ;

	double cumulativeParameter = 0;

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
	
	void addTaskSchedule(double time) {
		TaskSchedule.add(time);
	}
	
	public ArrayList<Double> getTaskSchdule(){
		return TaskSchedule;
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

	/**
	 *  Anytime there is a microlapse, the fp-percent and fd-percent are decremented
	 */
	void decrementFPFD() {
//      setFatigueFPPercent(Math.max(.000001, getFatigueFPPercent() * getFatigueFPDec())); 
//		setFatigueFDPercent(Math.max(.000001, getFatigueFDPercent() * getFatigueFDDec()));
	
//		setFatigueFPPercent(Math.max(.000001, getFatigueFPPercent() * Math.pow(getFatigueFPDec() , numberOfConsecutiveMicroLapses)) ); 
//		setFatigueFDPercent(Math.max(.000001, getFatigueFDPercent() * Math.pow(getFatigueFDDec() , numberOfConsecutiveMicroLapses)) );
	
		numberOfConsecutiveMicroLapses++;  // adding one to the number of consecutive Microlapses
	}
	
	/**
	 * @return the number of minutes that have passed from the beginning of the task
	 */
	double mpTime() {
		return ((int) ((model.getTime() - startTimeSC) / 60)) + cumulativeParameter;
	}

	@Override
	void update() {
		if (isFatigueEnabled()) {
			fatigueFP = getFatigueFPPercent() * (1 - getFatigueFPBMC() * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), getFatigueFPMC());
			fatigueUT = getFatigueUT0() * (1 - getFatigueUTBMC() * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), getFatigueUTMC());
		}
	}
	
	
	public double getFatigueFPPercent() {
		//return Math.max(.000001, Math.pow(getFatigueFPDec() , numberOfConsecutiveMicroLapses) ); // model 1 and 2
		//System.out.println(numberOfConsecutiveMicroLapses + " " +Math.max(.000001, (-2 / (1+Math.pow(Math.E,-fatigueFPDec *numberOfConsecutiveMicroLapses))) + 2 ));
		//return Math.max(.000001,  (-1 / ( 1 + Math.pow(Math.E,-fatigueFPDec * numberOfConsecutiveMicroLapses + fatigueFPDecC) ) ) + 1 ) ;  // model 3 Sigmoid Function
		return Math.max(.000001,  Math.pow (1 + numberOfConsecutiveMicroLapses , fatigueFPDec ) ) ;  // model 4 :reverse exponential function
	}

	public double getBioMathModelValueforHour(double hour){
		if (pvalues.isEmpty())
			return 0;
		else
			return pvalues.ceilingEntry(hour).getValue();
	}

	/**
	 * @param hour
	 * Initiates a new session by providing the number of hours since the beginning of the sleep schedule 
	 */
	public void setFatigueHour(double hour) {
		fatigueHour = hour;
	}

	/**
	 * When ever there is a new session this function should be called so the startTimeSc is set
	 */
	public void startFatigueSession() {  
		startTimeSC = model.getTime();
	}
	
	public void setCumulativeParameter(double AP){
		cumulativeParameter = AP;
	}
	
	
//	/**
//	 * for when you have an cumulative affect on the task that are being consecutively happening.
//	 */
//	public void startCumulativeFatigueSession(){
//		startTimeSC = Math.min(startTimeSC + cumulativeParameter , model.getTime());
//	}

	/**
	 *  OLD: This method is called just after any new task presentation (audio or visual)
	 *  NEW: This method is called at every production except wait. 
	 */
	public void fatigueResetPercentages(){
		numberOfConsecutiveMicroLapses = 1 ; // reseting the number of consecutive Microlapses back to 1
//		setFatigueFPPercent(1.0);
//		setFatigueFDPercent(1.0);
	}

	public void setFatigueFP(double fp) {
		fatigueFP = fp;
	}
	
	public double getFatigueFP(){
		return fatigueFP;
	}
	
	public double getFatigueUT(){
		return fatigueUT;
	}

	public double getFatigueFPOriginal() {
		return fatigueFPOriginal;
	}

	
	public boolean isSleep(double hour){
		for (int i = 0; i < wake.size(); i++) {
			if (hour>=wake.get(i) && hour < asleep.get(i))
				return false;
		}
		return true;
	}
	
	public double getTimeAwake(double hour){
		double timeAwake=0;
		for (int i = 0; i < wake.size(); i++) {
			if (hour>=wake.get(i) && hour < asleep.get(i))
				timeAwake =  hour - wake.get(i);
		}
		return timeAwake;
	}

	public double getFatigueFDPercent() {
		return Math.max(.000001, Math.pow(getFatigueFDDec() , numberOfConsecutiveMicroLapses) );  
	}

	public void resetFatigueModule() {

		fatigueFP =  fatigueFPOriginal;
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
	
	public boolean isFatigueEnabled() {
		return fatigueEnabled;
	}

	public void setFatigueEnabled(boolean fatigueEnabled) {
		this.fatigueEnabled = fatigueEnabled;
	}

	public boolean isRunWithMicrolapses() {
		return runWithMicrolapses;
	}

	public void setRunWithMicrolapses(boolean runWithMicrolapses) {
		this.runWithMicrolapses = runWithMicrolapses;
	}

	public double getFatigueDAT() {
		return fatigueDAT;
	}

	public void setFatigueDAT(double fatigueDAT) {
		this.fatigueDAT = fatigueDAT;
	}

	public double getFatigueUT0() {
		return fatigueUT0;
	}

	public void setFatigueUT0(double fatigueUT0) {
		this.fatigueUT0 = fatigueUT0;
	}

	
	public double getFatigueFPDec() {
		//return - Math.pow(fatigueFPDec , computeBioMathValueForHour()) + 2;
		return fatigueFPDec;
	}

	public void setFatigueFPDec(double fatigueFPDec) {
		this.fatigueFPDec = fatigueFPDec;
	}

	public double getFatigueFDDec() {
		return fatigueFDDec;
	}

	public void setFatigueFDDec(double fatigueFDDec) {
		this.fatigueFDDec = fatigueFDDec;
	}

	public double getFatigueFD() {
		return fatigueFD;
	}

	public void setFatigueFD(double fatigueFD) {
		this.fatigueFD = fatigueFD;
	}

	public double getFatigueFPBMC() {
		return fatigueFPBMC;
	}

	public void setFatigueFPBMC(double fatigueFPBMC) {
		this.fatigueFPBMC = fatigueFPBMC;
	}

	public double getFatigueFPMC() {
		return fatigueFPMC;
	}

	public void setFatigueFPMC(double fatigueFPMC) {
		this.fatigueFPMC = fatigueFPMC;
	}

	public double getFatigueUTBMC() {
		return fatigueUTBMC;
	}

	public void setFatigueUTBMC(double fatigueUTBMC) {
		this.fatigueUTBMC = fatigueUTBMC;
	}

	public double getFatigueUTMC() {
		return fatigueUTMC;
	}

	public void setFatigueUTMC(double fatigueUTMC) {
		this.fatigueUTMC = fatigueUTMC;
	}

	public double getFatigueFDBMC() {
		return fatigueFDBMC;
	}

	public void setFatigueFDBMC(double fatigueFDBMC) {
		this.fatigueFDBMC = fatigueFDBMC;
	}

	public double getFatigueFDC() {
		return fatigueFDC;
	}

	public void setFatigueFDC(double fatigueFDC) {
		this.fatigueFDC = fatigueFDC;
	}

	public double getTaskDuration() {
		return TaskDuration;
	}

	public void setTaskDuration(double task_Duration) {
		TaskDuration = task_Duration;
	}
	public String getOutputDIR() {
		return outputDIR;
	}

	public void setOutputDIR(String outputDIR) {
		this.outputDIR = outputDIR;
	}
	
	public void setFatigueFPDecC(double fatigueFPDecC) {
		this.fatigueFPDecC = fatigueFPDecC;
	}
	
}
