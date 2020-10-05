/** 
 *  Disaster ABM in MASON
 *  @author Annetta Burger
 *  Aug2020
 *  
 *  Code base from MASON Gridlock
 */

package disaster;

// Class imports
import java.text.DecimalFormat;
import com.vividsolutions.jts.geom.*;

// Holds methods and parameters for model space and time
public class Spacetime {
	
	public static DecimalFormat df = new DecimalFormat("###.##"); // formating to print statements
	
	
	//======================================
	//
	//     DATE AND TIME
	//
	//======================================
	
	// Step Clock parameters and methods
	
	// Timer variables
	static boolean timeron = false; // tracks whether agent is stopped
	static int stepcount = 0;  // time elapsed in the timer
	
	
	/**
	 * Clock: one step per minute
	 * @param currentStep
	 * @return
	 */
	// return the time of day
	public static long convertToTimeofDay(long currentStep) {
		return currentStep % (24*60);
	}

	
	/**
	 * Returns the time in 2400 clock time
	 * @param steps
	 * @return
	 */
	// 
    public static int time24(long steps)  {   //(e.g. long state.schedule.getSteps())
    		int t = (int) steps;	// get world time
    		int h = (t/60) % 24;				// get hours
    		int m = t % 60;							// get minutes
    		return 100*h + m;						// calc 24 hr clock
    }
	
    
    /**
     * Converts time into minutes
     * @param t
     * @return
     */
    public static int convertTominutes(int t) {  //e.g. double state.schedule.getTime()
    		int m = t % 60;							// get minutes 
    		return m;
    }
    
    
    /**
     * Converts time into hours
     * @param t
     * @return
     */
    public static int convertTohours(int t) {    //e.g. double state.schedule.getTime()
    		int h = (t/60) % 24;				// get hours	
		return h;
    }
    
    
    /**
     * Converts time into date
     * @param t
     * @return
     */
    public static int convertToday(int t) {  //e.g. double state.schedule.getTime()
		int d = t/(24*60);				// get day
		return d;
    }
    
    
    /**
     * Returns a string reporting the simulation day and time
     * @param time
     * @return
     */
   static String reportTime (double time) {  //e.g. double state.schedule.getTime()
    		int t = (int) time;
    		int d = convertToday(t);				// get day
    		int h = convertTohours(t);				// get hours
    		int m = convertTominutes(t);				// get minutes
    		String report = "day: " + d + " time: " + h + m;
    		if ((h < 10) && (m>=10)) report = "day: " + d + " time: 0" + h + m;
    		if ((h >= 10) && (m<10)) report = "day: " + d + " time: " + h + "0" + m;
    		if ((h < 10) && (m<10)) report = "day: " + d + " time: 0" + h + "0" + m;
    		return report;
    }
   
   
   /**
    * Simple step timer for agents
    * @param waitTime
    * @return
    */
   public static boolean steptimer ( int waitsteps ) {
	   System.out.println("Spacetime>steptimer: " + stepcount);
	   // timer updates the timecounter and returns true, unless timecount == waitTime
	   // if timer is set at start, change to stopped
	   if (stepcount == 0) {
		   timeron = true;
		   stepcount += 1;
		   System.out.println("Spacetime>start steptimer: " + stepcount);
	   }
	   else if (stepcount > waitsteps) {
		   		stepcount = 0; // reset the timer
		   		timeron = false;
		   		System.out.println("Spacetime>end steptimer");
	   		}
	   		else {
	   			stepcount += 1;  // simply update the timer count
	   			timeron = true;
	   			System.out.println("Spacetime>continue steptimer count: " + stepcount);
	   }
	   
	   return timeron;   
   }
    
   
	//======================================
	//
	//     SPACE
	//
	//======================================
   
   /**
    * Model coordinates are set in lat/long -- degrees:
    *   Conversions from degrees to meters are required for speed and distance calculations
    *   Length of 1 degree of Longitude = cosine (latitude) * length of degree (miles) at equator 
    *   1 degree = 111 km or 1 degree of longitude = cos(latitude) * 111.321km
    *   length of 1° of latitude = 1° * 69.172 miles = 69.172 miles or 1 degree latitude = 111.321km
    *   Lat Long Coordinates of the Simulation Area
    *   So .001 of a degree is 1.11 meter
    */

    public static double maxLong = 40.9828;
    public static double minLong = 40.5540;
    public static double maxLat = -73.6902;
    public static double minLat = -74.2585;
    
    /**
     * Returns kilometer distance from a lat/long
     * @param lon
     * @param lat
     * @return
     */
    public static double longdegToKilometers(double lon, double lat)  {
    	return Math.abs((lon*(Math.cos(lat) * 111.32)));
    }
    
    
    /**
     * Returns kilometer distance from a lat
     * @param lat
     * @return
     */
    public static double latdegToKilometers(double lat)  {
        return Math.abs((lat * 111.32));
    }
    
    
    /**
     * Returns kilometer distance from a lat/long degree
     * @param deg
     * @return
     */
    public static double degToKilometers(double deg)  {
    	// simple degree transformation based on latitude formula
    	double x = Math.abs( deg * 111.32);
    	return x;
    }
    
    
    /**
     * Returns lat/long distance from kilometers
     * @param km
     * @return
     */
    public static double kilometersToDegrees(double km)  {
    	// simple degree transformation based on latitude formula
    	double x = Math.abs( km / 111.32);
    	return x;
    }
    
    
    /**
     * Returns distance between two coordinates in kilometers
     * @param a
     * @param b
     * @return
     */
    public static double findKiloDist (Coordinate a, Coordinate b) {
    	double DegreeDist = a.distance(b);    	
    	double dist = degToKilometers(DegreeDist);
    	return dist;
    }
    
}
