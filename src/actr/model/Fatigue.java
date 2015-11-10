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

	boolean fatigueEnabled = false;
	boolean runWithMicrolapses = false;
	double fatigueStimulus = 1.0;
	double fatigueFPDec = 0.01440861; // new model: 0.01440861
	double fatigueFPOriginal = 0;
	double fatigueFP = 1.0; // 1.0
	double fatigueUT = 0;
	double fatigueDAT = 0;

	double fatigueFDDec = 0.0;
	double fatigueFD = 0;

	double fatigueFPBMC = 0;
	double fatigueFPMC = 0;
	double fatigueUTBMC = 0;
	double fatigueUTMC = 0;
	double fatigueUT0 = 2.0643395401332;
	double fatigueFDBMC = -0.02681;
	double fatigueFDC = 0.95743;
	double fatigueHour = 0;
	double fatigueFPPercent = 1;
	double fatigueFDPercent = 1;

	private ArrayList<Double> wake = new ArrayList<Double>();
	private ArrayList<Double> asleep = new ArrayList<Double>();
	private ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
	private TreeMap<Double, Double> pvalues = new TreeMap<Double, Double>();

	// fatigue utility hook
	double fp;
	double ut;
	double minutesPassed;

	// minutes that the subject were doing the same task before
	double startTimeSC = 0;
	double biomathPrediction;

	// parameters regarding time-on-task model
	double puTOT = 0; // production utility time-on-task
	double utTOT = 0; // utility threshold time-on-task

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
		// values : p , u , k , times
		values = BioMathModel.sleepsched(wake, asleep, 3.841432, 38.509212, 0.019455);
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

		// System.out.println(Pvalues.ceilingEntry(70.00));
		// System.out.println(Pvalues);
	}

	// fatigue modulation value at time t
	double computeFP() {
		return Math.pow(mpTime(), puTOT);
	}

	// fatigue parameter which modulates the utility threshold
	double computeFT() {
		return Math.pow(mpTime(), utTOT);
	}

	// Anytime there is a microlapse, the fp-percent and fd-percent are
	// decremented
	void decrementFPFD() {
		fatigueFPPercent = Math.max(.000001, fatigueFPPercent - fatigueFPDec);
		fatigueFDPercent = Math.max(.000001, fatigueFDPercent - fatigueFDDec);
	}

	// Calculate the number of minutes that have passed
	double mpTime() {
		return ((int) ((model.getTime() - startTimeSC) / 60));
	}

	public void startNewTask() {
		startTimeSC = model.getTime();
	}

	@Override
	void update() {
		if (fatigueEnabled) {
			fatigueFP = fatigueFPPercent * (1 - fatigueFPBMC * biomathPrediction) * Math.pow(1 + mpTime(), fatigueFPMC);
			if (model.verboseTrace)
				model.output("fatigue", "fp: " + fatigueFP);
			fatigueUT = fatigueUT0 * (1 - fatigueUTBMC * biomathPrediction) * Math.pow(1 + mpTime(), fatigueUTMC);
			if (model.verboseTrace)
				model.output("fatigue", "ut: " + fatigueUT);
		}
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
