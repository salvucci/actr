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
	private boolean runWithUtilityDecrement = true;
	
	private boolean fatiguePartialMatching = false;
	
	private double fatigueFPDec = 1; 	// Decrease in fp after each microlapse (NEW VALUE FOR THE MULTIPICATION)
	private double fatigueFPDecSleep1 = 0; 	// FPDec constant for sleep
	private double fatigueFPDecSleep2 = 0; 	// FPDec constant for sleep

	private double fatigueFPOriginal = 0;
	private double fatigueFP = 1.0;  			// "fatigue parameter" value scales utility calculations
	private double fatigueUT = 0;  				//Utility threshold (from utilty module)
	private double fatigueDAT = 0;

	private double fatigueFDDec = 0.0;
	private double fatigueFD = 0;				// ??

	private double fatigueFPBMC = 0; 			//Coefficient relating biomath value to fp
	private double fatigueFPMC = 0; 			// Coefficient relating minute within session to fp
	private double fatigueFPMC0 = 0;
	private double fatigueUTBMC = 0; 			//Coefficient relating biomath value to ut
	private double fatigueUTMC = 0;  			//oefficient relating minute within session to ut
	private double fatigueUTMC0 = 0;  			
	private double fatigueUT0 = 0;				//Constant ut offset
	private double fatigueUtility0 = 0;         //Constant utility offset
	private double fatigueFDBMC = -0.02681;  	//Coefficient relating biomath value to fd
	private double fatigueFDC = 0.95743;  		// Constant fd offset
	private double fatigueStartTime = 0;   			/* Initiates a new session by providing the number of hours 
													since the beginning of the sleep schedule.
													The format is : hh + mm/60
													e.g. 5:30 = 5.5  */
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
			// given a schedule of sleep/wake hours in the form of '((awake1 sleep1)
			// (awake2 sleep2)....)
			// returns a hashtable with all performance values where keys are the
			// time
			for (int i = 0; i < values.size(); i++)
				pvalues.put(values.get(i).get(3), values.get(i).get(0));
		}
	}

	// Biomathematical for the start time of the session in the form of hh + (mm/60)
	public double computeBioMathValueForHour(){
		if (pvalues.isEmpty())
			return 0;
		else
			return pvalues.ceilingEntry(fatigueStartTime).getValue();
	}
	
	public double computeBioMathValue(){
		if (pvalues.isEmpty())
			return 0;
		else
			return pvalues.ceilingEntry(fatigueStartTime + mpTime()/60).getValue();
	}

	/**
	 *  Anytime there is a microlapse, the fp-percent and fd-percent are decremented
	 */
	void decrementFPFD() {
//      setFatigueFPPercent(Math.max(.000001, getFatigueFPPercent() * getFatigueFPDec())); 
//		setFatigueFDPercent(Math.max(.000001, getFatigueFDPercent() * getFatigueFDDec()));
	
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
//			fatigueFP = getFatigueFPPercent() * (1 - getFatigueFPBMC() * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), getFatigueFPMC());
//			fatigueUT = getFatigueUT0() * (1 - getFatigueUTBMC() * computeBioMathValueForHour()) * Math.pow(1 + mpTime(), getFatigueUTMC());

			// NEW MODEL addition factor. *** THE UTILITY IS BEING CHANGED IN THE PRODUCTION CLASS *** 
			double BioMath = computeBioMathValue();
			double UTMC0 = getFatigueUTMC0();
			double UTMC = getFatigueUTMC();
			double UTBMC = getFatigueUTBMC();
			fatigueUT =  Math.pow(1 + mpTime(), -(UTMC + UTMC0 * BioMath)) + getFatigueUT0() - (UTBMC * BioMath) ;
		}
	}
	
	
	public double getFatigueFPPercent() {
//		return Math.max(.000001, Math.pow(getFatigueFPDec() , numberOfConsecutiveMicroLapses) ); // model 1 Original
		return Math.max(.000001, - Math.pow(Math.E , ((fatigueFPDec)) * (numberOfConsecutiveMicroLapses)) + 2  ); //NEW MODEL
		
//		return Math.min(
//				Math.max(.000001, Math.pow(getFatigueFPDec() , numberOfConsecutiveMicroLapses) ) , 
//				Math.max(.000001, - Math.pow(Math.E , ((fatigueFPDecSleep1)) * (numberOfConsecutiveMicroLapses-fatigueFPDecSleep2)) + 2  )
//				); // model 3
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
	public void setFatigueStartTime(double hour) {
		fatigueStartTime = hour;
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
		numberOfConsecutiveMicroLapses = 1 ; // NEW MODEL reseting the number of consecutive microlapses back to 1
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
	}
	
	public boolean isFatigueEnabled() {
		return fatigueEnabled;
	}

	public void setFatigueEnabled(boolean fatigueEnabled) {
		this.fatigueEnabled = fatigueEnabled;
	}

	public boolean isFatiguePartialMatching() {
		return fatiguePartialMatching;
	}

	public void setFatiguePartialMatching(boolean b) {
		this.fatiguePartialMatching = b;
	}
	
	public boolean isRunWithUtilityDecrement() {
		return runWithUtilityDecrement;
	}

	public void setRunWithUtilityDecrement(boolean runWithUtilityDecrement) {
		this.runWithUtilityDecrement = runWithUtilityDecrement;
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
	
	public void setFatigueFPDecSleep1(double fatigueFPDecSleep1) {
		this.fatigueFPDecSleep1 = fatigueFPDecSleep1;
	}
	public void setFatigueFPDecSleep2(double fatigueFPDecSleep2) {
		this.fatigueFPDecSleep2 = fatigueFPDecSleep2;
	}

	public double getFatigueFPMC0() {
		return fatigueFPMC0;
	}

	public void setFatigueFPMC0(double fatigueFPMC0) {
		this.fatigueFPMC0 = fatigueFPMC0;
	}

	public double getFatigueUTMC0() {
		return fatigueUTMC0;
	}

	public void setFatigueUTMC0(double fatigueUTMC0) {
		this.fatigueUTMC0 = fatigueUTMC0;
	}
	
}
