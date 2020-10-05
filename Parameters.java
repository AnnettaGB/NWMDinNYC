/** 
 *  Disaster ABM in MASON
 *  @author Annetta Burger and Bill Kennedy
 *  2018-19
 *  
 */

package disaster;

// Class imports
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


//=========================
/** 
 * Parameters Class to manage model inputs and testing parameters
 */
//=========================

// Holds model simulation parameters
public class Parameters 
{	
	
	private static final Boolean True = null;
	//================================
	/**
	 * Simulation Testing Parameters
	 */
	//================================
	
	//Comment in/out test parameters
	//Sample Population Size:230-23000 in Commute region Richard 2020 Data 
	//public static String popfile = "sample230.csv";
	//public static String popfile = "sample2300.csv";
	public static String popfile = "sample23k.csv";
	//public static String popfile = "sample230k.csv";
	
	// Social Behavior Parameters from Agent Group Parameters
	// indicates whether agents form groups -- either through the carPool method or individual nonroutine
	public static Boolean Carpool = true; // turn on carpool grouping behavior
	public static Boolean Emergent = true; // turn on emergent group behavior postimpact
	public static int maxGrpSize = 100;  // group size limits
	  // Note: this includes group commuting
	
	// Time of disaster
	// NWMD Parameters from Effects Parameters
	public static int tDetonation = 600; // time NWMD detonation in steps	// 600=10am day 1;  //2040=10am day 3	// time NWMD detonation in minutes (10am day 1)

	// Export parameters
	public static int tExportNetworks = 615; // time step to export social networks in Log class
	
	//===================================
	/**
	 * Simulation Input Data pulled into WorldBuilder Class
	 */
	//===================================
	
	//1 Commute Road Network
	public static String roadsShape = "Clean_Com_Road_NW_Richard.shp";
	//2 Commute Census Tracts
	public static String censusShape = "censusNYcommute.shp";
	//3 Water
	public static String waterShape = "NYCWater.shp";
	//4 Outer work places point
	public static String outerwrkfile = "Rich_wrkid_out_May.csv";
	//5 Work Places and RID Full Study Area
	public static String workfile = "Rich_wrk_rid_May.csv";
	//6 Schools and RID Full Study Area
	public static String schoolfile = "Rich_Edu_school_rid_may.csv";
	//7 DayCare and RID Full Study Area
	public static String daycarefile = "Rich_Edu_daycare_rid_May.csv";		
	//8 Commuter Road ID for verification of the Road Network
	public static String rdIDfile = "Rich_commute_ridrv1.csv";
	
	
	//===================================
	/**
	 * World Parameters
	 */
	//===================================
	
	// Trace & data storage
	// viz data for visualization tools
	public static boolean exportVizData = false;	// to record or not
	public static String exportVizDataFilename = ("exportVizDataWTime.csv");
	public static int startVizData = 5*60; // 5*60 = 5AM
	public static int stopVizData = 18*60; // 18*60;  // 18*60 = 6PM
	
	// collect agent loc data in trace  for plots of multiple runs	
	public static boolean saveAgentLocations= false;	// write data to file  NOTE: FILE NAME a constant in World.jave
	public static int 	  saveLocationsStart= 300;		// when to start saving agent locations data
	public static int     saveLocationsStop = 660;		// one hour after boom

	// write results data
	public static boolean exportnetworks = true;   // whether to record network data at simulation end
	
	
	//===================================
	/**
	 * Agent Parameters
	 */
	//===================================
		
	// Work hours 
	// shift assignments
	public static Boolean 	shiftWork 	= false;	// yes/no to calc distribution of shifts
	public static Double 	swingShift 	= 0.15;	// 15% work swing shift
	public static Double 	nightShift 	= 0.05;	//  5% work night shift
	public static Double 	dayShift 	= 1 - swingShift - nightShift;	// 80% work day shift
	// shift work: day shift presumed to be 8-5, swing 4-midnight, night midnight to 8am
	public static int 		dayStart	=  480 - 30;	// m 480 = 8am
	public static int 		dayEnd		= 1020 - 30;	// m1020 = 5pm
	public static int 		swingStart	= 1020 - 30;	// m1020 = 5pm
	public static int 		swingEnd	= 1440 - 30;	// m1440 = midnight
	public static int 		nightStart	= 1440 - 30;	// m1440 = midnight
	public static int 		nightEnd	=  480 - 30;	// m 480 = 8pm
	
	// Commuting
	public static Boolean randomizeCommute = true;	// yes/no to calc commuter variation
	public static int commuteDuration = 90;	// duration in minutes of randomized commuting time
	
	// Trace
	public static boolean traceDecisions 	= false;	// agent decisions	NOTE: FILE NAME a constant in World.java

	// Post-Impact
	// after impact, probability (in %) agents will shelter when at home or work
	public static int chanceShelteratHome = 100;
	public static int chanceShelteratWork = 100;
	
    
    
	//======================================
	//
	//     ROAD NETWORK MOVEMENT RATES
	//
	//======================================
     
    /**
     * Traveling parameters and methods:
     * 	Stats on commute from http://www.governing.com/gov-data/transportation-infrastructure/commute-time-averages-drive-public-transportation-bus-rail-by-metro-area.html
     *  Average commute (with public transport) 53 minutes/ by car 30 minutes
	 *  Also see https://www.citylab.com/transportation/2016/04/why-new-york-city-commutes-are-long/476475/
	 *  And for public transport: https://moovitapp.com/insights/en/Moovit_Insights_Public_Transit_Index-121
     */	

	public static double HIGHWAY = 89;  // 89 km or 55 miles per hour -- max speed in New York is 60mph or 97 km/h
	// highway = 1483. meters/minute
	public static double RESIDENTIAL = 40; // 40 km or 25 miles per hour
	// residential = 666.7 meters/minute
	public static double DRIVING_SPEED = 40;// Average commuting speed is 20 km or 12.42 miles per hour
	
	
	
	//===================================
	/**
	 * NWMD Parameters
	 */
	//===================================
	
	// NWMD Parameters
	public static Double tLat = 40.764290;	// point of detonation; latitude units: decimal degrees (intersection of 6th Ave & 57th Street)
	public static Double tLong = -73.977290;// point of detonation; longitude units: decimal degrees
	public static int R1 =  430;			// radius in yards or meters - 100% lethal radius
	public static int R2 = 1200;			// radius in yards or meters - building collapse, fire, or lethal radiation
	public static int R3 = 2500;			// radius in yards or meters - distance to no significant injuries

	// NWMD Parameters for internal use
	public static Double decDegreesPerMeter = 1.0/111300.0; // 1 degree = 111.3km or 0.01/1113.0;  
	public static Double z1radius = R1*decDegreesPerMeter;	// units: decimal degrees from meters: 0.01= 1,113 meters (0.001 degrees = 111.32 meters)
	public static Double z2radius = R2*decDegreesPerMeter;	// units: decimal degrees from meters: 0.01= 1,113 meters (0.001 degrees = 111.32 meters)
	public static Double z3radius = R3*decDegreesPerMeter;	// units: decimal degrees from meters: 0.01= 1,113 meters (0.001 degrees = 111.32 meters)

	private static GeometryFactory fact = new GeometryFactory();
	public static Point groundZero = fact.createPoint(new Coordinate(tLong,tLat));

	public static double maxDistGZ = 0.32;					// decimal degrees for arbitrary for math model of awareness based on distance from ground zero
	
	// Agents Fleeing on foot rates
	// fleeing parameters in meters per minute
	// note that the Agent class movement standard unit is in kilometers
	public static double fleeSlowestMetersPerMin 	= 1.0;		// 1 meter per minute  (to be calibrated)
	public static double fleeSlowMetersPerMin 		= 5.0;		// 5 meters per min
	public static double fleeFastMetersPerMin 		= 10.0;		// 10 meters per min
	// fleeing parameters in decimal degrees per minute
	public static double fleeSlowestDegreesPerMin 	= fleeSlowestMetersPerMin * decDegreesPerMeter;
	public static double fleeSlowDegreesPerMin 		= fleeSlowMetersPerMin    * decDegreesPerMeter;
	public static double fleeFastDegreesPerMin 		= fleeFastMetersPerMin    * decDegreesPerMeter;
	

}

