package actr.model;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeMap;

// Fatigue 

public class Fatigue extends Module {

	private Model model;

	boolean fatigue_enabled = false;
	boolean run_with_microlapses = false;
	double fatigue_stimulus = 1.0;
	double fatigue_fp_dec = 0.01440861; //new model: 0.01440861
	double fatigue_fp_original = 0;
	double fatigue_fp = 1.0; // 1.0
	double fatigue_ut = 0 ;
	double fatigue_dat = 0;

	double fatigue_fd_dec = 0.0;
	double fatigue_fd = 0;

	double fatigue_fpbmc = 0;
	double fatigue_fpmc = 0 ;
	double fatigue_utbmc = 0;
	double fatigue_utmc = 0;
	double fatigue_ut0 = 2.0643395401332;
	double fatigue_fdbmc  = -0.02681;
	double fatigue_fdc = 0.95743;
	double fatigue_hour = 0;
	double fatigue_fp_percent = 1;
	double fatigue_fd_percent = 1;
	
	ArrayList<Double> wake = new ArrayList<Double>();
	ArrayList<Double> asleep = new ArrayList<Double>();
	ArrayList<ArrayList<Double>> values= new ArrayList<ArrayList<Double>>();
	TreeMap<Double, Double> Pvalues = new TreeMap<Double, Double>();

	// fatigue utility hook 
	double fp;
	double ut;
	double minutes_passed;
	
	// minutes that the subject were doing the same task before
	double start_time_sc  = 0;	
	double biomath_prediction;
	
	//parameters regarding time-on-task model
	double PU_TOT = 0;  //production utility time-on-task
	double UT_TOT = 0;  //utility threshold time-on-task 
	
	
	


	Fatigue(Model model) {
		this.model = model;
		//reset_fatigue_module();
		
	}
	
	public void setSched(){
		// values :  p , u , k , times
		values= BioMathModel.sleepsched(wake,asleep,3.841432,38.509212,0.019455);
//		for (int i = 0; i < values.size(); i++) {
//			System.out.print(Utilities.toString(values.get(i)));	
//			System.out.println();
//		}
		
		// given a schedule of sleep/wake hours in the form of '((awake1 sleep1) (awake2 sleep2)....) 
		// returns a hashtable with all performance values where keys are the time 
		for (int i = 0; i < values.size(); i++) 
			Pvalues.put(values.get(i).get(3), values.get(i).get(0));

//		System.out.println(Pvalues.ceilingEntry(70.00));
//		System.out.println(Pvalues);
	}

	// fatigue modulation value at time t 
	double compute_fp(){
		return Math.pow(mp_time(), PU_TOT);
	}

	//fatigue parameter which modulates the utility threshold
	double compute_ft(){
		return Math.pow(mp_time(), UT_TOT);
	}
	
	
	// Anytime there is a microlapse, the fp-percent and fd-percent are decremented
	void decrement_fp_fd(){
		fatigue_fp_percent = Math.max(.000001, fatigue_fp_percent  - fatigue_fp_dec);
		fatigue_fd_percent = Math.max(.000001, fatigue_fd_percent  - fatigue_fd_dec);
	}
	
	// Calculate the number of minutes that have passed
	double mp_time(){
		return ((int)((model.getTime()-start_time_sc)/60));
	}
	
	public void start_new_task(){
		start_time_sc = model.getTime();
	}
	
	
	void update() {
		if (fatigue_enabled) {
			fatigue_fp = fatigue_fp_percent
					* (1-fatigue_fpbmc*biomath_prediction)
					* Math.pow(1+mp_time(), fatigue_fpmc);
			if (model.verboseTrace)
				model.output("fatigue", "fp: " + fatigue_fp);
			fatigue_ut = fatigue_ut0 * (1-fatigue_utbmc*biomath_prediction)
					* Math.pow(1+mp_time(), fatigue_utmc);
			if (model.verboseTrace)
				model.output("fatigue", "ut: " + fatigue_ut);
		}
	}

	public void setFatigue_fp(double fp) {
		fatigue_fp = fp;
	}

	public double getFatigue_fp_original() {
		return fatigue_fp_original;
	}

	public void reset_fatigue_module() {

		fatigue_fp = fatigue_stimulus * fatigue_fp_original;
		fatigue_ut = model.getProcedural().utilityThreshold;

		
//		fatigue_pending = nil;
//		fatigue_last_one_empty = nil;
//
//		fatigue_fp_percent = 1;
//		fatigue_fp = 1;
//		fatigue_fp_dec = 0.01440861;
//		fatigue_fd_percent = 1;
//		fatigue_fd = 0; 
//		fatigue_stimulus =1;
//		fatigue_fpbmc = 0;
//		fatigue_fpmc = 0;
//		fatigue_utbmc = 0;
//		fatigue_utmc = 0;
//		fatigue_ut0 = 2.0643395401332;
//		fatigue_fdbmc =-0.02681;
//		fatigue_fdc =0.95743;
//		fatigue_hour =0;
//		fatigue_start_time = 0;
//		fatigue_ut= (car (no-output (sgp :ut))))
//		fatigue_dat =(car (no-output (sgp :dat))));

	}
}
