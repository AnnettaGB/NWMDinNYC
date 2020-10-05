/**
 * Disaster ABM in MASON
 * @author Annetta Burger
 * Aug2020
 */

package disaster;

// Class imports
import java.io.IOException;
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
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

//Agent class for Individual agents
/**
*  Individual subclass of Agent builds out the individual characteristics of the agent 
*  Inherits the movement characteristics of Agent
*/
public class Indv extends Agent{

	//====================================
	//
	//      ATTRIBUTES
	//
	// Unique to individual agents
	//
	//====================================
	
	// Demographics
	private int age; 
	private String sex;

	// Health
    public boolean dead = false;
    //public double doubleHealth = 0.0;
    public double dose = 0.0;			// how much radiation the agent has received (units: Sv, 1=100REM)
	
	//private int hhtype;
    /**
     *  Household type:
		0      husband & wife (no children<18)
 	    1      husband & wife (with child/children<18)
    		2      male (no<18)
    		3      male (ch<18)
    		4      female (no<18)
    		5      female (ch<18)
    		6      nonfamily group
    		7      lone male <65
    		8      lone male >65
    		9      lone female<65
    		10     lone female>65
     */
	
    // Grouping -- agent's can only be in one group at a time
    private boolean inGroup = false;
    private boolean isLeader = false;
    private String indvGrpID = "";
    private String carGrpID = "";
    
	// Ego social networks
    public HashMap <String, Indv> idsToHouseMembers = new HashMap <String, Indv> ();  // current household
	private ArrayList <String> hholdnet = new ArrayList <String> (); // all household members
	private ArrayList <String> emergnet = new ArrayList <String> (); // emergent/emergency network

	// GIS, road network, and Census Data
	private String tract = "";
	private String county = "";
	// to be passed with super constructor
	private String homeID = "";  // in lieu of startID
	private String workID = "";  // in lieu of endID
	private String hmRdID = "";  // in lieu of startRdID
	private String wrkRdID = ""; // in lieu of endRdID
	private Node workNode;  // in lieu of startNode
	private Node homeNode;  // in lieu of endNode

	// Schedule data and commuting and movement statistics
	boolean StayAtHome; // agent is a stay-at-home agent
	boolean ToWork;  // agent going to (needs to be at) work
	boolean atWork;  // agent is at work
	boolean atHome;  // agent is at home
	boolean onCommute;// agent is commuting
	boolean fleeing;  // agent is fleeing
	boolean isHomeless;  // agent no longer has a home
	private double commutedist;
	private int tcommuteStart;
	private int tcommuteEnd;
	private int tcommuteTime;
	
	ArrayList<GeomPlanarGraphDirectedEdge> commutePath = // path agent travels
			new ArrayList<GeomPlanarGraphDirectedEdge>();
	
	
	// Emergency Response Attributes
	private boolean firstResp = false;		// flag for ID as first responder 
	private double distFromGroundZero = 9999;				// distance agent is from ground zero
	
	// Emergency Response Status
	int vStatus = 0;		// victim status: 0 - AOK, 1 - being aided,  2 - was aided
	int vStart = 0;			// step of start of aid
	int vStop = 0;			// step aid stopped
	int frStatus = 0;		// first responder status: 0 - not first responder, 1 - available to aid, 2 - aiding
	

	
	//==================================
    /** 	
     * 	Individual Agent Getters and Setters
     * 	Agent characteristics come from the demographics data files
     * 	Agent schedule comes from information derived from the data files
     */
	//===================================
	
	public void setTract(String x)		{ this.tract = x; }
	public void setCounty(String x)		{ this.county = x; }
	public void setIsFirstResp(boolean x)	{ this.firstResp = x; }		// first responder flag
    public void setAge(int x)			{ this.age = x; }
    public void setSex(String x)			{ this.sex = x; }
    public void setStayAtHome(boolean x) { this.StayAtHome = x; }
    public void setHomeID(String x)		{ this.homeID = x; }
    public void setWorkID(String x)		{ this.workID = x; }
    public void setHmRdID(String x)		{ this.hmRdID = x; }
    public void setWrkRdID(String x)	{ this.wrkRdID = x; }
    public void setHomeNode(Node x)		{ this.homeNode = x; }
    public void setWorkNode(Node x)		{ this.workNode = x; } 
    public void setToWork(boolean x)	{ this.ToWork = x; }
    public void setatWork(boolean x)	{ this.atWork = x; }
    public void setatHome(boolean x)	{ this.atHome = x; }
    public void setonCommute(boolean x)	{ this.onCommute = x; }
    public void setisHomeless(boolean x) { this.isHomeless = x; }
    public void set_tcommuteStart(int x)	{ this.tcommuteStart = x; }
    public void set_tcommuteEnd(int x)	{ this.tcommuteEnd = x; }
    public void set_tcommuteTime(int x)	{ this.tcommuteTime = x; }
    public void setcommuteDist(double x)	{ this.commutedist = x; }
    public void sethholdnet(ArrayList<String> x)  { this.hholdnet = x; }
    public void setemergnet(ArrayList<String> x)	{this.emergnet = x; }
    public void setinGroup(boolean x)	{ this.inGroup = x; }
    public void setisLeader(boolean x)	{ this.isLeader = x; }
    public void setindvGrpID(String x)	{ this.indvGrpID = x; }
    public void setcarGrpID(String x)	{ this.carGrpID = x; }

    public String getTract()  			{ return this.tract; }
	public String getCounty()			{ return this.county; }
	public boolean getIsFirstResp()		{ return this.firstResp; }		// first responder flag
    public int getAge()    				{ return this.age; }
    public String getSex()    			{ return this.sex; }
    public boolean getStayAtHome()		{ return this.StayAtHome; }
    public String getHomeID() 			{ return this.homeID; }
    public String getWorkID() 			{ return this.workID; }
    public String getHmRdID()			{ return this.hmRdID; }
    public String getWrkRdID()			{ return this.wrkRdID; } 
    public Node getHomeNode()			{ return this.homeNode; }
    public Node getWorkNode()			{ return this.workNode; }
    public boolean getToWork()			{ return this.ToWork; }
    public boolean getatWork()			{ return this.atWork; }  
    public boolean getatHome()			{ return this.atHome; }
    public boolean getonCommute()		{ return this.onCommute; }
    public boolean getisHomeless()		{ return this.isHomeless; }
    public int get_tcommuteStart()		{ return this.tcommuteStart; }
    public int get_tcommuteEnd()		{ return this.tcommuteEnd; }
    public int get_tcommuteTime()		{ return this.tcommuteTime; }
    public double getcommuteDist()		{ return this.commutedist; }
    public ArrayList<String> getHholdnet()  { return this.hholdnet; }
    public ArrayList<String> getEmergnet()  { return this.emergnet; } 
    public boolean getinGroup()			{ return this.inGroup; }
    public boolean getisLeader()		{ return this.isLeader; }
    public String getindvGrpID()		{ return this.indvGrpID; }
    public String getcarGrpID()			{ return this.carGrpID; }
	
    
    
	//====================================
    /**
     * Indv -- individual agent constructor
     * @param world
     * @param census
     * @param county
     * @param ID
     * @param home
     * @param work
     * @param homeroad
     * @param workroad
     * @param housenet
     */
	//====================================
	public Indv(World world, long seed, String census, String county, String ID, String age, String sex, String home, String work, String homeroad, String workroad, ArrayList<String> housenet) {
		super(world, seed); // super constructor sets superclass initialization data
		
		state = world;
		
		setID(ID);
		setTract(census);
		setCounty(county);
		setAge(Integer.parseInt(age));
		setSex(sex);
		setHomeID(home);
		setWorkID(work);
		setisHomeless(false);
		sethholdnet(housenet);
		setHealthStatus(1); // healthy
		setHmRdID(homeroad);
		setWrkRdID(workroad);
		setStartID(homeroad);
		setEndID(workroad);
		setatWork(false); // simulation starts with individual agents at home
		setatHome(true);  // simulation starts with individual agents at home
		setonCommute(false);  // simulation starts with individual agents at home
		
		// Count all the stay-at-home agents in the population
		if ( getHomeID() == getWorkID() ) {
			setStayAtHome(true);
			Log.stayAtHome ++; // tracks all the Stay-At-Home Agents
		}
		else { setStayAtHome(false); }
		Log.atHomeCount += 1; // all agents start at home
		
		GeomPlanarGraphEdge startingEdge = world.idsToEdges.get(hmRdID);
		GeomPlanarGraphEdge goalEdge = world.idsToEdges.get(wrkRdID);
		
		if (startingEdge == null) {
			Log.badhomeNode += 1;
			pathset = false;
		}
		else {	
			// set up information about where agent node is and where it's going
			// agent's home and work locations are at one end Node of the RdID edge
			setHomeNode(startingEdge.getDirEdge(0).getFromNode());
			setWorkNode(goalEdge.getDirEdge(0).getToNode());

			// set the agent(parent) class attributes needed for transitionToNextEdge() when commuting
			setStartNode(getHomeNode());
			setEndNode(getWorkNode());
			
			// set the location to be displayed
			GeometryFactory fact = new GeometryFactory();
			setLocation(new MasonGeometry(fact.createPoint(new Coordinate(10, 10))));
			Coordinate startCoord = null;
			startCoord = getHomeNode().getCoordinate();
			updatePosition(startCoord);
			currentCoord = startCoord;
	
			setinGroup(false);
			
			setGoal("commute");		
			setGoalNode(this.workNode); // WorkNode is created at initialization -- see EndNode in Agent superclass
			
			
			/** To check the households    		
			for (String x: getHholdnet()) {
	    		System.out.print(x + " ");
			}
			
			System.out.println(); 
			Scanner reader = new Scanner(System.in);  // Reading from System.in
			System.out.println("Indvt>Enter a number: ");
			int n = reader.nextInt(); // Scans the next token of the input as an int.
		*/
			
			this.getGeometry().setUserData(this);  // make object & health status attribute accessible for grouping and Portrayals (indicates color, etc.);

			pathset = setCommutepaths(world); // pathset is a boolean that agent has a path
		}

	}
	
    /** Set paths of an Agent: find an A* path to work!
    * 	@param state
    * 	@return whether or not the agent successfully found a path to work
    */
   public boolean setCommutepaths(World world)
   { 
	   // Set schedule and paths
	   // Create commute path and schedule
	   this.lastCoord1 = this.currentCoord;
	   this.lastCoord2 = this.currentCoord;
	   if (getHomeNode() != getWorkNode()) {
	       commutePath = findNewAStarPath(getHomeNode(), getWorkNode());
	       
	       if ( commutePath == null )  {
	    	   		return false;
	       }
	       else { 
	    	   		setToWork(false); // not time to go to work yet
	    	   		setatWork(false); // not at work
	    	   		set_tcommuteStart(730);  // @step 450 the first day/ 1890/3330/etc.
	    	   		set_tcommuteEnd(1830);  // @step 1110 the first day/ 2550/3990/etc.
	       
	    	   		currentPath = commutePath;	    	   
	    	   		beginPath(currentPath);
	    	       	setcommuteDist(getpathDistance(currentPath));
	    	       	
	    	   	   return true;
	       }
	       
	   }
	   else { 
		   if (getHomeNode() == getWorkNode()) {
//		   	   System.out.println("Indv>setCommutepaths>Initialized agent / commute start @time " + get_tcommuteStart() +
//		   	   		" no commute; agent at home: " + getLocation());
		   }
		   else {
			   Log.badagent += 1;
			   //System.out.println("Indv>setCommutepaths>No commute path and HomeNode doesn't match WorkNode; badagent " + getID());
		   }
		   return false;
	   }

   }
   
   
   /** Called every tick by the scheduler */
   /** moves the agent along the path 
    * @param world
    */
   @Override
public void step(SimState world){
	   distMoved = 0;
	   
	   state = (World) world;
	   
	   // Individuals continue routine, if they are alive and well 
	   if ( !this.dead ) {  // if agents are alive and not blocked  // && (this.getHealthStatus() != 72)
		   
		   state = (World) world; // update perception of the world at each step
		   long currentStep = state.schedule.getSteps();
	   
		   Coordinate beginCoord = currentCoord;
		   
		   // Health status is used as the first level trigger for behavior change post detonation
		   if ((this.getHealthStatus() <= 1) || (this.getHealthStatus() == 80) || (this.getHealthStatus() == 89)) {  // 80 & 89 are temporary testing parameters for routing around the impact area
			   routine();
		   
			   // calculate the average commute distance and time at end of the first day
			   if (Log.indvList.size() <= Log.atWorkCount) {
				   double sum_commuteDist = 0;
				   double sum_commuteTime = 0;
				   int numAgents = Log.indvList.size() - 1;
				   for (Indv a: Log.indvList) {
					   sum_commuteDist += a.getcommuteDist();
					   sum_commuteTime += a.get_tcommuteTime();
				   }
				   Log.avg_dCommute = sum_commuteDist / numAgents;
				   Log.avg_tCommute = sum_commuteTime / numAgents;
			   }
		   }

		   // Individuals are impacted by event, and move into nonroutine behavior
		   else if (this.getHealthStatus() > 1) {
			   nonroutine(currentStep, state);
		   }
		
	   }  // closes out !this.dead (if agent is dead)
	   
	   distMoved = -1;
	   
   }

   
   
	//======================================
	//
	//     ROUTINE
	//
	//======================================
   
   
   /**
    * Agents conduct their daily routines
    * @param world
    * sets routine goals
    */
   void routine() {
	   long currentStep = this.state.schedule.getSteps();
	   int time = Spacetime.time24(currentStep);
	   
	   // Agents commute if they do not stay at home or are not internally displaced
	   if (!getStayAtHome() && !getisHomeless()) {
	   
		   // Routine schedule and time check; depends on work
		   // check the time to see if the agent needs to start commuting to or from work
		   if (time == get_tcommuteStart()) {  // commute to work
			   setToWork(true);
			   setatHome(false);
			   setonCommute(true);
		   
			   if (!this.inGroup) { // only count this if the agent is not in a group
				   Log.atHomeCount -= 1;
				   Log.onCommuteCount += 1;
			   }
		   }	   
		   if (time == get_tcommuteEnd()) {  // commute from work
			   setToWork(false);
			   setonCommute(true);
		   
			   if (!this.inGroup) { // only count this if the agent is not in a group
				   Log.atWorkCount -= 1;
				   Log.onCommuteCount += 1;
			   }
		   }
	   
		   if (!this.inGroup) {  // boolean to check whether that individual travels as part of a group, i.e. carpools
			   // individuals in group commute with their group
			   // if it is time to commute, but agent has not arrived at work, travel commute path
			   if ( getToWork() && !getatWork() ) {
				   		
				   if ( getneedReroute() || gethaveDetour() || getonDetour() ) {
					   // confirm that the commute path is still good, i.e. don't need a detour
					   if ( getneedReroute() && !gethaveDetour() ) {  
						   reroute(state);
					   }
					   if ( gethaveDetour() && !getonDetour() ) {
						   goToDetour();
					   }
					   if ( getonDetour() ) {
						   commutedetour();
					   }
				   }

				   else {
					   commute();	
				   }
			   
			   }
		   
			   // if it is time to travel home, but agent has not arrived at home, travel commute path to its start
			   else if ( !getToWork() && getatWork() ) {
				   
				   if ( getneedReroute() || gethaveDetour() || getonDetour() ) {
					   // confirm that the commute path is still good, i.e. don't need a detour
					   if ( getneedReroute() && !gethaveDetour() ) {  
						   reroute(state);
					   }
					   if ( gethaveDetour() && !getonDetour() ) {
						   goToDetour();
					   }
					   if ( getonDetour() ) {
						   commutedetour();
					   }
				   }

				   else {
					   commute();	
				   }
			   }
		   
		   //checkRoute(); // uncomment to check if stuck in place, if so reroute
		   } 	   
	   }	   	   
   }

   
   /** 	Move agent on its commute used in step before event
    *   Equivalent to travelPath in superclass, but specialized for commute
    * 	@param world
    */
   void commute()	{
	   // get traffic data and check that edges are passable
       long currentStep = this.state.schedule.getSteps();
	   double edgeTrafficsize = this.state.edgeTraffic.get(currentEdge).size();
	   double edgeDistance = this.state.edgesToDistance.get(currentEdge);
	   double speedLimit = this.state.edgesToSpeedLimit.get(currentEdge);
	   double moveRate = speedLimit / 60;  // (n kilometers / 60 steps) to get km/step
	   
	   // if agent gets stuck because edge is impassable at time of impact
	   // assumes agent only gets stuck with moveRate = 0 one time when the roadnetwork is first destroyed
	   if (moveRate == 0) {
		   setHealthStatus(80); // changes color for tracking those agents stuck due to damaged edges
			   setneedReroute(true);
		   return; // break out of commute() if edge is impassable
	   }
	   
       // check that we've been placed on an Edge      
       if (getSegment() == null) {
           return;
       } // check that we haven't already reached our destination -- if yes, flip path and end step
       else if (reachedDestination) {
    	   	   
    	   	   if ( !getatWork() && getToWork() ) {  // wasn't at work and it's not time to leave
    	   		   int currentTime = Spacetime.time24(currentStep);
    	   		   int commuteTime = currentTime -  get_tcommuteStart();
    	   		   set_tcommuteTime(commuteTime);
	
         	      setatWork(true);  // now at work
         	      setonCommute(false);  // no longer commuting
         	      Log.atWorkCount += 1;
         	      Log.onCommuteCount -= 1;
    	   		   
      	   	  	  flipPath();
    	   	   }
    	   	   else if ( getatWork() && !getToWork() ) {  // not at work and not needing to be at work 
     	   		   setatWork(false);
     	   		   setonCommute(false);  // no longer commuting
     	   		   Log.atHomeCount += 1;
     	   		   Log.onCommuteCount -= 1;

    	   		   flipPath();
    	   	   }
    	   	   
    	   	   return;
       }

       // make sure that we're heading in the right direction -- continue path
//       boolean toWork = ((World) world).goToWork;
       if ((getToWork() && pathDirection < 0) || (!getToWork() && pathDirection > 0)) {
           flipPath();
       }

       speed = progress(edgeTrafficsize, edgeDistance, moveRate);
       currentIndex += speed;

       // Coordinate currentPos;
       // check to see if the progress has taken the current index beyond its goal
       // given the direction of movement. If so, proceed to the next edge
       if (linkDirection == 1 && currentIndex > endIndex) {
           currentCoord = getSegment().extractPoint(endIndex);
           transitionToNextEdge(edgeTrafficsize, edgeDistance, currentIndex - endIndex);

       } else if (linkDirection == -1 && currentIndex < startIndex) {
           currentCoord = getSegment().extractPoint(startIndex);
           transitionToNextEdge(edgeTrafficsize, edgeDistance, startIndex - currentIndex);
       } else { 
    	   // just update the position!
           currentCoord = getSegment().extractPoint(currentIndex);
       }
       updatePosition(currentCoord);
       
   }
    

  /** 
   * Flip the agent's path around for commuting purposes 
   */
  void flipPath() {
      reachedDestination = false;
      pathDirection = -pathDirection;
      linkDirection = -linkDirection;
  }

  
  //======================================
  // Rerouting methods for post-event
  //======================================
 
  /**
   * Reroute method to create detour path
   * @param world
   */
  public void reroute(World world) {
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
	   if ( getHomeNode() == null || getHomeNode().getCoordinate() == null) {
		   Log.IDPhome += 1;  // track internally displace people with no homes
		   setisHomeless(true);
		   
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
		   setHomeNode(tempnode);
	   }
	   
	   // checks for null worknodes
	   if ( getWorkNode() == null || getWorkNode().getCoordinate() == null ) {
		   Log.IDPwork += 1;  // track internally displace people with no workplaces
		   setWorkNode(getHomeNode());
		   setEndNode(getHomeNode());
	   }
	   
	   // check if there is an available currentPath based on the remaining edges or new Node destinations
	   // if agent is headed to work
	   if ( getToWork() && !getatWork() ) {
		   currentPath = findNewAStarPath(getStartNode(), getWorkNode());
		   if (currentPath != null) {
			   setneedReroute(false);
			   sethaveDetour(true);
		   }
		   // if there is no available path to work -- go home
		   else {
			   Log.IDPwork += 1;  // track internally displace people with no workplaces or way to workplaces
			   setWorkNode(getHomeNode());
			   setEndNode(getHomeNode());

			   currentPath = findNewAStarPath(getStartNode(), getHomeNode());
			   // if there is a path home -- route agent home
			   if (currentPath != null) {
				   setneedReroute(false);
				   sethaveDetour(true);
			   }
			   
			   // if there is also no path home -- find temp location
			   else {
				   int count = 0;
				   double awayfromgroundzero = .005; // .01 in degrees
				   // while no path is found to a random temporary node, reset the random startnode and try again
				   searchDist = .02;
				   Node tempnode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
				   while (tempnode == null) {
					   // look further away from ground zero for a startNode
					   setStartNode(findsafeNearestNode(this.getGeometry(), world.roadIntersections, world.roadNetwork, world.waterField, searchDist));
					   tempnode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
					   count += 1;
					   //System.out.println("Indv>reroute>try to find new templocation count: " + count);
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

				   setHomeNode(tempnode);
				   setWorkNode(getHomeNode());
				   setEndNode(getHomeNode());
				   currentPath = findNewAStarPath(getStartNode(), tempnode);
				   
				   setneedReroute(false);
				   sethaveDetour(true);
			   }
		   }
	   }
	   // if the agent is headed home
	   else if ( !getToWork() && getatWork() ) {
		   currentPath = findNewAStarPath(getStartNode(), getHomeNode());
		   if (currentPath != null) {
			   setneedReroute(false);
			   sethaveDetour(true);
		   }
		   else {
			   Log.IDPhome += 1;  // track internally displace people with no homes
			   setisHomeless(true);
			   int count = 0;
			   double awayfromgroundzero = .005; // .01 in degrees
			   Node tempnode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
			   searchDist = .02;
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

			   setHomeNode(tempnode);
			   setWorkNode(getHomeNode());
			   setEndNode(getHomeNode());

			   setneedReroute(false);
			   sethaveDetour(true);
		   }
	   }
	   else { 
		   setneedReroute(false);
		   sethaveDetour(false);
	   }   
 }
  
  
  public void goToDetour() {
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
	    
  }
  
  
  public boolean commutedetour() {
	   if (getHealthStatus() != 89) {
		   setHealthStatus(89); // changes color to track rerouting
	   }
	  // If agent travels detour path
	  // if at start coordinate, begin the path
	   if (getStartNode().getCoordinate().equals2D(currentCoord)) {
		   reachedDestination = false; // agent is not at the new destination
		   pathDirection = 1; // head the agent in the +1 path direction
		   beginPath(currentPath);
	   }
	   // if at the end of the path, reset the commute path & update locations
	   else if (getEndNode().getCoordinate().equals2D(currentCoord)) {
		   setneedReroute(false);  // no longer needs to be rerouted, agent has arrived at destination
		   setonDetour(false);  // agent has ended its detour
		   sethaveDetour(false);  // cancel detour route...return to normal
		   
		   // reset the agent's commute path
		   if (getEndNode() == getWorkNode()) {
			   setatWork(true);
			   setonCommute(false);
			   Log.atWorkCount += 1;
			   Log.onCommuteCount -= 1;
		   }
		   else if (getEndNode() == getHomeNode()) {
			   setatHome(true);
			   setonCommute(false);
			   Log.atHomeCount += 1;
			   Log.onCommuteCount -= 1;
		   }
		   
		   // create new commutepath if the agent does not stay at home now or is an IDP
		   // assumes work schedule remains the same
		   if (getisHomeless() && (getEndNode() == getHomeNode())) {
			   // need to decrement the atHomeCount if the agent is homeless
			   // some HomeNodes are temporary locations
			   Log.atHomeCount -= 1;
		   }
		   else if ( getHomeNode() == getWorkNode() ) {
			   // otherwise if the home location is the same as the work locations -- agent is now a stay-at-home
			   Log.stayAtHome += 1;
			   setStayAtHome(true);
		   }
		   else {
			   // otherwise we can just reset the commute path based on the new routing
			   // if the workNode or homeNode is null set to homeNode
			   commutePath = findNewAStarPath(getHomeNode(), getWorkNode());
			   
			   // check that path is good
			   if (commutePath != null) {
				   currentPath = commutePath;
				   setcommuteDist(getpathDistance(commutePath));
			   }
			   else {
				   // if the path is not good set agent to stay-at-home
				   setWorkNode(getHomeNode());
				   setEndNode(getHomeNode());
				   Log.stayAtHome += 1;
				   setStayAtHome(true);
			   }
			   
		   }
		   
		   // set agent at the right end of the path
		   // if at work need to begin at the end of the commute path
		   if (getatWork()) {
		       if (currentPath != null && currentPath.size() > 0)
		       { 
		    	   // set the agent on the end of the path
		    	   indexOnPath = currentPath.size()-1;  
			       GeomPlanarGraphEdge edge =
			            (GeomPlanarGraphEdge) currentPath.get(indexOnPath).getEdge();
			        setupEdge(edge);
			        updatePosition(segment.extractPoint(currentIndex));
		       }
		       // make sure flip path settings are correct
		       reachedDestination = false;
		       pathDirection = -pathDirection;
		       linkDirection = -linkDirection;
//			   beginPath(currentPath);
		   }
		   else {
			   reachedDestination = false; // agent is not at the new destination
			   pathDirection = 1; // head the agent in the +1 path direction
			   beginPath(currentPath);
			   return true;
		   }
	   
		   return false;
	   }
	   
	   travelPath();

	   return true;
	}
  
  
  
	//======================================
	//
	//     NON-ROUTINE
	//
	//======================================
	
  
  /**
   * Agents respond to non-routine events
   * Initially simple grouping behavior to move away from the event
   * @param world
   * sets routine goals
   */
  // Need to fix location status in nonroutine; only have good shelter and IDP home monitoring
  void nonroutine(long currentStep, World world) {	  	  
	  // In the nonroutine method agent is in high arousal or anxiety state
	  // Add function for shock?
	  
	  if (!this.getIsFirstResp()) {		  
		  // Update goals
		  // if agent is hurt, but not in zone 2 (i.e. Zone 3 agents), check whether it is already at a sheltering place (home or work)
		  // agents in zone 2 have no shelter, they flee
		  if (!Effects.inExZone(this)) {
			  // if agent is at home and the homeNode or workNode is not damaged, reset goal to shelter
			  // health is not further degraded due to damaged building/Node
			  // individual decides whether to stay in place
			  // decision can use Random an int value from chanceShelteratHome or chanceShelteratWorm
			  // this needs a design -- until find a basis for this probability
			  if (getatHome() && ( (getHomeNode() != null) && (getHomeNode().getCoordinate() != null) ) ) {
				  int random = new Random().nextInt(Parameters.chanceShelteratHome);  // 100% chance to stay at home
				  // if in a group, whole group gets same goalnode
				  // if at homeNode
				  if (getHomeNode().getCoordinate() == this.currentCoord) {
					  if ( getinGroup() ) {
						  Group g = getGrp(getindvGrpID(), world);
						  g.setGoalNode(homeNode);
					  }

					  // random chance to shelter; 0 means 100% chance of shelter
					  if (random == 0) {
						  setGoal("shelter");
						  // agent leaves emergent group once in a shelter
						  Group grp = this.getGrp(getindvGrpID(), world);
						  if (this.getinGroup() && grp.getGrptype() == "emergent") {
							  leaveGroup(grp);
						  }
						  Log.agentsSheltering += 1;
						  shelter();
					  }
				  }
			  }
			  
			  else if (getatWork() && ( (getWorkNode() != null) && (getWorkNode().getCoordinate() != null) ) ) {
				  int random = new Random().nextInt(Parameters.chanceShelteratWork);  // 50% chance to stay at work
				  // set goalNode to this location
				  // if at workNode
				  if (getWorkNode().getCoordinate() == this.currentCoord) {
					  if ( getinGroup() ) {
						  Group g = getGrp(getindvGrpID(), world);
						  g.setGoalNode(workNode);
					  }
					  if (random == 0) {
						  setGoal("shelter");
						  // if emergent group, agent leaves emergent group once in a shelter
						  Group grp = this.getGrp(getindvGrpID(), world);
						  if (this.getinGroup() && grp.getGrptype() == "emergent") {
							  leaveGroup(grp);
						  }

						  Log.agentsSheltering += 1;
						  shelter();
					  }
				  }
			  }
		  } // end Zone2 agent update goals
		  
		  // checks for grouping behavior -- instinctual response that occurs in all zones
		  //if ( Parameters.Grouping ) {  // Simulation run allows grouping behavior
		  if ( Parameters.Emergent ) {  // Simulation run allows grouping behavior
			  if (!inGroup) { // if you're not in a group, i.e. carpool
				  // can eventually add whether it has the goal of joining a group
				  
				  // try to join a group -- agents try to join group every step of routine
				  boolean joinedGroup = joinGroup();		  
				  
				  if ( !joinedGroup ) {
					  
					  // if individuals flee 
					  if ( getGoal() == "flee") {
						  // if agent is wounded, but ambulatory and is in a building (work or home)
						  if ( getHealthStatus() !=9 && (getatWork() || getatHome()) ) { // agents with health of 9, are fatally hurt and can't move
							  long delayStep = Parameters.tDetonation + 3*getHealthStatus();
							  if (currentStep > (delayStep)) {  // delay to escape building varied by health (6-24 minutes)
								  getAway(currentStep, world);
							  }
						  }
						  // else if agent is just wounded, but ambulatory
						  else if (getHealthStatus() !=9) {
							  getAway(currentStep, world);
						  }
						  
					  } // end of fleeing individuals
					  
					  else if (getGoal() == "findshelter") {
						  findShelter();
					  }  // end of shelter individuals check -- can be built out for other behavior
					  
					  else if (getGoal() == "shelter") {
						  shelter();
					  }
					  
				  }  // end of if !joinedGroup boolean			  
			  }  // end of not in a group 
			  
			  else {  // individual in a group
				  // individual moves with the group
			  }
			  
		  }  // end of Grouping parameter true
		  
		  else { // no grouping 
			  // Need to program out this basic flight scenario
			  if ( getGoal() == "flee" ) {
				  
				  if (getHealthStatus() != 9) {
					  long delayStep = Parameters.tDetonation + 3*getHealthStatus();
					  if ((currentStep > delayStep) || (getHealthStatus() == 72)) {  // delay to escape building varied by health (6-24 minutes), unless it is a blocked agent with health = 72
						  getAway(currentStep, state);
					  }
				  }
				  
			  } // end of fleeing individuals
			  
			  else if ( getGoal() == "findshelter" ){
				  findShelter();
			  }  // end of shelter individuals -- can be built out for other behavior
			  
			  else if (getGoal() == "shelter") {
				  shelter();
			  }
			  
		  } // end of non-grouping scenario
		  
	  } // end of !firstResponder Boolean
	  
	  else {
		  moveTowards(currentStep, world);
	  }
	  
  }
  
  
  
	//======================================
	//
	//     EMERGENCY BEHAVIOR
	//
	//======================================
  
  void shelter() {
	  // agents stay at safe home, work or tempnode outside 3rd ring
	  // i.e. if there is a first responder, provide aid, get help, or get information
	  //System.out.println("Indv>nonroutine>at shelter " + getID() + " goal: " + getGoal());
  }
  
  
  void findShelter() {
	  // used in getaway() method to step through finding routing and needed detours
	  // i.e. if there is a first responder, get help, or get information
	  //System.out.println("Indv>nonroutine>findshelter " + getID() + " goal: " + getGoal());
	  if (getneedReroute() || getonDetour() || gethaveDetour()) {
		  // confirm that the commute path is still good, i.e. don't need a detour
		  if ( getneedReroute() && !gethaveDetour() ) {  
			  //System.out.println("Indv>step>nonroutine>findshelter> " + getID() + " needs route home");
			  routeHome();
		  }
		  if ( gethaveDetour() && !getonDetour() ) {
			  //System.out.println("Indv>step>nonroutine>findshelter> " + getID() + " move towards detour start");
			  goToDetour();
		  }
		  if ( getonDetour() ) {
			  //System.out.println("Indv>step>nonroutine>findshelter> " + getID() + " detours");
			  detour();
		  }
	  }
  }
  
  
  void getAway(long currentStep, World world) {
	  // method for agent to flee from the area
	  //System.out.println("Indv>step>nonroutine>getAway>" + getID() + " health: " + getHealthStatus());

	   if ( (getGoalPoint() == null)  || (this.currentCoord == null) )
	   {
////		   System.out.println("Indv>getAway> attempted reacting with no goal or current location... " + fleeing + " " + firstResp + " " + getGoalPoint());
		   return;
	   }
	   
	   int time = Spacetime.time24(currentStep);

//		   System.out.println("Agent>getAway>on foot: health=" + getHealthStatus());
	   		   
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
	   
	   if (distFromGroundZero > Parameters.maxDistGZ) 
	   { 
		   Parameters.maxDistGZ = distFromGroundZero;	// update max
////		   System.out.println("Indv>getAway> updated max distance to " + distFromGroundZero);
	   }
	
	   //	   	System.out.println("Agent>react>fleeing>calculated step: cLat=" + cLat + " gLat= " + gLat + " dist to go= " + dz + " limit= " +this.fleeingRate);
		   
	   // during first hours
	   /*
	    *   1. Affected agents given fleeing goals
		*   2. First responders start toward ground zero when they learn of it
		*   3. Unaffected agents start commute home when they learn + delay (fnc of dist)
		*/
		   
		//==========================================
		// AFFECTED agents on foot have fleeing goal (goalPoint (type Coordinate))
		if (getGoalPoint() != null)  // given fleeing goal by Effects calculation
		{
			// Can reach in 1 step, i.e., this step?
			// limit step size based on speed
			// if dist to goal > allowed per step: reduce to speed limit
			if (dgoal > speedrate)	// if can't reach goal in 1 step (fleeing rate is dec.degrees) per min
			{
				// find the lat/long distance to travel for this step
				double convert = speedrate/dgoal;
			   	speedLat = speedLat * convert;
			   	speedLong = speedLong * convert;
	//			 System.out.println("Agent>react>fleeing>limiting speed= " + dz + " limit= " +this.fleeingRate);
			}
			// calculate new x/y (lat/long) for agent's potential position update
			double newLat = cLat + speedLat;
			double newLong = cLong + speedLong;
////			System.out.println("Indv>getAway>" + getID() + " fleeing>final step: speedLat= " + speedLat + " speedLong= " + speedLong + " newLat= " + newLat + " newLong= " + newLong);
			   	   
			// if already reached fleeing goal ...
			if (dgoal < 0.001) 
			{
				//System.out.println("Agent>step>getAway>reached fleeing goal, z3  radius " + Parameters.z3radius);
				// goal may still be in blast area, if still inside zone3, create new fleeing goal
				if (dist < Parameters.z3radius + 0.01)
				{
					// create new fleeing goal
					//System.out.println("Agent>step>getAway>reached fleeing goal inside Z3, creating new fleeing goal at dist " + dist );
					//setHealthStatus(-1); // reached way point (pink-getting aid) to be visible					
					Effects.createFleeingGoalCord(this, world);						
					
					// test new fleeing goal point to see whether it is in the water
					Coordinate savedCoord = currentCoord;
					updatePosition(getGoalPoint());
					Bag waterobjs = world.waterField.getCoveringObjects(getGeometry());
					if (!waterobjs.isEmpty()) {
						Coordinate newCoord = setAltGoalPt(world.waterField, getGoalPoint(), speedrate);
						setGoalPoint(newCoord);
					}
					updatePosition(savedCoord);
					
//					Coordinate fleeingCoord = findAltCoord(world.waterField, currentCoord, speedrate*3);
//					setGoalPoint(fleeingCoord);
				}
				else	// transfer to road system and look for shelter
				{
					setneedReroute(true);
					setGoal("findshelter");
				}
			} // end reached goal
			else // haven't reached interim fleeing goal, check to see whether made outside blast area or take step toward goal
			{
				//System.out.println("Indv>step>nonroutine>getAway>not at interim goal " + getGoalPoint() + " z3 " + Parameters.z3radius);
				if (dist > Parameters.z3radius + 0.01)	// check whether agent is outside the impact area
				{
					//System.out.println("Indv->step->nonroutine->getAway->outside z3");
					// transfer to road system and look for shelter
					setneedReroute(true);
					setGoal("findshelter");
				}
				else // evaluate movement on foot
				{
					// try out the new coordinate position
					Coordinate fleeingCoord = new Coordinate(newLong, newLat);
					//System.out.println("Indv->step->nonroutine->getAway->inside z3 to " + fleeingCoord + " " + dist);
					// if the new coordinate is not past the goal coodinate
					// move step at the movement rate otherwise move all the way to the goal
					if (Math.abs(speedLat) < Math.abs(dLat) || Math.abs(speedLong) < Math.abs(dLong))  // take a step toward goal  
					{
						//System.out.println("Indv->step->nonroutine->getAway->move doesn't take agent to destination " + fleeingCoord);
//						if (getHealthStatus() == 72)	// agent has already hit the water
//						{
//							System.out.println("Agent->react-> agent moving along water " + getID());
//							fleeingCoord = findAltCoord(world.waterField, fleeingCoord, speedrate);
//							newLong = fleeingCoord.x;
//							newLat = fleeingCoord.y;
//							// set goalPoint (fleeing goal) to current point
//						}
						//else // check the coordinate position move, i.e., not in water
						//{
							// get all the water objects at this position
							
							Bag polity = world.waterField.getCoveringObjects(this.getGeometry());
							//System.out.println("Agent->react-> bag= " + polity);
							// if the water polity is not empty (i.e. there is water at prospective position), stay at the current position
							if (! polity.isEmpty() || getHealthStatus() == 72) // not in the water
							{
								//System.out.println("Agent>getAway> agent almost in water " + getID());
								// reset fleeing coordinate
								//fleeingCoord = findTempLocationNode(world.roadIntersections, world.roadNetwork).getCoordinate();
								fleeingCoord = findAltCoord(world.waterField, currentCoord, speedrate);
								setGoalPoint(setAltGoalPt(world.waterField, fleeingCoord, speedrate));
								currentCoord = fleeingCoord;
								updatePosition(fleeingCoord);
//								newLong = fleeingCoord.x;
//								newLat = fleeingCoord.y;
		//						System.out.println("Agent->react-> at water's edge");
								newLong = cLong;	// don't move this step
								newLat = cLat;
//								fleeingCoord = new Coordinate(newLong, newLat);
								if (getHealthStatus() != 72)   // check already blocked
								{
									Log.agentsBlocked++;
									setHealthStatus(72);
								} // note that agent is blocked
							}
		//				// ready to move to newLat/newLong
							else {	
								// move step toward goal
								fleeingCoord = new Coordinate(newLong, newLat);
								// MasonGeometry fleeingStep = new MasonGeometry(fact.createPoint(fleeingCoord));
								currentCoord = new Coordinate(newLong,newLat);
								// currentLocation = new MasonGeometry(fact.createPoint(currentCoord)); //10,10)));
								updatePosition(fleeingCoord);
				//				System.out.println("Agent>react>moving step toward fleeing goal dz= " + dz + " fleeing rate= " + fleeingRate);
							}
						//}
					}
					else  // move all the way to the goal in one step
					{
						fleeingCoord = new Coordinate(gLong, gLat);
						updatePosition(fleeingCoord);
						//System.out.println("Agent>react>moving step to fleeing goal " + fleeingCoord);
					}
				}// eval on foot					
			}// end else hasn't reached fleeing goal
		    	
		  }// end has fleeing goal
	   
  }// end react

    
  //=======================
  // to/from road helper
  //=======================

  /**
   * Method to route agents back to their home location
   * Note: it does not reset the agent's commute paths
   * @param world
   */
  public void routeHome() {
	  //System.out.print("Indv>nonroutine>routeHome> outside R3, route home? >> ");
	  //System.out.println("Indv>nonroutine>routeHome>");
	  reachedDestination = true; // agent ends trip at the current point in order to reroute
	  // convert meter foot rate to the model's kilometers standard unit
	  double speedrate = Spacetime.degToKilometers(getMoveRateKmPerStep());
	  
	  // initial distance for search of a new node
	  double searchDist = 0.02; // start searching at for a distance in degrees for 1km
	  // do we want to update the movementRate?
	  //System.out.println("Indv>nonroutine>routeHome>moveRate: " + speedrate);
	  
	  // set the detour path startNode to the nearestNode
	  //System.out.println("Indv>nonroutine>routeHome>roadIntersections " + world.roadIntersections.getGeometries().size());
	  //System.out.println("Indv>nonroutine>routeHome>roadIntersections " + world.roadIntersections.getObjectsWithinDistance(this.getGeometry(), searchDist).size());
	  
	  // get only roadIntersections that are not across water
	  //world.roadIntersections.
	  
	  Node startNode = findsafeNearestNode(this.getGeometry(), state.roadIntersections, state.roadNetwork, state.waterField, searchDist);
	  if (startNode == null) {
		  //System.out.println("Indv>nonroutine>routeHome>no startNodes here, flee further");
		  return;
	  }
	  else setStartNode(startNode);
	  //setStartNode(findTempLocationNode(world.roadIntersections, world.roadNetwork)); // maybe rewrite this for the start node -- a little closer
	  setGoalPoint(getStartNode().getCoordinate()); // reset goal coordinate
	  //System.out.println("Indv>nonroutine>routeHome>current location: " + currentCoord + " distance " + currentCoord.distance(getStartNode().getCoordinate()));
	  //System.out.println("Indv>nonroutine>routeHome>startNode " + getStartNode() + " " + getStartNode().getCoordinate());
	  
	  // check whether home nodes were destroyed by impact -- nonroutine agents will not return to their commute, they shelter at the tempnode
	  //System.out.println("Indv>nonroutine>routeHome>if null destination Nodes set temp locations");
	  if ( getHomeNode() == null || getHomeNode().getCoordinate() == null) {
		  Log.IDPhome += 1;  // track internally displace people with no homes
		  setisHomeless(true);
		  Node tempNode = findTempLocationNode(state.roadIntersections, state.roadNetwork);
		  setHomeNode(tempNode);
		  setEndNode(tempNode);
		  setGoalPoint(getHomeNode().getCoordinate()); // reset goal coordinate
		  //System.out.println("Indv>nonroutine>routeHome>need new homeNode " + getHomeNode() + " " + getHomeNode().getCoordinate());
	  }
	  //System.out.println("Indv>nonroutine>routeHome>go to " + getStartNode().getCoordinate() + " to get to " + getHomeNode() + " " + getHomeNode().getCoordinate());
	  
	  // check if there is an available currentPath based on this HomeNode
	  // if so head to the new startNode
	  currentPath = findNewAStarPath(getStartNode(), getHomeNode());
	  if (currentPath != null) {
		  //System.out.println("Indv>nonroutine>routeHome>have detour path away from impact area");
		  //System.out.println("begin: " + currentPath.get(0).getCoordinate());
		  //System.out.println("end: " + currentPath.get(currentPath.size()-1).getCoordinate());
		  setneedReroute(false);
		  sethaveDetour(true);
	  }
	  else {
		   //System.out.println("Indv>nonroutine>routeHome>no path to homeNode");
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
			   //System.out.println("Indv>nonroutine>routeHome>try to find new start: " + count);
			   // only try to reset the start node using findsafeNearestNode() once more
			   // otherwise nudge the agent a little away from ground zero and its current position
			   if (count >= 1) {
				   double clat = this.getGeometry().geometry.getCoordinate().y;
				   double clong = this.getGeometry().geometry.getCoordinate().x;
				   double movelat = (clat + Parameters.tLat)/Math.abs(clat)*movedist;
				   double movelong = (clong + Parameters.tLong)/Math.abs(clong)*movedist;
				   
				   double newlat = clat + movelat;
				   double newlong = clong + movelong;
				   //System.out.println("Indv>reroute>move: " + movelat + " " + movelong);
			   	   Coordinate tempCoord = new Coordinate(newlong, newlat);
				   currentCoord = tempCoord;
				   updatePosition(currentCoord);
				   //System.out.println("Indv>reroute>move away at: " + count + " new coord " + currentCoord);
				   return;

			   }
		   }
		   //System.out.println("Indv>reroute>times reset the homenode and looked for a path: " + count);
		   
		   // once nodes and path are found
		   
		   setHomeNode(tempnode);  // agents shelter at this tempnode
		   setWorkNode(getHomeNode());
		   setEndNode(getHomeNode());
		   setGoalPoint(getHomeNode().getCoordinate()); // reset goal coordinate
		   //System.out.println("Indv>nonroutine>routeHome>new tempNode " + tempnode.getCoordinate() + " path found at count " + count);
		   setneedReroute(false);
		   sethaveDetour(true);
	  }
  }
  
  public boolean detour() {
	  // If agent travels detour path
	  // Confirm end path nodes	
	  //System.out.println("Indv>step>nonroutine>detour>endNode " + getEndNode().getCoordinate());
	  //System.out.println("Indv>step>nonroutine>detour>endNode " + getHomeNode().getCoordinate());
	  
//	  if (getEndNode().getCoordinate() == null) {
//		  Node tempNode = findTempLocationNode(world.roadIntersections, world.roadNetwork);
//		  setHomeNode(tempNode);
//		  setEndNode(tempNode);
//		  currentPath = findNewAStarPath(getStartNode(), getHomeNode());
//	  }
//	  
	  // if at start coordinate, begin the path
	  if (getStartNode().getCoordinate().equals2D(currentCoord)) {
		  //System.out.println("Indv>step>nonroutine>detour>" + getID() + " begin detour");
		  reachedDestination = false; // agent is not at the new destination
		  pathDirection = 1; // head the agent in the +1 path direction
		  beginPath(currentPath);
		  //return true;
	  }
	  // if at the end of the path, fleeing agent shelters???
	  
	  else if (getHomeNode().getCoordinate().equals2D(currentCoord)) {
		  //System.out.println("Indv>step>nonroutine>detour>" + getID() + " reaches destination");
		  //System.out.println("Indv>step>nonroutine>detour>" + getID() + " stays at homelocation");
		  // need an alternative non-fleeing goal
		  //setHealthStatus(1); // temporarily tracker to change color back to normal routine
		  setneedReroute(false);  // no longer needs to be rerouted, agent has arrived at destination
		  setonDetour(false);  // agent has ended its detour
		  sethaveDetour(false);  // cancel detour route...return to normal
		  // check if the end location is at the agent's original home location
		  // if so, update the monitors
		  if (getEndNode() == state.idsToEdges.get(hmRdID).getDirEdge(0).getFromNode()) {
			   setatHome(true);
			   Log.atHomeCount += 1;
			   setGoal("shelter");  // agent is no longer fleeing, is at home or a temporary location

		   }
		  else {
			   //System.out.println("Indv>step>nonroutine>detour>detour path failed!----------------------");
		   }
		  return false;
	   }
	  travelPath();
	  //System.out.println("Indv>step>nonroutine>detour>" + getID() + " at "+ currentCoord + " detour to " + getEndNode().getCoordinate() + " pathsize " + currentPath.size());
	  return true;
  }
  
  
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
		   //System.out.println("Indv>findsafeNearestNode>removed candidates: " + count);
		   count = 0; // reset removed candidate count
		   if (loopcount == 20) {
			   // agent couldn't find a candidate node, flee further out with a randomCoord move
			   setGoal("flee");
			   setneedReroute(false);
			   Coordinate newCoord = findRandCoord(blockedfield, this.currentCoord, this.getMoveRateKmPerStep());
			   this.updatePosition(newCoord);
			   return null;
		   }
		   //System.out.println("Indv>findsafeNearestNode>while loopcount: " + loopcount);
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
	   //System.out.println("Indv>findsafeNearestNode>nearest: " + nearest);
	   
	   Bag nearestNodes = nodefield.getCoveredObjects(nearest); 
	   //System.out.println("Indv>findsafeNearestNode>nearest Nodes: " + nearestNodes);
	   //System.out.println("Indv>findsafeNearestNode>number of nodes at this point: " + nearestNodes.size());
	   MasonGeometry nearestObj = (MasonGeometry) nearestNodes.get(0);
	   Coordinate nearestNodeCoord = nearestObj.geometry.getCoordinate();
	   //System.out.println("Agent>findNearestNode>node coordinate " + nearestNode.getCoordinate());
	   
	   // Now grab the node in the network at the nearest geometry in the node field.
	   Node nearestNode = null; 
	   
      Iterator<?> nodeIterator = roadNet.nodeIterator();
      
      while (nodeIterator.hasNext()) {
      	// Create a GeometryVectorField of points representing road intersections
          Node node = (Node) nodeIterator.next();
          Coordinate coord = node.getCoordinate();
          if (nearestNodeCoord == coord) {
       	   nearestNode = node;
          }
      }
      //System.out.println("Indv>findsafeNearestNode>nearest node " + nearestNode + " " + nearestNode.getCoordinate());
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
	  //System.out.println("Indv>reroute>findTempLocationNode>");
	  ArrayList<GeomPlanarGraphDirectedEdge> testpath = null;
	  int randomNum = 0;
	  Node randomNode = null; 
	  int count = 0;
	  
	  // look for location 1km away from stuck location --- in some cases less than this produced not enough node locations
	  double dist = .01;  // 1km -- ~.01 degrees
	  // find nodes in the modified roadNetwork within distance
	  Bag nearestNodes = nodeField.getObjectsWithinDistance(this.getGeometry(), dist);
	  //System.out.println("Indv>reroute>findTempLocation>nearestNodes: " + nearestNodes.size());
	  
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
			  //System.out.println("Indv>reroute>findTempLocationNode>no nodes with paths -- need new start");
			  return null;
		  }
		  //System.out.println("Indv>reroute>findTempLocationNode>grabbed node locations: " + numNodes);
	  
		  // pick a random node MasonGeometry by its Index
		  randomNum = new Random().nextInt(numNodes);
		  //System.out.println("Indv>reroute>findTempLocationNode>random: " + randomNum);
		  MasonGeometry randomgeo = (MasonGeometry) nearestNodes.get(randomNum);	  
	  
		  // Use nodeIterator to find and assign the Node in RoadNetwork that is at the random MasonGeometry	   
		  Iterator<?> nodeIterator = roadNet.nodeIterator();
       
		  while (randomNode == null) {
			  //System.out.println("Indv>reroute>findTempLocationNode>while count: " + count);
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
	  
      //System.out.println("Indv>reroute>findTempLocationNode>nodeCoord: " + randomNode.getCoordinate() + " at count: " + count);
	  return randomNode;
  }
    
 
//  private Node findRoadGoalSmallSearch()
//  {
//	   GeometryFactory fact = new GeometryFactory();
//	   Coordinate coord = null;
//	   Point point = null;
//	   Node bestNode = null;
//	   Node node = null;
//	   double minDist = 999;
//	   Iterator<Node> iter = Effects.nodesToGetOnOffRoadNetwork.iterator();
//
//	   while (iter.hasNext())
//	   {
//   	   node = iter.next();
//   	   coord = node.getCoordinate();
//   	   point = fact.createPoint(coord);
//   	   double dist = getLocation().geometry.distance(point);
//   	   if (dist < minDist)
//   	   {
//   		   minDist = dist;
//   		   bestNode = node;
//   	   }
//	   }
////      System.out.println("findNearest via small search... dist= " + minDist + " and node= " + node + " from " + count);
//	   return node;
//  }
  
  
public Coordinate findRandCoord(GeomVectorField water, Coordinate Coord, double speed) {
	  //System.out.println("Indv>nonroutine>getAway>findAltCoord>" + getID() + " blocked by water, fleeing " + Coord);
	  // create 4 test coordinates
	  double move = speed;
	  Coordinate testPosX = new Coordinate((Coord.x+move),Coord.y);
	  Coordinate testNegX = new Coordinate((Coord.x-move),Coord.y);
	  Coordinate testPosY = new Coordinate(Coord.x, (Coord.y+move));
	  Coordinate testNegY = new Coordinate(Coord.x, (Coord.y-move));
	  
	  //System.out.println("testcoords: " + testPosX + " " + testNegX + " " + testPosY + " " + testNegY);
	  
	  MasonGeometry testPosLat = new MasonGeometry(fact.createPoint(testPosX));
	  MasonGeometry testNegLat = new MasonGeometry(fact.createPoint(testNegX));
	  MasonGeometry testPosLong = new MasonGeometry(fact.createPoint(testPosY));
	  MasonGeometry testNegLong = new MasonGeometry(fact.createPoint(testNegY));
	  
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
	  //Bag waterobjects1 = water.getCoveringObjects(testPosLat.getGeometry());
	  if ( waterobjects1.isEmpty() ) {
		  candidateCoords.add(testPosX);
		  east = 0.5;
	  }	  
	  else east = 0.0;	  
	  // test NegX
	  updatePosition(testNegX);
	  Bag waterobjects2 = water.getCoveringObjects(this.getGeometry());
	  //Bag waterobjects2 = water.getCoveringObjects(testNegLat.getGeometry());
	  if ( waterobjects2.isEmpty() ) {
		  candidateCoords.add(testNegX);
		  west = 0.5;
	  }	  
	  else west = 0.0;
	  // test PosY
	  updatePosition(testPosY);
	  Bag waterobjects3 = water.getCoveringObjects(this.getGeometry());
	  //Bag waterobjects3 = water.getCoveringObjects(testPosLong.getGeometry());
	  if ( waterobjects3.isEmpty() ) {
		  candidateCoords.add(testPosY);
		  north = 0.5;
	  }
	  else north = 0.0;
	  // test NegY
	  updatePosition(testNegY);
	  Bag waterobjects4 = water.getCoveringObjects(this.getGeometry());
	  //Bag waterobjects4 = water.getCoveringObjects(testNegLong.getGeometry());
	  if ( waterobjects4.isEmpty() ) {
		  candidateCoords.add(testNegY);
		  south = 0.5;
	  }
	  else south = 0.0;
	  
	  if (candidateCoords.size() == 0) {
		  //System.out.println("Indv>nonroutine>getAway>findAltCoord>no candidates, stay in place");
		  alternativeCoord = Coord;
	  }
	  else {  
		  Coordinate tempCoord = null;
		  // remove any candidates that agent has already visited
//		  for (Object o: candidateCoords) {
//			  tempCoord = (Coordinate) o;
//			  if (tempCoord.equals2D(lastCoord1) || tempCoord.equals2D(lastCoord2)) {
//				  System.out.println("Indv>nonroutine>getAway>findAltCoord> " + lastCoord1 + " " + lastCoord2);
//				  candidateCoords.remove(o);
//			  }
//		  }
		  
		  //System.out.println("Indv>nonroutine>getAway>findAltCoord>candidates: ");
		  
		  // test each remaining candidate coordinate for the distance farthest from ground zero
		  double maxDist = 0;
		  for (Object o: candidateCoords) {		  
			  tempCoord = (Coordinate) o;
			  double tempDist = tempCoord.distance(Parameters.groundZero.getCoordinate()); // for NWMD
			  //double tempDist = tempCoord.distance(Parameters.epicenter);  // for Earthquake
			  //System.out.print(" " + tempCoord + " " + tempDist + " max " + maxDist);
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
	  //System.out.println();
	  //System.out.println("Indv>nonroutine>getAway>findRandCoord>new Coord " + getID() + " " + alternativeCoord );
	  
	  return alternativeCoord;  
  }


public Coordinate findAltCoord(GeomVectorField water, Coordinate Coord, double speed) {
	  //System.out.println("Indv>nonroutine>getAway>findAltGoalPt>" + getID() + " blocked by water");
	  
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
	  //System.out.println("Indv>nonroutine>getAway>findAltGoalPt>candidates: ");
	  
	  
	  // test each candidate coordinate for the distance farthest from ground zero
	  Coordinate tempCoord = null;
	  double maxDist = 0;
	  for (Object o: candidateCoords) {
		  tempCoord = (Coordinate) o;
		  //System.out.print(" " + tempCoord);
		  if ( tempCoord.distance(Parameters.groundZero.getCoordinate()) > maxDist ) {
			  maxDist = tempCoord.distance(Parameters.groundZero.getCoordinate());			  
			  altCoord = tempCoord;
		  }
	  }
	    	  
	  //System.out.println();
	  //System.out.println("Indv>nonroutine>getAway>findAltCoord>new Coord " + getID() + " " + altCoord);
	  
	  return altCoord;  
}


public Coordinate setAltGoalPt(GeomVectorField water, Coordinate Coord, double speed) {
	  //System.out.println("Indv>nonroutine>getAway>findAltGoalPt>" + getID() + " blocked by water");
	  
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
	  //System.out.println("Indv>nonroutine>getAway>findAltGoalPt>candidates: ");
	  
	  
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
	    	  
	  setGoalPoint(altGoal);
	  //System.out.println();
	  //System.out.println("Indv>nonroutine>getAway>findAltGoalPt>new Coord " + getID() + " " + altGoal );
	  
	  return altGoal;  
}

  
  
	//======================================
	//
	//     FIRST RESPONDER BEHAVIOR
	//
	//======================================
  
  /**
   * moveTowards() moves First Responders towards the impact area and aid victims
   * @param currentStep
   */
  void moveTowards(long currentStep, World world) {
	  // method for First Responder to move towards the area, find and assist victims
	  //System.out.println("Indv->step->nonroutine->moveTowards->" + getID() + " health: " + getHealthStatus());

	   if ( (getGoalPoint() == null)  || (this.currentCoord == null) )
	   {
		   //System.out.println("Indv>moveTowards> attempted reacting with no goal or current location... " + fleeing + " " + firstResp + " " + getGoalPoint());
		   return;
	   }

	   int time = Spacetime.time24(currentStep);

//		   System.out.println("Agent>getAway>on foot: health=" + getHealthStatus());
	   		   
	   // Basic spatial data for agents each step
	   double speedrate = Spacetime.degToKilometers(getMoveRateKmPerStep());  // need to convert kilometers to degree speed
	   double cLat 		= currentCoord.y;	// current agent Lat
	   double cLong		= currentCoord.x;
	   double gLat		= getGoalPoint().y;	// goal Lat
	   double gLong		= getGoalPoint().x;
	   double dLat		= gLat - cLat;			// change in Lat distance from current location to goal
	   double dLong		= gLong - cLong;        // change in Long distance from current location to goal
	   double speedLat;							// used for distance agent moves in one step
	   double speedLong;						// used for distance agent moves in one step
	   double dgoal		= Math.sqrt(dLat*dLat + dLong*dLong);	// rough lat/long distance from current location to goal
	   double dgzLat	= Parameters.tLat - cLat;
	   double dgzLong	= Parameters.tLong - cLong;
	   distFromGroundZero	 		= Math.sqrt(dgzLat*dgzLat + dgzLong*dgzLong); // dist from ground zero
	   
	   if (distFromGroundZero > Parameters.z2radius) // if outside R2, move toward ground zero
	   {
   			dLat		= gLat - cLat;		// first responder's goal is ground zero or victim between R2 and R3
   			dLong		= gLong - cLong;
	   }
	   else // inside R2, look for victims
	   {
		   // if reaches the first radius, head outbound
		   if (distFromGroundZero < Parameters.z1radius)   // turn around radius from ground zero
		   {
			   Effects.createFleeingGoalCord(this, world);
			   //System.out.println("Agent>react>responder reached high radiation and reversed");
		   }
		   
//		   System.out.println("Agent>first responder looking for victim");
		   
		   Indv victim = null;
		   
		   victim = findNeediestVictim(Parameters.fleeFastDegreesPerMin); 	// search within 1 step
		   if (victim != null)
		   {
			   	// move to victim
			    currentCoord = victim.currentCoord;
				updatePosition(victim.currentCoord);
				aid(victim, currentStep);
				return;
		   }
		   else // no victims within 1 step, look farther
			   	victim = findNeediestVictim(5*Parameters.fleeFastDegreesPerMin);
		   		if (victim != null)
		   		{
					// move step toward victim (only toward because not within 1 step)
		   			Point here = fact.createPoint(currentCoord);  // first responder's location
					double distToVictim = victim.getLocation().geometry.distance(here);
//					System.out.println("Agent>react>first responder>" + distToVictim + " movement: " + Parameters.fleeSlowestDegreesPerMin);
		   			double convert = speedrate/distToVictim;
		   			
		   			speedLat	= dLat;		// initialize these with full distance to goal
		   			speedLong 	= dLong;
			   		speedLat 	= speedLat * convert;
			   		speedLong 	= speedLong * convert;

			   		// create step toward goal: new position = current position + change in 1 step
			   		double newLat = cLat + speedLat;
			   		double newLong = cLong + speedLong;

					Coordinate newCoord = new Coordinate(newLong, newLat);
					currentCoord = new Coordinate(newLong,newLat);
					updatePosition(newCoord);
		   			return;   // this agent's step done
		   		}
		   		else
		   		{
//		   			System.out.println("Agent>firstResponding> move on");
		   			// if too hot, move away
		   			// else move toward ground zero
		   		}
	   } // look for victims
   }
  
  /**
   * Search
   * @param world
   * @param f		first responder
   * @return		victim agent who is alive, unaided, and worst in range
   * 
   * note: first responder could also be a victim. We exclude first responders
   *       being treated as victims.
   */
  //===========================================================
  Indv findNeediestVictim (Double range)
  {
//	   System.out.println("Agent>search>looking for unaided victims within " + range);
//	   Bag objs = new Bag();
//	   objs = world.agents.getObjectsWithinDistance(location, 0.001);
//	   for (obj o : objs)  
	   Indv victim = null;					// holder for victim
	   
	   // go through victims and determine the unaided on with worst status (if any)
	   for (Indv v: Log.indvList)  //(Indv v : Effects.r23Agents) need to update Effects
	   {	
			if ( (!v.dead) && !v.getIsFirstResp() )	// not dead nor 1st responder
			{
				if ( (v.vStatus == 0) && (v.healthStatus != 0) )	// not yet attended to and needs help
				{
					Point here = fact.createPoint(currentCoord);  // first responder's location
//					double dist = v.getLocation().geometry.distance(here);
//					System.out.println("Agent>findNeediest...>close? dist= " + dist);
					if (v.getLocation().geometry.distance(here) < range)
					{
						if (victim == null) 
						{
							victim = v;
//							System.out.println("Agent>findNeediestVictim> found first");
						}
						else
						{
							if (v.healthStatus > victim.healthStatus)
							{
								// new victim is more badly injured
								victim = v;
//								System.out.println("Agent>findNeediestVictim> updated victim");
							}
						}
//						System.out.println("Agent>search>found victim");
					}
				}
			}
	   }
	   return victim;
  }
  
  /**
   *  aid a victim
   * @param v	victim agent
   * @param current step
   */
  //======================================
  void aid (Indv v, long currentStep)
  {
	   int now = (int) currentStep;
	   
//	   System.out.println("Agent>aiding> victim: " + v + " health= " + v.healthStatus + " aided by " + f);
	   //System.out.println("Agent>aiding> victim health= " + getHealthStatus());
	   
	   if (v.vStatus == 0)	// not yet aided
	   {
		   	v.vStatus = 1;	// mark as being aided
		   	v.vStop = now;	// note end of aid
		   	frStatus = 2;	// mark as providing aid
		   	setHealthStatus(98); // mark as providing aid
  			getGeometry().setUserData(getHealthStatus());
  			Log.inTreatment++;
  			Log.agentsTreated++;
	   }
	   else // so that they don't immediately switch back to searching for a victim
	   if (v.vStatus == 1)// being aided - test stop
	   {
		   Log.inTreatment++;  // reset each step. updates to keep count current.
		   
		   if ( (now - v.vStart) > v.healthStatus)	// up to min of assistance (should be func of health status
		   {
			   v.vStatus = 2;	// aided
			   v.vStop = now;	// set when stopped
			   v.healthStatus = v.healthStatus - 1;		// result of first aid
			   frStatus = 1;	// available to aid
			   setHealthStatus(99); // not aiding victim
			   getGeometry().setUserData(getHealthStatus());   
			   Log.affectedReleased++;
			   //System.out.println("Agent>aid> victim treated and released");
		   }
	   }
	   	   	   
  }
  
  
  
	//======================================
	//
	//     GROUPING
	//
	//======================================
  
  
  boolean joinGroup() {  
	  boolean madeGroup = false;  
	  //System.out.println("Indv->step->joinGroup->location: " + this.getLocation());
	  
      // check geometries:
//      Bag geometries = this.state.agents.getGeometries();
//      for (int i =  0; i < geometries.size(); i++) {
//      	MasonGeometry geometry = (MasonGeometry) geometries.objs[i];
//      	if (geometry.isMovable) {
//      		
//      	}
//      }
	  
	  // Assume agents stay in place at first
	  
	  // Check for nearest groups
	  Bag nearestGroups = this.state.groups.getObjectsWithinDistance(this.getLocation(), .001); // within .1 km distance
	  
	  if (nearestGroups.isEmpty()) { 
		  //System.out.println("Indv->step->joinGroup->nearestGroups is Empty");	  
		  madeGroup = makeGroup();
		  
	  }
	  else {
		  //System.out.println("Indv->step->joinGroups->nearestGroups: ");
		  for (Object grp: nearestGroups) {
			  //System.out.println(grp);
			  Group gAgent = (Group)  (((MasonGeometry) grp).getUserData());
			  int gSize = gAgent.idsToMem.size();
			  if (gSize < Parameters.maxGrpSize && gAgent.getGoal() == this.getGoal()) {
				  gAgent.addMember(this);
				  //System.out.println("Indv->step->joinGroup->joined group: " + gAgent.getID());
			  }
			  else { // create new group
				  if (gAgent.getGoal() == this.getGoal()) {
					  madeGroup = makeGroup();
				  }
				  else {
					  //System.out.println("Indv->step->joinGroup->no group Goal");
				  }
			  }
		  }
	  }

	  //System.out.println("Indv->step->joinGroup->nearestGroups: " + this.getID() + " Bag Size: " +  nearestGroups.size());
	  
	  //Iterator i = nearestGroups.iterator();
	  // for loop to verify group id and goal
	  for (Object g: nearestGroups) {
		  Group grp = (Group)  (((MasonGeometry) g).getUserData());
		  String grpID = grp.getID();
		  String grpGoal = grp.getGoal();
		  //System.out.println("Indv: " + grp + " ID: " + grpID + " goal: " + grpGoal);
	  }
	  
	  return madeGroup;
	  
  }
  
  
  /**
   * Makes a group of two individuals -- emergent group
   */
boolean makeGroup() {
	  // Check for nearest individuals
	  // should be set to .00001 for a 1 meter distance
	  //System.out.println("Indv->step->joinGroup->makeGroup->" + this.getID() + " goal: " + this.getGoal());
	  Bag nearestIndvs = this.state.indvs.getObjectsWithinDistance(this.getLocation(), .001); // within .1 km distance
	  
	  if (nearestIndvs.isEmpty()) {
		  //System.out.println("Indv->step->joinGroup->makeGroup->nearestIndvs is Empty; no indvs near " + this.getID());
		  return false;
	  }  
	  
	  else {
		  //System.out.println("Indv->step->joinGroup->makeGroup->nearestIndvs:" + this.getID() + " Bag Size: " +  nearestIndvs.size());
		  boolean createGrp = createEmerGroup(nearestIndvs);
		  return createGrp;
		  
//		  Boolean madeGrp = false;
//		  
//		  for (Object a: nearestIndvs) {
//			  if (!madeGrp) { // loop through the bag of individuals, if we haven't made a group
//				  Indv agent = (Indv)  (((MasonGeometry) a).getUserData());
//				  
//				  if (agent.getinGroup()) { // if agent is in group don't consider it
//					  //System.out.println("Indv->step->makeGroup->individual already in group:" + agent.getID());
//				  }
//				  // create group if agent's goal matches another agent's goal
//				  else { 
//					  String agentID = agent.getID();
//					  if (agentID != this.getID()) {
//						  String agentGoal = agent.getGoal();
//						  //System.out.println("Indv: " + agent + " ID: " + agentID + " goal: " + agentGoal);
//						  if (agentGoal == this.getGoal()) {
//							  Integer grpID = state.idsToGrps.size(); // create group id
//							  System.out.println("Indv>joinGroup>makeGroup>leader " + agent.getStartNode() + " " + agent.getStartNode().getCoordinate());
//							  System.out.println("Indv>joinGroup>makeGroup>leader " + agent.getHomeNode() + " " + agent.getHomeNode().getCoordinate());
//							  Group g = new Group(state, grpID, agent, this, "emergent");
//							  madeGrp = true;
//							  // make the group movable & steppable in the model
//							  MasonGeometry newGeometry = g.getGeometry();
//							  newGeometry.isMovable = true;
//							  Log.grpList.add(g); // Arraylist of the groups
//							  state.schedule.scheduleRepeating(g);
//							  Log.grouppopulation++;
//							  state.groups.addGeometry(newGeometry);
//							  
//							  //System.out.println("Indv->joinGroup->makeGroup->group created: " + g.getID());
//							  //System.out.println("Indv->joinGroup->makeGroup->group members: ");
//							  for (String id: g.idsToMem.keySet()) {
//								  //String key = id;
//								  System.out.print(" " + id);
//							  }
//							  System.out.println();
//							  //System.out.println("Indv->joinGroup->makeGroup->inGroup: " + this.getinGroup() + " " + agent.getinGroup());
//						  }
//					  }
//					  else {
//						  //System.out.println("Indv->joinGroup->makeGroup->can't form group with itself:" + agent.getID());
//					  }
//				  }
//			  }
//		  }
//		  
//		  return madeGrp;
	  }
  }

// Method to make an emergent group
boolean createEmerGroup(Bag nearestIndvs) {
	  //System.out.println("Indv>step>joinGroup>makeGroup>creatEmerGroup>nearestIndvs:" + this.getID() + " Bag Size: " +  nearestIndvs.size());
	  Boolean madeGrp = false;
	  
	  for (Object a: nearestIndvs) {
		  if (!madeGrp) { // loop through the bag of individuals, if we haven't made a group
			  Indv agent = (Indv)  (((MasonGeometry) a).getUserData());
			  
			  if (agent.getinGroup()) { // if agent is in group don't consider it
				  //System.out.println("Indv->step->makeGroup->individual already in group:" + agent.getID());
			  }
			  // create group if agent's goal matches this agent's goal
			  else { 
				  String agentID = agent.getID();
				  if (agentID != this.getID()) {
					  String agentGoal = agent.getGoal();
					  //System.out.println("Indv: " + agent + " ID: " + agentID + " goal: " + agentGoal);
					  if (agentGoal == this.getGoal()) {
						  Integer grpID = state.idsToGrps.size(); // create group id
						  //System.out.println("Indv>joinGroup>makeGroup>leader " + agent.getStartNode() + " " + agent.getStartNode().getCoordinate());
						  //System.out.println("Indv>joinGroup>makeGroup>leader " + agent.getHomeNode() + " " + agent.getHomeNode().getCoordinate());
						  Group g = new Group(state, serialVersionUID, grpID, agent, this, "emergent");
						  madeGrp = true;
						  Log.emerGroups += 1;
						  // make the group movable & steppable in the model
						  MasonGeometry newGeometry = g.getGeometry();
						  newGeometry.isMovable = true;
						  Log.grpList.add(g); // Arraylist of the groups
						  state.schedule.scheduleRepeating(g);
						  Log.grouppopulation++;
						  state.groups.addGeometry(newGeometry);
						  state.emerGroups.add(g.getID()); // add group id to list of emergent groups
						  
						  //System.out.println("Indv->joinGroup->makeGroup->group created: " + g.getID());
						  //System.out.println("Indv->joinGroup->makeGroup->group members: ");
						  for (String id: g.idsToMem.keySet()) {
							  //String key = id;
							  //System.out.print(" " + id);
						  }
						  //System.out.println();
						  //g.updateHealth(); // updates health average and new movement speed
						  //System.out.println("Indv->joinGroup->makeGroup->inGroup: " + this.getinGroup() + " " + agent.getinGroup());
					  }
				  }
				  else {
					  //System.out.println("Indv->joinGroup->makeGroup->can't form group with itself:" + agent.getID());
				  }
			  }
		  }
	  }
	  
	  return madeGrp;
}
  
// method to leave group if goals do not match
// need to add multiple nodes????
boolean leaveGroup(Group grp) {
	//System.out.println("Indv>leaveGroup> " + getID() + " groupGoal " + grp.getGoal() + " size: " + grp.getSize());		
	String agentGoal = getGoal();
	String groupGoal = grp.getGoal();
	//System.out.println("Indv>leaveGroup> " + getID() + " agentGoal " + agentGoal + " group " + groupGoal + " size: " + grp.getSize());		
	
	if (agentGoal != groupGoal && grp.getGoalNode().equals(getGoalNode())) {
		//System.out.println("Indv>leaveGroup> goals & goal nodes do notmatch " + getGoalNode() + " " + grp.getGoalNode());
		
		// if the member was a leader, find new leader
		if (grp.getLeaderID() == getID()) {
			//System.out.println("Indv>leaveGroup>old leader " + grp.getLeaderID());
			grp.remMember(this);
			// choose new leader
			grp.selectSeniorLead();
			//System.out.println("Indv>leaveGroup>new leader " + grp.getLeaderID());
			//System.out.println("Indv>leaveGroup>updated group size " + grp.getSize());
			return true;
		}		
		else {
			grp.remMember(this);
			//System.out.println("Indv>leaveGroup>updated group size " + grp.getSize());
			return true;
		}
		
	}
	else {
		//System.out.println("Indv>leaveGroup>" + getID() + " stays in group");	
		return false;
	}
}


/**
 * Method to quickly get an agent's group
 * @param id
 * @param world
 * @return
 */
public Group getGrp(String id, World world)	{ 
	Group grp = world.idsToGrps.get(id);
	return grp;
}


  
	//======================================
	//
	//     TESTING
	//
	//======================================
  
  
  /**
   * Test Code Prints out Agent characteristics to a utility file in Results Class
   */
  void printAgent(String agentID, long step) {
	   if(getID().equals(agentID)) {
		   try {
			   Results.writeLine(this.getID()+
					   		"\t"+ step+ 
					   		"\t"+ this.getHomeNode().getCoordinate().x+ 
					   		"\t"+ this.getHomeNode().getCoordinate().y+ 
					   		"\t"+ this.getWorkNode().getCoordinate().x+ 
					   		"\t"+ this.getWorkNode().getCoordinate().y+ 
					   		"\t"+ this.getGoal() +
					   		"\t"+ this.currentCoord.x+ 
					   		"\t"+ this.currentCoord.y+ 
					   		"\t" + this.ToWork+ 
					   		"\t"+this.atWork+
					   		"\t"+this.reachedDestination);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   }
  }
  
    
}
