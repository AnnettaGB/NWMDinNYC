/**
*  Disaster ABM in MASON
*  @author Annetta Burger & Bill Kennedy
*  Aug2020
*/

	package disaster;

	// Class imports
	import java.io.IOException;
	import java.time.Clock;
	import java.time.Instant;
	import java.util.ArrayList;
	import java.util.HashMap;
	import java.util.Iterator;

	import sim.engine.SimState;
	import sim.engine.Steppable;
	
	
	//===========================
	/** 
	 * Monitors Class to manage model output variables
	 */
	//===========================
	public class Log implements Steppable {
		
		// Report simulation time 
    	static Clock modelclock = Clock.systemUTC();
    	static Instant timerSimStart = modelclock.instant();

		// routine behavior agent monitors
	    public static ArrayList<Indv> indvList = new ArrayList<Indv>();
	    public static ArrayList<Group> grpList = new ArrayList<Group>();
	    public static ArrayList<String> defunctGrps = new ArrayList<String>();

		public static int badagent;  // used to count number of bad agents created at data input
		public static int badhomeNode; // used to test data at initialization
		public static int agentpopulation;  // initially set in WorldBuilder
		public static int grouppopulation;  // initially set in WorldBuilder
		public static int stayAtHome; // counts the number of individual agents stay-at-home
	    public static int firstResponders;	// count of all responders  initially set in WorldBuilder
		public static int toSchoolDaycare; // agents who go to school or daycare
	    public static int atHomeCount; // agents at home at any point in time
	    public static int atWorkCount; // agents at work at any point in time
	    public static int onCommuteCount; // agents commuting
	    public static double avg_tCommute; // average agent commute time
	    public static double avg_dCommute; // average agent commute distance    
	    
	    // post detonation monitors
	    public static HashMap <String, ArrayList<Integer> > emersizemap = new HashMap <String, ArrayList<Integer> >();
	    public static int emerGroups; // number of emergent groups formed
	    public static int emerGrpRemNo; // number of remove members from emergent group
	    public static int inactvemerGrps; // number of inactive emergent groups
	    public static int indvDeaths; // number of agents killed, initialized to 0 at agent Start
	    public static int inZoneFirstResponders;  // initial exposed first inZoneFirstResponders
	    public static int agentsSheltering; // agents sheltering post impact
	    // IDPs are identified when the agents try to go to a work or home location with null values
	    public static int IDPhome;  // Internally Displaced Person (IDP) without a home
	    public static int IDPwork;  // Internally Displaced Person (IDP) without a workplace -- i.e. work from home designation

	    public static int firstRespZone1;	// first responders in zone 1 (dead)
	    public static int firstRespZone2;	// first responders in zone 2 (injured)
	    public static int firstRespZone3;	// first responders in zone 3 (healthy)
	    public static int firstRespZone4;	// first responders in zone 4 (outside damage area)

	    public static int affectedFleeing;  // 
	    public static int affectedInCare;  // agents getting first aid, rescue, or long-term victimeInCare
	    public static int affectedInMorgue;  // reported dead or dies after some victimeInCare
	    public static int affectedReleased;  // treated (long or short term) and released to go atHomeCount
	    public static int affectedHeadedHome;  // victims that didn't get care and left w/o victimeInCare

	    public static int agentsBlocked;  // agents at water's edge or entered damage area and stopped
	    public static int inTreatment;  // agents being treated
	    public static int agentsTreated;  // agents haven been agentsTreated
	    public static int popZone2;
	    public static int popZone3;
	    public static int[] healthCat = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0};

	    public static int bad09 = 0;  // bad agentsBlocked

	    
	    
	    /**==================================================
	     *         INIT EFFECTS
	     * 
	     * initialize (schedule) nWMD event
	     * =================================================
	    */
	    
	    /**
	     * Monitors Constructor
	     */
	    public static void Logs() {
	    	System.out.println("Logs>initialization>time start: " + timerSimStart);
	    }
	    
	    
	    /**
	     * Method to step monitors
	     * Primarily used to print data at each model step
	     * Useful for code verification
	     */
		@Override
		public void step(SimState world) {	
			
			System.out.println("===================Log====================");
	    	World state = (World) world;
	    	long steps = state.schedule.getSteps();
	    	Instant timerSimCurrent = modelclock.instant();
	    	
			// System print line of step counter
			System.out.println("Log>step>" + steps);
			
			int maxsize = 0;
			int minsize = 0;
			// Print emergent network log data 
			for (Group g:state.idsToGrps.values()) {
				String leadID = g.getLeaderID();
				int health = g.getHealthStatus();
				if ( health > 1 ) {
					int grpsize = g.getSize() + 1;
					System.out.println("Log>step>grpID" + g.getID() + " " + leadID + " " + health + " " + g.getGrptype() + " size " + grpsize);
					if (maxsize == 0 || (maxsize < grpsize)) {
						maxsize = grpsize;
					}
					if (minsize > 2) {
						minsize = grpsize;
					}
					
					if (g.idsToMem.size() > 0) {
						for (Indv a: g.idsToMem.values()) {
							System.out.print(" " + a.getID());
						}
						System.out.println();
					}
				}
			}
			if (steps == Parameters.tExportNetworks) {
				if (Parameters.exportnetworks) {
					try {
						Results.exportEgrpEdgeList("disasterEgrpEdgeList", state);
					} catch (IOException e) {
						System.out.println("World>finish>problem writing edgelist");
						e.printStackTrace();
					}	
				} // if exporting edgelist
			}
			
			writeGrpsize(state, steps);
			System.out.println("Log>step>#emerGroups: " + emerGroups + " inactemerGrps: " + inactvemerGrps);
			System.out.println("Log>step>maxsize: " + maxsize + " minsize: " + minsize);
			System.out.println("Log>step>#emerGroups members leaving group: " + emerGrpRemNo);
			System.out.println("==============================" + steps + "====== at " + timerSimCurrent + "=============================");
			
		}
	    
	    
		/**
		 * Method to initialize model logs
		 */
		public void initialize_logs() {

		// Clear/set environmental monitors
		indvList.clear();
		grpList.clear();
			
		// agent stats -- pre-event
		badagent = 0;  // used to count number of bad agents created at data input
		badhomeNode = 0;  // used to test data at initialization
		agentpopulation = 0;
		agentpopulation = 0;  // initially set in WorldBuilder
		grouppopulation = 0;  // initially set in WorldBuilder
	    firstResponders = 0;  // count of all responders  initially set in WorldBuilder
		stayAtHome = 0;  // stay-at-home agents
		atHomeCount = 0; // agents at home at any point in time
		atWorkCount = 0; // agents at work at any point in time
		toSchoolDaycare = 0;  // agents who go to school or daycare
		onCommuteCount = 0; // agents commuting
		avg_tCommute = 0; // average agent commute time
		avg_dCommute = 0; // average agent commute distance
		
		// agent stats -- post-event
		emerGroups = 0; // total number of emergent groups
		emerGrpRemNo = 0; // number of remove member from emergent group
		inactvemerGrps = 0; // number of inactive groups
	    indvDeaths = 0; // number of agents killed, initialized to 0 at agent Start
	    agentsSheltering = 0; // agents sheltering post impact
	    popZone2 = 0;
	    popZone3 = 0;
	    IDPhome = 0;  // Internally Displaced Person (IDP) without a home
	    IDPwork = 0;  // Internally Displaced Person (IDP) without a workplace -- i.e. work from home designation
	    
	    agentsBlocked = 0;  // agents at water's edge or entered damage area and stopped
	    inTreatment = 0;  // agents being treated
	    agentsTreated = 0;  // agents haven been agentsTreated
	    
	    affectedFleeing = 0;
	    affectedInCare = 0;  // agents getting first aid, rescue, or long-term victimeInCare
	    affectedInMorgue = 0;  // reported dead or dies after some victimeInCare
	    affectedReleased = 0;  // treated (long or short term) and released to go atHomeCount
	    affectedHeadedHome = 0;  // victims that didn't get care and left w/o victimeInCare

	    inZoneFirstResponders = 0;  // initial exposed first inZoneFirstResponders
	    firstRespZone1 = 0;	// first responders in zone 1 (dead)
	    firstRespZone2 = 0;	// first responders in zone 2 (injured)
	    firstRespZone3 = 0;	// first responders in zone 3 (healthy)
	    firstRespZone4 = 0;	// first responders in zone 4 (outside damage area)
	    
	}
		
	
		/**
		 * Write and print out frequency of emergent group sizes
		 * at each step right before the detonation and until simulation is finished
		 * @param state
		 * @param steps
		 */
	public void writeGrpsize(World state, long steps) {
		// after the step before the bomb goes off
		if (steps >= (Parameters.tDetonation - 1)) {
			
			// for each emergent group, add group count
			for (String id: state.emerGroups) {
				Group g = state.idsToGrps.get(id);
				String gsize = Integer.toString(g.getSize() + 1);
				
				// create first map entry, if it is null
				if (emersizemap == null) {
					ArrayList<Integer> count = new ArrayList<Integer>();
					count.add(1);
					emersizemap.put(gsize, count);
				}
				// if map has groups sizes already, add a count
				else {
					if (emersizemap.containsKey(gsize)) {
						emersizemap.get(gsize).add(1);
					}
					// else create a new gsize map entry
					else { 
						ArrayList<Integer> count = new ArrayList<Integer>();
						count.add(1);
						emersizemap.put(gsize, count);
					}
				}
				
			}
			
			System.out.println("Log>writeGrpsize>counts " );
			
			for (String size:emersizemap.keySet()) {
				System.out.println("   " + size + " " + emersizemap.get(size).size() );
			}
			System.out.println();
			emersizemap.clear();
			
		}
	}
		
}
