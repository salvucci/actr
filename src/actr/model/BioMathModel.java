package actr.model;
import java.lang.Math;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * McCauley 2013 Sleep Model implementation - sleephistory 
 * function provides the final alertness value as per the sleep schedule and hour
 * 
 * @author Ehsan 
 */
public class BioMathModel {

	static double precision= .05/3;

	static double alphaw= -0.028;
	static double alphas= -0.26;
	static double betaw= -0.26;
	static double betas= betaw;
	static double muw= 0.33;
	static double mus= -1.5;
	static double etaw= 0.0074;
	static double tau= 24;
	static double fi1= 21.2;
	static double Wc= 20.2;
	static double Ac= Wc/tau;
	static double etas= (Ac/(Ac-1))*etaw;		// equation 4

	static double lambdas=-0.49;				
	static double lambdaw=0.49;
	static double ksi=1.09;
	static double W=16;
	static double kappat0=(((Math.exp(lambdaw*W)*Math.exp(lambdas*(tau-W)))-1)/(Math.exp(lambdaw*W)-1));


	// Method for rounding a double by the precision 
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	// Method for concatenating two arrays
	public static double[] combine(double[] a, double[] b){
        int length = a.length + b.length;
        double[] result = new double[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
	public static double[] combine(double[] a, double b){
        int length = a.length +1;
        double[] result = new double[length];
        System.arraycopy(a, 0, result, 0, a.length);
        result[length-1]= b;
        return result;
	}

	public static ArrayList<ArrayList<Double>> rBind(ArrayList<ArrayList<Double>> top
			,ArrayList<ArrayList<Double>> bottom) {
		ArrayList<ArrayList<Double>> output = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < top.size(); i++) 
			output.add(top.get(i));
		for (int i = 0; i < bottom.size(); i++) 
			output.add(bottom.get(i));
		return output;
	}

	
	
	// equation 2
	static double[] dpw(double t,double t0,double p0,double u0,double k0){
		if (round(t,2)==round(t0,2)){
			double[] output= {p0};
			return output;
			}
		double[] sofarp=dpw(t-precision,t0,p0,u0,k0);
		double[] sofaru=duw(t-precision,t0,u0);
		double prevp=sofarp[sofarp.length-1];
		double prevu=sofaru[sofaru.length-1];
		return(combine(sofarp,prevp+precision*(alphaw*(prevp+betaw*prevu)+gw(t,t0,k0))));
	}
	
	static double[] dps(double t,double t0,double p0,double u0,double k0){
		if (round(t,2)==round(t0,2)){
			double[] output= {p0};
			return output;}
		double[] sofarp=dps(t-precision,t0,p0,u0,k0);
		double[] sofaru=dus(t-precision,t0,u0);
		double prevp=sofarp[sofarp.length-1];
		double prevu=sofaru[sofaru.length-1];
		return(combine(sofarp,prevp+precision*(alphas*(prevp+betas*prevu)+gs(t,t0,k0))));
	}

	// Equation 3
	static double[] duw(double t,double t0,double u0){	
		if (round(t,2)==round(t0,2)){
			double[] output= {u0};
			return output;
		}
		double[] sofar=duw(t-precision,t0,u0);
		double prev=sofar[sofar.length-1];
		return(combine(sofar,prev+etaw*prev*precision));
	}

	static double[] dus(double t,double t0,double u0){
		if (round(t,2)==round(t0,2)){
			double[] output= {u0};
			return output;
		}
		double[] sofar=dus(t-precision,t0,u0);
		double prev=sofar[sofar.length-1];
		return(combine(sofar,prev+(etas*prev+1)*precision));
	}

	// Equation 5
	static double gw(double t,double t0,double k0){
		double[] d=dkappaw(t,t0,k0);
		double dk=d[d.length-1];
		return(dk*(ct(t)+muw));
	}
	static double gs(double t,double t0,double k0){
		double[] d=dkappas(t,t0,k0);
		double dk=d[d.length-1];
		return(dk*(ct(t)+mus)+(alphas*betas)/etas);
	}

	// Equation 6
	static double ct(double t){
		return(Math.sin((2*Math.PI)*((t-fi1)/tau)));
	}

	// Equation 7
	static double[] dkappaw(double t,double t0,double k0){
		if (round(t,2)==round(t0,2)){
			double[] output= {k0};
			return output;
		}
		double[] sofar=dkappaw(t-precision,t0,k0);
		double prev=sofar[sofar.length-1];
		return(combine(sofar,prev+lambdaw*prev*(1-prev/ksi)*precision));
	}

	static double[] dkappas(double t,double t0,double k0){
		if (round(t,2)==round(t0,2)){
			double[] output= {k0};
			return output;
		}
		double[] sofar=dkappas(t-precision,t0,k0);
		double prev=sofar[sofar.length-1];
		return(combine(sofar,(prev+lambdas*prev*precision)));
	}	
	
	/*
	 * returns the list of p, u  and k values during time awake between hours t0 and t0+t,
	 * where t is the total time awake, and all other values are initial
	*/	
	static ArrayList<ArrayList<Double>> awakevalues(double t,double t0,double p0,double u0,double k0){
		//double[][] values={{0},{0},{0},{0}};
		ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
		double curt=t0;
		for (int i=1; i<=t+1;i++){
			double[] processp=dpw(i+t0,curt,p0,u0,k0);
			double[] processu=duw(i+t0,curt,u0);
			double[] processk=dkappaw(i+t0,curt,k0);
			ArrayList<Double> times = new ArrayList<Double>();
			for(double time=curt; time < i+t0; time+=precision)
				times.add(time);
			//System.out.println(times.size());
			//double[] times=seq(curt,i+t0,by=precision)
			for (int j = 1; j < times.size(); j++) {
				values.add(
						new ArrayList<Double>(Arrays.asList(processp[j],processu[j],processk[j],times.get(j)))
						);

			}
			//curvalues=data.frame(processp, processu, processk, times)
			//curvalues=curvalues[2:(dim(curvalues)[1]),]
			//values=rbind(values,curvalues)
			curt=curt+1;
			p0= processp[processp.length-1];
			u0= processu[processu.length-1];
			k0= processk[processk.length-1];
		}
		//System.out.println(curt);
		values.remove(0);
		//values=values[2:dim(values)[1],]
		return values;
	}

	/*
	 * returns the list of p, u  and k values during time asleep between hours t0 and t0+t,
	 * where t is the total time awake, and all other values are initial
	*/
	static ArrayList<ArrayList<Double>> asleepvalues(double t,double t0,double p0,double u0,double k0){
		ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
		double curt=t0;
		for (int i=1; i<=t+1;i++){
			double[] processp=dps(i+t0,curt,p0,u0,k0);
			double[] processu=dus(i+t0,curt,u0);
			double[] processk=dkappas(i+t0,curt,k0);
			ArrayList<Double> times = new ArrayList<Double>();
			for(double time=curt; time < i+t0; time+=precision)
				times.add(time);
			for (int j = 1; j < times.size(); j++) {
				values.add(
						new ArrayList<Double>(Arrays.asList(processp[j],processu[j],processk[j],times.get(j)))
						);

			}
			curt=curt+1;
			p0= processp[processp.length-1];
			u0= processu[processu.length-1];
			k0= processk[processk.length-1];
		}
		//System.out.println(curt);
		//values=values[2:dim(values)[1],]
		values.remove(0);
		return values;
	}

	/*
	 * takes in a list of times awake and times asleep and 
	 * returns the list of values for p, u, and k
	*/
	static ArrayList<ArrayList<Double>> sleepsched(ArrayList<Double> wake,ArrayList<Double> asleep,double p0,double u0,double k0){
		//values=data.frame(p0,u0,k0,wake[1])
		
		// values : "processp","processu","processk","times"
		ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
		values.add(
				new ArrayList<Double>(Arrays.asList(p0,u0,k0,wake.get(0)))
				);

		int i=0;
		while(i<(wake.size()-1)) {
			values=rBind(values,awakevalues(asleep.get(i)-wake.get(i)-precision,wake.get(i),
					values.get(values.size()-1).get(0),  // getting the p for the last row of values
					values.get(values.size()-1).get(1),  // getting the u for the last row of values
					values.get(values.size()-1).get(2)));  // getting the k for the last row of values
			values=rBind(values,asleepvalues(wake.get(i+1)-asleep.get(i)-precision,asleep.get(i),
					values.get(values.size()-1).get(0),  // getting the p for the last row of values
					values.get(values.size()-1).get(1),  // getting the u for the last row of values
					values.get(values.size()-1).get(2)));	// getting the k for the last row of values
			i++;
		}
		values=rBind(values,awakevalues(asleep.get(i)-wake.get(i),wake.get(i),
				values.get(values.size()-1).get(0),  // getting the p for the last row of values
				values.get(values.size()-1).get(1),  // getting the u for the last row of values
				values.get(values.size()-1).get(2)));  // getting the k for the last row of values
		return values;
	}
	
	
//	public static void main(String[] args) {
//		ArrayList<Double> wake= new ArrayList<Double>();
//		wake.add(7.5);
//		wake.add(31.5);
//		wake.add(55.5);
//		wake.add(79.5);
//		ArrayList<Double> sleep=new ArrayList<Double>();
//		sleep.add(23.5);
//		sleep.add(47.5);
//		sleep.add(71.5);
//		sleep.add(94.5);
//		
//		ArrayList<ArrayList<Double>> test= new ArrayList<ArrayList<Double>>();
//		test= sleepsched(wake,sleep,3.841432,38.509212,0.019455);
//				
//		for (int i = 0; i < test.size(); i++) {
//			System.out.print(Utilities.toString(test.get(i)));	
//			System.out.println();
//		}
//		//print(test)
//	}
//	
}
