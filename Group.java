/**
 * Disaster ABM in MASON
 * @author Annetta Burger
 * 2018-19
 */

package disaster;

//Class imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.MasonGeometry;

//Agent class for Groups of individual agents
/**
*  Group subclass of Agent builds out the characteristics of a group of individual agents 
*  Inherits the movement characteristics of Agent
*/
public class Group extends Agent {
	
	public static final long serialVersionUID = -1113018274619047013L;

	// Characteristics unique to groups
	String grptype = ""; // group can be either a carpool or emergent
	HashMap <String, Indv> idsToMem = new HashMap <String, Indv> (); //  HashMap of the group's individuals
	ArrayList<String> members = new ArrayList<String>();  // tracks all group members, including past
	ArrayList<String> currentCarpool = new ArrayList<String>();  // tracks current people in the group carpool as it changes throughout the day
	String leaderID = "";
	boolean fullcarpool; // used to indicate whether the carpool is full 
	boolean defunct; // used to indicate whether a group still exists
	
	// Health is used to adjust group agent's traveling speed
    int healthStatus = -1;		// agent's health, -1 = not set
    
    // Group goals are inherited from the Agent Class
    // varies between "commute", "findshelter", "shelter", "flee"
    
	// Schedule data and Commuting statistics
	boolean ToWork;  // agent going to (needs to be at) work
	boolean atWork;  // agent is at work
	private double commutedist;
	private int tcommuteStart;
	private int tcommuteEnd;
	private int tcommuteTime;
	
	// Emergency Response Attributes
	private double distFromGroundZero = 9999;				// distance agent is from ground zero
	
	
    // Group Agent Getters and Setters
	public void setGrptype(String x)	{ this.grptype = x; }
    public void setLeaderID(String x)		{ this.leaderID = x; }
    public void setfullcarpool(boolean x)	{ this.fullcarpool = x; }
    public void setDefunct(boolean x)	{ this.defunct = x; }
    @Override
	public void setHealthStatus(int x) 	{ this.healthStatus = x; }
    public void setToWork(boolean x)	{ this.ToWork = x; }
    public void setatWork(boolean x)	{ this.atWork = x; }
    public void set_tcommuteStart(int x)	{ this.tcommuteStart = x; }
    public void set_tcommuteEnd(int x)	{ this.tcommuteEnd = x; }
    public void set_tcommuteTime(int x)	{ this.tcommuteTime = x; }
    public void setcommuteDist(double x)	{ this.commutedist = x; }
    
    public String getGrptype()			{ return this.grptype; }
    public String getLeaderID()			{ return this.leaderID; }
    public boolean getfullcarpool()		{ return this.fullcarpool; }
    public boolean getDefunct()			{ return this.defunct; }
    @Override
	public int getHealthStatus()		{ return this.healthStatus; }
    public boolean getToWork()			{ return this.ToWork; }
    public boolean getatWork()			{ return this.atWork; }    
    public int get_tcommuteStart()		{ return this.tcommuteStart; }
    public int get_tcommuteEnd()		{ return this.tcommuteEnd; }
    public int get_tcommuteTime()		{ return this.tcommuteTime; }
    public double getcommuteDist()		{ return this.commutedist; }
    
    
    /**
     * Group --  group agent constructor
     * groups follow the designated group leader (paths, schedule, etc.)
     * @param world
     * @param grpID
     * @param neighbor
     * @param agent
     * @param goal
     */ 
	public Group(World world, long seed, Integer grpIDnum, Indv leader, Indv follower, String type) {
		super(world, seed);
		
		state = world;
			    
		setID(createID(grpIDnum));
		setGrptype(type);
		setfullcarpool(true);  // group starts with a full carpool
		setDefunct(false);  // group is active at construction
		setGoal(follower.getGoal()); // set group goal to the follower's goal (addresses group member needs first)
		world.idsToGrps.put(getID(), this); // add to HashMap used to retrieve agent objects
		
		// add first two members
		this.idsToMem.put(leader.getID(), leader);
		leader.setinGroup(true);
		leader.setindvGrpID(this.getID()); // set neighbor's group to this groupID
		addMember(leader);
		
		this.idsToMem.put(follower.getID(), follower);
		follower.setinGroup(true);
		follower.setindvGrpID(this.getID()); // set agent's group to this groupID
		addMember(follower);

		// set group leader for the group
		setLeaderID(leader.getID()); 
		leader.setisLeader(true);
		setLead(leader);
		
		// need to set group health
		updateHealth();
		
		// Set movement characteristics
		setStartID(leader.getStartID());
		setStartNode(leader.getStartNode());
		setGoalPoint(leader.getGoalPoint());
		setEndID(leader.getEndID());
		setEndNode(leader.getEndNode());
		
		// set the location to be displayed
		GeometryFactory fact = new GeometryFactory();
		setLocation(new MasonGeometry(fact.createPoint(new Coordinate(10, 10))));
		Coordinate startCoord = null;
		startCoord = leader.currentCoord;
		updatePosition(startCoord);
		currentCoord = startCoord;
		
		this.getGeometry().setUserData(this);  // make object & health status attribute accessible for WorldUI
			
	}
	
	
	/** Called every tick by the scheduler
	 * moves the agent along the path 
	 * @param world
	 */
	@Override
	public void step(SimState world) {
		// Group only steps, if it is not defunct
		if (!getDefunct()) {
		
			state = (World) world;
			long currentStep = state.schedule.getSteps();
			
			Coordinate beginCoord = currentCoord;
			
			// Update health of the group at beginning of each step			
			updateHealth();
			
			if (this.getSize() != 0) {		
				if ((this.getHealthStatus() <= 1 ) || (this.getHealthStatus() == 80) || (this.getHealthStatus() == 89)) {  // 80 & 89 are temporary testing parameters for routing around the impact area
					routine();
				}
				else if (this.getHealthStatus() > 1) {
					nonroutine(currentStep);
				}
				else {
				}
				
				double dist = Spacetime.findKiloDist(beginCoord, currentCoord);
				ckGroupCoordinates();
				
			}	
		}
		
	}
	
		
	//======================================
	//
	//     ROUTINE
	//
	//======================================
	
	
	/**
	 * Groups conduct their daily routines as carpools
	 * @param world
	 * sets routine goals
	 */
	void routine() {
		long currentStep = this.state.schedule.getSteps();
		int time = Spacetime.time24(currentStep);
		
		// Routine schedule depends on work
		// check the time to see if the agent needs to start commuting to or from work
		if (time == get_tcommuteStart()) {  // commute to work
			setToWork(true);
			this.getLeader().setToWork(true);
			Log.atHomeCount -= currentCarpool.size();
			Log.onCommuteCount += currentCarpool.size();
		}	   
		if (time == get_tcommuteEnd()) {  // commute from work
			setToWork(false);
			this.getLeader().setToWork(false);
			// only the driver is leaving work
			Log.atWorkCount -= 1;
			Log.onCommuteCount += 1;
		}
		
		// if it is time to commute, but group has not all arrived at work, travel commute path to work
		if ( getToWork() && !getatWork() ) {
			if ( getneedReroute() || gethaveDetour() || getonDetour() ) {
				// confirm that the commute path is still good, i.e. don't need a reroute and detour
				if ( !getfullcarpool() ) {
					findcarpool();
				}
				else {
					routeHome(); // place holder to route carpool groups around the impact area
				}
			}

			else {
				carPool();	
			}
		}
		   
		// if it is time to travel home, but group has not all arrived home, travel commute path to its start
		else if ( !getToWork() && getatWork() ) {
			if ( getneedReroute() || gethaveDetour() || getonDetour() ) {
				// confirm that the commute path is still good, i.e. don't need a reroute and detour
				if ( !getfullcarpool() ) {
					findcarpool();
				}
				else {
					routeHome(); // place holder to route carpool groups around the impact area
				}
			}
			else {
				carPool();
			}
		}
		
	}
	
	
	/**
	 * carPool method moves group carpool up and down a multiPath
	 * Note a 1 step/1 minute delay inherent in code is also represents a delay to drop off the rider
	 */
	public void carPool() {	
		ckPathNodes(currentPath);

		// get traffic data
	    long currentStep = this.state.schedule.getSteps();
		double speedLimit = this.state.edgesToSpeedLimit.get(currentEdge);
		double moveRate = speedLimit / 60;  // (n kilometers / 60 steps) to get km/step
		   
		// if group becomes stuck begin to reroute
		if (moveRate == 0) {
			setHealthStatus(80);
			setneedReroute(true);
			return;  // break out of commute if edge is impassable
		}
		
	       // check that we've been placed on an Edge      
	       if (getSegment() == null) {
	           return;
	       } // check that we haven't already reached our destination
	       else if (reachedDestination) {
	    	   ckPathNodes(currentPath);

	    	   // Handle carpool commuting to work
	    	   if ( !getLeader().getatWork() && getLeader().getToWork() ) {
	    		   // if the group leader is headed to work, drop off riders
	    		   
	    		   ArrayList<String> dropoffIDs = new ArrayList<String>();
		    	   for (String m: currentCarpool) { // find riders to be dropped off
		    		   String riderID = m;
		    		   Indv rider = this.idsToMem.get(riderID);
		    		   
		    		   // if the group's location is the rider's work location, drop them off
		    		   if ( (currentCoord == rider.getEndNode().getCoordinate()) && !rider.getisLeader() ) {  //  
		    			   dropoffIDs.add(rider.getID()); // collect riders that stay at this Node and are not the carpool leader
		    		   }
		    		   // if this is the driver set that they are arriving work or home
		    		   else if ( (currentCoord == rider.getEndNode().getCoordinate()) && rider.getisLeader() ) { 
		    			   if (rider.StayAtHome) {
		    				   rider.setatHome(true);
		    				   Log.atHomeCount += 1;
		    				   Log.onCommuteCount -= 1;
		    			   }
		    			   else {
		    				   rider.setatWork(true);
		    				   Log.atWorkCount += 1;
		    				   Log.onCommuteCount -= 1;
		    			   }
		    			   
		    		   }
		    	   }
		    	   
		    	   for (String d: dropoffIDs) { // drop off riders that 'work' at this Node by removing from the current carpool
		    		   String dID = d;
		    		   Indv dropoff = this.idsToMem.get(dID);
		    		   currentCarpool.remove(dID);
	    			   dropoff.setatWork(true);  // set their status to atWork
		    		   
	    			   if ( currentCarpool.size() > 1 ) {
	    				   setfullcarpool(false);
	    			   }
	    			   Log.atWorkCount += 1;
	    			   Log.onCommuteCount -= 1;
		    	   }
	    	   }
	    	   
	    	   // Handle carpool commuting from work
	    	   else if ( getLeader().getatWork() && !getLeader().getToWork() ) {
	    		   // if the group leader is headed home, pick up riders at this location
		    	   ckPathNodes(currentPath);
	    		   
	    		   ArrayList<String> pickupIDs = new ArrayList<String>();
	    			for (Indv mem: idsToMem.values()) {
			    		// if the rider's work location is the current carpool location, pick them up
	    				if ( (mem.getEndNode().getCoordinate() == currentCoord) && !mem.getisLeader()) {
	    					pickupIDs.add(mem.getID()); // collect riders that leave this Node and are not the carpool leader
	    				}
	    				// if this is the driver set that they are leaving work or home
	    				else if ( (mem.getEndNode().getCoordinate() == currentCoord) && mem.getisLeader() ) {
			    			if (mem.StayAtHome) {
			    				mem.setatHome(false);
			    				Log.atHomeCount -= 1;
			    				Log.onCommuteCount += 1;
			    			}
			    			else {
			    				mem.setatWork(false);
			    				Log.atWorkCount -= 1;
			    				Log.onCommuteCount += 1;
			    			}
	    				}
	    			}
	    			
	    			for (String p: pickupIDs) {
	    				String pID = p;
	    				Indv pickup = this.idsToMem.get(pID);
	    				currentCarpool.add(pID);
		    			pickup.setatWork(false);  // set their status to atWork
	    				
		    			if (currentCarpool.size() == idsToMem.size()) {
		    				setfullcarpool(true);
		    			}
		    			Log.atWorkCount -= 1;
		    			Log.onCommuteCount += 1;
		    			
		    			Iterator it = currentCarpool.listIterator();
		    			while (it.hasNext()) {
		    				//System.out.print(" " + it.next());
		    			}
	    			}
	    	   } 
	    	   
	       } // end reachedDestination conditional
	       
	       if (reachedFinalDestination) {
	    	   ckPathNodes(currentPath);
	    	   Indv driver = getLeader();
	    	   
	    	   if ( driver.getatWork() && driver.getToWork() ) {  // now at work because dropped off by carpool and it's not time to leave
    	   		   int currentTime = Spacetime.time24(currentStep);
    	   		   int commuteTime = currentTime -  get_tcommuteStart();
    	   		   set_tcommuteTime(commuteTime);
    	   		   setatWork(true);  // set group to at work        	      
    	   		   
      	   	  	  // reset path directions
      	   	  	  flipMultiPath();
    	   	   }
    	   	   else if ( driver.getatWork() && !driver.getToWork() ) {  // not at work and not needing to be at work 
     	   		   setatWork(false);
     	   		   Log.atHomeCount += currentCarpool.size();
     	   		   Log.onCommuteCount -= currentCarpool.size();

    	   		   // reset path directions
    	   		   flipMultiPath();
    	   	   }
    	   	   
    	   	   return;
    	   	   
	       } // end reachedFinalDestination conditional
	       
	       // make sure that we're heading in the right direction
	       if ((getToWork() && multipathDirection < 0) || (!getToWork() && multipathDirection > 0))
	       {
	           flipMultiPath();
	       }
	       
	   	   this.travelMultiPath();
	   	   
	   	   // update all the current carpool members location to the group position
	   	   updateCarpoolLocations();
	       
	}
	
	
	/** 
	 * Flip the group's multipath around for carpool purposes 
	 */
	void flipMultiPath() {
		reachedDestination = false;
		reachedFinalDestination = false;
		
		multipathDirection = -multipathDirection;
	    pathDirection = -pathDirection;
	    linkDirection = -linkDirection;
	    
	    nextPath();
	}
	
	
	//======================================
	// Rerouting methods for post-event
	//======================================
	 
	
	// placeholder method
	// pick up carpool members post-event
	/**
	 * Find and pick up carpool members from work/school
	 */
	public void findcarpool() {
		//System.out.println("Grp>step>routine>pickupmembers>");
		// if carpool member is not in the currentCarpool, pick them up
		for (Indv mem:idsToMem.values()) {
			if (!currentCarpool.contains(mem)) {		
			}
		}	
	}
	
	
	/**
	 * Group reroute method assumes the routine group has the same home location.
	 * When the group is triggered to reroute, the method checks home and work nodes
	 * for everyone in the group
	 * @param world
	 */
	  public void reroutegrp(World world) {
		  
		   if (getHealthStatus() != 80) {
			   setHealthStatus(80); // changes color to track those rerouting
		   }

		   reachedDestination = true; // agent ends trip at the current point in order to reroute
		   
		   // initial distance for search of a new node
		   double searchDist = 0.01; // start searching at for a distance in degrees for 1km
		   // set the movementRate for movement towards the start node of the detour
		   setMoveRateKmPerStep(10.0/60.0);  // movement rate for one step estimated to slow to 10km/hour in cars -- 10/60 steps
		   
		   // set the detour path startNode to the nearestNode
		   setStartNode(findNearestNode(this.getGeometry(), world.roadIntersections, world.roadNetwork, searchDist));
		   
		   // check whether any location destination nodes were destroyed by impact -- set to new defaults
		   // agents evaluate if they still have work or home location to return to 
		   // agent goes home -- if work is destroyed, if no home, agents finds a temporary location
		   // this should probably set the agents into some other nonroutine behavior --- thinking on it
		   if ( getLeader().getHomeNode() == null || getLeader().getHomeNode().getCoordinate() == null) {
			   // find a temp location
			   int count = 0;
			   double awayfromgroundzero = .005; // .01 in degrees
			   Node tempnode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
			   while (tempnode == null) {
				   // look further away from ground zero for a startNode
				   setStartNode(findsafeNearestNode(this.getGeometry(), world.roadIntersections, world.roadNetwork, world.waterField, searchDist));
				   tempnode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
				   count += 1;
				   
				   // only try to reset the start node using findsafeNearestNode() twice
				   // otherwise nudge the agent a little away from ground zero and its current position
				   if (count >= 1) {
					   double clat = this.getGeometry().geometry.getCoordinate().y;
					   double clong = this.getGeometry().geometry.getCoordinate().x;
					   double newlat = clat + awayfromgroundzero;
					   double newlong = clong + awayfromgroundzero;
				   	   Coordinate tempCoord = new Coordinate(newlong, newlat);
					   currentCoord = tempCoord;
					   updatePosition(currentCoord);

					   return;

				   }
			   }
			   
			   // set all group members as homeless with the new tempnode
			   for (Indv mem: idsToMem.values()) {
				   Log.IDPhome += 1;  // track internally displace people with no homes
				   mem.setisHomeless(true);
				   mem.setHomeNode(tempnode);
			   }
		   }
		   
		   // checks for null worknodes in the group and resets them
		   // groups don't go home post-event
		   for (Indv mem: idsToMem.values()) {
			   
			   Node workNode = mem.getWorkNode();
			   if ( workNode == null || workNode.getCoordinate() == null ) {
				   //nowork = true;
				   Log.IDPwork += 1;  // track internally displace people with no workplaces			   
				   mem.setWorkNode(mem.getHomeNode());
				   mem.setEndNode(mem.getHomeNode());
			   }
		   }		   
 
	 }
	  
	  
	  /**
	   * Method to move agent to detour coordinates
	   */
	  public void goToDetour() {
		  //System.out.println("Grp>reroute>goToDetour>");
		  Coordinate startCoord = getStartNode().getCoordinate();
		  
		  // when agent moves towards the start of the detour and returns true, else returns false
		  if (!startCoord.equals2D(currentCoord)) {
			  moveToCoord(startCoord);
			  return;
		  }
		  // if agent is now at the start of the detour, set on detour
		  if (startCoord.equals2D(currentCoord)) {
			  setonDetour(true);
			  return;
		  }
		  else {
			  //System.out.println("Indv>reroute>goToDetour>error with startCoord: " + startCoord + " current: " + currentCoord);
		  }	  
	  }
	  
	  
	
	//======================================
	//
	//     NON-ROUTINE
	//
	//======================================
	
	/**
	 * Nonroutine method used post-detonation  
	 * Current functions for EMERGENT GROUPS ONLY
	 * @param currentStep
	 */
	public void nonroutine(long currentStep) {
		
		// check/update group goal by checking the leader
		if (getLeader().getGoal() != getGoal()) {
			setGoal(getLeader().getGoal());
		}
		
		if (this.getGrptype() == "emergent") {
			
			if (Effects.inExZone(this)) {
				// if agent is at home and the homeNode is not damaged, reset goal to shelter
				// health is not further degraded due to damaged building/Node
				// individual decides whether to stay in place
				// decision can use Random an int value from chanceShelteratHome or chanceShelteratWorm
				// this needs a design -- until find a basis for this probability
				if ( (getLeader().getatHome() && ( (getLeader().getHomeNode() != null) && (getLeader().getHomeNode().getCoordinate() != null)) ) || 
						(getLeader().getatWork() && ( (getLeader().getWorkNode() != null) && (getLeader().getWorkNode().getCoordinate() != null)) )){
					int random = new Random().nextInt(Parameters.chanceShelteratHome);  // 100% chance to stay at home

					if (random == 0) {
						// emergent group disbands once in a shelter
						// set all members to shelter; remove members until set defunct
						for (Indv mem: idsToMem.values()) {
							mem.setGoal("shelter");
							remMember(mem);
							Log.agentsSheltering += 1;
						}
					}
				}
				
				// if emerg group fleeing
				if ( getGoal() == "flee") {
					// if agent is wounded, but ambulatory and is in a building (work or home)
					if ( getHealthStatus() !=9 && (getLeader().getatHome() || getLeader().getatWork()) ) { // agents with health of 9, are fatally hurt and can't move
						long delayStep = Parameters.tDetonation + 3*getHealthStatus();
						if (currentStep > (delayStep)) {  // delay to escape building varied by health (6-24 minutes)
							getGrpAway(currentStep);
						}
					}
					// else if agent is just wounded, but ambulatory
					else if (getHealthStatus() !=9) {
						getGrpAway(currentStep);
					}
					  
				} // end of fleeing individuals
				  
				else if (getGoal() == "findshelter") {
					findShelter();
				}  // end of shelter individuals check -- can be built out for other behavior
				  
				else if (getGoal() == "shelter") {
					// need to add a check for ishomeless after emergent group arrives at temp shelter
					shelter();
				}
				
			  } // end Zone2 agent update goals	and group
			else { 
				// outside the zone
				getGrpAway(currentStep);
			}  // end fleeing groups
				
			if (getGoal() == "findshelter") {
				findShelter();
			}  // end of shelter individuals check -- can be built out for other behavior
				  
			else if (getGoal() == "shelter") {
				// need to add a check for ishomeless after emergent group arrives at temp shelter
				shelter();
			}
			
		} // end emergent group nonroutine
		
		else { 
			// Carpool nonroutine behavior 
			// Placeholder for carpool group reroutes around impact area
		} // end carpool group nonroutine
		
	}
	

	
	//======================================
	//
	//     EMERGENCY BEHAVIOR
	//
	//======================================
	
	
	/**
	 * Placehlder method to shelter carpool groups
	 */
	public void shelter() {

	}
	
	
	/**
	 * Placeholder method to search for carpool members
	 * @param currentStep
	 * @param mem
	 */
	void findGrpMembers(long currentStep, Indv mem) {
		//System.out.println("Group>nonroutine>findGrpMembers " + getID() + " goal: " + getGoal());
		
		if (!getGoalPoint().equals(mem.currentCoord)) {
			setGoalPoint(mem.currentCoord);
		}
		else {
			getGrpAway(currentStep);
		}
				
	}
	
	
	/**
	 * Find routes for detour to shelter
	 */
	void findShelter() {
		// used in getaway() method to step through finding routing and needed detours
		// i.e. if there is a first responder, get help, or get information
		//System.out.println("Grp>nonroutine>findshelter " + getID() + " goal: " + getLeader().getGoal());
		if (getneedReroute() || getonDetour() || gethaveDetour()) {
			// confirm that the commute path is still good, i.e. don't need a detour
			if ( getneedReroute() && !gethaveDetour() ) {  
				//System.out.println("Grp>step>nonroutine>findshelter> " + getID() + " needs route home or temp shelter");
				if ( this.getGrptype() == "carpool") {
					//System.out.println("Grp>nonroutine>findshelter " + getID() + " is carpool");
					routeHome(); // place holder to route carpool groups around the impact area
				}
				else {
					// emergent groups
					//System.out.println("Grp>nonroutine>findshelter " + getID() + " is emergent");
					routeShelter();
				}			
			}
			
			if ( gethaveDetour() && !getonDetour() ) {
				//System.out.println("Grp>step>nonroutine>findshelter> " + getID() + " move towards detour start------------------------------");
				goToDetour();
			}
			
			if ( getonDetour() ) {
				//System.out.println("Grp>step>nonroutine>findshelter> " + getID() + " detours");
				detour();
			}
		}
	}
	
	
	/**
	 * Placeholder/hook to route group agents around impact area
	 */
	void routeHome() {
		
	}
	
	
	/**
	 * Move group agents away from impact area
	 * @param currentStep
	 */
	void getGrpAway(long currentStep) {
		  // method for agent to flee from the area

		   if ( (getGoalPoint() == null)  || (this.currentCoord == null) ) {
			   return;
		   }
		   
		   int time = Spacetime.time24(currentStep);
		   		   
		   // Basic spatial data for agents each step
		   double speedrate	= Spacetime.kilometersToDegrees(getMoveRateKmPerStep()); // change movement rate to degrees
		   double cLat 		= currentCoord.y;	// current agent Lat
		   double cLong		= currentCoord.x;
		   double gLat		= getGoalPoint().y;	// goal Lat
		   double gLong		= getGoalPoint().x;
		   double dLat		= gLat - cLat;			// change in Lat distance from current location to goal
		   double dLong		= gLong - cLong;        // change in Long distance from current location to goal
		   double speedLat	= dLat;		// initialize these with Lat distance from current location to goal; used for distance agent moves in one step
		   double speedLong = dLong;   // initialize these with Long distance from current location to goal; used for distance agent moves in one step
		   double dgoal		= Math.sqrt(dLat*dLat + dLong*dLong);	// rough lat/long distance from current location to goal
		   double dgzLat	= Parameters.tLat - cLat;
		   double dgzLong	= Parameters.tLong - cLong;
		   double dist 		= getLocation().geometry.distance(Parameters.groundZero);    
		   distFromGroundZero	 		= Math.sqrt(dgzLat*dgzLat + dgzLong*dgzLong); // dist from ground zero
		   
		   if (distFromGroundZero > Parameters.maxDistGZ) { 
			   Parameters.maxDistGZ = distFromGroundZero;	// update max
		   }
			   
			//==========================================
			// AFFECTED agents on foot have fleeing goal (goalPoint (type Coordinate))
			if (getGoalPoint() != null) { // given fleeing goal by Effects calculation
				
				// Can reach in 1 step, i.e., this step?
				// limit step size based on speed
				// if dist to goal > allowed per step: reduce to speed limit
				if (dgoal > speedrate)	{// if can't reach goal in 1 step (fleeing rate is dec.degrees) per min
					// find the lat/long distance to travel for this step
					double convert = speedrate/dgoal;
				   	speedLat = speedLat * convert;
				   	speedLong = speedLong * convert;
				}
				
				// calculate new x/y (lat/long) for agent's potential position update
				double newLat = cLat + speedLat;
				double newLong = cLong + speedLong;
				   	   
				// if already reached fleeing goal ...
				if (dgoal < 0.001) {
					// goal may still be in blast area, if still inside zone3, create new fleeing goal
					
					if (dist < Parameters.z3radius + 0.01) {
						// create new fleeing goal				
						Effects.createFleeingGoalCord(this, state);						
						
						// test new fleeing goal point to see whether it is in the water
						Coordinate savedCoord = currentCoord;
						updatePosition(getGoalPoint());
						Bag waterobjs = state.waterField.getCoveringObjects(getGeometry());
						if (!waterobjs.isEmpty()) {
							Coordinate newCoord = setAltGoalPt(state.waterField, getGoalPoint(), speedrate);
							setGrpGoalPt(newCoord);
						}
						updatePosition(savedCoord);
						updateLocations();
						
					}
					else {	// transfer to road system and look for shelter
						//System.out.println("Grp>step>getAway>at goalpoint; find shelter ");
						setneedReroute(true);
						setGoal("findshelter");
						updateMemGoals();
					}
				} // end reached goal
				
				else { // haven't reached interim fleeing goal, check to see whether made outside blast area or take step toward goal

					if (dist > Parameters.z3radius + 0.01) {	// check whether agent is outside the impact area
						// transfer to road system and look for shelter
						setneedReroute(true);
						setGoal("findshelter");
						updateMemGoals();
					}
					else { // evaluate movement on foot
						// try out the new coordinate position
						Coordinate fleeingCoord = new Coordinate(newLong, newLat);

						// if the new coordinate is not past the goal coodinate
						// move step at the movement rate otherwise move all the way to the goal
						if (Math.abs(speedLat) < Math.abs(dLat) || Math.abs(speedLong) < Math.abs(dLong)) { // take a step toward goal  
								
								Bag polity = state.waterField.getCoveringObjects(this.getGeometry());
								// if the water polity is not empty (i.e. there is water at prospective position), stay at the current position
								if (! polity.isEmpty() || getHealthStatus() == 72) { // not in the water
									// reset fleeing coordinate
									fleeingCoord = findAltCoord(state.waterField, currentCoord, speedrate);
									setGrpGoalPt(setAltGoalPt(state.waterField, fleeingCoord, speedrate));
									currentCoord = fleeingCoord;
									updatePosition(fleeingCoord);

									newLong = cLong;	// don't move this step
									newLat = cLat;

									if (getHealthStatus() != 72)   // check already blocked
									{
										Log.agentsBlocked++;
										setHealthStatus(72); 
									} // note that agent is blocked 
								}

								else {	
									// move step toward goal
									fleeingCoord = new Coordinate(newLong, newLat);
									currentCoord = new Coordinate(newLong,newLat);

									updatePosition(fleeingCoord);
									updateLocations();
								}

						}
						else  // move all the way to the goal in one step
						{
							fleeingCoord = new Coordinate(gLong, gLat);
							updatePosition(fleeingCoord);
							updateLocations();
						}
					}// eval on foot					
				}// end else hasn't reached fleeing goal
			    	
			  }// end has fleeing goal
		   
	  }// end react
	
	
	  //=======================
	  // to/from road helper
	  //=======================
	
	/**
	 * Unverified routeShelter method
	 * Placeholder for carpool groups routing around impact area
	 */
	  public void routeShelter() {
		  reachedDestination = true; // agent ends trip at the current point in order to reroute
		  // convert meter foot rate to the model's kilometers standard unit
		  double speedrate = Spacetime.degToKilometers(getMoveRateKmPerStep());
		  
		  // initial distance for search of a new node
		  double searchDist = 0.02; // start searching at for a distance in degrees for 1km
		  
		  Node startNode = findsafeNearestNode(this.getGeometry(), state.roadIntersections, state.roadNetwork, state.waterField, searchDist);
		  if (startNode == null) {
			  return;
		  }
		  else {
			  setStartNode(startNode);
		  }
		  
		  setGoalPoint(getStartNode().getCoordinate()); // reset goal coordinate

		  if ( getGoalNode() == null || getGoalNode().getCoordinate() == null) {
			  Node tempNode = findTempLocationNode(state.roadIntersections, state.roadNetwork);
			  if (tempNode == null) {
				  tempNode = findsafeNearestNode(this.getGeometry(), state.roadIntersections, state.roadNetwork, state.waterField, searchDist);
			  }

			  setGoalNode(tempNode);		  
			  setGoalPoint(getGoalNode().getCoordinate()); // reset goal coordinate
		  }

		  // check if there is an available currentPath based on this HomeNode
		  // if so head to the new startNode
		  currentPath = findNewAStarPath(getStartNode(), getGoalNode());
		  if (currentPath != null) {
			  setneedReroute(false);
			  sethaveDetour(true);
		  }
		  else {
			   // no path to homeNode; move to a new random location then search
			   int count = 0;
			   double movedist = .0005; // .01 in degrees
			   // while no path is found to a random temporary node, reset the random startnode and try again
			   searchDist = .02;
			   Node tempnode = findTempLocationNode(state.roadIntersections, state.roadNetwork);
			   while (tempnode == null) {
				   // look further away from ground zero for a startNode
				   setStartNode(findsafeNearestNode(this.getGeometry(), state.roadIntersections, state.roadNetwork, state.waterField, searchDist));
				   tempnode = findTempLocationNode(state.roadIntersections, state.roadNetwork);
				   count += 1;

				   // only try to reset the start node using findsafeNearestNode() once more
				   // otherwise nudge the agent a little away from ground zero and its current position
				   if (count >= 1) {
					   double clat = this.getGeometry().geometry.getCoordinate().y;
					   double clong = this.getGeometry().geometry.getCoordinate().x;
					   double movelat = (clat + Parameters.tLat)/Math.abs(clat)*movedist;
					   double movelong = (clong + Parameters.tLong)/Math.abs(clong)*movedist;
					   
					   double newlat = clat + movelat;
					   double newlong = clong + movelong;

				   	   Coordinate tempCoord = new Coordinate(newlong, newlat);
					   currentCoord = tempCoord;
					   updatePosition(currentCoord);

					   return;

				   }
			   }

			   // once nodes and path are found
			   
			   setGoalNode(tempnode);  // agents shelter at this tempnode
			   setEndNode(getGoalNode());
			   setGoalPoint(getGoalNode().getCoordinate()); // reset goal coordinate
			   setneedReroute(false);
			   sethaveDetour(true);
		  }
	  }
	
	  /**
	   * Unverified routeShelter method
	   * Placeholder for carpool groups detouring around impact area
	   * @return
	   */
	  public boolean detour() {
	  
		  // if at start coordinate, begin the path
		  if (getStartNode().getCoordinate().equals2D(currentCoord)) {
			  reachedDestination = false; // agent is not at the new destination
			  pathDirection = 1; // head the agent in the +1 path direction
			  beginPath(currentPath);
		  }
		  
		  // if at the end of the path, fleeing agent shelters???
		  
		  else if (getGoalNode().getCoordinate().equals2D(currentCoord)) {
			  // need an alternative non-fleeing goal
			  setneedReroute(false);  // no longer needs to be rerouted, agent has arrived at destination
			  setonDetour(false);  // agent has ended its detour
			  sethaveDetour(false);  // cancel detour route...return to normal
			  setGoal("shelter");  // agent is no longer fleeing, is at home or a temporary location
			  updateMemGoals();

			  return false;
		   }
		  travelPath();
		  
		  return true;
	  }
	
	
	  /**
	   * Unverified method
	   * Placeholder for carpool groups routing around impact area
	   * @param point
	   * @param nodefield
	   * @param roadNet
	   * @param blockedfield
	   * @param startDistance
	   * @return
	   */
	 public Node findsafeNearestNode(MasonGeometry point, GeomVectorField nodefield, GeomPlanarGraph roadNet, GeomVectorField blockedfield, double startDistance) {
		   Bag candidates = nodefield.getGeometries(); //world.roadIntersections.getGeometries();
		   double dist = startDistance;
		   int count = 0;
		   int loopcount = 0;
		   while (true) {
			   loopcount += 1;
			   // simply increase or decrease distance to find an appropriate number of candidates
			   candidates = nodefield.getObjectsWithinDistance(point, dist);  //world.roadIntersections.getObjectsWithinDistance(point, dist);
			   // remove any points that are within Zone 3 + .01 degrees
			   for (Object o: candidates) {
				   MasonGeometry geo = (MasonGeometry) o;
				   if (geo.geometry.distance(Parameters.groundZero) <= (Parameters.z3radius+.01)) {
					   candidates.remove(o);
					   count += 1;
				   }
			   }

			   count = 0; // reset removed candidate count
			   if (loopcount == 20) {
				   // agent couldn't find a candidate node, flee further out with a randomCoord move
				   setGoal("flee");
				   updateMemGoals();
				   setneedReroute(false);
				   Coordinate newCoord = findRandCoord(blockedfield, this.currentCoord, this.getMoveRateKmPerStep());
				   this.updatePosition(newCoord);
				   return null;
			   }

			   if (candidates.size() == 0) {
				   dist *= 10;
			   }
			   else if (candidates.size() > 3) {
				   dist *= 0.5;			   
			   }
			   else { break; }	   
		   }
		   
		   double minDist = Double.MAX_VALUE;
		   MasonGeometry nearest = null;
		   
		   // refinement step: now it's time to find the nearest among candidates
		   for (Object ele : candidates) {
			   MasonGeometry geo = (MasonGeometry) ele;
			   double tmp = geo.geometry.distance(point.geometry);
			   if (minDist > tmp) {
				   nearest = geo;
				   minDist = tmp;
			   }
		   }
 
		   Bag nearestNodes = nodefield.getCoveredObjects(nearest); 
		   MasonGeometry nearestObj = (MasonGeometry) nearestNodes.get(0);
		   Coordinate nearestNodeCoord = nearestObj.geometry.getCoordinate();
		   
		   // Now grab the node in the network at the nearest geometry in the node field.
		   Node nearestNode = null; 
		   
	      Iterator<?> nodeIterator = roadNet.nodeIterator();
	      
	      while (nodeIterator.hasNext())
	      {
	      	// Create a GeometryVectorField of points representing road intersections
	          Node node = (Node) nodeIterator.next();
	          Coordinate coord = node.getCoordinate();
	          if (nearestNodeCoord == coord) {
	       	   nearestNode = node;
	          }
	      }

	      return nearestNode;
	  }
	
	
	/**
	   * Finds a temporary Node location 1km near the agent
	   * removes nodes too close to ground zero
	   * checks for good path
	   * @param nodeField
	   * @param roadNet
	   * @return
	   */
	  Node findTempLocationNode(GeomVectorField nodeField, GeomPlanarGraph roadNet) {
		  // temporary method to put agents at a random location node near its current coordinate
		  ArrayList<GeomPlanarGraphDirectedEdge> testpath = null;
		  int randomNum = 0;
		  Node randomNode = null; 
		  int count = 0;
		  
		  // look for location 1km away from stuck location --- in some cases less than this produced not enough node locations
		  double dist = .01;  // 1km -- ~.01 degrees
		  // find nodes in the modified roadNetwork within distance
		  Bag nearestNodes = nodeField.getObjectsWithinDistance(this.getGeometry(), dist);
		  
		  // remove nodes too close to groundzero (inside z3radius), too close to the agent (within min distance of .0001, with null coordinates),
		  // or if has to pass close to ground zero to get there or 
		  // if the proposed node distance to ground zero is closer to ground zero than the current location, remove it from the list of candidates
		  for (Object o: nearestNodes) {
			  MasonGeometry geo = (MasonGeometry) o;
			  if (geo.geometry.distance(this.getGeometry().geometry) < .0001 || geo.geometry.distance(Parameters.groundZero) <= (Parameters.z3radius + .01)
					  || geo.geometry.getCoordinate() == null || geo.geometry.distance(Parameters.groundZero) <= this.getGeometry().geometry.distance(Parameters.groundZero)) {
				  nearestNodes.remove(o);
			  }
		  }
		  
		  // return a randomNode that has a path from the StartNode
		  while (testpath == null) {
			  int numNodes = nearestNodes.size();
			  if (numNodes == 0) {
				  return null;
			  }
		  
			  // pick a random node MasonGeometry by its Index
			  randomNum = new Random().nextInt(numNodes);
			  MasonGeometry randomgeo = (MasonGeometry) nearestNodes.get(randomNum);	  
		  
			  // Use nodeIterator to find and assign the Node in RoadNetwork that is at the random MasonGeometry	   
			  Iterator<?> nodeIterator = roadNet.nodeIterator();
	       
			  while (randomNode == null) {
				  Node node = (Node) nodeIterator.next();
				  if (node.getCoordinate().equals2D(randomgeo.geometry.getCoordinate())) {
					  randomNode = node;
				  }			  
			  }

			  testpath = findNewAStarPath(getStartNode(), randomNode);
			  // if the testpath doesn't work, remove the node from consideration in the nearestNodes bag
			  if (testpath == null) {
				  nearestNodes.remove(randomNum);
			  }

			  count += 1;
		  }

		  return randomNode;
	  }
	    
	  
	/**
	 * Find a random coordinate; used to reroute  
	 * @param water
	 * @param Coord
	 * @param speed
	 * @return
	 */
	public Coordinate findRandCoord(GeomVectorField water, Coordinate Coord, double speed) {
		  // create 4 test coordinates
		  double move = speed;
		  Coordinate testPosX = new Coordinate((Coord.x+move),Coord.y);
		  Coordinate testNegX = new Coordinate((Coord.x-move),Coord.y);
		  Coordinate testPosY = new Coordinate(Coord.x, (Coord.y+move));
		  Coordinate testNegY = new Coordinate(Coord.x, (Coord.y-move));
		  
		  // create four direction probabilities
		  double north = 1;
		  double south = 1;
		  double east = 1;
		  double west = 1;
		  
		  Bag candidateCoords = new Bag();
		  Coordinate alternativeCoord = new Coordinate();
		  
		  // Add coordinates not on water to a Bag of candidates for testing 
		  // Set the direction probabilities based on whether it is in the water
		  // test PosX
		  updatePosition(testPosX);
		  Bag waterobjects1 = water.getCoveringObjects(this.getGeometry());
		  if ( waterobjects1.isEmpty() ) {
			  candidateCoords.add(testPosX);
			  east = 0.5;
		  }	  
		  else east = 0.0;
		  
		  // test NegX
		  updatePosition(testNegX);
		  Bag waterobjects2 = water.getCoveringObjects(this.getGeometry());
		  if ( waterobjects2.isEmpty() ) {
			  candidateCoords.add(testNegX);
			  west = 0.5;
		  }	  
		  else west = 0.0;
		  
		  // test PosY
		  updatePosition(testPosY);
		  Bag waterobjects3 = water.getCoveringObjects(this.getGeometry());
		  if ( waterobjects3.isEmpty() ) {
			  candidateCoords.add(testPosY);
			  north = 0.5;
		  }
		  else north = 0.0;
		  
		  // test NegY
		  updatePosition(testNegY);
		  Bag waterobjects4 = water.getCoveringObjects(this.getGeometry());
		  if ( waterobjects4.isEmpty() ) {
			  candidateCoords.add(testNegY);
			  south = 0.5;
		  }
		  else south = 0.0;
		  
		  if (candidateCoords.size() == 0) {
			  alternativeCoord = Coord;
		  }
		  else {  
			  Coordinate tempCoord = null;
			  
			  // test each remaining candidate coordinate for the distance farthest from ground zero
			  double maxDist = 0;
			  for (Object o: candidateCoords) {		  
				  tempCoord = (Coordinate) o;
				  double tempDist = tempCoord.distance(Parameters.groundZero.getCoordinate()); // for NWMD
				  if ( tempDist > maxDist ) {
					  maxDist = tempDist;
					  // assign max dist from ground zero a higher probability
					  if (tempCoord.equals2D(testNegY)) {
						  south = 0.7;
					  }
					  else if (tempCoord.equals2D(testPosY)) {
						  north = 0.7;
					  }
					  else if (tempCoord.equals2D(testNegX)) {
						  west = 0.7;
					  }
					  else east = 0.7;
				  }
			  }
			  
			  double total = north+south+east+west;
			  double normalizedNorth = north/total;
			  double normalizedSouth = south/total;
			  double normalizedEast = east/total;
			  double normalizedWest = west/total;
			  // Cumulative distribution function (CDF)
			  double sum = 0.0;
			  double cdfNorth = normalizedNorth;
			  sum += normalizedNorth;
			  double cdfSouth = sum + normalizedSouth;
			  sum += normalizedSouth;
			  double cdfEast = sum + normalizedEast;
			  sum += normalizedEast;
			  double cdfWest = sum + normalizedWest;
			  
			  Random rnd = new Random();
			  double randomDirection = rnd.nextDouble();
			  
			  if(randomDirection < cdfNorth) {
				  // you should go to north
				  alternativeCoord = testPosY;
			  } else if(randomDirection < cdfSouth) {
				  // you should go to south
				  alternativeCoord = testNegY;
			  } else if(randomDirection < cdfEast) {
				  // you should go to east
				  alternativeCoord = testPosX;
			  } else {
				  // go west
				  alternativeCoord = testNegX;
			  }		  
			  
		  }
		  
		  return alternativeCoord;  
	  }


	/**
	 * Find alternate coordinate; used to reroute
	 * @param water
	 * @param Coord
	 * @param speed
	 * @return
	 */
	public Coordinate findAltCoord(GeomVectorField water, Coordinate Coord, double speed) {  
		  // create 4 coordinates to test the direction of the water
		  double move = speed;
		  Coordinate testPosX = new Coordinate((Coord.x+move),Coord.y);
		  Coordinate testNegX = new Coordinate((Coord.x-move),Coord.y);
		  Coordinate testPosY = new Coordinate(Coord.x, (Coord.y+move));
		  Coordinate testNegY = new Coordinate(Coord.x, (Coord.y-move));
		  
		  MasonGeometry testPosLat = new MasonGeometry(fact.createPoint(testPosX));
		  MasonGeometry testNegLat = new MasonGeometry(fact.createPoint(testNegX));
		  MasonGeometry testPosLong = new MasonGeometry(fact.createPoint(testPosY));
		  MasonGeometry testNegLong = new MasonGeometry(fact.createPoint(testNegY));
		  
		  Bag candidateCoords = new Bag();
		  Coordinate altCoord = new Coordinate();
		  
		  // Add coordinates not on water to a Bag of candidates for testing 
		  // test PosX  
		  Bag waterobjects1 = water.getCoveringObjects(testPosLat.getGeometry());
		  if ( waterobjects1.isEmpty() ) {
			  candidateCoords.add(testPosX);
		  }	  
		  
		  // test NegX
		  Bag waterobjects2 = water.getCoveringObjects(testNegLat.getGeometry());
		  if ( waterobjects2.isEmpty() ) {
			  candidateCoords.add(testNegX);
		  }	
		  
		  // test PosY
		  Bag waterobjects3 = water.getCoveringObjects(testPosLong.getGeometry());
		  if ( waterobjects3.isEmpty() ) {
			  candidateCoords.add(testPosY);
		  }
		  
		  // test NegY
		  Bag waterobjects4 = water.getCoveringObjects(testNegLong.getGeometry());
		  if ( waterobjects4.isEmpty() ) {
			  candidateCoords.add(testNegY);
		  }	  
		  
		  // test each candidate coordinate for the distance farthest from ground zero
		  Coordinate tempCoord = null;
		  double maxDist = 0;
		  for (Object o: candidateCoords) {
			  tempCoord = (Coordinate) o;
			  if ( tempCoord.distance(Parameters.groundZero.getCoordinate()) > maxDist ) {
				  maxDist = tempCoord.distance(Parameters.groundZero.getCoordinate());			  
				  altCoord = tempCoord;
			  }
		  }

		  return altCoord;  
	}

	/**
	 * Used to find and set an alternate goal coordinate for reroute
	 * @param water
	 * @param Coord
	 * @param speed
	 * @return
	 */
	public Coordinate setAltGoalPt(GeomVectorField water, Coordinate Coord, double speed) {  
		  // create 4 coordinates to test the direction of the water
		  double move = speed*10;
		  Coordinate testPosX = new Coordinate((Coord.x+move),Coord.y);
		  Coordinate testNegX = new Coordinate((Coord.x-move),Coord.y);
		  Coordinate testPosY = new Coordinate(Coord.x, (Coord.y+move));
		  Coordinate testNegY = new Coordinate(Coord.x, (Coord.y-move));
		  
		  MasonGeometry testPosLat = new MasonGeometry(fact.createPoint(testPosX));
		  MasonGeometry testNegLat = new MasonGeometry(fact.createPoint(testNegX));
		  MasonGeometry testPosLong = new MasonGeometry(fact.createPoint(testPosY));
		  MasonGeometry testNegLong = new MasonGeometry(fact.createPoint(testNegY));
		  
		  Bag candidateCoords = new Bag();
		  Coordinate altGoal = new Coordinate();
		  
		  // Add coordinates not on water to a Bag of candidates for testing 
		  // test PosX  
		  Bag waterobjects1 = water.getCoveringObjects(testPosLat.getGeometry());
		  if ( waterobjects1.isEmpty() ) {
			  candidateCoords.add(testPosX);
		  }	  
		  
		  // test NegX
		  Bag waterobjects2 = water.getCoveringObjects(testNegLat.getGeometry());
		  if ( waterobjects2.isEmpty() ) {
			  candidateCoords.add(testNegX);
		  }	
		  
		  // test PosY
		  Bag waterobjects3 = water.getCoveringObjects(testPosLong.getGeometry());
		  if ( waterobjects3.isEmpty() ) {
			  candidateCoords.add(testPosY);
		  }
		  
		  // test NegY
		  Bag waterobjects4 = water.getCoveringObjects(testNegLong.getGeometry());
		  if ( waterobjects4.isEmpty() ) {
			  candidateCoords.add(testNegY);
		  }
		  	  
		  // test each candidate coordinate for the distance farthest from ground zero
		  Coordinate tempCoord = null;
		  double maxDist = 0;
		  for (Object o: candidateCoords) {
			  tempCoord = (Coordinate) o;
			  //System.out.print(" " + tempCoord);
			  if ( tempCoord.distance(Parameters.groundZero.getCoordinate()) > maxDist ) {
				  maxDist = tempCoord.distance(Parameters.groundZero.getCoordinate());			  
				  altGoal = tempCoord;
			  }
		  }
		    	  
		  setGrpGoalPt(altGoal);
		  
		  return altGoal;  
	}

	
	
	//==============================================
	//
	// 		UTILITY FUNCTIONS
	//
	//==============================================
	
	/**
	 * Verification method
	 * Used to confirm group multiPath goalNodes
	 */
	void ckGoalNodes() {
		for (Node n: multiGoalNodes) {
			System.out.print(n.getCoordinate());
		}
	}
	
	
	/**
	 * Verification method
	 * Used to confirm group member coordinates
	 */
	void ckGroupCoordinates() {
		for (String id: currentCarpool) {
			Indv i = state.idsToIndvs.get(id);
		}
	}
	
	
    public int getSize() {
    	int size = 0;
    	size = this.idsToMem.size() - 1;
    	if (size == 0 ) {
    		return 0;
    	}
    	else {
    		return size;
    	}
	}
	
	/**
	 * Sets the Goal Point of the Group and its Leader
	 * @param coord
	 */
	void setGrpGoalPt(Coordinate coord) {
		getLeader().setGoalPoint(coord);
		setGoalPoint(coord);
	}
	
	
	/**
	 * Sets all the group action parameters to the leader's
	 * @param leader
	 */
	void setLead(Indv leader) {
		leader.setisLeader(true);
		setLeaderID(leader.getID());
		multiPath = leader.multiPath; // set group path to the leader's
	}
	
	
	/**
	 * Get the group leader
	 * Finds new leader, if leader is null
	 * @return
	 */
	Indv getLeader() {
		Indv leader;
		leader = idsToMem.get(getLeaderID());
		if (leader == null) {
			//System.out.println("Group>getLeader>no leader in group");
			return null;
		}
		else {
			return leader;
		}
	}
	
	
	/**
	 * Change the group leader
	 * @param oldLead
	 * @param newLead
	 */
	void changeLead(Indv newLead) {		
		getLeader().setisLeader(false);
		newLead.setisLeader(true);
		setLeaderID(newLead.getID());
	}
	

	/**
	 * Select the senior member of the group as new leader
	 * Groups with a leader under 16, become defunct
	 */
	public void selectSeniorLead() {
		int age = 0;
		String newLeadID = "";
		// select the oldest member of the group as the new leader
		for (Indv mem: idsToMem.values()) {
			if (age < mem.getAge()) {
				changeLead(mem);
				newLeadID = mem.getID();
			}
		}
	}
	
	
	/**
	 * Add member of group -- update group health
	 * @param newMem
	 */
	void addMember(Indv newMem) {
		members.add(newMem.getID());  
		this.idsToMem.put(newMem.getID(), newMem);
		newMem.setinGroup(true);
		newMem.setindvGrpID(getID());		
		updateHealth();	
		// update all the emergent network
		if (getGrptype() == "emergent") {
			for (Indv a:idsToMem.values()) {
				// update emergent network of every member, but the new member
				// create the emergent network for the new member
				if (a != newMem) {
					a.getEmergnet().add(newMem.getID());
					newMem.getEmergnet().add(a.getID());
				}
			}
		}
	}
	
	
	/**
	 * Remove member of group -- update group health
	 * @param oldMem
	 */
	void remMember(Indv oldMem) {
		this.idsToMem.remove(oldMem.getID(), oldMem);
		oldMem.setinGroup(false);
		
		// update the emergent network
		if (getGrptype() == "emergent") {
			members.remove(oldMem.getID());  // only emergent members can be permanently removed from the group members lit
			Log.emerGrpRemNo += 1;  // count number of remove members from emergent group
			
			for (Indv a:idsToMem.values()) {		
				// if the group size will be too small set it to defunct, i.e. less than 2 members
				// remaining member is to no group
				if (this.getSize() <= 1) {
					setDefunct(true);
					String grpid = getID();
					Log.defunctGrps.add(grpid);
					Log.inactvemerGrps += 1;
					// if group is defunct, remaining member is no longer in a group
					a.setinGroup(false);
				}
				
				// update emergent network of every member, but the old member
				// clear the emergent network for the old member
				if (a != oldMem) {
					a.getEmergnet().remove(oldMem.getID());
					oldMem.getEmergnet().clear();
				}
			}
		}
		
	}
	
	
	/**
	 * Update group members' locations
	 */
	void updateLocations() {
		for (Indv mem: idsToMem.values()) {
			mem.currentCoord = currentCoord;
			mem.updatePosition(currentCoord);
		}
	}
	
	
	/**
	 * Update carpool members' locations
	 */
	void updateCarpoolLocations() {
		Coordinate coord = currentCoord;
		for (String m: currentCarpool) {
			Indv member = idsToMem.get(m);
			member.currentCoord = coord;
			member.updatePosition(coord);
		}

	}
	
	
	
	/**
	 * Update group members' goal to group goal
	 */
	void updateMemGoals() {
		for (Indv mem: idsToMem.values()) {
			mem.setGoal(getGoal());
		}
	}
	
	
	/**
	 * Update group health based on health status of agents in the group
	 * also updates movement speeds of the group
	 */
	void updateHealth() {	
		if (this.getSize() != 0) {
			int grpHealths = 0;
			for (Indv mem: idsToMem.values()) {
				int health = mem.getHealthStatus();
				// if group agent is not dead, add their health to the group average, otherwise remove the dead member
				if (health != 10) {
					grpHealths += mem.getHealthStatus();
				}
			}
			int avgHealth = grpHealths / (this.idsToMem.size());
			
			this.setHealthStatus(avgHealth);
			// update movement rate based on health
			if (avgHealth >= 0 && avgHealth < 1) {
				setMoveRateKmPerStep(.01); // healthy
			}
			if (avgHealth >= 1 && avgHealth < 4) {
				setMoveRateKmPerStep(.005); // less healthy
			}
			else if (avgHealth >= 4 && avgHealth < 6) {
				setMoveRateKmPerStep(.001); // least healthy
			}
			else {
				setMoveRateKmPerStep(0); // non-ambulatory
			}
			
		}
		else {
			// Group>updateHealth>group only has one member
		}

	}
	
	
	/**
	 * Create a groupID string using a given number
	 * @param grpNum
	 * @return
	 */
	static String createID(Integer grpNum) {
		String identifier = "g";
		String num = Integer.toString(grpNum);
		String ID = identifier.concat(num);
		return ID;
	}
	
}
